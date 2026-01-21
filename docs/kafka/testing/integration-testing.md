# Integration Testing

Complete guide to integration testing with embedded and real Kafka brokers.

## Overview

Integration testing validates Kafka components working together with actual message brokers, ensuring:
- Producers and consumers interact correctly
- Message serialization/deserialization works end-to-end
- Error handling and DLQ routing functions properly
- Configuration settings apply correctly
- Performance meets requirements under realistic conditions

**Testing approaches**:
1. **EmbeddedKafka** - In-memory Kafka broker (fast, isolated)
2. **Testcontainers** - Dockerized Kafka (production-like)
3. **Dedicated Test Cluster** - Real Kafka cluster (staging environment)

**Use cases**:
- Validate message flow from producer to consumer
- Test partition assignment and rebalancing
- Verify DLQ routing and error handling
- Performance and load testing
- Chaos engineering scenarios

---

## Testing Pyramid for Kafka Integration

```
        ┌─────────────┐
        │ E2E Tests   │  ← 5% (Full stack, slow)
        └─────────────┘
      ┌─────────────────┐
      │Integration Tests│  ← 20% (With real Kafka)
      └─────────────────┘
    ┌───────────────────────┐
    │   Unit Tests          │  ← 75% (Fast, isolated)
    └───────────────────────┘
```

**Integration test characteristics**:
- Run against actual Kafka broker (embedded or containerized)
- Test message serialization, network communication, partition assignment
- Slower than unit tests (seconds vs milliseconds)
- More comprehensive coverage of integration scenarios

---

## Approach 1: EmbeddedKafka (Recommended)

### Setup with Spring Boot Test

**Dependency** (`build.gradle`):
```gradle
dependencies {
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.kafka:spring-kafka-test'
}
```

**Test configuration**:
```java
package net.lab1024.sa.admin.module.business.sample;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(
    partitions = 3,
    topics = {"smart-admin-test", "smart-admin-test-dlq"},
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092",
        "log.dir=/tmp/embedded-kafka"
    }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class KafkaIntegrationTest {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private TestMessageCollector messageCollector;

    @Test
    void testProducerAndConsumer() {
        // Given
        String topic = "smart-admin-test";
        String key = "test-key";
        String message = "Hello Kafka!";

        // When
        kafkaProducerService.send(topic, key, message);

        // Then
        await()
            .atMost(5, TimeUnit.SECONDS)
            .until(() -> messageCollector.getReceivedMessages().size() == 1);

        assertThat(messageCollector.getReceivedMessages())
            .hasSize(1)
            .containsExactly(message);
    }
}
```

### Test Message Collector

Create a component to capture consumed messages for verification:

```java
package net.lab1024.sa.admin.module.business.sample;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.mq.kafka.listener.AbstractKafkaListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@TestComponent
@Slf4j
public class TestMessageCollector extends AbstractKafkaListener {

    @Getter
    private final List<String> receivedMessages = new CopyOnWriteArrayList<>();

    @KafkaListener(
        topics = "smart-admin-test",
        groupId = "test-consumer-group",
        autoStartup = "true"
    )
    public void onMessage(ConsumerRecord<String, String> record) {
        handleMessage(record);
    }

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        log.info("Test collector received: {}", record.value());
        receivedMessages.add(record.value());
    }

    public void clear() {
        receivedMessages.clear();
    }
}
```

---

## Approach 2: Testcontainers (Production-like)

### Setup with Testcontainers

**Dependency** (`build.gradle`):
```gradle
dependencies {
    testImplementation 'org.testcontainers:testcontainers:1.19.0'
    testImplementation 'org.testcontainers:kafka:1.19.0'
    testImplementation 'org.testcontainers:junit-jupiter:1.19.0'
}
```

**Base test class**:
```java
package net.lab1024.sa.admin.module.business.sample;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
public abstract class AbstractKafkaContainerTest {

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    )
        .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
        .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
        .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
        .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("smart.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @BeforeAll
    static void setup() {
        kafka.start();
    }

    @AfterAll
    static void teardown() {
        kafka.stop();
    }
}
```

