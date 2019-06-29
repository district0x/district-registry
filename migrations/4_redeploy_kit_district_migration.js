const {copy, smartContractsTemplate, encodeContractEDN, linkBytecode, readSmartContractsFile, writeSmartContracts, getSmartContractAddress, setSmartContractAddress, copyContract} = require("./utils.js");
const fs = require("fs");
const {env, smartContractsPath, parameters} = require("../truffle.js");
const {registryPlaceholder, dntPlaceholder, forwarder1TargetPlaceholder, forwarder2TargetPlaceholder, minimeTokenFactoryPlaceholder, kitDistrictPlaceholder, zeroAddress, dsGuardANY, aragonENSNode} = require("./constants.js");

function requireContract(contractName, contractCopyName) {
  return artifacts.require(copyContract(contractName, contractCopyName));
}

let DistrictRegistry = requireContract("Registry", "DistrictRegistry");
let DistrictFactory = requireContract("DistrictFactory");
let District = requireContract("District");
let KitDistrict = requireContract("KitDistrict", "KitDistrict");

/**
 * This migration deploys and swaps KitDistrict contract and its dependencies
 *
 * Usage:
 * truffle migrate --network ganache/parity --reset --f 4 --to 4
 */

async function deploy_KitDistrict(deployer, daoFactoryAddr, ensAddr, fifsResolvingRegistrarAddr, dsGuardAddr, opts) {
  console.log("Deploying KitDistrict");

  await deployer.deploy(KitDistrict, daoFactoryAddr, ensAddr, fifsResolvingRegistrarAddr, Object.assign({}, opts, {gas: 4e6}));
  const kitDistrict = await KitDistrict.deployed();

  console.log("Setting authority of KitDistrict to DSGuard");
  await kitDistrict.setAuthority(dsGuardAddr, Object.assign({}, opts, {gas: 0.5e6}));

  console.log("Setting owner of KitDistrict to 0x0");
  await kitDistrict.setOwner(zeroAddress, Object.assign({}, opts, {gas: 0.5e6}));

  return kitDistrict;
}


async function deploy_District(deployer, dntAddr, districtChallengeAddr, stakeBankAddr, districtRegistryFwdAddr, opts) {
  console.log("Deploying District");

  const kitDistrict = await KitDistrict.deployed();

  linkBytecode(District, dntPlaceholder, dntAddr);
  linkBytecode(District, registryPlaceholder, districtRegistryFwdAddr);
  linkBytecode(District, forwarder1TargetPlaceholder, districtChallengeAddr);
  linkBytecode(District, forwarder2TargetPlaceholder, stakeBankAddr);
  linkBytecode(District, kitDistrictPlaceholder, kitDistrict.address);

  await deployer.deploy(District, Object.assign({}, opts, {gas: 6e6}));
  const district = await District.deployed();

  return district;
}


async function deploy_DistrictFactory(deployer, dntAddr, districtRegistryFwdAddr, oldDistrictFactoryAddr, opts) {
  console.log("Deploying DistrictFactory");

  const district = await District.deployed();

  linkBytecode(DistrictFactory, forwarder1TargetPlaceholder, district.address);

  await deployer.deploy(DistrictFactory, districtRegistryFwdAddr, dntAddr, Object.assign({}, opts, {gas: 1e6}));
  const districtFactory = await DistrictFactory.deployed();

  console.log("Allowing new DistrictFactory in DistrictRegistryForwarder");
  const districtRegistryForwarderInstance = await DistrictRegistry.at(districtRegistryFwdAddr);
  await districtRegistryForwarderInstance.setFactory(districtFactory.address, true, Object.assign({}, opts, {gas: 0.1e6}));

  console.log("Disabling old DistrictFactory in DistrictRegistryForwarder");
  await districtRegistryForwarderInstance.setFactory(oldDistrictFactoryAddr, false, Object.assign({}, opts, {gas: 0.1e6}));

  return districtFactory;
}


module.exports = async function(deployer, network, accounts) {
  const address = accounts[0];
  const gas = 6e6;
  const opts = {gas: gas, from: address};

  console.log("Redeploy KitDistrict Migration Started...");

  await deployer;
  console.log("@@@ using Web3 version:", web3.version);
  console.log("@@@ using address", address);

  var smartContracts = readSmartContractsFile(smartContractsPath);

  var dsGuardAddr = getSmartContractAddress(smartContracts, ":ds-guard");
  var dntAddr = getSmartContractAddress(smartContracts, ":DNT");
  var districtRegistryFwdAddr = getSmartContractAddress(smartContracts, ":district-registry-fwd");
  var oldDistrictFactoryAddr = getSmartContractAddress(smartContracts, ":district-factory");
  var districtChallengeAddr = getSmartContractAddress(smartContracts, ":district-challenge");
  var stakeBankAddr = getSmartContractAddress(smartContracts, ":stake-bank");
  var daoFactoryAddr = getSmartContractAddress(smartContracts, ":aragon/dao-factory") || parameters.DAOFactory;
  var ensAddr = getSmartContractAddress(smartContracts, ":ENS") || parameters.ENS;
  var fifsResolvingRegistrarAddr = getSmartContractAddress(smartContracts, ":aragon/fifs-resolving-registrar") || parameters.FIFSResolvingRegistrar;

  var kitDistrict = await deploy_KitDistrict(deployer, daoFactoryAddr, ensAddr, fifsResolvingRegistrarAddr, dsGuardAddr, opts);
  var district = await deploy_District(deployer, dntAddr, districtChallengeAddr, stakeBankAddr, districtRegistryFwdAddr, opts);
  var districtFactory = await deploy_DistrictFactory(deployer, dntAddr, districtRegistryFwdAddr, oldDistrictFactoryAddr, opts);

  console.log("New KitDistrict:", kitDistrict.address);
  console.log("New District:", district.address);
  console.log("New DistrictFactory:", districtFactory.address);

  setSmartContractAddress(smartContracts, ":kit-district", kitDistrict.address);
  setSmartContractAddress(smartContracts, ":district", district.address);
  setSmartContractAddress(smartContracts, ":district-factory", districtFactory.address);

  writeSmartContracts(smartContractsPath, smartContracts, env);

  console.log("KitDistrict successfully redeployed!");

};
