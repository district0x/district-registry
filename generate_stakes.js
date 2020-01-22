const { copy, smartContractsTemplate, encodeContractEDN, linkBytecode, readSmartContractsFile, writeSmartContracts, getSmartContractAddress, setSmartContractAddress, copyContract } = require("./migrations/utils.js");
const { env, smartContractsPath, parameters } = require("./truffle.js");
const fs = require ('fs');
const BN = require('bn.js');

const smartContracts = readSmartContractsFile(smartContractsPath);

// -- PARAMS-- //

const firstAccountAddress = '0x4c3F13898913F15F12F902d6480178484063A6Fb';
const MAX_WEIGHT = 1000000;
// const connectorWeight =  333333;
const connectorWeight =  500000;
//const connectorWeight =  MAX_WEIGHT;

async function contractEventInTx (contract, event, transactionHash) {
  var txReceipt = await web3.eth.getTransactionReceipt(transactionHash);
  var eventInterface = contract._jsonInterface.filter(element => element.name == event) [0];
  var eventLog = txReceipt.logs.filter (element => element.topics [0] == eventInterface.signature) [0];

  return web3.eth.abi.decodeLog(eventInterface.inputs, eventLog.data, eventLog.topics);
}

// npx truffle exec ./generate_stakes.js --network ganache
module.exports = async function(callback) {
  try {

    const registry = new web3.eth.Contract(JSON.parse(fs.readFileSync('./resources/public/contracts/build/Registry.json')).abi,
                                           getSmartContractAddress(smartContracts, ":district-registry-fwd"));

    const dnt = new web3.eth.Contract(JSON.parse(fs.readFileSync('./resources/public/contracts/build/District0xNetworkToken.json')).abi,
                                      getSmartContractAddress(smartContracts, ":DNT"));

    const districtFactory = new web3.eth.Contract(JSON.parse(fs.readFileSync('./resources/public/contracts/build/DistrictFactory.json')).abi,
                                                  getSmartContractAddress(smartContracts, ":district-factory"));

    const districtId = Math.random().toString(36).replace(/[^a-z]+/g, '').substr(0, 5);

    var extraData = districtFactory.methods.createDistrict(firstAccountAddress, "0x0", web3.utils.toHex(connectorWeight), districtId).encodeABI();

    var createTx = await dnt.methods.approveAndCall(districtFactory._address,
                                                    web3.utils.toHex(parameters.districtRegistryDb.deposit),
                                                    extraData)
        .send ({from: firstAccountAddress, gas: 10000000});

    var districtAddress = (await contractEventInTx (registry, "DistrictConstructedEvent", createTx.transactionHash)).registryEntry;

    console.log("DEBUG: " +  districtAddress);

    const district = new web3.eth.Contract(JSON.parse(fs.readFileSync('./resources/public/contracts/build/District.json')).abi, districtAddress);

    var n = 0;
    let i;
    let staked = new BN("0");

    for (i = 0; i < n; i++) {

      console.log("Currently staked: " + staked);
      var isStake = Math.random() >= 0.5;

      if (i == 0 || isStake == true) {

        var depositAmount = new BN(web3.utils.toWei(String(Math.floor(Math.random() * 20) + 1), "ether"));

        console.log("DEBUG: " + depositAmount);

        var extraData = await district.methods.stakeFor(firstAccountAddress, web3.utils.toHex(depositAmount)).encodeABI();

        console.log("DEBUG: " + JSON.stringify (extraData));

        stakeTx = await dnt.methods.approveAndCall(districtAddress, depositAmount, extraData)
          .send ({from: firstAccountAddress, gas: 10000000});

        staked = staked.add(depositAmount);
        console.log("Stake tx: " + stakeTx);

      } else {

        var unstakeAmount = new BN(web3.utils.toWei(String(BN.min(staked, Math.floor(Math.random() * 20) + 1)), "ether"));
        console.log("Unstaking " + unstakeAmount);

        unstakeTx = await district.methods.unstake (web3.utils.toHex (unstakeAmount)).send ({from: firstAccountAddress, gas: 10000000});

        staked = staked.sub(unstakeAmount);
        console.log("Unstake tx: " + unstakeTx);
      }

    }

  } catch (e) {
    console.log(e);
  }

  callback ();
}
