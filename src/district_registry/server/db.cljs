(ns district-registry.server.db
  (:require [district.server.config :refer [config]]
            [district.server.db :as db]
            [district.server.db.column-types :refer [address not-nil default-nil default-zero default-false sha3-hash primary-key]]
            [district.server.db.honeysql-extensions]
            [honeysql-postgres.helpers :as psqlh]
            [honeysql.core :as sql]
            [honeysql.helpers :as sqlh :refer [merge-where merge-order-by merge-left-join defhelper]]
            [medley.core :as medley]
            [mount.core :as mount :refer [defstate]]
            [taoensso.timbre :as log :refer-macros [info warn error]]))

(declare start)
(declare stop)

(defstate ^{:on-reload :noop} district-registry-db
  :start (start (merge
                  (:registry/db @config)
                  (:registry/db (mount/args))))
  :stop (stop))

(def ipfs-hash (sql/call :char (sql/inline 46)))

(def registry-entries-columns
  [[:reg-entry/address address primary-key not-nil]
   [:reg-entry/version :unsigned :integer not-nil]
   [:reg-entry/creator address not-nil]
   [:reg-entry/deposit :unsigned :BIG :INT not-nil]
   [:reg-entry/created-on :unsigned :integer not-nil]
   [:reg-entry/challenge-period-end :BIG :INT not-nil]
   ;; Make joining reg entries with their latest challenge a lot easier
   [:reg-entry/current-challenge-index :unsigned :integer]])

(def districts-columns
  [[:reg-entry/address address not-nil]
   [:district/meta-hash ipfs-hash]
   [:district/name :string]
   [:district/description :string]
   [:district/url :string]
   [:district/github-url :string]
   [:district/facebook-url :string]
   [:district/twitter-url :string]
   [:district/logo-image-hash :string]
   [:district/background-image-hash :string]
   [:district/dnt-staked :unsigned :BIG :INT not-nil]
   [:district/total-supply :unsigned :BIG :INT not-nil]
   [:district/ens-name :string]
   [:district/stake-bank :string not-nil]
   [(sql/call :primary-key :reg-entry/address)]
   [(sql/call :foreign-key :reg-entry/address) (sql/call :references :reg-entries :reg-entry/address)]])

(def initial-params-columns
  [[:initial-param/key :varchar not-nil]
   [:initial-param/db address not-nil]
   [:initial-param/value :unsigned :integer not-nil]
   [:initial-param/set-on :unsigned :integer default-nil]
   [(sql/call :primary-key :initial-param/key :initial-param/db)]])

