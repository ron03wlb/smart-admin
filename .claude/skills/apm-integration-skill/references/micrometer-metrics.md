# Micrometer Custom Metrics Guide

**Skill:** apm-integration-skill
**Component:** Micrometer + Spring Boot Actuator + Prometheus
**Purpose:** Custom business metrics and application monitoring

---

## Quick Start

### 1. Add Dependencies

**sa-admin/build.gradle:**
```gradle
dependencies {
    // Spring Boot Actuator (includes Micrometer)
    implementation 'org.springframework.boot:spring-boot-starter-actuator'

    // Prometheus registry
    implementation 'io.micrometer:micrometer-registry-prometheus'

    // Optional: Influx registry (time-series DB)
    implementation 'io.micrometer:micrometer-registry-influx'
}
```

### 2. Enable Actuator Endpoints

**application.yml:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator

  endpoint:
    health:
      show-details: always
    metrics:
      enabled: true
    prometheus:
      enabled: true

  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
      environment: ${SPRING_PROFILES_ACTIVE:dev}
      region: ${REGION:us-east-1}
```

### 3. Verify Endpoints

```bash
# Health check
curl http://localhost:1024/actuator/health

# All metrics
curl http://localhost:1024/actuator/metrics

# Prometheus format (for Grafana)
curl http://localhost:1024/actuator/prometheus
```

---

## Counter Metrics

**Use case:** Count events (orders created, users registered, errors occurred)

### Basic Counter

```java
package net.lab1024.sa.admin.module.business.order.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderManager orderManager;
    private final MeterRegistry meterRegistry;

    public ResponseDTO<OrderVO> createOrder(OrderAddForm form) {
        // Create order
        OrderEntity entity = orderManager.save(form);

        // Increment counter
        Counter.builder("smartadmin.order.created")
                .description("Total orders created")
                .tag("order_type", form.getOrderType())
                .tag("payment_method", form.getPaymentMethod())
                .register(meterRegistry)
                .increment();

        return ResponseDTO.ok(SmartBeanUtil.copy(entity, OrderVO.class));
    }
}
```

### Counter with Conditional Logic

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final MeterRegistry meterRegistry;

    public ResponseDTO<UserVO> register(UserRegisterForm form) {
        try {
            UserEntity user = createUser(form);

            // Success counter
            Counter.builder("smartadmin.user.register.success")
                    .description("Successful user registrations")
                    .tag("source", form.getSource())
                    .register(meterRegistry)
                    .increment();

            return ResponseDTO.ok(userVO);

        } catch (Exception e) {
            // Failure counter
            Counter.builder("smartadmin.user.register.failure")
                    .description("Failed user registrations")
                    .tag("source", form.getSource())
                    .tag("error_type", e.getClass().getSimpleName())
                    .register(meterRegistry)
                    .increment();

            throw e;
        }
    }
}
```

---

## Gauge Metrics

**Use case:** Track current values (active users, queue size, cache size)

### Simple Gauge

```java
@Service
@RequiredArgsConstructor
public class ConnectionPoolMonitor {

    private final MeterRegistry meterRegistry;
    private final DataSource dataSource;

    @PostConstruct
    public void registerMetrics() {
        // Database connection pool size
        Gauge.builder("smartadmin.db.pool.active", dataSource, ds -> {
                    HikariDataSource hikari = (HikariDataSource) ds;
                    return hikari.getHikariPoolMXBean().getActiveConnections();
                })
                .description("Active database connections")
                .register(meterRegistry);

        Gauge.builder("smartadmin.db.pool.idle", dataSource, ds -> {
                    HikariDataSource hikari = (HikariDataSource) ds;
                    return hikari.getHikariPoolMXBean().getIdleConnections();
                })
                .description("Idle database connections")
                .register(meterRegistry);
    }
}
```

### Gauge with Supplier

