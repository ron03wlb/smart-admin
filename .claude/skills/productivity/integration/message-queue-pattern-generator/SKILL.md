---
name: message-queue-pattern-generator
description: [P2 - Productivity] Generate Kafka/RocketMQ integration patterns (producer/consumer, event sourcing, CQRS, Saga, dead letter queues, idempotency, schema registry) for event-driven SmartAdmin applications. Use when implementing event-driven architecture, distributed transactions, or asynchronous processing. Triggers when user mentions "Kafka", "RocketMQ", "event-driven", "message queue", "CQRS", "event sourcing", "Saga pattern", or "async messaging".
---

# Message Queue Pattern Generator

**Priority:** P0 - Pain Point #4
**Sprint:** 2 (Weeks 5-8)
**Status:** ✅ Production Ready

## Purpose

Enable event-driven architecture by generating Kafka/RocketMQ integration patterns. Unlocks scalability and loose coupling for SmartAdmin applications.

## Trigger Keywords

This skill is automatically activated when the user's request contains:

**Primary Keywords** (High confidence):
- "Kafka" - Apache Kafka integration
- "RocketMQ" - Apache RocketMQ integration
- "event-driven" - Event-driven architecture
- "message queue" - Message queue integration
- "CQRS" - Command Query Responsibility Segregation
- "event sourcing" - Event sourcing pattern

**Secondary Keywords** (Medium confidence):
- "producer" / "consumer" - Context: Kafka/RocketMQ patterns
- "distributed transaction" - Context: message-based saga patterns
- "idempotency" - Context: idempotent message processing
- "dead letter queue" - Context: DLQ handling

**Phrase Patterns**:
- "Add [MQ] to [module]" - Example: "Add Kafka to Order processing"
- "Implement event-driven [pattern]" - Example: "Implement event-driven notifications"
- "Setup [Kafka/RocketMQ] for [use case]" - Example: "Setup Kafka for audit logging"

**Example User Requests**:
```
User: "Add Kafka integration to Order processing module"
User: "Implement event-driven architecture for user notifications"
User: "Setup RocketMQ for distributed transaction handling"
User: "Create event sourcing pattern for wallet operations"
```

**Note**: This skill can also be manually invoked via `/message-queue-pattern-generator` command.

## Problem Statement

**User Pain Point:** "事件驱动架构" (Event-driven architecture)

**Current Issues:**
- SmartAdmin has `foundation.mq.kafka` but lacks practical patterns
- Manual producer/consumer code is repetitive
- No systematic event sourcing patterns
- Distributed transaction handling is complex
- Missing idempotency and dead letter queue handling

## Solution Overview

This skill generates:
- ✅ Kafka producer/consumer code generation (with Spring Kafka)
- ✅ RocketMQ pattern templates (if applicable)
- ✅ Event sourcing patterns (event store, projections, snapshots)
- ✅ CQRS implementation (command/query separation, eventual consistency)
- ✅ Saga patterns for distributed transactions (choreography, orchestration)
- ✅ Dead letter queue handling (retry strategies, manual intervention)
- ✅ Message idempotency (deduplication with Redis)
- ✅ Schema registry integration (Avro, Protobuf, JSON Schema)
- ✅ Event versioning strategies (backward/forward compatibility)

## Quick Start

**Most common usage:**
```
User: "Generate Kafka producer for OrderCreated events with idempotency"
```

You will:
1. Generate Kafka producer code
2. Add event schema (Avro/Protobuf)
3. Implement idempotency check
4. Create consumer with dead letter queue
5. Add retry logic
6. Set up event versioning

## Scope

### Included
- Kafka/RocketMQ producer/consumer patterns
- Event sourcing (event store, projections)
- CQRS implementation
- Saga patterns (choreography, orchestration)
- Dead letter queue handling
- Idempotency with Redis
- Schema registry integration
- Event versioning strategies

### Not Included
- Kafka cluster setup
- Schema registry server deployment
- Custom serializers (uses standard Avro/Protobuf)

## Integration Points

- Uses `foundation.mq.kafka` module
- Integrates with `liteflow-rule-builder` for complex event processing
- Works with `smartadmin-crud-generator` for event-driven CRUD
- Compatible with `cache-strategy-generator` for event invalidation

## Success Criteria

