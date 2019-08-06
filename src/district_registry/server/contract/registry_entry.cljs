(ns district-registry.server.contract.registry-entry
  (:require
    [bignumber.core :as bn]
    [camel-snake-kebab.core :as cs :include-macros true]
    [cljs-solidity-sha3.core :refer [solidity-sha3]]
    [cljs-web3.eth :as web3-eth]
    [district-registry.server.contract.dnt :as dnt]
    [district-registry.shared.utils :refer [vote-option->num]]
    [district.server.smart-contracts :refer [contract-call instance contract-address]]
    [district.shared.async-helpers :refer [promise->]]
    [print.foo :refer [look] :include-macros true]))


(defn registry [contract-addr]
  (contract-call [:district contract-addr] :registry))


(defn registry-token [contract-addr]
  (contract-call [:district contract-addr] :registry-token))


(defn creator [contract-addr]
  (contract-call [:district contract-addr] :creator))


(defn deposit [contract-addr]
  (contract-call [:district contract-addr] :deposit))


(defn challenge-period-end [contract-addr]
  (contract-call [:district contract-addr] :challenge-period-end))


(defn challenges [contract-addr]
  (contract-call [:district contract-addr] :challenges))


(defn is-challenge-period-active? [contract-addr]
  (contract-call [:district contract-addr] :is-challenge-period-active))


(defn is-challengeable? [contract-addr]
  (contract-call [:district contract-addr] :is-challengeable))


(defn current-challenge-index [contract-addr]
  (contract-call [:district contract-addr] :current-challenge-index))


(defn get-challenge [contract-addr]
  (contract-call [:district contract-addr] :get-challenge))


(defn current-challenge [contract-addr]
  (contract-call [:district contract-addr] :current-challenge))


(defn create-challenge [contract-addr {:keys [:challenger :meta-hash]} & [opts]]
  (contract-call (instance :district contract-addr) :create-challenge [challenger meta-hash] (merge {:gas 1200000} opts)))


(defn create-challenge-data [{:keys [:challenger :meta-hash]}]
  (web3-eth/contract-get-data (instance :district) :create-challenge challenger meta-hash))


(defn approve-and-create-challenge [contract-addr {:keys [:amount] :as args} & [opts]]
  (dnt/approve-and-call {:spender contract-addr
                         :amount amount
                         :extra-data (create-challenge-data (merge {:challenger (:from opts)} args))}
                        (merge {:gas 6000000} opts)))


(defn commit-vote [contract-addr {:keys [:voter :amount :vote-option :salt]} & [opts]]
  (contract-call (instance :district contract-addr)
                 :commit-vote
                 [voter
                  (bn/number amount)
                  (solidity-sha3 (vote-option->num vote-option) salt)]
                 (merge {:gas 1200000} opts)))


(defn commit-vote-data [{:keys [:voter :amount :vote-option :salt]}]
  (web3-eth/contract-get-data (instance :district) :commit-vote voter (bn/number amount) (solidity-sha3 (vote-option->num vote-option) salt)))


(defn approve-and-commit-vote [contract-addr {:keys [:amount] :as args} & [opts]]
  (dnt/approve-and-call {:spender contract-addr
                         :amount amount
                         :extra-data (commit-vote-data (merge {:voter (:from opts)} args))}
                        (merge opts {:gas 2200000})))


(defn reveal-vote [contract-addr {:keys [:vote-option :salt]} & [opts]]
  (contract-call (instance :district contract-addr) :reveal-vote [(vote-option->num vote-option) salt] (merge {:gas 500000} opts)))


(defn claim-reward [contract-addr & [opts]]
  (contract-call (instance :district contract-addr) :claim-reward [(:from opts)] (merge {:gas 500000} opts)))
