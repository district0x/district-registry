(ns district-registry.ui.config
  (:require
    [graphql-query.core :refer [graphql-query]]
    [district-registry.shared.graphql-schema :refer [graphql-schema]]
    [district-registry.shared.smart-contracts-dev :as smart-contracts-dev]
    [district-registry.shared.smart-contracts-prod :as smart-contracts-prod]
    [district-registry.shared.smart-contracts-qa :as smart-contracts-qa]
    [mount.core :refer [defstate]])
  (:require-macros [district-registry.shared.macros :refer [get-environment]]))

(def contracts-to-load [:DNT :district :param-change :district-factory :param-change-factory :ENS])

(def development-config
  {:debug? true
   :logging {:level :debug
             :console? true}
   :time-source :js-date
   :smart-contracts {:contracts (select-keys smart-contracts-dev/smart-contracts contracts-to-load)}
   :web3-balances {:contracts (select-keys smart-contracts-dev/smart-contracts [:DNT])}
   :web3 {:url "http://localhost:8549"}
   :web3-tx {:disable-loading-recommended-gas-prices? true}
   :web3-tx-log {:disable-using-localstorage? true
                 :tx-costs-currencies [:USD]
                 :etherscan-url "https://rinkeby.etherscan.io"}
   :graphql {:schema graphql-schema
             :url "http://localhost:6400/graphql"}
   :ipfs {:host "http://127.0.0.1:5001"
          :endpoint "/api/v0"
          :gateway "http://127.0.0.1:8080/ipfs"}
   :router {:html5? false}
   :router-google-analytics {:enabled? false}
   :aragon-url "https://rinkeby.aragon.org/#/"})

(def qa-config
  {:logging {:level :warn
             :console? true
             :sentry {:dsn "https://4bb89c9cdae14444819ff0ac3bcba253@sentry.io/1306960"
                      :environment "QA"}}
   :time-source :js-date
   :smart-contracts {:contracts (select-keys smart-contracts-qa/smart-contracts contracts-to-load)}
   :web3-balances {:contracts (select-keys smart-contracts-qa/smart-contracts [:DNT])}
   :web3 {:url "https://rinkeby.district0x.io"}
   :web3-tx {:disable-loading-recommended-gas-prices? true}
   :web3-tx-log {:disable-using-localstorage? false
                 :tx-costs-currencies [:USD]
                 :etherscan-url "https://rinkeby.etherscan.io"}
   :graphql {:schema graphql-schema
             :url "http://api.registry.qa.district0x.io/"}
   :ipfs {:host "https://ipfs.qa.district0x.io/api"
          :endpoint "/api/v0"
          :gateway "https://ipfs.qa.district0x.io/gateway/ipfs"}
   :router {:html5? true}
   :router-google-analytics {:enabled? false}
   :aragon-url "https://rinkeby.aragon.org/#/"})

(def qa-dev-config (merge (assoc-in qa-config [:router :html5?] false)
                          {:ipfs {:host "http://127.0.0.1:5001"
                                  :endpoint "/api/v0"
                                  :gateway "http://127.0.0.1:8080/ipfs"}
                           :web3 {:url "http://localhost:8545"}
                           :graphql {:schema graphql-schema
                                     :url "http://localhost:6400/graphql"}}))

(def production-config
  {:logging {:level :warn
             :console? false
             :sentry {:dsn "https://4bb89c9cdae14444819ff0ac3bcba253@sentry.io/1306960"
                      :environment "PRODUCTION"}}
   :time-source :js-date
   :smart-contracts {:contracts (select-keys smart-contracts-prod/smart-contracts contracts-to-load)}
   :web3-balances {:contracts (select-keys smart-contracts-prod/smart-contracts [:DNT])}
   :web3 {:url "https://mainnet.district0x.io"}
   :web3-tx-log {:disable-using-localstorage? false
                 :open-on-tx-hash? true
                 :tx-costs-currencies [:USD]
                 :etherscan-url "https://etherscan.io"}
   :graphql {:schema graphql-schema
             :url "http://api.registry.district0x.io/"}
   :ipfs {:host "https://ipfs.district0x.io/api"
          :endpoint "/api/v0"
          :gateway "https://ipfs.district0x.io/gateway/ipfs"}
   :router {:html5? true}
   :router-google-analytics {:enabled? true}
   :aragon-url "https://mainnet.aragon.org/#/"})

(def config-map
  (condp = (get-environment)
    "prod" production-config
    "qa" qa-config
    "qa-dev" qa-dev-config
    "dev" development-config))
