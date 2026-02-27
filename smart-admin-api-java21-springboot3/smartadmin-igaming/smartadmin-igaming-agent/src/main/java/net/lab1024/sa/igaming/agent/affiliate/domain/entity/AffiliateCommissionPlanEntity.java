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
 * Affiliate commission plan configuration entity.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_affiliate_commission_plan")
public class AffiliateCommissionPlanEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long planId;

  private String planName;

  /** Plan type. See {@link net.lab1024.sa.igaming.common.constant.CommissionPlanTypeEnum}. */
  private Integer planType;

  /** Tier configuration as JSONB string. */
  private String tiersJson;

  /** Settlement period. See {@link net.lab1024.sa.igaming.common.constant.SettlementPeriodEnum}. */
  private Integer settlementPeriod;

  private Boolean negativeCarryover;

  /** Reset threshold DECIMAL(19,4). */
  private BigDecimal resetThreshold;

  private Boolean enabled;

  private Boolean deleted;

  @Version private Integer version;
}
