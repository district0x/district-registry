(ns district-registry.ui.ari.page
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

(def ari-proposals-query
  "GraphQL query to fetch ARI proposals with pagination"
  [:search-ari-proposals
   {:order-by :ari-proposals.order-by/created-on
    :order-dir :desc
    :first 20}
   [:total-count
    [:items
     [:ari-proposal/proposal-id
      :ari-proposal/proposal-type
      :ari-proposal/status
      :ari-proposal/reasoning-hash
      :ari-proposal/amount
      :ari-proposal/recipient
      :ari-proposal/created-on
      :ari-proposal/veto-period-start
      :ari-proposal/veto-period-end
      :ari-proposal/council-weights-snapshot
      :ari-proposal/executed-on
      :ari-proposal/vetoed
      :ari-proposal/veto-percentage]]]])

(def active-proposals-query
  "GraphQL query for active proposals only"
  [:search-ari-proposals
   {:status [:Pending :VetoPeriod]
    :order-by :ari-proposals.order-by/created-on
    :order-dir :desc
    :first 10}
   [:total-count
    [:items
     [:ari-proposal/proposal-id
      :ari-proposal/proposal-type
      :ari-proposal/status
      :ari-proposal/reasoning-hash
      :ari-proposal/veto-period-end
      :ari-proposal/veto-percentage]]]])

;;
;; Helper Functions
;;

(defn proposal-type->string [type]
  "Convert proposal type enum to readable string"
  (case type
    :TreasuryTransfer "Treasury Transfer"
    :BountyCreation "Bounty Creation"
    :GrantApproval "Grant Approval"
    :ParameterChange "Parameter Change"
    :CustomAction "Custom Action"
    "Unknown"))

(defn proposal-status->string [status]
  "Convert proposal status enum to readable string"
  (case status
    :Pending "Pending"
    :VetoPeriod "Veto Period"
    :Approved "Approved"
    :Vetoed "Vetoed"
    :Executed "Executed"
    :Cancelled "Cancelled"
    "Unknown"))

(defn proposal-status->color [status]
  "Get color class for proposal status"
  (case status
    :Pending "status-pending"
    :VetoPeriod "status-veto"
    :Approved "status-approved"
    :Vetoed "status-vetoed"
    :Executed "status-executed"
    :Cancelled "status-cancelled"
    ""))

(defn time-remaining [end-timestamp]
  "Calculate time remaining until end timestamp"
  (let [now (tc/to-long (t/now))
        end (* end-timestamp 1000)
        diff (- end now)]
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
      "Ended")))

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

(defn veto-progress-bar
  "Visual progress bar for veto threshold (0-30%)"
  [{:keys [:veto-percentage]}]
  (let [veto-pct (or veto-percentage 0)
        threshold 30
        progress-width (min (* (/ veto-pct threshold) 100) 100)
        is-vetoed (>= veto-pct threshold)]
    [:div.veto-progress
     [:div.veto-progress-label
      [:span.veto-current (str (format/format-number veto-pct {:max-fraction-digits 2}) "%")]
      [:span.veto-threshold (str " / " threshold "% threshold")]]
     [:div.veto-progress-bar
      [:div.veto-progress-fill
       {:class (when is-vetoed "vetoed")
        :style {:width (str progress-width "%")}}]]
     (when is-vetoed
       [:div.veto-status "⚠️ Veto Threshold Reached"])]))

