const {linkBytecode, readSmartContractsFile, writeSmartContracts, getSmartContractAddress, setSmartContractAddress, copyContract, Status} = require("./utils.js");
const fs = require("fs");
const {env, smartContractsPath, parameters} = require("../truffle.js");
const {registryPlaceholder, dntPlaceholder, forwarder1TargetPlaceholder, forwarder2TargetPlaceholder, ensPlaceholder} = require("./constants.js");

function requireContract(contractName, contractCopyName) {
  return artifacts.require(copyContract(contractName, contractCopyName));
}

let status = new Status("8");

let DistrictRegistry = requireContract("Registry", "DistrictRegistry");
let DistrictFactory = requireContract("DistrictFactory");
let District = requireContract("District");
let DistrictRegistryForwarder = requireContract("MutableForwarder");

/**
 * This migration deploys District contract and its dependencies to enable snapshot integration
 *
 * Usage:
 * truffle migrate --network ganache/parity --reset --f 8 --to 8
 */

async function deploy_DistrictRegistry(deployer, districtRegistryFwdAddr, districtRegistryDbAddr, opts) {
  console.log("Deploying DistrictRegistry");

  await deployer.deploy(DistrictRegistry, Object.assign({}, opts, {gas: 3.2e6}));
  const districtRegistry = await DistrictRegistry.deployed();

  return districtRegistry;
}

async function setRegistryFwdTarget(districtRegistryFwdAddr, districtRegistryAddr, opts) {
  console.log("Pointing DistrictRegistryForwarder to new Registry");
  const districtRegistryForwarder = await DistrictRegistryForwarder.at(districtRegistryFwdAddr);
  await districtRegistryForwarder.setTarget(districtRegistryAddr, Object.assign({}, opts, {gas: 0.5e6}));
}


async function deploy_District(deployer, ensAddr, dntAddr, districtChallengeAddr, stakeBankAddr, districtRegistryFwdAddr, opts) {
  console.log("Deploying District");

  linkBytecode(District, dntPlaceholder, dntAddr);
  linkBytecode(District, registryPlaceholder, districtRegistryFwdAddr);
  linkBytecode(District, forwarder1TargetPlaceholder, districtChallengeAddr);
  linkBytecode(District, forwarder2TargetPlaceholder, stakeBankAddr);
  linkBytecode(District, ensPlaceholder, ensAddr);

  await deployer.deploy(District, Object.assign({}, opts, {gas: 6.2e6}));
  const district = await District.deployed();

  return district;
}


async function deploy_DistrictFactory(deployer, dntAddr, districtRegistryFwdAddr, oldDistrictFactoryAddr, districtAddr, opts) {
  console.log("Deploying DistrictFactory");

  linkBytecode(DistrictFactory, forwarder1TargetPlaceholder, districtAddr);

  await deployer.deploy(DistrictFactory, districtRegistryFwdAddr, dntAddr, Object.assign({}, opts, {gas: 1.5e6}));
  const districtFactory = await DistrictFactory.deployed();

  return districtFactory;
}


async function allowDistrictFactory(districtRegistryFwdAddr, districtFactoryAddr, opts) {
  console.log("Allowing new DistrictFactory in DistrictRegistryForwarder");
  const districtRegistryForwarderInstance = await DistrictRegistry.at(districtRegistryFwdAddr);
  await districtRegistryForwarderInstance.setFactory(districtFactoryAddr, true, Object.assign({}, opts, {gas: 0.1e6}));

  // Note: Disabling old DistrictFactory will be done in a separated migration script
}


module.exports = async function(deployer, network, accounts) {
  const address = accounts[0];
  const gas = 6e6;
  const opts = {gas: gas, from: address};

  console.log("Redeploy District For Snapshot Integration Migration Started...");

  await deployer;
  console.log("@@@ using Web3 version:", web3.version);
  console.log("@@@ using address", address);

  var smartContracts = readSmartContractsFile(smartContractsPath);

  var dntAddr = getSmartContractAddress(smartContracts, ":DNT");
  var districtRegistryFwdAddr = getSmartContractAddress(smartContracts, ":district-registry-fwd");
  var districtRegistryDbAddr = getSmartContractAddress(smartContracts, ":district-registry-db");
  var oldDistrictFactoryAddr = getSmartContractAddress(smartContracts, ":district-factory");
  var districtChallengeAddr = getSmartContractAddress(smartContracts, ":district-challenge");
  var stakeBankAddr = getSmartContractAddress(smartContracts, ":stake-bank");
  var ensAddr = getSmartContractAddress(smartContracts, ":ENS") || parameters.ENS;

  await status.step(async ()=>{
    let districtRegistry = await deploy_DistrictRegistry(deployer, districtRegistryFwdAddr, districtRegistryDbAddr, opts);
    return {districtRegistry: districtRegistry.address};
  });

  await status.step(async ()=>{
    let districtRegistryAddr = status.getValue('districtRegistry');
    await setRegistryFwdTarget(districtRegistryFwdAddr, districtRegistryAddr, opts);
    return {};
  });

  await status.step(async ()=>{
    let district = await deploy_District(deployer, ensAddr, dntAddr, districtChallengeAddr, stakeBankAddr, districtRegistryFwdAddr, opts);
    return {district: district.address};
  });

  await status.step(async (status)=>{
    let districtAddr = status.getValue('district');
    let districtFactory = await deploy_DistrictFactory(deployer, dntAddr, districtRegistryFwdAddr, oldDistrictFactoryAddr, districtAddr, opts);
    return {districtFactory: districtFactory.address};
  });

  await status.step(async (status)=>{
    let districtFactoryAddr = status.getValue('districtFactory');
    await allowDistrictFactory(districtRegistryFwdAddr, districtFactoryAddr, opts);
    return {};
  });

  console.log("New District Registry:", status.getValue('districtRegistry'));
  console.log("New District:", status.getValue('district'));
  console.log("New DistrictFactory:", status.getValue('districtFactory'));

  setSmartContractAddress(smartContracts, ":district-registry", status.getValue('districtRegistry'));
  setSmartContractAddress(smartContracts, ":district", status.getValue('district'));
  setSmartContractAddress(smartContracts, ":district-factory", status.getValue('districtFactory'));

  writeSmartContracts(smartContractsPath, smartContracts, env);

  status.clean();

  console.log("District successfully redeployed!");

};
