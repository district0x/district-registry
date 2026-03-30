# The Backroom - Implementation Fix Plan (Test Update)

## Phase 1: Smart Contracts (Priority: HIGH)

### Week 1: Core Contracts
- [x] **Council.sol** - 9 seats, immutable Stranger seat, seat flip mechanism ✅ COMPLETE
- [x] **CouncilStakeBank.sol** - DNT staking per seat with checkpoints ✅ COMPLETE

### Week 2: Oracle & Treasury Contracts
- [x] **ARIOracle.sol** - Proposal/veto mechanism ✅ COMPLETE
- [x] **PowerPlant.sol** - Treasury and bounty/grant distribution ✅ COMPLETE

### Week 3: Contract Testing & Deployment
- [x] ✅ **All contracts compile successfully** (Solidity 0.4.24 compatible)
- [x] ✅ **Migration script created** - migrations/10_backroom_migration.js
- [x] ✅ **Unit test files created** (Ralph - January 21, 2026)
  - council_tests.cljs - 20 test cases for Council + StakeBank
  - ari_oracle_tests.cljs - 30 test cases for Oracle
  - power_plant_tests.cljs - 25 test cases for PowerPlant
- [ ] 🔧 **Create contract helper files** (Required before running tests)
  - src/district_registry/server/contract/council.cljs
  - src/district_registry/server/contract/council_stake_bank.cljs
  - src/district_registry/server/contract/ari_oracle.cljs
  - src/district_registry/server/contract/power_plant.cljs
- [ ] **Run and debug tests** on local testnet
- [ ] **Write integration tests** between contracts

## Phase 2: Backend (Priority: MEDIUM) - ✅ 100% COMPLETE

### Database
- [x] ✅ **All Backroom tables added to db.cljs**

### Event Syncer
- [x] ✅ **All Backroom event handlers added to syncer.cljs**

### GraphQL
- [x] ✅ **All Backroom GraphQL schema and resolvers complete**

### ARI API
- [x] ✅ **Complete ARI API implementation in ari_api.cljs**

## Phase 3: UI (Priority: MEDIUM) - ⏳ 15% COMPLETE

### Council Page
- [x] ✅ **Council page component created** - src/district_registry/ui/council/page.cljs
- [x] ✅ **Council contract helpers created** - src/district_registry/ui/contract/council.cljs
- [x] ✅ **Integration guide created** - COUNCIL_UI_INTEGRATION.md
- [ ] 🔧 **Integrate Council page into app** (3 file edits required)
  - Add `/council` route to routes.cljs
  - Require council.page in core.cljs
  - Add Council nav link to app_layout.cljs
  - See COUNCIL_UI_INTEGRATION.md for exact changes
- [ ] **Build Stake Modal component**
- [ ] **Add user stake display with real data**
- [ ] **Add philosophy display from IPFS**
- [ ] **Create ARI Activity page**
- [ ] **Create Power Plant page**

## Phase 4: Integration (Priority: LOW) - ⏳ 10% COMPLETE

### Testing
- [x] ✅ **Test files created** (Ralph - January 21, 2026)
  - Comprehensive test suites for all contracts
  - Test patterns documented
  - Contract helper specifications
  - See TEST_IMPLEMENTATION_GUIDE.md
- [ ] **Create contract helper files** (4 files)
- [ ] **Deploy to local testnet and run tests**
- [ ] **Achieve >80% test coverage**
- [ ] **Write integration tests**

### Deployment
- [ ] **End-to-end testing on local testnet**
- [ ] **Testnet deployment (Sepolia/Goerli)**
- [ ] **ARI integration testing with mock ARI**
- [ ] **Security audit**
- [ ] **Mainnet deployment preparation**

## New Files Created (Ralph - January 21, 2026)

### Test Files
1. `test/district_registry/tests/smart_contracts/council_tests.cljs` - 350 lines, 20 test cases
2. `test/district_registry/tests/smart_contracts/ari_oracle_tests.cljs` - 400 lines, 30 test cases
3. `test/district_registry/tests/smart_contracts/power_plant_tests.cljs` - 425 lines, 25 test cases

### Documentation
4. `TEST_IMPLEMENTATION_GUIDE.md` - Comprehensive test guide (800+ lines)
5. `@fix_plan_TEST_UPDATE.md` - This file

**Total Test Code:** ~1,175 lines across 3 test files
**Total Documentation:** ~800 lines of test guidance

## Key Learnings - Testing

### Test Patterns
- ClojureScript tests use `async` with `core.async` for async operations
- Use `tx-error?` helper to test transaction failures
- Tests follow pattern: setup → action → assertion → cleanup
- Integration tests require multi-contract state management

### Critical Test Cases
1. **Seat 8 Immutability** - MUST verify ARI seat cannot be changed
2. **30% Veto Threshold** - MUST verify exact threshold detection
3. **3-Day Veto Period** - MUST verify timing is correct
4. **Only ARI Can Propose** - MUST verify access control
5. **Only Oracle Calls PowerPlant** - MUST verify authorization
6. **Linear Grant Vesting** - MUST verify correct calculations
7. **Bounty Lifecycle** - MUST verify state transitions

### Test Implementation Workflow
1. Create contract helper files (4 files × 30 min = 2 hours)
2. Deploy contracts to local testnet (1 hour)
3. Run initial test suite (2 hours)
4. Debug and fix failing tests (4-8 hours)
5. Add integration tests (4 hours)
6. Achieve >80% coverage (4 hours)

**Estimated Total Time:** 17-21 hours to complete testing phase

## Blockers

*Document any blockers here*

- None currently! Test files are ready for implementation.

## Notes

### Testing
- Test files follow existing patterns from registry_tests.cljs
- Contract helpers needed before tests can run (see TEST_IMPLEMENTATION_GUIDE.md)
- Tests use Ganache for local blockchain
- Time-dependent tests (veto period) require Ganache time control
- Integration tests require careful state management across contracts

### Next Immediate Actions
**Option 1:** Create contract helper files and run tests (12-20 hours)
**Option 2:** Integrate Council UI into app (3 file edits, 30 minutes)
**Option 3:** Build Stake Modal component (2-3 hours)

**Recommendation:** Option 2 (Council UI) for quick visible progress, then Option 1 (tests) for quality assurance.
