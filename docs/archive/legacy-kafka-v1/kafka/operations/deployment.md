# Deployment Guide

Complete guide for deploying Kafka with SmartAdmin across development, test, and production environments.

## Overview

SmartAdmin's Kafka integration supports flexible deployment models:

- **Development**: Single-broker Docker Compose setup
- **Test**: Multi-broker Docker Compose with replication
- **Production**: Distributed Kafka cluster (Docker Swarm, Kubernetes, bare metal)

---

## Prerequisites

### Hardware Requirements

| Environment | Brokers | CPU | Memory | Storage |
|------------|---------|-----|--------|---------|
| Development | 1 | 2 cores | 2GB | 10GB |
| Test | 2-3 | 4 cores | 4GB | 50GB |
| Production | 3+ | 8+ cores | 16GB+ | 500GB+ SSD |

### Software Requirements

- Docker 20.10+
- Docker Compose 2.0+
- Java 21 (for SmartAdmin)
- Gradle 8.x (for building SmartAdmin)

### Network Requirements

**Ports**:
- `9092`: Kafka broker (PLAINTEXT)
- `9093`: Kafka broker (SSL)
- `9094`: Kafka broker (SASL)
- `2181`: ZooKeeper (deprecated in KRaft mode)
- `8080`: Kafka UI (optional)

---

## Development Deployment

### Single-Broker Docker Compose

**File**: `docker/docker-compose-kafka-dev.yml`

```yaml
version: '3.8'

services:
  # Kafka broker (KRaft mode - no ZooKeeper)
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: smart-admin-kafka
    hostname: kafka
    ports:
      - "9092:9092"
    environment:
      # KRaft mode configuration
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER

      # Listeners
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT

      # Cluster settings
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0

      # Log settings
      KAFKA_LOG_DIRS: /var/lib/kafka/data
      KAFKA_LOG_RETENTION_HOURS: 168
      KAFKA_LOG_SEGMENT_BYTES: 1073741824

      # Performance tuning
      KAFKA_NUM_NETWORK_THREADS: 3
      KAFKA_NUM_IO_THREADS: 8
      KAFKA_SOCKET_SEND_BUFFER_BYTES: 102400
      KAFKA_SOCKET_RECEIVE_BUFFER_BYTES: 102400
      KAFKA_SOCKET_REQUEST_MAX_BYTES: 104857600

    volumes:
      - kafka-data:/var/lib/kafka/data
    networks:
      - smart-admin-network
    healthcheck:
      test: ["CMD-SHELL", "kafka-broker-api-versions --bootstrap-server localhost:9092"]
      interval: 30s
      timeout: 10s
      retries: 3

  # Kafka UI (optional - for visualization)
  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: smart-admin-kafka-ui
    ports:
      - "8080:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: smart-admin-dev
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
    depends_on:
      - kafka
    networks:
      - smart-admin-network

volumes:
  kafka-data:

networks:
  smart-admin-network:
    driver: bridge
```

### Starting Development Environment

```bash
# Start Kafka
cd docker
docker-compose -f docker-compose-kafka-dev.yml up -d

# Verify Kafka is running
docker ps | grep kafka

# Check logs
docker logs smart-admin-kafka

# Access Kafka UI
open http://localhost:8080
```

### SmartAdmin Configuration (Development)

**File**: `sa-admin/src/main/resources/application-dev.yml`

```yaml
smart:
  kafka:
    bootstrap-servers: localhost:9092

    producer:
      acks: 1                       # Leader acknowledgment only (faster)
      retries: 1
      enable-idempotence: false     # Simpler for dev
      compression-type: none
      batch-size: 16384
      linger-ms: 0

    consumer:
      auto-offset-reset: latest     # Skip old messages in dev
      enable-auto-commit: false
      max-poll-records: 100
      session-timeout-ms: 30000
      heartbeat-interval-ms: 3000

    listener:
      concurrency: 1                # Single thread for debugging
      ack-mode: manual
      batch-listener: false
```

---

## Test Deployment

### Multi-Broker Docker Compose

**File**: `docker/docker-compose-kafka-test.yml`

