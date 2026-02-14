# Monitoring and Metrics

Comprehensive guide to monitoring SmartAdmin's Kafka integration with Prometheus, Grafana, and custom metrics.

## Overview

Effective monitoring provides:
- **Real-time visibility** into producer/consumer health
- **Performance insights** (throughput, latency, consumer lag)
- **Proactive alerting** before issues impact users
- **Capacity planning** data for scaling decisions

**Monitoring Stack**:
- Spring Boot Actuator - Metrics exposure
- Micrometer - Metrics collection
- Prometheus - Time-series database
- Grafana - Visualization and dashboards
- Alertmanager - Alert routing and notifications

---

## Key Metrics

### Producer Metrics

| Metric | Description | Target | Alert Threshold |
|--------|-------------|--------|-----------------|
| `kafka.producer.record-send-total` | Total records sent | Increasing | - |
| `kafka.producer.record-error-total` | Failed sends | 0 | > 10/min |
| `kafka.producer.record-send-rate` | Messages/sec | Depends on load | - |
| `kafka.producer.request-latency-avg` | Average send latency | < 10ms | > 50ms |
| `kafka.producer.request-latency-max` | Max send latency | < 50ms | > 200ms |
| `kafka.producer.batch-size-avg` | Average batch size | Depends on config | - |
| `kafka.producer.buffer-available-bytes` | Buffer free space | > 50% | < 10% |

### Consumer Metrics

| Metric | Description | Target | Alert Threshold |
|--------|-------------|--------|-----------------|
| `kafka.consumer.records-consumed-total` | Total records consumed | Increasing | - |
| `kafka.consumer.records-lag` | Consumer lag | < 100 | > 1000 |
| `kafka.consumer.records-lag-max` | Max lag across partitions | < 100 | > 1000 |
| `kafka.consumer.fetch-latency-avg` | Average fetch latency | < 5ms | > 50ms |
| `kafka.consumer.commit-latency-avg` | Offset commit latency | < 10ms | > 100ms |
| `kafka.consumer.coordinator-sync-time-avg` | Rebalance time | < 1s | > 10s |

### Broker Metrics

| Metric | Description | Target | Alert Threshold |
|--------|-------------|--------|-----------------|
| `kafka_server_replicamanager_leadercount` | Leader partitions | Balanced | Imbalanced |
| `kafka_server_replicamanager_underreplicatedpartitions` | Under-replicated partitions | 0 | > 0 |
| `kafka_controller_activecontrollercount` | Active controllers | 1 | != 1 |
| `kafka_server_brokertopicmetrics_messagesinpersec` | Messages in/sec | - | Sudden drop |
| `kafka_network_requestmetrics_totaltimems` | Request processing time | < 10ms | > 100ms |

### Application Metrics (SmartAdmin)

| Metric | Description | Target | Alert Threshold |
|--------|-------------|--------|-----------------|
| `kafka.dlq.messages.total` | DLQ message count | Low | > 100 |
| `kafka.batch.processing.duration` | Batch processing time | < 1s | > 5s |
| `kafka.consumer.errors.total` | Consumer errors | 0 | > 10/min |
| `kafka.messages.sent.total` | Application send count | - | Sudden drop |
| `kafka.messages.received.total` | Application receive count | - | Sudden drop |

---

## Prometheus Integration

### Step 1: Add Dependencies

**File**: `sa-admin/build.gradle`

```gradle
dependencies {
    // Prometheus metrics
    implementation 'io.micrometer:micrometer-registry-prometheus'

    // Spring Boot Actuator
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
}
```

### Step 2: Configure Actuator

**File**: `sa-admin/src/main/resources/application.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator

  metrics:
    export:
      prometheus:
        enabled: true
        step: 1m                    # Scrape interval

    tags:
      application: smart-admin
      environment: ${spring.profiles.active}
      instance: ${HOSTNAME:localhost}

    distribution:
      percentiles-histogram:
        http.server.requests: true
        kafka.consumer.fetch.latency: true
        kafka.producer.request.latency: true

  health:
    kafka:
      enabled: true

  endpoint:
    health:
      show-details: always
    metrics:
      enabled: true
    prometheus:
      enabled: true
```

### Step 3: Verify Metrics Endpoint

```bash
# Check available metrics
curl http://localhost:1024/actuator/metrics

# Check Prometheus format
curl http://localhost:1024/actuator/prometheus

# Check Kafka-specific metrics
curl http://localhost:1024/actuator/metrics/kafka.producer.record-send-total
```

