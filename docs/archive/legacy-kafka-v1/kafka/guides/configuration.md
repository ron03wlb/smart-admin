# Configuration Guide

Complete configuration reference for SmartAdmin's Kafka integration covering all producer, consumer, and operational settings.

## Overview

SmartAdmin's Kafka configuration uses a hierarchical namespace under `smart.kafka`:

```yaml
smart:
  kafka:
    bootstrap-servers:     # Connection
    producer:              # Producer settings
    consumer:              # Consumer settings
    listener:              # Listener container settings
    batch:                 # Batch processing settings
```

---

## Connection Configuration

### Bootstrap Servers

```yaml
smart:
  kafka:
    # Single broker (development)
    bootstrap-servers: localhost:9092

    # Multiple brokers (production)
    bootstrap-servers: kafka-1:9092,kafka-2:9092,kafka-3:9092
```

**Best Practices**:
- **Development**: Single broker `localhost:9092`
- **Test**: 2-3 brokers for replication testing
- **Production**: 3+ brokers for high availability

---

## Producer Configuration

### Basic Producer Settings

```yaml
smart:
  kafka:
    producer:
      # Acknowledgment
      acks: all                     # Wait for all replicas (most reliable)
      # Options: 0 (fire-and-forget), 1 (leader only), all (all replicas)

      # Retries
      retries: 3                    # Retry 3 times on failure
      retry-backoff-ms: 100         # 100ms between retries

      # Idempotence
      enable-idempotence: true      # Exactly-once semantics
      max-in-flight-requests-per-connection: 1  # Required for ordering with idempotence

      # Timeouts
      request-timeout-ms: 30000     # 30s request timeout
```

### Performance Tuning

```yaml
smart:
  kafka:
    producer:
      # Batching
      batch-size: 16384             # 16KB batch size
      linger-ms: 5                  # Wait 5ms to batch messages
      buffer-memory: 33554432       # 32MB buffer

      # Compression
      compression-type: lz4         # lz4, gzip, snappy, zstd, none

      # Throughput vs ordering trade-off
      max-in-flight-requests-per-connection: 5  # Higher throughput
      # Set to 1 for strict ordering
```

### Transactional Producer

```yaml
smart:
  kafka:
    producer:
      # Enable transactions
      transaction-id-prefix: tx-smart-admin-

      # Required for transactions
      acks: all
      enable-idempotence: true
      max-in-flight-requests-per-connection: 1
```

---

## Consumer Configuration

### Basic Consumer Settings

```yaml
smart:
  kafka:
    consumer:
      # Offset management
      enable-auto-commit: false     # Manual commit (recommended)
      auto-offset-reset: earliest   # Start from beginning if no offset
      # Options: earliest, latest, none

      # Session management
      session-timeout-ms: 45000     # 45s session timeout
      heartbeat-interval-ms: 3000   # 3s heartbeat
      max-poll-interval-ms: 300000  # 5min max processing time

      # Polling
      max-poll-records: 500         # Fetch up to 500 messages per poll
      fetch-min-size: 1             # Min bytes to fetch (1 byte = no wait)
      fetch-max-wait: 500           # Max wait time for fetch-min-size (ms)
```

### Consumer Group Settings

```yaml
smart:
  kafka:
    consumer:
      # Group configuration
      group-id: smart-admin-default-group  # Default group ID

      # Isolation level
      isolation-level: read_committed      # For transactional messages
      # Options: read_uncommitted, read_committed
```

### Deserialization

```yaml
smart:
  kafka:
    consumer:
      # Key/Value deserializers (defaults to String)
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
```

---

## Listener Container Configuration

### Basic Listener Settings

```yaml
smart:
  kafka:
    listener:
      # Acknowledgment mode
      ack-mode: manual              # Manual acknowledgment
      # Options: manual, batch, time, count, manual_immediate

      # Concurrency
      concurrency: 3                # 3 concurrent consumers per @KafkaListener

      # Batch mode
      batch-listener: false         # Set to true for batch processing
```

### Error Handling

```yaml
smart:
  kafka:
    listener:
      # Error handling
      missing-topics-fatal: false   # Don't fail if topic doesn't exist yet

      # Retry template (optional)
      retry-topic-enabled: false
```

---

## Batch Processing Configuration

### Batch Consumer Settings

