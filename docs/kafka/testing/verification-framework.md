# Verification Framework

Comprehensive quality verification framework with 6-dimension scoring system for Kafka integration.

## Overview

The SmartAdmin Kafka Verification Framework provides a systematic approach to validating Kafka integration quality across six critical dimensions. This framework ensures production-ready deployments through structured testing, validation, and quality scoring.

**Purpose**:
- Establish objective quality metrics
- Provide repeatable verification procedures
- Identify gaps before production deployment
- Track improvement progress over time
- Ensure compliance with production standards

**Quality Dimensions** (0-100 points each):
1. **L1: Configuration** - Proper setup and environment-specific configs
2. **L2: Code Quality** - Code structure, patterns, and standards
3. **L3: Functional Correctness** - Features work as expected
4. **L4: Reliability** - Resilience to failures and errors
5. **L5: Performance** - Throughput, latency, resource usage
6. **L6: Operations** - Monitoring, logging, troubleshooting

**Total Score** = (L1 + L2 + L3 + L4 + L5 + L6) / 6

**Quality Gates**:
- **90-100**: Production Ready ✅
- **80-89**: Good Quality ✓
- **70-79**: Acceptable ⚠️
- **60-69**: Needs Improvement ❌
- **< 60**: Not Production Ready ❌❌

---

## L1: Configuration Verification (0-100 points)

### Overview

Validate that all required configurations are properly set and environment-specific configs are correct.

**Scoring criteria**:
- ✅ All required properties configured (30 points)
- ✅ Environment-specific configs (dev/test/prod) (20 points)
- ✅ Health check endpoints enabled (15 points)
- ✅ Monitoring configured (15 points)
- ✅ Security settings (SSL/SASL) (10 points)
- ✅ Documentation complete (10 points)

### Verification Procedures

#### Step 1: Verify Bootstrap Servers

**Command**:
```bash
# Check application configuration
grep "smart.kafka.bootstrap-servers" smart-admin-api-java21-springboot3/sa-admin/src/main/resources/*/sa-base.yaml

# Expected output per environment:
# dev/sa-base.yaml:    bootstrap-servers: localhost:9092
# test/sa-base.yaml:   bootstrap-servers: kafka-test.internal:9092
# prod/sa-base.yaml:   bootstrap-servers: kafka-prod-1:9092,kafka-prod-2:9092,kafka-prod-3:9092
```

**Validation**:
- [ ] Dev: Single broker (localhost:9092)
- [ ] Test: Test cluster address
- [ ] Prod: Multiple brokers (3+) for high availability

**Points**: 10/30

---

#### Step 2: Verify Producer Configuration

**Command**:
```bash
# Check producer settings
grep -A 10 "producer:" smart-admin-api-java21-springboot3/sa-admin/src/main/resources/*/sa-base.yaml
```

**Required settings**:
```yaml
producer:
  acks: all                    # Wait for all replicas
  retries: 3                   # Retry on failures
  enable-idempotence: true     # Exactly-once semantics
  compression-type: lz4        # Compression for throughput
  batch-size: 16384           # Batch size in bytes
  linger-ms: 10               # Wait time for batching
```

**Validation**:
- [ ] `acks=all` for durability
- [ ] `enable-idempotence=true` for exactly-once
- [ ] Compression enabled (lz4, snappy, or gzip)
- [ ] Batch settings configured

**Points**: 10/30

---

#### Step 3: Verify Consumer Configuration

**Command**:
```bash
# Check consumer settings
grep -A 10 "consumer:" smart-admin-api-java21-springboot3/sa-admin/src/main/resources/*/sa-base.yaml
```

**Required settings**:
```yaml
consumer:
  auto-offset-reset: earliest      # Read from beginning on first start
  enable-auto-commit: false        # Manual offset commit
  max-poll-records: 500           # Batch size
  fetch-min-bytes: 1              # Minimum fetch size
  fetch-max-wait-ms: 500          # Max wait time
```

