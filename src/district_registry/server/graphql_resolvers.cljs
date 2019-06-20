(ns district-registry.server.graphql-resolvers
  (:require
    [bignumber.core :as bn]
    [cljs-web3.eth :as web3-eth]
    [cljs.nodejs :as nodejs]
    [clojure.pprint :refer [pprint]]
    [district-registry.server.utils :as server-utils]
    [district.graphql-utils :as graphql-utils]
    [district.parsers :as parsers]
    [district.server.config :refer [config]]
    [district.server.db :as db]
    [district.server.smart-contracts :as smart-contracts]
    [district.server.web3 :as web3]
    [district.shared.error-handling :refer [try-catch-throw]]
    [district.web3-utils :as web3-utils]
    [honeysql.core :as sql]
    [honeysql.helpers :as sqlh]
    [taoensso.timbre :as log]))

(def whitelisted-config-keys [:ipfs])

(defn config-query-resolver []
  (log/debug "config-query-resolver")
  (try-catch-throw
    (select-keys @config whitelisted-config-keys)))

(def enum graphql-utils/kw->gql-name)

(def graphql-fields (nodejs/require "graphql-fields"))

(defn- query-fields
  "Returns the first order fields"
  [document & [path]]
  (->> (-> document
         graphql-fields
         (js->clj))
    (#(if-let [p (name path)]
        (get-in % [p])
        %))
    keys
    (map graphql-utils/gql-name->kw)
    set))


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


(defn get-stake-history [{:keys [:challenge/commit-period-end :stake-history/staker :reg-entry/address]}]
  (let [query (cond-> {:select [:*]
                       :from [:stake-history]
                       :order-by [[:stake-history/staked-on :desc]]
                       :where [:= :reg-entry/address address]
                       :limit 1}
                staker (sqlh/merge-where [:= :stake-history/staker staker])
                commit-period-end (sqlh/merge-where [:< :stake-history/staked-on commit-period-end]))]
    (db/get query)))


(defn reg-entry-status [now {:keys [:reg-entry/address :reg-entry/challenge-period-end]}]
  (let [{:keys [:challenge/index
                :challenge/commit-period-end
                :challenge/reveal-period-end
                :challenge/votes-include
                :challenge/votes-exclude]}
        (db/get {:select [:*]
                 :from [:challenges]
                 :where [:= :challenges.reg-entry/address address]
                 :order-by [[:challenges.challenge/index :desc]]})

        {:keys [:stake-history/dnt-total-staked]}
        (get-stake-history {:challenge/commit-period-end commit-period-end
                            :reg-entry/address address})]
    (cond
      (and (< now challenge-period-end) (not index)) :reg-entry.status/challenge-period
      (< now commit-period-end) :reg-entry.status/commit-period
      (< now reveal-period-end) :reg-entry.status/reveal-period
      (or
        (< votes-exclude (+ votes-include (or dnt-total-staked 0)))
        (< challenge-period-end now)) :reg-entry.status/whitelisted
      :else :reg-entry.status/blacklisted)))


