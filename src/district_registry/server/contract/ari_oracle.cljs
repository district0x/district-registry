(ns district-registry.server.contract.ari-oracle
  (:require [district.server.smart-contracts :as smart-contracts]))

;; Read functions

(defn get-proposal [contract-key proposal-id]
  (smart-contracts/contract-call contract-key :get-proposal [proposal-id]))

(defn check-veto [contract-key proposal-id]
  (smart-contracts/contract-call contract-key :check-veto [proposal-id]))

(defn veto-period-duration [contract-key]
  (smart-contracts/contract-call contract-key :veto-period-duration))

(defn veto-threshold-bps [contract-key]
  (smart-contracts/contract-call contract-key :veto-threshold-bps))

(defn dnt-token [contract-key]
  (smart-contracts/contract-call contract-key :dnt-token))

(defn ari-address [contract-key]
  (smart-contracts/contract-call contract-key :ari-address))

(defn proposal-count [contract-key]
  (smart-contracts/contract-call contract-key :proposal-count))

;; Write functions

(defn create-proposal [contract-key proposal-type reasoning-hash amount recipient & [opts]]
  (smart-contracts/contract-send contract-key :create-proposal
                                  [proposal-type reasoning-hash amount recipient]
                                  (merge opts {:gas 300000})))

(defn start-veto-period [contract-key proposal-id & [opts]]
  (smart-contracts/contract-send contract-key :start-veto-period [proposal-id]
                                  (merge opts {:gas 200000})))

(defn execute-proposal [contract-key proposal-id & [opts]]
  (smart-contracts/contract-send contract-key :execute-proposal [proposal-id]
                                  (merge opts {:gas 300000})))

(defn cancel-proposal [contract-key proposal-id & [opts]]
  (smart-contracts/contract-send contract-key :cancel-proposal [proposal-id]
                                  (merge opts {:gas 100000})))

(defn set-ari-address [contract-key new-ari-addr & [opts]]
  (smart-contracts/contract-send contract-key :set-ari-address [new-ari-addr]
                                  (merge opts {:gas 100000})))

;; Event functions

(defn proposal-created-event-in-tx [contract-key tx-receipt]
  (smart-contracts/contract-event-in-tx contract-key :ProposalCreated tx-receipt))

(defn veto-cast-event-in-tx [contract-key tx-receipt]
  (smart-contracts/contract-event-in-tx contract-key :VetoCast tx-receipt))

(defn proposal-approved-event-in-tx [contract-key tx-receipt]
  (smart-contracts/contract-event-in-tx contract-key :ProposalApproved tx-receipt))

(defn proposal-vetoed-event-in-tx [contract-key tx-receipt]
  (smart-contracts/contract-event-in-tx contract-key :ProposalVetoed tx-receipt))

(defn proposal-executed-event-in-tx [contract-key tx-receipt]
  (smart-contracts/contract-event-in-tx contract-key :ProposalExecuted tx-receipt))

(defn proposal-cancelled-event-in-tx [contract-key tx-receipt]
  (smart-contracts/contract-event-in-tx contract-key :ProposalCancelled tx-receipt))
