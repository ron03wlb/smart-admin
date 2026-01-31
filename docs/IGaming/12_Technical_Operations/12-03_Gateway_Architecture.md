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

##### 📊 Diagram 1: API Gateway 統一接入架構 (API Gateway Unified Ingress Architecture)

```mermaid
graph TB
    subgraph "CDN & DDoS Protection Layer - CDN 與 DDoS 防護層"
        CF[Cloudflare / Akamai<br/>Global CDN + WAF<br/>DDoS Protection<br/>Rate Limit: 1000 req/s per IP]
    end

    subgraph "Client Layer - 客戶端層"
        WEB[Web Browser<br/>www.casino-brand.com]
        MOBILE[Mobile App<br/>api.casino-brand.com/mobile]
        ADMIN[Admin Panel<br/>admin.casino-brand.com]
    end

    WEB --> CF
    MOBILE --> CF
    ADMIN --> CF

    subgraph "API Gateway Layer - API 閘道層"
        KONG[Kong / Apache APISIX<br/>Port: 443 - HTTPS<br/>Rate Limit: 100 req/s per IP<br/>Circuit Breaker Enabled]
    end

    CF --> KONG

    subgraph "Routing & Plugin Layer - 路由與插件層"
        R1[Route: www.casino-brand.com<br/>Plugins: Cache, Compression]
        R2[Route: api.casino-brand.com<br/>Plugins: JWT Auth, Rate Limit]
        R3[Route: api.casino-brand.com/mobile<br/>Plugins: Signature Auth, Device Context]
        R4[Route: admin.casino-brand.com<br/>Plugins: JWT Auth, IP Whitelist]

        KONG --> R1
        KONG --> R2
        KONG --> R3
        KONG --> R4
    end

    subgraph "Upstream Services - 上游服務層"
        CDN_SSR[Frontend CDN / SSR Server<br/>Static Assets + SSR<br/>Port: 3000]
        API_V1[Backend API Cluster - v1<br/>Player/Wallet/Game APIs<br/>Port: 8080]
        API_V2[Backend API Cluster - v2<br/>New APIs<br/>Port: 8081]
        MOBILE_API[Mobile API Cluster<br/>Dedicated for App<br/>Port: 8082]
        BACKOFFICE[Backoffice Cluster<br/>Admin APIs<br/>Port: 9090]
    end

    R1 --> CDN_SSR
    R2 --> API_V1
    R2 --> API_V2
    R3 --> MOBILE_API
    R4 --> BACKOFFICE

    subgraph "Service Mesh Layer - 服務網格層"
        API_V1 --> SVC1[Player Service<br/>Port: 8001]
        API_V1 --> SVC2[Wallet Service<br/>Port: 8002]
        API_V1 --> SVC3[Game Service<br/>Port: 8003]

        BACKOFFICE --> ADMIN1[User Management<br/>Port: 9001]
        BACKOFFICE --> ADMIN2[Finance Management<br/>Port: 9002]
        BACKOFFICE --> ADMIN3[Risk Management<br/>Port: 9003]
    end

    subgraph "Shared Infrastructure - 共享基礎設施"
        REDIS[Redis Cluster<br/>Rate Limit State<br/>Session Cache]
        PG[PostgreSQL<br/>Gateway Logs<br/>Audit Trail]
        MONITOR[Prometheus + Grafana<br/>Metrics + Alerts]
    end

    KONG --> REDIS
    KONG --> PG
    KONG --> MONITOR

    style CF fill:#FF9800,stroke:#E65100,stroke-width:3px,color:#000
    style KONG fill:#FFC107,stroke:#F57F00,stroke-width:3px,color:#000

    style R1 fill:#E3F2FD,stroke:#1976D2,stroke-width:2px
    style R2 fill:#E3F2FD,stroke:#1976D2,stroke-width:2px
    style R3 fill:#E3F2FD,stroke:#1976D2,stroke-width:2px
    style R4 fill:#E3F2FD,stroke:#1976D2,stroke-width:2px

    style CDN_SSR fill:#C8E6C9,stroke:#388E3C,stroke-width:2px
    style API_V1 fill:#C8E6C9,stroke:#388E3C,stroke-width:2px
    style API_V2 fill:#C8E6C9,stroke:#388E3C,stroke-width:2px
    style MOBILE_API fill:#C8E6C9,stroke:#388E3C,stroke-width:2px
    style BACKOFFICE fill:#C8E6C9,stroke:#388E3C,stroke-width:2px

    style REDIS fill:#FFE082,stroke:#F57F00,stroke-width:2px
    style PG fill:#FFE082,stroke:#F57F00,stroke-width:2px
    style MONITOR fill:#FFE082,stroke:#F57F00,stroke-width:2px
```text

