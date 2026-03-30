(ns district-registry.ui.power-plant.page
  (:require
    [district-registry.ui.components.app-layout :refer [app-layout]]
    [district.format :as format]
    [district.graphql-utils :as gql-utils]
    [district.ui.component.page :refer [page]]
    [district.ui.graphql.subs :as gql]
    [district.ui.router.subs :as router-subs]
    [district.ui.web3-accounts.subs :as account-subs]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [cljs-time.core :as t]
    [cljs-time.coerce :as tc]))

;;
;; GraphQL Queries
;;

(def bounties-query
  "GraphQL query to fetch bounties"
  [:search-bounties
   {:order-by :bounties.order-by/created-on
    :order-dir :desc
    :first 20}
   [:total-count
    [:items
     [:bounty/bounty-id
      :bounty/metadata-hash
      :bounty/reward
      :bounty/token
      :bounty/deadline
      :bounty/assignee
      :bounty/status
      :bounty/created-on
      :bounty/completed-on
      :bounty/claimed-on]]]])

(def grants-query
  "GraphQL query to fetch grants"
  [:search-grants
   {:order-by :grants.order-by/created-on
    :order-dir :desc
    :first 20}
   [:total-count
    [:items
     [:grant/grant-id
      :grant/recipient
      :grant/total-amount
      :grant/token
      :grant/disbursed-amount
      :grant/vesting-start
      :grant/vesting-duration
      :grant/vesting-end
      :grant/metadata-hash
      :grant/created-on
      :grant/vested-amount
      :grant/remaining-amount]]]])

(def active-bounties-query
  "GraphQL query for active bounties only"
  [:search-bounties
   {:status [:Open :Assigned]
    :order-by :bounties.order-by/created-on
    :order-dir :desc
    :first 10}
   [:total-count
    [:items
     [:bounty/bounty-id
      :bounty/metadata-hash
      :bounty/reward
      :bounty/deadline
      :bounty/status]]]])

;;
;; Helper Functions
;;

(defn bounty-status->string [status]
  "Convert bounty status enum to readable string"
  (case status
    :Open "Open"
    :Assigned "Assigned"
    :Completed "Completed"
    :Claimed "Claimed"
    "Unknown"))

(defn bounty-status->color [status]
  "Get color class for bounty status"
  (case status
    :Open "status-open"
    :Assigned "status-assigned"
    :Completed "status-completed"
    :Claimed "status-claimed"
    ""))

(defn vesting-progress [grant]
  "Calculate vesting progress percentage"
  (let [total (:grant/total-amount grant)
        disbursed (:grant/disbursed-amount grant 0)
        vested (:grant/vested-amount grant 0)]
    {:total total
     :disbursed disbursed
     :vested vested
     :remaining (- total disbursed)
     :vested-pct (if (pos? total) (* (/ vested total) 100) 0)
     :disbursed-pct (if (pos? total) (* (/ disbursed total) 100) 0)}))

(defn time-until [timestamp]
  "Calculate time until timestamp"
  (let [now (tc/to-long (t/now))
        target (* timestamp 1000)
        diff (- target now)]
    (if (pos? diff)
      (let [seconds (quot diff 1000)
            minutes (quot seconds 60)
            hours (quot minutes 60)
            days (quot hours 24)]
        (cond
          (> days 1) (str days " days")
          (> hours 1) (str hours " hours")
          (> minutes 1) (str minutes " minutes")
          :else (str seconds " seconds")))
      "Overdue")))

;;
;; Components
;;

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

(defn treasury-balance-card
  "Display treasury balance for a token"
  [{:keys [token balance]}]
  [:div.treasury-card
   [:div.treasury-token
    [:span.token-name token]]
   [:div.treasury-balance
    [:span.balance-value (format/format-token balance {:token token})]]])

(defn treasury-overview
  "Overview of Power Plant treasury balances"
  []
  (let [dnt-balance (r/atom 0)
        eth-balance (r/atom 0)]
    (fn []
      [:div.treasury-overview
       [:h2 "Treasury Balances"]
       [:div.treasury-grid
        [treasury-balance-card {:token "DNT" :balance @dnt-balance}]
        [treasury-balance-card {:token "ETH" :balance @eth-balance}]]
       [:p.treasury-note
        "The Power Plant manages the district-registry treasury, distributing resources through bounties and grants approved by ARI."]])))

