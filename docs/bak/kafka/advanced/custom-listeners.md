# Custom Listeners

Advanced guide to developing custom Kafka message listeners in SmartAdmin.

## Overview

**SmartAdmin provides**:
- `AbstractKafkaListener` - Base class for single-message processing
- `AbstractBatchKafkaListener` - Base class for batch processing with graceful degradation

**When to create custom listeners**:
- Specific error handling strategies
- Custom retry logic
- Message filtering and routing
- Performance monitoring hooks
- Special business requirements

---

## Understanding AbstractKafkaListener

### Design Pattern

**Template Method Pattern**:
```java
public abstract class AbstractKafkaListener {

    // Template method (final - cannot override)
    protected final void handleMessage(ConsumerRecord<String, String> record) {
        try {
            doHandle(record);  // Subclass implements
        } catch (Exception e) {
            handleError(record, e);  // Automatic DLQ routing
        }
    }

    // Abstract method - subclass must implement
    protected abstract void doHandle(ConsumerRecord<String, String> record);

    // Hook method - subclass can override
    protected void handleError(ConsumerRecord<String, String> record, Exception e) {
        // Default: Send to DLQ
    }
}
```

**Benefits**:
- ✅ Consistent error handling across all consumers
- ✅ Automatic Dead Letter Queue routing
- ✅ Built-in logging
- ✅ No boilerplate code

### Source Code Analysis

**Location**: `sa-common/mq/kafka/listener/AbstractKafkaListener.java`

```java
@Slf4j
public abstract class AbstractKafkaListener {

    @Autowired(required = false)
    private KafkaProducerService kafkaProducerService;

    /**
     * Template method for message handling
     */
    protected void handleMessage(ConsumerRecord<String, String> record) {
        String topic = record.topic();
        String key = record.key();
        long offset = record.offset();
        int partition = record.partition();

        log.info("📥 Received message | Topic: {} | Partition: {} | Offset: {} | Key: {}",
            topic, partition, offset, key);

        try {
            doHandle(record);
            log.info("✅ Message processed successfully | Topic: {} | Key: {}", topic, key);

        } catch (Exception e) {
            log.error("❌ Message processing failed | Topic: {} | Key: {} | Partition: {} | Offset: {}",
                topic, key, partition, offset, e);

            handleError(record, e);
        }
    }

    /**
     * Abstract method - subclass implements business logic
     */
    protected abstract void doHandle(ConsumerRecord<String, String> record);

    /**
     * Error handling hook - subclass can override
     * Default: Send to DLQ
     */
    protected void handleError(ConsumerRecord<String, String> record, Exception e) {
        if (kafkaProducerService != null) {
            sendToDLQ(record, e);
        }
    }

    /**
     * Send failed message to Dead Letter Queue
     */
    private void sendToDLQ(ConsumerRecord<String, String> record, Exception e) {
        String dlqTopic = record.topic() + "-dlq";

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

        String dlqMessageJson = JSON.toJSONString(dlqMessage);
        kafkaProducerService.send(dlqTopic, record.key(), dlqMessageJson);

        log.warn("⚠️ Message sent to DLQ | Topic: {} | Key: {}", dlqTopic, record.key());
    }
}
```

---

## Pattern 1: Custom Error Handling

### Use Case

Different error handling strategies for different exception types:

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class CustomErrorHandlingListener extends AbstractKafkaListener {

    private final OrderService orderService;
    private final KafkaProducerService kafkaProducerService;
    private final RetryQueue retryQueue;

    @KafkaListener(
        topics = KafkaConst.Topic.ORDER,
        groupId = KafkaConst.Group.ORDER
    )
    public void onMessage(ConsumerRecord<String, String> record) {
        handleMessage(record);
    }

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        OrderDTO order = JSON.parseObject(record.value(), OrderDTO.class);
        orderService.processOrder(order);
    }

    /**
     * Custom error handling strategy
     */
    @Override
    protected void handleError(ConsumerRecord<String, String> record, Exception e) {
        String key = record.key();

        // Strategy 1: Validation errors → Send to DLQ immediately
        if (e instanceof ValidationException) {
            log.warn("Validation error, sending to DLQ | Key: {}", key);
            super.handleError(record, e);  // Use default DLQ logic
            return;
        }

        // Strategy 2: Temporary errors → Retry queue
        if (e instanceof DataAccessException || e instanceof TimeoutException) {
            log.warn("Temporary error, adding to retry queue | Key: {}", key);
            retryQueue.add(record, 3);  // Retry 3 times with backoff
            return;
        }

        // Strategy 3: Business logic errors → Alert and DLQ
        if (e instanceof BusinessException) {
            log.error("Business logic error, alerting team | Key: {}", key, e);
            alertTeam(record, e);
            super.handleError(record, e);
            return;
        }

        // Strategy 4: Unknown errors → Log and DLQ
        log.error("Unknown error, sending to DLQ | Key: {}", key, e);
        super.handleError(record, e);
    }

    private void alertTeam(ConsumerRecord<String, String> record, Exception e) {
        // Send alert to monitoring system
    }
}
```

---

## Pattern 2: Retry Logic with Exponential Backoff

### Implementation

**Retry Queue Service**:
```java
@Service
@Slf4j
@RequiredArgsConstructor
public class RetryQueueService {

