# Common Issues and Solutions

Comprehensive guide to diagnosing and resolving the most common Kafka integration issues in SmartAdmin.

## Overview

This document covers the 7 most common Kafka issues and their solutions:

1. **Connection failures** - Application can't connect to Kafka
2. **Message send failures** - Producer errors and timeouts
3. **Consumer not receiving messages** - Consumer subscription issues
4. **Consumer lag** - Message backlog accumulation
5. **DLQ accumulation** - Dead letter queue growing
6. **Duplicate messages** - Messages processed multiple times
7. **Disk space issues** - Kafka storage exhaustion

---

## Issue 1: Kafka Connection Failure

### Symptoms

```
ERROR: Connection to node -1 (localhost/127.0.0.1:9092) could not be established
org.apache.kafka.common.errors.TimeoutException: Failed to update metadata after 60000 ms
```

**Application behavior**:
- ❌ Application fails to start
- ❌ Producer send() throws exceptions
- ❌ Consumer never receives messages

### Root Causes

| Cause | Likelihood | Impact |
|-------|-----------|--------|
| Kafka service not running | 70% | Critical |
| Incorrect bootstrap-servers config | 20% | Critical |
| Network/firewall blocking | 5% | Critical |
| DNS resolution failure | 5% | Critical |

### Diagnosis Steps

**Step 1: Verify Kafka is running**
```bash
# Check Kafka container status
docker ps | grep kafka

# Expected output:
# smart-admin-kafka   Up (healthy)   9092/tcp

# If not running:
docker-compose ps kafka
```

**Step 2: Check connectivity**
```bash
# Test port connectivity
telnet localhost 9092

# Expected: "Connected to localhost"
# If failed: "Connection refused"

# Alternative test
nc -zv localhost 9092
```

**Step 3: Verify configuration**
```bash
# Check SmartAdmin configuration
cat sa-admin/src/main/resources/dev/sa-base.yaml | grep bootstrap-servers

# Expected:
# bootstrap-servers: localhost:9092

# Check actual Kafka listener
docker logs smart-admin-kafka | grep "Kafka Server started"
```

**Step 4: Check DNS resolution** (Docker environment)
```bash
# If using Docker network
docker exec smart-admin-app ping kafka

# If fails, check network configuration
docker network inspect smart-admin-network
```

### Solutions

**Solution 1: Start Kafka service**
```bash
cd docker
docker-compose up -d kafka

# Wait for health check
docker-compose ps kafka
# STATUS should show "Up (healthy)"
```

**Solution 2: Fix configuration**
```yaml
# File: sa-admin/src/main/resources/dev/sa-base.yaml
smart:
  kafka:
    # Localhost (running on host)
    bootstrap-servers: localhost:9092

    # Docker container name (same network)
    bootstrap-servers: kafka:9092

    # Multiple brokers
    bootstrap-servers: kafka-1:9092,kafka-2:9092,kafka-3:9092
```

**Solution 3: Fix Docker networking**
```yaml
# File: docker/docker-compose.yml
services:
  kafka:
    networks:
      - smart-admin-network

  smart-admin:
    networks:
      - smart-admin-network
    environment:
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092  # Use container name

networks:
  smart-admin-network:
    driver: bridge
```

**Solution 4: Check firewall** (production)
```bash
# Allow Kafka port
sudo ufw allow 9092/tcp

# Check iptables
sudo iptables -L -n | grep 9092
```

---

## Issue 2: Message Send Timeout

### Symptoms

```
ERROR: Failed to send message
org.apache.kafka.common.errors.TimeoutException: Expiring 1 record(s) for smart-admin-order
```

**Application behavior**:
- ⚠️ Producer.send() throws TimeoutException
- ⚠️ Some messages succeed, others fail
- ⚠️ Intermittent failures

### Root Causes

| Cause | Likelihood | Impact |
|-------|-----------|--------|
| Topic doesn't exist (auto-create disabled) | 40% | High |
| Kafka broker overloaded | 30% | High |
| Network latency too high | 15% | Medium |
| Request timeout too short | 10% | Low |
| Broker disk full | 5% | Critical |

### Diagnosis Steps

**Step 1: Check topic exists**
```bash
docker exec smart-admin-kafka kafka-topics \
  --bootstrap-server localhost:9092 --list

# Check if your topic is in the list
```

