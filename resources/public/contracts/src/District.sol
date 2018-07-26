pragma solidity ^0.4.24;

import "./RegistryEntry.sol"; 
import "math/Power.sol";
import "token/stakebank/StakeBank.sol";

/**
 * @title Contract created for each submitted District into the DistrictFactory TCR.
 *
 * @dev It extends base RegistryEntry with additional state for storing IPFS hashes for a district data.
 * Full copy of this contract is NOT deployed with each submission in order to save gas. Only forwarder contracts
 * pointing into single intance of it.
 */

contract District is RegistryEntry, MiniMeToken, StakeBank, Power

{
  /** 
   * @dev IPFS hash of file that contains all data from form fields
   */
  bytes public infoHash;


  /**
   * @dev reserve ratio, represented in ppm, 1-1000000
   * 1/3 corresponds to y= multiple * x^2
   * 1/2 corresponds to y= multiple * x
   * 2/3 corresponds to y= multiple * x^1/2
   * multiple will depends on contract initialization,
   * specificallytotalAmount and poolBalance parameters
   * we might want to add an 'initialize' function that will allow
   * the owner to send ether to the contract and mint a given amount of tokens
   */
  uint32 public dntWeight;

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
    bytes _infoHash,
    uint32 _dntWeight
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

    super.constructPower();

    challengePeriodEnd = ~uint(0);

    infoHash = _infoHash;

    require(_dntWeight >= 1 && _dntWeight <= MAX_WEIGHT);
    dntWeight = _dntWeight;
  }

  function setInfoHash(bytes _infoHash) public
  {
    require(msg.sender == creator);
    infoHash = _infoHash;
  }

  /**
   * @dev given a token supply, connector balance, weight and a deposit amount (in the connector token),
   * calculates the return for a given conversion (in the main token)
   *
   * Formula:
   * Return = _supply * ((1 + _depositAmount / _connectorBalance) ^ (_connectorWeight / 1000000) - 1)
   *
   * @param _supply              token total supply
   * @param _connectorBalance    total connector balance
   * @param _connectorWeight     connector weight, represented in ppm, 1-1000000
   * @param _depositAmount       deposit amount, in connector token
   *
   *  @return purchase return amount
   */
  function calculatePurchaseReturn(
    uint256 _supply,
    uint256 _connectorBalance,
    uint32 _connectorWeight,
    uint256 _depositAmount) public constant returns (uint256)
  {
    // validate input
    require(_supply > 0 && _connectorBalance > 0 && _connectorWeight > 0 && _connectorWeight <= MAX_WEIGHT);

    // special case for 0 deposit amount
    if (_depositAmount == 0) {
      return 0;
    }

    // special case if the weight = 100%
    if (_connectorWeight == MAX_WEIGHT) {
      return _supply.mul(_depositAmount).div(_connectorBalance);
    }

    uint256 result;
    uint8 precision;
    uint256 baseN = _depositAmount.add(_connectorBalance);
    (result, precision) = power(baseN, _connectorBalance, _connectorWeight, MAX_WEIGHT);
    uint256 temp = _supply.mul(result) >> precision;
    return temp - _supply;
  }

  function estimateReturnForStake(uint _amount) public view returns (uint) {
    return calculatePurchaseReturn(
      totalSupply().add(1e19),
      totalStaked().add(1e14),
      dntWeight,
      _amount
    );
  }

  /// @notice Stakes a certain amount of tokens for another user.
  /// @param _user Address of the user to stake for.
  /// @param _amount Amount of tokens to stake.
  /// @param _data Data field used for signalling in more complex staking applications.
  function stakeFor(address _user, uint _amount, bytes _data) 
  public 
  {
    super.stakeFor(_user, _amount, _data);
    require(generateTokens(_user, estimateReturnForStake(_amount)));
    maybeAdjustStakeDelta(_user, int(_amount));
  }

  function stake(uint256 _amount, bytes _data) public {
    stakeFor(msg.sender, _amount, _data);
  }

  /// @notice Unstakes a certain amount of tokens.
  /// @param _amount Amount of tokens to unstake.
  /// @param _data Data field used for signalling in more complex staking applications.
  function unstake(uint _amount, bytes _data) public {
    require(_amount > 0);
    uint staked = totalStakedFor(msg.sender);
    uint minted = balanceOf(msg.sender);
    uint toDestroy = minted.div(staked.div(_amount));
    super.unstake(_amount, _data);
    require(this.destroyTokens(msg.sender, toDestroy));
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