**Validation**:
- [ ] `enable-auto-commit=false` for manual control
- [ ] `auto-offset-reset` set appropriately
- [ ] Batch size configured for performance

**Points**: 10/30

---

#### Step 4: Verify Environment-Specific Configs

**Test each environment**:

**Development**:
```bash
# Run with dev profile
./gradlew :sa-admin:bootRun -Penv=dev

# Check loaded configuration
curl http://localhost:1024/actuator/env | jq '.propertySources[] | select(.name | contains("sa-base")) | .properties."smart.kafka.bootstrap-servers"'
```

**Test**:
```bash
./gradlew :sa-admin:bootRun -Penv=test
# Verify test kafka cluster connection
```

**Production**:
```bash
./gradlew :sa-admin:bootRun -Penv=prod
# Verify production kafka cluster connection
```

**Validation**:
- [ ] Dev profile connects to localhost
- [ ] Test profile connects to test cluster
- [ ] Prod profile connects to production cluster
- [ ] No hardcoded values, all externalized

**Points**: 20/30

---

#### Step 5: Verify Health Checks

**Command**:
```bash
# Check Kafka health endpoint
curl http://localhost:1024/actuator/health/kafka | jq '.'
```

**Expected output**:
```json
{
  "status": "UP",
  "components": {
    "kafka": {
      "status": "UP",
      "details": {
        "clusterId": "...",
        "brokerId": 1,
        "nodes": 3
      }
    }
  }
}
```

**Validation**:
- [ ] Health endpoint returns `UP` status
- [ ] Cluster details present
- [ ] Broker count correct

**Points**: 15/30

---

#### Step 6: Verify Monitoring Configuration

**Check Prometheus metrics exposure**:
```bash
curl http://localhost:1024/actuator/prometheus | grep kafka_producer

# Expected metrics:
# kafka_producer_record_send_total
# kafka_producer_record_error_total
# kafka_consumer_records_consumed_total
# kafka_consumer_lag
```

**Validation**:
- [ ] Prometheus endpoint enabled
- [ ] Kafka producer metrics exposed
- [ ] Kafka consumer metrics exposed
- [ ] Consumer lag metric available

**Points**: 15/30

---

#### Step 7: Verify Security Settings (Production)

**For production with SSL/SASL**:
```yaml
spring:
  kafka:
    properties:
      security.protocol: SASL_SSL
      sasl.mechanism: SCRAM-SHA-512
      sasl.jaas.config: |
        org.apache.kafka.common.security.scram.ScramLoginModule required
        username="${KAFKA_USERNAME}"
        password="${KAFKA_PASSWORD}";
    ssl:
      trust-store-location: file:/etc/kafka/truststore.jks
      trust-store-password: ${TRUSTSTORE_PASSWORD}
```

**Validation**:
- [ ] Security protocol configured (SASL_SSL)
- [ ] SASL mechanism specified
- [ ] Credentials externalized (not hardcoded)
- [ ] SSL truststore configured

**Points**: 10/30

---

### L1 Scoring Formula

```
L1 Score =
  (Bootstrap Servers: 10) +
  (Producer Config: 10) +
  (Consumer Config: 10) +
  (Environment Configs: 20) +
  (Health Checks: 15) +
  (Monitoring: 15) +
  (Security: 10) +
  (Documentation: 10)
= 100 points
```

---

## L2: Code Quality Verification (0-100 points)

### Overview

Validate code structure, patterns, and adherence to SmartAdmin conventions.

**Scoring criteria**:
- ✅ Producer service implemented correctly (25 points)
- ✅ Consumer listeners follow AbstractKafkaListener pattern (25 points)
- ✅ Error handling with DLQ routing (20 points)
- ✅ Logging and metrics instrumentation (15 points)
- ✅ Thread safety and concurrency (10 points)
- ✅ Code review passed (5 points)

### Verification Procedures

#### Step 1: Verify Producer Service

**Location**: `sa-common/sa-common-module-mq-kafka/src/main/java/net/lab1024/sa/common/mq/kafka/service/KafkaProducerService.java`

