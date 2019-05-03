(ns district-registry.ui.components.stake
  (:require
   [bignumber.core :as bn]
   [cljs-web3.core :as web3]
   [clojure.pprint :refer [pprint]]
   [district-registry.ui.contract.district :as district]
   [district.format :as format]
   [district.ui.graphql.subs :as gql]
   [district.ui.web3-accounts.subs :as account-subs]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]))

(defn ^:private gql-stake-info [district-address]
  (let [active-account (subscribe [::account-subs/active-account])]
    (subscribe [::gql/query
                {:queries [[:district {:reg-entry/address district-address}
                            [:reg-entry/address
                             :district/total-supply
                             [:district/dnt-staked-for {:staker @active-account}]
                             [:district/balance-of {:staker @active-account}]]]]}
                {:refetch-on #{::district/approve-and-stake-for-success
                               ::district/unstake-success}}])))

(defn stake-form [district-address]
  (let [input (r/atom "1")
        on-change (fn [e]
                    (let [s (-> e .-target .-value)
                          i (js/parseInt s)]
                      (if (or
                            (and
                              (integer? i)
                              (pos? i)))
                        (reset! input s))))
        query (gql-stake-info district-address)
        dnt-balance (subscribe [:district.ui.web3-account-balances.subs/active-account-balance :DNT])]
    (fn [district-address]
      [:div.box-cta.stake-form
       [:form
        [:div.form-btns
         [:div.cta-btns
          [:button.cta-btn {:disabled (not (and
                                             (bn/bignumber? @dnt-balance)
                                             (bn/< 0 @dnt-balance)))
                            :on-click (fn [e]
                                        (.preventDefault e)
                                        (dispatch [::district/approve-and-stake-for {:address district-address
                                                                                     :dnt (-> @input
                                                                                            web3/to-big-number
                                                                                            (web3/to-wei :ether))}]))}
           "Stake"]
          [:button.cta-btn {:disabled (->> @query
                                        :district
                                        :district/dnt-staked-for
                                        not)
                            :on-click (fn [e]
                                        (.preventDefault e)
                                        (dispatch [::district/unstake {:address district-address
                                                                       :dnt (-> @input
                                                                              web3/to-big-number
                                                                              (web3/to-wei :ether))}]))}
           "Unstake"]]
         [:fieldset
          [:input.stake-input
           {:type "number"
            :value @input
            :on-change on-change}]
          [:span.cur "DNT"]]]]])))

(defn stake-info [district-address]
  (let [query (gql-stake-info district-address)]
    (fn []
      (when-not (:graphql/loading? @query)
        (let [{:as district
               :keys [:district/total-supply
                      :district/dnt-staked-for
                      :district/balance-of]} (:district @query)]
          [:p
           (str "You staked " (-> dnt-staked-for
                                (web3/from-wei :ether)
                                format/format-dnt))
           [:br]
           (str "Owning "
             (-> balance-of
               web3/to-big-number
               (web3/from-wei :ether)
               .toNumber
               (format/format-number {:min-fraction-digits 2
                                      :max-fraction-digits 2}))
             " (" (if (and (bn/bignumber? balance-of) (.isPositive balance-of))
                    (->
                      (.div balance-of total-supply)
                      (.times 100)
                      .toNumber
                      (format/format-number {:min-fraction-digits 2
                                             :max-fraction-digits 2}))
                    0) "%) "
             "governance tokens")])))))
