(ns district-registry.ui.home.page
  (:require
    [bignumber.core :as bn]
    [cljs-web3.core :as web3]
    [clojure.pprint :refer [pprint]]
    [district-registry.ui.components.app-layout :refer [app-layout]]
    [district-registry.ui.components.nav :as nav]
    [district-registry.ui.components.stake :as stake]
    [district-registry.ui.contract.district :as district]
    [district.format :as format]
    [district.ui.component.page :refer [page]]
    [district.ui.graphql.subs :as gql]
    [district.ui.router.subs :as router-subs]
    [district.ui.web3-accounts.subs :as account-subs]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn build-query [active-account route-query]
  [:search-districts
   {:order-by (keyword "districts.order-by" (:order-by route-query))
    :order-dir :desc
    :statuses (case (:status route-query)
                "in-registry" [:reg-entry.status/challenge-period
                               :reg-entry.status/commit-period
                               :reg-entry.status/reveal-period
                               :reg-entry.status/whitelisted]
                "challenged" [:reg-entry.status/commit-period
                              :reg-entry.status/reveal-period]
                "blacklisted" [:reg-entry.status/blacklisted])
    :first 100}
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


(defn district-tile [{:keys [:district/background-image-hash
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
  (let [nav-to-details-props {:style {:cursor "pointer"}
                              :on-click #(dispatch [:district.ui.router.events/navigate
                                                    :route/detail
                                                    {:address address}])}]
    [:div.grid-box
     [:div.box-image nav-to-details-props
      [district-image background-image-hash]]
     [:div.box-text
      [:div.box-logo.sized nav-to-details-props
       [district-image logo-image-hash]]
      [:div.inner
       [:h2 nav-to-details-props (format/truncate name 64)]
       [:p nav-to-details-props (format/truncate description 200)]
       [:div.h-line]
       [stake/stake-info address]
       [stake/stake-form address]]
      [:div.arrow-blob
       (nav/a {:route [:route/detail {:address address}]}
              [:span.arr.icon-arrow-right])]]]))


(defn loader []
  (let [mounted? (r/atom false)]
    (fn []
      (when-not @mounted?
        (js/setTimeout #(swap! mounted? not)))
      [:div#loader-wrapper {:class (str "fade-in" (when @mounted? " visible"))}
       [:div#loader
        [:div.loader-graphic
         ;; [:img.blob.spacer {:src "/images/svg/loader-blob.svg"}]
         [:div.loader-floater
          [:img.bg.spacer {:src "/images/svg/loader-bg.svg"}]
          [:div.turbine
           [:img.base {:src "/images/svg/turbine-base.svg"}]
           [:div.wheel [:img {:src "/images/svg/turbine-blade.svg"}]]
           [:img.cover {:src "/images/svg/turbine-cover.svg"}]]
          [:div.fan
           {:data-num "1"}
           [:img.base {:src "/images/svg/fan-base.svg"}]
           [:div.wheel [:img {:src "/images/svg/fan-spokes.svg"}]]]
          [:div.fan
           {:data-num "2"}
           [:img.base {:src "/images/svg/fan-base.svg"}]
           [:div.wheel [:img {:src "/images/svg/fan-spokes.svg"}]]]]]]])))


(defn district-tiles [active-account route-query]
  (let [q (subscribe [::gql/query
                      {:queries [(build-query active-account route-query)]}
                      {:refetch-on #{::district/approve-and-stake-for-success
                                     ::district/unstake-success}}])
        result (:search-districts @q)
        districts (:items result)]
    (cond
      (:graphql/loading? @q) [loader]
      (empty? districts) [:div.no-districts
                          [:h2 "No districts found"]]
      :else [:div.grid.spaced
             (->> districts
               (map (fn [{:as district
                          :keys [:reg-entry/address]}]
                      ^{:key address} [district-tile district]))
               doall)])))


(defmethod page :route/home []
  (let [active-account (subscribe [::account-subs/active-account])
        route-query (subscribe [::router-subs/active-page-query])
        status (or (:status @route-query) "in-registry")
        order-by (or (:order-by @route-query) "created-on")
        order-by-kw (keyword "districts.order-by" order-by)
        order-by-kw->str {:districts.order-by/created-on "Creation Date"
                          :districts.order-by/dnt-staked "DNT Staked"}
        select-menu-open? (r/atom false)]
    (fn []
      [app-layout
       [:section#intro
        [:div.bg-wrap
         [:div.background.sized
          [:img {:src "/images/blobbg-top@2x.png"}]]]
        [:div.container
         [:nav.subnav
          [:ul
           [:li {:class (when (= status "in-registry") "on")}
            (nav/a {:route [:route/home {} (assoc @route-query :status "in-registry")]
                    :class "cta-btn"}
                   "In Registry")]
           [:li {:class (when (= status "challenged") "on")}
            (nav/a {:class "cta-btn"
                    :route [:route/home {} (assoc @route-query :status "challenged")]}
                   "Challenged")]
           [:li {:class (when (= status "blacklisted") "on")}
            (nav/a {:route [:route/home {} (assoc @route-query :status "blacklisted")]}
                   "Blacklisted")]]]
         [:p
          "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat aute irure dolor in reprehenderit."]]]
       [:section#registry-grid
        [:div.container
         [:div.select-menu {:class (when @select-menu-open? "on")
                            :on-click #(swap! select-menu-open? not)}
          [:div.select-choice.cta-btn
           [:div.select-text (order-by-kw order-by-kw->str)]
           [:div.arrow [:span.arr.icon-arrow-down]]]
          [:div.select-drop
           [:ul
            (->> order-by-kw->str
              keys
              (remove #(= order-by-kw %))
              (map (fn [k]
                     [:li {:key k}
                      (nav/a {:route [:route/home {} (assoc @route-query :order-by (name k))]}
                             (order-by-kw->str k))]))
              doall)]]]
         [district-tiles @active-account (assoc @route-query
                                           :status status
                                           :order-by order-by)]]]])))
