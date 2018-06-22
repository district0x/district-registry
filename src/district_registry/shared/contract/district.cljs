(ns district-registry.shared.contract.district
  (:require
   [bignumber.core :as bn]
   [cljs-web3.core :as web3]
   [district.web3-utils :refer [web3-time->local-date-time empty-address? wei->eth-number]]))

(def load-district-keys [:district/meta-hash
                     :district/total-supply
                     :district/total-minted
                     :district/token-id-start])

(defn parse-load-district [contract-addr district & [{:keys [:parse-dates?]}]]
  (when district
    (let [district (zipmap load-district-keys district)]
      (-> district
        (assoc :reg-entry/address contract-addr)
        (update :district/meta-hash web3/to-ascii)
        (update :district/total-supply bn/number)
        (update :district/total-minted bn/number)
        (update :district/token-id-start bn/number)))))
