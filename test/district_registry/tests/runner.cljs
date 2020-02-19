(ns district-registry.tests.runner
  (:require
   [cljs.nodejs :as nodejs]
   [cljs.test :refer [run-tests]]
   [district-registry.shared.smart-contracts-dev :refer [smart-contracts]]
   [district-registry.tests.smart-contracts.deployment-tests]
   [district-registry.tests.smart-contracts.district-tests]
   [district-registry.tests.smart-contracts.kit-district-tests]
   [district-registry.tests.smart-contracts.param-change-tests]
   [district-registry.tests.smart-contracts.registry-entry-tests]
   [district-registry.tests.smart-contracts.registry-tests]
   [district.server.logging]
   [district.server.web3]
   [district.server.smart-contracts]
   [district.shared.async-helpers :as async-helpers]
   [mount.core :as mount]
   [doo.runner :refer-macros [doo-tests]]
   [taoensso.timbre :as log]))

(nodejs/enable-util-print!)

(async-helpers/extend-promises-as-channels!)

(defn start-and-run-tests []
  (-> (mount/with-args {:web3 {:url "ws://127.0.0.1:8545"}
                        :smart-contracts {:contracts-var #'smart-contracts}
                        :logging {:level :info
                                  :console? false}})
      (mount/only [#'district.server.logging/logging
                   #'district.server.web3/web3
                   #'district.server.smart-contracts/smart-contracts])
      (mount/start)
      (as-> $ (log/warn "Started" $)))

  (run-tests
   'district-registry.tests.smart-contracts.deployment-tests
   'district-registry.tests.smart-contracts.registry-entry-tests
   'district-registry.tests.smart-contracts.district-tests
   'district-registry.tests.smart-contracts.registry-tests
   'district-registry.tests.smart-contracts.kit-district-tests
   'district-registry.tests.smart-contracts.param-change-tests))

(start-and-run-tests)
