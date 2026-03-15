package net.lab1024.sa.igaming.activity.turnover.domain.entity;

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
 * Turnover odds threshold rule entity — configures odds thresholds for turnover qualification
 * (Layer 2: Finance Center - Risk Filter).
 *
 * <p>This entity defines minimum odds requirements for bets to count toward turnover. For example:
 *
 * <ul>
 *   <li>European Odds (EUR): >= 1.5
 *   <li>Hong Kong Odds (HK): >= 0.5
 *   <li>Malay Odds (MY): > 0.5
 *   <li>Indonesian Odds (ID): >= 1.2
 * </ul>
 *
 * <p><b>Anti-Arbitrage Protection:</b> Low odds bets (e.g., betting on both red and black in
 * roulette) are excluded from turnover to prevent bonus abuse.
 *
 * <p><b>Database Table:</b> t_turnover_odds_threshold_rule
 *
 * <p><b>Multi-Tenant Isolation:</b> Row-Level Security (RLS) enforced at database layer
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_turnover_odds_threshold_rule")
public class TurnoverOddsThresholdRuleEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long ruleId;

  /** Rule code (unique per tenant). Example: "OT_EUR_1_5", "OT_HK_0_5" */
  private String ruleCode;

  /** Rule name (display name). Example: "歐洲盤 >= 1.5" */
  private String ruleName;

  /** Odds type. See {@link net.lab1024.sa.igaming.activity.turnover.constant.OddsTypeEnum}. */
  private Integer oddsType;

  /** Threshold value. DECIMAL(10,4) precision. Example: 1.5000, 0.5000 */
  private BigDecimal thresholdValue;

  /** Comparison operator. VARCHAR(2). Values: '>=', '>', '<=', '<', '=' */
  private String comparisonOperator;

  /** Effective from timestamp (nullable - applies immediately if null). */
  private OffsetDateTime effectiveFrom;

  /** Effective to timestamp (nullable - never expires if null). */
  private OffsetDateTime effectiveTo;

  /**
   * Status. See {@link net.lab1024.sa.igaming.activity.turnover.constant.TurnoverRuleStatusEnum}.
   */
  private Integer status;

  /** Remark (business notes). */
  private String remark;

  /** Soft delete flag. */
  private Boolean deleted;

  /** Optimistic lock version. */
  @Version private Integer version;
}
