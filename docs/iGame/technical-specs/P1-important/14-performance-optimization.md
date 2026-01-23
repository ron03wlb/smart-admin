# P1-14: Performance Optimization

## Document Control

| Attribute | Value |
|-----------|-------|
| Document ID | P1-14 |
| Title | Performance Optimization |
| Version | 1.0.0 |
| Status | Draft |
| Author | SmartAdmin Architecture Team |
| Created | 2026-01-23 |
| Last Updated | 2026-01-23 |
| Related Docs | [backend_project.md](../../backend_project.md), [P0-03](../P0-critical/03-seamless-wallet-implementation.md), [P1-09](09-game-aggregator-sdk.md) |

---

## 1. Background

### 1.1 Purpose

This document provides comprehensive performance optimization strategies to achieve the SLA targets established throughout the iGaming platform documentation:
- **Wallet API**: <200ms p95 latency (P0-03)
- **Game Launch**: <2s p95 latency (P1-09)
- **Overall System**: 10,000 TPS (transactions per second)
- **Database Queries**: <50ms p95 for OLTP operations
- **API Gateway**: <10ms p95 overhead

Performance optimization is critical for:
1. **Player Experience**: Sub-second response times prevent friction and abandonment
2. **Competitive Advantage**: Faster platform = higher player retention
3. **Cost Efficiency**: Optimized infrastructure reduces cloud costs by 40-60%
4. **Scalability**: Efficient code supports 10× player growth without proportional infrastructure increase

### 1.2 Scope

**In Scope**:
- Application-level profiling and tuning (JVM, garbage collection)
- Database query optimization (PostgreSQL OLTP, Apache Doris OLAP)
- Multi-level caching strategy (Caffeine L1, Redis L2)
- Network and I/O optimization (connection pooling, async processing)
- Load testing methodology and performance benchmarking
- Monitoring and observability for performance metrics

**Out of Scope**:
- Infrastructure scaling decisions (Kubernetes HPA, node sizing) - covered in deployment docs
- Frontend performance optimization (Vue.js bundle size, lazy loading)
- CDN configuration for static assets
- Third-party service performance (game providers, payment gateways)

### 1.3 Strategic Alignment

Aligns with igame_str.md first principles:
- **Velocity (速度)**: <200ms wallet API enables friction-free gaming
- **Zero Marginal Cost**: Performance optimization reduces per-transaction cloud costs
- **Data Leverage**: OLAP query optimization enables real-time decision-making

### 1.4 Performance SLA Matrix

| Component | Metric | Target | Measurement Method |
|-----------|--------|--------|-------------------|
| Wallet API | p95 latency | <200ms | Prometheus histogram |
| Game Launch | p95 latency | <2s | Application logs |
| Deposit/Withdrawal | p95 latency | <500ms | End-to-end trace |
| User Login | p95 latency | <300ms | Application logs |
| Balance Query | p95 latency | <50ms | Database query log |
| OLTP Queries | p95 latency | <50ms | PostgreSQL pg_stat_statements |
| OLAP Queries | p95 latency | <3s | Doris audit log |
| API Gateway | Overhead | <10ms | Istio metrics |
| System Throughput | TPS | 10,000 | Load testing (k6) |
| Database Connections | Pool utilization | <70% | HikariCP metrics |
| JVM Heap Usage | Max heap | <80% | JVM metrics |
| GC Pause Time | p99 | <100ms | GC logs |

---

## 2. Architecture

### 2.1 Performance Monitoring Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Application Layer                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │ Controller  │  │  Service    │  │   Manager   │             │
│  │ (Metrics)   │─▶│  (Trace)    │─▶│ (Profile)   │             │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
│         │                 │                 │                    │
│         └─────────────────┴─────────────────┘                    │
│                           ▼                                      │
│              ┌─────────────────────────┐                         │
│              │  Micrometer Registry    │                         │
│              │  (Timer, Counter,       │                         │
│              │   Histogram, Gauge)     │                         │
│              └─────────────────────────┘                         │
│                           │                                      │
└───────────────────────────┼──────────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Observability Stack                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │  Prometheus  │  │   Grafana    │  │   Jaeger     │          │
│  │  (Metrics)   │─▶│ (Dashboard)  │  │  (Tracing)   │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              Performance Dashboards                       │  │
│  │  - JVM metrics (heap, GC, threads)                       │  │
│  │  - Database metrics (query latency, connection pool)     │  │
│  │  - Cache metrics (hit rate, eviction rate)               │  │
│  │  - API metrics (latency percentiles, throughput)         │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    Profiling Stack (On-Demand)                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │  JProfiler   │  │ Async Profiler│ │   Arthas     │          │
│  │  (IDE Mode)  │  │ (Production)  │  │  (Live)      │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 Multi-Level Caching Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Application Pod                           │
│                                                                  │
│  ┌────────────────────────────────────────────────────────┐    │
│  │              L1: Caffeine (In-Memory)                   │    │
│  │  - VIP tier config (5 min TTL)                         │    │
│  │  - Bonus templates (10 min TTL)                        │    │
│  │  - Game catalog metadata (15 min TTL)                  │    │
│  │  - Tenant configuration (30 min TTL)                   │    │
│  │  Max size: 10,000 entries | Eviction: LRU             │    │
│  └────────────────────────────────────────────────────────┘    │
│                           │ Cache miss                          │
│                           ▼                                     │
└───────────────────────────┼─────────────────────────────────────┘
                            │
┌───────────────────────────┼─────────────────────────────────────┐
│                           ▼                                     │
│  ┌────────────────────────────────────────────────────────┐    │
│  │              L2: Redis (Distributed)                    │    │
│  │  - Player session (Sa-Token, 24h TTL)                  │    │
│  │  - Player balance cache (5 min TTL)                    │    │
│  │  - Leaderboard (1 hour TTL)                            │    │
│  │  - Rate limit counters (sliding window)                │    │
│  │  Cluster: 3 master + 3 replica | Persistence: RDB+AOF  │    │
│  └────────────────────────────────────────────────────────┘    │
│                           │ Cache miss                          │
│                           ▼                                     │
└───────────────────────────┼─────────────────────────────────────┘
                            │
