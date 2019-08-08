(ns district-registry.tests.smart-contracts.district-tests
  (:require
    [bignumber.core :as bn]
    [cljs-promises.async :refer-macros [<?]]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs-web3.evm :as web3-evm]
    [cljs.test :refer-macros [deftest is testing use-fixtures async]]
    [clojure.core.async :refer [<! go]]
    [district-registry.server.contract.challenge :as challenge]
    [district-registry.server.contract.district :as district]
    [district-registry.server.contract.eternal-db :as eternal-db]
    [district-registry.server.contract.registry :as registry]
    [district-registry.server.contract.registry-entry :as registry-entry]
    [district-registry.server.contract.stake-bank :as stake-bank]
    [district-registry.shared.utils :refer [vote-option->kw reg-entry-status->kw]]
    [district-registry.tests.smart-contracts.utils :refer [create-district tx-error?]]
    [district.cljs-utils :as cljs-utils]
    [district.server.web3 :refer [web3]]
    [district.web3-utils :refer [eth->wei-number eth->wei]]
    [print.foo :include-macros true :refer [look]]))

(def meta-hash1 "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH")
(def meta-hash2 "QmdsBbZgkwTJgaxfGgegGxLJW72RjU1bpWzvMe1vXxXEGf")

(deftest approve-and-stake-for-dnt-weight-1000000
  (async done
    (go
      (let [[creator staker1 staker2] (web3-eth/accounts @web3)
            [deposit] (->> (<? (eternal-db/get-uint-values :district-registry-db [:deposit]))
                        (map bn/number))
            aragon-id (cljs-utils/rand-str 10)
            event-args (<! (create-district creator deposit meta-hash1 1000000 aragon-id))
            registry-entry (:registry-entry event-args)
            stake-bank (<? (district/stake-bank registry-entry))]

        (testing "District can be staked to under valid conditions"
          (let [tx (<? (district/approve-and-stake-for registry-entry
                                                       {:amount (eth->wei 1)
                                                        :user staker1}
                                                       {:from staker1}))]
            (let [challenge-event-args (:args (registry/district-stake-changed-event-in-tx :district-registry-fwd tx))
                  {:keys [:staker-dnt-staked :is-unstake :voting-token-total-supply :stake-id :dnt-total-staked :staked-amount :staker :staker-voting-token-balance]} challenge-event-args]
              (is (= (bn/number staker-dnt-staked)
                     (eth->wei-number 1)
                     (bn/number dnt-total-staked)
                     (bn/number staked-amount)
                     (bn/number (<? (stake-bank/total-staked stake-bank)))
                     (bn/number (<? (stake-bank/total-staked-for stake-bank staker1)))))
              (is (false? is-unstake))
              (is (= (bn/number voting-token-total-supply)
                     (bn/number staker-voting-token-balance)
                     9999000099990000999))
              (is (= 0 (bn/number stake-id)))
              (is (= staker staker1)))))


        (testing "District can be staked to, multiple times"
          (let [tx (<? (district/approve-and-stake-for registry-entry
                                                       {:amount (eth->wei 2)
                                                        :user staker2}
                                                       {:from staker2}))]
            (let [challenge-event-args (:args (registry/district-stake-changed-event-in-tx :district-registry-fwd tx))
                  {:keys [:staker-dnt-staked :is-unstake :voting-token-total-supply :stake-id :dnt-total-staked :staked-amount :staker :staker-voting-token-balance]} challenge-event-args]
              (is (= (bn/number staker-dnt-staked)
                     (eth->wei-number 2)
                     (bn/number staked-amount)
                     (bn/number (<? (stake-bank/total-staked-for stake-bank staker2)))))

              (is (= (bn/number (<? (stake-bank/total-staked stake-bank)))
                     (eth->wei-number 3)
                     (bn/number dnt-total-staked)))
              (is (false? is-unstake))
              (is (= (bn/number voting-token-total-supply) 23331222425905803000))
              (is (= (bn/number staker-voting-token-balance) 13332222325915804000))
              (is (= 1 (bn/number stake-id)))
              (is (= staker staker2)))))


        (testing "District can be unstaked from"
          (let [tx (<? (district/unstake registry-entry
                                         {:amount (eth->wei 0.75)}
                                         {:from staker1}))]
            (let [challenge-event-args (:args (registry/district-stake-changed-event-in-tx :district-registry-fwd tx))
                  {:keys [:staker-dnt-staked :is-unstake :voting-token-total-supply :stake-id :dnt-total-staked :staked-amount :staker :staker-voting-token-balance]} challenge-event-args]

              (is (= (eth->wei-number 0.75)
                     (bn/number staked-amount)))

              (is (= (bn/number staker-dnt-staked)
                     (eth->wei-number 0.25)
                     (bn/number (<? (stake-bank/total-staked-for stake-bank staker1)))))

              (is (= (bn/number (<? (stake-bank/total-staked stake-bank)))
                     (eth->wei-number 2.25)
                     (bn/number dnt-total-staked)))

              (is (true? is-unstake))
              (is (= (bn/number voting-token-total-supply) 15831972350913305000))
              (is (= (bn/number staker-voting-token-balance) 2499750024997500400))
              (is (= 2 (bn/number stake-id)))
              (is (= staker staker1)))))

        (done)))))


