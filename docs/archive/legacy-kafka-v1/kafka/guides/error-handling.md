# Error Handling

Comprehensive guide to error handling strategies in SmartAdmin's Kafka integration, including DLQ routing, retry patterns, and recovery strategies.

## Overview

SmartAdmin provides multi-layered error handling:

1. **Automatic DLQ Routing**: Failed messages sent to Dead Letter Queue
2. **Graceful Degradation**: Batch failures fall back to single-message processing
3. **Retry Configuration**: Producer-level automatic retries
4. **Exception Classification**: Categorize errors for appropriate handling
5. **Circuit Breakers**: Prevent cascading failures

---

## Automatic DLQ Routing

### AbstractKafkaListener Integration

The easiest error handling pattern - extend `AbstractKafkaListener`:

```java
@Component
@Slf4j
public class OrderListener extends AbstractKafkaListener {

    @Autowired
    private OrderService orderService;

    @KafkaListener(
        topics = KafkaConst.Topic.ORDER,
        groupId = KafkaConst.Group.ORDER
    )
    public void onOrderMessage(ConsumerRecord<String, String> record) {
        handleMessage(record);  // Automatic DLQ on exception
    }

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        // Your business logic
        OrderDTO order = JSON.parseObject(record.value(), OrderDTO.class);
        orderService.processOrder(order);

        // Any exception thrown here:
        // 1. Logged with full stack trace
        // 2. Sent to DLQ automatically
        // 3. Original offset committed (message handled)
    }
}
```

**What Happens on Exception**:
```mermaid
flowchart LR
    A[doHandle throws] --> B[AbstractKafkaListener catches]
    B --> C[Log error + stack trace]
    C --> D[Create DeadLetterMessage]
    D --> E[Send to {topic}-dlq]
    E --> F[Commit offset]
    F --> G[Continue processing]
```

**Benefits**:
- ✅ **Zero boilerplate**: No try-catch needed
- ✅ **No message loss**: Failed messages preserved
- ✅ **Automatic recovery**: DLQ messages can be replayed
- ✅ **Audit trail**: Full error context preserved

---

## Exception Classification

### Error Categories

```java
public enum ErrorType {
    /**
     * Transient errors - may succeed on retry
     * Examples: Connection timeout, service unavailable
     */
    TRANSIENT,

    /**
     * Data errors - require data fix before retry
     * Examples: Invalid JSON, missing required fields
     */
    DATA_ERROR,

    /**
     * Business errors - violate business rules
     * Examples: Insufficient balance, duplicate order
     */
    BUSINESS_ERROR,

    /**
     * Permanent errors - cannot be retried
     * Examples: Record not found (deleted), unsupported operation
     */
    PERMANENT
}
```

### Custom Error Handling by Type

```java
@Component
@Slf4j
public class ClassifiedErrorListener extends AbstractKafkaListener {

    @Autowired
    private AlertService alertService;
    @Autowired
    private OrderService orderService;

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        try {
            OrderDTO order = JSON.parseObject(record.value(), OrderDTO.class);
            orderService.processOrder(order);
        } catch (Exception e) {
            ErrorType errorType = classifyError(e);

            switch (errorType) {
                case TRANSIENT -> {
                    log.warn("Transient error: {}", e.getMessage());
                    // Will be sent to DLQ, can retry after service recovers
                }
                case DATA_ERROR -> {
                    log.error("Data error: {}", e.getMessage());
                    alertService.sendAlert("Invalid message format: " + record.key());
                }
                case BUSINESS_ERROR -> {
                    log.warn("Business error: {}", e.getMessage());
                    // Business team needs to review
                }
                case PERMANENT -> {
                    log.error("Permanent error: {}", e.getMessage());
                    // Discard or archive
                }
            }

            // Re-throw to trigger DLQ routing
            throw e;
        }
    }

    private ErrorType classifyError(Exception e) {
        String message = e.getMessage();
        String className = e.getClass().getSimpleName();

        if (message.contains("timeout") || message.contains("unavailable")) {
            return ErrorType.TRANSIENT;
        }
        if (className.contains("JsonParseException") || message.contains("required")) {
            return ErrorType.DATA_ERROR;
        }
        if (e instanceof BusinessException) {
            return ErrorType.BUSINESS_ERROR;
        }
        return ErrorType.PERMANENT;
    }
}
```

---

## Producer Retry Configuration

### Automatic Producer Retries

```yaml
smart:
  kafka:
    producer:
      # Retry configuration
      retries: 3                    # Retry 3 times before giving up
      retry-backoff-ms: 100         # 100ms between retries

      # Delivery guarantees
      acks: all                     # Wait for all replicas
      enable-idempotence: true      # Exactly-once semantics

      # Timeout
      request-timeout-ms: 30000     # 30s request timeout
```

