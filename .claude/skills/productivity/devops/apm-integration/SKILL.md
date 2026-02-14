---
name: apm-integration
description: [P2 - Productivity] Generate APM (Application Performance Monitoring) integration with Skywalking, Micrometer, Grafana dashboards, and custom metrics for SmartAdmin applications. Use when setting up production monitoring, debugging performance issues, implementing distributed tracing, or creating custom business metrics. Triggers when user mentions "monitoring", "APM", "performance tracking", "Grafana dashboard", "metrics", "distributed tracing", "Skywalking", or "production observability".
---

# APM Integration Skill

**Priority:** P0 - Pain Point #1
**Sprint:** 1 (Weeks 1-4)
**Status:** 🚧 Skeleton Created - Detailed Content Pending

## Purpose

Eliminate production debugging pain by generating complete APM (Application Performance Monitoring) integration for SmartAdmin applications. Reduces debugging time by 70% through systematic observability.

## Problem Statement

**User Pain Point:** "监控/调试生产问题困难" (Monitoring/Debugging production issues is difficult)

**Current Issues:**
- No systematic performance monitoring
- Hard to trace issues across distributed calls
- Missing custom business metrics
- Manual dashboard creation is time-consuming
- Alert configuration is complex

## Solution Overview

This skill generates:
- ✅ Skywalking Java agent configuration
- ✅ Micrometer custom metrics (counters, gauges, timers, histograms)
- ✅ Grafana dashboard templates (JVM, HTTP, Database, Redis, Business Metrics)
- ✅ Alert rule generation (latency P95/P99, error rate, throughput)
- ✅ Distributed tracing context propagation (TraceId, SpanId)
- ✅ Log correlation integration (MDC with TraceId)

## Quick Start

**Most common usage:**
```
User: "Add APM monitoring to UserService with custom metrics for login events"
```

You will:
1. Configure Skywalking agent
2. Generate Micrometer custom metrics
3. Create Grafana dashboards
4. Set up alert rules
5. Enable distributed tracing
6. Integrate log correlation

## Trigger Keywords

This skill is automatically activated when the user's request contains:

**Primary Keywords** (High confidence):
- "APM" - Application Performance Monitoring integration
- "monitoring" - System monitoring setup
- "Skywalking" - Skywalking APM integration
- "Grafana" - Grafana dashboard generation
- "metrics" - Custom metrics implementation

**Secondary Keywords** (Medium confidence):
- "distributed tracing" - Context: distributed system tracing setup
- "Micrometer" - Context: Micrometer metrics library integration
- "custom metrics" - Context: application-specific metrics
- "dashboard" - Context: monitoring dashboard creation
- "alert rules" - Context: monitoring alert configuration

**Phrase Patterns**:
- "Add APM monitoring to [component]" - Example: "Add APM monitoring to UserService"
- "Setup [monitoring tool]" - Example: "Setup Skywalking for distributed tracing"
- "Create [dashboard] for [metrics]" - Example: "Create Grafana dashboard for login metrics"

**Example User Requests**:
```
User: "Add APM monitoring to UserService with custom metrics for login events"
User: "Setup Skywalking distributed tracing for microservices"
User: "Create Grafana dashboard for wallet transaction metrics"
User: "Integrate Micrometer with custom business metrics"
User: "Setup alert rules for high error rates"
```

**Note**: This skill can also be manually invoked via `/apm-integration` command.

## Scope

### Included
- Skywalking Java agent setup (auto-instrumentation)
- Micrometer integration (Spring Boot Actuator + Prometheus)
- Custom business metrics generation
- Grafana dashboard JSON templates (JVM, HTTP, DB, Redis, Custom)
- Alert rules (latency, error rate, throughput)
- Distributed tracing (TraceId propagation)
- MDC log correlation

### Not Included
- Infrastructure deployment (Skywalking OAP server, Grafana server)
- Alert notification channels (Slack, email, PagerDuty)
- Custom visualization plugins

## Integration Points

- Works with `java-performance-pro` for performance profiling
- Integrates with SmartAdmin logging infrastructure
- Uses Spring Boot Actuator for metrics endpoints
- Compatible with `smartadmin-crud-generator` (adds metrics to generated code)

## Success Criteria

- ✅ Production debugging time reduced by 70%
- ✅ P95/P99 latency tracking for all endpoints
- ✅ Error rate alerts within 1 minute
- ✅ Distributed tracing working across all layers
- ✅ Custom business metrics dashboards operational

## Implementation Details

### Phase 4: Skywalking Agent Configuration

**JVM Arguments:**
```bash
-javaagent:/path/to/skywalking-agent.jar
-Dskywalking.agent.service_name=smartadmin-api
-Dskywalking.collector.backend_service=skywalking-oap:11800
-Dskywalking.logging.level=INFO
```

