package net.lab1024.sa.igaming.agent.credit.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * Agent credit view object for API responses.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Data
public class AgentCreditVO {

  @Schema(description = "Agent credit record ID")
  private Long agentCreditId;

  @Schema(description = "Agent ID")
  private Long agentId;

  @Schema(description = "Parent agent ID")
  private Long parentId;

  @Schema(description = "Credit limit")
  private BigDecimal creditLimit;

  @Schema(description = "Used credit")
  private BigDecimal usedCredit;

  @Schema(description = "Allocated to children")
  private BigDecimal allocatedToChildren;

  @Schema(description = "Available credit (limit - used - allocated)")
  private BigDecimal availableCredit;

  @Schema(description = "Position percent")
  private BigDecimal positionPercent;

  @Schema(description = "Max position cap")
  private BigDecimal maxPosition;

  @Schema(description = "Status: 1=Active, 2=Frozen, 3=Suspended")
  private Integer status;

  @Schema(description = "Frozen timestamp")
  private OffsetDateTime frozenAt;

  @Schema(description = "Last settlement timestamp")
  private OffsetDateTime lastSettlementTime;

  @Schema(description = "Created time")
  private OffsetDateTime createTime;
}
