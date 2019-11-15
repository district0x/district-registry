(ns district-registry.server.graphql-resolvers
  (:require
    [district-registry.server.utils :as server-utils]
    [district-registry.shared.utils :refer [vote-option->kw]]
    [district.cljs-utils :as cljs-utils]
    [district.graphql-utils :as graphql-utils]
    [district.parsers :as parsers]
    [district.server.db :as db]
    [district-registry.server.db :as server-db]
    [district.server.smart-contracts :as smart-contracts]
    [district.shared.error-handling :refer [try-catch-throw]]
    [honeysql.core :as sql]
    [honeysql.format :as sql-format]
    [honeysql.helpers :as sqlh]
    [taoensso.timbre :as log]))


(def enum graphql-utils/kw->gql-name)

(defn paged-query
  "Execute a paged query.
  query: a map honeysql query.
  page-size: a int
  page-start-idx: a int
  Returns a map with [:items :total-count :end-cursor :has-next-page]"
  [query page-size page-start-idx]
  (let [paged-query (cond-> query
                      page-size (assoc :limit page-size)
                      page-start-idx (assoc :offset page-start-idx))
        total-count (count (db/all query))
        result (db/all paged-query)
        last-idx (cond-> (count result)
                   page-start-idx (+ page-start-idx))]
    {:items result
     :total-count total-count
     :end-cursor (str last-idx)
     :has-next-page (not= last-idx total-count)}))


(defn district-query-resolver [_ {:keys [:reg-entry/address]}]
  (db/get {:select [:*]
           :from [[:districts :d] [:reg-entries :re]]
           :where [:and
                   [:= :d.reg-entry/address :re.reg-entry/address]
                   [:= :re.reg-entry/address address]]}))

