# Idempotency

Comprehensive guide to implementing idempotent message processing in SmartAdmin's Kafka integration.

## Overview

**Idempotency** ensures that processing the same message multiple times produces the same result as processing it once. This is critical for distributed systems where duplicate messages can occur due to:
- Network retries
- Producer retries
- Consumer rebalancing
- Kafka "at-least-once" delivery semantics

**Why Idempotency Matters**:
- Prevents duplicate orders, payments, notifications
- Ensures data consistency
- Enables safe retries
- Simplifies error recovery

**SmartAdmin Approach**: Multiple idempotency strategies depending on requirements.

---

## Understanding Duplicate Messages

### When Duplicates Occur

**Producer Side**:
```
Client sends message → Broker receives → Ack lost in network
Client retries → Duplicate message created
```

**Consumer Side**:
```
Consumer processes message → Crashes before committing offset
Rebalance occurs → Message reprocessed
```

**Kafka Guarantees**:
- **At-most-once**: Message may be lost (acks=0)
- **At-least-once**: Message may be duplicated (acks=1 or all)
- **Exactly-once**: Message processed once (transactions + idempotent producer)

### Example Duplicate Scenario

```java
// Producer sends order
kafkaProducerService.send("smart-admin-order", orderJson);
// ✅ Broker receives and stores
// ❌ Network failure - ack not received
// ⚠️ Producer retries automatically
// 💥 Duplicate order created!
```

**Without Idempotency**:
- Order #12345 created twice
- Customer charged twice
- Inventory decremented twice

**With Idempotency**:
- Second message detected as duplicate
- Processing skipped
- Single order created

---

## Idempotency Strategies

### Strategy Comparison

| Strategy | Pros | Cons | Use Case |
|----------|------|------|----------|
| **Redis Cache** | Fast, simple, TTL support | Redis dependency | High-volume, short retention |
| **Database Unique Constraint** | Durable, no extra dependency | DB overhead | Financial transactions |
| **Kafka Transactions** | Native exactly-once | Complex, performance cost | Critical operations |
| **Message Versioning** | Simple, stateless | Requires version tracking | Event sourcing |
| **Idempotent Operations** | No infrastructure needed | Limited applicability | Naturally idempotent ops |

---

## Pattern 1: Redis-Based Deduplication

### Implementation

**Configuration**:
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379

smart:
  kafka:
    idempotency:
      enabled: true
      ttl: 24h              # Deduplication window
      key-prefix: "kafka:processed:"
```

**Consumer Implementation**:
```java
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderListener extends AbstractKafkaListener {

    private final RedisTemplate<String, String> redisTemplate;
    private final OrderService orderService;

    @KafkaListener(
        topics = KafkaConst.Topic.ORDER,
        groupId = KafkaConst.Group.ORDER
    )
    public void onMessage(ConsumerRecord<String, String> record) {
        handleMessage(record);
    }

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        String messageId = record.key();  // Unique message ID
        String cacheKey = "kafka:processed:order:" + messageId;

        // Check if already processed
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
            log.warn("⚠️ Duplicate message detected, skipping | MessageId: {}", messageId);
            return;
        }

        // Process message
        OrderDTO order = JSON.parseObject(record.value(), OrderDTO.class);
        orderService.createOrder(order);

        // Mark as processed (24 hour TTL)
        redisTemplate.opsForValue().set(cacheKey, "1", 24, TimeUnit.HOURS);
        log.info("✅ Message processed and cached | MessageId: {}", messageId);
    }
}
```

### Enhanced Redis Pattern with Atomic Operations

**Problem**: Race condition between check and set

**Solution**: Use Redis Lua script for atomicity

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class IdempotencyGuard {

    private final StringRedisTemplate redisTemplate;

    private static final String LUA_SCRIPT =
        "if redis.call('exists', KEYS[1]) == 1 then " +
        "    return 0 " +
        "else " +
        "    redis.call('setex', KEYS[1], ARGV[1], ARGV[2]) " +
        "    return 1 " +
        "end";

    /**
     * Atomically check and mark message as processed
     * @param messageId Unique message identifier
     * @param ttlSeconds TTL in seconds
     * @return true if this is first processing, false if duplicate
     */
    public boolean tryProcess(String messageId, long ttlSeconds) {
        String key = "kafka:processed:" + messageId;

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(LUA_SCRIPT);
        script.setResultType(Long.class);

        Long result = redisTemplate.execute(
            script,
            Collections.singletonList(key),
            String.valueOf(ttlSeconds),
            "1"
        );

        return result != null && result == 1L;
    }
}
```

