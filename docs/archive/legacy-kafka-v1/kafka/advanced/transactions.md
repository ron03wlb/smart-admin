# Transactions

Transactional message patterns and exactly-once semantics in SmartAdmin's Kafka integration.

## Overview

**Kafka transactions** provide atomicity guarantees for:
- Multiple message produces to multiple topics
- Consumer offset commits combined with message produces
- Exactly-once processing semantics (EOS)

**SmartAdmin Use Cases**:
- Critical business transactions (orders, payments)
- Multi-topic coordination (saga patterns)
- Exactly-once data pipelines

**Trade-offs**:
- ✅ **Pros**: Strong consistency, exactly-once guarantees
- ❌ **Cons**: Higher latency, increased complexity, reduced throughput

---

## Kafka Transaction Concepts

### Transaction Guarantees

**Without Transactions**:
```java
// Problem: If producer crashes between sends, partial state
kafkaProducerService.send("topic-order", orderMsg);        // ✅ Sent
// 💥 Crash here
kafkaProducerService.send("topic-inventory", inventoryMsg); // ❌ Not sent
// Result: Inconsistent state
```

**With Transactions**:
```java
// Solution: All or nothing
transactionalProducer.beginTransaction();
try {
    transactionalProducer.send("topic-order", orderMsg);
    transactionalProducer.send("topic-inventory", inventoryMsg);
    transactionalProducer.commitTransaction();  // ✅ Both visible atomically
} catch (Exception e) {
    transactionalProducer.abortTransaction();  // ❌ Neither visible
}
```

### Transaction Coordinator

**How it works**:
1. Producer requests **transaction ID** from coordinator
2. Producer **begins transaction**
3. Messages marked as **transactional** (not visible yet)
4. Producer **commits** → messages become visible atomically
5. Or **aborts** → messages discarded

**Visual Flow**:
```
Producer → TC: Begin transaction
Producer → Broker: Send msg1 (uncommitted)
Producer → Broker: Send msg2 (uncommitted)
Producer → TC: Commit transaction
TC → Brokers: Mark messages committed
Consumers: See msg1 and msg2 atomically
```

---

## Configuration

### Producer Configuration

**File**: `sa-admin/src/main/resources/dev/sa-base.yaml`

```yaml
smart:
  kafka:
    producer:
      # Enable transactions
      transactional-id: smart-admin-tx-${spring.application.name}-${random.uuid}
      enable-idempotence: true    # Required for transactions
      acks: all                   # Required for transactions
      max-in-flight-requests-per-connection: 5

      # Transaction timeout
      transaction-timeout-ms: 60000  # 60 seconds
```

**Key Properties**:
- **transactional-id**: Unique identifier per producer instance
  - Must be unique across all producer instances
  - Enables exactly-once semantics
  - Use dynamic suffix (UUID) for multiple instances

- **enable-idempotence**: Must be true for transactions
- **acks**: Must be "all" for transaction durability

### Consumer Configuration

**File**: `sa-admin/src/main/resources/dev/sa-base.yaml`

```yaml
smart:
  kafka:
    consumer:
      # Read only committed messages
      isolation-level: read_committed    # Default: read_uncommitted

      # Disable auto-commit (manual commit in transaction)
      enable-auto-commit: false
      auto-commit-interval-ms: 0
```

**Isolation Levels**:
- **read_uncommitted** (default): See all messages immediately
- **read_committed**: See only committed transactional messages

---

## Pattern 1: Transactional Producer (Write-Only)

### Use Case

Atomic writes to multiple topics without consuming:

```
Order Service produces:
  topic-order → order created
  topic-inventory → inventory reserved
  topic-notification → email notification queued
```

### Implementation

**Transactional Producer Factory**:
```java
@Configuration
public class KafkaTransactionalProducerConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    public ProducerFactory<String, String> transactionalProducerFactory(
        KafkaProperties kafkaProperties
    ) {
        Map<String, Object> config = new HashMap<>();
        config.putAll(kafkaProperties.buildProducerProperties());

        // Transactional configuration
        config.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG,
            "smart-admin-tx-" + applicationName + "-" + UUID.randomUUID());
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.ACKS_CONFIG, "all");

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, String> transactionalKafkaTemplate(
        ProducerFactory<String, String> transactionalProducerFactory
    ) {
        return new KafkaTemplate<>(transactionalProducerFactory);
    }
}
```

