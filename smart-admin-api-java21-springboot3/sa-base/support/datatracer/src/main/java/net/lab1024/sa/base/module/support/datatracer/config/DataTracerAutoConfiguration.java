package net.lab1024.sa.base.module.support.datatracer.config;

import net.lab1024.sa.base.datasource.config.DataSourceAutoConfiguration;
import net.lab1024.sa.base.mybatis.config.MybatisAutoConfiguration;
import net.lab1024.sa.base.web.config.WebAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;

/**
 * SmartAdmin Base Support DataTracer - AutoConfiguration for data change tracking module
 *
 * @author 1024创新实验室
 * @since 2026-01-21
 */
@AutoConfiguration
@AutoConfigureAfter({
  WebAutoConfiguration.class,
  MybatisAutoConfiguration.class,
  DataSourceAutoConfiguration.class
})
public class DataTracerAutoConfiguration {
  // AutoConfiguration marker class
  // Note: Uses compileOnly dependency on dict module to avoid circular dependency
}
