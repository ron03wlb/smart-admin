# 07-02-01 網關核心架構 (Gateway Core Architecture)

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

    style CF fill:#FF9800
    style KONG fill:#FFC107

    style R1 fill:#E3F2FD
    style R2 fill:#E3F2FD
    style R3 fill:#E3F2FD
    style R4 fill:#E3F2FD

    style CDN_SSR fill:#C8E6C9
    style API_V1 fill:#C8E6C9
    style API_V2 fill:#C8E6C9
    style MOBILE_API fill:#C8E6C9
    style BACKOFFICE fill:#C8E6C9

    style REDIS fill:#FFE082
    style PG fill:#FFE082
    style MONITOR fill:#FFE082
```

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
```

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
```

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
```

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
```

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
```

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