### Custom Retry Logic

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ResilientProducer {

    private final KafkaProducerService kafkaProducerService;
    private final FailedMessageRepository failedMessageRepo;

    public void sendWithRetry(String topic, String key, String message, int maxAttempts) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                kafkaProducerService.send(topic, key, message);
                log.info("Message sent successfully on attempt {}", attempt);
                return;  // Success
            } catch (Exception e) {
                log.warn("Send failed on attempt {}: {}", attempt, e.getMessage());

                if (attempt == maxAttempts) {
                    // Max retries reached, persist for later
                    saveFailedMessage(topic, key, message, e);
                    throw new BusinessException("Message send failed after " + maxAttempts + " attempts");
                }

                // Exponential backoff
                sleep(100L * (1L << (attempt - 1)));  // 100ms, 200ms, 400ms...
            }
        }
    }

    private void saveFailedMessage(String topic, String key, String message, Exception e) {
        FailedMessage failed = new FailedMessage();
        failed.setTopic(topic);
        failed.setKey(key);
        failed.setMessage(message);
        failed.setError(e.getMessage());
        failed.setAttempts(0);
        failedMessageRepo.save(failed);
    }
}
```

---

## Circuit Breaker Pattern

### Resilience4j Integration

**Dependency**:
```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot2</artifactId>
    <version>1.7.1</version>
</dependency>
```

**Configuration**:
```yaml
resilience4j:
  circuitbreaker:
    instances:
      orderService:
        failure-rate-threshold: 50        # Open at 50% failure rate
        wait-duration-in-open-state: 60s  # Wait 60s before retry
        sliding-window-size: 10           # Last 10 calls
        permitted-calls-in-half-open: 5   # Test with 5 calls
```

**Usage**:
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ResilientOrderService {

    private final OrderDao orderDao;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @CircuitBreaker(name = "orderService", fallbackMethod = "processOrderFallback")
    public void processOrder(OrderDTO order) {
        // Database operation (may fail)
        orderDao.insert(order);
    }

    public void processOrderFallback(OrderDTO order, Exception e) {
        log.error("Circuit breaker fallback triggered: {}", e.getMessage());

        // Fallback logic:
        // 1. Save to local cache
        // 2. Send to DLQ
        // 3. Return gracefully
        throw new BusinessException("Service temporarily unavailable");
    }
}
```

---

## Consumer Error Strategies

### Strategy 1: Fail Fast with DLQ

**Best for**: Transient errors, data quality issues

```java
@Component
@Slf4j
public class FailFastListener extends AbstractKafkaListener {

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        // Parse and validate
        OrderDTO order = JSON.parseObject(record.value(), OrderDTO.class);

        if (!isValid(order)) {
            throw new BusinessException("Invalid order data");  // → DLQ
        }

        // Process (may throw)
        processOrder(order);  // → DLQ on exception
    }
}
```

### Strategy 2: Skip Invalid Messages

**Best for**: Non-critical data, logging/analytics

```java
@Component
@Slf4j
public class SkipInvalidListener extends AbstractKafkaListener {

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        try {
            OrderDTO order = JSON.parseObject(record.value(), OrderDTO.class);

            if (!isValid(order)) {
                log.warn("Skipping invalid order: {}", record.key());
                return;  // Skip, offset committed
            }

            processOrder(order);
        } catch (JSONException e) {
            log.error("Skipping unparseable message: {}", record.value());
            return;  // Skip, offset committed
        }
    }
}
```

### Strategy 3: Partial Processing with Degradation

**Best for**: Batch operations, high throughput

```java
@Component
@Slf4j
public class OrderBatchListener extends AbstractBatchKafkaListener<OrderDTO> {

    @Override
    protected void doBatchHandle(List<ConsumerRecord<String, String>> records) {
        // Try batch operation
        List<OrderDTO> orders = parseOrders(records);
        orderService.batchInsert(orders);  // May throw
    }

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        // Fallback: single-message processing
        OrderDTO order = JSON.parseObject(record.value(), OrderDTO.class);
        orderService.insert(order);  // May throw → DLQ
    }
}
```

---

## DLQ Processing Strategies

