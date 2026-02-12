# 信用網絡技術架構（Credit Network Technical Architecture）

> **業務需求**: [信用網絡需求（Credit Network Requirements）](../../requirements/07_Agent_Operations/Credit_Network_Requirements.md)
> **規範來源**: [source-archive/07_Agent_Center/07-02_Credit_Network_Logic.md](../../source-archive/07_Agent_Center/07-02_Credit_Network_Logic.md)
> **視角**: 技術架構（Technical Architecture）
> **目標讀者**: 架構師、後端開發人員（Architects, Backend Developers）

---

## 1. 系統架構概覽（System Architecture Overview）

信用網絡（Credit Network）系統管理分層的信用額度分配、持倉（Position Taking）計算以及跨代理樹結構的定期結算（Settlement）。它使用分佈式鎖、樂觀並發控制和事件驅動架構。

---

## 2. 信用傳播樹架構（Credit Propagation Tree Architecture）

```mermaid
graph TB
    subgraph "平台層級（Platform Level）"
        P["平台<br/>總信用池：$10M<br/>已分配：$8M<br/>可用：$2M<br/>公司持倉：10%"]
    end

    subgraph "總代理層級（Master Agent Level）"
        P -->|分配 $5M<br/>持倉：20%| M1["總代理 A<br/>信用額度：$5M<br/>已使用：$3.5M<br/>使用率：70%"]

        P -->|分配 $3M<br/>持倉：15%| M2["總代理 B<br/>信用額度：$3M<br/>已使用：$2.85M<br/>使用率：95% 警報"]
    end

    subgraph "代理 L1 層級（Agent L1 Level）"
        M1 -->|分配 $2M| A1["代理 L1-A1<br/>使用率：60%"]
        M1 -->|分配 $1.5M| A2["代理 L1-A2<br/>使用率：100% 凍結"]
        M2 -->|分配 $2.8M| A4["代理 L1-A4<br/>使用率：93% 警告"]
    end

    subgraph "玩家層級（Player Level）"
        A1 -->|玩家數：50| PG1["玩家群組 1<br/>淨虧損：$100k"]
        A4 -->|玩家數：100| PG3["玩家群組 3<br/>淨虧損：$2.6M"]
    end

    style P fill:#E6E6FA
    style M1 fill:#90EE90
    style M2 fill:#FFB6C1
    style A2 fill:#FF6B6B
    style A4 fill:#FFD93D
```

---

## 3. 信用分配流程（Credit Allocation Flow）- 並發控制（Concurrency Control）

```mermaid
flowchart TD
    START[父代理分配信用給子代理] --> INPUT[輸入：child_agent_id、amount、position_%]

    INPUT --> VALIDATE1{輸入驗證}
    VALIDATE1 -->|amount <= 0| ERR1[錯誤：無效金額]
    VALIDATE1 -->|position_% > 100| ERR2[錯誤：無效持倉百分比]
    VALIDATE1 -->|OK| LOCK

    LOCK["獲取分佈式鎖<br/>Redis: SET NX credit:parent:$id TTL=30s"] --> LOCK_CHECK{鎖獲取成功？}
    LOCK_CHECK -->|否 - 重試 < 3| WAIT["等待 - 指數退避<br/>重試 1：100ms<br/>重試 2：200ms<br/>重試 3：400ms"]
    WAIT --> LOCK
    LOCK_CHECK -->|否 - 重試 >= 3| ERR4["錯誤：鎖超時"]
    LOCK_CHECK -->|是| READ_PARENT

    READ_PARENT["讀取父代理信用記錄<br/>SELECT * FROM agent_credit<br/>WHERE agent_id = parent FOR UPDATE"] --> VERSION_CHECK{版本匹配？}
    VERSION_CHECK -->|否| RETRY_VERSION{重試次數 < 3？}
    RETRY_VERSION -->|是| READ_PARENT
    RETRY_VERSION -->|否| ERR5["錯誤：樂觀鎖衝突"]

    VERSION_CHECK -->|是| CALC_AVAILABLE
    CALC_AVAILABLE["計算可用信用：<br/>available = parent.limit - parent.used - parent.allocated_to_children"]

    CALC_AVAILABLE --> AVAILABLE_CHECK{available >= amount？}
    AVAILABLE_CHECK -->|否| ERR6["錯誤：父代理信用不足"]
    AVAILABLE_CHECK -->|是| UPDATE_PARENT

    UPDATE_PARENT["UPDATE agent_credit SET<br/>allocated_to_children += amount,<br/>version = version + 1<br/>WHERE agent_id = parent AND version = $current_version"]

    UPDATE_PARENT --> UPDATE_CHILD["UPDATE agent_credit SET<br/>credit_limit = new_amount,<br/>position_percent = new_position"]

    UPDATE_CHILD --> INSERT_AUDIT["INSERT INTO credit_allocation_audit<br/>(parent_id, child_id, old_limit, new_limit, delta, reason, operator)"]

    INSERT_AUDIT --> COMMIT[COMMIT Transaction]
    COMMIT --> RELEASE_LOCK["釋放 Redis 鎖<br/>DEL credit:parent:$id"]
    RELEASE_LOCK --> PUBLISH_EVENT["發佈事件到 Kafka：<br/>topic: agent.credit.allocated"]
    PUBLISH_EVENT --> SUCCESS["返回成功"]

    style SUCCESS fill:#90EE90
    style ERR1 fill:#FFB6C1
    style ERR4 fill:#FFD700
    style ERR5 fill:#FFD700
    style ERR6 fill:#FFB6C1
    style LOCK fill:#DDA0DD
    style COMMIT fill:#90EE90
```

