# Debugging Tips

Advanced techniques and tools for troubleshooting complex Kafka integration issues in SmartAdmin.

## Overview

This guide covers:
- **Logging strategies** - What to log and how
- **Diagnostic tools** - Kafka CLI, monitoring tools, debuggers
- **Debugging patterns** - Systematic approaches to complex issues
- **Performance profiling** - Identifying bottlenecks
- **Production debugging** - Safe techniques for live systems

---

## Logging Strategies

### Enhanced Producer Logging

**Add detailed logging to producer**:
```java
@Slf4j
@RequiredArgsConstructor
public class OrderService {
    private final KafkaProducerService kafkaProducerService;

    public void createOrder(OrderDTO order) {
        String message = JSON.toJSONString(order);
        String key = "ORDER-" + order.getOrderId();

        long startTime = System.currentTimeMillis();

        try {
            kafkaProducerService.send(KafkaConst.Topic.ORDER, key, message,
                new ProducerCallback<>() {
                    @Override
                    public void onSuccess(SendResult result) {
                        long duration = System.currentTimeMillis() - startTime;
                        RecordMetadata metadata = result.getRecordMetadata();

                        log.info("✅ Message sent | Key: {} | Topic: {} | Partition: {} | Offset: {} | Duration: {}ms",
                            key, metadata.topic(), metadata.partition(), metadata.offset(), duration);
                    }

                    @Override
                    public void onFailure(Throwable ex) {
                        long duration = System.currentTimeMillis() - startTime;
                        log.error("❌ Send failed | Key: {} | Duration: {}ms | Error: {}",
                            key, duration, ex.getMessage(), ex);
                    }
                });

        } catch (Exception e) {
            log.error("💥 Send exception | Key: {} | Error: {}", key, e.getMessage(), e);
            throw e;
        }
    }
}
```

### Enhanced Consumer Logging

**Add detailed logging to consumer**:
```java
@Component
@Slf4j
public class OrderListener extends AbstractKafkaListener {

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        long startTime = System.currentTimeMillis();
        String key = record.key();

        // Log ingestion
        log.info("📥 Message received | Topic: {} | Partition: {} | Offset: {} | Key: {} | Size: {} bytes",
            record.topic(), record.partition(), record.offset(), key, record.value().length());

        try {
            OrderDTO order = JSON.parseObject(record.value(), OrderDTO.class);

            // Log processing start
            log.debug("🔄 Processing order | Key: {} | OrderId: {}", key, order.getOrderId());

            processOrder(order);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ Processing complete | Key: {} | Duration: {}ms", key, duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ Processing failed | Key: {} | Duration: {}ms | Error: {}",
                key, duration, e.getMessage(), e);
            throw e;  // Will go to DLQ
        }
    }
}
```

### Structured Logging with MDC

**Add correlation IDs for tracing**:
```java
@Override
protected void doHandle(ConsumerRecord<String, String> record) {
    String correlationId = UUID.randomUUID().toString();

    // Add to Mapped Diagnostic Context
    MDC.put("correlationId", correlationId);
    MDC.put("topic", record.topic());
    MDC.put("partition", String.valueOf(record.partition()));
    MDC.put("offset", String.valueOf(record.offset()));

    try {
        log.info("Processing message");  // Will include MDC fields
        processOrder(record.value());
    } finally {
        MDC.clear();
    }
}
```

**Logback configuration** (`logback-spring.xml`):
```xml
<pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} [corrId:%X{correlationId}] - %msg%n</pattern>
```

---

## Diagnostic Tools

### Kafka CLI Tools

**Essential commands for debugging**:

**1. Topic inspection**:
```bash
# Get topic details
docker exec smart-admin-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --describe --topic smart-admin-order

# Check retention settings
docker exec smart-admin-kafka kafka-configs \
  --bootstrap-server localhost:9092 \
  --describe --entity-type topics \
  --entity-name smart-admin-order
```

**2. Message inspection**:
```bash
# Read last 10 messages
docker exec smart-admin-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic smart-admin-order \
  --max-messages 10 \
  --property print.key=true \
  --property key.separator=":" \
  --property print.timestamp=true

# Read messages from specific offset
docker exec smart-admin-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic smart-admin-order \
  --partition 0 \
  --offset 100 \
  --max-messages 5
```

