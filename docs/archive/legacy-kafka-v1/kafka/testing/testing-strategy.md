# Testing Strategy

Comprehensive testing approach for SmartAdmin's Kafka integration with 6-dimension quality scoring system.

## Overview

**Testing Pyramid for Kafka Integration**:
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

**Testing Layers**:
1. **Unit Tests** - Test components in isolation
2. **Integration Tests** - Test with embedded/real Kafka
3. **Contract Tests** - Validate message schemas
4. **Performance Tests** - Load and throughput testing
5. **E2E Tests** - Full application flow
6. **Chaos Tests** - Failure scenarios

---

## 6-Dimension Quality Scoring System

### Dimension 1: Configuration (L1)

**Score**: 0-100 points

**Criteria**:
- ✅ All required properties configured (30 points)
- ✅ Environment-specific configs (dev/test/prod) (20 points)
- ✅ Health check endpoints enabled (15 points)
- ✅ Monitoring configured (15 points)
- ✅ Security settings (SSL/SASL) (10 points)
- ✅ Documentation complete (10 points)

**Test Coverage**:
```java
@SpringBootTest
class KafkaConfigurationTest {

    @Autowired
    private KafkaProperties kafkaProperties;

    @Test
    void testBootstrapServersConfigured() {
        assertThat(kafkaProperties.getBootstrapServers())
            .isNotEmpty()
            .contains("localhost:9092");
    }

    @Test
    void testProducerIdempotenceEnabled() {
        assertThat(kafkaProperties.getProducer()
            .getProperties()
            .get("enable.idempotence"))
            .isEqualTo("true");
    }
}
```

---

### Dimension 2: Code Quality (L2)

**Score**: 0-100 points

**Criteria**:
- ✅ Producer service implemented correctly (25 points)
- ✅ Consumer listeners follow AbstractKafkaListener pattern (25 points)
- ✅ Error handling with DLQ routing (20 points)
- ✅ Logging and metrics instrumentation (15 points)
- ✅ Thread safety and concurrency (10 points)
- ✅ Code review passed (5 points)

**Test Coverage**:
```java
@SpringBootTest
class CodeQualityTest {

    @Test
    void testProducerServiceExists() {
        assertThat(kafkaProducerService).isNotNull();
    }

    @Test
    void testListenersExtendAbstractKafkaListener() {
        // Verify all @KafkaListener beans extend AbstractKafkaListener
        Map<String, Object> listeners = applicationContext
            .getBeansWithAnnotation(Component.class);

        listeners.values().stream()
            .filter(bean -> hasKafkaListenerMethod(bean))
            .forEach(bean ->
                assertThat(bean).isInstanceOf(AbstractKafkaListener.class)
            );
    }
}
```

---

### Dimension 3: Functional Correctness (L3)

**Score**: 0-100 points

**Criteria**:
- ✅ Messages sent successfully (20 points)
- ✅ Messages consumed correctly (20 points)
- ✅ Batch processing works (15 points)
- ✅ DLQ routing on failures (15 points)
- ✅ Message ordering preserved (10 points)
- ✅ Idempotency implemented (10 points)
- ✅ All edge cases handled (10 points)

**Test Coverage**:
```java
@SpringBootTest
@EmbeddedKafka
class FunctionalCorrectnessTest {

    @Test
    void testMessageSentAndReceived() {
        // Send message
        kafkaProducerService.send(TOPIC, "key", "value");

        // Wait for consumption
        await().atMost(5, SECONDS).until(() -> messageReceived);

        // Verify
        assertThat(receivedMessage).isEqualTo("value");
    }

    @Test
    void testBatchProcessing() {
        // Send 100 messages
        List<String> messages = IntStream.range(0, 100)
            .mapToObj(i -> "message-" + i)
            .toList();

        kafkaProducerService.sendBatchAsync(TOPIC, messages);

        // Verify batch consumed
        await().atMost(10, SECONDS).until(() ->
            consumedCount.get() == 100
        );
    }
}
```

---

### Dimension 4: Reliability (L4)

**Score**: 0-100 points

**Criteria**:
- ✅ Survives Kafka restart (25 points)
- ✅ Handles network failures (20 points)
- ✅ Consumer rebalancing handled (15 points)
- ✅ No message loss (20 points)
- ✅ DLQ messages recoverable (10 points)
- ✅ Graceful shutdown (10 points)

**Test Coverage**:
```java
@SpringBootTest
class ReliabilityTest {

    @Test
    void testSurvivesKafkaRestart() {
        // Send message
        kafkaProducerService.send(TOPIC, "key", "message1");

        // Restart Kafka
        embeddedKafkaBroker.restart();

        // Send another message
        kafkaProducerService.send(TOPIC, "key", "message2");

        // Both should be consumed
        await().atMost(10, SECONDS).until(() ->
            consumedCount.get() == 2
        );
    }

    @Test
    void testNoMessageLossOnRebalance() {
        // Start sending messages
        CompletableFuture.runAsync(() -> {
            for (int i = 0; i < 1000; i++) {
                kafkaProducerService.send(TOPIC, "key", "msg-" + i);
            }
        });

        // Trigger rebalance by adding consumer
        startNewConsumerInstance();

        // Wait for all messages
        await().atMost(30, SECONDS).until(() ->
            consumedCount.get() == 1000
        );
    }
}
```

---

### Dimension 5: Performance (L5)

**Score**: 0-100 points

