package net.lab1024.sa.igaming.game.domain.vo;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * Reconciliation summary VO.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class ReconciliationVO {

  private Long reconciliationId;
  private LocalDate reconciliationDate;
  private String providerCode;
  private Integer totalGpTransactions;
  private Integer totalPlatformTransactions;
  private Integer matchedCount;
  private Integer mismatchCount;
  private Integer missingCount;
  private Integer extraCount;
  private Integer status;
  private OffsetDateTime createTime;
}