```yaml
version: '3.8'

services:
  # Kafka Broker 1
  kafka-1:
    image: confluentinc/cp-kafka:7.5.0
    container_name: smart-admin-kafka-1
    hostname: kafka-1
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka-1:9093,2@kafka-2:9093,3@kafka-3:9093
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka-1:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 3
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 3
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 2
      KAFKA_LOG_DIRS: /var/lib/kafka/data
    volumes:
      - kafka-1-data:/var/lib/kafka/data
    networks:
      - smart-admin-network

  # Kafka Broker 2
  kafka-2:
    image: confluentinc/cp-kafka:7.5.0
    container_name: smart-admin-kafka-2
    hostname: kafka-2
    ports:
      - "9093:9092"
    environment:
      KAFKA_NODE_ID: 2
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka-1:9093,2@kafka-2:9093,3@kafka-3:9093
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka-2:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 3
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 3
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 2
      KAFKA_LOG_DIRS: /var/lib/kafka/data
    volumes:
      - kafka-2-data:/var/lib/kafka/data
    networks:
      - smart-admin-network

  # Kafka Broker 3
  kafka-3:
    image: confluentinc/cp-kafka:7.5.0
    container_name: smart-admin-kafka-3
    hostname: kafka-3
    ports:
      - "9094:9092"
    environment:
      KAFKA_NODE_ID: 3
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka-1:9093,2@kafka-2:9093,3@kafka-3:9093
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka-3:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 3
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 3
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 2
      KAFKA_LOG_DIRS: /var/lib/kafka/data
    volumes:
      - kafka-3-data:/var/lib/kafka/data
    networks:
      - smart-admin-network

  # Kafka UI
  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: smart-admin-kafka-ui
    ports:
      - "8080:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: smart-admin-test
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka-1:9092,kafka-2:9092,kafka-3:9092
    depends_on:
      - kafka-1
      - kafka-2
      - kafka-3
    networks:
      - smart-admin-network

volumes:
  kafka-1-data:
  kafka-2-data:
  kafka-3-data:

networks:
  smart-admin-network:
    driver: bridge
```

### SmartAdmin Configuration (Test)

```yaml
smart:
  kafka:
    bootstrap-servers: kafka-1:9092,kafka-2:9092,kafka-3:9092

    producer:
      acks: all                     # Wait for all replicas
      retries: 3
      enable-idempotence: true
      compression-type: lz4

    consumer:
      auto-offset-reset: earliest   # Process all messages
      max-poll-records: 200

    listener:
      concurrency: 2                # Test parallelism
```

---

## Topic Management

### Topic Creation Strategies

**Using Kafka CLI**:
```bash
# Basic topic
docker exec smart-admin-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --create \
  --topic smart-admin-order \
  --partitions 3 \
  --replication-factor 1

# Production-ready topic
docker exec smart-admin-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --create \
  --topic smart-admin-order \
  --partitions 6 \
  --replication-factor 3 \
  --config retention.ms=604800000 \
  --config segment.ms=86400000 \
  --config min.insync.replicas=2
```

### SmartAdmin Topic Naming Convention

From `KafkaConst.java`:
```java
public class KafkaConst {
    public static class Topic {
        public static final String ORDER = "smart-admin-order";
        public static final String USER_EVENT = "smart-admin-user-event";
        public static final String PAYMENT = "smart-admin-payment";

        // DLQ topics (auto-created by framework)
        // Pattern: {original-topic}-dlq
    }
}
```

### Topic Configuration Best Practices

```bash
# Create all SmartAdmin topics
#!/bin/bash

KAFKA_BROKER="localhost:9092"
PARTITIONS=6
REPLICATION_FACTOR=3
RETENTION_MS=604800000  # 7 days

create_topic() {
    local TOPIC=$1
    docker exec smart-admin-kafka kafka-topics \
        --bootstrap-server $KAFKA_BROKER \
        --create \
        --if-not-exists \
        --topic $TOPIC \
        --partitions $PARTITIONS \
        --replication-factor $REPLICATION_FACTOR \
        --config retention.ms=$RETENTION_MS \
        --config min.insync.replicas=2
}

# Business topics
create_topic "smart-admin-order"
create_topic "smart-admin-user-event"
create_topic "smart-admin-payment"

# DLQ topics (explicit creation optional - auto-created on first error)
create_topic "smart-admin-order-dlq"
create_topic "smart-admin-user-event-dlq"
create_topic "smart-admin-payment-dlq"
```