**Step 2: Check broker status**
```bash
# Check broker logs
docker logs smart-admin-kafka --tail 100

# Look for:
# ✅ "Kafka Server started"
# ❌ "OutOfMemoryError"
# ❌ "No space left on device"
```

**Step 3: Check producer configuration**
```yaml
# Check timeout settings
smart:
  kafka:
    producer:
      request-timeout-ms: 30000      # 30 seconds
      retries: 3
      max-block-ms: 60000            # Max wait for buffer
```

**Step 4: Test with Kafka console producer**
```bash
# Manual send test
docker exec -it smart-admin-kafka kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic smart-admin-order

# Type a message and press Enter
# If this also times out, issue is with Kafka, not your app
```

### Solutions

**Solution 1: Create topic manually**
```bash
docker exec smart-admin-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --create \
  --topic smart-admin-order \
  --partitions 3 \
  --replication-factor 1
```

**Solution 2: Enable auto topic creation**
```yaml
# File: docker/docker-compose.yml (Kafka environment)
KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'true'
```

**Solution 3: Increase timeout**
```yaml
smart:
  kafka:
    producer:
      request-timeout-ms: 60000     # 60 seconds
      delivery-timeout-ms: 120000   # 2 minutes
```

**Solution 4: Check broker health**
```bash
# Check broker metrics
docker exec smart-admin-kafka kafka-broker-api-versions \
  --bootstrap-server localhost:9092

# Check disk space
docker exec smart-admin-kafka df -h

# Increase broker resources if needed
docker-compose up -d --scale kafka=1 --force-recreate
```

---

## Issue 3: Consumer Not Receiving Messages

### Symptoms

```
INFO: KafkaListenerEndpointRegistry initialized
# But NO "Processing message" logs appear
```

**Application behavior**:
- ✅ Application starts successfully
- ✅ Producer sends messages successfully
- ❌ Consumer never processes messages

### Root Causes

| Cause | Likelihood | Impact |
|-------|-----------|--------|
| Consumer offset already at latest | 50% | Medium |
| Consumer group ID mismatch | 20% | High |
| Topic subscription failed | 15% | High |
| Rebalance stuck/failing | 10% | High |
| No messages in topic | 5% | Low |

### Diagnosis Steps

**Step 1: Check consumer group status**
```bash
# List all consumer groups
docker exec smart-admin-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 --list

# Check specific group details
docker exec smart-admin-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group smart-admin-order-group

# Expected output:
# GROUP           TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
# smart-admin-... smart-admin-... 0          100             100             0
```

**Step 2: Check topic has messages**
```bash
# Get topic offset (message count)
docker exec smart-admin-kafka kafka-run-class \
  kafka.tools.GetOffsetShell \
  --broker-list localhost:9092 \
  --topic smart-admin-order

# Output shows offset per partition
# smart-admin-order:0:150  (150 messages in partition 0)
```

**Step 3: Check application logs**
```bash
# Check for consumer registration
grep "Assigned to partitions" logs/smart-admin.log

# Expected:
# INFO: Assigned to partitions: [smart-admin-order-0, smart-admin-order-1]
```

**Step 4: Verify listener configuration**
```java
@KafkaListener(
    topics = KafkaConst.Topic.ORDER,           // Correct topic?
    groupId = KafkaConst.Group.ORDER,          // Correct group?
    containerFactory = "kafkaListenerContainerFactory"  // Correct factory?
)
```

### Solutions

**Solution 1: Reset consumer offset**
```bash
# Reset to earliest (process all messages)
docker exec smart-admin-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group smart-admin-order-group \
  --reset-offsets --to-earliest \
  --topic smart-admin-order \
  --execute
```

**Or configure in application**:
```yaml
smart:
  kafka:
    consumer:
      auto-offset-reset: earliest  # Start from beginning
```

**Solution 2: Fix group ID mismatch**
```java
// Ensure consistency
public class KafkaConst {
    public static class Group {
        public static final String ORDER = "smart-admin-order-group";
    }
}

@KafkaListener(
    topics = KafkaConst.Topic.ORDER,
    groupId = KafkaConst.Group.ORDER  // Use constant
)
```

**Solution 3: Force rebalance**
```bash
# Restart application to trigger rebalance
./gradlew :sa-admin:bootRun

# Or restart consumer container
docker-compose restart smart-admin
```

**Solution 4: Send test message**
```bash
# Send a test message
curl -X POST http://localhost:1024/business/sample/kafka/send \
  -H "Content-Type: application/json" \
  -d '{"message": "test"}'

# Check consumer logs immediately
tail -f logs/smart-admin.log
```