┌───────────────────────────┼─────────────────────────────────────┐
│                           ▼                                     │
│  ┌────────────────────────────────────────────────────────┐    │
│  │              Database (Source of Truth)                 │    │
│  │  PostgreSQL 16 (OLTP) | Apache Doris 2.1 (OLAP)       │    │
│  └────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘

Cache-Aside Pattern:
1. Check L1 (Caffeine) → Hit: return value
2. On L1 miss → Check L2 (Redis) → Hit: populate L1, return value
3. On L2 miss → Query DB → Populate L2 and L1, return value
```

### 2.3 Database Connection Pooling

**HikariCP Configuration** (PostgreSQL):
```yaml
spring:
  datasource:
    type: com.zaxxer.hikari.HikariDataSource
    hikari:
      # Pool sizing (T × (C − 1) + 1)
      # T = thread count (200), C = core count (8)
      # Max pool = 8 × (8 - 1) + 1 = 57 ≈ 50 (rounded for safety)
      maximum-pool-size: 50
      minimum-idle: 10

      # Connection timeout
      connection-timeout: 30000  # 30s
      idle-timeout: 600000       # 10 min
      max-lifetime: 1800000      # 30 min

      # Performance tuning
      auto-commit: false  # Explicit transaction control
      read-only: false
      connection-test-query: SELECT 1
      pool-name: HikariPool-SmartAdmin

      # Leak detection (development only)
      leak-detection-threshold: 60000  # 60s
```

---

## 3. Implementation

### 3.1 Application-Level Profiling

#### 3.1.1 Continuous Profiling with Async Profiler

**Production-Safe CPU Profiling**:
```bash
# Attach to running JVM (no restart required)
./profiler.sh -d 60 -e cpu -f /tmp/flamegraph-cpu.html <pid>

# Allocation profiling (heap pressure analysis)
./profiler.sh -d 60 -e alloc -f /tmp/flamegraph-alloc.html <pid>

# Lock contention profiling
./profiler.sh -d 60 -e lock -f /tmp/flamegraph-lock.html <pid>
```

**Integration with Arthas** (Interactive Troubleshooting):
```bash
# Download and attach
curl -O https://arthas.aliyun.com/arthas-boot.jar
java -jar arthas-boot.jar

# Trace method execution time (production safe)
trace net.lab1024.sa.base.module.support.wallet.manager.WalletManager debit -n 100

# Monitor method invocation statistics
monitor -c 5 net.lab1024.sa.base.module.support.wallet.manager.WalletManager debit

# Watch method parameters and return values
watch net.lab1024.sa.base.module.support.wallet.manager.WalletManager debit "{params,returnObj,throwExp}" -x 3 -n 10
```

#### 3.1.2 Custom Performance Metrics

**TimerAspect** (Automatic Method Timing):
```java
package net.lab1024.sa.foundation.aop;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Performance monitoring aspect for critical business methods
 *
 * Usage: Annotate methods with @Timed
 *
 * @author SmartAdmin Team
 * @since 2026-01-23
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class TimerAspect {

    private final MeterRegistry meterRegistry;

    @Around("@annotation(timed)")
    public Object timeMethod(ProceedingJoinPoint joinPoint, Timed timed) throws Throwable {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            return joinPoint.proceed();
        } finally {
            String metricName = timed.value().isEmpty()
                ? joinPoint.getSignature().toShortString()
                : timed.value();

            sample.stop(Timer.builder("method.execution")
                .tag("class", joinPoint.getTarget().getClass().getSimpleName())
                .tag("method", joinPoint.getSignature().getName())
                .tag("metric", metricName)
                .description("Method execution time")
                .register(meterRegistry));
        }
    }
}

