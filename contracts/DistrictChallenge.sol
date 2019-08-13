pragma solidity ^0.4.24;

import "./Challenge.sol";
import "./StakeBank.sol";

/**
 * @title Challenge contract for creating District challenges
 * @dev It extends base Challenge contract to count staked tokens as votes
 */

contract DistrictChallenge is Challenge {

  StakeBank public stakeBank;

  int public stakeDelta;
  mapping(address => int) public stakeDeltasFor;

  /**
   * @dev Sets stake bank contract address
   * Can be called only by related registry entry
   * Cannot be called more than once
   * @param _stakeBank Stakebank contract address
   */
  function setStakeBank(StakeBank _stakeBank)
    public
    onlyOwner
  {
    require(address(_stakeBank) != address(0));
    require(address(stakeBank) == address(0));
    stakeBank = _stakeBank;
  }

  /**
   * @dev Returns total amount of tokens voted for Include
   * Amount of tokens staked before vote commit period end is counted as votes for Include
   * @return Amount of tokens
   */
  function voteOptionIncludeAmount()
    public
    view
    returns (uint)
  {
    return uint(int(totalStakedAtChallengeCreation()) + stakeDelta + int(votesInclude));
  }

  /**
   * @dev Returns whether voter voted for winning vote option
   * Amount of tokens staked before vote commit period end is counted as votes for Include
   * @param _voter Address of a voter
   * @return True if voter voted for a winning vote option
   */
  function votedWinningVoteOption(address _voter)
    public
    view
    returns (bool) {

    return
      super.votedWinningVoteOption(_voter) ||
      (winningVoteOption() == VoteOption.Include && voteOptionIncludeVoterAmount(_voter) > 0);
  }

  /**
   * @dev Returns amount a voter voted for vote option Include
   * Amount of tokens staked before vote commit period end is counted as votes for Include
   * @param _voter Address of a voter
   * @return The amount a voter voted for vote option Include
   */
  function voteOptionIncludeVoterAmount(address _voter)
    public
    view
    returns (uint)
  {
    return
      uint(
        int(totalStakedForAtChallengeCreation(_voter)) +
        stakeDeltasFor[_voter] +
        int(super.voteOptionIncludeVoterAmount(_voter))
      );
  }

  /**
   * @dev Returns amount of staked tokens at challenge creation for given address
   * @param _address Address of a staker
   * @return The amount of tokens
   */
  function totalStakedForAtChallengeCreation(address _address)
    public
    view
    returns (uint256)
  {
    return stakeBank.totalStakedForAt(_address, creationBlock);
  }

  /**
   * @dev Returns amount of total staked tokens at challenge creation
   * @return The amount of tokens
   */
  function totalStakedAtChallengeCreation()
    public
    view
    returns (uint256)
  {
    return stakeBank.totalStakedAt(creationBlock);
  }

  /**
   * @dev Keeps track of stake delta, so we have data about the amount of tokens staked between
   * challenge creation and vote commit period end
   * @param _voter Address of a voter
   * @param _amount Amount of tokens
   */
  function adjustStakeDelta(address _voter, int _amount)
    public
    onlyOwner
  {
    stakeDelta += _amount;
    stakeDeltasFor[_voter] += _amount;
  }


}
