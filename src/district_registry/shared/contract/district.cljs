(ns district-registry.shared.contract.district
  (:require
   [bignumber.core :as bn]
   [cljs-web3.core :as web3]
   [district.web3-utils :refer [web3-time->local-date-time empty-address? wei->eth-number]]))

(def load-district-keys
  [:district/meta-hash
   :district/dnt-weight
   :district/dnt-staked
   :district/total-supply])

(def load-stake-keys
  [:stake/dnt
   :stake/tokens])

(defn parse-load-district [contract-addr district & [{:keys [:parse-dates?]}]]
  (when district
    (let [district (zipmap load-district-keys district)]
      (-> district
        (assoc :reg-entry/address contract-addr)
        (update :district/meta-hash web3/to-ascii)
        (update :district/dnt-weight bn/number)
        (update :district/dnt-staked bn/number)
        (update :district/total-supply bn/number)
        ))))

(defn parse-load-stake [contract-addr staker-addr stake & [{:keys [:parse-dates?]}]]
  (when stake
    (let [stake (zipmap load-stake-keys stake)]
      (-> stake
        (assoc :reg-entry/address contract-addr)
        (assoc :stake/staker staker-addr)
        (update :stake/dnt bn/number)
        (update :stake/tokens bn/number)))))