---

## Issue 4: High Consumer Lag

### Symptoms

```
Consumer Lag: 10,000+ messages
Messages accumulating faster than being consumed
```

**Application behavior**:
- ⚠️ Messages delayed by minutes/hours
- ⚠️ Consumer lag keeps growing
- ⚠️ Real-time processing becomes batch processing

### Root Causes

| Cause | Likelihood | Impact |
|-------|-----------|--------|
| Slow message processing | 50% | High |
| Insufficient consumer concurrency | 30% | High |
| Too few partitions | 15% | Medium |
| Database/external API slow | 5% | High |

### Diagnosis Steps

**Step 1: Measure consumer lag**
```bash
docker exec smart-admin-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group smart-admin-order-group

# Watch LAG column:
# LAG < 100: Healthy
# LAG 100-1000: Warning
# LAG > 1000: Critical
```

**Step 2: Analyze processing time**
```bash
# Add timing logs in consumer
@Override
protected void doHandle(ConsumerRecord<String, String> record) {
    long startTime = System.currentTimeMillis();
    processOrder(record.value());
    long duration = System.currentTimeMillis() - startTime;
    log.info("Processing time: {}ms", duration);
}

# Analyze logs
grep "Processing time" logs/smart-admin.log | awk '{sum+=$4; count++} END {print "Average:", sum/count, "ms"}'
```

**Step 3: Check concurrency configuration**
```yaml
smart:
  kafka:
    listener:
      concurrency: 1  # ⚠️ Only 1 thread!
```

**Step 4: Check partition count**
```bash
docker exec smart-admin-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --describe --topic smart-admin-order

# PartitionCount: 1  (⚠️ Bottleneck!)
```

### Solutions

**Solution 1: Increase consumer concurrency**
```yaml
smart:
  kafka:
    listener:
      concurrency: 5  # 5 threads per consumer instance
```

**Rule**: `concurrency` ≤ partition count

**Solution 2: Add more partitions**
```bash
# Increase partitions (cannot decrease!)
docker exec smart-admin-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --alter --topic smart-admin-order \
  --partitions 10
```

**Solution 3: Use batch processing**
```java
@Component
public class OrderBatchListener extends AbstractBatchKafkaListener<OrderDTO> {

    @Override
    protected void doBatchHandle(List<ConsumerRecord<String, String>> records) {
        // Process 100 messages at once
        List<OrderDTO> orders = parseOrders(records);
        orderService.batchInsert(orders);  // Efficient!
    }
}
```

**Solution 4: Optimize business logic**
```java
// ❌ Bad: N+1 database queries
for (OrderDTO order : orders) {
    customerDao.selectById(order.getCustomerId());  // Query per order!
}

// ✅ Good: Batch query
List<Long> customerIds = orders.stream()
    .map(OrderDTO::getCustomerId)
    .distinct()
    .toList();
Map<Long, Customer> customers = customerDao.selectBatchIds(customerIds);
```

**Solution 5: Scale horizontally**
```bash
# Start multiple consumer instances (different machines/containers)
docker-compose up -d --scale smart-admin=3

# Kafka automatically distributes partitions across instances
```

---

## Issue 5: DLQ Message Accumulation

### Symptoms

```
INFO: Message sent to DLQ: smart-admin-order-dlq
DLQ topic growing continuously, no processing
```

**Application behavior**:
- ⚠️ Failed messages sent to DLQ
- ⚠️ DLQ messages never processed
- ⚠️ Business data loss risk

### Root Causes

| Cause | Likelihood | Impact |
|-------|-----------|--------|
| No DLQ consumer implemented | 60% | High |
| Data format issues | 20% | High |
| Business logic bugs | 15% | High |
| Invalid/malformed messages | 5% | Medium |

### Diagnosis Steps

**Step 1: Check DLQ message count**
```bash
docker exec smart-admin-kafka kafka-run-class \
  kafka.tools.GetOffsetShell \
  --broker-list localhost:9092 \
  --topic smart-admin-order-dlq

# Output: smart-admin-order-dlq:0:500  (500 messages!)
```

**Step 2: Inspect DLQ messages**
```bash
docker exec -it smart-admin-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic smart-admin-order-dlq \
  --from-beginning \
  --max-messages 10

# Examine message structure and error details
```

