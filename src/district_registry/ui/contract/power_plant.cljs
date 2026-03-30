(ns district-registry.ui.contract.power-plant
  (:require
    [district.ui.logging.events :as logging]
    [district.ui.notification.events :as notification-events]
    [district.ui.smart-contracts.queries :as contract-queries]
    [district.ui.web3-accounts.queries :as account-queries]
    [district.ui.web3-tx.events :as tx-events]
    [re-frame.core :as re-frame]))

;;
;; Power Plant Contract Events
;;
;; These events handle interactions with the PowerPlant.sol contract
;; for bounties, grants, and treasury management.
;;

;;
;; Bounty Functions
;;

(re-frame/reg-event-fx
  ::assign-bounty
  (fn [{:keys [db]} [_ {:keys [bounty-id]}]]
    (let [active-account (account-queries/active-account db)]
      {:dispatch [::logging/info
                  [::assign-bounty]
                  {:bounty-id bounty-id
                   :assignee active-account}]
       :district.ui.web3-tx/send-tx
       {:instance (contract-queries/instance db :power-plant)
        :fn [:assign-bounty bounty-id active-account]
        :args {:from active-account}
        :tx-opts {:from active-account
                  :gas 150000}
        :on-tx-receipt [::assign-bounty-receipt bounty-id]
        :on-tx-receipt-n [[::notification-events/show
                           {:message (str "Bounty #" bounty-id " assigned to you!")
                            :type :success}]]
        :on-error [::assign-bounty-error]}})))

(re-frame/reg-event-fx
  ::assign-bounty-receipt
  (fn [{:keys [db]} [_ bounty-id tx-receipt]]
    {:dispatch [::logging/info
                [::assign-bounty-receipt]
                {:bounty-id bounty-id
                 :tx-receipt tx-receipt}]}))

(re-frame/reg-event-fx
  ::assign-bounty-error
  (fn [{:keys [db]} [_ error]]
    {:dispatch-n [[::logging/error
                   [::assign-bounty-error]
                   {:error error}]
                  [::notification-events/show
                   {:message "Error assigning bounty. You may not have permission."
                    :type :error}]]}))

(re-frame/reg-event-fx
  ::mark-bounty-complete
  (fn [{:keys [db]} [_ {:keys [bounty-id]}]]
    (let [active-account (account-queries/active-account db)]
      {:dispatch [::logging/info
                  [::mark-bounty-complete]
                  {:bounty-id bounty-id
                   :from active-account}]
       :district.ui.web3-tx/send-tx
       {:instance (contract-queries/instance db :power-plant)
        :fn [:mark-bounty-complete bounty-id]
        :args {:from active-account}
        :tx-opts {:from active-account
                  :gas 120000}
        :on-tx-receipt [::mark-bounty-complete-receipt bounty-id]
        :on-tx-receipt-n [[::notification-events/show
                           {:message (str "Bounty #" bounty-id " marked as complete!")
                            :type :success}]]
        :on-error [::mark-bounty-complete-error]}})))

(re-frame/reg-event-fx
  ::mark-bounty-complete-receipt
  (fn [{:keys [db]} [_ bounty-id tx-receipt]]
    {:dispatch [::logging/info
                [::mark-bounty-complete-receipt]
                {:bounty-id bounty-id
                 :tx-receipt tx-receipt}]}))

(re-frame/reg-event-fx
  ::mark-bounty-complete-error
  (fn [{:keys [db]} [_ error]]
    {:dispatch-n [[::logging/error
                   [::mark-bounty-complete-error]
                   {:error error}]
                  [::notification-events/show
                   {:message "Error marking bounty complete. You may not be the assignee."
                    :type :error}]]}))

(re-frame/reg-event-fx
  ::claim-bounty
  (fn [{:keys [db]} [_ {:keys [bounty-id]}]]
    (let [active-account (account-queries/active-account db)]
      {:dispatch [::logging/info
                  [::claim-bounty]
                  {:bounty-id bounty-id
                   :from active-account}]
       :district.ui.web3-tx/send-tx
       {:instance (contract-queries/instance db :power-plant)
        :fn [:claim-bounty bounty-id]
        :args {:from active-account}
        :tx-opts {:from active-account
                  :gas 180000}
        :on-tx-receipt [::claim-bounty-receipt bounty-id]
        :on-tx-receipt-n [[::notification-events/show
                           {:message (str "Bounty #" bounty-id " reward claimed!")
                            :type :success}]]
        :on-error [::claim-bounty-error]}})))

