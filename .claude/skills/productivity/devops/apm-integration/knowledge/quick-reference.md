# APM Integration Skill - Quick Reference

**Version**: 1.0.0
**Last Updated**: 2026-02-02
**Skill**: apm-integration (P2 - Productivity/DevOps)

---

## Command Quick Reference

### Basic Commands

| Command | Purpose | Duration |
|---------|---------|----------|
| APM Tool Selection | Choose APM tool for project | ~5 min |
| SkyWalking Setup | Install SkyWalking agent | ~10 min |
| Micrometer Integration | Add Prometheus metrics | ~8 min |
| Grafana Dashboard | Create monitoring dashboard | ~15 min |
| Alert Configuration | Setup alerting rules | ~10 min |

### Rapid Development Workflow

| Step | Action | Time |
|------|--------|------|
| 1. Select APM | Choose tool (SkyWalking/Micrometer/Zipkin) | ~5 min |
| 2. Install Agent | Add dependencies and configuration | ~10 min |
| 3. Deploy | Start application with APM | ~5 min |
| 4. Create Dashboard | Setup Grafana monitoring | ~15 min |
| 5. Configure Alerts | Add alerting rules | ~10 min |

**Total**: ~45 minutes per APM integration

---

## APM Tool Selection Matrix

### Tool 1: Apache SkyWalking (Recommended for SmartAdmin)

**Use When**: Need distributed tracing + metrics + logs in one solution

**Pros**:
- ✅ Zero code intrusion (Java agent)
- ✅ Distributed tracing (trace ID propagation)
- ✅ Service topology map
- ✅ JVM metrics out-of-the-box
- ✅ Supports PostgreSQL, Redis, MyBatis

**Cons**:
- ⚠️ Requires OAP backend server
- ⚠️ UI less customizable than Grafana

**Quick Setup**:
```bash
# 1. Download SkyWalking agent
wget https://archive.apache.org/dist/skywalking/java-agent/8.15.0/apache-skywalking-java-agent-8.15.0.tgz
tar -xzf apache-skywalking-java-agent-8.15.0.tgz

# 2. Configure agent
export SW_AGENT_NAME=smartadmin-api
export SW_AGENT_COLLECTOR_BACKEND_SERVICES=skywalking-oap:11800

# 3. Run application with agent
java -javaagent:/path/to/skywalking-agent.jar \
     -Dskywalking.agent.service_name=smartadmin-api \
     -jar smartadmin-app.jar
```

**Time to Setup**: 10-15 minutes

---

### Tool 2: Micrometer + Prometheus + Grafana

**Use When**: Need flexible metrics + custom dashboards

**Pros**:
- ✅ Highly customizable Grafana dashboards
- ✅ Integrates with existing Prometheus
- ✅ Rich metrics library (JVM, HTTP, DB)
- ✅ Spring Boot native support

**Cons**:
- ⚠️ No distributed tracing (need Zipkin)
- ⚠️ Requires code changes for custom metrics

**Quick Setup**:
```gradle
// build.gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'io.micrometer:micrometer-registry-prometheus'
}
```

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

**Time to Setup**: 8-12 minutes

---

### Tool 3: Zipkin (Distributed Tracing Only)

**Use When**: Only need distributed tracing

**Pros**:
- ✅ Lightweight
- ✅ Simple UI
- ✅ Easy integration with Spring Cloud Sleuth

**Cons**:
- ⚠️ No metrics or alerting
- ⚠️ Requires Spring Cloud Sleuth dependency

**Quick Setup**:
```gradle
dependencies {
    implementation 'org.springframework.cloud:spring-cloud-starter-zipkin'
    implementation 'org.springframework.cloud:spring-cloud-sleuth-zipkin'
}
```

```yaml
spring:
  zipkin:
    base-url: http://localhost:9411
  sleuth:
    sampler:
      probability: 1.0  # 100% sampling
```

**Time to Setup**: 5-8 minutes

---

## Integration Pattern 1: SkyWalking Full Setup

**Implementation**:

