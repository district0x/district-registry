pragma solidity ^0.4.18;

import "./RegistryEntry.sol"; 
// import "proxy/Forwarder.sol"; 
import "token/erc20/MintableToken.sol";
import "./District0xNetworkToken.sol";
import "token/erc900/TokenReturningStakeBank.sol";
/* import "./DistrictConfig.sol"; */

/**
 * @title Contract created for each submitted District into the DistrictFactory TCR.
 *
 * @dev It extends base RegistryEntry with additional state for storing IPFS hashes for a district data.
 * Full copy of this contract is NOT deployed with each submission in order to save gas. Only forwarder contracts
 * pointing into single intance of it.
 */

contract District is RegistryEntry, MintableToken, TokenReturningStakeBank 
{




  /* string public constant name = "District Token"; */
  /* string public constant symbol = "DT"; */
  /* uint8 public constant decimals = 18; */
  bytes public infoHash; // state variable for storing IPFS hash of file that contains all data from form fields


  /**
   * @dev Constructor for this contract.
   * Native constructor is not used, because users create only forwarders pointing into single instance of this contract,
   * therefore constructor must be called explicitly.

   * @param _creator Creator of a district
   * @param _version Version of District contract
   * @param _infoHash IPFS hash of data related to a district
   */
  function construct(
    address _creator,
    uint _version,
    bytes _infoHash
  )
  public
  {
    super.construct(_creator, _version);
    super.constructStakeBank(registryToken,this,1);

    infoHash = _infoHash;
  }

  // function setInfoHash(bytes _infoHash) public
  // {
  //   require(msg.sender == creator);
  //   infoHash = _infoHash;
  // }

}
