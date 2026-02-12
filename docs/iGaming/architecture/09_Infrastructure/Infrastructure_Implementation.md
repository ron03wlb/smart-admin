# 基礎設施實作（Infrastructure Implementation）

> **業務需求**: 不適用 — 純技術基礎設施文件
> **規範來源**: [00-16_Infrastructure_Implementation.md](../../source-archive/00_Foundation/guides/00-16_Infrastructure_Implementation.md)
> **目標讀者**: Architects, DevOps Engineers, Backend Engineers
> **最後同步**: 2026-02-09

---

## 1. 概覽（Overview）

本文檔涵蓋 iGaming 平台基礎設施的技術實作，包括 API Gateway 設計、Blue-Green 部署策略以及 API 速率限制。所有實作均遵循 SmartAdmin 架構模式，並以達成生產級可靠性為目標。

**注意**：本文檔為純技術內容，無對應的需求文檔。所有基礎設施決策均由非功能性需求（可用性、可擴展性、效能）驅動。

---

## 2. API Gateway 設計（API Gateway Design）

**狀態**：PLANNED（Phase 5+）
**模組**：12_Technical_Operations, 10_Platform_Management

### 實作目標（Implementation Goal）

配置 Spring Cloud Gateway，具備動態路由、Filter Chain（身份驗證、速率限制）以及透過 Resilience4j 整合的 Circuit Breaker。

### 實作閱讀順序（Implementation Reading Order）

| 順序 | 文檔 | 章節 | 重點 |
|-------|----------|---------|-------|
| 1 | [09-02-01 Gateway Core](../../source-archive/09_Technical_Infrastructure/09-02-01_Gateway_Core.md) | S2 Route Config | 動態路由 |
| 2 | [09-02-01 Gateway Core](../../source-archive/09_Technical_Infrastructure/09-02-01_Gateway_Core.md) | S3 Filter Chain | 身份驗證、速率限制 |
| 3 | [09-02-01 Gateway Core](../../source-archive/09_Technical_Infrastructure/09-02-01_Gateway_Core.md) | S4 Circuit Breaker | Resilience4j |

### Gateway 架構（Gateway Architecture）

```
Client Request
    ↓
[Spring Cloud Gateway]
    ↓
Route Predicate Matching → Route Not Found → 404
    ↓ (matched)
Filter Chain Execution
    ├── 1. Authentication Filter (Sa-Token validation)
    ├── 2. Rate Limiting Filter (Redis-based token bucket)
    ├── 3. Request Logging Filter (audit trail)
    └── 4. Circuit Breaker Filter (Resilience4j)
    ↓
Backend Service (load balanced)
    ↓
Response → Client
```

### SmartAdmin 層級映射（SmartAdmin Layer Mapping）

| 元件 | 技術 | 配置 |
|-----------|-----------|---------------|
| Gateway Framework | Spring Cloud Gateway | Reactive 路由引擎 |
| Auth Filter | Sa-Token | 每次請求的 Token 驗證 |
| Rate Limiter | Redisson | Token Bucket 演算法搭配 Redis 後端 |
| Circuit Breaker | Resilience4j | 可配置的失敗閾值 |
| Load Balancer | Spring Cloud LoadBalancer | Round-robin 搭配健康檢查 |

