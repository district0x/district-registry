pragma solidity ^0.4.18;
// DistrictFactory
import "RegistryEntryFactory.sol";
import "District.sol";

/**
 * @title Factory contract for creating District contracts
 *
 * @dev Users submit new districts into this contract.
 */

contract DistrictFactory is RegistryEntryFactory {
  uint public constant version = 1;
  District public districtToken;

  function DistrictFactory(Registry _registry, MiniMeToken _registryToken, District _districtToken)
  RegistryEntryFactory(_registry, _registryToken)
  {
    districtToken = _districtToken;
  }

  /**
   * @dev Creates new District forwarder contract and add it into the registry
   * It initializes forwarder contract with initial state. For comments on each param, see District::construct
   */
  function createDistrict(
    address _creator,
    bytes _infoHash
  )
  public
  {
    District district = District(createRegistryEntry(_creator));

    district.construct(
      _creator,
      version,
      _infoHash
    );
  }
  
}

