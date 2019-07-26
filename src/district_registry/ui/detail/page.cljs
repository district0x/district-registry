(ns district-registry.ui.detail.page
  (:require
    [bignumber.core :as bn]
    [cljsjs.bignumber]
    [clojure.string :as str]
    [district-registry.ui.components.app-layout :refer [app-layout]]
    [district-registry.ui.components.donut-chart :refer [donut-chart]]
    [district-registry.ui.components.nav :as nav]
    [district-registry.ui.components.stake :as stake]
    [district-registry.ui.contract.registry-entry :as reg-entry]
    [district-registry.ui.events :as events]
    [district-registry.ui.not-found.page :as not-found]
    [district-registry.ui.spec :as spec]
    [district-registry.ui.subs :as subs]
    [district.format :as format]
    [district.graphql-utils :as gql-utils]
    [district.parsers :as parsers]
    [district.ui.component.form.input :as inputs]
    [district.ui.component.page :refer [page]]
    [district.ui.component.tx-button :refer [tx-button]]
    [district.ui.graphql.subs :as gql]
    [district.ui.ipfs.subs :as ipfs-subs]
    [district.ui.now.subs :as now-subs]
    [district.ui.router.subs :as router-subs]
    [district.ui.web3-account-balances.subs :as account-balances-subs]
    [district.ui.web3-accounts.subs :as account-subs]
    [district.ui.web3-tx-id.subs :as tx-id-subs]
    [district.web3-utils :as web3-utils]
    [goog.string :as gstring]
    [medley.core :as medley]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [reagent.ratom :as ratom]))

(def format-dnt (comp format/format-dnt web3-utils/wei->eth-number))
(def format-number (comp format/format-number web3-utils/wei->eth-number))
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
     [:challenge/index
      :challenge/challenger
      :challenge/comment
      :challenge/created-on
      :challenge/reward-pool
      :challenge/commit-period-end
      :challenge/reveal-period-end
      :challenge/votes-include
      :challenge/votes-exclude
      :challenge/votes-include-from-staking
      :challenge/challenger-reward-claimed-on
      :challenge/creator-reward-claimed-on
      :challenge/winning-vote-option
      [:challenge/vote {:voter active-account}
       [:vote/secret-hash
        :vote/option
        :vote/amount
        :vote/revealed-on
        :vote/claimed-reward-on
        :vote/reclaimed-votes-on
        :vote/reward
        :vote/amount-from-staking]]]]
    :district/meta-hash
    :district/name
    :district/description
    :district/url
    :district/github-url
    :district/facebook-url
    :district/twitter-url
    :district/aragon-id
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


(defn district-background []
  (let [ipfs (subscribe [::ipfs-subs/ipfs])]
    (fn [image-hash]
      (when image-hash
        (when-let [url (:gateway @ipfs)]
          [:div.background-image {:style {:background-image (str "url('" (format/ensure-trailing-slash url) image-hash "')")}}
           [:img {:src "/images/district-bg-mask.png"}]])))))


(defn- edit-district-button [{:keys [:reg-entry/address :reg-entry/creator :reg-entry/status]}]
  (let [active-account (subscribe [::account-subs/active-account])]
    (when (and address
               creator
               status
               (= @active-account creator)
               (= (gql-utils/gql-name->kw status) :reg-entry.status/whitelisted))
      [:form.edit-district-button
       [nav/a
        {:route [:route/edit {:address address}]}
        [:button.cta-btn "Edit"]]])))


