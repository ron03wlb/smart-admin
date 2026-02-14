# Producer Guide

Complete guide to producing messages with SmartAdmin's Kafka integration, covering all sending patterns, configuration, and best practices.

## Overview

SmartAdmin provides a unified `KafkaProducerService` interface for sending messages to Kafka topics. It supports:

- **Simple Send**: Single message, fire-and-forget
- **Keyed Send**: Messages with partition keys
- **Batch Send**: Multiple messages in parallel
- **Callback Support**: Custom success/failure handling
- **Transactional Support**: Integrated with Spring `@Transactional`

---

## KafkaProducerService Interface

### Core Methods

```java
public interface KafkaProducerService {
    /**
     * Send message asynchronously (fire-and-forget)
     */
    void send(String topic, String message);

    /**
     * Send message with key (controls partitioning)
     */
    void send(String topic, String key, String message);

    /**
     * Send message with custom callback
     */
    void send(String topic, String message, ProducerCallback<String, String> callback);

    /**
     * Send message with key and callback
     */
    void send(String topic, String key, String message,
              ProducerCallback<String, String> callback);

    /**
     * Send batch asynchronously (parallel execution)
     */
    CompletableFuture<List<SendResult<String, String>>> sendBatchAsync(
        String topic, List<String> messages);

    /**
     * Send batch with keys asynchronously
     */
    CompletableFuture<List<SendResult<String, String>>> sendBatchAsync(
        String topic, List<Map.Entry<String, String>> keyedMessages);
}
```

---

## Basic Usage

### Dependency Injection

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final KafkaProducerService kafkaProducerService;

    // Use constructor injection (NOT @Autowired field injection)
}
```

### Simple Send (Fire-and-Forget)

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final KafkaProducerService kafkaProducerService;

    public void createOrder(OrderForm form) {
        // Create order
        OrderEntity order = createOrderEntity(form);
        employeeDao.insert(order);

        // Send message asynchronously
        String message = "Order created: " + order.getOrderNo();
        kafkaProducerService.send(KafkaConst.Topic.ORDER, message);

        // Method returns immediately, message sent in background
    }
}
```

**Characteristics**:
- ✅ **Non-blocking**: Returns immediately
- ✅ **High throughput**: ~1,000 msg/s
- ✅ **Automatic retry**: Configured retries on failure
- ⚠️ **No acknowledgment**: Success/failure not immediately known
- ⚠️ **Use for non-critical messages**: Logs, metrics, analytics

---

## Keyed Messages

### Why Use Keys?

Message keys control:
1. **Partitioning**: Same key → same partition → ordering guaranteed
2. **Compaction**: Latest value per key retained (log compaction)
3. **Consumer routing**: Key-based processing logic

### Send with Key

```java
@Service
@RequiredArgsConstructor
public class UserEventService {

    private final KafkaProducerService kafkaProducerService;

    public void publishUserEvent(Long userId, String eventType) {
        String key = "USER-" + userId;          // Key: ensures ordering per user
        String message = eventType + "|" + userId;

        kafkaProducerService.send(KafkaConst.Topic.USER_EVENTS, key, message);
    }
}
```

**Key Selection Guidelines**:

| Use Case | Key Strategy | Example |
|----------|--------------|---------|
| User events | User ID | `USER-12345` |
| Order processing | Order ID | `ORDER-20260121-001` |
| Device metrics | Device ID | `DEVICE-ABC123` |
| Global events | Null or fixed key | `null` or `GLOBAL` |

---

## Callback Pattern

