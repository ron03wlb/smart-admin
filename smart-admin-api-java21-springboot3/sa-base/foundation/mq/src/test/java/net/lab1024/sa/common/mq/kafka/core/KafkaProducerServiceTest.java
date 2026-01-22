package net.lab1024.sa.common.mq.kafka.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.lab1024.sa.common.mq.kafka.batch.BatchSendResult;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * KafkaProducerService 集成测试
 *
 * <p>使用 EmbeddedKafka 进行集成测试，验证以下功能：
 *
 * <ul>
 *   <li>单条消息异步/同步发送
 *   <li>批量消息异步/同步发送
 *   <li>带 key 的消息发送（保证顺序）
 *   <li>发送结果和错误处理
 * </ul>
 *
 * @author 1024创新实验室
 * @since 2024-01-01
 */
@SpringJUnitConfig
@EmbeddedKafka(
    partitions = 3,
    topics = {
      KafkaProducerServiceTest.TEST_TOPIC,
      KafkaProducerServiceTest.TEST_BATCH_TOPIC,
      KafkaProducerServiceTest.TEST_KEYED_TOPIC
    },
    brokerProperties = {
      "listeners=PLAINTEXT://localhost:0",
      "port=0",
      "auto.create.topics.enable=true"
    })
@DisplayName("KafkaProducerService 集成测试")
class KafkaProducerServiceTest {

  static final String TEST_TOPIC = "test-single-topic";
  static final String TEST_BATCH_TOPIC = "test-batch-topic";
  static final String TEST_KEYED_TOPIC = "test-keyed-topic";

  private EmbeddedKafkaBroker embeddedKafkaBroker;
  private KafkaTemplate<String, String> kafkaTemplate;
  private KafkaProducerService producerService;
  private Consumer<String, String> consumer;

  @BeforeEach
  void setUp(EmbeddedKafkaBroker embeddedKafkaBroker) {
    this.embeddedKafkaBroker = embeddedKafkaBroker;

    // 创建 Producer
    Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafkaBroker);
    producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
    producerProps.put(ProducerConfig.RETRIES_CONFIG, 3);

    ProducerFactory<String, String> producerFactory =
        new DefaultKafkaProducerFactory<>(producerProps);
    kafkaTemplate = new KafkaTemplate<>(producerFactory);

    // 创建 ProducerService
    producerService = new KafkaProducerServiceImpl(kafkaTemplate);

    // 创建 Consumer
    Map<String, Object> consumerProps =
        KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
    consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

