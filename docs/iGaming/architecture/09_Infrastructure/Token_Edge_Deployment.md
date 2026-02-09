# Token 邊緣部署與運維 (Token Edge Deployment)

> **Canonical Source**: [09-13-03 Edge Deployment](../../source-archive/09_Technical_Infrastructure/09-13-03_Edge_Deployment.md)
> **View**: Technical Architecture (Development & DevOps)

---

## 1. 重放攻擊防護

### 1.1 Nonce 驗證機制

**Nonce（Number Once）**: 隨機字符串，確保每個請求唯一，防止攻擊者截獲並重放合法請求。

**Java 實現（Redis SETNX 原子操作）**:

```java
@Service
@RequiredArgsConstructor
public class NonceValidator {

    private final RedisTemplate<String, String> redisTemplate;
    private static final Duration NONCE_WINDOW = Duration.ofMinutes(5);

    /**
     * Validate and store Nonce
     *
     * @param nonce UUID format random string
     * @return true if first use, false if reuse (replay attack)
     */
    public boolean validateAndStore(String nonce) {
        String key = "nonce:" + nonce;

        // Use Redis SETNX (atomic operation)
        Boolean success = redisTemplate.opsForValue().setIfAbsent(
            key,
            String.valueOf(Instant.now().getEpochSecond()),
            NONCE_WINDOW
        );

        if (Boolean.FALSE.equals(success)) {
            // Nonce already exists (replay attack)
            auditLogService.log(null, "NONCE_REUSED", "Nonce: " + nonce);
            return false;
        }

        return true;
    }
}
```

**Redis Lua 腳本優化**（避免 Race Condition）:

```lua
-- nonce_check.lua
local key = KEYS[1]
local value = ARGV[1]
local ttl = tonumber(ARGV[2])

-- Check if Nonce exists
if redis.call('EXISTS', key) == 1 then
    return 0  -- Nonce exists (replay attack)
else
    -- Store Nonce
    redis.call('SET', key, value, 'EX', ttl)
    return 1  -- First use
end
```

**Java Lua 腳本調用**:

```java
public boolean validateNonceWithLua(String nonce) {
    String script = """
        local key = KEYS[1]
        local value = ARGV[1]
        local ttl = tonumber(ARGV[2])
        if redis.call('EXISTS', key) == 1 then
            return 0
        else
            redis.call('SET', key, value, 'EX', ttl)
            return 1
        end
        """;

    Long result = redisTemplate.execute(
        new DefaultRedisScript<>(script, Long.class),
        Collections.singletonList("nonce:" + nonce),
        String.valueOf(Instant.now().getEpochSecond()),
        String.valueOf(NONCE_WINDOW.toSeconds())
    );

    return result != null && result == 1;
}
```

### 1.2 時間戳驗證

**防止重放舊請求**（允許 +/- 5 分鐘偏移，考慮時區及 NTP 同步延遲）:

```java
public boolean validateTimestamp(long timestamp) {
    long now = Instant.now().getEpochSecond();
    long diff = Math.abs(now - timestamp);

    // Allow +/- 5 minute offset (timezone + NTP sync delay)
    if (diff > 300) {
        auditLogService.log(null, "TIMESTAMP_EXPIRED", String.format(
            "Timestamp: %d, Now: %d, Diff: %d seconds", timestamp, now, diff
        ));
        return false;
    }

    return true;
}
```

### 1.3 Token Rotation

**Refresh Token 輪換核心邏輯**:

1. 用戶使用 Refresh Token 刷新 Access Token
2. 服務器頒發新的 Access Token + **新的 Refresh Token**
3. **舊的 Refresh Token 立即失效**（加入黑名單）

**防止重放攻擊**:
- 攻擊者即使截獲 Refresh Token，也只能使用一次
- 下次刷新時，服務器會檢測到重複使用（已在黑名單中），拒絕請求並觸發安全警報

---

## 2. 限流與熔斷

### 2.1 限流策略

**Token Bucket 算法配置**:

