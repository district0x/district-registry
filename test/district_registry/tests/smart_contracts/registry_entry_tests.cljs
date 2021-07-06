(ns district-registry.tests.smart-contracts.registry-entry-tests
  (:require [bignumber.core :as bn]
            [cljs-web3-next.eth :as web3-eth]
            [cljs-web3-next.evm :as web3-evm]
            [cljs-web3-next.utils :as web3-utils]
            [cljs.test :refer-macros [async deftest is testing]]
            [cljs.core.async :refer [go <!]]
            [district-registry.server.contract.challenge :as challenge]
            [district-registry.server.contract.dnt :as dnt]
            [district-registry.server.contract.eternal-db :as eternal-db]
            [district-registry.server.contract.registry :as registry]
            [district-registry.server.contract.registry-entry :as registry-entry]
            [district-registry.shared.utils :refer [reg-entry-status->kw vote-option->kw]]
            [district-registry.tests.smart-contracts.utils :refer [create-district tx-reverted? next-ens-name]]
            [district.cljs-utils :as cljs-utils]
            [district.server.web3 :refer [web3]]))

(def meta-hash1 "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH")

(deftest approve-and-create-district-test
  (async done
    (go
      (let [[creator] (<! (web3-eth/accounts @web3))
            [deposit] (->> (<! (eternal-db/get-uint-values :district-registry-db [:deposit]))
                        (map bn/number))
            ens-name (next-ens-name)
            {:keys [:registry-entry :version
                    :timestamp :meta-hash] :as event-args} (<! (create-district creator deposit meta-hash1 ens-name))]
        (testing "District can be created under valid conditions"
          (is (web3-utils/address? @web3 registry-entry))
          (is (= meta-hash (web3-utils/to-hex @web3 meta-hash1)))
          (is (= (bn/number (:deposit event-args)) deposit))
          (is (= (bn/number version) 1))
          (is (pos? (bn/number timestamp)))
          (is (= ens-name (:ens-name event-args)))
          (is (true? (<! (registry-entry/is-challengeable? registry-entry)))))
        ;(testing "Cannot create district with same ens-name"     ;; currently there is no limitation of creating multiple district with same ens-name
        ;  (is (tx-reverted? (<! (create-district creator deposit meta-hash1 ens-name)))))
        (done)))))

(deftest create-challenge-and-vote-include
  (async done
    (go
      (let [[creator challenger voter] (<! (web3-eth/accounts @web3))
            [deposit commit-period-duration reveal-period-duration] (->> (<! (eternal-db/get-uint-values :district-registry-db [:deposit :commit-period-duration :reveal-period-duration]))
                                                                      (map bn/number))
            ens-name (next-ens-name)
            salt (cljs-utils/rand-str 10)
            {:keys [registry-entry] :as event-args} (<! (create-district creator deposit meta-hash1 ens-name))]

        (testing "District can be challenged under valid conditions"
          (let [tx-receipt (<! (registry-entry/approve-and-create-challenge registry-entry
                                                                    {:challenger challenger
                                                                     :meta-hash meta-hash1
                                                                     :amount deposit}
                                                                    {:from challenger}))]

            (let [{:keys [:meta-hash :reveal-period-end :index :reward-pool :commit-period-end] :as challenge-event-args} (<! (registry/challenge-created-event-in-tx [:district-registry :district-registry-fwd] tx-receipt))]
              (is (= challenger (:challenger challenge-event-args)))
              (is (= meta-hash (web3-utils/to-hex @web3 meta-hash1)))
              (is (= 0 (bn/number index)))
              (is (bn/> reward-pool 0))
              (is (= registry-entry (:registry-entry challenge-event-args)))
              (is (bn/> reveal-period-end 0))
              (is (bn/> commit-period-end 0))
              (is (false? (<! (registry-entry/is-challengeable? registry-entry))))
              (is (true? (<! (registry-entry/is-challenge-period-active? registry-entry)))))))

        (testing "Challenge contract is correctly constructed"
          (let [challenge-addr (<! (registry-entry/current-challenge registry-entry))]
            (is (web3-utils/address? @web3 challenge-addr))
            (is (= :reg-entry.status/commit-period (reg-entry-status->kw (bn/number (<! (challenge/status challenge-addr))))))
            (is (true? (<! (challenge/is-vote-commit-period-active? challenge-addr))))))

        (testing "Can vote in challenge"
          (let [tx (<! (registry-entry/approve-and-commit-vote registry-entry
                                                               {:amount (web3-utils/to-wei @web3 1 :ether)
                                                                :vote-option :vote-option/include
                                                                :salt salt}
                                                               {:from voter}))
                vote-event-args (<! (registry/vote-committed-event-in-tx [:district-registry :district-registry-fwd] tx))]
            (is (= voter (:voter vote-event-args)))
            (is (= (bn/number (web3-utils/to-wei @web3 1 :ether))
                   (bn/number (:amount vote-event-args))))))

        (<! (web3-evm/increase-time @web3 (inc commit-period-duration)))

        (testing "Cannot reveal invalid vote option"
          (is (tx-reverted? (<! (registry-entry/reveal-vote registry-entry
                                                             {:vote-option :vote-option/exclude
                                                              :salt salt}
                                                             {:from voter})))))

        (testing "Can reveal vote"
          (let [tx (<! (registry-entry/reveal-vote registry-entry
                                                   {:vote-option :vote-option/include
                                                    :salt salt}
                                                   {:from voter}))
                reveal-event-args (<! (registry/vote-revealed-event-in-tx [:district-registry :district-registry-fwd] tx))]
            (is (= (bn/number (web3-utils/to-wei @web3 1 :ether))
                   (bn/number (:amount reveal-event-args))))
            (is (= voter (:voter reveal-event-args)))
            (is (= :vote-option/include (vote-option->kw (bn/number (:option reveal-event-args)))))))

        (testing "Can claim vote reward"

          (<! (web3-evm/increase-time @web3 (inc reveal-period-duration)))

          (let [tx (<! (registry-entry/claim-reward registry-entry {:from voter}))
                vote-reward-event-args (<! (registry/vote-reward-claimed-event-in-tx [:district-registry :district-registry-fwd] tx))]
            (is (= voter (:voter vote-reward-event-args)))
            (is (= (bn/number (:amount vote-reward-event-args))) (/ deposit 2))))

        (testing "Can claim creator reward"
          (let [tx (<! (registry-entry/claim-reward registry-entry {:from creator}))
                creator-reward-event-args (<! (registry/creator-reward-claimed-event-in-tx [:district-registry :district-registry-fwd] tx))]
            (is (= creator (:creator creator-reward-event-args)))
            (is (= (bn/number (:amount creator-reward-event-args)) (/ deposit 2)))))

        (done)))))

