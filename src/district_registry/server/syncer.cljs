(ns district-registry.server.syncer
  (:require [bignumber.core :as bn]
            [camel-snake-kebab.core :as camel-snake-kebab]
            [cljs-web3-next.core :as web3-core]
            [cljs-web3-next.eth :as web3-eth]
            [cljs-web3-next.utils :as web3-utils]
            [cljs.core.async :as async]
            [district-registry.server.db :as db]
            [district-registry.server.ipfs :as ipfs]
            [district-registry.server.utils :as server-utils]
            [district-registry.shared.utils :refer [vote-option->num]]
            [district.server.config :refer [config]]
            [district.server.smart-contracts :as smart-contracts]
            [district.server.web3 :refer [web3 ping-start ping-stop]]
            [district.server.web3-events :as web3-events]
            [district.shared.async-helpers :refer [safe-go <?]]
            [district.shared.error-handling :refer [try-catch]]
            [mount.core :as mount :refer [defstate]]
            [taoensso.timbre :as log]
            [clojure.string :as str]))

(declare start)
(declare stop)

(defstate ^{:on-reload :noop} syncer
  :start (start (merge
                  (:syncer @config)
                  (:syncer (mount/args))))
  :stop (stop syncer))


(defn- insert-registry-entry! [registry-entry timestamp]
  (db/insert-registry-entry! (merge registry-entry
                                    {:reg-entry/created-on timestamp})))


(defn- transform-district-keys [[k v]]
  [(keyword "district" (name k)) v])

(defn district-constructed-event [_ {:keys [:args]}]
  (try-catch
    (let [{:keys [:registry-entry :timestamp :creator :meta-hash :version :deposit :challenge-period-end :stake-bank :ens-name] :as arguments} args]
      (log/info "district-constructed-event" arguments)
      (let [registry-entry-data {:reg-entry/address registry-entry
                                 :reg-entry/creator creator
                                 :reg-entry/version version
                                 :reg-entry/created-on timestamp
                                 :reg-entry/deposit (bn/number deposit)
                                 :reg-entry/challenge-period-end (bn/number challenge-period-end)}
            district {:reg-entry/address registry-entry
                      :district/meta-hash (web3-utils/to-ascii @web3 meta-hash)
                      :district/dnt-staked 0
                      :district/total-supply 0
                      :district/stake-bank stake-bank
                      :district/ens-name ens-name}]
        (insert-registry-entry! registry-entry-data timestamp)
        (db/insert-district! district)
        (let [{:keys [:district/meta-hash]} district]
          (.then (server-utils/get-ipfs-meta @ipfs/ipfs meta-hash)
                 (fn [district-meta]
                   (try-catch
                     (->> district-meta
                       (map transform-district-keys)
                       (into {:reg-entry/address registry-entry})
                       (db/update-district!))))))))))


(defn- avoid-ens-name-update [registry-entry district-meta]
  (let [current-ens-name (:district/ens-name (db/get-district {:reg-entry/address registry-entry} [:district/ens-name]))]
    ;; only allows setting a new ens-name if it is not set yet.
    (if (str/blank? current-ens-name)
      district-meta
      (assoc district-meta :district/ens-name current-ens-name))))


(defn district-meta-hash-changed-event [_ {:keys [:args]}]
  (try-catch
    (let [{:keys [:registry-entry :meta-hash]} args]
      (.then (server-utils/get-ipfs-meta @ipfs/ipfs (web3-utils/to-ascii @web3 meta-hash))
             (fn [district-meta]
               (try-catch
                 (->> district-meta
                   (map transform-district-keys)
                   (into {:reg-entry/address registry-entry})
                   (avoid-ens-name-update registry-entry)
                   (db/update-district!))))))))


(defn param-change-constructed-event [_ {:keys [:args]}]
  (try-catch
    (let [{:keys [:registry-entry :creator :version :deposit :challenge-period-end :db :key :value :timestamp]} args]
      (insert-registry-entry! {:reg-entry/address registry-entry
                               :reg-entry/creator creator
                               :reg-entry/version version
                               :reg-entry/created-on timestamp
                               :reg-entry/deposit (bn/number deposit)
                               :reg-entry/challenge-period-end (bn/number challenge-period-end)}
                              timestamp)

      (db/insert-or-replace-param-change!
        {:reg-entry/address registry-entry
         :param-change/db db
         :param-change/key key
         :param-change/value (bn/number value)
         :param-change/initial-value (:initial-param/value (db/get-initial-param key db))}))))


(defn param-change-applied-event [_ {:keys [:args]}]
  (try-catch
    (let [{:keys [:registry-entry :timestamp]} args]
      (db/update-param-change! {:reg-entry/address registry-entry
                                :param-change/applied-on timestamp}))))