(defn reg-entry-status-sql-clause [now]
  (sql/call
    :case
    [:and
     [:< now :re.reg-entry/challenge-period-end]
     [:= :c.challenge/index nil]] (enum :reg-entry.status/challenge-period)
    [:< now :c.challenge/commit-period-end] (enum :reg-entry.status/commit-period)
    [:< now :c.challenge/reveal-period-end] (enum :reg-entry.status/reveal-period)
    [:or
     [:< :c.challenge/votes-exclude
      ;; Count staked DNT as votes-include
      (sql/raw
        (str (sql-format/*name-transform-fn* (cljs-utils/kw->str :c.challenge/votes-include)) " + ("
             (first (sql/format
                      {:select [(sql/call :case
                                          [:= :%count.* (sql/inline 0)] (sql/inline 0)
                                          :else :stake-history/dnt-total-staked)]
                       :from [[:stake-history :sh]]
                       :group-by [:sh.reg-entry/address]
                       :having [:<= (sql/call :max :sh.stake-history/staked-on) :c.challenge/commit-period-end]
                       :where [:and
                               [:= :re.reg-entry/address :sh.reg-entry/address]
                               [:<= :sh.stake-history/staked-on :c.challenge/commit-period-end]]}))
             ")"))]
     [:< :re.reg-entry/challenge-period-end now]] (enum :reg-entry.status/whitelisted)
    :else (enum :reg-entry.status/blacklisted)))


(defn search-districts-query-resolver [_ {:keys [:statuses :order-by :order-dir :first :after] :as args}]
  (log/debug "search-districts-query-resolver" args)
  (try-catch-throw
    (let [statuses-set (when statuses (set statuses))
          now (server-utils/now-in-seconds)
          page-start-idx (when after (parsers/parse-int after))
          page-size first
          query (cond-> {:select [:re.* :d.*]
                         :from [[:reg-entries :re]]
                         :join [[:districts :d] [:= :re.reg-entry/address :d.reg-entry/address]]
                         :left-join [[:challenges :c] [:and
                                                       [:= :re.reg-entry/address :c.reg-entry/address]
                                                       [:= :re.reg-entry/current-challenge-index :c.challenge/index]]]}
                  statuses-set (sqlh/merge-where [:in (reg-entry-status-sql-clause now) statuses-set])
                  order-by (sqlh/merge-order-by [[(get {:districts.order-by/reveal-period-end :c.challenge/reveal-period-end
                                                        :districts.order-by/commited-period-end :c.challenge/commit-period-end
                                                        :districts.order-by/created-on :re.reg-entry/created-on
                                                        :districts.order-by/dnt-staked :d.district/dnt-staked
                                                        :districts.order-by/total-supply :d.district/total-supply}
                                                       (graphql-utils/gql-name->kw order-by))
                                                  (or (keyword order-dir) :asc)]]))]
      (paged-query query page-size page-start-idx))))


(defn param-change-query-resolver [_ {:keys [:reg-entry/address] :as args}]
  (log/debug "param-change args" args)
  (try-catch-throw
    (let [sql-query (db/get {:select [:*]
                             :from [:param-changes]
                             :join [:reg-entries [:= :reg-entries.reg-entry/address :param-changes.reg-entry/address]]
                             :where [:= address :param-changes.reg-entry/address]})]
      (log/debug "param-change query" sql-query)
      sql-query)))


(defn search-param-changes-query-resolver [_ {:keys [:key :db :order-by :order-dir :group-by :first :after]
                                              :or {order-dir :asc}
                                              :as args}]
  (log/debug "search-param-changes args" args)
  (try-catch-throw
    (let [db (if (contains? #{"districtRegistryDb" "paramChangeRegistryDb"} db)
               (smart-contracts/contract-address (graphql-utils/gql-name->kw db))
               db)
          param-changes-query (cond-> {:select [:*]
                                       :from [:param-changes]
                                       :left-join [:reg-entries [:= :reg-entries.reg-entry/address :param-changes.reg-entry/address]]}
                                key (sqlh/merge-where [:= key :param-changes.param-change/key])
                                db (sqlh/merge-where [:= db :param-changes.param-change/db])
                                order-by (sqlh/merge-where [:not= nil :param-changes.param-change/applied-on])
                                order-by (sqlh/merge-order-by [[(get {:param-changes.order-by/applied-on :param-changes.param-change/applied-on}
                                                                     (graphql-utils/gql-name->kw order-by))
                                                                order-dir]])
                                group-by (merge {:group-by [(get {:param-changes.group-by/key :param-changes.param-change/key}
                                                                 (graphql-utils/gql-name->kw group-by))]}))
          param-changes-result (paged-query param-changes-query
                                            first
                                            (when after
                                              (parsers/parse-int after)))]

      (if-not (= 0 (:total-count param-changes-result))
        param-changes-result
        (do
          (log/debug "No parameter changes could be retrieved. Querying for initial parameters")
          (let [initial-params-query {:select [[:initial-params.initial-param/key :param-change/key]
                                               [:initial-params.initial-param/db :param-change/db]
                                               [:initial-params.initial-param/value :param-change/value]
                                               [:initial-params.initial-param/set-on :param-change/applied-on]]
                                      :from [:initial-params]
                                      :where [:and [:= key :initial-params.initial-param/key]
                                              [:= db :initial-params.initial-param/db]]}]
            (paged-query initial-params-query
                         first
                         (when after
                           (parsers/parse-int after)))))))))


(defn param-query-resolver [_ {:keys [:db :key] :as args}]
  (log/debug "param-query-resolver" args)
  (try-catch-throw
    (let [sql-query (db/get {:select [[:param-change/db :param/db]
                                      [:param-change/key :param/key]
                                      [:param-change/value :param/value]]
                             :from [:param-changes]
                             :where [:and [:= db :param-changes.param-change/db]
                                     [:= key :param-changes.param-change/key]]
                             :order-by [:param-changes.param-change/applied-on]
                             :limit 1})]
      (log/debug "param-query-resolver" sql-query)
      sql-query)))


(defn params-query-resolver [_ {:keys [:db :keys] :as args}]
  (log/debug "params-query-resolver" args)
  (try-catch-throw
    (let [sql-query (db/all {:select [[:param-change/db :param/db]
                                      [:param-change/key :param/key]
                                      [:param-change/value :param/value]]
                             :from [:param-changes]
                             :where [:and [:= db :param-changes.param-change/db]
                                     [:in :param-changes.param-change/key keys]]
                             :order-by [:param-changes.param-change/applied-on]})]
      (log/debug "params-query-resolver" sql-query)
      sql-query)))


(defn stake-history-resolver [_ {:keys [:reg-entry/address :from]
                                 :as args}]
  (log/info "stake-history-resolver args" args)
  (try-catch-throw
    (let [sql-query (db/all {:select [:*]
                             :from [:stake-history]
                             :order-by [[:stake-history/stake-id :asc]]
                             :where [:= :reg-entry/address address]})]
      sql-query)))


(defn vote->option-resolver [{:keys [:vote/option]}]
  (cond
    (= 1 option) (enum :vote-option/include)
    (= 2 option) (enum :vote-option/exclude)
    :else (enum :vote-option/neither)))

(defn reg-entry->status-resolver [reg-entry]
  (enum (server-db/reg-entry-status (server-utils/now-in-seconds) reg-entry)))


(defn- winning-vote-option [{:keys [:challenge/votes-exclude :challenge/votes-include :stake-history/dnt-total-staked]}]
  (if (>= votes-exclude (+ votes-include (or dnt-total-staked 0)))
    :vote-option/exclude
    :vote-option/include))


(defn vote->reward-resolver [{:keys [:reg-entry/address
                                     :challenge/index
                                     :vote/option
                                     :vote/voter
                                     :vote/amount
                                     :challenge/reward-pool
                                     :challenge/votes-include
                                     :challenge/votes-exclude
                                     :challenge/commit-period-end] :as vote}]
  (log/debug "vote->reward-resolver args" vote)
  (try-catch-throw
    (let [{:keys [:stake-history/staker-dnt-staked] :as query1}
          (server-db/get-stake-history {:reg-entry/address address
                                        :challenge/commit-period-end commit-period-end
                                        :stake-history/staker voter})

          {:keys [:stake-history/dnt-total-staked] :as query2}
          (server-db/get-stake-history {:reg-entry/address address
                                        :challenge/commit-period-end commit-period-end})

          winning-vote-opt (winning-vote-option
                             {:challenge/votes-exclude votes-exclude
                              :challenge/votes-include votes-include
                              :stake-history/dnt-total-staked dnt-total-staked})
          option (vote-option->kw option)]
      (log/debug "vote->reward-resolver query" [query1 query2])
      (cond
        (and (= winning-vote-opt :vote-option/include)
             (= option :vote-option/include))
        (let [amount (+ amount (or staker-dnt-staked 0))
              votes-include (+ votes-include (or dnt-total-staked 0))]
          (/ (* amount reward-pool) votes-include))

        (and (= winning-vote-opt :vote-option/include)
             (not= option :vote-option/include)
             (pos? staker-dnt-staked))
        (let [votes-include (+ votes-include (or dnt-total-staked 0))]
          (/ (* staker-dnt-staked reward-pool) votes-include))

        (and (= winning-vote-opt :vote-option/exclude)
             (= option :vote-option/exclude))
        (/ (* amount reward-pool) votes-exclude)

        :else nil))))


(defn vote->amount-resolver [{:keys [:reg-entry/address
                                     :challenge/commit-period-end
                                     :vote/option
                                     :vote/voter
                                     :vote/amount] :as vote}]
  (log/debug "vote->amount-resolver args" vote)
  (try-catch-throw
    (if (= (vote-option->kw option) :vote-option/include)
      (let [{:keys [:stake-history/staker-dnt-staked]}
            (server-db/get-stake-history {:reg-entry/address address
                                          :challenge/commit-period-end commit-period-end
                                          :stake-history/staker voter})]
        (+ amount (or staker-dnt-staked 0)))
      amount)))


(defn vote->amount-from-staking-resolver [{:keys [:reg-entry/address
                                                  :challenge/commit-period-end
                                                  :vote/option
                                                  :vote/voter
                                                  :vote/amount] :as vote}]
  (log/debug "vote->amount-from-staking-resolver args" vote)
  (try-catch-throw
    (let [{:keys [:stake-history/staker-dnt-staked]}
          (server-db/get-stake-history {:reg-entry/address address
                                        :challenge/commit-period-end commit-period-end
                                        :stake-history/staker voter})]
      staker-dnt-staked)))


(defn challenge->winning-vote-option-resolver [{:keys [:challenge/votes-include
                                                       :challenge/votes-exclude
                                                       :challenge/commit-period-end
                                                       :reg-entry/address]
                                                :as challenge}]
  (log/debug "challenge->winning-vote-option-resolver" challenge)
  (try-catch-throw
    (let [{:keys [:stake-history/dnt-total-staked]}
          (server-db/get-stake-history {:challenge/commit-period-end commit-period-end
                                        :reg-entry/address address})]
      (graphql-utils/kw->gql-name
        (if (>= votes-exclude (+ votes-include (or dnt-total-staked 0)))
          :vote-option/exclude
          :vote-option/include)))))


(defn challenge->votes-include-resolver [{:keys [:challenge/votes-include
                                                 :challenge/commit-period-end
                                                 :reg-entry/address]
                                          :as challenge}]
  (log/debug "challenge->votes-include-resolver" challenge)
  (try-catch-throw
    (let [{:keys [:stake-history/dnt-total-staked]}
          (server-db/get-stake-history {:challenge/commit-period-end commit-period-end
                                        :reg-entry/address address})]
      (+ votes-include (or dnt-total-staked 0)))))


(defn challenge->votes-include-from-staking-resolver [{:keys [:challenge/commit-period-end
                                                              :reg-entry/address]
                                                       :as challenge}]
  (log/debug "challenge->votes-include-from-staking-resolver" challenge)
  (try-catch-throw
    (let [{:keys [:stake-history/dnt-total-staked]}
          (server-db/get-stake-history {:challenge/commit-period-end commit-period-end
                                        :reg-entry/address address})]
      dnt-total-staked)))


(defn challenge->vote [{:keys [:reg-entry/address :challenge/index] :as challenge} {:keys [:voter]}]
  (log/debug "challenge->vote args" {:challenge challenge :voter voter})
  (try-catch-throw
    (let [vote (db/get {:select [:*]
                        :from [:votes]
                        :join [:reg-entries [:= :reg-entries.reg-entry/address :votes.reg-entry/address]]
                        :where [:and
                                [:= :votes.vote/voter voter]
                                [:= :votes.reg-entry/address address]
                                [:= :votes.challenge/index index]]})]
      (merge
        challenge
        (if (seq vote)
          vote
          {:reg-entry/address address
           :challenge/index index
           :vote/voter voter
           :vote/option 0
           :vote/amount 0})))))


(defn district-list->items-resolver [district-list]
  (:items district-list))


(defn param-change-list->items-resolver [param-change-list]
  (:items param-change-list))


(defn reg-entry->challenges [{:keys [:reg-entry/address] :as reg-entry}]
  (log/debug "reg-entry->challenges" {:reg-entry reg-entry})
  (try-catch-throw
    (db/all {:select [:*]
             :from [:challenges]
             :where [:= :challenges.reg-entry/address address]
             :order-by [[:challenges.challenge/index :asc]]})))


(def Query
  {:district district-query-resolver
   :search-districts search-districts-query-resolver
   :param-change param-change-query-resolver
   :search-param-changes search-param-changes-query-resolver
   :param param-query-resolver
   :params params-query-resolver
   :stake-history stake-history-resolver})


(def RegEntry
  {:reg-entry/challenges reg-entry->challenges
   :reg-entry/status reg-entry->status-resolver})


(def District
  (assoc RegEntry
    :district/dnt-staked-for (fn [{:keys [:reg-entry/address]} {:keys [:staker]}]
                               (let [{:keys [:stake-history/staker-dnt-staked]}
                                     (server-db/get-stake-history {:reg-entry/address address
                                                                   :stake-history/staker staker})]
                                 (or staker-dnt-staked 0)))

    :district/balance-of (fn [{:keys [:reg-entry/address]} {:keys [:staker]}]
                           (let [{:keys [:stake-history/staker-voting-token-balance]}
                                 (server-db/get-stake-history {:reg-entry/address address
                                                               :stake-history/staker staker})]
                             (or staker-voting-token-balance 0)))))

(def ParamChange
  RegEntry)

(def Challenge
  {:challenge/vote challenge->vote
   :challenge/winning-vote-option challenge->winning-vote-option-resolver
   :challenge/votes-include challenge->votes-include-resolver
   :challenge/votes-include-from-staking challenge->votes-include-from-staking-resolver})

(def Vote
  {:vote/option vote->option-resolver
   :vote/reward vote->reward-resolver
   :vote/amount vote->amount-resolver
   :vote/amount-from-staking vote->amount-from-staking-resolver})

(def resolvers-map
  {:Query Query
   :Vote Vote
   :Challenge Challenge
   :District District
   :DistrictList {:items district-list->items-resolver}
   :ParamChange ParamChange
   :ParamChangeList {:items param-change-list->items-resolver}})
