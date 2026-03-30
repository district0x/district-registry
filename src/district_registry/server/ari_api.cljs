(ns district-registry.server.ari-api
  "REST API endpoints for ARI (The Stranger) to interact with The Backroom governance system.

   ARI uses these endpoints to:
   - Read current council weights and state
   - Submit proposals with cryptographic signatures
   - Query proposal status and veto progress

   All write operations require ARI signature verification."
  (:require
    [cljs-web3-next.core :as web3-core]
    [cljs-web3-next.eth :as web3-eth]
    [cljs-web3-next.utils :as web3-utils]
    [district-registry.server.db :as db]
    [district-registry.server.utils :as server-utils]
    [district.server.config :refer [config]]
    [district.server.smart-contracts :as smart-contracts]
    [district.server.web3 :refer [web3]]
    [district.shared.error-handling :refer [try-catch-throw]]
    [mount.core :as mount :refer [defstate]]
    [taoensso.timbre :as log]))

;; ========================================
;; Configuration
;; ========================================

(def ari-address
  "The authorized Ethereum address for ARI to submit proposals.
   This should be configured via environment variables in production."
  (atom nil))

(defn set-ari-address! [address]
  (reset! ari-address address)
  (log/info "ARI address configured" {:address address}))

;; ========================================
;; Signature Verification
;; ========================================

(defn verify-ari-signature
  "Verifies that a message was signed by the authorized ARI address.

   Parameters:
   - message: The original message that was signed
   - signature: The hex-encoded signature

   Returns: Boolean indicating if signature is valid"
  [message signature]
  (try-catch-throw
    (let [message-hash (web3-utils/sha3 @web3 message)
          recovered-address (web3-eth/recover @web3 message-hash signature)]
      (log/debug "Verifying ARI signature"
                 {:message-hash message-hash
                  :recovered-address recovered-address
                  :expected-address @ari-address})
      (= (web3-utils/to-checksum-address @web3 recovered-address)
         (web3-utils/to-checksum-address @web3 @ari-address)))))

(defn require-ari-signature!
  "Middleware-style function that verifies ARI signature or throws error.

   Parameters:
   - message: The message that should have been signed
   - signature: The signature to verify

   Throws: Error if signature is invalid or ARI address not configured"
  [message signature]
  (when-not @ari-address
    (throw (js/Error. "ARI address not configured")))
  (when-not (verify-ari-signature message signature)
    (throw (js/Error. "Invalid ARI signature")))
  true)

;; ========================================
;; Read Endpoints
;; ========================================