(deftest approve-and-stake-for-dnt-weight-500000
  (async done
    (go
      (let [[creator staker1 staker2] (web3-eth/accounts @web3)
            [deposit] (->> (<? (eternal-db/get-uint-values :district-registry-db [:deposit]))
                        (map bn/number))
            aragon-id (cljs-utils/rand-str 10)
            event-args (<! (create-district creator deposit meta-hash1 500000 aragon-id))
            registry-entry (:registry-entry event-args)]

        (testing "District can be staked to under valid conditions"
          (let [tx (<? (district/approve-and-stake-for registry-entry
                                                       {:amount (eth->wei 1)
                                                        :user staker1}
                                                       {:from staker1}))]
            (let [challenge-event-args (:args (registry/district-stake-changed-event-in-tx :district-registry-fwd tx))
                  {:keys [:voting-token-total-supply :staker-voting-token-balance]} challenge-event-args]
              (is (= (bn/number voting-token-total-supply)
                     (bn/number staker-voting-token-balance)
                     4141782101273517000)))))

        (testing "District can be staked to, multiple times"
          (let [tx (<? (district/approve-and-stake-for registry-entry
                                                       {:amount (eth->wei 2)
                                                        :user staker2}
                                                       {:from staker2}))]
            (let [challenge-event-args (:args (registry/district-stake-changed-event-in-tx :district-registry-fwd tx))
                  {:keys [:voting-token-total-supply :staker-voting-token-balance]} challenge-event-args]

              (is (= (bn/number voting-token-total-supply) 8256840478545516000))
              (is (= (bn/number staker-voting-token-balance) 4115058377271998500)))))


        (testing "District can be unstaked from"
          (let [tx (<? (district/unstake registry-entry
                                         {:amount (eth->wei 0.75)}
                                         {:from staker1}))]
            (let [challenge-event-args (:args (registry/district-stake-changed-event-in-tx :district-registry-fwd tx))
                  {:keys [:voting-token-total-supply :staker-voting-token-balance]} challenge-event-args]

              (is (= (bn/number voting-token-total-supply) 5150503902590378000))
              (is (= (bn/number staker-voting-token-balance) 1035445525318379300)))))

        (done)))))