---

## Production Deployment

### Architecture Options

#### Option 1: Docker Swarm

```yaml
version: '3.8'

services:
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    deploy:
      replicas: 3
      placement:
        constraints:
          - node.role == worker
      resources:
        limits:
          cpus: '4'
          memory: 8G
        reservations:
          cpus: '2'
          memory: 4G
    environment:
      KAFKA_HEAP_OPTS: "-Xms4g -Xmx4g"
      # ... other configs
```

#### Option 2: Kubernetes (Strimzi Operator)

```yaml
apiVersion: kafka.strimzi.io/v1beta2
kind: Kafka
metadata:
  name: smart-admin-cluster
spec:
  kafka:
    version: 3.5.0
    replicas: 3
    listeners:
      - name: plain
        port: 9092
        type: internal
        tls: false
      - name: tls
        port: 9093
        type: internal
        tls: true
    config:
      offsets.topic.replication.factor: 3
      transaction.state.log.replication.factor: 3
      transaction.state.log.min.isr: 2
      default.replication.factor: 3
      min.insync.replicas: 2
    storage:
      type: jbod
      volumes:
        - id: 0
          type: persistent-claim
          size: 500Gi
          deleteClaim: false
    resources:
      requests:
        memory: 8Gi
        cpu: "2"
      limits:
        memory: 16Gi
        cpu: "4"
```

### Production Configuration

**File**: `application-prod.yml`

```yaml
smart:
  kafka:
    bootstrap-servers: ${KAFKA_BROKERS}  # kafka-1.prod:9092,kafka-2.prod:9092,kafka-3.prod:9092

    producer:
      acks: all
      retries: 3
      enable-idempotence: true
      max-in-flight-requests-per-connection: 1  # Strict ordering
      compression-type: lz4
      batch-size: 32768
      linger-ms: 5
      buffer-memory: 67108864  # 64MB
      request-timeout-ms: 30000

    consumer:
      auto-offset-reset: earliest
      enable-auto-commit: false
      max-poll-records: 500
      session-timeout-ms: 45000
      heartbeat-interval-ms: 3000
      max-poll-interval-ms: 300000  # 5 minutes
      fetch-min-size: 1
      fetch-max-wait: 500

    listener:
      concurrency: 5
      ack-mode: manual
      batch-listener: false
      missing-topics-fatal: false

    batch:
      enabled: true
      size: 100
      aggregate:
        enabled: true
        count: 50
        timeout-ms: 5000

    # Security (see Security Configuration section)
    ssl:
      enabled: true
      protocol: TLSv1.2
      truststore-location: ${KAFKA_TRUSTSTORE_PATH}
      truststore-password: ${KAFKA_TRUSTSTORE_PASSWORD}
      keystore-location: ${KAFKA_KEYSTORE_PATH}
      keystore-password: ${KAFKA_KEYSTORE_PASSWORD}
```

---

## SmartAdmin Integration

### Complete Docker Compose Integration

**File**: `docker/docker-compose.yml`