```yaml
smart:
  kafka:
    batch:
      enabled: true                 # Enable batch processing
      size: 100                     # Max messages per batch

      # Message aggregation
      aggregate:
        enabled: true
        count: 50                   # Aggregate 50 messages
        timeout-ms: 5000            # Or 5 seconds timeout
```

### Batch Container Factory

```yaml
smart:
  kafka:
    consumer:
      max-poll-records: 500         # Fetch up to 500 messages

    listener:
      batch-listener: true          # Enable batch mode
      ack-mode: manual              # Manual acknowledgment required
      concurrency: 3                # 3 concurrent batch consumers
```

---

## Environment-Specific Configuration

### Development (application-dev.yml)

```yaml
smart:
  kafka:
    bootstrap-servers: localhost:9092

    producer:
      acks: 1                       # Leader only (faster)
      retries: 1                    # Minimal retries
      enable-idempotence: false     # Simpler
      compression-type: none        # No compression

    consumer:
      auto-offset-reset: latest     # Skip old messages
      max-poll-records: 100         # Smaller batches

    listener:
      concurrency: 1                # Single thread
```

### Test (application-test.yml)

```yaml
smart:
  kafka:
    bootstrap-servers: kafka-test:9092

    producer:
      acks: all                     # Test reliability
      retries: 2
      enable-idempotence: true

    consumer:
      auto-offset-reset: earliest   # Process all messages
      max-poll-records: 200

    listener:
      concurrency: 2                # Limited parallelism
```

### Production (application-prod.yml)

```yaml
smart:
  kafka:
    bootstrap-servers: kafka-1:9092,kafka-2:9092,kafka-3:9092

    producer:
      acks: all                     # Maximum reliability
      retries: 3
      enable-idempotence: true
      max-in-flight-requests-per-connection: 1
      compression-type: lz4         # Enable compression

    consumer:
      auto-offset-reset: earliest   # Don't lose messages
      max-poll-records: 500         # Large batches
      session-timeout-ms: 45000
      heartbeat-interval-ms: 3000
      max-poll-interval-ms: 300000

    listener:
      concurrency: 5                # Higher throughput
      ack-mode: manual

    batch:
      enabled: true
      size: 100
```

---

## Security Configuration

### SSL/TLS

```yaml
smart:
  kafka:
    bootstrap-servers: kafka:9093  # SSL port

    ssl:
      enabled: true
      protocol: TLSv1.2
      truststore-location: classpath:kafka.truststore.jks
      truststore-password: ${TRUSTSTORE_PASSWORD}
      keystore-location: classpath:kafka.keystore.jks
      keystore-password: ${KEYSTORE_PASSWORD}
      key-password: ${KEY_PASSWORD}
```

### SASL Authentication

```yaml
smart:
  kafka:
    bootstrap-servers: kafka:9094  # SASL port

    sasl:
      enabled: true
      mechanism: PLAIN              # PLAIN, SCRAM-SHA-256, SCRAM-SHA-512
      jaas-config: |
        org.apache.kafka.common.security.plain.PlainLoginModule required
        username="${KAFKA_USERNAME}"
        password="${KAFKA_PASSWORD}";

    security:
      protocol: SASL_SSL            # SASL_PLAINTEXT, SASL_SSL
```

---

## Monitoring Configuration

### Metrics Configuration

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus

  metrics:
    export:
      prometheus:
        enabled: true

    tags:
      application: smart-admin
      environment: ${spring.profiles.active}

  health:
    kafka:
      enabled: true
```

### Custom Metrics

```yaml
smart:
  kafka:
    metrics:
      enabled: true
      prefix: smart.kafka           # Metric name prefix
      recording-level: INFO         # DEBUG, INFO, WARN
```

---

## Advanced Configuration

### Custom Serialization

```yaml
smart:
  kafka:
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer

      properties:
        spring.json.trusted.packages: net.lab1024.sa.common.mq.kafka.domain
```

### Partitioning Strategy

```yaml
smart:
  kafka:
    producer:
      # Partitioning
      partitioner-class: org.apache.kafka.clients.producer.RoundRobinPartitioner
      # Options: DefaultPartitioner, RoundRobinPartitioner, UniformStickyPartitioner
```

### Interceptors

```yaml
smart:
  kafka:
    producer:
      interceptor-classes: com.example.CustomProducerInterceptor

    consumer:
      interceptor-classes: com.example.CustomConsumerInterceptor
