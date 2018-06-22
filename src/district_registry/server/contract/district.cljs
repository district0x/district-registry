(ns district-registry.server.contract.district
  (:require
   [district.server.smart-contracts :refer [contract-call instance contract-address]]
   [district-registry.shared.contract.district :refer [parse-load-district]]))

(defn mint [contract-addr & [amount opts]]
  (contract-call [:district contract-addr] :mint (or amount 0) (merge {:gas 6000000} opts)))

(defn load-district [contract-addr]
  (parse-load-district contract-addr (contract-call (instance :district contract-addr) :load-district)))

(defn transfer-deposit [contract-addr & [opts]]
  (contract-call (instance :district contract-addr) :transfer-deposit (merge {:gas 300000} opts)))



