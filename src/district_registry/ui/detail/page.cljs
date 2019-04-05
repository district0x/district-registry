(ns district-registry.ui.detail.page
  (:require
   [cljs-web3.core :as web3]
   [clojure.pprint :refer [pprint]]
   [clojure.string :as str]
   [clojure.walk :as walk]
   [district-registry.ui.components.app-layout :refer [app-layout]]
   [district-registry.ui.components.stake :as stake]
   [district-registry.ui.contract.registry-entry :as reg-entry]
   [district-registry.ui.events :as events]
   [district-registry.ui.not-found.page :as not-found]
   [district-registry.ui.spec :as spec]
   [district-registry.ui.utils :as ui-utils]
   [district.format :as format]
   [district.graphql-utils :as graphql-utils]
   [district.ui.component.form.input :as inputs]
   [district.ui.component.page :refer [page]]
   [district.ui.component.tx-button :as tx-button]
   [district.ui.graphql.subs :as gql]
   [district.ui.now.subs :as now-subs]
   [district.ui.router.subs :as router-subs]
   [district.ui.web3-account-balances.subs :as account-balances-subs]
   [district.ui.web3-accounts.subs :as account-subs]
   [district.ui.web3-tx-id.subs :as tx-id-subs]
   [re-frame.core :refer [dispatch subscribe]]
   [reagent.core :as r]
   [reagent.ratom :as ratom]))

(defn build-query [{:as props
                    :keys [district active-account]}]
  [:district {:reg-entry/address district}
   [:reg-entry/address
    :reg-entry/version
    :reg-entry/status
    :reg-entry/creator
    :reg-entry/deposit
    :reg-entry/created-on
    :reg-entry/challenge-period-end
    [:reg-entry/challenges
     [:challenge/challenger
      :challenge/created-on
      :challenge/reward-pool
      :challenge/commit-period-end
      :challenge/reveal-period-end
      :challenge/votes-include
      :challenge/votes-exclude
      :challenge/votes-total
      :challenge/claimed-reward-on
      [:challenge/vote {:voter active-account}
       [:vote/secret-hash
        :vote/option
        :vote/amount
        :vote/revealed-on
        :vote/claimed-reward-on
        :vote/reward]]]]
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
    [:district/balance-of {:staker active-account}]]])

(defn normalize-status [status]
  (case status
    ("regEntry_status_challengePeriod"
     "regEntry_status_whitelisted")  :in-registry
    ("regEntry_status_commitPeriod"
     "regEntry_status_revealPeriod") :challenged
    "regEntry_status_blacklisted"    :blacklisted))

(defn district-image [image-hash]
  (when image-hash
    (let [gateway (subscribe [::gql/query {:queries [[:config [[:ipfs [:gateway]]]]]}])]
      (when-not (:graphql/loading? @gateway)
        (if-let [url (-> @gateway :config :ipfs :gateway)]
          [:img.spacer {:src (str (format/ensure-trailing-slash url) image-hash)}])))))