**架構說明**:

| 層級 | 組件 | 職責 | 技術棧 | 性能指標 |
|------|------|------|--------|----------|
| **CDN & DDoS 層** | Cloudflare / Akamai | 全球加速、DDoS 防護、WAF | Cloudflare Pro | P99 < 50ms (edge cache hit) |
| **API Gateway 層** | Kong / Apache APISIX | 路由分發、認證授權、限流熔斷 | Kong 3.x / APISIX 3.x | P99 < 100ms (gateway latency) |
| **Routing 層** | Route + Plugins | 根據 Host/Path 路由 + 插件鏈 | Lua plugins | Routing decision < 5ms |
| **Upstream 層** | Backend API Clusters | 業務邏輯處理 | Spring Boot 3.x | P99 < 500ms (API response) |
| **Service Mesh 層** | Microservices | 微服務架構 | Spring Cloud / K8s | Internal latency < 50ms |
| **Shared Infrastructure** | Redis, PostgreSQL, Monitoring | 共享基礎設施 | Redis 7.x, PostgreSQL 16.x | Redis P99 < 10ms |

**路由配置範例**:

**Route 1: Web Frontend (www.casino-brand.com)**
```yaml
routes:
  - name: frontend-route
    hosts:
      - www.casino-brand.com
    paths:
      - /
    strip_path: false
    plugins:
      - name: proxy-cache
        config:
          cache_ttl: 300  # 5 minutes
      - name: response-transformer
        config:
          add:
            headers:
              - "X-Cache-Status: HIT"
      - name: gzip
        config:
          min_length: 1000
```text

**Route 2: Public API (api.casino-brand.com)**
```yaml
routes:
  - name: api-route
    hosts:
      - api.casino-brand.com
    paths:
      - /v1/*
      - /v2/*
    strip_path: false
    plugins:
      - name: jwt
        config:
          secret_is_base64: false
      - name: rate-limiting
        config:
          minute: 100
          policy: redis
      - name: request-id
        config:
          header_name: X-Request-ID
```text

**Route 3: Mobile API (api.casino-brand.com/mobile)**
```yaml
routes:
  - name: mobile-route
    hosts:
      - api.casino-brand.com
    paths:
      - /mobile/v1/*
    strip_path: true
    plugins:
      - name: signature-auth  # Custom plugin
        config:
          app_secret: ${APP_SECRET}
          timestamp_tolerance: 300  # 5 minutes
          nonce_ttl: 300
      - name: device-context  # Custom plugin
        config:
          extract_headers:
            - X-Device-ID
            - X-Model
            - X-OS
      - name: rate-limiting
        config:
          minute: 50  # Stricter for mobile
```text

**Route 4: Admin Backoffice (admin.casino-brand.com)**
```yaml
routes:
  - name: admin-route
    hosts:
      - admin.casino-brand.com
    paths:
      - /api/*
    strip_path: false
    plugins:
      - name: ip-restriction
        config:
          allow:
            - 192.168.1.0/24  # Office IP
            - 10.0.0.0/16     # VPN IP
      - name: jwt
        config:
          claims_to_verify:
            - role: admin
      - name: request-termination  # Fail-secure if auth fails
        config:
          status_code: 403
          message: "Access Denied - Admin only"
```text

**流量分發邏輯**:

