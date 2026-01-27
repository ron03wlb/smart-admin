# Docker Compose Setup

Complete production-ready Docker Compose configuration for SmartAdmin with Kafka.

## Overview

This setup includes:
- **Kafka Broker** (KRaft mode, no ZooKeeper)
- **Kafka UI** (Web interface)
- **Prometheus** (Metrics collection)
- **Grafana** (Metrics visualization)
- **SmartAdmin** (Application)
- **MySQL** (Database)
- **Redis** (Cache)

**Use case**: Local development and testing with full observability

---

## Complete Docker Compose File

**File**: `docker/docker-compose-kafka-full.yml`

```yaml
version: '3.8'

services:
  # ===================
  # Kafka Broker
  # ===================
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: smart-admin-kafka
    hostname: kafka
    ports:
      - "9092:9092"      # External access
      - "9101:9101"      # JMX metrics
    environment:
      # KRaft configuration (no ZooKeeper)
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
      KAFKA_LOG_RETENTION_HOURS: 168  # 7 days

      # JMX for monitoring
      KAFKA_JMX_PORT: 9101
      KAFKA_JMX_HOSTNAME: localhost

    volumes:
      - kafka-data:/var/lib/kafka/data
    healthcheck:
      test: ["CMD-SHELL", "kafka-broker-api-versions --bootstrap-server localhost:9092 || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    networks:
      - smart-admin-network

  # ===================
  # Kafka UI
  # ===================
  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: smart-admin-kafka-ui
    ports:
      - "8080:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: smart-admin-cluster
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
      KAFKA_CLUSTERS_0_METRICS_PORT: 9101
      DYNAMIC_CONFIG_ENABLED: 'true'
    depends_on:
      kafka:
        condition: service_healthy
    networks:
      - smart-admin-network

  # ===================
  # Prometheus
  # ===================
  prometheus:
    image: prom/prometheus:v2.45.0
    container_name: smart-admin-prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/usr/share/prometheus/console_libraries'
      - '--web.console.templates=/usr/share/prometheus/consoles'
    networks:
      - smart-admin-network

  # ===================
  # Grafana
  # ===================
  grafana:
    image: grafana/grafana:10.0.0
    container_name: smart-admin-grafana
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_USER: admin
      GF_SECURITY_ADMIN_PASSWORD: admin123
      GF_INSTALL_PLUGINS: grafana-piechart-panel
    volumes:
      - ./grafana/provisioning:/etc/grafana/provisioning
      - ./grafana/dashboards:/var/lib/grafana/dashboards
      - grafana-data:/var/lib/grafana
    depends_on:
      - prometheus
    networks:
      - smart-admin-network

  # ===================
  # MySQL
  # ===================
  mysql:
    image: mysql:8.0
    container_name: smart-admin-mysql
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: sa123456
      MYSQL_DATABASE: smart-admin
      MYSQL_USER: sa_user
      MYSQL_PASSWORD: sa123456
    volumes:
      - mysql-data:/var/lib/mysql
      - ./mysql/init:/docker-entrypoint-initdb.d
    command: --default-authentication-plugin=mysql_native_password
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 3
    networks:
      - smart-admin-network

  # ===================
  # Redis
  # ===================
  redis:
    image: redis:7.0-alpine
    container_name: smart-admin-redis
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    command: redis-server --appendonly yes
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 3
    networks:
      - smart-admin-network

  # ===================
  # SmartAdmin Application
  # ===================
  smart-admin:
    build:
      context: ../smart-admin-api-java21-springboot3
      dockerfile: Dockerfile
    container_name: smart-admin-app
    ports:
      - "1024:1024"
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/smart-admin?useUnicode=true&characterEncoding=utf-8&useSSL=false
      SPRING_DATASOURCE_USERNAME: sa_user
      SPRING_DATASOURCE_PASSWORD: sa123456
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
      SMART_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      SMART_KAFKA_ENABLED: true
    depends_on:
      kafka:
        condition: service_healthy
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:1024/actuator/health || exit 1"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 120s
    networks:
      - smart-admin-network

volumes:
  kafka-data:
  mysql-data:
  redis-data:
  prometheus-data:
  grafana-data:

networks:
  smart-admin-network:
    driver: bridge
```

---

## Prometheus Configuration

**File**: `docker/prometheus/prometheus.yml`

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  # SmartAdmin application metrics
  - job_name: 'smart-admin'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['smart-admin:1024']

  # Kafka JMX metrics
  - job_name: 'kafka'
    static_configs:
      - targets: ['kafka:9101']

  # Prometheus self-monitoring
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
```

---

## Grafana Dashboard Configuration

**File**: `docker/grafana/provisioning/dashboards/dashboard.yml`

```yaml
apiVersion: 1

providers:
  - name: 'Kafka Dashboards'
    orgId: 1
    folder: ''
    type: file
    disableDeletion: false
    editable: true
    options:
      path: /var/lib/grafana/dashboards
```

**File**: `docker/grafana/provisioning/datasources/prometheus.yml`

```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
```

---

## SmartAdmin Dockerfile

**File**: `smart-admin-api-java21-springboot3/Dockerfile`

```dockerfile
FROM eclipse-temurin:21-jre-alpine

# Set working directory
WORKDIR /app

# Copy application JAR
COPY sa-admin/build/libs/sa-admin-*.jar app.jar