**Criteria**:
- ✅ Producer throughput ≥ 5,000 msg/sec (25 points)
- ✅ Consumer throughput ≥ 5,000 msg/sec (25 points)
- ✅ Batch processing ≥ 10,000 msg/sec (20 points)
- ✅ Latency p99 < 100ms (15 points)
- ✅ Consumer lag < 1,000 (10 points)
- ✅ Resource usage acceptable (5 points)

**Test Coverage**:
```java
@SpringBootTest
class PerformanceTest {

    @Test
    void testProducerThroughput() {
        int messageCount = 10000;
        long startTime = System.currentTimeMillis();

        // Send messages
        for (int i = 0; i < messageCount; i++) {
            kafkaProducerService.send(TOPIC, "key-" + i, "message-" + i);
        }

        long duration = System.currentTimeMillis() - startTime;
        double throughput = (messageCount * 1000.0) / duration;

        assertThat(throughput).isGreaterThan(5000);
        log.info("Producer throughput: {} msg/sec", throughput);
    }

    @Test
    void testConsumerLatency() {
        List<Long> latencies = new ArrayList<>();

        // Send messages with timestamp
        for (int i = 0; i < 1000; i++) {
            long sendTime = System.currentTimeMillis();
            kafkaProducerService.send(TOPIC, "key", sendTime + ":message");
        }

        // Calculate latencies
        await().atMost(30, SECONDS).until(() -> latencies.size() == 1000);

        // Check p99 latency
        Collections.sort(latencies);
        long p99 = latencies.get(990);

        assertThat(p99).isLessThan(100);
    }
}
```

---

### Dimension 6: Operations (L6)

**Score**: 0-100 points

**Criteria**:
- ✅ Monitoring dashboards available (20 points)
- ✅ Alerts configured (20 points)
- ✅ Logging complete and searchable (15 points)
- ✅ Health checks working (15 points)
- ✅ Troubleshooting runbooks (15 points)
- ✅ Backup and recovery tested (10 points)
- ✅ Documentation up-to-date (5 points)

**Test Coverage**:
```java
@SpringBootTest
class OperationsTest {

    @Test
    void testHealthCheckEndpoint() {
        ResponseEntity<String> response = restTemplate
            .getForEntity("/actuator/health/kafka", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void testMetricsAvailable() {
        ResponseEntity<String> response = restTemplate
            .getForEntity("/actuator/metrics/kafka.producer.record-send-total", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testLoggingEnabled() {
        // Verify Kafka logs are present
        ListAppender<ILoggingEvent> listAppender = getLogAppender();

        kafkaProducerService.send(TOPIC, "key", "value");

        await().atMost(2, SECONDS).until(() ->
            listAppender.list.stream()
                .anyMatch(event -> event.getMessage().contains("Message sent"))
        );
    }
}
```

---

## Quality Score Calculation

**Total Score** = (L1 + L2 + L3 + L4 + L5 + L6) / 6

**Grading Scale**:
- **90-100**: Production Ready ✅
- **80-89**: Good Quality ✓
- **70-79**: Acceptable ⚠️
- **60-69**: Needs Improvement ❌
- **< 60**: Not Production Ready ❌❌

---

## Testing Best Practices

### 1. Test Isolation

**Use embedded Kafka for unit/integration tests**:
```java
@SpringBootTest
@EmbeddedKafka(
    partitions = 3,
    topics = {"test-topic"},
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
    }
)
class IsolatedTest {
    // Test code
}
```

### 2. Test Data Management

**Use test fixtures**:
```java
@TestConfiguration
public class KafkaTestConfig {

    @Bean
    public TestDataBuilder testDataBuilder() {
        return new TestDataBuilder();
    }
}

public class TestDataBuilder {
    public OrderDTO buildOrder() {
        return OrderDTO.builder()
            .orderId("TEST-" + UUID.randomUUID())
            .amount(BigDecimal.valueOf(99.99))
            .build();
    }
}
```

### 3. Async Testing

**Use Awaitility**:
```java
import static org.awaitility.Awaitility.*;

@Test
void testAsyncProcessing() {
    kafkaProducerService.send(TOPIC, "key", "value");

    await()
        .atMost(Duration.ofSeconds(10))
        .pollInterval(Duration.ofMillis(100))
        .until(() -> messageProcessed);
}
```

### 4. Error Scenario Testing

**Test failure cases**:
```java
@Test
void testDLQRoutingOnError() {
    // Send invalid message
    kafkaProducerService.send(TOPIC, "key", "{invalid-json}");

    // Verify sent to DLQ
    await().atMost(5, SECONDS).until(() ->
        dlqMessageReceived
    );
}
```

---

## Testing Checklist

**Before Production Deployment**:

- [ ] All 6 dimensions scored ≥ 80
- [ ] Unit test coverage ≥ 80%
- [ ] Integration tests passing
- [ ] Performance tests meeting SLA
- [ ] Chaos tests passed
- [ ] Security scan completed
- [ ] Documentation reviewed
- [ ] Runbooks created
- [ ] Monitoring configured
- [ ] Alerts tested

---

## See Also

- [Unit Testing](/kafka/testing/unit-testing) - Unit test examples
- [Integration Testing](/kafka/testing/integration-testing) - Integration test setup
- [Verification Framework](/kafka/testing/verification-framework) - Detailed verification guide

---

**Last Updated**: 2026-01-22
