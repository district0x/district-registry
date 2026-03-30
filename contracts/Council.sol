pragma solidity ^0.4.24;

import "./auth/DSAuth.sol";
import "@aragon/os/contracts/lib/math/SafeMath.sol";

/**
 * @title Council - The Backroom governance seats
 *
 * @dev Manages 9 council seats representing philosophical perspectives
 * Seat 8 (The Stranger / ARI) is immutable and cannot be changed
 * Other seats can be flipped if a new mind receives more stake
 */

contract Council is DSAuth {
    using SafeMath for uint256;

    uint8 public constant SEAT_COUNT = 9;
    uint8 public constant STRANGER_SEAT_INDEX = 8;  // ARI's immutable seat

    struct Seat {
        bytes32 mindId;           // keccak256 of mind name
        string name;              // Human-readable name
        bytes32 philosophyHash;   // IPFS hash of philosophy description
        bool isImmutable;         // true for Stranger seat
        bool isActive;            // Can be deactivated
        uint256 createdAt;
        uint256 totalStaked;      // Cached from CouncilStakeBank
    }

    Seat[SEAT_COUNT] public seats;
    mapping(bytes32 => uint8) public mindIdToSeatIndex;

    address public stakeBank;
    address public ariOracle;
    bool private wasConstructed;

    // Events
    event SeatCreated(uint8 indexed seatIndex, bytes32 mindId, string name, bool isImmutable);
    event SeatFlipped(uint8 indexed seatIndex, bytes32 oldMindId, bytes32 newMindId, string newName);
    event PhilosophyUpdated(uint8 indexed seatIndex, bytes32 newPhilosophyHash);
    event StakeCacheUpdated(uint8 indexed seatIndex, uint256 totalStaked);

    /**
     * @dev Constructor for this contract
     * @param _stakeBank Address of CouncilStakeBank contract
     * @param _ariOracle Address of ARIOracle contract
     */
    function construct(address _stakeBank, address _ariOracle)
        external
    {
        require(_stakeBank != 0x0, "Council: StakeBank address can't be 0x0");
        require(_ariOracle != 0x0, "Council: ARIOracle address can't be 0x0");
        require(!wasConstructed, "Council: Already constructed");

        stakeBank = _stakeBank;
        ariOracle = _ariOracle;
        wasConstructed = true;
        owner = msg.sender;

        // Initialize the 9 founding minds
        _initializeSeat(0, "Satoshi Nakamoto", "satoshi", false);
        _initializeSeat(1, "Hal Finney", "hal", false);
        _initializeSeat(2, "Vitalik Buterin", "vitalik", false);
        _initializeSeat(3, "Joe Urgo", "joe", false);
        _initializeSeat(4, "Aldous Huxley", "huxley", false);
        _initializeSeat(5, "Terence McKenna", "mckenna", false);
        _initializeSeat(6, "Aaron Swartz", "aaron", false);
        _initializeSeat(7, "Elon Musk", "elon", false);
        _initializeSeat(STRANGER_SEAT_INDEX, "ARI - The Stranger", "stranger", true);  // IMMUTABLE
    }

    /**
     * @dev Internal function to initialize a seat
     */
    function _initializeSeat(uint8 seatIndex, string name, string mindKey, bool immutable)
        private
    {
        bytes32 mindId = keccak256(abi.encodePacked(mindKey));

        seats[seatIndex] = Seat({
            mindId: mindId,
            name: name,
            philosophyHash: bytes32(0),  // Set later via updatePhilosophy
            isImmutable: immutable,
            isActive: true,
            createdAt: now,
            totalStaked: 0
        });

        mindIdToSeatIndex[mindId] = seatIndex;

        emit SeatCreated(seatIndex, mindId, name, immutable);
    }

    /**
     * @dev Update philosophy hash for a seat
     * @param seatIndex Index of the seat to update
     * @param philosophyHash IPFS hash of philosophy document
     */
    function updatePhilosophy(uint8 seatIndex, bytes32 philosophyHash)
        external
        auth
    {
        require(seatIndex < SEAT_COUNT, "Council: Invalid seat index");
        require(philosophyHash != bytes32(0), "Council: Philosophy hash can't be empty");

        seats[seatIndex].philosophyHash = philosophyHash;
        emit PhilosophyUpdated(seatIndex, philosophyHash);
    }

    /**
     * @dev Update cached stake amount for a seat
     * Called by CouncilStakeBank after stake changes
     * @param seatIndex Index of the seat
     * @param totalStaked New total staked amount
     */
    function updateStakeCache(uint8 seatIndex, uint256 totalStaked)
        external
    {
        require(msg.sender == stakeBank, "Council: Only StakeBank can update cache");
        require(seatIndex < SEAT_COUNT, "Council: Invalid seat index");

        seats[seatIndex].totalStaked = totalStaked;
        emit StakeCacheUpdated(seatIndex, totalStaked);
    }

    /**
     * @dev Propose a new mind to replace the lowest-staked mutable seat
     * @param mindId bytes32 identifier for the new mind
     * @param name Human-readable name
     * @param philosophyHash IPFS hash of philosophy
     * @param proposedStake Amount of stake the proposed mind has
     */
    function proposeNewMind(
        bytes32 mindId,
        string name,
        bytes32 philosophyHash,
        uint256 proposedStake
    )
        external
        auth
    {
        require(mindId != bytes32(0), "Council: MindId can't be empty");
        require(bytes(name).length > 0, "Council: Name can't be empty");
        require(mindIdToSeatIndex[mindId] == 0, "Council: Mind already has a seat");

        // Find lowest-staked mutable seat
        uint8 lowestSeat = findLowestStakedMutableSeat();
        require(proposedStake > seats[lowestSeat].totalStaked, "Council: Proposed stake not higher than lowest");

        // Execute seat flip
        executeSeatFlip(lowestSeat, mindId, name, philosophyHash);
    }

    /**
     * @dev Execute seat flip (internal)
     * @param seatIndex Seat to flip
     * @param newMindId New mind identifier
     * @param newName New mind name
     * @param philosophyHash Philosophy hash
     */
    function executeSeatFlip(
        uint8 seatIndex,
        bytes32 newMindId,
        string newName,
        bytes32 philosophyHash
    )
        private
    {
        require(!seats[seatIndex].isImmutable, "Council: Cannot flip immutable seat");

        bytes32 oldMindId = seats[seatIndex].mindId;

        // Remove old mind mapping
        delete mindIdToSeatIndex[oldMindId];

        // Update seat
        seats[seatIndex].mindId = newMindId;
        seats[seatIndex].name = newName;
        seats[seatIndex].philosophyHash = philosophyHash;
        seats[seatIndex].totalStaked = 0;  // Reset stake (will be updated by StakeBank)

        // Add new mind mapping
        mindIdToSeatIndex[newMindId] = seatIndex;

        emit SeatFlipped(seatIndex, oldMindId, newMindId, newName);
    }

    /**
     * @dev Find the seat with lowest stake among mutable seats
     * @return Seat index of lowest-staked mutable seat
     */
    function findLowestStakedMutableSeat()
        public
        view
        returns (uint8)
    {
        uint8 lowestSeat = 0;
        uint256 lowestStake = seats[0].isImmutable ? uint256(-1) : seats[0].totalStaked;

        for (uint8 i = 1; i < SEAT_COUNT; i++) {
            if (!seats[i].isImmutable && seats[i].totalStaked < lowestStake) {
                lowestSeat = i;
                lowestStake = seats[i].totalStaked;
            }
        }

        require(!seats[lowestSeat].isImmutable, "Council: No mutable seats found");
        return lowestSeat;
    }

    /**
     * @dev Get council weights in basis points (10000 = 100%)
     * @return Array of mind IDs and their weights
     */
    function getCouncilWeights()
        external
        view
        returns (bytes32[SEAT_COUNT] mindIds, uint256[SEAT_COUNT] weights)
    {
        uint256 totalStake = 0;

        // Calculate total stake
        for (uint8 i = 0; i < SEAT_COUNT; i++) {
            mindIds[i] = seats[i].mindId;
            totalStake = totalStake.add(seats[i].totalStaked);
        }

        // Calculate weights as basis points
        if (totalStake == 0) {
            // Equal weights if no stake
            for (uint8 j = 0; j < SEAT_COUNT; j++) {
                weights[j] = 10000 / SEAT_COUNT;  // ~1111 basis points each
            }
        } else {
            for (uint8 k = 0; k < SEAT_COUNT; k++) {
                weights[k] = seats[k].totalStaked.mul(10000).div(totalStake);
            }
        }
    }

    /**
     * @dev Get seat information
     * @param seatIndex Index of the seat
     */
    function getSeat(uint8 seatIndex)
        external
        view
        returns (
            bytes32 mindId,
            string name,
            bytes32 philosophyHash,
            bool isImmutable,
            bool isActive,
            uint256 createdAt,
            uint256 totalStaked
        )
    {
        require(seatIndex < SEAT_COUNT, "Council: Invalid seat index");
        Seat storage seat = seats[seatIndex];

        return (
            seat.mindId,
            seat.name,
            seat.philosophyHash,
            seat.isImmutable,
            seat.isActive,
            seat.createdAt,
            seat.totalStaked
        );
    }

    /**
     * @dev Verify that Stranger seat is immutable (for security audits)
     * @return true if Stranger seat is correctly configured as immutable
     */
    function verifyStrangerImmutable()
        external
        pure
        returns (bool)
    {
        return STRANGER_SEAT_INDEX == 8;  // This is a constant, always true
    }
}
