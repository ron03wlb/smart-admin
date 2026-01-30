# Message Queue Reliability Patterns Guide

**Skill:** message-queue-pattern-generator
**Component:** Dead Letter Queue / Idempotency / Retry / Schema Registry
**Purpose:** Ensure reliable message processing with fault tolerance and data quality

---

## Pattern 1: Idempotency with Redis

### Why Idempotency?

**Problem:** Kafka guarantees at-least-once delivery
- Network issues cause retries
- Consumer restarts replay messages
- Duplicate processing causes data corruption

**Solution:** Track processed event IDs in Redis

---

### Implementation

```java
package net.lab1024.sa.admin.module.business.order.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.order.domain.event.OrderCreatedEvent;
import net.lab1024.sa.admin.module.business.notification.manager.NotificationManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotentOrderConsumer {

    private final NotificationManager notificationManager;
    private final StringRedisTemplate redisTemplate;

    private static final String IDEMPOTENCY_KEY_PREFIX = "event:processed:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofDays(7);  // Keep for 7 days

    @KafkaListener(topics = "smartadmin.order.created", groupId = "notification-group")
    public void handleOrderCreated(OrderCreatedEvent event, Acknowledgment ack) {
        String idempotencyKey = IDEMPOTENCY_KEY_PREFIX + event.getEventId();

        try {
            // Check if event already processed (idempotency check)
            Boolean alreadyProcessed = redisTemplate.opsForValue()
                .setIfAbsent(idempotencyKey, "1", IDEMPOTENCY_TTL);

            if (Boolean.FALSE.equals(alreadyProcessed)) {
                log.info("Event already processed (duplicate), skipping: eventId={}", event.getEventId());
                ack.acknowledge();  // Acknowledge duplicate
                return;
            }

            // Process event (first time)
            log.info("Processing OrderCreated event: orderId={}, eventId={}",
                event.getOrderId(), event.getEventId());

            notificationManager.sendOrderConfirmation(event);

            // Acknowledge after successful processing
            ack.acknowledge();

            log.info("Event processed successfully: eventId={}", event.getEventId());

        } catch (Exception e) {
            log.error("Failed to process event: eventId={}", event.getEventId(), e);

            // Remove idempotency key on failure (allow retry)
            redisTemplate.delete(idempotencyKey);

            throw e;  // Don't acknowledge - message will be retried
        }
    }
}
```

### Alternative: Database-based Idempotency

```java
package net.lab1024.sa.admin.module.business.order.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.order.dao.ProcessedEventDao;
import net.lab1024.sa.admin.module.business.order.domain.entity.ProcessedEventEntity;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseIdempotencyChecker {

    private final ProcessedEventDao processedEventDao;

    /**
     * Check and mark event as processed (atomic operation)
     */
    public boolean checkAndMarkProcessed(String eventId, String eventType) {
        try {
            ProcessedEventEntity entity = ProcessedEventEntity.builder()
                .eventId(eventId)
                .eventType(eventType)
                .processedAt(LocalDateTime.now())
                .build();

            processedEventDao.insert(entity);
            return true;  // First time processing

        } catch (DuplicateKeyException e) {
            log.info("Event already processed: eventId={}", eventId);
            return false;  // Duplicate
        }
    }
}
```

```sql
-- Processed events table
CREATE TABLE processed_events (
    event_id VARCHAR(100) PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    processed_at TIMESTAMP NOT NULL,
    INDEX idx_processed_at (processed_at)
);
```

---

## Pattern 2: Dead Letter Queue (DLQ)

### Why DLQ?

**Problem:** Some messages fail repeatedly (poison pills)
- Block consumer progress
- Waste processing resources
- Hard to identify problematic messages

**Solution:** Move failed messages to Dead Letter Queue after max retries

---

### Configuration

```yaml
# application.yml
spring:
  kafka:
    consumer:
      enable-auto-commit: false
      max-poll-records: 10
    listener:
      ack-mode: manual
      # Retry configuration
      retry:
        max-attempts: 3
        backoff:
          initial-interval: 1000    # 1 second
          multiplier: 2.0            # Exponential backoff
          max-interval: 10000        # 10 seconds max
```

### DLQ Implementation

