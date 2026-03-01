package net.lab1024.sa.igaming.risk.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Form for assigning a risk proposal to a reviewer.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@Data
public class RiskProposalAssignForm {

  @Schema(description = "Proposal ID to assign")
  @NotNull(message = "proposalId cannot be null")
  private Long proposalId;

  @Schema(description = "Assignee username")
  @NotBlank(message = "assignee cannot be blank")
  @Size(max = 64)
  private String assignee;
}
