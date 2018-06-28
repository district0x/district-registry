(ns district-registry.server.dev
  (:require
    [camel-snake-kebab.core :as cs :include-macros true]
    [cljs-time.core :as t]
    [cljs-web3.core :as web3]
    [cljs.nodejs :as nodejs]
    [cljs.pprint :as pprint]
    [clojure.pprint :refer [print-table]]
    [clojure.string :as str]
    [district-registry.server.db]
    [district-registry.server.deployer]
    [district-registry.server.generator]
    [district-registry.server.graphql-resolvers :refer [resolvers-map]]
    [district-registry.server.syncer]
    [district-registry.shared.graphql-schema :refer [graphql-schema]]
    [district-registry.shared.smart-contracts]
    [district.graphql-utils :as graphql-utils]
    [district.server.config :refer [config]]
    [district.server.db :as db]
    [district.server.db :refer [db]]
    [district.server.graphql :as graphql]
    [district.server.graphql.utils :as utils]
    [district.server.logging :refer [logging]]
    [district.server.middleware.logging :refer [logging-middlewares]]
    [district.server.smart-contracts]
    [district.server.web3 :refer [web3]]
    [district.server.web3-watcher]
    [goog.date.Date]
    [graphql-query.core :refer [graphql-query]]
    [mount.core :as mount]
    [print.foo :include-macros true]))

(nodejs/enable-util-print!)

(def graphql-module (nodejs/require "graphql"))
(def parse-graphql (aget graphql-module "parse"))
(def visit (aget graphql-module "visit"))

(defn on-jsload []
  (graphql/restart {:schema (utils/build-schema graphql-schema
                                                resolvers-map
                                                {:kw->gql-name graphql-utils/kw->gql-name
                                                 :gql-name->kw graphql-utils/gql-name->kw})
                    :field-resolver (utils/build-default-field-resolver graphql-utils/gql-name->kw)}))


(defn deploy-to-mainnet []
  (mount/stop #'district.server.web3/web3
              #'district.server.smart-contracts/smart-contracts)
  (mount/start-with-args (merge
                           (mount/args)
                           {:web3 {:port 8545}
                            :deployer {:write? true
                                       :gas-price (web3/to-wei 4 :gwei)}})
                         #'district.server.web3/web3
                         #'district.server.smart-contracts/smart-contracts))


(defn redeploy []
  (mount/stop)
  (-> (mount/with-args
        (merge
          (mount/args)
          {:deployer {:write? true
                      ;; :transfer-dnt-to-accounts 1
                      }}))
    (mount/start)
    pprint/pprint))


(defn resync []
  (mount/stop #'district-registry.server.db/district-registry-db
              #'district-registry.server.syncer/syncer)
  (-> (mount/start #'district-registry.server.db/district-registry-db
                   #'district-registry.server.syncer/syncer)
      pprint/pprint))


(defn -main [& _]
  (-> (mount/with-args
        {:config {:default {:logging {:level "info"
                                      :console? true}
                            :graphql {:port 6300
                                      :middlewares [logging-middlewares]
                                      :schema (utils/build-schema graphql-schema
                                                resolvers-map
                                                {:kw->gql-name graphql-utils/kw->gql-name
                                                 :gql-name->kw graphql-utils/gql-name->kw})
                                      :field-resolver (utils/build-default-field-resolver graphql-utils/gql-name->kw)
                                      :path "/graphql"
                                      :graphiql true}
                            :web3 {:port 8549}
                            :generator {:districts/use-accounts 1
                                        :districts/items-per-account 1
                                        :districts/scenarios [:scenario/buy]
                                        :param-changes/use-accounts 1
                                        :param-changes/items-per-account 1
                                        :param-changes/scenarios [:scenario/apply-param-change]}
                            :deployer {:transfer-dnt-to-accounts 1
                                       :initial-registry-params
                                       {:district-registry {:challenge-period-duration (t/in-seconds (t/minutes 10))
                                                            :commit-period-duration (t/in-seconds (t/minutes 2))
                                                            :reveal-period-duration (t/in-seconds (t/minutes 1))
                                                            :deposit (web3/to-wei 10 :ether)
                                                            :challenge-dispensation 50
                                                            :vote-quorum 50
                                                            :max-total-supply 10
                                                            :max-auction-duration (t/in-seconds (t/weeks 20))}
                                        :param-change-registry {:challenge-period-duration (t/in-seconds (t/minutes 10))
                                                                :commit-period-duration (t/in-seconds (t/minutes 2))
                                                                :reveal-period-duration (t/in-seconds (t/minutes 1))
                                                                :deposit (web3/to-wei 1000 :ether)
                                                                :challenge-dispensation 50
                                                                :vote-quorum 50}}}}}
         :smart-contracts {:contracts-var #'district-registry.shared.smart-contracts/smart-contracts
                           :print-gas-usage? true
                           :auto-mining? true}})
    (mount/except [#'district-registry.server.deployer/deployer
                   #'district-registry.server.generator/generator])
    (mount/start)
    pprint/pprint))

(set! *main-cli-fn* -main)

(defn select
  "Usage: (select [:*] :from [:districts])"
  [& [select-fields & r]]
  (-> (db/all (->> (partition 2 r)
                   (map vec)
                   (into {:select select-fields})))
      (print-table)))

(defn print-db
  "Prints all db tables to the repl"
  []
  (let [all-tables (->> (db/all {:select [:name] :from [:sqlite-master] :where [:= :type "table"]})
                     (map :name))]
    (doseq [t all-tables]
      (println "#######" (str/upper-case t) "#######")
      (select [:*] :from [(keyword t)])
      (println "\n\n"))))

(comment
  (redeploy))
