package net.lab1024.sa.igaming.risk.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Risk proposal review form.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class RiskProposalReviewForm {

  @Schema(description = "Proposal ID")
  @NotNull(message = "Proposal ID is required")
  private Long proposalId;

  @Schema(description = "Review comment")
  private String reviewComment;
}
