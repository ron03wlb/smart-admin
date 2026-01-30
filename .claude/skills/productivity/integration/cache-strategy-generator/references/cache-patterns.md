# Multi-Level Cache Patterns Guide

**Skill:** cache-strategy-generator
**Component:** Caffeine (L1) + Redis (L2) Multi-Level Cache
**Purpose:** Systematic caching strategies for performance optimization

---

## Architecture Overview

### Two-Level Cache Strategy

```
Request → L1 Cache (Caffeine) → L2 Cache (Redis) → Database
          ↓ Hit (fastest)      ↓ Hit (fast)      ↓ Miss (slow)
          Return immediately    Return & populate L1  Query & populate L1+L2
```

**Benefits:**
- ✅ L1 (Caffeine): Ultra-fast in-memory access (~1μs)
- ✅ L2 (Redis): Fast distributed cache (~1ms)
- ✅ Reduced database load by 80%+
- ✅ Horizontal scaling support

---

## Pattern 1: Cache-Aside (Lazy Loading)

**Use Case:** Most common pattern - load data on demand

### Basic Implementation

```java
package net.lab1024.sa.admin.module.business.product.manager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductCacheManager {

    private final ProductDao productDao;
    private final RedisTemplate<String, ProductEntity> redisTemplate;

    // L1 Cache (Caffeine)
    private final Cache<Long, ProductEntity> l1Cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats()
            .build();

    // Redis key prefix
    private static final String REDIS_KEY_PREFIX = "product:";

    /**
     * Get product with two-level cache
     */
    public ProductEntity getById(Long productId) {
        // 1. Try L1 cache
        ProductEntity product = l1Cache.getIfPresent(productId);
        if (product != null) {
            log.debug("L1 cache hit: {}", productId);
            return product;
        }

        // 2. Try L2 cache (Redis)
        String redisKey = REDIS_KEY_PREFIX + productId;
        product = redisTemplate.opsForValue().get(redisKey);
        if (product != null) {
            log.debug("L2 cache hit: {}", productId);
            // Populate L1 cache
            l1Cache.put(productId, product);
            return product;
        }

        // 3. Cache miss - load from database
        log.debug("Cache miss, loading from DB: {}", productId);
        product = productDao.selectById(productId);

        if (product != null) {
            // Populate both caches
            l1Cache.put(productId, product);
            redisTemplate.opsForValue().set(redisKey, product, Duration.ofMinutes(30));
        }

        return product;
    }

    /**
     * Invalidate cache on update
     */
    public void invalidate(Long productId) {
        l1Cache.invalidate(productId);
        redisTemplate.delete(REDIS_KEY_PREFIX + productId);
    }
}
```

### SmartAdmin Service Integration

```java
package net.lab1024.sa.admin.module.business.product.service;

import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductCacheManager cacheManager;

    /**
     * Get product with caching
     */
    public ResponseDTO<ProductVO> getProduct(Long productId) {
        ProductEntity entity = cacheManager.getById(productId);

        if (entity == null) {
            return ResponseDTO.userErrorParam("Product not found");
        }

        return ResponseDTO.ok(SmartBeanUtil.copy(entity, ProductVO.class));
    }

    /**
     * Update product and invalidate cache
     */
    @Transactional
    public ResponseDTO<Void> updateProduct(Long productId, ProductUpdateForm form) {
        // Update database
        ProductEntity entity = SmartBeanUtil.copy(form, ProductEntity.class);
        entity.setProductId(productId);
        productManager.updateById(entity);

        // Invalidate cache
        cacheManager.invalidate(productId);

        return ResponseDTO.ok();
    }
}
```

---

## Pattern 2: Write-Through

**Use Case:** Update cache synchronously when writing to database

```java
@Component
@RequiredArgsConstructor
public class WriteThroughCacheManager {

    private final ProductDao productDao;
    private final Cache<Long, ProductEntity> l1Cache;
    private final RedisTemplate<String, ProductEntity> redisTemplate;

    /**
     * Write-through: Update DB + cache synchronously
     */
    @Transactional
    public ProductEntity save(ProductEntity product) {
        // 1. Write to database
        productDao.insert(product);

        // 2. Update both caches immediately
        l1Cache.put(product.getProductId(), product);
        redisTemplate.opsForValue().set(
            REDIS_KEY_PREFIX + product.getProductId(),
            product,
            Duration.ofMinutes(30)
        );

        return product;
    }

    @Transactional
    public void update(ProductEntity product) {
        // 1. Update database
        productDao.updateById(product);

        // 2. Update both caches
        l1Cache.put(product.getProductId(), product);
        redisTemplate.opsForValue().set(
            REDIS_KEY_PREFIX + product.getProductId(),
            product,
            Duration.ofMinutes(30)
        );
    }
}
```

**Trade-offs:**
- ✅ Pros: Cache always consistent with database
- ⚠️ Cons: Higher write latency (synchronous cache update)

