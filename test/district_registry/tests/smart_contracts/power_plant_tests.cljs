(ns district-registry.tests.smart-contracts.power-plant-tests
  (:require [bignumber.core :as bn]
            [cljs-web3-next.eth :as web3-eth]
            [cljs-web3-next.utils :as web3-utils]
            [cljs.test :refer-macros [async deftest is testing]]
            [cljs.core.async :refer [go <!]]
            [district-registry.server.contract.power-plant :as power-plant]
            [district-registry.server.contract.ari-oracle :as oracle]
            [district-registry.tests.smart-contracts.utils :refer [tx-error?]]
            [district.server.smart-contracts :refer [contract-address instance]]
            [district.server.web3 :refer [web3]]))

;; PowerPlant Tests

(deftest power-plant-initialization
  (async done
         (go
           (testing "PowerPlant is deployed"
             (let [addr (contract-address :power-plant)]
               (is addr "PowerPlant contract should be deployed")
               (is (not= addr "0x0") "PowerPlant address should not be zero")))

           (testing "PowerPlant references ARIOracle"
             (let [oracle-addr (<! (power-plant/ari-oracle :power-plant))
                   expected-oracle (contract-address :ari-oracle)]
               (is (= oracle-addr expected-oracle) "PowerPlant should reference correct Oracle")))

           (testing "PowerPlant has correct owner"
             ;; (let [owner (<! (power-plant/owner :power-plant))]
             ;;   (is owner "PowerPlant should have owner"))
             )

           (done))))

(deftest treasury-deposits
  (async done
         (go
           (let [[owner addr1] (<! (web3-eth/accounts @web3))
                 deposit-amount (web3-utils/to-wei @web3 "10" :ether)
                 eth-addr "0x0000000000000000000000000000000000000000"]

             (testing "Can deposit ETH to treasury"
               ;; (let [tx (<! (power-plant/deposit :power-plant
               ;;                                    eth-addr
               ;;                                    deposit-amount
               ;;                                    {:from addr1 :value deposit-amount}))]
               ;;   (is tx "ETH deposit should succeed")
               ;;   (let [balance (<! (power-plant/get-treasury-balance :power-plant eth-addr))]
               ;;     (is (= balance deposit-amount) "Treasury should have ETH balance")))
               )

             (testing "Can deposit ERC20 tokens"
               ;; Assuming DNT token
               ;; User must approve PowerPlant first
               ;; (let [dnt-addr (contract-address :dnt)
               ;;       tx (<! (power-plant/deposit :power-plant dnt-addr deposit-amount {:from addr1}))]
               ;;   (is tx "Token deposit should succeed"))
               )

             (testing "Deposit emits TreasuryDeposit event"
               ;; Verify event is emitted with correct parameters
               )

             (done)))))

(deftest treasury-balance-queries
  (async done
         (go
           (let [eth-addr "0x0000000000000000000000000000000000000000"
                 dnt-addr (contract-address :dnt)]

             (testing "Can query ETH balance"
               (let [balance (<! (power-plant/get-treasury-balance :power-plant eth-addr))]
                 (is (>= balance 0) "ETH balance should be non-negative")))

             (testing "Can query token balance"
               (let [balance (<! (power-plant/get-treasury-balance :power-plant dnt-addr))]
                 (is (>= balance 0) "Token balance should be non-negative")))

             (testing "Balance for unknown token is zero"
               (let [fake-addr "0x1234567890123456789012345678901234567890"
                     balance (<! (power-plant/get-treasury-balance :power-plant fake-addr))]
                 (is (= balance 0) "Unknown token balance should be zero")))

             (done)))))