**Required methods**:
```java
public class KafkaProducerService {
    // Synchronous send
    public void send(String topic, String key, String value);

    // Asynchronous send
    public CompletableFuture<SendResult<String, String>> sendAsync(String topic, String key, String value);

    // Batch send
    public List<CompletableFuture<SendResult<String, String>>> sendBatchAsync(String topic, List<String> messages);
}
```

**Validation checklist**:
- [ ] Service uses `@RequiredArgsConstructor` for dependency injection
- [ ] No `@Autowired` field injection
- [ ] All methods handle exceptions appropriately
- [ ] Async methods return `CompletableFuture`
- [ ] Logging includes topic, key, and status
- [ ] Metrics recorded for send operations

**Points**: 25/25

---

#### Step 2: Verify Consumer Listeners

**Pattern**: All consumers must extend `AbstractKafkaListener`

**Example verification**:
```bash
# Find all @KafkaListener annotated classes
grep -r "@KafkaListener" smart-admin-api-java21-springboot3/sa-admin/src/main/java --include="*.java" -l

# For each listener, verify it extends AbstractKafkaListener
grep -A 5 "class.*Listener" <file> | grep "extends AbstractKafkaListener"
```

**Required pattern**:
```java
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderListener extends AbstractKafkaListener {

    @KafkaListener(topics = "smart-admin-order", groupId = "order-processor")
    public void onMessage(ConsumerRecord<String, String> record) {
        handleMessage(record);  // From AbstractKafkaListener
    }

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        // Business logic here
    }
}
```

**Validation checklist**:
- [ ] Extends `AbstractKafkaListener`
- [ ] Calls `handleMessage(record)` in listener method
- [ ] Overrides `doHandle()` for business logic
- [ ] Uses `@RequiredArgsConstructor` for dependency injection
- [ ] Proper exception handling (throws to trigger DLQ)

**Points**: 25/25

---

#### Step 3: Verify Error Handling

**Check AbstractKafkaListener DLQ logic**:
```java
protected void handleMessage(ConsumerRecord<String, String> record) {
    try {
        doHandle(record);
    } catch (Exception e) {
        log.error("Message processing failed", e);
        handleError(record, e);  // Sends to DLQ
    }
}
```

**Validation checklist**:
- [ ] All exceptions caught in `handleMessage()`
- [ ] `handleError()` method sends to DLQ
- [ ] DLQ topic follows naming convention: `{topic}-dlq`
- [ ] Error metadata included (stacktrace, timestamp)
- [ ] Business exceptions distinguish retriable vs permanent errors

**Points**: 20/20

---

#### Step 4: Verify Logging

**Required log statements**:

**Producer**:
```java
log.info("📤 Sending message | Topic: {} | Key: {}", topic, key);
log.info("✅ Message sent successfully | Topic: {} | Partition: {} | Offset: {}",
    topic, partition, offset);
log.error("❌ Failed to send message | Topic: {} | Key: {}", topic, key, e);
```

**Consumer**:
```java
log.info("📥 Received message | Topic: {} | Partition: {} | Offset: {}",
    record.topic(), record.partition(), record.offset());
log.info("✅ Message processed successfully | Key: {}", record.key());
log.error("❌ Message processing failed | Topic: {} | Key: {}",
    record.topic(), record.key(), e);
```

**Validation checklist**:
- [ ] Structured logging with placeholders (`{}`)
- [ ] Include topic, partition, offset in logs
- [ ] Emoji indicators for quick scanning (optional but recommended)
- [ ] Error logs include exception details
- [ ] No sensitive data logged

**Points**: 15/15

---

#### Step 5: Verify Thread Safety

**Check for concurrency issues**:

**Producer service**:
```java
// ✅ Good: KafkaTemplate is thread-safe
@Service
@RequiredArgsConstructor
public class KafkaProducerService {
    private final KafkaTemplate<String, String> kafkaTemplate;  // Thread-safe
}

// ❌ Bad: Mutable shared state
public class KafkaProducerService {
    private List<String> messageBuffer;  // Not thread-safe!
}
```