```
Request arrives at Cloudflare
  ↓
1. DDoS Protection + WAF Check
  ↓
2. DNS Resolution → Kong Gateway (origin)
  ↓
3. Kong matches Host + Path:
   - www.casino-brand.com → R1 → CDN/SSR
   - api.casino-brand.com/v1/* → R2 → API v1 Cluster
   - api.casino-brand.com/mobile/* → R3 → Mobile API Cluster
   - admin.casino-brand.com → R4 → Backoffice Cluster (IP check)
  ↓
4. Apply Plugin Chain:
   - Authentication (JWT / Signature)
   - Rate Limiting (Redis)
   - Circuit Breaker (if upstream unhealthy)
  ↓
5. Forward to Upstream Service
  ↓
6. Response Transformation + Cache
  ↓
7. Return to Client
```markdown

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
```text

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
```text

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
```text

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
```text

### 3.3.4 動態限流 (Adaptive Rate Limiting)

根據系統負載動態調整限流閾值：


## 3.4 熔斷器詳細配置 (Circuit Breaker Configuration)

### 3.4.1 熔斷器狀態機

##### 📊 Diagram 2: 熔斷器狀態機 (Circuit Breaker State Machine)

```mermaid
stateDiagram-v2
    [*] --> CLOSED: 系統啟動<br/>Initial State

    state CLOSED {
        [*] --> Monitoring
        Monitoring --> RequestForwarding: 正常轉發請求
        RequestForwarding --> ErrorTracking: 追蹤錯誤率
        ErrorTracking --> Monitoring
    }

    CLOSED --> OPEN: 觸發條件:<br/>- Error Rate > 50% (last 10 requests)<br/>- OR Timeout Rate > 50%<br/>- OR Avg Latency > 5000ms

    state OPEN {
        [*] --> FailFast
        FailFast --> ReturnError: 立即返回<br/>503 Service Unavailable<br/>(不調用上游)
        ReturnError --> Timer: 等待 30 秒
        Timer --> FailFast
    }

    OPEN --> HALF_OPEN: 30 秒後<br/>嘗試探測

    state HALF_OPEN {
        [*] --> ProbeRequest
        ProbeRequest --> WaitResponse: 發送 1 個探測請求<br/>到上游服務
    }

    HALF_OPEN --> CLOSED: ✅ 探測成功<br/>- Status Code: 2xx<br/>- Latency < 2000ms<br/>→ 重置計數器<br/>→ 恢復正常運營

    HALF_OPEN --> OPEN: ❌ 探測失敗<br/>- Status Code: 5xx<br/>- OR Timeout<br/>→ 重新跳閘<br/>→ 回到 30 秒等待

    CLOSED --> CLOSED: 錯誤率 < 50%<br/>持續監控

    note right of CLOSED
        正常狀態 (CLOSED):
        - 所有請求轉發至上游
        - 監控指標:
          * Error Rate (5xx)
          * Timeout Rate
          * Avg Latency
        - 滑動窗口: 最近 10 個請求
    end note

    note right of OPEN
        跳閘狀態 (OPEN):
        - 快速失敗 (Fail-Fast)
        - 不調用上游服務
        - 減輕上游壓力
        - 防止雪崩效應
        - 固定等待: 30 秒
    end note

    note right of HALF_OPEN
        半開狀態 (HALF_OPEN):
        - 發送探測請求
        - 驗證上游是否恢復
        - 成功 → CLOSED
        - 失敗 → OPEN (重新跳閘)
    end note
```text

**狀態轉換詳細說明**:

| 狀態 | 描述 | 請求處理 | 監控指標 | 轉換條件 |
|------|------|----------|----------|----------|
| **CLOSED (關閉)** | 正常運營狀態 | 所有請求轉發至上游 | Error Rate, Timeout Rate, Latency | Error Rate > 50% (last 10 requests) |
| **OPEN (開啟)** | 跳閘保護狀態 | 立即返回 503，不調用上游 | 等待計時器（30 秒） | 30 秒後自動進入 HALF_OPEN |
| **HALF_OPEN (半開)** | 探測恢復狀態 | 允許 1 個探測請求通過 | 探測請求成功率 | 成功 → CLOSED, 失敗 → OPEN |

**觸發條件計算範例**:

**場景 1: 錯誤率觸發 (Error Rate Trigger)**
```
最近 10 個請求結果:
[200, 200, 500, 500, 500, 200, 500, 500, 500, 500]

計算:
- 成功: 3 個 (200)
- 失敗: 7 個 (500)
- Error Rate = 7/10 = 70% > 50%

結果: 🔴 觸發熔斷 (CLOSED → OPEN)
```text

