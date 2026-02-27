package net.lab1024.sa.igaming.agent.affiliate.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * Affiliate commission settlement record entity.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_affiliate_commission_record")
public class AffiliateCommissionRecordEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long recordId;

  private Long agentId;

  private Long planId;

  private LocalDate settlementDate;

  /** Gross commission amount DECIMAL(19,4). */
  private BigDecimal grossAmount;

  /** Adjustment amount DECIMAL(19,4). */
  private BigDecimal adjustmentAmount;

  /** Carryover from previous period DECIMAL(19,4). */
  private BigDecimal carryoverAmount;

  /** Net commission amount DECIMAL(19,4). */
  private BigDecimal netAmount;

  /** Status. See {@link net.lab1024.sa.igaming.common.constant.CommissionRecordStatusEnum}. */
  private Integer status;

  private String approvedBy;

  private OffsetDateTime approvedAt;
}
