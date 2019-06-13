(ns district-registry.ui.contract.district
  (:require
    [bignumber.core :as bn]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs.spec.alpha :as s]
    [district.ui.logging.events :as logging]
    [district.ui.notification.events :as notification-events]
    [district.ui.smart-contracts.queries :as contract-queries]
    [district.ui.web3-accounts.queries :as account-queries]
    [district.ui.web3-tx.events :as tx-events]
    [district0x.re-frame.spec-interceptors :as spec-interceptors]
    [goog.string :as gstring]
    [print.foo :refer [look] :include-macros true]
    [re-frame.core :as re-frame :refer [reg-event-fx]]))

(def interceptors [re-frame/trim-v])

(re-frame/reg-event-fx
  ::approve-and-stake-for
  interceptors
  (fn [{:keys [db]} [{:keys [address dnt]}]]
    (let [active-account (account-queries/active-account db)
          extra-data (web3-eth/contract-get-data
                       (contract-queries/instance db :district)
                       :stake-for
                       (account-queries/active-account db)
                       dnt)]
      {:dispatch [::tx-events/send-tx
                  {:instance (contract-queries/instance db :DNT)
                   :fn :approve-and-call
                   :args [address
                          dnt
                          extra-data]
                   :tx-opts {:from active-account
                             :gas 6000000}
                   :tx-id {:approve-and-stake-for {:district address}}
                   :on-tx-success [::approve-and-stake-for-success]
                   :on-tx-hash-error [::logging/error [::approve-and-stake-for]]
                   :on-tx-error [::logging/error [::approve-and-stake-for]]}]})))

(re-frame/reg-event-fx
  ::approve-and-stake-for-success
  interceptors
  (constantly nil))

(re-frame/reg-event-fx
  ::unstake
  interceptors
  (fn [{:keys [db]} [{:keys [address dnt]}]]
    {:dispatch [::tx-events/send-tx
                {:instance (contract-queries/instance db :district address)
                 :fn :unstake
                 :args [(print.foo/look dnt)]
                 :tx-opts {:from (account-queries/active-account db)
                           :gas 6000000}
                 :tx-id {:unstake {:district address}}
                 :on-tx-success [::unstake-success]
                 :on-tx-hash-error [::logging/error [::unstake]]
                 :on-tx-error [::logging/error [::unstake]]}]}))

(re-frame/reg-event-fx
  ::unstake-success
  (constantly nil))
