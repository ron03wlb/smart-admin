# 12-03 網關架構 (Gateway Architecture)

## 1. 系統概述
API Gateway 是所有外部流量進入平台的唯一入口。
本模組基於 **Apache APISIX** 或 **Kong** 構建，負責路由分發、流量控制與基礎安全。

## 2. 路由策略 (Routing Strategy)

### 2.1 統一接入 (Unified Ingress)
所有流量通過 HTTPS 443 端口接入，根據 `Host` 與 `Path` 分發。
*   `api.casino-brand.com` -> 轉發至 Backend API Cluster。
*   `www.casino-brand.com` -> 轉發至 Frontend CDN / SSR Server。
*   `admin.casino-brand.com` -> 轉發至 Backoffice Cluster (需 IP 白名單)。

### 2.2 行動端專屬路由 (Mobile App Routing)
App 流量需特殊處理，以支援 "熱更新" 與 "簽名驗證"。
*   **Path**: `/api/mobile/v1/*`
*   **Plugin**:
    *   **Device Context**: 自動提取 Header 中的 `X-Device-ID`, `X-Model`, `X-OS` 並注入 Upstream Header，供後端 Risk Engine 使用。
    *   **Signature Auth**: 驗證 App 本地私鑰簽名 (防止 API 被腳本直接調用)。

**App 簽名規範 (Signature Spec)**:
所有來自 App 的請求必須包含以下 Header：
- `X-App-Version`: `1.0.0`
- `X-Timestamp`: `1716382910` (誤差允許 +/- 5分鐘)
- `X-Nonce`: `UUID` (防重放)
- `X-Signature`: `HMAC-SHA256(Path + Body + Timestamp + Nonce, AppSecret)`

**Gateway 驗證邏輯**:
1. 檢查 `Timestamp` 是否在有效期內。
2. 檢查 `Nonce` 是否曾在 Redis 中出現過 (TTL 5min)。
3. 使用後端保存的 `AppSecret` 重新計算簽名並比對。
4. 失敗則返回 `401 Unauthorized`。

---

## 3. 流量控制 (Traffic Control)

### 3.1 限流 (Rate Limiting)
*   **全域頻控**: 單 IP 每秒最多 50 次請求。
*   **敏感接口頻控**:
    *   `/api/auth/login`: 單 IP 每分鐘 5 次。
    *   `/api/wallet/withdraw`: 單 User 每小時 3 次。
*   **處置**: 超過閾值直接返回 HTTP 429 Too Many Requests。

### 3.2 熔斷 (Circuit Breaking)
當某個 Upstream Service (如 Game Provider A) 響應時間 > 2秒 或 錯誤率 > 10%：
*   Gateway 自動 "跳閘" (Open Circuit)。
*   接下來 30秒 內的請求直接返回 "維護中"，不再轉發給後端，防止雪崩效應。

---

## 3.3 限流演算法詳解 (Rate Limiting Algorithms)

### 3.3.1 演算法選擇矩陣

| 演算法 | 適用場景 | 優點 | 缺點 | 推薦用途 |
|---|---|---|---|---|
| **Token Bucket** | API Gateway 全域限流 | 允許突發流量 (burst)，平滑流量控制 | 實作複雜，需精確時間同步 | `/api/*` 所有公開 API |
| **Leaky Bucket** | 穩定速率限制 | 絕對恆定速率，無突發流量 | 無法應對合法突發需求 | `/api/auth/login` 防暴力破解 |
| **Fixed Window** | 簡單統計 | 實作簡單，低開銷 | 邊界問題 (window edge burst) | 非關鍵 API 簡單限流 |
| **Sliding Window Log** | 精確限流 | 完全精確，無邊界問題 | 記憶體開銷大 | 高價值 API (如支付) |
| **Sliding Window Counter** | 精確 + 效能平衡 | 較精確且高效 | 實作中等複雜度 | 推薦作為預設策略 |

### 3.3.2 Token Bucket 實作架構