(defn challenge-created-event [_ {:keys [:args]}]
  (try-catch
    (let [{:keys [:registry-entry :index :challenger :commit-period-end :reveal-period-end :reward-pool :meta-hash]} args
          index (bn/number index)
          challenged-entry {:reg-entry/address registry-entry
                            :challenge/index index
                            :challenge/challenger challenger
                            :challenge/commit-period-end (bn/number commit-period-end)
                            :challenge/reveal-period-end (bn/number reveal-period-end)
                            :challenge/reward-pool (bn/number reward-pool)
                            :challenge/meta-hash (web3-utils/to-ascii @web3 meta-hash)}]
      (db/insert-challenge! challenged-entry)
      (db/update-registry-entry! {:reg-entry/address registry-entry
                                  :reg-entry/current-challenge-index index})
      (.then (server-utils/get-ipfs-meta @ipfs/ipfs (web3-utils/to-ascii @web3 meta-hash))
             (fn [{:keys [comment]}]
               (let [challenge-extra-info {:reg-entry/address registry-entry
                                           :challenge/index index
                                           :challenge/comment comment}]
                 (try-catch
                  (db/update-challenge! challenge-extra-info))))))))


(defn vote-committed-event [_ {:keys [:args]}]
  (try-catch
    (let [{:keys [:registry-entry :index :voter :amount :timestamp]} args]
      (db/insert-vote!
        {:reg-entry/address registry-entry
         :challenge/index (bn/number index)
         :vote/voter voter
         :vote/amount (bn/number amount)
         :vote/option (vote-option->num :vote-option/neither)
         :vote/created-on timestamp}))))


