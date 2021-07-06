(ns district-registry.ui.contract.ens
  (:require
    [clojure.string :as str]
    [district.ui.logging.events :as logging]
    [district.ui.smart-contracts.queries :as contract-queries]
    [district.ui.web3-accounts.queries :as account-queries]
    [district.ui.web3.queries :as web3-queries]
    [district.web3-utils :as web3-utils]
    [print.foo :refer [look] :include-macros true]
    [re-frame.core :as re-frame :refer [reg-event-fx]]))

(def interceptors [re-frame/trim-v])

(def namehash
  (memoize
    (fn namehash* [name]
      (js/EthEnsNamehash.hash name))))


(defn normalize [name]
  (js/EthEnsNamehash.normalize name))


(defn valid-ens-name? [name]
  (try
    (normalize name)
    true
    (catch js/Error e
      false)))


(re-frame/reg-event-fx
  ::check-ownership
  interceptors
  (fn [{:keys [db]} [{:keys [:ens-name]}]]
    (when (and (not (str/blank? ens-name))
               (valid-ens-name? ens-name))
      {:web3/call {:web3 (web3-queries/web3 db)
                   :fns [{:instance (contract-queries/instance db :ENS)
                          :fn :owner
                          :args [(namehash ens-name)]
                          :on-success [::check-ownership-success ens-name]
                          :on-error [::logging/error [::check-ownership]]}]}})))


(re-frame/reg-event-fx
  ::check-ownership-success
  interceptors
  (fn [{:keys [db]} [ens-name result]]
    {:db (assoc-in db [:district-registry.ui.core/ens-name-owner (account-queries/active-account db) ens-name]
                 (= (str/lower-case result) (str/lower-case (account-queries/active-account db))))}))
