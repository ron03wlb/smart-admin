# Cache Warming Strategies Guide

**Skill:** cache-strategy-generator
**Component:** Cache Warming & Preloading
**Purpose:** Proactive cache population for optimal performance

---

## Why Cache Warming?

**Problem:** Cold cache after restart causes:
- Slow initial requests (cache miss storms)
- Database overload
- Poor user experience

**Solution:** Preload hot data into cache

---

## Strategy 1: Startup Warming

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheWarmingService {

    private final ProductDao productDao;
    private final Cache<Long, ProductEntity> l1Cache;
    private final RedisTemplate<String, ProductEntity> redisTemplate;

    @PostConstruct
    @Async
    public void warmCache() {
        log.info("Starting cache warming...");
        long startTime = System.currentTimeMillis();

        // Load hot products (most viewed)
        List<ProductEntity> hotProducts = productDao.selectHotProducts(1000);

        for (ProductEntity product : hotProducts) {
            l1Cache.put(product.getProductId(), product);
            redisTemplate.opsForValue().set(
                "product:" + product.getProductId(),
                product,
                Duration.ofMinutes(30)
            );
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Cache warming completed: {} products in {}ms",
                 hotProducts.size(), duration);
    }
}
```

---

## Strategy 2: Scheduled Warming

```java
@Component
@RequiredArgsConstructor
public class ScheduledCacheWarmer {

    @Scheduled(cron = "0 0 2 * * ?")  // 2 AM daily
    public void dailyWarm() {
        log.info("Starting scheduled cache warming...");

        // Load today's featured products
        List<ProductEntity> featured = productDao.selectFeaturedProducts();
        featured.forEach(this::warmProduct);

        // Load upcoming promotions
        List<ProductEntity> promotions = productDao.selectPromotionProducts();
        promotions.forEach(this::warmProduct);
    }

    private void warmProduct(ProductEntity product) {
        l1Cache.put(product.getProductId(), product);
        redisTemplate.opsForValue().set(
            "product:" + product.getProductId(),
            product,
            Duration.ofHours(24)
        );
    }
}
```

---

## Strategy 3: Predictive Warming

```java
@Component
@RequiredArgsConstructor
public class PredictiveCacheWarmer {

    /**
     * Warm related products when one is viewed
     */
    public void warmRelatedProducts(Long productId) {
        CompletableFuture.runAsync(() -> {
            // Get related products
            List<Long> relatedIds = productDao.selectRelatedProductIds(productId);

            // Warm in background
            relatedIds.forEach(id -> {
                ProductEntity product = productDao.selectById(id);
                if (product != null) {
                    l1Cache.put(id, product);
                    redisTemplate.opsForValue().set(
                        "product:" + id,
                        product,
                        Duration.ofMinutes(15)
                    );
                }
            });
        });
    }
}
```

---

## Best Practices

1. **Warm async:** Don't block startup
   ```java
   @Async
   @PostConstruct
   public void warmCache() { /*...*/ }
   ```

2. **Warm hot data only:** Not entire dataset
   ```java
   // ✅ Top 1000 products
   productDao.selectHotProducts(1000);

   // ❌ All 1M products
   productDao.selectAll();
   ```

3. **Monitor warming:** Track time and success
   ```java
   Timer.builder("smartadmin.cache.warming.duration")
        .register(meterRegistry)
        .record(() -> warmCache());
   ```

---

**Next:** [Monitoring](monitoring.md)
**Related:** [Cache Patterns](cache-patterns.md)
**Version:** 1.0.0
**Last Updated:** 2026-01-26
