(ns district-registry.server.syncer
  (:require
   [bignumber.core :as bn]
   [camel-snake-kebab.core :as cs :include-macros true]
   [cljs-ipfs-api.files :as ifiles]
   [cljs-solidity-sha3.core :refer [solidity-sha3]]
   [cljs-web3.core :as web3]
   [cljs-web3.eth :as web3-eth]
   [cljs.core.async :as a]
   [district-registry.server.contract.district :as district]
   [district-registry.server.contract.eternal-db :as eternal-db]
   [district-registry.server.contract.param-change :as param-change]
   [district-registry.server.contract.registry :as registry]
   [district-registry.server.contract.registry-entry :as registry-entry]
   [district-registry.server.db :as db]
   [district-registry.server.deployer]
   [district-registry.server.generator]
   [district-registry.server.ipfs]
   [district.server.config :refer [config]]
   [district.server.smart-contracts :as smart-contracts :refer [replay-past-events]]
   [district.server.web3 :refer [web3]]
   [district.shared.error-handling :refer [try-catch]]
   [district.web3-utils :as web3-utils]
   [mount.core :as mount :refer [defstate]]
   [print.foo :refer [look] :include-macros true]
   [taoensso.timbre :as log]))

(declare start)
(declare stop)
(defstate ^{:on-reload :noop} syncer
  :start (start (merge
                  (:syncer @config)
                  (:syncer (mount/args))))
  :stop (stop syncer))

(def info-text "smart-contract event")
(def error-text "smart-contract event error")

