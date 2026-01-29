# Cache Monitoring & Observability Guide

**Skill:** cache-strategy-generator
**Component:** Cache Metrics & Monitoring
**Purpose:** Track cache performance and health

---

## Key Metrics

### 1. Hit Rate

```java
@Component
@RequiredArgsConstructor
public class CacheMetrics {

    private final MeterRegistry meterRegistry;

    @Scheduled(fixedDelay = 60000)  // Every minute
    public void recordCaffeineStats() {
        CacheStats stats = l1Cache.stats();

        // Hit rate
        Gauge.builder("smartadmin.cache.hit_rate", stats, CacheStats::hitRate)
                .tag("cache", "l1")
                .tag("type", "product")
                .register(meterRegistry);

        // Miss rate
        Gauge.builder("smartadmin.cache.miss_rate", stats, CacheStats::missRate)
                .tag("cache", "l1")
                .register(meterRegistry);

        // Load success rate
        Gauge.builder("smartadmin.cache.load_success_rate", stats, s ->
                s.loadSuccessCount() / (double) s.loadCount())
                .tag("cache", "l1")
                .register(meterRegistry);
    }
}
```

### 2. Latency

```java
@Service
@RequiredArgsConstructor
public class MonitoredCacheService {

    private final MeterRegistry meterRegistry;

    public ProductEntity getProduct(Long id) {
        Timer.Sample sample = Timer.start(meterRegistry);

        ProductEntity product = cacheManager.getById(id);

        sample.stop(Timer.builder("smartadmin.cache.get.duration")
                .tag("cache_hit", product != null ? "true" : "false")
                .register(meterRegistry));

        return product;
    }
}
```

### 3. Size & Evictions

```java
// Cache size
Gauge.builder("smartadmin.cache.size", l1Cache, Cache::estimatedSize)
        .register(meterRegistry);

// Eviction count
Gauge.builder("smartadmin.cache.evictions", stats, CacheStats::evictionCount)
        .register(meterRegistry);
```

---

## Grafana Dashboard Queries

### Hit Rate
```promql
smartadmin_cache_hit_rate{cache="l1",type="product"}
```

### Average Load Time
```promql
rate(smartadmin_cache_get_duration_sum[5m]) /
rate(smartadmin_cache_get_duration_count[5m])
```

### Cache Efficiency
```promql
(smartadmin_cache_hit_rate * 100) > 90
```

---

## Alert Rules

```yaml
# Low hit rate alert
alert: LowCacheHitRate
expr: smartadmin_cache_hit_rate < 0.8
for: 10m
annotations:
  summary: "Cache hit rate below 80%"

# High eviction rate alert
alert: HighCacheEvictions
expr: rate(smartadmin_cache_evictions[5m]) > 100
for: 5m
annotations:
  summary: "High cache eviction rate"
```

---

## Logging Best Practices

```java
// Log cache operations
log.debug("Cache hit: type={}, id={}", "product", productId);
log.debug("Cache miss: type={}, id={}", "product", productId);
log.info("Cache invalidated: type={}, id={}, reason={}",
         "product", productId, "update");

// Log warming
log.info("Cache warming: loaded {} entries in {}ms", count, duration);
```

---

**Related:** [Cache Patterns](cache-patterns.md), [Invalidation Strategies](invalidation-strategies.md)
**Version:** 1.0.0
**Last Updated:** 2026-01-26
