(ns district-registry.tests.runner
  (:require
    [cljs-promises.async :refer-macros [<?]]
    [cljs-promises.async]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs.nodejs :as nodejs]
    [clojure.core.async :as async :refer [<!]]
    [district-registry.server.graphql-resolvers :refer [resolvers-map]]
    [district-registry.shared.graphql-schema :refer [graphql-schema]]
    [district-registry.tests.smart-contracts.deployment-tests]
    [district-registry.tests.smart-contracts.district-tests]
    [district-registry.tests.smart-contracts.registry-entry-tests]
    [district-registry.tests.smart-contracts.utils :as test-utils]
    [district.graphql-utils :as graphql-utils]
    [district.server.graphql :as graphql]
    [district.server.graphql.utils :as utils]
    [district.server.web3 :refer [web3]]
    [doo.runner :refer-macros [doo-tests]]
    [taoensso.timbre :as log]))

(nodejs/enable-util-print!)

(def child-process (nodejs/require "child_process"))
(def spawn (aget child-process "spawn"))

(set! (.-error js/console) (fn [x] (.log js/console x)))

(defn on-jsload []
  (graphql/restart {:schema (utils/build-schema graphql-schema
                                                resolvers-map
                                                {:kw->gql-name graphql-utils/kw->gql-name
                                                 :gql-name->kw graphql-utils/gql-name->kw})
                    :field-resolver (utils/build-default-field-resolver graphql-utils/gql-name->kw)}))

;; Lets prepare everything for the tests!!!

(defn start-and-run-tests []
  (async/go
    ((test-utils/create-before-fixture))
    (log/info "Running tests" ::deploy-contracts-and-run-tests)
    (cljs.test/run-tests
      'district-registry.tests.smart-contracts.deployment-tests
      'district-registry.tests.smart-contracts.registry-entry-tests
      'district-registry.tests.smart-contracts.district-tests)))

(defn deploy-contracts-and-run-tests
  "Redeploy smart contracts with truffle"
  []
  (log/warn "Redeploying contracts, please be patient..." ::redeploy)
  (let [child (spawn "truffle migrate --network ganache --f 2 --to 3" (clj->js {:stdio "inherit" :shell true}))]
    (-> child
      (.on "close" (fn []
                     ;; Give it some time to write smart_contracts.cljs
                     ;; if we remove the timeout, it start mount components while we still have the old smart_contract.cljs
                     (js/setTimeout #(start-and-run-tests) 5000))))))

(cljs-promises.async/extend-promises-as-pair-channels!)
#_(deploy-contracts-and-run-tests)
(start-and-run-tests)

