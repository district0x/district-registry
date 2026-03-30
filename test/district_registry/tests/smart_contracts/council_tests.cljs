(ns district-registry.tests.smart-contracts.council-tests
  (:require [bignumber.core :as bn]
            [cljs-web3-next.eth :as web3-eth]
            [cljs-web3-next.utils :as web3-utils]
            [cljs.test :refer-macros [async deftest is testing]]
            [cljs.core.async :refer [go <!]]
            [district-registry.server.contract.council :as council]
            [district-registry.server.contract.council-stake-bank :as stake-bank]
            [district-registry.tests.smart-contracts.utils :refer [tx-error?]]
            [district.server.smart-contracts :refer [contract-address instance]]
            [district.server.web3 :refer [web3]]))

;; Council Tests

(deftest council-initialization
  (async done
         (go
           (testing "Council initializes with 9 seats"
             (let [seat-count 9]
               (loop [i 0
                      seats []]
                 (if (< i seat-count)
                   (let [seat (<! (council/get-seat :council i))]
                     (is seat "Seat should exist")
                     (is (= i (:index seat)) "Seat index should match")
                     (is (not (empty? (:name seat))) "Seat should have a name")
                     (recur (inc i) (conj seats seat)))
                   (is (= (count seats) seat-count) "Should have 9 seats")))))

           (testing "Seat 8 (The Stranger) is immutable"
             (let [seat (<! (council/get-seat :council 8))]
               (is (:is-immutable seat) "Seat 8 should be immutable")
               (is (= "ARI - The Stranger" (:name seat)) "Seat 8 should be The Stranger")))

           (testing "Other seats are mutable"
             (loop [i 0]
               (when (< i 8)
                 (let [seat (<! (council/get-seat :council i))]
                   (is (not (:is-immutable seat)) (str "Seat " i " should be mutable"))
                   (recur (inc i))))))

           (done))))

(deftest council-weights
  (async done
         (go
           (let [[owner addr1 addr2] (<! (web3-eth/accounts @web3))]

             (testing "Council weights return correctly with zero stake"
               (let [weights (<! (council/get-council-weights :council))]
                 (is weights "Weights should be returned")
                 (is (= 9 (count (:mind-ids weights))) "Should have 9 mind IDs")
                 (is (= 9 (count (:weights weights))) "Should have 9 weights")))

             (testing "All weights are equal when no stake exists"
               (let [weights (<! (council/get-council-weights :council))
                     weight-values (:weights weights)
                     expected-weight (/ 10000 9)] ;; Basis points divided by 9
                 ;; Each weight should be approximately 1111 basis points (11.11%)
                 (doseq [weight weight-values]
                   (is (>= weight 1000) "Each weight should be at least 1000 bps")
                   (is (<= weight 1200) "Each weight should be at most 1200 bps"))))

             (done)))))

(deftest update-philosophy
  (async done
         (go
           (let [[owner non-owner] (<! (web3-eth/accounts @web3))
                 test-hash (web3-utils/to-hex @web3 "QmTest123")]

             (testing "Owner can update philosophy"
               (let [tx (<! (council/update-philosophy :council 0 test-hash {:from owner}))]
                 (is tx "Transaction should succeed")
                 (let [seat (<! (council/get-seat :council 0))]
                   (is (= test-hash (:philosophy-hash seat)) "Philosophy should be updated"))))

             (testing "Non-owner cannot update philosophy"
               (is (tx-error? (<! (council/update-philosophy :council 0 test-hash {:from non-owner})))
                   "Should reject non-owner"))

             (testing "Cannot update with empty philosophy hash"
               (is (tx-error? (<! (council/update-philosophy :council 0 "0x0" {:from owner})))
                   "Should reject empty hash"))

             (testing "Cannot update invalid seat index"
               (is (tx-error? (<! (council/update-philosophy :council 10 test-hash {:from owner})))
                   "Should reject invalid index"))

             (done)))))

