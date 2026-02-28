package net.lab1024.sa.app.igaming.wallet;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import net.lab1024.sa.common.mq.kafka.config.EventAutoConfiguration;
import net.lab1024.sa.common.mq.kafka.config.KafkaAutoConfiguration;
import net.lab1024.sa.common.mq.kafka.constant.IgamingKafkaConst;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.common.mq.kafka.event.DomainEventPublisher;
import net.lab1024.sa.igaming.wallet.consumer.WalletAuditConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * WalletAuditConsumer Kafka integration test — verifies that wallet events are received and
 * correctly logged as audit entries.
 *
 * <p>Uses a Kafka-only sliced context (no PostgreSQL) since WalletAuditConsumer only logs events
 * without database interaction. Log output is captured via Logback ListAppender.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Tag("integration")
@SpringBootTest(
    classes = WalletAuditKafkaIntegrationTest.TestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@DisplayName("WalletAuditConsumer Kafka 整合測試")
class WalletAuditKafkaIntegrationTest {

  @Configuration
  @EnableKafka
  @ImportAutoConfiguration({
    org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration.class,
  })
  @Import({
    KafkaAutoConfiguration.class,
    EventAutoConfiguration.class,
    net.lab1024.sa.common.json.util.JsonUtil.class,
    WalletAuditConsumer.class,
  })
  static class TestConfig {}

  @Container
  static final KafkaContainer KAFKA =
      new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("smart.kafka.enabled", () -> true);
    registry.add("smart.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    registry.add("smart.kafka.dead-letter-queue.enabled", () -> false);
    registry.add("smart.kafka.listener.ack-mode", () -> "RECORD");
  }

  @Autowired private DomainEventPublisher domainEventPublisher;

  private ListAppender<ILoggingEvent> listAppender;

  @BeforeEach
  void setUp() {
    Logger logger = (Logger) LoggerFactory.getLogger(WalletAuditConsumer.class);
    listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);
  }

  @AfterEach
  void tearDown() {
    Logger logger = (Logger) LoggerFactory.getLogger(WalletAuditConsumer.class);
    logger.detachAppender(listAppender);
    listAppender.stop();
  }

  @Test
  @DisplayName("wallet 事件 → [AUDIT] 日誌包含 eventType, aggregateId, tenantId")
  void walletEvent_auditLogged() {
    DomainEvent event =
        DomainEvent.builder()
            .eventType("WALLET_CREDITED")
            .aggregateType("Wallet")
            .aggregateId("wallet-100")
            .tenantId(1L)
            .build();

    domainEventPublisher.publish(IgamingKafkaConst.Topic.WALLET_EVENTS, event).join();

    await()
        .atMost(15, SECONDS)
        .untilAsserted(
            () ->
                assertThat(listAppender.list)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .anyMatch(
                        msg ->
                            msg.contains("[AUDIT]")
                                && msg.contains("WALLET_CREDITED")
                                && msg.contains("wallet-100")
                                && msg.contains("tenantId=1")));
  }

  @Test
  @DisplayName("wallet 事件 → [AUDIT] 日誌包含 traceId")
  void walletEvent_containsTraceId() {
    DomainEvent event =
        DomainEvent.builder()
            .eventType("WALLET_DEBITED")
            .aggregateType("Wallet")
            .aggregateId("wallet-200")
            .tenantId(2L)
            .traceId("trace-abc-123")
            .build();

    domainEventPublisher.publish(IgamingKafkaConst.Topic.WALLET_EVENTS, event).join();

    await()
        .atMost(15, SECONDS)
        .untilAsserted(
            () ->
                assertThat(listAppender.list)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .anyMatch(
                        msg ->
                            msg.contains("[AUDIT]")
                                && msg.contains("WALLET_DEBITED")
                                && msg.contains("trace-abc-123")));
  }
}
