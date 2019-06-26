pragma solidity ^0.4.24;

import "./Power.sol";
import "@aragon/os/contracts/lib/math/SafeMath.sol";
import "./ownership/Ownable.sol";
import "./minime/MiniMeTokenProxyTarget.sol";
import "./proxy/Forwarder1.sol";

contract StakeBank is Ownable, MiniMeTokenProxyTarget {

  MiniMeTokenFactory public constant minimeTokenFactory = MiniMeTokenFactory(0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE);
  uint32 public MAX_WEIGHT;
  using SafeMath for uint256;

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
  uint32 private dntWeight;

  uint256[128] private maxExpArray;

  StakeBankCheckpoint[] private stakeHistory;
  mapping (address => StakeBankCheckpoint[]) private stakesFor;

  Power private power;

  struct StakeBankCheckpoint {
    uint256 at;
    uint256 amount;
  }

  function construct(
    uint32 _dntWeight
  )
    public
  {
    require(MAX_WEIGHT == 0);
    MAX_WEIGHT = 1000000;
    super.construct(
      address(minimeTokenFactory),
      0x0,
      0,
      "District Voting Token",
      18,
      "DVT",
      false
    );

    require(_dntWeight >= 1 && _dntWeight <= MAX_WEIGHT);
    owner = msg.sender;
    changeController(owner);
    dntWeight = _dntWeight;
    power = Power(new Forwarder1());
    power.construct();
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
    uint256 _depositAmount
  )
    private constant returns (uint256)
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
    (result, precision) = power.power(baseN, _connectorBalance, _connectorWeight, MAX_WEIGHT);
    uint256 temp = _supply.mul(result) >> precision;
    return temp - _supply;
  }

  function estimateReturnForStake(uint _amount) private view returns (uint) {
    return calculatePurchaseReturn(
      totalSupply().add(1e19),
      totalStaked().add(1e14),
      dntWeight,
      _amount
    );
  }

  function stakeFor(address user, uint256 amount) public onlyOwner returns (uint) {
    updateStakeBankCheckpointAtNow(stakesFor[user], amount, false);
    updateStakeBankCheckpointAtNow(stakeHistory, amount, false);
    require(generateTokens(user, estimateReturnForStake(amount)));
    return amount;
  }

  function unstake(address user, uint256 amount) public onlyOwner returns (uint) {
    require(amount > 0);
    uint staked = totalStakedFor(user);
    uint minted = balanceOf(user);
    uint toDestroy = minted.mul(1000000000000000000).div(staked.mul(1000000000000000000).div(amount));
    require(destroyTokens(user, toDestroy));
    updateStakeBankCheckpointAtNow(stakesFor[user], amount, true);
    updateStakeBankCheckpointAtNow(stakeHistory, amount, true);
    return amount;
  }

  function totalStakedFor(address addr) public view returns (uint256) {
    StakeBankCheckpoint[] storage stakes = stakesFor[addr];
    if (stakes.length == 0) {
      return 0;
    }
    return stakes[stakes.length-1].amount;
  }

  function totalStaked() public view returns (uint256) {
    return totalStakedAt(block.number);
  }

  function lastStakedFor(address addr) public view returns (uint256) {
    StakeBankCheckpoint[] storage stakes = stakesFor[addr];
    if (stakes.length == 0) {
      return 0;
    }
    return stakes[stakes.length-1].at;
  }

  function totalStakedForAt(address addr, uint256 blockNumber) public view returns (uint256) {
    return stakedAt(stakesFor[addr], blockNumber);
  }

  function totalStakedAt(uint256 blockNumber) public view returns (uint256) {
    return stakedAt(stakeHistory, blockNumber);
  }

  function updateStakeBankCheckpointAtNow(StakeBankCheckpoint[] storage history, uint256 amount, bool isUnstake) 
    private {
    uint256 length = history.length;
    if (length == 0) {
      history.push(StakeBankCheckpoint({at: block.number, amount: amount}));
      return;
    }
    if (history[length-1].at < block.number) {
      history.push(StakeBankCheckpoint({at: block.number, amount: history[length-1].amount}));
    }
    StakeBankCheckpoint storage checkpoint = history[length];
    if (isUnstake) {
      checkpoint.amount = checkpoint.amount.sub(amount);
    } else {
      checkpoint.amount = checkpoint.amount.add(amount);
    }
  }

  function stakedAt(StakeBankCheckpoint[] storage history, uint256 blockNumber) private view returns (uint256) {
    uint256 length = history.length;
    if (length == 0 || blockNumber < history[0].at) {
      return 0;
    }
    if (blockNumber >= history[length-1].at) {
      return history[length-1].amount;
    }
    uint min = 0;
    uint max = length-1;
    while (max > min) {
      uint mid = (max + min + 1) / 2;
      if (history[mid].at <= blockNumber) {
        min = mid;
      } else {
        max = mid-1;
      }
    }
    return history[min].amount;
  }
}
