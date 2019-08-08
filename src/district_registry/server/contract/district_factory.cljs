(ns district-registry.server.contract.district-factory
  (:require
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [district-registry.server.contract.dnt :as dnt]
    [district.server.smart-contracts :refer [contract-call instance contract-address]]))

(defn create-district-data [{:keys [:creator :meta-hash :dnt-weight :aragon-id]}]
  (web3-eth/contract-get-data (instance :district-factory) :create-district creator (web3/to-hex meta-hash) dnt-weight aragon-id))

(defn approve-and-create-district [{:keys [:amount] :as args} & [opts]]
  (dnt/approve-and-call {:spender (contract-address :district-factory)
                         :amount amount
                         :extra-data (create-district-data (merge {:creator (:from opts)} args))}
                        (merge {:gas 10000000} opts)))