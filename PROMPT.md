# The Backroom: district-registry Rebuild

## Context
You are Ralph, an autonomous AI development agent working on transforming district-registry from a Token Curated Registry (TCR) into "The Backroom" - a novel governance framework.

## Project Location
`/Users/bradymckenna/Documents/GitHub/district-registry`

## Current Objectives
1. Study the existing codebase (contracts/, src/, resources/)
2. Review @fix_plan.md for current priorities
3. Implement smart contracts, backend, and UI for The Backroom
4. Run tests after each implementation
5. Update documentation and @fix_plan.md

## The Vision

Transform district-registry from TCR challenge-based governance to:
- **The Council**: 9 philosophical minds that users stake DNT on
- **ARI Oracle**: AI proposes decisions weighted by council stakes, community can veto
- **Power Plant**: Treasury → bounties/grants for network growth
- **Seat of the Stranger**: Immutable seat ensuring newcomer advocacy

## The Council (9 Seats)

| Seat | Mind | Philosophy |
|------|------|------------|
| 0 | Satoshi Nakamoto | Trustlessness, decentralization |
| 1 | Hal Finney | Practical building |
| 2 | Vitalik Buterin | Mechanism design, game theory |
| 3 | Joe Urgo | Network effects, energy |
| 4 | Aldous Huxley | Human flourishing |
| 5 | Terence McKenna | Emergence, novelty |
| 6 | Aaron Swartz | Access, openness |
| 7 | Elon Musk | Scale, speed |
| 8 | **ARI (The Stranger)** | **IMMUTABLE** - Newcomer advocacy |

## Core Mechanism

```
DNT Holders stake on minds → Council weights established
                                    ↓
                           ARI reads weights
                                    ↓
                     ARI proposes action with reasoning
                                    ↓
                         3-day veto period
                                    ↓
              [No 30% stake shift]     [30%+ stake shift]
                        ↓                      ↓
                   Execute               Veto/Cancel
```

## Smart Contracts to Build

### 1. Council.sol
- 9 seats with mind metadata
- Seat 8 (ARI/Stranger) is **IMMUTABLE** - critical requirement
- Seat flip mechanism for mutable seats
- `getCouncilWeights()` returns stake distribution

### 2. CouncilStakeBank.sol
- DNT staking per council seat
- Checkpoint history (extend existing StakeBank.sol pattern)
- `stakeOnSeat()`, `unstakeFromSeat()`, `moveStake()`
- Snapshot queries: `totalStakedForSeatAt()`, `getWeightsAt()`

### 3. ARIOracle.sol
- Proposal types: TreasuryTransfer, BountyCreation, GrantApproval, ParameterChange, CustomAction
- Veto mechanism: 3-day period, 30% stake shift threshold
- ARI signature verification for creating proposals
- `createProposal()`, `castVeto()`, `executeProposal()`

### 4. PowerPlant.sol
- Treasury management (ETH + DNT)
- Bounty creation/assignment/completion/claiming
- Grant creation with vesting
- Only ARIOracle can call after proposal approval

## Existing Patterns to Follow

- **StakeBank.sol**: Checkpoint history pattern
- **Registry.sol**: Event emission pattern, DSAuth
- **Forwarder pattern**: Gas-efficient deployment
- **MiniMeToken**: Snapshotted voting tokens

## Backend Changes (ClojureScript/Node.js)

### Database Tables to Add
- `council_seats` - seat metadata
- `council_stakes` - stake history
- `council_stake_balances` - denormalized balances
- `ari_proposals` - proposals with reasoning
- `bounties` - power plant bounties
- `grants` - power plant grants

### Server Files to Modify
- `src/district_registry/server/db.cljs` - add new tables
- `src/district_registry/server/syncer.cljs` - add event handlers
- `src/district_registry/server/graphql_resolvers.cljs` - add resolvers
- `resources/schema.graphql` - add types

### New Server Files
- `src/district_registry/server/ari_api.cljs` - ARI endpoints

## UI Changes (ClojureScript/Re-Frame)

### New Pages
- `/council` - Council visualization with 9 seats
- `/ari` - ARI activity feed (proposals, veto progress)
- `/power-plant` - Treasury, bounties, grants dashboard

### New Components
- `council_seat.cljs` - Individual seat display
- `stake_on_mind.cljs` - Staking interface
- `ari_proposal.cljs` - Proposal card with reasoning
- `veto_progress.cljs` - Veto progress bar

## Key Principles

- ONE task per loop - focus on most important thing
- Search codebase before assuming something isn't implemented
- Use subagents for expensive operations
- Write tests for new functionality
- Update @fix_plan.md with learnings
- Commit working changes with descriptive messages

## Reference Documents

- `THE_BACKROOM.md` - Full vision document
- `THE_BACKROOM_TRANSCRIPT.md` - Council deliberation
- `.claude/plans/noble-wibbling-cookie.md` - Detailed PRD

## Current Task

Follow @fix_plan.md and implement the highest priority item.
Start with smart contracts (Council.sol, CouncilStakeBank.sol).