### Custom Success/Failure Handling

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class CriticalOrderService {

    private final KafkaProducerService kafkaProducerService;
    private final OrderDao orderDao;

    public void createCriticalOrder(OrderForm form) {
        OrderEntity order = createOrderEntity(form);
        employeeDao.insert(order);

        String message = JSON.toJSONString(order);

        // Send with callback for critical messages
        kafkaProducerService.send(
            KafkaConst.Topic.ORDER,
            order.getOrderNo(),
            message,
            new ProducerCallback<String, String>() {
                @Override
                public void onSuccess(SendResult<String, String> result) {
                    RecordMetadata metadata = result.getRecordMetadata();
                    log.info("Critical order sent - Topic: {}, Partition: {}, Offset: {}",
                        metadata.topic(), metadata.partition(), metadata.offset());

                    // Update order status
                    orderDao.updateStatus(order.getId(), OrderStatus.MESSAGE_SENT);
                }

                @Override
                public void onFailure(Throwable ex) {
                    log.error("Failed to send critical order: {}", order.getOrderNo(), ex);

                    // Mark order for manual retry
                    orderDao.updateStatus(order.getId(), OrderStatus.MESSAGE_FAILED);

                    // Send alert
                    alertService.sendAlert("Kafka send failed for order: " + order.getOrderNo());
                }
            }
        );
    }
}
```

**When to Use Callbacks**:
- ✅ Critical business messages requiring acknowledgment
- ✅ Metrics collection (track send success rate)
- ✅ Database status updates based on send result
- ✅ Alerting on failures
- ❌ High-volume non-critical messages (adds overhead)

---

## Batch Sending

### Parallel Batch Send

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class BulkOrderService {

    private final KafkaProducerService kafkaProducerService;

    public void processBulkOrders(List<OrderEntity> orders) {
        // Convert orders to messages
        List<String> messages = orders.stream()
            .map(order -> JSON.toJSONString(order))
            .toList();

        // Send batch asynchronously (parallel execution)
        CompletableFuture<List<SendResult<String, String>>> future =
            kafkaProducerService.sendBatchAsync(KafkaConst.Topic.ORDER, messages);

        // Handle results asynchronously
        future.thenAccept(results -> {
            log.info("Sent {} messages successfully", results.size());

            // Process results
            for (int i = 0; i < results.size(); i++) {
                SendResult<String, String> result = results.get(i);
                OrderEntity order = orders.get(i);

                log.info("Order {} sent to partition {} at offset {}",
                    order.getOrderNo(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            }
        }).exceptionally(ex -> {
            log.error("Batch send failed", ex);
            return null;
        });
    }
}
```

### Batch Send with Keys

```java
@Service
@RequiredArgsConstructor
public class UserBulkEventService {

    private final KafkaProducerService kafkaProducerService;

    public void publishBulkUserEvents(List<UserEvent> events) {
        // Create keyed messages
        List<Map.Entry<String, String>> keyedMessages = events.stream()
            .map(event -> Map.entry(
                "USER-" + event.getUserId(),           // Key
                JSON.toJSONString(event)               // Value
            ))
            .toList();

        // Send batch with keys
        CompletableFuture<List<SendResult<String, String>>> future =
            kafkaProducerService.sendBatchAsync(KafkaConst.Topic.USER_EVENTS, keyedMessages);

        future.thenAccept(results ->
            log.info("Sent {} user events", results.size())
        );
    }
}
```

**Batch Send Performance**:

| Metric | Single Send | Batch Send (100) | Improvement |
|--------|-------------|------------------|-------------|
| Throughput | 1,000 msg/s | 5,000 msg/s | 5x |
| Latency | 5ms per msg | 10ms total | 50x faster |
| Network calls | 100 | 1 | 100x fewer |

---

## Message Headers

### Adding Custom Headers

```java
@Service
@RequiredArgsConstructor
public class EnhancedProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendWithHeaders(String topic, String key, String message) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, message);

        // Add custom headers
        record.headers()
            .add("messageType", "ORDER_CREATED".getBytes())
            .add("version", "1.0".getBytes())
            .add("source", "order-service".getBytes())
            .add("correlationId", UUID.randomUUID().toString().getBytes())
            .add("timestamp", String.valueOf(System.currentTimeMillis()).getBytes());

        kafkaTemplate.send(record);
    }
}
```

**Common Header Use Cases**:
- **Tracing**: `correlationId`, `requestId`, `spanId`
- **Versioning**: `messageVersion`, `schemaVersion`
- **Routing**: `messageType`, `priority`, `targetRegion`
- **Metadata**: `source`, `timestamp`, `userId`

---

## Transactional Messaging

### Database + Kafka Transaction

```java
@Service
@RequiredArgsConstructor
public class TransactionalOrderManager {

    private final OrderDao orderDao;
    private final KafkaProducerService kafkaProducerService;

    @Transactional(rollbackFor = Throwable.class)
    public void createOrderTransactional(OrderForm form) {
        // Database operation
        OrderEntity order = createOrderEntity(form);
        orderDao.insert(order);

        // Kafka send (participates in transaction if configured)
        String message = JSON.toJSONString(order);
        kafkaProducerService.send(KafkaConst.Topic.ORDER, message);

        // Both commit together or rollback together
        // Requires: spring.kafka.producer.transaction-id-prefix
    }
}
```

**Configuration for Transactions**:

