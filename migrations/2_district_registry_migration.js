const {copyContract, copy, smartContractsTemplate, encodeContractEDN, linkBytecode, encodeSmartContracts, writeSmartContracts, kitDistrictAppsToNum} = require("./utils.js");
const fs = require("fs");
const edn = require("jsedn");
const {env, smartContractsPath, parameters} = require("../truffle.js");
const {registryPlaceholder, dntPlaceholder, forwarder1TargetPlaceholder, forwarder2TargetPlaceholder, minimeTokenFactoryPlaceholder, kitDistrictPlaceholder, zeroAddress, dsGuardANY, aragonENSNode} = require("./constants.js");
const namehash = require('eth-ens-namehash');
const web3Utils = require('web3-utils');
const sha3 = web3Utils.sha3;

let smartContractsList = [];

// These are hashes from mainnet, but it's only for development. For testnet and mainnet we don't need to deploy these apps
const aragonVotingIPFS = web3Utils.fromAscii("QmZaUjYWqXeVWDpQLJdFUrewxhnWTiCSzv8RPCcL2FEXED"); // 2.0.4
const aragonVaultIPFS = web3Utils.fromAscii("QmYRcQBcHsietDuUmt7bbFG7BqetoYF85CxDn7pAa2wdnA"); // 3.0.1
const aragonFinanceIPFS = web3Utils.fromAscii("QmUigVxDYp4qcNZUwBe3Q1mG7Mq7rQrdctMreke1Cg32oZ"); // 2.0.6


function requireContract(contractName, contractCopyName) {
  return artifacts.require(copyContract(contractName, contractCopyName));
}


//
// Contract Artifacts
//

let DSGuard = requireContract("DSGuard");
let DNT = requireContract("District0xNetworkToken");
let DistrictFactory = requireContract("DistrictFactory");
let District = requireContract("District");
let ParamChangeRegistry = requireContract("ParamChangeRegistry");
let ParamChangeFactory = requireContract("ParamChangeFactory");
let ParamChangeRegistryForwarder = requireContract("MutableForwarder", "ParamChangeRegistryForwarder");
let ParamChangeRegistryDb = requireContract("EternalDb", "ParamChangeRegistryDb");
let ParamChange = requireContract("ParamChange");
let DistrictRegistry = requireContract("Registry", "DistrictRegistry");
let DistrictRegistryForwarder = requireContract("MutableForwarder", "DistrictRegistryForwarder");
let MiniMeTokenFactory = requireContract("MiniMeTokenFactory");
let Power = requireContract("Power");
let StakeBank = requireContract("StakeBank");
let Challenge = requireContract("Challenge");
let DistrictChallenge = requireContract("DistrictChallenge");
let DistrictRegistryDb = requireContract("EternalDb", "DistrictRegistryDb");
let KitDistrict = requireContract("KitDistrict", "KitDistrict");
let District0xEmails = requireContract("District0xEmails");

// Aragon Contracts
let Kernel = requireContract("Kernel", "Kernel");
let ACL = requireContract("ACL", "ACL");
let EVMScriptRegistryFactory = requireContract("EVMScriptRegistryFactory", "EVMScriptRegistryFactory");
let DAOFactory = requireContract("DAOFactory", "DAOFactory");
let ENS = requireContract("ENS", "ENS");
let PublicResolver = requireContract("PublicResolver", "PublicResolver");
let FIFSResolvingRegistrar = requireContract("FIFSResolvingRegistrar", "FIFSResolvingRegistrar");
let APMRegistry = requireContract("APMRegistry", "APMRegistry");
let APMRegistryFactory = requireContract("APMRegistryFactory", "APMRegistryFactory");
let Repo = requireContract("Repo", "Repo");
let ENSSubdomainRegistrar = requireContract("ENSSubdomainRegistrar", "ENSSubdomainRegistrar");
let Voting = requireContract("Voting", "Voting");
let Finance = requireContract("Finance", "Finance");
let Vault = requireContract("Vault", "Vault");


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
  var miniMeTokenFactory;
  if (parameters.MiniMeTokenFactory) {
    miniMeTokenFactory = await MiniMeTokenFactory.at(parameters.MiniMeTokenFactory);
  } else {
    console.log("Deploying MiniMeTokenFactory");
    await deployer.deploy(MiniMeTokenFactory, Object.assign({}, opts, {gas: 3.5e6}));
    miniMeTokenFactory = await MiniMeTokenFactory.deployed();
  }

  assignContract(miniMeTokenFactory, "MiniMeTokenFactory", "minime-token-factory");
}


