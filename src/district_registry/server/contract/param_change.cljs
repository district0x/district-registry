(ns district-registry.server.contract.param-change
  (:require
    [district.server.smart-contracts :refer [contract-call]]))


(defn db [contract-addr]
  (contract-call [:param-change contract-addr] :db))


(defn key [contract-addr]
  (contract-call [:param-change contract-addr] :key))


(defn value-type [contract-addr]
  (contract-call [:param-change contract-addr] :value-type))


(defn value [contract-addr]
  (contract-call [:param-change contract-addr] :value))


(defn original-value [contract-addr]
  (contract-call [:param-change contract-addr] :original-value))


(defn applied-on [contract-addr]
  (contract-call [:param-change contract-addr] :applied-on))


(defn meta-hash [contract-addr]
  (contract-call [:param-change contract-addr] :meta-hash))