/*
  Main Ethlance Deployment Script
 */

const {copy, smartContractsTemplate, encodeContractEDN, linkBytecode} = require("./utils.js");
const fs = require("fs");
const edn = require("jsedn");
const {env, contracts_build_directory, smart_contracts_path, parameters} = require("../truffle.js");


/*
  Returns the contract artifact for the given `contract_name`
 */
function requireContract(contract_name, contract_copy_name) {
  console.log("Creating Copy of " + contract_name + " for deployment...");
  const copy_name = contract_copy_name || contract_name + "_copy";
  console.log("- Contract Name: " + copy_name);
  copy(contract_name, copy_name, contracts_build_directory);
  return artifacts.require(copy_name);
}


//
// Placeholders
//

const registryPlaceholder = "feedfeedfeedfeedfeedfeedfeedfeedfeedfeed";
const dntPlaceholder = "deaddeaddeaddeaddeaddeaddeaddeaddeaddead";
const forwarderTargetPlaceholder = "beefbeefbeefbeefbeefbeefbeefbeefbeefbeef";
const challengeFactoryPlaceholder = "cccccccccccccccccccccccccccccccccccccccc";
const stakeBankFactoryPlaceholder = "dddddddddddddddddddddddddddddddddddddddd";
const powerFactoryPlaceholder = "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee";

const zeroAddress = "0x0000000000000000000000000000000000000000";
const dsGuardANY = "0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";

//
// Contract Artifacts
//

let DSGuard = requireContract("DSGuard");
let DNT = requireContract("District0xNetworkToken");
let DistrictFactory = requireContract("DistrictFactory");
let District = requireContract("District");
let ParamChangeRegistry = requireContract("ParamChangeRegistry");
let ParamChangeRegistryForwarder = requireContract("MutableForwarder", "ParamChangeRegistryForwarder");
let ParamChangeRegistryDb = requireContract("EternalDb", "ParamChangeRegistryDb");
let ParamChange = requireContract("ParamChange");
let DistrictRegistry = requireContract("Registry", "DistrictRegistry");
let DistrictRegistryForwarder = requireContract("MutableForwarder", "DistrictRegistryForwarder");
let MiniMeTokenFactory = requireContract("MiniMeTokenFactory");
let PowerFactory = requireContract("PowerFactory");
let StakeBankFactory = requireContract("StakeBankFactory");
let ParamChangeFactory = requireContract("ParamChangeFactory");
let ChallengeFactory = requireContract("ChallengeFactory");
let DistrictRegistryDb = requireContract("EternalDb", "DistrictRegistryDb");


async function deploy_DSGuard(deployer, opts) {
  console.log("Deploying DSGuard");
  await deployer.deploy(DSGuard, Object.assign({}, opts, {gas: 1.3e6}));
  const dsGuard = await DSGuard.deployed();

  // Set DSGuard Authority
  console.log("- Configuring DSGuard Authority...");
  await dsGuard.setAuthority(dsGuard.address, Object.assign({}, opts, {gas: 0.5e6}));

  assignContract(dsGuard, "DSGuard", "ds-guard");
}


async function deploy_MiniMeTokenFactory(deployer, opts) {
  console.log("Deploying MiniMeTokenFactory");
  await deployer.deploy(MiniMeTokenFactory, Object.assign({}, opts, {gas: 3.5e6}));
  const miniMeTokenFactory = await MiniMeTokenFactory.deployed();

  assignContract(miniMeTokenFactory, "MiniMeTokenFactory", "minime-token-factory");
}


async function deploy_DNT(deployer, opts) {
  console.log("Deploying DNT");

  const miniMeTokenFactory = await MiniMeTokenFactory.deployed();
  await deployer.deploy(DNT, miniMeTokenFactory.address, "1000000000000000000000000", opts);
  const dnt = await DNT.deployed();

  assignContract(dnt, "District0xNetworkToken", "DNT");
}


async function deploy_DistrictRegistryDb(deployer, opts) {
  console.log("Deploying DistrictRegistryDb");

  await deployer.deploy(DistrictRegistryDb, Object.assign({}, opts, {gas: 2.7e6}));
  const districtRegistryDb = await DistrictRegistryDb.deployed();

  await setInitialParameters(districtRegistryDb, "districtRegistryDb", opts);

  console.log("Setting authority of DistrictRegistryDb to DSGuard");
  const dsGuard = await DSGuard.deployed();
  await districtRegistryDb.setAuthority(dsGuard.address, Object.assign({}, opts, {gas: 0.5e6}));

  console.log("Setting owner of DistrictRegistryDb to 0x0");
  await districtRegistryDb.setOwner(zeroAddress, Object.assign({}, opts, {gas: 0.5e6}));

  assignContract(districtRegistryDb, "EternalDb", "district-registry-db");
}