| 角色 | 桶容量 | 補充速率 | 時間窗口 | 超出後處理 |
|------|-------|---------|---------|-----------|
| **玩家** | 100 tokens | 100 tokens/min | 1 分鐘 | HTTP 429 + Retry-After: 60 |
| **遊戲商** | 1000 tokens | 1000 tokens/min | 1 分鐘 | HTTP 429 |
| **第三方平台** | 200 tokens | 200 tokens/min | 1 分鐘 | HTTP 429 |
| **後台用戶** | 50 tokens | 50 tokens/min | 1 分鐘 | HTTP 429 |

**分層限流架構**:

```mermaid
graph TD
    A[Token 驗證請求] --> B{L1: 全局限流<br/>100,000 QPS}
    B -->|超出| C[HTTP 429<br/>Service Overload]
    B -->|通過| D{L2: 用戶級限流<br/>100 tokens/min}
    D -->|超出| E[HTTP 429<br/>Rate Limit Exceeded]
    D -->|通過| F{L3: IP 級限流<br/>1000 tokens/min}
    F -->|超出| G[HTTP 429<br/>Suspicious Activity]
    F -->|通過| H[執行 Token 驗證邏輯]
```

### 2.2 熔斷器設計

**Resilience4j 熔斷器配置**:

```java
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

@Configuration
public class CircuitBreakerConfiguration {

    @Bean
    public CircuitBreaker postgresCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)           // 50% failure rate triggers circuit break
            .waitDurationInOpenState(Duration.ofSeconds(30))  // 30s open duration
            .slidingWindowSize(100)             // Evaluate last 100 requests
            .minimumNumberOfCalls(10)           // Minimum 10 calls before evaluation
            .build();

        return CircuitBreakerRegistry.of(config)
            .circuitBreaker("postgres");
    }
}

// Usage example
@Service
@RequiredArgsConstructor
public class TokenDatabaseService {

    private final CircuitBreaker postgresCircuitBreaker;
    private final TokenRepository tokenRepository;

    public TokenValidationResult queryFromDatabase(String tokenId) {
        return CircuitBreaker.decorateSupplier(
            postgresCircuitBreaker,
            () -> tokenRepository.findByTokenId(tokenId).orElse(null)
        ).get();
    }
}
```

**熔斷器狀態機**:

```mermaid
stateDiagram-v2
    [*] --> CLOSED
    CLOSED --> OPEN: Failure rate >= 50%
    OPEN --> HALF_OPEN: After 30s wait
    HALF_OPEN --> CLOSED: Test success (10 calls >= 80% success)
    HALF_OPEN --> OPEN: Test failed
    CLOSED --> CLOSED: Normal requests
    OPEN --> OPEN: Reject all requests
```

### 2.3 降級方案

**當 PostgreSQL 不可用時的降級策略**:

```java
@Service
@RequiredArgsConstructor
public class TokenValidationService {

    private final CircuitBreaker postgresCircuitBreaker;
    private final RedisTemplate<String, String> redisTemplate;

    public TokenValidationResult validate(String token, ActorType actorType) {
        // Step 1: Try L1 + L2 cache
        TokenValidationResult result = queryFromCache(token);
        if (result != null) {
            return result;
        }

        // Step 2: Try PostgreSQL (with circuit breaker protection)
        try {
            result = CircuitBreaker.decorateSupplier(
                postgresCircuitBreaker,
                () -> queryFromDatabase(token)
            ).get();

            if (result != null) {
                return result;
            }

        } catch (Exception e) {
            // PostgreSQL unavailable, enter degraded mode
            log.warn("PostgreSQL circuit breaker open, entering degraded mode");
        }

        // Step 3: Degraded mode - JWT local validation (signature + expiry only)
        if (token.startsWith("eyJ")) {  // JWT Token
            try {
                result = validateJwtLocally(token);
                log.info("Degraded mode: JWT validated locally without database");
                return result;
            } catch (Exception e) {
                log.error("JWT local validation failed", e);
            }
        }

        // Step 4: Cannot validate, return error
        throw new TokenValidationException("Unable to validate token: database unavailable");
    }

    /**
     * Degraded mode: JWT local validation (signature + expiry only, no blacklist check)
     */
    private TokenValidationResult validateJwtLocally(String token) {
        DecodedJWT jwt = JWT.require(Algorithm.RSA256(publicKey, null))
            .build()
            .verify(token);

        if (jwt.getExpiresAt().before(new Date())) {
            throw new TokenExpiredException("Token expired");
        }

        Long userId = jwt.getClaim("userId").asLong();
        String actorType = jwt.getClaim("actorType").asString();

        return TokenValidationResult.builder()
            .valid(true)
            .userId(userId)
            .actorType(ActorType.valueOf(actorType))
            .degradedMode(true)  // Mark as degraded mode
            .build();
    }
}
```

