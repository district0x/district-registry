(ns district-registry.ui.contract.district
  (:require
    [bignumber.core :as bn]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs.spec.alpha :as s]
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