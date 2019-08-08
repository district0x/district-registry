(ns district-registry.tests.smart-contracts.param-change-tests
  (:require
    [bignumber.core :as bn]
    [cljs-promises.async :refer-macros [<?]]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs-web3.evm :as web3-evm]
    [cljs.test :refer-macros [deftest is testing use-fixtures async]]
    [clojure.core.async :refer [<! go]]
    [district-registry.server.contract.challenge :as challenge]
    [district-registry.server.contract.dnt :as dnt]
    [district-registry.server.contract.eternal-db :as eternal-db]
    [district-registry.server.contract.param-change :as param-change]
    [district-registry.server.contract.param-change-factory :as param-change-factory]
    [district-registry.server.contract.param-change-registry :as param-change-registry]
    [district-registry.server.contract.registry :as registry]
    [district-registry.server.contract.registry-entry :as registry-entry]
    [district-registry.shared.utils :refer [vote-option->kw reg-entry-status->kw]]
    [district-registry.tests.smart-contracts.utils :refer [create-district]]
    [district.cljs-utils :as cljs-utils]
    [district.server.smart-contracts :refer [contract-address]]
    [district.server.web3 :refer [web3]]
    [district.web3-utils :as web3-utils :refer [eth->wei eth->wei-number]]
    [print.foo :include-macros true :refer [look]]))

(def meta-hash1 "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH")

(defn create-param-change [creator deposit db key value meta-hash]
  (go
    (try
      (let [tx (<? (param-change-factory/approve-and-create-param-change {:db db
                                                                          :key key
                                                                          :value value
                                                                          :meta-hash meta-hash
                                                                          :amount deposit}
                                                                         {:from creator}))]
        (-> (param-change-registry/param-change-constructed-event-in-tx tx)
          :args))
      (catch :default))))


(deftest approve-and-create-param-change
  (async done
    (go
      (let [[creator] (web3-eth/accounts @web3)
            [deposit challenge-period-duration] (->> (<? (eternal-db/get-uint-values :param-change-registry-db [:deposit :challenge-period-duration]))
                                                  (map bn/number))]

        (testing "Can create param change for district db under valid conditions"
          (let [event-args (<! (create-param-change creator deposit (contract-address :district-registry-db) :deposit (eth->wei 1.1) meta-hash1))
                {:keys [:registry-entry :db :key :value :deposit :challenge-period-end :meta-hash :timestamp]} event-args]
            (is (web3/address? registry-entry))
            (is (= creator (:creator event-args)))
            (is (= db (contract-address :district-registry-db)))
            (is (bn/> challenge-period-end 0))
            (is (bn/> timestamp 0))
            (is (= meta-hash1 (web3/to-ascii meta-hash)))
            (is (= (keyword key) :deposit))
            (is (= (bn/number value) (eth->wei-number 1.1)))

            (testing "Cannot apply change before challenge period is over"
              (is (nil? (try (<? (param-change-registry/apply-param-change registry-entry {:from creator}))
                             (catch :default)))))

            (web3-evm/increase-time! @web3 [(inc challenge-period-duration)])

            (testing "Can apply change when challenge period is over"
              (is (<? (param-change-registry/apply-param-change registry-entry {:from creator})))

              (is (= (bn/number (first (<? (eternal-db/get-uint-values :district-registry-db [:deposit]))))
                     (eth->wei-number 1.1))))))


        (testing "Can create param change for param change db under valid conditions"
          (let [event-args (<! (create-param-change creator deposit (contract-address :param-change-registry-db) :deposit (eth->wei 2) meta-hash1))
                {:keys [:registry-entry :db :key :value :deposit :challenge-period-end :meta-hash :timestamp]} event-args]
            (is (web3/address? registry-entry))
            (is (= creator (:creator event-args)))
            (is (= db (contract-address :param-change-registry-db)))
            (is (bn/> challenge-period-end 0))
            (is (bn/> timestamp 0))
            (is (= meta-hash1 (web3/to-ascii meta-hash)))
            (is (= (keyword key) :deposit))
            (is (= (bn/number value) (eth->wei-number 2)))

            (web3-evm/increase-time! @web3 [(inc challenge-period-duration)])

            (testing "Can apply change when challenge period is over"
              (is (<? (param-change-registry/apply-param-change registry-entry {:from creator})))

              (is (= (bn/number (first (<? (eternal-db/get-uint-values :param-change-registry-db [:deposit]))))
                     (eth->wei-number 2))))))

        (done)))))


