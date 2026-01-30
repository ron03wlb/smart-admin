# Cache Invalidation Strategies Guide

**Skill:** cache-strategy-generator
**Component:** Cache Invalidation & Consistency
**Purpose:** Systematic cache invalidation patterns for data consistency

---

## Strategy Overview

### Cache Invalidation Challenges

**Phil Karlton's famous quote:**
> "There are only two hard things in Computer Science: cache invalidation and naming things."

**Common Problems:**
- Stale data served to users
- Cache-database inconsistency
- Over-invalidation (performance impact)
- Under-invalidation (correctness issues)

---

## Strategy 1: TTL-Based Invalidation

**Use Case:** Simple time-based expiration for non-critical data

### Basic TTL Configuration

```java
@Component
public class TTLCacheManager {

    // L1 Cache (Caffeine) with TTL
    private final Cache<Long, ProductEntity> l1Cache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)  // Absolute TTL
            .maximumSize(10_000)
            .build();

    // L2 Cache (Redis) with TTL
    @Autowired
    private RedisTemplate<String, ProductEntity> redisTemplate;

    public void cache(Long productId, ProductEntity product) {
        // L1: 5 minutes
        l1Cache.put(productId, product);

        // L2: 30 minutes
        redisTemplate.opsForValue().set(
            "product:" + productId,
            product,
            Duration.ofMinutes(30)
        );
    }
}
```

### Sliding Window TTL

```java
// Caffeine: Expire after last access
private final Cache<Long, ProductEntity> slidingCache = Caffeine.newBuilder()
        .expireAfterAccess(10, TimeUnit.MINUTES)  // Reset on access
        .maximumSize(10_000)
        .build();

// Redis: Manually reset TTL on access
public ProductEntity getWithSlidingTTL(Long productId) {
    String key = "product:" + productId;
    ProductEntity product = redisTemplate.opsForValue().get(key);

    if (product != null) {
        // Reset TTL on access
        redisTemplate.expire(key, Duration.ofMinutes(30));
    }

    return product;
}
```

**TTL Guidelines:**

| Data Type | L1 TTL | L2 TTL | Rationale |
|-----------|--------|--------|-----------|
| Hot data (product list) | 5 min | 30 min | Frequently accessed |
| Warm data (user profile) | 10 min | 1 hour | Moderate access |
| Cold data (settings) | 1 hour | 24 hours | Rarely changes |
| Critical data (price) | 1 min | 5 min | Needs freshness |

---

## Strategy 2: Event-Driven Invalidation

**Use Case:** Immediate invalidation on data changes (strong consistency)

### Database Trigger Approach

```java
@Component
@RequiredArgsConstructor
public class EventDrivenCacheManager {

    private final Cache<Long, ProductEntity> l1Cache;
    private final RedisTemplate<String, ProductEntity> redisTemplate;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Update product and publish invalidation event
     */
    @Transactional
    public void updateProduct(ProductEntity product) {
        // 1. Update database
        productDao.updateById(product);

        // 2. Publish cache invalidation event
        eventPublisher.publishEvent(
            new CacheInvalidationEvent(this, "product", product.getProductId())
        );
    }

    /**
     * Listen for invalidation events
     */
    @EventListener
    public void handleInvalidation(CacheInvalidationEvent event) {
        if ("product".equals(event.getCacheType())) {
            Long productId = event.getEntityId();

            // Invalidate L1
            l1Cache.invalidate(productId);

            // Invalidate L2
            redisTemplate.delete("product:" + productId);

            log.info("Cache invalidated for product: {}", productId);
        }
    }
}

// Event class
@Getter
public class CacheInvalidationEvent extends ApplicationEvent {
    private final String cacheType;
    private final Long entityId;

    public CacheInvalidationEvent(Object source, String cacheType, Long entityId) {
        super(source);
        this.cacheType = cacheType;
        this.entityId = entityId;
    }
}
```

### Kafka-Based Distributed Invalidation

```java
@Component
@RequiredArgsConstructor
public class KafkaCacheInvalidator {

    private final KafkaTemplate<String, CacheInvalidationMessage> kafkaTemplate;
    private final Cache<Long, ProductEntity> l1Cache;
    private final RedisTemplate<String, ProductEntity> redisTemplate;

    /**
     * Publish invalidation message to Kafka
     */
    public void invalidate(String cacheType, Long entityId) {
        CacheInvalidationMessage message = new CacheInvalidationMessage(
            cacheType,
            entityId,
            System.currentTimeMillis()
        );

        kafkaTemplate.send("cache-invalidation", message);
        log.info("Published cache invalidation: {}", message);
    }

    /**
     * Consume invalidation messages
     */
    @KafkaListener(topics = "cache-invalidation", groupId = "smartadmin-api")
    public void consumeInvalidation(CacheInvalidationMessage message) {
        if ("product".equals(message.getCacheType())) {
            Long productId = message.getEntityId();

            // Invalidate local caches
            l1Cache.invalidate(productId);
            redisTemplate.delete("product:" + productId);

            log.info("Consumed cache invalidation: {}", message);
        }
    }
}
```