**Step 1: Docker Compose (SkyWalking OAP + UI)**:
```yaml
version: '3.8'
services:
  skywalking-oap:
    image: apache/skywalking-oap-server:9.4.0
    container_name: skywalking-oap
    ports:
      - "11800:11800"  # gRPC
      - "12800:12800"  # HTTP
    environment:
      SW_STORAGE: postgresql
      SW_JDBC_URL: jdbc:postgresql://postgres:5432/skywalking
      SW_DATA_SOURCE_USER: postgres
      SW_DATA_SOURCE_PASSWORD: password

  skywalking-ui:
    image: apache/skywalking-ui:9.4.0
    container_name: skywalking-ui
    ports:
      - "8080:8080"
    environment:
      SW_OAP_ADDRESS: http://skywalking-oap:12800
```

**Step 2: Java Agent Configuration**:
```properties
# agent/config/agent.config
agent.service_name=smartadmin-api
collector.backend_service=localhost:11800

# Sampling rate (1 = 100%, 0.1 = 10%)
agent.sample_n_per_3_secs=10

# Ignore specific endpoints
trace.ignore_path=/health,/actuator/**

# Plugin configuration
plugin.jdbc.trace_sql_parameters=true
plugin.redis.trace_redis_commands=true
```

**Step 3: Run Application**:
```bash
java -javaagent:/opt/skywalking-agent/skywalking-agent.jar \
     -Dskywalking.agent.service_name=smartadmin-api \
     -Dskywalking.collector.backend_service=localhost:11800 \
     -jar smartadmin-app.jar
```

**Access UI**: http://localhost:8080

**Time to Setup**: 15-20 minutes

---

## Integration Pattern 2: Micrometer + Prometheus + Grafana

**Implementation**:

**Step 1: Add Dependencies**:
```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'io.micrometer:micrometer-registry-prometheus'
}
```

**Step 2: Configure Actuator**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: "*"
      base-path: /actuator
  endpoint:
    health:
      show-details: always
  metrics:
    tags:
      application: smartadmin-api
      environment: production
    export:
      prometheus:
        enabled: true
```

**Step 3: Prometheus Configuration**:
```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'smartadmin-api'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:1024']
```

**Step 4: Grafana Dashboard JSON** (Simplified):
```json
{
  "dashboard": {
    "title": "SmartAdmin API Metrics",
    "panels": [
      {
        "title": "Request Rate",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count[5m])"
          }
        ]
      },
      {
        "title": "Response Time (p99)",
        "targets": [
          {
            "expr": "histogram_quantile(0.99, http_server_requests_seconds_bucket)"
          }
        ]
      },
      {
        "title": "JVM Heap Usage",
        "targets": [
          {
            "expr": "jvm_memory_used_bytes{area=\"heap\"} / jvm_memory_max_bytes{area=\"heap\"} * 100"
          }
        ]
      }
    ]
  }
}
```

**Import Dashboard**: Grafana → Import → Upload JSON

**Time to Setup**: 12-18 minutes

---

## Custom Metrics Pattern

**Use When**: Need application-specific metrics

**Implementation**:
```java
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeDao employeeDao;
    private final MeterRegistry meterRegistry;

    /**
     * Custom counter metric
     */
    public void saveEmployee(EmployeeForm form) {
        employeeDao.insert(form);

        // Increment counter
        meterRegistry.counter("employee.created",
            "department", form.getDeptName()
        ).increment();
    }

    /**
     * Custom timer metric
     */
    @Timed(value = "employee.search", description = "Employee search duration")
    public List<EmployeeVO> searchEmployees(String keyword) {
        return employeeDao.search(keyword);
    }

    /**
     * Custom gauge metric
     */
    @PostConstruct
    public void registerGauges() {
        meterRegistry.gauge("employee.active.count", this, service -> {
            return employeeDao.countActive();
        });
    }
}
```

**Prometheus Query**:
```promql
# Employee creation rate
rate(employee_created_total[5m])

# Employee search p99 latency
histogram_quantile(0.99, employee_search_seconds_bucket)