```java
@Service
@RequiredArgsConstructor
public class CacheMonitor {

    private final MeterRegistry meterRegistry;
    private final CaffeineCache cache;

    @PostConstruct
    public void registerMetrics() {
        // Cache size gauge
        Gauge.builder("smartadmin.cache.size", cache, Cache::estimatedSize)
                .description("Current cache size")
                .tag("cache_name", "productCache")
                .register(meterRegistry);

        // Cache hit rate gauge
        Gauge.builder("smartadmin.cache.hit_rate", cache, c -> {
                    CacheStats stats = c.stats();
                    return stats.hitRate();
                })
                .description("Cache hit rate")
                .tag("cache_name", "productCache")
                .register(meterRegistry);
    }
}
```

### Collection Size Gauge

```java
@Service
public class QueueMonitor {

    private final MeterRegistry meterRegistry;
    private final BlockingQueue<OrderEvent> orderQueue;

    @PostConstruct
    public void registerMetrics() {
        // Queue size
        Gauge.builder("smartadmin.queue.size", orderQueue, Queue::size)
                .description("Order event queue size")
                .register(meterRegistry);

        // Queue capacity
        Gauge.builder("smartadmin.queue.capacity", orderQueue, q -> {
                    return q.size() + q.remainingCapacity();
                })
                .description("Order event queue capacity")
                .register(meterRegistry);
    }
}
```

---

## Timer Metrics

**Use case:** Measure duration and frequency (request latency, processing time)

### Basic Timer

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final MeterRegistry meterRegistry;

    public ResponseDTO<OrderVO> getOrder(Long orderId) {
        // Wrap method execution in timer
        return Timer.builder("smartadmin.order.get")
                .description("Time to fetch order")
                .tag("operation", "getById")
                .register(meterRegistry)
                .record(() -> {
                    // Business logic here
                    OrderEntity entity = orderManager.getById(orderId);
                    return ResponseDTO.ok(SmartBeanUtil.copy(entity, OrderVO.class));
                });
    }
}
```

### Timer with Sample

```java
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final MeterRegistry meterRegistry;

    public ResponseDTO<PaymentVO> processPayment(PaymentForm form) {
        // Start timer
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            // Process payment
            PaymentVO result = executePayment(form);

            // Stop timer with success tag
            sample.stop(Timer.builder("smartadmin.payment.process")
                    .description("Payment processing time")
                    .tag("payment_method", form.getPaymentMethod())
                    .tag("status", "success")
                    .register(meterRegistry));

            return ResponseDTO.ok(result);

        } catch (Exception e) {
            // Stop timer with failure tag
            sample.stop(Timer.builder("smartadmin.payment.process")
                    .description("Payment processing time")
                    .tag("payment_method", form.getPaymentMethod())
                    .tag("status", "failure")
                    .tag("error_type", e.getClass().getSimpleName())
                    .register(meterRegistry));

            throw e;
        }
    }
}
```

### Timer via @Timed Annotation

```java
@Service
public class OrderManager {

    @Timed(value = "smartadmin.order.save", description = "Time to save order to database")
    public OrderEntity save(OrderAddForm form) {
        // Business logic...
        return orderDao.insert(entity);
    }

    @Timed(
        value = "smartadmin.order.query",
        description = "Time to query orders",
        extraTags = {"operation", "list"}
    )
    public List<OrderEntity> listByStatus(Integer status) {
        // Query logic...
        return orderDao.selectByStatus(status);
    }
}
```

**Enable @Timed annotation:**
```java
@Configuration
public class MetricsConfiguration {

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}
```

---

## Distribution Summary

**Use case:** Track distribution of values (order amounts, file sizes, batch sizes)

### Basic Summary

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final MeterRegistry meterRegistry;

    public ResponseDTO<OrderVO> createOrder(OrderAddForm form) {
        OrderEntity entity = orderManager.save(form);

        // Record order amount distribution
        DistributionSummary.builder("smartadmin.order.amount")
                .description("Order amount distribution")
                .baseUnit("USD")
                .tag("order_type", form.getOrderType())
                .register(meterRegistry)
                .record(form.getTotalAmount().doubleValue());

        return ResponseDTO.ok(orderVO);
    }
}
```

### Summary with Percentiles