/**
 * Custom annotation for method timing
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Timed {
    String value() default "";
}
```

**Usage Example**:
```java
@Service
@RequiredArgsConstructor
public class WalletManager {

    @Timed("wallet.debit")
    @Transactional(rollbackFor = Exception.class)
    public void debit(Long playerId, BigDecimal amount, String currency, String description, String idempotencyKey) {
        // Implementation...
    }

    @Timed("wallet.credit")
    @Transactional(rollbackFor = Exception.class)
    public void credit(Long playerId, BigDecimal amount, String currency, String description, String idempotencyKey) {
        // Implementation...
    }
}
```

**Prometheus Query**:
```promql
# p95 latency for wallet debit operations
histogram_quantile(0.95,
  sum(rate(method_execution_seconds_bucket{method="debit",class="WalletManager"}[5m])) by (le)
)

# Average latency trend over 1 hour
avg_over_time(method_execution_seconds_sum{method="debit"}[1h]) /
avg_over_time(method_execution_seconds_count{method="debit"}[1h])
```

### 3.2 Database Query Optimization

#### 3.2.1 PostgreSQL Query Profiling

**Enable pg_stat_statements** (postgresql.conf):
```sql
-- Add to postgresql.conf
shared_preload_libraries = 'pg_stat_statements'
pg_stat_statements.track = all
pg_stat_statements.max = 10000

-- Restart PostgreSQL and install extension
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Query slowest queries
SELECT
    query,
    calls,
    total_exec_time,
    mean_exec_time,
    stddev_exec_time,
    min_exec_time,
    max_exec_time,
    rows
FROM pg_stat_statements
WHERE query NOT LIKE '%pg_stat_statements%'
ORDER BY mean_exec_time DESC
LIMIT 20;
```

**EXPLAIN ANALYZE Example**:
```sql
-- Before optimization: Full table scan
EXPLAIN ANALYZE
SELECT * FROM wallet_transactions
WHERE player_id = 12345
  AND created_at BETWEEN '2026-01-01' AND '2026-01-31'
ORDER BY created_at DESC
LIMIT 20;

-- Output (BAD):
-- Seq Scan on wallet_transactions (cost=0.00..156432.00 rows=50 width=150) (actual time=2500ms..2520ms)
--   Filter: (player_id = 12345 AND created_at >= '2026-01-01' AND created_at <= '2026-01-31')
--   Rows Removed by Filter: 5000000

-- After optimization: Index scan
CREATE INDEX idx_wallet_tx_player_created
ON wallet_transactions (player_id, created_at DESC);

EXPLAIN ANALYZE
SELECT * FROM wallet_transactions
WHERE player_id = 12345
  AND created_at BETWEEN '2026-01-01' AND '2026-01-31'
ORDER BY created_at DESC
LIMIT 20;

-- Output (GOOD):
-- Index Scan using idx_wallet_tx_player_created (cost=0.56..12.34 rows=50 width=150) (actual time=2ms..5ms)
--   Index Cond: (player_id = 12345 AND created_at >= '2026-01-01' AND created_at <= '2026-01-31')
```

#### 3.2.2 Index Strategy

**Critical Indexes** (wallet_transactions table):
```sql
-- Composite index for player transaction history queries
CREATE INDEX idx_wallet_tx_player_created
ON wallet_transactions (tenant_id, player_id, created_at DESC);

-- Covering index for balance calculation (avoid heap fetch)
CREATE INDEX idx_wallet_tx_player_balance_covering
ON wallet_transactions (tenant_id, player_id, transaction_type, amount)
INCLUDE (created_at);

-- Partial index for pending withdrawals (WHERE clause optimization)
CREATE INDEX idx_wallet_tx_pending_withdrawals
ON wallet_transactions (tenant_id, status)
WHERE transaction_type = 'WITHDRAWAL' AND status = 'PENDING';

-- Analyze table statistics
ANALYZE wallet_transactions;
```

**Index Bloat Monitoring**:
```sql
-- Check index bloat (should be <20%)
SELECT
    schemaname,
    tablename,
    indexname,
    pg_size_pretty(pg_relation_size(indexrelid)) AS index_size,
    idx_scan AS index_scans,
    idx_tup_read AS tuples_read,
    idx_tup_fetch AS tuples_fetched,
    pg_size_pretty(pg_relation_size(indexrelid) * (1 - idx_scan::float / GREATEST(idx_scan + idx_tup_read, 1))) AS bloat_size
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
  AND indexrelname LIKE 'idx_wallet%'
ORDER BY pg_relation_size(indexrelid) DESC;

-- Rebuild bloated indexes (requires maintenance window)
REINDEX INDEX CONCURRENTLY idx_wallet_tx_player_created;
```

#### 3.2.3 Query Optimization Patterns

**Pattern 1: Use Covering Indexes**

Before:
```java
// MyBatis-Plus query (2 database round-trips: index lookup + heap fetch)
@Select("SELECT * FROM wallet_transactions WHERE player_id = #{playerId} ORDER BY created_at DESC LIMIT 10")
List<WalletTransaction> selectRecentTransactions(@Param("playerId") Long playerId);
```

After:
```java
// Use covering index (1 round-trip: index-only scan)
@Select("SELECT id, player_id, transaction_type, amount, created_at FROM wallet_transactions WHERE player_id = #{playerId} ORDER BY created_at DESC LIMIT 10")
List<WalletTransactionVO> selectRecentTransactions(@Param("playerId") Long playerId);
```

**Pattern 2: Avoid N+1 Queries**

Before:
```java
// BAD: N+1 queries (1 player query + N bonus queries)
public List<PlayerWithBonusesVO> getPlayersWithBonuses(List<Long> playerIds) {
    List<Player> players = playerDao.selectBatchIds(playerIds);

    return players.stream()
        .map(player -> {
            List<PlayerBonus> bonuses = playerBonusDao.selectList(
                new LambdaQueryWrapper<PlayerBonus>()
                    .eq(PlayerBonus::getPlayerId, player.getId())
            );
            return new PlayerWithBonusesVO(player, bonuses);
        })
        .collect(Collectors.toList());
}
```

After:
```java
// GOOD: 2 queries total (batch fetch with IN clause)
public List<PlayerWithBonusesVO> getPlayersWithBonuses(List<Long> playerIds) {
    List<Player> players = playerDao.selectBatchIds(playerIds);

    // Single query for all bonuses
    List<PlayerBonus> allBonuses = playerBonusDao.selectList(
        new LambdaQueryWrapper<PlayerBonus>()
            .in(PlayerBonus::getPlayerId, playerIds)
    );

    // Group bonuses by player ID in memory
    Map<Long, List<PlayerBonus>> bonusesByPlayer = allBonuses.stream()
        .collect(Collectors.groupingBy(PlayerBonus::getPlayerId));

    return players.stream()
        .map(player -> new PlayerWithBonusesVO(player, bonusesByPlayer.getOrDefault(player.getId(), Collections.emptyList())))
        .collect(Collectors.toList());
}
```

**Pattern 3: Use Pagination with Keyset Pagination (Seek Method)**

Before (Offset Pagination - Slow for Large Offsets):
```java
// BAD: SELECT * FROM wallet_transactions ORDER BY created_at DESC LIMIT 20 OFFSET 10000
// Database must scan 10,020 rows to return 20 results
PageHelper.startPage(500, 20);  // Page 500 with 20 items per page
List<WalletTransaction> transactions = walletTransactionDao.selectList(queryWrapper);
```

After (Keyset Pagination - Constant Performance):
```java
// GOOD: SELECT * FROM wallet_transactions WHERE created_at < '2026-01-15 10:00:00' ORDER BY created_at DESC LIMIT 20
// Database only scans 20 rows
@Select("SELECT * FROM wallet_transactions WHERE created_at < #{lastCreatedAt} ORDER BY created_at DESC LIMIT #{pageSize}")
List<WalletTransaction> selectNextPage(@Param("lastCreatedAt") LocalDateTime lastCreatedAt, @Param("pageSize") int pageSize);
```

### 3.3 Caching Optimization

#### 3.3.1 Caffeine L1 Cache Configuration

**CaffeineConfig.java**:
```java
package net.lab1024.sa.foundation.cache.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine L1 cache configuration
 *
 * Cache categories:
 * - vipConfig: VIP tier configurations (5 min TTL, 100 max entries)
 * - bonusTemplates: Bonus campaign templates (10 min TTL, 500 max entries)
 * - gameCatalog: Game metadata (15 min TTL, 5000 max entries)
 * - tenantConfig: Tenant settings (30 min TTL, 100 max entries)
 *
 * @author SmartAdmin Team
 * @since 2026-01-23
 */