**場景 2: 超時率觸發 (Timeout Rate Trigger)**
```
最近 10 個請求結果:
[200, 200, TIMEOUT, 200, TIMEOUT, TIMEOUT, 200, TIMEOUT, TIMEOUT, TIMEOUT]

計算:
- 成功: 4 個 (200)
- 超時: 6 個 (TIMEOUT)
- Timeout Rate = 6/10 = 60% > 50%

結果: 🔴 觸發熔斷 (CLOSED → OPEN)
```text

**場景 3: 平均延遲觸發 (Average Latency Trigger)**
```
最近 10 個請求延遲 (ms):
[100, 200, 5500, 6000, 4500, 300, 5200, 5800, 5000, 4900]

計算:
- 平均延遲 = (100+200+5500+...+4900) / 10 = 4,250ms > 5,000ms
- 閾值: 5000ms

結果: 🟡 接近閾值（如果持續 > 5000ms 則觸發）
```text

**場景 4: 探測成功恢復 (Probe Success → CLOSED)**
```
狀態: OPEN (已跳閘 30 秒)
↓
進入 HALF_OPEN 狀態
↓
發送探測請求: GET /health
↓
上游響應: 200 OK (Latency: 150ms)
↓
結果: ✅ 探測成功
↓
狀態轉換: HALF_OPEN → CLOSED
↓
重置計數器: Error Rate = 0%, Timeout Rate = 0%
↓
恢復正常運營
```text

**場景 5: 探測失敗重新跳閘 (Probe Failure → OPEN)**
```
狀態: OPEN (已跳閘 30 秒)
↓
進入 HALF_OPEN 狀態
↓
發送探測請求: GET /health
↓
上游響應: 503 Service Unavailable (或 TIMEOUT)
↓
結果: ❌ 探測失敗
↓
狀態轉換: HALF_OPEN → OPEN
↓
重新計時: 再等待 30 秒
↓
循環直到探測成功
```text

**配置參數**:

```yaml
circuit_breaker:
  # 熔斷觸發條件
  threshold: 10                      # 最少請求數（滑動窗口）
  failure_threshold_percentage: 50  # 錯誤率閾值 (50%)
  timeout_threshold_ms: 5000        # 超時閾值 (5 秒)
  latency_threshold_ms: 5000        # 延遲閾值 (5 秒)

  # 狀態轉換參數
  open_duration_ms: 30000           # OPEN 狀態持續時間 (30 秒)
  half_open_max_requests: 1         # HALF_OPEN 狀態允許的探測請求數

  # 錯誤判定
  break_on_status_codes:
    - 500  # Internal Server Error
    - 502  # Bad Gateway
    - 503  # Service Unavailable
    - 504  # Gateway Timeout

  # 回退響應
  fallback:
    status_code: 503
    headers:
      - "Retry-After: 30"
    body: |
      {
        "error": "Service temporarily unavailable due to circuit breaker",
        "retry_after_seconds": 30,
        "state": "OPEN"
      }
```text

**監控指標與告警**:

```
# Prometheus Metrics
circuit_breaker_state{service="payment-gateway"} = 1  # 0=CLOSED, 1=OPEN, 2=HALF_OPEN
circuit_breaker_requests_total{service="payment-gateway", result="success"} = 1234
circuit_breaker_requests_total{service="payment-gateway", result="failure"} = 567
circuit_breaker_trips_total{service="payment-gateway"} = 3

# Alert Rules
- alert: CircuitBreakerOpen
  expr: circuit_breaker_state == 1
  for: 5m
  labels:
    severity: critical
  annotations:
    summary: "Circuit breaker open for {{ $labels.service }}"
    description: "Circuit breaker has been open for 5 minutes"

- alert: CircuitBreakerFrequentTrips
  expr: rate(circuit_breaker_trips_total[1h]) > 3
  labels:
    severity: warning
  annotations:
    summary: "Circuit breaker tripping frequently for {{ $labels.service }}"
```text

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
```text

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
```text

## 3.5 DDoS 緩解策略 (DDoS Mitigation)

### 3.5.1 多層 DDoS 防禦架構

