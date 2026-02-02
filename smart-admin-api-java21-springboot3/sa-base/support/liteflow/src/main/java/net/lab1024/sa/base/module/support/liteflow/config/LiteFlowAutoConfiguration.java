package net.lab1024.sa.base.module.support.liteflow.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * LiteFlow 自動配置
 *
 * <p>條件啟用：當 smart.liteflow.enabled = true 時生效
 *
 * <p>使用 LiteFlow SQL 配置源，需要在 application.yaml 配置：
 *
 * <pre>{@code
 * liteflow:
 *   rule-source-ext-data-map:
 *     url: jdbc:postgresql://localhost:5432/smartadmin
 *     driverClassName: org.postgresql.Driver
 *     username: postgres
 *     password: password
 *     applicationName: smartadmin
 *     chainTableName: t_liteflow_chain
 *     chainNameField: chain_code
 *     elDataField: chain_data
 *     chainEnableField: status
 *     scriptTableName: t_liteflow_script
 *     scriptIdField: script_code
 *     scriptDataField: script_data
 *     scriptTypeField: script_type
 *     scriptLanguageField: script_type
 * }</pre>
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(LiteFlowProperties.class)
@ConditionalOnProperty(prefix = "smart.liteflow", name = "enabled", havingValue = "true")
public class LiteFlowAutoConfiguration {

  public LiteFlowAutoConfiguration() {
    log.info("LiteFlow workflow engine module initialized");
  }
}
