package net.lab1024.sa.igaming.wallet.payment.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * Payment reconciliation entity — daily per-PSP reconciliation summary.
 *
 * @author iGaming Team
 * @since 2026-02-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_payment_reconciliation")
public class PaymentReconciliationEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long reconciliationId;

  private LocalDate reconciliationDate;

  private String pspCode;

  private Integer totalPspTransactions;

  private Integer totalPlatformTransactions;

  private Integer matchedCount;

  private Integer mismatchCount;

  private Integer missingCount;

  private Integer extraCount;

  private BigDecimal totalPspAmount;

  private BigDecimal totalPlatformAmount;

  private BigDecimal amountDifference;

  /** See {@link net.lab1024.sa.igaming.common.constant.ReconciliationStatusEnum}. */
  private Integer status;

  private Boolean deleted;
}
