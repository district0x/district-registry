pragma solidity ^0.4.18;

import  "./ForwarderDelegateProxy.sol";

contract Forwarder2 is ForwarderDelegateProxy {
  // After compiling contract, `beefbeef...` is replaced in the bytecode by the real target address
  address public constant target = 0xFEebfEebfeEBfeEBFEeBFEebfEeBFeeBfEebFeeb; // checksumed to silence warning

  /*
  * @dev Forwards all calls to target
  */
  function() public payable {
    delegatedFwd(target, msg.data);
  }
}