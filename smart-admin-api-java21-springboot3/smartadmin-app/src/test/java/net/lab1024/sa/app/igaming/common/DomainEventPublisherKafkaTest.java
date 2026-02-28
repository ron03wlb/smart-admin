package net.lab1024.sa.app.igaming.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.json.util.JsonUtil;
import net.lab1024.sa.common.mq.kafka.config.EventAutoConfiguration;
import net.lab1024.sa.common.mq.kafka.config.KafkaAutoConfiguration;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.common.mq.kafka.event.DomainEventPublisher;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * DomainEventPublisher Kafka integration test — verifies that events are correctly published to
 * Kafka topics via real Testcontainers Kafka broker.
 *
 * <p>Uses a minimal sliced context (Kafka only, no PostgreSQL) to validate the publishing
 * infrastructure: topic routing, partition key, tenantId auto-population, and sync delivery.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Tag("integration")
@SpringBootTest(
    classes = DomainEventPublisherKafkaTest.TestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@DisplayName("DomainEventPublisher Kafka 整合測試")
class DomainEventPublisherKafkaTest {

  @Configuration
  @ImportAutoConfiguration({JacksonAutoConfiguration.class})
  @Import({KafkaAutoConfiguration.class, EventAutoConfiguration.class, JsonUtil.class})
  static class TestConfig {}

  @Container
  static final KafkaContainer KAFKA =
      new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("smart.kafka.enabled", () -> true);
    registry.add("smart.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    registry.add("smart.kafka.dead-letter-queue.enabled", () -> false);
  }

  @Autowired private DomainEventPublisher domainEventPublisher;

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  @DisplayName("publish → 訊息送達正確 topic")
  void publish_sendsToCorrectTopic() {
    String topic = "test.publisher.events";
    DomainEvent event =
        DomainEvent.builder()
            .eventType("TEST_EVENT")
            .aggregateType("Test")
            .aggregateId("agg-123")
            .build();

    domainEventPublisher.publish(topic, event).join();

    List<ConsumerRecord<String, String>> records = pollRecords(topic, 1);
    assertThat(records).hasSize(1);
    DomainEvent received = JsonUtil.fromJson(records.get(0).value(), DomainEvent.class);
    assertThat(received.getEventType()).isEqualTo("TEST_EVENT");
    assertThat(received.getAggregateType()).isEqualTo("Test");
    assertThat(received.getAggregateId()).isEqualTo("agg-123");
    assertThat(received.getEventId()).isNotBlank();
    assertThat(received.getTimestamp()).isNotNull();
  }

  @Test
  @DisplayName("publish → aggregateId 作為 Kafka partition key")
  void publish_aggregateIdIsPartitionKey() {
    String topic = "test.key.events";
    DomainEvent event =
        DomainEvent.builder()
            .eventType("KEY_TEST")
            .aggregateType("Wallet")
            .aggregateId("wallet-456")
            .build();

    domainEventPublisher.publish(topic, event).join();

    List<ConsumerRecord<String, String>> records = pollRecords(topic, 1);
    assertThat(records).hasSize(1);
    assertThat(records.get(0).key()).isEqualTo("wallet-456");
  }

  @Test
  @DisplayName("publish → 自動填充 TenantContext tenantId")
  void publish_autoPopulatesTenantId() {
    String topic = "test.tenant.events";
    TenantContext.setTenantId(42L);

    DomainEvent event =
        DomainEvent.builder()
            .eventType("TENANT_TEST")
            .aggregateType("Player")
            .aggregateId("player-789")
            .build();

    domainEventPublisher.publish(topic, event).join();

    List<ConsumerRecord<String, String>> records = pollRecords(topic, 1);
    assertThat(records).hasSize(1);
    DomainEvent received = JsonUtil.fromJson(records.get(0).value(), DomainEvent.class);
    assertThat(received.getTenantId()).isEqualTo(42L);
  }

  @Test
  @DisplayName("publishSync → 同步發布成功回傳 true")
  void publishSync_returnsTrue() {
    String topic = "test.sync.events";
    DomainEvent event =
        DomainEvent.builder()
            .eventType("SYNC_TEST")
            .aggregateType("Risk")
            .aggregateId("risk-001")
            .build();

    boolean result = domainEventPublisher.publishSync(topic, event);

    assertThat(result).isTrue();

    List<ConsumerRecord<String, String>> records = pollRecords(topic, 1);
    assertThat(records).hasSize(1);
    DomainEvent received = JsonUtil.fromJson(records.get(0).value(), DomainEvent.class);
    assertThat(received.getEventType()).isEqualTo("SYNC_TEST");
  }

  /**
   * Polls records from a Kafka topic using a raw consumer.
   *
   * @param topic topic to consume from
   * @param expectedCount minimum number of records to wait for
   * @return collected records
   */
  private List<ConsumerRecord<String, String>> pollRecords(String topic, int expectedCount) {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-verify-" + UUID.randomUUID());
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

    try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
      consumer.subscribe(Collections.singletonList(topic));
      List<ConsumerRecord<String, String>> allRecords = new ArrayList<>();
      long deadline = System.currentTimeMillis() + 10_000;
      while (allRecords.size() < expectedCount && System.currentTimeMillis() < deadline) {
        ConsumerRecords<String, String> batch = consumer.poll(Duration.ofMillis(500));
        batch.forEach(allRecords::add);
      }
      return allRecords;
    }
  }
}
