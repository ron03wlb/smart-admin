package net.lab1024.sa.igaming.agent.affiliate.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * Affiliate commission adjustment ledger entity (immutable, INSERT only).
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_affiliate_adjustment")
public class AffiliateAdjustmentEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long adjustmentId;

  private Long agentId;

  /** Adjustment type. See {@link net.lab1024.sa.igaming.common.constant.AdjustmentTypeEnum}. */
  private Integer adjustmentType;

  /** Adjustment amount DECIMAL(19,4). */
  private BigDecimal amount;

  private LocalDate originalSettlementDate;

  private LocalDate targetSettlementDate;

  private String reason;

  private String createdBy;
}
