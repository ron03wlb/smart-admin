# Health Checks

Complete guide to configuring health checks for SmartAdmin's Kafka integration, including readiness/liveness probes and custom indicators.

## Overview

Health checks provide:
- **Service readiness** - Is Kafka available and ready to handle requests?
- **Liveness monitoring** - Is the application healthy and responsive?
- **Dependency validation** - Are all Kafka dependencies (brokers, topics) accessible?
- **Automated recovery** - Restart unhealthy containers automatically

**Health Check Types**:
- **Liveness**: Is the application running? (restart if failing)
- **Readiness**: Can the application serve traffic? (remove from load balancer if failing)
- **Startup**: Has the application finished initialization? (wait before liveness checks)

---

## Spring Boot Actuator Health

### Basic Configuration

**File**: `sa-admin/src/main/resources/application.yml`

```yaml
management:
  endpoint:
    health:
      enabled: true
      show-details: always          # Show component details
      show-components: always        # Show individual components
      probes:
        enabled: true                # Enable liveness/readiness

  health:
    kafka:
      enabled: true                  # Enable Kafka health indicator
    livenessState:
      enabled: true
    readinessState:
      enabled: true

  endpoints:
    web:
      exposure:
        include: health,info
```

### Health Endpoint

```bash
# Check overall health
curl http://localhost:1024/actuator/health

# Sample response
{
  "status": "UP",
  "components": {
    "kafka": {
      "status": "UP",
      "details": {
        "clusterId": "kafka-cluster-1",
        "nodes": 3
      }
    },
    "diskSpace": {
      "status": "UP"
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

### Liveness and Readiness Probes

```bash
# Liveness probe (is application alive?)
curl http://localhost:1024/actuator/health/liveness

# Response:
# {"status":"UP"} - Application is alive
# {"status":"DOWN"} - Application should be restarted

# Readiness probe (can application handle traffic?)
curl http://localhost:1024/actuator/health/readiness

# Response:
# {"status":"UP"} - Ready to receive traffic
# {"status":"DOWN"} - Remove from load balancer
```

---

## Custom Kafka Health Indicator

### Implementation

**File**: `sa-common/mq/kafka/health/KafkaHealthIndicator.java`

```java
package net.lab1024.sa.common.mq.kafka.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.common.Node;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

@Component("kafkaHealthIndicator")
@RequiredArgsConstructor
@Slf4j
public class KafkaHealthIndicator implements HealthIndicator {

    private final KafkaAdmin kafkaAdmin;
    private static final int TIMEOUT_SECONDS = 5;

