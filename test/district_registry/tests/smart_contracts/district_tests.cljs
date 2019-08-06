(ns district-registry.tests.smart-contracts.district-tests
  (:require
    [bignumber.core :as bn]
    [cljs-promises.async :refer-macros [<?]]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs-web3.evm :as web3-evm]
    [cljs.test :as test :refer-macros [deftest is testing use-fixtures async]]
    [clojure.core.async :refer [<! go]]
    [district-registry.server.contract.district-factory :as district-factory]
    [district-registry.server.contract.eternal-db :as eternal-db]
    [district-registry.server.contract.registry :as registry]
    [district-registry.server.contract.registry-entry :as registry-entry]
    [district-registry.shared.utils :refer [vote-option->kw]]
    [district-registry.tests.smart-contracts.utils :refer [tx-error?]]
    [district.cljs-utils :as cljs-utils]
    [district.server.web3 :refer [web3]]
    [district.web3-utils :as web3-utils]
    [print.foo :include-macros true :refer [look]]))

(def meta-hash1 "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH")

(defn create-district
  "Creates a district and returns construct events args"
  [& [creator deposit meta-hash dnt-weight aragon-id]]
  (go
    (try
      (let [tx (<? (district-factory/approve-and-create-district {:meta-hash meta-hash
                                                                  :dnt-weight dnt-weight
                                                                  :aragon-id aragon-id
                                                                  :amount deposit}
                                                                 {:from creator}))]
        (-> (registry/district-constructed-event-in-tx :district-registry-fwd tx)
          :args))
      (catch js/Error _
        nil))))

(deftest approve-and-create-district-test
  (async done
    (go
      (let [[creator] (web3-eth/accounts @web3)
            [deposit] (->> (<? (eternal-db/get-uint-values :district-registry-db [:deposit]))
                        (map bn/number))
            aragon-id (cljs-utils/rand-str 10)
            event-args (<! (create-district creator deposit meta-hash1 1000000 aragon-id))]

        (testing "District can be created under valid conditions"
          (let [{:keys [:registry-entry :dnt-weight :aragon-dao :version :timestamp :meta-hash]} event-args]
            (is (web3/address? registry-entry))
            (is (web3/address? aragon-dao))
            (is (= meta-hash (web3/to-hex meta-hash1)))
            (is (= (bn/number (:deposit event-args)) deposit))
            (is (= (bn/number version) 1))
            (is (pos? (bn/number timestamp)))
            (is (= aragon-id (:aragon-id event-args)))
            (is (= (bn/number dnt-weight) 1000000))

            (is (true? (<? (registry-entry/is-challengeable? registry-entry))))))

        (testing "Cannot create district with same aragonId"
          (is (nil? (<! (create-district creator deposit meta-hash1 1000000 aragon-id)))))

        (testing "Cannot create district with higher dnt weight than max weight"
          (is (nil? (<! (create-district creator deposit meta-hash1 1000001 (cljs-utils/rand-str 10))))))

        (done)))))


(deftest create-challenge-and-vote-include
  (async done
    (go
      (let [[creator challenger voter] (web3-eth/accounts @web3)
            [deposit challenge-period-duration reveal-period-duration] (->> (<? (eternal-db/get-uint-values :district-registry-db [:deposit :commit-period-duration :reveal-period-duration]))
                                                                         (map bn/number))
            aragon-id (cljs-utils/rand-str 10)
            salt (cljs-utils/rand-str 10)
            event-args (<! (create-district creator deposit meta-hash1 1000000 aragon-id))
            registry-entry (:registry-entry event-args)]

        (testing "District can be challenged under valid conditions"
          (let [tx (<? (registry-entry/approve-and-create-challenge registry-entry
                                                                    {:challenger challenger
                                                                     :meta-hash meta-hash1
                                                                     :amount deposit}
                                                                    {:from challenger}))]
            (let [challenge-event-args (:args (registry/challenge-created-event-in-tx :district-registry-fwd tx))
                  {:keys [:meta-hash :reveal-period-end :index :reward-pool :challenger :commit-period-end]} challenge-event-args]
              (is (= challenger (:challenger challenge-event-args)))
              (is (= meta-hash (web3/to-hex meta-hash1)))
              (is (= 0 (bn/number index)))
              (is (= 500000000000000000 (bn/number reward-pool)))
              (is (= registry-entry (:registry-entry challenge-event-args)))
              (is (bn/> reveal-period-end 0))
              (is (bn/> commit-period-end 0)))))

        (testing "Can vote in challenge"
          (let [tx (<? (registry-entry/approve-and-commit-vote registry-entry
                                                               {:amount (web3-utils/eth->wei 1)
                                                                :vote-option :vote-option/include
                                                                :salt salt}
                                                               {:from voter}))
                vote-event-args (:args (registry/vote-committed-event-in-tx :district-registry-fwd tx))]
            (is (= voter (:voter vote-event-args)))
            (is (= (bn/number (web3-utils/eth->wei 1))
                   (bn/number (:amount vote-event-args))))))

        (testing "Can reveal vote"
          (web3-evm/increase-time! @web3 [(inc challenge-period-duration)])
          (let [tx (<? (registry-entry/reveal-vote registry-entry
                                                   {:vote-option :vote-option/include
                                                    :salt salt}
                                                   {:from voter}))
                reveal-event-args (:args (registry/vote-revealed-event-in-tx :district-registry-fwd tx))]

            (is (= (bn/number (web3-utils/eth->wei 1))
                   (bn/number (:amount reveal-event-args))))
            (is (= voter (:voter reveal-event-args)))
            (is (= :vote-option/include (vote-option->kw (bn/number (:option reveal-event-args)))))))

        (testing "Can claim vote reward"
          (web3-evm/increase-time! @web3 [(inc reveal-period-duration)])
          (let [tx (<? (registry-entry/claim-reward registry-entry {:from voter}))
                vote-reward-event-args (:args (registry/vote-reward-claimed-event-in-tx :district-registry-fwd tx))]

            (is (= voter (:voter vote-reward-event-args)))
            (is (= (web3-utils/wei->eth-number (:amount vote-reward-event-args))
                   (/ (web3-utils/wei->eth-number deposit) 2)))))

        (testing "Can claim creator reward"
          (let [tx (<? (registry-entry/claim-reward registry-entry {:from creator}))
                creator-reward-event-args (:args (registry/creator-reward-claimed-event-in-tx :district-registry-fwd tx))]

            (is (= creator (:creator creator-reward-event-args)))
            (is (= (web3-utils/wei->eth-number (:amount creator-reward-event-args))
                   (/ (web3-utils/wei->eth-number deposit) 2)))))

        (done)))))