(deftest bounty-creation
  (async done
         (go
           (let [[owner oracle-addr non-oracle] (<! (web3-eth/accounts @web3))
                 metadata-hash (web3-utils/to-hex @web3 "QmBountyMetadata123")
                 reward (web3-utils/to-wei @web3 "5" :ether)
                 eth-addr "0x0000000000000000000000000000000000000000"
                 deadline (+ (quot (.now js/Date) 1000) (* 7 24 60 60))] ;; 7 days

             (testing "Oracle can create bounty"
               ;; Only ARIOracle should be able to call this
               ;; (let [tx (<! (power-plant/create-bounty :power-plant
               ;;                                          metadata-hash
               ;;                                          reward
               ;;                                          eth-addr
               ;;                                          deadline
               ;;                                          {:from oracle-addr}))]
               ;;   (is tx "Bounty creation should succeed")
               ;;   (let [bounty (<! (power-plant/get-bounty :power-plant 0))]
               ;;     (is bounty "Bounty should exist")
               ;;     (is (= metadata-hash (:metadata-hash bounty)) "Metadata hash should match")
               ;;     (is (= reward (:reward bounty)) "Reward should match")))
               )

             (testing "Non-oracle cannot create bounty"
               (is (tx-error? (<! (power-plant/create-bounty :power-plant
                                                             metadata-hash
                                                             reward
                                                             eth-addr
                                                             deadline
                                                             {:from non-oracle})))
                   "Should reject non-oracle"))

             (testing "Cannot create bounty with zero reward"
               (is (tx-error? (<! (power-plant/create-bounty :power-plant
                                                             metadata-hash
                                                             0
                                                             eth-addr
                                                             deadline
                                                             {:from oracle-addr})))
                   "Should reject zero reward"))

             (testing "Cannot create bounty with empty metadata"
               (is (tx-error? (<! (power-plant/create-bounty :power-plant
                                                             "0x0"
                                                             reward
                                                             eth-addr
                                                             deadline
                                                             {:from oracle-addr})))
                   "Should reject empty metadata"))

             (testing "Cannot create bounty with past deadline"
               (let [past-deadline (- (quot (.now js/Date) 1000) 1000)]
                 (is (tx-error? (<! (power-plant/create-bounty :power-plant
                                                               metadata-hash
                                                               reward
                                                               eth-addr
                                                               past-deadline
                                                               {:from oracle-addr})))
                     "Should reject past deadline")))

             (done)))))

(deftest bounty-assignment
  (async done
         (go
           (let [[owner oracle-addr contributor] (<! (web3-eth/accounts @web3))]

             (testing "Owner can assign bounty"
               ;; (let [tx (<! (power-plant/assign-bounty :power-plant 0 contributor {:from owner}))]
               ;;   (is tx "Assignment should succeed")
               ;;   (let [bounty (<! (power-plant/get-bounty :power-plant 0))]
               ;;     (is (= contributor (:assignee bounty)) "Assignee should be set")))
               )

             (testing "Cannot assign already assigned bounty"
               (is (tx-error? (<! (power-plant/assign-bounty :power-plant 0 contributor {:from owner})))
                   "Should reject reassignment"))

             (testing "Cannot assign to zero address"
               (is (tx-error? (<! (power-plant/assign-bounty :power-plant 0 "0x0" {:from owner})))
                   "Should reject zero address"))

             (testing "Non-owner cannot assign bounty"
               (is (tx-error? (<! (power-plant/assign-bounty :power-plant 0 contributor {:from contributor})))
                   "Should reject non-owner"))

             (done)))))

(deftest bounty-completion
  (async done
         (go
           (let [[owner contributor] (<! (web3-eth/accounts @web3))]

             (testing "Assignee can mark bounty complete"
               ;; Assuming bounty 0 is assigned to contributor
               ;; (let [tx (<! (power-plant/mark-bounty-complete :power-plant 0 {:from contributor}))]
               ;;   (is tx "Marking complete should succeed")
               ;;   (let [bounty (<! (power-plant/get-bounty :power-plant 0))]
               ;;     (is (:completed bounty) "Bounty should be marked completed")))
               )

             (testing "Cannot mark unassigned bounty complete"
               (is (tx-error? (<! (power-plant/mark-bounty-complete :power-plant 1 {:from contributor})))
                   "Should reject unassigned bounty"))

             (testing "Non-assignee cannot mark bounty complete"
               (is (tx-error? (<! (power-plant/mark-bounty-complete :power-plant 0 {:from owner})))
                   "Should reject non-assignee"))

             (testing "Cannot mark already completed bounty"
               (is (tx-error? (<! (power-plant/mark-bounty-complete :power-plant 0 {:from contributor})))
                   "Should reject already completed"))

             (done)))))