---

## Strategy 3: Manual Invalidation with Patterns

**Use Case:** Fine-grained control over invalidation logic

### Single Key Invalidation

```java
@Component
@RequiredArgsConstructor
public class ManualInvalidationManager {

    private final Cache<Long, ProductEntity> l1Cache;
    private final RedisTemplate<String, ProductEntity> redisTemplate;

    /**
     * Invalidate single entry
     */
    public void invalidateSingle(Long productId) {
        l1Cache.invalidate(productId);
        redisTemplate.delete("product:" + productId);
    }

    /**
     * Invalidate multiple entries
     */
    public void invalidateMultiple(Collection<Long> productIds) {
        // L1: Batch invalidation
        l1Cache.invalidateAll(productIds);

        // L2: Batch delete
        List<String> keys = productIds.stream()
                .map(id -> "product:" + id)
                .collect(Collectors.toList());
        redisTemplate.delete(keys);
    }

    /**
     * Invalidate all entries
     */
    public void invalidateAll() {
        l1Cache.invalidateAll();

        // Redis: Delete by pattern
        Set<String> keys = redisTemplate.keys("product:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
```

### Pattern-Based Invalidation

```java
/**
 * Invalidate by category (all products in category)
 */
public void invalidateByCategory(Long categoryId) {
    // Get all product IDs in category
    List<Long> productIds = productDao.selectIdsByCategoryId(categoryId);

    // Invalidate all
    invalidateMultiple(productIds);
}

/**
 * Invalidate by tag/label
 */
public void invalidateByTag(String tag) {
    // Redis: Delete keys by pattern
    Set<String> keys = redisTemplate.keys("product:*:tag:" + tag);
    if (keys != null && !keys.isEmpty()) {
        redisTemplate.delete(keys);
    }
}
```

---

## Strategy 4: Cache Tag Groups

**Use Case:** Group-based invalidation for related data

### Implementation with Redis Sets

```java
@Component
@RequiredArgsConstructor
public class CacheTagManager {

    private final RedisTemplate<String, String> redisTemplate;
    private final Cache<Long, ProductEntity> l1Cache;

    /**
     * Cache with tags
     */
    public void cacheWithTags(Long productId, ProductEntity product, String... tags) {
        String cacheKey = "product:" + productId;

        // 1. Cache data
        redisTemplate.opsForValue().set(cacheKey, product, Duration.ofMinutes(30));
        l1Cache.put(productId, product);

        // 2. Add to tag sets
        for (String tag : tags) {
            String tagKey = "tag:" + tag;
            redisTemplate.opsForSet().add(tagKey, cacheKey);
            // Set TTL for tag set
            redisTemplate.expire(tagKey, Duration.ofHours(1));
        }
    }

    /**
     * Invalidate by tag
     */
    public void invalidateByTag(String tag) {
        String tagKey = "tag:" + tag;

        // Get all cache keys for this tag
        Set<String> cacheKeys = redisTemplate.opsForSet().members(tagKey);

        if (cacheKeys != null && !cacheKeys.isEmpty()) {
            // Invalidate all associated caches
            for (String cacheKey : cacheKeys) {
                // Extract product ID from key
                Long productId = extractProductId(cacheKey);
                if (productId != null) {
                    l1Cache.invalidate(productId);
                }
            }

            // Delete cache keys from Redis
            redisTemplate.delete(cacheKeys);

            // Delete tag set
            redisTemplate.delete(tagKey);

            log.info("Invalidated {} entries for tag: {}", cacheKeys.size(), tag);
        }
    }

    private Long extractProductId(String cacheKey) {
        // Extract ID from "product:123" format
        String[] parts = cacheKey.split(":");
        return parts.length >= 2 ? Long.parseLong(parts[1]) : null;
    }
}
```

### Example Usage

```java
@Service
@RequiredArgsConstructor
public class ProductService {

    private final CacheTagManager cacheTagManager;

    public ResponseDTO<ProductVO> createProduct(ProductAddForm form) {
        ProductEntity entity = productManager.save(form);

        // Cache with tags
        cacheTagManager.cacheWithTags(
            entity.getProductId(),
            entity,
            "category:" + entity.getCategoryId(),
            "brand:" + entity.getBrandId(),
            "new-arrivals"
        );

        return ResponseDTO.ok(vo);
    }

    public ResponseDTO<Void> updateCategory(Long categoryId) {
        // Update category...

        // Invalidate all products in this category
        cacheTagManager.invalidateByTag("category:" + categoryId);

        return ResponseDTO.ok();
    }
}
```

---

## Strategy 5: Versioned Cache Keys

**Use Case:** Avoid race conditions, support atomic updates

### Version-Based Invalidation

