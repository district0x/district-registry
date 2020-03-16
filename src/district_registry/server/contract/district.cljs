(ns district-registry.server.contract.district
  (:require [cljs-web3-next.eth :as web3-eth]
            [cljs-web3-next.utils :as web3-utils]
            [district-registry.server.contract.dnt :as dnt]
            [district.server.smart-contracts :as smart-contracts]
            [district.server.web3 :refer [web3]]))

(defn stake-bank [contract-addr]
  (smart-contracts/contract-call [:district contract-addr] :stake-bank))

(defn meta-hash [contract-addr]
  (smart-contracts/contract-call [:district contract-addr] :meta-hash))

(defn stake-for-data [{:keys [:user :amount]}]
  (web3-eth/encode-abi (smart-contracts/instance :district) :stake-for [user amount]))

(defn approve-and-stake-for [contract-addr {:keys [:amount] :as args} & [opts]]
  (dnt/approve-and-call {:spender contract-addr
                         :amount (str amount)
                         :extra-data (stake-for-data (merge {:challenger (:from opts)} args))}
                        (merge {:gas 6000000} opts)))

(defn unstake [contract-addr {:keys [:amount]} & [opts]]
  (smart-contracts/contract-send [:district contract-addr] :unstake [amount] (merge {:gas 2200000} opts)))

(defn set-meta-hash [contract-addr {:keys [:meta-hash]} & [opts]]
  (smart-contracts/contract-send [:district contract-addr] :set-meta-hash [(web3-utils/to-hex @web3 meta-hash)] (merge {:gas 1000000} opts)))
