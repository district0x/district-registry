(ns district-registry.ui.contract.ari-oracle
  (:require
    [district.ui.logging.events :as logging]
    [district.ui.notification.events :as notification-events]
    [district.ui.smart-contracts.queries :as contract-queries]
    [district.ui.web3-accounts.queries :as account-queries]
    [district.ui.web3-tx.events :as tx-events]
    [district-registry.ui.contract.council :as council-contract]
    [re-frame.core :as re-frame]))

;;
;; ARI Oracle Contract Events
;;
;; These events handle interactions with the ARIOracle.sol contract
;; for viewing proposals and participating in the veto mechanism.
;;

(re-frame/reg-event-fx
  ::get-proposal
  (fn [{:keys [db]} [_ proposal-id]]
    {:dispatch [::logging/info
                [::get-proposal]
                {:proposal-id proposal-id}]
     :district.ui.web3-tx/call
     {:instance (contract-queries/instance db :ari-oracle)
      :fn [:get-proposal proposal-id]
      :on-success [::get-proposal-success proposal-id]
      :on-error [::get-proposal-error]}}))

(re-frame/reg-event-fx
  ::get-proposal-success
  (fn [{:keys [db]} [_ proposal-id proposal-data]]
    {:dispatch [::logging/info
                [::get-proposal-success]
                {:proposal-id proposal-id
                 :proposal-data proposal-data}]
     :db (assoc-in db [:ari-oracle :proposals proposal-id] proposal-data)}))

(re-frame/reg-event-fx
  ::get-proposal-error
  (fn [{:keys [db]} [_ error]]
    {:dispatch-n [[::logging/error
                   [::get-proposal-error]
                   {:error error}]
                  [::notification-events/show
                   {:message "Error fetching proposal"
                    :type :error}]]}))

(re-frame/reg-event-fx
  ::check-veto
  (fn [{:keys [db]} [_ proposal-id]]
    {:dispatch [::logging/info
                [::check-veto]
                {:proposal-id proposal-id}]
     :district.ui.web3-tx/call
     {:instance (contract-queries/instance db :ari-oracle)
      :fn [:check-veto proposal-id]
      :on-success [::check-veto-success proposal-id]
      :on-error [::check-veto-error]}}))

(re-frame/reg-event-fx
  ::check-veto-success
  (fn [{:keys [db]} [_ proposal-id veto-result]]
    {:dispatch [::logging/info
                [::check-veto-success]
                {:proposal-id proposal-id
                 :vetoed veto-result}]
     :db (assoc-in db [:ari-oracle :proposals proposal-id :vetoed] veto-result)}))

(re-frame/reg-event-fx
  ::check-veto-error
  (fn [{:keys [db]} [_ error]]
    {:dispatch-n [[::logging/error
                   [::check-veto-error]
                   {:error error}]
                  [::notification-events/show
                   {:message "Error checking veto status"
                    :type :error}]]}))

;;
;; Veto Participation via Stake Movement
;;
;; To veto a proposal, users don't explicitly vote - they move their stake
;; between council seats. If ≥30% of total stake shifts during the veto period,
;; the proposal is automatically vetoed.
;;

(re-frame/reg-event-fx
  ::move-stake-to-veto
  (fn [{:keys [db]} [_ {:keys [proposal-id from-seat to-seat amount]}]]
    (let [active-account (account-queries/active-account db)]
      {:dispatch-n [[::logging/info
                     [::move-stake-to-veto]
                     {:proposal-id proposal-id
                      :from-seat from-seat
                      :to-seat to-seat
                      :amount amount
                      :from active-account}]
                    ;; Delegate to Council contract's move-stake functionality
                    [::council-contract/move-stake
                     {:from-seat from-seat
                      :to-seat to-seat
                      :amount amount
                      :reason (str "Veto attempt for proposal #" proposal-id)}]]})))

;;
;; Proposal Status Queries
;;

(re-frame/reg-event-fx
  ::get-veto-period-duration
  (fn [{:keys [db]} [_]]
    {:district.ui.web3-tx/call
     {:instance (contract-queries/instance db :ari-oracle)
      :fn [:veto-period-duration]
      :on-success [::get-veto-period-duration-success]
      :on-error [::logging/error [::get-veto-period-duration-error]]}}))

(re-frame/reg-event-fx
  ::get-veto-period-duration-success
  (fn [{:keys [db]} [_ duration]]
    {:db (assoc-in db [:ari-oracle :veto-period-duration] duration)}))

(re-frame/reg-event-fx
  ::get-veto-threshold
  (fn [{:keys [db]} [_]]
    {:district.ui.web3-tx/call
     {:instance (contract-queries/instance db :ari-oracle)
      :fn [:veto-threshold-bps]
      :on-success [::get-veto-threshold-success]
      :on-error [::logging/error [::get-veto-threshold-error]]}}))

(re-frame/reg-event-fx
  ::get-veto-threshold-success
  (fn [{:keys [db]} [_ threshold]]
    {:db (assoc-in db [:ari-oracle :veto-threshold-bps] threshold)}))

;;
;; Subscriptions for UI State
;;

(re-frame/reg-sub
  ::proposal
  (fn [db [_ proposal-id]]
    (get-in db [:ari-oracle :proposals proposal-id])))

(re-frame/reg-sub
  ::veto-period-duration
  (fn [db _]
    (get-in db [:ari-oracle :veto-period-duration])))

(re-frame/reg-sub
  ::veto-threshold
  (fn [db _]
    (get-in db [:ari-oracle :veto-threshold-bps])))

;;
;; Initialization
;;

(re-frame/reg-event-fx
  ::load-ari-config
  (fn [{:keys [db]} [_]]
    {:dispatch-n [[::get-veto-period-duration]
                  [::get-veto-threshold]]}))
