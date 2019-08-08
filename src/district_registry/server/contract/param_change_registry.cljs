(ns district-registry.server.contract.param-change-registry
  (:require [district.server.smart-contracts :refer [contract-call create-event-filter contract-event-in-tx]]))

(defn apply-param-change [param-change-addr & [opts]]
  (contract-call :param-change-registry-fwd :apply-param-change [param-change-addr] (merge opts {:gas 1000000})))

(defn param-change-constructed-event-in-tx [tx-hash & args]
  (apply contract-event-in-tx tx-hash :param-change-registry-fwd :ParamChangeConstructedEvent args))

(defn param-change-applied-event-in-tx [tx-hash & args]
  (apply contract-event-in-tx tx-hash :param-change-registry-fwd :ParamChangeAppliedEvent args))