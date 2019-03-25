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

(defn stake-form [district-address]
  (let [address district-address
        input (r/atom "1")
        on-change (fn [e]
                    (let [s (-> e .-target .-value)
                          i (js/parseInt s)]
                      (if (or
                            (and
                              (integer? i)
                              (pos? i)))
                        (reset! input s))))]
    (fn [address]
      [:div.box-cta
       [:form
        [:div.form-btns
         [:div.cta-btns
          [:a.cta-btn {:href "#"
                       :on-click (fn []
                                   (dispatch [::district/approve-and-stake-for {:address address
                                                                                :dnt (-> @input
                                                                                       web3/to-big-number
                                                                                       (web3/to-wei :ether))}]))}
           "Stake"]
          [:a.cta-btn {:href "#"
                       :on-click (fn []
                                   (dispatch [::district/unstake {:address address
                                                                  :dnt (-> @input
                                                                         web3/to-big-number
                                                                         (web3/to-wei :ether))}]))}
           "Unstake"]]
         [:fieldset
          [:input {:type "number"
                   :value @input
                   :on-change on-change}]
          [:span.cur "DNT"]]]]])))

(defn stake-info [district-address]
  (let [active-account (subscribe [::account-subs/active-account])
        query (subscribe [::gql/query
                          {:queries [[:district {:reg-entry/address district-address}
                                      [:reg-entry/address
                                       :district/total-supply
                                       [:district/dnt-staked-for {:staker @active-account}]
                                       [:district/balance-of {:staker @active-account}]]]]}
                          {:refetch-on #{::district/approve-and-stake-for-success
                                         ::district/unstake-success}}])]
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
             (web3/from-wei :ether)
             format/format-token)
           " (" (if (and (bn/bignumber? balance-of) (.isPositive balance-of))
                  (.times (.div balance-of total-supply) 100)
                  0) "%) "
           "governance tokens")]))))
