'use strict';
const BN = require('bn.js');
const HDWalletProvider = require("@truffle/hdwallet-provider");
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
    // KitDistrict : {includeApps: ["voting", "vault"]},
    // KitDistrict : {includeApps: ["finance"]},
    // KitDistrict : {includeApps: ["voting"]},
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
    ENS: "0x00000000000C2E074eC69A0dFb2997BA6C7d2e1e",
    districtRegistryDb : {
      challengePeriodDuration : 0,
      commitPeriodDuration : 200, // seconds
      revealPeriodDuration : 200, // s econds
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
    // MiniMeTokenFactory: "0xa7dd95d9978dde794eae5233889f1ffebcdc9914",
    ENS: "0x314159265dd8dbb310642f98f50c066173c1259b",
    DAOFactory: "0xb9da44c051c6cc9e04b7e0f95e95d69c6a6d8031",
    FIFSResolvingRegistrar: "0x546aa2eae2514494eeadb7bbb35243348983c59d",
    KitDistrict : {includeApps: ["voting", "vault", "finance"]},
    District0xEmails: "0x5065ef0724b76421aeaafa87ba752d6c5d5499b5",
    districtRegistryDb : {
      challengePeriodDuration : 0,
      commitPeriodDuration : 259200, // seconds
      revealPeriodDuration : 259200, // seconds
      deposit : "10000000000000000000000", // 1e18 = 1 DNT
      challengeDispensation : 50, // percent
    },
    paramChangeRegistryDb : {
      challengePeriodDuration : 259200, // seconds
      commitPeriodDuration : 259200, // seconds
      revealPeriodDuration : 259200, // seconds
      deposit : "1000000000000000000000000000", // 1e18 = 1 DNT
      challengeDispensation : 50, // percent
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
      port: 8545,
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
    "infura-ropsten": {
      provider: () => new HDWalletProvider(process.env.MNENOMIC || process.env.ROPSTEN_PRIV_KEY, "https://ropsten.infura.io/v3/" + process.env.INFURA_API_KEY),
      network_id: 3,
      gas: 6e6,
      gasPrice: 5e9,
      skipDryRun: true
    },
    "infura-mainnet": {
      provider: () => new HDWalletProvider(process.env.MNENOMIC || process.env.MAINNET_PRIV_KEY, "https://mainnet.infura.io/v3/" + process.env.INFURA_API_KEY),
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