**Service with Transactions**:
```java
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderTransactionalService {

    private final KafkaTemplate<String, String> transactionalKafkaTemplate;

    @Transactional  // Spring transaction (database)
    public void createOrderWithKafkaTransaction(OrderDTO orderDTO) {
        // 1. Save to database (Spring transaction)
        OrderEntity orderEntity = saveOrderToDatabase(orderDTO);

        // 2. Send Kafka messages in transaction
        transactionalKafkaTemplate.executeInTransaction(operations -> {
            try {
                // Send order created event
                String orderMsg = JSON.toJSONString(orderEntity);
                operations.send(KafkaConst.Topic.ORDER, orderEntity.getOrderId(), orderMsg);

                // Reserve inventory
                InventoryReservationDTO reservation = buildReservation(orderEntity);
                String inventoryMsg = JSON.toJSONString(reservation);
                operations.send(KafkaConst.Topic.INVENTORY, orderEntity.getOrderId(), inventoryMsg);

                // Queue notification
                NotificationDTO notification = buildNotification(orderEntity);
                String notificationMsg = JSON.toJSONString(notification);
                operations.send(KafkaConst.Topic.NOTIFICATION, orderEntity.getOrderId(), notificationMsg);

                log.info("✅ All messages sent in transaction | OrderId: {}", orderEntity.getOrderId());
                return true;

            } catch (Exception e) {
                log.error("❌ Transaction failed, rolling back | OrderId: {}", orderEntity.getOrderId(), e);
                throw e;  // Rollback Kafka transaction
            }
        });
    }
}
```

---

## Pattern 2: Read-Process-Write (Exactly-Once)

### Use Case

Consumer reads, processes, and writes atomically:

```
Read from topic-order
  ↓
Process order
  ↓
Write to topic-payment (atomically with offset commit)
```

### Implementation

**Configuration**:
```yaml
smart:
  kafka:
    producer:
      transactional-id: smart-admin-order-processor-${random.uuid}
      enable-idempotence: true
      acks: all

    consumer:
      isolation-level: read_committed
      enable-auto-commit: false  # Manual commit in transaction
```

**Transactional Listener**:
```java
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderProcessorListener {

    private final KafkaTemplate<String, String> transactionalKafkaTemplate;
    private final OrderService orderService;

    @KafkaListener(
        topics = KafkaConst.Topic.ORDER,
        groupId = "order-processor-group",
        containerFactory = "transactionalKafkaListenerContainerFactory"
    )
    public void processOrderTransactionally(
        ConsumerRecord<String, String> record,
        Acknowledgment ack,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset
    ) {
        String orderId = record.key();

        // Execute in Kafka transaction
        transactionalKafkaTemplate.executeInTransaction(operations -> {
            try {
                // 1. Process message
                OrderDTO order = JSON.parseObject(record.value(), OrderDTO.class);
                PaymentRequestDTO paymentRequest = orderService.processOrderForPayment(order);

                // 2. Send to payment topic
                String paymentMsg = JSON.toJSONString(paymentRequest);
                operations.send(KafkaConst.Topic.PAYMENT, orderId, paymentMsg);

                // 3. Commit offset (part of transaction)
                ack.acknowledge();

                log.info("✅ Exactly-once processing | OrderId: {} | Topic: {} | Partition: {} | Offset: {}",
                    orderId, topic, partition, offset);

                return true;

            } catch (Exception e) {
                log.error("❌ Processing failed, transaction aborted | OrderId: {}", orderId, e);
                throw e;  // Abort transaction, offset not committed
            }
        });
    }
}
```

