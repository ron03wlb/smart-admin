# Kafka Producer/Consumer Patterns Guide

**Skill:** message-queue-pattern-generator
**Component:** Spring Kafka / Kafka Clients
**Purpose:** Generate production-ready Kafka integration for event-driven SmartAdmin applications

---

## Why Kafka?

- ✅ High-throughput event streaming (millions of messages/sec)
- ✅ Fault-tolerant with replication
- ✅ Decouples services for scalability
- ✅ Event sourcing foundation
- ✅ Real-time data pipelines

---

## Dependencies

```gradle
dependencies {
    // Spring Kafka (recommended for SmartAdmin)
    implementation 'org.springframework.kafka:spring-kafka:3.3.2'

    // Kafka clients
    implementation 'org.apache.kafka:kafka-clients:3.9.0'

    // Schema registry (Avro)
    implementation 'io.confluent:kafka-avro-serializer:7.9.0'

    // Testing
    testImplementation 'org.springframework.kafka:spring-kafka-test:3.3.2'
    testImplementation 'org.testcontainers:kafka:1.20.4'
}
```

---

## Pattern 1: Simple Producer (Event Publishing)

### Step 1: Configuration

```yaml
# application.yml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all  # Wait for all replicas
      retries: 3
      properties:
        max.in.flight.requests.per.connection: 1  # Ensure ordering
        enable.idempotence: true  # Exactly-once semantics
```

### Step 2: Event Schema

```java
package net.lab1024.sa.business.order.domain.event;

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
    private String eventId;  // Idempotency key
    private Integer eventVersion;  // For event versioning
}
```

### Step 3: Producer Service

```java
package net.lab1024.sa.business.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.business.order.domain.event.OrderCreatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    private static final String TOPIC = "smartadmin.order.created";

    /**
     * Publish OrderCreated event (async)
     */
    public void publishOrderCreated(OrderCreatedEvent event) {
        // Set event metadata
        event.setEventId(UUID.randomUUID().toString());
        event.setEventVersion(1);

        // Send with order ID as key (ensures partition ordering)
        CompletableFuture<SendResult<String, OrderCreatedEvent>> future =
            kafkaTemplate.send(TOPIC, event.getOrderId().toString(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Event published successfully: topic={}, partition={}, offset={}, eventId={}",
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset(),
                    event.getEventId()
                );
            } else {
                log.error("Failed to publish event: eventId={}", event.getEventId(), ex);
            }
        });
    }

    /**
     * Publish with synchronous confirmation (for critical events)
     */
    public SendResult<String, OrderCreatedEvent> publishOrderCreatedSync(OrderCreatedEvent event) {
        try {
            event.setEventId(UUID.randomUUID().toString());
            event.setEventVersion(1);

            SendResult<String, OrderCreatedEvent> result =
                kafkaTemplate.send(TOPIC, event.getOrderId().toString(), event).get();

            log.info("Event published (sync): topic={}, offset={}",
                result.getRecordMetadata().topic(),
                result.getRecordMetadata().offset()
            );

            return result;
        } catch (Exception e) {
            log.error("Failed to publish event (sync): orderId={}", event.getOrderId(), e);
            throw new RuntimeException("Event publishing failed", e);
        }
    }
}
```

### Step 4: SmartAdmin Service Integration

```java
package net.lab1024.sa.business.order.service;

import lombok.RequiredArgsConstructor;
import net.lab1024.sa.business.order.domain.entity.OrderEntity;
import net.lab1024.sa.business.order.domain.event.OrderCreatedEvent;
import net.lab1024.sa.business.order.domain.form.OrderAddForm;
import net.lab1024.sa.business.order.domain.vo.OrderVO;
import net.lab1024.sa.business.order.manager.OrderManager;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderManager orderManager;
    private final OrderEventProducer eventProducer;

    public ResponseDTO<OrderVO> createOrder(OrderAddForm form) {
        // Save order to database
        OrderEntity order = orderManager.createOrder(form);

        // Publish event asynchronously (fire-and-forget)
        OrderCreatedEvent event = OrderCreatedEvent.builder()
            .orderId(order.getOrderId())
            .userId(order.getUserId())
            .totalAmount(order.getTotalAmount())
            .orderStatus(order.getOrderStatus())
            .createdAt(LocalDateTime.now())
            .build();

        eventProducer.publishOrderCreated(event);

        // Return immediately (don't wait for event processing)
        OrderVO orderVO = orderManager.getOrderById(order.getOrderId());
        return ResponseDTO.ok(orderVO);
    }
}
```

---

## Pattern 2: Consumer with Manual Commit

### Step 1: Configuration