(deftest create-challenge-and-vote-exclude
  (async done
    (go
      (let [[creator challenger voter] (<! (web3-eth/accounts @web3))
            [deposit commit-period-duration reveal-period-duration] (->> (<! (eternal-db/get-uint-values :district-registry-db [:deposit :commit-period-duration :reveal-period-duration]))
                                                                      (map bn/number))
            ens-name (next-ens-name)
            salt (cljs-utils/rand-str 10)
            {:keys [registry-entry] :as event-args} (<! (create-district creator deposit meta-hash1 ens-name))
            _ (<! (registry-entry/approve-and-create-challenge registry-entry
                                                               {:challenger challenger
                                                                :meta-hash meta-hash1
                                                                :amount deposit}
                                                               {:from challenger}))
            challenge (<! (registry-entry/current-challenge registry-entry))]

        (testing "Is challenge status correct during commit period"
          (is (= :reg-entry.status/commit-period (reg-entry-status->kw (bn/number (<! (challenge/status challenge))))))
          (is (true? (<! (challenge/is-vote-commit-period-active? challenge)))))

        (<! (registry-entry/approve-and-commit-vote registry-entry
                                                    {:amount (web3-utils/to-wei @web3 1 :ether)
                                                     :vote-option :vote-option/exclude
                                                     :salt salt}
                                                    {:from voter}))

        (<! (web3-evm/increase-time @web3 (inc commit-period-duration)))
        (<! (web3-evm/mine-block @web3))

        (testing "Is challenge status correct during reveal period"
          (is (= :reg-entry.status/reveal-period (-> (<! (challenge/status challenge)) bn/number reg-entry-status->kw)))
          (is (true? (<! (challenge/is-vote-reveal-period-active? challenge))))
          (is (true? (<! (challenge/has-voted? challenge voter))))
          (is (false? (<! (challenge/has-voted? challenge challenger)))))

        (<! (registry-entry/reveal-vote registry-entry
                                        {:vote-option :vote-option/exclude
                                         :salt salt}
                                        {:from voter}))

        (<! (web3-evm/increase-time @web3 (inc reveal-period-duration)))
        (<! (web3-evm/mine-block @web3))

        (testing "Is challenge status correct after reveal period"
          (is (= :reg-entry.status/blacklisted (reg-entry-status->kw (bn/number (<! (challenge/status challenge))))))
          (is (true? (<! (challenge/is-vote-reveal-period-over? challenge))))
          (is (= :vote-option/exclude (vote-option->kw (bn/number (<! (challenge/winning-vote-option challenge))))))
          (is (= (bn/number (<! (challenge/vote-option-exclude-amount challenge)))
                 (bn/number (<! (challenge/winning-vote-option-amount challenge)))
                 (bn/number (web3-utils/to-wei @web3 1 :ether)))))

        (testing "Can claim vote reward"
          (let [previous-balance (<! (dnt/balance-of voter))
                tx (<! (registry-entry/claim-reward registry-entry {:from voter}))
                vote-reward-event-args (<! (registry/vote-reward-claimed-event-in-tx [:district-registry :district-registry-fwd] tx))]
            (is (bn/< previous-balance (<! (dnt/balance-of voter))))
            (is (= voter (:voter vote-reward-event-args)))
            (is (= (bn/number (:amount vote-reward-event-args)) (/ deposit 2)))
            (is (true? (<! (challenge/is-vote-reward-claimed? challenge voter))))))

        (testing "Can claim challenger reward"
          (let [previous-balance (<! (dnt/balance-of challenger))
                tx (<! (registry-entry/claim-reward registry-entry {:from challenger}))
                challenger-reward-event-args (<! (registry/challenger-reward-claimed-event-in-tx :district-registry-fwd tx))]
            (is (bn/< previous-balance (<! (dnt/balance-of challenger))))
            (is (= challenger (:challenger challenger-reward-event-args)))
            (is (= (bn/number (:amount challenger-reward-event-args)) (+ (/ deposit 2) deposit)))
            (is (true? (<! (challenge/is-challenger-reward-claimed? challenge))))))

        (done)))))

