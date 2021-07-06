(ns district-registry.server.contract.district-factory
  (:require [cljs-web3-next.eth :as web3-eth]
            [cljs-web3-next.utils :as web3-utils]
            [cljs.nodejs :as nodejs]
            [district-registry.server.contract.dnt :as dnt]
            [district.server.smart-contracts :as smart-contracts]
            [district.server.web3 :refer [web3]]))

(defonce namehash (nodejs/require "eth-ens-namehash"))

(defn create-district-data [{:keys [:creator :meta-hash :ens-name]}]
  (web3-eth/encode-abi (smart-contracts/instance :district-factory) :create-district [creator (web3-utils/to-hex @web3 meta-hash) (namehash.hash ens-name) ens-name]))

(defn approve-and-create-district [{:keys [:amount] :as args} & [opts]]
  (dnt/approve-and-call {:spender (smart-contracts/contract-address :district-factory)
                         :amount amount
                         :extra-data (create-district-data (merge {:creator (:from opts)} args))}
                        (merge {:gas 10000000} opts)))