```
[Token Bucket Algorithm]
┌──────────────────────────────────────────────────────────────┐
│  Bucket Configuration                                        │
│  - Capacity: 100 tokens                                      │
│  - Refill Rate: 10 tokens/second                            │
│  - Initial: 100 tokens                                       │
├──────────────────────────────────────────────────────────────┤
│  Request Flow                                                │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  1. Request arrives                                    │  │
│  │  2. Check bucket: tokens >= 1?                        │  │
│  │     - Yes: Consume 1 token, allow request            │  │
│  │     - No: Reject with 429 Too Many Requests           │  │
│  │  3. Background: Refill tokens at constant rate        │  │
│  └────────────────────────────────────────────────────────┘  │
├──────────────────────────────────────────────────────────────┤
│  Burst Handling                                              │
│  - Allows 100 requests instantly (burst)                    │
│  - Then throttles to 10 req/s sustained rate                │
│  - Bucket refills: empty → full in 10 seconds               │
└──────────────────────────────────────────────────────────────┘
```

**Redis 實作 (Lua Script)**:

```lua
-- KEYS[1]: Rate limit key (e.g., "rate_limit:user:12345")
-- ARGV[1]: Max tokens (capacity)
-- ARGV[2]: Refill rate (tokens per second)
-- ARGV[3]: Current timestamp

local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

-- Get current state
local state = redis.call('HMGET', key, 'tokens', 'last_refill')
local tokens = tonumber(state[1]) or capacity
local last_refill = tonumber(state[2]) or now

-- Calculate tokens to add since last refill
local time_passed = math.max(0, now - last_refill)
local tokens_to_add = time_passed * refill_rate
tokens = math.min(capacity, tokens + tokens_to_add)

-- Try to consume 1 token
if tokens >= 1 then
  tokens = tokens - 1
  redis.call('HMSET', key, 'tokens', tokens, 'last_refill', now)
  redis.call('EXPIRE', key, 3600)  -- TTL 1 hour
  return {1, tokens}  -- Allow, remaining tokens
else
  return {0, 0}  -- Deny
end
```

**API Gateway 整合 (Kong Plugin)**:

```yaml
plugins:
  - name: rate-limiting
    config:
      minute: 100  # Max 100 requests per minute
      policy: redis
      redis_host: redis-cluster.internal
      redis_port: 6379
      redis_database: 2
      fault_tolerant: true  # If Redis down, allow traffic (fail-open)
      hide_client_headers: false  # Expose X-RateLimit-* headers
```

### 3.3.3 分層限流策略 (Tiered Rate Limiting)

```
[Multi-Layer Rate Limiting]
┌──────────────────────────────────────────────────────────────┐
│  Layer 1: DDoS Protection (Cloudflare)                      │
│  - Scope: Per IP                                             │
│  - Limit: 1000 req/s                                         │
│  - Action: Block at edge (before hitting origin)             │
├──────────────────────────────────────────────────────────────┤
│  Layer 2: API Gateway Global (Kong/APISIX)                  │
│  - Scope: Per IP                                             │
│  - Limit: 100 req/s                                          │
│  - Action: Return 429 with Retry-After header               │
├──────────────────────────────────────────────────────────────┤
│  Layer 3: API Gateway Endpoint-Specific                     │
│  - /api/auth/login: 10 req/min per IP                       │
│  - /api/wallet/withdraw: 5 req/hour per user                │
│  - /api/players/search: 20 req/min per user                 │
├──────────────────────────────────────────────────────────────┤
│  Layer 4: Application-Level (Business Logic)                │
│  - Withdraw frequency: Max 3 times per day                  │
│  - Bonus claim: Once per promotion per player               │
│  - Action: Business rule validation, log to audit           │
└──────────────────────────────────────────────────────────────┘
```

### 3.3.4 動態限流 (Adaptive Rate Limiting)

根據系統負載動態調整限流閾值：

