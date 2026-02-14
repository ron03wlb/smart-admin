# Migration Guide

Complete guide for migrating to SmartAdmin Kafka integration and handling version upgrades.

## Overview

This guide covers:
- **New adoption**: Integrating Kafka into existing SmartAdmin application
- **Version upgrades**: Upgrading Kafka or Spring Kafka versions
- **Breaking changes**: Handling incompatibilities between versions
- **Rollback procedures**: Safely reverting changes if needed

---

## Migrating from No Kafka to SmartAdmin Kafka

### Prerequisites

**Before starting**:
- [ ] Kafka cluster available (development, test, production)
- [ ] Topics planned and created
- [ ] Security configured (SSL/SASL if production)
- [ ] Team trained on Kafka concepts

### Step 1: Add Dependencies

**File**: `sa-common/sa-common-module-mq-kafka/build.gradle`

```gradle
dependencies {
    // Spring Kafka
    api 'org.springframework.kafka:spring-kafka:3.1.0'

    // SmartAdmin base
    api project(':sa-base:sa-base-common')
    api project(':sa-base:sa-base-module-support')
}
```

**Verify**:
```bash
./gradlew :sa-common:sa-common-module-mq-kafka:dependencies --configuration compileClasspath
```

---

### Step 2: Enable Kafka Configuration

**File**: `sa-admin/src/main/resources/dev/sa-base.yaml`

```yaml
smart:
  kafka:
    enabled: true                      # Enable Kafka
    bootstrap-servers: localhost:9092  # Development Kafka

spring:
  kafka:
    producer:
      acks: 1
      retries: 0
    consumer:
      group-id: smart-admin-dev
      enable-auto-commit: false
      auto-offset-reset: earliest
    listener:
      ack-mode: MANUAL
```

**Test configuration**:
```bash
./gradlew :sa-admin:bootRun

# Check health endpoint
curl http://localhost:1024/actuator/health/kafka
```

---

### Step 3: Create Kafka Topics

```bash
# Create topics
docker exec smart-admin-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --create --topic smart-admin-order \
  --partitions 3 --replication-factor 1

docker exec smart-admin-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --create --topic smart-admin-order-dlq \
  --partitions 3 --replication-factor 1
```

---

### Step 4: Migrate Existing Synchronous Logic

**Before** (synchronous processing):
```java
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderDao orderDao;
    private final PaymentService paymentService;

    @Transactional
    public void createOrder(OrderDTO order) {
        // 1. Save order
        OrderEntity entity = orderDao.insert(order);

        // 2. Process payment (synchronous, blocking)
        paymentService.processPayment(entity.getOrderId());

        // 3. Send notification (synchronous, blocking)
        notificationService.sendOrderConfirmation(entity.getOrderId());
    }
}
```

**After** (asynchronous with Kafka):
```java
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderDao orderDao;
    private final KafkaProducerService kafkaProducerService;

    @Transactional
    public void createOrder(OrderDTO order) {
        // 1. Save order (fast)
        OrderEntity entity = orderDao.insert(order);

        // 2. Publish event (non-blocking)
        String orderJson = JSON.toJSONString(entity);
        kafkaProducerService.sendAsync("smart-admin-order", entity.getOrderId(), orderJson);

        // Returns immediately, payment/notification handled by consumers
    }
}

// New consumer for payment
@Component
@RequiredArgsConstructor
public class OrderPaymentListener extends AbstractKafkaListener {
    private final PaymentService paymentService;

    @KafkaListener(topics = "smart-admin-order", groupId = "payment-processor")
    public void onMessage(ConsumerRecord<String, String> record) {
        handleMessage(record);
    }

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        OrderEntity order = JSON.parseObject(record.value(), OrderEntity.class);
        paymentService.processPayment(order.getOrderId());
    }
}
```

**Benefits**:
- Response time: 500ms → 50ms (10x faster)
- Resilience: Payment failures don't block order creation
- Scalability: Payment processing can scale independently

---

### Step 5: Gradual Rollout