```java
@Component
@RequiredArgsConstructor
public class VersionedCacheManager {

    private final RedisTemplate<String, Object> redisTemplate;
    private final AtomicLong versionCounter = new AtomicLong(0);

    /**
     * Cache with version
     */
    public void cacheWithVersion(Long productId, ProductEntity product) {
        long version = versionCounter.incrementAndGet();
        String versionKey = "product:version:" + productId;
        String cacheKey = "product:" + productId + ":v" + version;

        // Store data with versioned key
        redisTemplate.opsForValue().set(cacheKey, product, Duration.ofMinutes(30));

        // Update version pointer
        redisTemplate.opsForValue().set(versionKey, version, Duration.ofMinutes(30));
    }

    /**
     * Get current version
     */
    public ProductEntity getWithVersion(Long productId) {
        String versionKey = "product:version:" + productId;
        Long version = (Long) redisTemplate.opsForValue().get(versionKey);

        if (version == null) {
            return null;
        }

        String cacheKey = "product:" + productId + ":v" + version;
        return (ProductEntity) redisTemplate.opsForValue().get(cacheKey);
    }

    /**
     * Invalidate by incrementing version (no actual deletion)
     */
    public void invalidate(Long productId) {
        // Just update version pointer - old cache expires naturally
        long newVersion = versionCounter.incrementAndGet();
        String versionKey = "product:version:" + productId;
        redisTemplate.opsForValue().set(versionKey, newVersion, Duration.ofMinutes(30));

        log.info("Invalidated product {} by version bump: {}", productId, newVersion);
    }
}
```

---

## Strategy 6: Consistency Patterns

### Eventually Consistent (Default)

```java
@Transactional
public void updateProduct(ProductEntity product) {
    // 1. Update database
    productDao.updateById(product);

    // 2. Invalidate cache (async acceptable)
    CompletableFuture.runAsync(() -> {
        cacheManager.invalidate(product.getProductId());
    });

    // Small window of inconsistency acceptable
}
```

### Strong Consistency (2PC-like)

```java
@Transactional
public void updateProductStrongConsistency(ProductEntity product) {
    // 1. Invalidate cache FIRST
    cacheManager.invalidate(product.getProductId());

    // 2. Update database
    productDao.updateById(product);

    // Cache miss will fetch fresh data from DB
    // No stale data served
}
```

### Read-Your-Writes Consistency

```java
@Transactional
public void updateWithReadYourWrites(ProductEntity product) {
    // 1. Update database
    productDao.updateById(product);

    // 2. Invalidate old cache
    cacheManager.invalidate(product.getProductId());

    // 3. Immediately populate with new data
    cacheManager.cache(product.getProductId(), product);

    // User immediately sees their changes
}
```

---

## Best Practices

### DO's ✅

1. **Use TTL as backup:** Even with manual invalidation, set TTL
   ```java
   // Always set TTL, even if invalidating manually
   redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(30));
   ```

2. **Invalidate conservatively:** When in doubt, invalidate
   ```java
   // Safer: Invalidate related data
   cacheManager.invalidate(productId);
   cacheManager.invalidateByCategory(product.getCategoryId());
   ```

3. **Log invalidations:** Track invalidation patterns
   ```java
   log.info("Cache invalidated: type={}, id={}, reason={}", type, id, reason);
   ```

4. **Monitor invalidation rate:** Too high = problem
   ```java
   Counter.builder("smartadmin.cache.invalidations")
          .tag("cache_type", "product")
          .register(meterRegistry)
          .increment();
   ```

### DON'Ts ❌

1. **Don't invalidate in loops:** Batch instead
   ```java
   // ❌ BAD
   for (Long id : productIds) {
       cacheManager.invalidate(id);
   }

   // ✅ GOOD
   cacheManager.invalidateMultiple(productIds);
   ```

2. **Don't forget distributed invalidation:** Multi-instance deployments
   ```java
   // ❌ BAD: Only invalidates local L1 cache
   l1Cache.invalidate(id);

   // ✅ GOOD: Invalidates L1 + L2 (reaches all instances)
   l1Cache.invalidate(id);
   redisTemplate.delete("product:" + id);
   ```

3. **Don't over-invalidate:** Hurts performance
   ```java
   // ❌ BAD: Invalidates all products on any change
   cacheManager.invalidateAll();

   // ✅ GOOD: Invalidates specific product
   cacheManager.invalidate(productId);
   ```

---

## Invalidation Decision Tree

```
Is data critical (financial, inventory)?
├─ YES → Use strong consistency (invalidate before write)
└─ NO → Is eventual consistency acceptable?
    ├─ YES → Use TTL + event-driven invalidation
    └─ NO → Are changes frequent?
        ├─ YES → Use short TTL (1-5 min)
        └─ NO → Use longer TTL (30+ min) + manual invalidation
```

---

**Next:** [Cache Warming](cache-warming.md)
**Related:** [Cache Patterns](cache-patterns.md), [Monitoring](monitoring.md)
**Version:** 1.0.0
**Last Updated:** 2026-01-26
