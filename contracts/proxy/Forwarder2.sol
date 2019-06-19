pragma solidity ^0.4.18;

import "../proxy/DelegateProxy.sol";

contract Forwarder2 is DelegateProxy {
  // After compiling contract, `beefbeef...` is replaced in the bytecode by the real target address
  address public constant target = 0xFEebfEebfeEBfeEBFEeBFEebfEeBFeeBfEebFeeb; // checksumed to silence warning

  /*
  * @dev Forwards all calls to target
  */
  function() public payable {
    delegatedFwd(target, msg.data);
  }
}