```java
@Service
@RequiredArgsConstructor
public class FileService {

    private final MeterRegistry meterRegistry;

    @PostConstruct
    public void init() {
        // Track file upload sizes
        DistributionSummary.builder("smartadmin.file.upload.size")
                .description("Uploaded file size distribution")
                .baseUnit("bytes")
                .publishPercentiles(0.5, 0.95, 0.99)  // P50, P95, P99
                .minimumExpectedValue(1024.0)          // 1KB min
                .maximumExpectedValue(10485760.0)      // 10MB max
                .register(meterRegistry);
    }

    public ResponseDTO<String> uploadFile(MultipartFile file) {
        // Record file size
        meterRegistry.summary("smartadmin.file.upload.size")
                .record(file.getSize());

        // Upload logic...
        return ResponseDTO.ok(fileUrl);
    }
}
```

---

## Custom Composite Metrics

### Health Score Calculator

```java
@Component
@RequiredArgsConstructor
public class SystemHealthMonitor {

    private final MeterRegistry meterRegistry;
    private final DataSource dataSource;
    private final RedisTemplate<String, String> redisTemplate;

    @Scheduled(fixedDelay = 60000)  // Every minute
    public void calculateHealthScore() {
        double healthScore = 100.0;

        // Check database health
        if (!isDatabaseHealthy()) {
            healthScore -= 40.0;
        }

        // Check Redis health
        if (!isRedisHealthy()) {
            healthScore -= 30.0;
        }

        // Check memory usage
        Runtime runtime = Runtime.getRuntime();
        double memoryUsage = (runtime.totalMemory() - runtime.freeMemory())
                           / (double) runtime.maxMemory();
        if (memoryUsage > 0.9) {
            healthScore -= 20.0;
        } else if (memoryUsage > 0.8) {
            healthScore -= 10.0;
        }

        // Record health score
        Gauge.builder("smartadmin.system.health_score", () -> healthScore)
                .description("Overall system health score (0-100)")
                .register(meterRegistry);
    }

    private boolean isDatabaseHealthy() {
        try {
            HikariDataSource hikari = (HikariDataSource) dataSource;
            return hikari.getHikariPoolMXBean().getActiveConnections()
                   < hikari.getMaximumPoolSize();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isRedisHealthy() {
        try {
            redisTemplate.getConnectionFactory()
                        .getConnection()
                        .ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

---

## Business Metrics Examples

### E-commerce Metrics

```java
@Service
@RequiredArgsConstructor
public class OrderMetrics {

    private final MeterRegistry meterRegistry;

    public void recordOrderCreated(OrderEntity order) {
        // Order count
        Counter.builder("smartadmin.business.order.created")
                .tag("payment_method", order.getPaymentMethod())
                .tag("order_status", order.getStatus().toString())
                .register(meterRegistry)
                .increment();

        // Order revenue
        DistributionSummary.builder("smartadmin.business.revenue")
                .baseUnit("USD")
                .tag("payment_method", order.getPaymentMethod())
                .register(meterRegistry)
                .record(order.getTotalAmount().doubleValue());

        // Order items count
        DistributionSummary.builder("smartadmin.business.order.items")
                .description("Number of items per order")
                .tag("order_type", order.getOrderType())
                .register(meterRegistry)
                .record(order.getItemCount());
    }

    public void recordPaymentCompleted(PaymentEntity payment) {
        // Payment success rate
        Counter.builder("smartadmin.business.payment.completed")
                .tag("payment_method", payment.getPaymentMethod())
                .tag("status", payment.getStatus())
                .register(meterRegistry)
                .increment();

        // Payment processing time
        Timer.builder("smartadmin.business.payment.duration")
                .description("Time from order to payment completion")
                .tag("payment_method", payment.getPaymentMethod())
                .register(meterRegistry)
                .record(Duration.between(payment.getCreatedAt(), payment.getCompletedAt()));
    }
}
```

### User Activity Metrics

```java
@Service
@RequiredArgsConstructor
public class UserActivityMetrics {

    private final MeterRegistry meterRegistry;

    public void recordLogin(Long userId, String loginMethod) {
        // Login count
        Counter.builder("smartadmin.user.login")
                .tag("method", loginMethod)
                .register(meterRegistry)
                .increment();

        // Daily active users (stored in cache, updated here)
        // This would typically use a Set in Redis to track unique users
    }

