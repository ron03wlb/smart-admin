package net.lab1024.sa.support.liteflow.config;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.spi.spring.SpringAware;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LiteFlow 手動配置類
 *
 * <p>手動創建 FlowExecutor bean，繞過官方自動配置問題
 *
 * @author SmartAdmin Team
 * @since 2026-03-13
 */
@Slf4j
@Configuration
@ConditionalOnProperty(
    prefix = "smart.liteflow",
    name = "manual-config-enabled",
    havingValue = "true")
public class LiteFlowManualConfiguration {

  /**
   * 手動創建 FlowExecutor bean
   *
   * @param applicationContext Spring ApplicationContext
   * @return FlowExecutor 實例
   */
  @Bean
  public FlowExecutor flowExecutor(ApplicationContext applicationContext) {
    log.info("Manually creating FlowExecutor bean");

    // 設置 Spring ApplicationContext 到 SpringAware（LiteFlow 需要）
    SpringAware springAware = new SpringAware();
    springAware.setApplicationContext(applicationContext);

    // 創建 LiteFlow 配置（使用空 XML，chain 將通過 TurnoverCalculationService 動態添加）
    LiteflowConfig config = new LiteflowConfig();
    config.setRuleSource("liteflow/liteflow.xml");

    // 初始化 FlowExecutor
    FlowExecutor executor = new FlowExecutor();
    try {
      executor.setLiteflowConfig(config);
      executor.init(true); // true = reload rules

      log.info("FlowExecutor initialized successfully with SQL rule source");
      return executor;
    } catch (Exception e) {
      log.error("Failed to initialize FlowExecutor", e);
      throw new IllegalStateException("FlowExecutor initialization failed", e);
    }
  }
}