```java
package net.lab1024.sa.admin.module.business.order.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.order.domain.event.OrderCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderConsumerWithDLQ {

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    private final OrderProcessingService processingService;

    private static final String DLQ_TOPIC = "smartadmin.order.created.dlq";
    private static final int MAX_RETRIES = 3;

    @KafkaListener(topics = "smartadmin.order.created", groupId = "order-processing-group")
    public void handleOrderCreated(
        OrderCreatedEvent event,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset,
        @Header(value = "retry-count", required = false) Integer retryCount,
        Acknowledgment ack
    ) {
        int currentRetryCount = retryCount != null ? retryCount : 0;

        try {
            log.info("Processing event: orderId={}, retry={}, partition={}, offset={}",
                event.getOrderId(), currentRetryCount, partition, offset);

            processingService.processOrder(event);

            ack.acknowledge();
            log.info("Event processed successfully: orderId={}", event.getOrderId());

        } catch (Exception e) {
            log.error("Failed to process event: orderId={}, retry={}",
                event.getOrderId(), currentRetryCount, e);

            if (currentRetryCount >= MAX_RETRIES) {
                // Max retries exceeded - send to DLQ
                log.warn("Max retries exceeded, sending to DLQ: orderId={}, retries={}",
                    event.getOrderId(), currentRetryCount);

                sendToDLQ(event, e.getMessage(), partition, offset);
                ack.acknowledge();  // Acknowledge to prevent infinite loop

            } else {
                // Retry with exponential backoff
                int nextRetryCount = currentRetryCount + 1;
                long backoffMs = calculateBackoff(nextRetryCount);

                log.info("Scheduling retry {}/{} after {}ms: orderId={}",
                    nextRetryCount, MAX_RETRIES, backoffMs, event.getOrderId());

                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }

                // Don't acknowledge - message will be retried
                throw e;
            }
        }
    }

    private void sendToDLQ(OrderCreatedEvent event, String errorMessage, int partition, long offset) {
        try {
            // Add error metadata
            event.setErrorMessage(errorMessage);
            event.setOriginalPartition(partition);
            event.setOriginalOffset(offset);
            event.setDlqTimestamp(LocalDateTime.now());

            kafkaTemplate.send(DLQ_TOPIC, event.getOrderId().toString(), event);

            log.info("Event sent to DLQ: orderId={}, topic={}", event.getOrderId(), DLQ_TOPIC);

        } catch (Exception e) {
            log.error("Failed to send event to DLQ: orderId={}", event.getOrderId(), e);
        }
    }

    private long calculateBackoff(int retryCount) {
        // Exponential backoff: 1s, 2s, 4s
        return (long) (1000 * Math.pow(2, retryCount - 1));
    }
}
```

### DLQ Monitoring and Recovery

```java
package net.lab1024.sa.admin.module.business.order.dlq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.order.domain.event.OrderCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DLQMonitoringService {

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    /**
     * Monitor DLQ for poison pills
     */
    @KafkaListener(topics = "smartadmin.order.created.dlq", groupId = "dlq-monitoring-group")
    public void monitorDLQ(OrderCreatedEvent event) {
        log.error("DLQ message detected: orderId={}, error={}, originalOffset={}",
            event.getOrderId(), event.getErrorMessage(), event.getOriginalOffset());

        // Alert operations team
        alertOperations(event);

        // Store in database for manual review
        storeDLQMessage(event);
    }

    /**
     * Manual retry from DLQ (after fixing issue)
     */
    public void retryFromDLQ(Long orderId) {
        OrderCreatedEvent event = loadFromDLQ(orderId);

        log.info("Retrying event from DLQ: orderId={}", orderId);

        // Send back to original topic
        kafkaTemplate.send("smartadmin.order.created", orderId.toString(), event);
    }

    private void alertOperations(OrderCreatedEvent event) {
        // Send alert via email, Slack, PagerDuty, etc.
    }

    private void storeDLQMessage(OrderCreatedEvent event) {
        // Store in database for manual review and retry
    }

    private OrderCreatedEvent loadFromDLQ(Long orderId) {
        // Load from database
        return null;
    }
}
```

---

## Pattern 3: Schema Registry Integration

### Why Schema Registry?