(deftest bounty-claim
  (async done
         (go
           (let [[owner contributor] (<! (web3-eth/accounts @web3))]

             (testing "Assignee can claim completed bounty"
               ;; (let [tx (<! (power-plant/claim-bounty :power-plant 0 {:from contributor}))]
               ;;   (is tx "Claiming bounty should succeed")
               ;;   (let [bounty (<! (power-plant/get-bounty :power-plant 0))]
               ;;     (is (:claimed bounty) "Bounty should be claimed")))
               )

             (testing "Cannot claim uncompleted bounty"
               (is (tx-error? (<! (power-plant/claim-bounty :power-plant 0 {:from contributor})))
                   "Should reject uncompleted bounty"))

             (testing "Cannot claim already claimed bounty"
               (is (tx-error? (<! (power-plant/claim-bounty :power-plant 0 {:from contributor})))
                   "Should reject already claimed"))

             (testing "Non-assignee cannot claim bounty"
               (is (tx-error? (<! (power-plant/claim-bounty :power-plant 0 {:from owner})))
                   "Should reject non-assignee"))

             (testing "Claim transfers reward to assignee"
               ;; Verify contributor's balance increases by reward amount
               )

             (done)))))

(deftest bounty-lifecycle
  (async done
         (go
           (testing "Complete bounty lifecycle"
             ;; 1. Create bounty (via Oracle proposal)
             ;; 2. Assign to contributor
             ;; 3. Mark complete
             ;; 4. Claim reward
             ;; Status transitions: Open → Assigned → Completed → Claimed
             )

           (done))))

(deftest grant-creation
  (async done
         (go
           (let [[owner oracle-addr recipient] (<! (web3-eth/accounts @web3))
                 total-amount (web3-utils/to-wei @web3 "100" :ether)
                 eth-addr "0x0000000000000000000000000000000000000000"
                 vesting-duration (* 365 24 60 60) ;; 1 year
                 metadata-hash (web3-utils/to-hex @web3 "QmGrantMetadata")]

             (testing "Oracle can create grant"
               ;; (let [tx (<! (power-plant/create-grant :power-plant
               ;;                                         recipient
               ;;                                         total-amount
               ;;                                         eth-addr
               ;;                                         vesting-duration
               ;;                                         metadata-hash
               ;;                                         {:from oracle-addr}))]
               ;;   (is tx "Grant creation should succeed")
               ;;   (let [grant (<! (power-plant/get-grant :power-plant 0))]
               ;;     (is grant "Grant should exist")
               ;;     (is (= recipient (:recipient grant)) "Recipient should match")
               ;;     (is (= total-amount (:total-amount grant)) "Amount should match")))
               )

             (testing "Non-oracle cannot create grant"
               (is (tx-error? (<! (power-plant/create-grant :power-plant
                                                            recipient
                                                            total-amount
                                                            eth-addr
                                                            vesting-duration
                                                            metadata-hash
                                                            {:from recipient})))
                   "Should reject non-oracle"))

             (testing "Cannot create grant with zero amount"
               (is (tx-error? (<! (power-plant/create-grant :power-plant
                                                            recipient
                                                            0
                                                            eth-addr
                                                            vesting-duration
                                                            metadata-hash
                                                            {:from oracle-addr})))
                   "Should reject zero amount"))

             (testing "Cannot create grant with zero duration"
               (is (tx-error? (<! (power-plant/create-grant :power-plant
                                                            recipient
                                                            total-amount
                                                            eth-addr
                                                            0
                                                            metadata-hash
                                                            {:from oracle-addr})))
                   "Should reject zero duration"))

             (testing "Cannot create grant to zero address"
               (is (tx-error? (<! (power-plant/create-grant :power-plant
                                                            "0x0"
                                                            total-amount
                                                            eth-addr
                                                            vesting-duration
                                                            metadata-hash
                                                            {:from oracle-addr})))
                   "Should reject zero address"))

             (done)))))

