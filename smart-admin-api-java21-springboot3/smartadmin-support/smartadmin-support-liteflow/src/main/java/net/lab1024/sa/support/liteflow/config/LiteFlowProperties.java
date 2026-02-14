package net.lab1024.sa.support.liteflow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * LiteFlow 配置屬性
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@Data
@ConfigurationProperties(prefix = "smart.liteflow")
public class LiteFlowProperties {

  /** 是否啟用 LiteFlow 模塊 */
  private Boolean enabled = false;

  /** 是否啟用數據庫數據源 */
  private Boolean databaseEnabled = true;

  /** 是否啟用執行日誌記錄 */
  private Boolean executionLogEnabled = true;

  /** 是否啟用指標統計 */
  private Boolean metricsEnabled = true;

  /** 日誌保留天數 */
  private Integer logRetentionDays = 30;

  /** 規則重載間隔（秒）0 = 禁用自動重載 */
  private Integer reloadInterval = 0;

  /** 默認腳本類型 */
  private String defaultScriptType = "qlexpress";

  /** 是否啟用異步執行 */
  private Boolean asyncEnabled = false;

  /** 最大線程池大小 */
  private Integer maxThreadPoolSize = 10;

  /** 執行超時時間（毫秒） */
  private Integer executionTimeout = 30000;
}
