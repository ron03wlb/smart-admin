package net.lab1024.sa.common.redislock.config;

import com.baomidou.lock.LockTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.ComponentScan;

/**
 * Lock4j 自動配置類
 *
 * <p>啟用 Lock4j 分佈式鎖功能，使用 Redisson 作為底層實現
 *
 * @author ron.chang
 * @since 2026-01-19
 */
@AutoConfiguration
@ConditionalOnClass(LockTemplate.class)
@ComponentScan(basePackages = "net.lab1024.sa.foundation.redislock")
public class Lock4jAutoConfiguration {
  // Lock4j 會自動配置 LockTemplate Bean
  // 通過 lock4j-redisson-spring-boot-starter 依賴，自動使用現有的 RedissonClient
}
