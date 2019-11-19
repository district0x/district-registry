var Migrations = artifacts.require("Migrations.sol");
const {env, smartContractsPath} = require("../truffle.js");
const {writeSmartContracts,getSmartContractAddress, readSmartContractsFile, setSmartContractAddress} = require("./utils.js");

module.exports = async function(deployer, network, accounts) {
  await deployer.deploy(Migrations, {gas: 1000000, from: accounts[0]});

  var smartContracts = readSmartContractsFile(smartContractsPath);
  const migrations = await Migrations.deployed();

  setSmartContractAddress(smartContracts, ":migrations", migrations.address);
  writeSmartContracts(smartContractsPath, smartContracts, env);
};
