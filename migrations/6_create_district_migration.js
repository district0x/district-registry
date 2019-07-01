const {copy, smartContractsTemplate, encodeContractEDN, linkBytecode, readSmartContractsFile, writeSmartContracts, getSmartContractAddress, setSmartContractAddress, copyContract} = require("./utils.js");
const fs = require("fs");
const {env, smartContractsPath, parameters} = require("../truffle.js");
const {registryPlaceholder, dntPlaceholder, forwarder1TargetPlaceholder, forwarder2TargetPlaceholder, minimeTokenFactoryPlaceholder, kitDistrictPlaceholder, zeroAddress, dsGuardANY, aragonENSNode} = require("./constants.js");

function requireContract(contractName, contractCopyName) {
  return artifacts.require(copyContract(contractName, contractCopyName));
}

let DNT = requireContract("District0xNetworkToken");
let DistrictFactory = requireContract("DistrictFactory");

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

  var dnt = await DNT.at(getSmartContractAddress(smartContracts, ":DNT"));
  var districtFactory = await DistrictFactory.at(getSmartContractAddress(smartContracts, ":district-factory"));

  var metaHash = web3.utils.toHex("Qmdpe5HCmjieUaSYoedQYZkTRu7aax8719hLAFyMQhcJhD");
  var dntWeight = 1000000;
  var aragonId = "nbtest1";
  var deposit = web3.utils.toHex(parameters.districtRegistryDb.deposit);

  var extraData = districtFactory.contract.methods.createDistrict(address, metaHash, dntWeight, aragonId).encodeABI();
  var amount = await dnt.contract.methods.approveAndCall(districtFactory.address, deposit, extraData)
                        .estimateGas({gas: 7500000});

  console.log(amount);

};