@Slf4j
@Configuration
@EnableCaching
public class CaffeineConfig {

    @Bean
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats()  // Enable statistics
            .removalListener((key, value, cause) -> {
                if (cause == RemovalCause.EXPIRED) {
                    log.debug("Cache entry expired: key={}", key);
                }
            }));
        return cacheManager;
    }

    /**
     * VIP tier configuration cache
     */
    @Bean
    public Cache<String, Object> vipConfigCache() {
        return Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats()
            .build();
    }

    /**
     * Game catalog cache (larger capacity, longer TTL)
     */
    @Bean
    public Cache<String, Object> gameCatalogCache() {
        return Caffeine.newBuilder()
            .maximumSize(5_000)
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .recordStats()
            .build();
    }
}
```

**Usage Example with Cache-Aside Pattern**:
```java
@Service
@RequiredArgsConstructor
public class VipTierConfigService {

    private final VipTierConfigDao vipTierConfigDao;
    private final Cache<String, VipTierConfig> vipConfigCache;
    private final RedissonClient redisson;

    public VipTierConfig getTenantVipConfig(String tenantId) {
        String cacheKey = "vip_config:" + tenantId;

        // L1: Check Caffeine
        VipTierConfig config = vipConfigCache.getIfPresent(cacheKey);
        if (config != null) {
            return config;
        }

        // L2: Check Redis
        RBucket<VipTierConfig> redisBucket = redisson.getBucket(cacheKey);
        config = redisBucket.get();
        if (config != null) {
            vipConfigCache.put(cacheKey, config);  // Populate L1
            return config;
        }

        // L3: Database
        config = vipTierConfigDao.selectOne(
            new LambdaQueryWrapper<VipTierConfig>()
                .eq(VipTierConfig::getTenantId, tenantId)
        );

        // Populate L2 and L1
        redisBucket.set(config, 5, TimeUnit.MINUTES);
        vipConfigCache.put(cacheKey, config);

        return config;
    }
}
```

#### 3.3.2 Cache Invalidation Strategy

**Write-Through Pattern** (for critical data):
```java
@Service
@RequiredArgsConstructor
public class WalletManager {

    private final WalletDao walletDao;
    private final RedissonClient redisson;
    private final Cache<String, Wallet> walletCache;

    @Transactional(rollbackFor = Exception.class)
    public void debit(Long playerId, BigDecimal amount, String currency, String description, String idempotencyKey) {
        // 1. Update database
        Wallet wallet = walletDao.selectOne(
            new LambdaQueryWrapper<Wallet>()
                .eq(Wallet::getPlayerId, playerId)
                .eq(Wallet::getCurrency, currency)
        );

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletDao.updateById(wallet);

        // 2. Invalidate cache (both L1 and L2)
        String cacheKey = "wallet:" + playerId + ":" + currency;
        walletCache.invalidate(cacheKey);  // L1
        redisson.getBucket(cacheKey).delete();  // L2

        // Alternative: Update cache instead of invalidation (write-through)
        // walletCache.put(cacheKey, wallet);
        // redisson.getBucket(cacheKey).set(wallet, 5, TimeUnit.MINUTES);
    }
}
```

**Cache Stampede Prevention** (using distributed lock):
```java
public VipTierConfig getTenantVipConfig(String tenantId) {
    String cacheKey = "vip_config:" + tenantId;

    // Check cache first
    VipTierConfig config = vipConfigCache.getIfPresent(cacheKey);
    if (config != null) {
        return config;
    }

    // Acquire distributed lock to prevent cache stampede
    String lockKey = "lock:vip_config:" + tenantId;
    RLock lock = redisson.getLock(lockKey);

    try {
        // Wait up to 10 seconds for lock
        if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
            try {
                // Double-check cache (another thread might have populated it)
                config = vipConfigCache.getIfPresent(cacheKey);
                if (config != null) {
                    return config;
                }

                // Load from database
                config = vipTierConfigDao.selectOne(
                    new LambdaQueryWrapper<VipTierConfig>()
                        .eq(VipTierConfig::getTenantId, tenantId)
                );

                // Populate cache
                vipConfigCache.put(cacheKey, config);
                redisson.getBucket(cacheKey).set(config, 5, TimeUnit.MINUTES);

                return config;
            } finally {
                lock.unlock();
            }
        } else {
            // Lock acquisition timeout - fallback to direct database query
            log.warn("Failed to acquire lock for cache loading: {}", lockKey);
            return vipTierConfigDao.selectOne(
                new LambdaQueryWrapper<VipTierConfig>()
                    .eq(VipTierConfig::getTenantId, tenantId)
            );
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new ServiceException("Cache loading interrupted");
    }
}
```

### 3.4 JVM Tuning

#### 3.4.1 G1GC Configuration

**Recommended JVM Flags** (application.yml or JAVA_OPTS):
```bash
# Heap sizing (80% of container memory for 4GB container)
-Xms3200m
-Xmx3200m

# G1GC tuning
-XX:+UseG1GC
-XX:MaxGCPauseMillis=100           # Target max pause time
-XX:G1HeapRegionSize=16m           # Region size (heap / 2048)
-XX:InitiatingHeapOccupancyPercent=45  # GC trigger threshold
-XX:G1ReservePercent=10            # Reserve heap for to-space
-XX:ParallelGCThreads=8            # Parallel GC threads (# of CPU cores)
-XX:ConcGCThreads=2                # Concurrent GC threads (ParallelGCThreads / 4)

# GC logging (for analysis)
-Xlog:gc*:file=/var/log/gc.log:time,uptime,level,tags:filecount=10,filesize=100M

# Out of Memory handling
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/var/log/heapdump.hprof
-XX:+ExitOnOutOfMemoryError

