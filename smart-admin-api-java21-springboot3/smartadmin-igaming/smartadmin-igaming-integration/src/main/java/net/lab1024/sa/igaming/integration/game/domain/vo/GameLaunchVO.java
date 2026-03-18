package net.lab1024.sa.igaming.integration.game.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * VO for game launch result.
 *
 * <p>Contains the game session token and launch URL for the player to start playing. The token is a
 * JWT signed by the platform and will be verified by the game provider during callbacks.
 *
 * @author iGaming Team
 * @since 2026-03-18
 */
@Data
public class GameLaunchVO {

  @Schema(description = "Game session token (JWT, valid for 24 hours)")
  private String sessionToken;

  @Schema(description = "Game launch URL (player navigates to this URL to start playing)")
  private String launchUrl;

  @Schema(description = "Game code (echo from request)")
  private String gameCode;

  @Schema(description = "Game provider code (echo from request)")
  private String providerCode;

  @Schema(description = "Session ID (for tracking and debugging)")
  private String sessionId;

  @Schema(description = "Token expiry timestamp (epoch seconds)")
  private Long tokenExpiresAt;
}
