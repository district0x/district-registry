(ns district-registry.ui.detail.page
  (:require
    [bignumber.core :as bn]
    [cljsjs.bignumber]
    [clojure.pprint :refer [pprint]]
    [clojure.string :as str]
    [district-registry.ui.components.app-layout :refer [app-layout]]
    [district-registry.ui.components.stake :as stake]
    [district-registry.ui.contract.registry-entry :as reg-entry]
    [district-registry.ui.events :as events]
    [district-registry.ui.not-found.page :as not-found]
    [district-registry.ui.spec :as spec]
    [district-registry.ui.subs :as district-registry-subs]
    [district.format :as format]
    [district.graphql-utils :as gql-utils]
    [district.parsers :as parsers]
    [district.ui.component.form.input :as inputs]
    [district.ui.component.page :refer [page]]
    [district.ui.component.tx-button :refer [tx-button]]
    [district.ui.graphql.subs :as gql]
    [district.ui.now.subs :as now-subs]
    [district.ui.router.subs :as router-subs]
    [district.ui.web3-account-balances.subs :as account-balances-subs]
    [district.ui.web3-accounts.subs :as account-subs]
    [district.ui.web3-tx-id.subs :as tx-id-subs]
    [district.web3-utils :as web3-utils]
    [goog.string :as gstring]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [reagent.ratom :as ratom]
    [medley.core :as medley]))

(def format-dnt (comp format/format-dnt web3-utils/wei->eth-number))
(def format-date (comp format/format-local-date gql-utils/gql-date->date))

(defn build-query [{:keys [:district :active-account]}]
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
      :challenge/comment
      :challenge/created-on
      :challenge/reward-pool
      :challenge/commit-period-end
      :challenge/reveal-period-end
      :challenge/votes-include
      :challenge/votes-exclude
      :challenge/votes-total
      :challenge/claimed-reward-on
      :challenge/winning-vote-option
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
  (case (gql-utils/gql-name->kw status)
    (:reg-entry.status/challenge-period :reg-entry.status/whitelisted) :in-registry
    (:reg-entry.status/commit-period :reg-entry.status/reveal-period) :challenged
    :reg-entry.status/blacklisted :blacklisted))


(defn district-background [image-hash]
  (when image-hash
    (let [gateway (subscribe [::gql/query {:queries [[:config [[:ipfs [:gateway]]]]]}])]
      (when-not (:graphql/loading? @gateway)
        (if-let [url (-> @gateway :config :ipfs :gateway)]
          [:div.background-image {:style {:background-image (str "url('" (format/ensure-trailing-slash url) image-hash "')")}}
           [:img {:src "/images/district-bg-mask.png"}]])))))

(defn info-section [{:keys [:district/name
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
          [:a {:href github-url :target :_blank}
           [:img {:src "/images/icon-fc-github@2x.png"}]]]
         [:div.title-icon                                   ; TODO Aragon link
          [:img {:src "/images/icon-fc-bird@2x.png"}]]]]
       [:ul.details-list
        [:li (str "Status: " (-> status
                               normalize-status
                               cljs.core/name
                               str/capitalize))]
        [:li (str "Added: " (format-date created-on))]
        [:li (str "Staked total: " (format-dnt dnt-staked))]
        [:li (str "Voting tokens issued: " (format-dnt total-supply))]]
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
       [district-background background-image-hash]]]
     [:pre.district-description description]]]])

(defn stake-section [{:keys [:reg-entry/address :reg-entry/status :district/dnt-weight]}]
  [:div
   [:h2 "Stake"]
   [:p
    "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec a augue quis metus sollicitudin mattis. Duis efficitur tellus felis, et tincidunt turpis aliquet non. Aenean augue metus, malesuada non rutrum ut, ornare ac orci."]
   [:h3 "Voting Token Issuance Curve"]
   [:img.spacer {:src (str "/images/curve-graph-" dnt-weight "-l.svg")}]
   [:div.stake
    [:div.row.spaced
     [stake/stake-info address]
     [stake/stake-form address]]]])

