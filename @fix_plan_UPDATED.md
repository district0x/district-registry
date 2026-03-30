# The Backroom - Implementation Fix Plan

## Phase 1: Smart Contracts (Priority: HIGH)

### Week 1: Core Contracts
- [x] **Council.sol** - 9 seats, immutable Stranger seat, seat flip mechanism ✅ COMPLETE
  - Extend from DSAuth for access control
  - Use bytes32 for mindId (keccak256 of name)
  - Store philosophy as IPFS hash
  - Seat 8 must be immutable (no function can change it)
  - Events: SeatCreated, SeatFlipped, PhilosophyUpdated

- [x] **CouncilStakeBank.sol** - DNT staking per seat with checkpoints ✅ COMPLETE
  - Follow existing StakeBank.sol checkpoint pattern
  - Mapping: seatIndex => staker => checkpoints
  - Functions: stakeOnSeat, unstakeFromSeat, moveStake
  - Snapshot queries: totalStakedForSeatAt, getWeightsAt
  - Events: StakedOnMind, UnstakedFromMind, StakeMoved

### Week 2: Oracle & Treasury Contracts
- [x] **ARIOracle.sol** - Proposal/veto mechanism ✅ COMPLETE
  - Proposal types enum: TreasuryTransfer, BountyCreation, GrantApproval, ParameterChange, CustomAction
  - 3-day veto period (vetoPeriodDuration)
  - 30% stake shift threshold (vetoThresholdBps = 3000)
  - ARI address verification for createProposal
  - Events: ProposalCreated, VetoCast, ProposalApproved, ProposalVetoed, ProposalExecuted

- [x] **PowerPlant.sol** - Treasury and bounty/grant distribution ✅ COMPLETE
  - Treasury balances per token
  - Bounty struct: id, metadataHash, reward, token, deadline, assignee, completed, claimed
  - Grant struct: id, recipient, totalAmount, disbursed, vestingStart, vestingDuration
  - Only ARIOracle can create bounties/grants
  - Events: TreasuryDeposit, BountyCreated, BountyCompleted, GrantCreated, GrantDisbursement

### Week 3: Contract Testing & Deployment
- [x] ✅ **All contracts compile successfully** (Solidity 0.4.24 compatible)
  - Fixed `type(uint256).max` → `uint256(-1)` in Council.sol:199
  - Replaced `./token/ERC20.sol` imports with `@aragon/apps-shared-minime/contracts/MiniMeToken.sol`
  - Removed abi.decode() usage in PowerPlant.sol (not available in 0.4.24)
  - Updated PowerPlant functions to take explicit parameters instead of callData
  - Added @aragon dependencies to package.json and ran npm install
- [x] ✅ **Migration script created** - migrations/10_backroom_migration.js
- [ ] Write unit tests for Council staking and weights
- [ ] Write unit tests for ARIOracle proposal/veto lifecycle
- [ ] Write unit tests for PowerPlant bounty/grant flows
- [ ] Write integration tests between contracts

## Phase 2: Backend (Priority: MEDIUM)

### Database
- [x] ✅ **All Backroom tables added to db.cljs**
  - council_seats - seat metadata (index, mindId, name, philosophy, immutability)
  - council_stakes - stake history with block numbers
  - council_stake_balances - denormalized balances per staker/seat
  - ari_proposals - proposal data with council weights snapshot
  - bounties - bounty metadata and status tracking
  - grants - grant vesting data
  - Added column-names definitions for all tables
  - Added tables to clean-db function
  - Added indexes for query optimization
  - Created insert/update/get helper functions for all tables
  - Added upsert-council-stake-balance! function for efficient balance updates

### Event Syncer
- [x] ✅ **All Backroom event handlers added to syncer.cljs**
  - Council events: SeatCreated, SeatFlipped, PhilosophyUpdated
  - CouncilStakeBank events: StakedOnMind, UnstakedFromMind, StakeMoved
  - ARIOracle events: ProposalCreated, VetoCast, ProposalApproved, ProposalVetoed, ProposalExecuted, ProposalCancelled
  - PowerPlant events: TreasuryDeposit, BountyCreated, BountyAssigned, BountyCompleted, BountyClaimed, GrantCreated, GrantDisbursement, TreasuryWithdrawal
  - All handlers follow existing pattern with try-catch and logging
  - Integrated into event-callbacks map with proper contract-key namespacing
  - Denormalized stake balances updated in real-time via upsert
  - Stake history tracked with block numbers for checkpoint queries

### GraphQL
- [x] ✅ **All Backroom GraphQL schema and resolvers complete**
  - schema.graphql: Added 8 types (CouncilSeat, ARIProposal, Bounty, Grant + List types)
  - schema.graphql: Added 3 enums (ARIProposalType, ARIProposalStatus, BountyStatus)
  - schema.graphql: Added 10 new queries to Query type
  - graphql_resolvers.cljs: Added 10 query resolvers with pagination support
  - graphql_resolvers.cljs: Added 8 field resolvers for computed fields (stakePercentage, vetoPercentage, etc.)
  - graphql_resolvers.cljs: Added 4 enum conversion resolvers (status, proposal-type)
  - Extended resolvers-map with all Backroom types
  - All resolvers follow existing patterns (paged-query, try-catch-throw, graphql-utils)
- [ ] Add mutations for staking and veto (UI-level - will be implemented with UI)