**Usage**:
```java
@Override
protected void doHandle(ConsumerRecord<String, String> record) {
    String messageId = record.key();

    // Atomic check-and-set
    if (!idempotencyGuard.tryProcess(messageId, 86400)) {  // 24 hours
        log.warn("Duplicate message, skipping: {}", messageId);
        return;
    }

    // Process message
    OrderDTO order = JSON.parseObject(record.value(), OrderDTO.class);
    orderService.createOrder(order);
}
```

---

## Pattern 2: Database Unique Constraint

### Database Schema

```sql
CREATE TABLE t_order_idempotency (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id VARCHAR(100) NOT NULL UNIQUE,  -- Idempotency key
    order_id BIGINT NOT NULL,
    processed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_processed_at (processed_at)
) ENGINE=InnoDB;

-- Auto-cleanup old records
CREATE EVENT cleanup_idempotency_records
ON SCHEDULE EVERY 1 DAY
DO
    DELETE FROM t_order_idempotency
    WHERE processed_at < DATE_SUB(NOW(), INTERVAL 7 DAY);
```

### Entity

```java
@Data
@TableName("t_order_idempotency")
public class OrderIdempotencyEntity extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("message_id")
    private String messageId;

    @TableField("order_id")
    private Long orderId;

    @TableField("processed_at")
    private LocalDateTime processedAt;
}
```

### Service Implementation

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderIdempotencyService {

    private final OrderIdempotencyDao orderIdempotencyDao;

    /**
     * Check if message already processed
     */
    public boolean isProcessed(String messageId) {
        LambdaQueryWrapper<OrderIdempotencyEntity> wrapper =
            Wrappers.<OrderIdempotencyEntity>lambdaQuery()
                .eq(OrderIdempotencyEntity::getMessageId, messageId);

        return orderIdempotencyDao.selectCount(wrapper) > 0;
    }

    /**
     * Record message as processed
     */
    public void recordProcessed(String messageId, Long orderId) {
        OrderIdempotencyEntity entity = new OrderIdempotencyEntity();
        entity.setMessageId(messageId);
        entity.setOrderId(orderId);
        entity.setProcessedAt(LocalDateTime.now());

        orderIdempotencyDao.insert(entity);
    }
}
```

### Consumer with Database Idempotency

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderListener extends AbstractKafkaListener {

    private final OrderIdempotencyService idempotencyService;
    private final OrderManager orderManager;

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        String messageId = record.key();

        // Check if already processed
        if (idempotencyService.isProcessed(messageId)) {
            log.warn("Duplicate order message, skipping | MessageId: {}", messageId);
            return;
        }

        // Parse and create order
        OrderDTO order = JSON.parseObject(record.value(), OrderDTO.class);
        Long orderId = orderManager.createOrderWithIdempotency(messageId, order);

        log.info("Order created successfully | MessageId: {} | OrderId: {}",
            messageId, orderId);
    }
}
```

### Manager with Transactional Idempotency

```java
@Service
@RequiredArgsConstructor
public class OrderManager {

    private final OrderDao orderDao;
    private final OrderIdempotencyDao idempotencyDao;

    @Transactional(rollbackFor = Throwable.class)
    public Long createOrderWithIdempotency(String messageId, OrderDTO orderDTO) {
        // Create order
        OrderEntity orderEntity = SmartBeanUtil.copy(orderDTO, OrderEntity.class);
        orderDao.insert(orderEntity);

        // Record idempotency (will fail if duplicate due to unique constraint)
        OrderIdempotencyEntity idempotencyEntity = new OrderIdempotencyEntity();
        idempotencyEntity.setMessageId(messageId);
        idempotencyEntity.setOrderId(orderEntity.getId());
        idempotencyEntity.setProcessedAt(LocalDateTime.now());

        try {
            idempotencyDao.insert(idempotencyEntity);
        } catch (DuplicateKeyException e) {
            // Duplicate detected - rollback transaction
            log.warn("Duplicate message detected during transaction | MessageId: {}",
                messageId);
            throw new BusinessException(OrderErrorCode.DUPLICATE_ORDER);
        }

        return orderEntity.getId();
    }
}
```

