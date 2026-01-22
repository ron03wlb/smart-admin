# API Reference

Complete API documentation for SmartAdmin Kafka integration.

## Overview

This reference documents all public APIs, classes, and methods for the SmartAdmin Kafka integration framework.

**Package structure**:
```
net.lab1024.sa.common.mq.kafka
├── config/              # Configuration classes
├── domain/              # Domain objects (DTOs, entities)
├── listener/            # Base listener classes
├── service/             # Producer services
└── util/                # Utility classes
```

**Core components**:
- **KafkaProducerService** - Service for sending messages to Kafka topics
- **AbstractKafkaListener** - Base class for single-message consumers
- **AbstractBatchKafkaListener** - Base class for batch consumers
- **KafkaAutoConfiguration** - Auto-configuration for Kafka components
- **DeadLetterMessage** - Domain object for DLQ messages

---

## KafkaProducerService

**Package**: `net.lab1024.sa.common.mq.kafka.service`

**Description**: Service for producing messages to Kafka topics with support for synchronous, asynchronous, and batch sending.

**Class declaration**:
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {
    private final KafkaTemplate<String, String> kafkaTemplate;
}
```

---

### send()

Send a message synchronously to a Kafka topic.

**Signature**:
```java
public void send(String topic, String key, String value)
```

**Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `topic` | String | The Kafka topic name (required) |
| `key` | String | The message key for partitioning (nullable) |
| `value` | String | The message payload (required) |

**Behavior**:
- Blocks until message is acknowledged by broker
- Throws exception on failure
- Logs send operation with topic, key, partition, and offset
- Suitable for critical operations requiring immediate feedback

**Exceptions**:
- `KafkaException` - If send fails after retries
- `IllegalArgumentException` - If topic or value is null

**Example**:
```java
@Service
@RequiredArgsConstructor
public class OrderService {
    private final KafkaProducerService kafkaProducerService;

    public void createOrder(OrderDTO order) {
        String orderJson = JSON.toJSONString(order);
        kafkaProducerService.send("smart-admin-order", order.getOrderId(), orderJson);
        log.info("Order sent to Kafka");
    }
}
```

**See also**:
- [sendAsync()](#sendasync) - For non-blocking sends
- [sendBatchAsync()](#sendbatchasync) - For batch sends

---

### sendAsync()

Send a message asynchronously to a Kafka topic.

**Signature**:
```java
public CompletableFuture<SendResult<String, String>> sendAsync(
    String topic,
    String key,
    String value
)
```

**Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `topic` | String | The Kafka topic name (required) |
| `key` | String | The message key for partitioning (nullable) |
| `value` | String | The message payload (required) |

**Returns**:
- `CompletableFuture<SendResult<String, String>>` - Future containing send result with metadata (partition, offset, timestamp)

**Behavior**:
- Returns immediately without blocking
- Result available via `CompletableFuture`
- Suitable for high-throughput scenarios
- Failures available via future exception handling

**Example**:
```java
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final KafkaProducerService kafkaProducerService;

    public void sendNotification(NotificationDTO notification) {
        String json = JSON.toJSONString(notification);

        kafkaProducerService.sendAsync("smart-admin-notification", notification.getUserId(), json)
            .thenAccept(result -> {
                log.info("✅ Notification sent | Partition: {} | Offset: {}",
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            })
            .exceptionally(ex -> {
                log.error("❌ Failed to send notification", ex);
                return null;
            });
    }
}
```

**See also**:
- [send()](#send) - For synchronous sends
- [sendBatchAsync()](#sendbatchasync) - For batch sends

---

### sendBatchAsync()

Send multiple messages asynchronously to a Kafka topic.

**Signature**:
```java
public List<CompletableFuture<SendResult<String, String>>> sendBatchAsync(
    String topic,
    List<String> messages
)
```

**Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `topic` | String | The Kafka topic name (required) |
| `messages` | List<String> | List of message payloads (required) |

**Returns**:
- `List<CompletableFuture<SendResult<String, String>>>` - List of futures, one per message

**Behavior**:
- Sends all messages asynchronously in parallel
- Returns list of futures immediately
- Each message gets an auto-generated UUID key
- Suitable for bulk operations (imports, migrations)

**Example**:
```java
@Service
@RequiredArgsConstructor
public class EmployeeImportService {
    private final KafkaProducerService kafkaProducerService;