async function deploy_ParamChangeRegistryDb(deployer, opts) {
  console.log("Deploying ParamChangeRegistryDb");

  await deployer.deploy(ParamChangeRegistryDb, Object.assign({}, opts, {gas: 2.7e6}));
  const paramChangeRegistryDb = await ParamChangeRegistryDb.deployed();

  await setInitialParameters(paramChangeRegistryDb, "paramChangeRegistryDb", opts);

  console.log("Setting authority of ParamChangeRegistryDb to DSGuard");
  const dsGuard = await DSGuard.deployed();
  await paramChangeRegistryDb.setAuthority(dsGuard.address, Object.assign({}, opts, {gas: 0.5e6}));

  console.log("Setting owner of ParamChangeRegistryDb to 0x0");
  await paramChangeRegistryDb.setOwner(zeroAddress, Object.assign({}, opts, {gas: 0.5e6}));

  assignContract(paramChangeRegistryDb, "EternalDb", "param-change-registry-db");
}


async function deploy_DistrictRegistry(deployer, opts) {
  console.log("Deploying DistrictRegistry");

  await deployer.deploy(DistrictRegistry, Object.assign({}, opts, {gas: 3e6}));
  const districtRegistry = await DistrictRegistry.deployed();

  assignContract(districtRegistry, "DistrictRegistry", "district-registry");
}


async function deploy_DistrictRegistryForwarder(deployer, opts) {
  console.log("Deploying DistrictRegistryForwarder");

  const districtRegistry = await DistrictRegistry.deployed();

  linkBytecode(DistrictRegistryForwarder, forwarderTargetPlaceholder, districtRegistry.address);

  await deployer.deploy(DistrictRegistryForwarder, Object.assign({}, opts, {gas: 0.7e6}));
  const districtRegistryForwarder = await DistrictRegistryForwarder.deployed();

  console.log("Constructing DistrictRegistryForwarder");
  const districtRegistryDb = await DistrictRegistryDb.deployed();
  const districtRegistryForwarderInstance = await DistrictRegistry.at(districtRegistryForwarder.address);
  districtRegistryForwarderInstance.construct(districtRegistryDb.address, Object.assign({}, opts, {gas: 0.5e6}));

  console.log("Giving permission to DistrictRegistryForwarder to change DistrictRegistryDb");
  const dsGuard = await DSGuard.deployed();
  await dsGuard.permit(districtRegistryForwarder.address, districtRegistryDb.address, dsGuardANY, Object.assign({}, opts, {gas: 0.2e6}));

  assignContract(districtRegistryForwarder, "MutableForwarder", "district-registry-fwd", {forwards_to: "district-registry"});
}


async function deploy_ParamChangeRegistry(deployer, opts) {
  console.log("Deploying ParamChangeRegistry");

  await deployer.deploy(ParamChangeRegistry, Object.assign({}, opts, {gas: 3.4e6}));
  const paramChangeRegistry = await ParamChangeRegistry.deployed();

  assignContract(paramChangeRegistry, "ParamChangeRegistry", "param-change-registry");
}


async function deploy_ParamChangeRegistryForwarder(deployer, opts) {
  console.log("Deploying ParamChangeRegistryForwarder");

  const paramChangeRegistry = await ParamChangeRegistry.deployed();
  linkBytecode(ParamChangeRegistryForwarder, forwarderTargetPlaceholder, paramChangeRegistry.address);
  await deployer.deploy(ParamChangeRegistryForwarder, Object.assign({}, opts, {gas: 0.7e6}));
  const paramChangeRegistryForwarder = await ParamChangeRegistryForwarder.deployed();
  
  console.log("Constructing ParamChangeRegistryForwarder");
  const paramChangeRegistryDb = await ParamChangeRegistryDb.deployed();
  const paramChangeRegistryForwarderInstance = await ParamChangeRegistry.at(paramChangeRegistryForwarder.address);
  paramChangeRegistryForwarderInstance.construct(paramChangeRegistryDb.address, Object.assign({}, opts, {gas: 0.5e6}));

  console.log("Giving permission to ParamChangeRegistryForwarder to grand permissions to other contracts");
  const dsGuard = await DSGuard.deployed();
  await dsGuard.permit(paramChangeRegistryForwarder.address, dsGuard.address, dsGuardANY, Object.assign({}, opts, {gas: 0.2e6}));

  console.log("Giving permission to ParamChangeRegistryForwarder to change ParamChangeRegistryDb");
  await dsGuard.permit(paramChangeRegistryForwarder.address, paramChangeRegistryDb.address, dsGuardANY, Object.assign({}, opts, {gas: 0.1e6}));

  console.log("Giving permission to ParamChangeRegistryForwarder to change DistrictRegistryDb");
  const districtRegistryDb = await DistrictRegistryDb.deployed();
  await dsGuard.permit(paramChangeRegistryForwarder.address, districtRegistryDb.address, dsGuardANY, Object.assign({}, opts, {gas: 0.1e6}));

  assignContract(paramChangeRegistryForwarder, "MutableForwarder", "param-change-registry-fwd", {forwards_to: "param-change-registry"});
}


