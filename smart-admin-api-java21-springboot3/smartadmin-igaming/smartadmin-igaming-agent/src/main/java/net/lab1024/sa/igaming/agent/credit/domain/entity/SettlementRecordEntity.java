package net.lab1024.sa.igaming.agent.credit.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * Weekly/monthly settlement record entity.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_settlement_record")
public class SettlementRecordEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long settlementRecordId;

  private Long agentId;

  private Long parentId;

  /** Settlement week identifier (e.g. 2026-W09). */
  private String settlementWeek;

  /** Settlement phase. See {@link net.lab1024.sa.igaming.common.constant.SettlementPhaseEnum}. */
  private Integer settlementPhase;

  /** Player loss DECIMAL(19,4). */
  private BigDecimal playerLoss;

  /** Agent own share DECIMAL(19,4). */
  private BigDecimal ownShare;

  /** Amount to parent DECIMAL(19,4). */
  private BigDecimal toParent;

  /** Amount to platform DECIMAL(19,4). */
  private BigDecimal toPlatform;

  /** Payment status. See {@link net.lab1024.sa.igaming.common.constant.PaymentVerifyStatusEnum}. */
  private Integer paymentStatus;

  private String paymentTxnId;

  private OffsetDateTime verifiedAt;
}
