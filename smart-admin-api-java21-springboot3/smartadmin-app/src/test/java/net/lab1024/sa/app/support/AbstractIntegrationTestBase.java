package net.lab1024.sa.app.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Abstract base class for integration tests requiring external infrastructure.
 *
 * <p>Provides PostgreSQL 16, Kafka (Confluent 7.6), and Redis 7 containers managed by
 * Testcontainers. All containers are static (shared across test methods within the class) for
 * performance.
 *
 * <p>Usage: extend this class and add {@code @Tag("integration")} to your test class so it runs
 * under the {@code integrationTest} Gradle task.
 *
 * @since 4.1.0
 * @see org.testcontainers.junit.jupiter.Testcontainers
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public abstract class AbstractIntegrationTestBase {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("smart_admin_test")
          .withUsername("test")
          .withPassword("test");

  @Container
  static final KafkaContainer KAFKA =
      new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

  @Container
  @SuppressWarnings("resource")
  static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    // PostgreSQL
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    registry.add("spring.flyway.enabled", () -> false);

    // Kafka (SmartAdmin custom config prefix: smart.kafka.*)
    registry.add("smart.kafka.enabled", () -> true);
    registry.add("smart.kafka.bootstrap-servers", KAFKA::getBootstrapServers);

    // Redis (Spring Data Redis + Redisson + JetCache all read from spring.data.redis.*)
    registry.add("spring.data.redis.host", REDIS::getHost);
    registry.add("spring.data.redis.port", REDIS::getFirstMappedPort);
  }
}
