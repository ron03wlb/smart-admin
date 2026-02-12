# 風控實作指南（Risk Implementation）

> **規範來源**: [00-14_Risk_Implementation.md](../../source-archive/00_Foundation/guides/00-14_Risk_Implementation.md)
> **目標讀者**: 架構師、後端工程師、數據工程師
> **業務需求**: [Risk_Requirements_Summary.md](../../requirements/05_Risk_Compliance/Risk_Requirements_Summary.md)
> **最後同步**: 2026-02-08

---

## 1. 概述（Overview）

本文檔涵蓋 iGaming 平台風控系統的技術實作，包括風控規則引擎、欺詐檢測演算法以及代理信用管理。所有實作遵循 SmartAdmin 分層架構模式。

---

## 2. 風控規則引擎（Risk Rule Engine）

**狀態**: 已規劃 (Phase 5+)
**模組**: 05_Risk_Control, 01_Core_Financial_Loop

### 實作目標（Implementation Goal）

設計並實作整合 Drools/LiteFlow 的規則引擎架構，具備動態規則配置與熱重載能力。

### 實作閱讀順序（Implementation Reading Order）

| 順序 | 文檔 | 章節 | 重點 |
|------|------|------|------|
| 1 | [05-01 Risk Framework](../../source-archive/05_Risk_Control/05-01_Risk_Framework.md) | S2 Rule Engine | Drools 整合 |
| 2 | [05-05 Risk Proposal Workflow](../../source-archive/05_Risk_Control/05-05_Risk_Proposal_Workflow.md) | S3 Approval Flow | 工作流設計 |
| 3 | [01-05 Withdrawal Risk](../../source-archive/01_Player_Center/01-05_Withdrawal_Risk.md) | S4 Risk Rules | 真實案例 |

### SmartAdmin 層級映射（SmartAdmin Layer Mapping）

| 層級 | 職責 |
|------|------|
| Controller | 風險評估 API、規則管理後台端點 (`@SaCheckPermission`) |
| Service | 規則匹配邏輯、風險分數聚合 (Vavr Option 用於可選規則配置查詢) |
| Manager | @Transactional 規則持久化 + 版本管理、@Cacheable 規則快取與熱重載 |
| Dao | 規則定義、風險事件日誌、評分歷史（透過 MyBatis Plus） |

### 規則引擎架構（Rule Engine Architecture）

```mermaid
graph LR
    A[風險事件<br/>Kafka Consumer] --> B[規則匹配<br/>Drools/LiteFlow]
    B --> C[分數計算<br/>加權求和]
    C --> D{分數<br/>範圍?}
    D -->|0-30| E[僅記錄<br/>非同步審計]
    D -->|31-60| F[標記審查<br/>建立提案]
    D -->|61-85| G[升級<br/>通知風控團隊]
    D -->|86-100| H[自動封鎖<br/>帳戶限制]

    B --> I[(規則存儲<br/>@Cacheable)]
    I -->|熱重載| B
```

### 風險分數計算（Risk Score Calculation）

```java
// Service layer: risk score aggregation
public RiskScore evaluate(RiskEvent event) {
    List<RuleResult> results = ruleEngine.matchAll(event);
    return results.stream()
        .map(r -> r.getScore() * r.getWeight())
        .reduce(0.0, Double::sum);
}
```

### 分數區間動作分發（Action Dispatch by Score）

| 分數範圍 | 動作 | 實作方式 |
|---------|------|---------|
| 0-30 | 記錄 | 非同步審計日誌寫入 (Manager) |
| 31-60 | 標記審查 | 建立風險提案 (Manager @Transactional) |
| 61-85 | 升級 | 通知風控團隊 + 建立緊急提案 |
| 86-100 | 自動封鎖 | 立即帳戶限制 (Manager @Transactional) |

### 驗證清單（Verification Checklist）

- [ ] 規則引擎執行正確
- [ ] 動態規則載入成功
- [ ] 規則優先順序正確
- [ ] 風險評分計算準確

### 常見陷阱（Common Pitfalls）

1. **規則衝突**: 多條規則同時匹配 -- 實作基於優先順序的解決機制與提前退出
2. **效能問題**: 規則過多導致執行緩慢 -- 使用索引規則匹配與 Drools RETE 優化
3. **熱更新失效**: 規則更新未生效 -- 在 Manager 中實作規則版本控制，更新時使用 @Cacheable 清除快取

### 資料庫結構（Database Schema）