**Test implementation**:
```java
package net.lab1024.sa.admin.module.business.sample;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

class KafkaContainerIntegrationTest extends AbstractKafkaContainerTest {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private TestMessageCollector messageCollector;

    @Test
    void testEndToEndMessageFlow() {
        // Given
        String topic = "smart-admin-container-test";
        String message = "Test message from Testcontainers";

        // When
        kafkaProducerService.send(topic, "key", message);

        // Then
        await()
            .atMost(10, TimeUnit.SECONDS)
            .until(() -> messageCollector.getReceivedMessages().contains(message));

        assertThat(messageCollector.getReceivedMessages())
            .contains(message);
    }
}
```

---

## Integration Test Patterns

### Pattern 1: End-to-End Message Flow

**Test scenario**: Send message → Consumer processes → Verify side effects

```java
@SpringBootTest
@EmbeddedKafka(topics = "smart-admin-order")
class OrderProcessingIntegrationTest {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDao orderDao;

    @Test
    void testOrderCreationFlow() {
        // Given
        OrderDTO order = OrderDTO.builder()
            .orderId("ORD-TEST-001")
            .customerId("CUST-123")
            .amount(new BigDecimal("199.99"))
            .status("PENDING")
            .build();

        // When
        String orderJson = JSON.toJSONString(order);
        kafkaProducerService.send("smart-admin-order", order.getOrderId(), orderJson);

        // Then: Wait for consumer to process
        await()
            .atMost(5, TimeUnit.SECONDS)
            .until(() -> orderDao.selectById(order.getOrderId()) != null);

        // Verify database state
        OrderEntity savedOrder = orderDao.selectById(order.getOrderId());
        assertThat(savedOrder).isNotNull();
        assertThat(savedOrder.getStatus()).isEqualTo("CONFIRMED");
    }
}
```

### Pattern 2: Batch Processing

**Test scenario**: Send multiple messages → Verify batch processing

```java
@SpringBootTest
@EmbeddedKafka(topics = "smart-admin-employee-import")
class EmployeeBatchImportTest {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private EmployeeDao employeeDao;

    @Test
    void testBatchImport() {
        // Given: Generate 100 test employees
        List<EmployeeImportDTO> employees = IntStream.range(0, 100)
            .mapToObj(i -> EmployeeImportDTO.builder()
                .employeeNo("EMP-" + i)
                .name("Employee " + i)
                .build())
            .toList();

        // When: Send batch
        List<CompletableFuture<SendResult<String, String>>> futures = employees.stream()
            .map(emp -> {
                String json = JSON.toJSONString(emp);
                return kafkaProducerService.sendAsync("smart-admin-employee-import", emp.getEmployeeNo(), json);
            })
            .toList();

        // Wait for all sends to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Then: Verify all imported
        await()
            .atMost(10, TimeUnit.SECONDS)
            .until(() -> employeeDao.selectCount(null) >= 100);

        assertThat(employeeDao.selectCount(null)).isGreaterThanOrEqualTo(100);
    }
}
```

### Pattern 3: DLQ Routing

**Test scenario**: Send invalid message → Verify DLQ routing

```java
@SpringBootTest
@EmbeddedKafka(topics = {"smart-admin-order", "smart-admin-order-dlq"})
class DLQRoutingIntegrationTest {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private DLQMessageCollector dlqCollector;

    @Test
    void testInvalidMessageRoutedToDLQ() {
        // Given: Invalid order (negative amount)
        OrderDTO invalidOrder = OrderDTO.builder()
            .orderId("ORD-INVALID")
            .amount(new BigDecimal("-99.99"))  // Invalid!
            .build();

        // When
        String orderJson = JSON.toJSONString(invalidOrder);
        kafkaProducerService.send("smart-admin-order", invalidOrder.getOrderId(), orderJson);

        // Then: Message should be in DLQ
        await()
            .atMost(5, TimeUnit.SECONDS)
            .until(() -> dlqCollector.getReceivedMessages().size() == 1);

        DeadLetterMessage dlqMessage = dlqCollector.getReceivedMessages().get(0);
        assertThat(dlqMessage.getOriginalTopic()).isEqualTo("smart-admin-order");
        assertThat(dlqMessage.getErrorMessage()).contains("Invalid amount");
    }
}
```