async function deploy_PowerFactory(deployer, opts) {
  console.log("Deploying PowerFactory");

  await deployer.deploy(PowerFactory, Object.assign({}, opts, {gas: 2e6}));
  const powerFactory = await PowerFactory.deployed();

  assignContract(powerFactory, "PowerFactory", "power-factory");
}


async function deploy_StakeBankFactory(deployer, opts) {
  console.log("Deploying StakeBankFactory");

  const powerFactory = await PowerFactory.deployed();
  linkBytecode(StakeBankFactory, powerFactoryPlaceholder, powerFactory.address);
  await deployer.deploy(StakeBankFactory, Object.assign({}, opts, {gas: 4.5e6}));
  const stakeBankFactory = await StakeBankFactory.deployed();

  assignContract(stakeBankFactory, "StakeBankFactory", "stake-bank-factory");
}


async function deploy_ChallengeFactory(deployer, opts) {
  console.log("Deploying ChallengeFactory");

  const dnt = await DNT.deployed();
  linkBytecode(ChallengeFactory, dntPlaceholder, dnt.address);
  await deployer.deploy(ChallengeFactory, Object.assign({}, opts, {gas: 2.5e6}));
  const challengeFactory = await ChallengeFactory.deployed();

  assignContract(challengeFactory, "ChallengeFactory", "challenge-factory");
}


async function deploy_District(deployer, opts) {
  console.log("Deploying District");

  const dnt = await DNT.deployed();
  const challengeFactory = await ChallengeFactory.deployed();
  const stakeBankFactory = await StakeBankFactory.deployed();
  const districtRegistryForwarder = await DistrictRegistryForwarder.deployed();

  linkBytecode(District, dntPlaceholder, dnt.address);
  linkBytecode(District, registryPlaceholder, districtRegistryForwarder.address);
  linkBytecode(District, challengeFactoryPlaceholder, challengeFactory.address);
  linkBytecode(District, stakeBankFactoryPlaceholder, stakeBankFactory.address);

  await deployer.deploy(District, Object.assign({}, opts, {gas: 4.5e6}));
  const district = await District.deployed();

  assignContract(district, "District", "district");
}


async function deploy_ParamChange(deployer, opts) {
  console.log("Deploying ParamChange");

  const dnt = await DNT.deployed();
  const challengeFactory = await ChallengeFactory.deployed();
  const paramChangeRegistryForwarder = await ParamChangeRegistryForwarder.deployed();

  linkBytecode(ParamChange, dntPlaceholder, dnt.address);
  linkBytecode(ParamChange, registryPlaceholder, paramChangeRegistryForwarder.address);
  linkBytecode(ParamChange, challengeFactoryPlaceholder, challengeFactory.address);

  await deployer.deploy(ParamChange, Object.assign({}, opts, {gas: 4.2e6}));
  const paramChange = await ParamChange.deployed();

  assignContract(paramChange, "ParamChange", "param-change");
}


async function deploy_DistrictFactory(deployer, opts) {
  console.log("Deploying DistrictFactory");

  const dnt = await DNT.deployed();
  const districtRegistryForwarder = await DistrictRegistryForwarder.deployed();
  const district = await District.deployed();

  linkBytecode(DistrictFactory, forwarderTargetPlaceholder, district.address);

  await deployer.deploy(DistrictFactory, districtRegistryForwarder.address, dnt.address, Object.assign({}, opts, {gas: 1e6}));
  const districtFactory = await DistrictFactory.deployed();

  console.log("Allowing new DistrictFactory in DistrictRegistryForwarder");
  const districtRegistryForwarderInstance = await DistrictRegistry.at(districtRegistryForwarder.address);
  await districtRegistryForwarderInstance.setFactory(districtFactory.address, true, Object.assign({}, opts, {gas: 0.1e6}));

  assignContract(districtFactory, "DistrictFactory", "district-factory");
}


