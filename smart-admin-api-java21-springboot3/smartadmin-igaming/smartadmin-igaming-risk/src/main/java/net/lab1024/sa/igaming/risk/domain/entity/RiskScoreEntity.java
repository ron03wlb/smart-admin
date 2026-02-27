package net.lab1024.sa.igaming.risk.domain.entity;

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
 * Player cumulative risk profile entity.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_risk_score")
public class RiskScoreEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long riskScoreId;

  private Long playerId;

  /** Cumulative risk score (weighted moving average) DECIMAL(8,4). */
  private BigDecimal cumulativeScore;

  /** Risk level. See {@link net.lab1024.sa.igaming.common.constant.RiskLevelEnum}. */
  private Integer riskLevel;

  private Integer totalAssessments;

  private OffsetDateTime lastAssessmentTime;

  /** Whether the player is auto-locked due to high risk. */
  private Boolean autoLocked;

  @Version private Integer version;
}
