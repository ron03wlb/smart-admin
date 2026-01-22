# Kafka Batch Testing Guide

本文档提供 SmartAdmin Kafka 批量功能的测试指南，包括单元测试、集成测试和性能测试。

## 目录

- [测试框架和依赖](#测试框架和依赖)
- [单元测试](#单元测试)
- [集成测试（EmbeddedKafka）](#集成测试embeddedkafka)
- [Mock 测试](#mock-测试)
- [性能测试](#性能测试)
- [CI/CD 集成](#cicd-集成)

---

## 测试框架和依赖

### 依赖配置

在 `build.gradle.kts` 中添加测试依赖：

```kotlin
dependencies {
    // Spring Boot Test
    testImplementation(libs.spring.boot.starter.test)

    // Spring Kafka Test (包含 EmbeddedKafka)
    testImplementation("org.springframework.kafka:spring-kafka-test")

    // Awaitility - 异步测试工具
    testImplementation("org.awaitility:awaitility:4.2.0")

    // Lombok for test
    testAnnotationProcessor(libs.lombok)
}
```

### 测试配置文件

创建 `src/test/resources/application-test.yaml`：

```yaml
logging:
  level:
    root: WARN
    net.lab1024.sa.common.mq.kafka: DEBUG
    org.apache.kafka: WARN

spring:
  kafka:
    consumer:
      auto-offset-reset: earliest
      enable-auto-commit: false
    producer:
      acks: all

smart:
  kafka:
    enabled: true
    batch:
      enabled: true
      size: 10
      aggregate:
        enabled: true
        count: 5
        timeout-ms: 500
```

---

## 单元测试

### MessageAggregator 单元测试

```java
package net.lab1024.sa.common.mq.kafka.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("MessageAggregator 单元测试")
class MessageAggregatorTest {

  private static final int TEST_COUNT_THRESHOLD = 5;
  private static final long TEST_TIMEOUT_MS = 500;

  private List<List<String>> batchResults;
  private MessageAggregator<String> aggregator;

  @BeforeEach
  void setUp() {
    batchResults = new CopyOnWriteArrayList<>();
  }

  @AfterEach
  void tearDown() {
    if (aggregator != null) {
      aggregator.shutdown();
    }
  }

  @Nested
  @DisplayName("构造函数验证")
  class ConstructorTests {

    @Test
    @DisplayName("无效参数应抛出异常")
    void testConstructor_invalidParams_throwsException() {
      assertThatThrownBy(() -> new MessageAggregator<>(0, 1000, msgs -> {}))
          .isInstanceOf(IllegalArgumentException.class);

      assertThatThrownBy(() -> new MessageAggregator<>(10, 0, msgs -> {}))
          .isInstanceOf(IllegalArgumentException.class);

      assertThatThrownBy(() -> new MessageAggregator<>(10, 1000, null))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("数量阈值触发")
  class CountThresholdTests {

    @Test
    @DisplayName("达到数量阈值时应触发批量处理")
    void testAggregateByCount_triggersBatchHandle() {
      // Given
      aggregator = createAggregator();

      // When
      for (int i = 0; i < TEST_COUNT_THRESHOLD; i++) {
        aggregator.add("message-" + i);
      }

      // Then
      assertThat(batchResults).hasSize(1);
      assertThat(batchResults.get(0)).hasSize(TEST_COUNT_THRESHOLD);
      assertThat(aggregator.getBufferSize()).isZero();
    }

    @Test
    @DisplayName("未达阈值不应触发批量处理")
    void testAggregateByCount_belowThreshold_noBatchHandle() {
      // Given
      aggregator = createAggregator();

      // When
      for (int i = 0; i < TEST_COUNT_THRESHOLD - 1; i++) {
        aggregator.add("message-" + i);
      }

      // Then
      assertThat(batchResults).isEmpty();
      assertThat(aggregator.getBufferSize()).isEqualTo(TEST_COUNT_THRESHOLD - 1);
    }
  }

  @Nested
  @DisplayName("超时触发")
  class TimeoutTests {

    @Test
    @DisplayName("超时后应触发批量处理")
    void testAggregateByTimeout_triggersBatchHandle() {
      // Given
      aggregator = createAggregator();

      // When
      aggregator.add("message-1");
      aggregator.add("message-2");

      // Then - 等待超时触发
      await()
          .atMost(TEST_TIMEOUT_MS * 3, TimeUnit.MILLISECONDS)
          .untilAsserted(() -> {
            assertThat(batchResults).hasSize(1);
            assertThat(batchResults.get(0)).hasSize(2);
          });
    }
  }

  @Nested
  @DisplayName("并发安全")
  class ConcurrencyTests {

    @Test
    @DisplayName("多线程并发添加应线程安全")
    void testConcurrency_threadSafe() throws InterruptedException {
      // Given
      int threadCount = 10;
      int messagesPerThread = TEST_COUNT_THRESHOLD * 2;
      aggregator = createAggregator();

      // When
      Thread[] threads = new Thread[threadCount];
      for (int t = 0; t < threadCount; t++) {
        int threadId = t;
        threads[t] = new Thread(() -> {
          for (int i = 0; i < messagesPerThread; i++) {
            aggregator.add("thread-" + threadId + "-msg-" + i);
          }
        });
        threads[t].start();
      }

      for (Thread thread : threads) {
        thread.join();
      }

      aggregator.flush();

      // Then
      int totalProcessed = batchResults.stream().mapToInt(List::size).sum();
      assertThat(totalProcessed).isEqualTo(threadCount * messagesPerThread);
    }
  }

  private MessageAggregator<String> createAggregator() {
    return new MessageAggregator<>(
        TEST_COUNT_THRESHOLD,
        TEST_TIMEOUT_MS,
        messages -> batchResults.add(new ArrayList<>(messages))
    );
  }
}
```

### AbstractBatchKafkaListener 单元测试

```java
package net.lab1024.sa.common.mq.kafka.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

@ExtendWith(MockitoExtension.class)
@DisplayName("AbstractBatchKafkaListener 单元测试")
class AbstractBatchKafkaListenerTest {

  @Mock
  private Acknowledgment acknowledgment;

  @Nested
  @DisplayName("批量处理成功")
  class BatchSuccessTests {

    @Test
    @DisplayName("批量处理成功时应调用 doBatchHandle 并确认")
    void testBatchConsume_success() {
      // Given
      TestBatchListener listener = new TestBatchListener();
      List<ConsumerRecord<String, String>> records = createRecords(3);

      // When
      listener.handleBatch(records, acknowledgment);

      // Then
      assertThat(listener.batchHandleCalled).isTrue();
      assertThat(listener.singleHandleCallCount).isZero();
      verify(acknowledgment).acknowledge();
    }
  }

  @Nested
  @DisplayName("降级处理")
  class DegradationTests {

    @Test
    @DisplayName("批量失败时应降级为单条处理")
    void testBatchConsume_batchFails_degradesToSingle() {
      // Given
      TestBatchListener listener = new TestBatchListener();
      listener.batchHandleShouldFail = true;
      List<ConsumerRecord<String, String>> records = createRecords(3);

      // When
      listener.handleBatch(records, acknowledgment);

      // Then
      assertThat(listener.batchHandleCalled).isTrue();
      assertThat(listener.singleHandleCallCount).isEqualTo(3);
      verify(acknowledgment).acknowledge();
    }
  }

  private List<ConsumerRecord<String, String>> createRecords(int count) {
    List<ConsumerRecord<String, String>> records = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      records.add(new ConsumerRecord<>("test-topic", 0, i, "key-" + i, "value-" + i));
    }
    return records;
  }

  static class TestBatchListener extends AbstractBatchKafkaListener<String> {
    boolean batchHandleCalled = false;
    boolean batchHandleShouldFail = false;
    int singleHandleCallCount = 0;

    @Override
    protected void doBatchHandle(List<ConsumerRecord<String, String>> records) throws Exception {
      batchHandleCalled = true;
      if (batchHandleShouldFail) {
        throw new RuntimeException("Batch handle failed");
      }
    }

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) throws Exception {
      singleHandleCallCount++;
    }
  }
}
```

---

## 集成测试（EmbeddedKafka）

### KafkaProducerService 集成测试

```java
package net.lab1024.sa.common.mq.kafka.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import net.lab1024.sa.common.mq.kafka.batch.BatchSendResult;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.*;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
@EmbeddedKafka(
    partitions = 3,
    topics = {"test-batch-topic"},
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:0",
        "port=0"
    })
@DisplayName("KafkaProducerService 集成测试")
class KafkaProducerServiceTest {

  private static final String TEST_TOPIC = "test-batch-topic";

  private KafkaTemplate<String, String> kafkaTemplate;
  private KafkaProducerService producerService;
  private Consumer<String, String> consumer;

  @BeforeEach
  void setUp(EmbeddedKafkaBroker embeddedKafkaBroker) {
    // 创建 Producer
    Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafkaBroker);
    producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

    ProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(producerProps);
    kafkaTemplate = new KafkaTemplate<>(producerFactory);
    producerService = new KafkaProducerServiceImpl(kafkaTemplate);

    // 创建 Consumer
    Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
    consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

    ConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);
    consumer = consumerFactory.createConsumer();
    embeddedKafkaBroker.consumeFromAllEmbeddedTopics(consumer);
  }

  @AfterEach
  void tearDown() {
    if (consumer != null) {
      consumer.close();
    }
  }

  @Test
  @DisplayName("批量异步发送应成功")
  void testSendBatchAsync_success() throws Exception {
    // Given
    List<String> messages = List.of("msg-1", "msg-2", "msg-3");

    // When
    BatchSendResult result = producerService.sendBatchAsync(TEST_TOPIC, messages)
        .get(10, TimeUnit.SECONDS);

    // Then
    assertThat(result.getTotal()).isEqualTo(3);
    assertThat(result.isAllSuccess()).isTrue();
    assertThat(result.getSuccessCount()).isEqualTo(3);
    assertThat(result.getFailureCount()).isZero();
  }

  @Test
  @DisplayName("批量同步发送应成功")
  void testSendBatchSync_success() {
    // Given
    List<String> messages = List.of("sync-1", "sync-2");

    // When
    BatchSendResult result = producerService.sendBatchSync(TEST_TOPIC, messages, 10000);

    // Then
    assertThat(result.isAllSuccess()).isTrue();
    assertThat(result.getTotal()).isEqualTo(2);
  }

  @Test
  @DisplayName("空列表发送应返回空结果")
  void testSendBatchAsync_emptyList() throws Exception {
    // When
    BatchSendResult result = producerService.sendBatchAsync(TEST_TOPIC, List.of())
        .get(5, TimeUnit.SECONDS);

    // Then
    assertThat(result.getTotal()).isZero();
    assertThat(result.isAllSuccess()).isTrue();
  }
}
```

### EmbeddedKafka 配置说明

```java
@EmbeddedKafka(
    // 分区数
    partitions = 3,

    // 预创建的 topic
    topics = {"topic-1", "topic-2"},

    // Broker 配置
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:0",  // 随机端口
        "port=0",
        "auto.create.topics.enable=true"
    },

    // 不使用 Zookeeper（使用 KRaft 模式）
    kraft = false
)
```

---

## Mock 测试

### 使用 Mockito Mock KafkaTemplate

```java
@ExtendWith(MockitoExtension.class)
class KafkaProducerServiceMockTest {

  @Mock
  private KafkaTemplate<String, String> kafkaTemplate;

  @InjectMocks
  private KafkaProducerServiceImpl producerService;

  @Test
  @DisplayName("发送失败时应返回正确的错误结果")
  void testSendAsync_failure() throws Exception {
    // Given
    CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
    failedFuture.completeExceptionally(new RuntimeException("Send failed"));

    when(kafkaTemplate.send(anyString(), any(), anyString()))
        .thenReturn(failedFuture);

    // When
    BatchSendResult result = producerService.sendBatchAsync("topic", List.of("msg"))
        .get(5, TimeUnit.SECONDS);

    // Then
    assertThat(result.isAllFailed()).isTrue();
    assertThat(result.getFailedMessages()).hasSize(1);
    assertThat(result.getFailedMessages().get(0).getErrorMessage()).contains("Send failed");
  }

  @Test
  @DisplayName("部分失败时应正确记录")
  void testSendAsync_partialFailure() throws Exception {
    // Given - 第一条成功，第二条失败
    SendResult<String, String> successResult = mock(SendResult.class);
    RecordMetadata metadata = new RecordMetadata(
        new TopicPartition("topic", 0), 0, 0, 0, 0, 0);
    when(successResult.getRecordMetadata()).thenReturn(metadata);

    CompletableFuture<SendResult<String, String>> successFuture =
        CompletableFuture.completedFuture(successResult);
    CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
    failedFuture.completeExceptionally(new RuntimeException("Failed"));

    when(kafkaTemplate.send(eq("topic"), isNull(), eq("msg-1")))
        .thenReturn(successFuture);
    when(kafkaTemplate.send(eq("topic"), isNull(), eq("msg-2")))
        .thenReturn(failedFuture);

    // When
    BatchSendResult result = producerService.sendBatchAsync("topic", List.of("msg-1", "msg-2"))
        .get(5, TimeUnit.SECONDS);

    // Then
    assertThat(result.isPartialSuccess()).isTrue();
    assertThat(result.getSuccessCount()).isEqualTo(1);
    assertThat(result.getFailureCount()).isEqualTo(1);
  }
}
```

---

## 性能测试

### 吞吐量测试

```java
@SpringBootTest
@EmbeddedKafka(partitions = 3, topics = {"perf-test-topic"})
class KafkaPerformanceTest {

  @Autowired
  private KafkaProducerService producerService;

  @Test
  @DisplayName("批量发送吞吐量测试")
  void testBatchSendThroughput() throws Exception {
    // Given
    int totalMessages = 10000;
    int batchSize = 100;
    List<String> batch = new ArrayList<>();
    for (int i = 0; i < batchSize; i++) {
      batch.add("{\"id\":" + i + ",\"data\":\"test message content\"}");
    }

    // When
    long startTime = System.currentTimeMillis();
    int batches = totalMessages / batchSize;
    int successCount = 0;

    for (int i = 0; i < batches; i++) {
      BatchSendResult result = producerService.sendBatchSync("perf-test-topic", batch, 30000);
      successCount += result.getSuccessCount();
    }

    long elapsed = System.currentTimeMillis() - startTime;

    // Then
    assertThat(successCount).isEqualTo(totalMessages);

    double throughput = totalMessages * 1000.0 / elapsed;
    System.out.printf("Throughput: %.2f messages/second%n", throughput);
    System.out.printf("Total time: %d ms%n", elapsed);

    // 断言：至少 1000 条/秒（可根据环境调整）
    assertThat(throughput).isGreaterThan(1000);
  }

  @Test
  @DisplayName("MessageAggregator 性能测试")
  void testAggregatorPerformance() throws Exception {
    // Given
    int totalMessages = 50000;
    AtomicInteger processedCount = new AtomicInteger(0);
    CountDownLatch latch = new CountDownLatch(1);

    MessageAggregator<String> aggregator = new MessageAggregator<>(
        100, 1000,
        messages -> {
          processedCount.addAndGet(messages.size());
          if (processedCount.get() >= totalMessages) {
            latch.countDown();
          }
        }
    );

    // When
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < totalMessages; i++) {
      aggregator.add("message-" + i);
    }
    aggregator.flush();
    latch.await(30, TimeUnit.SECONDS);
    long elapsed = System.currentTimeMillis() - startTime;

    // Then
    assertThat(processedCount.get()).isEqualTo(totalMessages);

    double throughput = totalMessages * 1000.0 / elapsed;
    System.out.printf("Aggregator throughput: %.2f messages/second%n", throughput);

    aggregator.shutdown();
  }
}
```

---

## CI/CD 集成

### Gradle 测试配置

```kotlin
tasks.test {
  useJUnitPlatform()

  // 并行执行测试
  maxParallelForks = Runtime.getRuntime().availableProcessors()

  // 测试超时
  timeout.set(Duration.ofMinutes(10))

  // 测试报告
  reports {
    junitXml.required.set(true)
    html.required.set(true)
  }

  // 系统属性
  systemProperty("spring.profiles.active", "test")

  // 日志输出
  testLogging {
    events("passed", "skipped", "failed")
    showStandardStreams = false
    exceptionFormat = TestExceptionFormat.FULL
  }
}

// Kafka 集成测试单独配置
tasks.register<Test>("kafkaIntegrationTest") {
  useJUnitPlatform {
    includeTags("kafka-integration")
  }
  shouldRunAfter(tasks.test)
}
```

### GitHub Actions 配置

```yaml
name: Kafka Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Cache Gradle packages
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}

      - name: Run MQ module tests
        run: ./gradlew :sa-base:foundation:mq:test --no-daemon

      - name: Upload test reports
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: test-reports
          path: sa-base/foundation/mq/build/reports/tests/
```

### 测试标签

使用 JUnit 5 标签组织测试：

```java
// 单元测试
@Tag("unit")
class MessageAggregatorTest { }

// 集成测试
@Tag("integration")
@Tag("kafka-integration")
class KafkaProducerServiceTest { }

// 性能测试
@Tag("performance")
class KafkaPerformanceTest { }
```

运行特定标签的测试：

```bash
# 只运行单元测试
./gradlew test -DincludeTags=unit

# 只运行 Kafka 集成测试
./gradlew test -DincludeTags=kafka-integration

# 排除性能测试
./gradlew test -DexcludeTags=performance
```

---

## 测试检查清单

### 单元测试检查项

- [ ] MessageAggregator 构造函数参数验证
- [ ] MessageAggregator 数量阈值触发
- [ ] MessageAggregator 超时触发
- [ ] MessageAggregator 手动 flush
- [ ] MessageAggregator 并发安全
- [ ] MessageAggregator shutdown 行为
- [ ] AbstractBatchKafkaListener 批量处理成功
- [ ] AbstractBatchKafkaListener 降级为单条处理
- [ ] AbstractBatchKafkaListener DLQ 集成
- [ ] BatchSendResult 状态判断方法

### 集成测试检查项

- [ ] KafkaProducerService 单条异步发送
- [ ] KafkaProducerService 单条同步发送
- [ ] KafkaProducerService 批量异步发送
- [ ] KafkaProducerService 批量同步发送
- [ ] KafkaProducerService 带 key 发送
- [ ] KafkaProducerService 空列表/null 处理
- [ ] Consumer 批量消费
- [ ] DLQ 消息发送

### 性能测试检查项

- [ ] 批量发送吞吐量 >= 1000 msg/s
- [ ] MessageAggregator 高并发下无死锁
- [ ] 内存使用稳定（无内存泄漏）

---

## 相关链接

- [快速开始指南](kafka-batch-quickstart.md)
- [API 参考文档](kafka-batch-api-reference.md)
- [示例代码](kafka-batch-examples.md)
