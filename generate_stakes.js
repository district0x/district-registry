const {copy, smartContractsTemplate, encodeContractEDN, linkBytecode, readSmartContractsFile, writeSmartContracts, getSmartContractAddress, setSmartContractAddress, copyContract} = require("./migrations/utils.js");
const {env, smartContractsPath, parameters} = require("./truffle.js");
const fs = require ('fs');
const BN = require('bn.js');

function requireContract(contractName, contractCopyName) {
  return artifacts.require(copyContract(contractName, contractCopyName));
}

let DNT = requireContract("District0xNetworkToken");
let DistrictFactory = requireContract("DistrictFactory");

// -- PARAMS-- //

// npx truffle exec ./plot_curves.js --network ganache
module.exports = async function(callback) {

  var smartContracts = readSmartContractsFile(smartContractsPath);

  var dnt = await DNT.at(getSmartContractAddress(smartContracts, ":DNT"));

  var districtFactory = await DistrictFactory.at(getSmartContractAddress(smartContracts, ":district-factory"));

  // console.log ("@@@ using Web3 version:", web3.version);
  var abi = JSON.parse(fs.readFileSync('./resources/public/contracts/build/District.json')).abi;


  var firstAccountAddress='0x4c3F13898913F15F12F902d6480178484063A6Fb';
  const district = new web3.eth.Contract(abi, "0x6652ab7df38d53220bd902c0d91378fa437ce858");


  var n = 1;
  var i;
  for (i = 0; i < n; i++) {


    // returns the amount of continuous token you get for the depositAmount of connector token
    try
    {

      var deposit = web3.utils.toHex(parameters.districtRegistryDb.deposit);

      var depositAmount = new BN(10);//new BN(web3.utils.toWei(String(Math.floor(Math.random() * 20) + 1), "ether"));
      var extraData = await district.methods.stakeFor(firstAccountAddress,web3.utils.toHex(depositAmount)).encodeABI();

      await dnt.approveAndCall(districtFactory.address, deposit, extraData, {from: firstAccountAddress, gas: 10000000});

      // var result = await stakeBank.methods.calculatePurchaseReturn(tokenSupply,connectorBalance, connectorWeight,depositAmount).call({from: '0x4c3F13898913F15F12F902d6480178484063A6Fb'});
      console.log("Stake Id: " + result);


    }catch (e){
      console.log(e);
    }

  }

  callback ();
}
