# Cache Strategy Generator - Quick Reference

**Version**: 1.0.0
**Last Updated**: 2026-02-02
**Skill**: cache-strategy-generator (P2 - Productivity/Integration)

---

## Command Quick Reference

| Command | Purpose | Duration |
|---------|---------|----------|
| L1 Cache (Caffeine) | Local in-memory cache | ~5 min |
| L2 Cache (Redis) | Distributed cache | ~8 min |
| Multi-level Cache | L1 + L2 hybrid | ~12 min |
| Cache-Aside Pattern | Load-through cache | ~10 min |
| Write-Through Pattern | Update cache on write | ~12 min |

---

## Cache Strategy Selection Matrix

| Scenario | Strategy | L1 (Caffeine) | L2 (Redis) | Time |
|----------|----------|---------------|------------|------|
| Hot data (<10MB) | L1 only | ✅ | ❌ | 5 min |
| Shared across instances | L2 only | ❌ | ✅ | 8 min |
| Hot + Distributed | Multi-level | ✅ | ✅ | 12 min |
| Session data | L2 only | ❌ | ✅ | 8 min |
| Configuration | L1 + TTL | ✅ | ❌ | 5 min |

---

## Pattern 1: L1 Cache (Caffeine)

**Use When**: Single instance, hot data <10MB

**Setup**:
```java
@Configuration
@EnableCaching
public class CaffeineCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        cacheManager.registerCustomCache("departments",
            Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(100)
                .recordStats()
                .build());

        cacheManager.registerCustomCache("employees",
            Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build());

        return cacheManager;
    }
}
```

**Usage**:
```java
@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentDao departmentDao;

    @Cacheable(value = "departments", key = "#id")
    public DepartmentVO getById(Long id) {
        DepartmentEntity entity = departmentDao.selectById(id);
        return SmartBeanUtil.copy(entity, DepartmentVO.class);
    }

    @CacheEvict(value = "departments", key = "#id")
    public void update(Long id, DepartmentUpdateForm form) {
        departmentDao.updateById(form);
    }

    @CacheEvict(value = "departments", allEntries = true)
    public void clearAll() {
        // Clear all department cache
    }
}
```

**Time to Implement**: 5-8 minutes

---

## Pattern 2: L2 Cache (Redis)

**Use When**: Distributed system, shared cache needed

**Setup**:
```java
@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .withCacheConfiguration("employees",
                config.entryTtl(Duration.ofMinutes(5)))
            .withCacheConfiguration("departments",
                config.entryTtl(Duration.ofMinutes(10)))
            .build();
    }
}
```

**Usage** (same as L1):
```java
@Cacheable(value = "departments", key = "#id")
public DepartmentVO getById(Long id) {
    return departmentDao.selectById(id);
}
```

**Time to Implement**: 8-12 minutes

---

## Pattern 3: Multi-level Cache (L1 + L2)

**Use When**: Hot data + distributed system

**Implementation**:
```java
@Service
@RequiredArgsConstructor
public class MultiLevelCacheService {

    private final Cache<String, Object> l1Cache;  // Caffeine
    private final RedisTemplate<String, Object> redisTemplate;  // Redis

    /**
     * Get from L1 → L2 → DB
     */
    public <T> T get(String key, Supplier<T> dbLoader) {
        // L1 check
        T value = (T) l1Cache.getIfPresent(key);
        if (value != null) {
            return value;
        }

        // L2 check
        value = (T) redisTemplate.opsForValue().get(key);
        if (value != null) {
            // Write back to L1
            l1Cache.put(key, value);
            return value;
        }

        // DB load
        value = dbLoader.get();
        if (value != null) {
            // Write to L1 + L2
            l1Cache.put(key, value);
            redisTemplate.opsForValue().set(key, value, 10, TimeUnit.MINUTES);
        }

        return value;
    }

    /**
     * Evict from both levels
     */
    public void evict(String key) {
        l1Cache.invalidate(key);
        redisTemplate.delete(key);
    }
}

// Usage
@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final MultiLevelCacheService cacheService;
    private final DepartmentDao departmentDao;

    public DepartmentVO getById(Long id) {
        return cacheService.get(
            "dept:" + id,
            () -> departmentDao.selectById(id)
        );
    }
}
```

**Time to Implement**: 12-18 minutes

---

## Pattern 4: Cache-Aside (Lazy Loading)

**Use When**: Standard caching pattern

**Implementation**:
```java
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeDao employeeDao;
    private final RedisTemplate<String, EmployeeVO> redisTemplate;

    /**
     * Cache-Aside: Check cache → Load from DB → Update cache
     */
    public EmployeeVO getById(Long id) {
        String key = "employee:" + id;

        // 1. Check cache
        EmployeeVO cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }

        // 2. Load from DB
        EmployeeEntity entity = employeeDao.selectById(id);
        if (entity == null) {
            return null;
        }

        // 3. Update cache
        EmployeeVO vo = SmartBeanUtil.copy(entity, EmployeeVO.class);
        redisTemplate.opsForValue().set(key, vo, 5, TimeUnit.MINUTES);

        return vo;
    }

    /**
     * Update: Invalidate cache
     */
    public void update(Long id, EmployeeUpdateForm form) {
        employeeDao.updateById(form);

        // Invalidate cache
        redisTemplate.delete("employee:" + id);
    }
}
```