    ConsumerFactory<String, String> consumerFactory =
        new DefaultKafkaConsumerFactory<>(consumerProps);
    consumer = consumerFactory.createConsumer();
    embeddedKafkaBroker.consumeFromAllEmbeddedTopics(consumer);
  }

  @AfterEach
  void tearDown() {
    if (consumer != null) {
      consumer.close();
    }
  }

  @Nested
  @DisplayName("单条消息发送测试")
  class SingleMessageTests {

    @Test
    @DisplayName("sendAsync 无 key 应成功发送消息")
    void testSendAsync_noKey_success() throws Exception {
      // Given
      String message = "test-async-message";

      // When
      CompletableFuture<SendResult<String, String>> future =
          producerService.sendAsync(TEST_TOPIC, message);

      // Then
      SendResult<String, String> result = future.get(5, TimeUnit.SECONDS);
      assertThat(result).isNotNull();
      assertThat(result.getRecordMetadata().topic()).isEqualTo(TEST_TOPIC);

      // 验证消息被消费
      ConsumerRecords<String, String> records =
          KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5));
      assertThat(records.count()).isGreaterThanOrEqualTo(1);

      boolean found = false;
      for (ConsumerRecord<String, String> record : records) {
        if (record.topic().equals(TEST_TOPIC) && record.value().equals(message)) {
          found = true;
          break;
        }
      }
      assertThat(found).isTrue();
    }

    @Test
    @DisplayName("sendAsync 带 key 应成功发送消息")
    void testSendAsync_withKey_success() throws Exception {
      // Given
      String key = "test-key";
      String message = "test-async-message-with-key";

      // When
      CompletableFuture<SendResult<String, String>> future =
          producerService.sendAsync(TEST_TOPIC, key, message);

      // Then
      SendResult<String, String> result = future.get(5, TimeUnit.SECONDS);
      assertThat(result).isNotNull();
      assertThat(result.getProducerRecord().key()).isEqualTo(key);
    }

    @Test
    @DisplayName("sendSync 无 key 应成功发送消息")
    void testSendSync_noKey_success() {
      // Given
      String message = "test-sync-message";

      // When
      Optional<SendResult<String, String>> result = producerService.sendSync(TEST_TOPIC, message);

      // Then
      assertThat(result).isPresent();
      assertThat(result.get().getRecordMetadata().topic()).isEqualTo(TEST_TOPIC);
    }

    @Test
    @DisplayName("sendSync 带 key 应成功发送消息")
    void testSendSync_withKey_success() {
      // Given
      String key = "sync-key";
      String message = "test-sync-message-with-key";

      // When
      Optional<SendResult<String, String>> result =
          producerService.sendSync(TEST_TOPIC, key, message);

      // Then
      assertThat(result).isPresent();
      assertThat(result.get().getProducerRecord().key()).isEqualTo(key);
    }

    @Test
    @DisplayName("sendSync 自定义超时应正确工作")
    void testSendSync_customTimeout_success() {
      // Given
      String message = "test-sync-timeout-message";
      long timeoutMs = 5000;

      // When
      Optional<SendResult<String, String>> result =
          producerService.sendSync(TEST_TOPIC, null, message, timeoutMs);

      // Then
      assertThat(result).isPresent();
    }
  }

  @Nested
  @DisplayName("批量消息发送测试")
  class BatchMessageTests {

    @Test
    @DisplayName("sendBatchAsync 无 key 应成功发送所有消息")
    void testSendBatchAsync_noKey_success() throws Exception {
      // Given
      List<String> messages = List.of("batch-msg-1", "batch-msg-2", "batch-msg-3");

      // When
      CompletableFuture<BatchSendResult> future =
          producerService.sendBatchAsync(TEST_BATCH_TOPIC, messages);
      BatchSendResult result = future.get(10, TimeUnit.SECONDS);

      // Then
      assertThat(result.getTotal()).isEqualTo(3);
      assertThat(result.getSuccessCount()).isEqualTo(3);
      assertThat(result.getFailureCount()).isZero();
      assertThat(result.isAllSuccess()).isTrue();
      assertThat(result.isAllFailed()).isFalse();
      assertThat(result.isPartialSuccess()).isFalse();
    }

    @Test
    @DisplayName("sendBatchAsyncWithKeys 带 key 应成功发送所有消息")
    void testSendBatchAsyncWithKeys_success() throws Exception {
      // Given
      List<Map.Entry<String, String>> keyedMessages =
          List.of(
              new AbstractMap.SimpleEntry<>("key-1", "keyed-msg-1"),
              new AbstractMap.SimpleEntry<>("key-2", "keyed-msg-2"),
              new AbstractMap.SimpleEntry<>("key-1", "keyed-msg-3")); // 相同 key

      // When
      CompletableFuture<BatchSendResult> future =
          producerService.sendBatchAsyncWithKeys(TEST_KEYED_TOPIC, keyedMessages);
      BatchSendResult result = future.get(10, TimeUnit.SECONDS);

      // Then
      assertThat(result.getTotal()).isEqualTo(3);
      assertThat(result.isAllSuccess()).isTrue();
      assertThat(result.getSuccessResults()).hasSize(3);
    }

    @Test
    @DisplayName("sendBatchAsync 空列表应返回空结果")
    void testSendBatchAsync_emptyList_success() throws Exception {
      // Given
      List<String> messages = List.of();

      // When
      CompletableFuture<BatchSendResult> future =
          producerService.sendBatchAsync(TEST_BATCH_TOPIC, messages);
      BatchSendResult result = future.get(5, TimeUnit.SECONDS);

      // Then
      assertThat(result.getTotal()).isZero();
      assertThat(result.getSuccessCount()).isZero();
      assertThat(result.getFailureCount()).isZero();
      assertThat(result.isAllSuccess()).isTrue(); // 空列表视为全部成功
    }

    @Test
    @DisplayName("sendBatchSync 无 key 应成功发送所有消息")
    void testSendBatchSync_noKey_success() {
      // Given
      List<String> messages = List.of("sync-batch-1", "sync-batch-2");
      long timeoutMs = 10000;

      // When
      BatchSendResult result = producerService.sendBatchSync(TEST_BATCH_TOPIC, messages, timeoutMs);

      // Then
      assertThat(result.getTotal()).isEqualTo(2);
      assertThat(result.isAllSuccess()).isTrue();
    }

    @Test
    @DisplayName("sendBatchSyncWithKeys 带 key 应成功发送所有消息")
    void testSendBatchSyncWithKeys_success() {
      // Given
      List<Map.Entry<String, String>> keyedMessages =
          List.of(
              new AbstractMap.SimpleEntry<>("sync-key-1", "sync-keyed-msg-1"),
              new AbstractMap.SimpleEntry<>("sync-key-2", "sync-keyed-msg-2"));
      long timeoutMs = 10000;

      // When
      BatchSendResult result =
          producerService.sendBatchSyncWithKeys(TEST_KEYED_TOPIC, keyedMessages, timeoutMs);

      // Then
      assertThat(result.getTotal()).isEqualTo(2);
      assertThat(result.isAllSuccess()).isTrue();
    }

    @Test
    @DisplayName("大批量发送应成功处理")
    void testSendBatchAsync_largeBatch_success() throws Exception {
      // Given
      int batchSize = 100;
      List<String> messages = new ArrayList<>(batchSize);
      for (int i = 0; i < batchSize; i++) {
        messages.add("large-batch-msg-" + i);
      }

      // When
      CompletableFuture<BatchSendResult> future =
          producerService.sendBatchAsync(TEST_BATCH_TOPIC, messages);
      BatchSendResult result = future.get(30, TimeUnit.SECONDS);

      // Then
      assertThat(result.getTotal()).isEqualTo(batchSize);
      assertThat(result.isAllSuccess()).isTrue();
    }
  }

  @Nested
  @DisplayName("消息顺序保证测试")
  class MessageOrderingTests {

    @Test
    @DisplayName("相同 key 的消息应发送到相同分区")
    void testSameKey_samePartition() throws Exception {
      // Given
      String key = "order-key";
      List<Map.Entry<String, String>> keyedMessages = new ArrayList<>();
      for (int i = 0; i < 5; i++) {
        keyedMessages.add(new AbstractMap.SimpleEntry<>(key, "order-msg-" + i));
      }

      // When
      CompletableFuture<BatchSendResult> future =
          producerService.sendBatchAsyncWithKeys(TEST_KEYED_TOPIC, keyedMessages);
      BatchSendResult result = future.get(10, TimeUnit.SECONDS);

      // Then
      assertThat(result.isAllSuccess()).isTrue();

      // 验证所有消息都在同一分区
      int partition = result.getSuccessResults().get(0).getRecordMetadata().partition();
      for (SendResult<String, String> sendResult : result.getSuccessResults()) {
        assertThat(sendResult.getRecordMetadata().partition()).isEqualTo(partition);
      }
    }

    @Test
    @DisplayName("不同 key 的消息可能发送到不同分区")
    void testDifferentKeys_mayUseDifferentPartitions() throws Exception {
      // Given
      List<Map.Entry<String, String>> keyedMessages = new ArrayList<>();
      for (int i = 0; i < 10; i++) {
        keyedMessages.add(new AbstractMap.SimpleEntry<>("unique-key-" + i, "msg-" + i));
      }

      // When
      CompletableFuture<BatchSendResult> future =
          producerService.sendBatchAsyncWithKeys(TEST_KEYED_TOPIC, keyedMessages);
      BatchSendResult result = future.get(10, TimeUnit.SECONDS);

      // Then
      assertThat(result.isAllSuccess()).isTrue();
      // 不强制要求不同分区，但消息应成功发送
      assertThat(result.getSuccessResults()).hasSize(10);
    }
  }

  @Nested
  @DisplayName("BatchSendResult 验证测试")
  class BatchSendResultTests {

    @Test
    @DisplayName("BatchSendResult 应正确计算状态")
    void testBatchSendResult_statusCalculation() throws Exception {
      // Given
      List<String> messages = List.of("msg-1", "msg-2", "msg-3");

      // When
      CompletableFuture<BatchSendResult> future =
          producerService.sendBatchAsync(TEST_BATCH_TOPIC, messages);
      BatchSendResult result = future.get(10, TimeUnit.SECONDS);

      // Then - 全部成功场景
      assertThat(result.isAllSuccess()).isTrue();
      assertThat(result.isAllFailed()).isFalse();
      assertThat(result.isPartialSuccess()).isFalse();
      assertThat(result.getSuccessResults()).hasSize(3);
      assertThat(result.getFailedMessages()).isEmpty();
    }

    @Test
    @DisplayName("BatchSendResult.FailedMessage 应包含完整信息")
    void testFailedMessage_containsCompleteInfo() {
      // Given - 手动构建 FailedMessage 来验证结构
      BatchSendResult.FailedMessage failedMessage =
          BatchSendResult.FailedMessage.builder()
              .index(0)
              .key("test-key")
              .value("test-value")
              .errorMessage("Test error")
              .exception(new RuntimeException("Test exception"))
              .build();

      // Then
      assertThat(failedMessage.getIndex()).isZero();
      assertThat(failedMessage.getKey()).isEqualTo("test-key");
      assertThat(failedMessage.getValue()).isEqualTo("test-value");
      assertThat(failedMessage.getErrorMessage()).isEqualTo("Test error");
      assertThat(failedMessage.getException()).isNotNull();
    }
  }

  @Nested
  @DisplayName("null 和空值处理测试")
  class NullAndEmptyHandlingTests {

    @Test
    @DisplayName("sendBatchAsyncWithKeys null 列表应返回空结果")
    void testSendBatchAsyncWithKeys_nullList_success() throws Exception {
      // When
      CompletableFuture<BatchSendResult> future =
          producerService.sendBatchAsyncWithKeys(TEST_KEYED_TOPIC, null);
      BatchSendResult result = future.get(5, TimeUnit.SECONDS);

      // Then
      assertThat(result.getTotal()).isZero();
      assertThat(result.isAllSuccess()).isTrue();
    }

    @Test
    @DisplayName("sendBatchAsyncWithKeys 空列表应返回空结果")
    void testSendBatchAsyncWithKeys_emptyList_success() throws Exception {
      // When
      CompletableFuture<BatchSendResult> future =
          producerService.sendBatchAsyncWithKeys(TEST_KEYED_TOPIC, List.of());
      BatchSendResult result = future.get(5, TimeUnit.SECONDS);

      // Then
      assertThat(result.getTotal()).isZero();
      assertThat(result.isAllSuccess()).isTrue();
    }

    @Test
    @DisplayName("带 null key 的消息应成功发送")
    void testSendBatchAsyncWithKeys_nullKey_success() throws Exception {
      // Given
      List<Map.Entry<String, String>> keyedMessages =
          List.of(new AbstractMap.SimpleEntry<>(null, "msg-with-null-key"));

      // When
      CompletableFuture<BatchSendResult> future =
          producerService.sendBatchAsyncWithKeys(TEST_KEYED_TOPIC, keyedMessages);
      BatchSendResult result = future.get(10, TimeUnit.SECONDS);

      // Then
      assertThat(result.isAllSuccess()).isTrue();
    }
  }
}
