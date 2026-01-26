package net.lab1024.sa.foundation.captcha.config;

import net.lab1024.sa.foundation.cache.CacheService;
import net.lab1024.sa.foundation.captcha.CaptchaService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 验证码自动配置类
 *
 * <p>提供验证码服务的自动装配，简化应用集成验证码功能的配置。
 *
 * <p>使用方式：
 *
 * <pre>
 * 1. 在项目中引入 sa-common:captcha 依赖
 * 2. Spring Boot 会自动装配 CaptchaService
 * 3. 直接注入使用：private final CaptchaService captchaService;
 * </pre>
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2025-01-20 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@AutoConfiguration
public class CaptchaAutoConfiguration {

  /**
   * 自动装配验证码服务
   *
   * <p>如果应用程序已经定义了 CaptchaService Bean，则不会创建此 Bean。
   *
   * @param cacheService 缓存服务，用于存储验证码
   * @return CaptchaService 验证码服务实例
   */
  @Bean
  @ConditionalOnMissingBean(CaptchaService.class)
  public CaptchaService captchaService(CacheService cacheService) {
    return new CaptchaService(cacheService);
  }
}
