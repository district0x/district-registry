pragma solidity ^0.4.24;

import "./Challenge.sol";
import "./Registry.sol";
import "@aragon/os/contracts/lib/math/SafeMath.sol";
import "@aragon/apps-shared-minime/contracts/MiniMeToken.sol";
import "./utils/AddressUtils.sol";
import "./proxy/Forwarder1.sol";

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

  Registry public constant registry = Registry(0xfEEDFEEDfeEDFEedFEEdFEEDFeEdfEEdFeEdFEEd);
  MiniMeToken public constant registryToken = MiniMeToken(0xdeaDDeADDEaDdeaDdEAddEADDEAdDeadDEADDEaD);

  enum Status {ChallengePeriod, CommitPeriod, RevealPeriod, Blacklisted, Whitelisted}

  address public creator;
  uint public version;
  uint public deposit;
  uint public challengePeriodEnd;

  address[] public challenges;

  /**
   * @dev Modifier that disables function if registry is in emergency state
   */
  modifier notEmergency() {
    require(!registry.isEmergency());
    _;
  }

  /**
   * @dev Returns whether a registry entry is in active challenge period

   * @return True if registry entry is in challenge period
   */
  function isChallengePeriodActive()
  public
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
   * Transfers token deposit from challenger into this contract
   * Only 1 challenge can be created at a time

   * @param _challenger Address of a challenger
   * @param _challengeMetaHash IPFS hash of data related to a challenge
   */
  function createChallenge(
    address _challenger,
    bytes _challengeMetaHash
  )
  public
  notEmergency
  {
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

    Challenge challenge = Challenge(new Forwarder1());

    challenge.construct(
      this,
      _challenger,
      _challengeMetaHash,
      challengePeriodEnd,
      commitPeriodEnd,
      revealPeriodEnd,
      rewardPool,
      voteQuorum
    );

    challenges.push(address(challenge));

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

  /**
   * @dev Returns whether a registry entry is currently challengable

   * @return True if registry entry is currently challengable
   */
  function isChallengeable() public constant returns (bool) {
    return isChallengePeriodActive() && challenges.length == 0;
  }

  /**
   * @dev Returns index of the latest challenge

   * @return Index of the latest challenge
   */
  function currentChallengeIndex() public constant returns (uint) {
    if (challenges.length > 0) {
      return challenges.length - 1;
    } else {
      return 0;
    }
  }

  /**
   * @dev Returns a challenge given the index

   * @return Challenge
   */
  function getChallenge(uint _challengeIndex) public view returns (Challenge) {
    return Challenge(challenges[_challengeIndex]);
  }

  /**
   * @dev Returns whether a registry entry is currently challengable

   * @return True if registry entry is currently challengable
   */
  function currentChallenge() public view returns (Challenge) {
    return getChallenge(currentChallengeIndex());
  }

  /**
   * @dev Returns whether registry entry has whitelisted status

   * @return True if registry entry is whitelisted in TCR
   */
  function isWhitelisted() public constant returns (bool) {
    if (challenges.length == 0) {
      if (isChallengePeriodActive()) {
        return false;
      } else {
        return true;
      }
    } else {
      return currentChallenge().isWhitelisted();
    }
  }


  /**
   * @dev Commits encrypted vote to challenged entry
   * Locks voter's tokens in this contract. Returns when vote is revealed
   * Must be within commit period

   * @param _challengeIndex Index of a challenge to vote about
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
      _amount,
      challenge.commitPeriodEnd()
    );
  }

  /**
   * @dev Calls _commitVoteForChallenge

   * @param _challengeIndex Index of a challenge to vote about
   * @param _voter Address of a voter
   * @param _amount Amount of tokens to vote with
   * @param _secretHash Encrypted vote option with salt. sha3(voteOption, salt)
   */
  function commitVoteForChallenge(uint _challengeIndex, address _voter, uint _amount, bytes32 _secretHash) external {
    _commitVoteForChallenge(_challengeIndex, _voter, _amount, _secretHash);
  }


  /**
   * @dev Calls _commitVoteForChallenge for the latest challenge

   * @param _voter Address of a voter
   * @param _amount Amount of tokens to vote with
   * @param _secretHash Encrypted vote option with salt. sha3(voteOption, salt)
   */
  function commitVote(address _voter, uint _amount, bytes32 _secretHash) external {
    _commitVoteForChallenge(currentChallengeIndex(), _voter, _amount, _secretHash);
  }

  /**
   * @dev Reveals previously committed vote
   * Returns registryToken back to the voter
   * Must be within reveal period

   * @param _challengeIndex Index of a challenge
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
      amount,
      challenge.revealPeriodEnd()
    );
  }


  /**
   * @dev Calls _revealVoteForChallenge

   * @param _challengeIndex Index of a challenge
   * @param _voteOption Vote option voter previously voted with
   * @param _salt Salt with which user previously encrypted his vote option
   */
  function revealVoteForChallenge(
    uint _challengeIndex,
    Challenge.VoteOption _voteOption,
    string _salt
  )
  external
  {
    _revealVoteForChallenge(_challengeIndex, _voteOption, _salt);
  }

  /**
   * @dev Calls _revealVoteForChallenge for the latest challenge

   * @param _voteOption Vote option voter previously voted with
   * @param _salt Salt with which user previously encrypted his vote option
   */
  function revealVote(Challenge.VoteOption _voteOption, string _salt) external {
    _revealVoteForChallenge(currentChallengeIndex(), _voteOption, _salt);
  }

  /**
   * @dev Multipurpose function that distributes rewards generated by a challenge
   * It rewards challenger if challenger won
   * It rewards creator  if challenger lost
   * It rewards voter if voter voted for a winning option
   * It returns votes to a voter if the voter didn't reveal the vote

   * @param _challengeIndex Index of a challenge
   * @param _user Address of a user subjected to rewards
   */
  function _claimRewardForChallenge(uint _challengeIndex, address _user)
    internal
    notEmergency
  {
    Challenge challenge = getChallenge(_challengeIndex);

    uint challengerReward = challenge.safeClaimChallengerReward(_user);
    if (challengerReward > 0) {
      require(registryToken.transfer(challenge.challenger(), challengerReward));
      registry.fireChallengerRewardClaimedEvent(version, _challengeIndex, challenge.challenger(), challengerReward);
    }

    uint creatorReward = challenge.safeClaimCreatorReward(_user);
    if (creatorReward > 0) {
      require(registryToken.transfer(creator, creatorReward));
      registry.fireCreatorRewardClaimedEvent(version, _challengeIndex, creator, creatorReward);
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

  /**
   * @dev Calls _claimRewardForChallenge

   * @param _challengeIndex Index of a challenge
   * @param _user Address of a user subjected to rewards
   */
  function claimRewardForChallenge(uint _challengeIndex, address _user) external {
    _claimRewardForChallenge(_challengeIndex, _user);
  }

  /**
   * @dev Calls _claimRewardForChallenge for the latest challenge

   * @param _user Address of a user subjected to rewards
   */
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