```python
def calculate_dynamic_rate_limit():
    """
    Adjust rate limit based on system health
    """
    cpu_usage = get_cpu_usage_percent()
    memory_usage = get_memory_usage_percent()
    error_rate = get_error_rate_percent()

    base_limit = 100  # requests per second

    # Reduce limit if system under stress
    if cpu_usage > 80 or memory_usage > 85 or error_rate > 5:
        multiplier = 0.5  # Reduce to 50%
    elif cpu_usage > 60 or memory_usage > 70:
        multiplier = 0.7  # Reduce to 70%
    else:
        multiplier = 1.0  # Normal operation

    adjusted_limit = int(base_limit * multiplier)
    redis.setex('dynamic_rate_limit', 60, adjusted_limit)  # TTL 60s
    return adjusted_limit
```

## 3.4 熔斷器詳細配置 (Circuit Breaker Configuration)

### 3.4.1 熔斷器狀態機

```
[Circuit Breaker State Machine]
┌──────────────────────────────────────────────────────────────┐
│  CLOSED (正常狀態)                                            │
│  - All requests forwarded to upstream                        │
│  - Monitor: error_rate, timeout_rate, latency                │
│  ↓ If error_rate > 50% (last 10 requests)                    │
├──────────────────────────────────────────────────────────────┤
│  OPEN (跳閘狀態)                                              │
│  - All requests immediately fail (no upstream call)          │
│  - Return: 503 Service Unavailable                           │
│  - Duration: 30 seconds                                      │
│  ↓ After 30 seconds                                          │
├──────────────────────────────────────────────────────────────┤
│  HALF_OPEN (半開狀態)                                         │
│  - Allow 1 probe request to upstream                         │
│  ↓ If probe succeeds                                         │
│    → CLOSED (Reset, normal operation)                        │
│  ↓ If probe fails                                            │
│    → OPEN (Back to 30s wait)                                 │
└──────────────────────────────────────────────────────────────┘
```

### 3.4.2 熔斷器配置參數

**Kong Circuit Breaker Plugin**:

```yaml
plugins:
  - name: proxy-cache-advanced
  - name: circuit-breaker
    config:
      threshold: 10                # Min requests before evaluation
      window_size: 10              # Rolling window (last 10 requests)
      failure_threshold_percentage: 50  # Open if >50% fail
      timeout: 5000                # Timeout threshold (5 seconds)
      half_open_timeout: 30000     # Wait 30s before half-open
      break_on:
        - 500
        - 502
        - 503
        - 504
        - timeout
      fallback:
        status_code: 503
        content_type: application/json
        body: '{"error": "Service temporarily unavailable", "retry_after": 30}'
```

### 3.4.3 熔斷器監控儀表板

```
[Circuit Breaker Dashboard]
┌──────────────────────────────────────────────────────────────┐
│  Service: payment-gateway                                    │
│  State: OPEN (Tripped)                                       │
│  Last State Change: 2026-01-27 10:15:30 (30s ago)           │
│  Next Probe Attempt: 2026-01-27 10:16:00 (in 0s)            │
├──────────────────────────────────────────────────────────────┤
│  Metrics (Last 10 requests):                                 │
│  - Success: 3 (30%)                                          │
│  - Failure: 7 (70%) ← Triggered circuit breaker             │
│  - Timeout: 2                                                │
│  - Avg Latency: 8500ms                                       │
├──────────────────────────────────────────────────────────────┤
│  Actions:                                                    │
│  [Force Close] [Extend Open Duration] [View Error Logs]     │
└──────────────────────────────────────────────────────────────┘
```

## 3.5 DDoS 緩解策略 (DDoS Mitigation)

### 3.5.1 多層 DDoS 防禦架構

