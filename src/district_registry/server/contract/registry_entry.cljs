(ns district-registry.server.contract.registry-entry
  (:require [cljs-web3-next.eth :as web3-eth]
            [cljs-web3-next.utils :as web3-utils]
            [district-registry.server.contract.dnt :as dank-token]
            [district-registry.shared.utils :refer [vote-option->num]]
            [district.server.smart-contracts :as smart-contracts]
            [district.server.web3 :refer [web3]]))

(defn is-challenge-period-active? [contract-addr]
  (smart-contracts/contract-call [:district contract-addr] :is-challenge-period-active))

(defn is-challengeable? [contract-addr]
  (smart-contracts/contract-call [:district contract-addr] :is-challengeable))

(defn current-challenge [contract-addr]
  (smart-contracts/contract-call [:district contract-addr] :current-challenge))

(defn claim-reward [contract-addr & [opts]]
  (smart-contracts/contract-send (smart-contracts/instance :district contract-addr) :claim-reward [(:from opts)] (merge {:gas 500000} opts)))

(defn commit-vote-data [{:keys [:voter :amount :vote-option :salt]}]
  (web3-eth/encode-abi (smart-contracts/instance :district) :commit-vote [voter amount (web3-utils/solidity-sha3 @web3 (vote-option->num vote-option) salt)]))

(defn approve-and-commit-vote [contract-addr {:keys [:amount] :as args} & [opts]]
  (dank-token/approve-and-call {:spender contract-addr
                                :amount (str amount)
                                :extra-data (commit-vote-data (merge {:voter (:from opts)} args))}
                               (merge {:gas 1200000} opts)))

(defn reveal-vote [contract-addr {:keys [:vote-option :salt]} & [opts]]
  (smart-contracts/contract-send (smart-contracts/instance :district contract-addr) :reveal-vote [(vote-option->num vote-option) salt] (merge {:gas 500000} opts)))

(defn create-challenge-data [{:keys [:challenger :meta-hash]}]
  (web3-eth/encode-abi (smart-contracts/instance :district) :create-challenge [challenger (web3-utils/from-ascii @web3 meta-hash)]))

(defn approve-and-create-challenge [contract-addr {:keys [:amount] :as args} & [opts]]
  (dank-token/approve-and-call {:spender contract-addr
                                :amount (str amount)
                                :extra-data (create-challenge-data (merge {:challenger (:from opts)} args))}
                               (merge {:gas 6000000} opts)))
