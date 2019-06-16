(ns district-registry.server.contract.district
  (:require
   [cljs-web3.eth :as web3-eth]
   [district-registry.server.contract.dnt :as dnt]
   [district.server.smart-contracts :refer [contract-call instance contract-address]]))

(defn mint [contract-addr & [amount opts]]
  (contract-call [:district contract-addr] :mint [(or amount 0)] (merge {:gas 6000000} opts)))

(defn transfer-deposit [contract-addr & [opts]]
  (contract-call (instance :district contract-addr) :transfer-deposit (merge {:gas 300000} opts)))

(defn- stake-data [{:keys [amount staker]}]
  (web3-eth/contract-get-data (instance :district) :stake-for staker amount))

(defn approve-and-stake [{:keys [amount district] :as args} & [opts]]
  (dnt/approve-and-call {:spender district
                         :amount amount
                         :extra-data (stake-data (merge {:staker (:from opts)} args))}
    (merge {:gas 6000000} opts)))

(defn unstake [contract-addr amount & [opts]]
  (contract-call (instance :district contract-addr) :unstake [amount] (merge {:gas 600000} opts)))

(defn balance-of [contract-addr owner]
  (contract-call (instance :district contract-addr) :balance-of [owner]))