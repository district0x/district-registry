pragma solidity ^0.4.24;

import "./RegistryEntry.sol"; 
// import "proxy/Forwarder.sol"; 
import "token/erc20/StandardToken.sol";
import "./District0xNetworkToken.sol";
// import "token/erc900/TokenReturningStakeBank.sol";
import "token/erc900/StakeBank.sol";
import "bonding-curve/contracts/BancorFormula.sol";


/* import "./DistrictConfig.sol"; */

/**
 * @title Contract created for each submitted District into the DistrictFactory TCR.
 *
 * @dev It extends base RegistryEntry with additional state for storing IPFS hashes for a district data.
 * Full copy of this contract is NOT deployed with each submission in order to save gas. Only forwarder contracts
 * pointing into single intance of it.
 */

contract District is RegistryEntry, StandardToken, StakeBank, BancorFormula
{
  /**
   * @dev IPFS hash of file that contains all data from form fields
   */
  bytes public infoHash;

  /*
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
   * @dev Available balance of reserve DNT in contract
   */
  uint256 public dntBalance;


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
    bytes _infoHash
    // ,uint32 _reserveRatio
  )
  public
  {
    // uint32 _dntWeight = 333333;
    uint32 _dntWeight = 666666;
    // uint32 _dntWeight = MAX_WEIGHT;
    // uint32 _dntWeight = 1;

    require(_dntWeight >= MIN_WEIGHT && _dntWeight <= MAX_WEIGHT);
    // require(_reserveRatio > 0 && _reserveRatio <= 1000000 
      // MAX_WEIGHT
      // );

    super.construct(_creator, _version);
    super.constructStakeBank(registryToken);
    super.constructPower();

    challengePeriodEnd = ~uint(0);
    infoHash = _infoHash;
    dntWeight = _dntWeight;
  }

  function setInfoHash(bytes _infoHash) public
  {
    require(msg.sender == creator);
    infoHash = _infoHash;
  }

  function mint(uint _amount) private {
    require(_amount > 0);
    totalSupply_ = totalSupply_.add(_amount);
    balances[address(this)] = balances[address(this)].add(_amount);
  }

  function unmint(uint _amount) private {
    require(_amount > 0);
    balances[address(this)] = balances[address(this)].sub(_amount);
    totalSupply_ = totalSupply_.sub(_amount);
  }

  function getSupply() private constant returns (uint256) {
    // BancorFormula requires non-0 supply
    // return totalSupply_ + 1 ether;
    return totalSupply_ + 10 * 1e18;
    return totalSupply_ + 1 ether;
  }

  function getBalance() private constant returns (uint256) {
    // BancorFormula requires non-0 balance
    // return dntBalance + 1 ether * dntWeight / MAX_WEIGHT;
    // return dntBalance + 1 * 1e14;
    return dntBalance + 1 ether;
  }  

  function calculateStakeReturn(uint _amount) public constant returns (uint256) {
    return calculatePurchaseReturn(getSupply(), getBalance(), dntWeight, _amount);
  }

  function calculateUnstakeReturn(uint _amount) public constant returns (uint256) {
    return calculateSaleReturn(getSupply(), getBalance(), dntWeight, _amount);
  }

  /// @notice Stakes a certain amount of tokens for another user.
  /// @param _user Address of the user to stake for.
  /// @param _amount Amount of tokens to stake.
  /// @param _data Data field used for signalling in more complex staking applications.
  function stakeFor(address _user, uint _amount, bytes _data) public {
    uint returnAmount = calculateStakeReturn(_amount);

    dntBalance = dntBalance.add(_amount);

    mint(returnAmount);
    require(transfer(_user, returnAmount));

    super.stakeFor(_user, _amount, _data);

    maybeAdjustStakeDelta(_user, int(_amount));
  }

  /// @notice Unstakes a certain amount of tokens.
  /// @param _amount Amount of tokens to unstake.
  /// @param _data Data field used for signalling in more complex staking applications.
  function unstake(uint _amount, bytes _data) public {
    uint returnAmount = calculateUnstakeReturn(_amount);

    dntBalance = dntBalance.sub(returnAmount);

    allowed[msg.sender][this] = _amount;
    require(this.transferFrom(msg.sender, address(this), _amount));
    unmint(_amount);

    super.unstake(returnAmount, _data);

    // maybeAdjustStakeDelta(msg.sender, int(returnAmount) * -1);
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


  /**
   * @dev Available balance of reserve token in contract
   */
  // uint256 public poolBalance;


    /**
   * @dev Buy tokens
   * gas ~ 77825
   * TODO implement maxAmount that helps prevent miner front-running
   */
  // function buy() validGasPrice public payable returns(bool) {
  //   require(msg.value > 0);
  //   uint256 tokensToMint = calculatePurchaseReturn(totalSupply_, poolBalance, reserveRatio, msg.value);
  //   totalSupply_ = totalSupply_.add(tokensToMint);
  //   balances[msg.sender] = balances[msg.sender].add(tokensToMint);
  //   poolBalance = poolBalance.add(msg.value);
  //   LogMint(tokensToMint, msg.value);
  //   return true;
  // }

  /**
   * @dev Sell tokens
   * gas ~ 86936
   * @param sellAmount Amount of tokens to withdraw
   * TODO implement maxAmount that helps prevent miner front-running
   */
  // function sell(uint256 sellAmount) validGasPrice public returns(bool) {
  //   require(sellAmount > 0 && balances[msg.sender] >= sellAmount);
  //   uint256 ethAmount = calculateSaleReturn(totalSupply_, poolBalance, reserveRatio, sellAmount);
  //   msg.sender.transfer(ethAmount);
  //   poolBalance = poolBalance.sub(ethAmount);
  //   balances[msg.sender] = balances[msg.sender].sub(sellAmount);
  //   totalSupply_ = totalSupply_.sub(sellAmount);
  //   LogWithdraw(sellAmount, ethAmount);
  //   return true;
  // }

}




