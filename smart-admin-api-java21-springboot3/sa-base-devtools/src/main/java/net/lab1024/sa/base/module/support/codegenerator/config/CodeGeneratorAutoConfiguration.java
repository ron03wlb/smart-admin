package net.lab1024.sa.base.module.support.codegenerator.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;

/**
 * Code Generator Auto Configuration for Spring Boot 3.x
 *
 * <p>This auto-configuration class enables the code generator module by scanning all components
 * within the {@code net.lab1024.sa.base.module.support.codegenerator} package.
 *
 * <p>The code generator can be enabled or disabled via the following configuration property:
 *
 * <pre>
 * smart-admin:
 *   devtools:
 *     code-generator:
 *       enabled: true  # default: true
 * </pre>
 *
 * <p>When enabled, the following components will be registered:
 *
 * <ul>
 *   <li>CodeGeneratorController - REST endpoints for code generation
 *   <li>CodeGeneratorService - Core code generation business logic
 *   <li>CodeGeneratorTemplateService - Template processing service
 *   <li>Various variable services for template variable generation
 *   <li>DAO mappers for database table inspection
 * </ul>
 *
 * @author 1024创新实验室
 * @since 2025-01-21 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@AutoConfiguration
@ConditionalOnProperty(
    prefix = "smart-admin.devtools.code-generator",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@ComponentScan(basePackages = "net.lab1024.sa.base.module.support.codegenerator")
public class CodeGeneratorAutoConfiguration {
  // Component scanning handles all bean registration
  // No explicit @Bean definitions needed as all components use @Service, @Controller, @Mapper
}