---

## Pattern 3: Kafka Idempotent Producer

### Configuration

**Enable idempotent producer**:
```yaml
smart:
  kafka:
    producer:
      enable-idempotence: true    # Default: false
      acks: all                   # Required for idempotence
      retries: 2147483647         # Max retries
      max-in-flight-requests-per-connection: 5  # Default: 5
```

**What Idempotent Producer Does**:
- Assigns sequence number to each message
- Broker detects and rejects duplicates
- Transparent to application code
- Only prevents producer-side duplicates

**Limitations**:
- Only prevents duplicates within same producer session
- Does not prevent consumer-side duplicates
- Requires additional consumer-side deduplication

### Combined Pattern

```java
// Producer: Enable idempotent producer
@Configuration
public class KafkaProducerConfig {
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        return new DefaultKafkaProducerFactory<>(config);
    }
}

// Consumer: Redis-based deduplication
@Override
protected void doHandle(ConsumerRecord<String, String> record) {
    String messageId = record.key();

    if (!idempotencyGuard.tryProcess(messageId, 86400)) {
        return;  // Duplicate
    }

    processOrder(record.value());
}
```

---

## Pattern 4: Message Versioning

### Use Case

Event sourcing or audit trail where order matters:

```java
@Data
public class OrderEventDTO {
    private String orderId;
    private String eventType;       // CREATED, UPDATED, CANCELLED
    private Long version;            // Monotonically increasing
    private LocalDateTime timestamp;
    private OrderDTO payload;
}
```