**Problem:** Event schema changes break consumers
- No schema validation
- Backward/forward compatibility issues
- Runtime deserialization errors

**Solution:** Use Confluent Schema Registry with Avro

---

### Dependencies

```gradle
dependencies {
    implementation 'io.confluent:kafka-avro-serializer:7.9.0'
    implementation 'org.apache.avro:avro:1.12.0'
}
```

### Avro Schema Definition

```json
// order-created-event.avsc
{
  "namespace": "net.lab1024.sa.admin.module.business.order.domain.event.avro",
  "type": "record",
  "name": "OrderCreatedEvent",
  "fields": [
    {"name": "orderId", "type": "long"},
    {"name": "userId", "type": "long"},
    {"name": "totalAmount", "type": "string"},
    {"name": "orderStatus", "type": "string"},
    {"name": "createdAt", "type": "long", "logicalType": "timestamp-millis"},
    {"name": "eventId", "type": "string"},
    {"name": "eventVersion", "type": "int", "default": 1}
  ]
}
```

### Configuration

```yaml
# application.yml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_SERVERS:localhost:9092}
    properties:
      schema.registry.url: ${SCHEMA_REGISTRY_URL:http://localhost:8081}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: io.confluent.kafka.serializers.KafkaAvroSerializer
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: io.confluent.kafka.serializers.KafkaAvroDeserializer
      properties:
        specific.avro.reader: true
```

### Producer with Avro

```java
package net.lab1024.sa.admin.module.business.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.order.domain.event.avro.OrderCreatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvroEventProducer {

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    public void publishOrderCreated(Long orderId, Long userId, String totalAmount) {
        OrderCreatedEvent event = OrderCreatedEvent.newBuilder()
            .setOrderId(orderId)
            .setUserId(userId)
            .setTotalAmount(totalAmount)
            .setOrderStatus("CREATED")
            .setCreatedAt(System.currentTimeMillis())
            .setEventId(UUID.randomUUID().toString())
            .setEventVersion(1)
            .build();

        kafkaTemplate.send("smartadmin.order.created", orderId.toString(), event);

        log.info("Avro event published: orderId={}, schema registered", orderId);
    }
}
```

### Schema Evolution Example

```json
// order-created-event-v2.avsc (with new field)
{
  "namespace": "net.lab1024.sa.admin.module.business.order.domain.event.avro",
  "type": "record",
  "name": "OrderCreatedEvent",
  "fields": [
    {"name": "orderId", "type": "long"},
    {"name": "userId", "type": "long"},
    {"name": "totalAmount", "type": "string"},
    {"name": "orderStatus", "type": "string"},
    {"name": "createdAt", "type": "long", "logicalType": "timestamp-millis"},
    {"name": "eventId", "type": "string"},
    {"name": "eventVersion", "type": "int", "default": 2},
    {"name": "orderSource", "type": ["null", "string"], "default": null}  // New field (optional)
  ]
}
```

**Backward compatibility:** Old consumers can still read new events (ignore new field)
**Forward compatibility:** New consumers can read old events (use default value)

---

## Pattern 4: Retry with Exponential Backoff

```java
package net.lab1024.sa.admin.module.business.order.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetryableOrderConsumer {

    private final OrderProcessingService processingService;

    @KafkaListener(topics = "smartadmin.order.created", groupId = "order-processing-group")
    @Retryable(
        value = {ProcessingException.class},
        maxAttempts = 5,
        backoff = @Backoff(
            delay = 1000,      // Initial delay: 1 second
            multiplier = 2.0,  // Double each time
            maxDelay = 60000   // Max delay: 1 minute
        )
    )
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Processing order: orderId={}", event.getOrderId());

        try {
            processingService.processOrder(event);
            log.info("Order processed successfully: orderId={}", event.getOrderId());

        } catch (TemporaryException e) {
            log.warn("Temporary error, will retry: orderId={}", event.getOrderId(), e);
            throw e;  // Trigger retry

        } catch (PermanentException e) {
            log.error("Permanent error, skipping retry: orderId={}", event.getOrderId(), e);
            // Don't throw - skip retry
        }
    }
}
```

---

## Pattern 5: Circuit Breaker for External Services

