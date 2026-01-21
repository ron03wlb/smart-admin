package net.lab1024.sa.base.swagger.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * SmartAdmin Base Swagger - AutoConfiguration for swagger module
 *
 * @author 1024创新实验室
 * @since 2026-01-21
 */
@AutoConfiguration
@Import({SwaggerConfig.class})
public class SwaggerAutoConfiguration {
  // AutoConfiguration marker class
}
