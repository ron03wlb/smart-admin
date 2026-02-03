package net.lab1024.sa.support.heartbeat.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;

/**
 * Heartbeat Module Auto-Configuration
 *
 * <p>This module provides functionality for server heartbeat monitoring and recording.
 *
 * <p>Enable/disable via configuration:
 *
 * <pre>
 * smart-admin:
 *   support:
 *     heartbeat:
 *       enabled: true  # default is true
 * </pre>
 *
 * @author 1024创新实验室
 * @since 2024
 */
@AutoConfiguration
@ConditionalOnProperty(
    prefix = "smart-admin.support.heartbeat",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@ComponentScan("net.lab1024.sa.support.heartbeat")
public class HeartBeatAutoConfiguration {
  // Auto-configuration for heartbeat module
}