**降級模式限制**:
- 無法檢查黑名單（已封禁的玩家可能仍能訪問）
- 無法檢查 Device Fingerprint（跨設備訪問無法檢測）
- 仍可驗證簽名和過期時間（基本安全保證）

---

## 3. 高可用設計

### 3.1 Multi-AZ 部署架構

```mermaid
graph TD
    subgraph Internet
        A[Kong API Gateway<br/>Load Balancer]
    end

    subgraph AZ-1 us-east-1a
        B[Token Validation<br/>Service Instance 1]
        C[(Redis Master)]
        D[(PostgreSQL Primary)]
    end

    subgraph AZ-2 us-east-1b
        E[Token Validation<br/>Service Instance 2]
        F[(Redis Slave 1)]
        G[(PostgreSQL Replica 1)]
    end

    subgraph AZ-3 us-east-1c
        H[Token Validation<br/>Service Instance 3]
        I[(Redis Slave 2)]
        J[(PostgreSQL Replica 2)]
    end

    A --> B
    A --> E
    A --> H

    B --> C
    B --> D

    E --> F
    E --> G

    H --> I
    H --> J

    C -->|Replication| F
    C -->|Replication| I

    D -->|Streaming Replication| G
    D -->|Streaming Replication| J

    K[Redis Sentinel] --> C
    K --> F
    K --> I
```

**高可用配置**:

| 組件 | 配置 | 說明 |
|------|------|------|
| **Token Validation Service** | 3 實例（每個 AZ 1 個） | Kubernetes Deployment（3 Replicas） |
| **Redis** | 1 Master + 2 Slaves + 3 Sentinels | 自動故障轉移 |
| **PostgreSQL** | 1 Primary + 2 Replicas | Streaming Replication（同步模式） |
| **Kong API Gateway** | 2 實例（跨 AZ） | Active-Active 負載均衡 |

### 3.2 故障轉移

**Redis Sentinel 配置**:

```bash
# Sentinel configuration
sentinel monitor mymaster 10.0.1.100 6379 2  # 2 Sentinels agree to failover
sentinel down-after-milliseconds mymaster 5000  # 5s no response = down
sentinel failover-timeout mymaster 60000  # 60s failover timeout
sentinel parallel-syncs mymaster 1  # 1 Slave sync at a time
```

**故障轉移流程**:

```mermaid
sequenceDiagram
    participant S1 as Sentinel 1
    participant S2 as Sentinel 2
    participant S3 as Sentinel 3
    participant M as Redis Master
    participant R1 as Redis Slave 1
    participant R2 as Redis Slave 2

    Note over M: Redis Master Down

    S1->>M: PING (no response)
    S1->>S1: Mark Master as SDOWN<br/>(Subjective Down)

    S1->>S2: IS-MASTER-DOWN-BY-ADDR?
    S2-->>S1: YES

    S1->>S3: IS-MASTER-DOWN-BY-ADDR?
    S3-->>S1: YES

    Note over S1: Quorum reached (2/3)<br/>Mark as ODOWN (Objective Down)

    S1->>S1: Initiate Failover<br/>Elect Leader Sentinel

    S1->>R1: SLAVEOF NO ONE<br/>(Promote to Master)

    R1->>R1: Role change to Master

    S1->>R2: SLAVEOF 10.0.1.101 6379<br/>(Point to new Master)

    Note over R1: New Master accepts writes

    S1->>S1: Update config<br/>Notify applications
```

