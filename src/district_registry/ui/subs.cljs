(ns district-registry.ui.subs
  (:require
    [district.ui.web3-accounts.queries :as account-queries]
    [re-frame.core :as re-frame]))

(re-frame/reg-sub
  ::vote
  (fn [db [_ reg-entry-address]]
    (get-in db [:district-registry.ui.core/votes (account-queries/active-account db) reg-entry-address])))
