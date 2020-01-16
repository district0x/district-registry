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
// const StakeBank = artifacts.require("StakeBank");
const contractAddress = "0xc2de159ca5175da01a972c3a7e41ec0579a62948";

// -- PARAMS-- //

// parameter between 1  - 1e6
const connectorWeight = 1000000;
const init_tokenSupply = 1000;
const init_connectorBalance = 100;

// npx truffle exec ./plot_curves.js --network ganache
module.exports = async function(callback) {

  console.log ("@@@ using Web3 version:", web3.version);

  var abi = JSON.parse(fs.readFileSync('./resources/public/contracts/build/StakeBank.json')).abi;
  const stakeBank = new web3.eth.Contract(abi, contractAddress);
  // const stakeBank = StakeBank.at (contractAddress);

  var n = 100;
  var tokenSupply = new Array(n).fill(null);
  var connectorBalance = new Array(n).fill(null);
  var tokenPrice = new Array(n).fill(null);

  tokenSupply [0] = init_tokenSupply;
  connectorBalance [0] = init_connectorBalance;
  tokenPrice [0] = init_connectorBalance / (init_tokenSupply * connectorWeight)

  var i;
  for (i = 0; i < n; i++) {

    // random 1 - 200, denominated in connector token
    var depositAmount = Math.floor(Math.random() * 200) + 1;

    var currentTokenSupply = tokenSupply [i];
    var currentConnectorBalance = connectorBalance [i];

    console.log ("@@@ ARGS", currentTokenSupply);

    // returns the amount of continuous token you get for the depositAmount of connector token
    var result = await stakeBank.methods.calculatePurchaseReturn(currentTokenSupply, currentConnectorBalance, connectorWeight, depositAmount).call({from: '0x4c3F13898913F15F12F902d6480178484063A6Fb'});

    console.log ("@@@ RES", result);

    // price is denominated in connector tokens
    var currentPrice = depositAmount / result;
    console.log ("You receive " + result + " continuous tokens for the price of " + currentPrice);

    tokenSupply [i + 1] = result;
    connectorBalance [i + 1] = depositAmount;
    tokenPrice [i + 1] = currentPrice;

  }

  callback ();
}
