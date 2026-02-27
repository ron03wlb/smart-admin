package net.lab1024.sa.igaming.risk.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * Risk assessment view object.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class RiskAssessmentVO {

  @Schema(description = "Assessment ID")
  private Long assessmentId;

  @Schema(description = "Player ID")
  private Long playerId;

  @Schema(description = "Event type")
  private String eventType;

  @Schema(description = "Event ID")
  private String eventId;

  @Schema(description = "Risk score")
  private BigDecimal riskScore;

  @Schema(description = "Risk level")
  private Integer riskLevel;

  @Schema(description = "Decision")
  private Integer decision;

  @Schema(description = "Processing time (ms)")
  private Integer processingTimeMs;

  @Schema(description = "Create time")
  private OffsetDateTime createTime;
}