### Versioned Processing

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventListener extends AbstractKafkaListener {

    private final OrderVersionDao orderVersionDao;
    private final OrderService orderService;

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        OrderEventDTO event = JSON.parseObject(record.value(), OrderEventDTO.class);
        String orderId = event.getOrderId();
        Long messageVersion = event.getVersion();

        // Get current version from database
        Long currentVersion = orderVersionDao.getCurrentVersion(orderId);

        if (currentVersion != null && messageVersion <= currentVersion) {
            log.warn("⚠️ Stale or duplicate event, skipping | OrderId: {} | " +
                "MessageVersion: {} | CurrentVersion: {}",
                orderId, messageVersion, currentVersion);
            return;
        }

        // Process event
        orderService.processEvent(event);

        // Update version
        orderVersionDao.updateVersion(orderId, messageVersion);

        log.info("✅ Event processed | OrderId: {} | Version: {}",
            orderId, messageVersion);
    }
}
```

**Version Table**:
```sql
CREATE TABLE t_order_version (
    order_id VARCHAR(100) PRIMARY KEY,
    current_version BIGINT NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;
```

---

## Pattern 5: Naturally Idempotent Operations

### Examples of Idempotent Operations

**SET operations** (not INCREMENT):
```java
// ✅ Idempotent: Setting status
order.setStatus(OrderStatus.CONFIRMED);
orderDao.updateById(order);

// ❌ Not idempotent: Incrementing count
order.setRetryCount(order.getRetryCount() + 1);
orderDao.updateById(order);
```

**UPDATE with conditions**:
```sql
-- ✅ Idempotent: Update only if in specific state
UPDATE t_order
SET status = 'CONFIRMED', confirmed_at = NOW()
WHERE order_id = ? AND status = 'PENDING'

-- Result: 1 row updated on first execution, 0 rows on subsequent
```

**DELETE operations**:
```java
// ✅ Naturally idempotent
orderDao.deleteById(orderId);
// Second delete has no effect
```

### Implementation Pattern

```java
@Service
@RequiredArgsConstructor
public class OrderManager {

    private final OrderDao orderDao;

    /**
     * Idempotent order confirmation
     * Returns true if state changed, false if already confirmed
     */
    @Transactional(rollbackFor = Throwable.class)
    public boolean confirmOrder(String orderId) {
        // Only update if currently PENDING
        LambdaUpdateWrapper<OrderEntity> wrapper =
            Wrappers.<OrderEntity>lambdaUpdate()
                .eq(OrderEntity::getOrderId, orderId)
                .eq(OrderEntity::getStatus, OrderStatus.PENDING)
                .set(OrderEntity::getStatus, OrderStatus.CONFIRMED)
                .set(OrderEntity::getConfirmedAt, LocalDateTime.now());

        int rowsUpdated = orderDao.update(null, wrapper);

        if (rowsUpdated == 0) {
            log.info("Order already confirmed or not found | OrderId: {}", orderId);
            return false;
        }

        log.info("Order confirmed | OrderId: {}", orderId);
        return true;
    }
}
```

---

## Choosing the Right Strategy

### Decision Matrix

**Use Redis Cache when**:
- ✅ High message volume (>1000 msg/sec)
- ✅ Short deduplication window (hours to days)
- ✅ Fast lookup critical
- ✅ Redis already in use

**Use Database Constraint when**:
- ✅ Financial or critical transactions
- ✅ Long-term deduplication needed (weeks/months)
- ✅ Audit trail required
- ✅ No Redis available

**Use Kafka Idempotent Producer when**:
- ✅ Preventing producer retries
- ✅ Combined with consumer deduplication
- ✅ Exactly-once semantics needed

**Use Message Versioning when**:
- ✅ Event sourcing architecture
- ✅ Order of events matters
- ✅ Audit trail with versions

**Use Naturally Idempotent Operations when**:
- ✅ Operations are inherently idempotent
- ✅ Simplicity preferred
- ✅ No external dependencies desired

### Hybrid Approach (Recommended)

**Production Pattern**:
```
1. Kafka Idempotent Producer (prevent producer duplicates)
   ↓
2. Redis Cache (fast deduplication, 24h TTL)
   ↓
3. Database Unique Constraint (safety net)
   ↓
4. Idempotent Operations (wherever possible)
```

---

## Performance Considerations

### Redis Pattern Performance

**Benchmarks** (single Redis instance):
- **Lookup**: ~0.5ms average
- **Set**: ~0.7ms average
- **Throughput**: 50,000+ ops/sec

**Optimization**:
```java
// Use Redis pipelining for batch checks
@Override
protected void doBatchHandle(List<ConsumerRecord<String, String>> records) {
    // Prepare pipeline
    List<Object> results = redisTemplate.executePipelined(
        new SessionCallback<Object>() {
            @Override
            public <K, V> Object execute(RedisOperations<K, V> operations) {
                records.forEach(record -> {
                    String key = "kafka:processed:" + record.key();
                    operations.hasKey((K) key);
                });
                return null;
            }
        }
    );

    // Process non-duplicates
    for (int i = 0; i < records.size(); i++) {
        if (Boolean.FALSE.equals(results.get(i))) {
            processRecord(records.get(i));
        }
    }
}
```

### Database Pattern Performance

**Benchmarks** (MySQL with index):
- **Lookup**: ~2-5ms average
- **Insert**: ~3-7ms average
- **Throughput**: 5,000-10,000 ops/sec

**Optimization**:
```java
// Use batch insert with IGNORE
INSERT IGNORE INTO t_order_idempotency (message_id, order_id, processed_at)
VALUES
  ('msg-1', 12345, NOW()),
  ('msg-2', 12346, NOW()),
  ('msg-3', 12347, NOW());
```

### Cost-Benefit Analysis

| Strategy | Setup Cost | Runtime Cost | Maintenance | Reliability |
|----------|-----------|--------------|-------------|-------------|
| Redis | Low | Very Low | Low | High |
| Database | Medium | Low-Medium | Medium | Very High |
| Kafka Transactions | High | Medium-High | High | Very High |
| Versioning | Medium | Low | Low | High |
| Natural Idempotency | Low | None | None | Medium |

---

## Best Practices

### 1. Always Use Message Keys

```java
// ✅ Good: Unique message key
String messageKey = "ORDER-" + order.getOrderId();
kafkaProducerService.send(topic, messageKey, messageJson);

// ❌ Bad: No key (random partition, can't deduplicate)
kafkaProducerService.send(topic, messageJson);
```

### 2. TTL Should Match Business Requirements

```java
// ✅ Financial transactions: Long TTL
redisTemplate.opsForValue().set(key, "1", 30, TimeUnit.DAYS);

// ✅ Metrics/logs: Short TTL
redisTemplate.opsForValue().set(key, "1", 1, TimeUnit.HOURS);
```

### 3. Monitor Duplicate Rate

```java
@Component
@RequiredArgsConstructor
public class IdempotencyMetrics {

    private final MeterRegistry meterRegistry;

    public void recordDuplicate(String topic) {
        meterRegistry.counter("kafka.duplicates.detected",
            "topic", topic
        ).increment();
    }
}
```

**Alert if duplicate rate > 1%**:
```yaml
- alert: HighDuplicateRate
  expr: rate(kafka_duplicates_detected_total[5m]) / rate(kafka_messages_consumed_total[5m]) > 0.01
  annotations:
    summary: "High duplicate message rate detected"
```

### 4. Handle Edge Cases

```java
@Override
protected void doHandle(ConsumerRecord<String, String> record) {
    String messageId = record.key();

    // Edge case: Missing key
    if (StringUtils.isBlank(messageId)) {
        log.error("❌ Message key is null/empty, cannot deduplicate | " +
            "Topic: {} | Partition: {} | Offset: {}",
            record.topic(), record.partition(), record.offset());
        // Decision: Process anyway or send to DLQ?
        return;
    }

    // Edge case: Redis temporarily unavailable
    try {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
            return;  // Duplicate
        }
    } catch (Exception e) {
        log.error("⚠️ Redis check failed, processing anyway | MessageId: {}",
            messageId, e);
        // Fallback: Process message (risk duplicate vs. risk message loss)
    }

    // Process message
    processOrder(record.value());

    // Try to cache result (best effort)
    try {
        redisTemplate.opsForValue().set(cacheKey, "1", 24, TimeUnit.HOURS);
    } catch (Exception e) {
        log.error("⚠️ Failed to cache processed message | MessageId: {}",
            messageId, e);
    }
}
```

### 5. Test Duplicate Scenarios

```java
@SpringBootTest
class OrderListenerIdempotencyTest {

