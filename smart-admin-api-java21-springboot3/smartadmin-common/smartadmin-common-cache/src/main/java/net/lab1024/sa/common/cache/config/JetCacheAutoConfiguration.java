package net.lab1024.sa.common.cache.config;

import com.alicp.jetcache.CacheBuilder;
import com.alicp.jetcache.anno.CacheConsts;
import com.alicp.jetcache.anno.config.EnableCreateCacheAnnotation;
import com.alicp.jetcache.anno.config.EnableMethodCache;
import com.alicp.jetcache.anno.support.GlobalCacheConfig;
import com.alicp.jetcache.anno.support.SpringConfigProvider;
import com.alicp.jetcache.embedded.CaffeineCacheBuilder;
import com.alicp.jetcache.redis.lettuce.RedisLettuceCacheBuilder;
import com.alicp.jetcache.support.FastjsonKeyConvertor;
import com.alicp.jetcache.support.JavaValueDecoder;
import com.alicp.jetcache.support.JavaValueEncoder;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * JetCache 两级缓存自动配置
 *
 * <p>配置本地缓存(Caffeine) + 远程缓存(Redis Lettuce)
 *
 * @author 1024创新实验室
 * @since 2025-01-19 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@AutoConfiguration
@ConditionalOnClass(GlobalCacheConfig.class)
@EnableMethodCache(basePackages = "net.lab1024.sa")
@EnableCreateCacheAnnotation
@ComponentScan(basePackages = "net.lab1024.sa.foundation.cache")
public class JetCacheAutoConfiguration {

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

  /** Lettuce Redis Client */
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

  /** JetCache SpringConfigProvider */
  @Bean
  @ConditionalOnMissingBean
  public SpringConfigProvider springConfigProvider() {
    return new SpringConfigProvider();
  }

  /** JetCache GlobalCacheConfig - 两级缓存配置 */
  @Bean
  @ConditionalOnMissingBean
  public GlobalCacheConfig globalCacheConfig(RedisClient redisClient) {
    // 本地缓存配置 (Caffeine)
    Map<String, CacheBuilder> localBuilders = new HashMap<>();
    localBuilders.put(
        CacheConsts.DEFAULT_AREA,
        CaffeineCacheBuilder.createCaffeineCacheBuilder()
            .keyConvertor(FastjsonKeyConvertor.INSTANCE)
            .limit(1000)
            .expireAfterWrite(30, TimeUnit.MINUTES));

    // 远程缓存配置 (Redis Lettuce)
    // Key 前缀格式: {projectName}:{environment}:
    String keyPrefix = projectName + ":" + environment + ":";
    Map<String, CacheBuilder> remoteBuilders = new HashMap<>();
    remoteBuilders.put(
        CacheConsts.DEFAULT_AREA,
        RedisLettuceCacheBuilder.createRedisLettuceCacheBuilder()
            .keyConvertor(FastjsonKeyConvertor.INSTANCE)
            .valueEncoder(JavaValueEncoder.INSTANCE)
            .valueDecoder(JavaValueDecoder.INSTANCE)
            .redisClient(redisClient)
            .keyPrefix(keyPrefix)
            .expireAfterWrite(2, TimeUnit.HOURS));

    GlobalCacheConfig globalCacheConfig = new GlobalCacheConfig();
    globalCacheConfig.setLocalCacheBuilders(localBuilders);
    globalCacheConfig.setRemoteCacheBuilders(remoteBuilders);
    globalCacheConfig.setStatIntervalMinutes(15);
    globalCacheConfig.setAreaInCacheName(false);

    return globalCacheConfig;
  }

  /**
   * 提供缓存 Key 前缀
   *
   * @return Key 前缀，格式为 {projectName}:{environment}:
   */
  @Bean
  public String cacheKeyPrefix() {
    return projectName + ":" + environment + ":";
  }
}