**應用程序自動重連配置**:

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration()
            .master("mymaster")
            .sentinel("10.0.1.200", 26379)
            .sentinel("10.0.2.200", 26379)
            .sentinel("10.0.3.200", 26379);

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .clientOptions(ClientOptions.builder()
                .autoReconnect(true)  // Auto-reconnect
                .build())
            .build();

        return new LettuceConnectionFactory(sentinelConfig, clientConfig);
    }
}
```

### 3.3 災難恢復

**備份策略**:

| 組件 | 備份方式 | 頻率 | 保留期限 |
|------|---------|------|---------|
| **Redis** | AOF（Append-Only File） | 實時 | 7 天 |
| **PostgreSQL** | WAL 歸檔 + 基礎備份 | 每天 | 30 天 |
| **配置文件** | Git 版本控制 | 每次變更 | 永久 |

**PostgreSQL 備份腳本**:

```bash
#!/bin/bash
# postgresql_backup.sh

BACKUP_DIR="/backups/postgresql"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/token_db_$TIMESTAMP.sql.gz"

# Execute pg_dump (Token-related tables)
pg_dump -h localhost -U postgres -d token_db \
  --table=t_access_token \
  --table=t_refresh_token \
  --table=t_game_provider_token \
  | gzip > $BACKUP_FILE

# Delete backups older than 30 days
find $BACKUP_DIR -name "token_db_*.sql.gz" -mtime +30 -delete

echo "Backup completed: $BACKUP_FILE"
```

**災難恢復演練**（每季度執行）:
1. **模擬 AZ 故障**: 關閉 AZ-1 的所有服務
2. **驗證自動故障轉移**: 確認流量自動路由到 AZ-2 和 AZ-3
3. **恢復 AZ-1**: 從備份恢復數據，重新加入集群
4. **測試完整功能**: 執行端到端測試

---

## 4. 性能優化

### 4.1 性能指標

**SLI（Service Level Indicators）目標**:

| 指標 | P50 | P90 | P99 | P99.9 |
|------|-----|-----|-----|-------|
| **延遲（L1 命中）** | < 1ms | < 2ms | < 5ms | < 10ms |
| **延遲（L2 命中）** | < 5ms | < 8ms | < 15ms | < 30ms |
| **延遲（L3 命中）** | < 50ms | < 80ms | < 150ms | < 300ms |
| **吞吐量（單實例）** | 10,000 QPS | 12,000 QPS | 15,000 QPS | 20,000 QPS |

### 4.2 優化策略

**1. 連接池優化（HikariCP）**:

```yaml
# HikariCP configuration (PostgreSQL)
spring:
  datasource:
    hikari:
      maximum-pool-size: 50   # Pool size = (CPU cores x 2) + spindle count
      minimum-idle: 10
      connection-timeout: 5000   # 5s timeout
      idle-timeout: 600000       # 10min idle timeout
      max-lifetime: 1800000      # 30min max lifetime
```

**2. 批量處理優化（Redis Pipeline）**:

```java
// Use Redis Pipeline for batch queries (10x performance improvement)
public List<TokenValidationResult> validateBatch(List<String> tokens) {
    return redisTemplate.executePipelined(new RedisCallback<Object>() {
        @Override
        public Object doInRedis(RedisConnection connection) {
            for (String token : tokens) {
                connection.get(("token:access:" + extractTokenId(token)).getBytes());
            }
            return null;
        }
    }).stream()
        .map(result -> result != null ? deserialize((byte[]) result) : null)
        .collect(Collectors.toList());
}
```

**3. JWT 簽名驗證優化（公鑰緩存）**:

```java
// Cache public key (avoid file I/O on every validation)
@Component
public class JwtPublicKeyCache {

    private RSAPublicKey cachedPublicKey;

