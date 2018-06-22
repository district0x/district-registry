pragma solidity ^0.4.18;
//ignore, for storing config variables in district factory for governance

import "auth/DSAuth.sol";

contract DistrictConfig is DSAuth {
  address public depositCollector;
  address public districtAuctionCutCollector;
  uint public districtAuctionCut; // Values 0-10,000 map to 0%-100%

  function DistrictConfig(address _depositCollector, address _districtAuctionCutCollector, uint _districtAuctionCut) {
    require(_depositCollector != 0x0);
    require(_districtAuctionCutCollector != 0x0);
    require(_districtAuctionCut < 10000);
    depositCollector = _depositCollector;
    districtAuctionCutCollector = _districtAuctionCutCollector;
    districtAuctionCut = _districtAuctionCut;
  }

  function setDepositCollector(address _depositCollector) public auth {
    require(_depositCollector != 0x0);
    depositCollector = _depositCollector;
  }

  function setDistrictAuctionCutCollector(address _districtAuctionCutCollector) public auth {
    require(_districtAuctionCutCollector != 0x0);
    districtAuctionCutCollector = _districtAuctionCutCollector;
  }

  function setCollectors(address _collector) public auth {
    setDepositCollector(_collector);
    setDistrictAuctionCutCollector(_collector);
  }

  function setDistrictAuctionCut(uint _districtAuctionCut) public auth {
    require(_districtAuctionCut < 10000);
    districtAuctionCut = _districtAuctionCut;
  }
}
