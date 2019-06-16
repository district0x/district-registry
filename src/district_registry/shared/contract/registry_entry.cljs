(ns district-registry.shared.contract.registry-entry
  (:require
    [bignumber.core :as bn]
    [clojure.set :as set]
    [district.web3-utils :refer [web3-time->local-date-time empty-address? wei->eth-number]]))

(def statuses
  {0 :reg-entry.status/challenge-period
   1 :reg-entry.status/commit-period
   2 :reg-entry.status/reveal-period
   3 :reg-entry.status/blacklisted
   4 :reg-entry.status/whitelisted})

(def vote-options
  {0 :vote-option/neither
   1 :vote-option/include
   2 :vote-option/exclude})

(def vote-option->num (set/map-invert vote-options))

(defn parse-status [status]
  (statuses (bn/number status)))
