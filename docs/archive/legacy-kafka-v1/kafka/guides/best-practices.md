# Best Practices

Production-ready best practices for SmartAdmin's Kafka integration covering message design, reliability, performance, and operations.

## Message Design

### 1. Keep Messages Small

```java
// ❌ Bad: Large message (5MB)
String largeReport = generateDetailedReport();  // 5MB
kafkaProducerService.send(topic, largeReport);

// ✅ Good: Store externally, send reference
String reportId = s3Service.upload(report);
String message = JSON.toJSONString(Map.of(
    "reportId", reportId,
    "s3Path", "s3://bucket/reports/" + reportId,
    "timestamp", System.currentTimeMillis()
));
kafkaProducerService.send(topic, message);
```

**Guidelines**:
- **Optimal**: < 10KB
- **Acceptable**: < 100KB
- **Avoid**: > 1MB (use external storage)

### 2. Use Meaningful Keys

```java
// ❌ Bad: Random keys break ordering
String key = UUID.randomUUID().toString();

// ✅ Good: Meaningful keys ensure ordering
String key = "USER-" + userId;           // User events
String key = "ORDER-" + orderId;         // Order processing
String key = String.valueOf(userId);     // Simple user ID
```

**Key Benefits**:
- **Ordering**: Same key → same partition → guaranteed order
- **Compaction**: Retain latest value per key
- **Routing**: Partition by business entity

### 3. Include Metadata

```java
// ✅ Good: Rich message with metadata
@Data
public class OrderMessage {
    private String orderId;
    private BigDecimal amount;
    private Long userId;

    // Metadata
    private Long timestamp;
    private String version;          // Message schema version
    private String source;           // Originating service
    private String correlationId;   // Trace requests
}
```

### 4. Version Your Messages

```java
@Data
public class OrderMessage {
    private String version = "1.0";  // Schema version
    private String orderId;
    private BigDecimal amount;

    // Handle version in consumer
    public static OrderMessage parse(String json) {
        JSONObject obj = JSON.parseObject(json);
        String version = obj.getString("version");

        return switch (version) {
            case "1.0" -> parseV1(obj);
            case "2.0" -> parseV2(obj);
            default -> throw new IllegalArgumentException("Unknown version: " + version);
        };
    }
}
```

---

## Producer Best Practices

### 1. Use Batch Send for Bulk Operations

```java
// ❌ Bad: Individual sends
for (Order order : orders) {
    kafkaProducerService.send(topic, JSON.toJSONString(order));
}

// ✅ Good: Batch send
List<String> messages = orders.stream()
    .map(order -> JSON.toJSONString(order))
    .toList();
kafkaProducerService.sendBatchAsync(topic, messages);
```

**Performance**: 5x throughput improvement with batching.

### 2. Handle Send Failures

```java
// ✅ Good: Handle failures with callback
kafkaProducerService.send(topic, key, message, new ProducerCallback<>() {
    @Override
    public void onSuccess(SendResult result) {
        log.info("Message sent: partition={}, offset={}",
            result.getRecordMetadata().partition(),
            result.getRecordMetadata().offset());
    }

    @Override
    public void onFailure(Throwable ex) {
        log.error("Send failed: key={}", key, ex);
        // Store for retry
        failedMessageRepo.save(key, message);
    }
});
```

### 3. Use Topic Constants

```java
// ✅ Good: Constants
public class KafkaConst {
    public static class Topic {
        public static final String ORDER = "smart-admin-order";
        public static final String USER_EVENT = "smart-admin-user-event";
    }
}

kafkaProducerService.send(KafkaConst.Topic.ORDER, message);

// ❌ Bad: Hardcoded strings
kafkaProducerService.send("order-topic", message);
```

---

## Consumer Best Practices

### 1. Extend AbstractKafkaListener

```java
// ✅ Good: Automatic DLQ routing
@Component
@Slf4j
public class OrderListener extends AbstractKafkaListener {

    @KafkaListener(topics = KafkaConst.Topic.ORDER, groupId = KafkaConst.Group.ORDER)
    public void onMessage(ConsumerRecord<String, String> record) {
        handleMessage(record);  // Auto-DLQ on exception
    }

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        processOrder(record.value());
    }
}
```

**Benefits**:
- ✅ Automatic DLQ routing
- ✅ Built-in logging
- ✅ No boilerplate error handling

### 2. Implement Idempotency

```java
@Override
protected void doHandle(ConsumerRecord<String, String> record) {
    String orderId = record.key();

    // Check if already processed (Redis)
    String cacheKey = "processed:order:" + orderId;
    if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
        log.info("Duplicate message, skipping: {}", orderId);
        return;
    }

    // Process
    OrderDTO order = JSON.parseObject(record.value(), OrderDTO.class);
    orderService.processOrder(order);

    // Mark as processed (24h TTL)
    redisTemplate.opsForValue().set(cacheKey, "1", 24, TimeUnit.HOURS);
}
```

### 3. Set Appropriate Timeouts

```yaml
smart:
  kafka:
    consumer:
      # Ensure processing time < max-poll-interval-ms
      max-poll-interval-ms: 300000  # 5 minutes

    listener:
      concurrency: 3                # Scale based on load
```

