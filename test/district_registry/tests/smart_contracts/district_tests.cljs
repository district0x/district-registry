(ns district-registry.tests.smart-contracts.district-tests
  (:require [bignumber.core :as bn]
            [cljs-web3-next.eth :as web3-eth]
            [cljs-web3-next.evm :as web3-evm]
            [cljs.core.async :refer [go <!]]
            [cljs-web3-next.utils :as web3-utils]
            [cljs.test :refer-macros [async deftest is testing]]
            [district-registry.server.contract.challenge :as challenge]
            [district-registry.server.contract.district :as district]
            [district-registry.server.contract.eternal-db :as eternal-db]
            [district-registry.server.contract.registry :as registry]
            [district-registry.server.contract.registry-entry :as registry-entry]
            [district-registry.server.contract.stake-bank :as stake-bank]
            [district-registry.shared.utils :refer [reg-entry-status->kw vote-option->kw]]
            [district-registry.tests.smart-contracts.utils :refer [create-district tx-error?]]
            [district.cljs-utils :as cljs-utils]
            [district.server.web3 :refer [web3]]))

(def meta-hash1 "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH")
(def meta-hash2 "QmdsBbZgkwTJgaxfGgegGxLJW72RjU1bpWzvMe1vXxXEGf")

(deftest approve-and-stake-for
  (async done
         (go
           (let [[creator staker1 staker2] (<! (web3-eth/accounts @web3))
                 [deposit] (->> (<! (eternal-db/get-uint-values :district-registry-db [:deposit]))
                                (map bn/number))
                 aragon-id (cljs-utils/rand-str 10)
                 {:keys [registry-entry] :as event-args} (<! (create-district creator deposit meta-hash1 aragon-id))
                 stake-bank (<! (district/stake-bank registry-entry))]

             (testing "District can be staked to under valid conditions"
               (let [tx (<! (district/approve-and-stake-for registry-entry
                                                            {:amount (web3-utils/to-wei @web3 1 :ether)
                                                             :user staker1}
                                                            {:from staker1}))]

                 (let [{:keys [:staker-dnt-staked :is-unstake :voting-token-total-supply :stake-id :dnt-total-staked :staked-amount :staker :staker-voting-token-balance] :as challenge-event-args}
                       (<! (registry/district-stake-changed-event-in-tx [:district-registry :district-registry-fwd] tx))]

                   (is (= (bn/number (web3-utils/to-wei @web3 1 :ether))
                          (bn/number staker-dnt-staked)
                          (bn/number dnt-total-staked)
                          (bn/number staked-amount)
                          (bn/number (<! (stake-bank/total-staked stake-bank)))
                          (bn/number (<! (stake-bank/total-staked-for stake-bank staker1)))))

                   (is (false? is-unstake))

                   (is (= (bn/number voting-token-total-supply)
                          (bn/number staker-voting-token-balance)
                          1000000000000000000))

                   (is (= 0 (bn/number stake-id)))

                   (is (= staker staker1)))))

             (testing "District can be staked to, multiple times"
               (let [tx (<! (district/approve-and-stake-for registry-entry
                                                            {:amount (web3-utils/to-wei @web3 2 :ether)
                                                             :user staker2}
                                                            {:from staker2}))]
                 (let [{:keys [:staker-dnt-staked :is-unstake :voting-token-total-supply
                               :stake-id :dnt-total-staked :staked-amount :staker
                               :staker-voting-token-balance] :as challenge-event-args}
                       (<! (registry/district-stake-changed-event-in-tx [:district-registry :district-registry-fwd] tx))]

                   (is (= (bn/number staker-dnt-staked)
                          (bn/number (web3-utils/to-wei @web3 2 :ether))
                          (bn/number staked-amount)
                          (bn/number (<! (stake-bank/total-staked-for stake-bank staker2)))))

                   (is (= (bn/number (<! (stake-bank/total-staked stake-bank)))
                          (bn/number (web3-utils/to-wei @web3 3 :ether))
                          (bn/number dnt-total-staked)))

                   (is (false? is-unstake))
                   (is (= (bn/number voting-token-total-supply) 3000000000000000000))
                   (is (= (bn/number staker-voting-token-balance) 2000000000000000000))
                   (is (= 1 (bn/number stake-id)))
                   (is (= staker staker2)))))

             (testing "District can be unstaked from"
               (let [tx (<! (district/unstake registry-entry
                                              {:amount (web3-utils/to-wei @web3 0.75 :ether)}
                                              {:from staker1}))
                     {:keys [:staker-dnt-staked :is-unstake :voting-token-total-supply
                             :stake-id :dnt-total-staked :staked-amount :staker :staker-voting-token-balance]
                      :as challenge-event-args}
                     (<! (registry/district-stake-changed-event-in-tx [:district-registry :district-registry-fwd] tx))]

                 (is (= (bn/number (web3-utils/to-wei @web3 0.75 :ether))
                        (bn/number staked-amount)))

                 (is (= (bn/number staker-dnt-staked)
                        (bn/number (web3-utils/to-wei @web3 0.25 :ether))
                        (bn/number (<! (stake-bank/total-staked-for stake-bank staker1)))))

                 (is (= (bn/number (<! (stake-bank/total-staked stake-bank)))
                        (bn/number (web3-utils/to-wei @web3 2.25 :ether))
                        (bn/number dnt-total-staked)))

                 (is (true? is-unstake))
                 (is (= (bn/number voting-token-total-supply) 2250000000000000000))
                 (is (= (bn/number staker-voting-token-balance) 250000000000000000))
                 (is (= 2 (bn/number stake-id)))
                 (is (= staker staker1))))

             (done)))))

