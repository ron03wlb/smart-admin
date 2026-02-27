package net.lab1024.sa.igaming.agent.affiliate.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * Commission record view object for API responses.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Data
public class CommissionRecordVO {

  @Schema(description = "Record ID")
  private Long recordId;

  @Schema(description = "Agent ID")
  private Long agentId;

  @Schema(description = "Plan ID")
  private Long planId;

  @Schema(description = "Settlement date")
  private LocalDate settlementDate;

  @Schema(description = "Gross commission amount")
  private BigDecimal grossAmount;

  @Schema(description = "Adjustment amount")
  private BigDecimal adjustmentAmount;

  @Schema(description = "Carryover amount")
  private BigDecimal carryoverAmount;

  @Schema(description = "Net commission amount")
  private BigDecimal netAmount;

  @Schema(description = "Status: 1=Pending, 2=Approved, 3=Rejected")
  private Integer status;

  @Schema(description = "Approved by")
  private String approvedBy;

  @Schema(description = "Approved timestamp")
  private OffsetDateTime approvedAt;

  @Schema(description = "Created time")
  private OffsetDateTime createTime;
}
