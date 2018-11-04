(merge-env!
  :dependencies [['org.clojure/clojure (clojure-version)]])

(merge-env!
  :dependencies '[[camel-snake-kebab "0.4.0"]
                  [com.paren/serene "0.0.1-RC1"]
                  [com.walmartlabs/lacinia "0.31.0-SNAPSHOT-1" ]
                  [org.clojure/clojurescript "1.10.339"]
                  [org.clojure/spec.alpha "0.2.176"]])

(require
  '[clojure.java.io :as io]
  '[clojure.spec.alpha :as s]
  '[clojure.pprint :refer [pprint]]
  '[clojure.string :as string]
  '[clojure.walk :as walk]
  '[com.walmartlabs.lacinia :as lacinia]
  '[com.walmartlabs.lacinia.parser.schema :as lacinia.parser.schema]
  '[com.walmartlabs.lacinia.schema :as lacinia.schema]
  '[paren.serene :as serene]
  '[camel-snake-kebab.core :as cs])

(defn gql-name->kw [gql-name]
  (let [k (name gql-name)]
    (if (string/starts-with? k "__")
      gql-name
      (let [k (if (string/ends-with? k "_")
                (str (.slice k 0 -1) "?")
                k)
            parts (string/split k #"_")
            parts (if (< 2 (count parts))
                    [(string/join "." (butlast parts)) (last parts)]
                    parts)]
        (apply keyword (map cs/->kebab-case parts))))))

(defn get-introspection-query-response []
  (let [sdl (slurp "resources/schema.graphql")
        edn (lacinia.parser.schema/parse-schema sdl {})
        edn (update edn :scalars (fn [scalars]
                                   (->> scalars
                                     (map (fn [[k v]]
                                            [k (assoc v
                                                 :parse (s/conformer any?)
                                                 :serialize (s/conformer any?))]))
                                     (into {}))))
        schema (lacinia.schema/compile edn)
        resp (lacinia/execute schema serene/introspection-query {} {})]
    resp))

(defn distinct-by [k]
  (fn [rf]
    (let [seen (volatile! #{})]
      (fn
        ([] (rf))
        ([result] (rf result))
        ([result input]
         (if (contains? @seen (k input))
           result
           (do (vswap! seen conj (k input))
               (rf result input))))))))

(deftask spit-specs []
  (->> (comp
         (serene/prefix :gql)
         (serene/extend '{:gql/BigNumber bignumber.core/bignumber?})
         (map (partial
                walk/postwalk
                (fn [k]
                  (if (and
                        (keyword? k)
                        (namespace k)
                        (string/includes? (name k) "_"))
                    (gql-name->kw k)
                    k))))
         (distinct-by first))
    (serene/compile (get-introspection-query-response))
    (cons '(ns district-registry.shared.graphql-specs
             (:require
              [bignumber.core]
              [clojure.spec.alpha])))
    (map #(-> % pprint with-out-str))
    (string/join \newline)
    (spit "src/district_registry/shared/graphql_specs.cljs")))
