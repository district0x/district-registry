const {copyContract, assignContract, encodeContractEDN, writeSmartContracts} = require("./utils.js");
const {env, parameters} = require("../truffle.js");

function requireContract(contractName, contractCopyName) {
  return artifacts.require(copyContract(contractName, contractCopyName));
}

// The Backroom Contracts
let Council = requireContract("Council");
let CouncilStakeBank = requireContract("CouncilStakeBank");
let ARIOracle = requireContract("ARIOracle");
let PowerPlant = requireContract("PowerPlant");

// Existing contracts we need
let DNT = requireContract("District0xNetworkToken");
let DistrictRegistry = requireContract("Registry", "DistrictRegistry");

let smartContractsList = [];

async function deploy_CouncilStakeBank(deployer, opts) {
  console.log("Deploying CouncilStakeBank");
  await deployer.deploy(CouncilStakeBank, Object.assign({}, opts, {gas: 4e6}));
  const councilStakeBank = await CouncilStakeBank.deployed();

  console.log("- Calling construct() on CouncilStakeBank");
  await councilStakeBank.construct(Object.assign({}, opts, {gas: 1e6}));

  assignContract(councilStakeBank, "CouncilStakeBank", "council-stake-bank");
}

async function deploy_ARIOracle(deployer, opts) {
  console.log("Deploying ARIOracle");
  const dnt = await DNT.deployed();

  await deployer.deploy(ARIOracle, dnt.address, Object.assign({}, opts, {gas: 4e6}));
  const ariOracle = await ARIOracle.deployed();

  console.log("- Calling construct() on ARIOracle");
  // Default: 3 day veto period, 30% threshold
  const vetoPeriodDuration = 3 * 24 * 60 * 60; // 3 days in seconds
  const vetoThresholdBps = 3000; // 30%
  await ariOracle.construct(
    vetoPeriodDuration,
    vetoThresholdBps,
    Object.assign({}, opts, {gas: 1e6})
  );

  assignContract(ariOracle, "ARIOracle", "ari-oracle");
}

async function deploy_PowerPlant(deployer, opts) {
  console.log("Deploying PowerPlant");
  const ariOracle = await ARIOracle.deployed();

  await deployer.deploy(PowerPlant, ariOracle.address, Object.assign({}, opts, {gas: 4e6}));
  const powerPlant = await PowerPlant.deployed();

  console.log("- Calling construct() on PowerPlant");
  await powerPlant.construct(Object.assign({}, opts, {gas: 1e6}));

  assignContract(powerPlant, "PowerPlant", "power-plant");
}

async function deploy_Council(deployer, opts) {
  console.log("Deploying Council");
  const councilStakeBank = await CouncilStakeBank.deployed();
  const ariOracle = await ARIOracle.deployed();

  await deployer.deploy(Council, Object.assign({}, opts, {gas: 4e6}));
  const council = await Council.deployed();

  console.log("- Calling construct() on Council (initializing 9 seats)");
  await council.construct(
    councilStakeBank.address,
    ariOracle.address,
    Object.assign({}, opts, {gas: 3e6})
  );

  // Set Council as the owner of CouncilStakeBank
  console.log("- Setting Council as owner of CouncilStakeBank");
  await councilStakeBank.changeOwner(council.address, Object.assign({}, opts, {gas: 0.5e6}));

  // Set PowerPlant address in ARIOracle
  const powerPlant = await PowerPlant.deployed();
  console.log("- Setting PowerPlant address in ARIOracle");
  await ariOracle.setPowerPlant(powerPlant.address, Object.assign({}, opts, {gas: 0.5e6}));

  assignContract(council, "Council", "council");
}

module.exports = function(deployer, network, accounts) {
  const address = accounts[0];
  const gas = 4e6;
  const opts = {from: address, gas: gas};

  deployer.then(async () => {
    console.log("\n===============================================");
    console.log("DEPLOYING THE BACKROOM CONTRACTS");
    console.log("Network:", network);
    console.log("Address:", address);
    console.log("===============================================\n");

    // Deploy in order of dependencies
    await deploy_CouncilStakeBank(deployer, opts);
    await deploy_ARIOracle(deployer, opts);
    await deploy_PowerPlant(deployer, opts);
    await deploy_Council(deployer, opts);

    console.log("\n===============================================");
    console.log("THE BACKROOM DEPLOYMENT COMPLETE");
    console.log("===============================================");
    console.log("Council:", (await Council.deployed()).address);
    console.log("CouncilStakeBank:", (await CouncilStakeBank.deployed()).address);
    console.log("ARIOracle:", (await ARIOracle.deployed()).address);
    console.log("PowerPlant:", (await PowerPlant.deployed()).address);
    console.log("===============================================\n");

    // Write smart contracts to EDN files for ClojureScript
    await writeSmartContracts(
      smartContractsList,
      "src/district_registry/shared/smart_contracts_dev.cljs",
      env
    );
  });
};
