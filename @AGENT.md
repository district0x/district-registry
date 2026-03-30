# Agent Build Instructions - district-registry

## Project Overview

district-registry is a ClojureScript project with:
- **Smart Contracts**: Solidity 0.4.24, deployed via Truffle
- **Server**: ClojureScript compiled to Node.js, GraphQL API
- **UI**: ClojureScript with Re-Frame (Redux-like) and Reagent (React wrapper)

## Prerequisites

- Node.js >= 20.18.1 (check `.tool-versions`)
- Java JDK >= 18 (for Clojure)
- Babashka (`bb`) - ClojureScript task runner
- PostgreSQL (for server database)
- Ganache (for local Ethereum testnet)

## Project Setup

```bash
# Install Node dependencies (root, ui, server)
yarn install
cd ui && yarn install && cd ..
cd server && yarn install && cd ..

# Create database
psql -c "CREATE DATABASE district_registry_dev;"
```

## Running Development Environment

```bash
# Start local Ethereum testnet
bb testnet-dev

# Deploy smart contracts (in another terminal)
npx truffle migrate --network ganache --reset

# Start server compilation (watches for changes)
bb watch-server

# Start server (in another terminal)
bb run-server

# Start UI compilation (watches for changes)
bb watch-ui
```

## Running Tests

```bash
# Smart contract tests
npx truffle test

# Specific contract test
npx truffle test test/council_test.js

# Server tests (if available)
bb run-server-tests
```

## Build Commands

```bash
# Compile smart contracts
npx truffle compile

# Compile server for production
DISTRICT_REGISTRY_ENV=prod bb compile-server

# Compile UI for production
DISTRICT_REGISTRY_ENV=prod bb compile-ui

# Compile CSS
bb compile-css
```

## Key Directories

```
contracts/           # Solidity smart contracts
migrations/          # Truffle deployment scripts
src/district_registry/
  server/           # ClojureScript server code
    db.cljs         # Database schema and queries
    syncer.cljs     # Blockchain event indexer
    graphql_resolvers.cljs
  ui/               # ClojureScript UI code
    components/     # Reagent components
    events.cljs     # Re-Frame events
    subs.cljs       # Re-Frame subscriptions
resources/
  schema.graphql    # GraphQL schema
shared/             # Shared ClojureScript code
```

## Smart Contract Patterns

- **Forwarder Pattern**: Contracts deployed as forwarders pointing to single implementation
- **DSAuth**: Access control (check `auth/DSAuth.sol`)
- **EternalDb**: Key-value storage (check `db/EternalDb.sol`)
- **Checkpoint History**: For historical queries (see `StakeBank.sol`)

## Adding New Smart Contracts

1. Create contract in `contracts/`
2. Import necessary dependencies (DSAuth, SafeMath, etc.)
3. Follow existing patterns (forwarder, events, modifiers)
4. Create migration in `migrations/`
5. Add tests in `test/`

## Adding New Database Tables

1. Add table definition in `src/district_registry/server/db.cljs`
2. Add sync handlers in `src/district_registry/server/syncer.cljs`
3. Add GraphQL types in `resources/schema.graphql`
4. Add resolvers in `src/district_registry/server/graphql_resolvers.cljs`

## Adding New UI Pages

1. Create page component in `src/district_registry/ui/<page>/page.cljs`
2. Add route in routing configuration
3. Add Re-Frame events in `events.cljs`
4. Add Re-Frame subscriptions in `subs.cljs`

## Key Learnings

- Use `bb` (babashka) for all build tasks
- Server must be restarted manually after ClojureScript changes
- Contracts use MutableForwarder for upgradability
- Events are indexed by syncer into PostgreSQL for fast queries

## Environment Variables

```bash
DISTRICT_REGISTRY_ENV=dev|qa|prod
ETHLANCE_CONFIG_PATH=/path/to/config.edn
UI_CONFIG_PATH=/path/to/ui-config.edn
```

## Feature Completion Checklist

Before marking ANY feature as complete:

- [ ] Smart contract compiles without errors
- [ ] Contract tests pass (`npx truffle test`)
- [ ] Server compiles (`bb watch-server` no errors)
- [ ] UI compiles (`bb watch-ui` no errors)
- [ ] Changes committed with conventional commits
- [ ] @fix_plan.md updated
- [ ] Documentation updated if needed
