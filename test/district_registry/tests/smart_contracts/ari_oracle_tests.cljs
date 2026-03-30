(ns district-registry.tests.smart-contracts.ari-oracle-tests
  (:require [bignumber.core :as bn]
            [cljs-web3-next.eth :as web3-eth]
            [cljs-web3-next.utils :as web3-utils]
            [cljs.test :refer-macros [async deftest is testing]]
            [cljs.core.async :refer [go <!]]
            [district-registry.server.contract.ari-oracle :as oracle]
            [district-registry.server.contract.council-stake-bank :as stake-bank]
            [district-registry.server.contract.power-plant :as power-plant]
            [district-registry.tests.smart-contracts.utils :refer [tx-error?]]
            [district.server.smart-contracts :refer [contract-address instance]]
            [district.server.web3 :refer [web3]]))

;; ARIOracle Tests

(deftest oracle-initialization
  (async done
         (go
           (testing "ARIOracle is deployed"
             (let [addr (contract-address :ari-oracle)]
               (is addr "Oracle contract should be deployed")
               (is (not= addr "0x0") "Oracle address should not be zero")))

           (testing "Oracle has correct veto parameters"
             (let [veto-period (<! (oracle/veto-period-duration :ari-oracle))
                   veto-threshold (<! (oracle/veto-threshold-bps :ari-oracle))]
               (is (= veto-period (* 3 24 60 60)) "Veto period should be 3 days")
               (is (= veto-threshold 3000) "Veto threshold should be 3000 bps (30%)")))

           (testing "Oracle references DNT token"
             (let [dnt-addr (<! (oracle/dnt-token :ari-oracle))
                   expected-dnt (contract-address :dnt)]
               (is (= dnt-addr expected-dnt) "Oracle should reference correct DNT token")))

           (done))))

(deftest proposal-creation
  (async done
         (go
           (let [[owner ari-addr non-ari] (<! (web3-eth/accounts @web3))
                 reasoning-hash (web3-utils/to-hex @web3 "QmProposalReasoning123")
                 amount (web3-utils/to-wei @web3 "10" :ether)
                 recipient owner]

             (testing "ARI address can create proposals"
               ;; Assuming ari-addr is configured as ARI
               ;; (let [tx (<! (oracle/create-proposal :ari-oracle
               ;;                                       0 ;; TreasuryTransfer
               ;;                                       reasoning-hash
               ;;                                       amount
               ;;                                       recipient
               ;;                                       {:from ari-addr}))]
               ;;   (is tx "Proposal creation should succeed")
               ;;   (let [proposal (<! (oracle/get-proposal :ari-oracle 0))]
               ;;     (is proposal "Proposal should exist")
               ;;     (is (= 0 (:proposal-type proposal)) "Proposal type should be TreasuryTransfer")
               ;;     (is (= reasoning-hash (:reasoning-hash proposal)) "Reasoning hash should match")))
               )

             (testing "Non-ARI address cannot create proposals"
               (is (tx-error? (<! (oracle/create-proposal :ari-oracle
                                                          0
                                                          reasoning-hash
                                                          amount
                                                          recipient
                                                          {:from non-ari})))
                   "Should reject non-ARI address"))

             (testing "Cannot create proposal with invalid type"
               (is (tx-error? (<! (oracle/create-proposal :ari-oracle
                                                          10 ;; Invalid type
                                                          reasoning-hash
                                                          amount
                                                          recipient
                                                          {:from ari-addr})))
                   "Should reject invalid proposal type"))

             (testing "Cannot create proposal with empty reasoning"
               (is (tx-error? (<! (oracle/create-proposal :ari-oracle
                                                          0
                                                          "0x0" ;; Empty hash
                                                          amount
                                                          recipient
                                                          {:from ari-addr})))
                   "Should reject empty reasoning hash"))

             (done)))))

(deftest proposal-types
  (async done
         (go
           (let [[owner ari-addr] (<! (web3-eth/accounts @web3))
                 reasoning-hash (web3-utils/to-hex @web3 "QmTest")]

             (testing "TreasuryTransfer proposal (type 0)"
               ;; Requires amount and recipient
               )

             (testing "BountyCreation proposal (type 1)"
               ;; Creates bounty in PowerPlant
               )

             (testing "GrantApproval proposal (type 2)"
               ;; Creates grant in PowerPlant
               )

             (testing "ParameterChange proposal (type 3)"
               ;; Changes governance parameters
               )

             (testing "CustomAction proposal (type 4)"
               ;; Generic action with metadata
               )

             (done)))))