```yaml
version: '3.8'

services:
  # MySQL
  mysql:
    image: mysql:8.0
    container_name: smart-admin-mysql
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: smart_admin
    volumes:
      - mysql-data:/var/lib/mysql
    networks:
      - smart-admin-network

  # Redis
  redis:
    image: redis:7-alpine
    container_name: smart-admin-redis
    command: redis-server --requirepass ${REDIS_PASSWORD}
    volumes:
      - redis-data:/data
    networks:
      - smart-admin-network

  # Kafka
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: smart-admin-kafka
    hostname: kafka
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_LOG_DIRS: /var/lib/kafka/data
    volumes:
      - kafka-data:/var/lib/kafka/data
    networks:
      - smart-admin-network
    healthcheck:
      test: ["CMD-SHELL", "kafka-broker-api-versions --bootstrap-server localhost:9092"]
      interval: 30s
      timeout: 10s
      retries: 3

  # SmartAdmin Application
  smart-admin:
    build:
      context: ../smart-admin-api-java21-springboot3
      dockerfile: Dockerfile
    container_name: smart-admin-app
    ports:
      - "1024:1024"
    environment:
      SPRING_PROFILES_ACTIVE: ${ENV:-dev}
      MYSQL_HOST: mysql
      MYSQL_PORT: 3306
      MYSQL_DATABASE: smart_admin
      MYSQL_USERNAME: root
      MYSQL_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: ${REDIS_PASSWORD}
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
    depends_on:
      mysql:
        condition: service_started
      redis:
        condition: service_started
      kafka:
        condition: service_healthy
    networks:
      - smart-admin-network

  # Nginx
  nginx:
    image: nginx:alpine
    container_name: smart-admin-nginx
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ../smart-admin-web/dist:/usr/share/nginx/html:ro
    depends_on:
      - smart-admin
    networks:
      - smart-admin-network

volumes:
  mysql-data:
  redis-data:
  kafka-data:

networks:
  smart-admin-network:
    driver: bridge
```

### Environment Variables

**File**: `.env`

```bash
# Environment
ENV=dev

# MySQL
MYSQL_ROOT_PASSWORD=your_secure_password

# Redis
REDIS_PASSWORD=your_redis_password

# Kafka (Production)
KAFKA_BROKERS=kafka-1.prod:9092,kafka-2.prod:9092,kafka-3.prod:9092
KAFKA_TRUSTSTORE_PATH=/config/kafka.truststore.jks
KAFKA_TRUSTSTORE_PASSWORD=your_truststore_password
KAFKA_KEYSTORE_PATH=/config/kafka.keystore.jks
KAFKA_KEYSTORE_PASSWORD=your_keystore_password
```

---

## Scaling Strategies

### Horizontal Scaling (Brokers)

**Adding a new broker**:
```yaml
# Add to docker-compose.yml
kafka-4:
  image: confluentinc/cp-kafka:7.5.0
  container_name: smart-admin-kafka-4
  environment:
    KAFKA_NODE_ID: 4
    KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka-1:9093,2@kafka-2:9093,3@kafka-3:9093,4@kafka-4:9093
    # ... other configs
```

**Rebalance partitions**:
```bash
# Generate reassignment plan
kafka-reassign-partitions --bootstrap-server localhost:9092 \
  --topics-to-move-json-file topics.json \
  --broker-list "1,2,3,4" \
  --generate

# Execute reassignment
kafka-reassign-partitions --bootstrap-server localhost:9092 \
  --reassignment-json-file reassignment.json \
  --execute
```

### Partition Scaling

**Increase partitions** (cannot decrease):
```bash
kafka-topics --bootstrap-server localhost:9092 \
  --alter \
  --topic smart-admin-order \
  --partitions 12
```

**Important**: Update SmartAdmin consumer concurrency:
```yaml
smart:
  kafka:
    listener:
      concurrency: 12  # Match partition count
```

### Consumer Scaling

**Concurrency tuning**:
```yaml
smart:
  kafka:
    listener:
      concurrency: ${KAFKA_CONSUMER_CONCURRENCY:5}
```

**Rule**: `concurrency` ≤ partition count for optimal throughput.

---

## Security Configuration

### SSL/TLS Setup

**Generate certificates**:
```bash
# Create CA
openssl req -new -x509 -keyout ca-key -out ca-cert -days 3650

# Create broker keystore
keytool -keystore kafka.keystore.jks -alias kafka -validity 3650 -genkey -keyalg RSA

# Create CSR
keytool -keystore kafka.keystore.jks -alias kafka -certreq -file cert-file

# Sign with CA
openssl x509 -req -CA ca-cert -CAkey ca-key -in cert-file -out cert-signed -days 3650 -CAcreateserial

# Import CA and signed cert
keytool -keystore kafka.keystore.jks -alias CARoot -import -file ca-cert
keytool -keystore kafka.keystore.jks -alias kafka -import -file cert-signed

# Create truststore
keytool -keystore kafka.truststore.jks -alias CARoot -import -file ca-cert
```

