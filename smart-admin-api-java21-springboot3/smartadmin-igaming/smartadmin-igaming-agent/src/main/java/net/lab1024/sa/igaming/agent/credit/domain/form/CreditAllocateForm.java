package net.lab1024.sa.igaming.agent.credit.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

/**
 * Form for allocating credit from parent to child agent.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Data
public class CreditAllocateForm {

  @Schema(description = "Parent agent ID")
  @NotNull(message = "parentId cannot be null")
  private Long parentId;

  @Schema(description = "Child agent ID")
  @NotNull(message = "childId cannot be null")
  private Long childId;

  @Schema(description = "Amount to allocate")
  @NotNull(message = "amount cannot be null")
  @DecimalMin(value = "0.0001", message = "amount must be positive")
  private BigDecimal amount;

  @Schema(description = "Position percent for the child")
  private BigDecimal positionPercent;

  @Schema(description = "Reason for allocation")
  private String reason;

  @Schema(description = "Tenant ID")
  @NotNull(message = "tenantId cannot be null")
  private Long tenantId;
}