(defn proposal-card
  "Individual ARI proposal card"
  [{:keys [:ari-proposal/proposal-id
           :ari-proposal/proposal-type
           :ari-proposal/status
           :ari-proposal/reasoning-hash
           :ari-proposal/amount
           :ari-proposal/recipient
           :ari-proposal/created-on
           :ari-proposal/veto-period-start
           :ari-proposal/veto-period-end
           :ari-proposal/council-weights-snapshot
           :ari-proposal/executed-on
           :ari-proposal/vetoed
           :ari-proposal/veto-percentage] :as proposal}]
  [:div.proposal-card
   [:div.proposal-header
    [:div.proposal-id
     [:span.label "Proposal #"]
     [:span.value proposal-id]]
    [:div.proposal-status
     {:class (proposal-status->color status)}
     (proposal-status->string status)]]

   [:div.proposal-body
    [:div.proposal-type
     [:strong (proposal-type->string proposal-type)]]

    (when reasoning-hash
      [:div.proposal-reasoning
       [:div.reasoning-label "ARI's Reasoning:"]
       [:div.reasoning-hash
        [:a {:href (str "https://ipfs.io/ipfs/" reasoning-hash)
             :target "_blank"
             :rel "noopener noreferrer"}
         [:span "📄 View on IPFS"]
         [:code (subs reasoning-hash 0 8) "..."]]]])

    (when amount
      [:div.proposal-amount
       [:span.label "Amount: "]
       [:span.value (format/format-token amount {:token "DNT"})]])

    (when recipient
      [:div.proposal-recipient
       [:span.label "Recipient: "]
       [:span.value [:code (subs recipient 0 10) "..."]]])

    ;; Veto progress for active proposals
    (when (= status :VetoPeriod)
      [:div.proposal-veto
       [veto-progress-bar proposal]
       (when veto-period-end
         [:div.time-remaining
          [:span.label "Time Remaining: "]
          [:span.value (time-remaining veto-period-end)]])])

    ;; Council weights snapshot
    (when council-weights-snapshot
      [:div.proposal-weights
       [:div.weights-label "Council Weights at Creation:"]
       [:div.weights-display
        (map-indexed
          (fn [idx weight]
            ^{:key idx}
            [:span.weight-item
             [:span.seat (str "Seat " idx ": ")]
             [:span.percentage (str (format/format-number weight {:max-fraction-digits 1}) "%")]])
          council-weights-snapshot)]])]

   [:div.proposal-footer
    [:div.proposal-dates
     [:span.created
      [:span.label "Created: "]
      [:span.value (format/format-date created-on)]]
     (when executed-on
       [:span.executed
        [:span.label " | Executed: "]
        [:span.value (format/format-date executed-on)]])]

    ;; Cast veto button (only for active veto period)
    (when (= status :VetoPeriod)
      [:div.proposal-actions
       [:button.btn-veto
        {:on-click #(dispatch [:district-registry.ui.contract.council/move-stake
                               {:proposal-id proposal-id}])}
        "Move Stake to Veto"]])]])

(defn proposal-filter
  "Filter proposals by status"
  [active-filter]
  (let [filters [:All :Pending :VetoPeriod :Approved :Vetoed :Executed :Cancelled]]
    [:div.proposal-filters
     [:span.filter-label "Filter by Status: "]
     (for [filter filters]
       ^{:key filter}
       [:button.filter-btn
        {:class (when (= @active-filter filter) "active")
         :on-click #(reset! active-filter filter)}
        (if (= filter :All) "All" (proposal-status->string filter))])]))

(defn proposals-list
  "List of all ARI proposals"
  [proposals]
  (let [active-filter (r/atom :All)]
    (fn [proposals]
      (let [filtered-proposals (if (= @active-filter :All)
                                 proposals
                                 (filter #(= (:ari-proposal/status %) @active-filter) proposals))]
        [:div.proposals-list
         [proposal-filter active-filter]
         [:div.proposals-count
          (str (count filtered-proposals) " proposal" (when (not= 1 (count filtered-proposals)) "s"))]
         (if (empty? filtered-proposals)
           [:div.no-proposals
            [:p "No proposals match the selected filter."]]
           [:div.proposals-grid
            (for [proposal filtered-proposals]
              ^{:key (:ari-proposal/proposal-id proposal)}
              [proposal-card proposal])])]))))

(defn ari-intro
  "ARI Activity page introduction section"
  []
  [:div#ari-intro
   [:div.container
    [:h1 "ARI Activity"]
    [:p.ari-description
     "ARI (Adaptive Reasoning Intelligence) proposes actions based on council weights. "
     "The community has 3 days to veto proposals by moving ≥30% of total stakes. "
     "View all proposals, their reasoning, and current veto progress."]]])

(defn how-ari-works
  "Educational section explaining ARI mechanism"
  []
  [:div.how-it-works
   [:div.container
    [:h2 "How ARI Proposals Work"]
    [:div.mechanism-grid
     [:div.mechanism-step
      [:div.step-number "1"]
      [:h3 "ARI Analyzes"]
      [:p "ARI reads current council weights and proposes an action with detailed reasoning stored on IPFS."]]
     [:div.mechanism-step
      [:div.step-number "2"]
      [:h3 "3-Day Veto Period"]
      [:p "Community members can veto by moving their stakes. The proposal needs ≥30% stake shift to be vetoed."]]
     [:div.mechanism-step
      [:div.step-number "3"]
      [:h3 "Execution or Veto"]
      [:p "If veto threshold isn't reached, the proposal executes automatically. Otherwise, it's vetoed and cancelled."]]]
    [:div.veto-info
     [:h3 "Passive Veto Mechanism"]
     [:p "Unlike traditional voting, you don't explicitly cast a veto vote. Instead, "
         "you signal disagreement by moving your stake between council seats. "
         "This creates a philosophical negotiation rather than adversarial voting."]]]])

;;
;; Page Component
;;

(defmethod page :route/ari []
  (let [search (subscribe [::gql/query
                           {:queries [ari-proposals-query]}
                           {:id :ari-page}])]
    (fn []
      (let [ari-proposals (-> @search :search-ari-proposals :items)]
        [app-layout
         [:div.ari-page
          [ari-intro]
          [:div.container
           (if (empty? ari-proposals)
             [loader]
             [proposals-list ari-proposals])]
          [how-ari-works]]]))))
