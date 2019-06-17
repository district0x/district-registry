(ns district-registry.server.syncer
  (:require
    [bignumber.core :as bn]
    [camel-snake-kebab.core :as cs :include-macros true]
    [cljs-solidity-sha3.core :refer [solidity-sha3]]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [district-registry.server.contract.dnt :as dnt]
    [district-registry.server.contract.registry-entry :as registry-entry]
    [district-registry.server.db :as db]
    [district-registry.server.ipfs :as ipfs]
    [district-registry.server.utils :as server-utils]
    [district.server.config :refer [config]]
    [district.server.smart-contracts :refer [replay-past-events]]
    [district.server.web3 :refer [web3]]
    [district.server.web3-events :refer [register-callback! unregister-callbacks! register-after-past-events-dispatched-callback!]]
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


(defn- insert-registry-entry! [registry-entry timestamp]
  (db/insert-registry-entry! (merge registry-entry
                                    {:reg-entry/created-on timestamp})))


(defn district-constructed-event [_ {:keys [:args]}]
  (try-catch
    (let [{:keys [:registry-entry :timestamp :creator :meta-hash :version :deposit :challenge-period-end :dnt-weight]} args]
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
                      :district/total-supply 0}]
        (insert-registry-entry! registry-entry-data timestamp)
        (db/insert-district! district)
        (let [{:keys [:district/meta-hash]} district]
          (.then (server-utils/get-ipfs-meta @ipfs/ipfs meta-hash)
                 (fn [district-meta]
                   (try-catch
                     (->> district-meta
                       (map (fn [[k v]]
                              [(keyword "district" (name k))
                               v]))
                       (into {:reg-entry/address registry-entry})
                       (db/update-district!))))))))))


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
    (let [{:keys [:registry-entry :index :challenger :commit-period-end :reveal-period-end :reward-pool :meta-hash]} args]
      (db/insert-challenge!
        {:reg-entry/address registry-entry
         :challenge/index (bn/number index)
         :challenge/challenger challenger
         :challenge/commit-period-end (bn/number commit-period-end)
         :challenge/reveal-period-end (bn/number reveal-period-end)
         :challenge/reward-pool (bn/number reward-pool)
         :challenge/meta-hash (web3/to-ascii meta-hash)})
      (db/update-registry-entry! {:reg-entry/address registry-entry
                                  :reg-entry/current-challenge-index (bn/number index)})
      (.then (server-utils/get-ipfs-meta @ipfs/ipfs (web3/to-ascii meta-hash))
             (fn [{:keys [comment]}]
               (try-catch
                 (db/update-challenge! {:reg-entry/address registry-entry
                                        :challenge/index (bn/number index)
                                        :challenge/comment comment})))))))


(defn vote-committed-event [_ {:keys [:args]}]
  (try-catch
    (let [{:keys [:registry-entry :index :voter :amount :timestamp]} args]
      (db/insert-vote!
        {:reg-entry/address registry-entry
         :challenge/index (bn/number index)
         :vote/voter voter
         :vote/amount (bn/number amount)
         :vote/option 0                                     ; neither, changed to include/exclude when revealed
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
                                 (:vote/amount vote'))
                         (update :challenge/votes-total + (:vote/amount vote')))]
        (db/update-challenge! challenge')
        (db/update-vote! vote')))))


(defn vote-reward-claimed-event [_ {:keys [:args]}]
  (try-catch
    (let [{:keys [:registry-entry :index :timestamp :version :voter]} args]
      (db/update-vote! {:reg-entry/address registry-entry
                        :vote/voter voter
                        :challenge/index (bn/number index)
                        :vote/claimed-reward-on timestamp}))))


(defn votes-reclaimed-event [_ {:keys [:args]}]
  (try-catch
    (let [{:keys [:registry-entry :index :timestamp :version :voter]} args]
      (db/update-vote! {:reg-entry/address registry-entry
                        :vote/voter voter
                        :challenge/index (bn/number index)
                        :vote/reclaimed-votes-on timestamp}))))


(defn challenge-reward-claimed-event [_ {:keys [:args]}]
  (try-catch
    (let [{:keys [:registry-entry :index :timestamp :version :challenger :amount]} args]
      (db/update-challenge! {:reg-entry/address registry-entry
                             :challenge/index (bn/number index)
                             :challenge/claimed-reward-on timestamp}))))


(defn stake-changed-event [_ {:keys [:args]}]
  (try-catch
    (let [{:keys [:registry-entry :staker-tokens :staker-dnt-staked :dnt-staked :total-supply :staker]} args]
      (db/update-district!
        {:reg-entry/address registry-entry
         :district/dnt-staked (bn/number dnt-staked)
         :district/total-supply (bn/number total-supply)})
      (db/insert-or-replace-stake!
        {:reg-entry/address registry-entry
         :stake/staker staker
         :stake/dnt (bn/number staker-dnt-staked)
         :stake/tokens (bn/number staker-tokens)}))))


(defn eternal-db-event [_ {:keys [:args :address]}]
  (try-catch
    (let [{:keys [:records :values :timestamp]} args
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
      (update-in [:args :timestamp] (fn [timestamp]
                                      (if timestamp
                                        (bn/number timestamp)
                                        (:timestamp (web3-eth/get-block @web3 (:block-number event))))))
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
           :district-registry/challenge-created-event challenge-created-event
           :district-registry/vote-committed-event vote-committed-event
           :district-registry/vote-revealed-event vote-revealed-event
           :district-registry/vote-reward-claimed-event vote-reward-claimed-event
           :district-registry/votes-reclaimed-event votes-reclaimed-event
           :district-registry/challenge-reward-claimed-event challenge-reward-claimed-event
           :district-registry/stake-changed-event stake-changed-event}

          callback-ids (doseq [[event-key callback] event-callbacks]
                         (register-callback! event-key (dispatcher callback)))]

      (assoc opts :callback-ids callback-ids))))

(defn stop [syncer]
  (when-not (:disabled? @syncer)
    (unregister-callbacks! (:callback-ids @syncer))))