```yaml
# application.yml
spring:
  kafka:
    consumer:
      bootstrap-servers: ${KAFKA_SERVERS:localhost:9092}
      group-id: smartadmin-order-consumer-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      enable-auto-commit: false  # Manual commit for reliability
      auto-offset-reset: earliest  # Start from beginning if no offset
      properties:
        spring.json.trusted.packages: "net.lab1024.sa.business.order.domain.event"
        isolation.level: read_committed  # Only read committed messages (transactional)
```

### Step 2: Consumer Service

```java
package net.lab1024.sa.business.notification.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.business.notification.manager.NotificationManager;
import net.lab1024.sa.business.order.domain.event.OrderCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final NotificationManager notificationManager;

    @KafkaListener(
        topics = "smartadmin.order.created",
        groupId = "smartadmin-notification-consumer-group",
        concurrency = "3"  // 3 concurrent consumer threads
    )
    public void handleOrderCreated(
        @Payload OrderCreatedEvent event,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset,
        Acknowledgment acknowledgment
    ) {
        try {
            log.info("Received OrderCreated event: orderId={}, eventId={}, partition={}, offset={}",
                event.getOrderId(), event.getEventId(), partition, offset);

            // Process event (idempotency handled in manager)
            notificationManager.sendOrderConfirmation(event);

            // Manual commit after successful processing
            acknowledgment.acknowledge();

            log.info("OrderCreated event processed successfully: eventId={}", event.getEventId());

        } catch (Exception e) {
            log.error("Failed to process OrderCreated event: eventId={}, will retry",
                event.getEventId(), e);
            // Don't acknowledge - message will be redelivered
            throw e;
        }
    }
}
```

---

## Pattern 3: Batch Consumer (High Throughput)

```java
package net.lab1024.sa.business.analytics.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.business.analytics.manager.AnalyticsManager;
import net.lab1024.sa.business.order.domain.event.OrderCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAnalyticsBatchConsumer {

    private final AnalyticsManager analyticsManager;

    @KafkaListener(
        topics = "smartadmin.order.created",
        groupId = "smartadmin-analytics-consumer-group",
        containerFactory = "batchKafkaListenerContainerFactory"
    )
    public void handleOrderBatch(
        List<OrderCreatedEvent> events,
        Acknowledgment acknowledgment
    ) {
        try {
            log.info("Received batch of {} OrderCreated events", events.size());

            // Batch processing for analytics
            analyticsManager.processBatchOrders(events);

            // Commit entire batch
            acknowledgment.acknowledge();

            log.info("Batch processed successfully: count={}", events.size());

        } catch (Exception e) {
            log.error("Failed to process order batch: size={}", events.size(), e);
            throw e;
        }
    }
}
```

### Batch Configuration

```java
package net.lab1024.sa.foundation.mq.kafka.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

@Configuration
public class KafkaBatchConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> batchKafkaListenerContainerFactory(
        ConsumerFactory<String, Object> consumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);  // Enable batch mode
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }
}
```

---

## Pattern 4: Transactional Producer (Exactly-Once Semantics)

### Configuration

```yaml
spring:
  kafka:
    producer:
      transaction-id-prefix: smartadmin-tx-  # Enable transactions
      acks: all
      properties:
        enable.idempotence: true
```

### Transactional Service

```java
package net.lab1024.sa.business.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.business.order.domain.entity.OrderEntity;
import net.lab1024.sa.business.order.domain.event.OrderCreatedEvent;
import net.lab1024.sa.business.order.domain.event.PaymentRequestedEvent;
import net.lab1024.sa.business.order.manager.OrderManager;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderTransactionalService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderManager orderManager;

    /**
     * Atomic database write + multiple event publishing
     */
    @Transactional
    public void createOrderWithEvents(OrderEntity order) {
        // Execute in Kafka transaction
        kafkaTemplate.executeInTransaction(operations -> {
            // 1. Save to database (Spring @Transactional)
            orderManager.saveOrder(order);

            // 2. Publish OrderCreated event
            OrderCreatedEvent orderEvent = OrderCreatedEvent.builder()
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .build();
            operations.send("smartadmin.order.created", orderEvent);

            // 3. Publish PaymentRequested event
            PaymentRequestedEvent paymentEvent = PaymentRequestedEvent.builder()
                .orderId(order.getOrderId())
                .amount(order.getTotalAmount())
                .build();
            operations.send("smartadmin.payment.requested", paymentEvent);

            // All-or-nothing: if any step fails, everything rolls back
            return true;
        });

        log.info("Order created with transactional events: orderId={}", order.getOrderId());
    }
}
```

---

## Pattern 5: Kafka Streams (Stateful Processing)

