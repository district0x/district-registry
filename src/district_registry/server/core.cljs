(ns district-registry.server.core
  (:require
    [cljs.nodejs :as nodejs]
    [district.server.config :refer [config]]
    [district.server.logging]
    [district.server.middleware.logging :refer [logging-middlewares]]
    [district.server.web3-watcher]
    [district-registry.server.db]
    [district-registry.server.deployer]
    [district-registry.server.generator]
    [district-registry.server.syncer]
    [district-registry.shared.smart-contracts]
    [mount.core :as mount]
    [taoensso.timbre :refer-macros [info warn error]]))

(nodejs/enable-util-print!)

(defn -main [& _]
  (-> (mount/with-args
        {:config {:default {:web3 {:port 8545}}}
         :smart-contracts {:contracts-var #'district-registry.shared.smart-contracts/smart-contracts}
         :graphql {:port 6300
                   :middlewares [logging-middlewares]
                   :schema "type Query { hello: String}"
                   :root-value {:hello (constantly "Hello world")}
                   :path "/graphql"}
         :web3-watcher {:on-online (fn []
                                     (warn "Ethereum node went online again")
                                     (mount/stop #'district-registry.server.db/district-registry-db)
                                     (mount/start #'district-registry.server.db/district-registry-db
                                                  #'district-registry.server.syncer/syncer))
                        :on-offline (fn []
                                      (warn "Ethereum node went offline")
                                      (mount/stop #'district-registry.server.syncer/syncer))}})
    (mount/except [#'district-registry.server.deployer/deployer
                   #'district-registry.server.generator/generator])
    (mount/start))
  (warn "System started" {:config @config}))

(set! *main-cli-fn* -main)