(deftest create-challenge-and-vote-include
  (async done
    (go
      (let [[creator challenger voter] (web3-eth/accounts @web3)
            [deposit commit-period-duration reveal-period-duration] (->> (<? (eternal-db/get-uint-values :param-change-registry-db [:deposit :commit-period-duration :reveal-period-duration]))
                                                                      (map bn/number))
            event-args (<! (create-param-change creator deposit (contract-address :district-registry-db) :commit-period-duration 601 meta-hash1))
            registry-entry (:registry-entry event-args)
            salt (cljs-utils/rand-str 10)]

        (testing "Can challenge param change under valid conditions"
          (let [tx (<? (registry-entry/approve-and-create-challenge registry-entry
                                                                    {:challenger challenger
                                                                     :meta-hash meta-hash1
                                                                     :amount deposit}
                                                                    {:from challenger}))]
            (let [challenge-event-args (:args (registry/challenge-created-event-in-tx :param-change-registry-fwd tx))
                  {:keys [:meta-hash :reveal-period-end :index :reward-pool :challenger :commit-period-end]} challenge-event-args]
              (is (= challenger (:challenger challenge-event-args)))
              (is (= meta-hash (web3/to-hex meta-hash1)))
              (is (= 0 (bn/number index)))
              (is (= registry-entry (:registry-entry challenge-event-args)))
              (is (bn/> reveal-period-end 0))
              (is (bn/> commit-period-end 0))
              (is (false? (<? (registry-entry/is-challengeable? registry-entry))))
              (is (true? (<? (registry-entry/is-challenge-period-active? registry-entry))))))


          (testing "Challenge contract is correctly constructed"
            (let [challenge-addr (<? (registry-entry/current-challenge registry-entry))]
              (is (web3/address? challenge-addr))
              (is (= (reg-entry-status->kw (bn/number (<? (challenge/status challenge-addr))))
                     :reg-entry.status/commit-period))
              (is (true? (<? (challenge/was-challenged? challenge-addr))))
              (is (true? (<? (challenge/is-vote-commit-period-active? challenge-addr))))))


          (testing "Param change cannot be applied during commit period"
            (is (nil? (try (<? (param-change-registry/apply-param-change registry-entry {:from creator}))
                           (catch :default)))))


          (testing "Can vote in challenge"
            (let [tx (<? (registry-entry/approve-and-commit-vote registry-entry
                                                                 {:amount (web3-utils/eth->wei 1)
                                                                  :vote-option :vote-option/include
                                                                  :salt salt}
                                                                 {:from voter}))
                  vote-event-args (:args (registry/vote-committed-event-in-tx :param-change-registry-fwd tx))]
              (is (= voter (:voter vote-event-args)))
              (is (= (bn/number (web3-utils/eth->wei 1))
                     (bn/number (:amount vote-event-args))))))

          (web3-evm/increase-time! @web3 [(inc commit-period-duration)])

          (testing "Can reveal vote"
            (let [tx (<? (registry-entry/reveal-vote registry-entry
                                                     {:vote-option :vote-option/include
                                                      :salt salt}
                                                     {:from voter}))
                  reveal-event-args (:args (registry/vote-revealed-event-in-tx :param-change-registry-fwd tx))]

              (is (= (bn/number (web3-utils/eth->wei 1))
                     (bn/number (:amount reveal-event-args))))
              (is (= voter (:voter reveal-event-args)))
              (is (= :vote-option/include (vote-option->kw (bn/number (:option reveal-event-args)))))))


          (testing "Can claim vote reward"
            (web3-evm/increase-time! @web3 [(inc reveal-period-duration)])
            (let [tx (<? (registry-entry/claim-reward registry-entry {:from voter}))
                  vote-reward-event-args (:args (registry/vote-reward-claimed-event-in-tx :param-change-registry-fwd tx))]

              (is (= voter (:voter vote-reward-event-args)))
              (is (= (web3-utils/wei->eth-number (:amount vote-reward-event-args))
                     (/ (web3-utils/wei->eth-number deposit) 2)))))


          (testing "Can apply change when reveal period is over"
            (is (<? (param-change-registry/apply-param-change registry-entry {:from creator})))

            (is (= (bn/number (first (<? (eternal-db/get-uint-values :district-registry-db [:commit-period-duration]))))
                   601))))

        (done)))))