    @PostConstruct
    public void loadPublicKey() throws Exception {
        String publicKeyContent = Files.readString(Path.of("/keys/public_key.pem"));
        byte[] keyBytes = Base64.getDecoder().decode(publicKeyContent
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s", ""));

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        cachedPublicKey = (RSAPublicKey) keyFactory.generatePublic(keySpec);
    }

    public RSAPublicKey getPublicKey() {
        return cachedPublicKey;
    }
}
```

### 4.3 壓力測試結果

**測試環境**:
- AWS EC2 c5.2xlarge（8 vCPU, 16 GB RAM）
- Redis Cluster（r5.large x 3）
- PostgreSQL（db.r5.xlarge, Primary + 2 Replicas）
- JMeter 5.5（1000 並發用戶）

**測試結果**:

| 場景 | 吞吐量 | 平均延遲 | P99 延遲 | 錯誤率 | 緩存命中率 |
|------|-------|---------|---------|-------|-----------|
| **正常負載（1,000 QPS）** | 1,200 QPS | 5ms | 15ms | 0% | 99.5% |
| **高負載（10,000 QPS）** | 11,500 QPS | 12ms | 45ms | 0.1% | 98.2% |
| **峰值負載（20,000 QPS）** | 18,000 QPS | 35ms | 120ms | 1.2% | 95.0% |
| **極限負載（50,000 QPS）** | 32,000 QPS | 180ms | 800ms | 15% | 80.0% |

**結論**:
- 單實例可支持 **10,000 QPS**（滿足目標）
- P99 延遲 < 50ms（正常負載下）
- 超過 20,000 QPS 時需要水平擴展（增加實例數）

---

## 5. 監控與告警

### 5.1 Prometheus 指標定義

```java
@Component
public class TokenValidationMetrics {

    private final Counter validationTotal;
    private final Counter validationSuccess;
    private final Counter validationFailure;
    private final Histogram validationLatency;
    private final Gauge cacheHitRate;

    public TokenValidationMetrics(MeterRegistry meterRegistry) {
        // Total validation count
        validationTotal = Counter.builder("token_validation_total")
            .description("Total number of token validation requests")
            .tag("actor_type", "all")
            .register(meterRegistry);

        // Successful validation count
        validationSuccess = Counter.builder("token_validation_success")
            .description("Number of successful validations")
            .tag("actor_type", "all")
            .register(meterRegistry);

        // Failed validation count
        validationFailure = Counter.builder("token_validation_failure")
            .description("Number of failed validations")
            .tag("error_code", "unknown")
            .register(meterRegistry);

        // Validation latency distribution
        validationLatency = Histogram.builder("token_validation_latency")
            .description("Token validation latency in milliseconds")
            .buckets(1, 5, 10, 50, 100, 500, 1000)
            .register(meterRegistry);

        // Cache hit rate
        cacheHitRate = Gauge.builder("token_cache_hit_rate", this, m -> calculateHitRate())
            .description("Cache hit rate (L1 + L2)")
            .register(meterRegistry);
    }

    private double calculateHitRate() {
        long l1Hits = caffeineCache.stats().hitCount();
        long l2Hits = redisHitCounter.get();
        long totalRequests = validationTotal.count();

        return (double) (l1Hits + l2Hits) / totalRequests;
    }
}
```

### 5.2 告警規則

**Prometheus AlertManager 規則**（`token_validation_alerts.yml`）:

```yaml
groups:
  - name: token_validation
    interval: 30s
    rules:
      # 1. High validation failure rate
      - alert: TokenValidationHighFailureRate
        expr: |
          (rate(token_validation_failure[5m]) / rate(token_validation_total[5m])) > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Token validation failure rate > 5%"
          description: "{{ $value | humanizePercentage }} of validations are failing"

      # 2. High P99 latency
      - alert: TokenValidationHighLatency
        expr: |
          histogram_quantile(0.99, rate(token_validation_latency_bucket[5m])) > 100
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "P99 latency > 100ms"
          description: "P99 latency is {{ $value }}ms"

      # 3. Low cache hit rate
      - alert: TokenCacheLowHitRate
        expr: |
          token_cache_hit_rate < 0.90
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Cache hit rate < 90%"
          description: "Current hit rate: {{ $value | humanizePercentage }}"