**Consumer listener**:
```java
// ✅ Good: Stateless listener
@Component
public class OrderListener extends AbstractKafkaListener {
    // No mutable state
}

// ❌ Bad: Shared mutable state
@Component
public class OrderListener extends AbstractKafkaListener {
    private int messageCount;  // Race condition!
}
```

**Validation checklist**:
- [ ] Producer service is stateless
- [ ] Consumer listeners are stateless
- [ ] Shared state uses concurrent collections
- [ ] Proper synchronization if mutable state needed

**Points**: 10/10

---

### L2 Scoring Formula

```
L2 Score =
  (Producer Service: 25) +
  (Consumer Listeners: 25) +
  (Error Handling: 20) +
  (Logging: 15) +
  (Thread Safety: 10) +
  (Code Review: 5)
= 100 points
```

---

## L3: Functional Correctness Verification (0-100 points)

### Overview

Validate that all Kafka features work correctly end-to-end.

**Scoring criteria**:
- ✅ Messages sent successfully (20 points)
- ✅ Messages consumed correctly (20 points)
- ✅ Batch processing works (15 points)
- ✅ DLQ routing on failures (15 points)
- ✅ Message ordering preserved (10 points)
- ✅ Idempotency implemented (10 points)
- ✅ All edge cases handled (10 points)

### Verification Procedures

#### Test 1: Basic Send and Receive

**Test script**:
```bash
# Send test message
curl -X POST http://localhost:1024/business/sample/kafka/basic/send \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "12345",
    "message": "Test message",
    "type": "INFO"
  }'

# Check logs for consumption
docker logs smart-admin-app 2>&1 | grep "Message processed successfully"
```

**Expected outcome**:
- Producer log: `✅ Message sent successfully`
- Consumer log: `✅ Message processed successfully`
- No errors in logs

**Points**: 20/20 if successful

---

#### Test 2: Batch Processing

**Test script**:
```bash
# Send 100 messages
for i in {1..100}; do
  curl -X POST http://localhost:1024/business/sample/kafka/batch/send \
    -H "Content-Type: application/json" \
    -d "{\"batchId\":\"BATCH-001\",\"sequenceNo\":$i}"
done

# Verify batch consumption
docker logs smart-admin-app 2>&1 | grep "Batch processed"
```

**Expected outcome**:
- Log shows: `📦 Processing batch | Size: 100`
- All 100 messages consumed
- Batch processing faster than individual (measure time)

**Points**: 15/15 if successful

---

#### Test 3: DLQ Routing

**Test script**:
```bash
# Send invalid message (negative amount)
curl -X POST http://localhost:1024/business/sample/kafka/basic/send \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORD-999",
    "amount": -99.99
  }'

# Check DLQ topic
docker exec smart-admin-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic smart-admin-order-dlq \
  --from-beginning \
  --max-messages 1
```

**Expected outcome**:
- Main topic consumer logs error
- DLQ message contains original message + error details
- Consumer doesn't crash

**Points**: 15/15 if DLQ message present

---

#### Test 4: Message Ordering

**Test script**:
```bash
# Send 10 messages with same key (should go to same partition)
for i in {1..10}; do
  curl -X POST http://localhost:1024/business/sample/kafka/basic/send \
    -H "Content-Type: application/json" \
    -d "{\"orderId\":\"ORD-001\",\"sequenceNo\":$i}"
done

# Check consumer logs for ordering
docker logs smart-admin-app 2>&1 | grep "sequenceNo" | grep "ORD-001"
```

**Expected outcome**:
- Messages consumed in order: 1, 2, 3, ..., 10
- All messages for same key go to same partition

**Points**: 10/10 if ordered

---

#### Test 5: Idempotency

