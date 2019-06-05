pragma solidity ^0.4.24;

import "./RegistryEntry.sol";
import "./StakeBank.sol";
import "./StakeBankFactory.sol";

/**
 * @title Contract created for each submitted District into the DistrictFactory TCR.
 *
 * @dev It extends base RegistryEntry with additional state for storing IPFS hashes for a district data.
 * Full copy of this contract is NOT deployed with each submission in order to save gas. Only forwarder contracts
 * pointing into single intance of it.
 */

contract District is RegistryEntry {

  StakeBankFactory private constant stakeBankFactory = StakeBankFactory(0xDDdDddDdDdddDDddDDddDDDDdDdDDdDDdDDDDDDd);
  StakeBank private stakeBank;

  /**
   * @dev IPFS hash of file that contains all data from form fields
   */
  bytes private metaHash;

  /**
   * @dev Constructor for this contract.
   * Native constructor is not used, because users create only forwarders pointing into single instance of this contract,
   * therefore constructor must be called explicitly.

   * @param _creator Creator of a district
   * @param _version Version of District contract
   * @param _metaHash IPFS hash of data related to a district
   */
  function construct(
    address _creator,
    uint _version,
    bytes _metaHash,
    uint32 _dntWeight
  )
    public
  {
    super.construct(_creator, _version);
    stakeBank = StakeBank(stakeBankFactory.createStakeBank(_dntWeight));
    challengePeriodEnd = ~uint(0);
    metaHash = _metaHash;
    registry.fireDistrictConstructedEvent(version, creator, metaHash, deposit, challengePeriodEnd, _dntWeight);
  }

  function setMetaHash(bytes _metaHash)
    public
  {
    require(msg.sender == creator);
    metaHash = _metaHash;
  }

  /// @param _owner The address that's balance is being requested
  /// @return The balance of `_owner` at the current block
  function balanceOf(address _owner)
    public constant returns (uint256 balance)
  {
    return stakeBank.balanceOf(_owner);
  }

  /// @notice Stakes a certain amount of tokens for another user.
  /// @param _user Address of the user to stake for.
  /// @param _amount Amount of tokens to stake.
  function stakeFor(address _user, uint _amount)
    public
  {
    require(registryToken.transferFrom(_user, address(this), _amount));
    stakeBank.stakeFor(_user, _amount);
    maybeAdjustStakeDelta(_user, int(_amount));
    registry.fireDistrictStakeChangedEvent(version, stakeBank.totalStaked(), stakeBank.totalSupply(), _user, stakeBank.totalStakedFor(_user), stakeBank.balanceOf(_user));
  }

  /// @notice Unstakes a certain amount of tokens.
  /// @param _amount Amount of tokens to unstake.
  function unstake(uint _amount)
    public
  {
    stakeBank.unstake(msg.sender, _amount);
    maybeAdjustStakeDelta(msg.sender, int(_amount) * -1);
    registry.fireDistrictStakeChangedEvent(version, stakeBank.totalStaked(), stakeBank.totalSupply(), msg.sender, stakeBank.totalStakedFor(msg.sender), stakeBank.balanceOf(msg.sender));
  }

  function maybeAdjustStakeDelta(
    address _voter,
    int _amount
  )
    private
  {
    if (challenges.length != 0 && currentChallenge().isVoteCommitPeriodActive()) {
      currentChallenge().adjustStakeDelta(_voter, _amount);
    }
  }

  function isChallengeable()
    internal view returns (bool) {
    return isChallengePeriodActive() &&
      (challenges.length == 0 ||
       currentChallenge().status() == Challenge.Status.Whitelisted);
  }

}