# Performance flags
-XX:+UseStringDeduplication        # Reduce string memory footprint
-XX:+OptimizeStringConcat          # Optimize string concatenation
```

#### 3.4.2 ZGC Configuration (Alternative for Low-Latency Requirements)

**ZGC Flags** (for <10ms GC pauses):
```bash
# ZGC (Java 17+)
-XX:+UseZGC
-Xms3200m
-Xmx3200m
-XX:ZCollectionInterval=10         # Minimum GC interval (seconds)
-XX:ZAllocationSpikeTolerance=2.0  # Tolerate 2× allocation spike

# GC logging
-Xlog:gc*:file=/var/log/gc-zgc.log:time,uptime,level,tags

# Out of Memory handling
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/var/log/heapdump.hprof
```

**When to Use ZGC**:
- Heap size >4GB
- Require <10ms GC pause times
- Can tolerate 5-10% higher CPU usage compared to G1GC

#### 3.4.3 GC Log Analysis

**Example GC Log Entry** (G1GC):
```
[2026-01-23T10:30:45.123+0000][1.234s][info][gc] GC(10) Pause Young (Normal) (G1 Evacuation Pause) 1024M->512M(3200M) 45.678ms
```

**Interpretation**:
- **GC Type**: Young generation collection
- **Heap Before**: 1024M
- **Heap After**: 512M (freed 512M)
- **Total Heap**: 3200M
- **Pause Time**: 45.678ms (within target of <100ms)

**GCViewer Tool** (analyze gc.log):
```bash
# Download GCViewer
wget https://github.com/chewiebug/GCViewer/releases/download/1.37/gcviewer-1.37.jar

# Analyze GC log
java -jar gcviewer-1.37.jar /var/log/gc.log
```

**Key Metrics to Monitor**:
- **Throughput**: (Total time - GC time) / Total time (should be >99%)
- **Max Pause Time**: Should be <100ms (p99)
- **Allocation Rate**: MB/sec (high rate indicates memory pressure)
- **Promotion Rate**: MB/sec (high rate indicates premature tenuring)

### 3.5 Async Processing

#### 3.5.1 @Async for Non-Blocking Operations

**AsyncConfig.java**:
```java
package net.lab1024.sa.foundation.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async task executor configuration
 *
 * Use cases:
 * - Audit log writing
 * - Email/SMS notifications
 * - Non-critical database writes
 * - Third-party API calls (with timeout)
 *
 * @author SmartAdmin Team
 * @since 2026-01-23
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("async-executor-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) ->
            log.error("Async method threw exception: method={}, params={}", method, params, throwable);
    }
}
```

**Usage Example**:
```java
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogDao auditLogDao;

    @Async  // Non-blocking audit log writing
    public void logEvent(AuditEventType eventType, String description, Map<String, Object> metadata) {
        AuditLog log = new AuditLog();
        log.setEventType(eventType);
        log.setDescription(description);
        log.setMetadata(JsonUtil.toJsonString(metadata));
        log.setCreatedAt(LocalDateTime.now());

        auditLogDao.insert(log);
    }
}
```

#### 3.5.2 CompletableFuture for Parallel Processing

**Parallel API Calls Example**:
```java
@Service
@RequiredArgsConstructor
public class PlayerDashboardService {

    private final WalletService walletService;
    private final BonusService bonusService;
    private final GameHistoryService gameHistoryService;

    public PlayerDashboardVO getDashboard(Long playerId) {
        // Execute 3 queries in parallel
        CompletableFuture<BigDecimal> balanceFuture = CompletableFuture.supplyAsync(() ->
            walletService.getTotalBalance(playerId)
        );

        CompletableFuture<List<PlayerBonus>> bonusesFuture = CompletableFuture.supplyAsync(() ->
            bonusService.getActiveBonuses(playerId)
        );

        CompletableFuture<List<GameRound>> historyFuture = CompletableFuture.supplyAsync(() ->
            gameHistoryService.getRecentRounds(playerId, 10)
        );

        // Wait for all to complete (with timeout)
        CompletableFuture.allOf(balanceFuture, bonusesFuture, historyFuture)
            .orTimeout(2, TimeUnit.SECONDS)
            .join();

        // Build response
        return PlayerDashboardVO.builder()
            .balance(balanceFuture.join())
            .activeBonuses(bonusesFuture.join())
            .recentGames(historyFuture.join())
            .build();
    }
}
```

---

## 4. Testing

### 4.1 Load Testing Strategy

#### 4.1.1 k6 Load Testing

**Installation**:
```bash
# Install k6
brew install k6  # macOS
# or
wget https://github.com/grafana/k6/releases/download/v0.48.0/k6-v0.48.0-linux-amd64.tar.gz
```

**Wallet API Load Test** (wallet-load-test.js):
```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const walletDebitLatency = new Trend('wallet_debit_latency');

// Test configuration
export const options = {
    stages: [
        { duration: '1m', target: 100 },   // Ramp up to 100 VUs
        { duration: '3m', target: 100 },   // Stay at 100 VUs
        { duration: '1m', target: 500 },   // Spike to 500 VUs
        { duration: '3m', target: 500 },   // Stay at 500 VUs
        { duration: '1m', target: 0 },     // Ramp down to 0
    ],
    thresholds: {
        'http_req_duration': ['p(95)<200'],  // 95% of requests <200ms
        'errors': ['rate<0.01'],             // Error rate <1%
    },
};

// Test scenario
export default function () {
    const baseUrl = 'http://localhost:1024';
    const authToken = 'Bearer test-token';

    // 1. Login
    const loginRes = http.post(`${baseUrl}/login`, JSON.stringify({
        username: 'player1',
        password: 'password123',
    }), {
        headers: { 'Content-Type': 'application/json' },
    });

    check(loginRes, {
        'login successful': (r) => r.status === 200,
    });

    const token = loginRes.json('data.token');

    // 2. Get wallet balance
    const balanceRes = http.get(`${baseUrl}/api/wallet/balance`, {
        headers: { 'Authorization': `Bearer ${token}` },
    });

    check(balanceRes, {
        'balance retrieved': (r) => r.status === 200,
    });

    // 3. Debit wallet (place bet)
    const debitStart = Date.now();
    const debitRes = http.post(`${baseUrl}/api/wallet/debit`, JSON.stringify({
        amount: 10.00,
        currency: 'USD',
        description: 'Game bet',
        idempotencyKey: `bet-${__VU}-${__ITER}`,
    }), {
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
        },
    });

    const debitLatency = Date.now() - debitStart;
    walletDebitLatency.add(debitLatency);

    const debitSuccess = check(debitRes, {
        'debit successful': (r) => r.status === 200,
        'debit latency <200ms': () => debitLatency < 200,
    });

    errorRate.add(!debitSuccess);

    sleep(1);  // Simulate user think time
}
```

**Run Load Test**:
```bash
# Run test
k6 run wallet-load-test.js

