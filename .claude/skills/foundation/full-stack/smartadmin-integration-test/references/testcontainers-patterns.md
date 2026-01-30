# Testcontainers Configuration Patterns

Advanced Testcontainers setup patterns for SmartAdmin integration tests.

## Table of Contents

- [Basic Setup](#basic-setup)
- [PostgreSQL Container](#postgresql-container)
- [Redis Container](#redis-container)
- [Kafka Container](#kafka-container)
- [Container Reuse](#container-reuse)
- [Custom Configuration](#custom-configuration)
- [Troubleshooting](#troubleshooting)

## Basic Setup

SmartAdmin currently uses standard Spring Boot test configuration without explicit Testcontainers. Tests rely on:
1. `@SpringBootTest` - Loads full Spring context
2. `@ActiveProfiles("test")` - Uses test profile configuration
3. `@Transactional` - Auto-rollback after each test

**Current approach (BaseIntegrationTest.java):**
```java
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {
    @Autowired protected DataSource dataSource;
}
```

**When tests run:**
- Spring Boot uses `application-test.yml` or `application-test.properties`
- Database connection configured for test environment
- Each test runs in a transaction that rolls back automatically

## PostgreSQL Container

For explicit Testcontainers setup with PostgreSQL:

### Basic PostgreSQL Setup

```java
@SpringBootTest
@Testcontainers
@Transactional
public abstract class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("smartadmin_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

### With Init Scripts

```java
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
    .withDatabaseName("smartadmin_test")
    .withUsername("test")
    .withPassword("test")
    .withInitScript("sql/init-test-db.sql"); // SQL file in src/test/resources/sql/
```

### With Flyway Migration

```java
@SpringBootTest
@Testcontainers
@Transactional
public abstract class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("smartadmin_test");

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Enable Flyway migration
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }
}
```

## Redis Container

For cache and session testing:

### Basic Redis Setup

```java
@SpringBootTest
@Testcontainers
@Transactional
public abstract class BaseIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    protected CacheManager cacheManager;
}
```

### Redis with Authentication

```java
@Container
static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
    .withExposedPorts(6379)
    .withCommand("redis-server --requirepass testpassword");

@DynamicPropertySource
static void registerRedisProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    registry.add("spring.data.redis.password", () -> "testpassword");
}
```

## Kafka Container

For message queue testing:

### Basic Kafka Setup

```java
@SpringBootTest
@Testcontainers
@Transactional
public abstract class BaseIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );

    @DynamicPropertySource
    static void registerKafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
}
```

### With Consumer Configuration

```java
@DynamicPropertySource
static void registerKafkaProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
    registry.add("spring.kafka.consumer.group-id", () -> "test-group");
}
```

## Container Reuse

Optimize test performance by reusing containers across test classes.

### Singleton Container Pattern

```java
public class TestContainers {

    private static final PostgreSQLContainer<?> POSTGRES;
    private static final GenericContainer<?> REDIS;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("smartadmin_test")
            .withReuse(true);
        POSTGRES.start();

        REDIS = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .withReuse(true);
        REDIS.start();
    }

    public static PostgreSQLContainer<?> getPostgres() {
        return POSTGRES;
    }

    public static GenericContainer<?> getRedis() {
        return REDIS;
    }
}
```

**Usage in tests:**
```java
@SpringBootTest
@Transactional
public abstract class BaseIntegrationTest {

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        PostgreSQLContainer<?> postgres = TestContainers.getPostgres();
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        GenericContainer<?> redis = TestContainers.getRedis();
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }
}
```

## Custom Configuration

### Network Between Containers

When containers need to communicate:

```java
@Container
static Network network = Network.newNetwork();

@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
    .withNetwork(network)
    .withNetworkAliases("postgres");

@Container
static KafkaContainer kafka = new KafkaContainer(
    DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
).withNetwork(network)
 .withNetworkAliases("kafka");
```

### Custom Wait Strategies

```java
@Container
static GenericContainer<?> customApp = new GenericContainer<>("myapp:latest")
    .withExposedPorts(8080)
    .waitingFor(Wait.forHttp("/actuator/health")
        .forStatusCode(200)
        .withStartupTimeout(Duration.ofMinutes(2)));
```

### Environment Variables

```java
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
    .withEnv("POSTGRES_INITDB_ARGS", "--encoding=UTF-8 --lc-collate=C --lc-ctype=C");
```

## Troubleshooting

### Container Startup Timeout

**Problem:** Container fails to start within default timeout.

**Solution:** Increase startup timeout:
```java
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
    .withStartupTimeout(Duration.ofMinutes(5));
```

### Port Conflicts

**Problem:** Port already in use.

**Solution:** Use dynamic ports (Testcontainers default):
```java
// Don't specify fixed ports
GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
    .withExposedPorts(6379); // Testcontainers maps to random host port

// Access with getMappedPort
int redisPort = redis.getMappedPort(6379);
```

### Slow Test Startup

**Problem:** Tests start slowly due to container initialization.

**Solution:** Enable container reuse:
```java
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
    .withReuse(true); // Reuse container across test runs
```

**Note:** Requires `testcontainers.reuse.enable=true` in `~/.testcontainers.properties`

### Database Schema Not Found

**Problem:** Tables don't exist in test database.

**Solution:** Enable Flyway or use init scripts:
```java
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
    .withInitScript("sql/schema.sql");
```

Or configure Flyway:
```java
@DynamicPropertySource
static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.flyway.enabled", () -> "true");
    registry.add("spring.flyway.locations", () -> "classpath:db/migration");
}
```

### Connection Pool Exhaustion

**Problem:** Tests fail with "Connection pool exhausted" errors.

**Solution:** Configure smaller connection pool for tests:
```java
@DynamicPropertySource
static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.hikari.maximum-pool-size", () -> "5");
    registry.add("spring.datasource.hikari.minimum-idle", () -> "2");
}
```

## Docker Desktop Requirements

Testcontainers requires Docker Desktop to be running.

**Verify Docker is running:**
```bash
docker ps
```

**If Docker not running:**
- Start Docker Desktop application
- Wait for "Docker Desktop is running" status
- Run tests again

## Best Practices

1. **Use static containers** - Containers should be `static` fields to share across all test methods
2. **Leverage @DynamicPropertySource** - Configure Spring properties dynamically from container state
3. **Enable reuse for local development** - Set `testcontainers.reuse.enable=true` in `~/.testcontainers.properties`
4. **Use alpine images** - Smaller images start faster (`postgres:15-alpine` vs `postgres:15`)
5. **Clean up resources** - Use `@AfterAll` to stop containers explicitly if not using static containers
6. **Test isolation** - Use `@Transactional` on test class for auto-rollback to keep database clean

## Current SmartAdmin Approach

**As of 2026-01-24:**
- SmartAdmin integration tests **do not** explicitly use Testcontainers
- Tests rely on `@SpringBootTest` with test profile configuration
- Database connection configured via `application-test.yml`
- `@Transactional` on BaseIntegrationTest provides auto-rollback

**When to add Testcontainers:**
- When tests require isolated database instance
- When testing Redis cache behavior explicitly
- When testing Kafka message processing
- When running tests in CI/CD without external DB

**Migration path:**
1. Add Testcontainers dependencies to build.gradle
2. Update BaseIntegrationTest with container configuration
3. Remove external database dependency from test profile
4. Enable container reuse for faster local testing
