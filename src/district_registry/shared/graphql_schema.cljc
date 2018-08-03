(ns district-registry.shared.graphql-schema
  #?(:clj (:require [clojure.java.io :as io])
     :cljs (:require-macros [district-registry.shared.graphql-schema :refer [slurp-graphql-schema]])))

#?(:clj
   (defmacro slurp-graphql-schema []
     (-> "schema.graphql"
       io/resource
       io/reader
       slurp))
   :cljs
   (def graphql-schema
     (slurp-graphql-schema)))
