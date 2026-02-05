package net.lab1024.sa.common.cache.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * SmartAdmin 緩存配置 - 提供 RedisClient 和 cacheKeyPrefix beans
 *
 * <p>不創建 GlobalCacheConfig，讓官方 JetCache autoconfiguration 處理
 *
 * @author 1024創新實驗室
 * @since 2026-02-05 Copyright <a href="https://1024lab.net">1024創新實驗室</a>
 */
@Configuration
@ComponentScan(basePackages = "net.lab1024.sa.common.cache")
public class CacheConfig {

  @Value("${spring.data.redis.host:127.0.0.1}")
  private String redisHost;

  @Value("${spring.data.redis.port:6379}")
  private int redisPort;

  @Value("${spring.data.redis.password:}")
  private String redisPassword;

  @Value("${spring.data.redis.database:1}")
  private int redisDatabase;

  @Value("${spring.data.redis.timeout:10000ms}")
  private Duration redisTimeout;

  @Value("${project.name:smart-admin}")
  private String projectName;

  @Value("${spring.profiles.active:dev}")
  private String environment;

  /** Lettuce Redis Client - 用於 JetCacheServiceImpl */
  @Bean
  @ConditionalOnMissingBean
  public RedisClient redisClient() {
    RedisURI.Builder builder =
        RedisURI.builder().withHost(redisHost).withPort(redisPort).withDatabase(redisDatabase);

    if (redisPassword != null && !redisPassword.isBlank()) {
      builder.withPassword(redisPassword.toCharArray());
    }

    if (redisTimeout != null) {
      builder.withTimeout(redisTimeout);
    }

    RedisClient client = RedisClient.create(builder.build());
    client.setOptions(ClientOptions.builder().autoReconnect(true).build());
    return client;
  }

  /**
   * 提供緩存 Key 前綴 - 用於 JetCacheServiceImpl
   *
   * @return Key 前綴，格式為 {projectName}:{environment}:
   */
  @Bean
  public String cacheKeyPrefix() {
    return projectName + ":" + environment + ":";
  }
}