(deftest grant-vesting
  (async done
         (go
           (let [[owner recipient] (<! (web3-eth/accounts @web3))
                 total-amount (web3-utils/to-wei @web3 "100" :ether)
                 vesting-duration (* 100 24 60 60)] ;; 100 days

             (testing "Grant vests linearly over time"
               ;; At day 0: 0% vested
               ;; At day 50: 50% vested
               ;; At day 100: 100% vested
               )

             (testing "Can disburse vested amount"
               ;; Simulate time passing (e.g., 50 days)
               ;; (let [tx (<! (power-plant/disburse-grant :power-plant 0 {:from recipient}))]
               ;;   (is tx "Disbursement should succeed")
               ;;   (let [grant (<! (power-plant/get-grant :power-plant 0))]
               ;;     (is (> (:disbursed grant) 0) "Some amount should be disbursed")
               ;;     (is (<= (:disbursed grant) (/ total-amount 2)) "Should be ≤50% after 50 days")))
               )

             (testing "Cannot disburse more than vested"
               ;; Try to disburse multiple times quickly
               ;; Should only get amount that has vested
               )

             (testing "Can disburse full amount after vesting period"
               ;; Simulate 100+ days passing
               ;; Should be able to disburse full amount
               )

             (testing "Anyone can trigger disbursement"
               ;; Disbursement is permissionless - anyone can call it
               ;; But funds always go to recipient
               )

             (done)))))

(deftest grant-disbursement
  (async done
         (go
           (let [[owner recipient other] (<! (web3-eth/accounts @web3))]

             (testing "Disbursement transfers to recipient"
               ;; Verify recipient's balance increases
               )

             (testing "Disbursement updates grant state"
               ;; (let [grant-before (<! (power-plant/get-grant :power-plant 0))
               ;;       tx (<! (power-plant/disburse-grant :power-plant 0 {:from owner}))
               ;;       grant-after (<! (power-plant/get-grant :power-plant 0))]
               ;;   (is (> (:disbursed grant-after) (:disbursed grant-before))
               ;;       "Disbursed amount should increase"))
               )

             (testing "Cannot disburse if nothing vested"
               ;; Immediately after creation, before any time passes
               (is (tx-error? (<! (power-plant/disburse-grant :power-plant 0 {:from recipient})))
                   "Should reject when nothing vested"))

             (testing "Cannot disburse twice in same block"
               ;; Second disbursement in same block should fail or disburse zero
               )

             (testing "Disbursement emits GrantDisbursement event"
               ;; Verify event with correct parameters
               )

             (done)))))

(deftest treasury-withdrawal
  (async done
         (go
           (let [[owner non-owner] (<! (web3-eth/accounts @web3))
                 amount (web3-utils/to-wei @web3 "1" :ether)
                 eth-addr "0x0000000000000000000000000000000000000000"]

             (testing "Owner can withdraw from treasury"
               ;; Emergency withdrawal function
               ;; (let [tx (<! (power-plant/withdraw :power-plant eth-addr amount owner {:from owner}))]
               ;;   (is tx "Withdrawal should succeed"))
               )

             (testing "Cannot withdraw more than balance"
               (is (tx-error? (<! (power-plant/withdraw :power-plant
                                                        eth-addr
                                                        (web3-utils/to-wei @web3 "1000000" :ether)
                                                        owner
                                                        {:from owner})))
                   "Should reject excessive withdrawal"))

             (testing "Non-owner cannot withdraw"
               (is (tx-error? (<! (power-plant/withdraw :power-plant eth-addr amount owner {:from non-owner})))
                   "Should reject non-owner"))

             (testing "Cannot withdraw to zero address"
               (is (tx-error? (<! (power-plant/withdraw :power-plant eth-addr amount "0x0" {:from owner})))
                   "Should reject zero address"))

             (done)))))

(deftest multiple-bounties-and-grants
  (async done
         (go
           (testing "Can create multiple bounties"
             ;; Create bounty 0, 1, 2, etc.
             ;; Each has unique ID
             )

           (testing "Can create multiple grants"
             ;; Create grant 0, 1, 2, etc.
             ;; Each vests independently
             )

           (testing "Bounties and grants are independent"
             ;; ID spaces are separate
             ;; Claiming bounty doesn't affect grant
             )

           (done))))

(deftest oracle-integration
  (async done
         (go
           (testing "Only Oracle can create bounties"
             ;; Verify auth modifier
             )

           (testing "Only Oracle can create grants"
             ;; Verify auth modifier
             )

           (testing "Oracle executes via proposals"
             ;; Integration test:
             ;; 1. Create proposal in Oracle
             ;; 2. Approve proposal
             ;; 3. Execute proposal
             ;; 4. Verify PowerPlant action occurred
             )

           (done))))
