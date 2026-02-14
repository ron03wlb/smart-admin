# Example 1: Multi-Level Cache Strategy

## Scenario
實現二級緩存（Caffeine L1 + Redis L2）for 用戶查詢

## Input
```bash
User: "Add two-level cache for user queries"
```

## Generated Output
```java
@Service
@RequiredArgsConstructor
public class UserCacheService {
    private final LoadingCache<Long, User> caffeineCache;
    private final RedisTemplate<String, User> redisTemplate;

    public User getUser(Long userId) {
        // L1: Caffeine (本地緩存)
        User user = caffeineCache.get(userId);
        if (user != null) {
            return user;
        }

        // L2: Redis (分佈式緩存)
        String key = "user:" + userId;
        user = redisTemplate.opsForValue().get(key);
        if (user != null) {
            caffeineCache.put(userId, user);
            return user;
        }

        // DB: 數據庫查詢
        user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            redisTemplate.opsForValue().set(key, user, 1, TimeUnit.HOURS);
            caffeineCache.put(userId, user);
        }

        return user;
    }
}
```

## Expected Result
- L1 命中率：~80%（亞秒級響應）
- L2 命中率：~15%（毫秒級響應）
- DB 查詢：~5%（秒級響應）
- 整體查詢性能提升 90%
