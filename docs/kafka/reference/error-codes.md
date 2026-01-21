# Error Codes Reference

Complete error code catalog with troubleshooting guide for SmartAdmin Kafka integration.

## Overview

This document catalogs all error codes, exceptions, and failure scenarios you may encounter when working with Kafka in SmartAdmin, along with troubleshooting steps.

**Error categories**:
1. **Connection Errors** - Bootstrap server, network issues
2. **Producer Errors** - Message sending failures
3. **Consumer Errors** - Message consumption failures
4. **Serialization Errors** - Data format issues
5. **Configuration Errors** - Property misconfigurations
6. **Operational Errors** - Rebalancing, timeout issues

---

## Connection Errors

### KAFKA-CONN-001: Connection Refused

**Error message**:
```
org.apache.kafka.common.errors.TimeoutException: Failed to update metadata after 60000 ms.
Caused by: java.net.ConnectException: Connection refused
```

**Cause**: Cannot connect to Kafka bootstrap servers

**Common reasons**:
- Kafka broker is not running
- Wrong bootstrap server address
- Network firewall blocking port 9092
- Docker network misconfiguration

**Solution**:
```bash
# 1. Verify Kafka is running
docker ps | grep kafka

# 2. Test connectivity
telnet localhost 9092

# 3. Check bootstrap servers configuration
grep "bootstrap-servers" sa-admin/src/main/resources/*/sa-base.yaml

# 4. Verify Kafka logs
docker logs smart-admin-kafka
```

**Fix**:
```yaml
# Correct configuration
smart:
  kafka:
    bootstrap-servers: localhost:9092  # Ensure correct address
```

---

### KAFKA-CONN-002: Broker Not Available

**Error message**:
```
org.apache.kafka.common.errors.BrokerNotAvailableException: Broker may not be available
```

**Cause**: Kafka broker exists but is not ready to accept connections

**Solution**:
```bash
# Wait for broker to be ready
docker exec smart-admin-kafka kafka-broker-api-versions \
  --bootstrap-server localhost:9092

# Check broker health
curl http://localhost:1024/actuator/health/kafka
```

**Typical scenario**: Broker is starting up, wait 10-30 seconds

---

### KAFKA-CONN-003: Authentication Failed

**Error message**:
```
org.apache.kafka.common.errors.SaslAuthenticationException:
Authentication failed: Invalid username or password
```

**Cause**: SASL authentication credentials are incorrect

**Solution**:
```yaml
# Verify credentials configuration
spring:
  kafka:
    properties:
      security.protocol: SASL_SSL
      sasl.mechanism: SCRAM-SHA-512
      sasl.jaas.config: |
        org.apache.kafka.common.security.scram.ScramLoginModule required
        username="${KAFKA_USERNAME}"  # Environment variable
        password="${KAFKA_PASSWORD}";
```

**Check**:
```bash
# Verify environment variables are set
echo $KAFKA_USERNAME
echo $KAFKA_PASSWORD
```

---

## Producer Errors

### KAFKA-PROD-001: Message Too Large

**Error message**:
```
org.apache.kafka.common.errors.RecordTooLargeException:
The message is 2097152 bytes when serialized which is larger than 1048576
```

**Cause**: Message exceeds `max.request.size` (default 1 MB)

**Solution**:

**Option 1**: Increase max message size (not recommended)
```yaml
spring:
  kafka:
    producer:
      properties:
        max.request.size: 2097152  # 2 MB
```

**Option 2**: Split large messages (recommended)
```java
// Split large payload into smaller chunks
List<String> chunks = splitMessage(largePayload, 500_000);  // 500 KB chunks
for (String chunk : chunks) {
    kafkaProducerService.send(topic, messageId, chunk);
}
```

---

### KAFKA-PROD-002: Timeout Exception

**Error message**:
```
org.apache.kafka.common.errors.TimeoutException:
Expiring 1 record(s) for smart-admin-order-0: 30036 ms has passed since batch creation
```

**Cause**: Producer cannot send message within `delivery.timeout.ms`

**Common reasons**:
- Broker overloaded
- Network latency
- Insufficient retries

**Solution**:
```yaml
spring:
  kafka:
    producer:
      properties:
        delivery.timeout.ms: 120000     # 2 minutes (increased)
        request.timeout.ms: 30000       # 30 seconds
        retries: 5                      # More retries
```

---

### KAFKA-PROD-003: Not Enough Replicas

**Error message**:
```
org.apache.kafka.common.errors.NotEnoughReplicasException:
Messages are rejected since there are fewer in-sync replicas than required
```

**Cause**: Not enough in-sync replicas to meet `min.insync.replicas` requirement