**Strategy**: Feature flag for gradual adoption

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    private final KafkaProducerService kafkaProducerService;
    private final PaymentService paymentService;

    @Value("${feature.kafka.enabled:false}")
    private boolean kafkaEnabled;

    public void createOrder(OrderDTO order) {
        orderDao.insert(order);

        if (kafkaEnabled) {
            // New path: Kafka async
            kafkaProducerService.sendAsync("smart-admin-order", order.getOrderId(), orderJson);
        } else {
            // Old path: Synchronous
            paymentService.processPayment(order.getOrderId());
        }
    }
}
```

**Rollout plan**:
1. **Week 1**: 10% traffic → Kafka, monitor errors
2. **Week 2**: 50% traffic → Kafka, monitor performance
3. **Week 3**: 100% traffic → Kafka, remove feature flag

---

## Upgrading Kafka Versions

### Kafka 3.3.x → 3.5.x

**Breaking changes**: None major

**Recommended changes**:
- Update broker version
- Update client library (Spring Kafka)
- Test compatibility

**Steps**:

**1. Update dependencies**:
```gradle
dependencies {
    api 'org.springframework.kafka:spring-kafka:3.1.0'  // Updated
}
```

**2. Test locally**:
```bash
# Pull new Kafka image
docker pull confluentinc/cp-kafka:7.5.0

# Update docker-compose.yml
kafka:
  image: confluentinc/cp-kafka:7.5.0
```

**3. Deploy to test environment**:
```bash
# Gracefully restart brokers one by one
docker restart smart-admin-kafka-1
sleep 30
docker restart smart-admin-kafka-2
sleep 30
docker restart smart-admin-kafka-3
```

**4. Monitor**:
```bash
# Check consumer lag
docker exec smart-admin-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --all-groups

# Check error rate
curl http://localhost:1024/actuator/metrics/kafka.producer.record-error-rate
```

---

### Spring Kafka 2.9.x → 3.1.x

**Breaking changes**:

**1. Removed deprecated methods**:
```java
// ❌ Removed
kafkaTemplate.send(record).get();

// ✅ New
CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(record);
future.get();
```

**2. Changed error handler API**:
```java
// ❌ Old
@Bean
public ErrorHandler errorHandler() {
    return new SeekToCurrentErrorHandler();
}

// ✅ New
@Bean
public CommonErrorHandler errorHandler() {
    return new DefaultErrorHandler();
}
```

**Migration steps**:

**1. Search for deprecated APIs**:
```bash
# Find deprecated method usages
grep -r "SeekToCurrentErrorHandler" smart-admin-api-java21-springboot3/sa-admin/src/main/java
```

**2. Update code**:
```java
// Update all usages to new API
@Bean
public CommonErrorHandler errorHandler() {
    return new DefaultErrorHandler(
        new FixedBackOff(1000L, 3L)  // 3 retries with 1s delay
    );
}
```

**3. Test thoroughly**:
```bash
./gradlew :sa-admin:test --tests "*KafkaTest"
```

---

## Breaking Changes by Version

### SmartAdmin Kafka v1.0 → v2.0 (Hypothetical)

**Breaking change**: `AbstractKafkaListener` signature changed

**v1.0**:
```java
@Override
protected void doHandle(String message) {
    // Old signature
}
```

**v2.0**:
```java
@Override
protected void doHandle(ConsumerRecord<String, String> record) {
    // New signature with full record access
}
```

**Migration**:
```java
// Update all listeners
@Override
protected void doHandle(ConsumerRecord<String, String> record) {
    String message = record.value();  // Extract value
    // Rest of logic unchanged
}
```

---

## Migration Checklist

### Pre-Migration

- [ ] **Backup**: Snapshot current state (database, configs, offsets)
- [ ] **Document**: Current architecture and message flows
- [ ] **Identify**: All integration points and dependencies
- [ ] **Plan**: Rollback strategy
- [ ] **Communicate**: Notify team and stakeholders
- [ ] **Test environment**: Set up identical test cluster

### During Migration

- [ ] **Feature flag**: Enable/disable new code path
- [ ] **Parallel run**: Run old and new systems simultaneously
- [ ] **Monitor**: Watch metrics, logs, errors closely
- [ ] **Gradual rollout**: 10% → 50% → 100% traffic
- [ ] **Verify**: Functional correctness, performance, reliability

### Post-Migration

- [ ] **Monitor**: 24-48 hours of production monitoring
- [ ] **Cleanup**: Remove old code paths
- [ ] **Document**: Update architecture diagrams
- [ ] **Retrospective**: Lessons learned
- [ ] **Celebrate**: Migration complete! 🎉

---

## Rollback Procedures

### Scenario 1: Configuration Rollback

**Issue**: New Kafka configuration causing errors

**Rollback**:
```bash
# 1. Disable Kafka
# Edit sa-base.yaml
smart:
  kafka:
    enabled: false  # Disable immediately

