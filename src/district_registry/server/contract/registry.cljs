(ns district-registry.server.contract.registry
  (:require [district.server.smart-contracts :as smart-contracts]))

(defn set-factory [contract-key {:keys [:factory :factory?]} & [opts]]
  (smart-contracts/contract-send contract-key :set-factory [factory factory?] (merge opts {:gas 100000})))

(defn add-registry-entry [contract-key reg-entry & [opts]]
  (smart-contracts/contract-send contract-key :set-factory [reg-entry] (merge opts {:gas 100000})))

(defn set-emergency [contract-key emergency? & [opts]]
  (smart-contracts/contract-send contract-key :set-emergency [emergency?] (merge opts {:gas 100000})))

(defn district-constructed-event-in-tx [contract-key tx-receipt]
    (smart-contracts/contract-event-in-tx contract-key :DistrictConstructedEvent tx-receipt))

(defn challenge-created-event-in-tx
  "usage : (challenge-created-event-in-tx [:district-registry :district-registry-fwd] tx-receipt)"
  [contract-key tx-receipt]
  (smart-contracts/contract-event-in-tx contract-key :ChallengeCreatedEvent tx-receipt))

(defn vote-committed-event-in-tx [contract-key tx-receipt]
  (smart-contracts/contract-event-in-tx contract-key :VoteCommittedEvent tx-receipt))

(defn vote-revealed-event-in-tx [contract-key tx-receipt]
  (smart-contracts/contract-event-in-tx contract-key :VoteRevealedEvent tx-receipt))

(defn votes-reclaimed-event-in-tx [contract-key tx-receipt]
  (smart-contracts/contract-event-in-tx contract-key :VotesReclaimedEvent tx-receipt))

(defn vote-reward-claimed-event-in-tx [contract-key tx-receipt]
  (smart-contracts/contract-event-in-tx contract-key :VoteRewardClaimedEvent tx-receipt))

(defn challenger-reward-claimed-event-in-tx [contract-key tx-receipt]
  (smart-contracts/contract-event-in-tx contract-key :ChallengerRewardClaimedEvent tx-receipt))

(defn creator-reward-claimed-event-in-tx [contract-key tx-receipt]
  (smart-contracts/contract-event-in-tx contract-key :CreatorRewardClaimedEvent tx-receipt))

(defn district-stake-changed-event-in-tx [contract-key tx-receipt]
  (smart-contracts/contract-event-in-tx contract-key :DistrictStakeChangedEvent tx-receipt))

(defn district-meta-hash-changed-event-in-tx [contract-key tx-receipt]
  (smart-contracts/contract-event-in-tx contract-key :DistrictMetaHashChangedEvent tx-receipt))

(defn factory? [contract-key factory]
  (smart-contracts/contract-call contract-key :is-factory [factory]))

(defn registry-entry? [contract-key reg-entry]
  (smart-contracts/contract-call contract-key :is-registry-entry [reg-entry]))

(defn emergency? [contract-key]
  (smart-contracts/contract-call contract-key :is-emergency))
