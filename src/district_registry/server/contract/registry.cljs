(ns district-registry.server.contract.registry
  (:require [district.server.smart-contracts :refer [contract-call create-event-filter contract-event-in-tx]]))

(defn db [contract-key]
  (contract-call contract-key :db))

(defn construct [contract-key {:keys [:db]} & [opts]]
  (contract-call contract-key :construct [db] (merge {:gas 100000} opts)))

(defn set-factory [contract-key {:keys [:factory :factory?]} & [opts]]
  (contract-call contract-key :set-factory [factory factory?] (merge opts {:gas 100000})))

(defn challenge-created-event [contract-key opts on-event]
  (create-event-filter contract-key :ChallengeCreatedEvent {} opts on-event))

(defn vote-committed-event [contract-key opts on-event]
  (create-event-filter contract-key :VoteCommittedEvent {} opts on-event))

(defn vote-revealed-event [contract-key opts on-event]
  (create-event-filter contract-key :VoteRevealedEvent {} opts on-event))

(defn votes-reclaimed-event [contract-key opts on-event]
  (create-event-filter contract-key :VotesReclaimedEvent {} opts on-event))

(defn vote-reward-claimed-event [contract-key opts on-event]
  (create-event-filter contract-key :VoteRewardClaimedEvent {} opts on-event))

(defn challenger-reward-claimed-event [contract-key opts on-event]
  (create-event-filter contract-key :ChallengerRewardClaimedEvent {} opts on-event))

(defn creator-reward-claimed-event [contract-key opts on-event]
  (create-event-filter contract-key :CreatorRewardClaimedEvent {} opts on-event))

(defn district-constructed-event [contract-key opts on-event]
  (create-event-filter contract-key :DistrictConstructedEvent {} opts on-event))

(defn district-stake-changed-event [contract-key opts on-event]
  (create-event-filter contract-key :DistrictStakeChangedEvent {} opts on-event))

(defn district-meta-hash-changed-event [contract-key opts on-event]
  (create-event-filter contract-key :DistrictMetaHashChangedEvent {} opts on-event))

(defn district-constructed-event-in-tx [contract-key tx-hash & args]
  (apply contract-event-in-tx tx-hash contract-key :DistrictConstructedEvent args))

(defn challenge-created-event-in-tx [contract-key tx-hash & args]
  (apply contract-event-in-tx tx-hash contract-key :ChallengeCreatedEvent args))

(defn vote-committed-event-in-tx [contract-key tx-hash & args]
  (apply contract-event-in-tx tx-hash contract-key :VoteCommittedEvent args))

(defn vote-revealed-event-in-tx [contract-key tx-hash & args]
  (apply contract-event-in-tx tx-hash contract-key :VoteRevealedEvent args))

(defn votes-reclaimed-event-in-tx [contract-key tx-hash & args]
  (apply contract-event-in-tx tx-hash contract-key :VotesReclaimedEvent args))

(defn vote-reward-claimed-event-in-tx [contract-key tx-hash & args]
  (apply contract-event-in-tx tx-hash contract-key :VoteRewardClaimedEvent args))

(defn challenger-reward-claimed-event-in-tx [contract-key tx-hash & args]
  (apply contract-event-in-tx tx-hash contract-key :ChallengerRewardClaimedEvent args))

(defn creator-reward-claimed-event-in-tx [contract-key tx-hash & args]
  (apply contract-event-in-tx tx-hash contract-key :CreatorRewardClaimedEvent args))

(defn district-stake-changed-event-in-tx [contract-key tx-hash & args]
  (apply contract-event-in-tx tx-hash contract-key :DistrictStakeChangedEvent args))

(defn district-meta-hash-changed-event-in-tx [contract-key tx-hash & args]
  (apply contract-event-in-tx tx-hash contract-key :DistrictMetaHashChangedEvent args))

(defn is-factory? [contract-key factory]
  (contract-call contract-key :is-factory [factory]))