async function deploy_DNT(deployer, opts) {
  var dnt;
  if (parameters.DNT) {
    console.log("Using existing DNT");
    dnt = await DNT.at(parameters.DNT);
  } else {
    console.log("Deploying DNT");
    const miniMeTokenFactory = await MiniMeTokenFactory.deployed();
    await deployer.deploy(DNT, miniMeTokenFactory.address, "1000000000000000000000000", opts);
    dnt = await DNT.deployed();
  }

  assignContract(dnt, "District0xNetworkToken", "DNT");
}


async function getDNT() {
  if (parameters.DNT) {
    return DNT.at(parameters.DNT);
  } else {
    return DNT.deployed();
  }
}

async function getMiniMeTokenFactory() {
  if (parameters.MiniMeTokenFactory) {
    return MiniMeTokenFactory.at(parameters.MiniMeTokenFactory);
  } else {
    return MiniMeTokenFactory.deployed();
  }
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

  await deployer.deploy(DistrictRegistry, Object.assign({}, opts, {gas: 3.2e6}));
  const districtRegistry = await DistrictRegistry.deployed();

  assignContract(districtRegistry, "Registry", "district-registry");
}


async function deploy_DistrictRegistryForwarder(deployer, opts) {
  console.log("Deploying DistrictRegistryForwarder");

  const districtRegistry = await DistrictRegistry.deployed();

  linkBytecode(DistrictRegistryForwarder, forwarder1TargetPlaceholder, districtRegistry.address);

  await deployer.deploy(DistrictRegistryForwarder, Object.assign({}, opts, {gas: 0.7e6}));
  const districtRegistryForwarder = await DistrictRegistryForwarder.deployed();

  console.log("Constructing DistrictRegistryForwarder");
  const districtRegistryDb = await DistrictRegistryDb.deployed();
  const districtRegistryForwarderInstance = await DistrictRegistry.at(districtRegistryForwarder.address);
  districtRegistryForwarderInstance.construct(districtRegistryDb.address, Object.assign({}, opts, {gas: 0.5e6}));

  console.log("Giving permission to DistrictRegistryForwarder to change DistrictRegistryDb");
  const dsGuard = await DSGuard.deployed();
  await dsGuard.permit(districtRegistryForwarder.address, districtRegistryDb.address, dsGuardANY, Object.assign({}, opts, {gas: 0.2e6}));

  assignContract(districtRegistryForwarder, "MutableForwarder", "district-registry-fwd", {forwardsTo: "district-registry"});
}


async function deploy_ParamChangeRegistry(deployer, opts) {
  console.log("Deploying ParamChangeRegistry");

  await deployer.deploy(ParamChangeRegistry, Object.assign({}, opts, {gas: 4e6}));
  const paramChangeRegistry = await ParamChangeRegistry.deployed();

  assignContract(paramChangeRegistry, "ParamChangeRegistry", "param-change-registry");
}


async function deploy_ParamChangeRegistryForwarder(deployer, opts) {
  console.log("Deploying ParamChangeRegistryForwarder");

  const paramChangeRegistry = await ParamChangeRegistry.deployed();
  linkBytecode(ParamChangeRegistryForwarder, forwarder1TargetPlaceholder, paramChangeRegistry.address);
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

  assignContract(paramChangeRegistryForwarder, "MutableForwarder", "param-change-registry-fwd", {forwardsTo: "param-change-registry"});
}


async function deploy_Power(deployer, opts) {
  console.log("Deploying Power");

  await deployer.deploy(Power, Object.assign({}, opts, {gas: 2e6}));
  const power = await Power.deployed();

  assignContract(power, "Power", "power");
}


async function deploy_StakeBank(deployer, opts) {
  console.log("Deploying StakeBank");

  const power = await Power.deployed();
  const miniMeTokenFactory = await getMiniMeTokenFactory();
  linkBytecode(StakeBank, forwarder1TargetPlaceholder, power.address);
  linkBytecode(StakeBank, minimeTokenFactoryPlaceholder, miniMeTokenFactory.address);
  await deployer.deploy(StakeBank, Object.assign({}, opts, {gas: 5.2e6}));
  const stakeBank = await StakeBank.deployed();

  assignContract(stakeBank, "StakeBank", "stake-bank");
}