```java
package net.lab1024.sa.business.analytics.stream;

import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.business.order.domain.event.OrderCreatedEvent;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.Duration;

@Slf4j
@Configuration
public class OrderAnalyticsStream {

    @Bean
    public KStream<String, OrderCreatedEvent> orderStream(StreamsBuilder builder) {
        // Input stream
        KStream<String, OrderCreatedEvent> orderStream = builder
            .stream("smartadmin.order.created",
                Consumed.with(Serdes.String(), new JsonSerde<>(OrderCreatedEvent.class)));

        // Aggregation: Count orders by user in 1-hour windows
        KTable<Windowed<Long>, Long> orderCountByUser = orderStream
            .groupBy((key, order) -> order.getUserId())
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofHours(1)))
            .count();

        // Output to new topic
        orderCountByUser
            .toStream()
            .map((windowedUserId, count) -> {
                String key = windowedUserId.key() + "@" + windowedUserId.window().start();
                return KeyValue.pair(key, count);
            })
            .to("smartadmin.order.count.hourly",
                Produced.with(Serdes.String(), Serdes.Long()));

        log.info("Order analytics stream initialized");
        return orderStream;
    }
}
```

---

## SmartAdmin Integration Example

### Complete Order Module with Kafka

```java
// 1. Controller layer
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    public ResponseDTO<OrderVO> createOrder(@RequestBody @Valid OrderAddForm form,
                                           @LoginUser RequestUser requestUser) {
        form.setUserId(requestUser.getUserId());
        return orderService.createOrder(form);
    }
}

// 2. Service layer
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderManager orderManager;
    private final OrderEventProducer eventProducer;

    public ResponseDTO<OrderVO> createOrder(OrderAddForm form) {
        OrderEntity order = orderManager.createOrder(form);

        // Publish event (async, non-blocking)
        eventProducer.publishOrderCreated(OrderCreatedEvent.builder()
            .orderId(order.getOrderId())
            .userId(order.getUserId())
            .totalAmount(order.getTotalAmount())
            .orderStatus(order.getOrderStatus())
            .createdAt(LocalDateTime.now())
            .build());

        return ResponseDTO.ok(orderManager.getOrderById(order.getOrderId()));
    }
}

// 3. Manager layer
@Component
@RequiredArgsConstructor
public class OrderManager {

    private final OrderDao orderDao;

    @Transactional(rollbackFor = Exception.class)
    public OrderEntity createOrder(OrderAddForm form) {
        OrderEntity order = SmartBeanUtil.copy(form, OrderEntity.class);
        order.setOrderStatus("CREATED");
        order.setCreatedAt(LocalDateTime.now());
        orderDao.insert(order);
        return order;
    }

    public OrderVO getOrderById(Long orderId) {
        OrderEntity order = orderDao.selectById(orderId);
        return SmartBeanUtil.copy(order, OrderVO.class);
    }
}

// 4. Event consumer (different module)
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationManager notificationManager;

    @KafkaListener(topics = "smartadmin.order.created",
                   groupId = "smartadmin-notification-group")
    public void handleOrderCreated(OrderCreatedEvent event, Acknowledgment ack) {
        try {
            notificationManager.sendOrderConfirmation(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to send notification: orderId={}", event.getOrderId(), e);
            throw e;
        }
    }
}
```

---

## Testing with Testcontainers

```java
package net.lab1024.sa.business.order.service;

import net.lab1024.sa.business.order.domain.event.OrderCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
@DirtiesContext
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

        // Wait for async publishing
        await().atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                // Verify event was published (check logs or consume)
            });
    }
}
```

---

## Best Practices

### 1. Event Design
- ✅ Use immutable events (all fields final)
- ✅ Include event metadata (eventId, version, timestamp)
- ✅ Keep events small (< 1MB)
- ✅ Use semantic versioning for schemas

### 2. Producer
- ✅ Use idempotent producer (`enable.idempotence=true`)
- ✅ Set `acks=all` for critical events
- ✅ Include partition key for ordering
- ✅ Handle publishing failures with retries

### 3. Consumer
- ✅ Use manual commit for reliability
- ✅ Implement idempotency (check eventId in Redis)
- ✅ Handle poison pills with dead letter queue
- ✅ Set appropriate `max.poll.records` for batch processing

### 4. Monitoring
- ✅ Track consumer lag
- ✅ Monitor partition distribution
- ✅ Alert on dead letter queue growth
- ✅ Measure end-to-end latency

---

**Next:** [Event Sourcing Patterns](event-sourcing-patterns.md)
**Version:** 1.0.0
**Last Updated:** 2026-01-26
