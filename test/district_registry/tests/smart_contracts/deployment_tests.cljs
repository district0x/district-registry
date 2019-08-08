(ns district-registry.tests.smart-contracts.deployment-tests
  (:require
    [bignumber.core :as bn]
    [cljs-promises.async :refer-macros [<?]]
    [cljs-time.coerce :refer [to-epoch from-long]]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs.test :refer-macros [deftest is testing run-tests use-fixtures async]]
    [clojure.core.async :as async :refer [<! go]]
    [district-registry.server.contract.dnt :as dnt]
    [district-registry.server.contract.ds-auth :as ds-auth]
    [district-registry.server.contract.ds-guard :as ds-guard]
    [district-registry.server.contract.minime-token :as minime-token]
    [district-registry.server.contract.registry :as registry]
    [district.server.config]
    [district.server.smart-contracts :refer [contract-address]]
    [district.server.web3 :refer [web3]]
    [district.shared.async-helpers :refer [promise->]]
    [district.web3-utils :as web3-utils]
    [print.foo :include-macros true]))


(deftest deployment-tests
  (testing "testing if deployment was succesfull"
    (let [[addr0] (web3-eth/accounts @web3)]
      (async done
        (go
          (is (= (<? (ds-auth/authority :ds-guard))
                 (contract-address :ds-guard)))

          (is (web3-utils/zero-address? (<? (dnt/controller))))

          (is (= (<? (ds-auth/authority :district-registry-db))
                 (contract-address :ds-guard)))

          (is (= (<? (ds-auth/authority :param-change-registry-db))
                 (contract-address :ds-guard)))

          (is (= (<? (ds-auth/authority :kit-district))
                 (contract-address :ds-guard)))

          (is (<? (ds-guard/can-call? {:src (contract-address :district-registry-fwd)
                                       :dst (contract-address :district-registry-db)
                                       :sig ds-guard/ANY})))

          (is (<? (ds-guard/can-call? {:src (contract-address :param-change-registry-fwd)
                                       :dst (contract-address :param-change-registry-db)
                                       :sig ds-guard/ANY})))

          (is (<? (ds-guard/can-call? {:src (contract-address :param-change-registry-fwd)
                                       :dst (contract-address :district-registry-db)
                                       :sig ds-guard/ANY})))

          (is (<? (ds-guard/can-call? {:src (contract-address :param-change-registry-fwd)
                                       :dst (contract-address :ds-guard)
                                       :sig ds-guard/ANY})))

          (is (false? (<? (ds-guard/can-call? {:src (contract-address :district-registry-fwd)
                                               :dst (contract-address :param-change-registry-db)
                                               :sig ds-guard/ANY}))))

          (is (<? (registry/factory? :district-registry-fwd
                                     (contract-address :district-factory))))

          (is (<? (registry/factory? :param-change-registry-fwd
                                     (contract-address :param-change-factory))))

          (is (bn/< 0 (<? (dnt/balance-of addr0))))

          (done))))))

