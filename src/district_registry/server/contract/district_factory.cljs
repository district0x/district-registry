(ns district-registry.server.contract.district-factory
  (:require
   [cljs-web3.eth :as web3-eth]
   [district.server.smart-contracts :refer [contract-call instance contract-address]]
   [district-registry.server.contract.dnt :as dank-token]))

(defn create-district [{:keys [:creator :meta-hash :total-supply]} & [opts]]
  (contract-call :district-factory :create-district creator meta-hash total-supply (merge {:gas 3000000} opts)))

(defn create-district-data [{:keys [:creator :meta-hash :total-supply]}]
  (web3-eth/contract-get-data (instance :district-factory) :create-district creator meta-hash total-supply))

(defn approve-and-create-district [{:keys [:amount] :as args} & [opts]]
  (dank-token/approve-and-call {:spender (contract-address :district-factory)
                                :amount amount
                                :extra-data (create-district-data (merge {:creator (:from opts)} args))}
    (merge {:gas 1200000} opts)))