(defn reg-entry-status-sql-clause [now]
  (sql/call                                                 ;; TODO: can we remove aliases here?
    :case
    [:and
     [:< now :re.reg-entry/challenge-period-end]
     [:= :c.challenge/index nil]] (enum :reg-entry.status/challenge-period)
    [:< now :c.challenge/commit-period-end] (enum :reg-entry.status/commit-period)
    [:< now :c.challenge/reveal-period-end] (enum :reg-entry.status/reveal-period)
    [:or
     [:< :c.challenge/votes-exclude :c.challenge/votes-include]
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
  (log/info "search-param-changes args" args)
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


(defn vote->option-resolver [{:keys [:vote/option] :as vote}]
  (cond
    (= 1 option) (enum :vote-option/include)
    (= 2 option) (enum :vote-option/exclude)
    :else (enum :vote-option/neither)))


(defn reg-entry->status-resolver [reg-entry]
  (enum (reg-entry-status (server-utils/now-in-seconds) reg-entry)))

(defn vote->reward-resolver [{:keys [:reg-entry/address
                                     :challenge/commit-period-end
                                     :challenge/index
                                     :vote/option
                                     :vote/voter
                                     :vote/amount] :as vote}]
  (log/debug "vote->reward-resolver args" vote)
  (try-catch-throw
    (let [now (server-utils/now-in-seconds)
          status (reg-entry-status now vote)

          {:keys [:stake-history/staker-dnt-staked]}
          (get-stake-history {:reg-entry/address address
                              :challenge/commit-period-end commit-period-end
                              :stake-history/staker voter})

          {:keys [:challenge/reward-pool] :as sql-query}
          (db/get
            {:select [:challenge/reward-pool]
             :from [:challenges]
             :where [:and
                     [:= address :challenges.reg-entry/address]
                     [:= index :challenges.challenge/index]]})]
      (log/debug "vote->reward-resolver query" sql-query)
      (cond
        (and (= :reg-entry.status/whitelisted status)
             (= option 1))
        (web3-utils/eth->wei (/ reward-pool (+ amount (or staker-dnt-staked 0))))

        (and (= :reg-entry.status/blacklisted status)
             (= option 2))
        (web3-utils/eth->wei (/ reward-pool amount))

        :else nil))))


(defn vote->amount-resolver [{:keys [:reg-entry/address
                                     :challenge/commit-period-end
                                     :vote/option
                                     :vote/voter
                                     :vote/amount] :as vote}]
  (log/debug "vote->amount-resolver args" vote)
  (try-catch-throw
    (if (= option 1)
      (let [{:keys [:stake-history/staker-dnt-staked]}
            (get-stake-history {:reg-entry/address address
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
          (get-stake-history {:reg-entry/address address
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
          (get-stake-history {:challenge/commit-period-end commit-period-end
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
          (get-stake-history {:challenge/commit-period-end commit-period-end
                              :reg-entry/address address})]
      (+ votes-include (or dnt-total-staked 0)))))


(defn challenge->vote [{:keys [:reg-entry/address :challenge/index] :as challenge} {:keys [:voter]}]
  (log/debug "challenge->vote args" {:challenge challenge :voter voter})
  (try-catch-throw
    (db/get {:select [:*]
             :from [:votes]
             :join [:reg-entries [:= :reg-entries.reg-entry/address :votes.reg-entry/address]]
             :where [:and
                     [:= :votes.vote/voter voter]
                     [:= :votes.reg-entry/address address]
                     [:= :votes.challenge/index index]]})))


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
  {:config config-query-resolver
   :district district-query-resolver
   :search-districts search-districts-query-resolver
   :param-change param-change-query-resolver
   :search-param-changes search-param-changes-query-resolver
   :param param-query-resolver
   :params params-query-resolver})


(def RegEntry
  {:reg-entry/challenges reg-entry->challenges
   :reg-entry/status reg-entry->status-resolver})


(def District
  (assoc RegEntry
    :district/dnt-staked-for (fn [{:keys [:reg-entry/address]} {:keys [:staker]}]
                               (let [{:keys [:stake-history/staker-dnt-staked]}
                                     (get-stake-history {:reg-entry/address address
                                                         :stake-history/staker staker})]
                                 (or staker-dnt-staked 0)))

    :district/balance-of (fn [{:keys [:reg-entry/address]} {:keys [:staker]}]
                           (let [{:keys [:stake-history/staker-voting-token-balance]}
                                 (get-stake-history {:reg-entry/address address
                                                     :stake-history/staker staker})]
                             (or staker-voting-token-balance 0)))))

(def ParamChange
  RegEntry)

(def Challenge
  {:challenge/vote challenge->vote
   :challenge/winning-vote-option challenge->winning-vote-option-resolver
   :challenge/votes-include challenge->votes-include-resolver})

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
