# 詐騙偵測系統架構（Fraud Detection System Architecture）

> **業務需求**: [Fraud_Detection_Requirements.md](../../requirements/05_Risk_Compliance/Fraud_Detection_Requirements.md)
> **目標讀者**: Backend Developers, Risk Control Engineers, Data Scientists
> **最後同步**: 2026-02-09

## 文檔資訊（Document Information）

| 屬性 | 值 |
|----------|-------|
| **版本** | 1.0.0 |
| **最後更新** | 2026-02-08 |
| **規範來源** | [05-02 Fraud Detection](../../source-archive/05_Risk_Control/05-02_Fraud_Detection.md) |
| **文檔類型** | Technical Architecture |
| **目標讀者** | Architects, Backend Engineers, DevOps, SRE |
| **相關文檔** | [Fraud_Detection_Requirements.md](../../requirements/05_Risk_Compliance/Fraud_Detection_Requirements.md) |

---

## 1. 系統架構概覽（System Architecture Overview）

### 1.1 五層偵測架構（5-Layer Detection Architecture）

```mermaid
graph TD
    A[下注請求<br/>玩家下注請求] --> B{第一層: 同步黑名單檢查<br/>回應時間: <10ms}

    B -->|Redis 快取查詢| C{檢查結果}

    C -->|命中: 黑名單/凍結/IP 封鎖| D[拒絕下注<br/>回應: 明確拒絕原因]

    C -->|通過| E[第二層: 交易處理<br/>TCC Transaction]

    E --> E1[Try 階段: 凍結資源<br/>Bonus + Cash + Credit]
    E1 --> E2[Confirm 階段:<br/>扣款 + 寫入日誌]
    E2 --> E3[寫入 outbox_event]
    E3 --> E4[提交 DB Transaction]

    E4 --> F[下注成功<br/>玩家看到結果]

    F -.->|非同步事件| G[第三層: 非同步風險分析<br/>事件驅動]

    G --> G1[Kafka Consumer<br/>Topic: wallet.debited]

    G1 --> G2[風險引擎執行規則<br/>載入 t_risk_rule_config]

    G2 --> G3{決策路由<br/>基於 action_type}

    G3 -->|action_type = BLOCK| G4[建立風險提案<br/>優先級: HIGH]

    G3 -->|action_type = FLAG| G5[建立風險提案<br/>優先級: MEDIUM]

    G3 -->|action_type = IGNORE| G6[僅記錄日誌]

    G4 --> H[第四層: 人工審核<br/>手動處置]
    G5 --> H

    H --> H1[審核員檢查提案]
    H1 --> H2{審核決策}

    H2 -->|APPROVED| H3[無需操作<br/>繼續監控]
    H2 -->|REJECTED| H4[凍結帳戶<br/>標記可疑資金]
    H2 -->|PARTIAL| H5[部分凍結]

    F -.->|玩家申請提款| I[第五層: 提款延遲檢查<br/>SAGA Step 2.5]

    I --> I1[查詢歷史提案<br/>時間窗口: 30 天]
    I1 --> I2[計算可疑金額]
    I2 --> I3{決策}

    I3 -->|可疑金額 = 0| I4[繼續提款]
    I3 -->|可疑金額 > 0| I5[凍結金額<br/>路由至審核隊列]

    style A fill:#e1f5ff
    style B fill:#fff4e1
    style E fill:#e1ffe1
    style F fill:#e1ffe1
    style G fill:#f0e1ff
    style H fill:#ffe1f0
    style I fill:#e1f0ff
```

### 1.2 架構設計原則（Architecture Design Principles）

| 原則 | 實作方式 | 優勢 |
|-----------|---------------|---------|
| **最小化同步阻塞** | 第一層僅檢查硬編碼規則 | <10ms 回應時間, Fail Open |
| **下注優先完成** | TCC 確保下注成功後才進行風險分析 | 零誤殺 |
| **非同步風險分析** | Kafka + Flink 事件驅動 | 非阻塞, 5s 分析 |
| **人工審核為主** | 自動生成提案，人工決策 | 避免 ML 誤報 |
| **事後資金攔截** | 提款時檢查（30 天） | 業界最佳實踐 |

---

## 2. SmartAdmin 架構對應（SmartAdmin Architecture Mapping）

### 2.1 層級對應（Layer Mapping）

| 層級 | 類別 | 職責 |
|-------|-------|----------------|
| **Controller** | `RiskProposalController` | API 介面 |
| **Service** | `RiskProposalService` | 業務邏輯（Vavr Option） |
| **Manager** | `RiskProposalManager` | 交易管理（@Transactional） |
| **Dao** | `RiskProposalDao` | 資料存取 |
| **Entity** | `RiskProposalEntity` | 資料實體 |

