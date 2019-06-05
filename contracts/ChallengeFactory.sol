pragma solidity ^0.4.24;

import "./Challenge.sol";

contract ChallengeFactory  {
  function createChallenge(
    address _challenger,
    bytes _metaHash,
    uint _challengePeriodEnd,
    uint _commitPeriodEnd,
    uint _revealPeriodEnd,
    uint _rewardPool,
    uint _voteQuorum
  )
    public
    returns (address) {
    address challenge = new Challenge(
      msg.sender,
      _challenger,
      _metaHash,
      _challengePeriodEnd,
      _commitPeriodEnd,
      _revealPeriodEnd,
      _rewardPool,
      _voteQuorum
    );
    return challenge;
  }
}
