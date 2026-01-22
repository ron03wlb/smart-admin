package net.lab1024.sa.base.module.support.mail.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;

/**
 * Mail Module Auto-Configuration
 *
 * <p>This module provides functionality for sending emails with template support.
 *
 * <p>Enable/disable via configuration:
 *
 * <pre>
 * smart-admin:
 *   support:
 *     mail:
 *       enabled: true  # default is true
 * </pre>
 *
 * @author 1024创新实验室
 * @since 2024
 */
@AutoConfiguration
@ConditionalOnProperty(
    prefix = "smart-admin.support.mail",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@ComponentScan("net.lab1024.sa.base.module.support.mail")
public class MailAutoConfiguration {
  // Auto-configuration for mail module
}