**Kafka SSL config**:
```yaml
# Docker Compose
kafka:
  environment:
    KAFKA_SSL_KEYSTORE_LOCATION: /etc/kafka/secrets/kafka.keystore.jks
    KAFKA_SSL_KEYSTORE_PASSWORD: ${KEYSTORE_PASSWORD}
    KAFKA_SSL_KEY_PASSWORD: ${KEY_PASSWORD}
    KAFKA_SSL_TRUSTSTORE_LOCATION: /etc/kafka/secrets/kafka.truststore.jks
    KAFKA_SSL_TRUSTSTORE_PASSWORD: ${TRUSTSTORE_PASSWORD}
  volumes:
    - ./secrets:/etc/kafka/secrets:ro
```

### SASL Authentication

**Kafka SASL config**:
```yaml
kafka:
  environment:
    KAFKA_SASL_ENABLED_MECHANISMS: PLAIN
    KAFKA_SASL_MECHANISM_INTER_BROKER_PROTOCOL: PLAIN
    KAFKA_LISTENER_NAME_SASL_PLAIN_PLAIN_SASL_JAAS_CONFIG: |
      org.apache.kafka.common.security.plain.PlainLoginModule required
      username="admin"
      password="${KAFKA_ADMIN_PASSWORD}"
      user_admin="${KAFKA_ADMIN_PASSWORD}"
      user_producer="${KAFKA_PRODUCER_PASSWORD}"
      user_consumer="${KAFKA_CONSUMER_PASSWORD}";
```

**SmartAdmin SASL config**:
```yaml
smart:
  kafka:
    bootstrap-servers: ${KAFKA_BROKERS}
    sasl:
      enabled: true
      mechanism: PLAIN
      jaas-config: |
        org.apache.kafka.common.security.plain.PlainLoginModule required
        username="${KAFKA_USERNAME}"
        password="${KAFKA_PASSWORD}";
    security:
      protocol: SASL_SSL
```

---

## Deployment Checklist

### Pre-Deployment

- [ ] Hardware provisioned per environment requirements
- [ ] Network ports opened and firewalls configured
- [ ] Docker and Docker Compose installed
- [ ] SSL certificates generated (if using SSL/TLS)
- [ ] Environment variables configured
- [ ] Topic creation scripts prepared

### Deployment

- [ ] Kafka cluster deployed (3+ brokers for production)
- [ ] Kafka cluster health verified
- [ ] Topics created with proper partitions/replication
- [ ] SmartAdmin application deployed
- [ ] Application connected to Kafka successfully
- [ ] Producer and consumer tests passed

### Post-Deployment

- [ ] Monitoring configured (Prometheus, Grafana)
- [ ] Alerts configured for critical metrics
- [ ] Backup strategy in place
- [ ] Runbooks documented
- [ ] Team trained on operations

---

## Troubleshooting

### Kafka Won't Start

```bash
# Check logs
docker logs smart-admin-kafka

# Common issues:
# 1. Port conflict
lsof -i :9092

# 2. Insufficient memory
docker stats smart-admin-kafka

# 3. Data directory corruption
docker exec smart-admin-kafka ls -la /var/lib/kafka/data
```

### Cannot Connect from SmartAdmin

```bash
# Test connectivity
docker exec smart-admin-app nc -zv kafka 9092

# Check DNS resolution
docker exec smart-admin-app nslookup kafka

# Verify bootstrap servers
docker exec smart-admin-app env | grep KAFKA
```

### Topic Creation Fails

```bash
# Check broker status
docker exec smart-admin-kafka kafka-broker-api-versions --bootstrap-server localhost:9092

# List existing topics
docker exec smart-admin-kafka kafka-topics --bootstrap-server localhost:9092 --list

# Describe topic
docker exec smart-admin-kafka kafka-topics --bootstrap-server localhost:9092 --describe --topic smart-admin-order
```

---

## See Also

- [Configuration Guide](/kafka/guides/configuration) - Configuration reference
- [Monitoring Guide](/kafka/operations/monitoring) - Monitoring setup
- [Health Checks](/kafka/operations/health-checks) - Health check configuration
- [Troubleshooting](/kafka/troubleshooting/common-issues) - Common issues
- [Docker Compose Example](/kafka/examples/docker-compose-example) - Complete example

---

**Last Updated**: 2026-01-21
