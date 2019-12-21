pragma solidity ^0.4.24;

import "./auth/DSAuth.sol";

import "@aragon/id/contracts/FIFSResolvingRegistrar.sol";

import "@aragon/templates-shared/contracts/BaseTemplate.sol";

import "@aragon/apps-shared-minime/contracts/MiniMeToken.sol";


/**
 * @title Contract responsible for creating and configuring Aragon DAO related to a District
 */

contract TemplateDistrict is BaseTemplate, DSAuth {
    string constant private ERROR_BAD_VOTE_SETTINGS = "BAD_VOTE_SETTINGS";

    FIFSResolvingRegistrar public aragonID;

    enum Apps {Voting, Vault, Finance}

    uint64 public votingSupportRequiredPct = 50 ^ 17; // 50%
    uint64 public votingMinAcceptQuorumPct = 25 ^ 17; // 25%
    uint64 public votingVoteTime = uint64(3 days);
    uint64 public financePeriodDuration = uint64(30 days);

    mapping(uint8 => bool) public includeApp;

    /**
    * @dev Constructor for this contract

    * @param _fac Address of Aragon DAOFactory contract
    * @param _ens Address of ENS contract
    * @param _aragonID Address of AragonID registrar contract
    * @param _includeApps Collection of Aragon Apps that should be preconfigured for each district
    */
    constructor (
        DAOFactory _daoFactory,
        ENS _ens,
        MiniMeTokenFactory _miniMeFactory,
        IFIFSResolvingRegistrar _aragonID,
        Apps[] _includeApps
    )
        BaseTemplate(_daoFactory, _ens, _miniMeFactory, _aragonID)
        public
    {
        _ensureAragonIdIsValid(_aragonID);
        _ensureMiniMeFactoryIsValid(_miniMeFactory);
        for (uint i; i < _includeApps.length; i++) {
          setAppIncluded(_includeApps[i], true);
    }

    /**
    * @dev Creates and configures an Aragon DAO
    * @param _id ENS name registered as <somename>.aragonid.eth
    * @param _token Address of token that will be used as voting token within Aragon DAO
    * @param _creator Address of the Aragon DAO creator
    * @param _votingSettings Array of [supportRequired, minAcceptanceQuorum, voteDuration] to set up the voting app of the organization
    * @param _financePeriod Initial duration for accounting periods, it can be set to zero in order to use the default of 30 days.
    * @param _useAgentAsVault Boolean to tell whether to use an Agent app as a more advanced form of Vault app
    */
    function newInstance(
        string memory _id,
        string _id,
        MiniMeToken _token,
        address _creator,
        bool _useAgentAsVault 
    )
        internal
    {
        _ensureTemplateSettings(_token, _votingSettings);
        (Kernel dao, ACL acl) = _createDAO();
        (, Voting voting) = _setupApps(dao, acl, _token, _useAgentAsVault);
        if (isAppIncluded(Apps.Voting) {
            _transferRootPermissionsFromTemplateAndFinalizeDAO(dao, voting);
        } else {
            _transferRootPermissionsFromTemplateAndFinalizeDAO(dao, _creator);
        }
        
        _registerID(_id, dao);
    }

    function _setupApps(
        Kernel _dao,
        ACL _acl,
        MiniMeToken _token,
        address _creator,
        bool _useAgentAsVault
    )
        internal
        returns (Finance, Voting)
    {
        if (isAppIncluded(Apps.Vault) || isAppIncluded(Apps.Finance)) {
          Vault agentOrVault = _useAgentAsVault ? _installDefaultAgentApp(_dao) : _installVaultApp(_dao);
        }

        if (isAppIncluded(Apps.Finance)) {
          Finance finance = _installFinanceApp(_dao, agentOrVault, financePeriodDuration);
        }

        if (isAppIncluded(Apps.Voting)) {
          Voting voting = _installVotingApp(_dao, token, [votingSupportRequiredPct, votingMinAcceptQuorumPct, votingVoteTime]);
        }

        // Setup permissions
        if (isAppIncluded(Apps.Voting)) {
          _createVotingPermissions(_acl, _voting, _voting, _creator, _voting);
        }

        if (isAppIncluded(Apps.Voting) && isAppIncluded(Apps.Finance)) {
          if (_useAgentAsVault) {
            _createAgentPermissions(_acl, Agent(_agentOrVault), _voting, _voting);
          }
          _createVaultPermissions(_acl, _agentOrVault, _finance, _voting);
          _createFinancePermissions(_acl, _finance, _voting, _voting);
        }

        _createEvmScriptsRegistryPermissions(_acl, _voting, _voting);

        return (finance, voting);
      }


    function _ensureTemplateSettings(
        MiniMeToken _token, 
        uint64[3] memory _votingSettings
    )
        private
        pure
    {
        require(address(_token) != address(0));
        require(_votingSettings.length == 3, ERROR_BAD_VOTE_SETTINGS);
    }


    /**
    * @dev Registers ENS name at <somename>.aragonid.eth

    * @param name Name to register
    * @param owner Owner of the name
    */
    function registerAragonID(string name, address owner) internal {
        aragonID.register(keccak256(abi.encodePacked(name)), owner);
    }

    /**
    * @dev Sets votingSupportRequiredPct parameter that will be preconfigured for all Aragon DAOs
    * Can be called only by authorized address
    * @param _votingSupportRequiredPct Percentage of voting support required
    */
    function setVotingSupportRequiredPct(uint64 _votingSupportRequiredPct) public auth {
        votingSupportRequiredPct = _votingSupportRequiredPct;
    }

    /**
    * @dev Sets votingMinAcceptQuorumPct parameter that will be preconfigured for all Aragon DAOs
    * Can be called only by authorized address
    * @param _votingMinAcceptQuorumPct Percentage of minimum accept quorum
    */
    function setVotingMinAcceptQuorumPct(uint64 _votingMinAcceptQuorumPct) public auth {
        votingMinAcceptQuorumPct = _votingMinAcceptQuorumPct;
    }

    /**
    * @dev Sets votingVoteTime parameter that will be preconfigured for all Aragon DAOs
    * Can be called only by authorized address
    * @param _votingVoteTime Voting vote time
    */
    function setVotingVoteTime(uint64 _votingVoteTime) public auth {
        votingVoteTime = _votingVoteTime;
    }

    /**
    * @dev Sets financePeriodDuration parameter that will be preconfigured for all Aragon DAOs
    * Can be called only by authorized address
    * @param _financePeriodDuration Finance period duration
    */
    function setFinancePeriodDuration(uint64 _financePeriodDuration) public auth {
        financePeriodDuration = _financePeriodDuration;
    }

    /**
    * @dev Sets which Aragon Apps will be preconfigured for all Aragon DAOs
    * Can be called only by authorized address
    * @param _includeApps IDs of apps
    * @param _includeApps Boolean if app should be included or not
    */
    function setAppsIncluded(Apps[] _includeApps, bool[] _isIncluded) public auth {
        require(_includeApps.length == _isIncluded.length);
        for (uint i; i < _includeApps.length; i++) {
          setAppIncluded(_includeApps[i], _isIncluded[i]);
        }
    }

    /**
    * @dev Sets whether a Aragon App should be preconfigured for all Aragon DAOs
    * Can be called only by authorized address
    * @param _app ID of app
    * @param _isIncluded Boolean if app should be included or not
    */
    function setAppIncluded(Apps _app, bool _isIncluded) public auth {
        includeApp[uint8(_app)] = _isIncluded;
    }

    /**
    * @dev Returns if app is currently being preconfigured for app Aragon DAOs
    * @param _app ID of app
    * @return Boolean if app is included or not
    */
    function isAppIncluded(Apps _app) public view returns (bool){
        return includeApp[uint8(_app)];
    }

}