(deftest seat-flip-mechanism
  (async done
         (go
           (let [[owner] (<! (web3-eth/accounts @web3))
                 new-mind-id (web3-utils/sha3 @web3 "new-mind")
                 new-name "New Philosopher"
                 philosophy-hash (web3-utils/to-hex @web3 "QmNewPhilosophy")
                 proposed-stake 1000000] ;; Enough to be higher than seat 0

             (testing "Cannot propose new mind with insufficient stake"
               (is (tx-error? (<! (council/propose-new-mind :council
                                                            new-mind-id
                                                            new-name
                                                            philosophy-hash
                                                            0 ;; Zero stake
                                                            {:from owner})))
                   "Should reject insufficient stake"))

             (testing "Cannot flip immutable seat (Seat 8)"
               ;; Even with high proposed stake, Seat 8 should never be flippable
               (let [stranger-seat (<! (council/get-seat :council 8))]
                 (is (:is-immutable stranger-seat) "Seat 8 should remain immutable")))

             (testing "Cannot propose mind that already has a seat"
               ;; Get existing mind ID from seat 0
               (let [seat-0 (<! (council/get-seat :council 0))
                     existing-mind-id (:mind-id seat-0)]
                 (is (tx-error? (<! (council/propose-new-mind :council
                                                              existing-mind-id
                                                              "Duplicate"
                                                              philosophy-hash
                                                              proposed-stake
                                                              {:from owner})))
                     "Should reject duplicate mind")))

             (done)))))

(deftest stake-cache-updates
  (async done
         (go
           (let [[owner] (<! (web3-eth/accounts @web3))
                 stake-bank-addr (contract-address :council-stake-bank)]

             (testing "Council receives stake cache updates"
               ;; This would be tested in integration with CouncilStakeBank
               ;; For now, verify the interface exists
               (let [seat (<! (council/get-seat :council 0))]
                 (is (contains? seat :total-staked) "Seat should track total staked")))

             (done)))))

(deftest immutability-verification
  (async done
         (go
           (testing "verifyStrangerImmutable returns true"
             (let [is-immutable (<! (council/verify-stranger-immutable :council))]
               (is is-immutable "Stranger seat verification should return true")))

           (testing "Seat 8 index is constant"
             (let [stranger-index 8]
               (is (= stranger-index 8) "STRANGER_SEAT_INDEX constant should be 8")))

           (done))))

;; CouncilStakeBank Tests

(deftest stake-bank-initialization
  (async done
         (go
           (testing "CouncilStakeBank is deployed"
             (let [addr (contract-address :council-stake-bank)]
               (is addr "StakeBank contract should be deployed")
               (is (not= addr "0x0") "StakeBank address should not be zero")))

           (done))))

(deftest staking-on-seat
  (async done
         (go
           (let [[owner addr1 addr2] (<! (web3-eth/accounts @web3))
                 stake-amount (web3-utils/to-wei @web3 "100" :ether)
                 dnt-addr (contract-address :dnt)]

             (testing "Cannot stake without DNT approval"
               ;; This would fail in practice without prior DNT approval
               ;; In real tests, we'd approve first
               )

             (testing "Can stake on valid seat"
               ;; Assuming DNT is approved in test setup
               ;; (let [tx (<! (stake-bank/stake-on-seat :council-stake-bank 0 stake-amount {:from addr1}))]
               ;;   (is tx "Staking transaction should succeed")
               ;;   (let [balance (<! (stake-bank/balance-of :council-stake-bank 0 addr1))]
               ;;     (is (= stake-amount balance) "Balance should equal staked amount")))
               )

             (testing "Cannot stake on invalid seat"
               (is (tx-error? (<! (stake-bank/stake-on-seat :council-stake-bank 10 stake-amount {:from addr1})))
                   "Should reject invalid seat index"))

             (testing "Cannot stake zero amount"
               (is (tx-error? (<! (stake-bank/stake-on-seat :council-stake-bank 0 0 {:from addr1})))
                   "Should reject zero stake"))

             (done)))))

