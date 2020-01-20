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

// npx truffle exec ./generate_stakes.js --network ganache
module.exports = async function(callback) {

  var smartContracts = readSmartContractsFile(smartContractsPath);

  var dnt = await DNT.at(getSmartContractAddress(smartContracts, ":DNT"));

  var districtFactory = await DistrictFactory.at(getSmartContractAddress(smartContracts, ":district-factory"));

  // console.log ("@@@ using Web3 version:", web3.version);
  var abi = JSON.parse(fs.readFileSync('./resources/public/contracts/build/District.json')).abi;

  var firstAccountAddress='0x4c3F13898913F15F12F902d6480178484063A6Fb';
  const districtAddress="0xe7014fa4a390e67e873664cd43e042d2f5a4fbf8";
  const district = new web3.eth.Contract(abi, districtAddress);

  var depositAmount;// = new BN(web3.utils.toWei(String(Math.floor(Math.random() * 20) + 1), "ether"));

  var n = 10;
  var i;
  for (i = 0; i < n; i++) {
    try {

      var isStake = Math.random() >= 0.5;

      if (i == 0 || isStake == true) {
        depositAmount = new BN(web3.utils.toWei(String(Math.floor(Math.random() * 20) + 1), "ether"));
        var extraData = await district.methods.stakeFor(firstAccountAddress, web3.utils.toHex(depositAmount)).encodeABI();

        stakeTx = await dnt.approveAndCall(districtAddress, depositAmount, extraData, {from: firstAccountAddress, gas: 10000000});

        console.log("Stake tx: " + stakeTx);
      } else {

        unstakeTx = await district.methods.unstake (web3.utils.toHex (depositAmount)).send ({from: firstAccountAddress, gas: 10000000});

        console.log("Unstake tx: " + unstakeTx);
      }

    } catch (e) {
      console.log(e);
    }

  }

  callback ();
}
