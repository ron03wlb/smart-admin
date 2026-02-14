# Frequently Asked Questions (FAQ)

Answers to common questions about SmartAdmin's Kafka integration.

## General Questions

### Q: What is SmartAdmin's Kafka integration?

**A**: SmartAdmin's Kafka integration (`sa-common/mq/kafka`) is a Spring Kafka wrapper providing:
- Simplified producer/consumer APIs
- Automatic Dead Letter Queue (DLQ) handling
- Batch processing with graceful degradation
- Message aggregation
- Pre-configured Spring Boot autoconfiguration

It's designed to make Kafka integration effortless in SmartAdmin applications.

---

### Q: What Kafka versions are supported?

**A**: SmartAdmin Kafka integration supports:
- **Kafka**: 3.5.0+
- **Spring Kafka**: 3.x (compatible with Spring Boot 3.x)
- **Java**: 21+

Older versions may work but are not officially tested.

---

### Q: Is Kafka required for SmartAdmin?

**A**: No. Kafka is optional. Enable it via configuration:
```yaml
smart:
  kafka:
    enabled: true  # Default: false
```

If disabled, Kafka beans won't be created and the application starts normally.

---

### Q: Can I use SmartAdmin Kafka with Redis/RocketMQ together?

**A**: Yes! SmartAdmin supports multiple message queues simultaneously:
- **Kafka** - High-throughput event streaming
- **RocketMQ** - Transactional messages
- **Redis** - Pub/Sub and simple queuing

Each module is independent and can be enabled/disabled separately.

---

## Configuration Questions

### Q: What's the minimum configuration needed?

**A**: Only `bootstrap-servers`:
```yaml
smart:
  kafka:
    bootstrap-servers: localhost:9092
```

All other settings have sensible defaults. See [Configuration Guide](/kafka/guides/configuration) for full options.

---

### Q: How do I configure different environments?

**A**: Use Spring profiles:

**application-dev.yml**:
```yaml
smart:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      acks: 1  # Faster for dev
```

**application-prod.yml**:
```yaml
smart:
  kafka:
    bootstrap-servers: kafka-1.prod:9092,kafka-2.prod:9092,kafka-3.prod:9092
    producer:
      acks: all  # Reliable for prod
      enable-idempotence: true
```

---

### Q: How do I enable SSL/TLS?

**A**: Configure SSL properties:
```yaml
smart:
  kafka:
    bootstrap-servers: kafka:9093  # SSL port
    ssl:
      enabled: true
      protocol: TLSv1.2
      truststore-location: classpath:kafka.truststore.jks
      truststore-password: ${KAFKA_TRUSTSTORE_PASSWORD}
      keystore-location: classpath:kafka.keystore.jks
      keystore-password: ${KAFKA_KEYSTORE_PASSWORD}
```

