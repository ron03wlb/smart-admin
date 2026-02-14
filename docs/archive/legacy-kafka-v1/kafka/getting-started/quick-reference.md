# Quick Reference

API cheat sheet and common patterns for SmartAdmin Kafka.

## Producer API

### Basic Send

```java
// Simple send (async)
kafkaProducerService.send("topic-name", message);

// Send with key
kafkaProducerService.send("topic-name", key, message);

// Send with callback
kafkaProducerService.send("topic-name", message, new ProducerCallback<String, String>() {
    @Override
    public void onSuccess(SendResult<String, String> result) {
        log.info("Message sent to partition: {}",
            result.getRecordMetadata().partition());
    }

    @Override
    public void onFailure(Throwable ex) {
        log.error("Failed to send message", ex);
    }
});
```

### Batch Send (Async)

```java
// Send batch (no keys)
List<String> messages = List.of("msg1", "msg2", "msg3");
CompletableFuture<List<SendResult<String, String>>> future =
    kafkaProducerService.sendBatchAsync(KafkaConst.Topic.ORDER, messages);

// Handle batch results
future.thenAccept(results -> {
    results.forEach(result -> {
        log.info("Message sent to partition {}, offset {}",
            result.getRecordMetadata().partition(),
            result.getRecordMetadata().offset());
    });
});

// Send batch with keys
List<Map.Entry<String, String>> keyedMessages = List.of(
    Map.entry("key1", "message1"),
    Map.entry("key2", "message2")
);
CompletableFuture<List<SendResult<String, String>>> future2 =
    kafkaProducerService.sendBatchAsync(KafkaConst.Topic.ORDER, keyedMessages);
```

### Batch Send Pattern

```java
// Collect messages and send in batch
List<String> orderMessages = orders.stream()
    .map(order -> "Order:" + order.getOrderNo())
    .toList();

kafkaProducerService.sendBatchAsync(KafkaConst.Topic.ORDER, orderMessages)
    .thenAccept(results -> log.info("Sent {} messages", results.size()))
    .exceptionally(ex -> {
        log.error("Batch send failed", ex);
        return null;
    });
```

## Consumer API

### Single Message Consumer (Recommended Pattern)

Extend `AbstractKafkaListener` for automatic error handling and DLQ support:

```java
@Component
@Slf4j
public class OrderListener extends AbstractKafkaListener {

    @KafkaListener(
        topics = KafkaConst.Topic.ORDER,
        groupId = KafkaConst.Group.ORDER
    )
    public void onOrderMessage(ConsumerRecord<String, String> record) {
        handleMessage(record);  // Provided by AbstractKafkaListener
    }

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        String message = record.value();
        String key = record.key();

        // Your business logic
        processOrder(message);
    }
}
```

### Batch Consumer (High-Throughput)

Extend `AbstractBatchKafkaListener` for batch processing with degradation:

```java
@Component
@Slf4j
public class OrderBatchListener extends AbstractBatchKafkaListener<OrderMessage> {

    @KafkaListener(
        topics = KafkaConst.Topic.ORDER,
        groupId = KafkaConst.Group.ORDER,
        containerFactory = "batchKafkaListenerContainerFactory"
    )
    public void onOrderBatch(
        List<ConsumerRecord<String, String>> records,
        Acknowledgment ack
    ) {
        handleBatch(records, ack);
    }

    @Override
    protected void doBatchHandle(List<ConsumerRecord<String, String>> records) {
        // Batch processing logic
        List<OrderMessage> orders = records.stream()
            .map(r -> parseOrder(r.value()))
            .toList();

        orderService.batchProcess(orders);  // Efficient batch DB operation
    }

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        // Single-message fallback (auto-degradation on batch failure)
        OrderMessage order = parseOrder(record.value());
        orderService.process(order);
    }

    @Override
    protected int getBatchSize() {
        return 100;  // Max batch size
    }
}
```

### Access Message Metadata

```java
@Override
protected void doHandle(ConsumerRecord<String, String> record) {
    // Message data
    String key = record.key();
    String value = record.value();

    // Metadata
    String topic = record.topic();
    int partition = record.partition();
    long offset = record.offset();
    long timestamp = record.timestamp();

    // Headers
    Headers headers = record.headers();
    Header correlationId = headers.lastHeader("correlationId");

    log.info("Topic: {}, Partition: {}, Offset: {}, Key: {}",
        topic, partition, offset, key);
}
```

## Configuration Snippets

### Complete Configuration (application.yml)

```yaml
smart:
  kafka:
    # Connection
    bootstrap-servers: localhost:9092

    # Producer (Production-Grade)
    producer:
      acks: all                             # ✅ Wait for all replicas
      retries: 3                            # ✅ Retry 3 times
      enable-idempotence: true              # ✅ Avoid duplicates
      max-in-flight-requests-per-connection: 1  # ⚠️ Order guarantee
      batch-size: 16384                     # 16KB batch
      linger-ms: 5                          # 5ms wait for batching
      compression-type: lz4                 # LZ4 compression

    # Consumer (Production-Grade)
    consumer:
      enable-auto-commit: false             # ✅ Manual commit
      auto-offset-reset: earliest           # Start from beginning
      max-poll-records: 500                 # Batch size per poll
      session-timeout-ms: 45000             # 45s session timeout
      heartbeat-interval-ms: 3000           # 3s heartbeat
      max-poll-interval-ms: 300000          # 5min max processing time

    # Batch Processing (Optional)
    batch:
      enabled: true
      size: 100                             # Max messages per batch
      aggregate:
        enabled: true
        count: 50                           # Aggregate 50 messages
        timeout-ms: 5000                    # Or 5s timeout

    # Listener Container
    listener:
      batch-listener: true                  # Enable batch mode
      ack-mode: manual                      # Manual acknowledgment
      concurrency: 3                        # 3 concurrent consumers
```

