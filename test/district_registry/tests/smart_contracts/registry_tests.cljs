(ns district-registry.tests.smart-contracts.registry-tests
  (:require
    [bignumber.core :as bn]
    [cljs-promises.async :refer-macros [<?]]
    [cljs-web3.eth :as web3-eth]
    [cljs.test :refer-macros [deftest is testing use-fixtures async]]
    [clojure.core.async :refer [<! go]]
    [district-registry.server.contract.ds-auth :as ds-auth]
    [district-registry.server.contract.eternal-db :as eternal-db]
    [district-registry.server.contract.mutable-forwarder :as mutable-forwarder]
    [district-registry.server.contract.registry :as registry]
    [district-registry.server.contract.registry-entry :as registry-entry]
    [district-registry.shared.utils :refer [reg-entry-status->kw]]
    [district-registry.tests.smart-contracts.utils :refer [create-district]]
    [district.cljs-utils :as cljs-utils]
    [district.server.smart-contracts :refer [contract-address]]
    [district.server.web3 :refer [web3]]
    [district.web3-utils :refer [eth->wei-number eth->wei]]
    [print.foo :include-macros true :refer [look]]))

(def meta-hash1 "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH")

(deftest set-apps-parameters
  (async done
    (go
      (let [[owner non-owner] (web3-eth/accounts @web3)]

        (testing "District Registry can have factory set under valid conditions"
          (is (<? (registry/set-factory :district-registry-fwd
                                        {:factory (contract-address :district-factory)
                                         :factory? true}
                                        {:from owner}))))

        (testing "District Registry cannot have factory set without permissions"
          (is (nil? (try (<? (registry/set-factory :district-registry-fwd
                                                   {:factory (contract-address :district-factory)
                                                    :factory? true}
                                                   {:from non-owner}))
                         (catch :default)))))

        (done)))))


(deftest add-registry-entry
  (async done
    (go
      (let [[owner other-addr] (web3-eth/accounts @web3)]
        (testing "addRegistryEntry cannot be executed by user's address"
          (is (nil? (try (<? (registry/add-registry-entry :district-registry-fwd other-addr {:from owner}))
                         (catch :default)))))
        (done)))))


(deftest set-emergency
  (async done
    (go
      (let [[owner non-owner] (web3-eth/accounts @web3)
            [deposit] (->> (<? (eternal-db/get-uint-values :district-registry-db [:deposit]))
                        (map bn/number))
            aragon-id (cljs-utils/rand-str 10)]

        (testing "Non owner cannot set emergency"
          (is (nil? (try (<? (registry/set-emergency :district-registry-fwd true {:from non-owner}))
                         (catch :default)))))

        (testing "Owner can set emergency"
          (is (<? (registry/set-emergency :district-registry-fwd true {:from owner})))
          (true? (<? (registry/emergency? :district-registry-fwd))))

        (testing "Cannot create district during an emergency"
          (is (nil? (<! (create-district owner deposit meta-hash1 1000000 aragon-id)))))

        (testing "Can create district without emergency"
          (is (<? (registry/set-emergency :district-registry-fwd false {:from owner})))
          (let [event-args (<! (create-district owner deposit meta-hash1 1000000 aragon-id))
                registry-entry (:registry-entry event-args)]

            (is (<? (registry/set-emergency :district-registry-fwd true {:from owner})))

            (testing "Cannot create challenge during emergency"
              (is (nil? (try (<? (registry-entry/approve-and-create-challenge registry-entry
                                                                              {:challenger non-owner
                                                                               :meta-hash meta-hash1
                                                                               :amount deposit}
                                                                              {:from non-owner}))
                             (catch :default)))))))

        (is (<? (registry/set-emergency :district-registry-fwd false {:from owner})))

        (done)))))


(deftest set-target
  (async done
    (go
      (let [[owner non-owner new-target] (web3-eth/accounts @web3)]

        (is (= (contract-address :district-registry)
               (<? (mutable-forwarder/target :district-registry-fwd {:ignore-forward? true}))))

        (is (= (contract-address :param-change-registry)
               (<? (mutable-forwarder/target :param-change-registry-fwd {:ignore-forward? true}))))

        (testing "Non owner cannot set new target to registry"
          (is (nil? (try (<? (mutable-forwarder/set-target :district-registry-fwd new-target {:from non-owner :ignore-forward? true}))
                         (catch :default))))

          (is (nil? (try (<? (mutable-forwarder/set-target :param-change-registry-fwd new-target {:from non-owner :ignore-forward? true}))
                         (catch :default)))))

        (testing "Owner can set new target to registry"
          (is (<? (mutable-forwarder/set-target :district-registry-fwd new-target {:from owner :ignore-forward? true})))
          (is (<? (mutable-forwarder/set-target :param-change-registry-fwd new-target {:from owner :ignore-forward? true})))

          (is (= new-target
                 (<? (mutable-forwarder/target :district-registry-fwd {:ignore-forward? true}))))

          (is (= new-target
                 (<? (mutable-forwarder/target :param-change-registry-fwd {:ignore-forward? true})))))


        (is (<? (mutable-forwarder/set-target :district-registry-fwd (contract-address :district-registry) {:from owner :ignore-forward? true})))
        (is (<? (mutable-forwarder/set-target :param-change-registry-fwd (contract-address :param-change-registry) {:from owner :ignore-forward? true})))

        (done)))))