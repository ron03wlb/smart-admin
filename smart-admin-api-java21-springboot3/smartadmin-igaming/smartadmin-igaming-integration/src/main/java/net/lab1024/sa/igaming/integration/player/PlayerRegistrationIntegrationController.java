package net.lab1024.sa.igaming.integration.player;

import cn.dev33.satoken.annotation.SaIgnore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.integration.player.domain.form.PlayerRegistrationIntegrationForm;
import net.lab1024.sa.igaming.integration.player.domain.vo.PlayerRegistrationResultVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Player registration integration controller - public API for player onboarding.
 *
 * <p>This controller provides a single endpoint for complete player registration with automatic
 * wallet creation. It is designed for external use (player-facing web/mobile app) and does not
 * require authentication.
 *
 * <p><b>Security Notes:</b>
 *
 * <ul>
 *   <li>{@code @SaIgnore} - No authentication required (public registration endpoint)
 *   <li>Rate limiting should be applied at API gateway level
 *   <li>CAPTCHA verification recommended for production
 * </ul>
 *
 * @author iGaming Team
 * @since 2026-03-18
 * @see PlayerRegistrationIntegrationService
 */
@Tag(name = "iGaming - Player Registration Integration")
@RestController
@RequestMapping("/igaming/integration/player")
@RequiredArgsConstructor
public class PlayerRegistrationIntegrationController {

  private final PlayerRegistrationIntegrationService playerRegistrationIntegrationService;

  /**
   * Register a new player with automatic wallet creation.
   *
   * <p>This endpoint orchestrates the complete player onboarding process:
   *
   * <ol>
   *   <li>Player account creation (Argon2id password hashing)
   *   <li>CASH wallet creation
   *   <li>BONUS wallet creation
   *   <li>Kafka event publishing (PLAYER_REGISTERED)
   *   <li>First deposit bonus eligibility check
   * </ol>
   *
   * <p><b>Success Response Example:</b>
   *
   * <pre>
   * {
   *   "ok": true,
   *   "code": 1,
   *   "data": {
   *     "player": {
   *       "playerId": 1001,
   *       "username": "testuser",
   *       "tokenValue": "eyJ...",
   *       "vipLevel": 0
   *     },
   *     "wallets": [
   *       {"walletId": 2001, "walletType": 1, "balance": "0.0000", ...},
   *       {"walletId": 2002, "walletType": 2, "balance": "0.0000", ...}
   *     ],
   *     "firstDepositBonusEligible": true,
   *     "referralCode": "ABC123"
   *   }
   * }
   * </pre>
   *
   * <p><b>Error Response Example:</b>
   *
   * <pre>
   * {
   *   "ok": false,
   *   "code": 30001,
   *   "msg": "Username already exists"
   * }
   * </pre>
   *
   * @param form player registration form (username, password, email, phone, etc.)
   * @return player registration result with auth token, wallets, and bonus eligibility
   */
  @Operation(summary = "Register player with wallets")
  @PostMapping("/register")
  @SaIgnore
  public ResponseDTO<PlayerRegistrationResultVO> registerPlayerWithWallet(
      @Valid @RequestBody PlayerRegistrationIntegrationForm form) {
    return playerRegistrationIntegrationService.registerPlayerWithWallet(form);
  }
}