    public void importEmployees(List<EmployeeDTO> employees) {
        List<String> messages = employees.stream()
            .map(JSON::toJSONString)
            .toList();

        List<CompletableFuture<SendResult<String, String>>> futures =
            kafkaProducerService.sendBatchAsync("smart-admin-employee-import", messages);

        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> log.info("✅ Batch import complete | Count: {}", employees.size()))
            .exceptionally(ex -> {
                log.error("❌ Batch import failed", ex);
                return null;
            });
    }
}
```

**Performance note**: Batch sending is 6x faster than individual sends for large datasets (>100 messages).

**See also**:
- [sendAsync()](#sendasync) - For single async sends
- [Batch Operations Guide](/kafka/guides/batch-operations) - Detailed batch patterns

---

### sendWithKey()

Send a message with explicit key for partition assignment.

**Signature**:
```java
public void sendWithKey(String topic, String key, String value)
```

**Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `topic` | String | The Kafka topic name (required) |
| `key` | String | The partition key (required, not null) |
| `value` | String | The message payload (required) |

**Behavior**:
- Ensures message goes to partition based on key hash
- Messages with same key go to same partition (ordering)
- Blocks until acknowledged

**Example**:
```java
// Ensure all orders for same customer go to same partition
kafkaProducerService.sendWithKey(
    "smart-admin-order",
    customerId,  // Key ensures ordering per customer
    orderJson
);
```

---

## AbstractKafkaListener

**Package**: `net.lab1024.sa.common.mq.kafka.listener`

**Description**: Base class for Kafka message listeners providing error handling, DLQ routing, and lifecycle hooks.

**Class declaration**:
```java
@Slf4j
public abstract class AbstractKafkaListener {
    @Autowired
    private KafkaProducerService kafkaProducerService;
}
```

**Usage pattern**:
```java
@Component
@RequiredArgsConstructor
public class OrderListener extends AbstractKafkaListener {

    @KafkaListener(topics = "smart-admin-order", groupId = "order-processor")
    public void onMessage(ConsumerRecord<String, String> record) {
        handleMessage(record);  // Calls base class method
    }

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        // Your business logic here
    }
}
```

---

### handleMessage()

Main message handling method with error handling and DLQ routing.

**Signature**:
```java
protected void handleMessage(ConsumerRecord<String, String> record)
```

**Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `record` | ConsumerRecord<String, String> | The consumed Kafka record |

**Behavior**:
1. Calls `beforeHandle()` lifecycle hook
2. Calls `doHandle()` with business logic
3. Calls `afterHandle()` lifecycle hook
4. On exception: calls `handleError()` which sends to DLQ
5. Logs all operations with structured fields

**Exception handling**:
- Catches all exceptions from `doHandle()`
- Logs error with full context
- Routes failed message to DLQ topic (`{topic}-dlq`)
- Does not rethrow (prevents consumer crash)

**Example**:
```java
@Component
public class PaymentListener extends AbstractKafkaListener {

