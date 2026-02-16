package net.lab1024.sa.app.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Smoke test verifying Testcontainers infrastructure starts correctly.
 *
 * <p>Validates that PostgreSQL, Kafka, and Redis containers start and accept connections. This test
 * does NOT start the Spring application context (no {@code @SpringBootTest}) to keep it fast and
 * independent of database schema.
 *
 * <p>For full integration tests that need the Spring context, extend {@link
 * AbstractIntegrationTestBase} instead.
 *
 * @since 4.1.0
 */
@Tag("integration")
@Testcontainers
class ContainerSmokeTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("smoke_test")
          .withUsername("test")
          .withPassword("test");

  @Container
  static final KafkaContainer KAFKA =
      new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

  @Container
  @SuppressWarnings("resource")
  static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

  @Test
  void postgresContainerIsRunning() throws Exception {
    try (Connection conn = POSTGRES.createConnection("");
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT 1")) {
      assertThat(rs.next()).isTrue();
      assertThat(rs.getInt(1)).isEqualTo(1);
    }
  }

  @Test
  void redisContainerIsRunning() {
    assertThat(REDIS.isRunning()).isTrue();
    assertThat(REDIS.getHost()).isNotNull();
    assertThat(REDIS.getMappedPort(6379)).isPositive();
  }

  @Test
  void kafkaContainerIsRunning() {
    assertThat(KAFKA.isRunning()).isTrue();
    assertThat(KAFKA.getBootstrapServers()).isNotEmpty();
  }
}