```sql
-- Risk rule definitions with versioning
CREATE TABLE risk_rules (
    id              BIGSERIAL PRIMARY KEY,
    rule_code       VARCHAR(64) NOT NULL UNIQUE,
    rule_name       VARCHAR(128) NOT NULL,
    rule_type       VARCHAR(32) NOT NULL,   -- VELOCITY, AMOUNT, PATTERN, DEVICE, GEO
    category        VARCHAR(32) NOT NULL,   -- DEPOSIT, WITHDRAWAL, LOGIN, BET, REGISTRATION
    priority        INT NOT NULL DEFAULT 100,
    weight          NUMERIC(5,2) NOT NULL DEFAULT 1.00,
    condition_expr  TEXT NOT NULL,           -- Drools/LiteFlow expression
    action_type     VARCHAR(16) NOT NULL,   -- LOG, FLAG, ESCALATE, BLOCK
    score_value     INT NOT NULL DEFAULT 0, -- 0-100
    status          VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    version         INT NOT NULL DEFAULT 1,
    tenant_id       BIGINT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_risk_rules_category ON risk_rules(category, status);
CREATE INDEX idx_risk_rules_tenant ON risk_rules(tenant_id, status);

-- Risk assessment event log
CREATE TABLE risk_assessments (
    id              BIGSERIAL PRIMARY KEY,
    event_id        VARCHAR(64) NOT NULL UNIQUE,  -- idempotency
    player_id       BIGINT NOT NULL,
    event_type      VARCHAR(32) NOT NULL,   -- DEPOSIT, WITHDRAWAL, LOGIN, BET
    risk_score      INT NOT NULL,           -- 0-100 aggregated score
    action_taken    VARCHAR(16) NOT NULL,   -- LOG, FLAG, ESCALATE, BLOCK
    matched_rules   JSONB,                  -- array of rule_codes that matched
    event_data      JSONB,                  -- original event payload
    reviewed_by     BIGINT,                 -- admin user who reviewed (nullable)
    review_result   VARCHAR(16),            -- APPROVED, REJECTED, ESCALATED
    reviewed_at     TIMESTAMP,
    tenant_id       BIGINT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_risk_assessments_player ON risk_assessments(player_id, created_at DESC);
CREATE INDEX idx_risk_assessments_score ON risk_assessments(risk_score) WHERE risk_score >= 60;
CREATE INDEX idx_risk_assessments_review ON risk_assessments(action_taken, review_result) WHERE review_result IS NULL;
```

---

## 3. 欺詐檢測演算法（Fraud Detection Algorithm）

**狀態**: 已規劃 (Phase 5+)
**模組**: 05_Risk_Control, 01_Player_Center

### 實作目標（Implementation Goal）

實作裝置指紋識別、行為分析以及機器學習模型整合，用於欺詐檢測。

### 實作閱讀順序（Implementation Reading Order）

| 順序 | 文檔 | 章節 | 重點 |
|------|------|------|------|
| 1 | [05-02 Fraud Detection](../../source-archive/05_Risk_Control/05-02_Fraud_Detection.md) | S2 Device Fingerprint | FingerprintJS |
| 2 | [05-02 Fraud Detection](../../source-archive/05_Risk_Control/05-02_Fraud_Detection.md) | S3 Behavioral Analysis | 異常檢測 |
| 3 | [01-01 Player Lifecycle](../../source-archive/01_Player_Center/01-01_Player_Lifecycle.md) | S5 Risk Scoring | 玩家分類 |

### SmartAdmin 層級映射（SmartAdmin Layer Mapping）

| 層級 | 職責 |
|------|------|
| Controller | 指紋收集 API、欺詐報告查詢端點 |
| Service | 指紋匹配、行為模式分析 (Vavr Option 用於裝置查詢) |
| Manager | @Transactional 欺詐案件建立、@Cacheable 指紋索引、機器學習模型推理協調 |
| Dao | 裝置指紋存儲、欺詐案件記錄、行為事件日誌 |

### 裝置指紋架構（Device Fingerprint Architecture）

```
Browser/App → FingerprintJS SDK → Fingerprint API → Fingerprint Store
                                        ↓
                                  Match Service → Known Device? → Risk Score Adjustment
                                        ↓
                                  New Device → Create Record + Flag for Review
```

### 機器學習模型整合（ML Model Integration）

```java
// Manager layer: ML model inference with @Cacheable model config
@Cacheable("fraud-model-config")
public ModelConfig getActiveModelConfig() {
    return modelConfigDao.selectActiveModel();
}

// Service layer: fraud evaluation
public FraudScore evaluate(PlayerBehavior behavior) {
    ModelConfig config = fraudManager.getActiveModelConfig();
    return Option.of(mlClient.predict(behavior, config))
        .map(this::mapToFraudScore)
        .getOrElse(FraudScore.unknown());
}
```

### 驗證清單（Verification Checklist）

- [ ] 裝置指紋正確生成
- [ ] 異常行為檢測有效
- [ ] 機器學習模型準確度達標
- [ ] 誤報率在可接受範圍內

### 常見陷阱（Common Pitfalls）

1. **高誤報率**: 合法玩家被誤判 -- 調整閾值，實作人工審查佇列
2. **模型漂移**: 歷史模型效能下降 -- 透過 Snail-Job 定期重新訓練
3. **特徵不足**: 缺少風險信號 -- 持續擴展特徵工程管道