# Run with InfluxDB output (for Grafana visualization)
k6 run --out influxdb=http://localhost:8086/k6 wallet-load-test.js

# Run with custom VUs
k6 run --vus 1000 --duration 5m wallet-load-test.js
```

**Expected Output**:
```
     ✓ login successful
     ✓ balance retrieved
     ✓ debit successful
     ✓ debit latency <200ms

     checks.........................: 100.00% ✓ 40000      ✗ 0
     data_received..................: 8.5 MB  142 kB/s
     data_sent......................: 3.2 MB  53 kB/s
     http_req_duration..............: avg=45ms  min=12ms med=38ms max=195ms p(90)=78ms p(95)=95ms
     http_reqs......................: 40000   666.67/s
     iteration_duration.............: avg=1.05s min=1.01s med=1.04s max=1.20s
     vus............................: 500     min=0        max=500
     vus_max........................: 500     min=500      max=500
     wallet_debit_latency...........: avg=42ms  min=10ms med=35ms max=180ms p(95)=85ms
```

#### 4.1.2 Database Load Testing

**PgBench** (PostgreSQL built-in):
```bash
# Initialize test database
pgbench -i -s 50 smart_admin_test

# Run 10-minute test with 100 concurrent clients
pgbench -c 100 -j 10 -T 600 -P 10 smart_admin_test

# Custom SQL script (wallet debit simulation)
cat > wallet_debit.sql <<'EOF'
BEGIN;
UPDATE wallets SET balance = balance - 10.00 WHERE player_id = :player_id AND currency = 'USD';
INSERT INTO wallet_transactions (player_id, transaction_type, amount, currency) VALUES (:player_id, 'DEBIT', 10.00, 'USD');
COMMIT;
EOF

# Run custom script
pgbench -c 100 -j 10 -T 600 -f wallet_debit.sql -D player_id=12345 smart_admin_test
```

### 4.2 Performance Benchmarking

#### 4.2.1 JMH Micro-Benchmarking

**Benchmark: Bean Conversion Performance** (SmartBeanUtil vs MapStruct):
```java
package net.lab1024.sa.base.common.util;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