---

## 4. 並發控制設計（Concurrency Control Design）

| 並發問題 | 場景 | 解決方案 | 實現方式 |
|---------|------|---------|---------|
| **重複分配** | 父代理同時分配給 2 個子代理，總額超過可用額度 | Redis 分佈式鎖 | `SET NX credit:parent:${id}` TTL=30s |
| **版本衝突** | 2 個操作同時修改父代理的 `allocated_to_children` | 樂觀鎖 | `WHERE version = ? AND UPDATE version = version + 1` |
| **超額分配** | 子代理 A 收到信用，父代理可用額度不足以分配給子代理 B | 悲觀鎖 | `SELECT ... FOR UPDATE` |
| **撤銷衝突** | 父代理撤銷信用時子代理正在使用 | 檢查子代理已使用信用 | `child.used_credit <= new_limit` |
| **死鎖** | 父代理 A 鎖定子代理 B，父代理 B 鎖定子代理 A | 有序加鎖 | 始終先鎖定 `MIN(parent_id, child_id)` |

---

## 5. 每週結算序列（Weekly Settlement Sequence）

```mermaid
sequenceDiagram
    participant CronJob
    participant SettlementService
    participant AgentL2
    participant AgentL1
    participant MasterAgent
    participant Platform
    participant DB
    participant NotificationService

    Note over CronJob,NotificationService: 每週結算 - 週一 12:00 AM

    rect rgb(255, 230, 230)
        Note over CronJob,DB: 階段 1：凍結與計算（12:00 - 13:00）

        CronJob->>SettlementService: triggerWeeklySettlement(week=W-1)
        SettlementService->>DB: BEGIN TRANSACTION (SERIALIZABLE)
        SettlementService->>DB: UPDATE credit_accounts SET status='FROZEN'

        loop 每個代理 L2（自下而上）
            SettlementService->>AgentL2: calculatePosition(week=W-1)
            AgentL2->>DB: SELECT SUM(player_bets - player_wins) FROM player_transactions
            DB-->>AgentL2: net_player_loss = $100,000
            AgentL2->>DB: INSERT INTO settlement_records (agent_id, week, own_share, to_parent)
        end

        loop 每個代理 L1
            AgentL1->>DB: SELECT SUM(to_parent) FROM settlement_records WHERE parent_id = L1
            AgentL1->>DB: INSERT INTO settlement_records
        end

        SettlementService->>DB: COMMIT
    end

    rect rgb(230, 255, 230)
        Note over SettlementService,NotificationService: 階段 2：收款（週一 13:00 - 週五 18:00）

        NotificationService->>AgentL2: Email："您欠款 $50k。截止日期：週五 18:00"
        AgentL2->>SettlementService: submitPaymentProof(txn_id, amount)
    end

    rect rgb(230, 230, 255)
        Note over SettlementService,Platform: 階段 3：驗證與重置（週五 18:00 - 週六 12:00）

        SettlementService->>DB: UPDATE agent_credit SET used_credit = 0, status = 'ACTIVE'
    end
```

---

## 6. 資料庫架構（Database Schema）

### 6.1 agent_credit 表

