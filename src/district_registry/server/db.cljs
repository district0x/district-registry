(ns district-registry.server.db
  (:require
    [district.server.config :refer [config]]
    [district.server.db :as db]
    [district.server.db.column-types :refer [address not-nil default-nil default-zero default-false sha3-hash primary-key]]
    [district.server.db.honeysql-extensions]
    [honeysql.core :as sql]
    [honeysql.helpers :refer [merge-where merge-order-by merge-left-join defhelper]]
    [mount.core :as mount :refer [defstate]]
    [taoensso.timbre :as logging :refer-macros [info warn error]]
    [medley.core :as medley]))

(declare start)
(declare stop)
(defstate ^{:on-reload :noop} district-registry-db
  :start (start (merge (:districtfactory/db @config)
                       (:districtfactory/db (mount/args))))
  :stop (stop))

(def ipfs-hash (sql/call :char (sql/inline 46)))

(def registry-entries-columns
  [[:reg-entry/address address primary-key not-nil]
   [:reg-entry/version :unsigned :integer not-nil]
   [:reg-entry/creator address not-nil]
   [:reg-entry/deposit :unsigned :BIG :INT not-nil]
   [:reg-entry/created-on :unsigned :integer not-nil]
   [:reg-entry/challenge-period-end :unsigned :integer not-nil]
   [:challenge/challenger address default-nil]
   [:challenge/created-on :unsigned :integer default-nil]
   [:challenge/voting-token address default-nil]
   [:challenge/reward-pool :unsigned :BIG :INT default-nil]
   [:challenge/meta-hash ipfs-hash default-nil]
   [:challenge/comment :varchar default-nil]
   [:challenge/commit-period-end :unsigned :integer default-nil]
   [:challenge/reveal-period-end :unsigned :integer default-nil]
   [:challenge/votes-for :BIG :INT default-nil]
   [:challenge/votes-against :BIG :INT default-nil]
   [:challenge/claimed-reward-on :unsigned :integer default-nil]])


(def districts-columns
  [[:reg-entry/address address not-nil]
   [:district/title :varchar not-nil]
   [:district/number :unsigned :integer default-nil]
   [:district/image-hash ipfs-hash not-nil]
   [:district/meta-hash ipfs-hash not-nil]
   [:district/total-supply :unsigned :integer not-nil]
   [:district/total-minted :unsigned :integer not-nil]
   [:district/token-id-start :unsigned :integer not-nil]
   [:district/total-trade-volume :BIG :INT default-nil]
   [:district/first-mint-on :unsigned :integer default-nil]
   [(sql/call :foreign-key :reg-entry/address) (sql/call :references :reg-entries :reg-entry/address)]])


(def votes-columns
  [[:reg-entry/address address not-nil]
   [:vote/voter address not-nil]
   [:vote/option :unsigned :integer not-nil]
   [:vote/amount :unsigned :BIG :INT default-nil]
   [:vote/created-on :unsigned :integer default-nil]
   [:vote/revealed-on :unsigned :integer default-nil]
   [:vote/claimed-reward-on :unsigned :integer default-nil]
   [(sql/call :primary-key :vote/voter :reg-entry/address)]
   [[(sql/call :foreign-key :reg-entry/address) (sql/call :references :reg-entries :reg-entry/address)]]])


(def param-changes-columns
  [[:reg-entry/address address not-nil]
   [:param-change/db address not-nil]
   [:param-change/key :varchar not-nil]
   [:param-change/value :unsigned :integer not-nil]
   [:param-change/applied-on :unsigned :integer default-nil]
   [(sql/call :primary-key :reg-entry/address)]
   [[(sql/call :foreign-key :reg-entry/address) (sql/call :references :reg-entries :reg-entry/address)]]])

(def registry-entry-column-names (map first registry-entries-columns))
(def districts-column-names (map first districts-columns))
(def param-change-column-names (filter keyword? (map first param-changes-columns)))
(def votes-column-names (map first votes-columns))

(defn- index-name [col-name]
  (keyword (namespace col-name) (str (name col-name) "-index")))


(defn start [opts]
  (db/run! {:create-table [:reg-entries]
            :with-columns [registry-entries-columns]})

  (db/run! {:create-table [:districts]
            :with-columns [districts-columns]})

  (db/run! {:create-table [:param-changes]
            :with-columns [param-changes-columns]})

  (db/run! {:create-table [:votes]
            :with-columns [votes-columns]})

  ;; TODO create indexes
  #_(doseq [column (rest registry-entry-column-names)]
      (db/run! {:create-index (index-name column) :on [:offerings column]})))


(defn stop []
  ;; (db/run! {:drop-table [:votes]})
  ;; (db/run! {:drop-table [:param-changes]})
  ;; (db/run! {:drop-table [:districts]})
  ;; (db/run! {:drop-table [:tags]})

  )

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

(def insert-registry-entry! (create-insert-fn :reg-entries registry-entry-column-names))
(def update-registry-entry! (create-update-fn :reg-entries registry-entry-column-names :reg-entry/address))
(def get-registry-entry (create-get-fn :reg-entries :reg-entry/address))

(def insert-district! (create-insert-fn :districts districts-column-names))
(def update-district! (create-update-fn :districts districts-column-names :reg-entry/address))

(def insert-param-change! (create-insert-fn :param-changes param-change-column-names))
(def update-param-change! (create-update-fn :param-changes param-change-column-names :reg-entry/address))

(def insert-vote! (create-insert-fn :votes votes-column-names))
(def update-vote! (create-update-fn :votes votes-column-names [:reg-entry/address :vote/voter]))


