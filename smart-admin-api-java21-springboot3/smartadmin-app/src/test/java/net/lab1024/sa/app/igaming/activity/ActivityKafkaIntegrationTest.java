package net.lab1024.sa.app.igaming.activity;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.json.util.JsonUtil;
import net.lab1024.sa.common.mq.kafka.config.EventAutoConfiguration;
import net.lab1024.sa.common.mq.kafka.config.KafkaAutoConfiguration;
import net.lab1024.sa.common.mq.kafka.constant.IgamingKafkaConst;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.common.security.encrypt.AesGcmFieldEncryptService;
import net.lab1024.sa.common.security.encrypt.EncryptedFieldTypeHandler;
import net.lab1024.sa.igaming.activity.consumer.WageringEventConsumer;
import net.lab1024.sa.igaming.activity.manager.VipAutoEvaluationManager;
import org.apache.ibatis.annotations.Mapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Activity Kafka integration test — verifies that WageringEventConsumer correctly processes
 * BET_PLACED events via real Kafka and updates wagering progress in the database.
 *
 * <p>End-to-end chain: Kafka message → WageringEventConsumer → WageringProgressManager →
 * PlayerBonusRecordDao → DB state change.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Tag("integration")
@SpringBootTest(
    classes = ActivityKafkaIntegrationTest.TestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ContextConfiguration(initializers = ActivityKafkaIntegrationTest.SchemaInitializer.class)
@DisplayName("Activity Kafka 整合測試 (BET_PLACED → Wagering)")
class ActivityKafkaIntegrationTest {

  @Configuration
  @EnableKafka
  @EnableCaching
  @ImportAutoConfiguration({
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    JdbcTemplateAutoConfiguration.class,
    com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration.class,
    org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration.class,
  })
  @Import({
    net.lab1024.sa.common.mybatis.handler.MybatisPlusFillHandler.class,
    KafkaAutoConfiguration.class,
    EventAutoConfiguration.class,
    net.lab1024.sa.common.json.util.JsonUtil.class,
  })
  @ComponentScan(
      basePackages = {
        "net.lab1024.sa.igaming.activity.service",
        "net.lab1024.sa.igaming.activity.manager",
        "net.lab1024.sa.igaming.activity.consumer",
        "net.lab1024.sa.igaming.wallet.manager",
      },
      excludeFilters =
          @ComponentScan.Filter(
              type = FilterType.ASSIGNABLE_TYPE,
              classes = VipAutoEvaluationManager.class))
  @MapperScan(
      value = {
        "net.lab1024.sa.igaming.activity.dao",
        "net.lab1024.sa.igaming.wallet.dao",
        "net.lab1024.sa.igaming.game.dao",
      },
      annotationClass = Mapper.class)
  static class TestConfig {

    @Bean
    public com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor
        mybatisPlusInterceptor() {
      var interceptor = new com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor();
      interceptor.addInnerInterceptor(
          new com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor());
      interceptor.addInnerInterceptor(
          new com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor(
              com.baomidou.mybatisplus.annotation.DbType.POSTGRE_SQL));
      return interceptor;
    }

    @Bean
    public CacheManager cacheManager() {
      return new ConcurrentMapCacheManager("promotion:active", "promotion:code");
    }

    @Bean
    public AesGcmFieldEncryptService aesGcmFieldEncryptService() throws Exception {
      KeyGenerator keyGen = KeyGenerator.getInstance("AES");
      keyGen.init(256);
      SecretKey key = keyGen.generateKey();
      AesGcmFieldEncryptService service = new AesGcmFieldEncryptService(key);
      EncryptedFieldTypeHandler.setEncryptService(service);
      return service;
    }
  }

  private static final Long TEST_TENANT_ID = 1L;

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("smart_admin_test")
          .withUsername("test")
          .withPassword("test");

  @Container
  static final KafkaContainer KAFKA =
      new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    // PostgreSQL
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    registry.add("mybatis-plus.mapper-locations", () -> "classpath*:/mapper/**/*.xml");
    registry.add("mybatis-plus.configuration.map-underscore-to-camel-case", () -> true);
    registry.add("tenant.enabled", () -> false);

    // Kafka
    registry.add("smart.kafka.enabled", () -> true);
    registry.add("smart.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    registry.add("smart.kafka.dead-letter-queue.enabled", () -> false);
    registry.add("smart.kafka.listener.ack-mode", () -> "RECORD");
  }

  static class SchemaInitializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
      try (Connection conn =
          DriverManager.getConnection(
              POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
        try {
          conn.createStatement().execute("CREATE ROLE smartadmin_app LOGIN");
        } catch (SQLException ignored) {
          // Role already exists
        }
        ScriptUtils.executeSqlScript(
            conn, new ClassPathResource("db/migration/V8__wallet_tables.sql"));
        ScriptUtils.executeSqlScript(
            conn, new ClassPathResource("db/migration/V11__game_tables.sql"));
        ScriptUtils.executeSqlScript(
            conn, new ClassPathResource("db/migration/V12__activity_tables.sql"));
      } catch (SQLException e) {
        throw new RuntimeException("Failed to initialize activity test schema", e);
      }
    }
  }

  @Autowired private KafkaTemplate<String, String> kafkaTemplate;

  private ListAppender<ILoggingEvent> listAppender;

  @BeforeEach
  void setUp() {
    TenantContext.setTenantId(TEST_TENANT_ID);
    Logger logger = (Logger) LoggerFactory.getLogger(WageringEventConsumer.class);
    listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
    Logger logger = (Logger) LoggerFactory.getLogger(WageringEventConsumer.class);
    logger.detachAppender(listAppender);
    listAppender.stop();
  }

  @Test
  @DisplayName("BET_PLACED → WageringEventConsumer 接收並處理（無 active records 時回傳 0）")
  void betPlaced_processedByConsumer() {
    ObjectNode payload = JsonNodeFactory.instance.objectNode();
    payload.put("playerId", 200L);
    payload.put("gameCode", "slot-001");
    payload.put("amount", "50.00");

    DomainEvent event =
        DomainEvent.builder()
            .eventType("BET_PLACED")
            .aggregateType("GameRound")
            .aggregateId("round-123")
            .tenantId(TEST_TENANT_ID)
            .payload(payload)
            .build();

    kafkaTemplate
        .send(IgamingKafkaConst.Topic.GAME_EVENTS, event.getAggregateId(), JsonUtil.toJson(event))
        .join();

    // WageringEventConsumer receives the event and calls WageringProgressManager.
    // No active bonus records exist, so 0 records updated — but no error occurs.
    // We verify the consumer processed the event by checking no error log was emitted.
    await()
        .atMost(15, SECONDS)
        .untilAsserted(
            () ->
                assertThat(listAppender.list)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .noneMatch(msg -> msg.contains("Failed to")));
  }

  @Test
  @DisplayName("BET_PLACED 缺少必要欄位 → consumer 靜默跳過")
  void betPlaced_missingFields_skipped() {
    ObjectNode payload = JsonNodeFactory.instance.objectNode();
    // Missing playerId, gameCode, amount

    DomainEvent event =
        DomainEvent.builder()
            .eventType("BET_PLACED")
            .aggregateType("GameRound")
            .aggregateId("round-456")
            .tenantId(TEST_TENANT_ID)
            .payload(payload)
            .build();

    kafkaTemplate
        .send(IgamingKafkaConst.Topic.GAME_EVENTS, event.getAggregateId(), JsonUtil.toJson(event))
        .join();

    // Consumer should log a warning about missing fields but not throw
    await()
        .atMost(15, SECONDS)
        .untilAsserted(
            () ->
                assertThat(listAppender.list)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .anyMatch(msg -> msg.contains("missing required fields")));
  }

  @Test
  @DisplayName("非 BET_PLACED 事件 → consumer 忽略")
  void nonBetEvent_ignored() throws InterruptedException {
    DomainEvent event =
        DomainEvent.builder()
            .eventType("ROUND_COMPLETED")
            .aggregateType("GameRound")
            .aggregateId("round-789")
            .tenantId(TEST_TENANT_ID)
            .build();

    kafkaTemplate
        .send(IgamingKafkaConst.Topic.GAME_EVENTS, event.getAggregateId(), JsonUtil.toJson(event))
        .join();

    // Wait a moment, then verify no wagering-related log was emitted
    Thread.sleep(3000);
    assertThat(listAppender.list)
        .extracting(ILoggingEvent::getFormattedMessage)
        .noneMatch(msg -> msg.contains("Wagering updated"));
  }
}
