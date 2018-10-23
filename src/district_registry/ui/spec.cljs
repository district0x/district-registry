(ns district-registry.ui.spec
  (:require
   [cljs.spec.alpha :as s]))

(s/def ::str string?)
(s/def ::not-empty? (complement empty?))
(s/def ::int integer?)
(s/def ::pos pos?)

(s/def ::pos-int (s/and ::int ::pos))
(s/def ::challenge-comment (s/and ::str ::not-empty?))

(s/def ::url (fn [s]
               (and
                 (string? s)
                 (re-find #"^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]" s))))

(defn check [type data]
  (s/valid? type data))