    private final KafkaProducerService kafkaProducerService;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String RETRY_COUNT_KEY = "kafka:retry:count:";

    public void scheduleRetry(ConsumerRecord<String, String> record, int maxRetries) {
        String retryKey = RETRY_COUNT_KEY + record.topic() + ":" + record.key();

        // Get current retry count
        String countStr = redisTemplate.opsForValue().get(retryKey);
        int currentRetry = countStr != null ? Integer.parseInt(countStr) : 0;

        if (currentRetry >= maxRetries) {
            log.warn("Max retries exceeded, sending to DLQ | Key: {} | Retries: {}",
                record.key(), currentRetry);
            sendToDLQ(record);
            redisTemplate.delete(retryKey);
            return;
        }

        // Increment retry count
        int nextRetry = currentRetry + 1;
        redisTemplate.opsForValue().set(retryKey, String.valueOf(nextRetry), 24, TimeUnit.HOURS);

        // Calculate exponential backoff: 2^retry * 1000ms
        long delayMs = (long) Math.pow(2, nextRetry) * 1000;

        // Send to retry topic with delay header
        RetryMessage retryMessage = RetryMessage.builder()
            .originalTopic(record.topic())
            .originalKey(record.key())
            .originalValue(record.value())
            .retryCount(nextRetry)
            .scheduledFor(System.currentTimeMillis() + delayMs)
            .build();

        String retryTopic = record.topic() + "-retry";
        kafkaProducerService.send(retryTopic, record.key(), JSON.toJSONString(retryMessage));

        log.info("📤 Scheduled for retry | Key: {} | RetryCount: {} | DelayMs: {}",
            record.key(), nextRetry, delayMs);
    }
}
```

**Retry Listener**:
```java
@Component
@Slf4j
@RequiredArgsConstructor
public class RetryListener extends AbstractKafkaListener {

    private final KafkaProducerService kafkaProducerService;
    private final OrderService orderService;

    @KafkaListener(
        topics = KafkaConst.Topic.ORDER_RETRY,
        groupId = "order-retry-group"
    )
    public void onMessage(ConsumerRecord<String, String> record) {
        RetryMessage retryMessage = JSON.parseObject(record.value(), RetryMessage.class);

        // Check if scheduled time reached
        long now = System.currentTimeMillis();
        if (now < retryMessage.getScheduledFor()) {
            long waitMs = retryMessage.getScheduledFor() - now;
            log.info("⏰ Too early, rescheduling | Key: {} | WaitMs: {}",
                retryMessage.getOriginalKey(), waitMs);

            // Send back to retry topic
            kafkaProducerService.send(KafkaConst.Topic.ORDER_RETRY, record.key(), record.value());
            return;
        }

        // Retry processing
        log.info("🔁 Retrying message | Key: {} | RetryCount: {}",
            retryMessage.getOriginalKey(), retryMessage.getRetryCount());

        handleMessage(record);
    }

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        RetryMessage retryMessage = JSON.parseObject(record.value(), RetryMessage.class);
        OrderDTO order = JSON.parseObject(retryMessage.getOriginalValue(), OrderDTO.class);

