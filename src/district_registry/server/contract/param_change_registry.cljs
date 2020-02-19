(ns district-registry.server.contract.param-change-registry
  (:require [district.server.smart-contracts :as smart-contracts]))

(defn param-change-constructed-event-in-tx [tx-hash]
  (smart-contracts/contract-event-in-tx tx-hash :param-change-registry-fwd :ParamChangeConstructedEvent))

(defn apply-param-change [param-change-address & [opts]]
  (smart-contracts/contract-send [:param-change-registry :param-change-registry-fwd]
                                 :apply-param-change [param-change-address] (merge {:ignore-forward? false :gas 700000} opts)))
