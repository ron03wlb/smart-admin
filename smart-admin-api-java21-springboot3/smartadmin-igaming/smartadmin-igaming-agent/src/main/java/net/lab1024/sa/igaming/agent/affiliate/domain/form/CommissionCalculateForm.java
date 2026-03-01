package net.lab1024.sa.igaming.agent.affiliate.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * Form for triggering commission calculation for an agent.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@Data
public class CommissionCalculateForm {

  @Schema(description = "Agent ID")
  @NotNull
  private Long agentId;

  @Schema(description = "Settlement date")
  @NotNull
  private LocalDate settlementDate;

  @Schema(description = "Gross revenue for commission calculation")
  @NotNull
  @DecimalMin("0")
  private BigDecimal grossRevenue;
}