**Container Factory**:
```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, String> transactionalKafkaListenerContainerFactory(
    ConsumerFactory<String, String> consumerFactory,
    KafkaTemplate<String, String> transactionalKafkaTemplate
) {
    ConcurrentKafkaListenerContainerFactory<String, String> factory =
        new ConcurrentKafkaListenerContainerFactory<>();

    factory.setConsumerFactory(consumerFactory);
    factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

    // Enable transaction support
    factory.getContainerProperties().setKafkaAwareTransactionManager(
        new KafkaTransactionManager<>(transactionalKafkaTemplate.getProducerFactory())
    );

    return factory;
}
```

---

## Spring @Transactional Integration

### Database + Kafka Dual Transactions

**Problem**: Coordinate database and Kafka transactions

**ChainedTransactionManager Pattern** (Spring 5+):
```java
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderManager {

    private final OrderDao orderDao;
    private final KafkaTemplate<String, String> transactionalKafkaTemplate;
    private final DataSourceTransactionManager dbTransactionManager;
    private final KafkaTransactionManager<String, String> kafkaTransactionManager;

    /**
     * Execute database and Kafka operations in coordinated transactions
     * Note: Not true 2PC, but best-effort coordination
     */
    @Transactional(transactionManager = "chainedTransactionManager", rollbackFor = Throwable.class)
    public void createOrderWithDualTransaction(OrderDTO orderDTO) {
        // 1. Database operation (controlled by Spring @Transactional)
        OrderEntity orderEntity = SmartBeanUtil.copy(orderDTO, OrderEntity.class);
        orderDao.insert(orderEntity);

        // 2. Kafka operation (controlled by KafkaTransactionManager)
        String orderMsg = JSON.toJSONString(orderEntity);
        transactionalKafkaTemplate.send(KafkaConst.Topic.ORDER, orderEntity.getOrderId(), orderMsg);

        log.info("✅ Dual transaction committed | OrderId: {}", orderEntity.getOrderId());
    }

    @Bean
    public ChainedTransactionManager chainedTransactionManager(
        DataSourceTransactionManager dbTransactionManager,
        KafkaTransactionManager<String, String> kafkaTransactionManager
    ) {
        return new ChainedTransactionManager(dbTransactionManager, kafkaTransactionManager);
    }
}
```

**Important**: ChainedTransactionManager is **not** true 2PC. If Kafka transaction fails after database commit, database changes are NOT rolled back.

### Saga Pattern (Recommended)

For true atomicity across database and Kafka, use **Saga pattern**:

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderSagaService {

    private final OrderDao orderDao;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public void createOrderSaga(OrderDTO orderDTO) {
        String orderId = orderDTO.getOrderId();

        // 1. Send message FIRST (no database dependency)
        kafkaTemplate.executeInTransaction(operations -> {
            String orderMsg = JSON.toJSONString(orderDTO);
            operations.send(KafkaConst.Topic.ORDER_SAGA, orderId, orderMsg);
            return true;
        });

        // 2. Listener processes message and updates database
        // If database update fails, send compensating transaction
    }
}

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderSagaListener extends AbstractKafkaListener {

    private final OrderManager orderManager;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @KafkaListener(topics = KafkaConst.Topic.ORDER_SAGA, groupId = "order-saga-group")
    public void onMessage(ConsumerRecord<String, String> record) {
        handleMessage(record);
    }

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        OrderDTO orderDTO = JSON.parseObject(record.value(), OrderDTO.class);
        String orderId = orderDTO.getOrderId();

        try {
            // Try to save to database
            orderManager.createOrder(orderDTO);
            log.info("✅ Saga completed successfully | OrderId: {}", orderId);

        } catch (Exception e) {
            log.error("❌ Saga failed, sending compensating transaction | OrderId: {}", orderId, e);

            // Send compensating transaction
            CompensationDTO compensation = new CompensationDTO(orderId, "ORDER_CREATION_FAILED");
            String compensationMsg = JSON.toJSONString(compensation);
            kafkaTemplate.send(KafkaConst.Topic.ORDER_COMPENSATION, orderId, compensationMsg);
        }
    }
}
```

---

## Performance Considerations

### Benchmarks

**Without Transactions**:
- Latency: 5-10ms per message
- Throughput: 50,000 msg/sec

**With Transactions**:
- Latency: 20-50ms per transaction
- Throughput: 5,000-10,000 tx/sec
- **Impact**: ~4-5x latency increase, ~5-10x throughput reduction

### Optimization Strategies

**1. Batch within Transaction**:
```java
transactionalKafkaTemplate.executeInTransaction(operations -> {
    // Send multiple messages in single transaction
    for (OrderDTO order : orders) {
        String msg = JSON.toJSONString(order);
        operations.send(KafkaConst.Topic.ORDER, order.getOrderId(), msg);
    }
    return true;
});
// Cost: 1 transaction for N messages instead of N transactions
```

**2. Adjust Transaction Timeout**:
```yaml
smart:
  kafka:
    producer:
      transaction-timeout-ms: 30000  # 30s for long batch operations