**Solution**:
```bash
# Check topic configuration
docker exec smart-admin-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --describe --topic smart-admin-order

# Verify ISR (in-sync replicas)
# Topic: smart-admin-order  Partition: 0  Replicas: 1,2,3  Isr: 1
```

**Fix**: Ensure enough brokers are running and healthy

---

## Consumer Errors

### KAFKA-CONS-001: Offset Out of Range

**Error message**:
```
org.apache.kafka.clients.consumer.OffsetOutOfRangeException:
Fetch position FetchPosition{offset=1500, offsetEpoch=Optional.empty} is out of range
```

**Cause**: Committed offset no longer exists (messages deleted due to retention)

**Solution**:
```yaml
spring:
  kafka:
    consumer:
      auto-offset-reset: earliest  # Reset to beginning
      # OR
      auto-offset-reset: latest    # Skip to end
```

**Manual reset**:
```bash
# Reset consumer group offsets
docker exec smart-admin-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group order-processor \
  --reset-offsets --to-earliest \
  --topic smart-admin-order \
  --execute
```

---

### KAFKA-CONS-002: Rebalance in Progress

**Error message**:
```
org.apache.kafka.clients.consumer.CommitFailedException:
Commit cannot be completed since the group has already rebalanced
```

**Cause**: Consumer took too long to process messages, group rebalanced

**Solution**:
```yaml
spring:
  kafka:
    consumer:
      max-poll-interval-ms: 600000    # Increase to 10 minutes
      max-poll-records: 100           # Reduce batch size
```

**Rule**: Ensure `processing_time < max-poll-interval-ms`

---

### KAFKA-CONS-003: Deserialization Failed

**Error message**:
```
org.apache.kafka.common.errors.SerializationException:
Error deserializing key/value for partition smart-admin-order-0 at offset 42
```

**Cause**: Cannot deserialize message (format mismatch, corrupted data)

**Solution**:

**Option 1**: Skip failed message
```yaml
spring:
  kafka:
    consumer:
      properties:
        spring.kafka.listener.error-handler: seeker  # Skip and seek
```

**Option 2**: Send to DLQ (handled by AbstractKafkaListener automatically)

**Prevention**: Validate data before sending:
```java
// Producer side validation
if (isValidJson(orderJson)) {
    kafkaProducerService.send(topic, key, orderJson);
} else {
    log.error("Invalid JSON format, not sending");
}
```

---

## Serialization Errors

### KAFKA-SER-001: JSON Parse Error

**Error message**:
```
com.alibaba.fastjson2.JSONException:
illegal input pos 15, character {, line 1, column 16, fastjson-version 2.0.40
```

**Cause**: Invalid JSON format in message payload

**Example of invalid JSON**:
```json
{"orderId": "ORD-001", "amount": }  // Missing value
{"orderId": 'ORD-001'}              // Single quotes invalid
```

**Solution**:
```java
// Robust deserialization with error handling
try {
    OrderDTO order = JSON.parseObject(record.value(), OrderDTO.class);
} catch (JSONException e) {
    log.error("Invalid JSON format | Value: {}", record.value(), e);
    throw new BusinessException("Invalid message format");  // Goes to DLQ
}
```

---

## Configuration Errors

### KAFKA-CFG-001: Kafka Not Enabled

**Error message**:
```
org.springframework.beans.factory.NoSuchBeanDefinitionException:
No qualifying bean of type 'KafkaProducerService' available
```

**Cause**: `smart.kafka.enabled` is `false` or not set

**Solution**:
```yaml
smart:
  kafka:
    enabled: true  # Must be explicitly enabled
```

---

### KAFKA-CFG-002: Bootstrap Servers Not Configured

**Error message**:
```
java.lang.IllegalArgumentException:
bootstrap.servers must be non-null
```

**Cause**: `smart.kafka.bootstrap-servers` is not set

**Solution**:
```yaml
smart:
  kafka:
    enabled: true
    bootstrap-servers: localhost:9092  # Required
```

---

### KAFKA-CFG-003: Invalid Property Value

**Error message**:
```
org.apache.kafka.common.config.ConfigException:
Invalid value -1 for configuration acks: String must be one of: all, -1, 0, 1
```

**Cause**: Configuration property has invalid value

**Solution**:
```yaml
# Correct values
spring:
  kafka:
    producer:
      acks: all  # Valid: 0, 1, all (or -1)
```

---

## Operational Errors

### KAFKA-OP-001: Consumer Lag Too High

**Symptom**: Consumer cannot keep up with producer

**Check lag**:
```bash
docker exec smart-admin-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group order-processor

# Output shows LAG > 1000
GROUP           TOPIC     PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
order-processor order     0          5000           10000           5000
```

**Solutions**:

**1. Increase concurrency**:
```yaml
spring:
  kafka:
    listener:
      concurrency: 6  # Match partition count
```