(deftest approve-and-stake-for-dnt-weight-333333
  (async done
    (go
      (let [[creator staker1 staker2] (web3-eth/accounts @web3)
            [deposit] (->> (<? (eternal-db/get-uint-values :district-registry-db [:deposit]))
                        (map bn/number))
            aragon-id (cljs-utils/rand-str 10)
            event-args (<! (create-district creator deposit meta-hash1 333333 aragon-id))
            registry-entry (:registry-entry event-args)]

        (testing "District can be staked to under valid conditions"
          (let [tx (<? (district/approve-and-stake-for registry-entry
                                                       {:amount (eth->wei 1)
                                                        :user staker1}
                                                       {:from staker1}))]
            (let [challenge-event-args (:args (registry/district-stake-changed-event-in-tx :district-registry-fwd tx))
                  {:keys [:voting-token-total-supply :staker-voting-token-balance]} challenge-event-args]
              (is (= (bn/number voting-token-total-supply)
                     (bn/number staker-voting-token-balance)
                     2598997618827561000)))))

        (testing "District can be staked to, multiple times"
          (let [tx (<? (district/approve-and-stake-for registry-entry
                                                       {:amount (eth->wei 2)
                                                        :user staker2}
                                                       {:from staker2}))]
            (let [challenge-event-args (:args (registry/district-stake-changed-event-in-tx :district-registry-fwd tx))
                  {:keys [:voting-token-total-supply :staker-voting-token-balance]} challenge-event-args]

              (is (= (bn/number voting-token-total-supply) 4937694492970016000))
              (is (= (bn/number staker-voting-token-balance) 2338696874142454300)))))


        (testing "District can be unstaked from"
          (let [tx (<? (district/unstake registry-entry
                                         {:amount (eth->wei 0.75)}
                                         {:from staker1}))]
            (let [challenge-event-args (:args (registry/district-stake-changed-event-in-tx :district-registry-fwd tx))
                  {:keys [:voting-token-total-supply :staker-voting-token-balance]} challenge-event-args]

              (is (= (bn/number voting-token-total-supply) 2988446278849344500))
              (is (= (bn/number staker-voting-token-balance) 649749404706890200)))))

        (done)))))


(deftest staking-counts-as-vote-include
  (async done
    (go
      (let [[creator challenger staker1] (web3-eth/accounts @web3)
            [deposit commit-period-duration reveal-period-duration] (->> (<? (eternal-db/get-uint-values :district-registry-db [:deposit :commit-period-duration :reveal-period-duration]))
                                                                         (map bn/number))
            aragon-id (cljs-utils/rand-str 10)
            event-args (<! (create-district creator deposit meta-hash1 500000 aragon-id))
            registry-entry (:registry-entry event-args)
            salt (cljs-utils/rand-str 10)
            _ (<? (district/approve-and-stake-for registry-entry
                                                  {:amount (eth->wei 2)
                                                   :user staker1}
                                                  {:from staker1}))
            _ (<? (registry-entry/approve-and-create-challenge registry-entry
                                                               {:challenger challenger
                                                                :meta-hash meta-hash1
                                                                :amount deposit}
                                                               {:from challenger}))
            challenge (<? (registry-entry/current-challenge registry-entry))]

        (<? (registry-entry/approve-and-commit-vote registry-entry
                                                    {:amount (eth->wei 1)
                                                     :vote-option :vote-option/exclude
                                                     :salt salt}
                                                    {:from challenger}))

        (web3-evm/increase-time! @web3 [(inc commit-period-duration)])

        (<? (registry-entry/reveal-vote registry-entry
                                        {:vote-option :vote-option/exclude
                                         :salt salt}
                                        {:from challenger}))

        (web3-evm/increase-time! @web3 [(inc reveal-period-duration)])

        (testing "District is whitelisted after being challenged"
          (is (= (reg-entry-status->kw (bn/number (<? (challenge/status challenge))))
                 :reg-entry.status/whitelisted))

          (is (= (vote-option->kw (bn/number (<? (challenge/winning-vote-option challenge))))
                 :vote-option/include))

          (is (= (bn/number (<? (challenge/vote-option-include-amount challenge)))
                 (bn/number (<? (challenge/winning-vote-option-amount challenge)))
                 (eth->wei-number 2))))

        (done)))))


