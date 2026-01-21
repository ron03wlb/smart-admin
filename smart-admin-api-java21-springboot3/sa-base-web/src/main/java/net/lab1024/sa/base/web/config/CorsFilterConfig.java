package net.lab1024.sa.base.web.config;

import net.lab1024.sa.base.core.config.SystemEnvironmentConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域配置
 *
 * @author 1024创新实验室: 罗伊
 * @since 2021/11/15 20:38 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Configuration
@Conditional(SystemEnvironmentConfig.class)
public class CorsFilterConfig {

  @Value("${access-control-allow-origin}")
  private String accessControlAllowOrigin;

  /** 跨域配置 */
  @Bean
  public CorsFilter corsFilter() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowCredentials(true);
    // 设置访问源地址
    config.addAllowedOriginPattern(accessControlAllowOrigin);
    // 设置访问源请求头
    config.addAllowedHeader("*");
    // 设置访问源请求方法
    config.addAllowedMethod("*");
    // 对接口配置跨域设置
    source.registerCorsConfiguration("/**", config);
    return new CorsFilter(source);
  }
}
