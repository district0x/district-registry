# District Registry

A community-curated registry of marketplaces on the district0x Network.

## Development
Compile contracts (assumes you have `solc` installed):
```bash
lein solc
```
Auto compile contracts on changes:
```bash
lein solc auto
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