##### 📊 Diagram 3: 多層 DDoS 防禦架構 (Multi-Layer DDoS Defense Architecture)

```mermaid
flowchart TD
    ATTACKER[攻擊者<br/>DDoS Attack Source<br/>Botnet / Script Kiddie]

    ATTACKER -->|大量惡意請求<br/>100k+ req/s| LAYER1

    subgraph LAYER1["Layer 1: Network Layer (L3/L4) - 網絡層防禦"]
        L1_PROVIDER[Cloudflare / Akamai / AWS Shield<br/>Global Anycast Network]
        L1_SYN[SYN Flood Protection<br/>SYN Cookie 驗證]
        L1_UDP[UDP Amplification Protection<br/>NTP/DNS Reflection 緩解]
        L1_RATE[Rate Limit: 100K pps per IP<br/>超過直接丟棄]
        L1_ANYCAST[Anycast Routing<br/>分散流量至全球節點]

        L1_PROVIDER --> L1_SYN
        L1_PROVIDER --> L1_UDP
        L1_PROVIDER --> L1_RATE
        L1_PROVIDER --> L1_ANYCAST
    end

    L1_ANYCAST -->|合法流量<br/>< 1000 req/s per IP| LAYER2

    subgraph LAYER2["Layer 2: Application Layer (L7) - 應用層防禦"]
        L2_WAF[Cloudflare WAF<br/>Web Application Firewall]
        L2_HTTP[HTTP Flood Protection<br/>識別 HTTP Flood 模式]
        L2_SLOWLORIS[Slowloris Protection<br/>檢測慢速 HTTP DoS]
        L2_CHALLENGE[JavaScript Challenge<br/>疑似 IP 需完成 JS 驗證]
        L2_CAPTCHA[CAPTCHA 挑戰<br/>高風險請求需人工驗證]

        L2_WAF --> L2_HTTP
        L2_WAF --> L2_SLOWLORIS
        L2_HTTP --> L2_CHALLENGE
        L2_SLOWLORIS --> L2_CHALLENGE
        L2_CHALLENGE --> L2_CAPTCHA
    end

    LAYER2 -->|驗證通過<br/>< 100 req/s per IP| LAYER3

    subgraph LAYER3["Layer 3: Behavioral Analysis (ML-based) - 行為分析層"]
        L3_BOT[Cloudflare Bot Management<br/>Imperva Advanced Bot Protection]
        L3_PATTERN[Bot Pattern Detection<br/>時序、Header、行為分析]
        L3_GOODBOT[Good Bot Whitelist<br/>Googlebot, Bingbot]
        L3_BADBOT[Bad Bot Detection<br/>Scraper, Credential Stuffing]
        L3_BLOCK[Block Suspicious Bots<br/>403 Forbidden]

        L3_BOT --> L3_PATTERN
        L3_PATTERN --> L3_GOODBOT
        L3_PATTERN --> L3_BADBOT
        L3_BADBOT --> L3_BLOCK
    end

    L3_GOODBOT -->|合法爬蟲<br/>允許通過| LAYER4
    L3_PATTERN -->|人類用戶<br/>正常流量| LAYER4

    subgraph LAYER4["Layer 4: Origin Protection - 源站保護層"]
        L4_HIDE[Hide Origin IP<br/>僅允許 Cloudflare IP 訪問]
        L4_WHITELIST[IP Whitelist<br/>Cloudflare IP Range Only]
        L4_AUTOSCALE[Health-based Auto-scaling<br/>攻擊時水平擴展]
        L4_BACKUP[Backup Origin<br/>不同區域的備份源站]
        L4_MONITOR[Real-time Monitoring<br/>攻擊檢測與自動切換]

        L4_HIDE --> L4_WHITELIST
        L4_WHITELIST --> L4_AUTOSCALE
        L4_AUTOSCALE --> L4_BACKUP
        L4_BACKUP --> L4_MONITOR
    end

    L4_MONITOR -->|清潔流量<br/>< 50 req/s per IP| ORIGIN[Origin Server<br/>Kong/APISIX Gateway<br/>Backend API Cluster]

    L3_BLOCK --> REJECT1[🚫 拒絕<br/>403 Forbidden]
    L2_CAPTCHA -->|驗證失敗| REJECT2[🚫 拒絕<br/>CAPTCHA Failed]
    L1_RATE -->|超過限流| REJECT3[🚫 丟棄<br/>Silently Drop]

    style ATTACKER fill:#FF6B6B,stroke:#C92A2A,stroke-width:3px,color:#000
    style LAYER1 fill:#FFF3E0,stroke:#E65100,stroke-width:2px
    style LAYER2 fill:#E1F5FE,stroke:#0277BD,stroke-width:2px
    style LAYER3 fill:#F3E5F5,stroke:#6A1B9A,stroke-width:2px
    style LAYER4 fill:#E8F5E9,stroke:#2E7D32,stroke-width:2px
    style ORIGIN fill:#4CAF50,stroke:#1B5E20,stroke-width:3px,color:#FFF
    style REJECT1 fill:#FFCDD2,stroke:#C62828,stroke-width:2px
    style REJECT2 fill:#FFCDD2,stroke:#C62828,stroke-width:2px
    style REJECT3 fill:#FFCDD2,stroke:#C62828,stroke-width:2px
```text

