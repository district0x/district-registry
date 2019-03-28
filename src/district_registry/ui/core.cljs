(ns district-registry.ui.core
  (:require
   [cljs.spec.alpha :as s]
   [clojure.string :as str]
   [district-registry.shared.graphql-schema :refer [graphql-schema]]
   [district-registry.shared.routes :refer [routes]]
   [district-registry.shared.smart-contracts :refer [smart-contracts]]
   [district-registry.ui.about.page]
   [district-registry.ui.detail.page]
   [district-registry.ui.home.page]
   [district-registry.ui.ipfs]
   [district-registry.ui.not-found.page]
   [district-registry.ui.submit.page]
   [district.ui.component.router :refer [router]]
   [district.ui.graphql]
   [district.ui.notification]
   [district.ui.now]
   [district.ui.reagent-render]
   [district.ui.router-google-analytics]
   [district.ui.router]
   [district.ui.smart-contracts]
   [district.ui.web3-account-balances]
   [district.ui.web3-accounts]
   [district.ui.web3-balances]
   [district.ui.web3-tx-id]
   [district.ui.web3-tx-log]
   [district.ui.web3-tx]
   [district.ui.web3]
   [district.ui.window-size]
   [mount.core :as mount]
   [print.foo :include-macros true]))

(def debug? ^boolean js/goog.DEBUG)

(def skipped-contracts [:ds-guard :param-change-registry-db :district-registry-db :minime-token-factory])

(defn ^:export init []
  (s/check-asserts debug?)
  (enable-console-print!)
  (-> (mount/with-args
        {:web3 {:url "http://localhost:8549"}
         :smart-contracts {:contracts (apply dissoc smart-contracts skipped-contracts)}
         :web3-balances {:contracts (select-keys smart-contracts [:DNT])}
         :web3-account-balances {:for-contracts [:ETH :DNT]}
         :web3-tx-log {:open-on-tx-hash? true
                       :tx-costs-currencies [:USD]}
         :reagent-render {:id "app"
                          :component-var #'router}
         :router {:routes routes
                  :default-route :route/not-found}
         :router-google-analytics {:enabled? (not debug?)}
         :graphql {:schema graphql-schema
                   :url "http://localhost:6300/graphql"}
         :ipfs {:host "http://127.0.0.1:5001" :endpoint "/api/v0"}})
    (mount/start)))
