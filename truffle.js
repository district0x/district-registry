'use strict';
const DISTRICT_REGISTRY_ENV = process.env.DISTRICT_REGISTRY_ENV || "dev";


const smartContractsPaths = {
  "dev" : '/src/district_registry/shared/smart_contracts_dev.cljs',
  "qa" : '/src/district_registry/shared/smart_contracts_qa.cljs',
  "prod" :'/src/district_registry/shared/smart_contracts_prod.cljs'
};


let parameters = {
  "dev": {
    districtRegistryDb : {
      challengePeriodDuration : 0,
      commitPeriodDuration : 60, // seconds
      revealPeriodDuration : 60, // seconds
      deposit : 1e18, // 1e18 = 1 DNT
      challengeDispensation : 50, // percent
      voteQuorum : 50, // percent
    },

    paramChangeRegistryDb : {
      challengePeriodDuration : 600, // seconds
      commitPeriodDuration : 600, // seconds
      revealPeriodDuration : 600, // seconds
      deposit : 1e18, // 1e18 = 1 DNT
      challengeDispensation : 50, // percent
      voteQuorum : 50 // percent
    }
  },
  "qa" : {
    ENS: "0x98df287b6c145399aaa709692c8d308357bc085d",
    DAOFactory: "0x2298d27a9b847c681d2b2c2828ab9d79013f5f1d",
    FIFSResolvingRegistrar: "0x3665e7bfd4d3254ae7796779800f5b603c43c60d",
    districtRegistryDb : {
      challengePeriodDuration : 0,
      commitPeriodDuration : 600, // seconds
      revealPeriodDuration : 600, // seconds
      deposit : 1e18, // 1e18 = 1 DNT
      challengeDispensation : 50, // percent
      voteQuorum : 50, // percent
    },

    paramChangeRegistryDb : {
      challengePeriodDuration : 600, // seconds
      commitPeriodDuration : 600, // seconds
      revealPeriodDuration : 600, // seconds
      deposit : 1e18, // 1e18 = 1 DNT
      challengeDispensation : 50, // percent
      voteQuorum : 50 // percent
    }
  },
  "prod" : {
    DNT: "0x0abdace70d3790235af448c88547603b945604ea",
    MiniMeTokenFactory: "0xa7dd95d9978dde794eae5233889f1ffebcdc9914",
    ENS: "0x314159265dd8dbb310642f98f50c066173c1259b",
    DAOFactory: "0x595b34c93aa2c2ba0a38daeede629a0dfbdcc559",
    FIFSResolvingRegistrar: "0x546aa2eae2514494eeadb7bbb35243348983c59d",
    districtRegistryDb : {
      challengePeriodDuration : 0,
      commitPeriodDuration : 600, // seconds
      revealPeriodDuration : 600, // seconds
      deposit : 1e18, // 1e18 = 1 DNT
      challengeDispensation : 50, // percent
      voteQuorum : 50, // percent
    },

    paramChangeRegistryDb : {
      challengePeriodDuration : 600, // seconds
      commitPeriodDuration : 600, // seconds
      revealPeriodDuration : 600, // seconds
      deposit : 1e18, // 1e18 = 1 DNT
      challengeDispensation : 50, // percent
      voteQuorum : 50 // percent
    }
  }
};

module.exports = {
  env: DISTRICT_REGISTRY_ENV,
  smart_contracts_path: __dirname + smartContractsPaths[DISTRICT_REGISTRY_ENV],
  contracts_build_directory: __dirname + '/resources/public/contracts/build/',
  parameters : parameters[DISTRICT_REGISTRY_ENV],

  networks: {
    ganache: {
      host: 'localhost',
      port: 8549,
      gas: 6e6, // gas limit
      gasPrice: 6e9, // 20 gwei, default for ganache
      network_id: '*',
      skipDryRun: true
    },

    parity: {
      host: 'localhost',
      port: 8545,
      gas: 6e6,
      gasPrice: 6e9, // 6 gwei
      network_id: '*',
      skipDryRun: true
    }
  },

  compilers: {
    solc: {
      version: "0.4.24",
    }
  }
}