```

**3. Use Non-Transactional for Non-Critical Operations**:
```java
// Critical: Use transactions
transactionalProducer.send(orderTopic, orderMsg);

// Non-critical: Skip transactions
normalProducer.send(metricsTopic, metricsMsg);
```

---

## Best Practices

### 1. Use Unique Transactional IDs

```java
// ✅ Good: UUID suffix prevents conflicts
"smart-admin-tx-" + applicationName + "-" + UUID.randomUUID()

// ❌ Bad: Multiple instances conflict
"smart-admin-tx-fixed-id"
```

### 2. Set Appropriate Timeouts

```yaml
smart:
  kafka:
    producer:
      transaction-timeout-ms: 60000  # Match business logic duration
      delivery-timeout-ms: 120000    # Should be > transaction-timeout-ms
```

### 3. Handle Transaction Failures

```java
try {
    transactionalKafkaTemplate.executeInTransaction(operations -> {
        // Transaction logic
        return true;
    });
} catch (ProducerFencedException | OutOfOrderSequenceException e) {
    // Fatal: Cannot recover, close producer
    log.error("Fatal transaction error", e);
    throw e;
} catch (KafkaException e) {
    // Retriable: Retry transaction
    log.warn("Transaction failed, retrying", e);
    retryTransaction();
}
```

### 4. Monitor Transaction Metrics

```java
@Component
@RequiredArgsConstructor
public class TransactionMetrics {

    private final MeterRegistry meterRegistry;

    public void recordTransactionCommit(boolean success) {
        meterRegistry.counter("kafka.transactions.commits",
            "status", success ? "success" : "failure"
        ).increment();
    }

    public void recordTransactionDuration(long durationMs) {
        meterRegistry.timer("kafka.transactions.duration").record(durationMs, TimeUnit.MILLISECONDS);
    }
}
```

---

## Trade-Offs

| Aspect | Without Transactions | With Transactions |
|--------|---------------------|-------------------|
| **Consistency** | At-least-once | Exactly-once |
| **Latency** | 5-10ms | 20-50ms |
| **Throughput** | 50k msg/sec | 5-10k tx/sec |
| **Complexity** | Low | High |
| **Use Case** | Logs, metrics, events | Orders, payments, critical data |

---

## Troubleshooting

### Issue: Transaction Timeout

**Symptom**:
```
org.apache.kafka.common.errors.TimeoutException: Transaction timeout expired
```

**Solution**:
```yaml
smart:
  kafka:
    producer:
      transaction-timeout-ms: 120000  # Increase timeout
```

---

### Issue: Producer Fenced

**Symptom**:
```
org.apache.kafka.common.errors.ProducerFencedException
```

**Cause**: Another producer with same transactional ID started

**Solution**: Ensure unique transactional IDs using UUID suffix

---

## See Also

- [Idempotency](/kafka/advanced/idempotency) - Deduplication strategies
- [Error Handling](/kafka/guides/error-handling) - Exception handling
- [Best Practices](/kafka/guides/best-practices) - Production recommendations
- [Monitoring](/kafka/operations/monitoring) - Transaction metrics

---

**Last Updated**: 2026-01-22
