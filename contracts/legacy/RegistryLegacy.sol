pragma solidity ^0.4.24;

/**
 Contract to support bringing legacy events after changing the event signature.
 This will not be deployed to the blockchain, but just used for generating the contract descriptor such that
 the events can be still searched using the old signature.
*/
contract RegistryLegacy {
	event DistrictConstructedEvent(address registryEntry, uint version, address creator, bytes metaHash, uint deposit, uint challengePeriodEnd, address stakeBank, address aragonDao, string aragonId, uint timestamp);
}