**Test script**:
```bash
# Send same message twice
MESSAGE='{"orderId":"ORD-IDEMPOTENT","messageId":"MSG-12345"}'

curl -X POST http://localhost:1024/business/sample/kafka/basic/send \
  -H "Content-Type: application/json" -d "$MESSAGE"

sleep 2

curl -X POST http://localhost:1024/business/sample/kafka/basic/send \
  -H "Content-Type: application/json" -d "$MESSAGE"

# Check database for duplicates
docker exec smart-admin-mysql mysql -u sa_user -psa123456 smart-admin \
  -e "SELECT COUNT(*) FROM t_order WHERE order_id='ORD-IDEMPOTENT';"
```

**Expected outcome**:
- Only 1 record in database (not 2)
- Second message detected as duplicate
- Log shows: `⚠️ Duplicate message detected`

**Points**: 10/10 if no duplicates

---

### L3 Scoring Formula

```
L3 Score =
  (Send/Receive: 20) +
  (Batch: 15) +
  (DLQ: 15) +
  (Ordering: 10) +
  (Idempotency: 10) +
  (Edge Cases: 10) +
  (Consumer Consumption: 20)
= 100 points
```

---

## L4: Reliability Verification (0-100 points)

### Overview

Validate system resilience to failures and errors.

**Scoring criteria**:
- ✅ Survives Kafka restart (25 points)
- ✅ Handles network failures (20 points)
- ✅ Consumer rebalancing handled (15 points)
- ✅ No message loss (20 points)
- ✅ DLQ messages recoverable (10 points)
- ✅ Graceful shutdown (10 points)

### Verification Procedures

#### Test 1: Kafka Restart

**Test script**:
```bash
# 1. Send initial messages
curl -X POST http://localhost:1024/business/sample/kafka/basic/send \
  -d '{"orderId":"ORD-BEFORE"}'

# 2. Restart Kafka
docker restart smart-admin-kafka

# 3. Wait for Kafka to be ready
sleep 30

# 4. Send messages after restart
curl -X POST http://localhost:1024/business/sample/kafka/basic/send \
  -d '{"orderId":"ORD-AFTER"}'

# 5. Verify both messages consumed
docker logs smart-admin-app 2>&1 | grep -E "ORD-BEFORE|ORD-AFTER"
```

**Expected outcome**:
- Both messages consumed successfully
- Producer reconnects automatically
- Consumer resumes from last committed offset

**Points**: 25/25 if successful

---

#### Test 2: Network Partition

**Test script**:
```bash
# 1. Send messages continuously in background
while true; do
  curl -X POST http://localhost:1024/business/sample/kafka/basic/send \
    -d "{\"orderId\":\"ORD-$(date +%s)\"}" &
  sleep 1
done

# 2. Simulate network partition (block Kafka port)
docker exec smart-admin-kafka iptables -A INPUT -p tcp --dport 9092 -j DROP

# 3. Wait 10 seconds
sleep 10

# 4. Restore network
docker exec smart-admin-kafka iptables -D INPUT -p tcp --dport 9092 -j DROP

# 5. Check for message loss
# Count sent vs consumed messages
```

**Expected outcome**:
- Producer retries failed sends
- No messages lost after network restored
- Consumer resumes consumption

**Points**: 20/20 if no loss

---

#### Test 3: Consumer Rebalancing

**Test script**:
```bash
# 1. Start with single consumer instance
./gradlew :sa-admin:bootRun

# 2. Send 1000 messages
for i in {1..1000}; do
  curl -X POST http://localhost:1024/business/sample/kafka/basic/send \
    -d "{\"orderId\":\"ORD-$i\"}" &
done

# 3. Start second consumer instance (triggers rebalance)
./gradlew :sa-admin:bootRun -Dserver.port=1025

# 4. Verify all 1000 messages consumed
docker exec smart-admin-mysql mysql -u sa_user -psa123456 smart-admin \
  -e "SELECT COUNT(*) FROM t_order;"
```

**Expected outcome**:
- All 1000 messages consumed
- Rebalancing logs visible
- No duplicate processing

**Points**: 15/15 if successful

---

#### Test 4: Message Loss Verification