(deftest staking-in-commit-period-counts-as-vote-include
  (async done
    (go
      (let [[creator challenger staker1] (web3-eth/accounts @web3)
            [deposit commit-period-duration reveal-period-duration] (->> (<? (eternal-db/get-uint-values :district-registry-db [:deposit :commit-period-duration :reveal-period-duration]))
                                                                         (map bn/number))
            aragon-id (cljs-utils/rand-str 10)
            event-args (<! (create-district creator deposit meta-hash1 500000 aragon-id))
            registry-entry (:registry-entry event-args)
            salt (cljs-utils/rand-str 10)
            _ (<? (registry-entry/approve-and-create-challenge registry-entry
                                                               {:challenger challenger
                                                                :meta-hash meta-hash1
                                                                :amount deposit}
                                                               {:from challenger}))
            challenge (<? (registry-entry/current-challenge registry-entry))]

        (<? (registry-entry/approve-and-commit-vote registry-entry
                                                    {:amount (eth->wei 1)
                                                     :vote-option :vote-option/exclude
                                                     :salt salt}
                                                    {:from challenger}))

        (<? (district/approve-and-stake-for registry-entry
                                            {:amount (eth->wei 2)
                                             :user staker1}
                                            {:from staker1}))

        (web3-evm/increase-time! @web3 [(inc commit-period-duration)])

        (<? (registry-entry/reveal-vote registry-entry
                                        {:vote-option :vote-option/exclude
                                         :salt salt}
                                        {:from challenger}))

        (web3-evm/increase-time! @web3 [(inc reveal-period-duration)])

        (testing "District is whitelisted after being challenged"
          (is (= (reg-entry-status->kw (bn/number (<? (challenge/status challenge))))
                 :reg-entry.status/whitelisted))

          (is (= (vote-option->kw (bn/number (<? (challenge/winning-vote-option challenge))))
                 :vote-option/include))

          (is (= (bn/number (<? (challenge/vote-option-include-amount challenge)))
                 (bn/number (<? (challenge/winning-vote-option-amount challenge)))
                 (eth->wei-number 2))))

        (done)))))


(deftest set-meta-hash
  (async done
    (go
      (let [[creator challenger] (web3-eth/accounts @web3)
            [deposit commit-period-duration reveal-period-duration] (->> (<? (eternal-db/get-uint-values :district-registry-db [:deposit :commit-period-duration :reveal-period-duration]))
                                                                         (map bn/number))
            aragon-id (cljs-utils/rand-str 10)
            event-args (<! (create-district creator deposit meta-hash1 333333 aragon-id))
            registry-entry (:registry-entry event-args)]


        (is (= (web3/to-hex meta-hash1) (<? (district/meta-hash registry-entry))))

        (testing "District can have meta hash updated under valid conditions"
          (let [tx (<? (district/set-meta-hash registry-entry
                                               {:meta-hash meta-hash2}
                                               {:from creator}))]
            (let [meta-hash-event-args (:args (registry/district-meta-hash-changed-event-in-tx :district-registry-fwd tx))]

              (is (= (web3/to-hex meta-hash2)
                     (<? (district/meta-hash registry-entry))
                     (:meta-hash meta-hash-event-args))))))


        (testing "District cannot have meta hash updated when it's challenged"

          (<? (registry-entry/approve-and-create-challenge registry-entry
                                                           {:challenger challenger
                                                            :meta-hash meta-hash1
                                                            :amount deposit}
                                                           {:from challenger}))

          (is (nil? (try (<? (district/set-meta-hash registry-entry
                                                     {:meta-hash meta-hash2}
                                                     {:from creator}))
                         (catch :default)))))


        (testing "District cannot have meta hash updated when it's blacklisted"

          (web3-evm/increase-time! @web3 [(inc commit-period-duration)])
          (web3-evm/increase-time! @web3 [(inc reveal-period-duration)])

          (is (nil? (try (<? (district/set-meta-hash registry-entry
                                                     {:meta-hash meta-hash2}
                                                     {:from creator}))
                         (catch :default)))))

        (done)))))
