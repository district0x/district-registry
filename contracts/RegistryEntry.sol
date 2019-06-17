pragma solidity ^0.4.24;

import "./Challenge.sol";
import "./ChallengeFactory.sol";
import "./Registry.sol";
import "./math/SafeMath.sol";
import "minimetoken/contracts/MiniMeToken.sol";
import "./utils/AddressUtils.sol";

/**
 * @title Contract created with each submission to a TCR
 *
 * @dev It contains all state and logic related to TCR challenging and voting
 * Full copy of this contract is NOT deployed with each submission in order to save gas. Only forwarder contracts
 * pointing into single intance of it.
 * This contract is meant to be extended by domain specific registry entry contracts (District, ParamChange)
 */

contract RegistryEntry is ApproveAndCallFallBack {

  using SafeMath for uint;

  Registry internal constant registry = Registry(0xfEEDFEEDfeEDFEedFEEdFEEDFeEdfEEdFeEdFEEd);
  MiniMeToken internal constant registryToken = MiniMeToken(0xdeaDDeADDEaDdeaDdEAddEADDEAdDeadDEADDEaD);
  ChallengeFactory internal constant challengeFactory = ChallengeFactory(0xCcCCccccCCCCcCCCCCCcCcCccCcCCCcCcccccccC);

  enum Status {ChallengePeriod, CommitPeriod, RevealPeriod, Blacklisted, Whitelisted}

  address internal creator;
  uint internal version;
  uint internal deposit;
  uint internal challengePeriodEnd;

  address[] internal challenges;

  /**
   * @dev Modifier that disables function if registry is in emergency state
   */
  modifier notEmergency() {
    require(!registry.isEmergency());
    _;
  }

  function isChallengePeriodActive()
  internal
  constant
  returns (bool) {
    return now <= challengePeriodEnd;
  }

  /**
   * @dev Constructor for this contract.
   * Native constructor is not used, because users create only forwarders into single instance of this contract,
   * therefore constructor must be called explicitly.
   * Must NOT be callable multiple times
   * Transfers TCR entry token deposit from sender into this contract

   * @param _creator Creator of a district
   * @param _version Version of District contract
   */
  function construct(
    address _creator,
    uint _version
  )
  public
  {
    require(challengePeriodEnd == 0);
    deposit = registry.db().getUIntValue(registry.depositKey());
    require(registryToken.transferFrom(msg.sender, this, deposit));
    challengePeriodEnd = now.add(registry.db().getUIntValue(registry.challengePeriodDurationKey()));
    creator = _creator;
    version = _version;
  }

  /**
   * @dev Creates a challenge for this TCR entry
   * Must be within challenge period
   * Entry can be challenged only once
   * Transfers token deposit from challenger into this contract
   * Forks registry token (DankToken) in order to create single purpose voting token to vote about this challenge

   * @param _challenger Address of a challenger
   * @param _challengeMetaHash IPFS hash of meta data related to this challenge
   */
  function createChallenge(
    address _challenger,
    bytes _challengeMetaHash
  )
  public
  notEmergency
  {
    require(_challenger != 0x0);
    require(isChallengeable());
    require(registryToken.transferFrom(_challenger, address(this), deposit));

    uint commitDuration = registry.db().getUIntValue(registry.commitPeriodDurationKey());
    uint revealDuration = registry.db().getUIntValue(registry.revealPeriodDurationKey());
    uint commitPeriodEnd = now.add(commitDuration);
    uint revealPeriodEnd = commitPeriodEnd.add(revealDuration);
    uint rewardPool = ((100 - registry.db()
    .getUIntValue(
      registry.challengeDispensationKey()
    )
    ).mul(deposit)) / 100;
    uint voteQuorum = registry.db().getUIntValue(registry.voteQuorumKey());

    address challenge = challengeFactory.createChallenge(
      _challenger,
      _challengeMetaHash,
      challengePeriodEnd,
      commitPeriodEnd,
      revealPeriodEnd,
      rewardPool,
      voteQuorum
    );

    challenges.push(challenge);

    registry.fireChallengeCreatedEvent(
      version,
      currentChallengeIndex(),
      _challenger,
      commitPeriodEnd,
      revealPeriodEnd,
      rewardPool,
      _challengeMetaHash
    );
  }

  function isChallengeable() internal constant returns (bool) {
    return isChallengePeriodActive() && challenges.length == 0;
  }

  function currentChallengeIndex() internal constant returns (uint) {
    return challenges.length - 1;
  }

  function getChallenge(uint _challengeIndex) internal view returns (Challenge) {
    return Challenge(challenges[_challengeIndex]);
  }

  function currentChallenge() internal view returns (Challenge) {
    return getChallenge(currentChallengeIndex());
  }


  /**
   * @dev Commits encrypted vote to challenged entry
   * Locks voter's tokens in this contract. Returns when vote is revealed
   * Must be within commit period
   * Voting takes full balance of voter's voting token

   * @param _voter Address of a voter
   * @param _amount Amount of tokens to vote with
   * @param _secretHash Encrypted vote option with salt. sha3(voteOption, salt)
   */
  function _commitVoteForChallenge(
    uint _challengeIndex,
    address _voter,
    uint _amount,
    bytes32 _secretHash
  )
  internal
  notEmergency
  {
    require(registryToken.transferFrom(_voter, this, _amount));
    Challenge challenge = getChallenge(_challengeIndex);
    challenge.commitVote(_voter, _amount, _secretHash);
    registry.fireVoteCommittedEvent(
      version,
      _challengeIndex,
      _voter,
      _amount
    );
  }

  function commitVoteForChallenge(uint _challengeIndex, address _voter, uint _amount, bytes32 _secretHash) external {
    _commitVoteForChallenge(_challengeIndex, _voter, _amount, _secretHash);
  }

  function commitVote(address _voter, uint _amount, bytes32 _secretHash) external {
    _commitVoteForChallenge(currentChallengeIndex(), _voter, _amount, _secretHash);
  }

  /**
   * @dev Reveals previously committed vote
   * Returns registryToken back to the voter
   * Must be within reveal period

   * @param _voteOption Vote option voter previously voted with
   * @param _salt Salt with which user previously encrypted his vote option
   */
  function _revealVoteForChallenge(
    uint _challengeIndex,
    Challenge.VoteOption _voteOption,
    string _salt
  )
  internal
  notEmergency
  {
    address _voter = msg.sender;

    Challenge challenge = getChallenge(_challengeIndex);
    uint amount = challenge.revealVote(_voter, _voteOption, _salt);

    require(registryToken.transfer(_voter, amount));

    registry.fireVoteRevealedEvent(
      version,
      _challengeIndex,
      _voter,
      uint(_voteOption),
      amount
    );
  }

  function revealVoteForChallenge(
    uint _challengeIndex,
    Challenge.VoteOption _voteOption,
    string _salt
  )
  external
  {
    _revealVoteForChallenge(_challengeIndex, _voteOption, _salt);
  }

  function revealVote(Challenge.VoteOption _voteOption, string _salt) external {
    _revealVoteForChallenge(currentChallengeIndex(), _voteOption, _salt);
  }

  function _claimRewardForChallenge(uint _challengeIndex, address _user)
    internal
    notEmergency
  {
    Challenge challenge = getChallenge(_challengeIndex);

    uint challengeReward = challenge.safeClaimChallengeReward(_user, deposit);
    if (challengeReward > 0) {
      require(registryToken.transfer(challenge.challenger(), challengeReward));
      registry.fireChallengeRewardClaimedEvent(version, _challengeIndex, challenge.challenger(), challengeReward);
    }

    uint voteReward = challenge.safeClaimVoteReward(_user);
    if (voteReward > 0) {
      require(registryToken.transfer(_user, voteReward));
      registry.fireVoteRewardClaimedEvent(version, _challengeIndex, _user, voteReward);
    } else {
      uint reclaimableVotes = challenge.safeReclaimVotes(_user);
      if (reclaimableVotes > 0) {
        require(registryToken.transfer(_user, reclaimableVotes));
        registry.fireVotesReclaimedEvent(version, _challengeIndex, _user, reclaimableVotes);
      }
    }
  }

  function claimRewardForChallenge(uint _challengeIndex, address  _user) external {
    _claimRewardForChallenge(_challengeIndex, _user);
  }

  function claimReward(address _user) external {
    _claimRewardForChallenge(currentChallengeIndex(), _user);
  }

  /**
   * @dev Function called by MiniMeToken when somebody calls approveAndCall on it.
   * This way token can be transferred to a recipient in a single transaction together with execution
   * of additional logic
   * @param _from Sender of transaction approval
   * @param _amount Amount of approved tokens to transfer
   * @param _token Token that received the approval
   * @param _data Bytecode of a function and passed parameters, that should be called after token approval
   */
  function receiveApproval(
    address _from,
    uint256 _amount,
    address _token,
    bytes _data
  )
  public
  {
    _from;
    _amount;
    _token;
    require(address(this).call(_data));
  }
}