(re-frame/reg-event-fx
  ::claim-bounty-receipt
  (fn [{:keys [db]} [_ bounty-id tx-receipt]]
    {:dispatch [::logging/info
                [::claim-bounty-receipt]
                {:bounty-id bounty-id
                 :tx-receipt tx-receipt}]}))

(re-frame/reg-event-fx
  ::claim-bounty-error
  (fn [{:keys [db]} [_ error]]
    {:dispatch-n [[::logging/error
                   [::claim-bounty-error]
                   {:error error}]
                  [::notification-events/show
                   {:message "Error claiming bounty. Bounty may not be completed or already claimed."
                    :type :error}]]}))

;;
;; Grant Functions
;;

(re-frame/reg-event-fx
  ::disburse-grant
  (fn [{:keys [db]} [_ {:keys [grant-id]}]]
    (let [active-account (account-queries/active-account db)]
      {:dispatch [::logging/info
                  [::disburse-grant]
                  {:grant-id grant-id
                   :from active-account}]
       :district.ui.web3-tx/send-tx
       {:instance (contract-queries/instance db :power-plant)
        :fn [:disburse-grant grant-id]
        :args {:from active-account}
        :tx-opts {:from active-account
                  :gas 180000}
        :on-tx-receipt [::disburse-grant-receipt grant-id]
        :on-tx-receipt-n [[::notification-events/show
                           {:message (str "Grant #" grant-id " vested amount disbursed!")
                            :type :success}]]
        :on-error [::disburse-grant-error]}})))

(re-frame/reg-event-fx
  ::disburse-grant-receipt
  (fn [{:keys [db]} [_ grant-id tx-receipt]]
    {:dispatch [::logging/info
                [::disburse-grant-receipt]
                {:grant-id grant-id
                 :tx-receipt tx-receipt}]}))

(re-frame/reg-event-fx
  ::disburse-grant-error
  (fn [{:keys [db]} [_ error]]
    {:dispatch-n [[::logging/error
                   [::disburse-grant-error]
                   {:error error}]
                  [::notification-events/show
                   {:message "Error disbursing grant. You may not be the recipient or no amount is vested yet."
                    :type :error}]]}))

;;
;; Treasury Query Functions
;;

(re-frame/reg-event-fx
  ::get-treasury-balance
  (fn [{:keys [db]} [_ {:keys [token]}]]
    {:dispatch [::logging/info
                [::get-treasury-balance]
                {:token token}]
     :district.ui.web3-tx/call
     {:instance (contract-queries/instance db :power-plant)
      :fn [:get-treasury-balance token]
      :on-success [::get-treasury-balance-success token]
      :on-error [::get-treasury-balance-error]}}))

(re-frame/reg-event-fx
  ::get-treasury-balance-success
  (fn [{:keys [db]} [_ token balance]]
    {:dispatch [::logging/info
                [::get-treasury-balance-success]
                {:token token
                 :balance balance}]
     :db (assoc-in db [:power-plant :treasury token] balance)}))

(re-frame/reg-event-fx
  ::get-treasury-balance-error
  (fn [{:keys [db]} [_ error]]
    {:dispatch [::logging/error
                [::get-treasury-balance-error]
                {:error error}]}))

;;
;; Subscriptions for UI State
;;

(re-frame/reg-sub
  ::treasury-balance
  (fn [db [_ token]]
    (get-in db [:power-plant :treasury token])))

;;
;; Initialization
;;

(re-frame/reg-event-fx
  ::load-treasury-balances
  (fn [{:keys [db]} [_]]
    (let [dnt-token (get-in db [:smart-contracts :DNT :address])
          eth-address "0x0000000000000000000000000000000000000000"]
      {:dispatch-n [[::get-treasury-balance {:token dnt-token}]
                    [::get-treasury-balance {:token eth-address}]]})))