**防禦層詳細說明**:

| 防禦層 | 防護範圍 | 攻擊類型 | 處置措施 | 誤殺率 | 成本 |
|--------|----------|----------|----------|--------|------|
| **Layer 1: Network (L3/L4)** | SYN Flood, UDP Amplification, ICMP Flood | 網絡層 DDoS | SYN Cookie, 流量清洗, 黑洞路由 | 極低 (< 0.01%) | $$$$ (CDN 費用) |
| **Layer 2: Application (L7)** | HTTP Flood, Slowloris, GET/POST Flood | 應用層 DDoS | JS Challenge, CAPTCHA, Rate Limit | 低 (< 1%) | $$$ (WAF 費用) |
| **Layer 3: Behavioral (ML)** | Bot Scraping, Credential Stuffing, API Abuse | 自動化攻擊 | Bot 指紋識別, 行為分析, 機器學習 | 中 (1-5%) | $$ (Bot Management) |
| **Layer 4: Origin Protection** | 繞過 CDN 的直接攻擊 | 源站直接攻擊 | IP 白名單, Auto-scaling, 故障轉移 | 極低 (< 0.01%) | $ (Infrastructure) |

**防禦效果分析**:

**攻擊場景 1: 大規模 DDoS 攻擊 (100Gbps)**
```
攻擊流量: 100 Gbps (約 100k pps)
├─ Layer 1 (Cloudflare): 吸收 95 Gbps (Anycast 分散)
│  └─ 剩餘: 5 Gbps 通過
├─ Layer 2 (WAF): 識別 HTTP Flood，攔截 4 Gbps
│  └─ 剩餘: 1 Gbps 通過
├─ Layer 3 (Bot Management): 識別自動化工具，攔截 0.8 Gbps
│  └─ 剩餘: 0.2 Gbps 通過
└─ Layer 4 (Origin): Auto-scaling 水平擴展，承受 0.2 Gbps

結果: ✅ 源站安全（僅承受 0.2% 原始攻擊流量）
```text

**攻擊場景 2: 慢速 HTTP DoS (Slowloris)**
```
攻擊特徵: 保持大量 HTTP 連接，但極慢傳輸
├─ Layer 1: 通過（網絡層無法檢測）
├─ Layer 2 (Slowloris Protection): 檢測到慢速連接
│  └─ 強制關閉超過 30 秒未完成的連接
│  └─ 攔截率: 99%
└─ Layer 3: 不需要（已在 Layer 2 攔截）

結果: ✅ 攻擊失敗（Layer 2 攔截）
```text

**攻擊場景 3: 分散式爬蟲 (Distributed Scraping)**
```
攻擊特徵: 10,000 個不同 IP 緩慢爬取數據
├─ Layer 1: 通過（單 IP 流量正常）
├─ Layer 2: 通過（HTTP 行為正常）
├─ Layer 3 (Bot Management): 識別爬蟲特徵
│  ├─ User-Agent 模式識別 (85% 檢出)
│  ├─ Header 指紋識別 (10% 檢出)
│  └─ 行為時序分析 (5% 檢出)
│  └─ 總攔截率: 98%
└─ Layer 4: 剩餘 2% 通過（可接受範圍）

結果: ✅ 爬蟲大量攔截（98%）
```text