**Sample output**:
```json
{
  "name": "kafka.producer.record-send-total",
  "description": "The total number of records sent",
  "baseUnit": "records",
  "measurements": [
    {
      "statistic": "COUNT",
      "value": 12345.0
    }
  ],
  "availableTags": [
    {
      "tag": "kafka.version",
      "values": ["3.5.0"]
    }
  ]
}
```

---

## Prometheus Configuration

### Prometheus Setup

**File**: `docker/prometheus/prometheus.yml`

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    cluster: 'smart-admin'
    environment: 'production'

scrape_configs:
  # SmartAdmin application metrics
  - job_name: 'smart-admin'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['smart-admin:1024']
        labels:
          service: 'smart-admin-backend'

  # Kafka broker metrics (JMX Exporter)
  - job_name: 'kafka-broker'
    static_configs:
      - targets: ['kafka-1:7071', 'kafka-2:7071', 'kafka-3:7071']
        labels:
          service: 'kafka'

  # Kafka UI metrics
  - job_name: 'kafka-ui'
    static_configs:
      - targets: ['kafka-ui:8080']
        labels:
          service: 'kafka-ui'
```

### Docker Compose Integration

**File**: `docker/docker-compose.yml`

```yaml
version: '3.8'

services:
  # Prometheus
  prometheus:
    image: prom/prometheus:latest
    container_name: smart-admin-prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--storage.tsdb.retention.time=30d'
    networks:
      - smart-admin-network
    restart: unless-stopped

  # Grafana
  grafana:
    image: grafana/grafana:latest
    container_name: smart-admin-grafana
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_USER: admin
      GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_ADMIN_PASSWORD}
      GF_INSTALL_PLUGINS: grafana-piechart-panel
    volumes:
      - ./grafana/provisioning:/etc/grafana/provisioning:ro
      - ./grafana/dashboards:/var/lib/grafana/dashboards:ro
      - grafana-data:/var/lib/grafana
    depends_on:
      - prometheus
    networks:
      - smart-admin-network
    restart: unless-stopped

  # Alertmanager
  alertmanager:
    image: prom/alertmanager:latest
    container_name: smart-admin-alertmanager
    ports:
      - "9093:9093"
    volumes:
      - ./alertmanager/alertmanager.yml:/etc/alertmanager/alertmanager.yml:ro
    networks:
      - smart-admin-network
    restart: unless-stopped

volumes:
  prometheus-data:
  grafana-data:

networks:
  smart-admin-network:
    driver: bridge
