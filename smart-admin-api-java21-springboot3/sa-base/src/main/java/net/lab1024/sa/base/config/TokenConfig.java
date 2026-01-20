package net.lab1024.sa.base.config;

import cn.dev33.satoken.config.SaTokenConfig;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.securityprotect.service.SecurityConfigProvider;
import org.springframework.context.annotation.Configuration;

/**
 * 三级等保配置初始化后最低活跃频率全局配置
 *
 * @author 1024创新实验室-创始人兼主任:卓大
 * @since 2024/11/24 Copyright <a href="https://1024lab.net">1024创新实验室</a> ，Since 2012
 */
@Configuration
@RequiredArgsConstructor
public class TokenConfig {

  private final SecurityConfigProvider securityConfigProvider;

  // 此配置会覆盖 sa-base.yaml 中的配置
  @jakarta.annotation.Resource
  public void configSaToken(SaTokenConfig config) {
    config.setActiveTimeout(securityConfigProvider.getLoginActiveTimeoutSeconds());
  }
}