```

---

## Configuration Profiles

### Profile-Based Configuration

**application.yml**:
```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}

smart:
  kafka:
    # Common settings
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
```

**application-dev.yml**:
```yaml
smart:
  kafka:
    bootstrap-servers: localhost:9092
    # Development-specific settings
```

**application-prod.yml**:
```yaml
smart:
  kafka:
    bootstrap-servers: ${KAFKA_BROKERS}  # From environment variable
    # Production-specific settings
```

---

## Configuration Best Practices

### 1. Use Environment Variables

```yaml
smart:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      acks: ${KAFKA_PRODUCER_ACKS:all}
    consumer:
      group-id: ${KAFKA_CONSUMER_GROUP:smart-admin-group}
```

### 2. Separate Secrets

```yaml
smart:
  kafka:
    ssl:
      truststore-password: ${KAFKA_TRUSTSTORE_PASSWORD}
      keystore-password: ${KAFKA_KEYSTORE_PASSWORD}
    sasl:
      jaas-config: |
        org.apache.kafka.common.security.plain.PlainLoginModule required
        username="${KAFKA_USERNAME}"
        password="${KAFKA_PASSWORD}";
```

### 3. Document Custom Settings

```yaml
smart:
  kafka:
    # Custom setting for order processing
    # Default: 100
    # Range: 10-500
    # Impact: Higher values improve throughput but increase memory usage
    batch:
      size: 100
```

### 4. Validate Configuration

```java
@Configuration
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaConfigValidation {

    @Bean
    public CommandLineRunner validateKafkaConfig(KafkaProperties properties) {
        return args -> {
            if (properties.getProducer().getAcks().equals("all") &&
                !properties.getProducer().isEnableIdempotence()) {
                log.warn("Producer has acks=all but idempotence is disabled");
            }

            if (properties.getConsumer().getMaxPollIntervalMs() < 60000) {
                log.warn("max-poll-interval-ms is very low: {}",
                    properties.getConsumer().getMaxPollIntervalMs());
            }
        };
    }
}
```

---

## Configuration Checklist

### Development

- [ ] `bootstrap-servers` points to localhost
- [ ] `auto-offset-reset` set to `latest`
- [ ] `concurrency` set to 1 or 2
- [ ] Compression disabled for simplicity
- [ ] Minimal retries

### Test

- [ ] `bootstrap-servers` points to test cluster
- [ ] `auto-offset-reset` set to `earliest`
- [ ] `concurrency` limited (2-3)
- [ ] Reliability settings enabled
- [ ] Monitoring configured

### Production

- [ ] `bootstrap-servers` lists all brokers
- [ ] `acks` set to `all`
- [ ] `enable-idempotence` set to `true`
- [ ] `retries` set to 3+
- [ ] `compression-type` configured (lz4 recommended)
- [ ] `max-poll-interval-ms` appropriate for workload
- [ ] `concurrency` optimized for throughput
- [ ] Security (SSL/SASL) configured
- [ ] Monitoring and metrics enabled
- [ ] Secrets in environment variables

---

## Troubleshooting Configuration

### Common Issues

**Issue**: Consumer kicked out of group

```yaml
# Solution: Increase max-poll-interval-ms
smart:
  kafka:
    consumer:
      max-poll-interval-ms: 600000  # 10 minutes
```

**Issue**: Messages not ordered

```yaml
# Solution: Enable strict ordering
smart:
  kafka:
    producer:
      enable-idempotence: true
      max-in-flight-requests-per-connection: 1
```

**Issue**: High latency

```yaml
# Solution: Reduce batching
smart:
  kafka:
    producer:
      linger-ms: 0                  # Send immediately
      batch-size: 0                 # Disable batching
```

**Issue**: Memory issues with large batches

```yaml
# Solution: Reduce batch size and poll records
smart:
  kafka:
    consumer:
      max-poll-records: 100         # Smaller batches
    batch:
      size: 50                      # Smaller processing batches
```

---

## See Also

- [Producer Guide](/kafka/guides/producer-guide) - Producer usage
- [Consumer Guide](/kafka/guides/consumer-guide) - Consumer usage
- [Batch Operations](/kafka/guides/batch-operations) - Batch configuration
- [Performance Tuning](/kafka/operations/performance-tuning) - Optimization
- [Configuration Reference](/kafka/reference/configuration-reference) - Complete reference

---

**Last Updated**: 2026-01-21