(deftest create-challenge-and-vote-exclude
  (async done
    (go
      (let [[creator challenger voter] (web3-eth/accounts @web3)
            [deposit commit-period-duration reveal-period-duration] (->> (<? (eternal-db/get-uint-values :param-change-registry-db [:deposit :commit-period-duration :reveal-period-duration]))
                                                                      (map bn/number))
            event-args (<! (create-param-change creator deposit (contract-address :param-change-registry-db) :challenge-period-duration 602 meta-hash1))
            registry-entry (:registry-entry event-args)
            salt (cljs-utils/rand-str 10)
            _ (<? (registry-entry/approve-and-create-challenge registry-entry
                                                               {:challenger challenger
                                                                :meta-hash meta-hash1
                                                                :amount deposit}
                                                               {:from challenger}))
            challenge (<? (registry-entry/current-challenge registry-entry))]


        (testing "Can vote in challenge"
          (let [tx (<? (registry-entry/approve-and-commit-vote registry-entry
                                                               {:amount (web3-utils/eth->wei 1)
                                                                :vote-option :vote-option/exclude
                                                                :salt salt}
                                                               {:from voter}))
                vote-event-args (:args (registry/vote-committed-event-in-tx :param-change-registry-fwd tx))]
            (is (= voter (:voter vote-event-args)))
            (is (= (bn/number (web3-utils/eth->wei 1))
                   (bn/number (:amount vote-event-args))))))

        (web3-evm/increase-time! @web3 [(inc commit-period-duration)])

        (testing "Param change cannot be applied during reveal period"
          (is (nil? (try (<? (param-change-registry/apply-param-change registry-entry {:from creator}))
                         (catch :default)))))

        (testing "Can reveal vote"
          (let [tx (<? (registry-entry/reveal-vote registry-entry
                                                   {:vote-option :vote-option/exclude
                                                    :salt salt}
                                                   {:from voter}))
                reveal-event-args (:args (registry/vote-revealed-event-in-tx :param-change-registry-fwd tx))]

            (is (= (bn/number (web3-utils/eth->wei 1))
                   (bn/number (:amount reveal-event-args))))
            (is (= voter (:voter reveal-event-args)))
            (is (= :vote-option/exclude (vote-option->kw (bn/number (:option reveal-event-args)))))))

        (web3-evm/increase-time! @web3 [(inc reveal-period-duration)])

        (testing "Can claim vote reward"
          (let [tx (<? (registry-entry/claim-reward registry-entry {:from voter}))
                vote-reward-event-args (:args (registry/vote-reward-claimed-event-in-tx :param-change-registry-fwd tx))]

            (is (= voter (:voter vote-reward-event-args)))
            (is (= (web3-utils/wei->eth-number (:amount vote-reward-event-args))
                   (/ (web3-utils/wei->eth-number deposit) 2)))
            (is (true? (<? (challenge/is-vote-reward-claimed? challenge voter))))))


        (testing "Can claim challenger reward"
          (let [tx (<? (registry-entry/claim-reward registry-entry {:from challenger}))
                challenger-reward-event-args (:args (registry/challenger-reward-claimed-event-in-tx :param-change-registry-fwd tx))]

            (is (= challenger (:challenger challenger-reward-event-args)))
            (is (= (web3-utils/wei->eth-number (:amount challenger-reward-event-args))
                   (+ (/ (web3-utils/wei->eth-number deposit) 2)
                      (web3-utils/wei->eth-number deposit))))
            (is (true? (<? (challenge/is-challenge-reward-claimed? challenge))))))


        (testing "Param change cannot be applied if param change got blacklisted"
          (is (nil? (try (<? (param-change-registry/apply-param-change registry-entry {:from creator}))
                         (catch :default)))))

        (done)))))


(deftest multiple-param-changes-with-same-key
  (async done
    (go
      (let [[creator other-addr] (web3-eth/accounts @web3)
            [deposit challenge-period-duration] (->> (<? (eternal-db/get-uint-values :param-change-registry-db [:deposit :challenge-period-duration]))
                                                  (map bn/number))
            new-value1 (+ 20 (rand-int 1000))
            event-args1 (<! (create-param-change creator deposit (contract-address :district-registry-db) :reveal-period-duration new-value1 meta-hash1))
            registry-entry1 (:registry-entry event-args1)
            new-value2 (+ 20 (rand-int 1000))
            event-args2 (<! (create-param-change creator deposit (contract-address :district-registry-db) :reveal-period-duration new-value2 meta-hash1))
            registry-entry2 (:registry-entry event-args2)]

        (web3-evm/increase-time! @web3 [(inc challenge-period-duration)])

        (testing "Can apply first param change"
          (let [previous-balance (<? (dnt/balance-of creator))]
            (is (<? (param-change-registry/apply-param-change registry-entry1 {:from other-addr})))

            (testing "It transferred deposit back to the creator"
              (is (bn/< previous-balance (<? (dnt/balance-of creator)))))

            (is (= (bn/number (first (<? (eternal-db/get-uint-values :district-registry-db [:reveal-period-duration]))))
                   new-value1))))

        (testing "Cannot apply second param change, because original value doesn't match"
          (is (nil? (try (<? (param-change-registry/apply-param-change registry-entry2 {:from other-addr}))
                         (catch :default)))))

        (done)))))