**Test with `acks=all`**:
```bash
# Configure producer with acks=all
# Send 10,000 messages
for i in {1..10000}; do
  curl -X POST http://localhost:1024/business/sample/kafka/basic/send \
    -d "{\"messageId\":\"MSG-$i\"}"
done

# Kill broker in middle of sending
docker kill smart-admin-kafka

# Restart broker
docker start smart-admin-kafka

# Count consumed messages
docker exec smart-admin-mysql mysql -u sa_user -psa123456 smart-admin \
  -e "SELECT COUNT(*) FROM t_messages;"

# Should be 10,000 (no loss)
```

**Expected outcome**:
- All 10,000 messages in database
- No message loss despite broker failure

**Points**: 20/20 if 10,000 messages

---

### L4 Scoring Formula

```
L4 Score =
  (Kafka Restart: 25) +
  (Network Failures: 20) +
  (Rebalancing: 15) +
  (Message Loss: 20) +
  (DLQ Recovery: 10) +
  (Graceful Shutdown: 10)
= 100 points
```

---

## L5: Performance Verification (0-100 points)

### Overview

Validate throughput, latency, and resource usage meet requirements.

**Scoring criteria**:
- ✅ Producer throughput ≥ 5,000 msg/sec (25 points)
- ✅ Consumer throughput ≥ 5,000 msg/sec (25 points)
- ✅ Batch processing ≥ 10,000 msg/sec (20 points)
- ✅ Latency p99 < 100ms (15 points)
- ✅ Consumer lag < 1,000 (10 points)
- ✅ Resource usage acceptable (5 points)

### Verification Procedures

#### Test 1: Producer Throughput

**Test script** (`performance-test.sh`):
```bash
#!/bin/bash

MESSAGES=10000
START=$(date +%s%3N)

for i in $(seq 1 $MESSAGES); do
  curl -s -X POST http://localhost:1024/business/sample/kafka/basic/send \
    -H "Content-Type: application/json" \
    -d "{\"messageId\":\"MSG-$i\"}" > /dev/null &

  # Limit concurrent requests
  if [ $((i % 100)) -eq 0 ]; then
    wait
  fi
done

wait
END=$(date +%s%3N)

DURATION=$((END - START))
THROUGHPUT=$((MESSAGES * 1000 / DURATION))

echo "Sent $MESSAGES messages in ${DURATION}ms"
echo "Throughput: $THROUGHPUT msg/sec"

# Verify >= 5000 msg/sec
if [ $THROUGHPUT -ge 5000 ]; then
  echo "✅ Producer throughput test PASSED"
  exit 0
else
  echo "❌ Producer throughput test FAILED"
  exit 1
fi
```

**Points**: 25/25 if ≥ 5,000 msg/sec

---

#### Test 2: Consumer Throughput

**Test script**:
```bash
# Send 10,000 messages
./performance-test.sh

# Measure consumption time
START=$(date +%s%3N)

# Wait for all messages consumed
while [ $(docker exec smart-admin-mysql mysql -u sa_user -psa123456 smart-admin \
  -se "SELECT COUNT(*) FROM t_messages;") -lt 10000 ]; do
  sleep 0.1
done

END=$(date +%s%3N)
DURATION=$((END - START))
THROUGHPUT=$((10000 * 1000 / DURATION))

echo "Consumed 10,000 messages in ${DURATION}ms"
echo "Throughput: $THROUGHPUT msg/sec"
```

**Points**: 25/25 if ≥ 5,000 msg/sec

---

#### Test 3: Latency Measurement

**Test script**:
```bash
# Send 100 messages with timestamps
for i in {1..100}; do
  TIMESTAMP=$(date +%s%3N)
  curl -X POST http://localhost:1024/business/sample/kafka/basic/send \
    -d "{\"messageId\":\"LATENCY-$i\",\"sendTime\":$TIMESTAMP}"
done

# Calculate latencies from logs
docker logs smart-admin-app 2>&1 | grep "LATENCY-" | \
  awk '{print $NF}' | sort -n | \
  awk '{arr[NR]=$1} END {print "p50:", arr[int(NR*0.5)], "p95:", arr[int(NR*0.95)], "p99:", arr[int(NR*0.99)]}'
```

