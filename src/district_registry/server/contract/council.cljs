(ns district-registry.server.contract.council
  (:require [district.server.smart-contracts :as smart-contracts]))

;; Read functions

(defn get-seat [contract-key seat-index]
  (smart-contracts/contract-call contract-key :get-seat [seat-index]))

(defn get-council-weights [contract-key]
  (smart-contracts/contract-call contract-key :get-council-weights))

(defn verify-stranger-immutable [contract-key]
  (smart-contracts/contract-call contract-key :verify-stranger-immutable))

;; Write functions

(defn update-philosophy [contract-key seat-index philosophy-hash & [opts]]
  (smart-contracts/contract-send contract-key :update-philosophy [seat-index philosophy-hash]
                                  (merge opts {:gas 100000})))

(defn propose-new-mind [contract-key mind-id name philosophy-hash proposed-stake & [opts]]
  (smart-contracts/contract-send contract-key :propose-new-mind
                                  [mind-id name philosophy-hash proposed-stake]
                                  (merge opts {:gas 200000})))

(defn update-stake-cache [contract-key seat-index total-staked & [opts]]
  (smart-contracts/contract-send contract-key :update-stake-cache [seat-index total-staked]
                                  (merge opts {:gas 100000})))

;; Event functions

(defn seat-created-event-in-tx [contract-key tx-receipt]
  (smart-contracts/contract-event-in-tx contract-key :SeatCreated tx-receipt))

(defn seat-flipped-event-in-tx [contract-key tx-receipt]
  (smart-contracts/contract-event-in-tx contract-key :SeatFlipped tx-receipt))

(defn philosophy-updated-event-in-tx [contract-key tx-receipt]
  (smart-contracts/contract-event-in-tx contract-key :PhilosophyUpdated tx-receipt))
