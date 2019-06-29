const {copy, smartContractsTemplate, encodeContractEDN, linkBytecode, readSmartContractsFile, writeSmartContracts, getSmartContractAddress, setSmartContractAddress, copyContract} = require("./utils.js");
const fs = require("fs");
const {env, smartContractsPath, parameters} = require("../truffle.js");
const {registryPlaceholder, dntPlaceholder, forwarder1TargetPlaceholder, forwarder2TargetPlaceholder, minimeTokenFactoryPlaceholder, kitDistrictPlaceholder, zeroAddress, dsGuardANY, aragonENSNode} = require("./constants.js");

function requireContract(contractName, contractCopyName) {
  return artifacts.require(copyContract(contractName, contractCopyName));
}

let KitDistrict = requireContract("KitDistrict", "KitDistrict");

/**
 * This migration deploys and swaps KitDistrict contract and its dependencies
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

  console.log("arr:", await kitDistrict.aragonID());
  console.log("ens:", await kitDistrict.ens());
  console.log("fac:", await kitDistrict.fac());


};
