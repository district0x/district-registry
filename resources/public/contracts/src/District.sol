pragma solidity ^0.4.18;

import "RegistryEntry.sol";
import "proxy/Forwarder.sol";
import "token/erc20/MintableToken.sol";
import "token/erc900/TokenReturningStakeBank.sol";
/* import "./DistrictConfig.sol"; */

/**
 * @title Contract created for each submitted District into the DistrictFactory TCR.
 *
 * @dev It extends base RegistryEntry with additional state for storing IPFS hashes for a district data.
 * Full copy of this contract is NOT deployed with each submission in order to save gas. Only forwarder contracts
 * pointing into single intance of it.
 */

contract District is RegistryEntry, MintableToken, TokenReturningStakeBank {
  string public constant name = "District Token";
  string public constant symbol = "DT";
  uint8 public constant decimals = 18;
  bytes public infoHash; // state variable for storing IPFS hash of file that contains all data from form fields

  /* DistrictConfig public constant districtConfig = DistrictConfig(0xABCDabcdABcDabcDaBCDAbcdABcdAbCdABcDABCd); */
  /* bytes32 public constant maxTotalSupplyKey = sha3("maxTotalSupply"); */
  /* DistrictToken public constant districtToken = DistrictToken(0xdaBBdABbDABbDabbDaBbDabbDaBbdaBbdaBbDAbB); */
  /* bytes public metaHash; */
  /* uint public tokenIdStart; */
  /* uint public totalSupply; */
  /* uint public totalMinted; */


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
    infoHash = _infoHash;
  }

  function setInfoHash(bytes _infoHash) public
  {
    require(msg.sender == creator);
    infoHash = _infoHash;
  }

  /**
   * @dev Transfers deposit to deposit collector
   * Must be callable only for whitelisted unchallenged registry entries
   */
  /* function transferDeposit() */
  /* public */
  /* notEmergency */
  /* onlyWhitelisted */
  /* { */
  /*   require(!wasChallenged()); */
  /*   /\* require(registryToken.transfer(districtConfig.depositCollector(), deposit)); *\/ */
  /*   registry.fireRegistryEntryEvent("depositTransferred", version); */
  /* } */

  /* function mint(uint _amount) */
  /* public */
  /* notEmergency */
  /* onlyWhitelisted */
  /* { */
  /*   uint restSupply = totalSupply.sub(totalMinted); */
  /*   if (_amount == 0 || _amount > restSupply) { */
  /*     _amount = restSupply; */
  /*   } */
  /*   require(_amount > 0); */
  /*   /\* tokenIdStart = districtToken.totalSupply().add(1); *\/ */
  /*   uint tokenIdEnd = tokenIdStart.add(_amount); */
  /*   for (uint i = tokenIdStart; i < tokenIdEnd; i++) { */
  /*     /\* districtToken.mint(creator, i); *\/ */
  /*     totalMinted = totalMinted + 1; */
  /*   } */
  /*   var eventData = new uint[](3); */
  /*   eventData[0] = uint(creator); */
  /*   eventData[1] = tokenIdStart; */
  /*   eventData[2] = tokenIdEnd - 1; */
  /*   registry.fireRegistryEntryEvent("minted", version, eventData); */
  /* } */

  /**
   * @dev Returns all state related to this contract for simpler offchain access
   */
  /* function loadDistrict() public constant returns (bytes, uint, uint, uint) { */
  /*   return ( */
  /*   metaHash, */
  /*   totalSupply, */
  /*   totalMinted, */
  /*   tokenIdStart */
  /*   ); */
  /* } */

}
