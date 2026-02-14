package net.lab1024.sa.support.message.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;

/**
 * Message Module Auto-Configuration
 *
 * <p>This module provides functionality for system message management.
 *
 * <p>Enable/disable via configuration:
 *
 * <pre>
 * smart-admin:
 *   support:
 *     message:
 *       enabled: true  # default is true
 * </pre>
 *
 * @author 1024创新实验室
 * @since 2024
 */
@AutoConfiguration
@ConditionalOnProperty(
    prefix = "smart-admin.support.message",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@ComponentScan("net.lab1024.sa.support.message")
public class MessageAutoConfiguration {
  // Auto-configuration for message module
}
