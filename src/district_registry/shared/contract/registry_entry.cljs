(ns district-registry.shared.contract.registry-entry
  (:require
    [clojure.set :as set]))

(def vote-options
  {0 :vote-option/neither
   1 :vote-option/include
   2 :vote-option/exclude})

(def vote-option->num (set/map-invert vote-options))
