(ns district-registry.server.contract.power-plant
  (:require [district.server.smart-contracts :as smart-contracts]))

;; Read functions

(defn get-treasury-balance [contract-key token]
  (smart-contracts/contract-call contract-key :get-treasury-balance [token]))

(defn get-bounty [contract-key bounty-id]
  (smart-contracts/contract-call contract-key :get-bounty [bounty-id]))

(defn get-grant [contract-key grant-id]
  (smart-contracts/contract-call contract-key :get-grant [grant-id]))

(defn ari-oracle [contract-key]
  (smart-contracts/contract-call contract-key :ari-oracle))

(defn owner [contract-key]
  (smart-contracts/contract-call contract-key :owner))

(defn bounty-count [contract-key]
  (smart-contracts/contract-call contract-key :bounty-count))

(defn grant-count [contract-key]
  (smart-contracts/contract-call contract-key :grant-count))

;; Write functions

(defn deposit [contract-key token amount & [opts]]
  (smart-contracts/contract-send contract-key :deposit [token amount]
                                  (merge opts {:gas 150000})))

(defn create-bounty [contract-key metadata-hash reward token deadline & [opts]]
  (smart-contracts/contract-send contract-key :create-bounty
                                  [metadata-hash reward token deadline]
                                  (merge opts {:gas 250000})))

(defn assign-bounty [contract-key bounty-id assignee & [opts]]
  (smart-contracts/contract-send contract-key :assign-bounty [bounty-id assignee]
                                  (merge opts {:gas 100000})))

(defn mark-bounty-complete [contract-key bounty-id & [opts]]
  (smart-contracts/contract-send contract-key :mark-bounty-complete [bounty-id]
                                  (merge opts {:gas 100000})))

(defn claim-bounty [contract-key bounty-id & [opts]]
  (smart-contracts/contract-send contract-key :claim-bounty [bounty-id]
                                  (merge opts {:gas 150000})))

(defn create-grant [contract-key recipient total-amount token vesting-duration metadata-hash & [opts]]
  (smart-contracts/contract-send contract-key :create-grant
                                  [recipient total-amount token vesting-duration metadata-hash]
                                  (merge opts {:gas 300000})))

(defn disburse-grant [contract-key grant-id & [opts]]
  (smart-contracts/contract-send contract-key :disburse-grant [grant-id]
                                  (merge opts {:gas 150000})))

(defn withdraw [contract-key token amount to & [opts]]
  (smart-contracts/contract-send contract-key :withdraw [token amount to]
                                  (merge opts {:gas 150000})))

;; Event functions

(defn treasury-deposit-event-in-tx [contract-key tx-receipt]
  (smart-contracts/contract-event-in-tx contract-key :TreasuryDeposit tx-receipt))

(defn bounty-created-event-in-tx [contract-key tx-receipt]
  (smart-contracts/contract-event-in-tx contract-key :BountyCreated tx-receipt))

(defn bounty-assigned-event-in-tx [contract-key tx-receipt]
  (smart-contracts/contract-event-in-tx contract-key :BountyAssigned tx-receipt))

(defn bounty-completed-event-in-tx [contract-key tx-receipt]
  (smart-contracts/contract-event-in-tx contract-key :BountyCompleted tx-receipt))

(defn bounty-claimed-event-in-tx [contract-key tx-receipt]
  (smart-contracts/contract-event-in-tx contract-key :BountyClaimed tx-receipt))

(defn grant-created-event-in-tx [contract-key tx-receipt]
  (smart-contracts/contract-event-in-tx contract-key :GrantCreated tx-receipt))

(defn grant-disbursement-event-in-tx [contract-key tx-receipt]
  (smart-contracts/contract-event-in-tx contract-key :GrantDisbursement tx-receipt))

(defn treasury-withdrawal-event-in-tx [contract-key tx-receipt]
  (smart-contracts/contract-event-in-tx contract-key :TreasuryWithdrawal tx-receipt))