### 2.2 Entity 層 - 風險提案實體（Entity Layer - Risk Proposal Entity）

```java
package net.lab1024.sa.business.risk.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import net.lab1024.sa.common.mybatis.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Risk Proposal Entity
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_risk_proposal")
public class RiskProposalEntity extends BaseEntity {

    @TableField("proposal_no")
    private String proposalNo;

    @TableField("bet_id")
    private String betId;

    @TableField("player_id")
    private Long playerId;

    @TableField("withdrawal_request_id")
    private Long withdrawalRequestId;

    @TableField("matched_rules")
    private List<String> matchedRules;

    @TableField("flagged_reasons")
    private List<String> flaggedReasons;

    @TableField("suspicious_amount")
    private BigDecimal suspiciousAmount;

    @TableField("status")
    private String status;

    @TableField("priority")
    private String priority;

    @TableField("reviewer_id")
    private Long reviewerId;

    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;

    @TableField("review_notes")
    private String reviewNotes;

    @TableField("approved_amount")
    private BigDecimal approvedAmount;

    @TableField("compensation_type")
    private String compensationType;

    @TableField("compensation_amount")
    private BigDecimal compensationAmount;

    @TableField("compensation_executed_at")
    private LocalDateTime compensationExecutedAt;
}
```

### 2.3 Dao 層 - 資料存取（Dao Layer - Data Access）

```java
package net.lab1024.sa.business.risk.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import net.lab1024.sa.business.risk.domain.entity.RiskProposalEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RiskProposalDao extends BaseMapper<RiskProposalEntity> {

    default List<RiskProposalEntity> selectByPlayerIdAndStatus(
        Long playerId,
        String status,
        LocalDateTime startDate
    ) {
        return this.selectList(new LambdaQueryWrapper<RiskProposalEntity>()
            .eq(RiskProposalEntity::getPlayerId, playerId)
            .eq(RiskProposalEntity::getStatus, status)
            .ge(RiskProposalEntity::getCreatedAt, startDate)
            .eq(RiskProposalEntity::getDeleted, false)
            .orderByDesc(RiskProposalEntity::getCreatedAt)
        );
    }

    default int countByPlayerId(Long playerId, LocalDateTime startDate) {
        return Math.toIntExact(this.selectCount(new LambdaQueryWrapper<RiskProposalEntity>()
            .eq(RiskProposalEntity::getPlayerId, playerId)
            .ge(RiskProposalEntity::getCreatedAt, startDate)
            .eq(RiskProposalEntity::getDeleted, false)
        ));
    }
}
```

### 2.4 Manager 層 - 交易管理（Manager Layer - Transaction Management）

```java
package net.lab1024.sa.business.risk.manager;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Component  // SmartAdmin Pattern: Manager uses @Component, not @Service
@RequiredArgsConstructor
public class RiskProposalManager {

    private final RiskProposalDao riskProposalDao;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional(rollbackFor = Throwable.class)
    public String createProposalWithTransaction(RiskProposalEntity entity) {
        // 1. Insert proposal record
        riskProposalDao.insert(entity);

        // 2. Send Kafka event
        kafkaTemplate.send("risk.proposal.created", RiskProposalEvent.builder()
            .proposalId(entity.getId())
            .playerId(entity.getPlayerId())
            .suspiciousAmount(entity.getSuspiciousAmount())
            .priority(entity.getPriority())
            .createdAt(entity.getCreatedAt())
            .build()
        );

        return entity.getId().toString();
    }

    @Transactional(rollbackFor = Throwable.class)
    public boolean approveProposalWithTransaction(
        Long proposalId,
        Long reviewerId,
        String reviewNotes
    ) {
        RiskProposalEntity entity = riskProposalDao.selectById(proposalId);
        if (entity == null) {
            return false;
        }

        entity.setStatus("APPROVED");
        entity.setReviewerId(reviewerId);
        entity.setReviewedAt(LocalDateTime.now());
        entity.setReviewNotes(reviewNotes);

        riskProposalDao.updateById(entity);

        kafkaTemplate.send("risk.proposal.approved", RiskProposalEvent.builder()
            .proposalId(entity.getId())
            .playerId(entity.getPlayerId())
            .reviewerId(reviewerId)
            .build()
        );

        return true;
    }
}
```

### 2.5 Service 層 - 業務邏輯（Service Layer - Business Logic with Vavr Option）

