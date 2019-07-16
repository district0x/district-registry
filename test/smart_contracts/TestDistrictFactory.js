const {increaseTime, expectFailure} = require("./utils.js");
const {env, parameters} = require("../../truffle.js");

const DistrictFactory = artifacts.require('DistrictFactory_copy');
const DNT = artifacts.require("District0xNetworkToken_copy");
const DistrictRegistry = artifacts.require("DistrictRegistry");
const DistrictRegistryForwarder = artifacts.require("DistrictRegistryForwarder");
const DistrictRegistryDb = artifacts.require("DistrictRegistryDb");

contract('DistrictFactory', function(accounts) {

  describe('Create district', async () => {
    it("should have the shared context", async() => {
      let districtFactory = await DistrictFactory.deployed();
      let dnt = await DNT.deployed();
      let districtRegistryDb = await DistrictRegistryDb.deployed();
      let districtRegistry = await DistrictRegistry.at((await DistrictRegistryForwarder.deployed()).address);

      var metaHash = web3.utils.toHex("Qmdpe5HCmjieUaSYoedQYZkTRu7aax8719hLAFyMQhcJhD");
      var dntWeight = 1000000;
      var aragonId = "test1";
      var deposit = web3.utils.toHex(parameters.districtRegistryDb.deposit);
      var address = accounts[0];

      var extraData = districtFactory.contract.methods.createDistrict(address, metaHash, dntWeight, aragonId).encodeABI();

      await dnt.approveAndCall(districtFactory.address, deposit, extraData, {from: address, gas: 10000000});

      var balance = await dnt.balanceOf(address);

      console.log(web3.utils.fromWei(balance, "ether").toString());
      console.log(districtRegistry.address);
      console.log(districtFactory.address);
      console.log(dnt.address);

      var events = await districtRegistry.contract.getPastEvents("DistrictConstructedEvent", {fromBlock: 0});

      console.log(events);
    });
  });
});