(defn get-council-state
  "Returns the current state of The Council.

   Returns:
   - seats: Array of all 9 council seats with metadata
   - total-staked: Total DNT staked across all seats
   - timestamp: Current block timestamp"
  []
  (try-catch-throw
    (let [seats (db/all {:select [:*]
                         :from [:council-seats]
                         :order-by [:council-seat/index]})
          total-staked (reduce #(+ %1 (:council-seat/total-staked %2 0)) 0 seats)]
      {:seats seats
       :total-staked total-staked
       :timestamp (server-utils/now-in-seconds)
       :block-number (web3-eth/block-number @web3)})))

(defn get-council-weights
  "Returns the current stake distribution across council seats as percentages.

   Returns:
   - weights: Map of seat index to percentage (0-100)
   - total-staked: Total DNT staked
   - snapshot-block: Current block number for historical reference"
  []
  (try-catch-throw
    (let [seats (db/all {:select [:*]
                         :from [:council-seats]
                         :where [:= :council-seat/is-active true]
                         :order-by [:council-seat/index]})
          total-staked (reduce #(+ %1 (:council-seat/total-staked %2 0)) 0 seats)
          weights (if (zero? total-staked)
                   (into {} (map #(vector (:council-seat/index %) 0) seats))
                   (into {} (map #(vector (:council-seat/index %)
                                         (* 100.0 (/ (:council-seat/total-staked % 0)
                                                    total-staked)))
                                seats)))]
      {:weights weights
       :total-staked total-staked
       :snapshot-block (web3-eth/block-number @web3)
       :timestamp (server-utils/now-in-seconds)})))

(defn get-active-proposals
  "Returns all active ARI proposals (Pending or VetoPeriod status).

   Returns:
   - proposals: Array of active proposals with veto progress"
  []
  (try-catch-throw
    (let [now (server-utils/now-in-seconds)
          proposals (db/all {:select [:*]
                            :from [:ari-proposals]
                            :where [:or
                                   [:= :ari-proposal/status 0]  ; Pending
                                   [:= :ari-proposal/status 1]] ; VetoPeriod
                            :order-by [[:ari-proposal/created-on :desc]]})]
      {:proposals proposals
       :count (count proposals)
       :timestamp now})))

(defn get-proposal-status
  "Returns detailed status of a specific proposal.

   Parameters:
   - proposal-id: The ID of the proposal to query

   Returns:
   - proposal: Full proposal data
   - veto-percentage: Current veto progress as percentage
   - time-remaining: Seconds until veto period ends (if applicable)"
  [proposal-id]
  (try-catch-throw
    (let [proposal (db/get {:select [:*]
                           :from [:ari-proposals]
                           :where [:= :ari-proposal/id proposal-id]})
          now (server-utils/now-in-seconds)
          total-stake (or (:sum (db/get {:select [[(cljs.core/symbol "sum(council_seat.total_staked)") :sum]]
                                         :from [:council-seats]
                                         :where [:= :council-seat/is-active true]}))
                         0)
          veto-percentage (if (zero? total-stake)
                           0
                           (* 100.0 (/ (:ari-proposal/veto-stake-against proposal 0)
                                      total-stake)))
          time-remaining (if (= 1 (:ari-proposal/status proposal))
                          (max 0 (- (:ari-proposal/veto-deadline proposal) now))
                          nil)]
      (if proposal
        {:proposal proposal
         :veto-percentage veto-percentage
         :time-remaining time-remaining
         :total-stake total-stake
         :timestamp now}
        {:error "Proposal not found"
         :proposal-id proposal-id}))))

(defn get-treasury-balance
  "Returns current Power Plant treasury balances.

   Returns:
   - balances: Map of token address to balance
   - eth-balance: Native ETH balance
   - timestamp: Current timestamp"
  []
  (try-catch-throw
    (let [power-plant-address (smart-contracts/contract-address :power-plant)]
      (if power-plant-address
        (let [eth-balance (web3-eth/get-balance @web3 power-plant-address)
              ;; TODO: Query ERC20 token balances when needed
              ]
          {:eth-balance (str eth-balance)
           :balances {}
           :timestamp (server-utils/now-in-seconds)})
        {:error "PowerPlant contract not deployed"}))))

;; ========================================
;; Write Endpoints (Require ARI Signature)
;; ========================================

(defn create-proposal
  "Creates a new proposal from ARI.

   This is the main endpoint for ARI to submit governance proposals.
   Requires valid ARI signature.

   Parameters:
   - proposal-type: Integer 0-4 (TreasuryTransfer, BountyCreation, etc.)
   - reasoning-hash: IPFS hash containing ARI's reasoning
   - amount: Amount for transfers/bounties/grants (optional)
   - recipient: Recipient address for transfers/grants (optional)
   - metadata: Additional proposal metadata as JSON
   - signature: ARI's cryptographic signature

   Returns:
   - tx-hash: Transaction hash of proposal creation
   - proposal-id: ID of created proposal"
  [{:keys [proposal-type reasoning-hash amount recipient metadata signature] :as params}]
  (try-catch-throw
    ;; Verify ARI signature
    (let [message (str proposal-type reasoning-hash amount recipient metadata)]
      (require-ari-signature! message signature))

    ;; Validate proposal parameters
    (when-not (and (>= proposal-type 0) (<= proposal-type 4))
      (throw (js/Error. "Invalid proposal type. Must be 0-4")))
    (when-not reasoning-hash
      (throw (js/Error. "reasoning-hash is required")))

    (log/info "Creating ARI proposal"
              {:proposal-type proposal-type
               :reasoning-hash reasoning-hash
               :amount amount
               :recipient recipient})

    ;; Call smart contract to create proposal
    ;; This will trigger an event that syncer.cljs will pick up
    (let [ari-oracle (smart-contracts/instance :ari-oracle)
          tx-hash (case proposal-type
                   0 ; TreasuryTransfer
                   (smart-contracts/contract-call ari-oracle
                                                  :create-proposal
                                                  [proposal-type reasoning-hash amount recipient])
                   1 ; BountyCreation
                   (smart-contracts/contract-call ari-oracle
                                                  :create-proposal
                                                  [proposal-type reasoning-hash amount nil])
                   2 ; GrantApproval
                   (smart-contracts/contract-call ari-oracle
                                                  :create-proposal
                                                  [proposal-type reasoning-hash amount recipient])
                   ; Default for other types
                   (smart-contracts/contract-call ari-oracle
                                                  :create-proposal
                                                  [proposal-type reasoning-hash 0 nil]))]
      {:tx-hash tx-hash
       :proposal-type proposal-type
       :reasoning-hash reasoning-hash
       :timestamp (server-utils/now-in-seconds)
       :message "Proposal submitted successfully. Monitor proposal-id from ProposalCreated event."})))

;; ========================================
;; API Response Helpers
;; ========================================

(defn success-response
  "Creates a standardized success response"
  [data]
  {:success true
   :data data
   :timestamp (server-utils/now-in-seconds)})

(defn error-response
  "Creates a standardized error response"
  [message & [details]]
  {:success false
   :error message
   :details details
   :timestamp (server-utils/now-in-seconds)})

;; ========================================
;; HTTP Handlers (for express/ring integration)
;; ========================================

(defn handle-get-council-state [req res]
  (try
    (let [data (get-council-state)]
      (.json res (clj->js (success-response data))))
    (catch js/Error e
      (log/error "Error in get-council-state" {:error (.-message e)})
      (.status res 500)
      (.json res (clj->js (error-response (.-message e)))))))

(defn handle-get-council-weights [req res]
  (try
    (let [data (get-council-weights)]
      (.json res (clj->js (success-response data))))
    (catch js/Error e
      (log/error "Error in get-council-weights" {:error (.-message e)})
      (.status res 500)
      (.json res (clj->js (error-response (.-message e)))))))

(defn handle-get-active-proposals [req res]
  (try
    (let [data (get-active-proposals)]
      (.json res (clj->js (success-response data))))
    (catch js/Error e
      (log/error "Error in get-active-proposals" {:error (.-message e)})
      (.status res 500)
      (.json res (clj->js (error-response (.-message e)))))))

(defn handle-get-proposal-status [req res]
  (try
    (let [proposal-id (js/parseInt (.-id (.-params req)))
          data (get-proposal-status proposal-id)]
      (.json res (clj->js (success-response data))))
    (catch js/Error e
      (log/error "Error in get-proposal-status" {:error (.-message e)})
      (.status res 500)
      (.json res (clj->js (error-response (.-message e)))))))

(defn handle-create-proposal [req res]
  (try
    (let [body (js->clj (.-body req) :keywordize-keys true)
          data (create-proposal body)]
      (.json res (clj->js (success-response data))))
    (catch js/Error e
      (log/error "Error in create-proposal" {:error (.-message e)})
      (.status res 400)
      (.json res (clj->js (error-response (.-message e)))))))

;; ========================================
;; Module Initialization
;; ========================================

(defn start [{:keys [ari-address] :as opts}]
  (log/info "Starting ARI API module" opts)
  (when ari-address
    (set-ari-address! ari-address))
  ::started)

(defn stop []
  (log/info "Stopping ARI API module")
  ::stopped)

(defstate ari-api
  :start (start (merge
                  (:ari-api @config)
                  (:ari-api (mount/args))))
  :stop (stop))

;; ========================================
;; Public API for export
;; ========================================

(def api-endpoints
  "Map of endpoint paths to handler functions for integration with HTTP server"
  {:get-council-state handle-get-council-state
   :get-council-weights handle-get-council-weights
   :get-active-proposals handle-get-active-proposals
   :get-proposal-status handle-get-proposal-status
   :create-proposal handle-create-proposal})
