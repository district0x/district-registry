(ns district-registry.server.contract.stake-bank
  (:require
    [district-registry.shared.utils :refer [vote-option->num]]
    [district.server.smart-contracts :refer [contract-call instance contract-address]]
    [print.foo :refer [look] :include-macros true]))


(defn total-staked-for [contract-addr user]
  (contract-call [:stake-bank contract-addr] :total-staked-for [user]))


(defn total-staked [contract-addr]
  (contract-call [:stake-bank contract-addr] :total-staked))


(defn last-staked-for [contract-addr user]
  (contract-call [:stake-bank contract-addr] :last-staked-for [user]))