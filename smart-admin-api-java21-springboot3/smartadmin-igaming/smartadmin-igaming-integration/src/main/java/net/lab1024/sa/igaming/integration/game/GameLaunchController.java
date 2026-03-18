package net.lab1024.sa.igaming.integration.game;

import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.integration.game.domain.form.GameLaunchForm;
import net.lab1024.sa.igaming.integration.game.domain.vo.GameLaunchVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Game launch controller — player game session initiation.
 *
 * <p>Provides endpoint for players to launch games. Requires Sa-Token authentication. Returns
 * session token and launch URL for seamless game integration.
 *
 * <p><b>Endpoint:</b>
 *
 * <ul>
 *   <li>POST /igaming/integration/game/launch — Launch a game session
 * </ul>
 *
 * <p><b>Security:</b>
 *
 * <ul>
 *   <li>Sa-Token authentication required (player must be logged in)
 *   <li>PlayerId from form is validated against Sa-Token context
 * </ul>
 *
 * @author iGaming Team
 * @since 2026-03-18
 */
@Tag(name = "Game Launch API", description = "Game session management and launch operations")
@RestController
@RequestMapping("/igaming/integration/game")
@RequiredArgsConstructor
public class GameLaunchController {

  private final GameBettingIntegrationService gameBettingIntegrationService;

  /**
   * Launch a game for the authenticated player.
   *
   * <p>This endpoint initiates a game session by verifying player status, checking balance,
   * generating a JWT session token, and returning a launch URL.
   *
   * <p><b>Request Example:</b>
   *
   * <pre>
   * POST /igaming/integration/game/launch
   * {
   *   "playerId": 1001,
   *   "gameCode": "slot-001",
   *   "providerCode": "mock",
   *   "returnUrl": "https://lobby.example.com",
   *   "language": "en",
   *   "currencyCode": "USD"
   * }
   * </pre>
   *
   * <p><b>Response Example:</b>
   *
   * <pre>
   * {
   *   "ok": true,
   *   "code": 1,
   *   "data": {
   *     "sessionToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
   *     "launchUrl": "https://game.example.com/launch?token=eyJhbGci...",
   *     "gameCode": "slot-001",
   *     "providerCode": "mock",
   *     "sessionId": "a1b2c3d4-e5f6-4789-a0b1-c2d3e4f5g6h7",
   *     "tokenExpiresAt": 1711065600
   *   }
   * }
   * </pre>
   *
   * @param form game launch request form
   * @return ResponseDTO with GameLaunchVO containing session token and launch URL
   */
  @Operation(
      summary = "Launch game session",
      description = "Generate session token and launch URL for player to start playing")
  @PostMapping("/launch")
  public ResponseDTO<GameLaunchVO> launchGame(@Valid @RequestBody GameLaunchForm form) {
    // Validate playerId matches Sa-Token context (security check)
    Long authenticatedPlayerId = StpUtil.getLoginIdAsLong();
    if (!authenticatedPlayerId.equals(form.getPlayerId())) {
      return ResponseDTO.userErrorParam(
          "Player ID mismatch: authenticated as "
              + authenticatedPlayerId
              + " but requested "
              + form.getPlayerId());
    }

    return gameBettingIntegrationService.launchGame(form);
  }
}