- ✅ Event-driven patterns deployed in at least 3 SmartAdmin modules
- ✅ Kafka integration tested with Testcontainers
- ✅ Saga pattern validated with distributed transaction scenario
- ✅ Idempotency preventing duplicate processing
- ✅ Dead letter queue handling operational

## Detailed Documentation

### Configuration Guides

1. **[Kafka Producer/Consumer Patterns](references/kafka-patterns.md)**
   - Simple producer (async, sync, transactional)
   - Consumer with manual commit
   - Batch consumer for high throughput
   - Kafka Streams for stateful processing
   - SmartAdmin integration examples
   - **Lines:** ~700+ lines with 5 patterns

2. **[Event Sourcing Patterns](references/event-sourcing-patterns.md)**
   - Event store implementation (database, Kafka)
   - Aggregate rebuilding from events
   - Projections (read models)
   - Snapshots for performance
   - Temporal queries (point-in-time state)
   - **Lines:** ~600+ lines

3. **[CQRS and Saga Patterns](references/cqrs-saga-patterns.md)**
   - CQRS implementation (command/query separation)
   - Choreography-based saga
   - Orchestration-based saga
   - Compensation transactions
   - Saga state management
   - **Lines:** ~650+ lines

4. **[Reliability Patterns](references/reliability-patterns.md)**
   - Idempotency with Redis/Database
   - Dead letter queue (DLQ) handling
   - Schema registry integration (Avro)
   - Retry with exponential backoff
   - Circuit breaker for external services
   - **Lines:** ~650+ lines

## Implementation Workflow

### Step 1: Add Dependencies (2 minutes)

```gradle
dependencies {
    // Spring Kafka (recommended)
    implementation 'org.springframework.kafka:spring-kafka:3.3.2'

    // Kafka clients
    implementation 'org.apache.kafka:kafka-clients:3.9.0'

    // Schema registry (optional)
    implementation 'io.confluent:kafka-avro-serializer:7.9.0'

    // Circuit breaker (optional)
    implementation 'io.github.resilience4j:resilience4j-spring-boot3:2.2.0'

    // Testing
    testImplementation 'org.springframework.kafka:spring-kafka-test:3.3.2'
    testImplementation 'org.testcontainers:kafka:1.20.4'
}
```

### Step 2: Configure Kafka (5 minutes)

```yaml
# application.yml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
      properties:
        enable.idempotence: true
    consumer:
      group-id: smartadmin-order-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      enable-auto-commit: false
      auto-offset-reset: earliest
      properties:
        spring.json.trusted.packages: "net.lab1024.sa.admin.module.business.*.domain.event"
```

### Step 3: Create Event Schema (3 minutes)

```java
package net.lab1024.sa.admin.module.business.order.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
    private Long orderId;
    private Long userId;
    private BigDecimal totalAmount;
    private String orderStatus;
    private LocalDateTime createdAt;
    private String eventId;         // For idempotency
    private Integer eventVersion;    // For schema evolution
}
```

### Step 4: Implement Producer (10 minutes)

```java
package net.lab1024.sa.admin.module.business.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.order.domain.event.OrderCreatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    private static final String TOPIC = "smartadmin.order.created";

    public void publishOrderCreated(OrderCreatedEvent event) {
        event.setEventId(UUID.randomUUID().toString());
        event.setEventVersion(1);

        CompletableFuture<SendResult<String, OrderCreatedEvent>> future =
            kafkaTemplate.send(TOPIC, event.getOrderId().toString(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Event published: topic={}, offset={}, eventId={}",
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().offset(),
                    event.getEventId());
            } else {
                log.error("Failed to publish event: eventId={}", event.getEventId(), ex);
            }
        });
    }
}
```

### Step 5: Implement Consumer with Idempotency (15 minutes)

