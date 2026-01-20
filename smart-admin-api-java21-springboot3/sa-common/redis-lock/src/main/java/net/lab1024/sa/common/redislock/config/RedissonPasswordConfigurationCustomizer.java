package net.lab1024.sa.common.redislock.config;

import org.redisson.config.Config;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.stereotype.Component;

/**
 * redission对于password 为空处理有问题，重新设置下
 *
 * @author 1024创新实验室-主任:卓大
 * @since 2024/7/16 01:04:18 Copyright <a href="https://1024lab.net">1024创新实验室</a> ，Since 2012
 */
@Component
public class RedissonPasswordConfigurationCustomizer
    implements RedissonAutoConfigurationCustomizer {

  @Override
  public void customize(Config configuration) {
    if (configuration.isSingleConfig() && isEmpty(configuration.useSingleServer().getPassword())) {
      configuration.useSingleServer().setPassword(null);
    }

    if (configuration.isClusterConfig()
        && isEmpty(configuration.useClusterServers().getPassword())) {
      configuration.useClusterServers().setPassword(null);
    }
    if (configuration.isSentinelConfig()
        && isEmpty(configuration.useSentinelServers().getPassword())) {
      configuration.useSentinelServers().setPassword(null);
    }
  }

  private static boolean isEmpty(String str) {
    return str == null || str.isEmpty();
  }
}
