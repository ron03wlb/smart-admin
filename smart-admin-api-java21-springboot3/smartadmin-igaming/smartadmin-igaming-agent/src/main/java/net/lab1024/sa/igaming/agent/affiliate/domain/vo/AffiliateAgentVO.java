package net.lab1024.sa.igaming.agent.affiliate.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * Affiliate agent view object for API responses.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Data
public class AffiliateAgentVO {

  @Schema(description = "Agent ID")
  private Long agentId;

  @Schema(description = "Username")
  private String username;

  @Schema(description = "Parent agent ID")
  private Long parentAgentId;

  @Schema(description = "Hierarchy path")
  private String hierarchyPath;

  @Schema(description = "Agent level")
  private Integer agentLevel;

  @Schema(description = "Commission plan ID")
  private Long commissionPlanId;

  @Schema(description = "Total players")
  private Integer totalPlayers;

  @Schema(description = "Active players")
  private Integer activePlayers;

  @Schema(description = "Total commission earned")
  private BigDecimal totalCommission;

  @Schema(description = "Status: 1=Active, 2=Frozen, 3=Suspended")
  private Integer status;

  @Schema(description = "Referral code")
  private String referralCode;

  @Schema(description = "Created time")
  private OffsetDateTime createTime;
}
