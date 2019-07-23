pragma solidity ^0.4.24;

import "./auth/DSAuth.sol";
import "./db/EternalDb.sol";
import "./proxy/MutableForwarder.sol"; // Keep it included despite not being used (for compiler)

/**
 * @title Central contract for TCR registry
 *
 * @dev Manages state about deployed registry entries and factories
 * Serves as a central point for firing all registry entry events
 * This contract is not accessed directly, but through MutableForwarder. See MutableForwarder.sol for more comments.
 */

contract Registry is DSAuth {

  address private target; // Keep it here, because this contract is deployed as MutableForwarder

  event ChallengeCreatedEvent(address registryEntry, uint version, uint index, address challenger, uint commitPeriodEnd, uint revealPeriodEnd, uint rewardPool, bytes metaHash, uint timestamp);
  event ChallengerRewardClaimedEvent(address registryEntry, uint version, uint index, address challenger, uint amount, uint timestamp);
  event CreatorRewardClaimedEvent(address registryEntry, uint version, uint index, address creator, uint amount, uint timestamp);
  event DistrictConstructedEvent(address registryEntry, uint version, address creator, bytes metaHash, uint deposit, uint challengePeriodEnd, uint32 dntWeight, address aragonDao, string aragonId, uint timestamp);
  event DistrictStakeChangedEvent(address registryEntry, uint version, uint dntTotalStaked, uint votingTokenTotalSupply, address staker, uint stakerDntStaked, uint stakerVotingTokenBalance, uint stakedAmount, bool isUnstake, uint timestamp);
  event DistrictMetaHashChangedEvent(address registryEntry, uint version, bytes metaHash, uint timestamp);
  event VotesReclaimedEvent(address registryEntry, uint version, uint index, address voter, uint amount, uint timestamp);
  event VoteCommittedEvent(address registryEntry, uint version, uint index, address voter, uint amount, uint commitPeriodEnd, uint timestamp);
  event VoteRevealedEvent(address registryEntry, uint version, uint index, address voter, uint option, uint amount, uint revealPeriodEnd, uint timestamp);
  event VoteRewardClaimedEvent(address registryEntry, uint version, uint index, address voter, uint amount, uint timestamp);

  bytes32 public constant challengeDispensationKey = keccak256("challengeDispensation");
  bytes32 public constant challengePeriodDurationKey = keccak256("challengePeriodDuration");
  bytes32 public constant commitPeriodDurationKey = keccak256("commitPeriodDuration");
  bytes32 public constant depositKey = keccak256("deposit");
  bytes32 public constant maxAuctionDurationKey = keccak256("maxAuctionDuration");
  bytes32 public constant maxTotalSupplyKey = keccak256("maxTotalSupply");
  bytes32 public constant revealPeriodDurationKey = keccak256("revealPeriodDuration");
  bytes32 public constant voteQuorumKey = keccak256("voteQuorum");

  EternalDb public db;
  bool private wasConstructed;

  /**
   * @dev Constructor for this contract.
   * Native constructor is not used, because we use a forwarder pointing to single instance of this contract,
   * therefore constructor must be called explicitly.
   * @param _db Address of EternalDb related to this registry
   */
  function construct(EternalDb _db) 
    external
  {
    require(address(_db) != 0x0, "Registry: Address can't be 0x0");
    db = _db;
    wasConstructed = true;
    owner = msg.sender;
  }

  modifier onlyFactory() {
    require(isFactory(msg.sender), "Registry: Sender should be factory");
    _;
  }

  modifier onlyRegistryEntry() {
    require(isRegistryEntry(msg.sender), "Registry: Sender should registry entry");
    _;
  }

  modifier notEmergency() {
    require(!isEmergency(),"Registry: Emergency mode is enable");
    _;
  }

  /**
   * @dev Sets whether address is factory allowed to add registry entries into registry
   * Must be callable only by authenticated user
   * @param _factory Address of a factory contract
   * @param _isFactory Whether the address is allowed factory
   */
  function setFactory(address _factory, bool _isFactory)
    external
    auth
  {
    db.setBooleanValue(keccak256(abi.encodePacked("isFactory", _factory)), _isFactory);
  }

  /**
   * @dev Adds address as valid registry entry into the Registry
   * Must be callable only by allowed factory contract
   * @param _registryEntry Address of new registry entry
   */
  function addRegistryEntry(address _registryEntry)
    external
    onlyFactory
    notEmergency
  {
    db.setBooleanValue(keccak256(abi.encodePacked("isRegistryEntry", _registryEntry)), true);
  }

  /**
   * @dev Sets emergency state to pause all trading operations
   * Must be callable only by authenticated user
   * @param _isEmergency True if emergency is happening
   */
  function setEmergency(bool _isEmergency)
    external
    auth
  {
    db.setBooleanValue("isEmergency", _isEmergency);
  }

  /**
   * @dev Returns whether address is valid registry entry factory

   * @return True if address is factory
   */
  function isFactory(address factory)
    public constant returns (bool)
  {
    return db.getBooleanValue(keccak256(abi.encodePacked("isFactory", factory)));
  }

  /**
   * @dev Returns whether address is valid registry entry
   * @return True if address is registry entry
   */
  function isRegistryEntry(address registryEntry)
    public constant returns (bool)
  {
    return db.getBooleanValue(keccak256(abi.encodePacked("isRegistryEntry", registryEntry)));
  }

  /**
   * @dev Returns whether emergency stop is happening

   * @return True if emergency is happening
   */
  function isEmergency()
    public constant returns (bool)
  {
    return db.getBooleanValue("isEmergency");
  }


  function fireDistrictConstructedEvent(
    uint version,
    address creator,
    bytes metaHash,
    uint deposit,
    uint challengePeriodEnd,
    uint32 dntWeight,
    address aragonDao,
    string aragonId
  )
    public
    onlyRegistryEntry
  {
    emit DistrictConstructedEvent(msg.sender, version, creator, metaHash, deposit, challengePeriodEnd, dntWeight, aragonDao, aragonId, now);
  }
  function fireDistrictStakeChangedEvent(uint version, uint dntTotalStaked, uint votingTokenTotalSupply, address staker, uint stakerDntStaked, uint stakerVotingTokenBalance, uint stakedAmount, bool isUnstake)
    public
    onlyRegistryEntry
  {
    emit DistrictStakeChangedEvent(msg.sender, version, dntTotalStaked, votingTokenTotalSupply, staker, stakerDntStaked, stakerVotingTokenBalance, stakedAmount, isUnstake, now);
  }

  function fireDistrictMetaHashChangedEvent(uint version, bytes metahash)
    public
    onlyRegistryEntry
  {
    emit DistrictMetaHashChangedEvent(msg.sender, version, metahash, now);
  }

  function fireChallengeCreatedEvent(
    uint version,
    uint index,
    address challenger,
    uint commitPeriodEnd,
    uint revealPeriodEnd,
    uint rewardPool,
    bytes metaHash
  )
    public
    onlyRegistryEntry
  {
    emit ChallengeCreatedEvent(msg.sender, version, index, challenger, commitPeriodEnd, revealPeriodEnd, rewardPool, metaHash, now);
  }

  function fireVoteCommittedEvent(uint version, uint index, address voter, uint amount, uint commitPeriodEnd)
    public
    onlyRegistryEntry
  {
    emit VoteCommittedEvent(msg.sender, version, index, voter, amount, commitPeriodEnd, now);
  }

  function fireVoteRevealedEvent(uint version, uint index, address voter, uint option, uint amount, uint revealPeriodEnd)
    public
    onlyRegistryEntry
  {
    emit VoteRevealedEvent(msg.sender, version, index, voter, option, amount, revealPeriodEnd, now);
  }

  function fireVotesReclaimedEvent(uint version, uint index, address voter, uint amount)
    public
    onlyRegistryEntry
  {
    emit VotesReclaimedEvent(msg.sender, version, index, voter, amount, now);
  }

  function fireVoteRewardClaimedEvent(uint version, uint index, address voter, uint amount)
    public
    onlyRegistryEntry
  {
    emit VoteRewardClaimedEvent(msg.sender, version, index, voter, amount, now);
  }

  function fireChallengerRewardClaimedEvent(uint version, uint index, address challenger, uint amount)
    public
    onlyRegistryEntry
  {
    emit ChallengerRewardClaimedEvent(msg.sender, version, index, challenger, amount, now);
  }

  function fireCreatorRewardClaimedEvent(uint version, uint index, address creator, uint amount)
    public
    onlyRegistryEntry
  {
    emit CreatorRewardClaimedEvent(msg.sender, version, index, creator, amount, now);
  }

}
