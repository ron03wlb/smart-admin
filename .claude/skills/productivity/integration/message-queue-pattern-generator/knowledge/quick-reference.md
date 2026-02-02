# Message Queue Pattern Generator - Quick Reference

**Version**: 1.0.0
**Last Updated**: 2026-02-02
**Skill**: message-queue-pattern-generator (P2 - Productivity/Integration)

---

## Command Quick Reference

| Command | Purpose | Duration |
|---------|---------|----------|
| Kafka Setup | Setup Kafka producer/consumer | ~15 min |
| RocketMQ Setup | Setup RocketMQ producer/consumer | ~12 min |
| Dead Letter Queue | Implement DLQ handling | ~10 min |
| Retry Strategy | Configure retry mechanism | ~8 min |
| Transaction Message | Implement distributed transaction | ~20 min |

---

## Message Queue Selection

### Option 1: Kafka (Recommended for High Throughput)

**Use When**: High throughput, event streaming, log aggregation

**Pros**:
- ✅ High throughput (millions/sec)
- ✅ Horizontal scalability
- ✅ Strong ordering guarantees (per partition)
- ✅ Message replay capability

**Cons**:
- ⚠️ At-least-once delivery (need idempotency)
- ⚠️ Complex setup

**Setup**:
```gradle
dependencies {
    implementation 'org.springframework.kafka:spring-kafka:3.2.0'
}
```

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all  # Wait for all replicas
      retries: 3
    consumer:
      group-id: smartadmin-consumer
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      enable-auto-commit: false  # Manual commit for reliability
      properties:
        spring.json.trusted.packages: net.lab1024.sa.*
```

**Producer Example**:
```java
@Component
@RequiredArgsConstructor
public class EmployeeEventProducer {

    private final KafkaTemplate<String, EmployeeEvent> kafkaTemplate;

    /**
     * Publish employee created event
     */
    public void publishEmployeeCreated(Long employeeId, String name) {
        EmployeeEvent event = EmployeeEvent.builder()
            .employeeId(employeeId)
            .name(name)
            .eventType(EventType.CREATED)
            .timestamp(LocalDateTime.now())
            .build();

        // Send with key for partition routing
        kafkaTemplate.send("employee-events", employeeId.toString(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to send event: {}", event, ex);
                } else {
                    log.info("Event sent: partition={}, offset={}",
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                }
            });
    }
}
```

**Consumer Example**:
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class EmployeeEventConsumer {

    private final EmployeeService employeeService;

    /**
     * Consume employee events with manual commit
     */
    @KafkaListener(
        topics = "employee-events",
        groupId = "smartadmin-consumer",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeEmployeeEvent(
        @Payload EmployeeEvent event,
        @Header(KafkaHeaders.RECEIVED_KEY) String key,
        Acknowledgment ack
    ) {
        try {
            log.info("Processing event: key={}, event={}", key, event);

            // Idempotent processing
            boolean processed = employeeService.processEvent(event);

            if (processed) {
                // Commit offset only after successful processing
                ack.acknowledge();
                log.info("Event processed successfully: {}", event.getEmployeeId());
            }
        } catch (Exception e) {
            log.error("Failed to process event: {}", event, e);
            // Don't commit - will retry
        }
    }
}
```

**Time to Setup**: 15-20 minutes

---

### Option 2: RocketMQ (Alibaba's MQ)

**Use When**: Transaction messages, scheduled delivery, distributed systems

**Pros**:
- ✅ Transaction message support
- ✅ Scheduled/delayed messages
- ✅ Broadcast and clustering modes
- ✅ Message filtering

**Cons**:
- ⚠️ Less mature ecosystem than Kafka
- ⚠️ Chinese documentation primarily

**Setup**:
```gradle
dependencies {
    implementation 'org.apache.rocketmq:rocketmq-spring-boot-starter:2.3.0'
}
```

```yaml
rocketmq:
  name-server: localhost:9876
  producer:
    group: smartadmin-producer
    send-message-timeout: 3000
    retry-times-when-send-failed: 2
  consumer:
    group: smartadmin-consumer
    consume-timeout: 15
```

