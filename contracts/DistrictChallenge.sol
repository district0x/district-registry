pragma solidity ^0.4.24;

import "./Challenge.sol";
import "./StakeBank.sol";

contract DistrictChallenge is Challenge {

  StakeBank public stakeBank;

  int public stakeDelta;
  mapping(address => int) public stakeDeltasFor;

  function setStakeBank(StakeBank _stakeBank)
    public
    onlyOwner
  {
    require(address(_stakeBank) != address(0));
    require(address(stakeBank) == address(0));
    stakeBank = _stakeBank;
  }

  function voteOptionIncludeAmount()
    public
    view
    returns (uint)
  {
    return uint(int(totalStakedAtChallengeCreation()) + stakeDelta + int(votesInclude));
  }

  function voteOptionIncludeVoterAmount(address _voter)
    public
    view
    returns (uint)
  {
    return uint(
            int(totalStakedForAtChallengeCreation(_voter)) +
            stakeDeltasFor[_voter] +
            int(votes[_voter].amount
          ));
  }

  function totalStakedForAtChallengeCreation(address _address) public view returns (uint256) {
    return stakeBank.totalStakedForAt(_address, creationBlock);
  }

  function totalStakedAtChallengeCreation() public view returns (uint256) {
    return stakeBank.totalStakedAt(creationBlock);
  }

  function adjustStakeDelta(address _voter, int _amount)
    public
    onlyOwner
  {
    stakeDelta += _amount;
    stakeDeltasFor[_voter] += _amount;
  }


}
