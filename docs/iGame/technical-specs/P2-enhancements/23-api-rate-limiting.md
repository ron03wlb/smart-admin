# P2-23: API Rate Limiting & DDoS Protection

**Version**: 1.0
**Last Updated**: 2026-01-23
**Status**: Draft
**Priority**: P2 (Enhancement)

---

## Table of Contents

1. [Background & Strategic Context](#1-background--strategic-context)
2. [Requirements](#2-requirements)
3. [Architecture Design](#3-architecture-design)
4. [Rate Limiting Algorithms](#4-rate-limiting-algorithms)
5. [Multi-Tenant Rate Limiting](#5-multi-tenant-rate-limiting)
6. [Distributed Rate Limiter (Redis)](#6-distributed-rate-limiter-redis)
7. [Rate Limit Configuration](#7-rate-limit-configuration)
8. [SmartAdmin Implementation](#8-smartadmin-implementation)
9. [DDoS Protection](#9-ddos-protection)
10. [Testing Strategy](#10-testing-strategy)
11. [Operations & Monitoring](#11-operations--monitoring)
12. [Appendices](#12-appendices)

---

## 1. Background & Strategic Context

### 1.1 Business Context

**From [P1-15: Security Hardening](../P1-important/15-security-hardening.md)**:
> "Rate limiting prevents brute-force attacks, credential stuffing, and API abuse. Distributed rate limiting across multiple pods ensures consistent enforcement."

**From [backend_project.md](../../backend_project.md) Section 3.4**:
> "Multi-tenant architecture requires resource isolation to prevent noisy neighbors. Rate limiting ensures fair resource allocation across tenants."

### 1.2 Problem Statement

**Current Limitation**:
- No API rate limiting (vulnerable to brute-force, DDoS, scraping)
- No per-tenant quotas (one merchant can monopolize resources)
- No protection against credential stuffing attacks
- API abuse can degrade performance for all users

**Business Impact**:
- **Security Risk**: 3,200 login attempts/minute during credential stuffing attack (actual incident)
- **Service Degradation**: Scraper bots caused 40% increase in database load
- **Revenue Loss**: $15K hosting cost spike during DDoS attack
- **Reputation Damage**: Legitimate players experienced slow response times

### 1.3 Success Criteria

| Metric | Target | Measurement |
|--------|--------|-------------|
| **Rate Limit Accuracy** | ±1% of configured limit | Distributed enforcement across 10 pods |
| **Enforcement Latency** | <5ms p95 | Redis lookup + increment |
| **False Positive Rate** | <0.1% | Legitimate requests blocked |
| **DDoS Mitigation** | Block 99.9% of attack traffic | Challenge-response for suspicious IPs |
| **Multi-Tenant Isolation** | 100% quota enforcement | Tenant A cannot exceed Tenant B's quota |

---

## 2. Requirements

### 2.1 Functional Requirements

**FR-RATE-001: Per-Endpoint Rate Limiting**
- Configure rate limits per API endpoint (e.g., `/api/login`: 5 req/min, `/api/wallet/balance`: 100 req/min)
- Support different limits for different HTTP methods (POST stricter than GET)

**FR-RATE-002: Multi-Tenant Rate Limiting**
- Per-tenant quotas (e.g., Tenant A: 10K req/hour, Tenant B: 50K req/hour)
- Premium tier overrides (VIP merchants get higher limits)

**FR-RATE-003: Per-User Rate Limiting**
- Per-user limits for authenticated endpoints (e.g., 1000 API calls/hour per player)
- Anonymous IP-based limits for public endpoints (e.g., 100 req/hour per IP)

**FR-RATE-004: Rate Limit Response Headers**
- `X-RateLimit-Limit`: Maximum requests allowed
- `X-RateLimit-Remaining`: Requests remaining in current window
- `X-RateLimit-Reset`: Unix timestamp when limit resets
- HTTP 429 (Too Many Requests) when limit exceeded

**FR-RATE-005: DDoS Protection**
- Automatic IP blocking after excessive failed requests
- Challenge-response (CAPTCHA) for suspicious traffic
- Whitelist for trusted IPs (office, payment providers)

### 2.2 Non-Functional Requirements

**NFR-RATE-001: Performance**
- Rate limit check: <5ms p95 latency
- Distributed coordination: <10ms cross-pod synchronization

**NFR-RATE-002: Scalability**
- Support 100K+ active rate limit keys in Redis
- Handle 10K requests/second with rate limiting enabled

**NFR-RATE-003: Availability**
- Fail-open: If Redis unavailable, allow requests (degrade gracefully)
- Local fallback cache (Caffeine) when Redis is down

---

## 3. Architecture Design

### 3.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      Client Request                              │
│  GET /api/wallet/balance                                        │
│  Headers: Authorization: Bearer xxx                             │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│               RateLimitInterceptor (Spring MVC)                  │
│  1. Identify rate limit key:                                    │
│     - Endpoint: /api/wallet/balance                             │
│     - Tenant: merchant_001                                       │
│     - User: player_12345                                         │
│     - IP: 203.0.113.42                                           │
│  2. Check rate limit via RateLimiterService                     │
│  3. If exceeded → HTTP 429, else proceed                        │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│              RateLimiterService (Redis-backed)                   │
│                                                                  │
│  Redis Key: rate_limit:endpoint:/api/wallet/balance:tenant:001  │
│  Value: Sliding window counter (requests in last 60s)           │
│                                                                  │
│  Lua Script (atomic):                                            │
│    local current = redis.call('INCR', key)                      │
│    if current == 1 then                                          │
│        redis.call('EXPIRE', key, window_seconds)                │
│    end                                                           │
│    return current                                                │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Rate Limit Decision                            │
│  if (current <= limit) → Allow request                          │
│  else → HTTP 429 with Retry-After header                        │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 Key Design Decisions

**Decision 1: Redisson RateLimiter (Token Bucket Algorithm)**
- **Rationale**: Proven library, supports distributed rate limiting across pods
- **Alternative Rejected**: Rolling window (more complex, higher Redis memory usage)

**Decision 2: Multi-Level Rate Limiting**
- **Pattern**: Check global → tenant → user → IP limits (fail fast on first violation)
- **Rationale**: Granular control, prevents both tenant abuse and individual user abuse

**Decision 3: Fail-Open Strategy**
- **Pattern**: If Redis unavailable, allow requests (log warning)
- **Rationale**: Availability > strict rate limiting (business continuity)
- **Mitigation**: Local Caffeine cache for 30-second window

**Decision 4: Lua Script for Atomicity**
- **Pattern**: INCR + EXPIRE in single Lua script (atomic operation)
- **Rationale**: Prevents race condition where key increments but doesn't expire

---

## 4. Rate Limiting Algorithms

### 4.1 Token Bucket Algorithm (Redisson)

**How it works**:
1. Each endpoint has a "bucket" with N tokens
2. Each request consumes 1 token
3. Bucket refills at rate R tokens/second
4. If bucket empty → reject request (HTTP 429)

**Properties**:
- ✅ Allows bursts (bucket can accumulate tokens)
- ✅ Smooth average rate over time
- ❌ More complex than fixed window

**Redisson Implementation**:

```java
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final RedissonClient redissonClient;

    public boolean tryAcquire(String key, long limit, long windowSeconds) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);

        // Configure: limit permits per windowSeconds
        rateLimiter.trySetRate(RateType.OVERALL, limit, windowSeconds, RateIntervalUnit.SECONDS);

        // Try to acquire 1 permit
        return rateLimiter.tryAcquire(1);
    }
}
```

### 4.2 Sliding Window Counter (Alternative)

**How it works**:
1. Track request count in current time window (e.g., last 60 seconds)
2. Use Redis INCR with TTL
3. If count > limit → reject request

**Redis Lua Script**:

```lua
local key = KEYS[1]
local limit = tonumber(ARGV[1])
local window = tonumber(ARGV[2])

local current = redis.call('INCR', key)

if current == 1 then
    redis.call('EXPIRE', key, window)
end

if current > limit then
    return 0  -- Rejected
else
    return 1  -- Allowed
end
```

**Java Wrapper**:

```java
public boolean tryAcquireSlidingWindow(String key, long limit, long windowSeconds) {
    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    script.setScriptText(SLIDING_WINDOW_SCRIPT);
    script.setResultType(Long.class);

    Long result = redisTemplate.execute(
        script,
        Collections.singletonList(key),
        limit,
        windowSeconds
    );

    return result != null && result == 1;
}
```

---

## 5. Multi-Tenant Rate Limiting

### 5.1 Tenant Quota Configuration

**Database Schema**:

```sql
CREATE TABLE t_tenant_quota (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    quota_type VARCHAR(50) NOT NULL,  -- REQUESTS_PER_HOUR, REQUESTS_PER_MINUTE
    quota_limit BIGINT NOT NULL,
    tier VARCHAR(20) NOT NULL,  -- FREE, BASIC, PREMIUM, ENTERPRISE
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(tenant_id, quota_type)
);

-- Example data
INSERT INTO t_tenant_quota (tenant_id, quota_type, quota_limit, tier) VALUES
('merchant_001', 'REQUESTS_PER_HOUR', 10000, 'BASIC'),
('merchant_002', 'REQUESTS_PER_HOUR', 100000, 'PREMIUM'),
('merchant_003', 'REQUESTS_PER_HOUR', 1000000, 'ENTERPRISE');
```

### 5.2 Tenant Quota Enforcement

```java
@Service
@RequiredArgsConstructor
public class TenantRateLimiterService {

    private final RateLimiterService rateLimiterService;
    private final TenantQuotaDao tenantQuotaDao;
    private final RedisService redisService;

    public boolean checkTenantQuota(String tenantId) {
        // Fetch tenant quota from cache
        String cacheKey = "tenant_quota:" + tenantId;
        TenantQuota quota = redisService.get(cacheKey);

        if (quota == null) {
            quota = tenantQuotaDao.selectByTenantId(tenantId);
            redisService.set(cacheKey, quota, Duration.ofMinutes(10));
        }

        if (quota == null) {
            // Default quota for unknown tenants
            quota = new TenantQuota(10000L, QuotaType.REQUESTS_PER_HOUR);
        }

        // Check rate limit
        String rateLimitKey = String.format("rate_limit:tenant:%s", tenantId);
        return rateLimiterService.tryAcquire(
            rateLimitKey,
            quota.getQuotaLimit(),
            3600  // 1 hour in seconds
        );
    }
}
```

---

## 6. Distributed Rate Limiter (Redis)

### 6.1 Rate Limit Key Design

**Key Pattern**:
```
rate_limit:{scope}:{identifier}:{window}
```

**Examples**:
- `rate_limit:endpoint:/api/login:tenant:merchant_001:60s`
- `rate_limit:user:player_12345:3600s`
- `rate_limit:ip:203.0.113.42:60s`
- `rate_limit:global:all:3600s`

### 6.2 Rate Limit Configuration

```yaml
# application.yml
rate-limit:
  enabled: true
  fail-open: true  # Allow requests if Redis unavailable

  # Global limits (across all tenants)
  global:
    requests-per-minute: 100000
    requests-per-hour: 1000000

  # Per-endpoint limits
  endpoints:
    - path: /api/login
      method: POST
      limit: 5
      window-seconds: 60
      scope: IP  # Rate limit by IP address

    - path: /api/wallet/deposit
      method: POST
      limit: 10
      window-seconds: 60
      scope: USER  # Rate limit by authenticated user

    - path: /api/wallet/balance
      method: GET
      limit: 100
      window-seconds: 60
      scope: USER

    - path: /api/game/launch
      method: GET
      limit: 50
      window-seconds: 60
      scope: USER

  # IP-based limits (for anonymous endpoints)
  ip:
    requests-per-minute: 100
    requests-per-hour: 1000

  # User-based limits (for authenticated endpoints)
  user:
    requests-per-minute: 500
    requests-per-hour: 10000
```

---

## 7. Rate Limit Configuration

### 7.1 Configuration Entity

```java
@Data
public class RateLimitConfig {
    private String path;
    private String method;  // GET, POST, PUT, DELETE
    private long limit;
    private long windowSeconds;
    private RateLimitScope scope;  // GLOBAL, TENANT, USER, IP
}

public enum RateLimitScope {
    GLOBAL,   // Across all tenants and users
    TENANT,   // Per tenant
    USER,     // Per authenticated user
    IP        // Per IP address
}
```

### 7.2 Configuration Manager

```java
@Service
@RequiredArgsConstructor
public class RateLimitConfigManager {

    private final RateLimitProperties properties;
    private final Map<String, RateLimitConfig> configCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // Load endpoint configurations
        for (RateLimitConfig config : properties.getEndpoints()) {
            String key = config.getPath() + ":" + config.getMethod();
            configCache.put(key, config);
        }
    }

    public RateLimitConfig getConfig(String path, String method) {
        String key = path + ":" + method;
        return configCache.get(key);
    }
}
```

---

## 8. SmartAdmin Implementation

### 8.1 RateLimitInterceptor

**File**: `sa-base/infrastructure/web/src/main/java/net/lab1024/sa/base/interceptor/RateLimitInterceptor.java`

```java
package net.lab1024.sa.base.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.base.service.RateLimiterService;
import net.lab1024.sa.base.service.TenantContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Rate Limit Interceptor
 *
 * Enforces API rate limits at multiple levels:
 * 1. Global (across all tenants)
 * 2. Tenant (per merchant)
 * 3. User (per authenticated player)
 * 4. IP (per client IP address)
 *
 * @author lab1024
 * @since 2026-01-23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;
    private final RateLimitConfigManager configManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                            Object handler) throws Exception {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Get rate limit config for this endpoint
        RateLimitConfig config = configManager.getConfig(path, method);

        if (config == null) {
            return true;  // No rate limit configured
        }

        // Determine rate limit key based on scope
        String rateLimitKey = buildRateLimitKey(config, request);

        // Check rate limit
        boolean allowed = rateLimiterService.tryAcquire(
            rateLimitKey,
            config.getLimit(),
            config.getWindowSeconds()
        );

        if (!allowed) {
            // Rate limit exceeded
            response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);  // HTTP 429
            response.setHeader("X-RateLimit-Limit", String.valueOf(config.getLimit()));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("X-RateLimit-Reset", String.valueOf(
                System.currentTimeMillis() / 1000 + config.getWindowSeconds()
            ));
            response.setHeader("Retry-After", String.valueOf(config.getWindowSeconds()));

            response.getWriter().write("{\"code\":429,\"message\":\"Too many requests\"}");
            response.setContentType("application/json");

            log.warn("Rate limit exceeded: key={}, limit={}", rateLimitKey, config.getLimit());
            return false;
        }

        // Add rate limit headers to response
        long remaining = rateLimiterService.getRemaining(rateLimitKey, config.getLimit());
        response.setHeader("X-RateLimit-Limit", String.valueOf(config.getLimit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("X-RateLimit-Reset", String.valueOf(
            System.currentTimeMillis() / 1000 + config.getWindowSeconds()
        ));

        return true;
    }

    private String buildRateLimitKey(RateLimitConfig config, HttpServletRequest request) {
        return switch (config.getScope()) {
            case GLOBAL -> String.format("rate_limit:global:%s:%s",
                config.getPath(), config.getWindowSeconds());

            case TENANT -> String.format("rate_limit:tenant:%s:%s:%s",
                TenantContextHolder.getTenantId(), config.getPath(), config.getWindowSeconds());

            case USER -> {
                Long userId = getCurrentUserId(request);
                yield String.format("rate_limit:user:%d:%s:%s",
                    userId, config.getPath(), config.getWindowSeconds());
            }

            case IP -> {
                String ip = getClientIp(request);
                yield String.format("rate_limit:ip:%s:%s:%s",
                    ip, config.getPath(), config.getWindowSeconds());
            }
        };
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip.split(",")[0].trim();  // First IP in X-Forwarded-For chain
    }
}
```

### 8.2 Controller Layer (Annotation-Based)

**Optional**: Use `@RateLimit` annotation for method-level configuration

```java
@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletController {

    @PostMapping("/deposit")
    @RateLimit(limit = 10, windowSeconds = 60, scope = RateLimitScope.USER)
    public ResponseDTO<DepositResult> deposit(@RequestBody @Valid DepositForm form) {
        // Deposit logic
    }

    @GetMapping("/balance")
    @RateLimit(limit = 100, windowSeconds = 60, scope = RateLimitScope.USER)
    public ResponseDTO<WalletBalance> getBalance() {
        // Balance lookup
    }
}
```

---

## 9. DDoS Protection

### 9.1 IP Blocking Strategy

**Auto-block after excessive failed requests**:

```java
@Service
@RequiredArgsConstructor
public class DDoSProtectionService {

    private final RedisService redisService;

    public void recordFailedRequest(String ip) {
        String key = "ddos:failed:" + ip;
        Long failedCount = redisService.increment(key);

        if (failedCount == 1) {
            // Set expiry on first failure (1 hour window)
            redisService.expire(key, Duration.ofHours(1));
        }

        // Block IP if >100 failed requests in 1 hour
        if (failedCount > 100) {
            blockIp(ip, Duration.ofHours(24));
            log.warn("IP blocked due to excessive failed requests: {}", ip);
        }
    }

    public boolean isIpBlocked(String ip) {
        String key = "ddos:blocked:" + ip;
        return redisService.exists(key);
    }

    private void blockIp(String ip, Duration duration) {
        String key = "ddos:blocked:" + ip;
        redisService.set(key, "1", duration);
    }
}
```

### 9.2 CAPTCHA Challenge

**Trigger CAPTCHA for suspicious IPs**:

```java
@Component
@RequiredArgsConstructor
public class CaptchaInterceptor implements HandlerInterceptor {

    private final DDoSProtectionService ddosService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                            Object handler) {
        String ip = getClientIp(request);

        // Check if IP is blocked
        if (ddosService.isIpBlocked(ip)) {
            response.setStatus(403);  // Forbidden
            response.getWriter().write("{\"code\":403,\"message\":\"IP blocked\"}");
            return false;
        }

        // Check if CAPTCHA required
        if (ddosService.requiresCaptcha(ip)) {
            String captchaToken = request.getHeader("X-Captcha-Token");

            if (captchaToken == null || !verifyCaptcha(captchaToken)) {
                response.setStatus(403);
                response.getWriter().write("{\"code\":403,\"message\":\"CAPTCHA required\"}");
                return false;
            }
        }

        return true;
    }
}
```

---

## 10. Testing Strategy

### 10.1 Rate Limit Tests

```java
@SpringBootTest
class RateLimiterServiceTest {

    @Autowired
    private RateLimiterService rateLimiterService;

    @Test
    void testRateLimitEnforcement() {
        String key = "test:rate_limit:" + UUID.randomUUID();
        long limit = 10;
        long windowSeconds = 60;

        // First 10 requests should succeed
        for (int i = 0; i < 10; i++) {
            boolean allowed = rateLimiterService.tryAcquire(key, limit, windowSeconds);
            assertThat(allowed).isTrue();
        }

        // 11th request should be rejected
        boolean allowed = rateLimiterService.tryAcquire(key, limit, windowSeconds);
        assertThat(allowed).isFalse();
    }

    @Test
    void testDistributedRateLimit() throws Exception {
        String key = "test:distributed:" + UUID.randomUUID();
        long limit = 100;
        long windowSeconds = 60;

        // Simulate 10 concurrent threads (10 pods)
        ExecutorService executor = Executors.newFixedThreadPool(10);
        AtomicInteger successCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            futures.add(executor.submit(() -> {
                for (int j = 0; j < 20; j++) {
                    if (rateLimiterService.tryAcquire(key, limit, windowSeconds)) {
                        successCount.incrementAndGet();
                    }
                }
            }));
        }

        // Wait for all threads
        for (Future<?> future : futures) {
            future.get();
        }

        // Should allow exactly 100 requests (limit)
        assertThat(successCount.get()).isEqualTo(100);

        executor.shutdown();
    }
}
```

---

## 11. Operations & Monitoring

### 11.1 Metrics

```promql
# Rate limit rejections
rate(rate_limit_rejections_total[5m])

# Top rejected endpoints
topk(10, sum by (endpoint) (rate(rate_limit_rejections_total[1h])))

# Top rate-limited IPs
topk(10, sum by (ip) (rate(rate_limit_rejections_total[1h])))

# Rate limit check latency
histogram_quantile(0.95,
  sum(rate(rate_limit_check_duration_bucket[5m])) by (le)
)
```

### 11.2 Alerts

```yaml
- alert: HighRateLimitRejectionRate
  expr: rate(rate_limit_rejections_total[5m]) > 100
  annotations:
    summary: "High rate limit rejection rate: {{ $value }} req/s"

- alert: PossibleDDoSAttack
  expr: sum(rate(rate_limit_rejections_total{scope="IP"}[1m])) > 1000
  annotations:
    summary: "Possible DDoS attack detected: {{ $value }} blocked req/s"
```

---

## 12. Appendices

### 12.1 Rate Limit Best Practices

1. **Start Generous**: Set high limits initially, reduce based on actual usage
2. **Communicate Limits**: Document rate limits in API documentation
3. **Provide Headers**: Always include `X-RateLimit-*` headers
4. **Differentiate Tiers**: Premium customers get higher limits
5. **Monitor Violations**: Alert on excessive rate limit hits (possible attack)

### 12.2 Cross-References

- [P1-15: Security Hardening](../P1-important/15-security-hardening.md) - Security best practices
- [backend_project.md](../../backend_project.md) - Overall architecture

---

**Document End**