**3. Consumer group analysis**:
```bash
# Detailed lag analysis
docker exec smart-admin-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group smart-admin-order-group \
  --verbose

# Export lag to CSV for analysis
docker exec smart-admin-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group smart-admin-order-group | \
  awk '{print $1","$2","$3","$4","$5","$6}' > lag-report.csv
```

### Kafka UI

**Install Kafka UI for visual debugging**:
```yaml
# docker-compose.yml
kafka-ui:
  image: provectuslabs/kafka-ui:latest
  ports:
    - "8080:8080"
  environment:
    KAFKA_CLUSTERS_0_NAME: smart-admin
    KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
```

**Features**:
- 📊 Visual topic browser
- 🔍 Message search and filter
- 👥 Consumer group monitoring
- ⚙️ Configuration management
- 📈 Real-time metrics

**Access**: http://localhost:8080

### Actuator Endpoints for Debugging

**Health check details**:
```bash
curl http://localhost:1024/actuator/health | jq .
```

**Kafka metrics**:
```bash
# Producer metrics
curl http://localhost:1024/actuator/metrics/kafka.producer.record-send-total
curl http://localhost:1024/actuator/metrics/kafka.producer.request-latency-avg

# Consumer metrics
curl http://localhost:1024/actuator/metrics/kafka.consumer.records-consumed-total
curl http://localhost:1024/actuator/metrics/kafka.consumer.records-lag
```

**List all Kafka metrics**:
```bash
curl http://localhost:1024/actuator/metrics | jq '.names[] | select(contains("kafka"))'
```

---

## Debugging Patterns

### Pattern 1: Message Lifecycle Tracing

**Trace a single message end-to-end**:

**Step 1**: Send with unique ID
```java
String uniqueId = "TRACE-" + UUID.randomUUID();
OrderDTO order = new OrderDTO();
order.setTraceId(uniqueId);
String message = JSON.toJSONString(order);

log.info("🚀 TRACE START | TraceId: {}", uniqueId);
kafkaProducerService.send(KafkaConst.Topic.ORDER, uniqueId, message);
```

**Step 2**: Log at each stage
```java
@Override
protected void doHandle(ConsumerRecord<String, String> record) {
    OrderDTO order = JSON.parseObject(record.value(), OrderDTO.class);
    log.info("📍 TRACE CONSUMER | TraceId: {} | Offset: {}", order.getTraceId(), record.offset());

    orderService.processOrder(order);

    log.info("🏁 TRACE END | TraceId: {}", order.getTraceId());
}
```

**Step 3**: Grep logs
```bash
grep "TRACE-12345" logs/smart-admin.log
```

**Expected output**:
```
10:00:00.123 🚀 TRACE START | TraceId: TRACE-12345
10:00:00.145 📍 TRACE CONSUMER | TraceId: TRACE-12345 | Offset: 150
10:00:00.200 🏁 TRACE END | TraceId: TRACE-12345
```

### Pattern 2: Performance Profiling

**Measure processing time distribution**:

**Step 1**: Add timing logs
```java
@Override
protected void doHandle(ConsumerRecord<String, String> record) {
    long t0 = System.currentTimeMillis();

    OrderDTO order = JSON.parseObject(record.value(), OrderDTO.class);
    long t1 = System.currentTimeMillis();

    orderService.processOrder(order);
    long t2 = System.currentTimeMillis();

    orderDao.insert(order);
    long t3 = System.currentTimeMillis();

    log.info("⏱️ Timing | Parse: {}ms | Process: {}ms | DB: {}ms | Total: {}ms",
        (t1-t0), (t2-t1), (t3-t2), (t3-t0));
}
```

**Step 2**: Analyze logs
```bash
# Calculate average times
grep "Timing" logs/smart-admin.log | \
  awk -F'|' '{
    parse+=$2; process+=$3; db+=$4; total+=$5; count++
  } END {
    print "Average Parse:", parse/count, "ms"
    print "Average Process:", process/count, "ms"
    print "Average DB:", db/count, "ms"
    print "Average Total:", total/count, "ms"
  }'
```

