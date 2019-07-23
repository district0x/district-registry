'use strict';
const HDWalletProvider = require("truffle-hdwallet-provider");
require('dotenv').config()  // Store environment-specific variable from '.env' to process.env

const DISTRICT_REGISTRY_ENV = process.env.DISTRICT_REGISTRY_ENV || "dev";

const smartContractsPaths = {
  "dev" : '/src/district_registry/shared/smart_contracts_dev.cljs',
  "qa" : '/src/district_registry/shared/smart_contracts_qa.cljs',
  "prod" :'/src/district_registry/shared/smart_contracts_prod.cljs'
};


let parameters = {
  "dev": {
    KitDistrict : {includeApps: ["voting", "vault", "finance"]},
//    KitDistrict : {includeApps: ["voting", "vault"]},
//    KitDistrict : {includeApps: ["finance"]},
//    KitDistrict : {includeApps: ["voting"]},
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
    DNT: "0xe450dcde6c059339a35eec0facbe62751cca6e8a",
    ENS: "0x98df287b6c145399aaa709692c8d308357bc085d",
    DAOFactory: "0x2298d27a9b847c681d2b2c2828ab9d79013f5f1d",
    FIFSResolvingRegistrar: "0x3665e7bfd4d3254ae7796779800f5b603c43c60d",
    KitDistrict : {includeApps: ["voting"]},
    District0xEmails: "0x3e6e8cdac0abab167644811b331594a500e8df7f",
    districtRegistryDb : {
      challengePeriodDuration : 0,
      commitPeriodDuration : 200, // seconds
      revealPeriodDuration : 200, // seconds
      deposit : 1e18, // 1e18 = 1 DNT
      challengeDispensation : 50, // percent
      voteQuorum : 50, // percent
    },

    paramChangeRegistryDb : {
      challengePeriodDuration : 200, // seconds
      commitPeriodDuration : 200, // seconds
      revealPeriodDuration : 200, // seconds
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
    KitDistrict : {includeApps: ["voting", "vault", "finance"]},
    District0xEmails: "0x5065ef0724b76421aeaafa87ba752d6c5d5499b5",
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
  smartContractsPath: __dirname + smartContractsPaths[DISTRICT_REGISTRY_ENV],
  contracts_build_directory: __dirname + '/resources/public/contracts/build/',
  parameters : parameters[DISTRICT_REGISTRY_ENV],

  networks: {
    ganache: {
      host: 'localhost',
      port: 8549,
      gas: 6e6, // gas limit
      gasPrice: 4e9,
      network_id: '*',
      skipDryRun: true
    },

    parity: {
      host: 'localhost',
      port: 8545,
      gas: 6e6,
      gasPrice: 3e9, // 6 gwei
      network_id: '*',
      skipDryRun: true
    },
    "infura-rinkeby": {
      provider: () => new HDWalletProvider(process.env.MNENOMIC, "https://rinkeby.infura.io/v3/" + process.env.INFURA_API_KEY),
      network_id: 4,
      gas: 6e6,
      gasPrice: 6e9,
      skipDryRun: true
    },
    "infura-mainnet": {
      provider: () => new HDWalletProvider(process.env.MNENOMIC, "https://mainnet.infura.io/v3/" + process.env.INFURA_API_KEY),
      network_id: 1,
      gas: 6e6,
      gasPrice: 4e9,
      skipDryRun: true
    }
  },

  compilers: {
    solc: {
      version: "0.4.24",
    }
  }
}