        orderService.processOrder(order);
    }
}
```

---

## Pattern 3: Message Filtering and Routing

### Dynamic Message Router

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderRouterListener extends AbstractKafkaListener {

    private final Map<String, OrderProcessor> processorMap;

    @KafkaListener(
        topics = KafkaConst.Topic.ORDER,
        groupId = "order-router-group"
    )
    public void onMessage(ConsumerRecord<String, String> record) {
        handleMessage(record);
    }

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        OrderDTO order = JSON.parseObject(record.value(), OrderDTO.class);
        String orderType = order.getOrderType();

        // Route to specific processor based on order type
        OrderProcessor processor = processorMap.get(orderType);

        if (processor == null) {
            log.warn("⚠️ No processor found for order type | Type: {} | OrderId: {}",
                orderType, order.getOrderId());
            throw new BusinessException(OrderErrorCode.UNSUPPORTED_ORDER_TYPE);
        }

        log.info("🔀 Routing to processor | Type: {} | OrderId: {}",
            orderType, order.getOrderId());
        processor.process(order);
    }
}

// Processor interface
public interface OrderProcessor {
    void process(OrderDTO order);
}

// Example processors
@Component("STANDARD")
@Slf4j
public class StandardOrderProcessor implements OrderProcessor {
    @Override
    public void process(OrderDTO order) {
        log.info("Processing standard order: {}", order.getOrderId());
        // Standard order logic
    }
}

@Component("EXPRESS")
@Slf4j
public class ExpressOrderProcessor implements OrderProcessor {
    @Override
    public void process(OrderDTO order) {
        log.info("Processing express order: {}", order.getOrderId());
        // Express order logic with priority
    }
}
```

---

## Pattern 4: Conditional Message Processing

### Filter Chain Pattern

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class FilteredOrderListener extends AbstractKafkaListener {

    private final List<MessageFilter> filters;
    private final OrderService orderService;

    @KafkaListener(
        topics = KafkaConst.Topic.ORDER,
        groupId = "filtered-order-group"
    )
    public void onMessage(ConsumerRecord<String, String> record) {
        handleMessage(record);
    }

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        OrderDTO order = JSON.parseObject(record.value(), OrderDTO.class);

        // Apply filter chain
        for (MessageFilter filter : filters) {
            if (!filter.accept(order)) {
                log.info("🚫 Message filtered out | Filter: {} | OrderId: {}",
                    filter.getClass().getSimpleName(), order.getOrderId());
                return;  // Skip processing
            }
        }

        // All filters passed - process message
        orderService.processOrder(order);
    }
}

// Filter interface
public interface MessageFilter {
    boolean accept(OrderDTO order);
}

// Example filters
@Component
@Order(1)
public class DuplicateFilter implements MessageFilter {

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public boolean accept(OrderDTO order) {
        String key = "order:" + order.getOrderId();
        return !Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}

@Component
@Order(2)
public class BusinessHoursFilter implements MessageFilter {

    @Override
    public boolean accept(OrderDTO order) {
        LocalTime now = LocalTime.now();
        return now.isAfter(LocalTime.of(9, 0)) && now.isBefore(LocalTime.of(21, 0));
    }
}

@Component
@Order(3)
public class AmountLimitFilter implements MessageFilter {

