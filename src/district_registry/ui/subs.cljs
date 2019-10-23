(ns district-registry.ui.subs
  (:require
    [district-registry.ui.config :as config]
    [district.format :as format]
    [district.ui.web3-accounts.queries :as account-queries]
    [re-frame.core :as re-frame]))

(re-frame/reg-sub
  ::vote
  (fn [db [_ reg-entry-address]]
    (get-in db [:district-registry.ui.core/votes (account-queries/active-account db) reg-entry-address])))

(re-frame/reg-sub
  ::aragon-id-available?
  (fn [db [_ aragon-id]]
    (get-in db [:district-registry.ui.core/aragon-id->available? aragon-id])))

(re-frame/reg-sub
  ::aragon-url
  (fn [_ [_ aragon-id]]
    (str (format/ensure-trailing-slash (:aragon-url config/config-map)) aragon-id)))

(re-frame/reg-sub
  ::active-account-has-email?
  (fn [db]
    (boolean (seq (get-in db [:district-registry.ui.my-account (account-queries/active-account db) :encrypted-email])))))

(re-frame/reg-sub
  ::estimated-return-for-stake
  (fn [db [_ stake-bank amount]]
    (get-in db [:district-registry.ui.contract.district/estimated-return-for-stake stake-bank amount])))