### ARI API
- [x] ✅ **Complete ARI API implementation in ari_api.cljs**
  - Read endpoints: get-council-state, get-council-weights, get-active-proposals, get-proposal-status, get-treasury-balance
  - ARI signature verification using web3 eth_recover
  - Write endpoint: create-proposal with signature requirement
  - HTTP handlers for express/ring integration
  - Mount lifecycle management (start/stop)
  - Comprehensive error handling and logging
  - Success/error response standardization
  - Configurable ARI address via environment

## Phase 3: UI (Priority: MEDIUM)

### Council Page ⏳ IN PROGRESS
- [x] ✅ **Council page component created** - `src/district_registry/ui/council/page.cljs`
  - 9-seat grid visualization
  - Individual council seat cards
  - Stake percentage and total staked display
  - Immutable badge for Seat 8 (ARI/The Stranger)
  - GraphQL integration using searchCouncilSeats query
  - Loading spinner
  - Error handling
  - "How It Works" information section
  - User stakes panel (placeholder)

- [x] ✅ **Council contract helpers created** - `src/district_registry/ui/contract/council.cljs`
  - Events: stake-on-seat, unstake-from-seat, move-stake, approve-dnt
  - Transaction handling with notifications
  - Error logging
  - Subscriptions for user stake queries (placeholders)

- [x] ✅ **Integration guide created** - `COUNCIL_UI_INTEGRATION.md`
  - Step-by-step integration instructions
  - Required changes to routes.cljs, core.cljs, app_layout.cljs
  - Complete CSS styling
  - Testing checklist
  - Known limitations documented

- [ ] 🔧 **Integrate Council page into app** (3 small file edits required)
  - Add `/council` route to routes.cljs
  - Require council.page in core.cljs
  - Add Council nav link to app_layout.cljs
  - See COUNCIL_UI_INTEGRATION.md for exact changes

- [ ] Stake on minds modal/form
- [ ] User's current stakes display (real data)
- [ ] Move stake between seats functionality
- [ ] Philosophy display (fetch from IPFS)
- [ ] DNT approval flow UI

### ARI Activity Page
- [ ] Proposal list with filtering by status
- [ ] Proposal detail with council analysis
- [ ] Veto progress bar
- [ ] Cast veto interface

### Power Plant Page
- [ ] Treasury balance display
- [ ] Active bounties list
- [ ] Active grants list
- [ ] Historical transactions

## Phase 4: Integration (Priority: LOW)

- [ ] End-to-end testing on local testnet
- [ ] Testnet deployment (Sepolia/Goerli)
- [ ] ARI integration testing with mock ARI
- [ ] Documentation updates
- [ ] Mainnet deployment preparation

## Key Learnings

*Update this section as you discover important patterns or gotchas*

- Existing StakeBank.sol uses checkpoint pattern for historical queries
- Registry.sol has event emission pattern to follow
- Forwarder pattern used for gas-efficient deployment
- DSAuth used for access control
- Solidity 0.4.24 doesn't support `type(uint256).max` - use `uint256(-1)` instead
- Solidity 0.4.24 doesn't have `abi.decode()` - must use explicit parameters
- Project uses MiniMeToken, not standard ERC20
- Database schema follows namespaced keyword pattern (e.g. :council-seat/index)
- Helper functions generated via create-insert-fn, create-update-fn, create-get-fn
- Event syncer uses dispatcher pattern with block-timestamp caching
- Upsert operations in syncer ensure idempotent event handling
- Event handlers receive both :args and :block-number in event map
- GraphQL resolvers use paged-query helper for pagination
- GraphQL field resolvers compute derived values (percentages, remaining amounts)
- GraphQL enum resolvers convert database integers to GraphQL enum strings
- ARI API uses web3 signature recovery for authentication
- ARI API provides both read (council state) and write (create proposal) endpoints
- Mount state management pattern used for lifecycle hooks
- **UI follows Re-frame pattern:** Subscribe to data, dispatch events for actions
- **Page components use defmethod:** `(defmethod page :route/name [])`
- **GraphQL queries inline in page:** Define query vectors directly in components
- **Transaction buttons use tx-button:** From district.ui.component.tx-button
- **Notifications automatic:** tx-events handles success/error notifications
- **Contract helpers in separate namespace:** Keep event handlers isolated

## Blockers

*Document any blockers here*

- None currently!

## Notes

- CRITICAL: Seat 8 (ARI/Stranger) must be immutable - ✅ VERIFIED in Council.sol:69 and isImmutable flag
- Follow existing code patterns in contracts/ directory
- Use bb (babashka) for ClojureScript compilation
- Tests use truffle test framework
- Migration script deployment order: CouncilStakeBank → ARIOracle → PowerPlant → Council
- Database tables created in start function with :if-not-exists flag
- Event handler naming convention: contract-key/event-name-event (e.g. :council/seat-created-event)
- GraphQL resolver naming convention: type-query-resolver, type->field-resolver
- GraphQL Query map extended via merge, not replacement
- ARI API endpoints should be mounted in core.cljs or separate HTTP server
- ARI address must be configured via environment variable in production
- **UI integration requires only 3 file edits** - see COUNCIL_UI_INTEGRATION.md
- Council page component complete and ready to use
- CSS provided in integration guide - add to resources/public/css/
