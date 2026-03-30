(ns district-registry.ui.contract.council
  (:require
    [bignumber.core :as bn]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs-web3.utils :as web3-utils]
    [district.ui.logging.events :as logging]
    [district.ui.notification.events :as notification-events]
    [district.ui.smart-contracts.queries :as contract-queries]
    [district.ui.web3-accounts.queries :as account-queries]
    [district.ui.web3-tx.events :as tx-events]
    [district.web3-utils :as web3-utils]
    [re-frame.core :as re-frame]))

(def interceptors [re-frame/trim-v])

;;
;; Events for staking on council seats
;;

(re-frame/reg-event-fx
  ::stake-on-seat
  interceptors
  (fn [{:keys [:db]} [{:keys [:seat-index :amount :seat-name] :as data}]]
    (let [active-account (account-queries/active-account db)
          tx-name (str "Stake on " seat-name)
          tx-opts {:from active-account}]
      {:dispatch [::tx-events/send-tx
                  {:instance (contract-queries/instance db :council-stake-bank)
                   :fn :stake-on-seat
                   :args [seat-index amount]
                   :tx-opts tx-opts
                   :tx-id {:stake-on-seat {:seat-index seat-index}}
                   :on-tx-success-n [[::logging/info
                                      (str tx-name " tx success")
                                      ::stake-on-seat]
                                     [::notification-events/show
                                      (str "Successfully staked on " seat-name "!")]]
                   :on-tx-error [::logging/error
                                (str tx-name " tx error")
                                {:user {:id active-account}
                                 :seat-index seat-index
                                 :amount amount}
                                ::stake-on-seat]}]})))

(re-frame/reg-event-fx
  ::stake-on-seat-success
  interceptors
  (fn [{:keys [:db]}]
    {:db db}))

(re-frame/reg-event-fx
  ::unstake-from-seat
  interceptors
  (fn [{:keys [:db]} [{:keys [:seat-index :amount :seat-name] :as data}]]
    (let [active-account (account-queries/active-account db)
          tx-name (str "Unstake from " seat-name)
          tx-opts {:from active-account}]
      {:dispatch [::tx-events/send-tx
                  {:instance (contract-queries/instance db :council-stake-bank)
                   :fn :unstake-from-seat
                   :args [seat-index amount]
                   :tx-opts tx-opts
                   :tx-id {:unstake-from-seat {:seat-index seat-index}}
                   :on-tx-success-n [[::logging/info
                                      (str tx-name " tx success")
                                      ::unstake-from-seat]
                                     [::notification-events/show
                                      (str "Successfully unstaked from " seat-name "!")]]
                   :on-tx-error [::logging/error
                                (str tx-name " tx error")
                                {:user {:id active-account}
                                 :seat-index seat-index
                                 :amount amount}
                                ::unstake-from-seat]}]})))

(re-frame/reg-event-fx
  ::unstake-from-seat-success
  interceptors
  (fn [{:keys [:db]}]
    {:db db}))

(re-frame/reg-event-fx
  ::move-stake
  interceptors
  (fn [{:keys [:db]} [{:keys [:from-seat :to-seat :amount :from-name :to-name] :as data}]]
    (let [active-account (account-queries/active-account db)
          tx-name (str "Move stake from " from-name " to " to-name)
          tx-opts {:from active-account}]
      {:dispatch [::tx-events/send-tx
                  {:instance (contract-queries/instance db :council-stake-bank)
                   :fn :move-stake
                   :args [from-seat to-seat amount]
                   :tx-opts tx-opts
                   :tx-id {:move-stake {:from-seat from-seat :to-seat to-seat}}
                   :on-tx-success-n [[::logging/info
                                      (str tx-name " tx success")
                                      ::move-stake]
                                     [::notification-events/show
                                      (str "Successfully moved stake from " from-name " to " to-name "!")]]
                   :on-tx-error [::logging/error
                                (str tx-name " tx error")
                                {:user {:id active-account}
                                 :from-seat from-seat
                                 :to-seat to-seat
                                 :amount amount}
                                ::move-stake]}]})))

(re-frame/reg-event-fx
  ::move-stake-success
  interceptors
  (fn [{:keys [:db]}]
    {:db db}))

;;
;; Approve DNT for staking
;;

(re-frame/reg-event-fx
  ::approve-dnt
  interceptors
  (fn [{:keys [:db]} [{:keys [:amount :on-success] :as data}]]
    (let [active-account (account-queries/active-account db)
          stake-bank-address (contract-queries/contract-address db :council-stake-bank)
          tx-opts {:from active-account}]
      {:dispatch [::tx-events/send-tx
                  {:instance (contract-queries/instance db :dnt)
                   :fn :approve
                   :args [stake-bank-address amount]
                   :tx-opts tx-opts
                   :tx-id {:approve-dnt {}}
                   :on-tx-success-n [[::logging/info
                                      "DNT approval tx success"
                                      ::approve-dnt]
                                     [::notification-events/show
                                      "DNT approved! You can now stake."]
                                     (when on-success on-success)]
                   :on-tx-error [::logging/error
                                "DNT approval tx error"
                                {:user {:id active-account}
                                 :amount amount}
                                ::approve-dnt]}]})))

(re-frame/reg-event-fx
  ::approve-dnt-success
  interceptors
  (fn [{:keys [:db]}]
    {:db db}))

;;
;; Subscriptions
;;

(re-frame/reg-sub
  ::seat-stake-for-user
  (fn [db [_ seat-index]]
    ;; This would query the user's stake for a specific seat
    ;; For now, returning 0 as placeholder
    ;; In production, this should query the GraphQL API or blockchain
    0))

(re-frame/reg-sub
  ::user-total-staked
  (fn [db _]
    ;; This would query the user's total stake across all seats
    ;; For now, returning 0 as placeholder
    0))