(deftest create-challenge-and-reclaim-votes
  (async done
    (go
      (let [[creator challenger voter] (<! (web3-eth/accounts @web3))
            [deposit commit-period-duration reveal-period-duration] (->> (<! (eternal-db/get-uint-values :district-registry-db [:deposit :commit-period-duration :reveal-period-duration]))
                                                                      (map bn/number))
            ens-name (next-ens-name)
            salt (cljs-utils/rand-str 10)
            {:keys [registry-entry] :as event-args} (<! (create-district creator deposit meta-hash1 ens-name))
            _ (<! (registry-entry/approve-and-create-challenge registry-entry
                                                               {:challenger challenger
                                                                :meta-hash meta-hash1
                                                                :amount deposit}
                                                               {:from challenger}))
            challenge (<! (registry-entry/current-challenge registry-entry))]
        (<! (registry-entry/approve-and-commit-vote registry-entry
                                                    {:amount (web3-utils/to-wei @web3 1 :ether)
                                                     :vote-option :vote-option/include
                                                     :salt salt}
                                                    {:from voter}))

        (<! (web3-evm/increase-time @web3 (inc (+ reveal-period-duration commit-period-duration))))
        (<! (web3-evm/mine-block @web3))

        (testing "Is challenge status correct after reveal period"
          (is (= :reg-entry.status/blacklisted (reg-entry-status->kw (bn/number (<! (challenge/status challenge))))))
          (is (true? (<! (challenge/is-vote-reveal-period-over? challenge))))
          (is (= :vote-option/exclude (vote-option->kw (bn/number (<! (challenge/winning-vote-option challenge))))))
          (is (false? (<! (challenge/are-votes-reclaimed? challenge voter)))))

        (testing "Can reclaim votes"
          (let [tx (<! (registry-entry/claim-reward registry-entry {:from voter}))
                votes-reclaimed-event-args (<! (registry/votes-reclaimed-event-in-tx [:district-registry :district-registry-fwd] tx))]
            (is (= voter (:voter votes-reclaimed-event-args)))
            (is (= (bn/number (:amount votes-reclaimed-event-args))
                   (bn/number (web3-utils/to-wei @web3 1 :ether))))
            (is (true? (<! (challenge/are-votes-reclaimed? challenge voter))))))

        (done)))))

