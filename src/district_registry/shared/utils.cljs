(ns district-registry.shared.utils
  (:require
    [clojure.set :as set]))

(def vote-option->kw
  {0 :vote-option/neither
   1 :vote-option/include
   2 :vote-option/exclude})

(def vote-option->num (set/map-invert vote-option->kw))
