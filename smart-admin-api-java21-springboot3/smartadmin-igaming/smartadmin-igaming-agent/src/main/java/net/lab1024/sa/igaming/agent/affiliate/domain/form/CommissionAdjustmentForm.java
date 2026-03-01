package net.lab1024.sa.igaming.agent.affiliate.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

/**
 * Form for creating a commission adjustment.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@Data
public class CommissionAdjustmentForm {

  @Schema(description = "Agent ID")
  @NotNull
  private Long agentId;

  @Schema(description = "Adjustment type: 1=Late Arrival, 2=Rollback, 3=Manual")
  @NotNull
  private Integer adjustmentType;

  @Schema(description = "Adjustment amount")
  @NotNull
  private BigDecimal amount;

  @Schema(description = "Reason for adjustment")
  @NotBlank
  @Size(max = 256)
  private String reason;
}