(defn info-section [{:as district
                     :keys [:district/name
                            :district/description
                            :district/background-image-hash
                            :district/logo-image-hash
                            :district/url
                            :district/github-url
                            :district/total-supply
                            :district/dnt-staked
                            :reg-entry/status
                            :reg-entry/created-on]}]
  [:div.box-wrap.overview
   [:div.back-arrow {:on-click #(dispatch [:district.ui.router.events/navigate :route/home])}
    [:span.icon-arrow-right]]
   [:div.body-text
    [:div.container
     [:div.overview-details
      [:div.col.txt
       [:div.title-wrap.spaced
        [:div.title-txt
         [:h1 name]
         [:a {:href url} url]]
        [:div.title-icons
         [:div.title-icon
          [:a {:href github-url}
           [:img {:src "/images/icon-fc-github@2x.png"}]]]
         [:div.title-icon ; TODO Aragon link
          [:img {:src "/images/icon-fc-bird@2x.png"}]]]]
       [:ul.details-list
        [:li (str "Status: " (-> status
                               normalize-status
                               cljs.core/name
                               str/capitalize))]
        ;; FIXME: Why are dates in 1970?
        [:li (str "Added: " (-> created-on
                              format/format-local-date))]
        [:li (str "Staked total: " (-> dnt-staked
                                     (web3/from-wei :ether)
                                     format/format-dnt
                                     ))]
        [:li (str "Voting tokens issued: " (-> total-supply
                                             (web3/from-wei :ether)
                                             format/format-token))]]
       [:nav.social
        [:ul
         [:li
          [:a {:target "_blank"
               :href (str "https://www.facebook.com/sharer/sharer.php?u=" js/window.location.href)}
           [:span.icon-facebook]]]
         [:li [:a {:target "_blank"
                   :href (str "https://twitter.com/home?status=" js/window.location.href)}
               [:span.icon-twitter]]]]]]
      ;; TODO: We aren't showing the logo image?
      [:div.col.img
       [district-image background-image-hash]]]
     [:p description]]]])

(defn stake-section [{:as district
                      :keys [:reg-entry/address
                             :reg-entry/status
                             :district/dnt-weight]}]
  (when (not=
          (normalize-status status)
          :blacklisted)
    [:div
     [:h2 "Stake"]
     [:p
      "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec a augue quis metus sollicitudin mattis. Duis efficitur tellus felis, et tincidunt turpis aliquet non. Aenean augue metus, malesuada non rutrum ut, ornare ac orci."]
     [:h3 "Voting Token Issuance Curve"]
     [:img.spacer {:src (str "/images/curve-graph-" dnt-weight "-l.png")}]
     [:div.stake
      [:div.row.spaced
       [stake/stake-info address]
       [stake/stake-form address]]]]))

(defn challenge-section [{:as district
                          :keys [:reg-entry/address
                                 :reg-entry/status
                                 :reg-entry/deposit]}]
  (let [form-data (r/atom {:challenge/comment nil})
        errors (ratom/reaction {:local (when-not (spec/check ::spec/challenge-comment (:challenge/comment @form-data))
                                         {:challenge/comment "Comment shouldn't be empty."})})
        tx-id (str address "challenges")
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {::reg-entry/approve-and-create-challenge tx-id}])
        tx-success? (subscribe [::tx-id-subs/tx-success? {::reg-entry/approve-and-create-challenge tx-id}])]
    (when (= (normalize-status status) :in-registry)
      (fn []
        [:div
         [:div.h-line]
         [:h2 "Challenge"]
         [:p
          "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec a augue quis metus sollicitudin mattis. Duis efficitur tellus felis, et tincidunt turpis aliquet non. Aenean augue metus, malesuada non rutrum ut, ornare ac orci."]
         [:form.challenge
          [inputs/textarea-input {:form-data form-data
                                  :id :challenge/comment
                                  :errors errors}]
          [:div.form-btns
           [:p (format/format-token deposit {:token "DNT"})]
           [tx-button/tx-button {:class "cta-btn"
                                 :primary true
                                 :disabled (or @tx-success? (not (empty? (:local @errors))))
                                 :pending? @tx-pending?
                                 :pending-text "Challenging..."
                                 :on-click #(dispatch [::events/add-challenge {:send-tx/id tx-id
                                                                               :reg-entry/address address
                                                                               :comment (:challenge/comment @form-data)
                                                                               :deposit deposit}])}
            "Challenge"]]]]))))

(defn remaining-time [to-time]
  (let [time-remaining (subscribe [::now-subs/time-remaining to-time])
        {:keys [:days :hours :minutes :seconds]} @time-remaining]
    [:b (str (format/pluralize days "Day") ", "
          hours " Hr. "
          minutes " Min. "
          seconds " Sec.")]))

