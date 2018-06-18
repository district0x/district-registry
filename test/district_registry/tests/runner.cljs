(ns district-registry.tests.runner
  (:require [cljs.nodejs :as nodejs]
            [cljs.test :refer [run-tests]]
            [district.graphql-utils :as graphql-utils]
            [district.server.graphql :as graphql]
            [district.server.graphql.utils :as utils]
            [district-registry.server.graphql-resolvers :refer [resolvers-map]]
            [district-registry.shared.graphql-schema :refer [graphql-schema]]
            [district-registry.tests.graphql-resolvers.graphql-resolvers-tests]
            [district-registry.tests.smart-contracts.deployment-tests]
            [district-registry.tests.smart-contracts.meme-auction-tests]
            [district-registry.tests.smart-contracts.meme-tests]
            [district-registry.tests.smart-contracts.param-change-tests]
            [district-registry.tests.smart-contracts.registry-entry-tests]
            [district-registry.tests.smart-contracts.registry-tests]))

(nodejs/enable-util-print!)

(defn on-jsload []
  (graphql/restart {:schema (utils/build-schema graphql-schema
                                                resolvers-map
                                                {:kw->gql-name graphql-utils/kw->gql-name
                                                 :gql-name->kw graphql-utils/gql-name->kw})
                    :field-resolver (utils/build-default-field-resolver graphql-utils/gql-name->kw)}))

(set! (.-error js/console) (fn [x] (.log js/console x)))

(defn -main [& _]
  (run-tests 'district-registry.tests.smart-contracts.deployment-tests))

(set! *main-cli-fn* -main)