# Active employee count
employee_active_count
```

**Time to Implement**: 5-10 minutes per metric

---

## Alert Configuration Pattern

**Use When**: Need proactive monitoring

**Prometheus Alerting Rules**:
```yaml
# alerts.yml
groups:
  - name: smartadmin-api
    interval: 30s
    rules:
      # High error rate
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value }}% (threshold: 5%)"

      # High response time
      - alert: HighResponseTime
        expr: histogram_quantile(0.99, http_server_requests_seconds_bucket) > 2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High response time (p99)"
          description: "P99 latency is {{ $value }}s (threshold: 2s)"

      # High heap usage
      - alert: HighHeapUsage
        expr: (jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High JVM heap usage"
          description: "Heap usage is {{ $value }}% (threshold: 90%)"

      # High CPU usage
      - alert: HighCPUUsage
        expr: process_cpu_usage > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High CPU usage"
          description: "CPU usage is {{ $value }}% (threshold: 80%)"
```

**Alertmanager Configuration**:
```yaml
# alertmanager.yml
global:
  resolve_timeout: 5m

route:
  receiver: 'slack-notifications'
  group_by: ['alertname', 'severity']
  group_wait: 10s
  group_interval: 5m
  repeat_interval: 4h

receivers:
  - name: 'slack-notifications'
    slack_configs:
      - api_url: 'https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK'
        channel: '#alerts'
        title: '{{ .GroupLabels.alertname }}'
        text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'
```

**Time to Configure**: 10-15 minutes

---

## Common Errors and Quick Fixes

### Error 1: Metrics Endpoint Not Exposed

**Symptom**: `/actuator/prometheus` returns 404

**Cause**: Actuator endpoints not exposed

**Quick Fix**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: prometheus,health,info,metrics
```

---

### Error 2: SkyWalking Agent Not Working

**Symptom**: No traces in SkyWalking UI

**Cause**: Agent not properly loaded or OAP unreachable

**Quick Fix**:
```bash
# Verify agent is loaded (check logs)
java -javaagent:/path/to/skywalking-agent.jar -jar app.jar

# Expected log:
# [SkyWalking] Agent started successfully

# Verify OAP connection
telnet localhost 11800
```

---

### Error 3: Grafana Dashboard No Data

**Symptom**: Grafana shows "No data"

**Cause**: Prometheus not scraping or wrong query

**Quick Fix**:
```bash
# Verify Prometheus scraping
curl http://localhost:9090/api/v1/targets

# Verify metrics endpoint
curl http://localhost:1024/actuator/prometheus

# Fix Prometheus config
scrape_configs:
  - job_name: 'smartadmin-api'
    static_configs:
      - targets: ['localhost:1024']  # Correct port
```

---

## Time Estimates (Production Data)

| Task | Setup | Configuration | Verification | Total |
|------|-------|---------------|--------------|-------|
| SkyWalking | 10 min | 5 min | 5 min | 20 min |
| Micrometer + Prometheus | 8 min | 4 min | 3 min | 15 min |
| Grafana Dashboard | 10 min | 5 min | 2 min | 17 min |
| Alert Configuration | 5 min | 8 min | 2 min | 15 min |
| Custom Metrics | 5 min | 5 min | 2 min | 12 min |

**Full APM Stack**: 45-60 minutes (SkyWalking + Grafana + Alerts)

---

## Validation Checklist

Before deploying APM:

- [ ] APM tool selected (SkyWalking/Micrometer/Zipkin)
- [ ] Agent/dependencies installed
- [ ] Application starts with APM enabled
- [ ] Metrics visible in UI/Grafana
- [ ] Distributed tracing working (trace IDs propagate)
- [ ] JVM metrics available (heap, CPU, threads)
- [ ] Custom metrics implemented (optional)
- [ ] Alerting rules configured
- [ ] Alert notifications tested (Slack/email)

---

**See Also**:
- [Java Performance Pro](../../analysis/java-performance-pro/) - Performance profiling
- [PostgreSQL Best Practices](../../integration/postgresql-best-practices/) - Database monitoring
- [SmartAdmin Patterns](./../../../.claude/shared/knowledge/smartadmin-patterns.md) - Monitoring patterns
