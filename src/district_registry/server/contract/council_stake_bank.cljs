(ns district-registry.server.contract.council-stake-bank
  (:require [district.server.smart-contracts :as smart-contracts]))

;; Read functions

(defn balance-of [contract-key seat-index staker]
  (smart-contracts/contract-call contract-key :balance-of [seat-index staker]))

(defn balance-of-at [contract-key seat-index staker block-number]
  (smart-contracts/contract-call contract-key :balance-of-at [seat-index staker block-number]))

(defn total-staked-for-seat [contract-key seat-index]
  (smart-contracts/contract-call contract-key :total-staked-for-seat [seat-index]))

(defn total-staked-for-seat-at [contract-key seat-index block-number]
  (smart-contracts/contract-call contract-key :total-staked-for-seat-at [seat-index block-number]))

(defn council [contract-key]
  (smart-contracts/contract-call contract-key :council))

;; Write functions

(defn stake-on-seat [contract-key seat-index amount & [opts]]
  (smart-contracts/contract-send contract-key :stake-on-seat [seat-index amount]
                                  (merge opts {:gas 200000})))

(defn unstake-from-seat [contract-key seat-index amount & [opts]]
  (smart-contracts/contract-send contract-key :unstake-from-seat [seat-index amount]
                                  (merge opts {:gas 200000})))

(defn move-stake [contract-key from-seat to-seat amount & [opts]]
  (smart-contracts/contract-send contract-key :move-stake [from-seat to-seat amount]
                                  (merge opts {:gas 250000})))

;; Event functions

(defn staked-on-mind-event-in-tx [contract-key tx-receipt]
  (smart-contracts/contract-event-in-tx contract-key :StakedOnMind tx-receipt))

(defn unstaked-from-mind-event-in-tx [contract-key tx-receipt]
  (smart-contracts/contract-event-in-tx contract-key :UnstakedFromMind tx-receipt))

(defn stake-moved-event-in-tx [contract-key tx-receipt]
  (smart-contracts/contract-event-in-tx contract-key :StakeMoved tx-receipt))
