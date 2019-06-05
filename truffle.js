'use strict';
const DISTRICT_REGISTRY_ENV = process.env.DISTRICT_REGISTRY_ENV || "dev";


const smartContractsPaths = {
  "dev" : '/src/district_registry/shared/smart_contracts_dev.cljs',
  "qa" : '/src/district_registry/shared/smart_contracts_qa.cljs',
  "prod" :'/src/district_registry/shared/smart_contracts_prod.cljs'
};


let parameters = {
  "qa" : {
    districtRegistryDb : {
      challengePeriodDuration : 600, // seconds (10 minutes)
      commitPeriodDuration : 600, // seconds
      revealPeriodDuration : 600, // seconds
      deposit : 1e18, // 1e18 = 1 DANK
      challengeDispensation : 50, // percent
      voteQuorum : 50, // percent
    },

    paramChangeRegistryDb : {
      challengePeriodDuration : 600, // seconds
      commitPeriodDuration : 600, // seconds
      revealPeriodDuration : 600, // seconds
      deposit : 1e18, // 1e18 = 1 DANK
      challengeDispensation : 50, // percent
      voteQuorum : 50 // percent
    },
  },
  "prod" : {
    districtRegistryDb : {
      challengePeriodDuration : 600, // seconds (10 minutes)
      commitPeriodDuration : 600, // seconds
      revealPeriodDuration : 600, // seconds
      deposit : 1e18, // 1e18 = 1 DANK
      challengeDispensation : 50, // percent
      voteQuorum : 50, // percent
    },

    paramChangeRegistryDb : {
      challengePeriodDuration : 600, // seconds
      commitPeriodDuration : 600, // seconds
      revealPeriodDuration : 600, // seconds
      deposit : 1e18, // 1e18 = 1 DANK
      challengeDispensation : 50, // percent
      voteQuorum : 50 // percent
    },
  }
};

parameters.dev = parameters.qa;


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
      network_id: '*'
    },

    parity: {
      host: 'localhost',
      port: 8545,
      gas: 6e6,
      gasPrice: 6e9, // 6 gwei
      network_id: '*'
    }
  },

  compilers: {
    solc: {
      version: "0.4.25",
    }
  }
}
