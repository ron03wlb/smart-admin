package net.lab1024.sa.base.module.support.serialnumber.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Serial Number Module Auto-Configuration
 *
 * <p>This module provides functionality for generating unique serial numbers with various patterns.
 *
 * <p>Enable/disable via configuration:
 *
 * <pre>
 * smart-admin:
 *   support:
 *     serialnumber:
 *       enabled: true  # default is true
 * </pre>
 *
 * @author 1024创新实验室
 * @since 2024
 */
@AutoConfiguration
@ConditionalOnProperty(
    prefix = "smart-admin.support.serialnumber",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@ComponentScan("net.lab1024.sa.base.module.support.serialnumber")
@EnableScheduling
public class SerialNumberAutoConfiguration {
  // Auto-configuration for serial number module
}
