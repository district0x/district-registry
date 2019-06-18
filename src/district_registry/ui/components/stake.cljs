(ns district-registry.ui.components.stake
  (:require
    [bignumber.core :as bn]
    [cljs-web3.core :as web3]
    [clojure.pprint :refer [pprint]]
    [district-registry.ui.contract.district :as district]
    [district.format :as format]
    [district.parsers :as parsers]
    [district.ui.component.tx-button :refer [tx-button]]
    [district.ui.graphql.subs :as gql]
    [district.ui.web3-accounts.subs :as account-subs]
    [district.ui.web3-tx-id.subs :as tx-id-subs]
    [district.web3-utils :as web3-utils]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [district.graphql-utils :as gql-utils]))


(defn- subscribe-stake-info [district-address]
  (let [active-account (subscribe [::account-subs/active-account])]
    (subscribe [::gql/query
                {:queries [[:district {:reg-entry/address district-address}
                            [:reg-entry/address
                             :reg-entry/status
                             :district/total-supply
                             :district/dnt-staked
                             :district/name
                             [:district/dnt-staked-for {:staker @active-account}]
                             [:district/balance-of {:staker @active-account}]]]]}
                {:refetch-on #{::district/approve-and-stake-for-success
                               ::district/unstake-success}}])))


(defn stake-form []
  (let [form-amount (r/atom nil)
        on-change (fn [e]
                    (reset! form-amount (aget e "target" "value")))
        dnt-balance (subscribe [:district.ui.web3-account-balances.subs/active-account-balance :DNT])]
    (fn [reg-entry-address]
      (let [stake-info @(subscribe-stake-info reg-entry-address)
            stake-tx-pending? @(subscribe [::tx-id-subs/tx-pending? {:approve-and-stake-for {:district reg-entry-address}}])
            unstake-tx-pending? @(subscribe [::tx-id-subs/tx-pending? {:unstake {:district reg-entry-address}}])
            district (:district stake-info)
            reg-entry-status (:reg-entry/status district)
            dnt-staked-for (:district/dnt-staked-for district)
            parsed-form-amount (parsers/parse-float @form-amount)]
        [:div.box-cta.stake-form
         [:form
          [:div.form-btns
           [:div.cta-btns
            (when (and reg-entry-status
                       (not= (gql-utils/gql-name->kw reg-entry-status)
                             :reg-entry.status/blacklisted))
              [tx-button {:class "cta-btn"
                          :disabled (or (not (bn/< 0 @dnt-balance))
                                        (not (pos? parsed-form-amount))
                                        unstake-tx-pending?)
                          :pending-text "Staking"
                          :pending? stake-tx-pending?
                          :on-click (fn [e]
                                      (js-invoke e "preventDefault")
                                      (dispatch [::district/approve-and-stake-for
                                                 {:reg-entry/address reg-entry-address
                                                  :district/name (:district/name district)
                                                  :amount (-> @form-amount
                                                            parsers/parse-float
                                                            web3-utils/eth->wei)}]))}
               "Stake"])
            [tx-button {:class "cta-btn"
                        :disabled (or stake-tx-pending?
                                      (not dnt-staked-for)
                                      (not (pos? parsed-form-amount))
                                      (> parsed-form-amount (web3-utils/wei->eth-number dnt-staked-for)))
                        :pending-text "Unstaking"
                        :pending? unstake-tx-pending?
                        :on-click (fn [e]
                                    (js-invoke e "preventDefault")
                                    (dispatch [::district/unstake
                                               {:reg-entry/address reg-entry-address
                                                :district/name (:district/name district)
                                                :amount (-> @form-amount
                                                          parsers/parse-float
                                                          web3-utils/eth->wei)}]))}
             "Unstake"]]
           [:fieldset
            [:input.dnt-input
             {:type :number
              :value @form-amount
              :on-change on-change}]
            [:span.cur "DNT"]]]]]))))


(defn stake-info []
  (fn [district-address]
    (let [query (subscribe-stake-info district-address)
          {:keys [:district/total-supply :district/dnt-staked-for :district/balance-of :district/dnt-staked]} (:district @query)
          total-supply (web3-utils/wei->eth-number total-supply)
          balance-of (web3-utils/wei->eth-number balance-of)
          dnt-staked-for (web3-utils/wei->eth-number dnt-staked-for)
          dnt-staked (web3-utils/wei->eth-number dnt-staked)]
      [:p
       (str "Total staked: " (or (-> dnt-staked format/format-dnt)
                                 "0 DNT"))
       [:br]
       (str "You staked: " (or (-> dnt-staked-for format/format-dnt)
                               "0 DNT"))
       [:br]
       (str "You own: "
            (or (format/format-number balance-of)
                0)
            (when (pos? balance-of)
              (str " (" (-> (/ balance-of total-supply)
                          (* 100)
                          format/format-number)
                   "%)"))
            " governance tokens")])))
