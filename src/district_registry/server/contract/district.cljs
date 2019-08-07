(ns district-registry.server.contract.district
  (:require
    [cljs-web3.eth :as web3-eth]
    [district-registry.server.contract.dnt :as dnt]
    [district.server.smart-contracts :refer [contract-call instance contract-address]]))


(defn stake-bank [contract-addr]
  (contract-call [:district contract-addr] :stake-bank))


(defn kit-district [contract-addr]
  (contract-call [:district contract-addr] :kit-district))


(defn meta-hash [contract-addr]
  (contract-call [:district contract-addr] :meta-hash))


(defn stake-for-data [{:keys [:user :amount]}]
  (web3-eth/contract-get-data (instance :district) :stake-for user amount))


(defn approve-and-stake-for [contract-addr {:keys [:amount] :as args} & [opts]]
  (dnt/approve-and-call {:spender contract-addr
                         :amount amount
                         :extra-data (stake-for-data (merge {:challenger (:from opts)} args))}
                        (merge {:gas 6000000} opts)))


(defn unstake [contract-addr {:keys [:amount]} & [opts]]
  (contract-call [:district contract-addr] :unstake [amount] (merge {:gas 2200000} opts)))


(defn set-meta-hash [contract-addr {:keys [:meta-hash]} & [opts]]
  (contract-call [:district contract-addr] :set-meta-hash [meta-hash] (merge {:gas 1000000} opts)))