# Expose port
EXPOSE 1024

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:1024/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
```

---

## Usage Instructions

### 1. Build Application

```bash
cd smart-admin-api-java21-springboot3
./gradlew clean build -x test
```

### 2. Start All Services

```bash
cd docker
docker-compose -f docker-compose-kafka-full.yml up -d
```

### 3. Check Service Status

```bash
docker-compose -f docker-compose-kafka-full.yml ps
```

**Expected output**:
```
NAME                     STATUS         PORTS
smart-admin-kafka        Up (healthy)   0.0.0.0:9092->9092/tcp
smart-admin-kafka-ui     Up             0.0.0.0:8080->8080/tcp
smart-admin-prometheus   Up             0.0.0.0:9090->9090/tcp
smart-admin-grafana      Up             0.0.0.0:3000->3000/tcp
smart-admin-mysql        Up (healthy)   0.0.0.0:3306->3306/tcp
smart-admin-redis        Up (healthy)   0.0.0.0:6379->6379/tcp
smart-admin-app          Up (healthy)   0.0.0.0:1024->1024/tcp
```

### 4. Verify Kafka

```bash
docker exec smart-admin-kafka kafka-topics \
  --bootstrap-server localhost:9092 --list
```

### 5. Create Test Topic

```bash
docker exec smart-admin-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --create --topic smart-admin-test \
  --partitions 3 --replication-factor 1
```

---

## Access Services

| Service | URL | Credentials |
|---------|-----|-------------|
| **SmartAdmin** | http://localhost:1024/swagger-ui.html | - |
| **Kafka UI** | http://localhost:8080 | - |
| **Prometheus** | http://localhost:9090 | - |
| **Grafana** | http://localhost:3000 | admin / admin123 |
| **MySQL** | localhost:3306 | sa_user / sa123456 |
| **Redis** | localhost:6379 | - |

---

## Testing the Setup

### Send Test Message

```bash
curl -X POST http://localhost:1024/business/sample/kafka/basic/send \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "12345",
    "message": "Docker Compose test message",
    "type": "INFO"
  }'
```

### View in Kafka UI

1. Open http://localhost:8080
2. Navigate to **Topics** → **smart-admin-notification**
3. Click **Messages** tab
4. See your test message

### View Metrics in Grafana

1. Open http://localhost:3000
2. Login: admin / admin123
3. Navigate to **Dashboards** → **Kafka Overview**
4. View real-time metrics

---

## Monitoring Commands

### View Logs

```bash
# All services
docker-compose -f docker-compose-kafka-full.yml logs -f

# Specific service
docker-compose -f docker-compose-kafka-full.yml logs -f smart-admin

# Kafka only
docker logs -f smart-admin-kafka
```

### Check Kafka Topics

```bash
docker exec smart-admin-kafka kafka-topics \
  --bootstrap-server localhost:9092 --list
```

### Check Consumer Groups

```bash
docker exec smart-admin-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 --list
```

### Describe Consumer Group

```bash
docker exec smart-admin-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group notification-processor
```

---

## Troubleshooting

### Service Won't Start

**Check logs**:
```bash
docker-compose -f docker-compose-kafka-full.yml logs kafka
```

**Common issues**:
- Port already in use: Change port in `docker-compose.yml`
- Insufficient memory: Allocate more RAM to Docker
- Network conflicts: Check `docker network ls`

---

### Kafka Connection Errors

**Verify Kafka is healthy**:
```bash
docker exec smart-admin-kafka kafka-broker-api-versions \
  --bootstrap-server localhost:9092
```

**Check SmartAdmin can reach Kafka**:
```bash
docker exec smart-admin-app curl -v telnet://kafka:9092
```

---

### Database Connection Errors

**Check MySQL is running**:
```bash
docker exec smart-admin-mysql mysqladmin ping -h localhost -u sa_user -psa123456
```

**Verify database exists**:
```bash
docker exec smart-admin-mysql mysql -u sa_user -psa123456 -e "SHOW DATABASES;"
```

---

## Shutdown and Cleanup

### Stop Services

```bash
docker-compose -f docker-compose-kafka-full.yml down
```

### Stop and Remove Volumes (WARNING: Deletes all data)

```bash
docker-compose -f docker-compose-kafka-full.yml down -v
```

### Remove Images

```bash
docker-compose -f docker-compose-kafka-full.yml down --rmi all
```

---

## Production Considerations

### 1. Multi-Broker Kafka Cluster

Replace single `kafka` service with multiple brokers:

```yaml
services:
  kafka-1:
    # ... similar config with KAFKA_NODE_ID: 1
  kafka-2:
    # ... similar config with KAFKA_NODE_ID: 2
  kafka-3:
    # ... similar config with KAFKA_NODE_ID: 3
```

### 2. Resource Limits

Add resource constraints:

```yaml
services:
  kafka:
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 4G
        reservations:
          cpus: '1.0'
          memory: 2G
```

### 3. Persistent Volumes

Use named volumes for data persistence:

```yaml
volumes:
  kafka-data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /data/kafka
```

### 4. Security

Add authentication and SSL:

```yaml
environment:
  KAFKA_SECURITY_PROTOCOL: SASL_SSL
  KAFKA_SASL_MECHANISM: SCRAM-SHA-512
  # ... additional security config
```

---

## See Also

- [Deployment Guide](/kafka/operations/deployment) - Production deployment
- [Monitoring Guide](/kafka/operations/monitoring) - Metrics and dashboards
- [Health Checks](/kafka/operations/health-checks) - Health check configuration

---

**Last Updated**: 2026-01-22