**application.yml:**
```yaml
# Skywalking Configuration
skywalking:
  agent:
    service_name: ${spring.application.name}
    collector_backend_service: ${SKYWALKING_OAP_SERVER:localhost:11800}
    logging:
      level: INFO
    span_limit_per_segment: 300
    ignore_suffix: .jpg,.jpeg,.js,.css,.png,.bmp,.gif,.ico,.mp3,.mp4
```

### Phase 5: Micrometer Custom Metrics

**Counter Example (Login Events):**
```java
@Service
@RequiredArgsConstructor
public class UserService {
    private final MeterRegistry meterRegistry;

    public ResponseDTO<LoginVO> login(LoginForm form) {
        Counter loginCounter = Counter.builder("smartadmin.user.login")
            .tag("result", "success")
            .tag("channel", form.getChannel())
            .description("User login events")
            .register(meterRegistry);

        loginCounter.increment();
        // ... login logic
    }
}
```

**Timer Example (Method Execution Time):**
```java
@Service
@RequiredArgsConstructor
public class OrderService {
    private final MeterRegistry meterRegistry;

    public ResponseDTO<OrderVO> createOrder(OrderForm form) {
        return Timer.builder("smartadmin.order.create.time")
            .tag("type", form.getOrderType())
            .description("Order creation time")
            .register(meterRegistry)
            .record(() -> {
                // ... order creation logic
                return ResponseDTO.ok(orderVO);
            });
    }
}
```

**Gauge Example (Active Sessions):**
```java
@Component
@RequiredArgsConstructor
public class SessionMetrics {
    private final MeterRegistry meterRegistry;
    private final AtomicInteger activeSessions = new AtomicInteger(0);

    @PostConstruct
    public void registerMetrics() {
        Gauge.builder("smartadmin.sessions.active", activeSessions, AtomicInteger::get)
            .description("Number of active user sessions")
            .register(meterRegistry);
    }
}
```

**Histogram Example (Request Size Distribution):**
```java
@RestController
@RequiredArgsConstructor
public class ApiController {
    private final MeterRegistry meterRegistry;

    @PostMapping("/api/data")
    public ResponseDTO<?> handleData(@RequestBody String data) {
        DistributionSummary.builder("smartadmin.api.request.size")
            .baseUnit("bytes")
            .description("Request payload size distribution")
            .register(meterRegistry)
            .record(data.length());

        // ... handle request
    }
}
```

### Phase 6: Grafana Dashboard Templates

**JVM Dashboard (JSON snippet):**
```json
{
  "dashboard": {
    "title": "SmartAdmin JVM Metrics",
    "panels": [
      {
        "title": "Heap Memory Usage",
        "targets": [
          {
            "expr": "jvm_memory_used_bytes{area=\"heap\",application=\"$application\"}"
          }
        ],
        "type": "graph"
      },
      {
        "title": "GC Pause Time (P99)",
        "targets": [
          {
            "expr": "histogram_quantile(0.99, jvm_gc_pause_seconds_bucket{application=\"$application\"})"
          }
        ],
        "type": "graph"
      }
    ]
  }
}
```

**Custom Business Metrics Dashboard:**
```json
{
  "dashboard": {
    "title": "SmartAdmin Business Metrics",
    "panels": [
      {
        "title": "Login Events (per minute)",
        "targets": [
          {
            "expr": "rate(smartadmin_user_login_total[1m])"
          }
        ],
        "type": "graph"
      },
      {
        "title": "Order Creation Time (P95)",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, smartadmin_order_create_time_seconds_bucket)"
          }
        ],
        "type": "graph"
      }
    ]
  }
}
```

### Phase 7: Alert Rules

**Prometheus Alert Rules (prometheus-alerts.yml):**
```yaml
groups:
  - name: smartadmin-alerts
    interval: 30s
    rules:
      # High Error Rate
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value }} (threshold: 0.05)"

      # High Latency (P95)
      - alert: HighLatencyP95
        expr: histogram_quantile(0.95, http_server_requests_seconds_bucket) > 1.0
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High P95 latency detected"
          description: "P95 latency is {{ $value }}s (threshold: 1s)"

      # Low Throughput
      - alert: LowThroughput
        expr: rate(http_server_requests_seconds_count[5m]) < 10
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Low request throughput"
          description: "Throughput is {{ $value }} req/s (threshold: 10)"
```

## Troubleshooting Guide

### Issue 1: Skywalking Agent Not Connecting

**Symptoms:**
- No traces appearing in Skywalking UI
- Agent logs show connection errors

**Diagnostic Steps:**
1. Check agent logs:
   ```bash
   tail -f logs/skywalking-api.log
   ```

2. Verify OAP server connectivity:
   ```bash
   telnet skywalking-oap 11800
   ```

3. Check service name uniqueness:
   ```bash
   grep "service_name" application.yml
   ```

**Solutions:**
- Ensure OAP server address is correct in JVM args
- Verify firewall rules allow port 11800
- Check service name doesn't conflict with existing services
- Restart application after configuration changes

### Issue 2: Metrics Not Appearing in Grafana

