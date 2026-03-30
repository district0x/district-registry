pragma solidity ^0.4.24;

import "@aragon/os/contracts/lib/math/SafeMath.sol";
import "./auth/DSAuth.sol";
import "@aragon/apps-shared-minime/contracts/MiniMeToken.sol";

/**
 * @title PowerPlant - Treasury and bounty/grant distribution
 *
 * @dev Manages treasury funds and distributes via bounties and grants
 * Only ARIOracle can create bounties/grants after proposal approval
 */

contract PowerPlant is DSAuth {
    using SafeMath for uint256;

    enum BountyStatus { Open, Assigned, Completed, Claimed, Expired }

    struct Bounty {
        uint256 id;
        bytes metadataHash;      // IPFS hash: description, requirements
        uint256 reward;
        address token;           // Address of reward token (0x0 for ETH)
        uint256 deadline;
        address assignee;
        BountyStatus status;
        uint256 createdAt;
        bool exists;
    }

    struct Grant {
        uint256 id;
        address recipient;
        uint256 totalAmount;
        uint256 disbursed;
        uint256 vestingStart;
        uint256 vestingDuration;  // in seconds
        bytes purposeHash;        // IPFS hash of grant purpose
        address token;
        uint256 createdAt;
        bool exists;
    }

    address public ariOracle;
    bool private wasConstructed;

    uint256 public bountyCount;
    uint256 public grantCount;

    mapping(uint256 => Bounty) public bounties;
    mapping(uint256 => Grant) public grants;
    mapping(address => uint256) public treasuryBalance;  // token => balance

    // Events
    event TreasuryDeposit(address indexed token, uint256 amount, address indexed from, uint256 timestamp);
    event BountyCreated(
        uint256 indexed id,
        bytes metadataHash,
        uint256 reward,
        address token,
        uint256 deadline
    );
    event BountyAssigned(uint256 indexed id, address indexed assignee, uint256 timestamp);
    event BountyCompleted(uint256 indexed id, address indexed completer, uint256 timestamp);
    event BountyClaimed(uint256 indexed id, address indexed claimer, uint256 amount, uint256 timestamp);
    event GrantCreated(
        uint256 indexed id,
        address indexed recipient,
        uint256 totalAmount,
        address token,
        uint256 vestingDuration
    );
    event GrantDisbursement(uint256 indexed id, address indexed recipient, uint256 amount, uint256 timestamp);
    event TreasuryWithdrawal(address indexed token, address indexed recipient, uint256 amount, uint256 timestamp);

    /**
     * @dev Constructor
     */
    function construct(address _ariOracle)
        external
    {
        require(_ariOracle != 0x0, "PowerPlant: ARIOracle address can't be 0x0");
        require(!wasConstructed, "PowerPlant: Already constructed");

        ariOracle = _ariOracle;
        wasConstructed = true;
        owner = msg.sender;
    }

    /**
     * @dev Deposit funds to treasury
     * @param token Address of token (0x0 for ETH)
     */
    function deposit(address token)
        external
        payable
    {
        if (token == 0x0) {
            // ETH deposit
            require(msg.value > 0, "PowerPlant: ETH amount must be positive");
            treasuryBalance[0x0] = treasuryBalance[0x0].add(msg.value);
            emit TreasuryDeposit(0x0, msg.value, msg.sender, now);
        } else {
            // ERC20 deposit
            uint256 balanceBefore = MiniMeToken(token).balanceOf(this);
            require(
                MiniMeToken(token).transferFrom(msg.sender, this, msg.value),
                "PowerPlant: Token transfer failed"
            );
            uint256 balanceAfter = MiniMeToken(token).balanceOf(this);
            uint256 amount = balanceAfter.sub(balanceBefore);

            treasuryBalance[token] = treasuryBalance[token].add(amount);
            emit TreasuryDeposit(token, amount, msg.sender, now);
        }
    }

    /**
     * @dev Withdraw from treasury (only via ARIOracle proposal)
     * @param recipient Recipient address
     * @param amount Amount to withdraw
     */
    function withdrawToRecipient(address recipient, uint256 amount)
        external
    {
        require(msg.sender == ariOracle, "PowerPlant: Only ARIOracle");
        require(recipient != 0x0, "PowerPlant: Recipient can't be 0x0");
        require(amount > 0, "PowerPlant: Amount must be positive");

        // Default to ETH for simple transfers
        _transferFunds(0x0, recipient, amount);
        emit TreasuryWithdrawal(0x0, recipient, amount, now);
    }

    /**
     * @dev Create bounty from ARIOracle proposal
     * @param metadataHash IPFS hash with bounty details
     * @param reward Bounty reward amount
     * @param token Token address (0x0 for ETH)
     * @param deadline Completion deadline
     */
    function createBountyFromProposal(
        bytes metadataHash,
        uint256 reward,
        address token,
        uint256 deadline
    )
        external
    {
        require(msg.sender == ariOracle, "PowerPlant: Only ARIOracle");
        _createBounty(metadataHash, reward, token, deadline);
    }

    /**
     * @dev Create bounty (internal)
     */
    function _createBounty(
        bytes metadataHash,
        uint256 reward,
        address token,
        uint256 deadline
    )
        private
    {
        require(reward > 0, "PowerPlant: Reward must be positive");
        require(deadline > now, "PowerPlant: Deadline must be in future");
        require(treasuryBalance[token] >= reward, "PowerPlant: Insufficient treasury balance");

        bountyCount = bountyCount.add(1);
        uint256 bountyId = bountyCount;

        bounties[bountyId] = Bounty({
            id: bountyId,
            metadataHash: metadataHash,
            reward: reward,
            token: token,
            deadline: deadline,
            assignee: 0x0,
            status: BountyStatus.Open,
            createdAt: now,
            exists: true
        });

        // Reserve funds
        treasuryBalance[token] = treasuryBalance[token].sub(reward);

        emit BountyCreated(bountyId, metadataHash, reward, token, deadline);
    }

    /**
     * @dev Assign bounty to someone (only ARIOracle)
     * @param bountyId Bounty ID
     * @param assignee Address to assign to
     */
    function assignBounty(uint256 bountyId, address assignee)
        external
    {
        require(msg.sender == ariOracle, "PowerPlant: Only ARIOracle");
        require(bounties[bountyId].exists, "PowerPlant: Bounty doesn't exist");
        require(bounties[bountyId].status == BountyStatus.Open, "PowerPlant: Bounty not open");
        require(assignee != 0x0, "PowerPlant: Assignee can't be 0x0");

        bounties[bountyId].assignee = assignee;
        bounties[bountyId].status = BountyStatus.Assigned;

        emit BountyAssigned(bountyId, assignee, now);
    }

    /**
     * @dev Mark bounty as completed
     * @param bountyId Bounty ID
     */
    function completeBounty(uint256 bountyId)
        external
    {
        Bounty storage bounty = bounties[bountyId];
        require(bounty.exists, "PowerPlant: Bounty doesn't exist");
        require(
            msg.sender == bounty.assignee || msg.sender == ariOracle,
            "PowerPlant: Only assignee or ARI"
        );
        require(bounty.status == BountyStatus.Assigned, "PowerPlant: Bounty not assigned");

        bounty.status = BountyStatus.Completed;
        emit BountyCompleted(bountyId, msg.sender, now);
    }

    /**
     * @dev Claim bounty reward
     * @param bountyId Bounty ID
     */
    function claimBountyReward(uint256 bountyId)
        external
    {
        Bounty storage bounty = bounties[bountyId];
        require(bounty.exists, "PowerPlant: Bounty doesn't exist");
        require(msg.sender == bounty.assignee, "PowerPlant: Only assignee can claim");
        require(bounty.status == BountyStatus.Completed, "PowerPlant: Bounty not completed");

        bounty.status = BountyStatus.Claimed;

        // Transfer reward
        _transferFunds(bounty.token, bounty.assignee, bounty.reward);

        emit BountyClaimed(bountyId, bounty.assignee, bounty.reward, now);
    }

    /**
     * @dev Create grant from ARIOracle proposal
     * @param recipient Grant recipient address
     * @param totalAmount Total grant amount
     * @param vestingDuration Vesting period in seconds
     * @param purposeHash IPFS hash of grant purpose
     * @param token Token address (0x0 for ETH)
     */
    function createGrantFromProposal(
        address recipient,
        uint256 totalAmount,
        uint256 vestingDuration,
        bytes purposeHash,
        address token
    )
        external
    {
        require(msg.sender == ariOracle, "PowerPlant: Only ARIOracle");
        _createGrant(recipient, totalAmount, vestingDuration, purposeHash, token);
    }

    /**
     * @dev Create grant (internal)
     */
    function _createGrant(
        address recipient,
        uint256 totalAmount,
        uint256 vestingDuration,
        bytes purposeHash,
        address token
    )
        private
    {
        require(recipient != 0x0, "PowerPlant: Recipient can't be 0x0");
        require(totalAmount > 0, "PowerPlant: Amount must be positive");
        require(treasuryBalance[token] >= totalAmount, "PowerPlant: Insufficient treasury balance");

        grantCount = grantCount.add(1);
        uint256 grantId = grantCount;

        grants[grantId] = Grant({
            id: grantId,
            recipient: recipient,
            totalAmount: totalAmount,
            disbursed: 0,
            vestingStart: now,
            vestingDuration: vestingDuration,
            purposeHash: purposeHash,
            token: token,
            createdAt: now,
            exists: true
        });

        // Reserve funds
        treasuryBalance[token] = treasuryBalance[token].sub(totalAmount);

        emit GrantCreated(grantId, recipient, totalAmount, token, vestingDuration);
    }

    /**
     * @dev Disburse vested grant amount
     * @param grantId Grant ID
     */
    function disburseGrant(uint256 grantId)
        external
    {
        Grant storage grant = grants[grantId];
        require(grant.exists, "PowerPlant: Grant doesn't exist");

        uint256 vested = _calculateVestedAmount(grant);
        uint256 available = vested.sub(grant.disbursed);

        require(available > 0, "PowerPlant: No funds available to disburse");

        grant.disbursed = grant.disbursed.add(available);

        // Transfer vested amount
        _transferFunds(grant.token, grant.recipient, available);

        emit GrantDisbursement(grantId, grant.recipient, available, now);
    }

    /**
     * @dev Calculate vested amount for a grant
     */
    function _calculateVestedAmount(Grant storage grant)
        private
        view
        returns (uint256)
    {
        if (now < grant.vestingStart) {
            return 0;
        }

        if (grant.vestingDuration == 0 || now >= grant.vestingStart.add(grant.vestingDuration)) {
            // Fully vested
            return grant.totalAmount;
        }

        // Partial vesting
        uint256 elapsed = now.sub(grant.vestingStart);
        return grant.totalAmount.mul(elapsed).div(grant.vestingDuration);
    }

    /**
     * @dev Transfer funds (ETH or ERC20)
     */
    function _transferFunds(address token, address recipient, uint256 amount)
        private
    {
        if (token == 0x0) {
            // ETH transfer
            recipient.transfer(amount);
        } else {
            // ERC20 transfer
            require(
                MiniMeToken(token).transfer(recipient, amount),
                "PowerPlant: Token transfer failed"
            );
        }
    }

    /**
     * @dev Get bounty details
     */
    function getBounty(uint256 bountyId)
        external
        view
        returns (
            bytes metadataHash,
            uint256 reward,
            address token,
            uint256 deadline,
            address assignee,
            BountyStatus status
        )
    {
        Bounty storage bounty = bounties[bountyId];
        return (
            bounty.metadataHash,
            bounty.reward,
            bounty.token,
            bounty.deadline,
            bounty.assignee,
            bounty.status
        );
    }

    /**
     * @dev Get grant details
     */
    function getGrant(uint256 grantId)
        external
        view
        returns (
            address recipient,
            uint256 totalAmount,
            uint256 disbursed,
            uint256 vestingStart,
            uint256 vestingDuration,
            address token
        )
    {
        Grant storage grant = grants[grantId];
        return (
            grant.recipient,
            grant.totalAmount,
            grant.disbursed,
            grant.vestingStart,
            grant.vestingDuration,
            grant.token
        );
    }

    /**
     * @dev Fallback function to receive ETH
     */
    function() external payable {
        treasuryBalance[0x0] = treasuryBalance[0x0].add(msg.value);
        emit TreasuryDeposit(0x0, msg.value, msg.sender, now);
    }
}
