(ns district-registry.ui.contract.district
  (:require
    [bignumber.core :as bn]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs.spec.alpha :as s]
    [clojure.string :as string]
    [district-registry.ui.contract.ens :as ens]
    [district.format :as format]
    [district.parsers :as parsers]
    [district.ui.logging.events :as logging]
    [district.ui.notification.events :as notification-events]
    [district.ui.smart-contracts.queries :as contract-queries]
    [district.ui.web3-accounts.queries :as account-queries]
    [district.ui.web3-tx.events :as tx-events]
    [district.ui.web3.queries :as web3-queries]
    [district.web3-utils :as web3-utils]
    [goog.string :as gstring]
    [print.foo :refer [look] :include-macros true]
    [re-frame.core :as re-frame :refer [reg-event-fx]]))

(def interceptors [re-frame/trim-v])

(def abi-resolver (js/JSON.parse "[{\"constant\":true,\"inputs\":[{\"name\":\"node\",\"type\":\"bytes32\"},{\"name\":\"key\",\"type\":\"string\"}],\"name\":\"text\",\"outputs\":[{\"name\":\"ret\",\"type\":\"string\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"node\",\"type\":\"bytes32\"},{\"name\":\"key\",\"type\":\"string\"},{\"name\":\"value\",\"type\":\"string\"}],\"name\":\"setText\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]"))

(re-frame/reg-event-fx
  ::approve-and-stake-for
  interceptors
  (fn [{:keys [db]} [{:keys [:reg-entry/address :district/name :amount]}]]
    (let [active-account (account-queries/active-account db)
          extra-data (web3-eth/contract-get-data
                       (contract-queries/instance db :district)
                       :stake-for
                       (account-queries/active-account db)
                       amount)
          tx-log-name (gstring/format "Stake %s into %s"
                                      (format/format-dnt (web3-utils/wei->eth-number amount))
                                      name)]
      {:dispatch [::tx-events/send-tx
                  {:instance (contract-queries/instance db :DNT)
                   :fn :approve-and-call
                   :args [address amount extra-data]
                   :tx-opts {:from active-account}
                   :tx-log {:name tx-log-name :related-href {:name :route/detail :params {:address address}}}
                   :tx-id {:approve-and-stake-for {:district address}}
                   :on-tx-success [::approve-and-stake-for-success]
                   :on-tx-error [::logging/error [::approve-and-stake-for]]}]})))


(re-frame/reg-event-fx
  ::approve-and-stake-for-success
  interceptors
  (constantly nil))


(re-frame/reg-event-fx
  ::unstake
  interceptors
  (fn [{:keys [db]} [{:keys [:reg-entry/address :district/name :amount]}]]
    (let [tx-log-name (gstring/format "Unstake %s from %s"
                                      (format/format-dnt (web3-utils/wei->eth-number amount))
                                      name)]
      {:dispatch [::tx-events/send-tx
                  {:instance (contract-queries/instance db :district address)
                   :fn :unstake
                   :args [amount]
                   :tx-opts {:from (account-queries/active-account db)}
                   :tx-log {:name tx-log-name :related-href {:name :route/detail :params {:address address}}}
                   :tx-id {:unstake {:district address}}
                   :on-tx-success [::unstake-success]
                   :on-tx-error [::logging/error [::unstake]]}]})))

(re-frame/reg-event-fx
  ::unstake-success
  (constantly nil))


(re-frame/reg-event-fx
  ::estimate-return-for-stake
  interceptors
  (fn [{:keys [db]} [{:keys [:amount :stake-bank] :as args}]]
    (let [amount (parsers/parse-float amount)
          args (assoc args :amount amount)]
      (when amount
        {:web3/call {:web3 (web3-queries/web3 db)
                     :fns [{:instance (contract-queries/instance db :stake-bank stake-bank)
                            :fn :estimate-return-for-stake
                            :args [(web3-utils/eth->wei amount)]
                            :on-success [::estimate-return-for-stake-success args]
                            :on-error [::logging/error [::estimate-return-for-stake]]}]}}))))


(re-frame/reg-event-fx
  ::estimate-return-for-stake-success
  interceptors
  (fn [{:keys [db]} [{:keys [:amount :stake-bank]} estimated-return]]
    {:db (assoc-in db [::estimated-return-for-stake stake-bank amount] (web3-utils/wei->eth-number estimated-return))}))


