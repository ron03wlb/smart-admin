package net.lab1024.sa.igaming.agent.credit.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * Credit allocation audit view object for API responses.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Data
public class CreditAllocationAuditVO {

  @Schema(description = "Audit record ID")
  private Long auditId;

  @Schema(description = "Parent (allocator) agent ID")
  private Long parentId;

  @Schema(description = "Child (receiver) agent ID")
  private Long childId;

  @Schema(description = "Old credit limit")
  private BigDecimal oldLimit;

  @Schema(description = "New credit limit")
  private BigDecimal newLimit;

  @Schema(description = "Change delta")
  private BigDecimal delta;

  @Schema(description = "Old position percent")
  private BigDecimal oldPosition;

  @Schema(description = "New position percent")
  private BigDecimal newPosition;

  @Schema(description = "Reason")
  private String reason;

  @Schema(description = "Operator")
  private String operator;

  @Schema(description = "Created time")
  private OffsetDateTime createTime;
}
