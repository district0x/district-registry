pragma solidity ^0.4.18;

import "./StakeBank.sol";

contract TokenReturningStakeBank is StakeBank {

    ERC20 public returnToken;

    uint256 public rate;

    /// @param _token Token that can be staked.
    /// @param _returnToken Token that is given to user once he stakes.
    /// @param _rate Rate of return tokens per token.
    function constructStakeBank(ERC20 _token, ERC20 _returnToken, uint256 _rate) internal {
        super.constructStakeBank(_token);
        require(address(_returnToken) != 0x0);
        require(_token != _returnToken);
        require(_rate > 0);

        returnToken = _returnToken;
        rate = _rate;
    }

    /// @notice Stakes a certain amount of tokens for another user.
    /// @param user Address of the user to stake for.
    /// @param amount Amount of tokens to stake.
    /// @param data Data field used for signalling in more complex staking applications.
    function stakeFor(address user, uint256 amount, bytes data) public {
        super.stakeFor(user, amount, data);
        require(returnToken.transfer(user, amount.mul(getRate())));
    }

    /// @notice Unstakes a certain amount of tokens.
    /// @param amount Amount of tokens to unstake.
    /// @param data Data field used for signalling in more complex staking applications.
    function unstake(uint256 amount, bytes data) public {
        super.unstake(amount, data);

        uint256 returnAmount = amount.div(getRate());
        require(returnAmount.mul(getRate()) == amount);

        require(returnToken.transferFrom(msg.sender, address(this), returnAmount));
    }

    /// @notice Returns conversion rate from token to returnToken. In function so it can be overridden.
    /// @return conversion rate.
    function getRate() public view returns (uint256) {
        return rate;
    }
}
