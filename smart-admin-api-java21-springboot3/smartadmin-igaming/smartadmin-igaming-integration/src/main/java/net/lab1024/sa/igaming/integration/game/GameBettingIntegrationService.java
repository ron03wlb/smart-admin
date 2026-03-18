package net.lab1024.sa.igaming.integration.game;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.igaming.common.code.GameErrorCode;
import net.lab1024.sa.igaming.common.code.PlayerErrorCode;
import net.lab1024.sa.igaming.common.constant.PlayerStatusEnum;
import net.lab1024.sa.igaming.game.adapter.GPAdapterFactory;
import net.lab1024.sa.igaming.game.adapter.GameProviderAdapter;
import net.lab1024.sa.igaming.game.dao.GameDao;
import net.lab1024.sa.igaming.game.domain.entity.GameEntity;
import net.lab1024.sa.igaming.game.domain.form.CallbackBalanceForm;
import net.lab1024.sa.igaming.game.domain.vo.CallbackResponseVO;
import net.lab1024.sa.igaming.game.service.GameCallbackService;
import net.lab1024.sa.igaming.integration.game.domain.form.GameLaunchForm;
import net.lab1024.sa.igaming.integration.game.domain.vo.GameLaunchVO;
import net.lab1024.sa.igaming.player.dao.PlayerDao;
import net.lab1024.sa.igaming.player.domain.entity.PlayerEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Game betting integration service — orchestrates game launch flow.
 *
 * <p>This service coordinates Player, Wallet, and Game modules to provide a seamless game launch
 * experience. It handles player verification, balance checking, session token generation, and game
 * provider integration.
 *
 * <p><b>Integration Flow:</b>
 *
 * <ol>
 *   <li>Verify player status (PlayerDao)
 *   <li>Check playable balance (GameCallbackService.processBalance)
 *   <li>Verify game exists and is enabled (GameDao)
 *   <li>Generate JWT session token (Auth0)
 *   <li>Call game provider authenticate API (GameProviderAdapter)
 *   <li>Return launch URL with session token
 * </ol>
 *
 * <p><b>Security:</b>
 *
 * <ul>
 *   <li>JWT token signed with HS256 algorithm
 *   <li>Token valid for 24 hours (86400 seconds)
 *   <li>Token contains playerId, tenantId, gameCode, providerCode
 *   <li>Sa-Token authentication enforced at controller layer
 * </ul>
 *
 * @author iGaming Team
 * @since 2026-03-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameBettingIntegrationService {

  private static final long TOKEN_VALIDITY_SECONDS = 86400L; // 24 hours
  private static final String TOKEN_ISSUER = "SmartAdmin-iGaming";

  private final PlayerDao playerDao;
  private final GameDao gameDao;
  private final GameCallbackService gameCallbackService;
  private final GPAdapterFactory gpAdapterFactory;

  @Value("${igaming.jwt.secret:change-me-in-production-environment-with-256-bit-key}")
  private String jwtSecret;

  @Value("${igaming.game.launch-url-template:https://game.example.com/launch?token={token}}")
  private String launchUrlTemplate;

  /**
   * Launch a game for the player.
   *
   * <p>This method orchestrates the complete game launch flow, including player verification,
   * balance checking, session token generation, and game provider integration.
   *
   * @param form game launch request form
   * @return ResponseDTO with GameLaunchVO containing session token and launch URL
   */
  public ResponseDTO<GameLaunchVO> launchGame(GameLaunchForm form) {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }

    // Step 1: Verify player status
    PlayerEntity player = playerDao.selectById(form.getPlayerId());
    if (player == null || player.getDeleted()) {
      return ResponseDTO.userErrorParam(PlayerErrorCode.PLAYER_NOT_FOUND.getMsg());
    }

    if (!player.getStatus().equals(PlayerStatusEnum.ACTIVE.getValue())) {
      // Player is not active - could be LOCKED, SUSPENDED, CLOSED, etc.
      if (player.getStatus().equals(PlayerStatusEnum.LOCKED.getValue())) {
        return ResponseDTO.userErrorParam(PlayerErrorCode.PLAYER_LOCKED.getMsg());
      } else if (player.getStatus().equals(PlayerStatusEnum.SUSPENDED.getValue())) {
        return ResponseDTO.userErrorParam(PlayerErrorCode.PLAYER_SUSPENDED.getMsg());
      } else if (player.getStatus().equals(PlayerStatusEnum.CLOSED.getValue())) {
        return ResponseDTO.userErrorParam(PlayerErrorCode.PLAYER_CLOSED.getMsg());
      } else {
        return ResponseDTO.userErrorParam(
            "Player account is not active (status: " + player.getStatus() + ")");
      }
    }

    // Step 2: Check playable balance (CASH available + eligible BONUS)
    CallbackBalanceForm balanceForm = new CallbackBalanceForm();
    balanceForm.setProviderCode(form.getProviderCode());
    balanceForm.setPlayerId(form.getPlayerId());
    balanceForm.setGameCode(form.getGameCode());
    balanceForm.setSignature("internal_call"); // Skip signature verification for internal call
    balanceForm.setTimestamp(System.currentTimeMillis());

    ResponseDTO<CallbackResponseVO> balanceResult = gameCallbackService.processBalance(balanceForm);
    if (!balanceResult.getOk()) {
      return ResponseDTO.userErrorParam(
          "Failed to query player balance: " + balanceResult.getMsg());
    }

    BigDecimal playableBalance = balanceResult.getData().getBalance();
    if (playableBalance.compareTo(BigDecimal.ZERO) <= 0) {
      return ResponseDTO.userErrorParam(GameErrorCode.INSUFFICIENT_BALANCE.getMsg());
    }

    // Step 3: Verify game exists and is enabled
    GameEntity game =
        gameDao.selectOne(
            Wrappers.<GameEntity>lambdaQuery()
                .eq(GameEntity::getGameCode, form.getGameCode())
                .eq(GameEntity::getDeleted, false));
    if (game == null) {
      return ResponseDTO.userErrorParam(GameErrorCode.GAME_NOT_FOUND.getMsg());
    }
    if (!game.getEnabled()) {
      return ResponseDTO.userErrorParam(
          GameErrorCode.GAME_DISABLED.getMsg() + " (" + game.getGameName() + ")");
    }

    // Step 4: Generate JWT session token
    String sessionId = UUID.randomUUID().toString();
    Instant now = Instant.now();
    Instant expiry = now.plusSeconds(TOKEN_VALIDITY_SECONDS);

    String sessionToken =
        JWT.create()
            .withIssuer(TOKEN_ISSUER)
            .withSubject(String.valueOf(form.getPlayerId()))
            .withClaim("playerId", form.getPlayerId())
            .withClaim("tenantId", tenantId)
            .withClaim("gameCode", form.getGameCode())
            .withClaim("providerCode", form.getProviderCode())
            .withClaim("sessionId", sessionId)
            .withClaim("username", player.getUsername())
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(expiry))
            .sign(Algorithm.HMAC256(jwtSecret));

    log.info(
        "Generated game session token: playerId={}, gameCode={}, sessionId={}, expiresAt={}",
        form.getPlayerId(),
        form.getGameCode(),
        sessionId,
        expiry);

    // Step 5: Call game provider authenticate API (optional, depends on GP requirements)
    Option<GameProviderAdapter> adapterOpt = gpAdapterFactory.getAdapter(form.getProviderCode());
    if (adapterOpt.isEmpty()) {
      return ResponseDTO.userErrorParam(
          GameErrorCode.PROVIDER_NOT_FOUND.getMsg() + " (" + form.getProviderCode() + ")");
    }

    GameProviderAdapter adapter = adapterOpt.get();
    Try<String> authResult = adapter.authenticate(form.getPlayerId(), tenantId);

    if (authResult.isFailure()) {
      log.error(
          "Game provider authentication failed: playerId={}, provider={}, error={}",
          form.getPlayerId(),
          form.getProviderCode(),
          authResult.getCause().getMessage());
      return ResponseDTO.userErrorParam(
          "Game provider authentication failed: " + authResult.getCause().getMessage());
    }

    String gpToken = authResult.get();
    log.info(
        "Game provider authentication succeeded: playerId={}, provider={}, gpToken={}",
        form.getPlayerId(),
        form.getProviderCode(),
        gpToken);

    // Step 6: Build launch URL
    String launchUrl =
        launchUrlTemplate
            .replace("{token}", sessionToken)
            .replace("{gameCode}", form.getGameCode())
            .replace("{providerCode}", form.getProviderCode())
            .replace("{playerId}", String.valueOf(form.getPlayerId()))
            .replace("{gpToken}", gpToken)
            .replace(
                "{returnUrl}",
                form.getReturnUrl() != null ? form.getReturnUrl() : "https://lobby.example.com")
            .replace("{language}", form.getLanguage() != null ? form.getLanguage() : "en")
            .replace("{currency}", form.getCurrencyCode() != null ? form.getCurrencyCode() : "USD");

    // Step 7: Build response VO
    GameLaunchVO vo = new GameLaunchVO();
    vo.setSessionToken(sessionToken);
    vo.setLaunchUrl(launchUrl);
    vo.setGameCode(form.getGameCode());
    vo.setProviderCode(form.getProviderCode());
    vo.setSessionId(sessionId);
    vo.setTokenExpiresAt(expiry.getEpochSecond());

    log.info(
        "Game launch successful: playerId={}, gameCode={}, sessionId={}, balance={}",
        form.getPlayerId(),
        form.getGameCode(),
        sessionId,
        playableBalance);

    return ResponseDTO.ok(vo);
  }

  /**
   * Verify JWT session token (for GP callback authentication).
   *
   * <p>This method is called by game providers to verify the session token before processing
   * debit/credit callbacks.
   *
   * @param token JWT session token
   * @return Option.some(playerId) if valid, Option.none() if invalid or expired
   */
  public Option<Long> verifySessionToken(String token) {
    try {
      var jwt =
          JWT.require(Algorithm.HMAC256(jwtSecret)).withIssuer(TOKEN_ISSUER).build().verify(token);

      Long playerId = jwt.getClaim("playerId").asLong();
      String gameCode = jwt.getClaim("gameCode").asString();
      String sessionId = jwt.getClaim("sessionId").asString();

      log.debug(
          "Session token verified: playerId={}, gameCode={}, sessionId={}",
          playerId,
          gameCode,
          sessionId);

      return Option.some(playerId);
    } catch (Exception e) {
      log.warn("Session token verification failed: {}", e.getMessage());
      return Option.none();
    }
  }
}
