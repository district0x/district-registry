(ns district-registry.server.contract.ds-auth
  (:require [district.server.smart-contracts :as smart-contracts]))

(defn owner [contract-key]
  (smart-contracts/contract-call contract-key :owner))

(defn authority [contract-key]
  (smart-contracts/contract-call contract-key :authority))