---

## Pattern 3: Write-Behind (Write-Back)

**Use Case:** High write throughput, eventual consistency acceptable

```java
@Component
@RequiredArgsConstructor
public class WriteBehindCacheManager {

    private final ProductDao productDao;
    private final Cache<Long, ProductEntity> l1Cache;
    private final RedisTemplate<String, ProductEntity> redisTemplate;
    private final ExecutorService asyncWriter;

    /**
     * Write-behind: Update cache first, DB asynchronously
     */
    public ProductEntity save(ProductEntity product) {
        // 1. Update caches immediately
        l1Cache.put(product.getProductId(), product);
        redisTemplate.opsForValue().set(
            REDIS_KEY_PREFIX + product.getProductId(),
            product,
            Duration.ofMinutes(30)
        );

        // 2. Schedule async DB write
        asyncWriter.submit(() -> {
            try {
                productDao.insert(product);
                log.info("Async DB write completed: {}", product.getProductId());
            } catch (Exception e) {
                log.error("Async DB write failed: {}", product.getProductId(), e);
                // Compensate: invalidate cache on failure
                l1Cache.invalidate(product.getProductId());
                redisTemplate.delete(REDIS_KEY_PREFIX + product.getProductId());
            }
        });

        return product;
    }
}
```

**Configuration:**
```java
@Configuration
public class WriteBehindConfiguration {

    @Bean("asyncCacheWriter")
    public ExecutorService asyncCacheWriter() {
        return new ThreadPoolExecutor(
            4,    // core threads
            8,    // max threads
            60,   // keepalive seconds
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            new ThreadFactoryBuilder()
                .setNameFormat("cache-writer-%d")
                .build(),
            new ThreadPoolExecutor.CallerRunsPolicy()  // Backpressure policy
        );
    }
}
```

**Trade-offs:**
- ✅ Pros: Extremely fast writes, reduced DB load
- ⚠️ Cons: Eventual consistency, risk of data loss on failure

---

## Pattern 4: Read-Through

**Use Case:** Transparent cache loading, simplified code

```java
@Component
@RequiredArgsConstructor
public class ReadThroughCacheManager {

    private final ProductDao productDao;
    private final RedisTemplate<String, ProductEntity> redisTemplate;

    // Caffeine with automatic loading
    private final LoadingCache<Long, ProductEntity> l1Cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats()
            .build(this::loadFromL2OrDatabase);  // Auto-load function

    /**
     * Read-through: Automatically loads missing data
     */
    public ProductEntity getById(Long productId) {
        // Cache handles loading automatically
        return l1Cache.get(productId);
    }

    /**
     * Load from L2 or database
     */
    private ProductEntity loadFromL2OrDatabase(Long productId) {
        // Try L2 cache (Redis)
        String redisKey = REDIS_KEY_PREFIX + productId;
        ProductEntity product = redisTemplate.opsForValue().get(redisKey);

        if (product != null) {
            log.debug("L2 cache hit during L1 load: {}", productId);
            return product;
        }

        // Load from database
        log.debug("Loading from DB during cache load: {}", productId);
        product = productDao.selectById(productId);

        if (product != null) {
            // Populate L2 cache
            redisTemplate.opsForValue().set(redisKey, product, Duration.ofMinutes(30));
        }

        return product;
    }
}
```

**Simplified Service:**
```java
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ReadThroughCacheManager cacheManager;

    public ResponseDTO<ProductVO> getProduct(Long productId) {
        // Cache automatically handles loading
        ProductEntity entity = cacheManager.getById(productId);

        if (entity == null) {
            return ResponseDTO.userErrorParam("Product not found");
        }

        return ResponseDTO.ok(SmartBeanUtil.copy(entity, ProductVO.class));
    }
}
```

---

## Pattern 5: Cache Stampede Prevention

**Problem:** Multiple threads try to load same data simultaneously when cache expires

### Solution 1: Redisson Distributed Lock

