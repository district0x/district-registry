(ns district-registry.ui.components.stake
  (:require
    [bignumber.core :as bn]
    [cljs-web3.core :as web3]
    [district-registry.shared.utils :refer [debounce]]
    [district-registry.ui.contract.district :as district]
    [district-registry.ui.subs :as subs]
    [district.format :as format]
    [district.graphql-utils :as gql-utils]
    [district.parsers :as parsers]
    [district.ui.component.form.input :refer [text-input]]
    [district.ui.component.tx-button :refer [tx-button]]
    [district.ui.graphql.subs :as gql]
    [district.ui.web3-accounts.subs :as account-subs]
    [district.ui.web3-tx-id.subs :as tx-id-subs]
    [district.web3-utils :as web3-utils]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))


(defn- subscribe-stake-info [district-address]
  (let [active-account (subscribe [::account-subs/active-account])]
    (subscribe [::gql/query
                {:queries [[:district {:reg-entry/address district-address}
                            [:reg-entry/address
                             :reg-entry/status
                             :district/total-supply
                             :district/dnt-staked
                             :district/name
                             :district/stake-bank
                             [:district/dnt-staked-for {:staker @active-account}]
                             [:district/balance-of {:staker @active-account}]]]]}
                {:refetch-on #{::district/approve-and-stake-for-success
                               ::district/unstake-success}}])))

(def debounced-estimate-return-for-stake
  (debounce
    (fn [amount stake-bank]
      (dispatch [::district/estimate-return-for-stake {:amount amount :stake-bank stake-bank}]))
    500))


(defn stake-form []
  (let [form-data (r/atom {:dnt-amount ""})
        dnt-balance (subscribe [:district.ui.web3-account-balances.subs/active-account-balance :DNT])]
    (fn [reg-entry-address {:keys [:disable-estimated-return?]}]
      (let [stake-info @(subscribe-stake-info reg-entry-address)
            stake-tx-pending? @(subscribe [::tx-id-subs/tx-pending? {:approve-and-stake-for {:district reg-entry-address}}])
            unstake-tx-pending? @(subscribe [::tx-id-subs/tx-pending? {:unstake {:district reg-entry-address}}])
            district (:district stake-info)
            reg-entry-status (:reg-entry/status district)
            dnt-staked-for (:district/dnt-staked-for district)
            parsed-dnt-amount (parsers/parse-float (:dnt-amount @form-data))
            estimated-return-for-stake @(subscribe [::subs/estimated-return-for-stake
                                                    (:district/stake-bank district)
                                                    parsed-dnt-amount])]
        [:div.box-cta.stake-form
         [:form
          [:div.form-btns
           [:div.cta-btns
            (when (and reg-entry-status
                       (not= (gql-utils/gql-name->kw reg-entry-status)
                             :reg-entry.status/blacklisted))
              [tx-button {:class "cta-btn"
                          :disabled (or (not (bn/< 0 @dnt-balance))
                                        (not (pos? parsed-dnt-amount))
                                        unstake-tx-pending?)
                          :pending-text "Staking"
                          :pending? stake-tx-pending?
                          :on-click (fn [e]
                                      (js-invoke e "preventDefault")
                                      (dispatch [::district/approve-and-stake-for
                                                 {:reg-entry/address reg-entry-address
                                                  :district/name (:district/name district)
                                                  :amount (-> (:dnt-amount @form-data)
                                                            parsers/parse-float
                                                            web3-utils/eth->wei)}]))}
               "Stake"])
            [tx-button {:class "cta-btn"
                        :disabled (or stake-tx-pending?
                                      (not dnt-staked-for)
                                      (not (pos? parsed-dnt-amount))
                                      (> parsed-dnt-amount (web3-utils/wei->eth-number dnt-staked-for)))
                        :pending-text "Unstaking"
                        :pending? unstake-tx-pending?
                        :on-click (fn [e]
                                    (js-invoke e "preventDefault")
                                    (dispatch [::district/unstake
                                               {:reg-entry/address reg-entry-address
                                                :district/name (:district/name district)
                                                :amount (-> (:dnt-amount @form-data)
                                                          parsers/parse-float
                                                          web3-utils/eth->wei)}]))}
             "Unstake"]]
           [:fieldset
            [text-input
             {:class "dnt-input"
              :id :dnt-amount
              :type :number
              :form-data form-data
              :on-change #(debounced-estimate-return-for-stake % (:district/stake-bank district))
              :errors {:local {:dnt-amount {:hint (if (and (pos? estimated-return-for-stake)
                                                           (not disable-estimated-return?))
                                                    (str "You'll receive "
                                                         (format/format-number estimated-return-for-stake)
                                                         " gov. tokens")
                                                    "\u00a0")}}}}]
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
