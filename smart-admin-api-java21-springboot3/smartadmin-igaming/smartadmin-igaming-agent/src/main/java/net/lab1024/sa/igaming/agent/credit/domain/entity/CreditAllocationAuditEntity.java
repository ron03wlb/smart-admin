package net.lab1024.sa.igaming.agent.credit.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * Credit allocation audit trail entity (immutable, INSERT only).
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_credit_allocation_audit")
public class CreditAllocationAuditEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long auditId;

  private Long parentId;

  private Long childId;

  /** Old credit limit DECIMAL(19,4). */
  private BigDecimal oldLimit;

  /** New credit limit DECIMAL(19,4). */
  private BigDecimal newLimit;

  /** Change delta DECIMAL(19,4). */
  private BigDecimal delta;

  /** Old position percent DECIMAL(8,4). */
  private BigDecimal oldPosition;

  /** New position percent DECIMAL(8,4). */
  private BigDecimal newPosition;

  private String reason;

  private String operator;
}