```java
@Component
@RequiredArgsConstructor
public class StampedePreventionCacheManager {

    private final ProductDao productDao;
    private final Cache<Long, ProductEntity> l1Cache;
    private final RedisTemplate<String, ProductEntity> redisTemplate;
    private final RedissonClient redissonClient;  // foundation.redis-lock

    /**
     * Get with stampede prevention
     */
    public ProductEntity getById(Long productId) {
        // 1. Try L1 cache
        ProductEntity product = l1Cache.getIfPresent(productId);
        if (product != null) {
            return product;
        }

        // 2. Try L2 cache
        String redisKey = REDIS_KEY_PREFIX + productId;
        product = redisTemplate.opsForValue().get(redisKey);
        if (product != null) {
            l1Cache.put(productId, product);
            return product;
        }

        // 3. Cache miss - use distributed lock to prevent stampede
        String lockKey = "lock:product:" + productId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // Try to acquire lock (wait up to 10 seconds, auto-release after 30 seconds)
            boolean acquired = lock.tryLock(10, 30, TimeUnit.SECONDS);

            if (acquired) {
                try {
                    // Double-check cache after acquiring lock
                    product = redisTemplate.opsForValue().get(redisKey);
                    if (product != null) {
                        l1Cache.put(productId, product);
                        return product;
                    }

                    // Load from database (only one thread does this)
                    log.info("Loading from DB with lock: {}", productId);
                    product = productDao.selectById(productId);

                    if (product != null) {
                        // Populate both caches
                        l1Cache.put(productId, product);
                        redisTemplate.opsForValue().set(redisKey, product, Duration.ofMinutes(30));
                    }

                    return product;
                } finally {
                    lock.unlock();
                }
            } else {
                // Failed to acquire lock - fallback to database
                log.warn("Failed to acquire lock, fallback to DB: {}", productId);
                return productDao.selectById(productId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Cache load interrupted", e);
        }
    }
}
```

### Solution 2: Caffeine Bulk Loading

```java
@Component
public class BulkLoadingCacheManager {

    // Caffeine with bulk loading support
    private final LoadingCache<Long, ProductEntity> l1Cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats()
            .build(this::loadSingle);

    /**
     * Get multiple products (prevents stampede for batch queries)
     */
    public Map<Long, ProductEntity> getAll(Set<Long> productIds) {
        // Caffeine loads missing entries in bulk
        return l1Cache.getAll(productIds);
    }

    private ProductEntity loadSingle(Long productId) {
        // Load single product
        return productDao.selectById(productId);
    }
}
```

---

## Pattern 6: Cache Refresh Strategy

**Use Case:** Refresh hot data before expiration to avoid cache miss

```java
@Component
@RequiredArgsConstructor
public class RefreshingCacheManager {

    private final ProductDao productDao;
    private final RedisTemplate<String, ProductEntity> redisTemplate;

    // Caffeine with refresh after write
    private final LoadingCache<Long, ProductEntity> l1Cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .refreshAfterWrite(5, TimeUnit.MINUTES)  // Refresh after 5 min
            .recordStats()
            .build(this::loadFromDatabase);

    public ProductEntity getById(Long productId) {
        // Cache automatically refreshes hot data
        return l1Cache.get(productId);
    }

    private ProductEntity loadFromDatabase(Long productId) {
        ProductEntity product = productDao.selectById(productId);

        if (product != null) {
            // Update L2 cache
            redisTemplate.opsForValue().set(
                REDIS_KEY_PREFIX + productId,
                product,
                Duration.ofMinutes(30)
            );
        }

        return product;
    }
}
```

---

## Comparison Matrix

| Pattern | Consistency | Write Speed | Read Speed | Complexity | Use Case |
|---------|-------------|-------------|------------|------------|----------|
| **Cache-Aside** | Eventual | Fast | Fast | Low | General purpose (RECOMMENDED) |
| **Write-Through** | Strong | Slow | Fast | Medium | Financial data, critical consistency |
| **Write-Behind** | Eventual | Very Fast | Fast | High | High write throughput, analytics |
| **Read-Through** | Eventual | Fast | Fast | Low | Read-heavy workloads |
| **Stampede Prevention** | Eventual | Fast | Fast | Medium | High-traffic hot data |
| **Refresh** | Eventual | Fast | Very Fast | Medium | Hot data with predictable access |

---

## SmartAdmin Integration Pattern (Recommended)

**Standard CRUD caching:**

```java
@Component
@RequiredArgsConstructor
public class StandardCacheManager {

    private final Cache<Long, ProductEntity> l1Cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats()
            .build();

    private final RedisTemplate<String, ProductEntity> redisTemplate;
    private final RedissonClient redissonClient;

    public ProductEntity getById(Long id) {
        return getWithStampedePrevention(id);
    }

    public void create(ProductEntity entity) {
        // Write-through for create
        l1Cache.put(entity.getProductId(), entity);
        redisTemplate.opsForValue().set(
            REDIS_KEY_PREFIX + entity.getProductId(),
            entity,
            Duration.ofMinutes(30)
        );
    }

    public void update(ProductEntity entity) {
        // Invalidate on update (safest)
        invalidate(entity.getProductId());
    }

    public void delete(Long id) {
        // Invalidate on delete
        invalidate(id);
    }

    private void invalidate(Long id) {
        l1Cache.invalidate(id);
        redisTemplate.delete(REDIS_KEY_PREFIX + id);
    }

    private ProductEntity getWithStampedePrevention(Long id) {
        // Implementation from Pattern 5
        // ... (stampede prevention logic)
    }
}
```

---

**Next:** [Invalidation Strategies](invalidation-strategies.md)
**Related:** [Cache Warming](cache-warming.md), [Monitoring](monitoring.md)
**Version:** 1.0.0
**Last Updated:** 2026-01-26
