pragma solidity ^0.4.24;

import "@aragon/os/contracts/common/IsContract.sol";
import "@aragon/kits-base/contracts/KitBase.sol";
import "@aragon/id/contracts/FIFSResolvingRegistrar.sol";

import "@aragon/apps-voting/contracts/Voting.sol";
import "@aragon/apps-vault/contracts/Vault.sol";
import "@aragon/apps-finance/contracts/Finance.sol";

import "@aragon/os/contracts/lib/ens/PublicResolver.sol"; // Not really required, just to make truffle happy

contract KitDistrict is KitBase, IsContract {
  FIFSResolvingRegistrar public aragonID;
  bytes32[3] public appIds;

  enum Apps { Voting, Vault, Finance}

  constructor(
    DAOFactory _fac,
    ENS _ens,
    FIFSResolvingRegistrar _aragonID,
    bytes32[3] _appIds
  )
  KitBase(_fac, _ens)
  public
  {
    require(isContract(address(_fac.regFactory())));

    aragonID = _aragonID;
    appIds = _appIds;
  }

  function createDAO(
    string _aragonId,
    MiniMeToken _token
  )
  public
  returns (
    Kernel dao,
    ACL acl,
    Finance finance,
    Vault vault,
    Voting voting
  )
  {
    dao = fac.newDAO(this);

    acl = ACL(dao.acl());

    acl.createPermission(this, dao, dao.APP_MANAGER_ROLE(), this);

    voting = Voting(
      dao.newAppInstance(
        appIds[uint8(Apps.Voting)],
        latestVersionAppBase(appIds[uint8(Apps.Voting)])
      )
    );
    emit InstalledApp(voting, appIds[uint8(Apps.Voting)]);

    vault = Vault(
      dao.newAppInstance(
        appIds[uint8(Apps.Vault)],
        latestVersionAppBase(appIds[uint8(Apps.Vault)]),
        new bytes(0),
        true
      )
    );
    emit InstalledApp(vault, appIds[uint8(Apps.Vault)]);

    finance = Finance(
      dao.newAppInstance(
        appIds[uint8(Apps.Finance)],
        latestVersionAppBase(appIds[uint8(Apps.Finance)])
      )
    );
    emit InstalledApp(finance, appIds[uint8(Apps.Finance)]);

    // permissions
    acl.createPermission(voting, voting, voting.MODIFY_QUORUM_ROLE(), voting);
    acl.createPermission(finance, vault, vault.TRANSFER_ROLE(), voting);
    acl.createPermission(voting, finance, finance.CREATE_PAYMENTS_ROLE(), voting);
    acl.createPermission(voting, finance, finance.EXECUTE_PAYMENTS_ROLE(), voting);
    acl.createPermission(voting, finance, finance.MANAGE_PAYMENTS_ROLE(), voting);

    // App inits
    vault.initialize();
    finance.initialize(vault, 30 days);
    voting.initialize(_token, 51^17, 25^17, 3 days);

    // EVMScriptRegistry permissions
    EVMScriptRegistry reg = EVMScriptRegistry(acl.getEVMScriptRegistry());
    acl.createPermission(voting, reg, reg.REGISTRY_ADD_EXECUTOR_ROLE(), voting);
    acl.createPermission(voting, reg, reg.REGISTRY_MANAGER_ROLE(), voting);

    // clean-up
    cleanupPermission(acl, voting, dao, dao.APP_MANAGER_ROLE());

    registerAragonID(_aragonId, dao);
    emit DeployInstance(dao);

    return (dao, acl, finance, vault, voting);
  }

  function registerAragonID(string name, address owner) internal {
    aragonID.register(keccak256(abi.encodePacked(name)), owner);
  }

}