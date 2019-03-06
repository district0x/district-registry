pragma solidity ^0.4.18;

import "./RegistryEntryFactory.sol";
import "./District.sol";

interface DemocracyKit {
  function newInstance(
    string name,
    address[] holders,
    uint256[] tokens,
    uint64 supportNeeded,
    uint64 minAcceptanceQuorum,
    uint64 voteDuration,
    address _token
  ) public;

  function foo() public;
}

/**
 * @title Factory contract for creating District contracts
 *
 * @dev Users submit new districts into this contract.
 */

contract DistrictFactory is RegistryEntryFactory {

  // DemocracyKit private constant democracyKit = DemocracyKit(0xaAaAaAaaAaAaAaaAaAAAAAAAAaaaAaAaAaaAaaAa);
  DemocracyKit private constant democracyKit = DemocracyKit(0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa);

  // DemocracyKit private constant democracyKit = DemocracyKit(0xd02a4b49c53ed1b386df1015583a4c4e2e70a4fe );



  uint public constant version = 1;

  function DistrictFactory(Registry _registry, MiniMeToken _registryToken)
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
    uint32 _dntWeight
  )
    public
  {
    District district = District(createRegistryEntry(_creator));
    district.construct(
      _creator,
      version,
      _metaHash,
      _dntWeight
    );
    address[] holders;
    uint256[] tokens;
    address mmt = address(district.stakeBank());
    // require(address(democracyKit) == 0xd02a4b49c53ed1b386df1015583a4c4e2e70a4fe );
    democracyKit.foo();

    // democracyKit.newInstance(
    //   "FooBar",
    //   holders,
    //   tokens,
    //   uint64(100),
    //   uint64(100),
    //   uint64(100),
    //   mmt
    // );

  }
}

