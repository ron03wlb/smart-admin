package net.lab1024.sa.igaming.activity.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * Promotion rule view object.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class PromotionRuleVO {

  @Schema(description = "Rule ID")
  private Long ruleId;

  @Schema(description = "Promotion code")
  private String promotionCode;

  @Schema(description = "Promotion name")
  private String promotionName;

  @Schema(description = "Promotion type (1-5)")
  private Integer promotionType;

  @Schema(description = "Status (1-3)")
  private Integer status;

  @Schema(description = "Start time")
  private OffsetDateTime startTime;

  @Schema(description = "End time")
  private OffsetDateTime endTime;

  @Schema(description = "Minimum deposit amount")
  private BigDecimal minDeposit;

  @Schema(description = "Bonus rate (1.0000 = 100%)")
  private BigDecimal bonusRate;

  @Schema(description = "Maximum bonus amount")
  private BigDecimal maxBonus;

  @Schema(description = "Wagering multiplier")
  private BigDecimal wageringMultiplier;

  @Schema(description = "Max claims per player")
  private Integer maxClaimsPerPlayer;

  @Schema(description = "Bonus expiry days")
  private Integer bonusExpiryDays;

  @Schema(description = "Game restriction (JSON)")
  private String gameRestriction;

  @Schema(description = "Create time")
  private OffsetDateTime createTime;

  @Schema(description = "Update time")
  private OffsetDateTime updateTime;
}