# 2. Restart application
docker restart smart-admin-app

# 3. Verify
curl http://localhost:1024/actuator/health
```

---

### Scenario 2: Code Rollback

**Issue**: New listener causing message loss

**Rollback**:
```bash
# 1. Revert to previous version
git revert HEAD
./gradlew clean build

# 2. Deploy previous version
docker-compose up -d smart-admin-app

# 3. Reset consumer offsets (if needed)
docker exec smart-admin-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group order-processor \
  --reset-offsets --to-datetime 2026-01-22T10:00:00.000 \
  --execute
```

---

### Scenario 3: Broker Rollback

**Issue**: Kafka 3.5 causing compatibility issues

**Rollback**:
```yaml
# docker-compose.yml
kafka:
  image: confluentinc/cp-kafka:7.3.0  # Revert to 7.3.0

# Restart
docker-compose down
docker-compose up -d
```

---

## Data Migration Strategies

### Strategy 1: Dual Write

Write to both old and new systems during transition.

```java
@Service
public class OrderService {
    public void createOrder(OrderDTO order) {
        // Write to database (old)
        orderDao.insert(order);

        // Write to Kafka (new)
        kafkaProducerService.send("smart-admin-order", order.getOrderId(), orderJson);

        // Both succeed or both fail
    }
}
```

**Duration**: 1-2 weeks
**Rollback**: Easy (just stop writing to Kafka)

---

### Strategy 2: Change Data Capture (CDC)

Automatically publish database changes to Kafka.

```yaml
# Debezium connector
connector:
  database: smart-admin
  table: t_order
  topic: smart-admin-order-cdc
```

**Duration**: Ongoing (permanent sync)
**Rollback**: Disable connector

---

## Testing Migration

### Test Plan

**1. Functional testing**:
```bash
# Send test message
curl -X POST http://localhost:1024/business/sample/kafka/basic/send \
  -d '{"orderId":"TEST-001"}'

# Verify consumption
docker logs smart-admin-app | grep "TEST-001"
```

**2. Performance testing**:
```bash
# Load test
for i in {1..1000}; do
  curl -X POST http://localhost:1024/business/sample/kafka/basic/send \
    -d "{\"orderId\":\"LOAD-$i\"}" &
done

# Measure throughput
```

**3. Chaos testing**:
```bash
# Kill broker during traffic
docker kill smart-admin-kafka-1

# Verify no message loss
# Check consumer lag
```

---

## Common Migration Issues

### Issue 1: Message Loss During Migration

**Symptom**: Some messages not processed

**Cause**: Consumer offset mismatch

**Solution**:
```bash
# Reset offsets to earliest
docker exec smart-admin-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group order-processor \
  --reset-offsets --to-earliest \
  --all-topics --execute
```

---

### Issue 2: Duplicate Processing

**Symptom**: Same message processed twice

**Cause**: Replay during cutover

**Solution**: Implement idempotency
```java
@Override
protected void doHandle(ConsumerRecord<String, String> record) {
    String messageId = record.key();

    // Check if already processed
    if (idempotencyGuard.isProcessed(messageId)) {
        log.warn("Duplicate message skipped | MessageId: {}", messageId);
        return;
    }

    // Process message
    processOrder(record.value());

    // Mark as processed
    idempotencyGuard.markProcessed(messageId);
}
```

---

## Version Compatibility Matrix

| SmartAdmin | Spring Boot | Spring Kafka | Kafka Broker | Java |
|------------|-------------|--------------|--------------|------|
| 3.5.x | 3.5.4 | 3.1.0 | 3.3.x - 3.5.x | 21 |
| 3.4.x | 3.4.0 | 3.0.0 | 3.2.x - 3.4.x | 17+ |
| 3.3.x | 3.3.0 | 2.9.x | 3.1.x - 3.3.x | 17+ |

---

## See Also

- [Configuration Reference](/kafka/reference/configuration-reference) - All configuration properties
- [Deployment Guide](/kafka/operations/deployment) - Production deployment
- [Troubleshooting](/kafka/troubleshooting/diagnostic-guide) - Common issues

---

**Last Updated**: 2026-01-22
