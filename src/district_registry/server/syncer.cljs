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
   [district-registry.server.utils :as server-utils]
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

(defn get-ipfs-meta [hash & [default]]
  (js/Promise.
    (fn [resolve reject]
      (log/info (str "Downloading: " "/ipfs/" hash) ::get-ipfs-meta)
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
              (log/error error-text {:error (ex-message e)} ::get-ipfs-meta)
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
  (db/insert-registry-entry! (merge registry-entry
                               {:reg-entry/created-on timestamp})))

(defn- add-param-change [{:keys [:param-change/key :param-change/db] :as param-change}]
  (let [{:keys [:initial-param/value] :as initial-param} (db/get-initial-param key db)]
    (db/insert-or-replace-param-change! (assoc param-change :param-change/initial-value value))))

(defmulti process-event (fn [contract-type ev done-chan] [contract-type (:event-type ev)]))

(defmethod process-event [:contract/district :constructed]
  [contract-type
   {:keys [:registry-entry :timestamp :creator :meta-hash
           :version :deposit :challenge-period-end
           :dnt-weight]
    :as ev}
   done-chan]
  (try-catch
    (let [registry-entry-data {:reg-entry/address registry-entry
                               :reg-entry/creator creator
                               :reg-entry/version version
                               :reg-entry/created-on timestamp
                               :reg-entry/deposit (bn/number deposit)
                               :reg-entry/challenge-period-end (bn/number challenge-period-end)}
          district {:reg-entry/address registry-entry
                    :district/meta-hash (web3/to-ascii meta-hash)
                    :district/dnt-weight (.toNumber dnt-weight)
                    :district/dnt-staked 0
                    :district/total-supply 0}]
      (add-registry-entry registry-entry-data timestamp)
      (let [{:keys [:district/meta-hash]} district]
        (.then (get-ipfs-meta meta-hash {:name "Dummy district name" ;;TODO
                                         :description "dummy description"
                                         :url "dummy url"
                                         :github-url "dummy gh url"
                                         :logo-image-hash "dummy logo hash"
                                         :background-image-hash "dummy background hash"})
          (fn [district-meta]
            (try-catch
              (->> district-meta
                (map (fn [[k v]]
                       [(keyword "district" (name k))
                        v]))
                (into district)
                (db/insert-district!)))
            (a/close! done-chan)))))))

(defmethod process-event [:contract/param-change :constructed]
  [contract-type {:keys [:registry-entry :timestamp] :as ev} done-chan]
  (try-catch
    (add-registry-entry registry-entry timestamp)
    (add-param-change registry-entry))
  (a/close! done-chan))

(defmethod process-event [:contract/param-change :change-applied]
  [contract-type {:keys [:registry-entry :timestamp] :as ev} done-chan]
  (try-catch
    (add-param-change registry-entry))
  (a/close! done-chan))

(defmethod process-event [:contract/registry-entry :challenge-created]
  [_ {:keys [:registry-entry
             :index
             :challenger
             :commit-period-end
             :reveal-period-end
             :reward-pool
             :meta-hash]
      :as ev}
   done-chan]
  (try-catch
    (db/insert-challenge! {:reg-entry/address registry-entry
                           :challenge/index (.toNumber index)
                           :challenge/challenger challenger
                           :challenge/commit-period-end (.toNumber commit-period-end)
                           :challenge/reveal-period-end (.toNumber reveal-period-end)
                           :challenge/reward-pool (.toNumber reward-pool)
                           :challenge/meta-hash (web3/to-ascii meta-hash)})
    (db/update-registry-entry! {:reg-entry/address registry-entry
                                :reg-entry/current-challenge-index (.toNumber index)})
    (.then (get-ipfs-meta (web3/to-ascii meta-hash) {:comment "Dummy comment"})
      (fn [{:keys [comment]}]
        (try-catch
          (db/update-challenge! {:reg-entry/address registry-entry
                                 :challenge/index (.toNumber index)
                                 :challenge/comment comment})
          (a/close! done-chan))))))

(defmethod process-event [:contract/registry-entry :vote-committed]
  [_
   {:keys [:registry-entry
           :index
           :voter
           :amount
           :timestamp]
    :as ev}
   done-chan]
  (try-catch
    (db/insert-vote!
      {:reg-entry/address registry-entry
       :challenge/index (.toNumber index)
       :vote/voter voter
       :vote/amount (.toNumber amount)
       :vote/option 0 ; neither, changed to include/exclude when revealed
       :vote/created-on timestamp}))
  (a/close! done-chan))

(defmethod process-event [:contract/registry-entry :vote-revealed]
  [_
   {:keys [:registry-entry
           :index
           :timestamp
           :voter
           :option
           :index]
    :as ev}
   done-chan]
  (try-catch
    (let [index (.toNumber index)
          option (.toNumber option)
          vote (db/get-vote
                 {:reg-entry/address registry-entry
                  :challenge/index index
                  :vote/voter voter}
                 [:*])
          vote' (assoc vote
                  :vote/option option
                  :vote/revealed-on timestamp)
          challenge (db/get-challenge
                      {:reg-entry/address registry-entry
                       :challenge/index index}
                      [:*])
          challenge' (update challenge
                       (case option
                         1 :challenge/votes-include
                         2 :challenge/votes-exclude)
                       (fnil + 0)
                       (:vote/amount vote'))]
      (db/update-challenge! challenge')
      (db/update-vote! vote')))
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

(defmethod process-event [:contract/district :stake-changed]
  [_
   {:keys [registry-entry
           staker-tokens
           staker-dnt-staked
           dnt-staked
           total-supply
           staker]
    :as ev}
   done-chan]
  (try-catch
    (db/update-district!
      {:reg-entry/address registry-entry
       :district/dnt-staked (.toNumber dnt-staked)
       :district/total-supply (.toNumber total-supply)})
    (db/insert-or-replace-stake!
      {:reg-entry/address registry-entry
       :stake/staker staker
       :stake/dnt (.toNumber staker-dnt-staked)
       :stake/tokens (.toNumber staker-tokens)}))
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
(defn enqueue-event [queue contract-type event-type err {:keys [args event] :as a}]
  (let [ev (-> args
             (assoc :contract-address (:address a))
             (assoc :event-type event-type)
             (update :timestamp (fn [ts]
                                  (if ts
                                    (bn/number ts)
                                    (server-utils/now-in-seconds))))
             (update :version bn/number))]
    (log/info (str "Enqueueing" " " info-text " " contract-type " " event-type) {:ev ev} ::dispatch-event)
    (a/put! queue [contract-type ev])))

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
        watchers [{:watcher (partial eternal-db/change-applied-event [:param-change-registry-db])
                   :on-event (partial enqueue-event event-queue :contract/eternal-db :eternal-db-event)}
                  {:watcher (partial eternal-db/change-applied-event [:district-registry-db])
                   :on-event (partial enqueue-event event-queue :contract/eternal-db :eternal-db-event)}
                  {:watcher (partial registry/district-constructed-event [:district-registry :district-registry-fwd])
                   :on-event (partial enqueue-event event-queue :contract/district :constructed)}
                  {:watcher (partial registry/district-stake-changed-event [:district-registry :district-registry-fwd])
                   :on-event (partial enqueue-event event-queue :contract/district :stake-changed)}
                  {:watcher (partial registry/challenge-created-event [:district-registry :district-registry-fwd])
                   :on-event (partial enqueue-event event-queue :contract/registry-entry :challenge-created)}
                  {:watcher (partial registry/vote-committed-event [:district-registry :district-registry-fwd])
                   :on-event (partial enqueue-event event-queue :contract/registry-entry :vote-committed)}
                  {:watcher (partial registry/vote-revealed-event [:district-registry :district-registry-fwd])
                   :on-event (partial enqueue-event event-queue :contract/registry-entry :vote-revealed)}]
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
