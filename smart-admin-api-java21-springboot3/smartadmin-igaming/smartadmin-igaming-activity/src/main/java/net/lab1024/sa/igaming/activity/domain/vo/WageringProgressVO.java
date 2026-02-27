package net.lab1024.sa.igaming.activity.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

/**
 * Wagering progress view object.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class WageringProgressVO {

  @Schema(description = "Bonus record ID")
  private Long recordId;

  @Schema(description = "Promotion name")
  private String promotionName;

  @Schema(description = "Wagering required")
  private BigDecimal wageringRequired;

  @Schema(description = "Wagering completed")
  private BigDecimal wageringCompleted;

  @Schema(description = "Progress percentage (0-100)")
  private BigDecimal progressPercent;

  @Schema(description = "Bonus record status")
  private Integer status;
}
