const {copy, smartContractsTemplate, encodeContractEDN, linkBytecode} = require("./utils.js");
const fs = require("fs");
const {env, contracts_build_directory, smart_contracts_path, parameters} = require("../truffle.js");


const DNT = artifacts.require("District0xNetworkToken_copy");
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

  const dnt = await DNT.deployed();

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
