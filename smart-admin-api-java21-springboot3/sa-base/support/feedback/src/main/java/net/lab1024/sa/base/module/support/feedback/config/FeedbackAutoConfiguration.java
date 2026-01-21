package net.lab1024.sa.base.module.support.feedback.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;

/**
 * Feedback Module Auto-Configuration
 *
 * <p>This module provides functionality for users to submit feedback.
 *
 * <p>Enable/disable via configuration:
 *
 * <pre>
 * smart-admin:
 *   support:
 *     feedback:
 *       enabled: true  # default is true
 * </pre>
 *
 * @author 1024创新实验室
 * @since 2024
 */
@AutoConfiguration
@ConditionalOnProperty(
    prefix = "smart-admin.support.feedback",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@ComponentScan("net.lab1024.sa.base.module.support.feedback")
@MapperScan("net.lab1024.sa.base.module.support.feedback.dao")
public class FeedbackAutoConfiguration {
  // Auto-configuration for feedback module
}
