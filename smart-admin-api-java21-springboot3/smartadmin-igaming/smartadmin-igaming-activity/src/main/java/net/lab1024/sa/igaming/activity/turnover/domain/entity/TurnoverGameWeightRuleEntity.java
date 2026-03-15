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
 * Turnover game weight rule entity — configures game category weights for turnover calculation
 * (Layer 3: Activity System).
 *
 * <p>This entity defines how much each game category contributes to valid turnover. For example:
 *
 * <ul>
 *   <li>Slots: 100% (full weight)
 *   <li>Live Casino (Baccarat): 15% (reduced due to low house edge)
 *   <li>Sports Betting: 100%
 * </ul>
 *
 * <p><b>Database Table:</b> t_turnover_game_weight_rule
 *
 * <p><b>Multi-Tenant Isolation:</b> Row-Level Security (RLS) enforced at database layer
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_turnover_game_weight_rule")
public class TurnoverGameWeightRuleEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long ruleId;

  /** Rule code (unique per tenant). Example: "GW_SLOTS_100", "GW_LIVE_15" */
  private String ruleCode;

  /** Rule name (display name). Example: "老虎機 100% 權重" */
  private String ruleName;

  /**
   * Game category. See {@link net.lab1024.sa.igaming.activity.turnover.constant.GameCategoryEnum}.
   */
  private Integer gameCategory;

  /** Weight percentage (0.00 ~ 100.00). DECIMAL(5,2) precision. */
  private BigDecimal weightPercentage;

  /** Priority (higher number = higher priority). Default: 100 */
  private Integer priority;

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