```java
package net.lab1024.sa.business.risk.service;

import io.vavr.control.Option;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RiskProposalService {

    private final RiskProposalManager riskProposalManager;
    private final RiskProposalDao riskProposalDao;
    private final PlayerDao playerDao;
    private final RedissonClient redissonClient;

    public Option<String> createProposal(RiskProposalCreateDTO createDTO) {
        return Option.of(playerDao.selectById(createDTO.getPlayerId()))
            .flatMap(player -> {
                String proposalId = riskProposalManager.createProposalWithTransaction(
                    RiskProposalEntity.builder()
                        .proposalNo(generateProposalNo())
                        .betId(createDTO.getBetId())
                        .playerId(createDTO.getPlayerId())
                        .matchedRules(createDTO.getFlaggedRules())
                        .flaggedReasons(createDTO.getFlaggedReasons())
                        .suspiciousAmount(createDTO.getSuspiciousAmount())
                        .status("PENDING_REVIEW")
                        .priority(calculatePriority(createDTO))
                        .build()
                );
                return Option.of(proposalId);
            });
    }

    public Option<List<RiskProposalVO>> findPendingProposals(Long playerId, int days) {
        String cacheKey = String.format("risk:proposal:player:%d:days:%d", playerId, days);

        RMap<String, List<RiskProposalVO>> cache = redissonClient.getMap(cacheKey);
        if (cache.isExists()) {
            return Option.of(cache.get("proposals"));
        }

        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<RiskProposalEntity> entities = riskProposalDao.selectByPlayerIdAndStatus(
            playerId, "PENDING_REVIEW", startDate
        );

        List<RiskProposalVO> vos = entities.stream()
            .map(entity -> SmartBeanUtil.copy(entity, RiskProposalVO.class))
            .collect(Collectors.toList());

        cache.put("proposals", vos);
        cache.expire(Duration.ofMinutes(5));

        return Option.of(vos);
    }

    private String calculatePriority(RiskProposalCreateDTO createDTO) {
        BigDecimal amount = createDTO.getSuspiciousAmount();
        if (amount.compareTo(new BigDecimal("10000")) > 0) return "URGENT";
        if (amount.compareTo(new BigDecimal("5000")) > 0) return "HIGH";
        if (amount.compareTo(new BigDecimal("1000")) > 0) return "MEDIUM";
        return "LOW";
    }
}
```

### 2.6 Controller 層 - API 介面（Controller Layer - API Interface）

```java
package net.lab1024.sa.business.risk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.domain.response.PageResult;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Risk Proposal Management")
@RestController
@RequestMapping("/risk/proposal")
@RequiredArgsConstructor
public class RiskProposalController {

    private final RiskProposalService riskProposalService;

    @Operation(summary = "Query player pending proposals")
    @GetMapping("/player/{playerId}/pending")
    @SaCheckPermission("risk:proposal:query")
    public ResponseDTO<List<RiskProposalVO>> queryPendingProposals(
        @PathVariable Long playerId,
        @RequestParam(defaultValue = "30") int days
    ) {
        return riskProposalService.findPendingProposals(playerId, days)
            .map(ResponseDTO::ok)
            .getOrElse(ResponseDTO.ok(Collections.emptyList()));
    }

    @Operation(summary = "Paginated query risk proposals")
    @PostMapping("/query")
    @SaCheckPermission("risk:proposal:query")
    public ResponseDTO<PageResult<RiskProposalVO>> queryProposals(
        @Valid @RequestBody RiskProposalQueryForm queryForm
    ) {
        return riskProposalService.queryProposals(queryForm)
            .map(ResponseDTO::ok)
            .getOrElse(ResponseDTO.error("Query failed"));
    }

    @Operation(summary = "Approve risk proposal")
    @PostMapping("/{proposalId}/approve")
    @SaCheckPermission("risk:proposal:approve")
    public ResponseDTO<Void> approveProposal(
        @PathVariable Long proposalId,
        @RequestParam(required = false) String reviewNotes
    ) {
        Long reviewerId = StpUtil.getLoginIdAsLong();
        boolean success = riskProposalService.approveProposal(proposalId, reviewerId, reviewNotes)
            .getOrElse(false);
        return success ? ResponseDTO.ok() : ResponseDTO.error("Approval failed");
    }
}
```

---

## 3. API 規範（API Specifications）

### 3.1 核心 API（Core APIs）

