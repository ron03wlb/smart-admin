package net.lab1024.sa.igaming.activity.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * Player bonus record view object.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class PlayerBonusRecordVO {

  @Schema(description = "Record ID")
  private Long recordId;

  @Schema(description = "Player ID")
  private Long playerId;

  @Schema(description = "Promotion code")
  private String promotionCode;

  @Schema(description = "Promotion name")
  private String promotionName;

  @Schema(description = "Bonus amount")
  private BigDecimal bonusAmount;

  @Schema(description = "Wagering required")
  private BigDecimal wageringRequired;

  @Schema(description = "Wagering completed")
  private BigDecimal wageringCompleted;

  @Schema(description = "Status (1-5)")
  private Integer status;

  @Schema(description = "Claimed at")
  private OffsetDateTime claimedAt;

  @Schema(description = "Completed at")
  private OffsetDateTime completedAt;

  @Schema(description = "Expired at")
  private OffsetDateTime expiredAt;
}
