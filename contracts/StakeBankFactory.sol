pragma solidity ^0.4.24;

import "./StakeBank.sol";

contract StakeBankFactory {
  function createStakeBank(uint32 _dntWeight)
    public returns (address)
  {
    address stakeBank = new StakeBank(msg.sender, _dntWeight);
    return stakeBank;
  }
}


