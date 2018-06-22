pragma solidity ^0.4.18;

import "./StakeBank.sol";

contract DelayedStakeBank is StakeBank {

    uint256 unstakeDelay;
    mapping (address => uint256) lastStaked;

    /// @param _token Token that can be staked.
    /// @param _unstakeDelay Earliest time (s) after last stake that stake can be withdrawn
    function DelayedStakeBank(ERC20 _token, uint256 _unstakeDelay) StakeBank(_token) public {
        unstakeDelay = _unstakeDelay;
    }

    /// @notice Stakes a certain amount of tokens.
    /// @param amount Amount of tokens to stake.
    /// @param data Data field used for signalling in more complex staking applications.
    function stake(uint256 amount, bytes data) public {
        stakeFor(msg.sender, amount, data);
    }

    /// @notice Overrides StakeBank.stakeFor, to prevent denial of service
    /// @param user Address of the user to stake for.
    /// @param amount Amount of tokens to stake.
    /// @param data Data field used for signalling in more complex staking applications.
    function stakeFor(address user, uint256 amount, bytes data) public {
        require(user == msg.sender);
        lastStaked[msg.sender] = block.number;
        StakeBank.stakeFor(user, amount, data);
    }

    /// @notice Unstakes a certain amount of tokens, if delay has passed.
    /// @dev Overrides StakeBank.unstake
    /// @param amount Amount of tokens to unstake.
    /// @param data Data field used for signalling in more complex staking applications.
    function unstake(uint256 amount, bytes data) public {
        require(block.number >= lastStaked[msg.sender].add(unstakeDelay));
        StakeBank.unstake(amount, data);
    }
}
