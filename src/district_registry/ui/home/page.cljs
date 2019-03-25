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
  [:div.grid-box
   [:div.box-image {:on-click #(dispatch [:district.ui.router.events/navigate :route/detail {:address address}])}
    [district-image background-image-hash]]
   [:div.box-text
    [:div.box-logo.sized {:on-click #(dispatch [:district.ui.router.events/navigate :route/detail {:address address}])}
     [district-image logo-image-hash]]
    [:div.inner
     [:h2 name]
     [:p description]
     [:div.h-line]
     [stake/stake-info address]
     [stake/stake-form address]]
    [:div.arrow-blob {:style {:background-image "url(images/module-arrow-blob@2x.png)"}}
     [:a {:on-click #(dispatch [:district.ui.router.events/navigate :route/detail {:address address}])}
      [:span.arr.icon-arrow-right]]]]])

(defn district-tiles [active-account form-data]
  (let [q (subscribe [::gql/query
                      {:queries [(build-query active-account form-data)]}
                      {:refetch-on #{::district/approve-and-stake-for-success
                                     ::district/unstake-success
                                     }}])]
    [:div.grid.spaced
     (->> @q
       :search-districts
       :items
       (map (fn [{:as district
                  :keys [:reg-entry/address]}]
              ^{:key address} [district-tile district]))
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
        active-account (subscribe [::account-subs/active-account])
        select-menu-open? (r/atom false)]
    (fn []
      [app-layout
       [:section#intro
        [:div.bg-wrap
         [:div.background.sized
          [:img {:src "images/blobbg-top@2x.png"}]]]
        [:div.container
         [:nav.subnav
          [:ul
           [:li {:class (when (= @status "in-registry") "on")}
            [:a.cta-btn {:id "in-registry"
                         :on-click status-handler}
             "In Registry"]]
           [:li {:class (when (= @status "challenged") "on")}
            [:a.cta-btn {:id "challenged"
                         :on-click status-handler}
             "Challenged"]]
           [:li {:class (when (= @status "blacklisted") "on")}
            [:a {:id "blacklisted"
                 :on-click status-handler}
             "Blacklisted"]]]]
         [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat aute irure dolor in reprehenderit."]]]
       [:section#registry-grid
        [:div.container
         [:div.select-menu {:class (when @select-menu-open? "on")
                            :on-click #(swap! select-menu-open? not) }
          [:div.select-choice.cta-btn
           [:div.select-text "Most Staked"]
           [:div.arrow [:span.arr.icon-arrow-down]]]
          [:div.select-drop
           [:ul
            [:li [:a {:href "#"
                      :id "created-on"
                      :checked (= (:order-by @form-data) :districts.order-by/created-on)
                      :on-click order-by-handler}
                  "Creation Date"]]
            [:li [:a {:href "#"
                      :id "total-supply"
                      :checked (= (:order-by @form-data) :districts.order-by/total-supply)
                      :on-click order-by-handler}
                  "Total Supply"]]
            [:li [:a {:href "#"
                      :id "dnt-staked"
                      :checked (= (:order-by @form-data) :districts.order-by/dnt-staked)
                      :on-click order-by-handler}
                  "DNT Staked"]]]]]]
        [district-tiles @active-account @form-data]]])))
