(ns district-registry.server.contract.challenge
  (:require [district.server.smart-contracts :as smart-contracts]))

(defn is-vote-reveal-period-active? [contract-addr]
  (smart-contracts/contract-call [:challenge contract-addr] :is-vote-reveal-period-active))

(defn is-vote-reward-claimed? [contract-addr voter]
  (smart-contracts/contract-call [:challenge contract-addr] :is-vote-reward-claimed [voter]))

(defn are-votes-reclaimed? [contract-addr voter]
  (smart-contracts/contract-call [:challenge contract-addr] :are-votes-reclaimed [voter]))

(defn is-challenger-reward-claimed? [contract-addr]
  (smart-contracts/contract-call [:challenge contract-addr] :is-challenger-reward-claimed))

(defn is-vote-commit-period-active? [contract-addr]
  (smart-contracts/contract-call [:challenge contract-addr] :is-vote-commit-period-active))

(defn is-vote-reveal-period-over? [contract-addr]
  (smart-contracts/contract-call [:challenge contract-addr] :is-vote-reveal-period-over))

(defn has-voted? [contract-addr voter]
  (smart-contracts/contract-call [:challenge contract-addr] :has-voted [voter]))

(defn status [contract-addr]
  (smart-contracts/contract-call [:challenge contract-addr] :status))

(defn vote-option-include-amount [contract-addr]
  (smart-contracts/contract-call [:challenge contract-addr] :vote-option-include-amount))

(defn vote-option-exclude-amount [contract-addr]
  (smart-contracts/contract-call [:challenge contract-addr] :vote-option-exclude-amount))

(defn winning-vote-option [contract-addr]
  (smart-contracts/contract-call [:challenge contract-addr] :winning-vote-option))

(defn winning-vote-option-amount [contract-addr]
  (smart-contracts/contract-call [:challenge contract-addr] :winning-vote-option-amount))
