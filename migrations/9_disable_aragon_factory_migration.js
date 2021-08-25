const {copy, readSmartContractsFile, getSmartContractAddress, copyContract} = require("./utils.js");
const fs = require("fs");
const {env, smartContractsPath, parameters} = require("../truffle.js");

function requireContract(contractName, contractCopyName) {
  return artifacts.require(copyContract(contractName, contractCopyName));
}

let DistrictRegistry = requireContract("Registry");

/**
 * This migration disables old DistrictFactory contract
 *
 * Usage:
 * truffle migrate --network ganache/parity --reset --f 9 --to 9
 */


async function disable_DistrictFactory(deployer, districtRegistryFwdAddr, oldDistrictFactoryAddr, opts) {
  const districtRegistryForwarderInstance = await DistrictRegistry.at(districtRegistryFwdAddr);

  console.log("Disabling old DistrictFactory in DistrictRegistryForwarder");
  await districtRegistryForwarderInstance.setFactory(oldDistrictFactoryAddr, false, Object.assign({}, opts, {gas: 0.1e6}));
}


module.exports = async function(deployer, network, accounts) {
  const address = accounts[0];
  const gas = 6e6;
  const opts = {gas: gas, from: address};

  // hardcoded old aragon-based disctrict factory address in mainnet
  const oldDistrictFactoryAddr = "0xb764e4d3693e8710078231eef102d9a9fa210718";

  console.log("Disable District Factory Migration Started...");

  await deployer;
  console.log("@@@ using Web3 version:", web3.version);
  console.log("@@@ using address", address);

  var smartContracts = readSmartContractsFile(smartContractsPath);

  var districtRegistryFwdAddr = getSmartContractAddress(smartContracts, ":district-registry-fwd");
  
  await disable_DistrictFactory(deployer, districtRegistryFwdAddr, oldDistrictFactoryAddr, opts);

  console.log("District factory successfully disabled!");

};