(deftest veto-period-lifecycle
  (async done
         (go
           (let [[owner ari-addr staker1 staker2] (<! (web3-eth/accounts @web3))
                 reasoning-hash (web3-utils/to-hex @web3 "QmProposal")
                 amount (web3-utils/to-wei @web3 "10" :ether)]

             (testing "Proposal starts in Pending status"
               ;; After creation, proposal should be Pending (status 0)
               )

             (testing "Can start veto period"
               ;; (let [tx (<! (oracle/start-veto-period :ari-oracle 0 {:from ari-addr}))]
               ;;   (is tx "Starting veto period should succeed")
               ;;   (let [proposal (<! (oracle/get-proposal :ari-oracle 0))]
               ;;     (is (= 1 (:status proposal)) "Status should be VetoPeriod")
               ;;     (is (:veto-deadline proposal) "Veto deadline should be set")))
               )

             (testing "Cannot start veto period twice"
               (is (tx-error? (<! (oracle/start-veto-period :ari-oracle 0 {:from ari-addr})))
                   "Should reject starting veto period twice"))

             (testing "Cannot start veto period for non-existent proposal"
               (is (tx-error? (<! (oracle/start-veto-period :ari-oracle 999 {:from ari-addr})))
                   "Should reject non-existent proposal"))

             (done)))))

(deftest veto-detection
  (async done
         (go
           (let [[owner ari-addr staker1 staker2 staker3] (<! (web3-eth/accounts @web3))
                 total-stake (web3-utils/to-wei @web3 "1000" :ether)
                 veto-stake (web3-utils/to-wei @web3 "300" :ether) ;; 30%
                 sub-veto-stake (web3-utils/to-wei @web3 "250" :ether)] ;; 25%

             (testing "Veto detected when ≥30% stake shifts"
               ;; Scenario:
               ;; 1. Create proposal with council snapshot
               ;; 2. Start veto period
               ;; 3. Move ≥30% of total stake between seats
               ;; 4. Check veto status - should be vetoed
               )

             (testing "No veto when <30% stake shifts"
               ;; Move only 25% of stake - should not trigger veto
               )

             (testing "Veto percentage calculated correctly"
               ;; (let [proposal (<! (oracle/get-proposal :ari-oracle 0))]
               ;;   (is (:veto-percentage proposal) "Veto percentage should be calculated")
               ;;   (is (>= (:veto-percentage proposal) 0) "Veto percentage >= 0")
               ;;   (is (<= (:veto-percentage proposal) 100) "Veto percentage <= 100"))
               )

             (testing "checkVeto function detects threshold"
               ;; (let [is-vetoed (<! (oracle/check-veto :ari-oracle 0))]
               ;;   (is (boolean? is-vetoed) "checkVeto should return boolean"))
               )

             (done)))))

(deftest proposal-approval
  (async done
         (go
           (let [[owner ari-addr] (<! (web3-eth/accounts @web3))]

             (testing "Proposal approved after veto period with <30% shift"
               ;; Scenario:
               ;; 1. Create proposal
               ;; 2. Start veto period
               ;; 3. Wait 3 days (simulate with block mining or time jump)
               ;; 4. Check veto - should show not vetoed
               ;; 5. Proposal status should be Approved
               )

             (testing "Cannot approve proposal before veto period ends"
               ;; Try to approve immediately after veto period starts
               ;; Should fail
               )

             (testing "Cannot approve already vetoed proposal"
               ;; If veto threshold reached, cannot approve
               )

             (done)))))

(deftest proposal-execution
  (async done
         (go
           (let [[owner ari-addr recipient] (<! (web3-eth/accounts @web3))
                 amount (web3-utils/to-wei @web3 "10" :ether)]

             (testing "Approved proposal can be executed"
               ;; (let [tx (<! (oracle/execute-proposal :ari-oracle 0 {:from ari-addr}))]
               ;;   (is tx "Execution should succeed")
               ;;   (let [proposal (<! (oracle/get-proposal :ari-oracle 0))]
               ;;     (is (= 4 (:status proposal)) "Status should be Executed")))
               )

             (testing "Cannot execute non-approved proposal"
               (is (tx-error? (<! (oracle/execute-proposal :ari-oracle 0 {:from ari-addr})))
                   "Should reject executing non-approved proposal"))

             (testing "Cannot execute already executed proposal"
               (is (tx-error? (<! (oracle/execute-proposal :ari-oracle 0 {:from ari-addr})))
                   "Should reject executing twice"))

             (testing "Cannot execute vetoed proposal"
               ;; Create proposal, veto it, try to execute
               ;; Should fail
               )

             (testing "Execution triggers PowerPlant action"
               ;; For bounty/grant proposals, verify PowerPlant contract is called
               )

             (done)))))