**Producer Example**:
```java
@Component
@RequiredArgsConstructor
public class EmployeeEventProducer {

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * Send normal message
     */
    public void sendEmployeeCreated(Long employeeId, String name) {
        EmployeeEvent event = new EmployeeEvent(employeeId, name);

        SendResult result = rocketMQTemplate.syncSend(
            "employee-events:CREATED",  // topic:tag
            MessageBuilder.withPayload(event)
                .setHeader("employeeId", employeeId)
                .build()
        );

        log.info("Message sent: msgId={}, status={}",
            result.getMsgId(), result.getSendStatus());
    }

    /**
     * Send delayed message (e.g., 5 minutes delay)
     */
    public void sendDelayedNotification(Long employeeId) {
        rocketMQTemplate.syncSend(
            "notifications",
            MessageBuilder.withPayload("Welcome notification")
                .build(),
            3000,  // timeout
            16     // delay level (16 = 30 min, see RocketMQ delay levels)
        );
    }

    /**
     * Send transaction message (distributed transaction)
     */
    @Transactional(rollbackFor = Throwable.class)
    public void sendTransactionMessage(Long employeeId) {
        Message<EmployeeEvent> message = MessageBuilder
            .withPayload(new EmployeeEvent(employeeId))
            .build();

        // Send half message
        rocketMQTemplate.sendMessageInTransaction(
            "employee-transaction",
            message,
            employeeId  // arg for local transaction executor
        );
    }
}

/**
 * Transaction listener for distributed transaction
 */
@RocketMQTransactionListener
@RequiredArgsConstructor
public class EmployeeTransactionListener implements RocketMQLocalTransactionListener {

    private final EmployeeManager employeeManager;

    /**
     * Execute local transaction
     */
    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(
        Message msg, Object arg
    ) {
        try {
            Long employeeId = (Long) arg;

            // Execute local DB transaction
            employeeManager.updateEmployee(employeeId);

            return RocketMQLocalTransactionState.COMMIT;  // Commit MQ message
        } catch (Exception e) {
            log.error("Local transaction failed", e);
            return RocketMQLocalTransactionState.ROLLBACK;  // Rollback MQ message
        }
    }

    /**
     * Check transaction status (for message recovery)
     */
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        // Check if local transaction succeeded (query DB)
        Long employeeId = (Long) msg.getHeaders().get("employeeId");

        boolean exists = employeeManager.exists(employeeId);
        return exists ?
            RocketMQLocalTransactionState.COMMIT :
            RocketMQLocalTransactionState.ROLLBACK;
    }
}
```

**Consumer Example**:
```java
@Component
@RocketMQMessageListener(
    topic = "employee-events",
    consumerGroup = "smartadmin-consumer",
    selectorExpression = "CREATED || UPDATED"  // Tag filtering
)
@RequiredArgsConstructor
public class EmployeeEventConsumer implements RocketMQListener<EmployeeEvent> {

    private final EmployeeService employeeService;

    @Override
    public void onMessage(EmployeeEvent event) {
        log.info("Received event: {}", event);
        employeeService.processEvent(event);
    }
}
```

**Time to Setup**: 12-15 minutes

---

## Message Queue Patterns

### Pattern 1: Idempotent Consumer

**Problem**: At-least-once delivery → duplicate messages

**Solution**: Idempotency key + database deduplication

```java
@Component
@RequiredArgsConstructor
public class IdempotentConsumer {

    private final MessageLogDao messageLogDao;
    private final EmployeeService employeeService;

    /**
     * Idempotent message processing
     */
    @KafkaListener(topics = "employee-events")
    @Transactional(rollbackFor = Throwable.class)
    public void consume(EmployeeEvent event, Acknowledgment ack) {
        String messageId = event.getMessageId();  // Unique ID from producer

        // Check if already processed
        MessageLogEntity log = messageLogDao.selectByMessageId(messageId);
        if (log != null && log.getStatus() == ProcessStatus.SUCCESS) {
            log.info("Message already processed: {}", messageId);
            ack.acknowledge();
            return;
        }

        try {
            // Insert log with PROCESSING status (unique constraint on message_id)
            messageLogDao.insert(MessageLogEntity.builder()
                .messageId(messageId)
                .status(ProcessStatus.PROCESSING)
                .build());

            // Process business logic
            employeeService.processEvent(event);

            // Update status to SUCCESS
            messageLogDao.updateStatus(messageId, ProcessStatus.SUCCESS);

            ack.acknowledge();
        } catch (DuplicateKeyException e) {
            // Another consumer already processing - skip
            log.warn("Duplicate message detected: {}", messageId);
            ack.acknowledge();
        } catch (Exception e) {
            // Mark as FAILED for retry
            messageLogDao.updateStatus(messageId, ProcessStatus.FAILED);
            log.error("Processing failed: {}", messageId, e);
            // Don't commit - will retry
        }
    }
}
```

**Database Schema**:
```sql
CREATE TABLE t_message_log (
    id BIGSERIAL PRIMARY KEY,
    message_id VARCHAR(64) UNIQUE NOT NULL,
    topic VARCHAR(100) NOT NULL,
    payload TEXT,
    status VARCHAR(20) NOT NULL,  -- PROCESSING, SUCCESS, FAILED
    retry_count INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_message_status ON t_message_log(status, created_at);
```

