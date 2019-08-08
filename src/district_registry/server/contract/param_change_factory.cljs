(ns district-registry.server.contract.param-change-factory
  (:require
    [camel-snake-kebab.core :as cs :include-macros true]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [district-registry.server.contract.dnt :as dnt]
    [district.server.smart-contracts :refer [contract-call instance contract-address]]))

(defn create-param-change-data [{:keys [:creator :meta-hash :db :key :value]}]
  (web3-eth/contract-get-data (instance :param-change-factory) :create-param-change creator db (cs/->camelCaseString key) value meta-hash))

(defn approve-and-create-param-change [{:keys [:amount] :as args} & [opts]]
  (dnt/approve-and-call {:spender (contract-address :param-change-factory)
                         :amount amount
                         :extra-data (create-param-change-data (merge {:creator (:from opts)} args))}
                        (merge {:gas 4000000} opts)))