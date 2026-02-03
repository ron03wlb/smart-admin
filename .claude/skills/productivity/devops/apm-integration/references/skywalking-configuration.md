# Skywalking Configuration Guide

**Skill:** apm-integration
**Component:** Apache Skywalking Java Agent
**Purpose:** Auto-instrumentation for distributed tracing

---

## Quick Start Configuration

### 1. Add Skywalking Agent Dependency

**Gradle (sa-admin/build.gradle):**
```gradle
dependencies {
    // Skywalking Java agent (auto-instrumentation)
    implementation 'org.apache.skywalking:apm-toolkit-trace:9.2.0'
    implementation 'org.apache.skywalking:apm-toolkit-logback-1.x:9.2.0'
}
```

### 2. Download Skywalking Agent

```bash
# Download Skywalking agent
cd /opt
wget https://archive.apache.org/dist/skywalking/java-agent/9.2.0/apache-skywalking-java-agent-9.2.0.tgz
tar -xzf apache-skywalking-java-agent-9.2.0.tgz
```

### 3. Configure Agent Properties

**Create:** `sa-admin/src/main/resources/skywalking-agent.config`

```properties
# Service name (appears in Skywalking UI)
agent.service_name=${SW_AGENT_NAME:smartadmin-api}

# Skywalking OAP server address
collector.backend_service=${SW_AGENT_COLLECTOR_BACKEND_SERVICES:127.0.0.1:11800}

# Sampling rate (1 = 100%, 0.5 = 50%)
agent.sample_n_per_3_secs=${SW_AGENT_SAMPLE:10}

# Log output
logging.level=${SW_LOGGING_LEVEL:INFO}
logging.file_name=skywalking-api.log
logging.dir=${SW_LOGGING_DIR:logs}

# Plugin configuration
plugin.jdbc.trace_sql_parameters=true
plugin.jdbc.sql_parameters_max_length=512

# Redis plugin
plugin.lettuce.trace_redis_parameters=true

# HTTP plugin
plugin.http.http_params_length_threshold=2048

# Ignore specific endpoints (health checks, metrics)
plugin.http.trace.ignore_path=/actuator/**,/swagger-ui/**,/v3/api-docs/**
```

### 4. JVM Startup Arguments

**Application startup script:**

```bash
#!/bin/bash
# smartadmin-startup.sh

SKYWALKING_AGENT_PATH="/opt/skywalking-agent/skywalking-agent.jar"
SKYWALKING_CONFIG_PATH="./skywalking-agent.config"

java -javaagent:${SKYWALKING_AGENT_PATH} \
     -Dskywalking.agent.service_name=smartadmin-api \
     -Dskywalking.collector.backend_service=skywalking-oap:11800 \
     -Dskywalking.logging.level=INFO \
     -jar sa-admin.jar
```

**Docker Compose:**
```yaml
version: '3.8'
services:
  smartadmin-api:
    image: smartadmin:latest
    environment:
      - SW_AGENT_NAME=smartadmin-api
      - SW_AGENT_COLLECTOR_BACKEND_SERVICES=skywalking-oap:11800
      - SW_AGENT_SAMPLE=10
      - SW_LOGGING_LEVEL=INFO
    volumes:
      - /opt/skywalking-agent:/skywalking-agent
    command: >
      java -javaagent:/skywalking-agent/skywalking-agent.jar
           -jar /app/sa-admin.jar
```

---

## Advanced Configuration

### Multi-Instance Deployment

**Using instance ID:**
```properties
# Unique instance identifier
agent.instance_name=${HOSTNAME:smartadmin-instance-1}

# Or use auto-generated UUID
agent.instance_uuid=${SW_AGENT_INSTANCE_UUID:}
```

### Custom Tags

**Add custom metadata:**
```properties
# Custom tags (appear in Skywalking UI)
agent.instance_properties[application]=smartadmin
agent.instance_properties[environment]=${ENV:production}
agent.instance_properties[region]=${REGION:us-east-1}
agent.instance_properties[version]=${APP_VERSION:4.0.0}
```

### Performance Tuning

