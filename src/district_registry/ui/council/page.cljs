(ns district-registry.ui.council.page
  (:require
    [district-registry.ui.components.app-layout :refer [app-layout]]
    [district.format :as format]
    [district.graphql-utils :as gql-utils]
    [district.ui.component.page :refer [page]]
    [district.ui.graphql.subs :as gql]
    [district.ui.router.subs :as router-subs]
    [district.ui.web3-accounts.subs :as account-subs]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(def council-seats-query
  "GraphQL query to fetch all council seats"
  [:search-council-seats
   {:only-active true
    :order-by :council-seats.order-by/index
    :order-dir :asc
    :first 9}
   [:total-count
    [:items
     [:council-seat/index
      :council-seat/mind-id
      :council-seat/name
      :council-seat/philosophy-hash
      :council-seat/is-immutable
      :council-seat/is-active
      :council-seat/created-on
      :council-seat/total-staked
      :council-seat/stake-percentage]]]])

(defn loader []
  "Loading spinner component"
  (let [mounted? (r/atom false)]
    (fn []
      (when-not @mounted?
        (js/setTimeout #(swap! mounted? not)))
      [:div#loader-wrapper {:class (str "fade-in" (when @mounted? " visible"))}
       [:div#loader
        [:div.loader-graphic
         [:div.loader-floater
          [:img.bg.spacer {:src "/images/svg/loader-bg.svg"}]
          [:div.turbine
           [:img.base {:src "/images/svg/turbine-base.svg"}]
           [:div.wheel [:img {:src "/images/svg/turbine-blade.svg"}]]
           [:img.cover {:src "/images/svg/turbine-cover.svg"}]]]]]])))

(defn council-seat-card
  "Individual council seat display card"
  [{:keys [:council-seat/index
           :council-seat/name
           :council-seat/philosophy-hash
           :council-seat/is-immutable
           :council-seat/total-staked
           :council-seat/stake-percentage]
    :as seat}]
  (let [active-account (subscribe [::account-subs/active-account])
        stake-modal-open? (r/atom false)]
    (fn []
      [:div.council-seat-card
       {:class (str "seat-" index
                   (when is-immutable " immutable")
                   (when (= index 8) " stranger-seat"))}
       [:div.seat-header
        [:div.seat-index (str "Seat " index)]
        (when is-immutable
          [:div.immutable-badge {:title "This seat is immutable and cannot be changed"}
           "IMMUTABLE"])]
       [:div.seat-body
        [:h3.seat-name name]
        [:div.seat-stats
         [:div.stat
          [:span.stat-label "Total Staked"]
          [:span.stat-value (format/format-token total-staked {:token "DNT"})]]
         [:div.stat
          [:span.stat-label "Weight"]
          [:span.stat-value (str (format/format-number stake-percentage {:max-fraction-digits 2}) "%")]]]]
       [:div.seat-footer
        [:button.cta-btn.stake-btn
         {:on-click #(reset! stake-modal-open? true)}
         "Stake on this Mind"]]])))

(defn council-grid
  "9-seat grid visualization of The Council"
  [seats]
  [:div.council-grid
   (if (empty? seats)
     [:div.no-seats
      [:p "No council seats found. Please ensure contracts are deployed."]]
     [:div.grid.council-grid-layout
      (for [seat seats]
        ^{:key (:council-seat/index seat)}
        [council-seat-card seat])])])

(defn user-stakes-panel
  "Display user's current stakes across all seats"
  [active-account]
  (when active-account
    [:div.user-stakes-panel
     [:h3 "Your Stakes"]
     [:p "View and manage your stakes across all council seats"]
     ;; TODO: Add GraphQL query for user's specific stakes
     [:div.user-stakes-list
      [:p.coming-soon "Detailed stake information coming soon..."]]]))

(defmethod page :route/council []
  (let [active-account (subscribe [::account-subs/active-account])
        council-query (subscribe [::gql/query {:queries [council-seats-query]}])
        result @council-query
        seats (get-in result [:search-council-seats :items])]
    (fn []
      [app-layout
       [:section#council-intro
        [:div.container
         [:h1 "The Council"]
         [:p.council-description
          "The Council represents nine philosophical perspectives that guide The Backroom's governance. "
          "Stake your DNT on the minds you want to influence decision-making. "
          "ARI (The Stranger) in Seat 8 permanently advocates for newcomers."]]]

       [:section#council-main
        [:div.container
         (cond
           (:graphql/loading? result)
           [loader]

           (:graphql/errors result)
           [:div.error-message
            [:p "Error loading council seats. Please check your connection and try again."]]

           :else
           [:div
            [council-grid seats]
            [user-stakes-panel @active-account]])]]

       [:section#council-info
        [:div.container
         [:h2 "How It Works"]
         [:div.info-grid
          [:div.info-card
           [:h3 "1. Stake on Minds"]
           [:p "Choose which philosophical perspectives you want to guide governance by staking DNT on council seats."]]
          [:div.info-card
           [:h3 "2. Weights Established"]
           [:p "Your stake contributes to each mind's weight in The Council. More stake = more influence."]]
          [:div.info-card
           [:h3 "3. ARI Proposes"]
           [:p "ARI reads the council weights and creates proposals weighted by the philosophical priorities you've set."]]
          [:div.info-card
           [:h3 "4. Community Vetoes"]
           [:p "If ≥30% of total stake shifts between seats during the 3-day veto period, the proposal is vetoed."]]]]]])))
