(ns district-registry.shared.graphql-schema
  (:require-macros [district-registry.shared.macros :refer [slurp-resource]]))

(def graphql-schema
  (slurp-resource "schema.graphql"))