async function deploy_ParamChangeFactory(deployer, opts) {
  console.log("Deploying ParamChangeFactory");

  const dnt = await DNT.deployed();
  const paramChangeRegistryForwarder = await ParamChangeRegistryForwarder.deployed();
  const paramChange = await ParamChange.deployed();

  linkBytecode(ParamChangeFactory, forwarderTargetPlaceholder, paramChange.address);

  await deployer.deploy(ParamChangeFactory, paramChangeRegistryForwarder.address, dnt.address, Object.assign({}, opts, {gas: 1e6}));
  const paramChangeFactory = await ParamChangeFactory.deployed();
  
  console.log("Allowing new ParamChangeFactory in ParamChangeRegistryForwarder");
  const paramChangeRegistryForwarderInstance = await ParamChangeRegistry.at(paramChangeRegistryForwarder.address);
  await paramChangeRegistryForwarderInstance.setFactory(paramChangeFactory.address, true, Object.assign({}, opts, {gas: 0.1e6}));

  assignContract(paramChangeFactory, "ParamChangeFactory", "param-change-factory");
}


async function setInitialParameters(instance, parametersKey, opts) {
  console.log("Setting initial paramterers in EternalDB " + parametersKey);

  return instance.setUIntValues(
    ['challengePeriodDuration',
     'commitPeriodDuration',
     'revealPeriodDuration',
     'deposit',
     'challengeDispensation',
     'voteQuorum'].map((key) => {return web3.utils.soliditySha3({t: "string", v: key})}),
     [parameters[parametersKey].challengePeriodDuration.toString(),
      parameters[parametersKey].commitPeriodDuration.toString(),
      parameters[parametersKey].revealPeriodDuration.toString(),
      parameters[parametersKey].deposit.toString(),
      parameters[parametersKey].challengeDispensation.toString(),
      parameters[parametersKey].voteQuorum.toString()],
    Object.assign({}, opts, {gas: 1e6})
  );
}



/*
  Deploy All Ethlance Contracts
 */
async function deployAll(deployer, opts) {
  await deploy_DSGuard(deployer, opts);

  await deploy_MiniMeTokenFactory(deployer, opts);
  await deploy_DNT(deployer, opts);

  await deploy_DistrictRegistryDb(deployer, opts);
  await deploy_ParamChangeRegistryDb(deployer, opts);

  await deploy_DistrictRegistry(deployer, opts);
  await deploy_DistrictRegistryForwarder(deployer, opts);

  await deploy_ParamChangeRegistry(deployer, opts);
  await deploy_ParamChangeRegistryForwarder(deployer, opts);

  await deploy_PowerFactory(deployer, opts);
  await deploy_StakeBankFactory(deployer, opts);
  await deploy_ChallengeFactory(deployer, opts);

  await deploy_District(deployer, opts);
  await deploy_ParamChange(deployer, opts);

  await deploy_DistrictFactory(deployer, opts);
  await deploy_ParamChangeFactory(deployer, opts);

  writeSmartContracts();
}


//
// Smart Contract Functions
//


let smart_contract_listing = [];
/*
  Concatenate the given contract to our smart contract listing.
 */
function assignContract(contract_instance, contract_name, contract_key, opts) {
  console.log("- Assigning '" + contract_name + "' to smart contract listing...");
  opts = opts || {};
  smart_contract_listing = smart_contract_listing.concat(
    encodeContractEDN(contract_instance, contract_name, contract_key, opts));
}

/*
  Write out our smart contract listing to the file defined by `smart_contracts_path`
 */
function writeSmartContracts() {
  console.log("Final Smart Contract Listing:");
  const smart_contracts = edn.encode(new edn.Map(smart_contract_listing));
  console.log(smart_contracts);
  console.log("Writing to smart contract file: " + smart_contracts_path + " ...");
  fs.writeFileSync(smart_contracts_path, smartContractsTemplate(smart_contracts, env));
}



//
// Begin Migration
//


module.exports = async function(deployer, network, accounts) {
  const address = accounts[0];
  const gas = 4e6;
  const opts = {gas: gas, from: address};

  console.log("District Registry Deployment Started...");

  await deployer;
  console.log("@@@ using Web3 version:", web3.version);
  console.log("@@@ using address", address);

  try {
    await deployAll(deployer, opts);
    console.log("District Registry Deployment Finished!");
  }
  catch(error) {
    console.error("ERROR: There was a problem during deployment");
    console.error(error);
  }
};
