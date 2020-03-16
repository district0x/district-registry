(ns district-registry.server.contract.district0x-emails
  (:require [district.server.smart-contracts :as smart-contracts]))

(defn get-email [{:keys [:district0x-emails/address]}]
  (smart-contracts/contract-call :district0x-emails :get-email [address]))
