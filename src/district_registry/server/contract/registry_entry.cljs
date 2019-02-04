(ns district-registry.server.contract.registry-entry
  (:require
   [bignumber.core :as bn]
   [camel-snake-kebab.core :as cs :include-macros true]
   [cljs-solidity-sha3.core :refer [solidity-sha3]]
   [cljs-web3.eth :as web3-eth]
   [district.server.smart-contracts :refer [contract-call instance contract-address]]
   [district-registry.server.contract.dnt :as dnt]
   [district-registry.server.contract.minime-token :as minime-token]
   [district-registry.shared.contract.registry-entry :refer [parse-status parse-load-registry-entry
                                                             parse-load-challenge
                                                             parse-load-vote vote-option->num]]))

(defn registry [contract-addr]
  (contract-call [:district contract-addr] :registry))

(defn status
  [contract-addr]
  (parse-status (contract-call [:district contract-addr] :status)))

(defn load-registry-entry [contract-addr]
  (parse-load-registry-entry
    contract-addr
    (contract-call (instance :district contract-addr) :load-registry-entry)))

(defn load-challenge [contract-addr challenge-index]
  (parse-load-challenge
    contract-addr
    challenge-index
    (contract-call (instance :district contract-addr) :load-challenge challenge-index)))

(defn create-challenge [contract-addr {:keys [:challenger :meta-hash]} & [opts]]
  (contract-call (instance :district contract-addr) :create-challenge challenger meta-hash (merge {:gas 1200000} opts)))

(defn create-challenge-data [{:keys [:challenger :meta-hash]}]
  (web3-eth/contract-get-data (instance :district) :create-challenge challenger meta-hash))

(defn approve-and-create-challenge [contract-addr {:keys [:amount] :as args} & [opts]]
  (dnt/approve-and-call {:spender contract-addr
                         :amount amount
                         :extra-data (create-challenge-data (merge {:challenger (:from opts)} args))}
    (merge {:gas 6000000} opts)))

(defn commit-vote [contract-addr {:keys [:index :voter :amount :vote-option :salt]} & [opts]]
  (contract-call (instance :district contract-addr)
    :commit-vote
    (bn/number index)
    voter
    (bn/number amount)
    (solidity-sha3 (vote-option->num vote-option) salt)
    (merge {:gas 1200000} opts)))

(defn commit-vote-data [{:keys [:index :voter :amount :vote-option :salt]}]
  (web3-eth/contract-get-data (instance :district) :commit-vote index voter (bn/number amount) (solidity-sha3 (vote-option->num vote-option) salt)))

(defn approve-and-commit-vote [contract-addr {:keys [:amount] :as args} & [opts]]
  (dnt/approve-and-call {:spender contract-addr
                         :amount amount
                         :extra-data (commit-vote-data (merge {:voter (:from opts)} args))}
    (merge opts {:gas 1200000})))

(defn reveal-vote [contract-addr {:keys [:index :vote-option :salt]} & [opts]]
  (contract-call (instance :district contract-addr) :reveal-vote index (vote-option->num vote-option) salt (merge {:gas 500000} opts)))

(defn claim-vote-reward [contract-addr {:keys [:index]} & [opts]]
  (contract-call (instance :district contract-addr) :claim-vote-reward index (:from opts) (merge {:gas 500000} opts)))

(defn load-vote [contract-addr challenge-index voter-address]
  (parse-load-vote
    contract-addr
    challenge-index
    voter-address
    (contract-call (instance :district contract-addr) :load-vote challenge-index voter-address)))

(defn vote-reward [contract-addr voter-address]
  (contract-call (instance :district contract-addr) :vote-reward voter-address))

(defn claim-challenge-reward [contract-addr & [opts]]
  (contract-call (instance :district contract-addr) :claim-challenge-reward (merge {:gas 500000} opts)))