**Step 3**: Identify bottleneck
```
Average Parse: 2 ms
Average Process: 5 ms
Average DB: 45 ms  ← Bottleneck!
Average Total: 52 ms
```

### Pattern 3: Correlation Analysis

**Correlate producer sends with consumer receives**:

**Step 1**: Timestamp producer sends
```java
String timestamp = String.valueOf(System.currentTimeMillis());
order.setProducerTimestamp(timestamp);

log.info("SEND | Key: {} | Timestamp: {}", key, timestamp);
```

**Step 2**: Calculate consumer lag
```java
@Override
protected void doHandle(ConsumerRecord<String, String> record) {
    OrderDTO order = JSON.parseObject(record.value(), OrderDTO.class);
    long producerTime = Long.parseLong(order.getProducerTimestamp());
    long consumerTime = System.currentTimeMillis();
    long lag = consumerTime - producerTime;

    log.info("RECEIVE | Key: {} | Lag: {}ms", record.key(), lag);

    if (lag > 5000) {
        log.warn("⚠️ High lag detected: {}ms", lag);
    }
}
```

**Step 3**: Analyze lag distribution
```bash
grep "RECEIVE" logs/smart-admin.log | awk '{print $8}' | sort -n
```

---

## Production Debugging

### Technique 1: Enable Debug Logging Dynamically

**Without restart** using Actuator:
```bash
# Enable DEBUG logging for Kafka
curl -X POST http://localhost:1024/actuator/loggers/org.springframework.kafka \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel":"DEBUG"}'

# Verify
curl http://localhost:1024/actuator/loggers/org.springframework.kafka | jq .
```

**Re-enable INFO after debugging**:
```bash
curl -X POST http://localhost:1024/actuator/loggers/org.springframework.kafka \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel":"INFO"}'
```

### Technique 2: Thread Dump Analysis

**Capture thread dump**:
```bash
# Using Actuator
curl http://localhost:1024/actuator/threaddump > threaddump.txt

# Or using jstack
docker exec smart-admin-app jstack 1 > threaddump.txt
```

**Analyze Kafka threads**:
```bash
grep -A 5 "kafka" threaddump.txt
```

**Look for**:
- `BLOCKED` threads (deadlock)
- `WAITING` threads (stuck on I/O)
- High number of `RUNNABLE` threads (CPU intensive)

### Technique 3: Heap Dump Analysis

**Capture heap dump**:
```bash
docker exec smart-admin-app jmap -dump:live,format=b,file=/tmp/heap.hprof 1

# Copy from container
docker cp smart-admin-app:/tmp/heap.hprof ./heap.hprof
```

**Analyze with Eclipse MAT or VisualVM**:
- Check for Kafka message accumulation
- Identify memory leaks in consumers
- Analyze object retention

### Technique 4: Safe Live Testing

**Test in production without disrupting traffic**:

**1. Create test consumer group**:
```java
@KafkaListener(
    topics = KafkaConst.Topic.ORDER,
    groupId = "debug-group",  // Separate group
    autoStartup = "false"     // Don't start automatically
)
public void debugConsumer(ConsumerRecord<String, String> record) {
    // Debug logic
}
```

**2. Start manually via Actuator**:
```bash
curl -X POST http://localhost:1024/actuator/kafka/start/debug-group
```

**3. Stop when done**:
```bash
curl -X POST http://localhost:1024/actuator/kafka/stop/debug-group
```

---

## Advanced Troubleshooting

### Debugging Message Loss

**Verify no message loss**:

**Step 1**: Count producer sends
```bash
grep "Message sent successfully" logs/smart-admin.log | wc -l
# Output: 10000
```

**Step 2**: Count consumer receives
```bash
grep "Processing message" logs/smart-admin.log | wc -l
# Output: 9950 ← Missing 50 messages!
```

**Step 3**: Check DLQ
```bash
docker exec smart-admin-kafka kafka-run-class \
  kafka.tools.GetOffsetShell \
  --broker-list localhost:9092 \
  --topic smart-admin-order-dlq
# Output: smart-admin-order-dlq:0:50 ← Found them!
```