async function deploy_Challenge(deployer, opts) {
  console.log("Deploying Challenge");

  const dnt = await getDNT();
  linkBytecode(Challenge, dntPlaceholder, dnt.address);
  await deployer.deploy(Challenge, Object.assign({}, opts, {gas: 3.5e6}));
  const challenge = await Challenge.deployed();

  assignContract(challenge, "Challenge", "challenge");
}

async function deploy_DistrictChallenge(deployer, opts) {
  console.log("Deploying District Challenge");

  const dnt = await getDNT();
  linkBytecode(DistrictChallenge, dntPlaceholder, dnt.address);
  await deployer.deploy(DistrictChallenge, Object.assign({}, opts, {gas: 3.5e6}));
  const districtChallenge = await DistrictChallenge.deployed();

  assignContract(districtChallenge, "DistrictChallenge", "district-challenge");
}


async function deploy_District(deployer, opts) {
  console.log("Deploying District");

  const dnt = await getDNT();
  const districtChallenge = await DistrictChallenge.deployed();
  const stakeBank = await StakeBank.deployed();
  const districtRegistryForwarder = await DistrictRegistryForwarder.deployed();
  const kitDistrict = await KitDistrict.deployed();

  linkBytecode(District, dntPlaceholder, dnt.address);
  linkBytecode(District, registryPlaceholder, districtRegistryForwarder.address);
  linkBytecode(District, forwarder1TargetPlaceholder, districtChallenge.address);
  linkBytecode(District, forwarder2TargetPlaceholder, stakeBank.address);
  linkBytecode(District, kitDistrictPlaceholder, kitDistrict.address);

  await deployer.deploy(District, Object.assign({}, opts, {gas: 6.2e6}));
  const district = await District.deployed();

  assignContract(district, "District", "district");
}


async function deploy_ParamChange(deployer, opts) {
  console.log("Deploying ParamChange");

  const dnt = await getDNT();
  const challenge = await Challenge.deployed();
  const paramChangeRegistryForwarder = await ParamChangeRegistryForwarder.deployed();

  linkBytecode(ParamChange, dntPlaceholder, dnt.address);
  linkBytecode(ParamChange, registryPlaceholder, paramChangeRegistryForwarder.address);
  linkBytecode(ParamChange, forwarder1TargetPlaceholder, challenge.address);

  await deployer.deploy(ParamChange, Object.assign({}, opts, {gas: 5.8e6}));
  const paramChange = await ParamChange.deployed();

  assignContract(paramChange, "ParamChange", "param-change");
}


async function deploy_DistrictFactory(deployer, opts) {
  console.log("Deploying DistrictFactory");

  const dnt = await getDNT();
  const districtRegistryForwarder = await DistrictRegistryForwarder.deployed();
  const district = await District.deployed();

  linkBytecode(DistrictFactory, forwarder1TargetPlaceholder, district.address);

  await deployer.deploy(DistrictFactory, districtRegistryForwarder.address, dnt.address, Object.assign({}, opts, {gas: 1.5e6}));
  const districtFactory = await DistrictFactory.deployed();

  console.log("Allowing new DistrictFactory in DistrictRegistryForwarder");
  const districtRegistryForwarderInstance = await DistrictRegistry.at(districtRegistryForwarder.address);
  await districtRegistryForwarderInstance.setFactory(districtFactory.address, true, Object.assign({}, opts, {gas: 0.1e6}));

  assignContract(districtFactory, "DistrictFactory", "district-factory");
}


async function deploy_ParamChangeFactory(deployer, opts) {
  console.log("Deploying ParamChangeFactory");

  const dnt = await getDNT();
  const paramChangeRegistryForwarder = await ParamChangeRegistryForwarder.deployed();
  const paramChange = await ParamChange.deployed();

  linkBytecode(ParamChangeFactory, forwarder1TargetPlaceholder, paramChange.address);

  await deployer.deploy(ParamChangeFactory, paramChangeRegistryForwarder.address, dnt.address, Object.assign({}, opts, {gas: 1.5e6}));
  const paramChangeFactory = await ParamChangeFactory.deployed();
  
  console.log("Allowing new ParamChangeFactory in ParamChangeRegistryForwarder");
  const paramChangeRegistryForwarderInstance = await ParamChangeRegistry.at(paramChangeRegistryForwarder.address);
  await paramChangeRegistryForwarderInstance.setFactory(paramChangeFactory.address, true, Object.assign({}, opts, {gas: 0.1e6}));

  assignContract(paramChangeFactory, "ParamChangeFactory", "param-change-factory");
}