```

---

## Custom Metrics

### KafkaMetrics Component

**File**: `sa-common/mq/kafka/metrics/KafkaMetrics.java`

```java
package net.lab1024.sa.common.mq.kafka.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class KafkaMetrics {

    private final MeterRegistry meterRegistry;

    /**
     * Record successful message send
     */
    public void recordMessageSent(String topic, boolean success) {
        meterRegistry.counter("kafka.messages.sent.total",
            "topic", topic,
            "status", success ? "success" : "failure"
        ).increment();
    }

    /**
     * Record message processing time
     */
    public void recordProcessingTime(String topic, long durationMs) {
        meterRegistry.timer("kafka.processing.duration",
            "topic", topic
        ).record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Record DLQ message
     */
    public void recordDLQMessage(String topic) {
        meterRegistry.counter("kafka.dlq.messages.total",
            "topic", topic
        ).increment();
    }

    /**
     * Record batch size
     */
    public void recordBatchSize(String topic, int batchSize) {
        meterRegistry.summary("kafka.batch.size",
            "topic", topic
        ).record(batchSize);
    }

    /**
     * Record consumer error
     */
    public void recordConsumerError(String topic, String errorType) {
        meterRegistry.counter("kafka.consumer.errors.total",
            "topic", topic,
            "error_type", errorType
        ).increment();
    }

    /**
     * Record consumer lag
     */
    public void recordConsumerLag(String topic, int partition, long lag) {
        meterRegistry.gauge("kafka.consumer.lag",
            "topic", topic,
            "partition", String.valueOf(partition)
        ).measure(lag);
    }
}
```

### Usage in Listeners

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderListener extends AbstractKafkaListener {

    private final OrderService orderService;
    private final KafkaMetrics kafkaMetrics;

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        long startTime = System.currentTimeMillis();
        String topic = record.topic();

        try {
            OrderDTO order = JSON.parseObject(record.value(), OrderDTO.class);
            orderService.processOrder(order);

            long duration = System.currentTimeMillis() - startTime;
            kafkaMetrics.recordProcessingTime(topic, duration);

        } catch (Exception e) {
            kafkaMetrics.recordConsumerError(topic, e.getClass().getSimpleName());
            throw e;  // Propagate to DLQ
        }
    }
}
```

---

## Grafana Dashboards

### Dashboard Configuration

**File**: `docker/grafana/provisioning/dashboards/dashboard.yml`

```yaml
apiVersion: 1

providers:
  - name: 'SmartAdmin Kafka'
    orgId: 1
    folder: 'Kafka'
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    allowUiUpdates: true
    options:
      path: /var/lib/grafana/dashboards
```

### Dashboard JSON

**File**: `docker/grafana/dashboards/kafka-overview.json`

```json
{
  "dashboard": {
    "title": "SmartAdmin Kafka Overview",
    "panels": [
      {
        "id": 1,
        "title": "Producer Throughput",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(kafka_producer_record_send_total[5m])",
            "legendFormat": "{{topic}}"
          }
        ]
      },
      {
        "id": 2,
        "title": "Consumer Lag",
        "type": "graph",
        "targets": [
          {
            "expr": "kafka_consumer_records_lag_max",
            "legendFormat": "{{topic}}-{{partition}}"
          }
        ]
      },
      {
        "id": 3,
        "title": "DLQ Messages",
        "type": "stat",
        "targets": [
          {
            "expr": "kafka_dlq_messages_total",
            "legendFormat": "{{topic}}"
          }
        ]
      },
      {
        "id": 4,
        "title": "Processing Time (P99)",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.99, kafka_processing_duration_bucket)",
            "legendFormat": "{{topic}}"
          }
        ]
      }
    ]
  }
}
```

### Key Panels

**1. Producer Metrics Panel**:
- Messages sent/sec
- Send errors/sec
- Average send latency (P50, P95, P99)
- Buffer utilization

**2. Consumer Metrics Panel**:
- Messages consumed/sec
- Consumer lag by topic/partition
- Fetch latency
- Commit latency
- Rebalance frequency

**3. DLQ Panel**:
- DLQ message count by topic
- DLQ error types
- DLQ processing rate

**4. System Health Panel**:
- Broker availability
- Under-replicated partitions
- Active controllers
- Topic partition distribution

---

## Alerting Rules

### Prometheus Alert Rules

**File**: `docker/prometheus/rules/kafka-alerts.yml`

```yaml
groups:
  - name: kafka_alerts
    interval: 30s
    rules:
      # Producer alerts
      - alert: HighProducerErrorRate
        expr: rate(kafka_producer_record_error_total[5m]) > 10
        for: 5m
        labels:
          severity: warning
          component: kafka
        annotations:
          summary: "High producer error rate on {{ $labels.topic }}"
          description: "Producer error rate is {{ $value }} errors/sec"

      - alert: ProducerHighLatency
        expr: kafka_producer_request_latency_avg > 100
        for: 10m
        labels:
          severity: warning
          component: kafka
        annotations:
          summary: "High producer latency on {{ $labels.topic }}"
          description: "Average producer latency is {{ $value }}ms"

      # Consumer alerts
      - alert: HighConsumerLag
        expr: kafka_consumer_records_lag_max > 1000
        for: 5m
        labels:
          severity: warning
          component: kafka
        annotations:
          summary: "High consumer lag on {{ $labels.topic }}"
          description: "Consumer lag is {{ $value }} messages"

      - alert: CriticalConsumerLag
        expr: kafka_consumer_records_lag_max > 10000
        for: 10m
        labels:
          severity: critical
          component: kafka
        annotations:
          summary: "CRITICAL consumer lag on {{ $labels.topic }}"
          description: "Consumer lag is {{ $value }} messages - immediate action required"

      - alert: ConsumerGroupDown
        expr: kafka_consumer_group_members == 0
        for: 2m
        labels:
          severity: critical
          component: kafka
        annotations:
          summary: "No active consumers in group {{ $labels.group }}"
          description: "Consumer group has no active members"

      # DLQ alerts
      - alert: HighDLQRate
        expr: rate(kafka_dlq_messages_total[5m]) > 10
        for: 5m
        labels:
          severity: warning
          component: kafka
        annotations:
          summary: "High DLQ message rate on {{ $labels.topic }}"
          description: "DLQ receiving {{ $value }} messages/sec"

      - alert: DLQAccumulation
        expr: kafka_dlq_messages_total > 100
        for: 10m
        labels:
          severity: warning
          component: kafka
        annotations:
          summary: "DLQ accumulating messages on {{ $labels.topic }}"
          description: "DLQ has {{ $value }} messages"

      # Broker alerts
      - alert: UnderReplicatedPartitions
        expr: kafka_server_replicamanager_underreplicatedpartitions > 0
        for: 5m
        labels:
          severity: critical
          component: kafka
        annotations:
          summary: "Under-replicated partitions detected"
          description: "{{ $value }} partitions are under-replicated"

      - alert: NoActiveController
        expr: kafka_controller_activecontrollercount != 1
        for: 1m
        labels:
          severity: critical
          component: kafka
        annotations:
          summary: "No active Kafka controller"
          description: "Kafka cluster has {{ $value }} active controllers (expected 1)"

      # Application alerts
      - alert: HighErrorRate
        expr: rate(kafka_consumer_errors_total[5m]) > 10
        for: 5m
        labels:
          severity: warning
          component: application
        annotations:
          summary: "High consumer error rate on {{ $labels.topic }}"
          description: "Error rate is {{ $value }} errors/sec"
```

### Alertmanager Configuration

**File**: `docker/alertmanager/alertmanager.yml`

```yaml
global:
  resolve_timeout: 5m
  slack_api_url: '${SLACK_WEBHOOK_URL}'

route:
  group_by: ['alertname', 'cluster', 'service']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 12h
  receiver: 'default'
  routes:
    - match:
        severity: critical
      receiver: 'critical'
      continue: true

    - match:
        severity: warning
      receiver: 'warning'

receivers:
  - name: 'default'
    email_configs:
      - to: 'ops-team@example.com'
        from: 'alertmanager@example.com'
        smarthost: 'smtp.example.com:587'
        auth_username: 'alertmanager'
        auth_password: '${SMTP_PASSWORD}'

  - name: 'critical'
    slack_configs:
      - channel: '#kafka-critical'
        title: 'CRITICAL: {{ .GroupLabels.alertname }}'
        text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'

    pagerduty_configs:
      - service_key: '${PAGERDUTY_SERVICE_KEY}'

  - name: 'warning'
    slack_configs:
      - channel: '#kafka-warnings'
        title: 'Warning: {{ .GroupLabels.alertname }}'
        text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'
```

---

## Monitoring Best Practices

### 1. Monitor the Right Metrics

**DO**:
- ✅ Consumer lag (most important)
- ✅ DLQ message count
- ✅ Producer/consumer error rates
- ✅ Processing latency (P99)
- ✅ Throughput (messages/sec)

**DON'T**:
- ❌ Monitor every single metric
- ❌ Set too many alerts (alert fatigue)
- ❌ Ignore trends (only react to alerts)

### 2. Set Meaningful Alert Thresholds

```yaml
# ✅ Good: Contextual thresholds
- alert: HighConsumerLag
  expr: kafka_consumer_records_lag_max > 1000
  for: 5m  # Allow temporary spikes

# ❌ Bad: Too sensitive
- alert: AnyLag
  expr: kafka_consumer_records_lag_max > 0
  for: 30s  # Fires constantly
```

### 3. Use Dashboards Effectively

**Layer metrics by audience**:
- **Executive dashboard**: High-level health, SLA metrics
- **Operations dashboard**: Lag, errors, broker health
- **Development dashboard**: Message rates, latencies, DLQ details

### 4. Correlate Metrics

```promql
# Correlate lag with throughput
kafka_consumer_records_lag_max
  /
rate(kafka_producer_record_send_total[5m])
```

---

## Troubleshooting Monitoring

### No Metrics Available

```bash
# Check actuator endpoint
curl http://localhost:1024/actuator/health

# Check Prometheus target status
curl http://localhost:9090/api/v1/targets

# Check if Micrometer is registered
curl http://localhost:1024/actuator/metrics | grep kafka
```

### Metrics Not Updating

```bash
# Check Prometheus scrape interval
curl http://localhost:9090/api/v1/status/config | jq '.data.yaml.global.scrape_interval'

# Force scrape
# In Prometheus UI: Status → Targets → Click "scrape" button
```

### High Cardinality Warning

```bash
# Identify high cardinality metrics
curl http://localhost:9090/api/v1/label/__name__/values

# Reduce cardinality by removing tags
management:
  metrics:
    tags:
      # Remove dynamic tags like message IDs
```

---

## See Also

- [Health Checks](/kafka/operations/health-checks) - Health check configuration
- [Deployment Guide](/kafka/operations/deployment) - Deployment setup
- [Performance Tuning](/kafka/operations/performance-tuning) - Optimization
- [Troubleshooting](/kafka/troubleshooting/common-issues) - Common issues
- [Best Practices](/kafka/guides/best-practices) - Monitoring best practices

---

**Last Updated**: 2026-01-21
