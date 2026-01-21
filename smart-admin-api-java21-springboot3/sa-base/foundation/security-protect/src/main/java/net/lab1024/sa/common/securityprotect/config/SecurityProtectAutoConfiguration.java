package net.lab1024.sa.common.securityprotect.config;

import net.lab1024.sa.common.securityprotect.service.FileSecurityService;
import net.lab1024.sa.common.securityprotect.service.PasswordComplexityService;
import net.lab1024.sa.common.securityprotect.service.PasswordEncryptService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 安全保护模块自动配置类
 *
 * <p>提供安全保护相关服务的自动装配，简化应用集成安全功能的配置。
 *
 * <p>使用方式：
 *
 * <pre>
 * 1. 在项目中引入 sa-common:security-protect 依赖
 * 2. Spring Boot 会自动装配以下服务：
 *    - PasswordEncryptService: 密码加密服务
 *    - PasswordComplexityService: 密码复杂度校验服务
 *    - FileSecurityService: 文件安全检测服务
 * 3. 直接注入使用
 * </pre>
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2025-01-20 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@AutoConfiguration
public class SecurityProtectAutoConfiguration {

  /**
   * 自动装配密码加密服务
   *
   * @return PasswordEncryptService 密码加密服务实例
   */
  @Bean
  @ConditionalOnMissingBean(PasswordEncryptService.class)
  public PasswordEncryptService passwordEncryptService() {
    return new PasswordEncryptService();
  }

  /**
   * 自动装配密码复杂度校验服务
   *
   * @return PasswordComplexityService 密码复杂度校验服务实例
   */
  @Bean
  @ConditionalOnMissingBean(PasswordComplexityService.class)
  public PasswordComplexityService passwordComplexityService() {
    return new PasswordComplexityService();
  }

  /**
   * 自动装配文件安全检测服务
   *
   * @return FileSecurityService 文件安全检测服务实例
   */
  @Bean
  @ConditionalOnMissingBean(FileSecurityService.class)
  public FileSecurityService fileSecurityService() {
    return new FileSecurityService();
  }
}