    @KafkaListener(topics = "smart-admin-payment", groupId = "payment-processor")
    public void onMessage(ConsumerRecord<String, String> record) {
        handleMessage(record);  // Handles errors automatically
    }

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        PaymentDTO payment = JSON.parseObject(record.value(), PaymentDTO.class);
        processPayment(payment);  // If this throws, message goes to DLQ
    }
}
```

**See also**:
- [doHandle()](#dohandle) - Business logic implementation
- [handleError()](#handleerror) - Error handling customization

---

### doHandle()

Abstract method for implementing business logic.

**Signature**:
```java
protected abstract void doHandle(ConsumerRecord<String, String> record)
```

**Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `record` | ConsumerRecord<String, String> | The consumed Kafka record |

**Implementation requirements**:
- Must be overridden by subclass
- Should contain only business logic
- Throw exceptions for errors (caught by `handleMessage()`)
- Should be idempotent

**Example**:
```java
@Override
protected void doHandle(ConsumerRecord<String, String> record) {
    // 1. Deserialize
    OrderDTO order = JSON.parseObject(record.value(), OrderDTO.class);

    // 2. Validate
    if (order.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
        throw new BusinessException("Invalid order amount");  // Goes to DLQ
    }

    // 3. Process
    orderService.processOrder(order);

    // 4. Log
    log.info("✅ Order processed | OrderId: {}", order.getOrderId());
}
```

---

### handleError()

Error handling method that routes failed messages to DLQ.

**Signature**:
```java
protected void handleError(ConsumerRecord<String, String> record, Exception e)
```

**Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `record` | ConsumerRecord<String, String> | The failed record |
| `e` | Exception | The exception that occurred |

**Default behavior**:
1. Logs error with full context (topic, partition, offset, key)
2. Creates `DeadLetterMessage` with error details
3. Sends to DLQ topic: `{originalTopic}-dlq`
4. Does not rethrow exception

**Customization**:
Override to implement custom error handling logic.

**Example**:
```java
@Override
protected void handleError(ConsumerRecord<String, String> record, Exception e) {
    // Custom logic: retry transient errors, DLQ permanent errors
    if (e instanceof TimeoutException) {
        log.warn("Transient error, will retry on rebalance");
        throw e;  // Rethrow to trigger reprocessing
    } else {
        log.error("Permanent error, sending to DLQ");
        super.handleError(record, e);  // Use default DLQ routing
    }
}
```

**See also**:
- [DeadLetterMessage](#deadlettermessage) - DLQ message structure
- [Error Handling Guide](/kafka/guides/error-handling) - Error patterns

---

### beforeHandle()

Lifecycle hook called before message processing.

**Signature**:
```java
protected void beforeHandle(ConsumerRecord<String, String> record)
```

**Default behavior**: Empty (no-op)

**Use cases**:
- Request ID generation
- MDC context setup
- Distributed tracing
- Metrics collection

**Example**:
```java
@Override
protected void beforeHandle(ConsumerRecord<String, String> record) {
    String requestId = UUID.randomUUID().toString();
    MDC.put("requestId", requestId);
    MDC.put("topic", record.topic());
    MDC.put("partition", String.valueOf(record.partition()));

    log.info("🔵 Processing started | Key: {}", record.key());
}
```

---

### afterHandle()

Lifecycle hook called after successful message processing.

**Signature**:
```java
protected void afterHandle(ConsumerRecord<String, String> record)
```

**Default behavior**: Empty (no-op)

**Use cases**:
- MDC cleanup
- Metrics recording
- Audit logging
- Cache invalidation

**Example**:
```java
@Override
protected void afterHandle(ConsumerRecord<String, String> record) {
    log.info("🟢 Processing completed | Key: {}", record.key());

    // Record metrics
    meterRegistry.counter("kafka.consumer.processed",
        "topic", record.topic(),
        "status", "success"
    ).increment();

    // Cleanup
    MDC.clear();
}
```

---

## AbstractBatchKafkaListener

**Package**: `net.lab1024.sa.common.mq.kafka.listener`

**Description**: Base class for batch message processing with graceful degradation to single-message processing on failure.

**Class declaration**:
```java
@Slf4j
public abstract class AbstractBatchKafkaListener<T> extends AbstractKafkaListener {
}
```

**Generic type parameter**:
- `T` - The domain object type (e.g., `OrderDTO`, `EmployeeDTO`)

**Usage pattern**:
```java
@Component
public class OrderBatchListener extends AbstractBatchKafkaListener<OrderDTO> {

    @KafkaListener(topics = "smart-admin-order-batch", groupId = "order-batch-processor")
    public void onBatchMessage(List<ConsumerRecord<String, String>> records) {
        handleBatchMessage(records);  // Calls base class method
    }

    @Override
    protected void doBatchHandle(List<ConsumerRecord<String, String>> records) {
        // Batch processing logic
    }
}
```

---

### handleBatchMessage()

Main batch processing method with graceful degradation.

**Signature**:
```java
protected void handleBatchMessage(List<ConsumerRecord<String, String>> records)
```

**Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `records` | List<ConsumerRecord<String, String>> | List of consumed records |

**Behavior**:
1. Attempts batch processing via `doBatchHandle()`
2. On failure: Falls back to single-message processing via `doHandle()`
3. Logs batch size and processing results
4. Individual failures in fallback mode go to DLQ

**Graceful degradation**:
```
Try: Batch processing (fast, 100 messages at once)
  ↓ (on error)
Fallback: Individual processing (slower, 1 at a time)
  ↓ (on error)
DLQ: Failed messages
```

**Example**:
```java
@Component
public class EmployeeImportListener extends AbstractBatchKafkaListener<EmployeeDTO> {

    @KafkaListener(
        topics = "smart-admin-employee-import",
        groupId = "employee-import-batch"
    )
    public void onBatchMessage(List<ConsumerRecord<String, String>> records) {
        handleBatchMessage(records);  // Handles batch + fallback automatically
    }