(deftest staking-counts-as-vote-include
  (async done
         (go
           (let [[creator challenger staker1] (<! (web3-eth/accounts @web3))
                 [deposit commit-period-duration reveal-period-duration] (->> (<! (eternal-db/get-uint-values :district-registry-db [:deposit :commit-period-duration :reveal-period-duration]))
                                                                              (map bn/number))
                 aragon-id (cljs-utils/rand-str 10)
                 {:keys [registry-entry] :as event-args} (<! (create-district creator deposit meta-hash1 aragon-id))

                 salt (cljs-utils/rand-str 10)
                 _ (<! (district/approve-and-stake-for registry-entry
                                                       {:amount (web3-utils/to-wei @web3 2 :ether)
                                                        :user staker1}
                                                       {:from staker1}))
                 _ (<! (registry-entry/approve-and-create-challenge registry-entry
                                                                    {:challenger challenger
                                                                     :meta-hash meta-hash1
                                                                     :amount deposit}
                                                                    {:from challenger}))
                 challenge (<! (registry-entry/current-challenge registry-entry))]

             (<! (registry-entry/approve-and-commit-vote registry-entry
                                                         {:amount (web3-utils/to-wei @web3 1 :ether)
                                                          :vote-option :vote-option/exclude
                                                          :salt salt}
                                                         {:from challenger}))

             (<! (web3-evm/increase-time @web3 (inc commit-period-duration)))
             (<! (web3-evm/mine-block @web3))

             (<! (registry-entry/reveal-vote registry-entry
                                             {:vote-option :vote-option/exclude
                                              :salt salt}
                                             {:from challenger}))

             (<! (web3-evm/increase-time @web3 (inc reveal-period-duration)))
             (<! (web3-evm/mine-block @web3))

             (testing "District is whitelisted after being challenged"
               (is (= (reg-entry-status->kw (bn/number (<! (challenge/status challenge))))
                      :reg-entry.status/whitelisted))

               (is (= (vote-option->kw (bn/number (<! (challenge/winning-vote-option challenge))))
                      :vote-option/include))

               (is (= (bn/number (<! (challenge/vote-option-include-amount challenge)))
                      (bn/number (<! (challenge/winning-vote-option-amount challenge)))
                      (bn/number (web3-utils/to-wei @web3 2 :ether)))))

             (done)))))