**Formula**: `batch_size * avg_processing_time < max-poll-interval-ms`

### 4. Use Batch Processing for High Throughput

```java
// ✅ Good: Batch consumer for high-volume topics
@Component
@Slf4j
public class OrderBatchListener extends AbstractBatchKafkaListener<OrderDTO> {

    @KafkaListener(
        topics = KafkaConst.Topic.ORDER,
        groupId = KafkaConst.Group.ORDER_BATCH,
        containerFactory = "batchKafkaListenerContainerFactory"
    )
    public void onBatch(List<ConsumerRecord<String, String>> records, Acknowledgment ack) {
        handleBatch(records, ack);
    }

    @Override
    protected void doBatchHandle(List<ConsumerRecord<String, String>> records) {
        // Efficient batch DB operation
        orderService.batchInsert(parseOrders(records));
    }

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        // Single-message fallback
        orderService.insert(parseOrder(record));
    }
}
```

---

## Reliability Best Practices

### 1. Enable Producer Idempotence

```yaml
smart:
  kafka:
    producer:
      enable-idempotence: true      # Exactly-once
      acks: all                     # Wait for all replicas
      retries: 3                    # Retry on failure
      max-in-flight-requests-per-connection: 1  # Strict ordering
```

### 2. Use Manual Offset Commit

```yaml
smart:
  kafka:
    consumer:
      enable-auto-commit: false     # Manual control

    listener:
      ack-mode: manual              # Explicit acknowledgment
```

**Benefits**:
- ✅ Commit only after successful processing
- ✅ Prevents message loss on failures
- ✅ At-least-once delivery guarantee

### 3. Monitor DLQ Topics

```java
@Scheduled(cron = "0 */5 * * * *")  // Every 5 minutes
public void checkDLQTopics() {
    List<String> dlqTopics = List.of(
        "smart-admin-order-dlq",
        "smart-admin-payment-dlq"
    );

    for (String topic : dlqTopics) {
        long messageCount = kafkaAdmin.getMessageCount(topic);

        if (messageCount > 100) {
            alertService.sendAlert("DLQ accumulation: " + topic + " has " + messageCount + " messages");
        }
    }
}
```

### 4. Implement Circuit Breakers

```java
@Service
@RequiredArgsConstructor
public class ResilientOrderService {

    @CircuitBreaker(name = "orderService", fallbackMethod = "processOrderFallback")
    public void processOrder(OrderDTO order) {
        orderDao.insert(order);  // May fail if DB down
    }

    public void processOrderFallback(OrderDTO order, Exception e) {
        log.error("Circuit breaker triggered: {}", e.getMessage());
        throw new BusinessException("Service temporarily unavailable");
    }
}
```

---

## Performance Best Practices

### 1. Tune Batch Sizes

```yaml
smart:
  kafka:
    producer:
      batch-size: 16384             # 16KB
      linger-ms: 5                  # 5ms wait

    consumer:
      max-poll-records: 500         # Fetch 500 messages

    batch:
      size: 100                     # Process 100 at once
```

### 2. Enable Compression

```yaml
smart:
  kafka:
    producer:
      compression-type: lz4         # Best balance (3:1 ratio, low CPU)
```

**Compression Comparison**:
- **lz4**: 3:1 ratio, low CPU (recommended)
- **snappy**: 2.5:1 ratio, low CPU
- **gzip**: 5:1 ratio, high CPU
- **zstd**: 6:1 ratio, medium CPU

### 3. Scale with Concurrency

```yaml
smart:
  kafka:
    listener:
      concurrency: 5                # Match partition count
```

**Rule**: Set concurrency ≤ partition count for optimal parallelism.

### 4. Use Connection Pooling

```yaml
smart:
  kafka:
    producer:
      # Connection pooling
      connections-max-idle-ms: 600000  # 10 min
      reconnect-backoff-ms: 50
      reconnect-backoff-max-ms: 1000
```

---

## Monitoring Best Practices

### 1. Track Key Metrics

```java
@Component
@RequiredArgsConstructor
public class KafkaMetrics {

    private final MeterRegistry meterRegistry;

    public void recordMessageSent(String topic, boolean success) {
        meterRegistry.counter("kafka.messages.sent",
            "topic", topic,
            "status", success ? "success" : "failure"
        ).increment();
    }

    public void recordProcessingTime(String topic, long durationMs) {
        meterRegistry.timer("kafka.processing.duration",
            "topic", topic
        ).record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordDLQMessage(String topic) {
        meterRegistry.counter("kafka.dlq.messages",
            "topic", topic
        ).increment();
    }
}
```

### 2. Set Up Alerts

**Prometheus Alert Rules**:
```yaml
groups:
  - name: kafka_alerts
    rules:
      - alert: KafkaConsumerLag
        expr: kafka_consumer_lag > 1000
        for: 5m
        annotations:
          summary: "Consumer lag exceeds 1000 messages"

      - alert: HighDLQRate
        expr: rate(kafka_dlq_messages[5m]) > 10
        for: 5m
        annotations:
          summary: "DLQ receiving > 10 messages/sec"

      - alert: ConsumerGroupDown
        expr: kafka_consumer_group_members == 0
        for: 2m
        annotations:
          summary: "No active consumers in group"
```