**攻擊場景 4: 繞過 CDN 直接攻擊源站**
```
攻擊方式: 攻擊者發現源站真實 IP，繞過 Cloudflare
├─ Layer 1-3: 繞過（未經過 CDN）
└─ Layer 4 (Origin Protection):
   ├─ IP Whitelist: 僅允許 Cloudflare IP
   │  └─ 攔截所有非 Cloudflare IP 的請求
   └─ 結果: ❌ 攻擊失敗（源站拒絕連接）

結果: ✅ 源站受保護（IP 白名單生效）
```text

**配置範例**:

**Cloudflare 配置 (Layer 1 + 2)**:
```terraform
resource "cloudflare_zone_settings_override" "igaming" {
  zone_id = var.zone_id

  settings {
    # Layer 1: Network DDoS
    security_level = "high"
    challenge_ttl  = 1800

    # Layer 2: Application DDoS
    waf {
      enabled = true
      mode    = "on"
    }

    rate_limiting {
      enabled = true
    }

    # Bot Management (Layer 3)
    bot_management {
      enabled    = true
      fight_mode = true
    }
  }
}

resource "cloudflare_rate_limit" "api_protection" {
  zone_id   = var.zone_id
  threshold = 100
  period    = 1
  match {
    request {
      url_pattern = "api.casino.com/*"
    }
  }
  action {
    mode    = "challenge"
    timeout = 60
  }
}
```text

**Origin Server 配置 (Layer 4)**:
```nginx
# Nginx: 僅允許 Cloudflare IP
http {
    # Cloudflare IP Whitelist
    geo $cloudflare_ip {
        default          0;
        173.245.48.0/20  1;
        103.21.244.0/22  1;
        # ... (完整 Cloudflare IP 列表)
    }

    server {
        listen 443 ssl;
        server_name api.casino.com;

        # Block non-Cloudflare IPs
        if ($cloudflare_ip = 0) {
            return 403 "Direct access forbidden";
        }

        # Auto-scaling based on load
        upstream backend {
            least_conn;
            server backend-1.internal:8080 weight=5;
            server backend-2.internal:8080 weight=5;
            server backend-3.internal:8080 weight=5 backup;
        }

        location / {
            proxy_pass http://backend;
            limit_req zone=api_limit burst=20;
        }
    }
}
```text

**監控與告警**:
```yaml
# Prometheus Alert Rules
groups:
  - name: ddos-protection
    rules:
      - alert: DDoSAttackDetected
        expr: rate(cloudflare_requests_blocked[5m]) > 1000
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "DDoS attack detected"
          description: "Cloudflare blocking {{ $value }} req/s"

      - alert: OriginUnderAttack
        expr: rate(nginx_http_requests_total{status="403"}[5m]) > 100
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "Origin receiving direct attack attempts"
```text

**成本估算**:

| 防禦層 | 提供商 | 月費用 (估算) | 流量費用 | 備註 |
|--------|--------|---------------|----------|------|
| Layer 1 + 2 | Cloudflare Pro | $200/月 | $0.04/GB | 包含基礎 DDoS 防護 |
| Layer 3 | Cloudflare Bot Management | $1,000/月 | - | ML-based Bot 檢測 |
| Layer 4 | AWS Auto-scaling | $500/月 | 按使用計費 | 基礎設施成本 |
| **總計** | - | **$1,700/月** | + 流量費 | 適用於中型 iGaming 平台 |

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
```text

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
```text

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
```text

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
```text

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

---

**文檔版本**: 1.0.0
**最後更新**: 2026-01-28
**維護團隊**: DevOps Team & SRE Team

---

## 📚 相關文檔

### 前置依賴
- [00-04 技術棧](../00_Concept_&_Analysis/00-04_Technology_Stack.md) - 技術選型

### 相關文檔
- [12-01 部署架構](./12-01_Deployment_Architecture.md) - 部署策略
- [09-01 權限控制](../09_System_Security/09-01_Admin_RBAC.md) - 網關鑑權