//
// ENS Deployments
//

async function deploy_ENS(deployer, opts) {
  var ens;
  if (!parameters.ENS) {
    console.log("Deploying ENS");

    await deployer.deploy(ENS, Object.assign({}, opts, {gas: 0.7e6}));
    ens = await ENS.deployed();

    console.log("Setting active account owner of .eth");
    await ens.setSubnodeOwner("0x0", sha3("eth"), opts.from, Object.assign({}, opts, {gas: 0.2e6}));
  } else {
    ens = await ENS.at(parameters.ENS);
  }

  assignContract(ens, "ENS", "ENS");
}

async function deploy_PublicResolver(deployer, opts) {
  if (!parameters.ENS) {
    console.log("Deploying ENS PublicResolver");

    const ens = await ENS.deployed();
    await deployer.deploy(PublicResolver, ens.address, Object.assign({}, opts, {gas: 1.9e6}));
    const publicResolver = await PublicResolver.deployed();

    console.log("Setting active account to be owner of resolver.eth");
    await ens.setSubnodeOwner(namehash.hash("eth"), sha3("resolver"), opts.from, Object.assign({}, opts, {gas: 0.2e6}));

    console.log("Setting resolver for resolver.eth");
    await ens.setResolver(namehash.hash("resolver.eth"), publicResolver.address, Object.assign({}, opts, {gas: 0.2e6}));

    console.log("Setting resolving address for resolver.eth");
    await publicResolver.setAddr(namehash.hash("resolver.eth"), publicResolver.address, Object.assign({}, opts, {gas: 0.3e6}));

    assignContract(publicResolver, "PublicResolver", "public-resolver");
  }
}

async function deploy_Kernel(deployer, opts) {
  console.log("Deploying Aragon Kernel");

  await deployer.deploy(ParamChangeFactory, 0, 0, Object.assign({}, opts, {gas: 1e6}));
  const paramChangeFactory = await ParamChangeFactory.deployed();

  console.log("Allowing new ParamChangeFactory in ParamChangeRegistryForwarder");
  const paramChangeRegistryForwarderInstance = await ParamChangeRegistry.at(paramChangeRegistryForwarder.address);
  await paramChangeRegistryForwarderInstance.setFactory(paramChangeFactory.address, true, Object.assign({}, opts, {gas: 0.1e6}));

  assignContract(paramChangeFactory, "ParamChangeFactory", "param-change-factory");
}

//
// Aragon Deployments
//

async function deploy_ACL(deployer, opts) {
  console.log("Deploying Aragon ACL");

  await deployer.deploy(ACL, Object.assign({}, opts, {gas: 4.5e6}));
  const acl = await ACL.deployed();

  assignContract(acl, "ACL", "aragon/acl");
}

async function deploy_EVMScriptRegistryFactory(deployer, opts) {
  console.log("Deploying Aragon EVMScriptRegistryFactory");

  await deployer.deploy(EVMScriptRegistryFactory, Object.assign({}, opts, {gas: 4.1e6}));
  const evmScriptRegistryFactory = await EVMScriptRegistryFactory.deployed();

  assignContract(evmScriptRegistryFactory, "EVMScriptRegistryFactory", "aragon/evm-script-registry-factory");
}

async function deploy_Kernel(deployer, opts) {
  console.log("Deploying Aragon Kernel");

  await deployer.deploy(Kernel, true, Object.assign({}, opts, {gas: 4.6e6}));
  const kernel = await Kernel.deployed();

  assignContract(kernel, "Kernel", "aragon/kernel");
}

async function deploy_DAOFactory(deployer, opts) {
  console.log("Deploying Aragon DAOFactory");

  const kernel = await Kernel.deployed();
  const acl = await ACL.deployed();
  const evmScriptRegistryFactory = await EVMScriptRegistryFactory.deployed();

  await deployer.deploy(DAOFactory, kernel.address, acl.address, evmScriptRegistryFactory.address, Object.assign({}, opts, {gas: 1.8e6}));
  const daoFactory = await DAOFactory.deployed();

  assignContract(daoFactory, "DAOFactory", "aragon/dao-factory");
}

async function deploy_ENSSubdomainRegistrar(deployer, opts) {
  console.log("Deploying Aragon ENSSubdomainRegistrar");

  const ens = await ENS.deployed();
  await deployer.deploy(ENSSubdomainRegistrar, Object.assign({}, opts, {gas: 6e6}));
  const ensSubdomainRegistrar = await ENSSubdomainRegistrar.deployed();

  assignContract(ensSubdomainRegistrar, "ENSSubdomainRegistrar", "aragon/ens-subdomain-registrar");
}

