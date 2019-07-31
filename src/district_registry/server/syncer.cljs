(ns district-registry.server.syncer
  (:require
    [bignumber.core :as bn]
    [camel-snake-kebab.core :as cs :include-macros true]
    [cljs-solidity-sha3.core :refer [solidity-sha3]]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [district-registry.server.db :as db]
    [district-registry.server.ipfs :as ipfs]
    [district-registry.server.utils :as server-utils]
    [district-registry.shared.utils :refer [vote-option->num]]
    [district.server.config :refer [config]]
    [district.server.smart-contracts :refer [replay-past-events]]
    [district.server.web3 :refer [web3]]
    [district.server.web3-events :refer [register-callback! unregister-callbacks! register-after-past-events-dispatched-callback!]]
    [district.shared.error-handling :refer [try-catch]]
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


(defn- insert-registry-entry! [registry-entry timestamp]
  (db/insert-registry-entry! (merge registry-entry
                                    {:reg-entry/created-on timestamp})))


(defn- transform-district-keys [[k v]]
  [(keyword "district" (name k)) v])


(defn district-constructed-event [_ {:keys [:args]}]
  (try-catch
    (let [{:keys [:registry-entry :timestamp :creator :meta-hash :version :deposit :challenge-period-end :dnt-weight :aragon-dao :aragon-id]} args]
      (let [registry-entry-data {:reg-entry/address registry-entry
                                 :reg-entry/creator creator
                                 :reg-entry/version version
                                 :reg-entry/created-on timestamp
                                 :reg-entry/deposit (bn/number deposit)
                                 :reg-entry/challenge-period-end (bn/number challenge-period-end)}
            district {:reg-entry/address registry-entry
                      :district/meta-hash (web3/to-ascii meta-hash)
                      :district/dnt-weight (bn/number dnt-weight)
                      :district/dnt-staked 0
                      :district/total-supply 0
                      :district/aragon-dao aragon-dao
                      :district/aragon-id aragon-id}]
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


(defn district-meta-hash-changed-event [_ {:keys [:args]}]
  (try-catch
    (let [{:keys [:registry-entry :meta-hash]} args]
      (.then (server-utils/get-ipfs-meta @ipfs/ipfs (web3/to-ascii meta-hash))
             (fn [district-meta]
               (try-catch
                 (->> district-meta
                   (map transform-district-keys)
                   (into {:reg-entry/address registry-entry})
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
          index (bn/number index)]
      (db/insert-challenge!
        {:reg-entry/address registry-entry
         :challenge/index index
         :challenge/challenger challenger
         :challenge/commit-period-end (bn/number commit-period-end)
         :challenge/reveal-period-end (bn/number reveal-period-end)
         :challenge/reward-pool (bn/number reward-pool)
         :challenge/meta-hash (web3/to-ascii meta-hash)})
      (db/update-registry-entry! {:reg-entry/address registry-entry
                                  :reg-entry/current-challenge-index index})
      (.then (server-utils/get-ipfs-meta @ipfs/ipfs (web3/to-ascii meta-hash))
             (fn [{:keys [comment]}]
               (try-catch
                 (db/update-challenge! {:reg-entry/address registry-entry
                                        :challenge/index index
                                        :challenge/comment comment})))))))


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
                         (map (fn [k] (when-let [v (records->values (solidity-sha3 k))] [k v])))
                         (into {}))]
      (doseq [[k v] keys->values]
        (db/insert-initial-param! {:initial-param/key k
                                   :initial-param/db address
                                   :initial-param/value (bn/number v)
                                   :initial-param/set-on timestamp})))))


(defn- dispatcher [callback]
  (fn [err event]
    (-> event
      (update-in [:args :timestamp] bn/number)
      (update-in [:args :version] bn/number)
      (->> (callback err)))))


(defn start [opts]
  (when-not (:disabled? opts)

    (when-not (web3/connected? @web3)
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
                         (register-callback! event-key (dispatcher callback)))]

      (assoc opts :callback-ids callback-ids))))

(defn stop [syncer]
  (when-not (:disabled? @syncer)
    (unregister-callbacks! (:callback-ids @syncer))))