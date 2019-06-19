pragma solidity ^0.4.24;

import "./Registry.sol";
import "./math/SafeMath.sol";
import "minimetoken/contracts/MiniMeToken.sol";
import "./ownership/Ownable.sol";

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

  address public challenger;
  bytes metaHash;
  uint challengePeriodEnd;
  uint commitPeriodEnd;
  uint creationBlock;
  uint revealPeriodEnd;
  uint rewardPool;
  uint voteQuorum;

  uint votesInclude;
  uint votesExclude;
  uint claimedRewardOn;
  mapping(address => Vote) votes;
  int stakeDelta;
  mapping(address => int) stakeDeltas;

  function construct(
    address _owner,
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
    require(_owner != address(0));
    owner = _owner;
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
    constant
    returns (bool) {
    return !isVoteCommitPeriodActive() && now <= revealPeriodEnd;
  }

  function isVoteRevealed(address _voter)
    public
    constant
    returns (bool) {
    return votes[_voter].revealedOn > 0;
  }

  function isVoteRewardClaimed(address _voter)
    public
    constant
    returns (bool) {
    return votes[_voter].claimedRewardOn > 0;
  }

  function areVotesReclaimed(address _voter)
    public
    constant
    returns (bool) {
    return votes[_voter].reclaimedVotesOn > 0;
  }

  function isChallengeRewardClaimed()
    public
    constant
    returns (bool) {
    return claimedRewardOn > 0;
  }

  function isChallengePeriodActive()
    public
    constant
    returns (bool) {
    return now <= challengePeriodEnd;
  }

  function isWhitelisted()
    public
    constant
    returns (bool) {
    return status() == Status.Whitelisted;
  }

  function isVoteCommitPeriodActive()
    public
    constant
    returns (bool) {
    return now <= commitPeriodEnd;
  }

  function isVoteRevealPeriodOver()
    public
    constant
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
    constant
    returns (bool) {
    return winningVoteOption() == VoteOption.Include;
  }

  function hasVoted(address _voter)
    public
    constant
    returns (bool) {
    return votes[_voter].amount != 0;
  }

  function wasChallenged()
    public
    constant
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
    constant
    returns (bool) {
    return votes[_voter].option == winningVoteOption();
  }

  /**
   * @dev Returns current status of a registry entry

   * @return Status
   */
  function status()
    public
    constant
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
  function challengeReward(uint deposit)
    public
    constant
    returns (uint) {
    return deposit.add(deposit.sub(rewardPool));
  }

  /**
   * @dev Returns token reward amount belonging to a voter for voting for a winning option
   * Staked DNT is automatically counted as voting for inclusion
   * @param _voter Address of a voter
   *
   * @return Amount of tokens
   */
  function voteReward(address _voter)
    public
    constant
    returns (uint) {
    uint winningAmount = winningVotesAmount();
    if (!votedWinningVoteOption(_voter)) {
      return 0;
    }
    int voterAmount =  stakeDeltas[_voter] + int(votes[_voter].amount);
    if (voterAmount > 0) {
      return (uint(voterAmount).mul(rewardPool)) / winningAmount;  
    } else {
      return 0;
    }
  }

  function adjustStakeDelta(
    address _voter,
    int _amount
  )
    public
    onlyOwner
  {
    stakeDelta += _amount;
    stakeDeltas[_voter] += _amount;
  }

  function isBlacklisted()
    private
    constant
    returns (bool) {
    return status() == Status.Blacklisted;
  }

  function includeVotesAmount()
    private
    constant
    returns (uint) {
    return uint(stakeDelta + int(votesInclude) + int(registryToken.balanceOfAt(this, creationBlock)));
  }

  /**
   * @dev Returns winning vote option in held voting according to vote quorum
   * If voteQuorum is 50, any majority of votes will win
   * If voteQuorum is 24, only 25 votes out of 100 is enough for Include to be winning option
   *
   * @return Winning vote option
   */
  function winningVoteOption()
    private
    constant
    returns (VoteOption) {
    if (!isVoteRevealPeriodOver()) {
      return VoteOption.Neither;
    }
    uint _votesInclude = includeVotesAmount();
    if (_votesInclude.mul(100) > voteQuorum.mul(_votesInclude.add(votesExclude))) {
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
  function winningVotesAmount()
    private
    constant
    returns (uint) {
    VoteOption voteOption = winningVoteOption();
    if (voteOption == VoteOption.Include) {
      return includeVotesAmount();
    } else if (voteOption == VoteOption.Exclude) {
      return votesExclude;
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
        !areVotesReclaimed(_voter)
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
      isVoteRevealed(_voter) &&
      votedWinningVoteOption(_voter)
    ) {
      reward = voteReward(_voter);
      if (reward > 0) {
        votes[_voter].claimedRewardOn = now;
      }
    }
    return reward;
  }

  function safeClaimChallengeReward(address _user, uint deposit)
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
      claimedRewardOn = now;
      reward = challengeReward(deposit);
    }
    return reward;
  }
}
