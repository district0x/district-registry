// npx truffle console --network ganache

// var contractAddress = "0xc2de159ca5175da01a972c3a7e41ec0579a62948";

// var abi = JSON.parse(fs.readFileSync('./resources/public/contracts/build/StakeBank.json')).abi;
// const contractInstance = new web3.eth.Contract(abi, contractAddress);

// var connectorWeight = 1000000;

// contructor is never called in the deploy script, call it once before executing the scrip:
// contractInstance.methods.construct(connectorWeight).send({from: '0x4c3F13898913F15F12F902d6480178484063A6Fb', gas: 5.2e6}).then( (receipt) => console.log (receipt) );

// confirm:
// contractInstance.methods.MAX_WEIGHT().call();

const fs = require ('fs');
const BN = require('bn.js');


// const StakeBank = artifacts.require("StakeBank");
const contractAddress = "0x24c51375f85def94f73c65701d4a2a14010ae0c7";

// -- PARAMS-- //

// parameter between 1  - 1e6
const MAX_WEIGHT = 1000000;

//const connectorWeight =  333333;
const connectorWeight =  500000;
// const connectorWeight =  MAX_WEIGHT;
const init_tokenSupply = new BN(web3.utils.toWei("10", "ether"));
const init_connectorBalance = new BN(web3.utils.toWei("1", "ether"));

// npx truffle exec ./plot_curves.js --network ganache
module.exports = async function(callback) {

  console.log ("@@@ using Web3 version:", web3.version);

  var abi = JSON.parse(fs.readFileSync('./resources/public/contracts/build/StakeBank.json')).abi;
  const stakeBank = new web3.eth.Contract(abi, contractAddress);
  // const stakeBank = StakeBank.at (contractAddress);

  var n = 1;
  var tokenSupply = init_tokenSupply;
  var connectorBalance = init_connectorBalance;

  var i;
  for (i = 0; i < n; i++) {



    // returns the amount of continuous token you get for the depositAmount of connector token
    try
    {
      // random 1 - 200, denominated in connector token
      var depositAmount = new BN(web3.utils.toWei(String(Math.floor(Math.random() * 10) + 1),"milliether"));

      console.log("Calling with  supply " + tokenSupply +  " balance " +  connectorBalance + " weight" + connectorWeight + " deposit " + depositAmount);
      var result = await debug(stakeBank.methods.calculatePurchaseReturn(web3.utils.toHex(tokenSupply), web3.utils.toHex(connectorBalance), connectorWeight,web3.utils.toHex(depositAmount)).call({from: '0x4c3F13898913F15F12F902d6480178484063A6Fb'}));
      result = new BN(result);

      var price = result.div(depositAmount);
      // console.log (web3.utils.fromWei(tokenSupply) + "   " + web3.utils.fromWei(price));
      console.log (tokenSupply + "   " + price);

      tokenSupply = tokenSupply.add(result);
      connectorBalance = connectorBalance.add(depositAmount);

    }catch (e){
      console.log(e);
    }

  }

  callback ();
}