(deftest create-multiple-challenges-multiple-votes
  (async done
    (go
      (let [[creator challenger1 challenger2 voter1 voter2 voter3] (<! (web3-eth/accounts @web3))
            [deposit commit-period-duration reveal-period-duration] (->> (<! (eternal-db/get-uint-values :district-registry-db [:deposit :commit-period-duration :reveal-period-duration]))
                                                                      (map bn/number))
            ens-name (next-ens-name)
            salt (cljs-utils/rand-str 10)
            {:keys [registry-entry] :as event-args} (<! (create-district creator deposit meta-hash1 ens-name))]

        (testing "First challenge"
          (let [_ (<! (registry-entry/approve-and-create-challenge registry-entry
                                                                   {:challenger challenger1
                                                                    :meta-hash meta-hash1
                                                                    :amount deposit}
                                                                   {:from challenger1}))
                challenge1 (<! (registry-entry/current-challenge registry-entry))]

            (<! (registry-entry/approve-and-commit-vote registry-entry
                                                        {:amount (web3-utils/to-wei @web3 1.7 :ether)
                                                         :vote-option :vote-option/include
                                                         :salt salt}
                                                        {:from voter1}))

            (<! (registry-entry/approve-and-commit-vote registry-entry
                                                        {:amount (web3-utils/to-wei @web3 0.3 :ether)
                                                         :vote-option :vote-option/include
                                                         :salt salt}
                                                        {:from voter2}))

            (<! (registry-entry/approve-and-commit-vote registry-entry
                                                        {:amount (web3-utils/to-wei @web3 1.9 :ether)
                                                         :vote-option :vote-option/exclude
                                                         :salt salt}
                                                        {:from voter3}))

            (<! (web3-evm/increase-time @web3 (inc commit-period-duration)))
            (<! (web3-evm/mine-block @web3))

            (<! (registry-entry/reveal-vote registry-entry
                                            {:vote-option :vote-option/include
                                             :salt salt}
                                            {:from voter1}))

            (<! (registry-entry/reveal-vote registry-entry
                                            {:vote-option :vote-option/include
                                             :salt salt}
                                            {:from voter2}))

            (<! (registry-entry/reveal-vote registry-entry
                                            {:vote-option :vote-option/exclude
                                             :salt salt}
                                            {:from voter3}))

            (<! (web3-evm/increase-time @web3 (inc reveal-period-duration)))
            (<! (web3-evm/mine-block @web3))

            (testing "Is challenge status correct after reveal period"
              (is (= :reg-entry.status/whitelisted (reg-entry-status->kw (bn/number (<! (challenge/status challenge1))))))
              (is (= :vote-option/include (vote-option->kw (bn/number (<! (challenge/winning-vote-option challenge1))))))
              (is (= (bn/number (<! (challenge/vote-option-include-amount challenge1)))
                     (bn/number (<! (challenge/winning-vote-option-amount challenge1)))
                     (bn/number (web3-utils/to-wei @web3 2 :ether)))))))

        (testing "Second challenge"
          (let [_ (<! (registry-entry/approve-and-create-challenge registry-entry
                                                                   {:challenger challenger2
                                                                    :meta-hash meta-hash1
                                                                    :amount deposit}
                                                                   {:from challenger2}))
                challenge2 (<! (registry-entry/current-challenge registry-entry))]

            (<! (registry-entry/approve-and-commit-vote registry-entry
                                                        {:amount (web3-utils/to-wei @web3 1.7 :ether)
                                                         :vote-option :vote-option/include
                                                         :salt salt}
                                                        {:from voter1}))

            (<! (registry-entry/approve-and-commit-vote registry-entry
                                                        {:amount (web3-utils/to-wei @web3 0.3 :ether)
                                                         :vote-option :vote-option/exclude
                                                         :salt salt}
                                                        {:from voter2}))

            (<! (registry-entry/approve-and-commit-vote registry-entry
                                                        {:amount (web3-utils/to-wei @web3 1.9 :ether)
                                                         :vote-option :vote-option/exclude
                                                         :salt salt}
                                                        {:from voter3}))

            (<! (web3-evm/increase-time @web3 (inc commit-period-duration)))
            (<! (web3-evm/mine-block @web3))

            (<! (registry-entry/reveal-vote registry-entry
                                            {:vote-option :vote-option/include
                                             :salt salt}
                                            {:from voter1}))

            (<! (registry-entry/reveal-vote registry-entry
                                            {:vote-option :vote-option/exclude
                                             :salt salt}
                                            {:from voter2}))

            (<! (registry-entry/reveal-vote registry-entry
                                            {:vote-option :vote-option/exclude
                                             :salt salt}
                                            {:from voter3}))

            (<! (web3-evm/increase-time @web3 (inc reveal-period-duration)))
            (<! (web3-evm/mine-block @web3))

            (testing "Is challenge status correct after reveal period"
              (is (= :reg-entry.status/blacklisted (reg-entry-status->kw (bn/number (<! (challenge/status challenge2))))))
              (is (= :vote-option/exclude (vote-option->kw (bn/number (<! (challenge/winning-vote-option challenge2))))))
              (is (= (bn/number (<! (challenge/vote-option-exclude-amount challenge2)))
                     (bn/number (<! (challenge/winning-vote-option-amount challenge2)))
                     (bn/number (web3-utils/to-wei @web3 2.2 :ether)))))))
        (done)))))
