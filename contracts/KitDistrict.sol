pragma solidity ^0.4.24;

import "./auth/DSAuth.sol";

import "@aragon/os/contracts/common/IsContract.sol";
import "@aragon/kits-base/contracts/KitBase.sol";
import "@aragon/id/contracts/FIFSResolvingRegistrar.sol";
import "@aragon/os/contracts/apm/APMNamehash.sol";

import "@aragon/apps-voting/contracts/Voting.sol";
import "@aragon/apps-vault/contracts/Vault.sol";
import "@aragon/apps-finance/contracts/Finance.sol";

// These are not really required, just to make truffle find artifacts
import "@aragon/os/contracts/factory/APMRegistryFactory.sol";
import "@aragon/os/contracts/lib/ens/PublicResolver.sol";

contract KitDistrict is KitBase, IsContract, APMNamehash, DSAuth {
  FIFSResolvingRegistrar public aragonID;
  bytes32[3] public appIds;

  enum Apps {Voting, Vault, Finance}

  uint64 public votingSupportRequiredPct = 51 ^ 17; // 51%
  uint64 public votingMinAcceptQuorumPct = 25 ^ 17; // 25%
  uint64 public votingVoteTime = 3 days;
  uint64 public financePeriodDuration = 30 days;

  constructor(
    DAOFactory _fac,
    ENS _ens,
    FIFSResolvingRegistrar _aragonID
  )
  KitBase(_fac, _ens)
  public
  {
    require(isContract(address(_fac.regFactory())));

    aragonID = _aragonID;
    appIds[uint8(Apps.Voting)] = apmNamehash("voting");
    appIds[uint8(Apps.Vault)] = apmNamehash("vault");
    appIds[uint8(Apps.Finance)] = apmNamehash("finance");
  }

  function createDAO(
    string _aragonId,
    MiniMeToken _token,
    address _creator
  )
  public
  returns (
    Kernel dao
  )
  {
    dao = fac.newDAO(this);

    ACL acl = ACL(dao.acl());

    acl.createPermission(this, dao, dao.APP_MANAGER_ROLE(), this);

    Voting voting = Voting(
      dao.newPinnedAppInstance(
        appIds[uint8(Apps.Voting)],
        latestVersionAppBase(appIds[uint8(Apps.Voting)])
      )
    );
    emit InstalledApp(voting, appIds[uint8(Apps.Voting)]);

    Vault vault = Vault(
      dao.newPinnedAppInstance(
        appIds[uint8(Apps.Vault)],
        latestVersionAppBase(appIds[uint8(Apps.Vault)]),
        new bytes(0),
        true
      )
    );
    emit InstalledApp(vault, appIds[uint8(Apps.Vault)]);

    Finance finance = Finance(
      dao.newPinnedAppInstance(
        appIds[uint8(Apps.Finance)],
        latestVersionAppBase(appIds[uint8(Apps.Finance)])
      )
    );
    emit InstalledApp(finance, appIds[uint8(Apps.Finance)]);

    // permissions
    acl.createPermission(voting, voting, voting.MODIFY_QUORUM_ROLE(), voting);
    acl.createPermission(voting, voting, voting.MODIFY_SUPPORT_ROLE(), voting);
    acl.createPermission(_creator, voting, voting.CREATE_VOTES_ROLE(), _creator);

    acl.createPermission(finance, vault, vault.TRANSFER_ROLE(), voting);
    acl.createPermission(voting, finance, finance.CREATE_PAYMENTS_ROLE(), voting);
    acl.createPermission(voting, finance, finance.EXECUTE_PAYMENTS_ROLE(), voting);
    acl.createPermission(voting, finance, finance.MANAGE_PAYMENTS_ROLE(), voting);

    // App inits
    vault.initialize();
    finance.initialize(vault, financePeriodDuration);
    voting.initialize(_token, votingSupportRequiredPct, votingMinAcceptQuorumPct, votingVoteTime);

    // EVMScriptRegistry permissions
    EVMScriptRegistry reg = EVMScriptRegistry(acl.getEVMScriptRegistry());
    acl.createPermission(voting, reg, reg.REGISTRY_ADD_EXECUTOR_ROLE(), voting);
    acl.createPermission(voting, reg, reg.REGISTRY_MANAGER_ROLE(), voting);

    // clean-up
    cleanupPermission(acl, voting, dao, dao.APP_MANAGER_ROLE());

    registerAragonID(_aragonId, dao);
    emit DeployInstance(dao);

    return dao;
  }

  function registerAragonID(string name, address owner) internal {
    aragonID.register(keccak256(abi.encodePacked(name)), owner);
  }

  function setVotingSupportRequiredPct(uint64 _votingSupportRequiredPct) public auth {
    votingSupportRequiredPct = _votingSupportRequiredPct;
  }

  function setVotingMinAcceptQuorumPct(uint64 _votingMinAcceptQuorumPct) public auth {
    votingMinAcceptQuorumPct = _votingMinAcceptQuorumPct;
  }

  function setVotingVoteTime(uint64 _votingVoteTime) public auth {
    votingVoteTime = _votingVoteTime;
  }

  function setFinancePeriodDuration(uint64 _financePeriodDuration) public auth {
    financePeriodDuration = _financePeriodDuration;
  }


}