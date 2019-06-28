(ns district-registry.ui.contract.ens
  (:require
    [clojure.string :as str]
    [district.ui.logging.events :as logging]
    [district.ui.smart-contracts.queries :as contract-queries]
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
  ::check-availability
  interceptors
  (fn [{:keys [db]} [{:keys [:aragon-id]}]]
    (when (and (not (str/blank? aragon-id))
               (valid-ens-name? aragon-id))
      {:web3/call {:web3 (web3-queries/web3 db)
                   :fns [{:instance (contract-queries/instance db :ENS)
                          :fn :owner
                          :args [(namehash (str aragon-id ".aragonid.eth"))]
                          :on-success [::check-availability-success aragon-id]
                          :on-error [::logging/error [::check-availability]]}]}})))


(re-frame/reg-event-fx
  ::check-availability-success
  interceptors
  (fn [{:keys [db]} [aragon-id result]]
    {:db (assoc-in db [:district-registry.ui.core/aragon-id->available? aragon-id]
                   (or (= result "0x")
                       (= result web3-utils/zero-address)))}))