(defn get-ipfs-data [hash & [default]]
  (js/Promise.
    (fn [resolve reject]
      (log/info (str "Downloading: " "/ipfs/" hash) ::get-ipfs-data)
      (ifiles/fget (str "/ipfs/" hash)
        {:req-opts {:compress false}}
        (fn [err content]
          (try
            (if (and (not err)
                  (not-empty content))
              ;; Get returns the entire content, this include CIDv0+more meta+data
              ;; TODO add better way of parsing get return
              (-> (re-find #".+(\{.+\})" content)
                second
                js/JSON.parse
                (js->clj :keywordize-keys true)
                resolve)
              (throw (js/Error. (str (or err "Error") " when downloading " "/ipfs/" hash ))))
            (catch :default e
              (log/error error-text {:error (ex-message e)} ::get-ipfs-data)
              (when goog.DEBUG
                (resolve default)))))))))

(defn- last-block-number []
  (web3-eth/block-number @web3))

(derive :contract/district :contract/registry-entry)
(derive :contract/param-change :contract/registry-entry)

;;;;;;;;;;;;;;;;;;;;;;
;; Event processors ;;
;;;;;;;;;;;;;;;;;;;;;;

(defn- add-registry-entry [registry-entry timestamp]
  (db/insert-registry-entry! (merge
                               (registry-entry/load-registry-entry registry-entry)
                               {:reg-entry/created-on timestamp})))

(defn- add-param-change [registry-entry]
  (let [{:keys [:param-change/key :param-change/db] :as param-change} (param-change/load-param-change registry-entry)
        {:keys [:initial-param/value] :as initial-param} (db/get-initial-param key db)]
    (db/insert-or-replace-param-change! (assoc param-change :param-change/initial-value value))))

(defmulti process-event (fn [contract-type ev done-chan] [contract-type (:event-type ev)]))

(defmethod process-event [:contract/district :constructed]
  [contract-type {:keys [:registry-entry :timestamp] :as ev} done-chan]
  (try-catch
    (add-registry-entry registry-entry timestamp)
    (let [{:keys [:reg-entry/creator]} (registry-entry/load-registry-entry registry-entry)
          {:keys [:district/info-hash] :as district} (district/load-district registry-entry)]
      (.then (get-ipfs-data info-hash {:name "Dummy district name" ;;TODO
                                       :description "dummy description"
                                       :url "dummy url"
                                       :github-url "dummy gh url"
                                       :logo-image-hash "dummy logo hash"
                                       :background-image-hash "dummy background hash"})
        (fn [district-info]
          (try-catch
            (->> district-info
              (map (fn [[k v]]
                     [(keyword "district" (name k))
                      v]))
              (into district)
              (db/insert-district!)))
          (a/close! done-chan))))))

(defmethod process-event [:contract/param-change :constructed]
  [contract-type {:keys [:registry-entry :timestamp] :as ev} done-chan]
  (try-catch
    (add-registry-entry registry-entry timestamp)
    (add-param-change registry-entry))
  (a/close! done-chan))

(defmethod process-event [:contract/param-change :change-applied]
  [contract-type {:keys [:registry-entry :timestamp] :as ev} done-chan]
  (try-catch
    ;; TODO: could also just change applied date to timestamp
    (add-param-change registry-entry))
  (a/close! done-chan))

(defmethod process-event [:contract/registry-entry :challenge-created]
  [_ {:keys [:registry-entry :timestamp :data] :as ev} done-chan]
  (try-catch
    (let [challenge-index (-> data first .toNumber)
          challenge (registry-entry/load-challenge registry-entry challenge-index)]
      (db/insert-challenge! (assoc challenge
                              :challenge/index challenge-index))
      (db/update-registry-entry! {:reg-entry/current-challenge-index challenge-index
                                  :reg-entry/address registry-entry})
      (.then (get-ipfs-data (:challenge/meta-hash challenge) {:comment "Dummy comment"})
        (fn [challenge-meta]
          (try-catch
            (db/update-challenge! (assoc challenge
                                    :challenge/comment (:comment challenge-meta)
                                    :challenge/index challenge-index)))
          (a/close! done-chan))))))

(defmethod process-event [:contract/registry-entry :vote-committed]
  [_ {:keys [:registry-entry :timestamp :data] :as ev} done-chan]
  (try-catch
    (let [[challenge-index voter] data
          challenge-index (.toNumber challenge-index)
          voter (web3-utils/uint->address voter)
          vote (registry-entry/load-vote registry-entry challenge-index voter)]
      (db/insert-vote! (assoc vote :vote/created-on timestamp))))
  (a/close! done-chan))

(defmethod process-event [:contract/registry-entry :vote-revealed]
  [_ {:keys [:registry-entry :timestamp :data] :as ev} done-chan]
  (try-catch
    (let [[challenge-index voter] data
          challenge-index (.toNumber challenge-index)
          voter (web3-utils/uint->address voter)
          vote (registry-entry/load-vote registry-entry challenge-index voter)
          challenge (registry-entry/load-challenge registry-entry challenge-index)]
      (db/update-challenge! challenge)
      (db/update-vote! vote)))
  (a/close! done-chan))

(defmethod process-event [:contract/registry-entry :vote-reward-claimed]
  [_ {:keys [:registry-entry :timestamp :data] :as ev} done-chan]
  (try-catch
    (let [[challenge-index voter] data
          challenge-index (.toNumber challenge-index)
          voter (web3-utils/uint->address voter)
          vote (registry-entry/load-vote registry-entry challenge-index voter)]
      (db/update-vote! vote)))
  (a/close! done-chan))

(defmethod process-event [:contract/registry-entry :challenge-reward-claimed]
  [_ {:keys [:registry-entry :timestamp :data] :as ev} done-chan]
  (try-catch
    (->> data
      first
      .toNumber
      (registry-entry/load-challenge registry-entry)
      db/update-challenge!))
  (a/close! done-chan))

(defn on-staked-or-unstaked [{:keys [:registry-entry :timestamp :data] :as ev}]
  (try-catch
    (->> data
      first
      web3-utils/uint->address
      (district/load-stake registry-entry)
      db/insert-or-replace-stake!)))

(defmethod process-event [:contract/registry-entry :staked]
  [_ event done-chan]
  (on-staked-or-unstaked event)
  (a/close! done-chan))

(defmethod process-event [:contract/registry-entry :unstaked]
  [_ event done-chan]
  (on-staked-or-unstaked event)
  (a/close! done-chan))

(defmethod process-event [:contract/eternal-db :eternal-db-event]
  [_ ev done-chan]
  (try-catch
    (let [{:keys [:contract-address :records :values :timestamp]} ev
          records->values (zipmap records values)
          keys->values (->> #{"challengePeriodDuration" "commitPeriodDuration" "revealPeriodDuration" "deposit"
                              "challengeDispensation" "voteQuorum"}
                         (map (fn [k] (when-let [v (records->values (web3/sha3 k))] [k v])))
                         (into {}))]
      (doseq [[k v] keys->values]
        (when-not (db/initial-param-exists? k contract-address)
          (db/insert-initial-param! {:initial-param/key k
                                     :initial-param/db contract-address
                                     :initial-param/value (bn/number v)
                                     :initial-param/set-on timestamp})))))
  (a/close! done-chan))

