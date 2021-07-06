(ns district-registry.tests.smart-contracts.registry-tests
  (:require [bignumber.core :as bn]
            [cljs-web3-next.eth :as web3-eth]
            [cljs-web3-next.utils :as web3-utils]
            [cljs.test :refer-macros [async deftest is testing]]
            [cljs.core.async :refer [go <!]]
            [district-registry.server.contract.eternal-db :as eternal-db]
            [district-registry.server.contract.mutable-forwarder :as mutable-forwarder]
            [district-registry.server.contract.registry :as registry]
            [district-registry.server.contract.registry-entry :as registry-entry]
            [district-registry.tests.smart-contracts.utils :refer [create-district tx-error? next-ens-name]]
            [district.cljs-utils :as cljs-utils]
            [district.server.smart-contracts :refer [contract-address]]
            [district.server.web3 :refer [web3]]))

(def meta-hash1 "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH")

(deftest set-apps-parameters
  (async done
         (go
           (let [[owner non-owner] (<! (web3-eth/accounts @web3))
                 tx (<! (registry/set-factory :district-registry-fwd
                                              {:factory (contract-address :district-factory)
                                               :factory? true}
                                              {:from owner}))]

             (testing "District Registry can have factory set under valid conditions"
               (is tx))

             (testing "District Registry cannot have factory set without permissions"
               (is (tx-error? (<! (registry/set-factory :district-registry-fwd
                                                        {:factory (contract-address :district-factory)
                                                         :factory? true}
                                                        {:from non-owner})))))

             (done)))))

(deftest add-registry-entry
  (async done
         (go
           (let [[owner other-addr] (<! (web3-eth/accounts @web3))]
             (testing "addRegistryEntry cannot be executed by user's address"
               (is (tx-error? (<! (registry/add-registry-entry :district-registry-fwd other-addr {:from owner})))))
             (done)))))

(deftest set-emergency
  (async done
         (go
           (let [[owner non-owner] (<! (web3-eth/accounts @web3))
                 [deposit] (->> (<! (eternal-db/get-uint-values :district-registry-db [:deposit]))
                                (map bn/number))
                 ens-name (next-ens-name)]

             (testing "Non owner cannot set emergency"
               (is (tx-error? (<! (registry/set-emergency :district-registry-fwd true {:from non-owner})))))

             (testing "Owner can set emergency"
               (let [tx (<! (registry/set-emergency :district-registry-fwd true {:from owner}))]
                 (is tx)
                 (true? (<! (registry/emergency? :district-registry-fwd)))))

             (testing "Cannot create district during an emergency"
               (is (tx-error? (<! (create-district owner deposit meta-hash1 ens-name)))))

             (testing "Can create district without emergency"
               (<! (registry/set-emergency :district-registry-fwd false {:from owner}))
               (let [event-args (<! (create-district owner deposit meta-hash1 ens-name))
                     registry-entry (:registry-entry event-args)]

                 (<! (registry/set-emergency :district-registry-fwd true {:from owner}))

                 (testing "Cannot create challenge during emergency"
                   (is (tx-error? (<! (registry-entry/approve-and-create-challenge registry-entry
                                                                                   {:challenger non-owner
                                                                                    :meta-hash meta-hash1
                                                                                    :amount deposit}
                                                                                   {:from non-owner})))))))

             (<! (registry/set-emergency :district-registry-fwd false {:from owner}))

             (done)))))

(deftest set-target
    (async done
           (go
             (let [[owner non-owner new-target] (<! (web3-eth/accounts @web3))]

               (is (= (web3-utils/address->checksum @web3 (contract-address :district-registry))
                      (<! (mutable-forwarder/target :district-registry-fwd))))

               (is (= (web3-utils/address->checksum @web3 (contract-address :param-change-registry))
                      (<! (mutable-forwarder/target :param-change-registry-fwd))))

               (testing "Non owner cannot set new target to registry"
                 (is (tx-error? (<! (mutable-forwarder/set-target :district-registry-fwd new-target {:from non-owner :ignore-forward? true}))))

                 (is (tx-error? (<! (mutable-forwarder/set-target :param-change-registry-fwd new-target {:from non-owner :ignore-forward? true})))))

               (testing "Owner can set new target to registry"
                 (<! (mutable-forwarder/set-target :district-registry-fwd new-target {:from owner :ignore-forward? true}))
                 (<! (mutable-forwarder/set-target :param-change-registry-fwd new-target {:from owner :ignore-forward? true}))

                 (is (= new-target
                        (<! (mutable-forwarder/target :district-registry-fwd))))

                 (is (= new-target
                        (<! (mutable-forwarder/target :param-change-registry-fwd)))))

               (<! (mutable-forwarder/set-target :district-registry-fwd (contract-address :district-registry) {:from owner :ignore-forward? true}))
               (<! (mutable-forwarder/set-target :param-change-registry-fwd (contract-address :param-change-registry) {:from owner :ignore-forward? true}))

               (done)))))