### Pattern 4: Consumer Rebalancing

**Test scenario**: Add consumer → Trigger rebalance → Verify no message loss

```java
@SpringBootTest
@EmbeddedKafka(partitions = 3, topics = "smart-admin-rebalance-test")
class ConsumerRebalancingTest {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private MessageCounter messageCounter;

    @Autowired
    private KafkaListenerEndpointRegistry endpointRegistry;

    @Test
    void testRebalancingWithNoMessageLoss() {
        // Given: Send 1000 messages
        int messageCount = 1000;
        for (int i = 0; i < messageCount; i++) {
            kafkaProducerService.send("smart-admin-rebalance-test", "key-" + i, "msg-" + i);
        }

        // When: Start additional consumer (triggers rebalance)
        MessageListenerContainer container = endpointRegistry.getListenerContainer("additional-consumer");
        container.start();

        // Wait for rebalancing to complete
        await()
            .atMost(10, TimeUnit.SECONDS)
            .until(() -> container.isRunning());

        // Then: All messages should be consumed
        await()
            .atMost(30, TimeUnit.SECONDS)
            .until(() -> messageCounter.getCount() == messageCount);

        assertThat(messageCounter.getCount()).isEqualTo(messageCount);
    }
}
```

### Pattern 5: Transactional Processing

**Test scenario**: Transactional send → Verify atomicity

```java
@SpringBootTest
@EmbeddedKafka(topics = {"smart-admin-order", "smart-admin-payment"})
class TransactionalProcessingTest {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private KafkaTemplate<String, String> transactionalTemplate;

    @Autowired
    private TestMessageCollector orderCollector;

    @Autowired
    private TestMessageCollector paymentCollector;

    @Test
    void testTransactionalSendCommit() {
        // Given
        String orderId = "ORD-TX-001";

        // When: Transactional send
        transactionalTemplate.executeInTransaction(operations -> {
            operations.send("smart-admin-order", orderId, "order-created");
            operations.send("smart-admin-payment", orderId, "payment-requested");
            return true;
        });

        // Then: Both messages should be received
        await()
            .atMost(5, TimeUnit.SECONDS)
            .until(() -> orderCollector.getReceivedMessages().size() == 1
                && paymentCollector.getReceivedMessages().size() == 1);

        assertThat(orderCollector.getReceivedMessages()).hasSize(1);
        assertThat(paymentCollector.getReceivedMessages()).hasSize(1);
    }

    @Test
    void testTransactionalSendRollback() {
        // When: Transaction aborted
        try {
            transactionalTemplate.executeInTransaction(operations -> {
                operations.send("smart-admin-order", "ORD-TX-002", "order-created");
                throw new RuntimeException("Simulated error");
            });
        } catch (RuntimeException ignored) {
        }

        // Then: No messages should be received
        await()
            .during(3, TimeUnit.SECONDS)
            .atMost(5, TimeUnit.SECONDS)
            .until(() -> orderCollector.getReceivedMessages().isEmpty());

        assertThat(orderCollector.getReceivedMessages()).isEmpty();
    }
}
```

---

## Performance and Load Testing

### Load Test Configuration