### Manual DLQ Review

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class DLQProcessor {

    private final KafkaProducerService kafkaProducerService;
    private final DeadLetterRepository dlqRepo;

    /**
     * Manually review DLQ messages and retry
     */
    public void processDLQ(String dlqTopic) {
        List<DeadLetterMessage> messages = fetchFromDLQ(dlqTopic);

        for (DeadLetterMessage dlqMsg : messages) {
            ErrorType errorType = classifyError(dlqMsg);

            switch (errorType) {
                case TRANSIENT -> retryMessage(dlqMsg);
                case DATA_ERROR -> fixAndRetry(dlqMsg);
                case BUSINESS_ERROR -> escalateToTeam(dlqMsg);
                case PERMANENT -> archiveMessage(dlqMsg);
            }
        }
    }

    private void retryMessage(DeadLetterMessage dlqMsg) {
        // Republish to original topic
        kafkaProducerService.send(
            dlqMsg.getOriginalTopic(),
            dlqMsg.getOriginalKey(),
            dlqMsg.getOriginalValue()
        );
        log.info("Retried message from DLQ: {}", dlqMsg.getOriginalKey());
    }
}
```

### Automatic DLQ Retry (Exponential Backoff)

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AutoDLQRetry {

    private final KafkaProducerService kafkaProducerService;

    @Scheduled(cron = "0 */10 * * * *")  // Every 10 minutes
    public void retryTransientFailures() {
        List<DeadLetterMessage> transientErrors = findTransientErrors();

        for (DeadLetterMessage dlqMsg : transientErrors) {
            int attemptCount = dlqMsg.getAttemptCount();

            if (attemptCount >= 5) {
                log.warn("Max retries exceeded: {}", dlqMsg.getOriginalKey());
                continue;
            }

            // Exponential backoff
            long delayMs = calculateBackoff(attemptCount);
            if (System.currentTimeMillis() - dlqMsg.getTimestamp() < delayMs) {
                continue;  // Not yet time to retry
            }

            // Retry
            try {
                kafkaProducerService.send(
                    dlqMsg.getOriginalTopic(),
                    dlqMsg.getOriginalKey(),
                    dlqMsg.getOriginalValue()
                );
                deleteDLQMessage(dlqMsg);
                log.info("Auto-retry successful: {}", dlqMsg.getOriginalKey());
            } catch (Exception e) {
                updateAttemptCount(dlqMsg, attemptCount + 1);
                log.warn("Auto-retry failed: {}", dlqMsg.getOriginalKey());
            }
        }
    }

    private long calculateBackoff(int attemptCount) {
        // 1min, 2min, 4min, 8min, 16min
        return 60000L * (1L << attemptCount);
    }
}
```

---

## Monitoring and Alerting

### Error Metrics

```java
@Component
@RequiredArgsConstructor
public class ErrorMetrics {

    private final MeterRegistry meterRegistry;

    public void recordConsumerError(String topic, ErrorType errorType) {
        meterRegistry.counter("kafka.consumer.errors.total",
            "topic", topic,
            "error_type", errorType.name()
        ).increment();
    }

    public void recordDLQMessage(String topic) {
        meterRegistry.counter("kafka.dlq.messages.total",
            "topic", topic
        ).increment();
    }

    public void recordRetrySuccess(String topic) {
        meterRegistry.counter("kafka.retry.success",
            "topic", topic
        ).increment();
    }
}
```

### Alerting Rules

```yaml
# Prometheus Alert Rules
groups:
  - name: kafka_errors
    rules:
      - alert: HighErrorRate
        expr: rate(kafka_consumer_errors_total[5m]) > 10
        for: 5m
        annotations:
          summary: "Kafka consumer error rate > 10/s for 5 minutes"

      - alert: DLQAccumulation
        expr: kafka_dlq_messages_total > 100
        for: 10m
        annotations:
          summary: "DLQ has > 100 messages for 10 minutes"
```

---

## Best Practices

### 1. Distinguish Error Types

```java
// ✅ Good: Specific exception types
if (order.getAmount() == null) {
    throw new ValidationException("Amount is required");
}
if (inventory < order.getQuantity()) {
    throw new BusinessException("Insufficient inventory");
}

// ❌ Bad: Generic exceptions
if (order.getAmount() == null) {
    throw new RuntimeException("Error");  // Too generic
}
```

### 2. Log with Context

```java
// ✅ Good: Rich context
log.error("Order processing failed - Key: {}, Partition: {}, Offset: {}, Error: {}",
    record.key(), record.partition(), record.offset(), e.getMessage(), e);

// ❌ Bad: Minimal context
log.error("Error", e);  // No context
```

### 3. Don't Swallow Exceptions

```java
// ❌ Bad: Exception swallowed
@Override
protected void doHandle(ConsumerRecord<String, String> record) {
    try {
        processOrder(record.value());
    } catch (Exception e) {
        log.error("Error", e);  // Logged but not handled!
    }
    // Offset committed, message lost!
}

// ✅ Good: Let framework handle
@Override
protected void doHandle(ConsumerRecord<String, String> record) {
    processOrder(record.value());  // Exception propagates to DLQ
}
```

### 4. Implement Idempotency

```java
@Override
protected void doHandle(ConsumerRecord<String, String> record) {
    String orderId = record.key();

    // Check if already processed
    if (orderDao.exists(orderId)) {
        log.info("Order already processed: {}", orderId);
        return;  // Skip duplicate
    }

    // Process order
    OrderDTO order = JSON.parseObject(record.value(), OrderDTO.class);
    orderDao.insert(order);
}
```

---

## See Also

- [Dead Letter Queue](/kafka/architecture/dead-letter-queue) - DLQ architecture
- [Consumer Guide](/kafka/guides/consumer-guide) - Consumer patterns
- [Batch Operations](/kafka/guides/batch-operations) - Batch error handling
- [Troubleshooting](/kafka/troubleshooting/common-issues) - Common issues
- [Best Practices](/kafka/guides/best-practices) - Production patterns

---

**Last Updated**: 2026-01-21
