pragma solidity ^0.4.18;

import "./Registry.sol";
import "./ParamChange.sol";
import "./auth/DSGuard.sol";

/**
 * @title Central contract for TCR parameter change registry
 *
 * @dev Extends Registry contract with additional logic for applying parameter change into a registry EternalDb.
 */

contract ParamChangeRegistry is Registry {

  event ParamChangeConstructedEvent(address registryEntry, uint version, address creator, address db, string key, uint value, uint deposit, uint challengePeriodEnd);
  event ParamChangeAppliedEvent(address registryEntry, uint version);

  /**
   * @dev Gives ParamChange contract temporary permission to apply its parameter changes into EthernalDb
   * Only address of valid ParamChange contract can be passed
   * Permission must be taken back right after applying the change

   * @param _paramChange Address of ParamChange contract
   */

  function applyParamChange(ParamChange _paramChange) public {
    require(isRegistryEntry(_paramChange));
    DSGuard guard = DSGuard(_paramChange.db().authority());
    guard.permit(_paramChange, _paramChange.db(), guard.ANY());
    _paramChange.applyChange();
    guard.forbid(_paramChange, _paramChange.db(), guard.ANY());
  }


  function fireParamChangeConstructedEvent(
    uint version,
    address creator,
    address _db,
    string key,
    uint value,
    uint deposit,
    uint challengePeriodEnd
  )
    public
    onlyRegistryEntry
  {
    emit ParamChangeConstructedEvent(msg.sender, version, creator, _db, key, value, deposit, challengePeriodEnd);
  }

  function fireParamChangeAppliedEvent(uint version)
    public
    onlyRegistryEntry
  {
    emit ParamChangeAppliedEvent(msg.sender, version);
  }
}

