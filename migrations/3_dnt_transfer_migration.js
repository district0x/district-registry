const {env, smartContractsPath, parameters} = require("../truffle.js");
const {copyContract, linkBytecode, encodeSmartContracts, writeSmartContracts, readSmartContractsFile, getSmartContractAddress, kitDistrictAppsToNum} = require("./utils.js");

function requireContract(contractName, contractCopyName) {
  return artifacts.require(copyContract(contractName, contractCopyName));
}

const DNT = requireContract("District0xNetworkToken");
const amount = "10000000000000000000000"

/**
 * This migration transfers DNT to the all ganache accounts for development purposes
 *
 * Usage:
 * truffle migrate --network ganache/parity --reset --f 3 --to 3
 */


module.exports = async function(deployer, network, accounts) {
  const address = accounts[0];
  const gas = 0.2e6;
  const opts = {gas: gas, from: address};

  console.log("DNT Tansfer Migration Started...");

  await deployer;
  console.log("@@@ using Web3 version:", web3.version);
  console.log("@@@ using address", address);

  var smartContracts = readSmartContractsFile(smartContractsPath);
  var dntAddr = getSmartContractAddress(smartContracts, ":DNT");
  const dnt = await DNT.at(dntAddr);

  // First one already owns DNT
  accounts.shift();

  try {
    for(var i in accounts) {
      console.log("Transferring DNT to " + accounts[i]);
      await dnt.transfer(accounts[i], amount, opts);
    }
    console.log("DNT successfully transferred!");
  }
  catch(error) {
    console.error("ERROR: There was a problem during DNT transfer");
    console.error(error);
  }
};
