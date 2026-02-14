# Grafana Dashboards Guide

**Skill:** apm-integration
**Component:** Grafana + Prometheus
**Purpose:** Visualization dashboards for metrics

---

## Quick Start

### 1. Configure Prometheus Data Source

**prometheus.yml:**
```yaml
scrape_configs:
  - job_name: 'smartadmin-api'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['smartadmin-api:1024']
        labels:
          application: 'smartadmin'
          environment: 'production'
```

### 2. Add Data Source in Grafana

1. Login to Grafana: `http://grafana:3000`
2. Configuration → Data Sources → Add data source
3. Select **Prometheus**
4. URL: `http://prometheus:9090`
5. Save & Test

---

## Dashboard Templates

### 1. JVM Metrics Dashboard

**Import JSON:** [jvm-dashboard.json](../assets/jvm-dashboard.json)

**Key Panels:**

**Heap Memory Usage:**
```promql
# Used heap memory
jvm_memory_used_bytes{area="heap",application="smartadmin"}

# Max heap memory
jvm_memory_max_bytes{area="heap",application="smartadmin"}

# Heap usage percentage
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100
```

**GC Metrics:**
```promql
# GC count rate (per second)
rate(jvm_gc_pause_seconds_count[5m])

# GC pause time (99th percentile)
jvm_gc_pause_seconds{quantile="0.99"}

# GC overhead percentage
rate(jvm_gc_pause_seconds_sum[5m]) * 100
```

**Thread Metrics:**
```promql
# Thread count
jvm_threads_live_threads

# Daemon threads
jvm_threads_daemon_threads

# Peak threads
jvm_threads_peak_threads
```

---

### 2. HTTP Metrics Dashboard

**Request Rate:**
```promql
# Requests per second (by status)
sum(rate(http_server_requests_seconds_count{application="smartadmin"}[5m])) by (status)

# Total requests per second
sum(rate(http_server_requests_seconds_count{application="smartadmin"}[5m]))
```

**Latency:**
```promql
# P50 latency
histogram_quantile(0.50, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri))

# P95 latency
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri))

# P99 latency
histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri))
```

**Error Rate:**
```promql
# 5xx error rate
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) /
sum(rate(http_server_requests_seconds_count[5m])) * 100

# 4xx error rate
sum(rate(http_server_requests_seconds_count{status=~"4.."}[5m])) /
sum(rate(http_server_requests_seconds_count[5m])) * 100
```

---

### 3. Database Metrics Dashboard

**Connection Pool:**
```promql
# Active connections
smartadmin_db_pool_active

# Idle connections
smartadmin_db_pool_idle

# Connection pool utilization
(smartadmin_db_pool_active / (smartadmin_db_pool_active + smartadmin_db_pool_idle)) * 100
```

**Query Performance:**
```promql
# Query rate
rate(smartadmin_db_query_total[5m])

# Slow query count (> 1s)
sum(rate(smartadmin_db_query_seconds_bucket{le="1.0"}[5m]))

# Query P95 latency
histogram_quantile(0.95, rate(smartadmin_db_query_seconds_bucket[5m]))
```

---

### 4. Redis Metrics Dashboard

**Commands:**
```promql
# Redis command rate
rate(smartadmin_redis_commands_total[5m])

# Commands by type
sum(rate(smartadmin_redis_commands_total[5m])) by (command)
```

**Hit Rate:**
```promql
# Cache hit rate
(smartadmin_cache_hits_total / (smartadmin_cache_hits_total + smartadmin_cache_misses_total)) * 100
```

---

### 5. Business Metrics Dashboard

**Order Metrics:**
```promql
# Orders created per minute
rate(smartadmin_order_created_total[1m]) * 60

# Orders by payment method
sum(rate(smartadmin_order_created_total[5m])) by (payment_method)

# Revenue per hour
sum(increase(smartadmin_business_revenue_sum[1h]))
```

**User Metrics:**
```promql
# Active users
smartadmin_user_active_count

# Login rate
rate(smartadmin_user_login_total[5m]) * 60

# Feature usage top 10
topk(10, sum(rate(smartadmin_feature_usage_total[5m])) by (feature))
```

---

## Alert Rules

**Create in Grafana Alerts or Prometheus alertmanager.yml:**

### High Error Rate Alert

```yaml
alert: HighErrorRate
expr: |
  sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) /
  sum(rate(http_server_requests_seconds_count[5m])) * 100 > 5
for: 5m
labels:
  severity: warning
annotations:
  summary: "High 5xx error rate on {{ $labels.instance }}"
  description: "Error rate is {{ $value }}% (threshold: 5%)"
```

### High Latency Alert

```yaml
alert: HighLatency
expr: |
  histogram_quantile(0.99,
    sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri)
  ) > 2
for: 10m
labels:
  severity: warning
annotations:
  summary: "High P99 latency on {{ $labels.uri }}"
  description: "P99 latency is {{ $value }}s (threshold: 2s)"
```

### Memory Usage Alert

```yaml
alert: HighMemoryUsage
expr: |
  (jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100 > 85
for: 5m
labels:
  severity: critical
annotations:
  summary: "High heap memory usage on {{ $labels.instance }}"
  description: "Heap usage is {{ $value }}% (threshold: 85%)"
```

---

## Dashboard JSON Templates

### Minimal JVM Dashboard

```json
{
  "dashboard": {
    "title": "SmartAdmin JVM Metrics",
    "panels": [
      {
        "title": "Heap Memory Usage",
        "targets": [
          {
            "expr": "jvm_memory_used_bytes{area='heap',application='smartadmin'}",
            "legendFormat": "Used"
          },
          {
            "expr": "jvm_memory_max_bytes{area='heap',application='smartadmin'}",
            "legendFormat": "Max"
          }
        ],
        "type": "graph"
      },
      {
        "title": "GC Pause Time (P99)",
        "targets": [
          {
            "expr": "jvm_gc_pause_seconds{quantile='0.99'}",
            "legendFormat": "{{ gc }}"
          }
        ],
        "type": "graph"
      }
    ]
  }
}
```

---

**Full Templates:** Available in `.claude/skills/apm-integration/assets/` directory
**Next:** [Alert Rules Configuration](alert-rules.md)
**Version:** 1.0.0
**Last Updated:** 2026-01-26
