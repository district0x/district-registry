pragma solidity ^0.4.24;

import "@aragon/os/contracts/lib/math/SafeMath.sol";
import "./auth/DSAuth.sol";
import "@aragon/apps-shared-minime/contracts/MiniMeToken.sol";

/**
 * @title CouncilStakeBank - DNT staking per council seat
 *
 * @dev Manages DNT staking for the 9 council seats
 * Uses checkpoint pattern for historical stake queries
 * Similar to StakeBank.sol but tracks per-seat stakes
 */

contract CouncilStakeBank is DSAuth {
    using SafeMath for uint256;

    uint8 public constant SEAT_COUNT = 9;

    struct StakeCheckpoint {
        uint256 blockNumber;
        uint256 amount;
    }

    // Mappings for stake tracking
    mapping(uint8 => mapping(address => StakeCheckpoint[])) public stakesPerSeat;  // seatIndex => staker => checkpoints
    mapping(uint8 => StakeCheckpoint[]) public totalStakePerSeat;  // seatIndex => total checkpoints
    StakeCheckpoint[] public globalStakeHistory;  // All stakes combined

    address public dntToken;
    address public council;
    bool private wasConstructed;

    // Events
    event StakedOnMind(address indexed staker, uint8 indexed seatIndex, uint256 amount, uint256 newBalance);
    event UnstakedFromMind(address indexed staker, uint8 indexed seatIndex, uint256 amount, uint256 newBalance);
    event StakeMoved(address indexed staker, uint8 fromSeat, uint8 toSeat, uint256 amount);

    /**
     * @dev Constructor
     * @param _dntToken Address of DNT token contract
     * @param _council Address of Council contract
     */
    function construct(address _dntToken, address _council)
        external
    {
        require(_dntToken != 0x0, "CouncilStakeBank: DNT address can't be 0x0");
        require(_council != 0x0, "CouncilStakeBank: Council address can't be 0x0");
        require(!wasConstructed, "CouncilStakeBank: Already constructed");

        dntToken = _dntToken;
        council = _council;
        wasConstructed = true;
        owner = msg.sender;
    }

    /**
     * @dev Stake DNT on a council seat
     * @param seatIndex Index of the seat to stake on
     * @param amount Amount of DNT to stake
     */
    function stakeOnSeat(uint8 seatIndex, uint256 amount)
        external
    {
        require(seatIndex < SEAT_COUNT, "CouncilStakeBank: Invalid seat index");
        require(amount > 0, "CouncilStakeBank: Amount must be positive");

        // Transfer DNT from staker to this contract
        require(
            MiniMeToken(dntToken).transferFrom(msg.sender, this, amount),
            "CouncilStakeBank: DNT transfer failed"
        );

        // Update checkpoints
        updateCheckpoint(stakesPerSeat[seatIndex][msg.sender], amount, false);
        updateCheckpoint(totalStakePerSeat[seatIndex], amount, false);
        updateCheckpoint(globalStakeHistory, amount, false);

        uint256 newBalance = totalStakedFor(msg.sender, seatIndex);

        // Update Council cache
        Council(council).updateStakeCache(seatIndex, totalStakedForSeat(seatIndex));

        emit StakedOnMind(msg.sender, seatIndex, amount, newBalance);
    }

    /**
     * @dev Unstake DNT from a council seat
     * @param seatIndex Index of the seat to unstake from
     * @param amount Amount of DNT to unstake
     */
    function unstakeFromSeat(uint8 seatIndex, uint256 amount)
        external
    {
        require(seatIndex < SEAT_COUNT, "CouncilStakeBank: Invalid seat index");
        require(amount > 0, "CouncilStakeBank: Amount must be positive");

        uint256 currentStake = totalStakedFor(msg.sender, seatIndex);
        require(currentStake >= amount, "CouncilStakeBank: Insufficient stake");

        // Update checkpoints
        updateCheckpoint(stakesPerSeat[seatIndex][msg.sender], amount, true);
        updateCheckpoint(totalStakePerSeat[seatIndex], amount, true);
        updateCheckpoint(globalStakeHistory, amount, true);

        // Transfer DNT back to staker
        require(
            MiniMeToken(dntToken).transfer(msg.sender, amount),
            "CouncilStakeBank: DNT transfer failed"
        );

        uint256 newBalance = totalStakedFor(msg.sender, seatIndex);

        // Update Council cache
        Council(council).updateStakeCache(seatIndex, totalStakedForSeat(seatIndex));

        emit UnstakedFromMind(msg.sender, seatIndex, amount, newBalance);
    }

    /**
     * @dev Move stake from one seat to another in a single transaction
     * @param fromSeat Source seat index
     * @param toSeat Destination seat index
     * @param amount Amount to move
     */
    function moveStake(uint8 fromSeat, uint8 toSeat, uint256 amount)
        external
    {
        require(fromSeat < SEAT_COUNT, "CouncilStakeBank: Invalid from seat");
        require(toSeat < SEAT_COUNT, "CouncilStakeBank: Invalid to seat");
        require(fromSeat != toSeat, "CouncilStakeBank: Cannot move to same seat");
        require(amount > 0, "CouncilStakeBank: Amount must be positive");

        uint256 currentStake = totalStakedFor(msg.sender, fromSeat);
        require(currentStake >= amount, "CouncilStakeBank: Insufficient stake");

        // Update from seat (unstake)
        updateCheckpoint(stakesPerSeat[fromSeat][msg.sender], amount, true);
        updateCheckpoint(totalStakePerSeat[fromSeat], amount, true);

        // Update to seat (stake)
        updateCheckpoint(stakesPerSeat[toSeat][msg.sender], amount, false);
        updateCheckpoint(totalStakePerSeat[toSeat], amount, false);

        // Global stake unchanged (just moving between seats)

        // Update Council caches for both seats
        Council(council).updateStakeCache(fromSeat, totalStakedForSeat(fromSeat));
        Council(council).updateStakeCache(toSeat, totalStakedForSeat(toSeat));

        emit StakeMoved(msg.sender, fromSeat, toSeat, amount);
    }

    /**
     * @dev Get current stake for an address on a specific seat
     * @param staker Address of the staker
     * @param seatIndex Seat index
     * @return Current stake amount
     */
    function totalStakedFor(address staker, uint8 seatIndex)
        public
        view
        returns (uint256)
    {
        StakeCheckpoint[] storage checkpoints = stakesPerSeat[seatIndex][staker];
        if (checkpoints.length == 0) {
            return 0;
        }
        return checkpoints[checkpoints.length - 1].amount;
    }

    /**
     * @dev Get total stake on a specific seat
     * @param seatIndex Seat index
     * @return Total stake amount
     */
    function totalStakedForSeat(uint8 seatIndex)
        public
        view
        returns (uint256)
    {
        return totalStakedForSeatAt(seatIndex, block.number);
    }

    /**
     * @dev Get total stake for a seat at a specific block number
     * @param seatIndex Seat index
     * @param blockNumber Block number for snapshot
     * @return Stake amount at that block
     */
    function totalStakedForSeatAt(uint8 seatIndex, uint256 blockNumber)
        public
        view
        returns (uint256)
    {
        return stakedAt(totalStakePerSeat[seatIndex], blockNumber);
    }

    /**
     * @dev Get stake for an address on a seat at a specific block
     * @param staker Address of the staker
     * @param seatIndex Seat index
     * @param blockNumber Block number
     * @return Stake amount at that block
     */
    function totalStakedForAt(address staker, uint8 seatIndex, uint256 blockNumber)
        public
        view
        returns (uint256)
    {
        return stakedAt(stakesPerSeat[seatIndex][staker], blockNumber);
    }

    /**
     * @dev Get total global stake across all seats
     * @return Total stake amount
     */
    function totalStaked()
        public
        view
        returns (uint256)
    {
        if (globalStakeHistory.length == 0) {
            return 0;
        }
        return globalStakeHistory[globalStakeHistory.length - 1].amount;
    }

    /**
     * @dev Get council weights at current block
     * @return Arrays of weights (basis points, 10000 = 100%)
     */
    function getWeightsAt(uint256 blockNumber)
        external
        view
        returns (uint256[SEAT_COUNT] weights)
    {
        uint256 totalStake = stakedAt(globalStakeHistory, blockNumber);

        if (totalStake == 0) {
            // Equal weights if no stake
            for (uint8 i = 0; i < SEAT_COUNT; i++) {
                weights[i] = 10000 / SEAT_COUNT;  // ~1111 basis points each
            }
        } else {
            for (uint8 j = 0; j < SEAT_COUNT; j++) {
                uint256 seatStake = totalStakedForSeatAt(j, blockNumber);
                weights[j] = seatStake.mul(10000).div(totalStake);
            }
        }
    }

    /**
     * @dev Update checkpoint in history array
     * @param history Checkpoint array to update
     * @param amount Amount to add/subtract
     * @param isSubtraction True if unstaking, false if staking
     */
    function updateCheckpoint(
        StakeCheckpoint[] storage history,
        uint256 amount,
        bool isSubtraction
    )
        private
    {
        uint256 length = history.length;

        if (length == 0) {
            // First checkpoint
            history.push(StakeCheckpoint({
                blockNumber: block.number,
                amount: isSubtraction ? 0 : amount  // Can't subtract from 0
            }));
            return;
        }

        StakeCheckpoint storage checkpoint;

        if (history[length - 1].blockNumber < block.number) {
            // New block, create new checkpoint
            uint256 newAmount = history[length - 1].amount;
            if (isSubtraction) {
                newAmount = newAmount.sub(amount);
            } else {
                newAmount = newAmount.add(amount);
            }

            history.push(StakeCheckpoint({
                blockNumber: block.number,
                amount: newAmount
            }));
        } else {
            // Same block, update existing checkpoint
            checkpoint = history[length - 1];
            if (isSubtraction) {
                checkpoint.amount = checkpoint.amount.sub(amount);
            } else {
                checkpoint.amount = checkpoint.amount.add(amount);
            }
        }
    }

    /**
     * @dev Binary search to find stake amount at a specific block
     * @param history Checkpoint array
     * @param blockNumber Block number
     * @return Stake amount
     */
    function stakedAt(StakeCheckpoint[] storage history, uint256 blockNumber)
        private
        view
        returns (uint256)
    {
        uint256 length = history.length;

        if (length == 0 || blockNumber < history[0].blockNumber) {
            return 0;
        }

        if (blockNumber >= history[length - 1].blockNumber) {
            return history[length - 1].amount;
        }

        // Binary search
        uint256 min = 0;
        uint256 max = length - 1;

        while (max > min) {
            uint256 mid = (max + min + 1) / 2;
            if (history[mid].blockNumber <= blockNumber) {
                min = mid;
            } else {
                max = mid - 1;
            }
        }

        return history[min].amount;
    }
}

// Interface for Council contract
contract Council {
    function updateStakeCache(uint8 seatIndex, uint256 totalStaked) external;
}