    public void recordFeatureUsage(String featureName, Long userId) {
        // Feature usage count
        Counter.builder("smartadmin.feature.usage")
                .tag("feature", featureName)
                .tag("user_type", getUserType(userId))
                .register(meterRegistry)
                .increment();
    }

    @Scheduled(fixedDelay = 300000)  // Every 5 minutes
    public void recordActiveUserCount() {
        // Get from Redis/Cache
        long activeUserCount = getActiveUserCountFromCache();

        Gauge.builder("smartadmin.user.active_count", () -> activeUserCount)
                .description("Currently active user count")
                .register(meterRegistry);
    }
}
```

---

## Global Metrics Configuration

### Central Metrics Configuration

```java
package net.lab1024.sa.base.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfiguration {

    @Bean
    public MeterRegistry meterRegistryCustomizer(MeterRegistry registry) {
        // Add common tags to all metrics
        registry.config()
                .commonTags("application", "smartadmin")
                .commonTags("version", "4.0.0")
                .commonTags("environment", System.getenv().getOrDefault("ENV", "dev"));

        // Register JVM metrics
        new ClassLoaderMetrics().bindTo(registry);
        new JvmMemoryMetrics().bindTo(registry);
        new JvmGcMetrics().bindTo(registry);
        new JvmThreadMetrics().bindTo(registry);
        new ProcessorMetrics().bindTo(registry);
        new UptimeMetrics().bindTo(registry);

        return registry;
    }
}
```

---

## Querying Metrics

### Via REST API

```bash
# Get all metric names
curl http://localhost:1024/actuator/metrics

# Get specific metric
curl http://localhost:1024/actuator/metrics/smartadmin.order.created

# Get metric with tags
curl http://localhost:1024/actuator/metrics/smartadmin.order.created?tag=order_type:ONLINE
```

### Via Prometheus Format

```bash
# Prometheus format (all metrics)
curl http://localhost:1024/actuator/prometheus

# Example output:
# smartadmin_order_created_total{order_type="ONLINE",payment_method="WECHAT"} 1234.0
# smartadmin_order_amount_sum{order_type="ONLINE"} 56789.50
# smartadmin_order_amount_count{order_type="ONLINE"} 1234
```

---

## Best Practices

### Naming Conventions

```java
// Use consistent naming: <namespace>.<component>.<metric>.<unit>
Counter.builder("smartadmin.order.created.total")      // Good
Counter.builder("orders_created")                       // Bad - no namespace
Counter.builder("smartadmin_order_creation_counter")    // Bad - redundant

// Use tags for dimensions
Counter.builder("smartadmin.order.created")
       .tag("order_type", "ONLINE")                     // Good - use tags
       .register(registry);

Counter.builder("smartadmin.order.created.online")      // Bad - dimension in name
```

### Performance Considerations

```java
// ❌ BAD: Creating new meter each time
public void processOrder(Order order) {
    Counter.builder("smartadmin.order.processed")
           .register(meterRegistry)    // Meter created every call!
           .increment();
}

// ✅ GOOD: Reuse meter
private final Counter orderCounter;

@PostConstruct
public void init() {
    orderCounter = Counter.builder("smartadmin.order.processed")
                          .register(meterRegistry);
}

public void processOrder(Order order) {
    orderCounter.increment();  // Reuse existing meter
}
```

### Tag Cardinality

```java
// ❌ BAD: High cardinality (unique user IDs)
Counter.builder("smartadmin.user.action")
       .tag("user_id", userId.toString())  // Millions of unique values!
       .register(registry);

// ✅ GOOD: Low cardinality (user types)
Counter.builder("smartadmin.user.action")
       .tag("user_type", getUserType(userId))  // Few unique values (VIP, NORMAL, etc.)
       .register(registry);
```

---

**Next:** [Grafana Dashboards](grafana-dashboards.md)
**Related:** [Skywalking Configuration](skywalking-configuration.md), [Alert Rules](alert-rules.md)
**Version:** 1.0.0
**Last Updated:** 2026-01-26
