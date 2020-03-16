(ns district-registry.server.contract.stake-bank
  (:require [district.server.smart-contracts :as smart-contracts]))

(defn total-staked-for [contract-addr user]
  (smart-contracts/contract-call [:stake-bank contract-addr] :total-staked-for [user]))

(defn total-staked [contract-addr]
  (smart-contracts/contract-call [:stake-bank contract-addr] :total-staked))

(defn last-staked-for [contract-addr user]
  (smart-contracts/contract-call [:stake-bank contract-addr] :last-staked-for [user]))
