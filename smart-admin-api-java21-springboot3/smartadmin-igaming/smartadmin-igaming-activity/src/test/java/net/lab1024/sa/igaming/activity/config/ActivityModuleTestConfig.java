package net.lab1024.sa.igaming.activity.config;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Test configuration for iGaming Activity module integration tests.
 *
 * <p>This configuration class enables component scanning for the activity module and provides a
 * minimal Spring Boot test context without requiring the full SmartAdminApplication.
 *
 * <p><b>Usage:</b> Used by {@link net.lab1024.sa.igaming.activity.turnover.BaseIntegrationTest} to
 * bootstrap Spring context for integration tests with Testcontainers.
 *
 * @author iGaming Team
 * @since 2026-03-17
 */
@SpringBootApplication(scanBasePackages = "net.lab1024.sa.igaming.activity")
public class ActivityModuleTestConfig {
  // Empty configuration class - Spring Boot will auto-configure based on classpath
}