**Time to Implement**: 15 minutes

---

### Pattern 2: Dead Letter Queue (DLQ)

**Problem**: Messages fail repeatedly and block consumer

**Solution**: Move failed messages to DLQ after max retries

```java
@Configuration
public class KafkaErrorHandlerConfig {

    /**
     * Configure DLQ with max 3 retries
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EmployeeEvent>
        kafkaListenerContainerFactory(
            ConsumerFactory<String, EmployeeEvent> consumerFactory,
            KafkaTemplate<String, EmployeeEvent> kafkaTemplate
        ) {

        ConcurrentKafkaListenerContainerFactory<String, EmployeeEvent> factory =
            new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(new DefaultErrorHandler(
            new DeadLetterPublishingRecoverer(kafkaTemplate),
            new FixedBackOff(1000L, 3L)  // 1s interval, 3 retries
        ));

        return factory;
    }
}

/**
 * DLQ Consumer - manual intervention required
 */
@Component
@Slf4j
public class DeadLetterQueueConsumer {

    @KafkaListener(topics = "employee-events.DLT")  // .DLT = Dead Letter Topic
    public void consumeDLQ(
        @Payload EmployeeEvent event,
        @Header(KafkaHeaders.EXCEPTION_MESSAGE) String errorMessage,
        @Header(KafkaHeaders.RECEIVED_KEY) String key
    ) {
        log.error("Message moved to DLQ: key={}, event={}, error={}",
            key, event, errorMessage);

        // Alert operations team
        // Store in database for manual review
        // Send notification
    }
}
```

**Time to Implement**: 10 minutes

---

### Pattern 3: Saga Pattern (Distributed Transaction)

**Problem**: Distributed transaction across multiple services

**Solution**: Choreography-based saga with compensating transactions

```java
/**
 * Order Service - Saga Orchestrator
 */
@Component
@RequiredArgsConstructor
public class OrderSagaOrchestrator {

    private final KafkaTemplate<String, SagaEvent> kafkaTemplate;
    private final OrderDao orderDao;

    /**
     * Start saga: Create order → Reserve inventory → Process payment
     */
    @Transactional(rollbackFor = Throwable.class)
    public void createOrder(OrderCreateForm form) {
        // Step 1: Create order (PENDING status)
        OrderEntity order = orderDao.insert(OrderEntity.builder()
            .userId(form.getUserId())
            .amount(form.getAmount())
            .status(OrderStatus.PENDING)
            .build());

        // Step 2: Publish inventory reservation event
        kafkaTemplate.send("inventory-events",
            SagaEvent.builder()
                .sagaId(order.getId())
                .eventType(SagaEventType.RESERVE_INVENTORY)
                .payload(form)
                .build()
        );
    }

    /**
     * Handle inventory reserved event
     */
    @KafkaListener(topics = "inventory-events")
    public void onInventoryReserved(SagaEvent event) {
        if (event.getEventType() == SagaEventType.INVENTORY_RESERVED) {
            // Step 3: Publish payment event
            kafkaTemplate.send("payment-events",
                SagaEvent.builder()
                    .sagaId(event.getSagaId())
                    .eventType(SagaEventType.PROCESS_PAYMENT)
                    .payload(event.getPayload())
                    .build()
            );
        } else if (event.getEventType() == SagaEventType.INVENTORY_RESERVATION_FAILED) {
            // Compensate: Cancel order
            orderDao.updateStatus(event.getSagaId(), OrderStatus.CANCELLED);
        }
    }

    /**
     * Handle payment processed event
     */
    @KafkaListener(topics = "payment-events")
    @Transactional(rollbackFor = Throwable.class)
    public void onPaymentProcessed(SagaEvent event) {
        if (event.getEventType() == SagaEventType.PAYMENT_PROCESSED) {
            // Success: Complete order
            orderDao.updateStatus(event.getSagaId(), OrderStatus.COMPLETED);
        } else if (event.getEventType() == SagaEventType.PAYMENT_FAILED) {
            // Compensate: Release inventory + Cancel order
            kafkaTemplate.send("inventory-events",
                SagaEvent.builder()
                    .sagaId(event.getSagaId())
                    .eventType(SagaEventType.RELEASE_INVENTORY)
                    .build()
            );
            orderDao.updateStatus(event.getSagaId(), OrderStatus.CANCELLED);
        }
    }
}

/**
 * Inventory Service - Saga Participant
 */
@Component
@RequiredArgsConstructor
public class InventoryEventConsumer {

    private final InventoryManager inventoryManager;
    private final KafkaTemplate<String, SagaEvent> kafkaTemplate;

    @KafkaListener(topics = "inventory-events")
    @Transactional(rollbackFor = Throwable.class)
    public void onReserveInventory(SagaEvent event) {
        if (event.getEventType() == SagaEventType.RESERVE_INVENTORY) {
            try {
                // Reserve inventory
                inventoryManager.reserve(event.getPayload());

                // Publish success event
                kafkaTemplate.send("inventory-events",
                    SagaEvent.builder()
                        .sagaId(event.getSagaId())
                        .eventType(SagaEventType.INVENTORY_RESERVED)
                        .build()
                );
            } catch (Exception e) {
                // Publish failure event
                kafkaTemplate.send("inventory-events",
                    SagaEvent.builder()
                        .sagaId(event.getSagaId())
                        .eventType(SagaEventType.INVENTORY_RESERVATION_FAILED)
                        .build()
                );
            }
        } else if (event.getEventType() == SagaEventType.RELEASE_INVENTORY) {
            // Compensating transaction
            inventoryManager.release(event.getSagaId());
        }
    }
}
```

