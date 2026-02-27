package net.lab1024.sa.igaming.activity.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * Form for adding a promotion rule.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class PromotionRuleAddForm {

  @Schema(description = "Promotion code (tenant-unique)")
  @NotBlank
  @Size(max = 64)
  private String promotionCode;

  @Schema(description = "Promotion name")
  @NotBlank
  @Size(max = 128)
  private String promotionName;

  @Schema(description = "Promotion type (1-5)")
  @NotNull
  private Integer promotionType;

  @Schema(description = "Start time")
  @NotNull
  private OffsetDateTime startTime;

  @Schema(description = "End time")
  @NotNull
  private OffsetDateTime endTime;

  @Schema(description = "Minimum deposit amount")
  private BigDecimal minDeposit;

  @Schema(description = "Bonus rate (1.0000 = 100%)")
  private BigDecimal bonusRate;

  @Schema(description = "Maximum bonus amount")
  @DecimalMin(value = "0.01")
  private BigDecimal maxBonus;

  @Schema(description = "Wagering multiplier (e.g. 20.00 = 20x)")
  @DecimalMin(value = "0.01")
  private BigDecimal wageringMultiplier;

  @Schema(description = "Max claims per player")
  @Positive
  private Integer maxClaimsPerPlayer;

  @Schema(description = "Bonus expiry days")
  @Positive
  private Integer bonusExpiryDays;

  @Schema(description = "Game restriction (JSON)")
  private String gameRestriction;
}
