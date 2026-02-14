package net.lab1024.sa.common.redis.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * SmartAdmin Base Redis - AutoConfiguration for redis module
 *
 * @author 1024创新实验室
 * @since 2026-01-21
 */
@AutoConfiguration
@Import({RedisConfig.class})
public class RedisAutoConfiguration {
  // AutoConfiguration marker class
}
