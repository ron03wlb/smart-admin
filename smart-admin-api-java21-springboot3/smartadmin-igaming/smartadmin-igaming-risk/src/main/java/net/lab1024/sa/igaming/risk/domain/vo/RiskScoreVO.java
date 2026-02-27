package net.lab1024.sa.igaming.risk.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * Risk score profile view object.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class RiskScoreVO {

  @Schema(description = "Risk score ID")
  private Long riskScoreId;

  @Schema(description = "Player ID")
  private Long playerId;

  @Schema(description = "Cumulative score")
  private BigDecimal cumulativeScore;

  @Schema(description = "Risk level")
  private Integer riskLevel;

  @Schema(description = "Total assessments")
  private Integer totalAssessments;

  @Schema(description = "Last assessment time")
  private OffsetDateTime lastAssessmentTime;

  @Schema(description = "Auto-locked")
  private Boolean autoLocked;

  @Schema(description = "Create time")
  private OffsetDateTime createTime;
}