      # 4. Redis unavailable
      - alert: RedisDown
        expr: |
          up{job="redis"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Redis instance is down"
          description: "Redis at {{ $labels.instance }} is unreachable"

      # 5. PostgreSQL unavailable
      - alert: PostgresDown
        expr: |
          up{job="postgres"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "PostgreSQL instance is down"
          description: "PostgreSQL at {{ $labels.instance }} is unreachable"
```

**告警通知渠道**:

| 嚴重級別 | 通知渠道 | 響應時間 |
|---------|---------|---------|
| **Critical** | PagerDuty + Slack + SMS | < 5 分鐘 |
| **Warning** | Slack | < 30 分鐘 |
| **Info** | Slack（靜默通知） | - |

---

## 6. 實施路線圖

### 6.1 Phase 1: 核心驗證功能

**時間**: Week 1-2（10 工作日）

| 任務 | 負責人 | 工時 | 依賴 |
|------|-------|------|------|
| 設計 API 接口（OpenAPI 3.0 規範） | Backend Dev | 0.5 天 | - |
| 實施 Token 解析邏輯（JWT + Opaque） | Backend Dev | 2 天 | - |
| 實施三層緩存（Caffeine + Redis + PostgreSQL） | Backend Dev | 3 天 | Task 2 |
| 實施黑名單檢查 | Backend Dev | 1 天 | Task 3 |
| 實施基礎限流（Token Bucket） | Backend Dev | 1.5 天 | Task 3 |
| 實施 Prometheus 監控 | DevOps | 1 天 | Task 3 |
| 單元測試 + 集成測試 | QA | 2 天 | All |
| 部署到測試環境 | DevOps | 0.5 天 | All |

**交付物**: 單一驗證接口、三層緩存（99% 命中率）、基礎限流、Prometheus 監控

### 6.2 Phase 2: 高級特性

**時間**: Week 3（5 工作日）

| 任務 | 負責人 | 工時 | 依賴 |
|------|-------|------|------|
| 實施批量驗證接口 | Backend Dev | 1.5 天 | Phase 1 |
| 實施撤銷接口 | Backend Dev | 1 天 | Phase 1 |
| 實施 Nonce 驗證（防重放攻擊） | Backend Dev | 1 天 | Phase 1 |
| 實施 HMAC-SHA256 簽名驗證（Game Provider） | Backend Dev | 1.5 天 | Phase 1 |
| 測試與驗證 | QA | 1 天 | All |

**交付物**: 批量驗證、撤銷接口、防重放攻擊、GP HMAC 驗證

### 6.3 Phase 3: 性能優化

**時間**: Week 4（5 工作日）

| 任務 | 負責人 | 工時 | 依賴 |
|------|-------|------|------|
| 實施熔斷器（Resilience4j） | Backend Dev | 1 天 | Phase 1, 2 |
| 實施降級方案（JWT 本地驗證） | Backend Dev | 1 天 | Phase 1, 2 |
| 壓力測試（JMeter 50,000 QPS） | QA | 1.5 天 | Phase 1, 2 |
| Multi-AZ 部署（Kubernetes） | DevOps | 1.5 天 | Phase 1, 2 |
| 生產環境上線 | DevOps | 1 天 | All |

**交付物**: 熔斷器、降級方案、壓力測試通過（10,000 QPS）、Multi-AZ 部署（99.95% SLA）

---

## 7. 附錄

### 7.1 常見問題

**Q1: 為什麼不使用 Gateway 的 JWT 驗證插件（Kong JWT Plugin）？**
- Kong JWT Plugin 只能驗證 JWT 簽名和過期時間，無法檢查黑名單
- 不支持 Opaque Token（Game Provider）
- 不支持 Nonce 驗證（防重放攻擊）
- 無法實施自定義限流策略（例如按角色類型限流）

**Q2: 三層緩存是否增加複雜度？**
- 性能提升顯著: 99% 命中率（L1），延遲 < 1ms
- 降低數據庫負載: 99.9% 的請求不需要訪問 PostgreSQL
- 需注意緩存一致性: 使用 Redis Pub/Sub 保證黑名單實時生效

**Q3: Token Validation Service 是否會成為單點故障？**
- Multi-AZ 部署（3 個實例，每個 AZ 1 個）
- Kubernetes 自動重啟（MTTR < 5 分鐘）
- 降級方案（JWT 本地驗證）
- Redis + PostgreSQL 高可用架構

### 7.2 測試用例

| 測試 ID | 測試場景 | 預期結果 |
|---------|---------|---------|
| TC-TV-001 | 驗證有效的 JWT Access Token | HTTP 200，返回用戶信息 |
| TC-TV-002 | 驗證已過期的 JWT Token | HTTP 401，TOKEN_EXPIRED |
| TC-TV-003 | 驗證簽名錯誤的 JWT Token | HTTP 401，TOKEN_INVALID_SIGNATURE |
| TC-TV-004 | 驗證在黑名單中的 Token | HTTP 401，TOKEN_BLACKLISTED |
| TC-TV-005 | 驗證格式錯誤的 Token | HTTP 400，TOKEN_MALFORMED |
| TC-TV-006 | 批量驗證 100 個 Token | HTTP 200，返回 100 個驗證結果 |
| TC-TV-007 | 撤銷單個 Token | 下次驗證返回 TOKEN_BLACKLISTED |
| TC-TV-008 | 撤銷用戶所有 Token | 該用戶所有 Token 失效 |
| TC-TV-009 | Nonce 首次使用 | 驗證成功 |
| TC-TV-010 | Nonce 重複使用（重放攻擊） | HTTP 403，NONCE_REUSED |
| TC-TV-011 | 時間戳過期（> 5 分鐘） | HTTP 403，TIMESTAMP_EXPIRED |
| TC-TV-012 | 超出限流（101 次/分鐘） | HTTP 429，RATE_LIMIT_EXCEEDED |
| TC-TV-013 | L1 緩存命中 | 延遲 < 5ms |
| TC-TV-014 | L2 緩存命中 | 延遲 < 15ms |
| TC-TV-015 | L3 數據庫查詢 | 延遲 < 50ms |

**集成測試範例**（Spring Boot + JUnit 5）:

```java
@SpringBootTest
@AutoConfigureMockMvc
class TokenValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    @DisplayName("TC-TV-004: Validate blacklisted Token")
    void testBlacklistedToken() throws Exception {
        // Given
        String token = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...";
        String tokenId = "abc123xyz";

        // Add Token to blacklist
        redisTemplate.opsForValue().set(
            "blacklist:token:" + tokenId,
            "{\"userId\": 12345, \"reason\": \"FRAUD_DETECTED\"}",
            Duration.ofMinutes(15)
        );

        // When
        mockMvc.perform(post("/api/token/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"actorType\":\"PLAYER\"}"))
            // Then
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.data.valid").value(false))
            .andExpect(jsonPath("$.data.errorCode").value("TOKEN_BLACKLISTED"));
    }

    @Test
    @DisplayName("TC-TV-010: Nonce reuse (replay attack)")
    void testNonceReused() throws Exception {
        // Given
        String nonce = UUID.randomUUID().toString();

        // First Nonce usage
        redisTemplate.opsForValue().set(
            "nonce:" + nonce,
            String.valueOf(Instant.now().getEpochSecond()),
            Duration.ofMinutes(5)
        );

        // When: Second use of same Nonce
        mockMvc.perform(post("/api/token/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                    "{\"token\":\"gp_token_abc123\",\"actorType\":\"GAME_PROVIDER\",\"nonce\":\"%s\"}",
                    nonce)))
            // Then
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.data.errorCode").value("NONCE_REUSED"));
    }
}
```

---

## 相關文檔

- [Token Validation Service](./Token_Validation_Service.md) - 服務總覽
- [Token Validation Architecture](./Token_Validation_Architecture.md) - 驗證架構
- [Token Cache Performance](./Token_Cache_Performance.md) - 緩存策略
- [Caching Strategy](./Caching_Strategy.md) - JetCache 緩存策略
- [Performance Monitoring](./Performance_Monitoring.md) - 性能監控