| 端點 | 方法 | 權限 | 說明 |
|----------|--------|------------|-------------|
| `/risk/proposal/player/{playerId}/pending` | GET | `risk:proposal:query` | 查詢玩家待審核提案 |
| `/risk/proposal/query` | POST | `risk:proposal:query` | 分頁查詢提案 |
| `/risk/proposal/{proposalId}/approve` | POST | `risk:proposal:approve` | 批准風險提案 |
| `/risk/multi-account/linkage/{playerId}` | GET | `risk:multi-account:query` | 查詢帳戶關聯圖 |
| `/risk/multi-account/freeze/{playerId}` | POST | `risk:multi-account:freeze` | 凍結帳戶 |
| `/risk/multi-account/merge` | POST | `risk:multi-account:merge` | 合併帳戶 |

### 3.2 請求/回應範例（Request/Response Examples）

**查詢待審核提案**:
```
GET /risk/proposal/player/{playerId}/pending?days=30
```

**回應**:
```json
{
  "code": 1,
  "msg": "ok",
  "data": [
    {
      "id": 12345,
      "proposalNo": "RP-20260202-123456",
      "betId": "BET-20260202-789",
      "playerId": 1001,
      "matchedRules": ["LOW_ODDS_WAGERING", "CROSS_MATCH_HEDGE"],
      "suspiciousAmount": 1500.00,
      "status": "PENDING_REVIEW",
      "priority": "MEDIUM",
      "createdAt": "2026-02-02T10:30:00"
    }
  ]
}
```

---

## 4. 資料庫架構（Database Schema）

### 4.1 風險提案表（Risk Proposal Table）

```sql
CREATE TABLE t_risk_proposal (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Proposal ID',
    proposal_no VARCHAR(50) NOT NULL UNIQUE COMMENT 'Proposal Number (e.g., RP-20260202-123456)',

    -- Related Information
    bet_id VARCHAR(50) NOT NULL COMMENT 'Bet ID',
    player_id BIGINT NOT NULL COMMENT 'Player ID',
    withdrawal_request_id BIGINT COMMENT 'Withdrawal Request ID (if linked)',

    -- Risk Information
    matched_rules JSON NOT NULL COMMENT 'Matched risk rule list',
    flagged_reasons JSON NOT NULL COMMENT 'Flag reason list',
    suspicious_amount DECIMAL(15, 2) NOT NULL COMMENT 'Suspicious amount',

    -- Status Management
    status VARCHAR(20) NOT NULL COMMENT 'Status (PENDING_REVIEW/APPROVED/REJECTED/PARTIAL_APPROVED)',
    priority VARCHAR(20) DEFAULT 'MEDIUM' COMMENT 'Priority (LOW/MEDIUM/HIGH/URGENT)',

    -- Review Information
    reviewer_id BIGINT COMMENT 'Reviewer ID',
    reviewed_at DATETIME COMMENT 'Review time',
    review_notes TEXT COMMENT 'Review notes',
    approved_amount DECIMAL(15, 2) COMMENT 'Approved amount (partial approval)',

    -- Compensation Information
    compensation_type VARCHAR(20) COMMENT 'Compensation type (REFUND/ADJUSTMENT/DEDUCTION)',
    compensation_amount DECIMAL(15, 2) COMMENT 'Compensation amount',
    compensation_executed_at DATETIME COMMENT 'Compensation execution time',

    -- Audit Fields
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,

    -- Index Optimization
    INDEX idx_bet_id (bet_id),
    INDEX idx_player_id_status (player_id, status, deleted),
    INDEX idx_created_at (created_at),
    INDEX idx_status_priority (status, priority, deleted),
    INDEX idx_withdrawal_request (withdrawal_request_id)
) COMMENT='Risk Proposal Table';
```

### 4.2 風險規則配置表（Risk Rule Configuration Table）

```sql
CREATE TABLE t_risk_rule_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_code VARCHAR(50) NOT NULL UNIQUE,
    rule_name VARCHAR(100) NOT NULL,
    rule_category VARCHAR(50) NOT NULL,
    action_type VARCHAR(20) NOT NULL COMMENT 'BLOCK/FLAG/IGNORE',
    game_types JSON COMMENT 'Applicable game types',
    excluded_games JSON COMMENT 'Excluded games',
    rule_params JSON COMMENT 'Rule parameters',
    enabled BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,

    INDEX idx_enabled (enabled, deleted),
    INDEX idx_category (rule_category)
) COMMENT='Risk Rule Configuration Table';
```

### 4.3 玩家風險檔案表（Player Risk Profile Table）

