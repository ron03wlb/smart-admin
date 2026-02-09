# Token 緩存性能設計 (Token Cache Performance)

> **Canonical Source**: [09-13-02 Cache Performance](../../source-archive/09_Technical_Infrastructure/09-13-02_Cache_Performance.md)
> **View**: Technical Architecture (Development & DevOps)

---

## 1. 驗證流程設計

### 1.1 Access Token 驗證流程

```mermaid
sequenceDiagram
    participant C as Client
    participant GW as API Gateway
    participant TVS as Token Validation Service
    participant L1 as L1 Caffeine
    participant L2 as L2 Redis
    participant L3 as L3 PostgreSQL

    C->>GW: API Request + Bearer Token
    GW->>TVS: POST /api/token/validate

    TVS->>L1: Check L1 Cache
    alt L1 Hit (99%)
        L1-->>TVS: Token Data (less than 1ms)
    else L1 Miss
        TVS->>L2: Check L2 Cache
        alt L2 Hit (90%)
            L2-->>TVS: Token Data (less than 5ms)
            TVS->>L1: Write to L1
        else L2 Miss
            TVS->>L3: Query PostgreSQL
            L3-->>TVS: Token Data (less than 50ms)
            TVS->>L2: Write to L2
            TVS->>L1: Write to L1
        end
    end

    TVS->>TVS: Check Blacklist
    TVS->>TVS: Validate Permissions
    TVS-->>GW: Validation Result
    GW-->>C: API Response
```

### 1.2 Refresh Token 驗證流程

```mermaid
sequenceDiagram
    participant C as Client
    participant SA as Auth Service
    participant R as Redis
    participant FP as FingerprintJS

    C->>SA: POST /auth/refresh<br/>{refresh_token, device_fp}
    SA->>R: GET refresh_token:{hash}
    R-->>SA: {player_id, device_fp, family_id, created_at}

    SA->>FP: Verify Device Fingerprint
    FP-->>SA: Match Result

    alt Valid + Device Match
        SA->>R: DELETE old token
        SA->>SA: Generate new tokens
        SA->>R: SET new refresh_token
        SA-->>C: New access_token + refresh_token
    else Token Reused
        SA->>R: DELETE ALL tokens in family
        SA-->>C: 401 Security Alert
    end
```

### 1.3 Game Provider HMAC 驗證流程

```mermaid
sequenceDiagram
    participant GP as Game Provider
    participant GW as API Gateway
    participant TVS as Token Validation

    GP->>GW: API Request + API Key + HMAC Signature
    GW->>TVS: Validate GP Token

    TVS->>TVS: 1. Verify API Key exists
    TVS->>TVS: 2. Check IP Whitelist
    TVS->>TVS: 3. Verify HMAC-SHA256 Signature
    TVS->>TVS: 4. Check Timestamp (±5min window)
    TVS->>TVS: 5. Check Nonce (anti-replay)

    alt All Checks Pass
        TVS-->>GW: Valid + GP Context
        GW-->>GP: API Response
    else Any Check Fails
        TVS-->>GW: 401 Unauthorized
        GW-->>GP: Error Response
    end
```

---

## 2. 三層緩存策略

### 2.1 L1 緩存: Caffeine (進程內)

| 配置 | 值 | 說明 |
|------|-----|------|
| **容量** | 10,000 entries | 最大緩存條目數 |
| **TTL** | 100s (寫入後) | 寫入後 100 秒過期 |
| **Access TTL** | 60s (訪問後) | 最後訪問後 60 秒過期 |
| **命中率** | 99% | L1 直接命中 |
| **延遲** | < 1ms | JVM Heap 訪問 |

**適用場景**: 高頻訪問的 Token（如活躍玩家的 Access Token）

### 2.2 L2 緩存: Redis (分佈式)

| 配置 | 值 | 說明 |
|------|-----|------|
| **容量** | 100 萬條 | Redis Cluster |
| **TTL** | 1h | 與 Token 有效期對齊 |
| **命中率** | 90% (of L1 miss) | L1 未命中時的備選 |
| **延遲** | < 5ms | 網絡訪問 |
| **序列化** | Kryo 5.5.0 | 高性能序列化 |

### 2.3 L3 存儲: PostgreSQL (持久化)

| 配置 | 值 | 說明 |
|------|-----|------|
| **容量** | 1000 萬條+ | 所有歷史 Token |
| **命中率** | 100% (of L2 miss) | 最終數據源 |
| **延遲** | < 50ms | 磁盤 I/O |
| **用途** | Blacklist、Token Family、審計 | 持久化存儲 |

---

## 3. 緩存一致性保證

### 3.1 Redis Pub/Sub 失效通知

```mermaid
flowchart LR
    REVOKE[Token Revoke<br/>Event] --> PUB[Redis Pub/Sub<br/>channel: token.invalidation]
    PUB --> S1[Instance 1<br/>Invalidate L1]
    PUB --> S2[Instance 2<br/>Invalidate L1]
    PUB --> S3[Instance 3<br/>Invalidate L1]

    style REVOKE fill:#FFCDD2
    style PUB fill:#FFF3E0
    style S1 fill:#C8E6C9
    style S2 fill:#C8E6C9
    style S3 fill:#C8E6C9
```

**流程**:
1. Token 撤銷時，寫入 Redis 黑名單
2. 發布失效事件至 Redis Pub/Sub
3. 所有實例的 L1 緩存清除對應 Token
4. 下次驗證時從 L2/L3 重新載入

### 3.2 緩存命中率分佈

```
Total Requests: 10,000 QPS
L1 Hit (Caffeine):  9,900 (99.0%) -> <1ms
L2 Hit (Redis):        90 (0.9%)  -> <5ms
L3 Hit (PostgreSQL):   10 (0.1%)  -> <50ms
──────────────────────────────────────────
Weighted Avg Latency: ~1.1ms
```

---

## 4. 性能基準

| 指標 | 目標 | 實測 |
|------|------|------|
| L1 命中率 | > 95% | 99% |
| L2 命中率 (of L1 miss) | > 85% | 90% |
| P99 延遲 (L1 hit) | < 10ms | < 1ms |
| P99 延遲 (L2 hit) | < 20ms | < 5ms |
| 單實例 QPS | > 10,000 | 12,500 |

---

## 相關文檔

- [Token Validation Service](./Token_Validation_Service.md) - 服務總覽
- [Token Validation Architecture](./Token_Validation_Architecture.md) - 驗證架構
- [Token Edge Deployment](./Token_Edge_Deployment.md) - 邊緣部署
- [Caching Strategy](./Caching_Strategy.md) - JetCache 緩存策略
