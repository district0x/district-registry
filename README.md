# District Registry

[![CircleCI](https://circleci.com/gh/district0x/district-registry.svg?style=svg)](https://circleci.com/gh/district0x/district-registry)

A community-curated registry of marketplaces on the district0x Network.

## Development
Compile contracts (assumes you have `truffle` installed):
```bash
truffle migrate --f 2 --to 2;
```

Start server:
```bash
ganache-cli -p 8549
lein repl
(start-server!)
node dev-server/district-registry.js
# Redeploy all smart contracts
(district-registry.server.dev/redeploy)
```

Start UI:
```bash
lein repl
(start-ui!)
# go to http://localhost:4177/
```

Start tests:
```bash
ganache-cli -p 8549
lein test-dev
```

## Using Makefile

The Dockerfile and Makefiles provide convenient way to automate routine procedures.

For example to build and push images try the following:
```sh
make build-push-images  BUILD_ENV={prod|qa|dev} DOCKER_REPO={local|district0x|aws_ecr_url}
```

## Smart Contract Architecture

Contract architecture mostly follows that of [Meme Factory](https://github.com/district0x/memefactory) as District Registry was initially forked from Meme Factory. The primary differences are related to minimizing contract sizes. Meme Factory uses a `RegistryEntryLib` library to provide most of the functionality for registry entries. However, District Registry entries may need to accommodate multiple challenges and `District` (the primary registry entry) has substantial additional functionality, so District Registry takes a different approach. The primary differences are as follows:

* Instead of integrating challenges into registry entries, `Challenge` and `ChallengeFactory` are separate contracts which are used inside of `RegistryEntry`.
* Functionality to stake district is split into separate `StakeBank` and `StakeBankFactory` contracts which are used inside of `District`.
* Functionality for bonding curve math (power functions) is split into separate `Power` and `PowerFactory` contracts that are used in `StakeBank`.
