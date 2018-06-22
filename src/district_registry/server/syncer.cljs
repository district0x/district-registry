(ns district-registry.server.syncer
  (:require
   [bignumber.core :as bn]
   [camel-snake-kebab.core :as cs :include-macros true]
   [cljs-web3.core :as web3]
   [cljs-web3.eth :as web3-eth]
   [district.server.config :refer [config]]
   [district.server.smart-contracts :refer [replay-past-events]]
   [district.server.web3 :refer [web3]]
   [district.web3-utils :as web3-utils]
   [district-registry.server.contract.district :as district]
   [district-registry.server.contract.param-change :as param-change]
   [district-registry.server.contract.registry :as registry]
   [district-registry.server.contract.registry-entry :as registry-entry]
   [district-registry.server.db :as db]
   [district-registry.server.deployer]
   [district-registry.server.generator]
   [mount.core :as mount :refer [defstate]]
   [taoensso.timbre :refer-macros [info warn error]]))

(declare start)
(declare stop)
(defstate ^{:on-reload :noop} syncer
  :start (start (merge (:syncer @config)
                       (:syncer (mount/args))))
  :stop (stop syncer))

(def info-text "smart-contract event")
(def error-text "smart-contract event error")

(defn on-constructed [{:keys [:registry-entry :timestamp] :as args} _ type]
  (info info-text {:args args} ::on-constructed)
  (try
    (db/insert-registry-entry! (merge (registry-entry/load-registry-entry registry-entry)
                                 (registry-entry/load-registry-entry-challenge registry-entry)
                                 {:reg-entry/created-on timestamp}))
    (if (= type :district)
      (db/insert-district! (merge (district/load-district registry-entry)
                             {:district/image-hash "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH"
                              :district/title "HapplyHarambe"}))
      (db/insert-param-change! (param-change/load-param-change registry-entry)))
    (catch :default e
      (error error-text {:args args :error (ex-message e)} ::on-constructed))))


(defn on-challenge-created [{:keys [:registry-entry :timestamp] :as args}]
  (info info-text {:args args} ::on-challenge-created)
  (try
    (db/update-registry-entry! (merge (registry-entry/load-registry-entry registry-entry)
                                      (registry-entry/load-registry-entry-challenge registry-entry)
                                      {:challenge/created-on timestamp}))
    (catch :default e
      (error error-text {:args args :error (ex-message e)} ::on-challenge-created))))


(defn on-vote-committed [{:keys [:registry-entry :timestamp :data] :as args}]
  (info info-text {:args args} ::on-vote-committed)
  (try
    (let [voter (web3-utils/uint->address (first data))
          vote (registry-entry/load-vote registry-entry voter)]
      (db/insert-vote! (merge vote {:vote/created-on timestamp})))
    (catch :default e
      (error error-text {:args args :error (ex-message e)} ::on-vote-committed))))


(defn on-vote-revealed [{:keys [:registry-entry :timestamp :data] :as args}]
  (info info-text {:args args} ::on-vote-revealed)
  (try
    (let [voter (web3-utils/uint->address (first data))]
      (db/update-registry-entry! (merge (registry-entry/load-registry-entry registry-entry)
                                        (registry-entry/load-registry-entry-challenge registry-entry)))
      (db/update-vote! (registry-entry/load-vote registry-entry voter)))
    (catch :default e
      (error error-text {:args args :error (ex-message e)} ::on-vote-revealed))))


(defn on-vote-reward-claimed [{:keys [:registry-entry :timestamp :data] :as args}]
  (info info-text {:args args} ::on-vote-reward-claimed)
  (try
    (let [voter (web3-utils/uint->address (first data))
          vote (registry-entry/load-vote registry-entry voter)]
      (db/update-vote! vote))
    (catch :default e
      (error error-text {:args args :error (ex-message e)} ::on-vote-reward-claimed))))


(defn on-challenge-reward-claimed [{:keys [:registry-entry :timestamp :data] :as args}]
  (info info-text {:args args} ::on-challenge-reward-claimed)
  (try
    (let [{:keys [:challenge/challenger :reg-entry/deposit] :as reg-entry}
          (merge (registry-entry/load-registry-entry registry-entry)
                 (registry-entry/load-registry-entry-challenge registry-entry))]

      (db/update-registry-entry! reg-entry))
    (catch :default e
      (error error-text {:args args :error (ex-message e)} ::on-challenge-reward-claimed))))


(def registry-entry-events
  {:constructed on-constructed
   :challenge-created on-challenge-created
   :vote-committed on-vote-committed
   :vote-revealed on-vote-revealed
   :vote-reward-claimed on-vote-reward-claimed
   :challenge-reward-claimed on-challenge-reward-claimed})


(defn dispatch-registry-entry-event [type err {{:keys [:event-type] :as args} :args :as event}]
  (let [event-type (cs/->kebab-case-keyword (web3-utils/bytes32->str event-type))]
    ((get registry-entry-events event-type identity)
      (-> args
        (assoc :event-type event-type)
        (update :timestamp bn/number)
        (update :version bn/number))
      event
      type)))


(defn start [opts]
  (when-not (web3/connected? @web3)
    (throw (js/Error. "Can't connect to Ethereum node")))
  [(-> (registry/registry-entry-event [:district-registry :district-registry-fwd] {} {:from-block 0 :to-block "latest"})
     (replay-past-events (partial dispatch-registry-entry-event :district)))
   (-> (registry/registry-entry-event [:param-change-registry :param-change-registry-fwd] {} {:from-block 0 :to-block "latest"})
     (replay-past-events (partial dispatch-registry-entry-event :param-change)))])


(defn stop [syncer]
  (doseq [filter (remove nil? @syncer)]
    (web3-eth/stop-watching! filter (fn [err]))))