```java
package net.lab1024.sa.admin.module.business.order.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalPaymentService {

    private final RestTemplate restTemplate;

    @CircuitBreaker(
        name = "paymentService",
        fallbackMethod = "paymentFallback"
    )
    public PaymentResponse processPayment(Long orderId, BigDecimal amount) {
        log.info("Calling external payment service: orderId={}, amount={}", orderId, amount);

        String url = "https://payment-api.example.com/process";
        PaymentRequest request = new PaymentRequest(orderId, amount);

        return restTemplate.postForObject(url, request, PaymentResponse.class);
    }

    /**
     * Fallback method when circuit is open
     */
    public PaymentResponse paymentFallback(Long orderId, BigDecimal amount, Exception e) {
        log.error("Payment service unavailable, using fallback: orderId={}", orderId, e);

        // Option 1: Queue for later processing
        paymentQueueService.queuePayment(orderId, amount);

        // Option 2: Return error response
        return PaymentResponse.error("Payment service temporarily unavailable");
    }
}
```

### Circuit Breaker Configuration

```yaml
# application.yml
resilience4j:
  circuitbreaker:
    instances:
      paymentService:
        failure-rate-threshold: 50          # Open circuit if 50% fail
        wait-duration-in-open-state: 60s    # Wait 60s before half-open
        sliding-window-size: 10             # Track last 10 calls
        minimum-number-of-calls: 5          # Min calls before calculating
```

---

## Complete SmartAdmin Integration

```java
// Service with all reliability patterns
@Service
@RequiredArgsConstructor
public class ReliableOrderEventConsumer {

    private final IdempotencyChecker idempotencyChecker;
    private final OrderProcessingService processingService;
    private final DLQService dlqService;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    @KafkaListener(topics = "smartadmin.order.created", groupId = "reliable-order-group")
    public void handleOrderCreated(OrderCreatedEvent event, Acknowledgment ack) {
        String eventId = event.getEventId();

        try {
            // 1. Idempotency check
            if (!idempotencyChecker.checkAndMarkProcessed(eventId, "OrderCreated")) {
                log.info("Duplicate event, skipping: eventId={}", eventId);
                ack.acknowledge();
                return;
            }

            // 2. Process with retry and circuit breaker
            processingService.processOrderWithResilience(event);

            // 3. Acknowledge success
            ack.acknowledge();
            log.info("Event processed successfully: eventId={}", eventId);

        } catch (Exception e) {
            log.error("Processing failed: eventId={}", eventId, e);

            // 4. Retry or DLQ
            int retryCount = getRetryCount(eventId);
            if (retryCount >= 3) {
                dlqService.sendToDLQ(event, e.getMessage());
                ack.acknowledge();
            } else {
                incrementRetryCount(eventId);
                throw e;  // Don't acknowledge - will retry
            }
        }
    }

    private int getRetryCount(String eventId) {
        String key = "retry:count:" + eventId;
        String count = redisTemplate.opsForValue().get(key);
        return count != null ? Integer.parseInt(count) : 0;
    }

    private void incrementRetryCount(String eventId) {
        String key = "retry:count:" + eventId;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofHours(1));
    }
}
```

---

## Best Practices

### Idempotency:
- ✅ Use unique event IDs (UUID)
- ✅ Check idempotency before processing
- ✅ Store processed IDs with TTL (7-30 days)
- ✅ Handle idempotency at consumer level (not producer)

### Dead Letter Queue:
- ✅ Set max retry limit (3-5 retries)
- ✅ Use exponential backoff
- ✅ Monitor DLQ size with alerts
- ✅ Implement manual retry mechanism
- ✅ Store DLQ messages in database for analysis

### Schema Registry:
- ✅ Use Avro for schema validation
- ✅ Test backward/forward compatibility
- ✅ Version schemas incrementally
- ✅ Add new fields as optional (with defaults)
- ✅ Never remove required fields

### Retry Strategy:
- ✅ Distinguish transient vs permanent errors
- ✅ Use circuit breaker for external dependencies
- ✅ Implement jitter to prevent thundering herd
- ✅ Set reasonable retry limits and backoff

---

**Version:** 1.0.0
**Last Updated:** 2026-01-26
