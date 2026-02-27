package net.lab1024.sa.igaming.agent.affiliate.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Form for approving or rejecting a commission record.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Data
public class CommissionApprovalForm {

  @Schema(description = "Commission record ID")
  @NotNull(message = "recordId cannot be null")
  private Long recordId;

  @Schema(description = "Approver name")
  private String approvedBy;
}