    @Override
    public boolean accept(OrderDTO order) {
        return order.getAmount().compareTo(BigDecimal.valueOf(10000)) <= 0;
    }
}
```

---

## Pattern 5: Performance Monitoring Hooks

### Instrumented Listener

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class InstrumentedOrderListener extends AbstractKafkaListener {

    private final OrderService orderService;
    private final MeterRegistry meterRegistry;

    @KafkaListener(
        topics = KafkaConst.Topic.ORDER,
        groupId = KafkaConst.Group.ORDER
    )
    public void onMessage(ConsumerRecord<String, String> record) {
        handleMessage(record);
    }

    @Override
    protected void handleMessage(ConsumerRecord<String, String> record) {
        long startTime = System.currentTimeMillis();
        String topic = record.topic();
        String status = "success";

        try {
            super.handleMessage(record);
        } catch (Exception e) {
            status = "failure";
            throw e;
        } finally {
            // Record processing duration
            long duration = System.currentTimeMillis() - startTime;
            meterRegistry.timer("kafka.message.processing.duration",
                "topic", topic,
                "status", status
            ).record(duration, TimeUnit.MILLISECONDS);

            // Record message count
            meterRegistry.counter("kafka.message.processed.total",
                "topic", topic,
                "status", status
            ).increment();

            log.info("📊 Metrics recorded | Topic: {} | Duration: {}ms | Status: {}",
                topic, duration, status);
        }
    }

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        OrderDTO order = JSON.parseObject(record.value(), OrderDTO.class);
        orderService.processOrder(order);
    }
}
```

---

## Lifecycle Hooks

### Custom Initialization and Cleanup

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class LifecycleAwareListener extends AbstractKafkaListener implements InitializingBean, DisposableBean {

    private final OrderService orderService;
    private ScheduledExecutorService scheduler;

    @Override
    public void afterPropertiesSet() {
        log.info("🚀 Listener initialized");

        // Start background tasks
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(
            this::healthCheck,
            0, 60, TimeUnit.SECONDS
        );
    }

    @Override
    public void destroy() {
        log.info("🛑 Listener shutting down");

        // Cleanup resources
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void healthCheck() {
        log.info("💓 Health check - Listener is active");
    }

    @KafkaListener(
        topics = KafkaConst.Topic.ORDER,
        groupId = KafkaConst.Group.ORDER
    )
    public void onMessage(ConsumerRecord<String, String> record) {
        handleMessage(record);
    }

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        OrderDTO order = JSON.parseObject(record.value(), OrderDTO.class);
        orderService.processOrder(order);
    }
}
```

---

## Testing Custom Listeners

### Unit Test

```java
@SpringBootTest
class CustomErrorHandlingListenerTest {

    @Autowired
    private CustomErrorHandlingListener listener;

    @MockBean
    private OrderService orderService;

    @MockBean
    private RetryQueueService retryQueueService;

    @Test
    void testValidationErrorSentToDLQ() {
        // Arrange
        String invalidJson = "{\"invalidField\":\"value\"}";
        ConsumerRecord<String, String> record =
            new ConsumerRecord<>("smart-admin-order", 0, 0, "ORDER-123", invalidJson);

        doThrow(new ValidationException("Invalid order"))
            .when(orderService).processOrder(any());

        // Act
        listener.handleMessage(record);

        // Assert
        // Verify DLQ send (implementation depends on your test setup)
    }

    @Test
    void testTemporaryErrorAddedToRetryQueue() {
        // Arrange
        String orderJson = "{\"orderId\":\"123\",\"amount\":100}";
        ConsumerRecord<String, String> record =
            new ConsumerRecord<>("smart-admin-order", 0, 0, "ORDER-123", orderJson);

        doThrow(new TimeoutException("Database timeout"))
            .when(orderService).processOrder(any());

        // Act
        listener.handleMessage(record);

        // Assert
        verify(retryQueueService, times(1)).scheduleRetry(eq(record), eq(3));
    }
}
```

---

## Best Practices

### 1. Override Only What You Need

```java
// ✅ Good: Override only error handling
@Override
protected void handleError(ConsumerRecord<String, String> record, Exception e) {
    // Custom error logic
}

// ❌ Bad: Override entire handleMessage (loses DLQ logic)
@Override
protected void handleMessage(ConsumerRecord<String, String> record) {
    // Custom logic - DLQ routing lost!
}
```

### 2. Keep doHandle() Simple

```java
// ✅ Good: Business logic in service layer
@Override
protected void doHandle(ConsumerRecord<String, String> record) {
    OrderDTO order = JSON.parseObject(record.value(), OrderDTO.class);
    orderService.processOrder(order);  // Delegate to service
}

// ❌ Bad: Complex logic in listener
@Override
protected void doHandle(ConsumerRecord<String, String> record) {
    // 100 lines of business logic
}
```

### 3. Use Dependency Injection

```java
// ✅ Good: Constructor injection
@RequiredArgsConstructor
public class MyListener extends AbstractKafkaListener {
    private final OrderService orderService;
}

// ❌ Bad: Field injection
public class MyListener extends AbstractKafkaListener {
    @Autowired
    private OrderService orderService;
}
```

---

## See Also

- [Consumer Guide](/kafka/guides/consumer-guide) - Basic consumer patterns
- [Error Handling](/kafka/guides/error-handling) - Error handling strategies
- [Extending Framework](/kafka/advanced/extending-framework) - Framework extension guide
- [Testing](/kafka/testing/unit-testing) - Unit testing listeners

---

**Last Updated**: 2026-01-22