(defn- prettify-url [url]
  (-> url
    (str/replace #"https?://" "")
    (str/replace #"/$" "")))


(defn info-section [{:keys [:district/name
                            :district/description
                            :district/background-image-hash
                            :district/logo-image-hash
                            :district/url
                            :district/github-url
                            :district/facebook-url
                            :district/twitter-url
                            :district/aragon-id
                            :district/total-supply
                            :district/dnt-staked
                            :reg-entry/status
                            :reg-entry/created-on
                            :reg-entry/address
                            :reg-entry/creator]}]
  (let []
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
           [:a {:href url :target :_blank} (prettify-url url)]]]
         [:ul.details-list
          (let [status (-> status
                         normalize-status
                         cljs.core/name)]
            [:li.status
             {:class status}
             (str "Status: " (str/capitalize status))])
          [:li (str "Added: " (format-date created-on))]
          [:li (str "Total staked: " (format-dnt dnt-staked))]
          [:li (str "Voting tokens issued: " (format-number total-supply))]]
         [:nav.social
          [:ul
           [:li
            [:a {:target "_blank"
                 :href @(subscribe [::subs/aragon-url aragon-id])}
             [:span.icon-aragon]]]
           (when github-url
             [:li
              [:a {:target "_blank"
                   :href github-url}
               [:span.icon-github]]])
           (when facebook-url
             [:li
              [:a {:target "_blank"
                   :href facebook-url}
               [:span.icon-facebook]]])
           (when twitter-url
             [:li [:a {:target "_blank"
                       :href twitter-url}
                   [:span.icon-twitter]]])]]]
        [:div.col.img
         [district-background background-image-hash]]]
       [:pre.district-description description]
       [edit-district-button
        {:reg-entry/address address
         :reg-entry/creator creator
         :reg-entry/status status}]]]]))


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
    (fn [{:keys [:reg-entry/address :reg-entry/status :reg-entry/deposit :district/name]}]
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
                                       :district/name name
                                       :comment (:challenge/comment @form-data)
                                       :deposit deposit}]))}
              "Challenge"]]]])))))


(defn format-remaining-time [to-time]
  (let [time-remaining (subscribe [::now-subs/time-remaining to-time])
        {:keys [:days :hours :minutes :seconds]} @time-remaining]
    (when-not (every? zero? [days hours minutes seconds])
      (str (format/pluralize days "day") " " (format/pluralize hours "hour")
           " " minutes " min. " seconds " sec."))))