(defn bounty-card
  "Individual bounty card"
  [{:keys [:bounty/bounty-id
           :bounty/metadata-hash
           :bounty/reward
           :bounty/token
           :bounty/deadline
           :bounty/assignee
           :bounty/status
           :bounty/created-on
           :bounty/completed-on
           :bounty/claimed-on] :as bounty}]
  [:div.bounty-card
   [:div.bounty-header
    [:div.bounty-id
     [:span.label "Bounty #"]
     [:span.value bounty-id]]
    [:div.bounty-status
     {:class (bounty-status->color status)}
     (bounty-status->string status)]]

   [:div.bounty-body
    (when metadata-hash
      [:div.bounty-metadata
       [:div.metadata-label "Description:"]
       [:div.metadata-link
        [:a {:href (str "https://ipfs.io/ipfs/" metadata-hash)
             :target "_blank"
             :rel "noopener noreferrer"}
         [:span "📄 View on IPFS"]
         [:code (subs metadata-hash 0 8) "..."]]]])

    [:div.bounty-reward
     [:span.label "Reward: "]
     [:span.value (format/format-token reward {:token token})]]

    (when deadline
      [:div.bounty-deadline
       [:span.label "Deadline: "]
       [:span.value (format/format-date deadline)]
       [:span.time-left " (" (time-until deadline) ")"]])

    (when assignee
      [:div.bounty-assignee
       [:span.label "Assigned to: "]
       [:span.value [:code (subs assignee 0 10) "..."]]])]

   [:div.bounty-footer
    [:div.bounty-dates
     [:span.created
      [:span.label "Created: "]
      [:span.value (format/format-date created-on)]]
     (when completed-on
       [:span.completed
        [:span.label " | Completed: "]
        [:span.value (format/format-date completed-on)]])]

    ;; Actions based on status
    (when (= status :Open)
      [:div.bounty-actions
       [:button.btn-assign
        {:on-click #(dispatch [:district-registry.ui.contract.power-plant/assign-bounty
                               {:bounty-id bounty-id}])}
        "Claim Bounty"]])

    (when (and (= status :Assigned) (= assignee (subscribe [::account-subs/active-account])))
      [:div.bounty-actions
       [:button.btn-complete
        {:on-click #(dispatch [:district-registry.ui.contract.power-plant/mark-bounty-complete
                               {:bounty-id bounty-id}])}
        "Mark Complete"]])

    (when (and (= status :Completed) (= assignee (subscribe [::account-subs/active-account])))
      [:div.bounty-actions
       [:button.btn-claim
        {:on-click #(dispatch [:district-registry.ui.contract.power-plant/claim-bounty
                               {:bounty-id bounty-id}])}
        "Claim Reward"]])]])