### Route 配置模式（Route Configuration Pattern）

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: player-service
          uri: lb://player-service
          predicates:
            - Path=/api/player/**
          filters:
            - name: CircuitBreaker
              args:
                name: player-cb
                fallbackUri: forward:/fallback/player
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 100
                redis-rate-limiter.burstCapacity: 200
```

### Filter Chain 順序（Filter Chain Order）

```java
@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {
    @Override
    public int getOrder() {
        return -100; // Authentication FIRST
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Sa-Token validation
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (!StpUtil.isLogin(token)) {
            return unauthorized(exchange);
        }
        return chain.filter(exchange);
    }
}
```

### 驗證檢查清單（Verification Checklist）

- [ ] Route 規則正確
- [ ] Filter Chain 正常運作
- [ ] Circuit Breaker 有效
- [ ] 負載均衡準確

### 常見陷阱（Common Pitfalls）

1. **Route 衝突**：多個 Route 匹配同一請求 -- 使用具體的 Path Predicate 並搭配排序
2. **Filter 順序錯誤**：身份驗證在速率限制之後執行 -- 確保 Auth Filter 擁有最低的 Order 值
3. **Circuit Breaker 閾值**：過於敏感或過於寬鬆 -- 根據 Staging 環境的實際錯誤率進行調整

---

## 3. Blue-Green 部署（Blue-Green Deployment）

**狀態**：PLANNED（Phase 5+）
**模組**：12_Technical_Operations

### 實作目標（Implementation Goal）

配置基於 Kubernetes 的 Blue-Green 部署，具備 Istio/Nginx 流量切換、回滾策略以及煙霧測試驗證。

### 實作閱讀順序（Implementation Reading Order）

| 順序 | 文檔 | 章節 | 重點 |
|-------|----------|---------|-------|
| 1 | [09-01 Deployment](../../source-archive/09_Technical_Infrastructure/09-01_Deployment.md) | S3 Deployment Strategy | Blue-Green |
| 2 | [09-01 Deployment](../../source-archive/09_Technical_Infrastructure/09-01_Deployment.md) | S4 Traffic Management | Istio/Nginx |
| 3 | [09-01 Deployment](../../source-archive/09_Technical_Infrastructure/09-01_Deployment.md) | S5 Monitoring Validation | Smoke Test |

### 部署架構（Deployment Architecture）

```
                    ┌──────────────┐
                    │   Ingress    │
                    │  Controller  │
                    └──────┬───────┘
                           │
                    ┌──────┴───────┐
                    │ Traffic Split │ (Istio VirtualService)
                    └──┬────────┬──┘
                       │        │
              ┌────────┴──┐  ┌──┴────────┐
              │   Blue    │  │   Green   │
              │ (current) │  │  (new)    │
              │ v1.2.0    │  │ v1.3.0    │
              └───────────┘  └───────────┘
```

### 部署流程（Deployment Pipeline）

```yaml
# Kubernetes Blue-Green Deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: smartadmin-green
  labels:
    app: smartadmin
    version: green
spec:
  replicas: 3
  selector:
    matchLabels:
      app: smartadmin
      version: green
  template:
    spec:
      containers:
        - name: smartadmin
          image: smartadmin:v1.3.0
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 1024
            initialDelaySeconds: 30
            periodSeconds: 10
```

### Istio 流量切換（Traffic Switching with Istio）

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: smartadmin-vs
spec:
  hosts:
    - smartadmin.example.com
  http:
    - route:
        - destination:
            host: smartadmin
            subset: blue
          weight: 0      # Old version
        - destination:
            host: smartadmin
            subset: green
          weight: 100    # New version (after validation)
```

### 煙霧測試策略（Smoke Test Strategy）

```bash
#!/bin/bash
# Post-deployment smoke test
ENDPOINTS=(
  "/actuator/health"
  "/api/system/health"
  "/api/system/version"
)

for endpoint in "${ENDPOINTS[@]}"; do
  status=$(curl -s -o /dev/null -w "%{http_code}" "http://green-service:1024${endpoint}")
  if [ "$status" != "200" ]; then
    echo "FAIL: ${endpoint} returned ${status}"
    exit 1  # Trigger rollback
  fi
done
echo "All smoke tests passed"
```

### 回滾策略（Rollback Strategy）

| 觸發條件 | 動作 | 持續時間 |
|---------|--------|----------|
| 煙霧測試失敗 | 自動回滾到 Blue | < 30 秒 |
| 錯誤率飆升（>5%） | 自動將流量切回 Blue | < 1 分鐘 |
| 手動回滾請求 | Istio 權重切換至 Blue | < 1 分鐘 |

### 驗證檢查清單（Verification Checklist）

- [ ] Blue/Green 環境獨立
- [ ] 流量切換順暢
- [ ] 回滾機制有效
- [ ] 監控告警正常運作

### 常見陷阱（Common Pitfalls）

1. **Database Migration**：Blue/Green 之間的 Schema 版本不匹配 -- 僅使用向後相容的 Migration
2. **狀態洩漏**：Session/Cache 未同步 -- 使用外部 Redis 作為共享狀態
3. **回滾失敗**：DNS Cache 阻止完整流量切換 -- 設定低 TTL 值，使用 Istio 進行即時切換

---

## 4. API 速率限制（API Rate Limiting）

**狀態**：PLANNED（Phase 5+）
**模組**：12_Technical_Operations, Foundation modules

### 實作目標（Implementation Goal）

實作基於 Redis 後端的 Token Bucket 速率限制，支援多層限制（全域、每租戶、每使用者）以及適當的 429 錯誤回應。

### 實作閱讀順序（Implementation Reading Order）

| 順序 | 文檔 | 章節 | 重點 |
|-------|----------|---------|-------|
| 1 | [09-03-01 Design Principles](../../source-archive/09_Technical_Infrastructure/09-03-01_Design_Principles.md) | S6 Rate Limiting | Token bucket / leaky bucket |
| 2 | Foundation Redis Limiter | - | Redisson rate limiter |
| 3 | [09-02-01 Gateway Core](../../source-archive/09_Technical_Infrastructure/09-02-01_Gateway_Core.md) | S3.4 Rate Limiting Filter | Gateway rate limiting |

### 速率限制架構（Rate Limiting Architecture）

```
Request → Gateway Rate Limiter (global)
              ↓
         Tenant Rate Limiter (per-tenant)
              ↓
         User Rate Limiter (per-user)
              ↓
         Endpoint Rate Limiter (per-API)
              ↓
         Backend Service
```

### 多層速率限制（Multi-Layer Rate Limiting）

| 層級 | 範圍 | 實作 | 預設限制 |
|-------|-------|---------------|----------------|
| Global | 所有請求 | Gateway filter | 10,000 req/s |
| Tenant | 每營運商 | Redisson RRateLimiter | 1,000 req/s |
| User | 每玩家 | Redisson RRateLimiter | 100 req/s |
| Endpoint | 每 API 路徑 | Spring annotation | 依端點而異 |

### Token Bucket 實作（Token Bucket Implementation）

```java
@Component
@RequiredArgsConstructor
public class RedisRateLimiter {
    private final RedissonClient redissonClient;

    public boolean tryAcquire(String key, long rate, long rateInterval,
                               RateIntervalUnit unit) {
        RRateLimiter limiter = redissonClient.getRateLimiter(key);
        limiter.trySetRate(RateType.OVERALL, rate, rateInterval, unit);
        return limiter.tryAcquire(1);
    }
}
```

### 端點級速率限制（Endpoint-Level Rate Limiting）

```java
// Custom annotation for endpoint-specific rate limiting
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int permits() default 100;
    int period() default 60;  // seconds
    String key() default "";  // defaults to endpoint path
}

// Controller usage
@GetMapping("/api/player/profile")
@RateLimit(permits = 50, period = 60)
public ResponseDTO<PlayerVO> getProfile() {
    // ...
}
```

### 429 錯誤回應（429 Error Response）

```java
// Standard rate limit exceeded response
public class RateLimitFilter implements GlobalFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientKey = extractClientKey(exchange);
        if (!rateLimiter.tryAcquire(clientKey, 100, 1, RateIntervalUnit.MINUTES)) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().add("Retry-After", "60");
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }
}
```

### 驗證檢查清單（Verification Checklist）

- [ ] 速率限制閾值正確
- [ ] Redis Rate Limiter 正常運作
- [ ] 多層速率限制生效
- [ ] 429 錯誤回應正確返回

### 常見陷阱（Common Pitfalls）

1. **粒度過粗**：僅全域限制過於激進 -- 實作多層限制
2. **Redis 單點**：Rate Limiter 未使用 Redis Cluster -- 配置 Redisson 為 Cluster 模式
3. **滑動視窗**：視窗未正確實作 -- 使用 Redisson 內建的 RRateLimiter，其已處理此問題

---

## 5. 參考文檔（Reference Documents）

| 領域 | 文檔 |
|------|----------|
| Gateway Core | [09-02-01 Gateway Core](../../source-archive/09_Technical_Infrastructure/09-02-01_Gateway_Core.md) |
| Deployment | [09-01 Deployment](../../source-archive/09_Technical_Infrastructure/09-01_Deployment.md) |
| API Design Principles | [09-03-01 Design Principles](../../source-archive/09_Technical_Infrastructure/09-03-01_Design_Principles.md) |
| Rate Limiting | [09-02-02 Rate Limiting](../../source-archive/09_Technical_Infrastructure/09-02-02_Rate_Limiting.md) |

---

## 6. 資料庫 Schema（Database Schema）

```sql
-- Gateway route configuration
CREATE TABLE t_gateway_route_config (
    id              BIGSERIAL PRIMARY KEY,
    route_id        VARCHAR(100) NOT NULL UNIQUE,
    uri             VARCHAR(500) NOT NULL,
    predicates      JSONB NOT NULL,
    filters         JSONB,
    metadata        JSONB,
    order_num       INTEGER NOT NULL DEFAULT 0,
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_route_enabled ON t_gateway_route_config(enabled, order_num);

-- Deployment history tracking
CREATE TABLE t_deployment_history (
    id              BIGSERIAL PRIMARY KEY,
    service_name    VARCHAR(100) NOT NULL,
    version         VARCHAR(50) NOT NULL,
    environment     VARCHAR(20) NOT NULL,
    deployment_type VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    deployed_by     VARCHAR(100),
    started_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMP,
    rollback_version VARCHAR(50),
    notes           TEXT
);

CREATE INDEX idx_deploy_service ON t_deployment_history(service_name, environment, started_at DESC);

-- Rate limiter configuration
CREATE TABLE t_rate_limiter_config (
    id              BIGSERIAL PRIMARY KEY,
    limiter_key     VARCHAR(200) NOT NULL UNIQUE,
    layer           VARCHAR(20) NOT NULL,
    replenish_rate  INTEGER NOT NULL,
    burst_capacity  INTEGER NOT NULL,
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_limiter_layer ON t_rate_limiter_config(layer, enabled);

-- Circuit breaker configuration
CREATE TABLE t_circuit_breaker_config (
    id              BIGSERIAL PRIMARY KEY,
    service_name    VARCHAR(100) NOT NULL UNIQUE,
    failure_rate_threshold DECIMAL(5, 2) NOT NULL DEFAULT 50,
    wait_duration_seconds INTEGER NOT NULL DEFAULT 30,
    sliding_window_size INTEGER NOT NULL DEFAULT 100,
    minimum_calls   INTEGER NOT NULL DEFAULT 10,
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
```

---

**文件版本**: 1.0.0
**最後更新**: 2026-02-08
**來源版本**: 4.0.0