(defmethod process-event :default
  [contract-type {:keys [:event-type] :as evt} done-chan]
  (log/warn (str "No process-event method defined for processing contract-type: " contract-type " event-type: " event-type) evt ::process-event)
  (a/close! done-chan))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; End of events processors ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn enqueue-event [queue contract-type err {:keys [args event] :as a}]
  (try-catch
    (let [event-type (cond
                       (:event-type args) (cs/->kebab-case-keyword (web3-utils/bytes32->str (:event-type args)))
                       event (cs/->kebab-case-keyword event))
          ev (-> args
               (assoc :contract-address (:address a))
               (assoc :event-type event-type)
               (update :timestamp bn/number)
               (update :version bn/number))]
      (log/info (str "Enqueueing" " " info-text " " contract-type " " event-type) {:ev ev} ::dispatch-event)
      (a/put! queue [contract-type ev]))))

(defn start [{:keys [:initial-param-query] :as opts}]

  (when-not (web3/connected? @web3)
    (throw (js/Error. "Can't connect to Ethereum node")))

  (when-not (= ::db/started @db/district-registry-db)
    (throw (js/Error. "Database module has not started")))

  (let [last-block-number (last-block-number)
        event-queue (a/chan)
        event-loop (a/go-loop []
                     (try-catch
                       (when-let [[contract-type event :as event-data] (a/<! event-queue)]
                         (log/info (str "Processing "  info-text " " contract-type " " (:event-type event)) {:event event} ::process-event)
                         (let [done-chan (a/promise-chan)
                               _ (process-event contract-type event done-chan)
                               timeout-chan (a/timeout 5000)
                               result (a/alt!
                                        done-chan :done
                                        timeout-chan :timeout)]
                           (when (= result :timeout)
                             (log/warn (str "Timed out "  info-text " " contract-type " " (:event-type event)) {:event event} ::timeout-event)))
                         (recur))))
        watchers [{:watcher (partial eternal-db/change-applied-event :param-change-registry-db)
                   :on-event (partial enqueue-event event-queue :contract/eternal-db)}
                  {:watcher (partial eternal-db/change-applied-event :district-registry-db)
                   :on-event (partial enqueue-event event-queue :contract/eternal-db)}
                  {:watcher (partial registry/registry-entry-event [:district-registry :district-registry-fwd])
                   :on-event (partial enqueue-event event-queue :contract/district)}
                  {:watcher (partial registry/registry-entry-event [:param-change-registry :param-change-registry-fwd])
                   :on-event (partial enqueue-event event-queue :contract/param-change)}]
        watchers (concat
                   ;; Replay every past events (from block 0 to (dec last-block-number))
                   (when (pos? last-block-number)
                     (->> watchers
                       (map (fn [{:keys [watcher on-event]}]
                              (-> (apply watcher [{} {:from-block 0 :to-block (dec last-block-number)}])
                                (replay-past-events on-event))))
                       doall))
                   ;; Filters that will watch for last event and dispatch
                   (->> watchers
                     (map (fn [{:keys [watcher on-event]}]
                            (apply watcher [{} "latest" on-event])))
                     doall))]
    {:event-queue event-queue
     :watchers watchers}))

(defn stop [syncer]
  (let [{:keys [event-queue watchers]} @syncer]
    (a/close! event-queue)
    (doseq [watcher watchers
            :when watcher]
      (web3-eth/stop-watching! watcher (fn [err])))))
