(ns district-registry.ui.subs
  (:require
    [district-registry.ui.config :as config]
    [district-registry.ui.contract.district :as district]
    [district.format :as format]
    [district.ui.web3-accounts.queries :as account-queries]
    [re-frame.core :as re-frame])
  (:require-macros [reagent.ratom :refer [reaction]]))

(re-frame/reg-sub
  ::vote
  (fn [db [_ reg-entry-address]]
    (get-in db [:district-registry.ui.core/votes (account-queries/active-account db) reg-entry-address])))

(re-frame/reg-sub
  ::owner-of-ens-name?
  (fn [db [_ ens-name]]
    (get-in db [:district-registry.ui.core/ens-name-owner (account-queries/active-account db) ens-name])))

(re-frame/reg-sub-raw
  ::has-snapshot?
  (fn [db [_ ens-name]]
    (re-frame/dispatch [::district/check-snapshot {:ens-name ens-name}])
    (reaction (get-in @db [:district-registry.ui.core/has-snapshot? ens-name]))))

(re-frame/reg-sub
  ::snapshot-url
  (fn [_ [_ ens-name]]
    (str (format/ensure-trailing-slash (:snapshot-url config/config-map)) ens-name)))

(re-frame/reg-sub
  ::active-account-has-email?
  (fn [db]
    (boolean (seq (get-in db [:district-registry.ui.my-account (account-queries/active-account db) :encrypted-email])))))

(re-frame/reg-sub
  ::estimated-return-for-stake
  (fn [db [_ stake-bank amount]]
    (get-in db [:district-registry.ui.contract.district/estimated-return-for-stake stake-bank amount])))
