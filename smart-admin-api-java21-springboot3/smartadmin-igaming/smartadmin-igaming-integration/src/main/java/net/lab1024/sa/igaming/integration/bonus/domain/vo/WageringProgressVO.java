package net.lab1024.sa.igaming.integration.bonus.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * Wagering progress VO — displays player's bonus wagering progress.
 *
 * <p>This VO shows the current status of a player's active bonus, including wagering progress,
 * remaining requirements, and estimated completion.
 *
 * @author iGaming Team
 * @since 2026-03-18
 */
@Data
public class WageringProgressVO {

  @Schema(description = "Bonus record ID (t_player_bonus_record.record_id)")
  private Long recordId;

  @Schema(description = "Player ID")
  private Long playerId;

  @Schema(description = "Bonus amount (original bonus awarded)")
  private BigDecimal bonusAmount;

  @Schema(description = "Wagering required (total turnover needed to unlock bonus)")
  private BigDecimal wageringRequired;

  @Schema(description = "Wagering completed (cumulative valid turnover)")
  private BigDecimal wageringCompleted;

  @Schema(description = "Wagering remaining (wageringRequired - wageringCompleted)")
  private BigDecimal wageringRemaining;

  @Schema(description = "Progress percentage (0-100%)")
  private BigDecimal progressPercentage;

  @Schema(
      description =
          "Bonus status (1: ACTIVE, 2: COMPLETED, 3: EXPIRED, 4: CANCELLED, 5: FORFEITED)")
  private Integer status;

  @Schema(description = "Bonus expiry timestamp")
  private OffsetDateTime expiresAt;

  @Schema(description = "Promotion code (e.g., 'FIRST_DEPOSIT_BONUS')")
  private String promotionCode;

  @Schema(description = "Whether wagering requirement is met (ready for conversion)")
  private Boolean isReadyForConversion;
}
