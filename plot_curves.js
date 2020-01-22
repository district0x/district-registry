const { readSmartContractsFile, getSmartContractAddress } = require("./migrations/utils.js");
const { smartContractsPath } = require("./truffle.js");
const fs = require ('fs');
const BN = require('bn.js');

const smartContracts = readSmartContractsFile(smartContractsPath);
const contractAddress = getSmartContractAddress(smartContracts, ":stake-bank");

// -- PARAMS-- //
const MAX_WEIGHT = 1000000;

// -- parameter between 1  - 1e6 -- //
// const connectorWeight =  333333;
const connectorWeight =  500000;
//const connectorWeight =  MAX_WEIGHT;

const init_tokenSupply = new BN(web3.utils.toWei("10", "ether"));
const init_connectorBalance = new BN(web3.utils.toWei("1", "ether"));

// npx truffle exec ./plot_curves.js --network ganache
module.exports = async function(callback) {

  try {

    var abi = JSON.parse(fs.readFileSync('./resources/public/contracts/build/StakeBank.json')).abi;
    const stakeBank = new web3.eth.Contract(abi, contractAddress);

    var weight = await stakeBank.methods.MAX_WEIGHT().call({from: '0x4c3F13898913F15F12F902d6480178484063A6Fb', gas: 5.2e6});
    if (weight == 0) { // if not initialized
      console.log ("init");
      var tx = await stakeBank.methods.construct(connectorWeight).send({from: '0x4c3F13898913F15F12F902d6480178484063A6Fb', gas: 5.2e6});
    }

    var n = 100;
    var tokenSupply = init_tokenSupply;
    var connectorBalance = init_connectorBalance;

    var i;
    for (i = 0; i < n; i++) {
      var isStake = Math.random() >= 0.5;
      if (i == 0 || isStake == true) {

        var depositAmount = new BN(web3.utils.toWei(String(Math.floor(Math.random() * 20) + 1), "ether"));

        // returns the amount of continuous token you get for the depositAmount of connector token
        var result = await stakeBank.methods.calculatePurchaseReturn(web3.utils.toHex(tokenSupply),
                                                                     web3.utils.toHex(connectorBalance),
                                                                     web3.utils.toHex(connectorWeight),
                                                                     web3.utils.toHex(depositAmount))
            .call({from: '0x4c3F13898913F15F12F902d6480178484063A6Fb'});

        result = new BN(result);

        // console.log ("Buying: " + result + " wei of continuous token for " + depositAmount + " wei of connector token");

        var price = web3.utils.fromWei(depositAmount) / web3.utils.fromWei(result);

        console.log (tokenSupply + ", " + price);

        tokenSupply = tokenSupply.add(result);
        connectorBalance = connectorBalance.add(depositAmount);

        // console.log ("continuousTokenSupply: " + tokenSupply + " connectorTokenBalance: " + connectorBalance);

      } else {

        // unstaking

        // TODO : random
        // var saleAmount = new BN(web3.utils.toWei("1"));

        var rand = new BN (web3.utils.toWei (String(Math.floor(Math.random() * 20) + 1), "ether"));
        var saleAmount = BN.min(tokenSupply, rand);

        // returns the amount of connector token you get for the saleAmount of continuous token
        var result = await stakeBank.methods.calculateSaleReturn(web3.utils.toHex(tokenSupply),
                                                                 web3.utils.toHex(connectorBalance),
                                                                 web3.utils.toHex(connectorWeight),
                                                                 web3.utils.toHex(saleAmount))
            .call({from: '0x4c3F13898913F15F12F902d6480178484063A6Fb'});

        result = new BN(result);
        tokenSupply = tokenSupply.sub(saleAmount);
        connectorBalance = connectorBalance.sub(result);

        // console.log ("Selling: " + saleAmount + " wei of continuous token for " + result + " wei of connector token");

        var price = web3.utils.fromWei(result) / web3.utils.fromWei(saleAmount);

        console.log (tokenSupply + ", " + price);

        // console.log ("continuousTokenSupply: " + tokenSupply + " connectorTokenBalance: " + connectorBalance);

      }


    }
  } catch (e) {
    console.log(e);
  }

  callback ();
}
