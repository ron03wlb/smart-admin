package net.lab1024.sa.igaming.risk.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * Risk proposal view object.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class RiskProposalVO {

  @Schema(description = "Proposal ID")
  private Long proposalId;

  @Schema(description = "Player ID")
  private Long playerId;

  @Schema(description = "Assessment ID")
  private Long assessmentId;

  @Schema(description = "Status")
  private Integer status;

  @Schema(description = "Priority")
  private Integer priority;

  @Schema(description = "Assignee")
  private String assignee;

  @Schema(description = "Review comment")
  private String reviewComment;

  @Schema(description = "SLA deadline")
  private OffsetDateTime slaDeadline;

  @Schema(description = "Resolved at")
  private OffsetDateTime resolvedAt;

  @Schema(description = "Create time")
  private OffsetDateTime createTime;
}
