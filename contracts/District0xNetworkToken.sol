pragma solidity ^0.4.24;

import "@aragon/apps-shared-minime/contracts/MiniMeToken.sol";

/**
 * @title Token used for curation of DistrictRegistry TCR
 *
 * @dev Standard MiniMe Token with pre-minted supply and with dead controller.
 */

contract District0xNetworkToken is MiniMeToken {
  constructor(MiniMeTokenFactory _tokenFactory, uint _mintedAmount)
    public
    MiniMeToken(
      _tokenFactory,
      MiniMeToken(address(0)),
      0,
      "District0x Network Token",
      18,
      "DNT",
      true
    )
  {
    generateTokens(msg.sender, _mintedAmount);
    changeController(0x0);
  }
}
