package net.lab1024.sa.igaming.integration.config;

import io.vavr.control.Option;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.lab1024.sa.common.mq.kafka.batch.BatchSendResult;
import net.lab1024.sa.common.mq.kafka.core.KafkaProducerService;
import net.lab1024.sa.common.mq.kafka.event.DomainEventPublisher;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.support.SendResult;

/**
 * Test configuration for Kafka-related beans.
 *
 * <p>Provides mock implementations of Kafka beans when {@code spring.kafka.enabled=false} in test
 * profile. This allows integration tests to run without a real Kafka broker while still verifying
 * event publishing behavior.
 *
 * <p><b>Provided Mock Beans:</b>
 *
 * <ul>
 *   <li>{@link KafkaProducerService} - Mock producer that returns completed futures without sending
 *       to Kafka
 *   <li>{@link DomainEventPublisher} - Real publisher using the mock producer
 * </ul>
 *
 * <p><b>Usage:</b> This configuration is automatically loaded by {@link
 * IntegrationModuleTestConfig} via {@code @Import(TestKafkaConfig.class)}.
 *
 * @author iGaming Team
 * @since 2026-03-18
 */
@TestConfiguration
public class TestKafkaConfig {

  /**
   * Provides a mock KafkaProducerService for tests.
   *
   * <p>This mock producer:
   *
   * <ul>
   *   <li>Accepts messages without throwing exceptions
   *   <li>Returns completed CompletableFutures with mock SendResult (mimics successful send)
   *   <li>Does NOT send messages to Kafka (integration tests don't need message consumption)
   * </ul>
   *
   * <p><b>Why Mock Instead of Real Kafka?</b>
   *
   * <ul>
   *   <li>Integration tests focus on player journey orchestration, not Kafka message delivery
   *   <li>Faster test execution (no Kafka container startup ~10s saved per test class)
   *   <li>Simpler test environment (fewer moving parts, easier debugging)
   * </ul>
   *
   * @return mock KafkaProducerService instance
   */
  @Bean
  @Primary
  public KafkaProducerService mockKafkaProducerService() {
    return new KafkaProducerService() {
      @Override
      public CompletableFuture<SendResult<String, String>> sendAsync(String topic, String message) {
        return sendAsync(topic, null, message);
      }

      @Override
      public CompletableFuture<SendResult<String, String>> sendAsync(
          String topic, String key, String message) {
        // Create mock RecordMetadata (partition=0, offset=1)
        RecordMetadata metadata = new RecordMetadata(new TopicPartition(topic, 0), 1L, 0, 0L, 0, 0);

        // Create mock SendResult
        SendResult<String, String> sendResult = new SendResult<>(null, metadata);

        return CompletableFuture.completedFuture(sendResult);
      }

      @Override
      public Option<SendResult<String, String>> sendSync(String topic, String message) {
        return sendSync(topic, null, message);
      }

      @Override
      public Option<SendResult<String, String>> sendSync(String topic, String key, String message) {
        RecordMetadata metadata = new RecordMetadata(new TopicPartition(topic, 0), 1L, 0, 0L, 0, 0);
        SendResult<String, String> sendResult = new SendResult<>(null, metadata);
        return Option.of(sendResult);
      }

      @Override
      public Option<SendResult<String, String>> sendSync(
          String topic, String key, String message, long timeoutMs) {
        return sendSync(topic, key, message);
      }

      @Override
      public CompletableFuture<BatchSendResult> sendBatchAsync(
          String topic, List<String> messages) {
        // Mock batch send - return success for all messages
        BatchSendResult result =
            BatchSendResult.builder()
                .total(messages.size())
                .successCount(messages.size())
                .failureCount(0)
                .successResults(List.of())
                .failedMessages(List.of())
                .build();
        return CompletableFuture.completedFuture(result);
      }

      @Override
      public CompletableFuture<BatchSendResult> sendBatchAsyncWithKeys(
          String topic, List<Map.Entry<String, String>> keyedMessages) {
        BatchSendResult result =
            BatchSendResult.builder()
                .total(keyedMessages.size())
                .successCount(keyedMessages.size())
                .failureCount(0)
                .successResults(List.of())
                .failedMessages(List.of())
                .build();
        return CompletableFuture.completedFuture(result);
      }

      @Override
      public BatchSendResult sendBatchSync(String topic, List<String> messages, long timeoutMs) {
        return BatchSendResult.builder()
            .total(messages.size())
            .successCount(messages.size())
            .failureCount(0)
            .successResults(List.of())
            .failedMessages(List.of())
            .build();
      }

      @Override
      public BatchSendResult sendBatchSyncWithKeys(
          String topic, List<Map.Entry<String, String>> keyedMessages, long timeoutMs) {
        return BatchSendResult.builder()
            .total(keyedMessages.size())
            .successCount(keyedMessages.size())
            .failureCount(0)
            .successResults(List.of())
            .failedMessages(List.of())
            .build();
      }
    };
  }

  /**
   * Provides a real DomainEventPublisher for tests using the mock KafkaProducerService.
   *
   * <p>This publisher uses the mock producer above, so events are NOT sent to Kafka but still go
   * through the full DomainEventPublisher logic (tenantId auto-population, traceId injection, JSON
   * serialization).
   *
   * @param kafkaProducerService mock producer injected by Spring
   * @return real DomainEventPublisher instance with mock producer
   */
  @Bean
  @Primary
  public DomainEventPublisher domainEventPublisher(KafkaProducerService kafkaProducerService) {
    return new DomainEventPublisher(kafkaProducerService);
  }
}