**Time to Implement**: 10-12 minutes

---

## Pattern 5: Write-Through Cache

**Use When**: Strong consistency required

**Implementation**:
```java
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeDao employeeDao;
    private final RedisTemplate<String, EmployeeVO> redisTemplate;

    /**
     * Write-Through: Update DB → Update cache
     */
    public void update(Long id, EmployeeUpdateForm form) {
        // 1. Update DB
        employeeDao.updateById(form);

        // 2. Update cache immediately
        EmployeeEntity entity = employeeDao.selectById(id);
        EmployeeVO vo = SmartBeanUtil.copy(entity, EmployeeVO.class);

        String key = "employee:" + id;
        redisTemplate.opsForValue().set(key, vo, 5, TimeUnit.MINUTES);
    }
}
```

**Time to Implement**: 10-12 minutes

---

## Cache Key Design Patterns

### Pattern 1: Hierarchical Keys

```java
// Good - Hierarchical
"user:123:profile"
"user:123:orders"
"dept:5:employees"

// Bad - Flat
"user123profile"
"userorders123"
```

### Pattern 2: Versioned Keys

```java
// Support cache invalidation by version
"config:v1:database"
"config:v2:database"  // New version, old cache auto-expires
```

### Pattern 3: Prefix by Environment

```java
@Value("${spring.profiles.active}")
private String env;

private String buildKey(String key) {
    return env + ":" + key;  // "prod:user:123"
}
```

---

## Cache Eviction Strategies

### Strategy 1: TTL-based

```java
// Fixed TTL
redisTemplate.opsForValue().set(key, value, 10, TimeUnit.MINUTES);

// Dynamic TTL based on data
Duration ttl = isHotData(key) ? Duration.ofMinutes(30) : Duration.ofMinutes(5);
redisTemplate.opsForValue().set(key, value, ttl);
```

### Strategy 2: Event-driven

```java
@Component
@RequiredArgsConstructor
public class CacheEvictionListener {

    private final RedisTemplate<String, Object> redisTemplate;

    @EventListener
    public void onEmployeeUpdated(EmployeeUpdatedEvent event) {
        // Evict related caches
        redisTemplate.delete("employee:" + event.getEmployeeId());
        redisTemplate.delete("dept:" + event.getDeptId() + ":employees");
    }
}
```

### Strategy 3: Batch Eviction

```java
/**
 * Evict all keys matching pattern
 */
public void evictPattern(String pattern) {
    Set<String> keys = redisTemplate.keys(pattern + "*");
    if (keys != null && !keys.isEmpty()) {
        redisTemplate.delete(keys);
    }
}

// Usage
evictPattern("employee:");  // Clear all employee caches
```

---

## Performance Optimization

### Optimization 1: Batch Operations

```java
/**
 * Get multiple values in one Redis call
 */
public List<EmployeeVO> getByIds(List<Long> ids) {
    List<String> keys = ids.stream()
        .map(id -> "employee:" + id)
        .collect(Collectors.toList());

    // Batch get (1 Redis call instead of N)
    List<EmployeeVO> cached = redisTemplate.opsForValue().multiGet(keys);

    // Handle cache misses...
    return cached;
}
```

### Optimization 2: Pipeline

```java
/**
 * Use Redis pipeline for multiple writes
 */
public void cacheMultiple(Map<Long, EmployeeVO> employees) {
    redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
        employees.forEach((id, vo) -> {
            String key = "employee:" + id;
            redisTemplate.opsForValue().set(key, vo, 5, TimeUnit.MINUTES);
        });
        return null;
    });
}
```

---

## Cache Monitoring

### Metrics to Track

```java
@Component
@RequiredArgsConstructor
public class CacheMetrics {

    private final MeterRegistry meterRegistry;

    /**
     * Record cache hit/miss
     */
    public void recordCacheAccess(String cacheName, boolean hit) {
        meterRegistry.counter("cache.access",
            "cache", cacheName,
            "result", hit ? "hit" : "miss"
        ).increment();
    }

    /**
     * Cache hit rate
     */
    public double getCacheHitRate(String cacheName) {
        double hits = getCounter(cacheName, "hit");
        double misses = getCounter(cacheName, "miss");

        return hits / (hits + misses);
    }
}
```

**Target Metrics**:
- Hit Rate: >80%
- Eviction Rate: <10%
- Memory Usage: <70%

---

## Time Estimates

| Pattern | Implementation | Testing | Total |
|---------|---------------|---------|-------|
| L1 Cache (Caffeine) | 5 min | 3 min | 8 min |
| L2 Cache (Redis) | 8 min | 5 min | 13 min |
| Multi-level Cache | 12 min | 8 min | 20 min |
| Cache-Aside | 10 min | 5 min | 15 min |
| Write-Through | 10 min | 5 min | 15 min |

---

**See Also**:
- [PostgreSQL Best Practices](../postgresql-best-practices/) - Database optimization
- [SmartAdmin Patterns](./../../../.claude/shared/knowledge/smartadmin-patterns.md) - Caching patterns