See [Deployment Guide - Security](/kafka/operations/deployment#security-configuration) for certificate setup.

---

### Q: Can I override Spring Kafka properties?

**A**: Yes. SmartAdmin properties are converted to standard Spring Kafka properties:
```yaml
smart:
  kafka:
    producer:
      request-timeout-ms: 60000  # → spring.kafka.producer.request-timeout-ms
```

You can also set Spring properties directly:
```yaml
spring:
  kafka:
    producer:
      properties:
        custom.property: value
```

---

## Producer Questions

### Q: How do I send a message?

**A**: Inject `KafkaProducerService`:
```java
@RequiredArgsConstructor
public class OrderService {
    private final KafkaProducerService kafkaProducerService;

    public void createOrder(OrderDTO order) {
        String message = JSON.toJSONString(order);
        kafkaProducerService.send(KafkaConst.Topic.ORDER, message);
    }
}
```

See [Producer Guide](/kafka/guides/producer-guide) for more patterns.

---

### Q: How do I send messages with keys for ordering?

**A**: Use the keyed send method:
```java
String key = "ORDER-" + order.getOrderId();
kafkaProducerService.send(KafkaConst.Topic.ORDER, key, message);
```

Messages with the same key go to the same partition, ensuring order.

---

### Q: How do I send messages in batches?

**A**: Use `sendBatchAsync()`:
```java
List<String> messages = orders.stream()
    .map(order -> JSON.toJSONString(order))
    .toList();

CompletableFuture<List<SendResult<String, String>>> future =
    kafkaProducerService.sendBatchAsync(KafkaConst.Topic.ORDER, messages);

future.thenAccept(results -> {
    log.info("Sent {} messages", results.size());
});
```

---

### Q: How do I handle send failures?

**A**: Use callback:
```java
kafkaProducerService.send(topic, key, message, new ProducerCallback<>() {
    @Override
    public void onSuccess(SendResult result) {
        log.info("Message sent: partition={}, offset={}",
            result.getRecordMetadata().partition(),
            result.getRecordMetadata().offset());
    }

    @Override
    public void onFailure(Throwable ex) {
        log.error("Send failed", ex);
        // Store for retry or alert
    }
});
```

---

### Q: What happens if Kafka is down when I send?

**A**: The send will:
1. Retry automatically (based on `retries` config)
2. If retries exhausted, throw exception
3. Your code should catch and handle (store for later, alert, etc.)

Enable idempotence to prevent duplicates on retry:
```yaml
smart:
  kafka:
    producer:
      enable-idempotence: true
```

---

## Consumer Questions

### Q: How do I create a consumer?

**A**: Extend `AbstractKafkaListener`:
```java
@Component
@Slf4j
public class OrderListener extends AbstractKafkaListener {

    @KafkaListener(topics = KafkaConst.Topic.ORDER, groupId = KafkaConst.Group.ORDER)
    public void onMessage(ConsumerRecord<String, String> record) {
        handleMessage(record);
    }

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        OrderDTO order = JSON.parseObject(record.value(), OrderDTO.class);
        processOrder(order);
    }
}
```

Exceptions thrown in `doHandle()` automatically send messages to DLQ.

---

### Q: Why extend AbstractKafkaListener instead of using @KafkaListener directly?

**A**: `AbstractKafkaListener` provides:
- ✅ Automatic DLQ routing on exceptions
- ✅ Built-in logging
- ✅ No boilerplate error handling
- ✅ Consistent error handling across all consumers

You can still use `@KafkaListener` directly if you need full control.

---

### Q: How do I process messages in batches?

**A**: Extend `AbstractBatchKafkaListener`:
```java
@Component
public class OrderBatchListener extends AbstractBatchKafkaListener<OrderDTO> {

    @KafkaListener(
        topics = KafkaConst.Topic.ORDER,
        groupId = KafkaConst.Group.ORDER_BATCH,
        containerFactory = "batchKafkaListenerContainerFactory"
    )
    public void onBatch(List<ConsumerRecord<String, String>> records, Acknowledgment ack) {
        handleBatch(records, ack);
    }

    @Override
    protected void doBatchHandle(List<ConsumerRecord<String, String>> records) {
        // Efficient batch DB insert
        orderService.batchInsert(parseOrders(records));
    }

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        // Fallback for single message processing
        orderService.insert(parseOrder(record));
    }
}
```

---

### Q: What is graceful degradation?

**A**: If batch processing fails, `AbstractBatchKafkaListener` automatically:
1. Catches the exception
2. Processes each message individually
3. Sends failed messages to DLQ
4. Commits successful messages

This prevents one bad message from blocking an entire batch.

---

### Q: How do I handle duplicate messages?

**A**: Implement idempotency using Redis:
```java
@Override
protected void doHandle(ConsumerRecord<String, String> record) {
    String orderId = record.key();
    String cacheKey = "processed:order:" + orderId;

    if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
        log.warn("Duplicate, skipping: {}", orderId);
        return;
    }

    processOrder(record.value());

    redisTemplate.opsForValue().set(cacheKey, "1", 24, TimeUnit.HOURS);
}
```

Or use database unique constraints.

---

### Q: How do I consume from multiple topics?

**A**: List topics in `@KafkaListener`:
```java
@KafkaListener(
    topics = {KafkaConst.Topic.ORDER, KafkaConst.Topic.PAYMENT},
    groupId = "multi-topic-group"
)
public void onMessage(ConsumerRecord<String, String> record) {
    String topic = record.topic();
    switch (topic) {
        case KafkaConst.Topic.ORDER -> processOrder(record);
        case KafkaConst.Topic.PAYMENT -> processPayment(record);
    }
}
```

---

### Q: Can I have multiple consumers for the same topic?

**A**: Yes! Use different `groupId`:
```java
// Real-time processing
@KafkaListener(topics = KafkaConst.Topic.ORDER, groupId = "order-realtime")
public void processRealtime(ConsumerRecord<String, String> record) { }

// Analytics (processes same messages)
@KafkaListener(topics = KafkaConst.Topic.ORDER, groupId = "order-analytics")
public void processAnalytics(ConsumerRecord<String, String> record) { }
```

Each group gets all messages independently.

---

## Performance Questions

### Q: How do I improve throughput?

**A**: Multiple strategies:

1. **Increase concurrency**:
```yaml
smart:
  kafka:
    listener:
      concurrency: 10  # 10 threads
```

2. **Add more partitions**:
```bash
kafka-topics --alter --topic smart-admin-order --partitions 20
```

3. **Use batch processing**:
```java
@Component
public class OrderBatchListener extends AbstractBatchKafkaListener<OrderDTO> { }
```

4. **Enable compression**:
```yaml
smart:
  kafka:
    producer:
      compression-type: lz4
```

---

### Q: What causes high consumer lag?

**A**: Common causes:
1. **Slow processing** - Optimize business logic
2. **Low concurrency** - Increase `concurrency` setting
3. **Few partitions** - Add more partitions
4. **Database bottleneck** - Use batch operations

Diagnose with:
```bash
kafka-consumer-groups --describe --group GROUP_NAME
```

See [Common Issues - Consumer Lag](/kafka/troubleshooting/common-issues#issue-4-high-consumer-lag).

---

### Q: Should I use batch processing?

**A**: Use batch processing when:
- ✅ High message volume (>1000 msg/sec)
- ✅ Messages can be processed together (e.g., database inserts)
- ✅ Latency is not critical (can wait for batch to fill)

Don't use batch when:
- ❌ Need real-time processing (< 1 second latency)
- ❌ Messages must be processed independently
- ❌ Low message volume (< 100 msg/sec)

---

## Troubleshooting Questions

### Q: Why is my application not receiving messages?

**A**: Common causes:
1. **Offset at end** - Consumer already consumed all messages
   - **Fix**: Reset offset to `earliest`
2. **Wrong group ID** - Consumer using different group
   - **Fix**: Verify `groupId` matches
3. **Topic doesn't exist** - Topic not created
   - **Fix**: Create topic or enable auto-create
4. **No messages** - Producer hasn't sent anything
   - **Fix**: Send test message

See [Diagnostic Guide - Consumer Issues](/kafka/troubleshooting/diagnostic-guide#workflow-3-consumer-lag-growing).

---

### Q: What are DLQ messages and how do I handle them?

**A**: **Dead Letter Queue (DLQ)** holds messages that failed processing.

**Automatic routing**: `AbstractKafkaListener` sends exceptions to DLQ automatically.

**DLQ topic naming**: `{original-topic}-dlq` (e.g., `smart-admin-order-dlq`)

**Process DLQ messages**:
```java
@KafkaListener(topics = "smart-admin-order-dlq", groupId = "dlq-processor")
public void onDLQMessage(ConsumerRecord<String, String> record) {
    DeadLetterMessage dlq = JSON.parseObject(record.value(), DeadLetterMessage.class);

    // Analyze error
    if (canRetry(dlq)) {
        // Republish to original topic
        kafkaProducerService.send(dlq.getOriginalTopic(), dlq.getOriginalValue());
    } else {
        // Archive or alert
    }
}
```

See [Dead Letter Queue Architecture](/kafka/architecture/dead-letter-queue).

---

### Q: How do I debug "Connection refused" errors?

**A**: Systematic check:
1. **Is Kafka running?**
   ```bash
   docker ps | grep kafka
   ```
2. **Is port accessible?**
   ```bash
   telnet localhost 9092
   ```
3. **Is configuration correct?**
   ```bash
   cat application.yml | grep bootstrap-servers
   ```
4. **Network issue?** (Docker)
   ```bash
   docker exec smart-admin-app ping kafka
   ```

See [Common Issues - Connection Failure](/kafka/troubleshooting/common-issues#issue-1-kafka-connection-failure).

---

## Operations Questions

### Q: How do I monitor Kafka integration?

**A**: SmartAdmin exposes metrics via Actuator:
```bash
curl http://localhost:1024/actuator/metrics/kafka.producer.record-send-total
curl http://localhost:1024/actuator/metrics/kafka.consumer.records-lag
```

Integrate with Prometheus/Grafana:
```yaml
management:
  metrics:
    export:
      prometheus:
        enabled: true
```

See [Monitoring Guide](/kafka/operations/monitoring).

---

### Q: How do I deploy Kafka with SmartAdmin?

**A**: Use Docker Compose:
```yaml
services:
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    # ... configuration

  smart-admin:
    depends_on:
      kafka:
        condition: service_healthy
```

See [Deployment Guide](/kafka/operations/deployment) for full examples.

---

### Q: How do I backup/restore Kafka topics?

**A**: Use `kafka-console-consumer` and `kafka-console-producer`:

**Backup**:
```bash
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic smart-admin-order \
  --from-beginning > backup.txt
```

**Restore**:
```bash
cat backup.txt | kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic smart-admin-order
```

For production, use Kafka MirrorMaker or enterprise backup solutions.

---

### Q: How do I scale consumers?

**A**: Three approaches:

1. **Vertical scaling** (increase concurrency):
```yaml
smart:
  kafka:
    listener:
      concurrency: 10
```

2. **Horizontal scaling** (multiple instances):
```bash
docker-compose up -d --scale smart-admin=3
```

3. **Partition scaling** (add partitions):
```bash
kafka-topics --alter --topic smart-admin-order --partitions 20
```

Rule: `total consumers` ≤ `partition count` for optimal distribution.

---

## See Also

- [Common Issues](/kafka/troubleshooting/common-issues) - Detailed issue resolution
- [Diagnostic Guide](/kafka/troubleshooting/diagnostic-guide) - Step-by-step diagnosis
- [Debugging Tips](/kafka/troubleshooting/debugging-tips) - Advanced techniques
- [Quick Start](/kafka/getting-started/quick-start) - Getting started
- [Best Practices](/kafka/guides/best-practices) - Production recommendations

---

**Last Updated**: 2026-01-21
