package net.lab1024.sa.common.mybatis.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * SmartAdmin Base MyBatis - AutoConfiguration for mybatis module
 *
 * @author 1024创新实验室
 * @since 2026-01-21
 */
@AutoConfiguration
@Import({MybatisPlusConfig.class})
public class MybatisAutoConfiguration {
  // AutoConfiguration marker class
}
