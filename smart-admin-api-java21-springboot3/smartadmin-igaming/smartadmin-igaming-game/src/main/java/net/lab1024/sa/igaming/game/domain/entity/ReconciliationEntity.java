package net.lab1024.sa.igaming.game.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * Reconciliation entity — daily reconciliation summary table.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_reconciliation")
public class ReconciliationEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long reconciliationId;

  private LocalDate reconciliationDate;

  private String providerCode;

  private Integer totalGpTransactions;

  private Integer totalPlatformTransactions;

  private Integer matchedCount;

  private Integer mismatchCount;

  private Integer missingCount;

  private Integer extraCount;

  /**
   * Reconciliation status. See {@link
   * net.lab1024.sa.igaming.common.constant.ReconciliationStatusEnum}.
   */
  private Integer status;

  private Boolean deleted;
}
