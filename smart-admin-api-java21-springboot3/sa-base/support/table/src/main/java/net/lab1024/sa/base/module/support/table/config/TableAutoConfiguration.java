package net.lab1024.sa.base.module.support.table.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;

/**
 * Table Column Customization Module Auto-Configuration
 *
 * <p>This module provides functionality for users to customize table columns in the frontend,
 * storing their preferences in the database.
 *
 * <p>Enable/disable via configuration:
 *
 * <pre>
 * smart-admin:
 *   support:
 *     table:
 *       enabled: true  # default is true
 * </pre>
 *
 * @author 1024创新实验室
 * @since 2024
 */
@AutoConfiguration
@ConditionalOnProperty(
    prefix = "smart-admin.support.table",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@ComponentScan("net.lab1024.sa.base.module.support.table")
public class TableAutoConfiguration {
  // Auto-configuration for table column customization module
}