```yaml
spring:
  kafka:
    producer:
      # Enable transactions
      transaction-id-prefix: tx-order-service-

      # Required for transactions
      acks: all
      enable-idempotence: true
      max-in-flight-requests-per-connection: 1
```

**Transaction Guarantees**:
- ✅ **Atomicity**: DB + Kafka commit together
- ✅ **No duplicates**: Idempotent producer
- ✅ **Ordering**: Single in-flight request
- ⚠️ **Performance**: ~30% throughput reduction
- ⚠️ **Complexity**: Requires careful configuration

---

## Error Handling

### Retry Configuration

```yaml
smart:
  kafka:
    producer:
      # Retry settings
      retries: 3                              # Retry 3 times
      retry-backoff-ms: 100                   # 100ms between retries

      # Delivery guarantees
      acks: all                               # Wait for all replicas
      enable-idempotence: true                # Exactly-once semantics

      # Ordering guarantee
      max-in-flight-requests-per-connection: 1  # Strict ordering
```

### Handling Send Failures

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ResilientProducerService {

    private final KafkaProducerService kafkaProducerService;
    private final FailedMessageRepository failedMessageRepo;

    public void sendWithFallback(String topic, String key, String message) {
        try {
            kafkaProducerService.send(topic, key, message, new ProducerCallback<>() {
                @Override
                public void onSuccess(SendResult<String, String> result) {
                    log.info("Message sent successfully");
                }

                @Override
                public void onFailure(Throwable ex) {
                    // Store failed message for later retry
                    FailedMessage failed = new FailedMessage();
                    failed.setTopic(topic);
                    failed.setKey(key);
                    failed.setMessage(message);
                    failed.setError(ex.getMessage());
                    failed.setAttempts(0);

                    failedMessageRepo.save(failed);

                    log.error("Message send failed, saved for retry: {}", key, ex);
                }
            });
        } catch (Exception e) {
            log.error("Unexpected error sending message", e);
            throw new BusinessException(KafkaErrorCode.SEND_FAILED);
        }
    }
}
```

### Retry Failed Messages

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class FailedMessageRetryService {

    private final FailedMessageRepository failedMessageRepo;
    private final KafkaProducerService kafkaProducerService;

    @Scheduled(cron = "0 */5 * * * *")  // Every 5 minutes
    public void retryFailedMessages() {
        List<FailedMessage> failed = failedMessageRepo.findPendingRetry(100);

        for (FailedMessage msg : failed) {
            if (msg.getAttempts() >= 5) {
                msg.setStatus("MAX_RETRIES_EXCEEDED");
                failedMessageRepo.save(msg);
                continue;
            }

            try {
                kafkaProducerService.send(msg.getTopic(), msg.getKey(), msg.getMessage());
                failedMessageRepo.delete(msg);
                log.info("Retry successful for message: {}", msg.getKey());
            } catch (Exception e) {
                msg.setAttempts(msg.getAttempts() + 1);
                msg.setLastError(e.getMessage());
                failedMessageRepo.save(msg);
                log.warn("Retry failed for message: {}", msg.getKey());
            }
        }
    }
}
```

---

## Performance Tuning

### Producer Configuration

```yaml
smart:
  kafka:
    producer:
      # Throughput optimization
      batch-size: 16384              # 16KB batch size
      linger-ms: 5                   # Wait 5ms to batch messages
      buffer-memory: 33554432        # 32MB buffer

      # Compression (improves network efficiency)
      compression-type: lz4          # lz4, gzip, snappy, zstd

      # Reliability
      acks: all                      # Wait for all replicas
      retries: 3                     # Retry up to 3 times

      # Performance vs ordering trade-off
      max-in-flight-requests-per-connection: 5  # Higher throughput
      # Set to 1 for strict ordering
```

### Batching Strategy

**When to Batch**:
- ✅ Bulk operations (>10 messages)
- ✅ High-volume scenarios (>1,000 msg/s)
- ✅ Messages can be grouped logically
- ❌ Real-time critical messages
- ❌ Small message volumes (<10 msg/s)

**Batching Code Pattern**:

```java
@Service
@RequiredArgsConstructor
public class OptimizedProducerService {

    private final KafkaProducerService kafkaProducerService;

    // Accumulator for batching
    private final List<String> messageBuffer = new CopyOnWriteArrayList<>();
    private static final int BATCH_SIZE = 100;

    public void sendOptimized(String message) {
        messageBuffer.add(message);

        if (messageBuffer.size() >= BATCH_SIZE) {
            flushBatch();
        }
    }

    @Scheduled(fixedDelay = 5000)  // Flush every 5 seconds
    public void flushBatch() {
        if (messageBuffer.isEmpty()) {
            return;
        }

        List<String> batch = new ArrayList<>(messageBuffer);
        messageBuffer.clear();

        kafkaProducerService.sendBatchAsync(KafkaConst.Topic.ORDER, batch)
            .thenAccept(results ->
                log.info("Flushed batch of {} messages", results.size())
            );
    }
}
```

