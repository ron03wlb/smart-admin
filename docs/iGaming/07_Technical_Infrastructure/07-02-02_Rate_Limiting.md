# 07-02-02 流量控制與限流 (Rate Limiting & Traffic Control)

## 1. 流量控制概述 (Traffic Control)

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

    note right of CLOSED : 正常狀態 (CLOSED):<br/>- 所有請求轉發至上游<br/>- 監控指標:<br/>  * Error Rate (5xx)<br/>  * Timeout Rate<br/>  * Avg Latency<br/>- 滑動窗口: 最近 10 個請求

    note right of OPEN : 跳閘狀態 (OPEN):<br/>- 快速失敗 (Fail-Fast)<br/>- 不調用上游服務<br/>- 減輕上游壓力<br/>- 防止雪崩效應<br/>- 固定等待: 30 秒

    note right of HALF_OPEN : 半開狀態 (HALF_OPEN):<br/>- 發送探測請求<br/>- 驗證上游是否恢復<br/>- 成功 → CLOSED<br/>- 失敗 → OPEN (重新跳閘)
```

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
```

**場景 2: 超時率觸發 (Timeout Rate Trigger)**
```
最近 10 個請求結果:
[200, 200, TIMEOUT, 200, TIMEOUT, TIMEOUT, 200, TIMEOUT, TIMEOUT, TIMEOUT]

計算:
- 成功: 4 個 (200)
- 超時: 6 個 (TIMEOUT)
- Timeout Rate = 6/10 = 60% > 50%

結果: 🔴 觸發熔斷 (CLOSED → OPEN)
```

**場景 3: 平均延遲觸發 (Average Latency Trigger)**
```
最近 10 個請求延遲 (ms):
[100, 200, 5500, 6000, 4500, 300, 5200, 5800, 5000, 4900]

計算:
- 平均延遲 = (100+200+5500+...+4900) / 10 = 4,250ms > 5,000ms
- 閾值: 5000ms

結果: 🟡 接近閾值（如果持續 > 5000ms 則觸發）
```

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
```

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
```

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
```

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