(defn bounties-list
  "List of all bounties"
  [bounties]
  (let [active-filter (r/atom :All)]
    (fn [bounties]
      (let [filtered-bounties (if (= @active-filter :All)
                                bounties
                                (filter #(= (:bounty/status %) @active-filter) bounties))]
        [:div.bounties-list
         [:h2 "Bounties"]
         [:div.bounty-filters
          [:span.filter-label "Filter: "]
          (for [filter [:All :Open :Assigned :Completed :Claimed]]
            ^{:key filter}
            [:button.filter-btn
             {:class (when (= @active-filter filter) "active")
              :on-click #(reset! active-filter filter)}
             (if (= filter :All) "All" (bounty-status->string filter))])]
         [:div.bounties-count
          (str (count filtered-bounties) " bount" (if (= 1 (count filtered-bounties)) "y" "ies"))]
         (if (empty? filtered-bounties)
           [:div.no-bounties
            [:p "No bounties match the selected filter."]]
           [:div.bounties-grid
            (for [bounty filtered-bounties]
              ^{:key (:bounty/bounty-id bounty)}
              [bounty-card bounty])])]))))

(defn grant-card
  "Individual grant card with vesting visualization"
  [{:keys [:grant/grant-id
           :grant/recipient
           :grant/total-amount
           :grant/token
           :grant/disbursed-amount
           :grant/vesting-start
           :grant/vesting-duration
           :grant/vesting-end
           :grant/metadata-hash
           :grant/created-on
           :grant/vested-amount
           :grant/remaining-amount] :as grant}]
  (let [progress (vesting-progress grant)]
    [:div.grant-card
     [:div.grant-header
      [:div.grant-id
       [:span.label "Grant #"]
       [:span.value grant-id]]
      [:div.grant-amount
       (format/format-token total-amount {:token token})]]

     [:div.grant-body
      [:div.grant-recipient
       [:span.label "Recipient: "]
       [:span.value [:code (subs recipient 0 10) "..."]]]

      (when metadata-hash
        [:div.grant-metadata
         [:a {:href (str "https://ipfs.io/ipfs/" metadata-hash)
              :target "_blank"
              :rel "noopener noreferrer"}
          [:span "📄 View Details on IPFS"]]])

      ;; Vesting progress visualization
      [:div.grant-vesting
       [:div.vesting-label "Vesting Progress"]
       [:div.vesting-bar
        [:div.vesting-fill-disbursed
         {:style {:width (str (:disbursed-pct progress) "%")}}]
        [:div.vesting-fill-vested
         {:style {:width (str (- (:vested-pct progress) (:disbursed-pct progress)) "%")}}]]
       [:div.vesting-stats
        [:div.stat
         [:span.label "Vested: "]
         [:span.value (format/format-token vested-amount {:token token})]]
        [:div.stat
         [:span.label "Disbursed: "]
         [:span.value (format/format-token disbursed-amount {:token token})]]
        [:div.stat
         [:span.label "Remaining: "]
         [:span.value (format/format-token remaining-amount {:token token})]]]]

      [:div.grant-timeline
       [:span.label "Vesting Period: "]
       [:span.value
        (str (format/format-date vesting-start)
             " → "
             (format/format-date vesting-end))]]]

     [:div.grant-footer
      [:div.grant-dates
       [:span.created
        [:span.label "Created: "]
        [:span.value (format/format-date created-on)]]]

      ;; Disburse button for recipient
      (when (and (pos? (- vested-amount disbursed-amount))
                 (= recipient @(subscribe [::account-subs/active-account])))
        [:div.grant-actions
         [:button.btn-disburse
          {:on-click #(dispatch [:district-registry.ui.contract.power-plant/disburse-grant
                                 {:grant-id grant-id}])}
          "Claim Vested Amount"]])]]))

(defn grants-list
  "List of all grants"
  [grants]
  [:div.grants-list
   [:h2 "Grants"]
   [:div.grants-count
    (str (count grants) " grant" (when (not= 1 (count grants)) "s"))]
   (if (empty? grants)
     [:div.no-grants
      [:p "No grants have been created yet."]]
     [:div.grants-grid
      (for [grant grants]
        ^{:key (:grant/grant-id grant)}
        [grant-card grant])])])

(defn power-plant-intro
  "Power Plant page introduction"
  []
  [:div#power-plant-intro
   [:div.container
    [:h1 "The Power Plant"]
    [:p.power-plant-description
     "The Power Plant manages the district-registry treasury, distributing resources to grow the network. "
     "ARI proposes bounties for specific work and grants for long-term contributions. "
     "All distributions are subject to the 3-day veto period."]]])

(defn how-it-works
  "Educational section"
  []
  [:div.how-it-works
   [:div.container
    [:h2 "How Distributions Work"]
    [:div.mechanism-grid
     [:div.mechanism-step
      [:div.step-number "1"]
      [:h3 "ARI Proposes"]
      [:p "ARI analyzes council weights and proposes a bounty or grant from the treasury."]]
     [:div.mechanism-step
      [:div.step-number "2"]
      [:h3 "Community Review"]
      [:p "3-day veto period allows community to veto by moving ≥30% of stakes."]]
     [:div.mechanism-step
      [:div.step-number "3"]
      [:h3 "Distribution"]
      [:p "If approved, bounties can be claimed after completion, and grants vest linearly over time."]]]
    [:div.distribution-info
     [:h3 "Types of Distributions"]
     [:ul
      [:li [:strong "Bounties:"] " One-time rewards for specific completed work"]
      [:li [:strong "Grants:"] " Long-term funding that vests linearly over a specified period"]]]]])

;;
;; Page Component
;;

(defmethod page :route/power-plant []
  (let [bounties-search (subscribe [::gql/query
                                     {:queries [bounties-query]}
                                     {:id :power-plant-bounties}])
        grants-search (subscribe [::gql/query
                                   {:queries [grants-query]}
                                   {:id :power-plant-grants}])]
    (fn []
      (let [bounties (-> @bounties-search :search-bounties :items)
            grants (-> @grants-search :search-grants :items)]
        [app-layout
         [:div.power-plant-page
          [power-plant-intro]
          [:div.container
           [treasury-overview]
           (if (and (empty? bounties) (empty? grants))
             [loader]
             [:div
              [bounties-list bounties]
              [grants-list grants]])]
          [how-it-works]]]))))
