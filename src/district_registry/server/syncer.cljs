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
  (info info-text type {:args args} ::on-constructed)
  (try
    (db/insert-registry-entry! (merge
                                 (registry-entry/load-registry-entry registry-entry)
                                 {:reg-entry/created-on timestamp}))
    (if (= type :district)
      (db/insert-district! (district/load-district registry-entry))
      (db/insert-param-change! (param-change/load-param-change registry-entry)))
    (catch :default e
      (error error-text {:args args :error (ex-message e)} ::on-constructed))))

(defn on-challenge-created [{:keys [:registry-entry :timestamp :data] :as args}]
  (info info-text {:args args} ::on-challenge-created)
  (try
    (let [challenge-index (-> data first .toNumber)]
      (db/insert-challenge!
        (merge
          (registry-entry/load-challenge registry-entry challenge-index)
          {:challenge/index challenge-index})))
    (catch :default e
      (error error-text {:args args :error (ex-message e)} ::on-challenge-created))))

(defn on-vote-committed [{:keys [:registry-entry :timestamp :data] :as args}]
  (info info-text {:args args} ::on-vote-committed)
  (try
    (let [[challenge-index voter] data
          challenge-index (.toNumber challenge-index)
          voter (web3-utils/uint->address voter)
          vote (registry-entry/load-vote registry-entry challenge-index voter)]
      (db/insert-vote! (merge vote
                         {:vote/created-on timestamp})))
    (catch :default e
      (error error-text {:args args :error (ex-message e)} ::on-vote-committed))))

(defn on-vote-revealed [{:keys [:registry-entry :timestamp :data] :as args}]
  (info info-text {:args args} ::on-vote-revealed)
  (try
    (let [[challenge-index voter] data
          challenge-index (.toNumber challenge-index)
          voter (web3-utils/uint->address voter)
          vote (registry-entry/load-vote registry-entry challenge-index voter)
          challenge (registry-entry/load-challenge registry-entry challenge-index)]
      (db/update-challenge! challenge)
      (db/update-vote! vote))
    (catch :default e
      (error error-text {:args args :error (ex-message e)} ::on-vote-revealed))))

(defn on-vote-reward-claimed [{:keys [:registry-entry :timestamp :data] :as args}]
  (info info-text {:args args} ::on-vote-reward-claimed)
  (try
    (let [[challenge-index voter] data
          challenge-index (.toNumber challenge-index)
          voter (web3-utils/uint->address voter)
          vote (registry-entry/load-vote registry-entry challenge-index voter)]
      (db/update-vote! vote))
    (catch :default e
      (error error-text {:args args :error (ex-message e)} ::on-vote-reward-claimed))))

(defn on-challenge-reward-claimed [{:keys [:registry-entry :timestamp :data] :as args}]
  (info info-text {:args args} ::on-challenge-reward-claimed)
  (try
    (->> data
      first
      .toNumber
      (registry-entry/load-challenge registry-entry)
      db/update-challenge!)
    (catch :default e
      (error error-text {:args args :error (ex-message e)} ::on-challenge-reward-claimed))))

(defn on-staked-or-unstaked-fn [event {:keys [:registry-entry :timestamp :data] :as args}]
  (info info-text {:args args} event)
  (try
    (db/update-district! (district/load-district registry-entry))
    (->> data
      first
      web3-utils/uint->address
      (district/load-stake registry-entry)
      db/insert-or-replace-stake!)
    (catch :default e
      (error error-text {:args args :error (ex-message e)} event))))

(def on-staked (partial on-staked-or-unstaked-fn ::on-staked))
(def on-unstaked (partial on-staked-or-unstaked-fn ::on-unstaked))

(def registry-entry-events
  {:constructed on-constructed
   :challenge-created on-challenge-created
   :vote-committed on-vote-committed
   :vote-revealed on-vote-revealed
   :vote-reward-claimed on-vote-reward-claimed
   :challenge-reward-claimed on-challenge-reward-claimed
   :staked on-staked
   :unstaked on-unstaked})

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

