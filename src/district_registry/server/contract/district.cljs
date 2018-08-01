(ns district-registry.server.contract.district
  (:require
   [district.server.smart-contracts :refer [contract-call instance contract-address]]
   [district-registry.server.contract.dnt :as dnt]
   [district-registry.shared.contract.district :refer [parse-load-district parse-load-stake]]
   [cljs-web3.eth :as web3-eth]))

(defn mint [contract-addr & [amount opts]]
  (contract-call [:district contract-addr] :mint (or amount 0) (merge {:gas 6000000} opts)))

(defn load-district [contract-addr]
  (parse-load-district
    contract-addr
    (contract-call (instance :district contract-addr) :load-district)))

(defn load-stake [contract-addr staker-addr]
  (parse-load-stake
    contract-addr
    staker-addr
    (contract-call (instance :district contract-addr) :load-stake staker-addr)))

(defn transfer-deposit [contract-addr & [opts]]
  (contract-call (instance :district contract-addr) :transfer-deposit (merge {:gas 300000} opts)))

(defn ^:private stake-data [{:keys [amount data staker]}]
  (web3-eth/contract-get-data (instance :district) :stake-for staker amount data))

(defn approve-and-stake [{:keys [amount district] :as args} & [opts]]
  (dnt/approve-and-call {:spender district
                         :amount amount
                         :extra-data (stake-data (merge {:staker (:from opts)} args))}
    (merge {:gas 6000000} opts)))

(defn unstake [contract-addr amount data & [opts]]
  (contract-call (instance :district contract-addr) :unstake amount data (merge {:gas 600000} opts)))

(defn balance-of [contract-addr owner]
  (contract-call (instance :district contract-addr) :balance-of owner))

(defn total-staked-for [contract-addr owner]
  (contract-call (instance :district contract-addr) :total-staked-for owner))
