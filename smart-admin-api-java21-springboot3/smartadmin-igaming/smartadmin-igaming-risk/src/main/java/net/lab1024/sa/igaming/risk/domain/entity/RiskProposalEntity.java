package net.lab1024.sa.igaming.risk.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * Risk review proposal (work order) entity.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_risk_proposal")
public class RiskProposalEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long proposalId;

  private Long playerId;

  private Long assessmentId;

  /** Status. See {@link net.lab1024.sa.igaming.common.constant.RiskProposalStatusEnum}. */
  private Integer status;

  /** Priority. See {@link net.lab1024.sa.igaming.common.constant.RiskLevelEnum}. */
  private Integer priority;

  private String assignee;

  private String reviewComment;

  private OffsetDateTime slaDeadline;

  private OffsetDateTime resolvedAt;

  @Version private Integer version;
}