**2. Increase partitions**:
```bash
docker exec smart-admin-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --alter --topic smart-admin-order \
  --partitions 6
```

**3. Optimize processing**:
```java
// Use batch processing
@Component
public class OrderBatchListener extends AbstractBatchKafkaListener<OrderDTO> {
    // Processes 100 messages at once
}
```

---

### KAFKA-OP-002: Out of Memory

**Error message**:
```
java.lang.OutOfMemoryError: Java heap space
```

**Cause**: Too many messages in memory

**Solutions**:

**1. Reduce `max-poll-records`**:
```yaml
spring:
  kafka:
    consumer:
      max-poll-records: 100  # Reduce from 500
```

**2. Increase heap size**:
```bash
java -Xms2g -Xmx4g -jar sa-admin.jar
```

**3. Process in smaller batches**:
```java
@Override
protected void doBatchHandle(List<ConsumerRecord<String, String>> records) {
    // Process in chunks of 50
    Lists.partition(records, 50).forEach(chunk -> {
        processChunk(chunk);
    });
}
```

---

## Exception Hierarchy

### Kafka Exceptions

```
KafkaException (base)
├── TimeoutException
├── InterruptException
├── SerializationException
├── AuthenticationException
│   ├── SaslAuthenticationException
│   └── AuthorizationException
├── InvalidTopicException
├── OffsetOutOfRangeException
├── RecordTooLargeException
├── NotEnoughReplicasException
└── CommitFailedException
```

### Handling Strategy

**Retriable exceptions** (retry):
- `TimeoutException`
- `NotEnoughReplicasException`
- `NetworkException`

**Non-retriable exceptions** (send to DLQ):
- `SerializationException`
- `RecordTooLargeException`
- `InvalidTopicException`
- `AuthenticationException`

**Example**:
```java
@Override
protected void handleError(ConsumerRecord<String, String> record, Exception e) {
    if (e instanceof TimeoutException) {
        log.warn("Retriable error, will retry");
        throw e;  // Retry
    } else {
        log.error("Non-retriable error, sending to DLQ");
        super.handleError(record, e);  // DLQ
    }
}
```

---

## Troubleshooting Workflow

### Step 1: Check Kafka Connectivity

```bash
# 1. Verify Kafka is running
docker ps | grep kafka

# 2. Check application logs
docker logs smart-admin-app 2>&1 | grep -i error

# 3. Test health endpoint
curl http://localhost:1024/actuator/health/kafka
```

### Step 2: Verify Configuration

```bash
# Check bootstrap servers
grep "bootstrap-servers" smart-admin-api-java21-springboot3/sa-admin/src/main/resources/*/sa-base.yaml

# Check enabled flag
grep "smart.kafka.enabled" smart-admin-api-java21-springboot3/sa-admin/src/main/resources/*/sa-base.yaml
```

### Step 3: Check Topic Exists

```bash
docker exec smart-admin-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --list | grep smart-admin
```

### Step 4: Monitor Consumer Groups

```bash
docker exec smart-admin-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group order-processor
```

### Step 5: Check Metrics

```bash
# Producer metrics
curl http://localhost:1024/actuator/prometheus | grep kafka_producer

# Consumer metrics
curl http://localhost:1024/actuator/prometheus | grep kafka_consumer
```

---

## Quick Reference

| Error Code | Category | Severity | Retry? |
|------------|----------|----------|--------|
| KAFKA-CONN-001 | Connection | High | Yes |
| KAFKA-CONN-002 | Connection | Medium | Yes |
| KAFKA-CONN-003 | Authentication | High | No |
| KAFKA-PROD-001 | Producer | Medium | No |
| KAFKA-PROD-002 | Producer | Medium | Yes |
| KAFKA-PROD-003 | Producer | High | Yes |
| KAFKA-CONS-001 | Consumer | Medium | No |
| KAFKA-CONS-002 | Consumer | Medium | Yes |
| KAFKA-CONS-003 | Consumer | Medium | No |
| KAFKA-SER-001 | Serialization | Low | No |
| KAFKA-CFG-001 | Configuration | High | No |
| KAFKA-CFG-002 | Configuration | High | No |
| KAFKA-CFG-003 | Configuration | Medium | No |
| KAFKA-OP-001 | Operational | Medium | N/A |
| KAFKA-OP-002 | Operational | High | N/A |

---

## See Also

- [Troubleshooting Guide](/kafka/troubleshooting/diagnostic-guide) - Diagnostic procedures
- [Common Issues](/kafka/troubleshooting/common-issues) - Frequent problems and solutions
- [Configuration Reference](/kafka/reference/configuration-reference) - All configuration properties
- [Health Checks](/kafka/operations/health-checks) - Monitoring health

---

**Last Updated**: 2026-01-22