async function deploy_Repo(deployer, opts) {
  console.log("Deploying Aragon Repo");

  await deployer.deploy(Repo, Object.assign({}, opts, {gas: 6e6}));
  const repo = await Repo.deployed();

  assignContract(Repo, "Repo", "aragon/repo");
}

async function deploy_APMRegistry(deployer, opts) {
  console.log("Deploying Aragon APMRegistry");

  await deployer.deploy(APMRegistry, Object.assign({}, opts, {gas: 6e6}));
  const apmRegistry = await APMRegistry.deployed();

  assignContract(apmRegistry, "APMRegistry", "aragon/apm-registry");
}

async function deploy_APMRegistryFactory(deployer, opts) {
  console.log("Deploying Aragon APMRegistryFactory");

  const ens = await ENS.deployed();
  const daoFactory = await DAOFactory.deployed();
  const repo = await Repo.deployed();
  const ensSubdomainRegistrar = await ENSSubdomainRegistrar.deployed();
  const apmRegistry = await APMRegistry.deployed();

  await deployer.deploy(APMRegistryFactory, daoFactory.address, apmRegistry.address, repo.address, ensSubdomainRegistrar.address, ens.address, zeroAddress, Object.assign({}, opts, {gas: 6e6}));
  const apmRegistryFactory = await APMRegistryFactory.deployed();

  console.log("Setting APMRegistryFactory to be owner of aragonpm.eth");
  await ens.setSubnodeOwner(namehash.hash("eth"), sha3("aragonpm"), apmRegistryFactory.address, Object.assign({}, opts, {gas: 0.2e6}));

  console.log("Creating new APM at aragonpm.eth");
  await apmRegistryFactory.newAPM(namehash.hash("eth"), sha3("aragonpm"), opts.from, Object.assign({}, opts, {gas: 7e6}));

  assignContract(apmRegistryFactory, "APMRegistryFactory", "aragon/apm-registry-factory");
}


async function deploy_FIFSResolvingRegistrar(deployer, opts) {
  console.log("Deploying Aragon FIFSResolvingRegistrar");

  const ens = await ENS.deployed();
  const publicResolver = await PublicResolver.deployed();

  await deployer.deploy(FIFSResolvingRegistrar, ens.address, publicResolver.address, aragonENSNode, Object.assign({}, opts, {gas: 0.7e6}));
  const fifsResolvingRegistrar = await FIFSResolvingRegistrar.deployed();

  console.log("Setting FIFSResolvingRegistrar owner of aragonid.eth");
  await ens.setSubnodeOwner(namehash.hash("eth"), sha3("aragonid"), fifsResolvingRegistrar.address, Object.assign({}, opts, {gas: 0.2e6}));

  assignContract(fifsResolvingRegistrar, "FIFSResolvingRegistrar", "aragon/fifs-resolving-registrar");
}

async function createRepo(repoName, contractAddress, ipfsHash, opts) {
  const publicResolver = await PublicResolver.deployed();
  const apmRegistryAddress = await publicResolver.addr(namehash.hash("aragonpm.eth"));
  const apmRegistry = await APMRegistry.at(apmRegistryAddress);
  await apmRegistry.newRepoWithVersion(repoName, opts.from, [1,0,0], contractAddress, ipfsHash, Object.assign({}, opts, {gas: 4e6}));
}

async function deploy_Voting(deployer, opts) {
  console.log("Deploying Aragon Voting");

  await deployer.deploy(Voting, Object.assign({}, opts, {gas: 5.6e6}));
  const voting = await Voting.deployed();

  console.log("Creating voting.aragonpm.eth repo");
  await createRepo("voting", voting.address, aragonVotingIPFS, opts);

  assignContract(voting, "Voting", "aragon/voting");
}

async function deploy_Vault(deployer, opts) {
  console.log("Deploying Aragon Vault");

  await deployer.deploy(Vault, Object.assign({}, opts, {gas: 3e6}));
  const vault = await Vault.deployed();

  console.log("Creating vault.aragonpm.eth repo");
  await createRepo("vault", vault.address, aragonVaultIPFS, opts);

  assignContract(vault, "Vault", "aragon/vault");
}

