(ns district-registry.shared.utils
  (:require
    [clojure.set :as set])
  (:import [goog.async Debouncer]))

(def vote-option->kw
  {0 :vote-option/neither
   1 :vote-option/include
   2 :vote-option/exclude})

(def vote-option->num (set/map-invert vote-option->kw))

(defn debounce [f interval]
  (let [dbnc (Debouncer. f interval)]
    ;; We use apply here to support functions of various arities
    (fn [& args] (.apply (.-fire dbnc) dbnc (to-array args)))))