```sql
CREATE TABLE t_player_risk_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id BIGINT NOT NULL UNIQUE,

    -- Risk Level
    risk_level VARCHAR(20) DEFAULT 'LOW' COMMENT 'Risk level (LOW/MEDIUM/HIGH/BLACKLIST)',
    risk_score INT DEFAULT 0 COMMENT 'Risk score (0-100)',

    -- Statistics
    total_bets INT DEFAULT 0,
    total_bet_amount DECIMAL(15, 2) DEFAULT 0,
    total_win_amount DECIMAL(15, 2) DEFAULT 0,
    win_rate DECIMAL(5, 2) DEFAULT 0,

    -- Risk Flags
    flagged_count INT DEFAULT 0,
    blocked_count INT DEFAULT 0,
    last_flagged_at DATETIME,

    -- Blacklist Information
    is_blacklisted BOOLEAN DEFAULT FALSE,
    blacklist_reason VARCHAR(500),
    blacklisted_at DATETIME,
    blacklisted_by BIGINT,

    -- Audit Fields
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,

    INDEX idx_risk_level (risk_level, deleted),
    INDEX idx_blacklist (is_blacklisted, deleted),
    INDEX idx_last_flagged (last_flagged_at)
) COMMENT='Player Risk Profile Table';
```

### 4.4 帳戶關聯表（Account Linkage Tables）

```sql
-- Account Linkage Table
CREATE TABLE t_account_linkage (
    id              BIGINT PRIMARY KEY,
    player_id_a     BIGINT NOT NULL,
    player_id_b     BIGINT NOT NULL,
    link_type       VARCHAR(32) NOT NULL,  -- DEVICE, IP, BANK, PHONE
    link_value      VARCHAR(256) NOT NULL,
    weight          INT NOT NULL,
    first_seen      TIMESTAMP NOT NULL,
    last_seen       TIMESTAMP NOT NULL,
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    verified        BOOLEAN DEFAULT FALSE,
    verified_reason VARCHAR(256),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (player_id_a, player_id_b, link_type),
    INDEX idx_player_a (player_id_a),
    INDEX idx_player_b (player_id_b),
    INDEX idx_link_type_value (link_type, link_value)
);

-- Device Fingerprint Table
CREATE TABLE t_device_fingerprint (
    id                  BIGINT PRIMARY KEY,
    fingerprint_id      VARCHAR(64) NOT NULL UNIQUE,
    stable_hash         VARCHAR(64) NOT NULL,
    dynamic_hash        VARCHAR(64) NOT NULL,
    behavior_hash       VARCHAR(64),
    raw_data            JSONB NOT NULL,
    first_player_id     BIGINT NOT NULL,
    player_ids          BIGINT[] DEFAULT '{}',
    player_count        INT DEFAULT 1,
    risk_score          DECIMAL(5,2) DEFAULT 0,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_stable_hash (stable_hash),
    INDEX idx_player_count (player_count) WHERE player_count > 1
);
```

### 4.5 效能索引設計（Index Design for Performance）

```sql
-- Optimized proposal query (covering index)
CREATE INDEX idx_proposal_query ON t_risk_proposal (
    player_id,
    status,
    created_at DESC,
    deleted
) INCLUDE (id, suspicious_amount, matched_rules, priority);

-- Withdrawal association query optimization
CREATE INDEX idx_withdrawal_request ON t_risk_proposal (
    withdrawal_request_id,
    status,
    deleted
);

-- Review queue query optimization
CREATE INDEX idx_review_queue ON t_risk_proposal (
    status,
    priority DESC,
    created_at ASC,
    deleted
);

-- High-risk linkage partial index
CREATE INDEX idx_linkage_high_risk ON t_account_linkage (player_id_a, weight)
    WHERE weight >= 100;
```

---

## 5. ML 模型架構（ML Model Architecture）

### 5.1 模型效能基準（Model Performance Benchmarks）

| 模型類型 | 目的 | 準確率目標 | 延遲目標 | 狀態 |
|------------|---------|-----------------|----------------|--------|
| **異常下注偵測** | 識別可疑下注模式 | >= 95% | < 50ms | 生產環境 |
| **多帳戶偵測** | 裝置/行為聚類 | >= 90% | < 100ms | 生產環境 |
| **紅利濫用偵測** | 識別紅利濫用行為 | >= 85% | < 200ms | 測試版 |
| **AML 風險評分** | AML 風險分級 | >= 80% | < 500ms | 測試版 |
| **詐騙交易偵測** | 支付詐騙識別 | >= 92% | < 100ms | 生產環境 |

### 5.2 模型訓練流程（Model Training Pipeline）