**Expected output**:
```
p50: 15ms  p95: 45ms  p99: 85ms
```

**Points**: 15/15 if p99 < 100ms

---

#### Test 4: Consumer Lag

**Check lag**:
```bash
docker exec smart-admin-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group order-processor

# Output:
# GROUP           TOPIC            PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
# order-processor smart-admin-order 0         10000          10000           0
# order-processor smart-admin-order 1         10000          10000           0
# order-processor smart-admin-order 2         10000          10000           0
```

**Validation**:
- [ ] Lag < 1,000 for all partitions
- [ ] Lag decreasing over time (catching up)

**Points**: 10/10 if lag < 1,000

---

### L5 Scoring Formula

```
L5 Score =
  (Producer Throughput: 25) +
  (Consumer Throughput: 25) +
  (Batch Throughput: 20) +
  (Latency: 15) +
  (Consumer Lag: 10) +
  (Resource Usage: 5)
= 100 points
```

---

## L6: Operations Verification (0-100 points)

### Overview

Validate monitoring, logging, and operational readiness.

**Scoring criteria**:
- ✅ Monitoring dashboards available (20 points)
- ✅ Alerts configured (20 points)
- ✅ Logging complete and searchable (15 points)
- ✅ Health checks working (15 points)
- ✅ Troubleshooting runbooks (15 points)
- ✅ Backup and recovery tested (10 points)
- ✅ Documentation up-to-date (5 points)

### Verification Procedures

#### Check 1: Health Endpoint

**Test**:
```bash
curl http://localhost:1024/actuator/health | jq '.'

# Expected:
{
  "status": "UP",
  "components": {
    "kafka": {
      "status": "UP"
    }
  }
}
```

**Points**: 15/15 if UP

---

#### Check 2: Metrics Availability

**Test**:
```bash
curl http://localhost:1024/actuator/prometheus | grep -E "kafka_producer|kafka_consumer"

# Expected metrics:
# kafka_producer_record_send_total{...} 1500.0
# kafka_producer_record_error_total{...} 0.0
# kafka_consumer_records_consumed_total{...} 1500.0
# kafka_consumer_records_lag{...} 0.0
```

**Points**: 20/20 if all metrics present

---

#### Check 3: Logging Verification

**Test log completeness**:
```bash
# Check logs contain required information
docker logs smart-admin-app 2>&1 | grep -E "Sending message|Message sent|Received message|Message processed"

# Verify log format
docker logs smart-admin-app 2>&1 | grep "📤.*Sending message" | head -n 1

# Expected format:
# 2026-01-22 10:30:15.123 INFO  [nio-1024-exec-1] KafkaProducerService : 📤 Sending message | Topic: smart-admin-order | Key: ORD-123
```

**Validation**:
- [ ] Timestamp present
- [ ] Log level present (INFO, ERROR)
- [ ] Thread name present
- [ ] Class name present
- [ ] Structured message with fields

**Points**: 15/15 if complete

---

### L6 Scoring Formula

```
L6 Score =
  (Monitoring: 20) +
  (Alerts: 20) +
  (Logging: 15) +
  (Health Checks: 15) +
  (Runbooks: 15) +
  (Backup/Recovery: 10) +
  (Documentation: 5)
= 100 points
```

---

## Production Readiness Checklist

Use this checklist before deploying to production:

### Configuration
- [ ] All environments configured (dev, test, prod)
- [ ] Bootstrap servers set correctly per environment
- [ ] Producer configured with `acks=all`, `enable-idempotence=true`
- [ ] Consumer configured with `enable-auto-commit=false`
- [ ] Security configured (SSL/SASL for production)
- [ ] Credentials externalized (not hardcoded)

### Code Quality
- [ ] All listeners extend `AbstractKafkaListener`
- [ ] Error handling with DLQ routing implemented
- [ ] Logging includes structured fields (topic, partition, offset)
- [ ] Thread-safe implementation (no shared mutable state)
- [ ] Code review completed

