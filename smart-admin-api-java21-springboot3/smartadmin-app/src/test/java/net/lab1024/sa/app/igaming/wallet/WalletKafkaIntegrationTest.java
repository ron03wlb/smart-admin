package net.lab1024.sa.app.igaming.wallet;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.json.util.JsonUtil;
import net.lab1024.sa.common.mq.kafka.config.EventAutoConfiguration;
import net.lab1024.sa.common.mq.kafka.config.KafkaAutoConfiguration;
import net.lab1024.sa.common.mq.kafka.constant.IgamingKafkaConst;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import org.apache.ibatis.annotations.Mapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
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
 * Wallet Kafka integration test — verifies that PlayerEventConsumer correctly freezes wallets when
 * receiving PLAYER_SUSPENDED / PLAYER_SELF_EXCLUDED events via real Kafka.
 *
 * <p>End-to-end chain: Kafka message → PlayerEventConsumer → WalletManager.freezePlayerWallets() →
 * DB state change (wallet.deleted = true).
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Tag("integration")
@SpringBootTest(
    classes = WalletKafkaIntegrationTest.TestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ContextConfiguration(initializers = WalletKafkaIntegrationTest.SchemaInitializer.class)
@DisplayName("Wallet Kafka 整合測試 (PlayerEvent → Wallet Freeze)")
class WalletKafkaIntegrationTest {

  @Configuration
  @EnableKafka
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
    JsonUtil.class,
    net.lab1024.sa.igaming.wallet.consumer.PlayerEventConsumer.class,
  })
  @ComponentScan(
      basePackages = {
        "net.lab1024.sa.igaming.wallet.manager",
      })
  @MapperScan(value = "net.lab1024.sa.igaming.wallet.dao", annotationClass = Mapper.class)
  static class TestConfig {

    @org.springframework.context.annotation.Bean
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
  }

  private static final Long TEST_TENANT_ID = 1L;
  private static final Long PLAYER_ID = 100L;

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
      } catch (SQLException e) {
        throw new RuntimeException("Failed to initialize wallet test schema", e);
      }
    }
  }

  @Autowired private WalletDao walletDao;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private KafkaTemplate<String, String> kafkaTemplate;

  @BeforeEach
  void setUp() {
    TenantContext.setTenantId(TEST_TENANT_ID);
    jdbcTemplate.execute("TRUNCATE t_wallet CASCADE");
    createWalletForPlayer(PLAYER_ID);
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  @DisplayName("PLAYER_SUSPENDED → PlayerEventConsumer → wallet.deleted=true")
  void playerSuspended_freezesWallets() {
    sendPlayerEvent("PLAYER_SUSPENDED", PLAYER_ID);

    await()
        .atMost(15, SECONDS)
        .untilAsserted(
            () -> {
              WalletEntity wallet = walletDao.selectById(getWalletId(PLAYER_ID));
              assertThat(wallet.getDeleted()).isTrue();
            });
  }

  @Test
  @DisplayName("PLAYER_SELF_EXCLUDED → PlayerEventConsumer → wallet.deleted=true")
  void playerSelfExcluded_freezesWallets() {
    sendPlayerEvent("PLAYER_SELF_EXCLUDED", PLAYER_ID);

    await()
        .atMost(15, SECONDS)
        .untilAsserted(
            () -> {
              WalletEntity wallet = walletDao.selectById(getWalletId(PLAYER_ID));
              assertThat(wallet.getDeleted()).isTrue();
            });
  }

  @Test
  @DisplayName("PLAYER_REGISTERED → wallet 不受影響")
  void playerRegistered_ignoredByWalletConsumer() throws InterruptedException {
    sendPlayerEvent("PLAYER_REGISTERED", PLAYER_ID);

    // Wait a moment for potential processing, then verify no change
    Thread.sleep(3000);
    WalletEntity wallet = walletDao.selectById(getWalletId(PLAYER_ID));
    assertThat(wallet.getDeleted()).isFalse();
  }

  @Test
  @DisplayName("無效 JSON → consumer 不 crash，wallet 不受影響")
  void malformedEvent_consumerHandlesGracefully() throws InterruptedException {
    kafkaTemplate
        .send(IgamingKafkaConst.Topic.PLAYER_EVENTS, "bad-key", "NOT_VALID_JSON{{{")
        .join();

    // Wait a moment for potential processing, then verify no crash and no change
    Thread.sleep(3000);
    WalletEntity wallet = walletDao.selectById(getWalletId(PLAYER_ID));
    assertThat(wallet.getDeleted()).isFalse();
  }

  private void sendPlayerEvent(String eventType, Long playerId) {
    ObjectNode payload = JsonNodeFactory.instance.objectNode();
    payload.put("playerId", playerId);

    DomainEvent event =
        DomainEvent.builder()
            .eventType(eventType)
            .aggregateType("Player")
            .aggregateId(String.valueOf(playerId))
            .tenantId(TEST_TENANT_ID)
            .payload(payload)
            .build();

    kafkaTemplate
        .send(IgamingKafkaConst.Topic.PLAYER_EVENTS, event.getAggregateId(), JsonUtil.toJson(event))
        .join();
  }

  private void createWalletForPlayer(Long playerId) {
    WalletEntity wallet = new WalletEntity();
    wallet.setPlayerId(playerId);
    wallet.setTenantId(TEST_TENANT_ID);
    wallet.setWalletType(1); // CASH
    wallet.setCurrencyCode("USD");
    wallet.setBalance(new BigDecimal("1000.0000"));
    wallet.setLockedAmount(BigDecimal.ZERO);
    wallet.setVersion(1);
    wallet.setDeleted(false);
    walletDao.insert(wallet);
  }

  private Long getWalletId(Long playerId) {
    return jdbcTemplate.queryForObject(
        "SELECT wallet_id FROM t_wallet WHERE player_id = ? LIMIT 1", Long.class, playerId);
  }
}
