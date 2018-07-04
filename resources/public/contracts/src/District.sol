pragma solidity ^0.4.18;

import "./RegistryEntry.sol"; 
// import "proxy/Forwarder.sol"; 
import "token/erc20/StandardToken.sol";
import "./District0xNetworkToken.sol";
import "token/erc900/TokenReturningStakeBank.sol";
/* import "./DistrictConfig.sol"; */

/**
 * @title Contract created for each submitted District into the DistrictFactory TCR.
 *
 * @dev It extends base RegistryEntry with additional state for storing IPFS hashes for a district data.
 * Full copy of this contract is NOT deployed with each submission in order to save gas. Only forwarder contracts
 * pointing into single intance of it.
 */

contract District is RegistryEntry
, StandardToken
, TokenReturningStakeBank 
{

  bytes public infoHash; // state variable for storing IPFS hash of file that contains all data from form fields

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
  )
  public
  {
    super.construct(_creator, _version);
    super.constructStakeBank(registryToken, this, 1);

    infoHash = _infoHash;
  }

  function setInfoHash(bytes _infoHash) public
  {
    require(msg.sender == creator);
    infoHash = _infoHash;
  }

  function mint(uint _amount)
  private
  {
    require(_amount > 0);
    totalSupply_ = totalSupply_.add(_amount);
    balances[address(this)] = balances[address(this)].add(_amount);
  }

  /// @notice Stakes a certain amount of tokens for another user.
  /// @param _user Address of the user to stake for.
  /// @param _amount Amount of tokens to stake.
  /// @param _data Data field used for signalling in more complex staking applications.
  function stakeFor(address _user, uint _amount, bytes _data) 
  public 
  {
    mint(_amount);
    super.stakeFor(_user, _amount, _data);
  }

  function unmint(uint _amount)
  private
  {
    require(_amount > 0);
    balances[address(this)] = balances[address(this)].sub(_amount);
    totalSupply_ = totalSupply_.sub(_amount);
  }

  /// @notice Unstakes a certain amount of tokens.
  /// @param _amount Amount of tokens to unstake.
  /// @param _data Data field used for signalling in more complex staking applications.
  function unstake(uint _amount, bytes _data) public {
    allowed[msg.sender][this] = _amount;
    super.unstake(_amount, _data);
    unmint(_amount);
  }

}
