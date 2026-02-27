package net.lab1024.sa.igaming.agent.credit.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * Settlement record view object for API responses.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Data
public class SettlementRecordVO {

  @Schema(description = "Settlement record ID")
  private Long settlementRecordId;

  @Schema(description = "Agent ID")
  private Long agentId;

  @Schema(description = "Parent agent ID")
  private Long parentId;

  @Schema(description = "Settlement week (e.g. 2026-W09)")
  private String settlementWeek;

  @Schema(description = "Settlement phase: 1=Freeze, 2=Collection, 3=Verification, 4=Report")
  private Integer settlementPhase;

  @Schema(description = "Player loss")
  private BigDecimal playerLoss;

  @Schema(description = "Agent own share")
  private BigDecimal ownShare;

  @Schema(description = "Amount to parent")
  private BigDecimal toParent;

  @Schema(description = "Amount to platform")
  private BigDecimal toPlatform;

  @Schema(description = "Payment status: 1=Pending, 2=Verified, 3=Overdue")
  private Integer paymentStatus;

  @Schema(description = "Payment transaction ID")
  private String paymentTxnId;

  @Schema(description = "Verified timestamp")
  private OffsetDateTime verifiedAt;

  @Schema(description = "Created time")
  private OffsetDateTime createTime;
}
