pragma solidity ^0.4.18;

import "./RegistryEntryFactory.sol";
import "./District.sol";

/**
 * @title Factory contract for creating District contracts
 *
 * @dev Users submit new districts into this contract.
 */

contract DistrictFactory is RegistryEntryFactory {
  uint public constant version = 1;

  constructor(Registry _registry, MiniMeToken _registryToken)
    public
    RegistryEntryFactory(_registry, _registryToken)
  {
  }

  /**
   * @dev Creates new District forwarder contract and add it into the registry
   * It initializes forwarder contract with initial state. For comments on each param, see District::construct
   */
  function createDistrict(
    address _creator,
    bytes _metaHash,
    uint32 _dntWeight,
    string _aragonId
  )
    public
  {
    District district = District(createRegistryEntry(_creator));
    district.construct(
      _creator,
      version,
      _metaHash,
      _dntWeight,
      _aragonId
    );
  }
}