```mermaid
flowchart LR
    subgraph 資料準備
        A[歷史資料] --> B[特徵工程]
        B --> C[資料清理]
        C --> D[訓練/驗證切分]
    end

    subgraph 模型訓練
        D --> E[模型選擇<br/>XGBoost/LightGBM]
        E --> F[超參數調優<br/>Optuna]
        F --> G[交叉驗證]
    end

    subgraph 評估部署
        G --> H[效能評估<br/>F1, AUC-ROC]
        H --> I{通過?}
        I -->|是| J[A/B 測試]
        I -->|否| E
        J --> K[生產部署]
    end
```

### 5.3 模型評估指標（Model Evaluation Metrics）

```java
public class MLModelMetrics {

    /**
     * Calculate F1 Score
     * F1 = 2 * (Precision * Recall) / (Precision + Recall)
     */
    public double calculateF1Score(ConfusionMatrix cm) {
        double precision = (double) cm.getTruePositives() /
            (cm.getTruePositives() + cm.getFalsePositives());
        double recall = (double) cm.getTruePositives() /
            (cm.getTruePositives() + cm.getFalseNegatives());

        if (precision + recall == 0) return 0;
        return 2 * (precision * recall) / (precision + recall);
    }

    /**
     * Calculate AUC-ROC
     */
    public double calculateAUCROC(List<PredictionResult> predictions) {
        predictions.sort(Comparator.comparing(PredictionResult::getScore).reversed());

        int positives = (int) predictions.stream().filter(p -> p.isActualPositive()).count();
        int negatives = predictions.size() - positives;

        if (positives == 0 || negatives == 0) return 0.5;

        double auc = 0;
        int tpCount = 0;

        for (PredictionResult pred : predictions) {
            if (pred.isActualPositive()) {
                tpCount++;
            } else {
                auc += tpCount;
            }
        }

        return auc / (positives * negatives);
    }
}
```

### 5.4 模型漂移監控（Model Drift Monitoring）

```yaml
模型漂移監控:
  資料漂移:
    - 特徵分佈變化 (KS Test, PSI)
    - 缺失值比例變化
    - 新類別值出現

  概念漂移:
    - 預測分佈變化
    - 準確率下降趨勢
    - 誤報率上升

  警報閾值:
    - PSI > 0.2: 重新評估模型
    - F1 下降 > 5%: 觸發重新訓練
    - 延遲增加 > 50%: 優化模型
```

---

## 6. 偵測流程圖（Detection Flow Diagrams）

### 6.1 多帳戶偵測流程（Multi-Account Detection Flow）

```mermaid
stateDiagram-v2
    [*] --> Detected: 系統偵測

    Detected --> Review: 權重 >= 100
    Detected --> Frozen: 權重 >= 150 或強關聯

    Review --> Verified: 確認為同住家人
    Review --> Frozen: 確認為多帳戶
    Review --> Cleared: 誤報

    Frozen --> Investigation: 人工調查
    Investigation --> Merged: 合併帳戶
    Investigation --> Terminated: 終止帳戶
    Investigation --> Unfrozen: 解凍

    Verified --> [*]
    Cleared --> [*]
    Merged --> [*]
    Terminated --> [*]
    Unfrozen --> [*]
```

### 6.2 身份農場偵測流程（Identity Farm Detection Flow）

```mermaid
stateDiagram-v2
    [*] --> DetectionSignal

    DetectionSignal --> ClusterAnalysis: 多個信號重疊
    ClusterAnalysis --> FarmIdentified: 確認帳戶群組

    FarmIdentified --> BatchFreeze: 凍結所有相關帳戶
    note right of BatchFreeze
        凍結原因: 身份農場
        保留證據快照
    end note

    BatchFreeze --> Investigation

    Investigation --> ConfirmedFraud: 證據充分
    Investigation --> PartialUnfreeze: 存在合法帳戶

    ConfirmedFraud --> BatchTerminate: 終止所有帳戶
    ConfirmedFraud --> SARReport: 涉及洗錢

    PartialUnfreeze --> [*]
    BatchTerminate --> [*]
    SARReport --> [*]
```

### 6.3 網路關聯圖（Network Correlation Graph）

```mermaid
graph LR
    subgraph 玩家 A 網路
        A1[IP: 203.0.113.1]
        A2[Device: FP-001]
        A3[Phone: +886-912-xxx]
    end

    subgraph 玩家 B 網路
        B1[IP: 203.0.113.1]
        B2[Device: FP-002]
        B3[Phone: +886-913-xxx]
    end

    subgraph 玩家 C 網路
        C1[IP: 203.0.113.50]
        C2[Device: FP-001]
        C3[Phone: +886-914-xxx]
    end

    A1 ---|相同 IP| B1
    A2 ---|相同裝置| C2

    style A1 fill:#f9f,stroke:#333
    style B1 fill:#f9f,stroke:#333
    style A2 fill:#bbf,stroke:#333
    style C2 fill:#bbf,stroke:#333
```