    @Autowired
    private OrderListener orderListener;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private OrderDao orderDao;

    @Test
    void testDuplicateMessageIgnored() {
        // Arrange
        String messageId = "ORDER-12345";
        String orderJson = "{\"orderId\":\"12345\",\"amount\":100}";
        ConsumerRecord<String, String> record =
            new ConsumerRecord<>("smart-admin-order", 0, 0, messageId, orderJson);

        // Act: Process first time
        orderListener.doHandle(record);
        long countAfterFirst = orderDao.selectCount(null);

        // Act: Process second time (duplicate)
        orderListener.doHandle(record);
        long countAfterSecond = orderDao.selectCount(null);

        // Assert: Only one order created
        assertThat(countAfterFirst).isEqualTo(1);
        assertThat(countAfterSecond).isEqualTo(1);
    }
}
```

---

## Troubleshooting

### Issue: Duplicate Messages Still Processed

**Symptoms**:
- Metrics show duplicates
- Database shows duplicate records

**Diagnosis**:
```bash
# Check Redis connectivity
redis-cli ping

# Check cache hit rate
redis-cli INFO stats | grep keyspace_hits

# Verify message keys are unique
docker exec smart-admin-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic smart-admin-order \
  --property print.key=true \
  --property key.separator=":" \
  --max-messages 10
```

**Solutions**:
1. **Missing keys**: Ensure producer always sets message key
2. **Redis down**: Check Redis health, add fallback logic
3. **Race condition**: Use atomic Redis operations (Lua script)
4. **TTL too short**: Increase deduplication window

---

### Issue: Redis Memory Exhausted

**Symptoms**:
```
OOM command not allowed when used memory > 'maxmemory'
```

**Solutions**:

**1. Configure eviction policy**:
```yaml
# redis.conf
maxmemory 2gb
maxmemory-policy allkeys-lru  # Evict least recently used
```

**2. Use shorter TTLs**:
```java
// Reduce from 24h to 6h
redisTemplate.opsForValue().set(key, "1", 6, TimeUnit.HOURS);
```

**3. Clean up old keys**:
```java
@Scheduled(cron = "0 0 2 * * *")  // 2 AM daily
public void cleanupOldKeys() {
    Set<String> keys = redisTemplate.keys("kafka:processed:*");
    // Delete keys older than TTL
}
```

---

## See Also

- [Error Handling](/kafka/guides/error-handling) - Exception handling strategies
- [Best Practices](/kafka/guides/best-practices) - Production recommendations
- [Monitoring](/kafka/operations/monitoring) - Track duplicate rates
- [Transactions](/kafka/advanced/transactions) - Kafka transaction support
- [Testing](/kafka/testing/unit-testing) - Unit testing idempotency

---

**Last Updated**: 2026-01-22
