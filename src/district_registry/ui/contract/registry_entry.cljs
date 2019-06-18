(ns district-registry.ui.contract.registry-entry
  (:require
    [cljs-solidity-sha3.core :refer [solidity-sha3]]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs.spec.alpha :as s]
    [district-registry.shared.contract.registry-entry :as reg-entry]
    [district.cljs-utils :as cljs-utils]
    [district.ui.logging.events :as logging]
    [district.ui.notification.events :as notification-events]
    [district.ui.router.events :as router-events]
    [district.ui.smart-contracts.queries :as contract-queries]
    [district.ui.web3-accounts.queries :as account-queries]
    [district.ui.web3-tx.events :as tx-events]
    [goog.string :as gstring]
    [print.foo :refer [look] :include-macros true]
    [re-frame.core :as re-frame]))

(def interceptors [re-frame/trim-v])

(re-frame/reg-event-fx
  ::approve-and-create-challenge
  interceptors
  (fn [{:keys [db]} [{:keys [:reg-entry/address :district/name :deposit]} {:keys [Hash]}]]
    (let [active-account (account-queries/active-account db)
          extra-data (web3-eth/contract-get-data (contract-queries/instance db :district address)
                                                 :create-challenge
                                                 active-account
                                                 Hash)
          tx-log-name (str "Challenge " name)]
      {:dispatch [::tx-events/send-tx
                  {:instance (contract-queries/instance db :DNT)
                   :fn :approve-and-call
                   :args [address deposit extra-data]
                   :tx-opts {:from active-account}
                   :tx-id {:approve-and-create-challenge {:reg-entry/address address}}
                   :tx-log {:name tx-log-name :related-href {:name :route/detail :params {:address address}}}
                   :on-tx-success-n [[::approve-and-create-challenge-success]
                                     [::logging/success [::approve-and-create-challenge]]
                                     [::notification-events/show (gstring/format "Challenge created for %s with metahash %s"
                                                                                 address Hash)]]
                   :on-tx-hash-error [::logging/error [::approve-and-create-challenge]]
                   :on-tx-error [::logging/error [::approve-and-create-challenge]]}]})))

(re-frame/reg-event-fx
  ::approve-and-create-challenge-success
  (constantly nil))


(re-frame/reg-event-fx
  ::approve-and-commit-vote
  [(re-frame/inject-cofx :store) interceptors]
  (fn [{:keys [:db :store]} [{:keys [:reg-entry/address :vote/amount :vote/option :district/name]}]]
    (let [active-account (account-queries/active-account db)
          salt (cljs-utils/rand-str 5)
          secret-hash (solidity-sha3 (reg-entry/vote-option->num option) salt)
          extra-data (web3-eth/contract-get-data (contract-queries/instance db :district address)
                                                 :commit-vote
                                                 active-account
                                                 amount
                                                 secret-hash)
          store-votes-args [[:district-registry.ui.core/votes active-account address] {:option option :salt salt}]
          tx-log-name (gstring/format "Commit vote to %s" name)]
      {:dispatch [::tx-events/send-tx
                  {:instance (contract-queries/instance db :DNT)
                   :fn :approve-and-call
                   :args [address amount extra-data]
                   :tx-opts {:from active-account}
                   :tx-id {:approve-and-commit-vote {:reg-entry/address address}}
                   :tx-log {:name tx-log-name :related-href {:name :route/detail :params {:address address}}}
                   :on-tx-success-n [[::logging/success [::approve-and-commit-vote]]
                                     [::notification-events/show "Voted"]
                                     [::approve-and-commit-vote-success]]
                   :on-tx-hash-error [::logging/error {:approve-and-commit-vote {:reg-entry/address address}}]
                   :on-tx-error [::logging/error {:approve-and-commit-vote {:reg-entry/address address}}]}]
       :store (apply assoc-in store store-votes-args)
       :db (apply assoc-in db store-votes-args)})))


(re-frame/reg-event-fx
  ::approve-and-commit-vote-success
  (constantly nil))


(re-frame/reg-event-fx
  ::reveal-vote
  [(re-frame/inject-cofx :store) interceptors]
  (fn [{:keys [:db :store]} [{:keys [:reg-entry/address :district/name]}]]
    (let [active-account (account-queries/active-account db)
          {:keys [option salt]} (get-in store [:district-registry.ui.core/votes active-account address])
          tx-log-name (str "Reveal vote for " name)]
      {:dispatch [::tx-events/send-tx
                  {:instance (contract-queries/instance db :district address)
                   :fn :reveal-vote
                   :args [(reg-entry/vote-option->num option) salt]
                   :tx-opts {:from active-account}
                   :tx-id {:reveal-vote {:reg-entry/address address}}
                   :tx-log {:name tx-log-name :related-href {:name :route/detail :params {:address address}}}
                   :on-tx-success-n [[::logging/success [::reveal-vote]]
                                     [::notification-events/show "Voted"]
                                     [::reveal-vote-success]]
                   :on-tx-hash-error [::logging/error [::reveal-vote]]
                   :on-tx-error [::logging/error [::reveal-vote]]}]})))


(re-frame/reg-event-fx
  ::reveal-vote-success
  (constantly nil))


(re-frame/reg-event-fx
  ::claim-reward
  interceptors
  (fn [{:keys [:db]} [{:keys [:reg-entry/address :challenge/index :district/name]}]]
    (let [active-account (account-queries/active-account db)
          tx-log-name (gstring/format "Claim reward from %s voting" name)]
      {:dispatch [::tx-events/send-tx
                  {:instance (contract-queries/instance db :district address)
                   :fn :claim-reward-for-challenge
                   :args [index active-account]
                   :tx-opts {:from active-account}
                   :tx-id {:claim-reward {:reg-entry/address address :challenge/index index}}
                   :tx-log {:name tx-log-name :related-href {:name :route/detail :params {:address address}}}
                   :on-tx-success-n [[::logging/success [::claim-reward]]
                                     [::notification-events/show "Succesfully claimed rewards"]
                                     [::claim-reward-success]]
                   :on-tx-hash-error [::logging/error [::claim-reward]]
                   :on-tx-error [::logging/error [::claim-reward]]}]})))


(re-frame/reg-event-fx
  ::claim-reward-success
  (constantly nil))


(re-frame/reg-event-fx
  ::set-meta-hash
  interceptors
  (fn [{:keys [:db]} [{:keys [:reg-entry/address :name] :as data} {:keys [Hash]}]]
    (let [active-account (account-queries/active-account db)
          tx-log-name (str "Edit " name)]
      {:dispatch [::tx-events/send-tx
                  {:instance (contract-queries/instance db :district (print.foo/look address))
                   :fn :set-meta-hash
                   :args [Hash]
                   :tx-opts {:from active-account}
                   :tx-id {:set-meta-hash {:reg-entry/address address}}
                   :tx-log {:name tx-log-name :related-href {:name :route/detail :params {:address address}}}
                   :on-tx-success-n [[::logging/success [::set-meta-hash]]
                                     [::notification-events/show "Succesfully saved district details"]
                                     [::set-meta-hash-success data]]
                   :on-tx-hash-error [::logging/error [::set-meta-hash]]
                   :on-tx-error [::logging/error [::set-meta-hash]]}]})))


(re-frame/reg-event-fx
  ::set-meta-hash-success
  interceptors
  (fn [_ [{:keys [:reg-entry/address]}]]
    {:dispatch [::router-events/navigate :route/detail {:address address}]}))
