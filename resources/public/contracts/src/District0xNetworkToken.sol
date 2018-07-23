pragma solidity ^0.4.18;

import "./token/minime/MiniMeToken.sol";
import "./token/erc20/StandardToken.sol";

/**
 * @title Token used for curation of District Registry TCR
 *
 * @dev Standard MiniMe Token with pre-minted supply and with dead controller.
 */

contract District0xNetworkToken is StandardToken, MiniMeToken {
  function District0xNetworkToken(address _tokenFactory, uint _mintedAmount)
  {
    super.constructMiniMeToken(
      _tokenFactory,
      0x0,
      0,
      "district0x Network Token",
      18,
      "DNT",
      true
    );
    generateTokens(msg.sender, _mintedAmount);
    changeController(0x0);
  }
}