    @Override
    protected void doBatchHandle(List<ConsumerRecord<String, String>> records) {
        List<EmployeeDTO> employees = records.stream()
            .map(record -> JSON.parseObject(record.value(), EmployeeDTO.class))
            .toList();

        employeeManager.batchInsert(employees);  // Single DB call for 100 records
        log.info("✅ Batch imported | Count: {}", employees.size());
    }

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        // Fallback: process one at a time
        EmployeeDTO employee = JSON.parseObject(record.value(), EmployeeDTO.class);
        employeeManager.insert(employee);
    }
}
```

**Performance**: Batch processing is typically 6-10x faster than individual processing.

---

### doBatchHandle()

Abstract method for batch processing logic.

**Signature**:
```java
protected abstract void doBatchHandle(List<ConsumerRecord<String, String>> records)
```

**Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `records` | List<ConsumerRecord<String, String>> | Batch of consumed records |

**Implementation requirements**:
- Process all records in batch (single DB transaction, single API call)
- Throw exception on batch-level failures
- Be idempotent (same batch can be reprocessed)

**Example**:
```java
@Override
protected void doBatchHandle(List<ConsumerRecord<String, String>> records) {
    log.info("📦 Processing batch | Size: {}", records.size());

    List<OrderDTO> orders = records.stream()
        .map(record -> JSON.parseObject(record.value(), OrderDTO.class))
        .toList();

    // Single database transaction for all orders
    orderManager.batchInsert(orders);
}
```

---

## Domain Objects

### DeadLetterMessage

**Package**: `net.lab1024.sa.common.mq.kafka.domain`

**Description**: Domain object representing a failed message in the Dead Letter Queue.

**Class declaration**:
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeadLetterMessage {
    private String originalTopic;
    private Integer originalPartition;
    private Long originalOffset;
    private String originalKey;
    private String originalValue;
    private String errorMessage;
    private String errorStackTrace;
    private LocalDateTime failedAt;
}
```

**Fields**:
| Field | Type | Description |
|-------|------|-------------|
| `originalTopic` | String | The topic where the message failed |
| `originalPartition` | Integer | The partition number |
| `originalOffset` | Long | The message offset |
| `originalKey` | String | The original message key |
| `originalValue` | String | The original message payload |
| `errorMessage` | String | The error message |
| `errorStackTrace` | String | Full exception stack trace |
| `failedAt` | LocalDateTime | Timestamp of failure |

**Usage**:
```java
// Automatically created by AbstractKafkaListener.handleError()
DeadLetterMessage dlqMessage = DeadLetterMessage.builder()
    .originalTopic(record.topic())
    .originalPartition(record.partition())
    .originalOffset(record.offset())
    .originalKey(record.key())
    .originalValue(record.value())
    .errorMessage(e.getMessage())
    .errorStackTrace(getStackTrace(e))
    .failedAt(LocalDateTime.now())
    .build();
```

**See also**:
- [DLQ Example](/kafka/examples/dlq-example) - Complete DLQ handling
- [Error Handling Guide](/kafka/guides/error-handling) - Error strategies

---

## Configuration Classes

### KafkaAutoConfiguration

**Package**: `net.lab1024.sa.common.mq.kafka.config`

**Description**: Auto-configuration class for Kafka components.

**Conditional activation**:
```java
@Configuration
@ConditionalOnProperty(prefix = "smart.kafka", name = "enabled", havingValue = "true")
public class KafkaAutoConfiguration {
}
```

**Beans provided**:
- `KafkaTemplate<String, String>` - Template for sending messages
- `KafkaProducerService` - Producer service wrapper
- `ConsumerFactory` - Factory for creating consumers
- `KafkaListenerContainerFactory` - Container factory for listeners

**Configuration properties**:
```yaml
smart:
  kafka:
    enabled: true                      # Enable/disable Kafka
    bootstrap-servers: localhost:9092  # Kafka broker addresses
```

---

### KafkaProperties

**Package**: `net.lab1024.sa.common.mq.kafka.config`

**Description**: Configuration properties for Kafka integration.

**Class declaration**:
```java
@Data
@ConfigurationProperties(prefix = "smart.kafka")
public class KafkaProperties {
    private Boolean enabled = false;
    private String bootstrapServers;
    private ProducerProperties producer;
    private ConsumerProperties consumer;
}
```

**Properties**:
| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `enabled` | Boolean | false | Enable Kafka integration |
| `bootstrap-servers` | String | - | Comma-separated broker addresses |
| `producer.*` | ProducerProperties | - | Producer configuration |
| `consumer.*` | ConsumerProperties | - | Consumer configuration |

**Example configuration**:
```yaml
smart:
  kafka:
    enabled: true
    bootstrap-servers: kafka-1:9092,kafka-2:9092,kafka-3:9092
    producer:
      acks: all
      retries: 3
      enable-idempotence: true
    consumer:
      enable-auto-commit: false
      auto-offset-reset: earliest
```

