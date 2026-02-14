# Cache Strategy Generator - Quick Reference

## 兩層快取配置

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats());
        return manager;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
```

## Cache-Aside 模式

```java
public ProductVO getById(Long id) {
    // 1. 查 L1 (Caffeine)
    ProductVO cached = caffeineCache.getIfPresent("product:" + id);
    if (cached != null) return cached;

    // 2. 查 L2 (Redis)
    cached = redisTemplate.opsForValue().get("product:" + id);
    if (cached != null) {
        caffeineCache.put("product:" + id, cached);
        return cached;
    }

    // 3. 查 DB
    ProductEntity entity = productDao.selectById(id);
    ProductVO vo = SmartBeanUtil.copy(entity, ProductVO.class);

    // 4. 寫入快取
    redisTemplate.opsForValue().set("product:" + id, vo, 10, TimeUnit.MINUTES);
    caffeineCache.put("product:" + id, vo);

    return vo;
}
```

## 快取失效策略

| 策略 | 適用場景 | 實現方式 |
|------|----------|----------|
| TTL | 資料允許短暫不一致 | `expire(key, 5, MINUTES)` |
| Event-driven | 強一致性需求 | 更新後發事件刪快取 |
| Manual | 手動控制 | API 提供刷新端點 |

## 防止快取穿透

```java
// 布隆過濾器
@Bean
public BloomFilter<Long> productIdBloomFilter() {
    BloomFilter<Long> filter = BloomFilter.create(
        Funnels.longFunnel(), 1000000, 0.01);
    productDao.selectAllIds().forEach(filter::put);
    return filter;
}

// 查詢前檢查
if (!bloomFilter.mightContain(id)) {
    return null; // 直接返回，不查 DB
}
```

## Redisson 分散式鎖

```java
public ProductVO getByIdWithLock(Long id) {
    RLock lock = redisson.getLock("lock:product:" + id);
    try {
        if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
            return doGetById(id);
        }
    } finally {
        lock.unlock();
    }
}
```
