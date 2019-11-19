const {readSmartContractsFile, getSmartContractAddress, copyContract, kitDistrictAppsToNum} = require("./utils.js");
const fs = require("fs");
const {env, smartContractsPath, parameters} = require("../truffle.js");
const {registryPlaceholder, dntPlaceholder, forwarder1TargetPlaceholder, forwarder2TargetPlaceholder, minimeTokenFactoryPlaceholder, kitDistrictPlaceholder, zeroAddress, dsGuardANY, aragonENSNode} = require("./constants.js");

function requireContract(contractName, contractCopyName) {
  return artifacts.require(copyContract(contractName, contractCopyName));
}

const KitDistrict = requireContract("KitDistrict");
let Migrations = requireContract("Migrations");

/**
 * This migration does dry run to create a new district and see gas costs
 *
 * Usage:
 * truffle migrate --network ganache/parity --reset --f 4 --to 4
 */


module.exports = async function(deployer, network, accounts) {
  const address = accounts[0];
  const gas = 6e6;
  const opts = {gas: gas, from: address};

  await deployer;
  console.log("@@@ using Web3 version:", web3.version);
  console.log("@@@ using address", address);

  var smartContracts = readSmartContractsFile(smartContractsPath);
  var kitDistrict = await KitDistrict.at(getSmartContractAddress(smartContracts, ":kit-district"));
  var includeApps = kitDistrictAppsToNum(["voting", "vault", "finance"]);
  var isIncluded = ["voting", "vault", "finance"].map((app) => parameters.KitDistrict.includeApps.includes(app));

  console.log("Setting KitDistrict included apps: ", includeApps, isIncluded);
  await kitDistrict.setAppsIncluded(includeApps, isIncluded, Object.assign({}, opts, {gas: 2e6}));

  console.log("Successfully changed includedApps in KitDistrict");

  console.log("Setting migration completed...");
  const migrationsAddress = getSmartContractAddress(smartContracts, ":migrations");
  const migrations = await Migrations.at(migrationsAddress);
  await migrations.setCompleted(7, Object.assign(opts, {gas: 100000}));
};