---

## 7. 效能優化（Performance Optimization）

### 7.1 Redis 快取策略（Redis Cache Strategy）

| 快取層級 | Key 格式 | TTL | 目的 |
|-------------|------------|-----|---------|
| **L1 - 提案列表** | `risk:proposal:player:{playerId}:days:{days}` | 5 分鐘 | 玩家待審核提案 |
| **L2 - 提案詳情** | `risk:proposal:detail:{proposalId}` | 10 分鐘 | 單一提案完整資訊 |
| **L3 - 風險檔案** | `risk:profile:player:{playerId}` | 30 分鐘 | 玩家風險檔案 |
| **L4 - 規則配置** | `risk:rule:config:game:{gameType}` | 1 小時 | 遊戲類型規則配置 |

### 7.2 查詢效能優化（Query Performance Optimization）

```java
/**
 * Parallel query optimization using Virtual Threads
 */
public class RiskProposalService {

    public CompletableFuture<CombinedResult> queryProposalsWithBets(Long playerId) {
        CompletableFuture<List<RiskProposalVO>> proposalsFuture = CompletableFuture.supplyAsync(
            () -> findPendingProposals(playerId, 30).getOrElse(Collections.emptyList()),
            virtualThreadExecutor
        );

        CompletableFuture<List<BetVO>> betsFuture = CompletableFuture.supplyAsync(
            () -> betService.findByPlayerId(playerId, 30).getOrElse(Collections.emptyList()),
            virtualThreadExecutor
        );

        return proposalsFuture.thenCombine(betsFuture, CombinedResult::new);
    }
}
```

### 7.3 效能目標（Performance Targets）

| 指標 | 目標 | 警報閾值 |
|--------|--------|-----------------|
| 提案查詢（P95） | <100ms | >200ms |
| 快取命中率 | >95% | <90% |
| Kafka 事件延遲 | <50ms | >100ms |
| DB 連線池使用率 | <50% | >80% |

---

## 8. 監控與警報（Monitoring and Alerting）

### 8.1 Prometheus 指標（Prometheus Metrics）

```yaml
# Business Metrics
risk_proposal_created_total:
  type: counter
  labels: [priority]
  description: "Total risk proposals created"

risk_proposal_pending_count:
  type: gauge
  description: "Pending review proposals count"

risk_proposal_approval_rate:
  type: gauge
  description: "Proposal approval rate (%)"

# ML Metrics
ml_model_prediction_total:
  type: counter
  labels: [model_name, model_version, prediction]
  description: "ML model prediction total"

ml_model_latency_seconds:
  type: histogram
  labels: [model_name, model_version]
  buckets: [0.01, 0.025, 0.05, 0.1, 0.25, 0.5]
  description: "ML model inference latency"

ml_model_accuracy:
  type: gauge
  labels: [model_name, model_version]
  description: "ML model accuracy (rolling 7 days)"

ml_model_drift_score:
  type: gauge
  labels: [model_name, drift_type]
  description: "Model drift score (PSI)"
```

### 8.2 AlertManager 規則（AlertManager Rules）

```yaml
groups:
  - name: risk_control_alerts
    interval: 30s
    rules:
      - alert: RiskProposalBacklogHigh
        expr: risk_proposal_pending_count > 100
        for: 10m
        labels:
          severity: warning
          team: risk
        annotations:
          summary: "Risk proposal backlog too high"
          description: "Pending proposals: {{ $value }}, exceeds threshold 100"

      - alert: RiskQueryLatencyHigh
        expr: histogram_quantile(0.95, rate(risk_query_duration_ms_bucket[5m])) > 200
        for: 5m
        labels:
          severity: warning
          team: backend
        annotations:
          summary: "Risk query P95 latency too high"
          description: "P95 latency: {{ $value }}ms, exceeds 200ms"

      - alert: RiskKafkaPublishErrors
        expr: rate(risk_kafka_event_publish_errors[5m]) > 10
        for: 5m
        labels:
          severity: critical
          team: backend
        annotations:
          summary: "Risk Kafka event publish error rate too high"
```

---

## 9. 測試策略（Testing Strategy）

### 9.1 單元測試（Unit Tests）