(defn vote-commit-section [{:as district
                            :keys [:reg-entry/address
                                   :reg-entry/status
                                   :reg-entry/challenges]}]
  (when (= "regEntry_status_commitPeriod" status)
    (let [{:as challenge
           :keys [:challenge/commit-period-end]} (last challenges)
          balance-dnt (subscribe [::account-balances-subs/active-account-balance :DNT])
          form-data (r/atom {:vote/amount nil})
          errors (ratom/reaction {:local (let [amount (-> @form-data :vote/amount js/parseInt (web3/to-wei :ether))]
                                           (cond-> {}
                                             (or (not (spec/check ::spec/pos-int amount))
                                               (< @balance-dnt amount))
                                             (assoc :vote/amount (str "Amount should be between 0 and your DNT balance"))))})
          tx-id address
          tx-pending? (subscribe [::tx-id-subs/tx-pending? {::reg-entry/approve-and-commit-vote tx-id}])
          tx-success? (subscribe [::tx-id-subs/tx-success? {::reg-entry/approve-and-commit-vote tx-id}])
          vote (fn [option]
                 (dispatch [::reg-entry/approve-and-commit-vote
                            {:send-tx/id tx-id
                             :reg-entry/address address
                             :vote/option option
                             :vote/amount (-> @form-data :vote/exclude js/parseInt (web3/to-wei :ether))}]))]
      (fn []
        [:div
         [:div.h-line]
         [:h2 "Commit"]
         [:p
          "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec a augue quis metus sollicitudin mattis. Duis efficitur tellus felis, et tincidunt turpis aliquet non. Aenean augue metus, malesuada non rutrum ut, ornare ac orci."]
         [:form.voting
          [:div.row.spaced
           [:p [remaining-time (ui-utils/gql-date->date commit-period-end)]]
           [:div.form-btns
            [:div.cta-btns
             [inputs/pending-button
              {:class "cta-btn"
               :pending? @tx-pending?
               :disabled (or (-> @errors :local :vote/amount empty? not)
                           @tx-success?)
               :pending-text "Voting..."
               :on-click #(vote :vote.option/include)}
              "Vote For"]
             [inputs/pending-button
              {:class "cta-btn"
               :pending? @tx-pending?
               :disabled (or (-> @errors :local :vote/amount empty? not)
                           @tx-success?)
               :pending-text "Voting..."
               :on-click #(vote :vote.option/exclude)}
              "Vote Against"]]
            [:fieldset
             [inputs/amount-input {:form-data form-data
                                   :id :vote/amount
                                   :placeholder "DNT"
                                   :errors errors}]]]
           [:div
            [:p "You can vote with up to " (-> @balance-dnt
                                             (web3/from-wei :ether)
                                             format/format-dnt)]
            [:p "Tokens will be returned to you after revealing your vote."]]]]]))))

(defn vote-reveal-section [{:as district
                            :keys [:reg-entry/address
                                   :reg-entry/status
                                   :reg-entry/challenges]}]
  (when (= "regEntry_status_revealPeriod" status)
    (let [{:as challenge
           :keys [:challenge/reveal-period-end :challenge/vote]} (last challenges)
          tx-id address
          tx-pending? (subscribe [::tx-id-subs/tx-pending? {::reg-entry/reveal-vote tx-id}])
          tx-success? (subscribe [::tx-id-subs/tx-success? {::reg-entry/reveal-vote tx-id}])]
      (fn []
        [:div
         [:div.h-line]
         [:h2 "Reveal"]
         [:p
          "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec a augue quis metus sollicitudin mattis. Duis efficitur tellus felis, et tincidunt turpis aliquet non. Aenean augue metus, malesuada non rutrum ut, ornare ac orci."]
         [:form.voting
          [:div.row.spaced
           [:p [remaining-time (ui-utils/gql-date->date reveal-period-end)]]
           [tx-button/tx-button {:class "cta-btn"
                                 :primary true
                                 :disabled @tx-success?
                                 :pending? @tx-pending?
                                 :pending-text "Revealing..."
                                 :on-click #(dispatch [::reg-entry/reveal-vote
                                                       {:send-tx/id tx-id
                                                        :reg-entry/address address}
                                                       vote])}
            "Reveal My Vote"]]]]))))

(defn main [{:as props
             :keys [district active-account]}]
  (let [query (subscribe [::gql/query
                          {:queries [(build-query props)]}])
        {:keys [district config]} @query]
    (cond
      (nil? district) nil
      (-> district :reg-entry/address nil?) [not-found/not-found]
      :else [:section#main
             [:div.container
              [info-section district]
              [:div.box-wrap.stats
               [:div.body-text
                [:div.container
                 [stake-section district]
                 [challenge-section district]
                 [vote-commit-section district]
                 [vote-reveal-section district]]]]]])))

(defmethod page :route/detail [& x]
  (let [params (subscribe [::router-subs/active-page-params])
        active-account (subscribe [::account-subs/active-account])]
    [app-layout
     [main {:district (:address @params)
            :active-account @active-account}]]))