(def param-changes-columns
  [[:reg-entry/address address not-nil]
   [:param-change/db address not-nil]
   [:param-change/key :varchar not-nil]
   [:param-change/value :unsigned :integer not-nil]
   [:param-change/initial-value :unsigned :integer not-nil]
   [:param-change/applied-on :unsigned :integer default-nil]
   [(sql/call :primary-key :reg-entry/address)]
   [[(sql/call :foreign-key :reg-entry/address) (sql/call :references :reg-entries :reg-entry/address)]]
   #_[[(sql/call :foreign-key :param-change/initial-value) (sql/call :references :initial-params :initial-param/value)]]])

(def challenges-columns
  [[:reg-entry/address address not-nil]
   [:challenge/index :unsigned :integer not-nil]
   [:challenge/challenger address default-nil]
   [:challenge/created-on :unsigned :integer default-nil]
   [:challenge/reward-pool :unsigned :BIG :INT default-nil]
   [:challenge/meta-hash ipfs-hash default-nil]
   [:challenge/comment :string]
   [:challenge/commit-period-end :unsigned :integer default-nil]
   [:challenge/reveal-period-end :unsigned :integer default-nil]
   [:challenge/votes-include :BIG :INT default-zero]
   [:challenge/votes-exclude :BIG :INT default-zero]
   [:challenge/challenger-reward-claimed-on :unsigned :integer default-nil]
   [(sql/call :primary-key :challenge/index :reg-entry/address)]
   [(sql/call :foreign-key :reg-entry/address) (sql/call :references :reg-entries :reg-entry/address)]])

(def votes-columns
  [[:reg-entry/address address not-nil]
   [:challenge/index :unsigned :integer not-nil]
   [:vote/voter address not-nil]
   [:vote/option :unsigned :integer not-nil]
   [:vote/amount :unsigned :BIG :INT default-zero]
   [:vote/created-on :unsigned :integer default-nil]
   [:vote/revealed-on :unsigned :integer default-nil]
   [:vote/claimed-reward-on :unsigned :integer default-nil]
   [:vote/reclaimed-votes-on :unsigned :integer default-nil]
   [(sql/call :primary-key :vote/voter :challenge/index :reg-entry/address)]
   [(sql/call :foreign-key :reg-entry/address) (sql/call :references :reg-entries :reg-entry/address)]])

(def stake-balances-columns
  [[:reg-entry/address address not-nil]
   [:stake-balance/staker address not-nil]
   [:stake-balance/dnt :unsigned :integer not-nil]
   [:stake-balance/voting-token :unsigned :integer not-nil]
   [(sql/call :primary-key :reg-entry/address :stake-balance/staker)]
   [(sql/call :foreign-key :reg-entry/address) (sql/call :references :districts :reg-entry/address)]])


(def stake-history-columns
  [[:reg-entry/address address not-nil]
   [:stake-history/stake-id :unsigned :integer not-nil]
   [:stake-history/staker address not-nil]
   [:stake-history/staked-on :unsigned :integer not-nil]
   [:stake-history/dnt-total-staked :BIG :INT not-nil]
   [:stake-history/voting-token-total-supply :BIG :INT not-nil]
   [:stake-history/staker-dnt-staked :BIG :INT not-nil]
   [:stake-history/staker-voting-token-balance :BIG :INT not-nil]
   [:stake-history/staked-amount :BIG :INT not-nil]
   [:stake-history/unstake? :unsigned :integer not-nil]
   [(sql/call :primary-key :reg-entry/address :stake-history/stake-id)]
   [(sql/call :foreign-key :reg-entry/address) (sql/call :references :districts :reg-entry/address)]])

(def events-columns
  [[:event/contract-key :varchar not-nil]
   [:event/event-name :varchar not-nil]
   [:event/last-log-index :integer not-nil]
   [:event/last-block-number :integer not-nil]
   [:event/count :integer not-nil]
   [(sql/call :primary-key :event/contract-key :event/event-name)]])

;; The Backroom tables
(def council-seats-columns
  [[:council-seat/index :unsigned :integer not-nil]
   [:council-seat/mind-id sha3-hash not-nil]
   [:council-seat/name :varchar not-nil]
   [:council-seat/philosophy-hash ipfs-hash default-nil]
   [:council-seat/is-immutable :boolean not-nil]
   [:council-seat/is-active :boolean default-nil]
   [:council-seat/created-on :unsigned :integer not-nil]
   [:council-seat/total-staked :BIG :INT default-zero]
   [(sql/call :primary-key :council-seat/index)]])

(def council-stakes-columns
  [[:council-seat/index :unsigned :integer not-nil]
   [:council-stake/staker address not-nil]
   [:council-stake/amount :BIG :INT not-nil]
   [:council-stake/staked-on :unsigned :integer not-nil]
   [:council-stake/block-number :unsigned :integer not-nil]
   [:council-stake/unstake? :boolean not-nil]
   [(sql/call :foreign-key :council-seat/index) (sql/call :references :council-seats :council-seat/index)]])

(def council-stake-balances-columns
  [[:council-seat/index :unsigned :integer not-nil]
   [:council-stake-balance/staker address not-nil]
   [:council-stake-balance/balance :BIG :INT not-nil]
   [(sql/call :primary-key :council-seat/index :council-stake-balance/staker)]
   [(sql/call :foreign-key :council-seat/index) (sql/call :references :council-seats :council-seat/index)]])

(def ari-proposals-columns
  [[:ari-proposal/id :unsigned :integer primary-key not-nil]
   [:ari-proposal/proposal-type :unsigned :integer not-nil]
   [:ari-proposal/reasoning-hash ipfs-hash not-nil]
   [:ari-proposal/council-weights-hash sha3-hash not-nil]
   [:ari-proposal/snapshot-block :unsigned :integer not-nil]
   [:ari-proposal/veto-deadline :unsigned :integer not-nil]
   [:ari-proposal/amount :BIG :INT default-zero]
   [:ari-proposal/recipient address default-nil]
   [:ari-proposal/status :unsigned :integer not-nil]
   [:ari-proposal/veto-stake-against :BIG :INT default-zero]
   [:ari-proposal/proposer address not-nil]
   [:ari-proposal/created-on :unsigned :integer not-nil]
   [:ari-proposal/executed-on :unsigned :integer default-nil]])

(def bounties-columns
  [[:bounty/id :unsigned :integer primary-key not-nil]
   [:bounty/metadata-hash ipfs-hash not-nil]
   [:bounty/reward :BIG :INT not-nil]
   [:bounty/token address default-nil]
   [:bounty/deadline :unsigned :integer not-nil]
   [:bounty/assignee address default-nil]
   [:bounty/status :unsigned :integer not-nil]
   [:bounty/created-on :unsigned :integer not-nil]
   [:bounty/completed-on :unsigned :integer default-nil]
   [:bounty/claimed-on :unsigned :integer default-nil]])

(def grants-columns
  [[:grant/id :unsigned :integer primary-key not-nil]
   [:grant/recipient address not-nil]
   [:grant/total-amount :BIG :INT not-nil]
   [:grant/disbursed :BIG :INT default-zero]
   [:grant/vesting-start :unsigned :integer not-nil]
   [:grant/vesting-duration :unsigned :integer not-nil]
   [:grant/purpose-hash ipfs-hash not-nil]
   [:grant/token address default-nil]
   [:grant/created-on :unsigned :integer not-nil]])

(def registry-entry-column-names (map first registry-entries-columns))
(def districts-column-names (map first districts-columns))
(def initial-params-column-names (filter keyword? (map first initial-params-columns)))
(def param-change-column-names (filter keyword? (map first param-changes-columns)))
(def votes-column-names (map first votes-columns))
(def challenges-column-names (map first challenges-columns))
(def stake-balances-column-names (map first stake-balances-columns))
(def stake-history-column-names (map first stake-history-columns))
(def events-column-names (filter keyword? (map first events-columns)))
(def council-seats-column-names (filter keyword? (map first council-seats-columns)))
(def council-stakes-column-names (filter keyword? (map first council-stakes-columns)))
(def council-stake-balances-column-names (filter keyword? (map first council-stake-balances-columns)))
(def ari-proposals-column-names (filter keyword? (map first ari-proposals-columns)))
(def bounties-column-names (filter keyword? (map first bounties-columns)))
(def grants-column-names (filter keyword? (map first grants-columns)))

(defn- index-name [col-name]
  (keyword (namespace col-name) (str (name col-name) "-index")))

(defn clean-db []
  (let [tables [:reg-entries
                :districts
                :challenges
                :initial-params
                :param-changes
                :votes
                :stake-balances
                :stake-history
                :events
                :council-seats
                :council-stakes
                :council-stake-balances
                :ari-proposals
                :bounties
                :grants]
        drop-table-if-exists (fn [t]
                               (psqlh/drop-table :if-exists t))]
    (doall
      (map (fn [t]
             (log/debug (str "Dropping table " t))
             (db/run! (drop-table-if-exists t)))
           tables))))


(defn create-indexes []
  (doseq [column [:reg-entry/created-on :reg-entry/challenge-period-end :reg-entry/current-challenge-index]]
    (db/run! {:create-index (index-name column) :on [:reg-entries column]}))

  (doseq [column [:district/dnt-staked :district/total-supply]]
    (db/run! {:create-index (index-name column) :on [:districts column]}))

  (doseq [column [:challenge/created-on :challenge/commit-period-end]]
    (db/run! {:create-index (index-name column) :on [:challenges column]}))

  (doseq [column [:param-change/key :param-change/db]]
    (db/run! {:create-index (index-name column) :on [:param-changes column]}))

  (doseq [column [:stake-balance/staker]]
    (db/run! {:create-index (index-name column) :on [:stake-balances column]}))

  ;; Backroom indexes
  (doseq [column [:council-seat/created-on :council-seat/total-staked]]
    (db/run! {:create-index (index-name column) :on [:council-seats column]}))

  (doseq [column [:council-stake-balance/staker]]
    (db/run! {:create-index (index-name column) :on [:council-stake-balances column]}))

  (doseq [column [:ari-proposal/created-on :ari-proposal/status :ari-proposal/veto-deadline]]
    (db/run! {:create-index (index-name column) :on [:ari-proposals column]}))

  (doseq [column [:bounty/status :bounty/deadline]]
    (db/run! {:create-index (index-name column) :on [:bounties column]}))

  (doseq [column [:grant/recipient]]
    (db/run! {:create-index (index-name column) :on [:grants column]})))


(defn start [{:keys [:resync?] :as opts}]
  (when resync?
    (log/info "Database module called with a resync flag.")
    (clean-db))

  (db/run! (-> (psqlh/create-table :reg-entries :if-not-exists)
             (psqlh/with-columns registry-entries-columns)))

  (db/run! (-> (psqlh/create-table :districts :if-not-exists)
             (psqlh/with-columns districts-columns)))

  (db/run! (-> (psqlh/create-table :challenges :if-not-exists)
             (psqlh/with-columns challenges-columns)))

  (db/run! (-> (psqlh/create-table :initial-params :if-not-exists)
             (psqlh/with-columns initial-params-columns)))

  (db/run! (-> (psqlh/create-table :param-changes :if-not-exists)
             (psqlh/with-columns param-changes-columns)))

  (db/run! (-> (psqlh/create-table [:votes] :if-not-exists)
             (psqlh/with-columns votes-columns)))

  (db/run! (-> (psqlh/create-table [:stake-balances] :if-not-exists)
             (psqlh/with-columns stake-balances-columns)))

  (db/run! (-> (psqlh/create-table [:stake-history] :if-not-exists)
             (psqlh/with-columns stake-history-columns)))

  (db/run! (-> (psqlh/create-table :events :if-not-exists)
               (psqlh/with-columns events-columns)))

  ;; Create Backroom tables
  (db/run! (-> (psqlh/create-table :council-seats :if-not-exists)
               (psqlh/with-columns council-seats-columns)))

  (db/run! (-> (psqlh/create-table :council-stakes :if-not-exists)
               (psqlh/with-columns council-stakes-columns)))

  (db/run! (-> (psqlh/create-table :council-stake-balances :if-not-exists)
               (psqlh/with-columns council-stake-balances-columns)))

  (db/run! (-> (psqlh/create-table :ari-proposals :if-not-exists)
               (psqlh/with-columns ari-proposals-columns)))

  (db/run! (-> (psqlh/create-table :bounties :if-not-exists)
               (psqlh/with-columns bounties-columns)))

  (db/run! (-> (psqlh/create-table :grants :if-not-exists)
               (psqlh/with-columns grants-columns)))

  (when resync?
    (create-indexes))

  ::started)

(defn stop []
  :stopped)

(defn create-insert-fn [table-name column-names & [{:keys [:insert-or-replace?]}]]
  (fn [item]
    (let [item (select-keys item column-names)]
      (db/run! {(if insert-or-replace? :insert-or-replace-into :insert-into) table-name
                :columns (keys item)
                :values [(vals item)]}))))

(defn create-update-fn [table-name column-names id-keys]
  (fn [item]
    (let [item (select-keys item column-names)
          id-keys (if (sequential? id-keys) id-keys [id-keys])]
      (assert
        (every? #(contains? item %) id-keys)
        (str
          "Required keys: " (prn-str id-keys)
          " Received keys: " (prn-str (keys item))))
      (db/run! {:update table-name
                :set item
                :where (concat
                         [:and]
                         (for [id-key id-keys]
                           [:= id-key (get item id-key)]))}))))

(defn create-get-fn [table-name id-keys]
  (let [id-keys (if (sequential? id-keys) id-keys [id-keys])]
    (fn [item fields]
      (cond-> (db/get {:select (if (sequential? fields) fields [fields])
                       :from [table-name]
                       :where (concat
                                [:and]
                                (for [id-key id-keys]
                                  [:= id-key (get item id-key)]))})
        (keyword? fields) fields))))

(defn get-initial-param [key db]
  (db/get {:select [:*]
           :from [:initial-params]
           :where [:and [:= :initial-param/key key]
                   [:= :initial-param/db db]]}))

(defn initial-param-exists? [key db]
  (boolean (seq (get-initial-param key db))))

(def insert-initial-param! (create-insert-fn :initial-params initial-params-column-names {:insert-or-replace? true}))

(def insert-registry-entry! (create-insert-fn :reg-entries registry-entry-column-names))
(def update-registry-entry! (create-update-fn :reg-entries registry-entry-column-names :reg-entry/address))
(def get-registry-entry (create-get-fn :reg-entries :reg-entry/address))

(def insert-district! (create-insert-fn :districts districts-column-names))
(def update-district! (create-update-fn :districts districts-column-names :reg-entry/address))
(def get-district (create-get-fn :districts :reg-entry/address))

(def insert-param-change! (create-insert-fn :param-changes param-change-column-names))
(def update-param-change! (create-update-fn :param-changes param-change-column-names :reg-entry/address))
(def insert-or-replace-param-change! (create-insert-fn :param-changes param-change-column-names {:insert-or-replace? true}))

(def get-challenge (create-get-fn :challenges [:reg-entry/address :challenge/index]))
(def insert-challenge! (create-insert-fn :challenges challenges-column-names))
(def update-challenge! (create-update-fn :challenges challenges-column-names [:reg-entry/address :challenge/index]))

(def insert-vote! (create-insert-fn :votes votes-column-names))
(def update-vote! (create-update-fn :votes votes-column-names [:reg-entry/address :challenge/index :vote/voter]))
(def get-vote (create-get-fn :votes [:reg-entry/address :challenge/index :vote/voter]))
(defn vote-exists? [& args]
  (boolean (seq (apply get-vote args))))

(def insert-stake-balance! (create-insert-fn :stake-balances stake-balances-column-names))
(def insert-or-replace-stake-balance! (create-insert-fn :stake-balances stake-balances-column-names {:insert-or-replace? true}))
(def update-stake-balance! (create-update-fn :stake-balances stake-balances-column-names [:reg-entry/address :stake-balance/staker]))

(def insert-stake-history! (create-insert-fn :stake-history stake-history-column-names))

;; Backroom database functions
(def insert-council-seat! (create-insert-fn :council-seats council-seats-column-names))
(def update-council-seat! (create-update-fn :council-seats council-seats-column-names :council-seat/index))
(def get-council-seat (create-get-fn :council-seats :council-seat/index))

(def insert-council-stake! (create-insert-fn :council-stakes council-stakes-column-names))

(def insert-council-stake-balance! (create-insert-fn :council-stake-balances council-stake-balances-column-names))
(def insert-or-replace-council-stake-balance! (create-insert-fn :council-stake-balances council-stake-balances-column-names {:insert-or-replace? true}))
(def update-council-stake-balance! (create-update-fn :council-stake-balances council-stake-balances-column-names [:council-seat/index :council-stake-balance/staker]))
(def get-council-stake-balance (create-get-fn :council-stake-balances [:council-seat/index :council-stake-balance/staker]))

;; Upsert function for council stake balances
(defn upsert-council-stake-balance! [args]
  (db/run! {:insert-into :council-stake-balances
            :values [(select-keys args council-stake-balances-column-names)]
            :upsert {:on-conflict [:council-seat/index :council-stake-balance/staker]
                     :do-update-set [:council-stake-balance/balance]}}))

(def insert-ari-proposal! (create-insert-fn :ari-proposals ari-proposals-column-names))
(def update-ari-proposal! (create-update-fn :ari-proposals ari-proposals-column-names :ari-proposal/id))
(def get-ari-proposal (create-get-fn :ari-proposals :ari-proposal/id))

(def insert-bounty! (create-insert-fn :bounties bounties-column-names))
(def update-bounty! (create-update-fn :bounties bounties-column-names :bounty/id))
(def get-bounty (create-get-fn :bounties :bounty/id))

(def insert-grant! (create-insert-fn :grants grants-column-names))
(def update-grant! (create-update-fn :grants grants-column-names :grant/id))
(def get-grant (create-get-fn :grants :grant/id))

(defn total-stake
  "Returns the total stake from a collection of stakes"
  [stakes]
  (->> stakes
       (map (fn [{:keys [:stake-history/unstake? :stake-history/staked-amount]}]
              (* staked-amount (if unstake? -1 1))))
       (reduce +)))

(defn get-stakers
  "Returns all the stakers addresses for a `reg-entry-address` which currenlty hold a positive stake"
  [reg-entry-address]
  (->> (db/all {:select [:*]
                :from [:stake-history]
                :where [:and
                        [:= :reg-entry/address reg-entry-address]]})
       (map #(update % :stake-history/unstake? = 1))
       (group-by :stake-history/staker)
       (keep (fn [[staker stakes]] (when (pos? (total-stake stakes))
                                     staker)))))

(defn get-stake-history [{:keys [:challenge/commit-period-end :stake-history/staker :reg-entry/address]} & [fields]]
  (let [query (cond-> {:select (or fields [:*])
                       :from [:stake-history]
                       :order-by [[:stake-history/stake-id :desc]]
                       :where [:= :reg-entry/address address]
                       :limit 1}
                staker (sqlh/merge-where [:= :stake-history/staker staker])
                commit-period-end (sqlh/merge-where [:<= :stake-history/staked-on commit-period-end]))]
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

(def get-last-event (create-get-fn :events [:event/contract-key :event/event-name]))

(defn upsert-event! [args]
  (db/run! {:insert-into :events
            :values [(select-keys args events-column-names)]
            :upsert {:on-conflict [:event/event-name :event/contract-key]
                     :do-update-set [:event/last-log-index :event/last-block-number :event/count]}}))
