(ns district-registry.ui.home.page
  (:require
   [bignumber.core :as bn]
   [cljs-web3.core :as web3]
   [clojure.pprint :refer [pprint]]
   [district-registry.ui.components.app-layout :refer [app-layout]]
   [district-registry.ui.components.stake :as stake]
   [district-registry.ui.contract.district :as district]
   [district.format :as format]
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]
   [district.ui.web3-accounts.subs :as account-subs]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]))

(defn build-query [active-account form-data]
  [:search-districts
   form-data
   [:total-count
    :end-cursor
    :has-next-page
    [:items [:reg-entry/address
             :reg-entry/version
             :reg-entry/status
             :reg-entry/creator
             :reg-entry/deposit
             :reg-entry/created-on
             :reg-entry/challenge-period-end
             [:reg-entry/challenges
              [:challenge/challenger]]
             :district/meta-hash
             :district/name
             :district/description
             :district/url
             :district/github-url
             :district/logo-image-hash
             :district/background-image-hash
             :district/dnt-weight
             :district/dnt-staked
             :district/total-supply
             [:district/dnt-staked-for {:staker active-account}]
             [:district/balance-of {:staker active-account}]]]]])

(defn district-image [image-hash]
  (when image-hash
    (let [gateway (subscribe [::gql/query
                              {:queries [[:config
                                          [[:ipfs [:gateway]]]]]}])]
      (when-not (:graphql/loading? @gateway)
        (if-let [url (-> @gateway :config :ipfs :gateway)]
          [:img.district-image {:src (str (format/ensure-trailing-slash url) image-hash)}])))))

(defn district-tile [{:as district
                      :keys [:district/background-image-hash
                             :district/balance-of
                             :district/description
                             :district/dnt-staked
                             :district/dnt-staked-for
                             :district/github-url
                             :district/meta-hash
                             :district/logo-image-hash
                             :district/name
                             :district/total-supply
                             :district/url
                             :reg-entry/address
                             :reg-entry/challenges
                             :reg-entry/deposit
                             :reg-entry/version]}]
  [:div
   [:div {:on-click #(dispatch [:district.ui.router.events/navigate :route/detail {:address address}])}
    [:h2 name ]
    [:h3 description]
    [district-image logo-image-hash]
    [district-image background-image-hash]]
   [stake/stake-info address]
   [stake/stake-form address]])

(defn district-tiles [active-account form-data]
  (let [q (subscribe [::gql/query
                      {:queries [(build-query active-account form-data)]}
                      {:refetch-on #{::district/approve-and-stake-for-success
                                     ::district/unstake-success
                                     }}])]
    [:ul
     (->> @q
       :search-districts
       :items
       (map (fn [{:as district
                  :keys [:reg-entry/address]}]
              [:li {:key address}
               [district-tile district]]))
       doall)]))

(defmethod page :route/home []
  (let [form-data (r/atom
                    {:first 10
                     :statuses [:reg-entry.status/challenge-period
                                :reg-entry.status/commit-period
                                :reg-entry.status/reveal-period
                                :reg-entry.status/whitelisted]
                     :order-by :districts.order-by/created-on
                     :order-dir :asc})
        order-dir-handler (fn [event]
                            (swap! form-data assoc :order-dir
                              (if (-> event
                                    .-target
                                    .-id
                                    (= "asc"))
                                :asc
                                :desc)))
        status (r/atom "in-registry")
        status-handler (fn [event]
                         (reset! status (-> event .-target .-id))
                         (swap! form-data assoc :statuses
                           (condp = (-> event .-target .-id)
                             "in-registry" [:reg-entry.status/challenge-period
                                            :reg-entry.status/commit-period
                                            :reg-entry.status/reveal-period
                                            :reg-entry.status/whitelisted]
                             "challenged"  [:reg-entry.status/commit-period
                                            :reg-entry.status/reveal-period]

                             "blacklisted" [:reg-entry.status/blacklisted])))
        order-by-handler (fn [event]
                           (swap! form-data assoc :order-by
                             (keyword "districts.order-by"
                               (-> event .-target .-id ))))
        active-account (subscribe [::account-subs/active-account])]
    (fn []
      [app-layout
       [:div
        [:form
         [:fieldset
          [:legend "Status"]
          [:label "In Registry"
           [:input {:id "in-registry"
                    :type "radio"
                    :checked (= @status "in-registry")
                    :on-change status-handler}]]
          [:label "Challenged"
           [:input {:id "challenged"
                    :type "radio"
                    :checked (= @status "challenged")
                    :on-change status-handler}]]
          [:label "Blacklisted"
           [:input {:id "blacklisted"
                    :type "radio"
                    :checked (= @status "blacklisted")
                    :on-change status-handler}]]]
         [:fieldset
          [:legend "Order By"]
          [:label "created-on"
           [:input {:id "created-on"
                    :type "radio"
                    :checked (= (:order-by @form-data) :districts.order-by/created-on)
                    :on-change order-by-handler}]]
          [:label "total-supply"
           [:input {:id "total-supply"
                    :type "radio"
                    :checked (= (:order-by @form-data) :districts.order-by/total-supply)
                    :on-change order-by-handler}]]
          [:label "dnt-staked"
           [:input {:id "dnt-staked"
                    :type "radio"
                    :checked (= (:order-by @form-data) :districts.order-by/dnt-staked)
                    :on-change order-by-handler}]]
          [:label "commit-period"
           [:input {:id "commit-period-end"
                    :type "radio"
                    :checked (= (:order-by @form-data) :districts.order-by/commit-period-end)
                    :on-change order-by-handler}]]
          [:label "reveal-period"
           [:input {:id "reveal-period-end"
                    :type "radio"
                    :checked (= (:order-by @form-data) :districts.order-by/reveal-period-end)
                    :on-change order-by-handler}]]]
         [:fieldset
          [:legend "Order Direction"]
          [:label "Ascending"
           [:input {:id "asc"
                    :type "radio"
                    :checked (= (:order-dir @form-data) :asc)
                    :on-change order-dir-handler}]]
          [:label "Descending"
           [:input {:id "desc"
                    :type "radio"
                    :checked (= (:order-dir @form-data) :desc)
                    :on-change order-dir-handler}]]]]
        [district-tiles @active-account @form-data]]])))
