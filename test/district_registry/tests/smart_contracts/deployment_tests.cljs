(ns district-registry.tests.smart-contracts.deployment-tests
  (:require [bignumber.core :as bn]
            [cljs-time.coerce :refer [to-epoch from-long]]
            [cljs-web3-next.eth :as web3-eth]
            [cljs-web3-next.helpers :as web3-helpers]
            [cljs-web3-next.utils :as web3-utils]
            [cljs.test :refer-macros [deftest is testing run-tests use-fixtures async]]
            [cljs.core.async :refer [go <!]]
            [district-registry.server.contract.dnt :as dnt]
            [district-registry.server.contract.ds-auth :as ds-auth]
            [district-registry.server.contract.ds-guard :as ds-guard]
            [district-registry.server.contract.minime-token :as minime-token]
            [district-registry.server.contract.registry :as registry]
            [district.server.config]
            [district.server.smart-contracts :refer [contract-address]]
            [district.server.web3 :refer [web3]]))

(deftest deployment-tests
  (async done
         (go
           (let [[addr0] (<! (web3-eth/accounts @web3))]

             (is (= (<! (ds-auth/authority :ds-guard))
                    (web3-utils/address->checksum @web3 (contract-address :ds-guard))))

             (is (web3-helpers/zero-address? (<! (dnt/controller))))

             (is (= (<! (ds-auth/authority :district-registry-db))
                    (web3-utils/address->checksum @web3 (contract-address :ds-guard))))

             (is (= (<! (ds-auth/authority :param-change-registry-db))
                    (web3-utils/address->checksum @web3 (contract-address :ds-guard))))

             (is (= (<! (ds-auth/authority :kit-district))
                    (web3-utils/address->checksum @web3 (contract-address :ds-guard))))

             (is (= true (<! (ds-guard/can-call? {:src (contract-address :district-registry-fwd)
                                                  :dst (contract-address :district-registry-db)
                                                  :sig ds-guard/ANY}))))

             (is (= true (<! (ds-guard/can-call? {:src (contract-address :param-change-registry-fwd)
                                                  :dst (contract-address :param-change-registry-db)
                                                  :sig ds-guard/ANY}))))

             (is (= true (<! (ds-guard/can-call? {:src (contract-address :param-change-registry-fwd)
                                                  :dst (contract-address :district-registry-db)
                                                  :sig ds-guard/ANY}))))

             (is (= true (<! (ds-guard/can-call? {:src (contract-address :param-change-registry-fwd)
                                                  :dst (contract-address :ds-guard)
                                                  :sig ds-guard/ANY}))))

             (is (false? (<! (ds-guard/can-call? {:src (contract-address :district-registry-fwd)
                                                  :dst (contract-address :param-change-registry-db)
                                                  :sig ds-guard/ANY}))))

             (is (= true (<! (registry/factory? :district-registry-fwd
                                          (contract-address :district-factory)))))

             (is (= true (<! (registry/factory? :param-change-registry-fwd
                                                (contract-address :param-change-factory)))))

             (is (bn/< 0 (<! (dnt/balance-of addr0))))

             (done)))))
