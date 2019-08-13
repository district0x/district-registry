pragma solidity ^0.4.24;

import "./Registry.sol";
import "@aragon/os/contracts/lib/math/SafeMath.sol";
import "@aragon/apps-shared-minime/contracts/MiniMeToken.sol";
import "./ownership/Ownable.sol";
import "./StakeBank.sol";
import "./RegistryEntry.sol";


/**
 * @title Contract created each time a registry entry is challenged
 *
 * @dev Full copy of this contract is NOT deployed with each challenge creation in order to save gas. Only forwarder contracts
 * pointing into single intance of it.
 */


contract Challenge is Ownable {

  using SafeMath for uint;

  MiniMeToken public constant registryToken = MiniMeToken(0xdeaDDeADDEaDdeaDdEAddEADDEAdDeadDEADDEaD);

  enum VoteOption {Neither, Include, Exclude}

  enum Status {ChallengePeriod, CommitPeriod, RevealPeriod, Blacklisted, Whitelisted}

  struct Vote {
    bytes32 secretHash;
    VoteOption option;
    uint amount;
    uint revealedOn;
    uint claimedRewardOn;
    uint reclaimedVotesOn;
  }

  RegistryEntry public registryEntry;
  address public challenger;
  bytes public metaHash;
  uint public challengePeriodEnd;
  uint public commitPeriodEnd;
  uint public creationBlock;
  uint public revealPeriodEnd;
  uint public rewardPool;
  uint public voteQuorum;

  uint public votesInclude;
  uint public votesExclude;
  uint public challengerRewardClaimedOn;
  uint public creatorRewardClaimedOn;
  mapping(address => Vote) public votes;


  /**
   * @dev Constructor for this contract
   * Native constructor is not used, because users create only forwarders into single instance of this contract,
   * therefore constructor must be called explicitly.
   * Must NOT be callable multiple times

   * @param _challenger Address of a challenger
   * @param _metaHash IPFS hash of data related to a challenge
   * @param _challengePeriodEnd Timestamp when challenge period ends
   * @param _revealPeriodEnd Timestamp when reveal period ends
   * @param _rewardPool Reward pool splitting ratio
   * @param _voteQuorum Vote quorum coefficient
   */
  function construct(
    RegistryEntry _registryEntry,
    address _challenger,
    bytes _metaHash,
    uint _challengePeriodEnd,
    uint _commitPeriodEnd,
    uint _revealPeriodEnd,
    uint _rewardPool,
    uint _voteQuorum
  )
    public {
    require(owner == address(0));
    registryEntry = _registryEntry;
    owner = msg.sender;
    challenger = _challenger;
    metaHash = _metaHash;
    challengePeriodEnd = _challengePeriodEnd;
    commitPeriodEnd = _commitPeriodEnd;
    creationBlock = block.number;
    revealPeriodEnd = _revealPeriodEnd;
    rewardPool = _rewardPool;
    voteQuorum = _voteQuorum;
  }

  /**
   * @dev Returns whether a vote reveal period is active
   * @return True if vote reveal period is active
   */
  function isVoteRevealPeriodActive()
    public
    view
    returns (bool) {
    return !isVoteCommitPeriodActive() && now <= revealPeriodEnd;
  }

  /**
   * @dev Returns whether a vote has been revealed
   * @param _voter Address of a voter
   * @return True if vote has been revealed already
   */
  function isVoteRevealed(address _voter)
    public
    view
    returns (bool) {
    return votes[_voter].revealedOn > 0;
  }

  /**
   * @dev Returns whether a vote reward has been claimed by voter
   * @param _voter Address of a voter
   * @return True if vote reward has been claimed already
   */
  function isVoteRewardClaimed(address _voter)
    public
    view
    returns (bool) {
    return votes[_voter].claimedRewardOn > 0;
  }

  /**
   * @dev Returns whether a voting tokens have been already reclaimed
   * @param _voter Address of a voter
   * @return True if voting tokens have been reclaimed already
   */
  function areVotesReclaimed(address _voter)
    public
    view
    returns (bool) {
    return votes[_voter].reclaimedVotesOn > 0;
  }

  /**
   * @dev Returns whether the challenger reward has been already claimed
   * @return True if challenger reward has been claimed already
   */
  function isChallengerRewardClaimed()
    public
    view
    returns (bool) {
    return challengerRewardClaimedOn > 0;
  }

  /**
   * @dev Returns whether the creator reward has been already claimed
   * @return True if creator reward has been claimed already
   */
  function isCreatorRewardClaimed()
    public
    view
    returns (bool) {
    return creatorRewardClaimedOn > 0;
  }

  /**
   * @dev Returns whether a vote challenge period is active
   * @return True if vote challenge period is active
   */
  function isChallengePeriodActive()
    public
    view
    returns (bool) {
    return now <= challengePeriodEnd;
  }

  /**
   * @dev Returns whether a status of a challenge is whitelisted
   * @return True if status is whitelisted
   */
  function isWhitelisted()
    public
    view
    returns (bool) {
    return status() == Status.Whitelisted;
  }

  /**
   * @dev Returns whether a vote commit period is active
   * @return True if vote commit period is active
   */
  function isVoteCommitPeriodActive()
    public
    view
    returns (bool) {
    return now <= commitPeriodEnd;
  }

  /**
   * @dev Returns whether a vote reveal period is over
   * @return True if vote reveal period is over
   */
  function isVoteRevealPeriodOver()
    public
    view
    returns (bool) {
    return revealPeriodEnd > 0 && now > revealPeriodEnd;
  }

  /**
   * @dev Returns whether Include is winning vote option
   * @return True if Include is winning option
   */
  function isWinningOptionInclude()
    public
    view
    returns (bool) {
    return winningVoteOption() == VoteOption.Include;
  }

  /**
   * @dev Returns whether a voter has voted already
   * @param _voter Address of a voter
   * @return True if voter has voted already
   */
  function hasVoted(address _voter)
    public
    view
    returns (bool) {
    return votes[_voter].amount != 0;
  }

  /**
   * @dev Returns whether this challenge was constructed
   * @return True if challenge was constructed
   */
  function wasChallenged()
    public
    view
    returns (bool) {
    return challenger != 0x0;
  }

  /**
   * @dev Returns whether voter voted for winning vote option
   * @param _voter Address of a voter
   * @return True if voter voted for a winning vote option
   */
  function votedWinningVoteOption(address _voter)
    public
    view
    returns (bool) {
    return votes[_voter].option == winningVoteOption();
  }

  /**
   * @dev Returns current result of a challenge
   * @return Status
   */
  function status()
    public
    view
    returns (Status) {
    if (isChallengePeriodActive() && !wasChallenged()) {
      return Status.ChallengePeriod;
    } else if (isVoteCommitPeriodActive()) {
      return Status.CommitPeriod;
    } else if (isVoteRevealPeriodActive()) {
      return Status.RevealPeriod;
    } else if (isVoteRevealPeriodOver()) {
      if (isWinningOptionInclude()) {
        return Status.Whitelisted;
      } else {
        return Status.Blacklisted;
      }
    } else {
      return Status.Whitelisted;
    }
  }

  /**
   * @dev Returns token reward amount belonging to a challenger
   * @return Amount of tokens
   */
  function challengerReward()
    public
    view
    returns (uint) {
    uint deposit = registryEntry.deposit();
    return deposit.add(deposit.sub(rewardPool));
  }

  /**
   * @dev Returns token reward amount belonging to a creator
   * @return Amount of tokens
   */
  function creatorReward()
    public
    view
    returns (uint) {
    uint deposit = registryEntry.deposit();
    return deposit.sub(rewardPool);
  }

  /**
   * @dev Returns amount a voter voted for vote option Include
   * @param _voter Address of a voter
   * @return The amount a voter voted for vote option Include
   */
  function voteOptionIncludeVoterAmount(address _voter)
    public
    view
    returns (uint)
  {
    uint amount = 0;
    if (votes[_voter].option == VoteOption.Include) {
      amount = votes[_voter].amount;
    }
    return amount;
  }

  /**
   * @dev Returns amount a voter voted for vote option Exclude
   * @param _voter Address of a voter
   * @return The amount a voter voted for vote option Exclude
   */
  function voteOptionExcludeVoterAmount(address _voter)
    public
    view
    returns (uint)
  {
    uint amount = 0;
    if (votes[_voter].option == VoteOption.Exclude) {
      amount = votes[_voter].amount;
    }
    return amount;
  }

  /**
   * @dev Returns token reward amount belonging to a voter for voting for a winning option
   * @param _voter Address of a voter
   * @return Amount of tokens
   */
  function voteReward(address _voter)
    public
    view
    returns (uint) {
    uint winningAmount = winningVoteOptionAmount();
    if (!votedWinningVoteOption(_voter)) {
      return 0;
    }
    uint voterAmount;
    if (isWinningOptionInclude()) {
      voterAmount = voteOptionIncludeVoterAmount(_voter);
    } else {
      voterAmount = voteOptionExcludeVoterAmount(_voter);
    }

    if (voterAmount > 0) {
      return (voterAmount.mul(rewardPool)).div(winningAmount);
    } else {
      return 0;
    }
  }

  /**
   * @dev Returns whether a status of a challenge is blacklisted
   * @return True if status is blacklisted
   */
  function isBlacklisted()
    public
    view
    returns (bool) {
    return status() == Status.Blacklisted;
  }

  /**
   * @dev Returns total amount of tokens voted for Include
   * @return Amount of tokens
   */
  function voteOptionIncludeAmount()
    public
    view
    returns (uint) {
    return votesInclude;
  }

  /**
   * @dev Returns total amount of tokens voted for Exclude
   * @return Amount of tokens
   */
  function voteOptionExcludeAmount()
    public
    view
    returns (uint) {
    return votesExclude;
  }

  /**
   * @dev Returns winning vote option in held voting according to vote quorum
   * If voteQuorum is 50, any majority of votes will win
   * If voteQuorum is 24, only 25 votes out of 100 is enough for Include to be winning option
   *
   * @return Winning vote option
   */
  function winningVoteOption()
    public
    view
    returns (VoteOption) {
    if (!isVoteRevealPeriodOver()) {
      return VoteOption.Neither;
    }
    uint _voteOptionIncludeAmount = voteOptionIncludeAmount();
    if (_voteOptionIncludeAmount.mul(100) > voteQuorum.mul(_voteOptionIncludeAmount.add(votesExclude))) {
      return VoteOption.Include;
    } else {
      return VoteOption.Exclude;
    }
  }

  /**
   * @dev Returns amount of votes for winning vote option
   * @return Amount of votes
   */
  function winningVoteOptionAmount()
    public
    view
    returns (uint) {
    VoteOption voteOption = winningVoteOption();
    if (voteOption == VoteOption.Include) {
      return voteOptionIncludeAmount();
    } else if (voteOption == VoteOption.Exclude) {
      return voteOptionExcludeAmount();
    } else {
      return 0;
    }
  }

  /**
   * @dev Commits a vote
   * Vote can be commited only during vote commit period
   * Voter cannot commit more than once
   * Amount must be larger than zero
   * @param _voter Address of a voter
   * @param _amount Amount of tokens to vote with
   * @param _secretHash Encrypted vote option with salt. sha3(voteOption, salt)
   */
  function commitVote(
    address _voter,
    uint _amount,
    bytes32 _secretHash
  )
    external
    onlyOwner
  {
    require(isVoteCommitPeriodActive());
    require(_amount > 0);
    require(!hasVoted(_voter));
    votes[_voter].secretHash = _secretHash;
    votes[_voter].amount = _amount;
  }

  /**
   * @dev Reveals previously committed vote
   * Vote can be commited only during vote reveal period
   * Secret hash must be correctly verified by given salt
   * Cannot be revealed more than once
   * @param _voter Address of a voter
   * @param _voteOption Vote option voter previously voted with
   * @param _salt Salt with which user previously encrypted his vote option
   */
  function revealVote(
    address _voter,
    VoteOption _voteOption,
    string _salt
  )
    external
    onlyOwner
    returns (uint)
  {
    require(isVoteRevealPeriodActive());
    require(keccak256(abi.encodePacked(uint(_voteOption), _salt)) == votes[_voter].secretHash);
    require(!isVoteRevealed(_voter));
    votes[_voter].revealedOn = now;
    uint amount = votes[_voter].amount;
    votes[_voter].option = _voteOption;
    if (_voteOption == VoteOption.Include) {
      votesInclude = votesInclude.add(amount);
    } else if (_voteOption == VoteOption.Exclude) {
      votesExclude = votesExclude.add(amount);
    } else {
      revert();
    }
    return amount;
  }

  /**
   * @dev Returns amount of tokens user is eligible to reclaim
   * Does not throw error if there's nothing to reclaim
   * @param _voter Address of a voter
   * @return Amount of tokens user is eligible to reclaim
   */
  function safeReclaimVotes(address _voter)
    external
    onlyOwner
    returns (uint)
  {
    uint amount = 0;
    if (isVoteRevealPeriodOver() &&
        !isVoteRevealed(_voter) &&
        !areVotesReclaimed(_voter) &&
        hasVoted(_voter)
    ) {
      votes[_voter].reclaimedVotesOn = now;
      amount = votes[_voter].amount;
    }
    return amount;
  }

  /**
   * @dev Returns amount of tokens user is eligible to receive as vote reward
   * Does not throw error if there's no reward
   * @param _voter Address of a voter
   * @return Amount of tokens user is eligible to receive as vote reward
   */
  function safeClaimVoteReward(address _voter)
    external
    onlyOwner
    returns (uint)
  {
    uint reward = 0;
    if (isVoteRevealPeriodOver() &&
      !isVoteRewardClaimed(_voter) &&
      votedWinningVoteOption(_voter)
    ) {
      reward = voteReward(_voter);
      if (reward > 0) {
        votes[_voter].claimedRewardOn = now;
      }
    }
    return reward;
  }

  /**
   * @dev Returns amount of tokens user is eligible to receive as challenger reward
   * Does not throw error if there's no reward
   * @param _user Address of a user
   * @return Amount of tokens user is eligible to receive as challenger reward
   */
  function safeClaimChallengerReward(address _user)
    external
    onlyOwner
    returns (uint)
  {
    uint reward = 0;
    if (isVoteRevealPeriodOver() &&
      !isChallengerRewardClaimed() &&
      !isWinningOptionInclude() &&
      challenger == _user
    ) {
      challengerRewardClaimedOn = now;
      reward = challengerReward();
    }
    return reward;
  }

  /**
   * @dev Returns amount of tokens user is eligible to receive as creator reward
   * Does not throw error if there's no reward
   * @param _user Address of a user
   * @return Amount of tokens user is eligible to receive as creator reward
   */
  function safeClaimCreatorReward(address _user)
    external
    onlyOwner
    returns (uint)
  {
    uint reward = 0;
    if (isVoteRevealPeriodOver() &&
      !isCreatorRewardClaimed() &&
      isWinningOptionInclude() &&
      registryEntry.creator() == _user
    ) {
      creatorRewardClaimedOn = now;
      reward = creatorReward();
    }
    return reward;
  }
}


