package net.lab1024.sa.starter.all.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;

/**
 * SmartAdmin Complete Starter 自動配置
 *
 * <p>完整功能啟動器，包含：
 *
 * <ul>
 *   <li>Web Starter（13 個 common 模塊）
 *   <li>數據庫 & ORM（MyBatis Plus, HikariCP, P6Spy）
 *   <li>緩存 & Redis（Redisson, JetCache, Lock4j）
 *   <li>消息隊列（Kafka 抽象）
 *   <li>所有 Support 模塊（17 個業務支持模塊）
 * </ul>
 *
 * <p>適用場景：開發環境、一體化部署
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@AutoConfiguration
public class StarterAllAutoConfiguration {
  // 標記類，依賴傳遞的 AutoConfiguration 生效
}