(defn challenge-section []
  (let [form-data (r/atom {:challenge/comment nil})
        errors (ratom/reaction {:local (when-not (spec/check ::spec/challenge-comment (:challenge/comment @form-data))
                                         {:challenge/comment "Comment shouldn't be empty."})})]
    (fn [{:keys [:reg-entry/address :reg-entry/status :reg-entry/deposit]}]
      (when (= (normalize-status status) :in-registry)
        (let [tx-pending? @(subscribe [::tx-id-subs/tx-pending? {:approve-and-create-challenge {:reg-entry/address address}}])]
          [:div
           [:div.h-line]
           [:h2 "Challenge"]
           [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec a augue quis metus sollicitudin mattis. Duis efficitur tellus felis, et tincidunt turpis aliquet non. Aenean augue metus, malesuada non rutrum ut, ornare ac orci."]
           [:form.challenge
            [inputs/textarea-input {:form-data form-data
                                    :id :challenge/comment
                                    :errors errors}]
            [:div.form-btns
             [:p (format/format-dnt (web3-utils/wei->eth-number deposit))]
             [tx-button
              {:class "cta-btn"
               :primary true
               :disabled (-> @errors :local boolean)
               :pending? tx-pending?
               :pending-text "Challenging..."
               :on-click (fn [e]
                           (js-invoke e "preventDefault")
                           (dispatch [::events/add-challenge
                                      {:reg-entry/address address
                                       :comment (:challenge/comment @form-data)
                                       :deposit deposit}]))}
              "Challenge"]]]])))))


(defn format-remaining-time [to-time]
  (let [time-remaining (subscribe [::now-subs/time-remaining to-time])
        {:keys [:days :hours :minutes :seconds]} @time-remaining]
    (when-not (every? zero? [days hours minutes seconds])
      (str (format/pluralize days "day") " " (format/pluralize hours "hour")
           " " minutes " min. " seconds " sec."))))


(defn- dispatch-vote [e option address form-data]
  (js-invoke e "preventDefault")
  (dispatch [::reg-entry/approve-and-commit-vote
             {:reg-entry/address address
              :vote/option option
              :vote/amount (-> form-data :vote/amount parsers/parse-float web3-utils/eth->wei)}]))


(defn- vote-button-disabled? [form-data balance-dnt voted?]
  (let [amount (parsers/parse-float (:vote/amount form-data))]
    (or
      (not amount)
      (bn/> (web3-utils/eth->wei amount) balance-dnt)
      voted?)))


(defn- challenger-comment [{:keys [:challenge/challenger :challenge/comment]}]
  [:div.row.spaced
   [:a.challenger-address
    {:href (format/etherscan-addr-url challenger)
     :target :_blank}
    "Challenger (" (subs challenger 0 7) "...):"]
   [:pre.challenge-comment (str "\"" comment "\"")]])


(defn vote-commit-section []
  (let [balance-dnt (subscribe [::account-balances-subs/active-account-balance :DNT])
        form-data (r/atom {:vote/amount ""})
        errors (ratom/reaction {:local {}})]
    (fn [{:keys [:reg-entry/address :reg-entry/status :reg-entry/challenges]}
         {:keys [:challenge/commit-period-end :challenge/comment] :as challange}]
      (let [tx-pending? @(subscribe [::tx-id-subs/tx-pending? {:approve-and-commit-vote {:reg-entry/address address}}])
            remaining-time (format-remaining-time (gql-utils/gql-date->date commit-period-end))
            {:keys [:challenge/vote]} (last challenges)
            voted? (pos? (:vote/amount vote))]
        [:div
         [:h2 "Vote"]
         [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec a augue quis metus sollicitudin mattis. Duis efficitur tellus felis, et tincidunt turpis aliquet non. Aenean augue metus, malesuada non rutrum ut, ornare ac orci."]
         [:form.voting
          [challenger-comment challange]
          [:div.row.spaced
           [:p [:b.remaining-time
                "Voting period "
                (if remaining-time
                  (str "ends in " remaining-time)
                  "has finished.")]]
           [:div.form-btns
            [:div.cta-btns
             [tx-button
              {:class "cta-btn"
               :pending? tx-pending?
               :disabled (vote-button-disabled? @form-data @balance-dnt voted?)
               :pending-text "Voting..."
               :on-click #(dispatch-vote % :vote-option/include address @form-data)}
              (if voted?
                "Voted"
                "Vote For")]
             (when-not voted?
               [tx-button
                {:class "cta-btn"
                 :pending? tx-pending?
                 :disabled (vote-button-disabled? @form-data @balance-dnt voted?)
                 :pending-text "Voting..."
                 :on-click #(dispatch-vote % :vote-option/exclude address @form-data)}
                "Vote Against"])]
            [:fieldset
             [inputs/amount-input
              {:class "dnt-input"
               :form-data form-data
               :id :vote/amount
               :errors errors
               :type :number
               :disabled (or (nil? remaining-time) voted?)}]
             [:span.cur "DNT"]]]
           [:div
            [:p "You can vote with up to "
             (-> @balance-dnt
               web3-utils/wei->eth-number
               format/format-dnt)
             "."
             [:br]
             "Tokens will be returned to you after revealing your vote."]]]]]))))


(defn vote-reveal-section []
  (fn [{:keys [:reg-entry/address :reg-entry/status :reg-entry/challenges]}
       {:keys [:challenge/comment :challenge/reveal-period-end :challenge/vote] :as challenge}]
    (let [tx-pending? (subscribe [::tx-id-subs/tx-pending? {:reveal-vote {:reg-entry/address address}}])
          remaining-time (format-remaining-time (gql-utils/gql-date->date reveal-period-end))
          no-vote? (and (:vote/option vote)
                        (= :vote-option/neither (gql-utils/gql-name->kw (:vote/option vote))))
          stored-vote @(subscribe [::district-registry-subs/vote address])]
      [:div
       [:h2 "Reveal"]
       [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec a augue quis metus sollicitudin mattis. Duis efficitur tellus felis, et tincidunt turpis aliquet non. Aenean augue metus, malesuada non rutrum ut, ornare ac orci."]
       [challenger-comment challenge]
       [:form.voting
        [:div.row.spaced
         [:p [:b (str "Reveal period " (if remaining-time
                                         (str "ends in " remaining-time)
                                         "has finished."))]]
         [tx-button
          {:class "cta-btn"
           :primary true
           :disabled (or (not remaining-time)
                         (boolean (:vote/revealed-on vote))
                         no-vote?
                         (and (not no-vote?) (not stored-vote)))
           :pending? @tx-pending?
           :pending-text "Revealing..."
           :on-click (fn [e]
                       (js-invoke e "preventDefault")
                       (dispatch [::reg-entry/reveal-vote {:reg-entry/address address}]))}
          (cond
            (:vote/revealed-on vote) "Revealed"
            no-vote? "You haven't voted"
            (and (not no-vote?) (not stored-vote)) "Secret not found in your browser"
            :else "Reveal My Vote")]]]])))

(defn- vote-reward-line [reward]
  (when reward
   [:span "Your vote reward: " (format-dnt reward) [:br]]))


(defn- calculate-challenge-reward [deposit reward-pool]
  (+ deposit (- deposit reward-pool)))


(defn- challenge-reward-line [reward]
  (when reward
    [:span "Your challenge reward: " [:b (format-dnt reward)] [:br]]))


(defn- total-reward-line [reward]
  (when reward
    [:span "Your total reward: " [:b (format-dnt reward)] [:br]]))


(defn vote-results-section []
  (fn [{:keys [:reg-entry/deposit]}
       {:keys [:challenge/challenger
               :challenge/votes-include
               :challenge/votes-exclude
               :challenge/votes-total
               :challenge/vote
               :challenge/winning-vote-option
               :challenge/claimed-reward-on
               :challenge/reveal-period-end
               :challenge/reward-pool] :as challenge}]
    (let [{:keys [:vote/option :vote/amount :vote/reward]} vote
          user-vote-option (gql-utils/gql-name->kw option)
          winning-vote-option (gql-utils/gql-name->kw winning-vote-option)
          active-account @(subscribe [::account-subs/active-account])
          challenge-reward (when (and (= challenger active-account)
                                      (= winning-vote-option :vote-option/exclude))
                             (calculate-challenge-reward deposit reward-pool))]
      #_ (print.foo/look vote)
      [:div
       [:h2 "Vote Results"]
       [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec a augue quis metus sollicitudin mattis. Duis efficitur tellus felis, et tincidunt turpis aliquet non. Aenean augue metus, malesuada non rutrum ut, ornare ac orci."]
       [challenger-comment challenge]
       [:div.row.spaced
        [:p
         [:span "Voting finished: " [:b (format-date reveal-period-end)]] [:br]
         [:span "Voted for inclusion: " [:b (format-dnt votes-include)]] [:br]
         [:span "Voted for blacklisting: " [:b (format-dnt votes-exclude)]] [:br]
         (when (contains? #{:vote-option/include :vote-option/exclude} user-vote-option)
           [:span "You voted: "
            [:b
             (gstring/format "%s for %s (%s)"
                             (format-dnt amount)
                             (case user-vote-option
                               :vote-option/include "inclusion"
                               :vote-option/exclude "blacklisting")
                             (format/format-percentage
                               amount
                               (case user-vote-option
                                 :vote-option/include votes-include
                                 :vote-option/exclude votes-exclude)))] [:br]])

         [challenge-reward-line challenge-reward]
         [vote-reward-line reward]
         [total-reward-line (when (and challenge-reward reward)
                              (+ challenge-reward reward))]]]])))


(defn main [props]
  (let [query (subscribe [::gql/query
                          {:queries [(build-query props)]}
                          {:refetch-on #{::reg-entry/approve-and-create-challenge-success
                                         ::reg-entry/approve-and-commit-vote-success
                                         ::reg-entry/reveal-vote-success
                                         ::reg-entry/reclaim-vote-amount-success
                                         ::reg-entry/claim-vote-reward-success}}])
        {:keys [:district]} @query
        {:keys [:reg-entry/challenges :reg-entry/status]} district
        reversed-challenges (reverse challenges)]
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
                 [challenge-section district]]]]
              (when (seq challenges)
                [:div.box-wrap.challenges
                 [:div.body-text
                  (for [[i challenge] (medley/indexed reversed-challenges)]
                    (let [current-challenge? (= challenge (first reversed-challenges))]
                      [:div.container.challenge
                       {:key i}
                       (when-not current-challenge?
                         [:div.h-line])
                       (cond
                         (and current-challenge?
                              (= :reg-entry.status/commit-period (gql-utils/gql-name->kw status)))
                         [vote-commit-section district challenge]

                         (and current-challenge?
                              (= :reg-entry.status/reveal-period (gql-utils/gql-name->kw status)))
                         [vote-reveal-section district challenge]

                         :else
                         [vote-results-section district challenge])]))]])]])))


(defmethod page :route/detail [& x]
  (let [params (subscribe [::router-subs/active-page-params])
        active-account (subscribe [::account-subs/active-account])]
    [app-layout
     [main {:district (:address @params)
            :active-account @active-account}]]))