(ns district-registry.shared.contract.district
  (:require
   [bignumber.core :as bn]
   [cljs-web3.core :as web3]
   [district.web3-utils :refer [web3-time->local-date-time empty-address? wei->eth-number]]))

(def load-district-keys
  [:district/info-hash
   :district/dnt-weight
   :district/dnt-staked
   :district/total-supply])

(defn parse-load-district [contract-addr district & [{:keys [:parse-dates?]}]]
  (when district
    (let [district (zipmap load-district-keys district)]
      (-> district
        (assoc :reg-entry/address contract-addr)
        (update :district/info-hash web3/to-ascii)
        (update :district/dnt-weight bn/number)
        (update :district/dnt-staked bn/number)
        (update :district/total-supply bn/number)
        ))))