**Step 3: Analyze error patterns**
```bash
# Check DLQ logs
grep "sent to DLQ" logs/smart-admin.log | \
  awk '{print $NF}' | sort | uniq -c | sort -rn

# Common errors:
# 250 JSONException
# 150 ValidationException
# 100 NullPointerException
```

### Solutions

**Solution 1: Implement DLQ consumer**
```java
@Component
@Slf4j
public class OrderDLQListener extends AbstractKafkaListener {

    @Autowired
    private OrderService orderService;

    @KafkaListener(
        topics = "smart-admin-order-dlq",
        groupId = "smart-admin-order-dlq-processor"
    )
    public void onDLQMessage(ConsumerRecord<String, String> record) {
        handleMessage(record);
    }

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        DeadLetterMessage dlqMsg = JSON.parseObject(record.value(), DeadLetterMessage.class);

        // Analyze error
        if (isRetryable(dlqMsg)) {
            // Republish to original topic
            kafkaProducerService.send(
                dlqMsg.getOriginalTopic(),
                dlqMsg.getOriginalKey(),
                dlqMsg.getOriginalValue()
            );
            log.info("Retried DLQ message: {}", dlqMsg.getOriginalKey());
        } else {
            // Log and archive non-retryable
            log.error("Non-retryable DLQ message: {}", dlqMsg.getException());
            archiveService.archive(dlqMsg);
        }
    }
}
```

**Solution 2: Fix data issues**
```java
// Add validation before sending
@Override
protected void doHandle(ConsumerRecord<String, String> record) {
    try {
        OrderDTO order = JSON.parseObject(record.value(), OrderDTO.class);

        // Validate
        if (order == null || order.getOrderId() == null) {
            log.error("Invalid message format: {}", record.value());
            throw new ValidationException("Invalid order data");
        }

        processOrder(order);

    } catch (JSONException e) {
        log.error("JSON parsing failed: {}", record.value(), e);
        throw e;  // Will go to DLQ
    }
}
```

**Solution 3: Scheduled DLQ retry**
```java
@Scheduled(cron = "0 */10 * * * *")  // Every 10 minutes
public void retryDLQMessages() {
    List<DeadLetterMessage> messages = fetchDLQMessages();

    for (DeadLetterMessage msg : messages) {
        if (canRetry(msg)) {
            kafkaProducerService.send(
                msg.getOriginalTopic(),
                msg.getOriginalKey(),
                msg.getOriginalValue()
            );
        }
    }
}
```

---

## Issue 6: Duplicate Message Processing

### Symptoms

```
WARN: Duplicate message detected, skipping: ORDER-12345
Same message processed multiple times
```

**Application behavior**:
- ⚠️ Same order processed twice
- ⚠️ Duplicate database records
- ⚠️ Duplicate external API calls

### Root Causes

| Cause | Likelihood | Impact |
|-------|-----------|--------|
| Consumer crash before ACK | 50% | High |
| Rebalance during processing | 30% | High |
| At-least-once delivery semantics | 15% | Expected |
| No idempotency implementation | 5% | High |

### Diagnosis Steps

**Step 1: Check for rebalances**
```bash
grep "Rebalancing" logs/smart-admin.log

# Frequent rebalances indicate instability
```

**Step 2: Verify ACK mode**
```yaml
smart:
  kafka:
    consumer:
      enable-auto-commit: false  # Manual commit
    listener:
      ack-mode: manual           # Explicit ACK
```

**Step 3: Check processing time vs timeout**
```yaml
# Processing time should be < max-poll-interval-ms
smart:
  kafka:
    consumer:
      max-poll-interval-ms: 300000  # 5 minutes

# If processing takes > 5 minutes, consumer kicked out and reprocesses
```

### Solutions

**Solution 1: Implement idempotency with Redis**
```java
@Override
protected void doHandle(ConsumerRecord<String, String> record) {
    String orderId = record.key();
    String cacheKey = "processed:order:" + orderId;

    // Check if already processed
    if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
        log.warn("Duplicate message, skipping: {}", orderId);
        return;  // Skip processing
    }

    // Process order
    OrderDTO order = JSON.parseObject(record.value(), OrderDTO.class);
    orderService.processOrder(order);

    // Mark as processed (24h TTL)
    redisTemplate.opsForValue().set(cacheKey, "1", 24, TimeUnit.HOURS);
}
```