**Adjust sampling and buffer:**
```properties
# Sampling strategy
agent.sample_n_per_3_secs=10  # 10 traces per 3 seconds
agent.ignore_suffix=.jpg,.jpeg,.js,.css,.png,.ico,.html

# Buffer size
buffer.channel_size=5000
buffer.buffer_size=3000

# Async reporter threads
reporter.max_buffer_size=3000
reporter.flush_interval=3
```

---

## Log Correlation (MDC Integration)

### 1. Add Logback Dependency

```gradle
dependencies {
    implementation 'org.apache.skywalking:apm-toolkit-logback-1.x:9.2.0'
}
```

### 2. Configure Logback Pattern

**logback-spring.xml:**
```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
            <layout class="org.apache.skywalking.apm.toolkit.log.logback.v1.x.TraceIdPatternLogbackLayout">
                <pattern>
                    %d{yyyy-MM-dd HH:mm:ss.SSS} [%tid] [%thread] %-5level %logger{36} - %msg%n
                </pattern>
            </layout>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="STDOUT" />
    </root>
</configuration>
```

**Pattern tokens:**
- `%tid` - Trace ID (distributed tracing ID)
- `%sw_ctx` - Full Skywalking context

### 3. Use in Code

```java
import org.apache.skywalking.apm.toolkit.trace.TraceContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    public ResponseDTO<OrderVO> getOrder(Long orderId) {
        // TraceId automatically added to logs
        log.info("Fetching order: {}", orderId);

        // Manually access TraceId if needed
        String traceId = TraceContext.traceId();
        log.info("Current TraceId: {}", traceId);

        // Business logic...
        return ResponseDTO.ok(orderVO);
    }
}
```

**Log output:**
```
2026-01-26 10:15:32.123 [TID:abc123def456] [http-nio-8080-exec-1] INFO  OrderService - Fetching order: 12345
2026-01-26 10:15:32.456 [TID:abc123def456] [http-nio-8080-exec-1] INFO  OrderService - Current TraceId: abc123def456
```

---

## Custom Spans (Manual Instrumentation)

### Basic Span Creation

```java
import org.apache.skywalking.apm.toolkit.trace.ActiveSpan;
import org.apache.skywalking.apm.toolkit.trace.Trace;

@Service
public class PaymentService {

    @Trace  // Auto-create span for this method
    public void processPayment(Long orderId, BigDecimal amount) {
        // Add custom tags to span
        ActiveSpan.tag("order.id", String.valueOf(orderId));
        ActiveSpan.tag("payment.amount", amount.toString());
        ActiveSpan.tag("payment.currency", "USD");

        // Add log event
        ActiveSpan.info("Payment validation started");

        // Business logic...

        ActiveSpan.info("Payment validation completed");
    }
}
```

### Advanced Span Nesting

```java
import org.apache.skywalking.apm.toolkit.trace.Span;

@Service
public class OrderManager {

    @Trace(operationName = "createOrder")
    public OrderEntity createOrder(OrderAddForm form) {
        ActiveSpan.tag("order.type", form.getOrderType());

        // Create nested span
        validateOrder(form);

        OrderEntity entity = saveOrder(form);

        // Send notification (async span)
        sendNotification(entity.getId());

        return entity;
    }

    @Trace(operationName = "validateOrder")
    private void validateOrder(OrderAddForm form) {
        ActiveSpan.tag("validation.step", "inventory_check");
        // Validation logic...
    }

    @Trace(operationName = "saveOrder")
    private OrderEntity saveOrder(OrderAddForm form) {
        ActiveSpan.tag("database.operation", "insert");
        // Save logic...
        return entity;
    }
}
```

---

## Plugin Configuration

### Database Query Tracing

```properties
# JDBC plugin
plugin.jdbc.trace_sql_parameters=true
plugin.jdbc.sql_parameters_max_length=512
plugin.jdbc.sql_body_max_length=2048

# MyBatis plugin (auto-enabled if MyBatis detected)
plugin.mybatis.trace_sql_parameters=true
```

### Redis Tracing

```properties
# Lettuce plugin (Spring Boot default)
plugin.lettuce.trace_redis_parameters=true

# Jedis plugin
plugin.jedis.trace_redis_parameters=true
```

### HTTP Client Tracing