```java
package net.lab1024.sa.admin.module.business.notification.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.notification.manager.NotificationManager;
import net.lab1024.sa.admin.module.business.order.domain.event.OrderCreatedEvent;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final NotificationManager notificationManager;
    private final StringRedisTemplate redisTemplate;

    private static final String IDEMPOTENCY_PREFIX = "event:processed:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofDays(7);

    @KafkaListener(topics = "smartadmin.order.created", groupId = "notification-group")
    public void handleOrderCreated(OrderCreatedEvent event, Acknowledgment ack) {
        String idempotencyKey = IDEMPOTENCY_PREFIX + event.getEventId();

        try {
            // Idempotency check
            Boolean alreadyProcessed = redisTemplate.opsForValue()
                .setIfAbsent(idempotencyKey, "1", IDEMPOTENCY_TTL);

            if (Boolean.FALSE.equals(alreadyProcessed)) {
                log.info("Duplicate event, skipping: eventId={}", event.getEventId());
                ack.acknowledge();
                return;
            }

            // Process event
            log.info("Processing OrderCreated: orderId={}, eventId={}",
                event.getOrderId(), event.getEventId());

            notificationManager.sendOrderConfirmation(event);

            ack.acknowledge();
            log.info("Event processed successfully: eventId={}", event.getEventId());

        } catch (Exception e) {
            log.error("Processing failed: eventId={}", event.getEventId(), e);
            redisTemplate.delete(idempotencyKey);  // Remove on failure for retry
            throw e;
        }
    }
}
```

### Step 6: Integrate with SmartAdmin Service (5 minutes)

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderManager orderManager;
    private final OrderEventProducer eventProducer;

    public ResponseDTO<OrderVO> createOrder(OrderAddForm form) {
        // 1. Save order to database
        OrderEntity order = orderManager.createOrder(form);

        // 2. Publish event (async, non-blocking)
        OrderCreatedEvent event = OrderCreatedEvent.builder()
            .orderId(order.getOrderId())
            .userId(order.getUserId())
            .totalAmount(order.getTotalAmount())
            .orderStatus(order.getOrderStatus())
            .createdAt(LocalDateTime.now())
            .build();

        eventProducer.publishOrderCreated(event);

        // 3. Return immediately
        OrderVO orderVO = orderManager.getOrderById(order.getOrderId());
        return ResponseDTO.ok(orderVO);
    }
}
```

### Step 7: Testing with Testcontainers (5 minutes)

```java
@SpringBootTest
@Testcontainers
class OrderEventProducerTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.9.0")
    );

    @Autowired
    private OrderEventProducer eventProducer;

    @Test
    void testPublishOrderCreated() {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
            .orderId(12345L)
            .userId(1L)
            .totalAmount(new BigDecimal("99.99"))
            .orderStatus("CREATED")
            .createdAt(LocalDateTime.now())
            .build();

        eventProducer.publishOrderCreated(event);

        // Verify event was published
        await().atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                // Consumer assertions
            });
    }
}
```

### Step 8: Verify (2 minutes)

```bash
# Check Kafka topics
kafka-topics --bootstrap-server localhost:9092 --list

# Consume messages
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic smartadmin.order.created --from-beginning

# Check consumer lag
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe --group smartadmin-order-group
```

**Total Time:** ~45 minutes (vs 2-3 days manual implementation)

## Troubleshooting Guide

### Issue: Consumer lag increasing

**Solution:** Scale consumers or batch processing
```java
// Increase concurrency
@KafkaListener(topics = "smartadmin.order.created",
               groupId = "order-group",
               concurrency = "5")  // 5 concurrent consumers
```

### Issue: Duplicate processing

**Solution:** Implement idempotency (see Step 5)

### Issue: Messages stuck in DLQ

**Solution:** Monitor and manual retry
```java
// Check DLQ size
long dlqSize = kafkaAdmin.listTopics()
    .names()
    .get()
    .stream()
    .filter(t -> t.endsWith(".dlq"))
    .count();

// Manual retry
dlqService.retryFromDLQ(orderId);
```

### Issue: Schema incompatibility

**Solution:** Use schema registry with versioning (see [Reliability Patterns](references/reliability-patterns.md))

## Performance Impact

**Expected Improvements:**
- Throughput: ~100K messages/second (3-node Kafka cluster)
- Latency: P95 < 10ms (producer), P95 < 50ms (end-to-end)
- Reliability: 99.99% delivery guarantee (with retries)
- Scalability: Horizontal scaling (add partitions + consumers)

**Resource Requirements:**
- Kafka cluster: 3 nodes (production) or 1 node (development)
- Memory: ~512MB per consumer
- CPU: < 5% per consumer (idle), < 30% (peak load)
- Network: ~10MB/s per 10K messages/sec

---

**Version:** 1.0.0-SKELETON
**Created:** 2026-01-26
**Sprint:** 2 (Weeks 5-8)
**Status:** Awaiting detailed implementation
