(ns district-registry.server.dev
  (:require
   [camel-snake-kebab.core :as cs :include-macros true]
   [cljs-time.core :as t]
   [cljs-web3.core :as web3]
   [cljs.nodejs :as nodejs]
   [cljs.pprint :as pprint]
   [clojure.pprint :refer [print-table]]
   [clojure.string :as str]
   [district-registry.server.constants :as constants]
   [district-registry.server.db]
   [district-registry.server.graphql-resolvers :refer [resolvers-map]]
   [district-registry.server.ipfs]
   [district-registry.server.syncer]
   [district-registry.shared.graphql-schema :refer [graphql-schema]]
   [district-registry.shared.smart-contracts-dev :as smart-contracts-dev]
   [district-registry.shared.smart-contracts-prod :as smart-contracts-prod]
   [district-registry.shared.smart-contracts-qa :as smart-contracts-qa]
   [district.graphql-utils :as graphql-utils]
   [district.server.config :refer [config]]
   [district.server.db :as db]
   [district.server.graphql :as graphql]
   [district.server.graphql.utils :as utils]
   [district.server.logging :refer [logging]]
   [district.server.middleware.logging :refer [logging-middlewares]]
   [district.server.smart-contracts]
   [district.server.web3 :refer [web3]]
   [district.server.web3-events]
   [district.server.web3-watcher]
   [goog.date.Date]
   [graphql-query.core :refer [graphql-query]]
   [mount.core :as mount]
   [print.foo :include-macros true]
   [taoensso.timbre :as log])
  (:require-macros [district-registry.shared.macros :refer [get-environment]]))

(nodejs/enable-util-print!)

(def graphql-module (nodejs/require "graphql"))
(def parse-graphql (aget graphql-module "parse"))

(defn on-jsload []
  (graphql/restart {:schema (utils/build-schema graphql-schema
                                                resolvers-map
                                                {:kw->gql-name graphql-utils/kw->gql-name
                                                 :gql-name->kw graphql-utils/gql-name->kw})
                    :field-resolver (utils/build-default-field-resolver graphql-utils/gql-name->kw)}))


(defn resync []
  (mount/stop #'district-registry.server.db/district-registry-db
              #'district-registry.server.syncer/syncer)
  (-> (mount/start #'district-registry.server.db/district-registry-db
                   #'district-registry.server.syncer/syncer)
      pprint/pprint))


(def contracts-var
  (condp = (get-environment)
    "prod" #'smart-contracts-prod/smart-contracts
    "qa" #'smart-contracts-qa/smart-contracts
    "qa-dev" #'smart-contracts-qa/smart-contracts
    "dev" #'smart-contracts-dev/smart-contracts))


(defn -main [& _]
  (-> (mount/with-args
        {:config {:default {:logging {:level "info"
                                      :console? true}
                            :graphql {:port 6400
                                      :middlewares [logging-middlewares]
                                      :schema (utils/build-schema graphql-schema
                                                                  resolvers-map
                                                                  {:kw->gql-name graphql-utils/kw->gql-name
                                                                   :gql-name->kw graphql-utils/gql-name->kw})
                                      :field-resolver (utils/build-default-field-resolver graphql-utils/gql-name->kw)
                                      :path "/graphql"
                                      :graphiql true}
                            :web3 {:port 8549}
                            :ipfs {:host "http://127.0.0.1:5001"
                                   :endpoint "/api/v0"
                                   :gateway "http://127.0.0.1:8080/ipfs"}}}
         :smart-contracts {:contracts-var contracts-var}
         :web3-events {:events constants/web3-events}})
    (mount/start)
    (as-> $ (log/warn "Started" {:components $
                                 :config @config}))))


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

(comment (print-db))

(comment (mount/stop))