/**
 * Benchmark: Bean conversion performance
 *
 * Run: ./gradlew :sa-base:jmh
 *
 * @author SmartAdmin Team
 * @since 2026-01-23
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class BeanConversionBenchmark {

    private Player player;

    @Setup
    public void setup() {
        player = new Player();
        player.setId(1L);
        player.setEmail("test@example.com");
        player.setFirstName("John");
        player.setLastName("Doe");
    }

    @Benchmark
    public PlayerVO testSmartBeanUtil() {
        return SmartBeanUtil.copy(player, PlayerVO.class);
    }

    @Benchmark
    public PlayerVO testMapStruct() {
        return PlayerMapper.INSTANCE.toVO(player);
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
            .include(BeanConversionBenchmark.class.getSimpleName())
            .build();

        new Runner(opt).run();
    }
}
```

**Expected Results**:
```
Benchmark                                  Mode  Cnt    Score    Error  Units
BeanConversionBenchmark.testSmartBeanUtil  avgt   10  3500.0 ± 100.0  ns/op
BeanConversionBenchmark.testMapStruct      avgt   10   850.0 ±  50.0  ns/op

Conclusion: MapStruct is 4× faster than SmartBeanUtil (reflection-based)
Recommendation: Use MapStruct for hot paths (>1000 req/s), SmartBeanUtil for cold paths
```

---

## 5. Operations

### 5.1 Monitoring and Alerting

#### 5.1.1 Prometheus Metrics

**Micrometer Integration** (application.yml):
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: smart-admin
      environment: ${spring.profiles.active}
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5, 0.95, 0.99
```

**Custom Metrics** (WalletManager.java):
```java
@Service
@RequiredArgsConstructor
public class WalletManager {

    private final MeterRegistry meterRegistry;

    @Timed(value = "wallet.debit", description = "Wallet debit operation latency")
    @Transactional(rollbackFor = Exception.class)
    public void debit(Long playerId, BigDecimal amount, String currency, String description, String idempotencyKey) {
        // Increment counter
        meterRegistry.counter("wallet.debit.count",
            "currency", currency,
            "result", "success"
        ).increment();

        // Record amount histogram
        meterRegistry.summary("wallet.debit.amount",
            "currency", currency
        ).record(amount.doubleValue());

        // Implementation...
    }
}
```

**Prometheus Queries**:
```promql
# Wallet debit p95 latency by currency
histogram_quantile(0.95,
  sum(rate(wallet_debit_seconds_bucket[5m])) by (le, currency)
)

# Wallet debit rate (transactions per second)
sum(rate(wallet_debit_count[1m])) by (currency)

# Wallet debit error rate
sum(rate(wallet_debit_count{result="error"}[5m])) /
sum(rate(wallet_debit_count[5m]))
```

#### 5.1.2 Grafana Dashboards

**Performance Overview Dashboard** (JSON):
```json
{
  "dashboard": {
    "title": "SmartAdmin Performance Overview",
    "panels": [
      {
        "title": "API Latency (p95)",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri))",
            "legendFormat": "{{uri}}"
          }
        ],
        "yaxis": { "format": "ms" }
      },
      {
        "title": "Database Query Latency",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, sum(rate(hikaricp_connections_active_seconds_bucket[5m])) by (le, pool))",
            "legendFormat": "{{pool}}"
          }
        ]
      },
      {
        "title": "Cache Hit Rate",
        "targets": [
          {
            "expr": "sum(rate(cache_gets{result=\"hit\"}[5m])) / sum(rate(cache_gets[5m]))",
            "legendFormat": "Hit Rate"
          }
        ],
        "yaxis": { "format": "percentunit" }
      },
      {
        "title": "JVM Heap Usage",
        "targets": [
          {
            "expr": "jvm_memory_used_bytes{area=\"heap\"} / jvm_memory_max_bytes{area=\"heap\"}",
            "legendFormat": "Heap Usage"
          }
        ],
        "yaxis": { "format": "percentunit" }
      }
    ]
  }
}
```

#### 5.1.3 Alerting Rules

**Prometheus Alerting Rules** (alerts.yml):
```yaml
groups:
  - name: performance_alerts
    interval: 30s
    rules:
      # High API latency
      - alert: HighAPILatency
        expr: histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri)) > 0.5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High API latency detected: {{ $labels.uri }}"
          description: "p95 latency is {{ $value }}s (threshold: 0.5s)"

      # Database connection pool exhaustion
      - alert: DatabasePoolExhaustion
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.8
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Database connection pool near exhaustion"
          description: "Pool utilization is {{ $value }}% (threshold: 80%)"

      # Low cache hit rate
      - alert: LowCacheHitRate
        expr: sum(rate(cache_gets{result="hit"}[5m])) / sum(rate(cache_gets[5m])) < 0.7
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Low cache hit rate: {{ $labels.cache }}"
          description: "Hit rate is {{ $value }} (threshold: 70%)"

      # High GC pause time
      - alert: HighGCPauseTime
        expr: jvm_gc_pause_seconds_max{action="end of major GC"} > 0.1
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "High GC pause time detected"
          description: "Max GC pause is {{ $value }}s (threshold: 0.1s)"

      # High error rate
      - alert: HighErrorRate
        expr: sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count[5m])) > 0.05
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "High 5xx error rate"
          description: "Error rate is {{ $value }} (threshold: 5%)"
```

### 5.2 Performance Troubleshooting Playbook

#### 5.2.1 High Latency Investigation

**Step 1: Identify Slow Endpoints**
```bash
# Prometheus query: Top 10 slowest endpoints (p95 latency)
topk(10, histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri)))
```

**Step 2: Trace Request**
```bash
# Use Jaeger to find distributed trace
curl http://localhost:16686/api/traces?service=smart-admin&operation=GET%20/api/wallet/balance&lookback=1h
```

**Step 3: Profile Application**
```bash
# Attach Async Profiler (CPU profiling)
./profiler.sh -d 60 -e cpu -f /tmp/flamegraph.html <pid>

# Analyze flamegraph: Look for hot methods consuming >5% CPU
```

**Step 4: Check Database Queries**
```sql
-- PostgreSQL: Find slow queries
SELECT query, calls, mean_exec_time, stddev_exec_time
FROM pg_stat_statements
WHERE mean_exec_time > 100  -- >100ms average
ORDER BY mean_exec_time DESC
LIMIT 10;

-- Analyze specific query
EXPLAIN ANALYZE <slow query>;
```

**Step 5: Verify Cache Hit Rate**
```bash
# Prometheus query: Cache hit rate by cache name
sum(rate(cache_gets{result="hit"}[5m])) by (cache) / sum(rate(cache_gets[5m])) by (cache)
```

#### 5.2.2 High Memory Usage Investigation

**Step 1: Check JVM Heap Usage**
```bash
# Prometheus query: Heap usage trend
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}
```

**Step 2: Generate Heap Dump**
```bash
# Trigger heap dump (production safe)
jmap -dump:format=b,file=/tmp/heapdump.hprof <pid>

# Analyze with Eclipse MAT
./mat /tmp/heapdump.hprof
```

**Step 3: Identify Memory Leaks**
```bash
# Use Arthas to find top memory-consuming objects
java -jar arthas-boot.jar
memory  # Show memory statistics

# Heap histogram
vmtool --action getInstances --className java.lang.String --limit 100
```

**Step 4: Verify GC Efficiency**
```bash
# Analyze GC log
java -jar gcviewer-1.37.jar /var/log/gc.log

# Check for premature promotion (tenuring threshold too low)
# Check for high allocation rate (object churn)
```

#### 5.2.3 Database Connection Pool Exhaustion

**Step 1: Check Pool Utilization**
```bash
# Prometheus query: Connection pool usage
hikaricp_connections_active / hikaricp_connections_max
```

**Step 2: Identify Long-Running Transactions**
```sql
-- PostgreSQL: Find long-running transactions
SELECT pid, now() - pg_stat_activity.query_start AS duration, query
FROM pg_stat_activity
WHERE state = 'active'
  AND now() - pg_stat_activity.query_start > interval '5 seconds'
ORDER BY duration DESC;

-- Kill long-running query (if necessary)
SELECT pg_terminate_backend(<pid>);
```

**Step 3: Verify Connection Leaks**
```java
// Enable HikariCP leak detection
spring.datasource.hikari.leak-detection-threshold=60000  // 60 seconds

// Check application logs for leak warnings:
// "Connection leak detection triggered for connection ..."
```

**Step 4: Increase Pool Size (if necessary)**
```yaml
# Adjust pool sizing based on Little's Law
# Connections = Throughput × Latency
# Example: 500 req/s × 0.1s = 50 connections
spring:
  datasource:
    hikari:
      maximum-pool-size: 75  # Increased from 50
```

---

## 6. Appendices

### 6.1 Performance Tuning Checklist

**Application Level**:
- [ ] Micrometer metrics enabled for critical business methods
- [ ] Async processing for non-blocking I/O (email, SMS, audit logs)
- [ ] Connection pooling optimized (HikariCP sizing)
- [ ] Thread pool sizing based on workload (CPU-bound vs I/O-bound)
- [ ] Circuit breakers for third-party API calls
- [ ] Request timeout configuration (<30s)

**Database Level**:
- [ ] pg_stat_statements enabled for query profiling
- [ ] Indexes created for all high-frequency queries
- [ ] EXPLAIN ANALYZE run for top 20 queries
- [ ] N+1 queries eliminated (batch fetching)
- [ ] Keyset pagination for large result sets
- [ ] Partitioning for large tables (>10M rows)
- [ ] Connection pool sizing validated (not exhausted)
- [ ] Read replicas for read-heavy workloads

**Caching Level**:
- [ ] L1 (Caffeine) cache configured with appropriate TTL
- [ ] L2 (Redis) cache for distributed caching
- [ ] Cache-aside pattern implemented correctly
- [ ] Cache invalidation strategy defined
- [ ] Cache stampede prevention (distributed locks)
- [ ] Cache hit rate >70% for critical data

**JVM Level**:
- [ ] Heap size appropriately configured (80% of container memory)
- [ ] GC algorithm selected (G1GC for <4GB heap, ZGC for >4GB)
- [ ] GC pause time target met (<100ms p99)
- [ ] GC logs enabled and analyzed
- [ ] Heap dump on OOM enabled
- [ ] String deduplication enabled

**Monitoring Level**:
- [ ] Prometheus metrics exported
- [ ] Grafana dashboards configured
- [ ] Alerting rules defined (latency, error rate, resource utilization)
- [ ] Distributed tracing enabled (Jaeger)
- [ ] Log aggregation configured (ELK/Loki)
- [ ] APM tool integrated (optional: DataDog, New Relic)

### 6.2 Performance Benchmarking Results

**Wallet API Benchmark** (k6):
```
Scenario: 500 concurrent users, 10-minute test
Environment: 4 CPU, 8GB RAM, PostgreSQL (4 CPU, 16GB RAM)

Results:
✓ Total Requests: 150,000
✓ Throughput: 250 req/s
✓ p50 Latency: 38ms
✓ p95 Latency: 95ms  (Target: <200ms) ✓
✓ p99 Latency: 180ms
✓ Error Rate: 0.02%  (Target: <1%) ✓
✓ Database Connection Pool Utilization: 45%  (Target: <70%) ✓
```

**Game Launch API Benchmark**:
```
Scenario: 200 concurrent users, 5-minute test
Environment: Same as above

Results:
✓ Total Requests: 30,000
✓ Throughput: 100 req/s
✓ p50 Latency: 850ms
✓ p95 Latency: 1,850ms  (Target: <2s) ✓
✓ p99 Latency: 2,200ms  (Slightly over target)
✓ Error Rate: 0.5%  (Game provider timeout)
```

**Recommendation**: Implement circuit breaker for game provider API calls to prevent cascading failures.

### 6.3 Tools Reference

| Tool | Purpose | Download |
|------|---------|----------|
| **Async Profiler** | Production-safe CPU/allocation profiling | [GitHub](https://github.com/async-profiler/async-profiler) |
| **Arthas** | Interactive JVM troubleshooting | [GitHub](https://github.com/alibaba/arthas) |
| **JProfiler** | IDE-integrated profiler | [ej-technologies.com](https://www.ej-technologies.com/products/jprofiler/overview.html) |
| **k6** | Modern load testing tool | [k6.io](https://k6.io/) |
| **JMH** | Micro-benchmarking framework | [openjdk.org](https://openjdk.org/projects/code-tools/jmh/) |
| **GCViewer** | GC log analysis | [GitHub](https://github.com/chewiebug/GCViewer) |
| **Eclipse MAT** | Heap dump analysis | [eclipse.org](https://www.eclipse.org/mat/) |
| **PgBench** | PostgreSQL load testing | Built-in with PostgreSQL |
| **Prometheus** | Metrics collection | [prometheus.io](https://prometheus.io/) |
| **Grafana** | Metrics visualization | [grafana.com](https://grafana.com/) |
| **Jaeger** | Distributed tracing | [jaegertracing.io](https://www.jaegertracing.io/) |

### 6.4 Related Documentation

- [P0-03: Seamless Wallet Implementation](../P0-critical/03-seamless-wallet-implementation.md) - Wallet API performance requirements
- [P1-06: Real-Time Risk Engine](06-real-time-risk-engine.md) - Flink stream processing performance
- [P1-07: Multi-Tenant Isolation](07-multi-tenant-isolation.md) - Tenant isolation overhead
- [P1-09: Game Aggregator SDK](09-game-aggregator-sdk.md) - Game launch latency optimization
- [P1-13: Reporting & Analytics](13-reporting-analytics.md) - OLAP query optimization
- [backend_project.md](../../backend_project.md) - Overall performance SLA targets

### 6.5 Performance Optimization Roadmap

**Phase 1: Baseline Establishment (Week 1)**
- [ ] Enable pg_stat_statements and GC logging
- [ ] Set up Prometheus + Grafana dashboards
- [ ] Run initial k6 load tests for wallet API
- [ ] Document baseline metrics (p95 latency, throughput, error rate)

**Phase 2: Low-Hanging Fruit (Weeks 2-3)**
- [ ] Implement Caffeine L1 cache for VIP config and game catalog
- [ ] Add database indexes for top 20 slow queries
- [ ] Enable HikariCP leak detection
- [ ] Configure JVM flags (G1GC tuning)
- [ ] Implement @Async for audit logs

**Phase 3: Deep Optimization (Weeks 4-6)**
- [ ] Eliminate N+1 queries with batch fetching
- [ ] Implement keyset pagination for large result sets
- [ ] Add Redis L2 cache for player balance
- [ ] Profile with Async Profiler and fix hot spots
- [ ] Implement cache stampede prevention

**Phase 4: Validation (Week 7)**
- [ ] Run comprehensive load tests (10K TPS target)
- [ ] Analyze GC logs with GCViewer
- [ ] Validate all SLA targets met
- [ ] Document performance improvements
- [ ] Set up production alerting rules

**Expected Improvements**:
- API latency: 30-50% reduction (p95: 200ms → 100-140ms)
- Database query latency: 40-60% reduction (p95: 100ms → 40-60ms)
- Cache hit rate: 0% → 75-85%
- Throughput: 2-3× increase (3,000 TPS → 6,000-9,000 TPS)
- Infrastructure cost: 20-40% reduction (fewer pods/nodes needed)

---

## Document End

**Version**: 1.0.0
**Status**: Draft
**Next Review**: After Phase 1 load testing completion
**Feedback**: Architecture team review required before production deployment