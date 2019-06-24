pragma solidity ^0.4.24;

import "./Registry.sol";
import "./math/SafeMath.sol";
import "minimetoken/contracts/MiniMeToken.sol";
import "./ownership/Ownable.sol";
import "./StakeBank.sol";
import "./RegistryEntry.sol";

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

  function isVoteRevealPeriodActive()
    public
    view
    returns (bool) {
    return !isVoteCommitPeriodActive() && now <= revealPeriodEnd;
  }

  function isVoteRevealed(address _voter)
    public
    view
    returns (bool) {
    return votes[_voter].revealedOn > 0;
  }

  function isVoteRewardClaimed(address _voter)
    public
    view
    returns (bool) {
    return votes[_voter].claimedRewardOn > 0;
  }

  function areVotesReclaimed(address _voter)
    public
    view
    returns (bool) {
    return votes[_voter].reclaimedVotesOn > 0;
  }

  function isChallengeRewardClaimed()
    public
    view
    returns (bool) {
    return challengerRewardClaimedOn > 0;
  }

  function isCreatorRewardClaimed()
    public
    view
    returns (bool) {
    return creatorRewardClaimedOn > 0;
  }

  function isChallengePeriodActive()
    public
    view
    returns (bool) {
    return now <= challengePeriodEnd;
  }

  function isWhitelisted()
    public
    view
    returns (bool) {
    return status() == Status.Whitelisted;
  }

  function isVoteCommitPeriodActive()
    public
    view
    returns (bool) {
    return now <= commitPeriodEnd;
  }

  function isVoteRevealPeriodOver()
    public
    view
    returns (bool) {
    return revealPeriodEnd > 0 && now > revealPeriodEnd;
  }

  /**
   * @dev Returns whether Include is winning vote option
   *
   * @return True if Include is winning option
   */
  function isWinningOptionInclude()
    public
    view
    returns (bool) {
    return winningVoteOption() == VoteOption.Include;
  }

  function hasVoted(address _voter)
    public
    view
    returns (bool) {
    return votes[_voter].amount != 0;
  }

  function wasChallenged()
    public
    view
    returns (bool) {
    return challenger != 0x0;
  }

  /**
   * @dev Returns whether voter voted for winning vote option
   * @param _voter Address of a voter
   *
   * @return True if voter voted for a winning vote option
   */
  function votedWinningVoteOption(address _voter)
    public
    view
    returns (bool) {
    return votes[_voter].option == winningVoteOption();
  }

  /**
   * @dev Returns current status of a registry entry

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
   *
   * @return Amount of token
   */
  function challengerReward()
    public
    view
    returns (uint) {
    uint deposit = registryEntry.deposit();
    return deposit.add(deposit.sub(rewardPool));
  }

  function creatorReward()
    public
    view
    returns (uint) {
    uint deposit = registryEntry.deposit();
    return deposit.sub(rewardPool);
  }

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
   *
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

  function isBlacklisted()
    public
    view
    returns (bool) {
    return status() == Status.Blacklisted;
  }

  function voteOptionIncludeAmount()
    public
    view
    returns (uint) {
    return votesInclude;
  }

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
   *
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

  function safeClaimChallengerReward(address _user)
    external
    onlyOwner
    returns (uint)
  {
    uint reward = 0;
    if (isVoteRevealPeriodOver() &&
      !isChallengeRewardClaimed() &&
      !isWinningOptionInclude() &&
      challenger == _user
    ) {
      challengerRewardClaimedOn = now;
      reward = challengerReward();
    }
    return reward;
  }

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


