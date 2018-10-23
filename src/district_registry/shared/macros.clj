(ns district-registry.shared.macros
  (:require
   [clojure.java.io :as io]
   [taoensso.timbre :as log]))

(defmacro slurp-resource [s]
  (-> s
    io/resource
    io/reader
    slurp))
