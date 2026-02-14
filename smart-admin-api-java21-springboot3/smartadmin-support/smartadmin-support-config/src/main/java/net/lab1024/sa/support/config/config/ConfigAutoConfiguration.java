package net.lab1024.sa.support.config.config;

import net.lab1024.sa.common.mybatis.config.MybatisAutoConfiguration;
import net.lab1024.sa.common.web.web.config.WebAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;

/**
 * SmartAdmin Base Support Config - AutoConfiguration for system configuration module
 *
 * @author 1024创新实验室
 * @since 2026-01-21
 */
@AutoConfiguration
@AutoConfigureAfter({WebAutoConfiguration.class, MybatisAutoConfiguration.class})
public class ConfigAutoConfiguration {
  // AutoConfiguration marker class
}
