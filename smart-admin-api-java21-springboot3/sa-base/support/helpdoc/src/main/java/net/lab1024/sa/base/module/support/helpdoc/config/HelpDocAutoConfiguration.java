package net.lab1024.sa.base.module.support.helpdoc.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;

/**
 * Help Document Module Auto-Configuration
 *
 * <p>This module provides functionality for help documentation management.
 *
 * <p>Enable/disable via configuration:
 *
 * <pre>
 * smart-admin:
 *   support:
 *     helpdoc:
 *       enabled: true  # default is true
 * </pre>
 *
 * @author 1024创新实验室
 * @since 2024
 */
@AutoConfiguration
@ConditionalOnProperty(
    prefix = "smart-admin.support.helpdoc",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@ComponentScan("net.lab1024.sa.base.module.support.helpdoc")
public class HelpDocAutoConfiguration {
  // Auto-configuration for help document module
}