(defn vote-revealed-event [_ {:keys [:args]}]
  (try-catch
    (let [{:keys [:registry-entry :index :timestamp :voter :option]} args]
      (let [index (bn/number index)
            option (bn/number option)
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
            challenge' (-> challenge
                         (update (case option
                                   1 :challenge/votes-include
                                   2 :challenge/votes-exclude)
                                 +
                                 (:vote/amount vote')))]
        (db/update-challenge! challenge')
        (db/update-vote! vote')))))


(defn vote-reward-claimed-event [_ {:keys [:args]}]
  (try-catch
    (let [{:keys [:registry-entry :index :timestamp :version :voter]} args]
      (let [index (bn/number index)]
        (if (db/vote-exists? {:reg-entry/address registry-entry
                              :vote/voter voter
                              :challenge/index index})
          (db/update-vote! {:reg-entry/address registry-entry
                            :vote/voter voter
                            :challenge/index index
                            :vote/claimed-reward-on timestamp})
          ;; User got reward solely because of staking, he didn't vote
          ;; We still need to insert vote to mark reward as claimed
          (db/insert-vote! {:reg-entry/address registry-entry
                            :challenge/index index
                            :vote/voter voter
                            :vote/amount 0
                            :vote/option (vote-option->num :vote-option/neither)
                            :vote/created-on timestamp
                            :vote/claimed-reward-on timestamp}))))))


(defn votes-reclaimed-event [_ {:keys [:args]}]
  (try-catch
    (let [{:keys [:registry-entry :index :timestamp :version :voter]} args]
      (db/update-vote! {:reg-entry/address registry-entry
                        :vote/voter voter
                        :challenge/index (bn/number index)
                        :vote/reclaimed-votes-on timestamp}))))


(defn challenger-reward-claimed-event [_ {:keys [:args]}]
  (try-catch
    (let [{:keys [:registry-entry :index :timestamp :version :challenger :amount]} args]
      (db/update-challenge! {:reg-entry/address registry-entry
                             :challenge/index (bn/number index)
                             :challenge/challenger-reward-claimed-on timestamp}))))


(defn creator-reward-claimed-event [_ {:keys [:args]}]
  (try-catch
    (let [{:keys [:registry-entry :index :timestamp :version :creator :amount]} args]
      (db/update-challenge! {:reg-entry/address registry-entry
                             :challenge/index (bn/number index)
                             :challenge/creator-reward-claimed-on timestamp}))))


(defn stake-changed-event [_ {:keys [:args]}]
  (try-catch
    (let [{:keys [:registry-entry
                  :stake-id
                  :staker-voting-token-balance
                  :staker-dnt-staked
                  :dnt-total-staked
                  :voting-token-total-supply
                  :staker
                  :staked-amount
                  :is-unstake
                  :timestamp]} args]
      (db/update-district!
        {:reg-entry/address registry-entry
         :district/dnt-staked (bn/number dnt-total-staked)
         :district/total-supply (bn/number voting-token-total-supply)})
      (db/insert-stake-history!
        {:reg-entry/address registry-entry
         :stake-history/stake-id (bn/number stake-id)
         :stake-history/staker staker
         :stake-history/staked-on timestamp
         :stake-history/dnt-total-staked (bn/number dnt-total-staked)
         :stake-history/voting-token-total-supply (bn/number voting-token-total-supply)
         :stake-history/staker-dnt-staked (bn/number staker-dnt-staked)
         :stake-history/staker-voting-token-balance (bn/number staker-voting-token-balance)
         :stake-history/staked-amount (bn/number staked-amount)
         :stake-history/unstake? (if is-unstake 1 0)}))))


(defn eternal-db-event [_ {:keys [:args :address]}]
  (try-catch
    (let [{:keys [:records :values :timestamp ]} args
          records->values (zipmap records values)
          keys->values (->> #{"challengePeriodDuration" "commitPeriodDuration" "revealPeriodDuration" "deposit"
                              "challengeDispensation" "voteQuorum" "maxTotalSupply" "maxAuctionDuration"}
                         (map (fn [k] (when-let [v (records->values (web3-utils/sha3 @web3 k))] [k v])))
                         (into {}))]
      (doseq [[k v] keys->values]
        (db/insert-initial-param! {:initial-param/key k
                                   :initial-param/db address
                                   :initial-param/value (bn/number v)
                                   :initial-param/set-on timestamp})))))

(defn- block-timestamp* [block-number]
  (let [out-ch (async/promise-chan)]
    (smart-contracts/wait-for-block block-number (fn [error result]
                                                   (if error
                                                     (async/put! out-ch error)
                                                     (let [{:keys [:timestamp]} (js->clj result :keywordize-keys true)]
                                                       (log/debug "cache miss for block-timestamp" {:block-number block-number
                                                                                                    :timestamp timestamp})
                                                       (async/put! out-ch timestamp)))))
    out-ch))

(def block-timestamp
  (memoize block-timestamp*))

(defn- get-event [{:keys [:event :log-index :block-number]
                   {:keys [:contract-key :forwards-to]} :contract}]
  {:event/contract-key (name (or forwards-to contract-key))
   :event/event-name (name event)
   :event/log-index log-index
   :event/block-number block-number})

(defn- dispatcher [callback]
  (fn [err {:keys [:block-number] :as event}]
    (safe-go
     (try
       (let [block-timestamp (<? (block-timestamp block-number))
             event (-> event
                       (update :event camel-snake-kebab/->kebab-case)
                       (update-in [:args :version] bn/number)
                       (update-in [:args :timestamp] (fn [timestamp]
                                                       (if timestamp
                                                         (bn/number timestamp)
                                                         block-timestamp))))
             {:keys [:event/contract-key :event/event-name :event/block-number :event/log-index]} (get-event event)
             {:keys [:event/last-block-number :event/last-log-index :event/count]
              :or {last-block-number -1
                   last-log-index -1
                   count 0}} (db/get-last-event {:event/contract-key contract-key :event/event-name event-name} [:event/last-log-index :event/last-block-number :event/count])
             evt {:event/contract-key contract-key
                  :event/event-name event-name
                  :event/count count
                  :last-block-number last-block-number
                  :last-log-index last-log-index
                  :block-number block-number
                  :log-index log-index}]
         (if (or (> block-number last-block-number)
                 (and (= block-number last-block-number) (> log-index last-log-index)))
           (let [result (callback err event)]
             (log/info "Handling new event" evt)
             ;; block if we need
             (when (satisfies? cljs.core.async.impl.protocols/ReadPort result)
               (<! result))
             (db/upsert-event! {:event/last-log-index log-index
                                :event/last-block-number block-number
                                :event/count (inc count)
                                :event/event-name event-name
                                :event/contract-key contract-key}))

           (log/info "Skipping handling of a persisted event" evt)))
       (catch js/Error error
         (log/error "Exception when handling event" {:error error
                                                     :event event})
         ;; So we crash as fast as we can and don't drag errors that are harder to debug
         (js/process.exit 1))))))

(defn start [opts]
  (when-not (web3-eth/is-listening? @web3)
    (throw (js/Error. "Can't connect to Ethereum node")))

  (when-not (= ::db/started @db/district-registry-db)
    (throw (js/Error. "Database module has not started")))

  (let [event-callbacks
        {:param-change-db/eternal-db-event eternal-db-event
         :param-change-registry/param-change-constructed-event param-change-constructed-event
         :param-change-registry/param-change-applied-event param-change-applied-event
         :district-registry-db/eternal-db-event eternal-db-event
         :district-registry/district-constructed-event district-constructed-event
         :district-registry/district-meta-hash-changed-event district-meta-hash-changed-event
         :district-registry/challenge-created-event challenge-created-event
         :district-registry/vote-committed-event vote-committed-event
         :district-registry/vote-revealed-event vote-revealed-event
         :district-registry/vote-reward-claimed-event vote-reward-claimed-event
         :district-registry/votes-reclaimed-event votes-reclaimed-event
         :district-registry/challenger-reward-claimed-event challenger-reward-claimed-event
         :district-registry/creator-reward-claimed-event creator-reward-claimed-event
         :district-registry/stake-changed-event stake-changed-event}
        callback-ids (doseq [[event-key callback] event-callbacks]
                       (web3-events/register-callback! event-key (dispatcher callback)))]
    (web3-events/register-after-past-events-dispatched-callback! (fn []
                                                                   (log/warn "Syncing past events finished")
                                                                   (ping-start {:ping-interval 10000})))
    (assoc opts :callback-ids callback-ids)))

(defn stop [syncer]
  (ping-stop)
  (web3-events/unregister-callbacks! (:callback-ids @syncer)))
