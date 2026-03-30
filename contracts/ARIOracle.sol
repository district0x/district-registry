pragma solidity ^0.4.24;

import "@aragon/os/contracts/lib/math/SafeMath.sol";
import "./auth/DSAuth.sol";

/**
 * @title ARIOracle - ARI proposal and veto mechanism
 *
 * @dev Allows ARI to propose actions weighted by council stakes
 * Community can veto proposals by shifting stake (30% threshold)
 * 3-day veto period before execution
 */

contract ARIOracle is DSAuth {
    using SafeMath for uint256;

    enum ProposalType { TreasuryTransfer, BountyCreation, GrantApproval, ParameterChange, CustomAction }
    enum ProposalStatus { Pending, VetoPeriod, Approved, Vetoed, Executed, Cancelled }

    struct Proposal {
        uint256 id;
        ProposalType proposalType;
        bytes32 reasoningHash;        // IPFS hash of ARI's full reasoning
        bytes32 councilWeightsHash;   // Snapshot of council weights at proposal time
        uint256 snapshotBlock;
        uint256 vetoDeadline;
        uint256 amount;               // For treasury transfers
        address recipient;            // For transfers/grants
        bytes callData;               // For custom actions
        ProposalStatus status;
        uint256 vetoStakeAgainst;     // Accumulated veto stake
        address proposer;             // Should be ARI address
        uint256 createdAt;
    }

    uint256 public vetoThresholdBps = 3000;      // 30% (basis points, 10000 = 100%)
    uint256 public vetoPeriodDuration = 3 days;

    address public ariAddress;                    // ARI's authorized signing address
    address public council;
    address public stakeBank;
    address public powerPlant;

    bool private wasConstructed;
    uint256 public proposalCount;

    mapping(uint256 => Proposal) public proposals;
    mapping(uint256 => mapping(address => uint256)) public vetoStakes;  // proposalId => voter => amount

    // Events
    event ProposalCreated(
        uint256 indexed id,
        ProposalType proposalType,
        bytes32 reasoningHash,
        uint256 vetoDeadline,
        address proposer
    );
    event VetoCast(uint256 indexed proposalId, address indexed voter, uint256 amount, uint256 totalVetoStake);
    event ProposalApproved(uint256 indexed id, uint256 timestamp);
    event ProposalVetoed(uint256 indexed id, uint256 totalVetoStake, uint256 timestamp);
    event ProposalExecuted(uint256 indexed id, uint256 timestamp);
    event ProposalCancelled(uint256 indexed id, uint256 timestamp);
    event ARIAddressUpdated(address indexed oldAddress, address indexed newAddress);

    /**
     * @dev Constructor
     */
    function construct(
        address _ariAddress,
        address _council,
        address _stakeBank,
        address _powerPlant
    )
        external
    {
        require(_ariAddress != 0x0, "ARIOracle: ARI address can't be 0x0");
        require(_council != 0x0, "ARIOracle: Council address can't be 0x0");
        require(_stakeBank != 0x0, "ARIOracle: StakeBank address can't be 0x0");
        require(_powerPlant != 0x0, "ARIOracle: PowerPlant address can't be 0x0");
        require(!wasConstructed, "ARIOracle: Already constructed");

        ariAddress = _ariAddress;
        council = _council;
        stakeBank = _stakeBank;
        powerPlant = _powerPlant;
        wasConstructed = true;
        owner = msg.sender;
    }

    /**
     * @dev Update ARI's authorized address
     * @param newAddress New ARI address
     */
    function updateARIAddress(address newAddress)
        external
        auth
    {
        require(newAddress != 0x0, "ARIOracle: New address can't be 0x0");
        address oldAddress = ariAddress;
        ariAddress = newAddress;
        emit ARIAddressUpdated(oldAddress, newAddress);
    }

    /**
     * @dev Create a new proposal (only ARI can call)
     * @param proposalType Type of proposal
     * @param reasoningHash IPFS hash of ARI's reasoning document
     * @param amount Amount for transfers/grants
     * @param recipient Recipient address for transfers/grants
     * @param callData Call data for custom actions
     */
    function createProposal(
        ProposalType proposalType,
        bytes32 reasoningHash,
        uint256 amount,
        address recipient,
        bytes callData
    )
        external
        returns (uint256)
    {
        require(msg.sender == ariAddress, "ARIOracle: Only ARI can create proposals");
        require(reasoningHash != bytes32(0), "ARIOracle: Reasoning hash required");

        proposalCount = proposalCount.add(1);
        uint256 proposalId = proposalCount;

        // Snapshot current council weights
        bytes32 weightsHash = _snapshotCouncilWeights();

        proposals[proposalId] = Proposal({
            id: proposalId,
            proposalType: proposalType,
            reasoningHash: reasoningHash,
            councilWeightsHash: weightsHash,
            snapshotBlock: block.number,
            vetoDeadline: now.add(vetoPeriodDuration),
            amount: amount,
            recipient: recipient,
            callData: callData,
            status: ProposalStatus.VetoPeriod,
            vetoStakeAgainst: 0,
            proposer: msg.sender,
            createdAt: now
        });

        emit ProposalCreated(
            proposalId,
            proposalType,
            reasoningHash,
            proposals[proposalId].vetoDeadline,
            msg.sender
        );

        return proposalId;
    }

    /**
     * @dev Cast veto vote against a proposal
     * @param proposalId ID of proposal to veto
     * @param amount Amount of stake weight to add to veto
     */
    function castVeto(uint256 proposalId, uint256 amount)
        external
    {
        Proposal storage proposal = proposals[proposalId];
        require(proposal.id != 0, "ARIOracle: Proposal doesn't exist");
        require(proposal.status == ProposalStatus.VetoPeriod, "ARIOracle: Not in veto period");
        require(now <= proposal.vetoDeadline, "ARIOracle: Veto period ended");
        require(amount > 0, "ARIOracle: Amount must be positive");

        // Add veto stake
        vetoStakes[proposalId][msg.sender] = vetoStakes[proposalId][msg.sender].add(amount);
        proposal.vetoStakeAgainst = proposal.vetoStakeAgainst.add(amount);

        emit VetoCast(proposalId, msg.sender, amount, proposal.vetoStakeAgainst);

        // Check if veto threshold reached
        if (_isVetoed(proposalId)) {
            proposal.status = ProposalStatus.Vetoed;
            emit ProposalVetoed(proposalId, proposal.vetoStakeAgainst, now);
        }
    }

    /**
     * @dev Execute an approved proposal after veto period
     * @param proposalId ID of proposal to execute
     */
    function executeProposal(uint256 proposalId)
        external
    {
        Proposal storage proposal = proposals[proposalId];
        require(proposal.id != 0, "ARIOracle: Proposal doesn't exist");
        require(proposal.status == ProposalStatus.VetoPeriod, "ARIOracle: Not in veto period");
        require(now > proposal.vetoDeadline, "ARIOracle: Veto period not ended");
        require(!_isVetoed(proposalId), "ARIOracle: Proposal was vetoed");

        proposal.status = ProposalStatus.Approved;
        emit ProposalApproved(proposalId, now);

        // Execute based on proposal type
        _executeProposalAction(proposal);

        proposal.status = ProposalStatus.Executed;
        emit ProposalExecuted(proposalId, now);
    }

    /**
     * @dev Cancel a proposal (emergency, governance only)
     * @param proposalId ID of proposal to cancel
     */
    function cancelProposal(uint256 proposalId)
        external
        auth
    {
        Proposal storage proposal = proposals[proposalId];
        require(proposal.id != 0, "ARIOracle: Proposal doesn't exist");
        require(
            proposal.status == ProposalStatus.Pending || proposal.status == ProposalStatus.VetoPeriod,
            "ARIOracle: Cannot cancel executed proposal"
        );

        proposal.status = ProposalStatus.Cancelled;
        emit ProposalCancelled(proposalId, now);
    }

    /**
     * @dev Check if proposal is vetoed (30% threshold)
     */
    function _isVetoed(uint256 proposalId)
        private
        view
        returns (bool)
    {
        Proposal storage proposal = proposals[proposalId];

        // Get total stake at snapshot block
        uint256 totalStake = CouncilStakeBank(stakeBank).totalStaked();
        if (totalStake == 0) {
            return false;
        }

        // Calculate veto percentage in basis points
        uint256 vetoPercentage = proposal.vetoStakeAgainst.mul(10000).div(totalStake);

        return vetoPercentage >= vetoThresholdBps;
    }

    /**
     * @dev Execute the proposal action based on type
     */
    function _executeProposalAction(Proposal storage proposal)
        private
    {
        if (proposal.proposalType == ProposalType.TreasuryTransfer) {
            PowerPlant(powerPlant).withdrawToRecipient(proposal.recipient, proposal.amount);
        }
        // TODO: Implement bounty/grant creation via explicit function calls
        // For now, these are commented out pending ARI API implementation
        // else if (proposal.proposalType == ProposalType.BountyCreation) {
        //     PowerPlant(powerPlant).createBountyFromProposal(...);
        // } else if (proposal.proposalType == ProposalType.GrantApproval) {
        //     PowerPlant(powerPlant).createGrantFromProposal(...);
        // }
        else if (proposal.proposalType == ProposalType.CustomAction) {
            // Execute custom call
            require(proposal.recipient.call(proposal.callData), "ARIOracle: Custom action failed");
        }
        // ParameterChange would modify contract state directly
    }

    /**
     * @dev Snapshot council weights (simplified - just store hash)
     */
    function _snapshotCouncilWeights()
        private
        view
        returns (bytes32)
    {
        // In production, this would store actual weights array
        // For now, just hash the block number as snapshot reference
        return keccak256(abi.encodePacked(block.number, council));
    }

    /**
     * @dev Get proposal details
     */
    function getProposal(uint256 proposalId)
        external
        view
        returns (
            ProposalType proposalType,
            bytes32 reasoningHash,
            uint256 vetoDeadline,
            ProposalStatus status,
            uint256 vetoStakeAgainst,
            address proposer
        )
    {
        Proposal storage proposal = proposals[proposalId];
        return (
            proposal.proposalType,
            proposal.reasoningHash,
            proposal.vetoDeadline,
            proposal.status,
            proposal.vetoStakeAgainst,
            proposal.proposer
        );
    }

    /**
     * @dev Check current veto percentage for a proposal
     */
    function getVetoPercentage(uint256 proposalId)
        external
        view
        returns (uint256)
    {
        Proposal storage proposal = proposals[proposalId];
        uint256 totalStake = CouncilStakeBank(stakeBank).totalStaked();

        if (totalStake == 0) {
            return 0;
        }

        return proposal.vetoStakeAgainst.mul(10000).div(totalStake);
    }
}

// Interface for CouncilStakeBank
contract CouncilStakeBank {
    function totalStaked() public view returns (uint256);
}

// Interface for PowerPlant
contract PowerPlant {
    function withdrawToRecipient(address recipient, uint256 amount) external;
    function createBountyFromProposal(bytes metadataHash, uint256 reward, address token, uint256 deadline) external;
    function createGrantFromProposal(address recipient, uint256 totalAmount, uint256 vestingDuration, bytes purposeHash, address token) external;
}
