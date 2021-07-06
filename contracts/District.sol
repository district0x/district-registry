pragma solidity ^0.4.24;

import "./RegistryEntry.sol";
import "./StakeBank.sol";
import "./proxy/Forwarder2.sol";
import "./DistrictChallenge.sol";
import "@aragon/os/contracts/lib/ens/ENS.sol";
import "@aragon/os/contracts/lib/ens/PublicResolver.sol";


/**
 * @title Contract created for each submitted District into the DistrictFactory TCR.
 *
 * @dev It extends base RegistryEntry with additional state for storing IPFS hashes for a district data.
 * Full copy of this contract is NOT deployed with each submission in order to save gas. Only forwarder contracts
 * pointing into single intance of it.
 */

contract District is RegistryEntry {

  StakeBank public stakeBank;
  ENS public constant ens = ENS(0xaAaAaAaaAaAaAaaAaAAAAAAAAaaaAaAaAaaAaaAa);

  /**
   * @dev IPFS hash of file that contains all data from form fields
   */
  bytes public metaHash;

  /**
   * @dev Constructor for this contract.
   * Native constructor is not used, because users create only forwarders pointing into single instance of this contract,
   * therefore constructor must be called explicitly.

   * @param _creator Creator of a district
   * @param _version Version of District contract
   * @param _metaHash IPFS hash of data related to a district
   * @param _ensNode Namehash of the ENS name registered by the sender
   * @param _ensName ENS name registered by the sender
   */
  function construct(
    address _creator,
    uint _version,
    bytes _metaHash,
    bytes32 _ensNode,
    string _ensName
  )
  public
  {
    super.construct(_creator, _version);
    // requires creator is owner of ENS domain and it does not have any linked snapshot
    require(_creator == ens.owner(_ensNode));
    address resolverAddr = ens.resolver(_ensNode);
    if (resolverAddr != address(0x0)) {
      PublicResolver resolver = PublicResolver(resolverAddr);
      require(bytes(resolver.text(_ensNode, "snapshot")).length == 0);
    }
    stakeBank = StakeBank(new Forwarder2());
    stakeBank.construct();
    challengePeriodEnd = ~uint(0);
    metaHash = _metaHash;
    registry.fireDistrictConstructedEvent(version, creator, metaHash, deposit, challengePeriodEnd, address(stakeBank), _ensName);
  }

  /**
   * @dev Sets new IPFS file for a district
   * Should be callable only by creator and only when district is in challengable state

   * @param _metaHash IPFS hash of data related to a district
   */
  function setMetaHash(bytes _metaHash)
  public
  {
    require(msg.sender == creator);
    require(isChallengeable());
    metaHash = _metaHash;
    registry.fireDistrictMetaHashChangedEvent(version, _metaHash);
  }

  /**
   * @dev Creates a new challenge for a district
   * District can be challenged multiple times, but not at the same time.
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
    super.createChallenge(_challenger, _challengeMetaHash);
    DistrictChallenge(currentChallenge()).setStakeBank(stakeBank);
  }

  /**
   * @dev Returns voting token balance for a user

   * @param _owner User owning voting token
   * @return Balance of a user
   */
  function balanceOf(address _owner)
  public constant returns (uint256 balance)
  {
    return stakeBank.balanceOf(_owner);
  }

  /**
   * @dev Stakes a certain amount of tokens for a user

   * @param _user Address of the user to stake for
   * @param _amount Amount of tokens to stake
   */
  function stakeFor(address _user, uint _amount)
  public
  {
    require(registryToken.transferFrom(_user, address(this), _amount));
    uint stakeId = stakeBank.stakeFor(_user, _amount);
    maybeAdjustStakeDelta(_user, int(_amount));
    registry.fireDistrictStakeChangedEvent(
      version,
      stakeId,
      stakeBank.totalStaked(),
      stakeBank.totalSupply(),
      _user,
      stakeBank.totalStakedFor(_user),
      stakeBank.balanceOf(_user),
      _amount,
      false
    );
  }


  /**
   * @dev Unstakes a certain amount of tokens

   * @param _amount Amount of tokens to unstake
   */
  function unstake(uint _amount)
  public
  {
    require(registryToken.transfer(msg.sender, _amount));
    uint stakeId = stakeBank.unstake(msg.sender, _amount);
    maybeAdjustStakeDelta(msg.sender, int(_amount) * - 1);
    registry.fireDistrictStakeChangedEvent(
      version,
      stakeId,
      stakeBank.totalStaked(),
      stakeBank.totalSupply(),
      msg.sender,
      stakeBank.totalStakedFor(msg.sender),
      stakeBank.balanceOf(msg.sender),
      _amount,
      true
    );
  }


  /**
   * @dev Keeps track of stake delta in case district is in vote commit period
   * This delta is then counted as votes for keeping a district in the registry

   * @param _voter Address of a user
   * @param _amount Amount of staked tokens
   */
  function maybeAdjustStakeDelta(
    address _voter,
    int _amount
  )
  private
  {
    if (challenges.length != 0 && currentChallenge().isVoteCommitPeriodActive()) {
      DistrictChallenge(currentChallenge()).adjustStakeDelta(_voter, _amount);
    }
  }

  /**
   * @dev Returns whether a district can be currently challenged

   * @return True if district can be challenged
   */
  function isChallengeable()
  public view returns (bool) {
    return isChallengePeriodActive() &&
    (challenges.length == 0 ||
    currentChallenge().status() == Challenge.Status.Whitelisted);
  }

}
