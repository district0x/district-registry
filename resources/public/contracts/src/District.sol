pragma solidity ^0.4.18;

import "./RegistryEntry.sol"; 
import "token/stakebank/StakeBank.sol";

/**
 * @title Contract created for each submitted District into the DistrictFactory TCR.
 *
 * @dev It extends base RegistryEntry with additional state for storing IPFS hashes for a district data.
 * Full copy of this contract is NOT deployed with each submission in order to save gas. Only forwarder contracts
 * pointing into single intance of it.
 */

contract District is RegistryEntry, MiniMeToken, StakeBank
{
  /** 
  * @dev IPFS hash of file that contains all data from form fields
  */
  bytes public infoHash;

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
    super.constructMiniMeToken(
      this,
      0x0,
      0,
      "TODO Token Name",
      18,
      "TODO",
      false 
    );
    changeController(this);
    super.constructStakeBank(registryToken);
    challengePeriodEnd = ~uint(0);

    infoHash = _infoHash;
  }

  function setInfoHash(bytes _infoHash) public
  {
    require(msg.sender == creator);
    infoHash = _infoHash;
  }

  /// @notice Stakes a certain amount of tokens for another user.
  /// @param _user Address of the user to stake for.
  /// @param _amount Amount of tokens to stake.
  /// @param _data Data field used for signalling in more complex staking applications.
  function stakeFor(address _user, uint _amount, bytes _data) 
  public 
  {
    super.stakeFor(_user, _amount, _data);
    require(generateTokens(_user, _amount));
    maybeAdjustStakeDelta(_user, int(_amount));
  }

  function stake(uint256 _amount, bytes _data) public {
    stakeFor(msg.sender, _amount, _data);
  }

  /// @notice Unstakes a certain amount of tokens.
  /// @param _amount Amount of tokens to unstake.
  /// @param _data Data field used for signalling in more complex staking applications.
  function unstake(uint _amount, bytes _data) public {
    super.unstake(_amount, _data);
    require(this.destroyTokens(msg.sender, _amount));
    maybeAdjustStakeDelta(msg.sender, int(_amount) * -1);
  }

  struct StakeDelta {
    uint creationBlock;
    int delta;
    mapping(address => int) deltas;
  }

  StakeDelta[] public stakeDeltas;

  function maybeAdjustStakeDelta(
    address _voter,
    int _amount
  ) 
  private 
  {
    if (isVoteCommitPeriodActive()) {
      uint idx = currentChallengeIndex();
      stakeDeltas[idx].delta += _amount;
      stakeDeltas[idx].deltas[_voter] += _amount;
    }
  }

  function wasChallenged() public constant returns (bool) {
    if (challenges.length == 0) {
      return false;
    }
    return currentChallenge().revealPeriodEnd > now;
  }

  function createChallenge(
    address _challenger,
    bytes _challengeMetaHash
  )
  public
  {
    super.createChallenge(_challenger, _challengeMetaHash);
    StakeDelta memory sd;
    sd.creationBlock = block.number;
    stakeDeltas.push(sd);
  }

  function votesIncludeNth(uint _challengeIndex) internal view returns (uint) {
    return uint(int(challenges[_challengeIndex].votesInclude) + stakeDeltas[_challengeIndex].delta);
  }

  function voterVotesIncludeNth(uint _challengeIndex, address _voter) public constant returns (uint) {
    int votes = int(super.voterVotesIncludeNth(_challengeIndex, _voter));
    return uint(votes + stakeDeltas[_challengeIndex].deltas[_voter]);
  }

  function claimCreatorRewardNth(uint _challengeIndex) public {
    revert();
  }

}
