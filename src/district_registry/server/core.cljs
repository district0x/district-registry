(ns district-registry.server.core
  (:require [cljs.nodejs :as nodejs]
            [district-registry.server.constants :as constants]
            [district-registry.server.db]
            [district-registry.server.emailer]
            [district-registry.server.graphql-resolvers :refer [resolvers-map]]
            [district-registry.server.ipfs]
            [district-registry.server.syncer]
            [district-registry.shared.graphql-schema :refer [graphql-schema]]
            [district-registry.shared.smart-contracts-dev :as smart-contracts-dev]
            [district-registry.shared.smart-contracts-prod :as smart-contracts-prod]
            [district-registry.shared.smart-contracts-qa :as smart-contracts-qa]
            [district.graphql-utils :as graphql-utils]
            [district.server.config :refer [config]]
            [district.server.graphql.utils :as utils]
            [district.server.graphql]
            [district.server.logging]
            [district.server.middleware.logging :refer [logging-middlewares]]
            [district.server.web3-events]
            [mount.core :as mount]
            [taoensso.timbre :as log :refer-macros [info warn error]])
  (:require-macros [district-registry.shared.macros :refer [get-environment]]))

(nodejs/enable-util-print!)

(def contracts-var
  (condp = (get-environment)
    "prod" #'smart-contracts-prod/smart-contracts
    "qa" #'smart-contracts-qa/smart-contracts
    "qa-dev" #'smart-contracts-qa/smart-contracts
    "dev" #'smart-contracts-dev/smart-contracts))

(defn -main [& _]
  (-> (mount/with-args
        {:config {:default {:smart-contracts {:contracts-var contracts-var}
                            :graphql {:port 6400
                                      :middlewares [logging-middlewares]
                                      :schema (utils/build-schema graphql-schema
                                                                  resolvers-map
                                                                  {:kw->gql-name graphql-utils/kw->gql-name
                                                                   :gql-name->kw graphql-utils/gql-name->kw})
                                      :field-resolver (utils/build-default-field-resolver graphql-utils/gql-name->kw)
                                      :path "/graphql"}
                            :web3-events {:events constants/web3-events
                                          :from-block 0
                                          :block-step 1000}
                            :sigterm {:on-sigterm (fn [args]
                                                    (log/warn "Received SIGTERM signal. Exiting" {:args args})
                                                    (mount/stop)
                                                    (.exit nodejs/process 0))}
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
                                                             #'district-registry.server.emailer/emailer))}}}})
      (mount/start))
  (warn "System started" {:config @config}))

(set! *main-cli-fn* -main)
