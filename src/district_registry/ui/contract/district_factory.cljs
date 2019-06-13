(ns district-registry.ui.contract.district-factory
  (:require
   [bignumber.core :as bn]
   [cljs-web3.eth :as web3-eth]
   [district.ui.logging.events :as logging]
   [district.ui.notification.events :as notification-events]
   [district.ui.smart-contracts.queries :as contract-queries]
   [district.ui.web3-accounts.queries :as account-queries]
   [district.ui.web3-tx.events :as tx-events]
   [print.foo :refer [look] :include-macros true]
   [re-frame.core :as re-frame]))

(def interceptors [re-frame/trim-v])

(re-frame/reg-event-fx
  ::approve-and-create-district
  interceptors
  (fn [{:keys [:db]} [{:keys [:deposit :dnt-weight :tx-id]} {:keys [Name Hash Size]}]]
    (let [active-account (account-queries/active-account db)
          extra-data (web3-eth/contract-get-data (contract-queries/instance db :district-factory)
                       :create-district
                       active-account
                       Hash
                       (bn/number dnt-weight))]
      {:dispatch [::tx-events/send-tx
                  {:instance (contract-queries/instance db :DNT)
                   :fn :approve-and-call
                   :args [(contract-queries/contract-address db :district-factory)
                          deposit
                          extra-data]
                   :tx-opts {:from active-account
                             :gas 7000000}
                   :tx-id {:approve-and-create-district tx-id}
                   :on-tx-success [::approve-and-create-district-success]
                   :on-tx-hash-error [::logging/error [::create-district]]
                   :on-tx-error [::logging/error [::create-district]]}]})))

(re-frame/reg-event-fx
  ::approve-and-create-district-success
  (fn [& args]
    {:dispatch [:district.ui.router.events/navigate :route/home]}))
