package net.lab1024.sa.igaming.activity.turnover;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests using Testcontainers.
 *
 * <p>Provides shared configuration for PostgreSQL 16 database container and Spring Boot test
 * context. All integration tests should extend this class to ensure consistent test environment.
 *
 * <p><b>Key Features:</b>
 *
 * <ul>
 *   <li>PostgreSQL 16-alpine container with auto-start lifecycle
 *   <li>Dynamic datasource configuration via {@code @DynamicPropertySource}
 *   <li>Spring Boot test context with {@code test} profile
 *   <li>Container reuse across test classes (via {@code testcontainers.properties})
 * </ul>
 *
 * <p><b>Usage Pattern:</b>
 *
 * <pre>
 * &#64;DisplayName("Turnover Calculation Integration Test")
 * class TurnoverCalculationIntegrationTest extends BaseIntegrationTest {
 *
 *     &#64;Autowired private TurnoverCalculationService service;
 *
 *     &#64;Test
 *     void shouldCalculateTurnoverCorrectly() {
 *         // Test implementation...
 *     }
 * }
 * </pre>
 *
 * @author iGaming Team
 * @since 2026-03-15
 */
@SpringBootTest(classes = {net.lab1024.sa.igaming.activity.config.ActivityModuleTestConfig.class})
@Testcontainers
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

  /**
   * PostgreSQL 16 database container shared across all integration tests.
   *
   * <p>Container lifecycle:
   *
   * <ul>
   *   <li>Started once before first test execution
   *   <li>Reused across test classes (if {@code testcontainers.reuse.enable=true})
   *   <li>Stopped after JVM shutdown
   * </ul>
   *
   * <p>Database credentials:
   *
   * <ul>
   *   <li>Database name: {@code testdb}
   *   <li>Username: {@code test}
   *   <li>Password: {@code test}
   * </ul>
   */
  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("testdb")
          .withUsername("test")
          .withPassword("test");

  /**
   * Configure Spring Boot datasource properties dynamically from Testcontainers.
   *
   * <p>Overrides {@code application-test.yml} datasource configuration with actual container JDBC
   * URL, username, and password.
   *
   * @param registry Spring Boot dynamic property registry
   */
  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }
}