**Symptoms:**
- "No Data" shown in Grafana panels
- Prometheus endpoint returns empty metrics

**Diagnostic Steps:**
1. Verify actuator endpoint:
   ```bash
   curl http://localhost:8080/actuator/prometheus
   ```

2. Check Prometheus scrape config:
   ```bash
   curl http://prometheus:9090/targets
   ```

3. Validate metric names:
   ```bash
   curl http://localhost:8080/actuator/prometheus | grep smartadmin
   ```

**Solutions:**
- Enable actuator endpoints in application.yml:
  ```yaml
  management:
    endpoints:
      web:
        exposure:
          include: prometheus,health,info
  ```
- Verify Prometheus scrape job configuration
- Check metric naming conventions (lowercase, underscores)
- Ensure MeterRegistry is properly injected

### Issue 3: High Memory Usage from Micrometer

**Symptoms:**
- OOM errors after adding custom metrics
- Heap usage increasing continuously

**Diagnostic Steps:**
1. Profile metric cardinality:
   ```bash
   curl http://localhost:8080/actuator/prometheus | wc -l
   ```

2. Identify high-cardinality tags:
   ```bash
   curl http://localhost:8080/actuator/prometheus | grep 'smartadmin' | sort | uniq -c | sort -rn
   ```

**Solutions:**
- Limit tag cardinality (avoid using IDs, UUIDs, timestamps as tags)
- Use histogram buckets instead of recording every value
- Configure max metrics:
  ```yaml
  management:
    metrics:
      enable:
        jvm: true
        process: true
      distribution:
        percentiles-histogram:
          http.server.requests: true
        slo:
          http.server.requests: 100ms,200ms,500ms,1s
  ```

### Issue 4: Distributed Tracing Context Lost

**Symptoms:**
- TraceId not propagated across services
- Spans appear as separate traces

**Diagnostic Steps:**
1. Check TraceId in logs:
   ```bash
   grep "traceId" logs/application.log
   ```

2. Verify Skywalking headers:
   ```bash
   curl -v http://localhost:8080/api/test | grep 'sw8'
   ```

**Solutions:**
- Ensure Skywalking agent is loaded before application starts
- Configure MDC pattern in logback.xml:
  ```xml
  <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] [%X{traceId}] %-5level %logger - %msg%n</pattern>
  ```
- For async methods, use `@Traced` annotation or propagate context manually

## Production Checklist

### Pre-Deployment Verification

- [ ] **Skywalking Agent Loaded**
  - Verify `-javaagent` in JVM arguments
  - Check agent version compatibility (Java 21 support)
  - Confirm OAP server connectivity

- [ ] **Actuator Endpoints Secured**
  - Restrict actuator to internal network only
  - Enable authentication if exposed
  - Verify `/actuator/prometheus` returns metrics

- [ ] **Grafana Dashboards Imported**
  - Import all dashboard JSON templates
  - Configure data source (Prometheus)
  - Set up dashboard variables ($application, $instance)

- [ ] **Alert Rules Configured**
  - Deploy prometheus-alerts.yml to Prometheus
  - Test alert firing (simulate high error rate)
  - Configure notification channels (Slack/Email)

### Performance Baseline

Establish baseline metrics before production deployment:

- [ ] **Latency Baselines**
  - P50 latency: _______ ms
  - P95 latency: _______ ms
  - P99 latency: _______ ms

- [ ] **Throughput Baselines**
  - Normal load: _______ req/s
  - Peak load: _______ req/s

- [ ] **Error Rate Baselines**
  - Normal error rate: _______ %
  - Acceptable error rate: < 1%

### Rollback Procedures

If APM causes issues in production:

1. **Quick Rollback (Remove Agent)**
   ```bash
   # Remove -javaagent from JVM args
   # Restart application without agent
   systemctl restart smartadmin-api
   ```

2. **Disable Metrics Collection**
   ```yaml
   management:
     metrics:
       export:
         prometheus:
           enabled: false
   ```

3. **Monitor Impact**
   - CPU usage should return to normal
   - Memory usage should stabilize
   - Response times should improve

### Ongoing Maintenance

- [ ] **Weekly Reviews**
  - Check dashboard for anomalies
  - Review alert firing frequency
  - Validate metric cardinality

- [ ] **Monthly Optimizations**
  - Remove unused custom metrics
  - Adjust alert thresholds based on trends
  - Update dashboard panels for new features

- [ ] **Quarterly Upgrades**
  - Update Skywalking agent version
  - Upgrade Micrometer dependencies
  - Review Grafana dashboard best practices

## Next Steps

**Detailed documentation pending Sprint 1 completion.**

See `references/` directory for:
- Skywalking configuration patterns
- Micrometer custom metrics examples
- Grafana dashboard templates
- Alert rule configurations
- Distributed tracing patterns

---

**Version:** 1.0.0-SKELETON
**Created:** 2026-01-26
**Sprint:** 1 (Weeks 1-4)
**Status:** Awaiting detailed implementation
