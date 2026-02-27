package net.lab1024.sa.igaming.agent.credit.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

/**
 * Form for recalling (reducing) credit from a child agent.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Data
public class CreditRecallForm {

  @Schema(description = "Parent agent ID")
  @NotNull(message = "parentId cannot be null")
  private Long parentId;

  @Schema(description = "Child agent ID")
  @NotNull(message = "childId cannot be null")
  private Long childId;

  @Schema(description = "New credit limit for the child")
  @NotNull(message = "newLimit cannot be null")
  @DecimalMin(value = "0", message = "newLimit must be non-negative")
  private BigDecimal newLimit;

  @Schema(description = "Reason for recall")
  private String reason;

  @Schema(description = "Tenant ID")
  @NotNull(message = "tenantId cannot be null")
  private Long tenantId;
}