### Environment-Specific Configuration

**Development (application-dev.yml)**:
```yaml
smart:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      auto-offset-reset: latest             # Skip old messages
    listener:
      concurrency: 1                        # Single thread
```

**Production (application-prod.yml)**:
```yaml
smart:
  kafka:
    bootstrap-servers: kafka-1:9092,kafka-2:9092,kafka-3:9092
    producer:
      acks: all
      enable-idempotence: true
    consumer:
      auto-offset-reset: earliest           # Don't lose messages
    listener:
      concurrency: 5                        # Higher throughput
```

## Common Patterns

### Error Handling with DLQ

**Automatic DLQ** (using AbstractKafkaListener):

```java
@Component
@Slf4j
public class OrderListener extends AbstractKafkaListener {

    @KafkaListener(topics = KafkaConst.Topic.ORDER,
                   groupId = KafkaConst.Group.ORDER)
    public void onOrderMessage(ConsumerRecord<String, String> record) {
        handleMessage(record);  // Auto-sends to DLQ on failure
    }

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        // If this throws exception, message goes to DLQ automatically
        processOrder(record.value());
    }
}
```

**Manual DLQ Handling**:

```java
@Component
@RequiredArgsConstructor
public class ManualDLQExample {

    private final DeadLetterService deadLetterService;

    @KafkaListener(topics = KafkaConst.Topic.ORDER,
                   groupId = KafkaConst.Group.ORDER)
    public void handleMessage(ConsumerRecord<String, String> record) {
        try {
            processOrder(record.value());
        } catch (BusinessException e) {
            // Manually send to DLQ
            deadLetterService.sendToDeadLetter(
                record.topic(),
                record.key(),
                record.value(),
                e.getMessage()
            );
        }
    }
}
```

**Batch Error Handling with Degradation**:

```java
@Component
public class OrderBatchListener extends AbstractBatchKafkaListener<Order> {

    @Override
    protected void doBatchHandle(List<ConsumerRecord<String, String>> records) {
        // Try batch processing
        try {
            batchProcessOrders(records);
        } catch (Exception e) {
            // Auto-degrades to single-message processing
            // Failed messages sent to DLQ automatically
            throw e;  // Triggers degradation
        }
    }

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        // Single message fallback (auto-DLQ on failure)
        processSingleOrder(record);
    }
}
```

### Idempotent Processing

```java
@KafkaListener(topics = "topic-name", groupId = "my-group")
public void handleMessage(
    @Payload String message,
    @Header(KafkaHeaders.RECEIVED_KEY) String messageId
) {
    // Check if already processed
    if (redisService.exists("processed:" + messageId)) {
        log.info("Message already processed: {}", messageId);
        return;
    }

    // Process message
    processMessage(message);

    // Mark as processed
    redisService.set("processed:" + messageId, "1", 24, TimeUnit.HOURS);
}
```

### Message Aggregation (Count or Time-based)

**Configure Aggregation**:

```yaml
smart:
  kafka:
    batch:
      aggregate:
        enabled: true
        count: 50                  # Aggregate 50 messages
        timeout-ms: 5000           # Or 5s timeout
```

**Using MessageAggregator**:

```java
@Component
@RequiredArgsConstructor
public class OrderAggregatorListener {

    private final MessageAggregator<OrderMessage> aggregator;
    private final OrderService orderService;

    @KafkaListener(topics = KafkaConst.Topic.ORDER,
                   groupId = KafkaConst.Group.ORDER)
    public void handleMessage(ConsumerRecord<String, String> record) {
        OrderMessage order = parseOrder(record.value());

        // Add to aggregator
        aggregator.add(order, orders -> {
            // This callback fires when:
            // - Count reaches 50, OR
            // - 5 seconds elapsed since first message
            log.info("Processing aggregated batch of {} orders", orders.size());
            orderService.batchProcess(orders);
        });
    }
}
```

### Transaction Support

```java
@Service
@RequiredArgsConstructor
public class OrderManager {

    private final EmployeeDao employeeDao;
    private final KafkaProducerService kafkaProducerService;

    @Transactional(rollbackFor = Throwable.class)
    public void processOrderWithTransaction(OrderForm form) {
        // Database operations
        OrderEntity order = createOrder(form);
        employeeDao.updateEmployee(employee);

        // Kafka send (participates in transaction)
        kafkaProducerService.send(
            KafkaConst.Topic.ORDER,
            "Order created: " + order.getOrderNo()
        );

        // Both commit or rollback together
    }
}
```

## Monitoring Endpoints

```bash
# Health check
GET /actuator/health/kafka

# Metrics
GET /actuator/metrics/kafka.producer.record-send-total
GET /actuator/metrics/kafka.consumer.records-consumed-total
```

## Common Commands

```bash
# List topics
kafka-topics.sh --list --bootstrap-server localhost:9092

# Describe topic
kafka-topics.sh --describe --topic topic-name --bootstrap-server localhost:9092

# Consume from beginning
kafka-console-consumer.sh --topic topic-name --from-beginning --bootstrap-server localhost:9092

# Produce test message
echo "test message" | kafka-console-producer.sh --topic topic-name --bootstrap-server localhost:9092
```

## See Also

- [Producer Guide](/kafka/guides/producer-guide) - Detailed producer documentation
- [Consumer Guide](/kafka/guides/consumer-guide) - Detailed consumer documentation
- [Configuration Reference](/kafka/reference/configuration-reference) - All configuration options
- [API Reference](/kafka/reference/api-reference) - Complete API documentation

::: warning Work in Progress
This document is being actively developed. More content coming soon!
:::