---

## 4. 代理信用管理（Agent Credit Management）

**狀態**: 已規劃 (Phase 5+)
**模組**: 07_Agent_Center, 05_Risk_Control

### 實作目標（Implementation Goal）

實作信用額度計算、風險警報、佣金結算（分成模式）以及信用凍結機制。

### 實作閱讀順序（Implementation Reading Order）

| 順序 | 文檔 | 章節 | 重點 |
|------|------|------|------|
| 1 | [07-02 Credit Network Logic](../../source-archive/07_Agent_Center/07-02_Credit_Network_Logic.md) | S2 Credit Network | 分成模式 |
| 2 | [05-04 Agent Credit Risk](../../source-archive/05_Risk_Control/05-04_Agent_Credit_Risk.md) | S3 Risk Control | 信用計算 |
| 3 | [07-03 Agent System](../../source-archive/07_Agent_Center/07-03_Agent_System.md) | S4 Risk Integration | 代理風控 |

### SmartAdmin 層級映射（SmartAdmin Layer Mapping）

| 層級 | 職責 |
|------|------|
| Controller | 信用查詢 API、結算 API、管理後台信用管理 (`@SaCheckPermission`) |
| Service | 信用計算、層級遍歷 (Vavr Option 用於可選上級代理查詢) |
| Manager | @Transactional 信用分配 + 凍結操作、@Transactional 結算處理、@Cacheable 代理層級結構 |
| Dao | 信用記錄、結算歷史、代理層級（透過 MyBatis Plus） |

### 信用計算邏輯（Credit Calculation Logic）

```java
// Service layer: credit line calculation
public CreditAllocation calculateCredit(Long agentId) {
    Agent agent = agentDao.selectById(agentId);
    return Option.of(agent)
        .map(a -> {
            BigDecimal parentCredit = getParentAvailableCredit(a.getParentId());
            BigDecimal riskFactor = calculateRiskFactor(a);
            return new CreditAllocation(
                parentCredit.multiply(riskFactor),
                a.getLevel()
            );
        })
        .getOrElse(CreditAllocation.empty());
}
```

### 信用監控閾值（Credit Monitoring Thresholds）

| 使用率 | 動作 | 實作方式 |
|-------|------|---------|
| 0-70% | 正常 | 無動作 |
| 71-85% | 警告 | 透過 Manager 非同步通知 |
| 86-95% | 限制 | 封鎖新註冊 (Manager @Transactional) |
| 96-100% | 凍結 | 凍結信用 + 升級 (Manager @Transactional) |

### 結算處理（Settlement Processing）

```java
// Manager layer: atomic settlement operation
@Transactional(rollbackFor = Throwable.class)
public void processSettlement(Long agentId, SettlementPeriod period) {
    BigDecimal commission = calculateCommission(agentId, period);
    creditDao.deductSettledAmount(agentId, commission);
    settlementDao.createRecord(agentId, commission, period);
    // Cascade settlement to sub-agents
    List<Long> subAgentIds = agentDao.selectSubAgentIds(agentId);
    subAgentIds.forEach(subId -> processSettlement(subId, period));
}
```

### 驗證清單（Verification Checklist）

- [ ] 信用額度計算正確
- [ ] 風險警報及時觸發
- [ ] 佣金結算準確
- [ ] 信用凍結機制有效

### 常見陷阱（Common Pitfalls）

1. **信用溢出**: 下級代理投注超過分配信用 -- 在 Service 接受投注前進行即時信用檢查
2. **結算延遲**: 佣金結算滯後 -- 使用 Snail-Job 排程任務自動化
3. **層級計算錯誤**: 多層級信用分配 -- 使用自動化整合測試驗證

---

## 5. 參考文檔（Reference Documents）

| 領域 | 文檔 |
|------|------|
| 風控框架 | [05-01 Risk Framework](../../source-archive/05_Risk_Control/05-01_Risk_Framework.md) |
| 欺詐檢測 | [05-02 Fraud Detection](../../source-archive/05_Risk_Control/05-02_Fraud_Detection.md) |
| 風險提案工作流 | [05-05 Risk Proposal Workflow](../../source-archive/05_Risk_Control/05-05_Risk_Proposal_Workflow.md) |
| 代理信用風險 | [05-04 Agent Credit Risk](../../source-archive/05_Risk_Control/05-04_Agent_Credit_Risk.md) |
| 信用網絡邏輯 | [07-02 Credit Network Logic](../../source-archive/07_Agent_Center/07-02_Credit_Network_Logic.md) |
| 代理系統 | [07-03 Agent System](../../source-archive/07_Agent_Center/07-03_Agent_System.md) |

---

**文檔版本**: 1.0.0
**最後更新**: 2026-02-08
**維護團隊**: iGaming 架構組