**Solution 2: Database unique constraint**
```sql
-- Add unique constraint
ALTER TABLE t_order ADD CONSTRAINT uk_order_id UNIQUE (order_id);
```

```java
try {
    orderDao.insert(order);
} catch (DuplicateKeyException e) {
    log.warn("Duplicate order, already processed: {}", order.getOrderId());
    return;  // Skip duplicate
}
```

**Solution 3: Increase poll interval**
```yaml
smart:
  kafka:
    consumer:
      max-poll-interval-ms: 600000  # 10 minutes (if processing is slow)
```

**Solution 4: Optimize processing speed**
```java
// Reduce processing time to minimize rebalance window
// Use batch operations, caching, async calls
```

---

## Issue 7: Kafka Disk Space Full

### Symptoms

```
ERROR: No space left on device
IOException: /var/lib/kafka/data/smart-admin-order-0/00000000000000000000.log
```

**Application behavior**:
- 🔴 Kafka broker crashes
- 🔴 Cannot write new messages
- 🔴 Potential data loss

### Root Causes

| Cause | Likelihood | Impact |
|-------|-----------|--------|
| Log retention too long | 60% | High |
| No log compression | 20% | Medium |
| Insufficient disk provisioning | 15% | Critical |
| Unexpected message volume spike | 5% | High |

### Diagnosis Steps

**Step 1: Check disk usage**
```bash
docker exec smart-admin-kafka df -h

# Look for /var/lib/kafka/data
# Warning: > 80% usage
# Critical: > 90% usage
```

**Step 2: Check log size**
```bash
docker exec smart-admin-kafka du -sh /var/lib/kafka/data/*

# Identify largest topics
docker exec smart-admin-kafka du -sh /var/lib/kafka/data/* | sort -rh | head -10
```

**Step 3: Check topic configuration**
```bash
docker exec smart-admin-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --describe --topic smart-admin-order

# Check:
# retention.ms (default: 7 days = 604800000 ms)
# segment.bytes (default: 1GB)
```

### Solutions

**Solution 1: Reduce retention time**
```bash
# Change retention to 24 hours
docker exec smart-admin-kafka kafka-configs \
  --bootstrap-server localhost:9092 \
  --alter --entity-type topics \
  --entity-name smart-admin-order \
  --add-config retention.ms=86400000
```

**Solution 2: Enable compression**
```yaml
# Producer-side compression
smart:
  kafka:
    producer:
      compression-type: lz4  # 3:1 compression ratio
```

```bash
# Topic-level compression
docker exec smart-admin-kafka kafka-configs \
  --bootstrap-server localhost:9092 \
  --alter --entity-type topics \
  --entity-name smart-admin-order \
  --add-config compression.type=lz4
```

**Solution 3: Increase disk space**
```yaml
# Docker Compose: Mount larger volume
services:
  kafka:
    volumes:
      - /mnt/large-disk/kafka-data:/var/lib/kafka/data
```

**Solution 4: Delete old topics**
```bash
# List all topics
docker exec smart-admin-kafka kafka-topics \
  --bootstrap-server localhost:9092 --list

# Delete unused topics
docker exec smart-admin-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --delete --topic old-topic-name
```

---

## Quick Reference: Error Codes

| Error Code | Error Type | Common Cause | Quick Fix |
|------------|-----------|--------------|-----------|
| `TimeoutException` | Connection | Kafka not reachable | Check Kafka status |
| `NotLeaderForPartitionException` | Producer | Broker rebalancing | Retry, increase timeout |
| `RecordTooLargeException` | Producer | Message > max.request.size | Reduce message size or increase limit |
| `OffsetOutOfRangeException` | Consumer | Offset invalid | Reset offset to earliest/latest |
| `CommitFailedException` | Consumer | Rebalance happened | Increase max-poll-interval-ms |
| `SerializationException` | Consumer | Deserialization failed | Check message format |
| `UnknownTopicOrPartitionException` | Both | Topic doesn't exist | Create topic |

---

## See Also

- [Diagnostic Guide](/kafka/troubleshooting/diagnostic-guide) - Step-by-step diagnosis
- [FAQ](/kafka/troubleshooting/faq) - Frequently asked questions
- [Debugging Tips](/kafka/troubleshooting/debugging-tips) - Advanced debugging
- [Error Handling](/kafka/guides/error-handling) - Error handling patterns
- [Monitoring](/kafka/operations/monitoring) - Proactive monitoring

---

**Last Updated**: 2026-01-21
