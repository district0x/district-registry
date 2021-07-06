(ns district-registry.tests.smart-contracts.utils
  (:require [cljs.core.async :refer [<! go]]
            [district-registry.server.contract.district-factory :as district-factory]
            [district.server.smart-contracts :as smart-contracts]))

(defn tx-reverted? [tx-receipt]
  (nil? tx-receipt))

(def tx-error? tx-reverted?)

(defn create-district
  "Creates a district and returns construct events args"
  [& [creator deposit meta-hash ens-name]]
  (go
    (try
      (let [tx-receipt (<! (district-factory/approve-and-create-district {:meta-hash meta-hash
                                                                          :ens-name ens-name
                                                                          :amount (str deposit)}
                                                                         {:from creator}))]
        (<! (smart-contracts/contract-event-in-tx [:district-registry :district-registry-fwd] :DistrictConstructedEvent tx-receipt)))
      (catch :default e
        false))))

(def ens-counter (atom -1))

(defn next-ens-name []
  (str (swap! ens-counter inc) "resolver.eth"))
