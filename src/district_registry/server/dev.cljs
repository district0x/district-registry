(ns district-registry.server.dev
  (:require [cljs.nodejs :as nodejs]
            [cljs.pprint :as pprint]
            [clojure.pprint :refer [print-table]]
            [clojure.string :as str]
            [district-registry.server.constants :as constants]
            district-registry.server.db
            district-registry.server.emailer
            [district-registry.server.graphql-resolvers :refer [resolvers-map]]
            district-registry.server.syncer
            [district-registry.shared.graphql-schema :refer [graphql-schema]]
            [district-registry.shared.smart-contracts-dev :as smart-contracts-dev]
            [district-registry.shared.smart-contracts-prod :as smart-contracts-prod]
            [district-registry.shared.smart-contracts-qa :as smart-contracts-qa]
            [district.graphql-utils :as graphql-utils]
            [district.server.config :refer [config]]
            [district.server.db :as db]
            [district.server.graphql :as graphql]
            [district.server.graphql.utils :as utils]
            [district.server.middleware.logging :refer [logging-middlewares]]
            district.server.web3-events
            [mount.core :as mount]
            [taoensso.timbre :as log])
  (:require-macros
   [district-registry.shared.macros :refer [get-environment]]))

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
                            :web3 {:url "ws://127.0.0.1:8545"
                                   :on-offline (fn []
                                                 (log/error "Ethereum node went offline, stopping syncing modules" ::web3-watcher)
                                                 (mount/stop #'district-registry.server.db/district-registry-db
                                                             #'district.server.web3-events/web3-events
                                                             #'district-registry.server.syncer/syncer
                                                             #'district-registry.server.emailer/emailer))
                                   :on-online (fn []
                                                (log/warn "Ethereum node went online again, starting syncing modules" ::web3-watcher)
                                                (mount/start #'district-registry.server.db/district-registry-db
                                                             #'district.server.web3-events/web3-events
                                                             #'district-registry.server.syncer/syncer
                                                             #'district-registry.server.emailer/emailer))}
                            :ipfs {:host "http://127.0.0.1:5001"
                                   :endpoint "/api/v0"
                                   :gateway "http://127.0.0.1:8080/ipfs"}
                            :emailer {:private-key "PLACEHOLDER"
                                      :api-key "PLACEHOLDER"
                                      :template-id "PLACEHOLDER"
                                      :from "registry@district0x.io"
                                      :print-mode? true}
                            :ui {:root-url "http://0.0.0.0:4177/#/"}
                            :smart-contracts {:contracts-var contracts-var}
                            :web3-events {:events constants/web3-events
                                          :from-block 0
                                          :block-step 1000}}}})
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