---

## Utility Classes

### KafkaConst

**Package**: `net.lab1024.sa.common.mq.kafka.constant`

**Description**: Constants for Kafka topics and consumer groups.

**Example**:
```java
public class KafkaConst {

    public static class Topic {
        public static final String ORDER = "smart-admin-order";
        public static final String PAYMENT = "smart-admin-payment";
        public static final String NOTIFICATION = "smart-admin-notification";

        // DLQ topics
        public static final String ORDER_DLQ = ORDER + "-dlq";
        public static final String PAYMENT_DLQ = PAYMENT + "-dlq";
    }

    public static class ConsumerGroup {
        public static final String ORDER_PROCESSOR = "order-processor";
        public static final String PAYMENT_PROCESSOR = "payment-processor";
        public static final String NOTIFICATION_PROCESSOR = "notification-processor";
    }
}
```

**Usage**:
```java
@KafkaListener(
    topics = KafkaConst.Topic.ORDER,
    groupId = KafkaConst.ConsumerGroup.ORDER_PROCESSOR
)
public void onMessage(ConsumerRecord<String, String> record) {
    handleMessage(record);
}
```

---

## Complete API Example

**Producer + Consumer integration**:

```java
// ===== Producer Service =====
@Service
@RequiredArgsConstructor
public class OrderService {
    private final KafkaProducerService kafkaProducerService;

    public void createOrder(OrderDTO order) {
        String orderJson = JSON.toJSONString(order);

        kafkaProducerService.sendAsync(
            KafkaConst.Topic.ORDER,
            order.getOrderId(),
            orderJson
        ).thenAccept(result -> {
            log.info("✅ Order sent | Partition: {} | Offset: {}",
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
        });
    }
}

// ===== Consumer Listener =====
@Component
@RequiredArgsConstructor
public class OrderListener extends AbstractKafkaListener {
    private final OrderProcessingService orderProcessingService;

    @KafkaListener(
        topics = KafkaConst.Topic.ORDER,
        groupId = KafkaConst.ConsumerGroup.ORDER_PROCESSOR
    )
    public void onMessage(ConsumerRecord<String, String> record) {
        handleMessage(record);
    }

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        OrderDTO order = JSON.parseObject(record.value(), OrderDTO.class);
        orderProcessingService.process(order);
        log.info("✅ Order processed | OrderId: {}", order.getOrderId());
    }

    @Override
    protected void beforeHandle(ConsumerRecord<String, String> record) {
        MDC.put("orderId", record.key());
    }

    @Override
    protected void afterHandle(ConsumerRecord<String, String> record) {
        MDC.clear();
    }
}

// ===== DLQ Consumer =====
@Component
@RequiredArgsConstructor
public class OrderDLQListener extends AbstractKafkaListener {
    private final OrderDLQService orderDLQService;

    @KafkaListener(
        topics = KafkaConst.Topic.ORDER_DLQ,
        groupId = "order-dlq-processor"
    )
    public void onDLQMessage(ConsumerRecord<String, String> record) {
        handleMessage(record);
    }

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        DeadLetterMessage dlqMessage = JSON.parseObject(
            record.value(),
            DeadLetterMessage.class
        );

        orderDLQService.analyzeAndRetry(dlqMessage);
    }
}
```

---

## Method Comparison

### Send Methods Comparison

| Method | Blocking | Return Type | Use Case | Performance |
|--------|----------|-------------|----------|-------------|
| `send()` | ✅ Yes | void | Critical operations | Slower |
| `sendAsync()` | ❌ No | CompletableFuture | High throughput | Fast |
| `sendBatchAsync()` | ❌ No | List<CompletableFuture> | Bulk operations | Fastest (6x) |

### Listener Methods Comparison

| Method | Batch | Fallback | Use Case |
|--------|-------|----------|----------|
| `AbstractKafkaListener` | ❌ No | N/A | Single message processing |
| `AbstractBatchKafkaListener` | ✅ Yes | ✅ Yes | Batch + graceful degradation |

---

## See Also

- [Quick Reference](/kafka/getting-started/quick-reference) - Quick API lookup
- [Configuration Reference](/kafka/reference/configuration-reference) - Complete config properties
- [Producer Guide](/kafka/guides/producer-guide) - Producer patterns
- [Consumer Guide](/kafka/guides/consumer-guide) - Consumer patterns
- [Error Handling](/kafka/guides/error-handling) - Error handling strategies

---

**Last Updated**: 2026-01-22