(defn- dispatch-vote [e option address district-name form-data]
  (js-invoke e "preventDefault")
  (dispatch [::reg-entry/approve-and-commit-vote
             {:reg-entry/address address
              :district/name district-name
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
   [:pre.challenge-comment comment]])

(defn- dispatch-period-finished [period]
  (js/setTimeout #(dispatch [period]) 1000))

(defn vote-commit-section []
  (let [balance-dnt (subscribe [::account-balances-subs/active-account-balance :DNT])
        form-data (r/atom {:vote/amount ""})
        errors (ratom/reaction {:local {}})
        period-finished-event-fired? (r/atom false)]
    (fn [{:keys [:reg-entry/address :reg-entry/status :reg-entry/challenges :district/name]}
         {:keys [:challenge/commit-period-end :challenge/comment] :as challange}]
      (let [tx-pending? @(subscribe [::tx-id-subs/tx-pending? {:approve-and-commit-vote {:reg-entry/address address}}])
            remaining-time (format-remaining-time (gql-utils/gql-date->date commit-period-end))
            {:keys [:challenge/vote]} (last challenges)
            voted? (pos? (:vote/amount vote))]
        (when (and commit-period-end
                   (not remaining-time)
                   (not @period-finished-event-fired?))
          (reset! period-finished-event-fired? true)
          (dispatch-period-finished ::reg-entry/commit-period-finished))

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
               :on-click #(dispatch-vote % :vote-option/include address name @form-data)}
              (if voted?
                "Voted"
                "Vote For")]
             (when-not voted?
               [tx-button
                {:class "cta-btn"
                 :pending? tx-pending?
                 :disabled (vote-button-disabled? @form-data @balance-dnt voted?)
                 :pending-text "Voting..."
                 :on-click #(dispatch-vote % :vote-option/exclude address name @form-data)}
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
  (let [period-finished-event-fired? (r/atom false)]
    (fn [{:keys [:reg-entry/address :reg-entry/status :reg-entry/challenges :district/name]}
         {:keys [:challenge/comment :challenge/reveal-period-end :challenge/vote] :as challenge}]
      (let [tx-pending? (subscribe [::tx-id-subs/tx-pending? {:reveal-vote {:reg-entry/address address}}])
            remaining-time (format-remaining-time (gql-utils/gql-date->date reveal-period-end))
            no-vote? (not (pos? (:vote/amount vote)))
            stored-vote @(subscribe [::subs/vote address])]

        (when (and reveal-period-end
                   (not remaining-time)
                   (not @period-finished-event-fired?))
          (reset! period-finished-event-fired? true)
          (dispatch-period-finished ::reg-entry/commit-period-finished))


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
                         (dispatch [::reg-entry/reveal-vote {:reg-entry/address address
                                                             :district/name name}]))}
            (cond
              (:vote/revealed-on vote) "Revealed"
              no-vote? "You haven't voted"
              (and (not no-vote?) (not stored-vote)) "Secret not found in your browser"
              :else "Reveal My Vote")]]]]))))

(defn- vote-reward-line [reward]
  (when reward
    [:span "Your vote reward: " [:b (format-dnt reward)] [:br]]))


(defn- calculate-challenger-reward [deposit reward-pool]
  (+ deposit (- deposit reward-pool)))


(defn- calculate-creator-reward [deposit reward-pool]
  (- deposit reward-pool))


(defn- challenger-reward-line [reward]
  (when reward
    [:span "Your challenge reward: " [:b (format-dnt reward)] [:br]]))


(defn- creator-reward-line [reward]
  (when reward
    [:span "Your challenge reward: " [:b (format-dnt reward)] [:br]]))


(defn- total-reward-line [reward]
  (when reward
    [:span "Your total reward: " [:b (format-dnt reward)] [:br]]))


(defn- vote-amount-line [{:keys [:vote/option :vote/amount :vote/amount-from-staking :challenge/votes-include :challenge/votes-exclude]}]
  [:<>
   (when (contains? #{:vote-option/include :vote-option/exclude} option)
     [:span "You voted: "
      [:b
       (gstring/format "%s for %s %s (%s)"
                       (format-dnt amount)
                       (case option
                         :vote-option/include "inclusion"
                         :vote-option/exclude "blacklisting")
                       (if (and (pos? amount-from-staking)
                                (= option :vote-option/include))
                         (str "(" (format-dnt amount-from-staking) " staked)")
                         "")
                       (format/format-percentage
                         amount
                         (case option
                           :vote-option/include votes-include
                           :vote-option/exclude votes-exclude)))] [:br]])
   (when (and (not= option :vote-option/include)
              (pos? amount-from-staking))
     [:span "You staked: " [:b (format-dnt amount-from-staking)] [:br]])])


(defn claim-reward-button [{:keys [:reg-entry/address
                                   :challenge/index
                                   :challenge/challenger
                                   :reg-entry/creator
                                   :vote/revealed-on
                                   :vote/reclaimed-votes-on
                                   :vote/amount] :as args}]
  (let [active-account @(subscribe [::account-subs/active-account])
        voted? (pos? amount)
        revealed? (boolean revealed-on)
        votes-reclaimed? (boolean reclaimed-votes-on)
        reward-claimed? (boolean (or (and (= challenger active-account)
                                          (:challenge/challenger-reward-claimed-on args))
                                     (:vote/claimed-reward-on args)
                                     (and (= creator active-account)
                                          (:challenge/creator-reward-claimed-on args))))
        has-reward-to-claim? (and (or (pos? (:challenge/challenger-reward args))
                                      (pos? (:vote/reward args))
                                      (pos? (:challenge/creator-reward args)))
                                  (not reward-claimed?))
        has-votes-to-reclaim? (and voted?
                                   (not revealed?)
                                   (not votes-reclaimed?))]
    [tx-button
     {:class "cta-btn"
      :disabled (and (not has-reward-to-claim?)
                     (not has-votes-to-reclaim?))
      :pending? @(subscribe [::tx-id-subs/tx-pending? {:claim-reward {:reg-entry/address address :challenge/index index}}])
      :pending-text "Claiming..."
      :on-click (fn [e]
                  (js-invoke e "preventDefault")
                  (dispatch [::reg-entry/claim-reward {:reg-entry/address address
                                                       :challenge/index index
                                                       :district/name (:district/name args)}]))}
     (cond
       has-reward-to-claim? "Claim Reward"
       has-votes-to-reclaim? "Reclaim Votes"
       reward-claimed? "Reward Claimed"
       votes-reclaimed? "Votes Reclaimed"
       voted? "You have no reward"
       :else "You haven't voted")]))


(defn vote-results-section []
  (fn [{:keys [:reg-entry/address :reg-entry/deposit :district/name :reg-entry/creator]}
       {:keys [:challenge/index
               :challenge/challenger
               :challenge/votes-include
               :challenge/votes-exclude
               :challenge/votes-include-from-staking
               :challenge/vote
               :challenge/winning-vote-option
               :challenge/challenger-reward-claimed-on
               :challenge/reveal-period-end
               :challenge/reward-pool] :as challenge}]
    (let [{:keys [:vote/option
                  :vote/amount
                  :vote/reward
                  :vote/claimed-reward-on
                  :vote/reclaimed-votes-on
                  :vote/revealed-on
                  :vote/amount-from-staking]} vote
          user-vote-option (gql-utils/gql-name->kw option)
          winning-vote-option (gql-utils/gql-name->kw winning-vote-option)
          active-account @(subscribe [::account-subs/active-account])
          challenger-reward (when (and (= challenger active-account)
                                       (= winning-vote-option :vote-option/exclude))
                              (calculate-challenger-reward deposit reward-pool))
          creator-reward (when (and (= creator active-account)
                                    (= winning-vote-option :vote-option/include))
                           (calculate-creator-reward deposit reward-pool))
          votes-total (+ votes-include votes-exclude)]
      [:div
       [:h2 "Vote Results"]
       [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec a augue quis metus sollicitudin mattis. Duis efficitur tellus felis, et tincidunt turpis aliquet non. Aenean augue metus, malesuada non rutrum ut, ornare ac orci."]
       [challenger-comment challenge]
       [:div.row.spaced
        [:div
         [:p
          [:span "Voting finished: " [:b (format-date reveal-period-end)]] [:br]
          [:span "Voted for inclusion: "
           [:b (gstring/format "%s %s (%s)"
                               (format-dnt votes-include)
                               (if (pos? votes-include-from-staking)
                                 (str "(" (format-dnt votes-include-from-staking) " staked)")
                                 "")
                               (format/format-percentage votes-include votes-total))]] [:br]
          [:span "Voted for blacklisting: "
           [:b
            (gstring/format "%s (%s)"
                            (format-dnt votes-exclude)
                            (format/format-percentage votes-exclude votes-total))]] [:br]
          [vote-amount-line
           {:vote/option user-vote-option
            :vote/amount amount
            :vote/amount-from-staking amount-from-staking
            :challenge/votes-exclude votes-exclude
            :challenge/votes-include votes-include}]
          [challenger-reward-line challenger-reward]
          [creator-reward-line creator-reward]
          [vote-reward-line reward]
          [total-reward-line (when (and challenger-reward reward)
                               (+ challenger-reward reward))]]
         [:form
          [claim-reward-button
           {:reg-entry/address address
            :reg-entry/creator creator
            :district/name name
            :challenge/index index
            :challenge/challenger-reward challenger-reward
            :challenge/creator-reward creator-reward
            :challenge/challenger-reward-claimed-on (:challenge/challenger-reward-claimed-on challenge)
            :challenge/challenger challenger
            :vote/reward reward
            :vote/claimed-reward-on claimed-reward-on
            :vote/reclaimed-votes-on reclaimed-votes-on
            :vote/revealed-on revealed-on
            :vote/amount amount}]]]
        (when (or (pos? votes-include)
                  (pos? votes-exclude))
          [donut-chart {:reg-entry/address address
                        :challenge/index index
                        :challenge/votes-include votes-include
                        :challenge/votes-exclude votes-exclude}])]])))


(defn main [props]
  (let [query (subscribe [::gql/query
                          {:queries [(build-query props)]}
                          {:refetch-on #{::reg-entry/approve-and-create-challenge-success
                                         ::reg-entry/approve-and-commit-vote-success
                                         ::reg-entry/reveal-vote-success
                                         ::reg-entry/reclaim-votes-success
                                         ::reg-entry/claim-reward-success
                                         ::reg-entry/commit-period-finished
                                         ::reg-entry/reveal-period-finished}}])
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