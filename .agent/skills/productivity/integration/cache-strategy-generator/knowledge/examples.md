# Cache Strategy Generator - Examples

## 範例 1: 商品查詢兩層快取

**需求**: 為商品詳情添加 L1+L2 快取，TTL 10 分鐘

```java
@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductDao productDao;
    private final Cache<String, ProductVO> l1Cache;
    private final RedisTemplate<String, ProductVO> redisTemplate;

    public ProductVO getById(Long id) {
        String key = "product:" + id;

        // L1 快取
        ProductVO cached = l1Cache.getIfPresent(key);
        if (cached != null) {
            log.debug("L1 cache hit: {}", key);
            return cached;
        }

        // L2 快取
        cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            log.debug("L2 cache hit: {}", key);
            l1Cache.put(key, cached);
            return cached;
        }

        // DB 查詢
        ProductEntity entity = productDao.selectById(id);
        if (entity == null) {
            return null;
        }

        ProductVO vo = SmartBeanUtil.copy(entity, ProductVO.class);

        // 寫入快取
        redisTemplate.opsForValue().set(key, vo, 10, TimeUnit.MINUTES);
        l1Cache.put(key, vo);

        return vo;
    }

    @Transactional(rollbackFor = Throwable.class)
    public void update(ProductUpdateForm form) {
        productDao.updateById(form.getId(), form);
        // 失效快取
        evictCache(form.getId());
    }

    private void evictCache(Long id) {
        String key = "product:" + id;
        l1Cache.invalidate(key);
        redisTemplate.delete(key);
    }
}
```

---

## 範例 2: 使用 Spring Cache 註解

```java
@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryDao categoryDao;

    @Cacheable(value = "categories", key = "#id", unless = "#result == null")
    public CategoryVO getById(Long id) {
        return SmartBeanUtil.copy(categoryDao.selectById(id), CategoryVO.class);
    }

    @Cacheable(value = "categoryTree", key = "'all'")
    public List<CategoryTreeVO> getTree() {
        return categoryDao.selectTree();
    }

    @CacheEvict(value = {"categories", "categoryTree"}, allEntries = true)
    public void update(CategoryUpdateForm form) {
        categoryDao.updateById(form.getId(), form);
    }
}
```

---

## 範例 3: 快取預熱

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheWarmUpRunner implements ApplicationRunner {
    private final ProductDao productDao;
    private final RedisTemplate<String, ProductVO> redisTemplate;

    @Override
    public void run(ApplicationArguments args) {
        log.info("開始快取預熱...");

        // 預熱熱門商品
        List<ProductEntity> hotProducts = productDao.selectHotProducts(100);
        for (ProductEntity entity : hotProducts) {
            ProductVO vo = SmartBeanUtil.copy(entity, ProductVO.class);
            redisTemplate.opsForValue().set(
                "product:" + entity.getProductId(),
                vo,
                30, TimeUnit.MINUTES
            );
        }

        log.info("快取預熱完成, 共 {} 條記錄", hotProducts.size());
    }
}
```

---

## 範例 4: 快取命中率監控

```java
@Component
@RequiredArgsConstructor
public class CacheMetrics {
    private final MeterRegistry registry;
    private final Cache<String, Object> caffeineCache;

    @Scheduled(fixedRate = 60000)
    public void recordMetrics() {
        CacheStats stats = caffeineCache.stats();

        registry.gauge("cache.caffeine.hit.rate", stats.hitRate());
        registry.gauge("cache.caffeine.eviction.count", stats.evictionCount());
        registry.gauge("cache.caffeine.size", caffeineCache.estimatedSize());
    }
}
```
