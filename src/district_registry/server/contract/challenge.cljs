(ns district-registry.server.contract.challenge
  (:require
    [district-registry.shared.utils :refer [vote-option->num]]
    [district.server.smart-contracts :refer [contract-call instance contract-address]]
    [print.foo :refer [look] :include-macros true]))


(defn is-vote-reveal-period-active? [contract-addr]
  (contract-call [:challenge contract-addr] :is-vote-reveal-period-active))


(defn is-vote-revealed? [contract-addr voter]
  (contract-call [:challenge contract-addr] :is-vote-revealed [voter]))


(defn is-vote-reward-claimed? [contract-addr voter]
  (contract-call [:challenge contract-addr] :is-vote-reward-claimed [voter]))


(defn are-votes-reclaimed? [contract-addr voter]
  (contract-call [:challenge contract-addr] :are-votes-reclaimed [voter]))


(defn is-challenger-reward-claimed? [contract-addr]
  (contract-call [:challenge contract-addr] :is-challenger-reward-claimed))

(defn is-creator-reward-claimed? [contract-addr]
  (contract-call [:challenge contract-addr] :is-creator-reward-claimed))


(defn is-challenge-period-active? [contract-addr]
  (contract-call [:challenge contract-addr] :is-challenge-period-active))


(defn is-whitelisted? [contract-addr]
  (contract-call [:challenge contract-addr] :is-whitelisted))


(defn is-vote-commit-period-active? [contract-addr]
  (contract-call [:challenge contract-addr] :is-vote-commit-period-active))


(defn is-vote-reveal-period-over? [contract-addr]
  (contract-call [:challenge contract-addr] :is-vote-reveal-period-over))


(defn is-winning-option-include? [contract-addr]
  (contract-call [:challenge contract-addr] :is-winning-option-include))


(defn has-voted? [contract-addr voter]
  (contract-call [:challenge contract-addr] :has-voted [voter]))


(defn voted-winning-vote-option? [contract-addr voter]
  (contract-call [:challenge contract-addr] :voted-winning-vote-option [voter]))


(defn status [contract-addr]
  (contract-call [:challenge contract-addr] :status))


(defn challenger-reward [contract-addr]
  (contract-call [:challenge contract-addr] :challenger-reward))


(defn creator-reward [contract-addr]
  (contract-call [:challenge contract-addr] :creator-reward))


(defn vote-option-include-voter-amount [contract-addr voter]
  (contract-call [:challenge contract-addr] :vote-option-include-voter-amount [voter]))


(defn vote-option-exclude-voter-amount [contract-addr voter]
  (contract-call [:challenge contract-addr] :vote-option-exclude-voter-amount [voter]))


(defn vote-reward [contract-addr voter]
  (contract-call [:challenge contract-addr] :vote-reward [voter]))


(defn is-blacklisted? [contract-addr]
  (contract-call [:challenge contract-addr] :is-blacklisted))


(defn vote-option-include-amount [contract-addr]
  (contract-call [:challenge contract-addr] :vote-option-include-amount))


(defn vote-option-exclude-amount [contract-addr]
  (contract-call [:challenge contract-addr] :vote-option-exclude-amount))


(defn winning-vote-option [contract-addr]
  (contract-call [:challenge contract-addr] :winning-vote-option))


(defn winning-vote-option-amount [contract-addr]
  (contract-call [:challenge contract-addr] :winning-vote-option-amount))