```sql
CREATE TABLE agent_credit (
    agent_id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(32) NOT NULL,
    parent_id BIGINT,
    credit_limit DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    used_credit DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    allocated_to_children DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    position_percent DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    max_position DECIMAL(5,2) NOT NULL DEFAULT 100.00,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version INT NOT NULL DEFAULT 0,
    frozen_at TIMESTAMP,
    last_settlement TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_tenant (tenant_id),
    INDEX idx_parent (parent_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 6.2 settlement_records 表

```sql
CREATE TABLE settlement_records (
    record_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(32) NOT NULL,
    agent_id BIGINT NOT NULL,
    parent_id BIGINT,
    settlement_week VARCHAR(10) NOT NULL,
    player_loss DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    own_share DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    to_parent DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    to_platform DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    payment_status VARCHAR(20) DEFAULT 'PENDING',
    payment_txn_id VARCHAR(100),
    verified_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_agent_week (agent_id, settlement_week),
    INDEX idx_parent_week (parent_id, settlement_week),
    INDEX idx_payment_status (payment_status),
    UNIQUE KEY uk_agent_week (agent_id, settlement_week)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 6.3 credit_allocation_audit 表

```sql
CREATE TABLE credit_allocation_audit (
    audit_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(32) NOT NULL,
    parent_id BIGINT NOT NULL,
    child_id BIGINT NOT NULL,
    old_limit DECIMAL(15,2),
    new_limit DECIMAL(15,2),
    delta DECIMAL(15,2),
    old_position DECIMAL(5,2),
    new_position DECIMAL(5,2),
    reason VARCHAR(500),
    operator VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_parent_child (parent_id, child_id),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 7. Kafka 事件架構（Kafka Event Schema）

```json
{
  "event_type": "agent.credit.allocated",
  "timestamp": "2026-01-27T15:30:00Z",
  "data": {
    "parent_id": "agent_001",
    "child_id": "agent_002",
    "old_limit": 1000000,
    "new_limit": 1500000,
    "delta": 500000,
    "position_percent": 45.0,
    "parent_available_after": 500000,
    "operator": "admin_123",
    "reason": "Performance upgrade"
  }
}
```

**下游消費者**：
- **風險管理服務（Risk Management Service）**：監控異常信用變更（例如突然 100% 增加）
- **通知服務（Notification Service）**：發送電子郵件/簡訊通知
- **分析服務（Analytics Service）**：追蹤信用分配趨勢、代理活動
- **審計服務（Audit Service）**：歸檔審計日誌（7 年保留期）

---

## 8. 錯誤碼定義（Error Code Definitions）

```java
public enum CreditErrorCode {
    CREDIT_001("INVALID_AMOUNT", 400, "Allocation amount must be > 0"),
    CREDIT_002("INVALID_POSITION", 400, "Position percentage must be <= 100%"),
    CREDIT_003("EXCEEDS_PARENT_LIMIT", 403, "Exceeds parent max position limit"),
    CREDIT_004("LOCK_TIMEOUT", 409, "Distributed lock acquisition timeout"),
    CREDIT_005("OPTIMISTIC_LOCK_CONFLICT", 409, "Optimistic lock version conflict"),
    CREDIT_006("INSUFFICIENT_PARENT_CREDIT", 403, "Parent available credit insufficient"),
    CREDIT_007("CANNOT_REDUCE_BELOW_USED", 409, "Cannot reduce below child used credit");

    private final String code;
    private final int httpStatus;
    private final String description;

    CreditErrorCode(String code, int httpStatus, String description) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.description = description;
    }
}
```

---

## 9. 風險控制整合（Risk Control Integration）

信用網絡風險檢測必須與風險框架整合以進行實時監控。

```java
/**
 * Credit risk assessment integration
 * Called during credit allocation and settlement
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditRiskAssessmentService {

    private final RiskEngineClient riskEngineClient;
    private final AgentCreditDao agentCreditDao;

    /**
     * Evaluate risk before credit allocation
     */
    public RiskAssessment evaluateAllocation(Long parentId, Long childId, BigDecimal amount) {
        AgentCredit child = agentCreditDao.selectById(childId);

        RiskCheckRequest request = RiskCheckRequest.builder()
            .agentId(childId)
            .parentId(parentId)
            .requestedAmount(amount)
            .registrationAge(child.getRegistrationAgeDays())
            .historicalOverdueCount(child.getOverdueCount())
            .build();

        return riskEngineClient.evaluate(request);
    }
}
```

---

## 10. SmartAdmin 架構映射（SmartAdmin Architecture Mapping）

| 層級 | 類別 | 職責 |
|-----|------|------|
| **Controller** | `CreditNetworkController` | 信用分配的 REST API 端點 |
| **Service** | `CreditNetworkService` | 業務邏輯，通過 Dao 進行單表查詢 |
| **Manager** | `CreditSettlementManager` | @Transactional 結算操作，多表寫入 |
| **Dao** | `AgentCreditDao` | agent_credit 表的 MyBatis Plus mapper |
| **Entity** | `AgentCreditEntity` | 資料庫實體映射 |

**關鍵架構規則**：
- `@Transactional(rollbackFor = Throwable.class)` 僅在 Manager 層
- Service 使用 `io.vavr.control.Option` 處理可空返回值
- 通過 `@RequiredArgsConstructor` + `private final` 進行構造器注入

---

**文檔版本**: 4.0.0
**最後更新**: 2026-02-09
**維護團隊**: 代理網絡團隊與後端團隊（Agent Network Team & Backend Team）