    @Override
    public Health health() {
        try {
            AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties());

            try {
                // Check cluster connectivity
                DescribeClusterResult clusterResult = adminClient.describeCluster();

                // Get cluster ID
                String clusterId = clusterResult.clusterId()
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

                // Get nodes
                Collection<Node> nodes = clusterResult.nodes()
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

                // Get controller
                Node controller = clusterResult.controller()
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

                // Build health response
                return Health.up()
                    .withDetail("clusterId", clusterId)
                    .withDetail("nodeCount", nodes.size())
                    .withDetail("controller", controller.idString())
                    .withDetail("bootstrapServers", kafkaAdmin.getConfigurationProperties().get("bootstrap.servers"))
                    .build();

            } finally {
                adminClient.close();
            }

        } catch (Exception e) {
            log.error("Kafka health check failed", e);
            return Health.down()
                .withException(e)
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

### Enhanced Health Check with Topic Validation

```java
@Component("kafkaEnhancedHealthIndicator")
@RequiredArgsConstructor
@Slf4j
public class KafkaEnhancedHealthIndicator implements HealthIndicator {

    private final KafkaAdmin kafkaAdmin;
    private final List<String> requiredTopics = List.of(
        "smart-admin-order",
        "smart-admin-user-event",
        "smart-admin-payment"
    );

    @Override
    public Health health() {
        try {
            AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties());

            try {
                // Check cluster
                DescribeClusterResult clusterResult = adminClient.describeCluster();
                String clusterId = clusterResult.clusterId().get(5, TimeUnit.SECONDS);
                Collection<Node> nodes = clusterResult.nodes().get(5, TimeUnit.SECONDS);

                // Check topics
                Set<String> topicNames = adminClient.listTopics().names().get(5, TimeUnit.SECONDS);
                List<String> missingTopics = requiredTopics.stream()
                    .filter(topic -> !topicNames.contains(topic))
                    .toList();

                // Determine health status
                if (!missingTopics.isEmpty()) {
                    return Health.down()
                        .withDetail("clusterId", clusterId)
                        .withDetail("nodeCount", nodes.size())
                        .withDetail("missingTopics", missingTopics)
                        .withDetail("reason", "Required topics are missing")
                        .build();
                }

                return Health.up()
                    .withDetail("clusterId", clusterId)
                    .withDetail("nodeCount", nodes.size())
                    .withDetail("topicCount", topicNames.size())
                    .withDetail("requiredTopicsPresent", true)
                    .build();

            } finally {
                adminClient.close();
            }

        } catch (Exception e) {
            log.error("Kafka health check failed", e);
            return Health.down()
                .withException(e)
                .build();
        }
    }
}
```

---

## Docker Health Checks

### Docker Compose Configuration

**File**: `docker/docker-compose.yml`

```yaml
version: '3.8'

services:
  # Kafka broker
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: smart-admin-kafka
    healthcheck:
      test: ["CMD-SHELL", "kafka-broker-api-versions --bootstrap-server localhost:9092 || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  # SmartAdmin application
  smart-admin:
    image: smart-admin:latest
    container_name: smart-admin-app
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:1024/actuator/health || exit 1"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 120s    # Wait for Kafka connection
    depends_on:
      kafka:
        condition: service_healthy
```

### Health Check Script

**File**: `docker/scripts/healthcheck.sh`

```bash
#!/bin/bash
set -e

# Check application health
HEALTH_URL="http://localhost:1024/actuator/health"

# Wait for service to be ready
MAX_RETRIES=30
RETRY_INTERVAL=2

for i in $(seq 1 $MAX_RETRIES); do
    if curl -f -s "$HEALTH_URL" > /dev/null 2>&1; then
        echo "Health check passed"
        exit 0
    fi

    echo "Health check attempt $i/$MAX_RETRIES failed, retrying in ${RETRY_INTERVAL}s..."
    sleep $RETRY_INTERVAL
done

echo "Health check failed after $MAX_RETRIES attempts"
exit 1
```

---

## Kubernetes Probes

### Deployment Configuration

**File**: `k8s/smart-admin-deployment.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: smart-admin
spec:
  replicas: 3
  selector:
    matchLabels:
      app: smart-admin
  template:
    metadata:
      labels:
        app: smart-admin
    spec:
      containers:
        - name: smart-admin
          image: smart-admin:latest
          ports:
            - containerPort: 1024

          # Startup probe (wait for initial startup)
          startupProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 1024
            initialDelaySeconds: 60
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 30    # 5 minutes max startup time

          # Liveness probe (restart if failing)
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 1024
            initialDelaySeconds: 10
            periodSeconds: 30
            timeoutSeconds: 5
            failureThreshold: 3

          # Readiness probe (remove from service if failing)
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 1024
            initialDelaySeconds: 10
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3

          resources:
            requests:
              memory: "512Mi"
              cpu: "500m"
            limits:
              memory: "2Gi"
              cpu: "2"
```

---

## Health Check Patterns

### Pattern 1: Fail Fast

**Use case**: Development, quick feedback

```yaml
management:
  health:
    kafka:
      enabled: true
      timeout: 5s           # Fast timeout
```

**Behavior**: Quickly marks service as DOWN if Kafka unreachable

---

### Pattern 2: Graceful Degradation

**Use case**: Production, high availability

```yaml
management:
  health:
    kafka:
      enabled: true
      timeout: 30s          # Longer timeout
```

**Custom implementation**:
```java
@Override
public Health health() {
    try {
        // Try to connect
        checkKafka();
        return Health.up().build();
    } catch (TimeoutException e) {
        // Degraded state - warn but don't fail
        return Health.status("DEGRADED")
            .withDetail("warning", "Kafka slow to respond")
            .build();
    } catch (Exception e) {
        // Critical failure
        return Health.down().withException(e).build();
    }
}
```

---

### Pattern 3: Conditional Health

**Use case**: Optional Kafka dependency

```java
@Component
@ConditionalOnProperty(name = "smart.kafka.health.enabled", havingValue = "true")
public class ConditionalKafkaHealthIndicator implements HealthIndicator {
    // Only register health indicator if Kafka health checks are enabled
}
```

**Configuration**:
```yaml
smart:
  kafka:
    health:
      enabled: ${KAFKA_HEALTH_ENABLED:true}
```

---

## Load Balancer Integration

### Nginx Health Check

**File**: `docker/nginx/nginx.conf`

```nginx
upstream smart_admin_backend {
    # Health check interval: 10s
    server smart-admin-1:1024 max_fails=3 fail_timeout=30s;
    server smart-admin-2:1024 max_fails=3 fail_timeout=30s;
    server smart-admin-3:1024 max_fails=3 fail_timeout=30s;
}

server {
    listen 80;

    location /actuator/health {
        proxy_pass http://smart_admin_backend;
        proxy_connect_timeout 5s;
        proxy_read_timeout 10s;

        # Health check endpoint
        access_log off;
    }

    location / {
        proxy_pass http://smart_admin_backend;

        # Only route to healthy backends
        proxy_next_upstream error timeout http_502 http_503 http_504;
        proxy_next_upstream_tries 3;
    }
}
```

---

## Monitoring Health Checks

### Prometheus Metrics

**Expose health check status**:
```yaml
management:
  metrics:
    export:
      prometheus:
        enabled: true
    enable:
      health: true
```

**Query health status**:
```promql
# Check if Kafka health is UP
up{job="smart-admin",endpoint="kafka"}

# Alert if Kafka health is DOWN
kafka_health_status{status="DOWN"} > 0
```

### Grafana Dashboard Panel

```json
{
  "id": 1,
  "title": "Kafka Health Status",
  "type": "stat",
  "targets": [
    {
      "expr": "health_kafka_status",
      "legendFormat": "Kafka Health"
    }
  ],
  "fieldConfig": {
    "defaults": {
      "thresholds": {
        "mode": "absolute",
        "steps": [
          {"value": 0, "color": "red"},
          {"value": 1, "color": "green"}
        ]
      },
      "mappings": [
        {"type": "value", "value": "0", "text": "DOWN"},
        {"type": "value", "value": "1", "text": "UP"}
      ]
    }
  }
}
```

---

## Troubleshooting Health Checks

### Health Check Always Fails

**Symptom**: `/actuator/health` returns `{"status":"DOWN"}`

**Diagnosis**:
```bash
# Check detailed health response
curl http://localhost:1024/actuator/health | jq .

# Check specific component
curl http://localhost:1024/actuator/health/kafka | jq .

# Check application logs
docker logs smart-admin-app | grep -i "health"
```

**Common causes**:
1. Kafka not reachable
2. Timeout too short
3. Required topics missing
4. Authentication failure

---

### Health Check Times Out

**Symptom**: Health endpoint takes > 5s to respond

**Solution**:
```yaml
management:
  health:
    kafka:
      timeout: 10s        # Increase timeout
```

**Or use async health checks**:
```java
@Component
public class AsyncKafkaHealthIndicator implements HealthIndicator {

    private volatile Health cachedHealth = Health.up().build();

    @Scheduled(fixedRate = 30000)  // Update every 30s
    public void updateHealth() {
        cachedHealth = performHealthCheck();
    }

    @Override
    public Health health() {
        return cachedHealth;  // Return cached status instantly
    }
}
```

---

### False Positive Health Checks

**Symptom**: Health reports UP but Kafka isn't working

**Solution**: Implement comprehensive checks
```java
@Override
public Health health() {
    Health.Builder builder = Health.up();

    // 1. Check cluster connectivity
    checkClusterConnectivity(builder);

    // 2. Check topic accessibility
    checkTopics(builder);

    // 3. Check producer functionality
    checkProducerHealth(builder);

    // 4. Check consumer group status
    checkConsumerGroups(builder);

    return builder.build();
}
```

---

## Best Practices

### 1. Configure Appropriate Timeouts

```yaml
# Development (fast feedback)
management:
  health:
    kafka:
      timeout: 5s

# Production (avoid false negatives)
management:
  health:
    kafka:
      timeout: 30s
```

### 2. Use Startup Probes in Kubernetes

```yaml
startupProbe:
  httpGet:
    path: /actuator/health/liveness
  failureThreshold: 30      # 5 minutes max startup
  periodSeconds: 10
```

### 3. Separate Liveness and Readiness

**Liveness**: Only check if application is alive (not Kafka)
**Readiness**: Check if Kafka is ready AND application is ready

```java
// Liveness: Simple ping
@Component("livenessIndicator")
public class LivenessIndicator implements HealthIndicator {
    @Override
    public Health health() {
        return Health.up().build();  // Always UP if app is running
    }
}

// Readiness: Check Kafka
@Component("readinessIndicator")
public class ReadinessIndicator implements HealthIndicator {
    @Override
    public Health health() {
        // Check Kafka connectivity
        return checkKafka();
    }
}
```

### 4. Cache Health Status for Performance

```java
@Scheduled(fixedRate = 30000)
public void updateHealthCache() {
    this.cachedHealth = performExpensiveHealthCheck();
}
```

---

## See Also

- [Monitoring Guide](/kafka/operations/monitoring) - Monitoring setup
- [Deployment Guide](/kafka/operations/deployment) - Deployment configuration
- [Troubleshooting](/kafka/troubleshooting/common-issues) - Common health check issues
- [Configuration](/kafka/guides/configuration) - Health check configuration

---

**Last Updated**: 2026-01-21