async function deploy_Finance(deployer, opts) {
  console.log("Deploying Aragon Finance");

  await deployer.deploy(Finance, Object.assign({}, opts, {gas: 8.9e6}));
  const finance = await Finance.deployed();

  console.log("Creating finance.aragonpm.eth repo");
  await createRepo("finance", finance.address, aragonFinanceIPFS, opts);

  assignContract(finance, "Finance", "aragon/finance");
}

async function getDAOFactory() {
  if (parameters.DAOFactory) {
    return DAOFactory.at(parameters.DAOFactory);
  } else {
    return DAOFactory.deployed();
  }
}

async function getENS() {
  if (parameters.ENS) {
    return ENS.at(parameters.ENS);
  } else {
    return ENS.deployed();
  }
}

async function getFIFSResolvingRegistrar() {
  if (parameters.FIFSResolvingRegistrar) {
    return FIFSResolvingRegistrar.at(parameters.FIFSResolvingRegistrar);
  } else {
    return FIFSResolvingRegistrar.deployed();
  }
}

async function deploy_KitDistrict(deployer, opts) {
  console.log("Deploying KitDistrict");

  const daoFactory = await getDAOFactory();
  const ens = await getENS();
  const fifsResolvingRegistrar = await getFIFSResolvingRegistrar();
  const includeApps = kitDistrictAppsToNum(parameters.KitDistrict.includeApps);

  await deployer.deploy(KitDistrict, daoFactory.address, ens.address, fifsResolvingRegistrar.address, includeApps, Object.assign({}, opts, {gas: 4.9e6}));
  const kitDistrict = await KitDistrict.deployed();

  console.log("Setting authority of KitDistrict to DSGuard");
  const dsGuard = await DSGuard.deployed();
  await kitDistrict.setAuthority(dsGuard.address, Object.assign({}, opts, {gas: 0.5e6}));

  assignContract(kitDistrict, "KitDistrict", "kit-district");
}

async function deploy_District0xEmails(deployer, opts) {
  var district0xEmails;

  if (parameters.District0xEmails) {
    console.log("Using existing District0xEmails");
    district0xEmails = await District0xEmails.at(parameters.District0xEmails);
  } else {
    console.log("Deploying District0xEmails");
    await deployer.deploy(District0xEmails, Object.assign({}, opts, {gas: 2e6}));
    district0xEmails = await District0xEmails.deployed();
  }

  assignContract(district0xEmails, "District0xEmails", "district0x-emails");
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

async function deploy_Aragon(deployer, opts) {
  if (!parameters.DAOFactory) {
    await deploy_ACL(deployer, opts);
    await deploy_EVMScriptRegistryFactory(deployer, opts);
    await deploy_Kernel(deployer, opts);
    await deploy_DAOFactory(deployer, opts);

    await deploy_ENSSubdomainRegistrar(deployer, opts);
    await deploy_FIFSResolvingRegistrar(deployer, opts);

    await deploy_Repo(deployer, opts);
    await deploy_APMRegistry(deployer, opts);
    await deploy_APMRegistryFactory(deployer, opts);

    await deploy_Voting(deployer, opts);
    await deploy_Vault(deployer, opts);
    await deploy_Finance(deployer, opts);
  }
}

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

  await deploy_Power(deployer, opts);
  await deploy_StakeBank(deployer, opts);
  await deploy_Challenge(deployer, opts);
  await deploy_DistrictChallenge(deployer, opts);

  await deploy_ENS(deployer, opts);
  await deploy_PublicResolver(deployer, opts);

  await deploy_Aragon(deployer, opts);

  await deploy_KitDistrict(deployer, opts);

  await deploy_District(deployer, opts);
  await deploy_ParamChange(deployer, opts);

  await deploy_DistrictFactory(deployer, opts);
  await deploy_ParamChangeFactory(deployer, opts);

  await deploy_District0xEmails(deployer, opts);

  writeSmartContracts(smartContractsPath, smartContractsList, env)
}


//
// Smart Contract Functions
//


function assignContract(contract_instance, contractName, contract_key, opts) {
  console.log("- Assigning '" + contractName + "' to smart contract listing...");
  opts = opts || {};
  smartContractsList = smartContractsList.concat(
    encodeContractEDN(contract_instance, contractName, contract_key, opts));
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
  console.log("@@@ using parameters", parameters);

  try {
    await deployAll(deployer, opts);
    console.log("District Registry Deployment Finished!");
  }
  catch(error) {
    console.error("ERROR: There was a problem during deployment");
    console.error(error);
  }
};
