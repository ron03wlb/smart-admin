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
 * Turnover status factor rule entity — configures settlement status factors for turnover
 * calculation (Layer 2: Finance Center).
 *
 * <p>This entity defines how each settlement status affects turnover calculation:
 *
 * <ul>
 *   <li>WIN/LOSS/HALF_WIN/HALF_LOSS: 100% (counts fully toward turnover)
 *   <li>DRAW/CANCEL: 0% (does not count toward turnover)
 *   <li>VOID: 0% (bet invalidated)
 * </ul>
 *
 * <p><b>Important:</b> SmartAdmin uses "Standard Principal Method" where HALF_WIN/HALF_LOSS count
 * as 100% of bet amount (not 50%).
 *
 * <p><b>Database Table:</b> t_turnover_status_factor_rule
 *
 * <p><b>Multi-Tenant Isolation:</b> Row-Level Security (RLS) enforced at database layer
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_turnover_status_factor_rule")
public class TurnoverStatusFactorRuleEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long ruleId;

  /** Rule code (unique per tenant). Example: "SF_WIN_100", "SF_DRAW_0" */
  private String ruleCode;

  /** Rule name (display name). Example: "贏局 100% 流水" */
  private String ruleName;

  /**
   * Settlement status. See {@link
   * net.lab1024.sa.igaming.activity.turnover.constant.SettlementStatusEnum}.
   */
  private Integer settlementStatus;

  /** Factor percentage (0.00 ~ 100.00). DECIMAL(5,2) precision. */
  private BigDecimal factorPercentage;

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