```java
@SpringBootTest
@EmbeddedKafka(partitions = 6, topics = "smart-admin-load-test")
class KafkaLoadTest {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private MessageCounter messageCounter;

    @Test
    void testProducerThroughput() {
        // Given
        int messageCount = 10000;
        long startTime = System.currentTimeMillis();

        // When: Send 10,000 messages
        for (int i = 0; i < messageCount; i++) {
            kafkaProducerService.send("smart-admin-load-test", "key-" + i, "message-" + i);
        }

        long sendDuration = System.currentTimeMillis() - startTime;
        double throughput = (messageCount * 1000.0) / sendDuration;

        // Then
        assertThat(throughput).isGreaterThan(5000);  // > 5,000 msg/sec
        log.info("Producer throughput: {} msg/sec", String.format("%.2f", throughput));
    }

    @Test
    void testConsumerThroughput() {
        // Given: 10,000 messages already sent
        int messageCount = 10000;
        for (int i = 0; i < messageCount; i++) {
            kafkaProducerService.send("smart-admin-load-test", "key-" + i, "message-" + i);
        }

        // When: Measure consumption time
        long startTime = System.currentTimeMillis();
        await()
            .atMost(30, TimeUnit.SECONDS)
            .until(() -> messageCounter.getCount() >= messageCount);
        long consumeDuration = System.currentTimeMillis() - startTime;

        // Then
        double throughput = (messageCount * 1000.0) / consumeDuration;
        assertThat(throughput).isGreaterThan(5000);  // > 5,000 msg/sec
        log.info("Consumer throughput: {} msg/sec", String.format("%.2f", throughput));
    }
}
```

### Latency Testing

```java
@SpringBootTest
@EmbeddedKafka(topics = "smart-admin-latency-test")
class KafkaLatencyTest {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Test
    void testEndToEndLatency() {
        List<Long> latencies = new ArrayList<>();
        int sampleSize = 100;

        for (int i = 0; i < sampleSize; i++) {
            long sendTime = System.currentTimeMillis();
            String message = sendTime + ":test-message";

            kafkaProducerService.send("smart-admin-latency-test", "key", message);

            // Wait for message to be consumed (collector records receive time)
            await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> latencyCollector.getLatestMessage() != null
                    && latencyCollector.getLatestMessage().startsWith(String.valueOf(sendTime)));

            long receiveTime = latencyCollector.getReceiveTime();
            long latency = receiveTime - sendTime;
            latencies.add(latency);
        }

        // Calculate statistics
        Collections.sort(latencies);
        long p50 = latencies.get(50);
        long p95 = latencies.get(95);
        long p99 = latencies.get(99);

        log.info("Latency - p50: {}ms, p95: {}ms, p99: {}ms", p50, p95, p99);

        // Assert
        assertThat(p99).isLessThan(100);  // p99 < 100ms
    }
}
```

---

## Chaos Engineering Tests

### Network Failure Simulation

```java
@SpringBootTest
@EmbeddedKafka
class ChaoEngineeringTest {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Test
    void testKafkaBrokerRestart() {
        // Given: Send initial message
        kafkaProducerService.send("smart-admin-chaos", "key1", "message-before-restart");

        await()
            .atMost(5, TimeUnit.SECONDS)
            .until(() -> messageCollector.getReceivedMessages().size() == 1);

        // When: Restart Kafka broker
        embeddedKafkaBroker.restart();

        // Wait for broker to be ready
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollDelay(1, TimeUnit.SECONDS)
            .until(() -> {
                try {
                    kafkaProducerService.send("smart-admin-chaos", "test", "ping");
                    return true;
                } catch (Exception e) {
                    return false;
                }
            });

        // Then: Send message after restart
        kafkaProducerService.send("smart-admin-chaos", "key2", "message-after-restart");

        await()
            .atMost(10, TimeUnit.SECONDS)
            .until(() -> messageCollector.getReceivedMessages().size() == 2);

        // Verify both messages received
        assertThat(messageCollector.getReceivedMessages())
            .hasSize(2)
            .contains("message-before-restart", "message-after-restart");
    }
}
```

---

## Best Practices

### 1. Use Awaitility for Async Assertions

```java
import static org.awaitility.Awaitility.*;

// ✅ Good: Use Awaitility
await()
    .atMost(10, TimeUnit.SECONDS)
    .pollInterval(100, TimeUnit.MILLISECONDS)
    .until(() -> messageCollector.getReceivedMessages().size() == 10);

// ❌ Bad: Thread.sleep()
Thread.sleep(5000);
assertThat(messageCollector.getReceivedMessages()).hasSize(10);
```

