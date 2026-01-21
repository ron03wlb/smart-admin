package net.lab1024.sa.base.web.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * SmartAdmin Base Web - AutoConfiguration for web module
 *
 * @author 1024创新实验室
 * @since 2026-01-21
 */
@AutoConfiguration
@Import({JsonConfig.class, CorsFilterConfig.class, AsyncConfig.class})
public class WebAutoConfiguration {
  // AutoConfiguration marker class
}
