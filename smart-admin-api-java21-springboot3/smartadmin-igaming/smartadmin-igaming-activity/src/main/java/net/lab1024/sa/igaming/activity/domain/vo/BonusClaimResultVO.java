package net.lab1024.sa.igaming.activity.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

/**
 * Bonus claim result view object.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class BonusClaimResultVO {

  @Schema(description = "Bonus record ID")
  private Long recordId;

  @Schema(description = "Bonus amount awarded")
  private BigDecimal bonusAmount;

  @Schema(description = "Wagering requirement")
  private BigDecimal wageringRequired;

  @Schema(description = "Wallet balance after claim")
  private BigDecimal walletBalance;

  @Schema(description = "Bonus record status")
  private Integer status;
}
