package net.lab1024.sa.igaming.wallet.payment.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * Reconciliation exception entity — records discrepancies found during reconciliation.
 *
 * @author iGaming Team
 * @since 2026-02-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_reconciliation_exception")
public class ReconciliationExceptionEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long exceptionId;

  private Long reconciliationId;

  private String orderNo;

  /** See {@link net.lab1024.sa.igaming.common.constant.ReconciliationExceptionTypeEnum}. */
  private Integer exceptionType;

  private BigDecimal platformAmount;

  private BigDecimal pspAmount;

  private BigDecimal differenceAmount;

  /** Resolution status: 1=PENDING, 2=INVESTIGATING, 3=RESOLVED, 4=WRITE_OFF. */
  private Integer resolutionStatus;

  private String notes;

  private OffsetDateTime resolvedAt;

  private Boolean deleted;
}
