package net.lab1024.sa.support.liteflow.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * LiteFlow Properties 配置 - 無條件註冊
 *
 * <p>此配置類負責註冊 LiteFlowProperties bean，不受 @ConditionalOnProperty 限制。 這樣可以確保即使 LiteFlow 模塊未啟用，依賴
 * LiteFlowProperties 的 @Component 類也能正常創建。
 *
 * @author SmartAdmin Team
 * @since 2026-02-05
 */
@Configuration
@EnableConfigurationProperties(LiteFlowProperties.class)
public class LiteFlowPropertiesConfiguration {
  // Empty configuration class - only registers LiteFlowProperties bean
}