(defn build-snapshot-file
  [name network state-bank]
  (js/Blob. [(js/JSON.stringify (clj->js {
    :name name
    :network network
    :symbol "DVT"
    :strategies [
      {
        :name "erc20-balance-of"
        :params {
          :address state-bank
          :symbol "DVT"
          :decimals 18
        }
      }
    ]
    :filters {}
    :plugins {}}))]))


(re-frame/reg-event-fx
  ::setup-snapshot
  interceptors
  (fn [{:keys [db]} [{:keys [:reg-entry/address :ens-name :name :state-bank] :as data}]]
    (let [network (web3/version-network (web3-queries/web3 db))]
      {:ipfs/call {:func "add"
                   :args [(build-snapshot-file name network state-bank)]
                   :on-success [::setup-snapshot-ens {:reg-entry/address address :ens-name ens-name}]
                   :on-error [::logging/error "Failed to upload data to ipfs" ::setup-snapshot]}})))


(re-frame/reg-event-fx
  ::setup-snapshot-ens
  interceptors
  (fn [{:keys [db]} [{:keys [:reg-entry/address :ens-name] :as args} {:keys [Hash]}]]
    (let [namehash (ens/namehash ens-name)]
      {:web3/call {:web3 (web3-queries/web3 db)
                   :fns [{:instance (contract-queries/instance db :ENS)
                          :fn :resolver
                          :args [namehash]
                          :on-success [::setup-snapshot-in-resolver {:namehash namehash :text (str "ipfs://" Hash) :reg-entry/address address :ens-name ens-name}]
                          :on-error [::logging/error [::setup-snapshot-ens]]}]}})))


(re-frame/reg-event-fx
  ::setup-snapshot-in-resolver
  interceptors
  (fn [{:keys [:db]} [{:keys [:namehash :text :reg-entry/address :ens-name] :as data} resolver-addr]]
    (when (not (web3-utils/empty-address? resolver-addr))
      (let [active-account (account-queries/active-account db)
            instance (web3-eth/contract-at (web3-queries/web3 db) abi-resolver resolver-addr)]
        {:dispatch [::tx-events/send-tx
                          {:instance instance
                           :fn :setText
                           :args [namehash "snapshot" text]
                           :tx-opts {:from active-account}
                           :tx-log {:name "Setting snapshot to district" :related-href {:name :route/detail :params {:address address}}}
                           :tx-id {:setup-snapshot-for {:district address}}
                           :on-tx-success [::setup-snapshot-success {:ens-name ens-name}]
                           :on-tx-error [::logging/error [::setup-snapshot-in-resolver]]}]}))))


(re-frame/reg-event-fx
  ::setup-snapshot-success
  interceptors
  (fn [{:keys [:db]} [{:keys [:ens-name] :as data} resolver-addr]]
    {:dispatch [::check-snapshot data]}))


(re-frame/reg-event-fx
  ::check-snapshot
  interceptors
  (fn [{:keys [db]} [{:keys [:ens-name] :as args}]]
    (let [namehash (ens/namehash ens-name)]
      {:web3/call {:web3 (web3-queries/web3 db)
                   :fns [{:instance (contract-queries/instance db :ENS)
                          :fn :resolver
                          :args [namehash]
                          :on-success [::check-snapshot-in-resolver {:namehash namehash :ens-name ens-name}]
                          :on-error [::logging/error [::check-snapshot]]}]}})))


(re-frame/reg-event-fx
  ::check-snapshot-in-resolver
  interceptors
  (fn [{:keys [:db]} [{:keys [:namehash :ens-name] :as data} resolver-addr]]
    (when (not (web3-utils/empty-address? resolver-addr))
      (let [instance (web3-eth/contract-at (web3-queries/web3 db) abi-resolver resolver-addr)]
        {:web3/call {:web3 (web3-queries/web3 db)
                     :fns [{:instance instance
                            :fn :text
                            :args [namehash "snapshot"]
                            :on-success [::check-snapshot-in-resolver-success {:ens-name ens-name}]
                            :on-error [::logging/error [::check-snapshot-in-resolver]]}]}}))))


(re-frame/reg-event-fx
  ::check-snapshot-in-resolver-success
  interceptors
  (fn [{:keys [:db]} [{:keys [:ens-name]} text]]
    {:db (assoc-in db [:district-registry.ui.core/has-snapshot? ens-name] (not (string/blank? text)))}))