```
[DDoS Defense Layers]
┌──────────────────────────────────────────────────────────────┐
│  Layer 1: Network Layer (L3/L4)                             │
│  Provider: Cloudflare / Akamai / AWS Shield                 │
│  - SYN Flood Protection                                      │
│  - UDP Amplification Protection                              │
│  - Rate Limit: 100K pps per IP                              │
│  - Anycast routing to absorb traffic                         │
├──────────────────────────────────────────────────────────────┤
│  Layer 2: Application Layer (L7)                            │
│  Provider: Cloudflare WAF / API Gateway                     │
│  - HTTP Flood Protection                                     │
│  - Slowloris Protection (slow HTTP DoS)                     │
│  - JavaScript Challenge for suspicious IPs                  │
│  - CAPTCHA for high-risk requests                           │
├──────────────────────────────────────────────────────────────┤
│  Layer 3: Behavioral Analysis (ML-based)                    │
│  Provider: Imperva / Cloudflare Bot Management              │
│  - Detect bot patterns (timing, headers, behavior)          │
│  - Distinguish good bots (Google) from bad bots             │
│  - Block scrapers, credential stuffing                       │
├──────────────────────────────────────────────────────────────┤
│  Layer 4: Origin Protection (Last Line of Defense)          │
│  - Hide origin IP (only Cloudflare IPs allowed)             │
│  - Health-based auto-scaling (scale out during attack)      │
│  - Backup origin in different region                         │
└──────────────────────────────────────────────────────────────┘
```

### 3.5.2 Cloudflare 整合配置

```terraform
# Terraform: Cloudflare Zone Configuration
resource "cloudflare_zone_settings_override" "igaming_zone" {
  zone_id = var.cloudflare_zone_id

  settings {
    # DDoS Protection
    security_level = "high"  # Challenge suspicious visitors
    challenge_ttl  = 1800    # Challenge valid for 30 min

    # Rate Limiting
    rate_limiting {
      enabled = true
    }

    # Bot Management
    bot_management {
      enabled = true
      fight_mode = true
    }

    # WAF
    waf {
      enabled = true
      mode    = "on"
    }

    # Cache Everything (reduce origin load)
    cache_level = "aggressive"

    # HTTP/3 (QUIC) for performance
    http3 = "on"

    # TLS Settings
    min_tls_version       = "1.2"
    automatic_https_rewrites = "on"
  }
}

# Rate Limiting Rule
resource "cloudflare_rate_limit" "api_global" {
  zone_id   = var.cloudflare_zone_id
  threshold = 100
  period    = 1  # seconds
  match {
    request {
      url_pattern = "api.casino.com/*"
    }
  }
  action {
    mode    = "challenge"  # or "block"
    timeout = 60
  }
}
```

## 3.6 API 版本管理策略 (API Versioning Strategy)

### 3.6.1 版本策略選擇

| 策略 | 範例 | 優點 | 缺點 | 推薦 |
|---|---|---|---|---|
| **URL Path** | `/v1/players`, `/v2/players` | 清晰，易於路由 | URL 冗長 | ✅ 推薦 |
| **Header** | `Accept: application/vnd.api+json; version=2` | URL 乾淨 | 難以測試，不直觀 | ❌ |
| **Query Param** | `/players?version=2` | 簡單 | 易被忽略，難以強制 | ❌ |
| **Content Negotiation** | `Accept: application/vnd.casino.v2+json` | RESTful | 複雜 | ❌ |

### 3.6.2 版本生命週期管理

```
[API Version Lifecycle]
┌──────────────────────────────────────────────────────────────┐
│  v1.0 (Current Stable)                                       │
│  - Released: 2025-01-01                                      │
│  - Status: Supported                                         │
│  - EOL: 2027-01-01 (2 years support)                         │
├──────────────────────────────────────────────────────────────┤
│  v2.0 (New Release)                                          │
│  - Released: 2026-01-01                                      │
│  - Status: Supported                                         │
│  - Deprecate v1.0 starting: 2026-07-01 (6 months notice)    │
├──────────────────────────────────────────────────────────────┤
│  v1.0 (Deprecated)                                           │
│  - Deprecation Notice: 2026-07-01                            │
│  - Warning header: "Warning: 299 - API v1 deprecated"       │
│  - Documentation marked as deprecated                        │
│  - No new features, security fixes only                      │
│  - EOL: 2027-01-01 (6 months after deprecation)             │
└──────────────────────────────────────────────────────────────┘
```

### 3.6.3 版本路由配置

**Kong/APISIX Configuration**:

```yaml
services:
  - name: player-service-v1
    url: http://player-service-v1.internal:8080
    routes:
      - name: player-v1-route
        paths:
          - /v1/players
        strip_path: true
    plugins:
      - name: response-transformer
        config:
          add:
            headers:
              - "X-API-Version: 1.0"
              - "X-API-Deprecated: true"
              - "X-API-EOL: 2027-01-01"

  - name: player-service-v2
    url: http://player-service-v2.internal:8080
    routes:
      - name: player-v2-route
        paths:
          - /v2/players
        strip_path: true
    plugins:
      - name: response-transformer
        config:
          add:
            headers:
              - "X-API-Version: 2.0"
```

### 3.6.4 廢棄通知機制

```javascript
// v1 API Response (Deprecated)
HTTP/1.1 200 OK
X-API-Version: 1.0
X-API-Deprecated: true
X-API-Deprecation-Date: 2026-07-01
X-API-EOL-Date: 2027-01-01
X-API-Migration-Guide: https://docs.casino.com/api/migration-v1-to-v2
Warning: 299 - "API v1 is deprecated and will be removed on 2027-01-01. Please migrate to v2."

{
  "data": {...},
  "meta": {
    "deprecation_warning": {
      "message": "This API version is deprecated",
      "sunset_date": "2027-01-01",
      "replacement": "/v2/players",
      "migration_guide": "https://docs.casino.com/api/migration-v1-to-v2"
    }
  }
}
```

## 3.7 監控與告警 (Gateway Monitoring)

### 3.7.1 關鍵指標

| 指標 | 正常範圍 | Warning Threshold | Critical Threshold |
|---|---|---|---|
| Gateway Latency P99 | < 50ms | > 100ms | > 200ms |
| Upstream Latency P99 | < 200ms | > 500ms | > 1000ms |
| Error Rate (5xx) | < 0.1% | > 1% | > 5% |
| Rate Limit Hit Rate | < 1% | > 5% | > 10% |
| Circuit Breaker Trips | 0/hour | > 3/hour | > 10/hour |
| DDoS Attack Volume | 0 | Moderate | Severe |

### 3.7.2 監控儀表板

```
[API Gateway Dashboard (Grafana)]
┌──────────────────────────────────────────────────────────────┐
│  Traffic Overview (Last 1 Hour)                              │
│  - Total Requests: 5.2M (1,444 req/s avg)                    │
│  - Success Rate: 99.92% (4,158 errors)                       │
│  - Blocked by Rate Limit: 12,345 (0.24%)                     │
│  - Blocked by WAF: 8,901 (0.17%)                             │
├──────────────────────────────────────────────────────────────┤
│  Top Endpoints (by QPS)                                      │
│  1. GET /api/v1/games/lobby - 456 req/s                     │
│  2. POST /api/v1/wallet/balance - 234 req/s                 │
│  3. POST /api/v1/auth/refresh - 189 req/s                   │
├──────────────────────────────────────────────────────────────┤
│  Top Error Sources                                           │
│  1. 503 from game-provider-pragmatic (timeout) - 2,103      │
│  2. 429 rate limit /api/auth/login - 5,678                  │
│  3. 500 internal error /api/wallet/transfer - 234           │
├──────────────────────────────────────────────────────────────┤
│  Circuit Breaker Status                                      │
│  - game-provider-pragmatic: OPEN (Tripped 5 min ago)        │
│  - payment-gateway-nuvei: CLOSED (Healthy)                   │
│  - risk-engine: CLOSED (Healthy)                             │
└──────────────────────────────────────────────────────────────┘
```

---

**文件版本**: V2.0 (Enhanced)
**最後更新**: 2026-01-27
**狀態**: Architecture-Level Complete

## 4. 安全防護 (WAF Integration)

### 4.1 ModSecurity / Coraza
*   **SQL Injection**: 攔截包含 `UNION SELECT`, `DROP TABLE` 的請求。
*   **XSS**: 攔截包含 `<script>` 的 Payload。
*   **Bot Protection**: 攔截 User-Agent 為 `curl`, `python-requests` 的非瀏覽器流量 (除非是授權的 Server-to-Server API)。
