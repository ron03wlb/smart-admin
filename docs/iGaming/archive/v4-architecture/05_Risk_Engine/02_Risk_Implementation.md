# 風控實作指南（Risk Implementation）

> **規範來源**: [00-14_Risk_Implementation.md](../../source-archive/00_Foundation/guides/00-14_Risk_Implementation.md)
> **目標讀者**: 架構師、後端工程師、數據工程師
> **業務需求**: [Risk_Requirements_Summary.md](../../requirements/05_Risk_Compliance/02_Risk_Requirements_Summary.md)
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

### 風險分數計算（Risk Score Calculation） — 完整實作

```java
package net.lab1024.sa.business.risk;

import io.vavr.control.Option;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 風險規則引擎服務
 * 負責規則匹配、風險評分與動作分發
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskRuleEngineService {

    private final RiskRuleDao riskRuleDao;
    private final RiskAssessmentDao riskAssessmentDao;
    private final RiskActionManager riskActionManager;
    private final DroolsRuleEngine droolsRuleEngine;

    /**
     * 評估風險事件
     *
     * @param event 風險事件（存款、提款、投注等）
     * @return 風險評分結果（0-100）
     */
    public RiskScore evaluate(RiskEvent event) {
        // 載入適用於該事件類型的規則
        List<RiskRuleEntity> rules = riskRuleDao.findActiveByCategory(event.getCategory());

        // 執行所有規則並收集結果
        List<RuleResult> results = rules.stream()
            .map(rule -> droolsRuleEngine.execute(rule, event))
            .filter(Option::isDefined)
            .map(Option::get)
            .collect(Collectors.toList());

        // 加權求和計算總風險分數
        double totalScore = results.stream()
            .mapToDouble(r -> r.getScore() * r.getWeight())
            .sum();

        // 記錄評估結果
        RiskAssessmentEntity assessment = RiskAssessmentEntity.builder()
            .eventId(event.getEventId())
            .playerId(event.getPlayerId())
            .eventType(event.getEventType())
            .riskScore((int) Math.round(totalScore))
            .matchedRules(JsonUtil.toJsonString(results.stream()
                .map(RuleResult::getRuleCode)
                .collect(Collectors.toList())))
            .eventData(JsonUtil.toJsonString(event))
            .build();

        // 根據分數範圍分發動作
        String actionTaken = dispatchAction((int) totalScore, assessment);
        assessment.setActionTaken(actionTaken);

        riskAssessmentDao.insert(assessment);

        return RiskScore.builder()
            .score((int) totalScore)
            .actionTaken(actionTaken)
            .matchedRuleCount(results.size())
            .build();
    }

    /**
     * 根據風險分數分發動作
     *
     * @param score 風險分數（0-100）
     * @param assessment 評估記錄
     * @return 採取的動作類型
     */
    private String dispatchAction(int score, RiskAssessmentEntity assessment) {
        if (score >= 70) {
            // [70, 100]: 自動拒絕 (AUTO_REJECT)
            riskActionManager.blockAccount(assessment);
            return "AUTO_REJECT";
        } else if (score >= 30) {
            // [30, 70): 人工審核 (MANUAL_REVIEW)
            riskActionManager.flagForReview(assessment);
            return "MANUAL_REVIEW";
        } else {
            // [0, 30): 自動通過 (AUTO_APPROVE)
            return "AUTO_APPROVE";
        }
    }
}
```

### 分數區間動作分發（Action Dispatch by Score）