(deftest staking-in-commit-period-counts-as-vote-include
  (async done
         (go
           (let [[creator challenger staker1] (<! (web3-eth/accounts @web3))
                 [deposit commit-period-duration reveal-period-duration] (->> (<! (eternal-db/get-uint-values :district-registry-db [:deposit :commit-period-duration :reveal-period-duration]))
                                                                              (map bn/number))
                 aragon-id (cljs-utils/rand-str 10)
                 salt (cljs-utils/rand-str 10)
                 {:keys [registry-entry] :as event-args} (<! (create-district creator deposit meta-hash1 aragon-id))

                 _ (<! (registry-entry/approve-and-create-challenge registry-entry
                                                                    {:challenger challenger
                                                                     :meta-hash meta-hash1
                                                                     :amount deposit}
                                                                    {:from challenger}))
                 challenge (<! (registry-entry/current-challenge registry-entry))]

             (<! (registry-entry/approve-and-commit-vote registry-entry
                                                         {:amount (web3-utils/to-wei @web3 1 :ether)
                                                          :vote-option :vote-option/exclude
                                                          :salt salt}
                                                         {:from challenger}))

             (<! (district/approve-and-stake-for registry-entry
                                                 {:amount (web3-utils/to-wei @web3 2 :ether)
                                                  :user staker1}
                                                 {:from staker1}))

             (<! (web3-evm/increase-time @web3 (inc commit-period-duration)))
             (<! (web3-evm/mine-block @web3))

             (<! (registry-entry/reveal-vote registry-entry
                                             {:vote-option :vote-option/exclude
                                              :salt salt}
                                             {:from challenger}))

             (<! (web3-evm/increase-time @web3 (inc reveal-period-duration)))
             (<! (web3-evm/mine-block @web3))

             (testing "District is whitelisted after being challenged"
               (is (= (reg-entry-status->kw (bn/number (<! (challenge/status challenge))))
                      :reg-entry.status/whitelisted))

               (is (= (vote-option->kw (bn/number (<! (challenge/winning-vote-option challenge))))
                      :vote-option/include))

               (is (= (bn/number (<! (challenge/vote-option-include-amount challenge)))
                      (bn/number (<! (challenge/winning-vote-option-amount challenge)))
                      (bn/number (web3-utils/to-wei @web3 2 :ether)))))

             (done)))))

(deftest set-meta-hash
  (async done
         (go
           (let [[creator challenger] (<! (web3-eth/accounts @web3))
                 [deposit commit-period-duration reveal-period-duration] (->> (<! (eternal-db/get-uint-values :district-registry-db [:deposit :commit-period-duration :reveal-period-duration]))
                                                                              (map bn/number))
                 aragon-id (cljs-utils/rand-str 10)
                 {:keys [registry-entry] :as event-args} (<! (create-district creator deposit meta-hash1 aragon-id))]

             (is (= (web3-utils/to-hex @web3 meta-hash1) (<! (district/meta-hash registry-entry))))

             (testing "District can have meta hash updated under valid conditions"
               (let [tx (<! (district/set-meta-hash registry-entry
                                                      {:meta-hash meta-hash2}
                                                      {:from creator}))
                     {:keys [meta-hash] :as meta-hash-event-args}
                     (<! (registry/district-meta-hash-changed-event-in-tx [:district-registry :district-registry-fwd] tx))]

                 (is (= (web3-utils/to-hex @web3 meta-hash2)
                        (<! (district/meta-hash registry-entry))
                        meta-hash))))

             (testing "District cannot have meta hash updated when it's challenged"
               (<! (registry-entry/approve-and-create-challenge registry-entry
                                                                {:challenger challenger
                                                                 :meta-hash meta-hash1
                                                                 :amount deposit}
                                                                {:from challenger}))

               (is (tx-error? (<! (district/set-meta-hash registry-entry
                                                          {:meta-hash meta-hash2}
                                                          {:from creator})))))

             (testing "District cannot have meta hash updated when it's blacklisted"
               (<! (web3-evm/increase-time @web3 (inc (+ reveal-period-duration commit-period-duration))))
               (<! (web3-evm/mine-block @web3))
               (is (tx-error? (<! (district/set-meta-hash registry-entry
                                                          {:meta-hash meta-hash2}
                                                          {:from creator})))))

             (done)))))