```java
@SpringBootTest
@RequiredArgsConstructor
class RiskProposalServiceTest {

    private final RiskProposalService riskProposalService;

    @MockBean
    private RiskProposalDao riskProposalDao;

    @Test
    void testCreateProposal_Success() {
        // Given
        RiskProposalCreateDTO createDTO = RiskProposalCreateDTO.builder()
            .betId("BET-20260202-123456")
            .playerId(1001L)
            .flaggedRules(List.of("LOW_ODDS_WAGERING", "CROSS_MATCH_HEDGE"))
            .suspiciousAmount(new BigDecimal("1500.00"))
            .build();

        when(playerDao.selectById(1001L)).thenReturn(new PlayerEntity());

        // When
        Option<String> proposalId = riskProposalService.createProposal(createDTO);

        // Then
        assertTrue(proposalId.isDefined());
        verify(riskProposalDao, times(1)).insert(any(RiskProposalEntity.class));
    }

    @Test
    void testFindPendingProposals_CacheHit() {
        // Given
        Long playerId = 1001L;
        String cacheKey = "risk:proposal:player:1001:days:30";

        RMap<String, List<RiskProposalVO>> mockCache = mock(RMap.class);
        when(redissonClient.getMap(cacheKey)).thenReturn(mockCache);
        when(mockCache.isExists()).thenReturn(true);
        when(mockCache.get("proposals")).thenReturn(Collections.emptyList());

        // When
        Option<List<RiskProposalVO>> result = riskProposalService.findPendingProposals(playerId, 30);

        // Then
        assertTrue(result.isDefined());
        verify(riskProposalDao, never()).selectByPlayerIdAndStatus(any(), any(), any());
    }
}
```

### 9.2 整合測試（Integration Tests）

```java
@SpringBootTest
@Testcontainers
@RequiredArgsConstructor
class WithdrawalDeferredRiskCheckIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("test_db")
        .withUsername("test")
        .withPassword("test");

    private final WithdrawalSagaService withdrawalSagaService;

    private final RiskProposalDao riskProposalDao;

    @Test
    void testDeferredRiskCheck_NoSuspiciousAmount_ShouldProceed() {
        WithdrawalRequest request = WithdrawalRequest.builder()
            .playerId(1001L)
            .amount(new BigDecimal("5000.00"))
            .build();

        Option<StepResult> result = withdrawalSagaService.performDeferredRiskCheck(request);

        assertTrue(result.isDefined());
        assertEquals(StepResult.Action.PROCEED, result.get().getAction());
    }

    @Test
    void testDeferredRiskCheck_WithSuspiciousAmount_ShouldFreeze() {
        WithdrawalRequest request = WithdrawalRequest.builder()
            .playerId(1001L)
            .amount(new BigDecimal("5000.00"))
            .build();

        riskProposalDao.insert(RiskProposalEntity.builder()
            .betId("BET-20260202-123456")
            .playerId(1001L)
            .suspiciousAmount(new BigDecimal("1500.00"))
            .status("PENDING_REVIEW")
            .build());

        Option<StepResult> result = withdrawalSagaService.performDeferredRiskCheck(request);

        assertTrue(result.isDefined());
        assertEquals(StepResult.Action.FREEZE, result.get().getAction());
        assertEquals(new BigDecimal("1500.00"), result.get().getFrozenAmount());
    }
}
```

---

## 相關文檔（Related Documents）

- [05-01 Risk Framework](../../source-archive/05_Risk_Control/05-01_Risk_Framework.md) - 風險系統整體架構
- [05-02-01 Detection Model](../../source-archive/05_Risk_Control/05-02-01_Detection_Model.md) - 五層架構詳細說明
- [05-02-02 Rule Configuration](../../source-archive/05_Risk_Control/05-02-02_Rule_Configuration.md) - 提案服務與延遲檢查
- [05-02-03 ML Integration](../../source-archive/05_Risk_Control/05-02-03_ML_Integration.md) - 多維度規則
- [05-02-04 Operations Tools](../../source-archive/05_Risk_Control/05-02-04_Operations_Tools.md) - 監控與警報
- [05-02-05 Multi-Account Detection](../../source-archive/05_Risk_Control/05-02-05_Multi_Account_Detection.md) - 裝置指紋識別

---

## 變更日誌（Change Log）

### v1.0.0 (2026-02-08)

**初始版本**:
- 從源文檔提取技術架構
- 系統架構概覽（五層設計）
- SmartAdmin 架構對應（Controller/Service/Manager/Dao/Entity）
- API 規範與程式碼範例
- 資料庫架構與索引優化
- ML 模型架構與訓練流程
- Mermaid 圖表（偵測流程、關聯圖）
- 效能優化策略
- 監控與警報配置
- 測試策略與程式碼範例