### 3. Log with Context

```java
@Override
protected void doHandle(ConsumerRecord<String, String> record) {
    long startTime = System.currentTimeMillis();

    try {
        log.info("Processing - Topic: {}, Partition: {}, Offset: {}, Key: {}",
            record.topic(), record.partition(), record.offset(), record.key());

        processOrder(record.value());

        long duration = System.currentTimeMillis() - startTime;
        log.info("Processed successfully - Key: {}, Duration: {}ms", record.key(), duration);
    } catch (Exception e) {
        long duration = System.currentTimeMillis() - startTime;
        log.error("Processing failed - Key: {}, Duration: {}ms", record.key(), duration, e);
        throw e;
    }
}
```

---

## Operational Best Practices

### 1. Use Separate Consumer Groups

```java
// Real-time processing
@KafkaListener(topics = KafkaConst.Topic.ORDER, groupId = "order-realtime")
public void processRealtime(ConsumerRecord<String, String> record) {
    // Real-time order fulfillment
}

// Analytics processing (same topic, different group)
@KafkaListener(topics = KafkaConst.Topic.ORDER, groupId = "order-analytics")
public void processAnalytics(ConsumerRecord<String, String> record) {
    // Order analytics
}
```

**Benefits**:
- ✅ Independent processing speeds
- ✅ Different failure handling
- ✅ Replay without affecting real-time

### 2. Plan for Topic Growth

```bash
# Create topics with sufficient partitions
kafka-topics.sh --create \
  --topic smart-admin-order \
  --partitions 6 \
  --replication-factor 3 \
  --config retention.ms=604800000  # 7 days
```

**Guidelines**:
- Start with 6-12 partitions
- Cannot reduce partition count later
- Can increase, but requires rebalancing

### 3. Set Retention Policies

```yaml
# Topic-level retention
kafka-topics.sh --alter \
  --topic smart-admin-order \
  --config retention.ms=604800000    # 7 days
  --config retention.bytes=1073741824  # 1GB
```

### 4. Graceful Shutdown

```java
@Component
@RequiredArgsConstructor
public class KafkaShutdownHook {

    private final KafkaListenerEndpointRegistry registry;

    @PreDestroy
    public void onShutdown() {
        log.info("Shutting down Kafka consumers gracefully...");

        registry.getAllListenerContainers().forEach(container -> {
            container.stop(() ->
                log.info("Stopped container: {}", container.getListenerId())
            );
        });

        log.info("All Kafka consumers stopped");
    }
}
```

---

## Security Best Practices

### 1. Use Environment Variables for Secrets

```yaml
smart:
  kafka:
    bootstrap-servers: ${KAFKA_BROKERS}

    ssl:
      truststore-password: ${KAFKA_TRUSTSTORE_PASSWORD}
      keystore-password: ${KAFKA_KEYSTORE_PASSWORD}

    sasl:
      jaas-config: |
        org.apache.kafka.common.security.plain.PlainLoginModule required
        username="${KAFKA_USERNAME}"
        password="${KAFKA_PASSWORD}";
```

### 2. Enable SSL/TLS in Production

```yaml
smart:
  kafka:
    bootstrap-servers: kafka:9093  # SSL port

    ssl:
      enabled: true
      protocol: TLSv1.2
      truststore-location: classpath:kafka.truststore.jks
      keystore-location: classpath:kafka.keystore.jks
```

### 3. Validate Input

```java
@Override
protected void doHandle(ConsumerRecord<String, String> record) {
    OrderDTO order = JSON.parseObject(record.value(), OrderDTO.class);

    // Validate
    if (order == null || order.getOrderId() == null) {
        log.error("Invalid message: {}", record.value());
        throw new ValidationException("Invalid order data");
    }

    if (order.getAmount() != null && order.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
        throw new ValidationException("Amount must be positive");
    }

    processOrder(order);
}
```

---

## Testing Best Practices

### 1. Use Embedded Kafka for Tests

```java
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"test-topic"})
class KafkaIntegrationTest {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Test
    void testSendAndReceive() {
        kafkaProducerService.send("test-topic", "test-message");
        // Assert message received
    }
}
```

### 2. Test Error Scenarios

```java
@Test
void testDLQRouting() {
    // Send invalid message
    kafkaProducerService.send(KafkaConst.Topic.ORDER, "invalid-json");

    // Assert DLQ contains message
    List<String> dlqMessages = consumeFromDLQ("smart-admin-order-dlq");
    assertThat(dlqMessages).hasSize(1);
}
```

---

## See Also

- [Producer Guide](/kafka/guides/producer-guide) - Producer patterns
- [Consumer Guide](/kafka/guides/consumer-guide) - Consumer patterns
- [Configuration](/kafka/guides/configuration) - Configuration reference
- [Error Handling](/kafka/guides/error-handling) - Error strategies
- [Performance Tuning](/kafka/operations/performance-tuning) - Optimization

---

**Last Updated**: 2026-01-21