```properties
# RestTemplate, WebClient, OkHttp auto-traced
plugin.http.http_params_length_threshold=2048
plugin.http.http_body_length_threshold=1024
```

---

## Environment-Specific Configuration

### Development Environment

```properties
agent.service_name=smartadmin-api-dev
collector.backend_service=localhost:11800
agent.sample_n_per_3_secs=-1  # Trace everything
logging.level=DEBUG
plugin.jdbc.trace_sql_parameters=true
```

### Production Environment

```properties
agent.service_name=smartadmin-api-prod
collector.backend_service=skywalking-oap-cluster:11800
agent.sample_n_per_3_secs=10  # Sample 10 per 3 seconds
logging.level=WARN
plugin.jdbc.trace_sql_parameters=false  # Security: don't log SQL params
```

---

## Verification

### Check Agent Status

**Log file:** `logs/skywalking-api.log`
```
2026-01-26 10:00:00 INFO [main] - Skywalking agent initialized successfully
2026-01-26 10:00:01 INFO [main] - Backend service: skywalking-oap:11800
2026-01-26 10:00:01 INFO [main] - Service name: smartadmin-api
```

### Verify in Skywalking UI

1. Open Skywalking UI: `http://skywalking-ui:8080`
2. Check **Dashboard** → Service: `smartadmin-api` should appear
3. Check **Topology** → Services should be connected
4. Check **Trace** → Recent traces should appear

### Test Trace Propagation

```bash
# Make API request
curl -X GET http://localhost:1024/api/order/12345 \
     -H "Content-Type: application/json"

# Check Skywalking UI → Trace
# Should see full trace with:
# - HTTP request span
# - Service method spans
# - Database query spans
# - Redis cache spans
```

---

## Troubleshooting

### Agent Not Connecting

**Check 1: Network connectivity**
```bash
telnet skywalking-oap 11800
# Should connect successfully
```

**Check 2: Agent loaded**
```bash
# In application logs, look for:
grep "Skywalking agent" logs/application.log
```

**Check 3: Backend service config**
```properties
# Verify correct OAP address
collector.backend_service=skywalking-oap:11800  # Correct
collector.backend_service=skywalking-oap:8080   # Wrong (UI port)
```

### No Traces Appearing

**Check sampling config:**
```properties
# -1 = trace everything (dev)
agent.sample_n_per_3_secs=-1

# 10 = sample 10 per 3 seconds (prod)
agent.sample_n_per_3_secs=10
```

**Force trace specific endpoints:**
```java
@Trace  // Always create span
public ResponseDTO<UserVO> getUser(Long userId) {
    // ...
}
```

### High Overhead

**Reduce sampling:**
```properties
agent.sample_n_per_3_secs=5  # Less aggressive sampling
```

**Disable SQL parameter logging:**
```properties
plugin.jdbc.trace_sql_parameters=false
```

**Ignore high-traffic endpoints:**
```properties
plugin.http.trace.ignore_path=/actuator/**,/metrics/**,/health/**
```

---

## Integration with SmartAdmin

### Service Layer Example

```java
package net.lab1024.sa.admin.module.business.order.service;

import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import org.apache.skywalking.apm.toolkit.trace.ActiveSpan;
import org.apache.skywalking.apm.toolkit.trace.Trace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderManager orderManager;

    @Trace(operationName = "OrderService.getOrder")
    public ResponseDTO<OrderVO> getOrder(Long orderId) {
        // Add custom tags
        ActiveSpan.tag("order.id", String.valueOf(orderId));
        ActiveSpan.tag("business.module", "order");

        log.info("Fetching order: {}", orderId);

        OrderEntity entity = orderManager.getById(orderId);
        if (entity == null) {
            ActiveSpan.tag("result.status", "not_found");
            return ResponseDTO.userErrorParam("Order not found");
        }

        OrderVO vo = SmartBeanUtil.copy(entity, OrderVO.class);
        ActiveSpan.tag("result.status", "success");

        return ResponseDTO.ok(vo);
    }
}
```

---

**Next:** [Micrometer Custom Metrics](micrometer-metrics.md)
**Related:** [Grafana Dashboards](grafana-dashboards.md)
**Version:** 1.0.0
**Last Updated:** 2026-01-26
