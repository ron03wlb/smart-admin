package net.lab1024.sa.igaming.agent.credit.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * Agent credit limit tracking entity.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_agent_credit")
public class AgentCreditEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long agentCreditId;

  private Long agentId;

  private Long parentId;

  /** Credit limit DECIMAL(19,4). */
  private BigDecimal creditLimit;

  /** Used credit DECIMAL(19,4). */
  private BigDecimal usedCredit;

  /** Credit allocated to children DECIMAL(19,4). */
  private BigDecimal allocatedToChildren;

  /** Position percent DECIMAL(8,4). */
  private BigDecimal positionPercent;

  /** Max position cap DECIMAL(8,4). */
  private BigDecimal maxPosition;

  /** Status. See {@link net.lab1024.sa.igaming.common.constant.AgentStatusEnum}. */
  private Integer status;

  private OffsetDateTime frozenAt;

  private OffsetDateTime lastSettlementTime;

  private Boolean deleted;

  @Version private Integer version;
}