### Testing
- [ ] Unit tests passing (≥80% coverage)
- [ ] Integration tests passing
- [ ] DLQ routing verified
- [ ] Idempotency tested
- [ ] Performance tests meet SLA (≥5,000 msg/sec)

### Reliability
- [ ] Survives Kafka broker restart
- [ ] Handles network failures gracefully
- [ ] Consumer rebalancing tested
- [ ] No message loss verified
- [ ] Graceful shutdown tested

### Operations
- [ ] Health check endpoint working
- [ ] Prometheus metrics exposed
- [ ] Grafana dashboard configured
- [ ] Alerts configured (consumer lag, error rate)
- [ ] Troubleshooting runbooks created
- [ ] Backup and recovery procedures documented

### Documentation
- [ ] Architecture documented
- [ ] Configuration reference complete
- [ ] Troubleshooting guide available
- [ ] Runbooks for common scenarios
- [ ] API documentation up-to-date

---

## Quality Score Calculation

### Manual Score Calculation

```
Total Score = (L1 + L2 + L3 + L4 + L5 + L6) / 6

Example:
- L1 (Configuration): 95/100
- L2 (Code Quality): 90/100
- L3 (Functional): 100/100
- L4 (Reliability): 85/100
- L5 (Performance): 80/100
- L6 (Operations): 75/100

Total = (95 + 90 + 100 + 85 + 80 + 75) / 6 = 87.5

Grade: Good Quality ✓ (80-89)
```

### Automated Score Collection

**Create verification script** (`verify-kafka.sh`):
```bash
#!/bin/bash

echo "Starting Kafka Quality Verification..."

# L1: Configuration
echo "Checking L1: Configuration..."
L1=0
# Add checks and increment L1

# L2: Code Quality
echo "Checking L2: Code Quality..."
L2=0
# Add checks and increment L2

# L3: Functional
echo "Checking L3: Functional Correctness..."
L3=0
# Run functional tests

# L4: Reliability
echo "Checking L4: Reliability..."
L4=0
# Run reliability tests

# L5: Performance
echo "Checking L5: Performance..."
L5=0
# Run performance tests

# L6: Operations
echo "Checking L6: Operations..."
L6=0
# Check operational readiness

# Calculate total
TOTAL=$(( (L1 + L2 + L3 + L4 + L5 + L6) / 6 ))

echo "===== Quality Score ====="
echo "L1 (Configuration): $L1/100"
echo "L2 (Code Quality): $L2/100"
echo "L3 (Functional): $L3/100"
echo "L4 (Reliability): $L4/100"
echo "L5 (Performance): $L5/100"
echo "L6 (Operations): $L6/100"
echo "TOTAL: $TOTAL/100"

if [ $TOTAL -ge 90 ]; then
  echo "✅ Production Ready"
  exit 0
elif [ $TOTAL -ge 80 ]; then
  echo "✓ Good Quality"
  exit 0
elif [ $TOTAL -ge 70 ]; then
  echo "⚠️ Acceptable"
  exit 1
else
  echo "❌ Not Production Ready"
  exit 1
fi
```

---

## Continuous Verification

### CI/CD Integration

**GitHub Actions example** (`.github/workflows/kafka-verification.yml`):
```yaml
name: Kafka Quality Verification

on:
  push:
    branches: [master, develop]
  pull_request:
    branches: [master]

jobs:
  verify-kafka:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Set up Java 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Start Kafka (Docker Compose)
        run: docker-compose -f docker/docker-compose-kafka.yml up -d

      - name: Run Kafka Verification
        run: ./verify-kafka.sh

      - name: Upload Verification Report
        uses: actions/upload-artifact@v3
        with:
          name: kafka-verification-report
          path: verification-report.json
```

---

## See Also

- [Testing Strategy](/kafka/testing/testing-strategy) - Overall testing approach
- [Unit Testing](/kafka/testing/unit-testing) - Unit test patterns
- [Integration Testing](/kafka/testing/integration-testing) - Integration test setup

---

**Last Updated**: 2026-01-22
