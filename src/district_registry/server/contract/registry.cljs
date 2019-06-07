(ns district-registry.server.contract.registry
  (:require [district.server.smart-contracts :refer [contract-call contract-event-in-tx]]))

(defn db [contract-key]
  (contract-call contract-key :db))

(defn construct [contract-key {:keys [:db]} & [opts]]
  (contract-call contract-key :construct [db] (merge {:gas 100000} opts)))

(defn set-factory [contract-key {:keys [:factory :factory?]} & [opts]]
  (contract-call contract-key :set-factory [factory factory?] (merge opts {:gas 100000})))

(defn district-constructed-event [contract-key & args]
  (apply contract-call contract-key :DistrictConstructedEvent args))

(defn district-stake-changed-event [contract-key & args]
  (apply contract-call contract-key :DistrictStakeChangedEvent args))

(defn challenge-created-event [contract-key & args]
  (apply contract-call contract-key :ChallengeCreatedEvent args))

(defn vote-committed-event [contract-key & args]
  (apply contract-call contract-key :VoteCommittedEvent args))

(defn vote-revealed-event [contract-key & args]
  (apply contract-call contract-key :VoteRevealedEvent args))

(defn factory? [contract-key factory]
  (contract-call contract-key :is-factory [factory]))