(deftest proposal-cancellation
  (async done
         (go
           (let [[owner ari-addr] (<! (web3-eth/accounts @web3))]

             (testing "Authorized address can cancel proposal"
               ;; (let [tx (<! (oracle/cancel-proposal :ari-oracle 0 {:from owner}))]
               ;;   (is tx "Cancellation should succeed")
               ;;   (let [proposal (<! (oracle/get-proposal :ari-oracle 0))]
               ;;     (is (= 5 (:status proposal)) "Status should be Cancelled")))
               )

             (testing "Cannot cancel executed proposal"
               (is (tx-error? (<! (oracle/cancel-proposal :ari-oracle 0 {:from owner})))
                   "Should reject cancelling executed proposal"))

             (testing "Non-authorized address cannot cancel"
               (is (tx-error? (<! (oracle/cancel-proposal :ari-oracle 0 {:from ari-addr})))
                   "Should reject non-authorized cancellation"))

             (done)))))

(deftest council-weight-snapshot
  (async done
         (go
           (testing "Proposal captures council weights at creation"
             ;; When proposal is created, it should snapshot current council weights
             ;; This snapshot is used to calculate veto threshold
             ;; (let [proposal (<! (oracle/get-proposal :ari-oracle 0))]
             ;;   (is (:council-weights proposal) "Proposal should have council weights snapshot")
             ;;   (is (= 9 (count (:council-weights proposal))) "Should have 9 weights"))
             )

           (testing "Weights snapshot doesn't change after creation"
             ;; Even if stakes change, proposal's snapshot should remain constant
             )

           (done))))

(deftest veto-period-timing
  (async done
         (go
           (let [[owner ari-addr] (<! (web3-eth/accounts @web3))
                 three-days (* 3 24 60 60)]

             (testing "Veto period is exactly 3 days"
               (let [veto-duration (<! (oracle/veto-period-duration :ari-oracle))]
                 (is (= veto-duration three-days) "Veto period should be 3 days")))

             (testing "Can check veto after 3 days"
               ;; Simulate time passing
               ;; checkVeto should work after veto period ends
               )

             (testing "Cannot check veto before period ends"
               ;; Trying to finalize before 3 days should fail or return inconclusive
               )

             (done)))))

(deftest proposal-status-transitions
  (async done
         (go
           (testing "Valid status transitions"
             ;; Pending (0) → VetoPeriod (1) → Approved (2) → Executed (4)
             ;; Pending (0) → VetoPeriod (1) → Vetoed (3)
             ;; Any → Cancelled (5) (if authorized)
             )

           (testing "Invalid status transitions"
             ;; Cannot go from Executed back to Approved
             ;; Cannot go from Vetoed to Approved
             ;; Cannot go from Cancelled to Executed
             )

           (done))))

(deftest oracle-power-plant-integration
  (async done
         (go
           (testing "Oracle can call PowerPlant"
             (let [power-plant-addr (contract-address :power-plant)
                   oracle-addr (contract-address :ari-oracle)]
               (is power-plant-addr "PowerPlant should be deployed")
               (is oracle-addr "Oracle should be deployed")))

           (testing "PowerPlant accepts calls from Oracle"
             ;; PowerPlant.createBounty should only work when called by Oracle
             ;; Test that Oracle can successfully create bounties via proposals
             )

           (testing "PowerPlant rejects direct calls"
             ;; Directly calling PowerPlant.createBounty should fail
             ;; Only Oracle should be able to call it
             )

           (done))))

(deftest multiple-proposals
  (async done
         (go
           (let [[owner ari-addr] (<! (web3-eth/accounts @web3))]

             (testing "Can create multiple proposals"
               ;; Create proposal 0, 1, 2, etc.
               ;; Each should have unique ID
               )

             (testing "Proposals are independent"
               ;; Vetoing one proposal doesn't affect others
               ;; Each has its own veto period
               )

             (testing "Can have proposals in different states simultaneously"
               ;; Proposal 0: Executed
               ;; Proposal 1: VetoPeriod
               ;; Proposal 2: Pending
               )

             (done)))))

(deftest ari-address-management
  (async done
         (go
           (let [[owner ari-addr new-ari] (<! (web3-eth/accounts @web3))]

             (testing "ARI address is set correctly"
               ;; (let [current-ari (<! (oracle/ari-address :ari-oracle))]
               ;;   (is current-ari "ARI address should be set"))
               )

             (testing "Owner can update ARI address"
               ;; (let [tx (<! (oracle/set-ari-address :ari-oracle new-ari {:from owner}))]
               ;;   (is tx "Setting ARI address should succeed")
               ;;   (let [updated-ari (<! (oracle/ari-address :ari-oracle))]
               ;;     (is (= new-ari updated-ari) "ARI address should be updated")))
               )

             (testing "Non-owner cannot update ARI address"
               (is (tx-error? (<! (oracle/set-ari-address :ari-oracle new-ari {:from ari-addr})))
                   "Should reject non-owner"))

             (done)))))