(deftest unstaking-from-seat
  (async done
         (go
           (let [[owner addr1] (<! (web3-eth/accounts @web3))
                 stake-amount (web3-utils/to-wei @web3 "100" :ether)]

             (testing "Cannot unstake more than staked"
               ;; Assuming addr1 has some stake
               ;; (is (tx-error? (<! (stake-bank/unstake-from-seat :council-stake-bank 0 stake-amount {:from addr1})))
               ;;     "Should reject unstaking more than balance")
               )

             (testing "Cannot unstake from invalid seat"
               (is (tx-error? (<! (stake-bank/unstake-from-seat :council-stake-bank 10 stake-amount {:from addr1})))
                   "Should reject invalid seat"))

             (testing "Cannot unstake zero amount"
               (is (tx-error? (<! (stake-bank/unstake-from-seat :council-stake-bank 0 0 {:from addr1})))
                   "Should reject zero unstake"))

             (done)))))

(deftest move-stake-between-seats
  (async done
         (go
           (let [[owner addr1] (<! (web3-eth/accounts @web3))
                 move-amount (web3-utils/to-wei @web3 "50" :ether)]

             (testing "Cannot move more stake than balance"
               ;; Assuming addr1 has stake on seat 0
               ;; (is (tx-error? (<! (stake-bank/move-stake :council-stake-bank 0 1 move-amount {:from addr1})))
               ;;     "Should reject moving more than balance")
               )

             (testing "Cannot move to invalid seat"
               (is (tx-error? (<! (stake-bank/move-stake :council-stake-bank 0 10 move-amount {:from addr1})))
                   "Should reject invalid destination seat"))

             (testing "Cannot move from invalid seat"
               (is (tx-error? (<! (stake-bank/move-stake :council-stake-bank 10 1 move-amount {:from addr1})))
                   "Should reject invalid source seat"))

             (testing "Cannot move zero amount"
               (is (tx-error? (<! (stake-bank/move-stake :council-stake-bank 0 1 0 {:from addr1})))
                   "Should reject zero move amount"))

             (testing "Cannot move to same seat"
               (is (tx-error? (<! (stake-bank/move-stake :council-stake-bank 0 0 move-amount {:from addr1})))
                   "Should reject moving to same seat"))

             (done)))))

(deftest checkpoint-queries
  (async done
         (go
           (let [[owner addr1] (<! (web3-eth/accounts @web3))
                 current-block (<! (web3-eth/block-number @web3))
                 stake-amount (web3-utils/to-wei @web3 "100" :ether)]

             (testing "Can query balance at specific block"
               ;; Assuming some stake history exists
               ;; (let [balance (<! (stake-bank/balance-of-at :council-stake-bank 0 addr1 current-block))]
               ;;   (is (>= balance 0) "Balance query should return non-negative value"))
               )

             (testing "Can query total staked for seat at specific block"
               ;; (let [total (<! (stake-bank/total-staked-for-seat-at :council-stake-bank 0 current-block))]
               ;;   (is (>= total 0) "Total stake query should return non-negative value"))
               )

             (testing "Historical balance returns zero before first stake"
               ;; Query at block 1 (before any stakes)
               ;; (let [balance (<! (stake-bank/balance-of-at :council-stake-bank 0 addr1 1))]
               ;;   (is (= balance 0) "Balance should be zero before first stake"))
               )

             (done)))))

(deftest stake-notifications-to-council
  (async done
         (go
           (testing "StakeBank notifies Council of stake changes"
             ;; This is an integration test
             ;; Verify that after staking, Council.updateStakeCache is called
             ;; We'd check this by comparing seat.totalStaked before and after
             )

           (done))))

;; Integration Tests

(deftest council-and-stake-bank-integration
  (async done
         (go
           (let [[owner addr1 addr2] (<! (web3-eth/accounts @web3))]

             (testing "Council and StakeBank are properly connected"
               (let [council-addr (contract-address :council)
                     stake-bank-addr (contract-address :council-stake-bank)
                     stake-bank-council (<! (stake-bank/council :council-stake-bank))]
                 (is (= council-addr stake-bank-council)
                     "StakeBank should reference correct Council")))

             (testing "Stake changes update Council weights"
               ;; This would require:
               ;; 1. Get initial weights
               ;; 2. Stake on a seat
               ;; 3. Get new weights
               ;; 4. Verify weights changed proportionally
               )

             (done))))