**Time to Implement**: 40-60 minutes

---

## Monitoring and Observability

### Pattern: Kafka Consumer Lag Monitoring

```java
@Component
@RequiredArgsConstructor
public class KafkaConsumerMetrics {

    private final MeterRegistry meterRegistry;
    private final AdminClient adminClient;

    /**
     * Monitor consumer lag for alerting
     */
    @Scheduled(fixedRate = 30000)  // Every 30 seconds
    public void monitorConsumerLag() {
        Map<TopicPartition, OffsetAndMetadata> offsets =
            adminClient.listConsumerGroupOffsets("smartadmin-consumer")
                .partitionsToOffsetAndMetadata()
                .get();

        offsets.forEach((partition, offset) -> {
            long currentOffset = offset.offset();
            long endOffset = getPartitionEndOffset(partition);
            long lag = endOffset - currentOffset;

            // Record metric
            meterRegistry.gauge("kafka.consumer.lag",
                Tags.of(
                    "topic", partition.topic(),
                    "partition", String.valueOf(partition.partition())
                ),
                lag
            );

            // Alert if lag > 1000
            if (lag > 1000) {
                log.warn("High consumer lag detected: topic={}, partition={}, lag={}",
                    partition.topic(), partition.partition(), lag);
            }
        });
    }
}
```

**Grafana Dashboard Metrics**:
- Consumer lag (per partition)
- Message throughput (messages/sec)
- Processing latency (p50, p95, p99)
- Error rate (%)
- DLQ message count

---

## Common Errors and Quick Fixes

### Error 1: Consumer Rebalancing Too Frequently

**Symptom**: `Consumer group rebalancing in progress`

**Cause**: `max.poll.interval.ms` too short for processing time

**Fix**:
```yaml
spring:
  kafka:
    consumer:
      properties:
        max.poll.interval.ms: 600000  # 10 minutes (was 300000)
        max.poll.records: 100  # Reduce batch size
```

---

### Error 2: Message Loss

**Symptom**: Messages not consumed after restart

**Cause**: `enable-auto-commit: true` + processing failure

**Fix**:
```yaml
spring:
  kafka:
    consumer:
      enable-auto-commit: false  # Manual commit only after success
```

```java
@KafkaListener(topics = "events")
public void consume(Event event, Acknowledgment ack) {
    try {
        process(event);
        ack.acknowledge();  // Commit only after success
    } catch (Exception e) {
        // Don't commit - will retry
    }
}
```

---

### Error 3: Out of Order Messages

**Symptom**: Messages processed in wrong order

**Cause**: Multiple partitions + parallel consumers

**Fix**: Use **single partition** for ordering-sensitive topics
```java
// Producer: Use same key for related messages
kafkaTemplate.send("orders", orderId.toString(), event);  // All events for orderId go to same partition
```

---

## Time Estimates

| Task | Implementation | Testing | Total |
|------|---------------|---------|-------|
| Kafka Setup | 10 min | 5 min | 15 min |
| RocketMQ Setup | 8 min | 4 min | 12 min |
| Idempotent Consumer | 10 min | 5 min | 15 min |
| Dead Letter Queue | 8 min | 2 min | 10 min |
| Retry Strategy | 5 min | 3 min | 8 min |
| Transaction Message | 15 min | 5 min | 20 min |
| Saga Pattern | 40 min | 20 min | 60 min |

---

**See Also**:
- [Cache Strategy Generator](../cache-strategy-generator/) - Caching patterns
- [CI/CD Pipeline Builder](../../devops/cicd-pipeline-builder/) - Deploy MQ in production
- [SmartAdmin Patterns](../../../../shared/knowledge/smartadmin-patterns.md) - Transaction management