### 2. Isolate Tests with @DirtiesContext

```java
@SpringBootTest
@EmbeddedKafka
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IsolatedIntegrationTest {
    // Fresh ApplicationContext after all tests
}
```

### 3. Clean State Between Tests

```java
@BeforeEach
void setup() {
    messageCollector.clear();
    orderDao.deleteAll();
}
```

### 4. Use Test-Specific Topics

```java
// ✅ Good: Unique test topic
private static final String TEST_TOPIC = "smart-admin-test-" + UUID.randomUUID();

// ❌ Bad: Shared production topic
private static final String TEST_TOPIC = "smart-admin-order";
```

### 5. Configure Reasonable Timeouts

```java
// Fast tests: 5 seconds
await().atMost(5, TimeUnit.SECONDS).until(...);

// Rebalancing tests: 30 seconds
await().atMost(30, TimeUnit.SECONDS).until(...);

// Load tests: 60 seconds
await().atMost(60, TimeUnit.SECONDS).until(...);
```

### 6. Verify with Multiple Assertions

```java
@Test
void testMessageProcessing() {
    kafkaProducerService.send(TOPIC, "key", "message");

    await().until(() -> messageCollector.hasMessage("message"));

    // Verify side effects
    assertThat(orderDao.selectById("key")).isNotNull();
    assertThat(orderDao.selectById("key").getStatus()).isEqualTo("PROCESSED");
    assertThat(auditDao.selectByOrderId("key")).hasSize(1);
}
```

---

## Test Configuration Reference

### application-test.yml

```yaml
smart:
  kafka:
    enabled: true
    bootstrap-servers: ${spring.embedded.kafka.brokers}  # Auto-configured by @EmbeddedKafka

spring:
  kafka:
    # Producer configuration
    producer:
      bootstrap-servers: ${spring.embedded.kafka.brokers}
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      acks: all
      retries: 3

    # Consumer configuration
    consumer:
      bootstrap-servers: ${spring.embedded.kafka.brokers}
      group-id: test-consumer-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      auto-offset-reset: earliest
      enable-auto-commit: false

    # Listener configuration
    listener:
      ack-mode: manual
      concurrency: 1  # Single thread for predictable testing
```

---

## Troubleshooting Integration Tests

### Issue 1: Test Timeout

**Symptom**: Tests hang or timeout waiting for messages

**Causes**:
- Consumer not started (`autoStartup = false`)
- Wrong topic name
- Serialization error
- Consumer group not configured

**Solution**:
```java
// Verify consumer is running
@Autowired
private KafkaListenerEndpointRegistry registry;

@Test
void debugConsumerStatus() {
    registry.getListenerContainers().forEach(container -> {
        log.info("Container: {} | Running: {}",
            container.getListenerId(),
            container.isRunning());
    });
}
```

### Issue 2: Flaky Tests

**Symptom**: Tests pass sometimes, fail other times

**Causes**:
- Race conditions in async code
- Insufficient wait time
- State pollution between tests

**Solution**:
```java
// Use longer timeouts
await().atMost(30, TimeUnit.SECONDS).until(...);

// Clean state
@BeforeEach
void setup() {
    messageCollector.clear();
}

// Isolate contexts
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
```

### Issue 3: Port Already in Use

**Symptom**: `Address already in use: bind`

**Cause**: Previous test didn't release port

**Solution**:
```java
@EmbeddedKafka(
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:0",  // Random port
        "auto.create.topics.enable=true"
    }
)
```

---

## See Also

- [Testing Strategy](/kafka/testing/testing-strategy) - Overall testing approach
- [Unit Testing](/kafka/testing/unit-testing) - Unit test patterns
- [Verification Framework](/kafka/testing/verification-framework) - Quality scoring system

---

**Last Updated**: 2026-01-22
