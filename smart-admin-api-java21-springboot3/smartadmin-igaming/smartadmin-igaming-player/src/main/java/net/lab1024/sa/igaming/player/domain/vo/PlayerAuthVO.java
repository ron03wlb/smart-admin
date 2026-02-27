package net.lab1024.sa.igaming.player.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Player authentication response VO — returned on register/login.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class PlayerAuthVO {

  @Schema(description = "Player ID")
  private Long playerId;

  @Schema(description = "Username")
  private String username;

  @Schema(description = "Sa-Token value")
  private String tokenValue;

  @Schema(description = "VIP level")
  private Integer vipLevel;
}
