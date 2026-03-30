;; Council (The Backroom) Database Schema Additions
;; Add these definitions to src/district_registry/server/db.cljs after events-columns

;; Council Seats table - 9 philosophical minds
(def council-seats-columns
  [[:council-seat/index :unsigned :integer primary-key not-nil]
   [:council-seat/mind-id sha3-hash not-nil]
   [:council-seat/name :string not-nil]
   [:council-seat/philosophy-hash ipfs-hash default-nil]
   [:council-seat/is-immutable :boolean default-false]
   [:council-seat/is-active :boolean default-false]
   [:council-seat/created-on :unsigned :integer not-nil]
   [:council-seat/total-staked :unsigned :BIG :INT default-zero]])

;; Council Stakes table - stake history per seat
(def council-stakes-columns
  [[:council-stake/id :serial primary-key]
   [:council-seat/index :unsigned :integer not-nil]
   [:council-stake/staker address not-nil]
   [:council-stake/amount :unsigned :BIG :INT not-nil]
   [:council-stake/is-unstake :boolean default-false]
   [:council-stake/block-number :unsigned :BIG :INT not-nil]
   [:council-stake/timestamp :unsigned :integer not-nil]
   [:council-stake/tx-hash sha3-hash not-nil]
   [(sql/call :foreign-key :council-seat/index) (sql/call :references :council-seats :council-seat/index)]])

;; Council Stake Balances table - denormalized current balances
(def council-stake-balances-columns
  [[:council-seat/index :unsigned :integer not-nil]
   [:council-stake-balance/staker address not-nil]
   [:council-stake-balance/balance :unsigned :BIG :INT default-zero]
   [(sql/call :primary-key :council-seat/index :council-stake-balance/staker)]
   [(sql/call :foreign-key :council-seat/index) (sql/call :references :council-seats :council-seat/index)]])

;; ARI Proposals table - proposals from ARI oracle
(def ari-proposals-columns
  [[:ari-proposal/id :unsigned :integer primary-key not-nil]
   [:ari-proposal/type :varchar not-nil]  ; TreasuryTransfer, BountyCreation, etc.
   [:ari-proposal/reasoning-hash ipfs-hash not-nil]
   [:ari-proposal/reasoning-text :text default-nil]  ; Fetched from IPFS
   [:ari-proposal/council-weights-snapshot :jsonb default-nil]
   [:ari-proposal/snapshot-block :unsigned :BIG :INT not-nil]
   [:ari-proposal/veto-deadline :unsigned :integer not-nil]
   [:ari-proposal/amount :unsigned :BIG :INT default-zero]
   [:ari-proposal/recipient address default-nil]
   [:ari-proposal/call-data :text default-nil]
   [:ari-proposal/status :varchar not-nil]  ; Pending, VetoPeriod, Approved, Vetoed, Executed
   [:ari-proposal/veto-stake-against :unsigned :BIG :INT default-zero]
   [:ari-proposal/created-on :unsigned :integer not-nil]
   [:ari-proposal/executed-on :unsigned :integer default-nil]])

;; Proposal Vetos table - veto votes on proposals
(def proposal-vetos-columns
  [[:ari-proposal/id :unsigned :integer not-nil]
   [:proposal-veto/staker address not-nil]
   [:proposal-veto/amount :unsigned :BIG :INT not-nil]
   [:proposal-veto/created-on :unsigned :integer not-nil]
   [(sql/call :primary-key :ari-proposal/id :proposal-veto/staker)]
   [(sql/call :foreign-key :ari-proposal/id) (sql/call :references :ari-proposals :ari-proposal/id)]])

;; Bounties table - Power Plant bounties
(def bounties-columns
  [[:bounty/id :unsigned :integer primary-key not-nil]
   [:bounty/metadata-hash ipfs-hash not-nil]
   [:bounty/title :string default-nil]
   [:bounty/description :text default-nil]
   [:bounty/reward :unsigned :BIG :INT not-nil]
   [:bounty/token-address address default-nil]
   [:bounty/deadline :unsigned :integer default-nil]
   [:bounty/assignee address default-nil]
   [:bounty/completed :boolean default-false]
   [:bounty/claimed :boolean default-false]
   [:bounty/created-on :unsigned :integer not-nil]])

;; Grants table - Power Plant grants with vesting
(def grants-columns
  [[:grant/id :unsigned :integer primary-key not-nil]
   [:grant/recipient address not-nil]
   [:grant/total-amount :unsigned :BIG :INT not-nil]
   [:grant/disbursed :unsigned :BIG :INT default-zero]
   [:grant/vesting-start :unsigned :integer not-nil]
   [:grant/vesting-duration :unsigned :integer not-nil]
   [:grant/purpose-hash ipfs-hash default-nil]
   [:grant/purpose-text :text default-nil]
   [:grant/created-on :unsigned :integer not-nil]])

;; Column name definitions (add after existing column definitions)
(def council-seats-column-names (map first council-seats-columns))
(def council-stakes-column-names (map first council-stakes-columns))
(def council-stake-balances-column-names (map first council-stake-balances-columns))
(def ari-proposals-column-names (map first ari-proposals-columns))
(def proposal-vetos-column-names (map first proposal-vetos-columns))
(def bounties-column-names (map first bounties-columns))
(def grants-column-names (map first grants-columns))

;; Add to clean-db function tables list:
;; :council-seats :council-stakes :council-stake-balances
;; :ari-proposals :proposal-vetos :bounties :grants

;; Add to create-indexes function:
;; (doseq [column [:council-seat/index :council-seat/total-staked]]
;;   (db/run! {:create-index (index-name column) :on [:council-seats column]}))
;;
;; (doseq [column [:council-stake/staker :council-stake/timestamp]]
;;   (db/run! {:create-index (index-name column) :on [:council-stakes column]}))
;;
;; (doseq [column [:ari-proposal/status :ari-proposal/created-on]]
;;   (db/run! {:create-index (index-name column) :on [:ari-proposals column]}))

;; Add to start function (create tables):
;; (db/run! (-> (psqlh/create-table :council-seats :if-not-exists)
;;            (psqlh/with-columns council-seats-columns)))
;;
;; (db/run! (-> (psqlh/create-table :council-stakes :if-not-exists)
;;            (psqlh/with-columns council-stakes-columns)))
;;
;; (db/run! (-> (psqlh/create-table :council-stake-balances :if-not-exists)
;;            (psqlh/with-columns council-stake-balances-columns)))
;;
;; (db/run! (-> (psqlh/create-table :ari-proposals :if-not-exists)
;;            (psqlh/with-columns ari-proposals-columns)))
;;
;; (db/run! (-> (psqlh/create-table :proposal-vetos :if-not-exists)
;;            (psqlh/with-columns proposal-vetos-columns)))
;;
;; (db/run! (-> (psqlh/create-table :bounties :if-not-exists)
;;            (psqlh/with-columns bounties-columns)))
;;
;; (db/run! (-> (psqlh/create-table :grants :if-not-exists)
;;            (psqlh/with-columns grants-columns)))