| 分數範圍 | 動作 | 實作方式 |
|---------|------|---------|
| [0, 30) | AUTO_APPROVE | 非同步審計日誌寫入 (Manager) |
| [30, 70) | MANUAL_REVIEW | 建立風險提案，人工審核 (Manager @Transactional) |
| [70, 100] | AUTO_REJECT | 自動拒絕，立即帳戶限制 (Manager @Transactional) |

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
CREATE TABLE t_risk_rule (
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

CREATE INDEX idx_t_risk_rule_category ON t_risk_rule(category, status);
CREATE INDEX idx_t_risk_rule_tenant ON t_risk_rule(tenant_id, status);

-- Risk assessment event log
CREATE TABLE t_risk_assessment (
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

CREATE INDEX idx_t_risk_assessment_player ON t_risk_assessment(player_id, created_at DESC);
CREATE INDEX idx_t_risk_assessment_score ON t_risk_assessment(risk_score) WHERE risk_score >= 60;
CREATE INDEX idx_t_risk_assessment_review ON t_risk_assessment(action_taken, review_result) WHERE review_result IS NULL;
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

### 風險動作管理器（Risk Action Manager）

```java
package net.lab1024.sa.business.risk;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 風險動作管理器
 * 負責執行風險評估後的處置操作（封鎖、標記、升級）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RiskActionManager {

    private final PlayerDao playerDao;
    private final RiskProposalDao riskProposalDao;
    private final NotificationService notificationService;

    /**
     * 封鎖玩家帳戶（86-100 分）
     */
    @Transactional(rollbackFor = Throwable.class)
    public void blockAccount(RiskAssessmentEntity assessment) {
        playerDao.updateAccountStatus(assessment.getPlayerId(), "BLOCKED");

        // 建立高優先級風險提案
        RiskProposalEntity proposal = RiskProposalEntity.builder()
            .playerId(assessment.getPlayerId())
            .assessmentId(assessment.getId())
            .priority("HIGH")
            .proposalType("AUTO_BLOCK")
            .reason("自動封鎖：風險分數 >= 86")
            .riskScore(assessment.getRiskScore())
            .status("PENDING")
            .build();

        riskProposalDao.insert(proposal);

        log.warn("Account blocked due to high risk score: playerId={}, score={}",
            assessment.getPlayerId(), assessment.getRiskScore());
    }

    /**
     * 升級至風控團隊（61-85 分）
     */
    @Transactional(rollbackFor = Throwable.class)
    public void escalateToTeam(RiskAssessmentEntity assessment) {
        RiskProposalEntity proposal = RiskProposalEntity.builder()
            .playerId(assessment.getPlayerId())
            .assessmentId(assessment.getId())
            .priority("MEDIUM")
            .proposalType("ESCALATE")
            .reason("升級審查：風險分數 61-85")
            .riskScore(assessment.getRiskScore())
            .status("PENDING")
            .build();

        riskProposalDao.insert(proposal);

        // 通知風控團隊
        notificationService.notifyRiskTeam("風險升級", assessment);

        log.info("Risk escalated to team: playerId={}, score={}",
            assessment.getPlayerId(), assessment.getRiskScore());
    }

    /**
     * 標記審查（31-60 分）
     */
    @Transactional(rollbackFor = Throwable.class)
    public void flagForReview(RiskAssessmentEntity assessment) {
        RiskProposalEntity proposal = RiskProposalEntity.builder()
            .playerId(assessment.getPlayerId())
            .assessmentId(assessment.getId())
            .priority("LOW")
            .proposalType("FLAG")
            .reason("標記審查：風險分數 31-60")
            .riskScore(assessment.getRiskScore())
            .status("PENDING")
            .build();

        riskProposalDao.insert(proposal);

        log.info("Player flagged for review: playerId={}, score={}",
            assessment.getPlayerId(), assessment.getRiskScore());
    }

    /**
     * 熱重載規則快取
     */
    @CacheEvict(value = "riskRules", allEntries = true)
    @Transactional(rollbackFor = Throwable.class)
    public void reloadRuleCache() {
        log.info("Risk rule cache reloaded");
    }
}
```

---

### 機器學習模型整合（ML Model Integration） — 完整實作

```java
package net.lab1024.sa.business.fraud;

import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * 欺詐檢測服務
 * 負責裝置指紋匹配、行為分析與 ML 模型推理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final DeviceFingerprintDao deviceFingerprintDao;
    private final FraudManager fraudManager;
    private final MLClient mlClient;

    /**
     * 評估玩家行為的欺詐風險
     *
     * @param behavior 玩家行為數據
     * @return 欺詐分數（0-1.0）
     */
    public FraudScore evaluate(PlayerBehavior behavior) {
        // 獲取當前活躍的 ML 模型配置
        ModelConfig config = fraudManager.getActiveModelConfig();

        // 執行 ML 模型推理
        return Try.of(() -> mlClient.predict(behavior, config))
            .map(this::mapToFraudScore)
            .onFailure(e -> log.error("ML model prediction failed: playerId={}", behavior.getPlayerId(), e))
            .toOption()
            .getOrElse(FraudScore.unknown());
    }

    /**
     * 檢查裝置指紋是否為已知裝置
     *
     * @param fingerprintId 裝置指紋 ID
     * @param playerId 玩家 ID
     * @return 裝置檢查結果
     */
    public Option<DeviceFingerprintVO> checkDeviceFingerprint(String fingerprintId, Long playerId) {
        return deviceFingerprintDao.findByFingerprint(fingerprintId)
            .filter(device -> device.getPlayerId().equals(playerId));
    }

    /**
     * 註冊新裝置指紋
     */
    public void registerNewDevice(String fingerprintId, Long playerId, String deviceInfo) {
        DeviceFingerprintEntity entity = DeviceFingerprintEntity.builder()
            .fingerprintId(fingerprintId)
            .playerId(playerId)
            .deviceInfo(deviceInfo)
            .firstSeen(LocalDateTime.now())
            .lastSeen(LocalDateTime.now())
            .riskLevel("UNKNOWN")
            .build();

        deviceFingerprintDao.insert(entity);

        // 標記為待審查（新裝置風險）
        fraudManager.flagNewDeviceForReview(entity);
    }

    private FraudScore mapToFraudScore(MLPredictionResult result) {
        return FraudScore.builder()
            .score(result.getProbability())
            .confidence(result.getConfidence())
            .modelVersion(result.getModelVersion())
            .build();
    }
}

/**
 * 欺詐管理器
 * 負責 ML 模型配置管理與新裝置審查
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FraudManager {

    private final ModelConfigDao modelConfigDao;
    private final DeviceFingerprintDao deviceFingerprintDao;
    private final RiskProposalDao riskProposalDao;

    /**
     * 獲取當前活躍的 ML 模型配置（帶快取）
     */
    @Cacheable(value = "fraud-model-config", unless = "#result == null")
    public ModelConfig getActiveModelConfig() {
        return modelConfigDao.selectActiveModel()
            .getOrElseThrow(() -> new RuntimeException("No active ML model configured"));
    }

    /**
     * 標記新裝置待審查
     */
    @Transactional(rollbackFor = Throwable.class)
    public void flagNewDeviceForReview(DeviceFingerprintEntity device) {
        RiskProposalEntity proposal = RiskProposalEntity.builder()
            .playerId(device.getPlayerId())
            .priority("LOW")
            .proposalType("NEW_DEVICE")
            .reason("新裝置指紋檢測")
            .status("PENDING")
            .build();

        riskProposalDao.insert(proposal);

        log.info("New device flagged for review: playerId={}, fingerprintId={}",
            device.getPlayerId(), device.getFingerprintId());
    }
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

### 信用計算邏輯（Credit Calculation Logic） — 完整實作

```java
package net.lab1024.sa.business.agent;

import io.vavr.control.Option;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 代理信用服務
 * 負責信用額度計算、使用率監控與層級遍歷
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentCreditService {

    private final AgentDao agentDao;
    private final AgentCreditDao agentCreditDao;
    private final AgentCreditManager agentCreditManager;

    /**
     * 計算代理信用額度
     *
     * @param agentId 代理 ID
     * @return 信用分配結果
     */
    public CreditAllocation calculateCredit(Long agentId) {
        return agentDao.selectById(agentId)
            .map(agent -> {
                // 獲取上級可用信用
                BigDecimal parentCredit = getParentAvailableCredit(agent.getParentId());

                // 計算風險係數（基於歷史表現）
                BigDecimal riskFactor = calculateRiskFactor(agent);

                // 信用額度 = 上級可用信用 × 風險係數
                BigDecimal allocatedCredit = parentCredit.multiply(riskFactor);

                return CreditAllocation.builder()
                    .agentId(agentId)
                    .allocatedCredit(allocatedCredit)
                    .agentLevel(agent.getLevel())
                    .riskFactor(riskFactor)
                    .build();
            })
            .getOrElse(CreditAllocation.empty());
    }

    /**
     * 檢查信用使用率並觸發警報
     *
     * @param agentId 代理 ID
     * @return 信用使用率（0-100）
     */
    public BigDecimal checkCreditUtilization(Long agentId) {
        return agentCreditDao.findByAgentId(agentId)
            .map(credit -> {
                BigDecimal utilizationRate = credit.getUsedCredit()
                    .divide(credit.getTotalCredit(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

                // 根據使用率觸發動作
                triggerActionByUtilization(agentId, utilizationRate);

                return utilizationRate;
            })
            .getOrElse(BigDecimal.ZERO);
    }

    /**
     * 獲取上級可用信用
     */
    private BigDecimal getParentAvailableCredit(Long parentId) {
        if (parentId == null) {
            return BigDecimal.valueOf(1000000); // 根代理預設信用
        }

        return agentCreditDao.findByAgentId(parentId)
            .map(credit -> credit.getTotalCredit().subtract(credit.getUsedCredit()))
            .getOrElse(BigDecimal.ZERO);
    }

    /**
     * 計算風險係數（0.5-1.0，越高表示信用越好）
     */
    private BigDecimal calculateRiskFactor(AgentEntity agent) {
        // 基於歷史結算記錄、逾期次數、代理層級等計算
        int overdueCount = agentCreditDao.countOverdueSettlements(agent.getId());
        BigDecimal baseRiskFactor = BigDecimal.valueOf(1.0);

        // 每次逾期降低 10%
        BigDecimal penalty = BigDecimal.valueOf(0.1).multiply(BigDecimal.valueOf(overdueCount));
        return baseRiskFactor.subtract(penalty).max(BigDecimal.valueOf(0.5));
    }

    /**
     * 根據信用使用率觸發動作
     */
    private void triggerActionByUtilization(Long agentId, BigDecimal utilizationRate) {
        if (utilizationRate.compareTo(BigDecimal.valueOf(96)) >= 0) {
            // 96-100%: 凍結信用
            agentCreditManager.freezeCredit(agentId, "使用率超過 96%");
        } else if (utilizationRate.compareTo(BigDecimal.valueOf(86)) >= 0) {
            // 86-95%: 限制新註冊
            agentCreditManager.restrictNewRegistrations(agentId);
        } else if (utilizationRate.compareTo(BigDecimal.valueOf(71)) >= 0) {
            // 71-85%: 警告
            agentCreditManager.sendCreditWarning(agentId, utilizationRate);
        }
    }
}
```

### 信用監控閾值（Credit Monitoring Thresholds）

| 使用率 | 動作 | 實作方式 |
|-------|------|---------|
| 0-70% | 正常 | 無動作 |
| 71-85% | 警告 | 透過 Manager 非同步通知 |
| 86-95% | 限制 | 封鎖新註冊 (Manager @Transactional) |
| 96-100% | 凍結 | 凍結信用 + 升級 (Manager @Transactional) |

### 結算處理（Settlement Processing） — 完整實作

```java
package net.lab1024.sa.business.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 代理信用管理器
 * 負責信用分配、凍結操作與結算處理
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentCreditManager {

    private final AgentDao agentDao;
    private final AgentCreditDao agentCreditDao;
    private final SettlementDao settlementDao;
    private final NotificationService notificationService;

    /**
     * 處理代理結算（原子性操作，支援多層級級聯）
     *
     * @param agentId 代理 ID
     * @param period 結算週期
     */
    @Transactional(rollbackFor = Throwable.class)
    public void processSettlement(Long agentId, SettlementPeriod period) {
        // 計算佣金
        BigDecimal commission = calculateCommission(agentId, period);

        // 扣除已結算金額
        agentCreditDao.deductSettledAmount(agentId, commission);

        // 建立結算記錄
        SettlementEntity settlement = SettlementEntity.builder()
            .agentId(agentId)
            .period(period.toString())
            .commission(commission)
            .settlementDate(LocalDate.now())
            .status("COMPLETED")
            .build();

        settlementDao.insert(settlement);

        // 級聯結算下級代理
        List<Long> subAgentIds = agentDao.selectSubAgentIds(agentId);
        subAgentIds.forEach(subId -> processSettlement(subId, period));

        // 清除代理層級結構快取
        evictAgentHierarchyCache();

        log.info("Settlement processed: agentId={}, period={}, commission={}",
            agentId, period, commission);
    }

    /**
     * 凍結代理信用
     *
     * @param agentId 代理 ID
     * @param reason 凍結原因
     */
    @Transactional(rollbackFor = Throwable.class)
    public void freezeCredit(Long agentId, String reason) {
        agentCreditDao.updateFreezeStatus(agentId, true);

        CreditFreezeLogEntity freezeLog = CreditFreezeLogEntity.builder()
            .agentId(agentId)
            .reason(reason)
            .frozenAt(LocalDateTime.now())
            .build();

        creditFreezeLogDao.insert(freezeLog);

        // 通知代理與風控團隊
        notificationService.notifyAgent(agentId, "信用已凍結", reason);
        notificationService.notifyRiskTeam("代理信用凍結", agentId);

        log.warn("Agent credit frozen: agentId={}, reason={}", agentId, reason);
    }

    /**
     * 限制新玩家註冊
     */
    @Transactional(rollbackFor = Throwable.class)
    public void restrictNewRegistrations(Long agentId) {
        agentDao.updateRegistrationStatus(agentId, "RESTRICTED");

        log.info("New registrations restricted for agent: agentId={}", agentId);
    }

    /**
     * 發送信用警告通知
     */
    public void sendCreditWarning(Long agentId, BigDecimal utilizationRate) {
        String message = String.format("信用使用率達 %.2f%%，請注意控制", utilizationRate);
        notificationService.notifyAgent(agentId, "信用警告", message);

        log.info("Credit warning sent: agentId={}, utilization={}%", agentId, utilizationRate);
    }

    /**
     * 獲取代理層級結構（帶快取）
     */
    @Cacheable(value = "agentHierarchy", key = "#agentId")
    public AgentHierarchy getAgentHierarchy(Long agentId) {
        return agentDao.selectHierarchy(agentId)
            .getOrElseThrow(() -> new RuntimeException("Agent not found: " + agentId));
    }

    /**
     * 清除代理層級結構快取
     */
    @CacheEvict(value = "agentHierarchy", allEntries = true)
    public void evictAgentHierarchyCache() {
        log.info("Agent hierarchy cache evicted");
    }

    /**
     * 計算代理佣金（基於分成模式）
     */
    private BigDecimal calculateCommission(Long agentId, SettlementPeriod period) {
        // 查詢該代理在結算週期內的投注數據
        BigDecimal totalBetAmount = settlementDao.sumBetAmountByPeriod(agentId, period);

        // 查詢代理佣金比例
        BigDecimal commissionRate = agentDao.selectById(agentId)
            .map(AgentEntity::getCommissionRate)
            .getOrElse(BigDecimal.valueOf(0.05)); // 預設 5%

        return totalBetAmount.multiply(commissionRate);
    }
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
