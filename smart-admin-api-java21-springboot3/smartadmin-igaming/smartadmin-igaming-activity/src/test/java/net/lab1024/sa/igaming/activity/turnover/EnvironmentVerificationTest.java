package net.lab1024.sa.igaming.activity.turnover;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Environment verification test — validates test infrastructure setup.
 *
 * <p>This test ensures that:
 *
 * <ul>
 *   <li>Spring Boot test context loads successfully
 *   <li>Testcontainers PostgreSQL container starts correctly
 *   <li>Database connection is established
 *   <li>Basic JDBC operations work
 * </ul>
 *
 * <p><b>Purpose:</b> Smoke test to validate Phase 1 test infrastructure before implementing actual
 * test cases.
 *
 * @author iGaming Team
 * @since 2026-03-15
 */
@DisplayName("環境驗證測試")
class EnvironmentVerificationTest extends BaseIntegrationTest {

  @Autowired(required = false)
  private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("Spring Boot 測試上下文應該正確加載")
  void springContextShouldLoad() {
    // If context loads, this test passes automatically
    assertThat(true).isTrue();
  }

  @Test
  @DisplayName("PostgreSQL Testcontainers應該正常運行")
  void postgresContainerShouldBeRunning() {
    assertThat(postgres.isRunning()).isTrue();
    assertThat(postgres.getDatabaseName()).isEqualTo("testdb");
    assertThat(postgres.getUsername()).isEqualTo("test");
  }

  @Test
  @DisplayName("資料庫連接應該可用")
  void databaseConnectionShouldWork() {
    assertThat(jdbcTemplate).isNotNull();

    // Verify database connection by executing simple query
    Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
    assertThat(result).isEqualTo(1);
  }

  @Test
  @DisplayName("TurnoverTestFixture應該可用")
  void turnoverTestFixtureShouldBeAccessible() {
    // Verify TurnoverTestFixture static methods are accessible
    var context =
        net.lab1024.sa.igaming.activity.turnover.service.TurnoverTestFixture.createContext(
            new java.math.BigDecimal("100.00"), 1, 1);

    assertThat(context).isNotNull();
    assertThat(context.getBetId()).startsWith("BET-TEST-");
    assertThat(context.getBetAmount()).isEqualByComparingTo("100.00");
    assertThat(context.getGameCategory()).isEqualTo(1);
  }
}
