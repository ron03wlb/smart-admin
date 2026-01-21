package net.lab1024.sa.base.datasource.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * SmartAdmin Base DataSource - AutoConfiguration for datasource module
 *
 * @author 1024创新实验室
 * @since 2026-01-21
 */
@AutoConfiguration
@Import({DataSourceConfig.class})
public class DataSourceAutoConfiguration {
  // AutoConfiguration marker class
}
