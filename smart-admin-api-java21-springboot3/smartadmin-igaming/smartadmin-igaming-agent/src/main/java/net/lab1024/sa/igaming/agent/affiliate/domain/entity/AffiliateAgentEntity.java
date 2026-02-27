package net.lab1024.sa.igaming.agent.affiliate.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * Affiliate agent master data entity.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_affiliate_agent")
public class AffiliateAgentEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long agentId;

  private String username;

  private Long parentAgentId;

  /** Hierarchy path (e.g. /1/5/12/). */
  private String hierarchyPath;

  private Integer agentLevel;

  private Long commissionPlanId;

  private Integer totalPlayers;

  private Integer activePlayers;

  /** Total commission earned DECIMAL(19,4). */
  private BigDecimal totalCommission;

  /** Status. See {@link net.lab1024.sa.igaming.common.constant.AgentStatusEnum}. */
  private Integer status;

  private String referralCode;

  private Boolean deleted;

  @Version private Integer version;
}
