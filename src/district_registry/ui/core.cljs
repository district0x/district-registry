(ns district-registry.ui.core
  (:require
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [district-registry.shared.graphql-schema :refer [graphql-schema]]
    [district-registry.shared.routes :refer [routes]]
    [district-registry.ui.about.page]
    [district-registry.ui.config :as config]
    [district-registry.ui.detail.page]
    [district-registry.ui.home.page]
    [district-registry.ui.not-found.page]
    [district-registry.ui.submit.page]
    [district.cljs-utils :as cljs-utils]
    [district.ui.component.router :refer [router]]
    [district.ui.graphql.events]
    [district.ui.graphql]
    [district.ui.ipfs]
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

(defn dev-setup! []
  (when (:debug? config/config-map)
    (s/check-asserts true)
    (enable-console-print!)))

(defn ^:export init []
  (dev-setup!)
  (let [full-config (cljs-utils/merge-in
                      config/config-map
                      {:smart-contracts {:format :truffle-json}
                       :web3-account-balances {:for-contracts [:ETH :DNT]}
                       :web3-tx-log {:open-on-tx-hash? true
                                     :tx-costs-currencies [:USD]}
                       :reagent-render {:id "app"
                                        :component-var #'router}
                       :router {:routes routes
                                :default-route :route/not-found}
                       :notification {:default-show-duration 3000
                                      :default-hide-duration 1000}})]

    (js/console.log "Entire config:" (clj->js full-config))
    (-> (mount/with-args full-config)
      (mount/start))))