### Compression Comparison

| Compression | Ratio | CPU | Use Case |
|-------------|-------|-----|----------|
| **none** | 1:1 | Low | Small messages, low latency |
| **lz4** | 3:1 | Low | **Recommended** - balanced |
| **snappy** | 2.5:1 | Low | Fast compression |
| **gzip** | 5:1 | High | High compression, lower volume |
| **zstd** | 6:1 | Medium | Best compression, modern |

---

## Best Practices

### 1. Message Size

```java
// ❌ Bad: Large messages (>1MB) hurt performance
String largeMessage = generateLargeReport();  // 5MB
kafkaProducerService.send(topic, largeMessage);

// ✅ Good: Store large data externally, send reference
String reportId = uploadReportToS3(report);
String message = JSON.toJSONString(Map.of(
    "reportId", reportId,
    "s3Path", "s3://bucket/reports/" + reportId
));
kafkaProducerService.send(topic, message);
```

**Recommended Message Sizes**:
- **Optimal**: <10KB
- **Acceptable**: <100KB
- **Avoid**: >1MB (use external storage)

### 2. JSON Serialization

```java
// ✅ Good: Consistent JSON serialization
@Service
@RequiredArgsConstructor
public class OrderProducerService {

    public void sendOrder(OrderEntity order) {
        String message = JSON.toJSONString(order);  // Consistent format
        kafkaProducerService.send(KafkaConst.Topic.ORDER, message);
    }
}
```

### 3. Topic Naming

```java
public class KafkaConst {
    public static class Topic {
        public static final String ORDER = "smart-admin-order";
        public static final String USER_EVENT = "smart-admin-user-event";
        public static final String PAYMENT = "smart-admin-payment";
    }
}

// ✅ Use constants, not hardcoded strings
kafkaProducerService.send(KafkaConst.Topic.ORDER, message);

// ❌ Avoid hardcoded strings
kafkaProducerService.send("order-topic", message);
```

### 4. Monitoring

```java
@Component
@RequiredArgsConstructor
public class ProducerMetrics {

    private final MeterRegistry meterRegistry;

    public void recordSend(String topic, boolean success) {
        meterRegistry.counter("kafka.producer.send.total",
            "topic", topic,
            "status", success ? "success" : "failure"
        ).increment();
    }
}
```

---

## Common Pitfalls

### 1. Blocking Operations

```java
// ❌ Bad: Blocking on send
CompletableFuture<SendResult> future = kafkaTemplate.send(topic, message);
future.get();  // BLOCKS thread

// ✅ Good: Async with callback
kafkaProducerService.send(topic, message, new ProducerCallback<>() {
    @Override
    public void onSuccess(SendResult result) { }

    @Override
    public void onFailure(Throwable ex) { }
});
```

### 2. Missing Error Handling

```java
// ❌ Bad: No error handling
kafkaProducerService.send(topic, message);  // What if it fails?

// ✅ Good: Handle failures
kafkaProducerService.send(topic, message, new ProducerCallback<>() {
    @Override
    public void onSuccess(SendResult result) {
        log.info("Sent successfully");
    }

    @Override
    public void onFailure(Throwable ex) {
        log.error("Send failed", ex);
        handleFailure(message);
    }
});
```

### 3. Incorrect Key Selection

```java
// ❌ Bad: Random keys break ordering
String randomKey = UUID.randomUUID().toString();
kafkaProducerService.send(topic, randomKey, message);

// ✅ Good: Meaningful keys ensure ordering
String userKey = "USER-" + userId;
kafkaProducerService.send(topic, userKey, message);
```

---

## See Also

- [Quick Reference](/kafka/getting-started/quick-reference) - API cheat sheet
- [Consumer Guide](/kafka/guides/consumer-guide) - Consumer patterns
- [Batch Operations](/kafka/guides/batch-operations) - Batch processing
- [Configuration](/kafka/guides/configuration) - Producer configuration
- [Error Handling](/kafka/guides/error-handling) - Error strategies
- [Best Practices](/kafka/guides/best-practices) - Production patterns

---

**Last Updated**: 2026-01-21
