package net.lab1024.sa.base.module.support.liteflow.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * LiteFlow 執行日誌實體
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@Data
@TableName("t_liteflow_execution_log")
public class LiteFlowExecutionLogEntity {

  /** 日誌ID（主鍵） */
  @TableId(type = IdType.AUTO)
  private Long logId;

  /** 執行的流程編碼 */
  private String chainCode;

  /** 請求追蹤ID（UUID） */
  private String requestId;

  /** 執行狀態：0-失敗 1-成功 */
  private Integer executionStatus;

  /** 執行時長（毫秒） */
  private Integer executionTime;

  /** 輸入參數（JSON格式） */
  private String inputParams;

  /** 輸出結果（JSON格式） */
  private String outputResult;

  /** 錯誤信息 */
  private String errorMessage;

  /** 錯誤堆棧（完整異常信息） */
  private String errorStack;

  /** 創建時間 */
  private LocalDateTime createTime;
}
