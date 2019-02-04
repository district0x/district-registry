pragma solidity ^0.4.24;

import "Power.sol";

contract PowerFactory {
  function createPower()
    public returns (address)
  {
    address power = new Power();
    return power;
  }
}
