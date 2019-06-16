(ns district-registry.server.db
  (:require
   [district.server.config :refer [config]]
   [district.server.db :as db]
   [district.server.db.column-types :refer [address not-nil default-nil default-zero default-false sha3-hash primary-key]]
   [district.server.db.honeysql-extensions]
   [honeysql.core :as sql]
   [honeysql.helpers :refer [merge-where merge-order-by merge-left-join defhelper]]
   [medley.core :as medley]
   [mount.core :as mount :refer [defstate]]
   [taoensso.timbre :as logging :refer-macros [info warn error]]))

(declare start)
(declare stop)
(defstate ^{:on-reload :noop} district-registry-db
  :start (start (merge
                  (:db @config)
                  (:db (mount/args))))
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
   [:district/logo-image-hash :string]
   [:district/background-image-hash :string]
   [:district/dnt-weight :unsigned :integer not-nil]
   [:district/dnt-staked :unsigned :BIG :INT not-nil]
   [:district/total-supply :unsigned :BIG :INT not-nil]
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
   [:challenge/votes-total :BIG :INT default-zero]
   [:challenge/claimed-reward-on :unsigned :integer default-nil]
   [(sql/call :primary-key :challenge/index :reg-entry/address)]
   [(sql/call :foreign-key :reg-entry/address) (sql/call :references :reg-entries :reg-entry/address)]])

(def votes-columns
  [[:reg-entry/address address not-nil]
   [:challenge/index :unsigned :integer not-nil]
   [:vote/voter address not-nil]
   [:vote/option :unsigned :integer not-nil]
   [:vote/amount :unsigned :BIG :INT default-nil]
   [:vote/created-on :unsigned :integer default-nil]
   [:vote/revealed-on :unsigned :integer default-nil]
   [:vote/claimed-reward-on :unsigned :integer default-nil]
   [:vote/reclaimed-amount-on :unsigned :integer default-nil]
   [(sql/call :primary-key :vote/voter :challenge/index :reg-entry/address)]
   [(sql/call :foreign-key :reg-entry/address) (sql/call :references :reg-entries :reg-entry/address)]])

(def stakes-columns
  [[:reg-entry/address address not-nil]
   [:stake/staker address not-nil]
   [:stake/dnt :unsigned :integer not-nil]
   [:stake/tokens :unsigned :integer not-nil]
   [(sql/call :primary-key :reg-entry/address :stake/staker)]
   [(sql/call :foreign-key :reg-entry/address) (sql/call :references :districts :reg-entry/address)]])

(def registry-entry-column-names (map first registry-entries-columns))
(def districts-column-names (map first districts-columns))
(def initial-params-column-names (filter keyword? (map first initial-params-columns)))
(def param-change-column-names (filter keyword? (map first param-changes-columns)))
(def votes-column-names (map first votes-columns))
(def challenges-column-names (map first challenges-columns))
(def stakes-column-names (map first stakes-columns))

(defn- index-name [col-name]
  (keyword (namespace col-name) (str (name col-name) "-index")))

(defn start [opts]

  (db/run! {:create-table [:reg-entries]
            :with-columns [registry-entries-columns]})

  (db/run! {:create-table [:districts]
            :with-columns [districts-columns]})

  (db/run! {:create-table [:challenges]
            :with-columns [challenges-columns]})

  (db/run! {:create-table [:initial-params]
            :with-columns [initial-params-columns]})

  (db/run! {:create-table [:param-changes]
            :with-columns [param-changes-columns]})

  (db/run! {:create-table [:votes]
            :with-columns [votes-columns]})

  (db/run! {:create-table [:stakes]
            :with-columns [stakes-columns]})

  ::started)

(defn stop []
  (db/run! {:drop-table [:stakes]})
  (db/run! {:drop-table [:votes]})
  (db/run! {:drop-table [:challenges]})
  (db/run! {:drop-table [:param-changes]})
  (db/run! {:drop-table [:initial-params]})
  (db/run! {:drop-table [:districts]})
  (db/run! {:drop-table [:reg-entries]}))

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

(def insert-initial-param! (create-insert-fn :initial-params initial-params-column-names))

(def insert-registry-entry! (create-insert-fn :reg-entries registry-entry-column-names))
(def update-registry-entry! (create-update-fn :reg-entries registry-entry-column-names :reg-entry/address))
(def get-registry-entry (create-get-fn :reg-entries :reg-entry/address))

(def insert-district! (create-insert-fn :districts districts-column-names))
(def update-district! (create-update-fn :districts districts-column-names :reg-entry/address))

(def insert-param-change! (create-insert-fn :param-changes param-change-column-names))
(def update-param-change! (create-update-fn :param-changes param-change-column-names :reg-entry/address))
(def insert-or-replace-param-change! (create-insert-fn :param-changes param-change-column-names {:insert-or-replace? true}))

(def get-challenge (create-get-fn :challenges [:reg-entry/address :challenge/index]))
(def insert-challenge! (create-insert-fn :challenges challenges-column-names))
(def update-challenge! (create-update-fn :challenges challenges-column-names [:reg-entry/address :challenge/index]))

(def insert-vote! (create-insert-fn :votes votes-column-names))
(def update-vote! (create-update-fn :votes votes-column-names [:reg-entry/address :challenge/index :vote/voter]))
(def get-vote (create-get-fn :votes [:reg-entry/address :challenge/index :vote/voter]))

(def insert-stake! (create-insert-fn :stakes stakes-column-names))
(def insert-or-replace-stake! (create-insert-fn :stakes stakes-column-names {:insert-or-replace? true}))
(def update-stake! (create-update-fn :stakes stakes-column-names [:reg-entry/address :stake/staker]))