**Step 4**: Analyze DLQ messages
```bash
docker exec -it smart-admin-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic smart-admin-order-dlq \
  --from-beginning | head -10
```

### Debugging Rebalance Issues

**Detect frequent rebalances**:
```bash
grep "Rebalancing" logs/smart-admin.log | \
  awk '{print $1}' | uniq -c

# Output:
# 10 10:00:00.123  ← 10 rebalances in 1 second! Problem!
```

**Root cause analysis**:
```bash
# Check max.poll.interval.ms violations
grep "max.poll.interval.ms" logs/smart-admin.log

# Check GC pauses
grep "GC pause" logs/gc.log | awk '{if($3>5000) print}'
```

**Solution**: Increase `max-poll-interval-ms` or optimize processing.

### Debugging Serialization Issues

**Enable detailed error logging**:
```java
@Override
protected void doHandle(ConsumerRecord<String, String> record) {
    try {
        OrderDTO order = JSON.parseObject(record.value(), OrderDTO.class);
        processOrder(order);
    } catch (JSONException e) {
        log.error("❌ JSON parse failed | Offset: {} | Raw: {} | Error: {}",
            record.offset(),
            record.value().substring(0, Math.min(200, record.value().length())),  // First 200 chars
            e.getMessage(), e);
        throw e;
    }
}
```

**Common issues**:
- Missing required fields
- Type mismatch (String vs Number)
- Invalid JSON syntax
- Encoding issues (UTF-8)

---

## Tools and Scripts

### Diagnostic Script

**File**: `scripts/kafka-diagnostics.sh`

```bash
#!/bin/bash
# Comprehensive Kafka diagnostics

echo "=== Kafka Health Check ==="
docker ps | grep kafka
echo ""

echo "=== Topics ==="
docker exec smart-admin-kafka kafka-topics --bootstrap-server localhost:9092 --list
echo ""

echo "=== Consumer Groups ==="
docker exec smart-admin-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --list
echo ""

echo "=== Consumer Lag ==="
docker exec smart-admin-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group smart-admin-order-group
echo ""

echo "=== DLQ Message Count ==="
docker exec smart-admin-kafka kafka-run-class \
  kafka.tools.GetOffsetShell \
  --broker-list localhost:9092 \
  --topic smart-admin-order-dlq
echo ""

echo "=== Application Health ==="
curl -s http://localhost:1024/actuator/health | jq .
echo ""

echo "=== Recent Errors ==="
tail -100 logs/smart-admin.log | grep ERROR
```

**Usage**:
```bash
chmod +x scripts/kafka-diagnostics.sh
./scripts/kafka-diagnostics.sh > diagnostics-$(date +%Y%m%d-%H%M%S).txt
```

---

## Best Practices

### DO: Use Feature Flags for Debug Code

```java
@Value("${kafka.debug.enabled:false}")
private boolean debugEnabled;

@Override
protected void doHandle(ConsumerRecord<String, String> record) {
    if (debugEnabled) {
        log.debug("Full record: {}", record);
    }
    processOrder(record.value());
}
```

### DO: Collect Context Before Escalating

**Checklist for bug reports**:
- [ ] Kafka version
- [ ] Spring Kafka version
- [ ] Configuration (sanitized)
- [ ] Full stack trace
- [ ] Thread dump (if hanging)
- [ ] Consumer group lag
- [ ] Topic metrics
- [ ] Recent changes

### DON'T: Debug in Production Without Backups

Before making changes:
```bash
# Export consumer offsets
docker exec smart-admin-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group smart-admin-order-group > offsets-backup.txt
```

---

## See Also

- [Common Issues](/kafka/troubleshooting/common-issues) - Known issues and solutions
- [Diagnostic Guide](/kafka/troubleshooting/diagnostic-guide) - Systematic diagnosis
- [FAQ](/kafka/troubleshooting/faq) - Frequently asked questions
- [Monitoring](/kafka/operations/monitoring) - Proactive monitoring
- [Performance Tuning](/kafka/operations/performance-tuning) - Optimization

---

**Last Updated**: 2026-01-21
