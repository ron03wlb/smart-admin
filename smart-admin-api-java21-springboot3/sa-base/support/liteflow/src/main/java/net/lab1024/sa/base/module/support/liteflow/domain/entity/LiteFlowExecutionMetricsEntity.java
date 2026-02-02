package net.lab1024.sa.base.module.support.liteflow.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * LiteFlow 執行指標聚合實體（每日統計）
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@Data
@TableName("t_liteflow_execution_metrics")
public class LiteFlowExecutionMetricsEntity {

  /** 指標ID（主鍵） */
  @TableId(type = IdType.AUTO)
  private Long metricId;

  /** 流程編碼 */
  private String chainCode;

  /** 統計日期 */
  private LocalDate metricDate;

  /** 執行總數 */
  private Integer totalCount;

  /** 成功數 */
  private Integer successCount;

  /** 失敗數 */
  private Integer failureCount;

  /** 平均執行時間（毫秒） */
  private Integer avgExecutionTime;

  /** 最大執行時間（毫秒） */
  private Integer maxExecutionTime;

  /** 最小執行時間（毫秒） */
  private Integer minExecutionTime;

  /** 創建時間 */
  private LocalDateTime createTime;

  /** 更新時間 */
  private LocalDateTime updateTime;
}
