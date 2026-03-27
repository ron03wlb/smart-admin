# V23 提款審核工作流評估報告 (Withdrawal Approval Workflow Evaluation)

**評估日期**: 2026-03-10
**版本**: 1.0.0
**評估者**: SmartAdmin iGaming Team
**狀態**: ✅ **建議實施獨立 t_withdrawal_approval 表**

---

## 📋 執行摘要 (Executive Summary)

### 評估結論

經過對現有支付模塊架構、業務需求文檔和行業合規要求的全面分析，**強烈建議創建獨立的 `t_withdrawal_approval` 表**來支持提款審核工作流。

### 關鍵決策理由

| 維度 | 現有 t_payment_order 能力 | V23 需求 | 差距評估 |
|------|-------------------------|---------|---------|
| **訂單狀態** | 7 種狀態（PENDING → SUCCESS） | ✅ 足夠 | 無差距 |
| **多級審批鏈** | ❌ 無支持 | L1/L2/L3 三級審批 | 🔴 嚴重差距 |
| **風險評分整合** | ❌ 無字段 | 0-100 分數 + 路由規則 | 🔴 嚴重差距 |
| **KYC/AML 整合** | ❌ 無連結 | 身份驗證 + 制裁篩查 | 🔴 嚴重差距 |
| **SAGA 狀態追蹤** | ❌ 無補償機制 | 補償流程 + 重試策略 | 🔴 嚴重差距 |
| **審計追蹤** | 僅訂單級 | 審批歷史 + 決策理由 | 🟡 中度差距 |

---

## 🔍 現有架構分析 (Current Architecture Analysis)

### V9 Payment Tables (現有實現)

**t_payment_order 核心字段**:
```sql
CREATE TABLE t_payment_order (
    payment_order_id    BIGSERIAL       PRIMARY KEY,
    order_no            VARCHAR(64)     NOT NULL,
    player_id           BIGINT          NOT NULL,
    wallet_id           BIGINT          NOT NULL,
    order_type          SMALLINT        NOT NULL,  -- 1=DEPOSIT, 2=WITHDRAWAL
    amount              DECIMAL(19,4)   NOT NULL,
    currency_code       VARCHAR(10)     NOT NULL DEFAULT 'USD',
    status              SMALLINT        NOT NULL DEFAULT 1,  -- 7 states
    psp_code            VARCHAR(30)     NOT NULL,
    psp_transaction_id  VARCHAR(128),
    request_id          VARCHAR(64)     NOT NULL,  -- Idempotency key
    version             INT             NOT NULL DEFAULT 0,  -- Optimistic lock
    -- V16 新增
    reconciliation_status SMALLINT      NOT NULL DEFAULT 1,
    ...
);
```

**PaymentService.createWithdrawal() 現有流程**:
```java
// Line 204-309 (105 lines)
public ResponseDTO<PaymentOrderVO> createWithdrawal(WithdrawRequestForm form) {
    // 1. 冪等性檢查 (requestId)
    // 2. 加載錢包並檢查可用餘額
    // 3. 鎖定提款金額 (WalletLockEntity)
    // 4. 解析 PSP 配置
    // 5. 驗證金額範圍 (minWithdrawal, maxWithdrawal)
    // 6. 創建訂單 (PENDING 狀態)
    // 7. 調用 PSP Adapter 發起提款
    // 8. 更新訂單為 PROCESSING
}
```

**現有實現的優點**:
- ✅ 完整的冪等性保護（Request ID + 唯一索引）
- ✅ 三層防護機制（Redisson 鎖 + 樂觀鎖 + Request ID）
- ✅ 資金鎖定機制（WalletLockEntity）
- ✅ PSP 適配器架構（Strategy Pattern）
- ✅ 對帳系統集成（reconciliation_status）

**現有實現的限制**:
- ❌ 無人工審批機制（直接調用 PSP，無審核隊列）
- ❌ 無 KYC 驗證整合（未檢查 player.kycLevel）
- ❌ 無風險評分邏輯（未調用風控服務）
- ❌ 無多級審批流程（無審批員概念）
- ❌ 無 SAGA 補償邏輯（僅基礎錯誤處理）

---

## 📖 業務需求分析 (Business Requirements Analysis)

### 全球合規要求 (Global Regulatory Requirements)

**英國 UKGC (Gambling Commission)**:
- **自動處理率**: 96.3% 的提款必須自動處理
- **處理時效**: 僅 0.1% 可超過 48 小時
- **身份驗證**: 禁止在提款時才要求 KYC（必須在投注前完成）
- **財務風險檢查**: 月提款 £150 觸發強化盡職調查

**馬耳他 MGA (Malta Gaming Authority)**:
- **強化盡職調查門檻**: €2,000 累計提款
- **AML 合規**: 必須篩查 PEP (Politically Exposed Persons) 和制裁名單

**菲律賓 PAGCOR**:
- **KYC 時限**: 3 天內完成身份驗證
- **反洗錢申報**: 單日 PHP 500 萬（約 USD 89,000）

**巴西新法規 (Lei 14.790/2023)**:
- **處理時限**: 120 分鐘內完成提款
- **支付方式**: PIX 即時支付佔 96%，禁止信用卡和加密貨幣

### 提款審核工作流需求 (Withdrawal Approval Workflow Requirements)

**多級審批路由規則**:
```
┌──────────────────────────────────────────────────────────────┐
│ 風險分數 (Risk Score)    │ 審批路由 (Approval Route)       │
├──────────────────────────────────────────────────────────────┤
│ 0-30   (低風險, <$1000)  │ ✅ 自動審批 (Auto-Approve)      │
│ 31-50  (中風險)          │ ⚠️ L1 人工審核 (Junior Analyst) │
│ 51-70  (中高風險)        │ ⚠️ L1 + L2 審核 (Senior Analyst)│
│ 71-100 (高風險)          │ 🔴 L1 + L2 + L3 (Manager)       │
└──────────────────────────────────────────────────────────────┘
```

**SAGA 工作流步驟** (從 01-05_Withdrawal_Risk.md):
1. **Step 1**: 風險評估服務（規則引擎 + ML 模型 <100ms）
2. **Step 2**: KYC/AML 驗證服務（身份驗證 + PEP 篩查 + 可疑活動標記）
3. **Step 2.5**: 延遲風控檢查（Risk Proposal 整合，v2.1.0 新增）
4. **Step 3**: 審批路由決策（基於風險分數的多級審批）
5. **Step 4**: 支付執行（PSP 調用 + 狀態追蹤）

**補償事務矩陣**:
| 失敗步驟 | 補償動作 | 重試策略 | 最終狀態 |
|---------|---------|---------|---------|
| Step 1: 風險評估 | 釋放鎖定資金 | 指數退避 (3次) | REJECTED / FAILED |
| Step 2: KYC驗證 | 保留資金 + 轉待驗證 | 無需重試（等待玩家補件） | PENDING_VERIFICATION |
| Step 2.5: 延遲風控 | 凍結可疑金額 + 生成審核提案 | 無需重試（轉人工審核） | PENDING_MANUAL_REVIEW |
| Step 3: 審批路由 | 轉備用審批員 | 自動路由 | PENDING_APPROVAL |
| Step 4: 支付執行 | 切換備用通道 | 冪等重試（無限次） | PAYMENT_FAILED → REFUNDED |

---

## 🎯 方案決策 (Solution Decision)

### 方案對比

#### 方案 A: 擴展 t_payment_order（不推薦）

**實施方式**:
```sql
ALTER TABLE t_payment_order ADD COLUMN approval_status SMALLINT;
ALTER TABLE t_payment_order ADD COLUMN approver_id BIGINT;
ALTER TABLE t_payment_order ADD COLUMN approval_time TIMESTAMPTZ;
ALTER TABLE t_payment_order ADD COLUMN rejection_reason VARCHAR(500);
ALTER TABLE t_payment_order ADD COLUMN risk_score INT;
ALTER TABLE t_payment_order ADD COLUMN kyc_check_result SMALLINT;
ALTER TABLE t_payment_order ADD COLUMN aml_check_result SMALLINT;
```

**優點**:
- ✅ 實施簡單（僅添加字段）
- ✅ 無需額外 JOIN 查詢
- ✅ 減少一張表的維護成本

**缺點**:
- ❌ 無法支持多級審批鏈（L1 → L2 → L3）
- ❌ 審批歷史無法追溯（覆蓋式更新）
- ❌ 責任分離混亂（訂單生命週期 vs 審批生命週期）
- ❌ 無法處理並行審批（多個審批員同時審核）
- ❌ 表字段過度膨脹（違反單一職責原則）
- ❌ 難以擴展（未來可能需要審批委派、審批撤回等功能）

#### 方案 B: 創建 t_withdrawal_approval（✅ 推薦）

**實施方式**:
```sql
CREATE TABLE t_withdrawal_approval (
    approval_id         BIGSERIAL       PRIMARY KEY,
    payment_order_id    BIGINT          NOT NULL REFERENCES t_payment_order(payment_order_id),
    approval_stage      SMALLINT        NOT NULL,  -- 1=L1, 2=L2, 3=L3
    approval_status     SMALLINT        NOT NULL,  -- PENDING, APPROVED, REJECTED
    approver_id         BIGINT,
    assigned_at         TIMESTAMPTZ,
    reviewed_at         TIMESTAMPTZ,
    approval_notes      TEXT,
    rejection_reason    VARCHAR(500),
    risk_score          INT,
    kyc_check_result    SMALLINT,
    aml_check_result    SMALLINT,
    saga_state          VARCHAR(50),
    retry_count         INT             DEFAULT 0,
    ...
);
```

**優點**:
- ✅ 支持多級審批鏈（每級一條記錄）
- ✅ 完整審批歷史追溯（不可變記錄）
- ✅ 清晰責任分離（訂單表 vs 審批表）
- ✅ 靈活擴展性（審批委派、批量審批、審批模板）
- ✅ 符合 SAGA 模式（每步驟的狀態追蹤）
- ✅ 符合審計合規要求（完整 WHO/WHEN/WHY 記錄）

**缺點**:
- ⚠️ 需要額外 JOIN 查詢（性能可通過索引優化）
- ⚠️ 增加一張表的維護成本（可接受）

### 最終決策：✅ 方案 B (創建 t_withdrawal_approval)

**決策理由**:
1. **合規要求**: 全球監管機構（UKGC, MGA, PAGCOR）均要求完整的審批歷史追溯
2. **業務複雜度**: 多級審批、SAGA 補償流程、Risk Proposal 整合均需專用表支持
3. **架構原則**: 符合單一職責原則（SRP）和關注點分離（Separation of Concerns）
4. **長期可維護性**: 未來功能擴展（審批模板、批量審批、審批委派）更易實現

---

## 📐 V23 數據庫遷移設計 (V23 Database Migration Design)

### 表結構設計

**t_withdrawal_approval (提款審批記錄)**:
```sql
CREATE TABLE IF NOT EXISTS t_withdrawal_approval (
    approval_id             BIGSERIAL       PRIMARY KEY,
    payment_order_id        BIGINT          NOT NULL REFERENCES t_payment_order(payment_order_id),
    tenant_id               BIGINT          NOT NULL,

    -- 審批階段與狀態
    approval_stage          SMALLINT        NOT NULL,  -- 1=L1, 2=L2, 3=L3, 4=AUTO
    approval_status         SMALLINT        NOT NULL DEFAULT 1,  -- 1=PENDING, 2=APPROVED, 3=REJECTED, 4=TIMEOUT, 5=DELEGATED

    -- 審批員信息
    approver_id             BIGINT,
    assigned_at             TIMESTAMPTZ,
    reviewed_at             TIMESTAMPTZ,
    approval_notes          TEXT,
    rejection_reason        VARCHAR(500),

    -- 風險評估結果
    risk_score              INT,  -- 0-100
    risk_category           VARCHAR(20),  -- LOW, MEDIUM, MEDIUM_HIGH, HIGH
    routing_reason          VARCHAR(200),  -- 路由到此審批級別的原因

    -- KYC/AML 檢查結果
    kyc_status              SMALLINT,  -- KycVerificationStatusEnum
    kyc_level               SMALLINT,  -- KycLevelEnum
    aml_check_result        VARCHAR(50),  -- PASS, SUSPICIOUS, BLOCKED
    sanctions_screening     VARCHAR(50),  -- CLEAR, MATCH, PENDING
    pep_check_result        VARCHAR(50),  -- NOT_PEP, PEP_DOMESTIC, PEP_FOREIGN

    -- SAGA 工作流狀態
    saga_state              VARCHAR(50),  -- RISK_ASSESSMENT, KYC_VERIFICATION, APPROVAL_ROUTING, PAYMENT_EXECUTION
    compensation_state      VARCHAR(50),  -- NONE, COMPENSATING, COMPENSATED, COMPENSATION_FAILED
    retry_count             INT             NOT NULL DEFAULT 0,
    last_retry_at           TIMESTAMPTZ,

    -- SLA 追蹤
    sla_deadline            TIMESTAMPTZ,  -- 24h auto-approve/reject deadline
    sla_breached            BOOLEAN         NOT NULL DEFAULT FALSE,

    -- 審批決策詳細信息
    approval_metadata       JSONB,  -- 擴展字段（存儲設備指紋、IP、風險模型版本等）

    deleted                 BOOLEAN         NOT NULL DEFAULT FALSE,
    create_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_approval_order_stage UNIQUE (payment_order_id, approval_stage)
);

COMMENT ON TABLE t_withdrawal_approval IS '提款審批記錄 (支持多級審批鏈)';
COMMENT ON COLUMN t_withdrawal_approval.approval_id IS '審批記錄唯一標識';
COMMENT ON COLUMN t_withdrawal_approval.payment_order_id IS '關聯支付訂單 ID';
COMMENT ON COLUMN t_withdrawal_approval.approval_stage IS '審批階段: 1=L1, 2=L2, 3=L3, 4=AUTO';
COMMENT ON COLUMN t_withdrawal_approval.approval_status IS '審批狀態: 1=PENDING, 2=APPROVED, 3=REJECTED, 4=TIMEOUT, 5=DELEGATED';
COMMENT ON COLUMN t_withdrawal_approval.risk_score IS '風險評分 (0-100)';
COMMENT ON COLUMN t_withdrawal_approval.saga_state IS 'SAGA 工作流當前狀態';
COMMENT ON COLUMN t_withdrawal_approval.compensation_state IS '補償事務狀態';
COMMENT ON COLUMN t_withdrawal_approval.sla_deadline IS 'SLA 截止時間（24h 自動決策）';

CREATE INDEX IF NOT EXISTS idx_withdrawal_approval_order ON t_withdrawal_approval (payment_order_id);
CREATE INDEX IF NOT EXISTS idx_withdrawal_approval_status ON t_withdrawal_approval (approval_status, assigned_at);
CREATE INDEX IF NOT EXISTS idx_withdrawal_approval_approver ON t_withdrawal_approval (approver_id, approval_status);
CREATE INDEX IF NOT EXISTS idx_withdrawal_approval_tenant ON t_withdrawal_approval (tenant_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_withdrawal_approval_sla ON t_withdrawal_approval (sla_deadline) WHERE sla_breached = FALSE;
```

**t_approval_delegation (審批委派記錄，可選擴展)**:
```sql
CREATE TABLE IF NOT EXISTS t_approval_delegation (
    delegation_id       BIGSERIAL       PRIMARY KEY,
    approval_id         BIGINT          NOT NULL REFERENCES t_withdrawal_approval(approval_id),
    from_approver_id    BIGINT          NOT NULL,
    to_approver_id      BIGINT          NOT NULL,
    delegation_reason   VARCHAR(200),
    delegated_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    tenant_id           BIGINT          NOT NULL,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE t_approval_delegation IS '審批委派記錄（審批員臨時授權）';
```

### 枚舉類定義

**WithdrawalApprovalStageEnum.java**:
```java
public enum WithdrawalApprovalStageEnum implements BaseEnum {
    L1_JUNIOR_ANALYST(1, "L1 初級分析師"),
    L2_SENIOR_ANALYST(2, "L2 高級分析師"),
    L3_MANAGER(3, "L3 經理審批"),
    AUTO_APPROVED(4, "系統自動審批"),
    ;

    private final Integer value;
    private final String desc;
}
```

**WithdrawalApprovalStatusEnum.java**:
```java
public enum WithdrawalApprovalStatusEnum implements BaseEnum {
    PENDING(1, "待審批"),
    APPROVED(2, "已批准"),
    REJECTED(3, "已拒絕"),
    TIMEOUT(4, "超時自動決策"),
    DELEGATED(5, "已委派"),
    ;

    private final Integer value;
    private final String desc;
}
```

**SagaStateEnum.java**:
```java
public enum SagaStateEnum implements BaseEnum {
    RISK_ASSESSMENT(1, "風險評估中"),
    KYC_VERIFICATION(2, "KYC驗證中"),
    RISK_PROPOSAL_CHECK(3, "延遲風控檢查"),
    APPROVAL_ROUTING(4, "審批路由中"),
    PAYMENT_EXECUTION(5, "支付執行中"),
    COMPLETED(6, "已完成"),
    COMPENSATING(7, "補償中"),
    COMPENSATED(8, "已補償"),
    COMPENSATION_FAILED(9, "補償失敗"),
    ;

    private final Integer value;
    private final String desc;
}
```

---

## 🚀 實施計劃 (Implementation Plan)

### Phase 1: 數據庫遷移 (Week 1)

**交付物**:
- ✅ `V23__withdrawal_approval_workflow.sql` 遷移腳本
- ✅ 枚舉類定義（4 個 Enum 類）
- ✅ Entity 定義（2 個 Entity 類）

**驗收標準**:
- Flyway 遷移成功執行
- 所有索引創建成功
- RLS 策略正確應用

### Phase 2: Manager & Service 實現 (Week 1-2)

**交付物**:
- ✅ `WithdrawalApprovalManager.java` - 審批事務管理
- ✅ `WithdrawalApprovalService.java` - 業務邏輯層
- ✅ `WithdrawalApprovalDao.java` - 數據訪問層
- ✅ 集成 PaymentService.createWithdrawal() 調用風控服務

**關鍵方法**:
```java
// WithdrawalApprovalManager.java
@Transactional(rollbackFor = Throwable.class)
public Try<Void> createApprovalRecord(
    Long paymentOrderId,
    Integer riskScore,
    WithdrawalApprovalStageEnum stage
);

@Transactional(rollbackFor = Throwable.class)
public Try<Void> approveWithdrawal(Long approvalId, Long approverId, String notes);

@Transactional(rollbackFor = Throwable.class)
public Try<Void> rejectWithdrawal(Long approvalId, Long approverId, String reason);
```

### Phase 3: SAGA 工作流整合 (Week 2-3)

**交付物**:
- ✅ SAGA 狀態機實現（使用 Spring State Machine 或 Temporal）
- ✅ 補償事務邏輯（每個 Step 的補償處理器）
- ✅ Kafka 事件發布/訂閱（Step 間異步通訊）

**SAGA 流程示例**:
```java
// SagaOrchestrator.java
public void orchestrateWithdrawalApproval(Long paymentOrderId) {
    // Step 1: 風險評估
    RiskAssessmentResult risk = riskService.assessWithdrawal(paymentOrderId);
    if (risk.getScore() > 70) {
        compensateStep1(paymentOrderId);  // 釋放鎖定資金
        return;
    }

    // Step 2: KYC/AML 驗證
    KycVerificationResult kyc = kycService.verifyPlayer(playerId);
    if (!kyc.isPassed()) {
        compensateStep2(paymentOrderId);  // 保留資金 + 轉待驗證
        return;
    }

    // Step 3: 審批路由
    WithdrawalApprovalStageEnum stage = routeApprovalStage(risk.getScore());
    approvalManager.createApprovalRecord(paymentOrderId, risk.getScore(), stage);

    // 等待人工審批或超時自動決策...
}
```

### Phase 4: Controller & API (Week 3)

**交付物**:
- ✅ `WithdrawalApprovalController.java` - Admin 審批端點
- ✅ Swagger/Knife4j API 文檔
- ✅ 集成測試（完整 SAGA 流程）

**API 端點設計**:
```java
@RestController
@Tag(name = "Withdrawal Approval")
public class WithdrawalApprovalController {

    @GetMapping("/withdrawal/approval/pending")
    @SaCheckPermission("withdrawal:approval:view")
    public ResponseDTO<PageResult<WithdrawalApprovalVO>> getPendingApprovals(
        WithdrawalApprovalQueryForm form);

    @PostMapping("/withdrawal/approval/approve")
    @SaCheckPermission("withdrawal:approval:approve")
    public ResponseDTO<Void> approveWithdrawal(@RequestBody ApprovalDecisionForm form);

    @PostMapping("/withdrawal/approval/reject")
    @SaCheckPermission("withdrawal:approval:reject")
    public ResponseDTO<Void> rejectWithdrawal(@RequestBody ApprovalDecisionForm form);

    @PostMapping("/withdrawal/approval/delegate")
    @SaCheckPermission("withdrawal:approval:delegate")
    public ResponseDTO<Void> delegateApproval(@RequestBody ApprovalDelegationForm form);
}
```

---

## 📊 預期成果 (Expected Outcomes)

### 功能成果

1. **多級審批流程**:
   - ✅ 支持 L1/L2/L3 三級審批鏈
   - ✅ 基於風險分數的智能路由（0-30自動, 31-50 L1, 51-70 L1+L2, 71-100 L1+L2+L3）
   - ✅ SLA 監控（24h 超時自動決策）

2. **KYC/AML 整合**:
   - ✅ 檢查玩家 KYC 驗證狀態（kycLevel, kycStatus）
   - ✅ PEP 篩查（Politically Exposed Persons）
   - ✅ 制裁名單比對（Sanctions Screening）

3. **SAGA 補償流程**:
   - ✅ 每個步驟失敗的補償邏輯（釋放資金、轉待驗證、自動路由等）
   - ✅ 重試策略（指數退避、冪等重試）
   - ✅ 狀態追蹤（saga_state, compensation_state）

4. **審計合規**:
   - ✅ 完整審批歷史（不可變記錄）
   - ✅ WHO/WHEN/WHY 追溯（approver_id, reviewed_at, approval_notes）
   - ✅ 風險決策理由（routing_reason, risk_score）

### 性能指標

| 指標 | 目標值 | 測量方式 |
|------|--------|---------|
| **風險評估延遲** | < 100ms | Step 1 執行時間 |
| **KYC 驗證延遲** | < 500ms | Step 2 執行時間 |
| **審批路由延遲** | < 50ms | Step 3 執行時間 |
| **端到端延遲（自動審批）** | < 1s | 提款請求 → PSP 調用 |
| **審批隊列查詢** | < 200ms | 分頁查詢（100條/頁） |
| **數據庫寫入** | < 50ms | 單筆審批記錄插入 |

### 合規指標

| 監管機構 | 要求 | 實現方式 | 驗證方法 |
|---------|------|---------|---------|
| **UKGC** | 96.3% 自動處理 | 風險分數 0-30 自動審批 | 統計報表（每月） |
| **UKGC** | 僅 0.1% 超過 48h | SLA 監控 + 超時自動決策 | SLA 儀表板 |
| **MGA** | €2,000 強化盡職調查 | 累計提款金額檢查 | 風險評估規則 |
| **PAGCOR** | 3 天 KYC 時限 | KYC 狀態檢查（Step 2） | KYC 驗證服務 |
| **巴西** | 120 分鐘處理時限 | SLA 截止時間設置 | SLA 追蹤表 |

---

## ⚠️ 風險與應對措施 (Risks and Mitigation)

### 風險 1: 審批員不在線導致超時

**影響**: 中高
**應對措施**:
1. **備用審批員機制**: 自動路由到同級其他審批員
2. **升級機制**: 24h 無響應自動升級到上一級審批員
3. **SLA 監控**: 實時告警（審批隊列超過 50 筆 → 通知主管）

### 風險 2: SAGA 補償失敗

**影響**: 高
**應對措施**:
1. **冪等性保證**: 補償操作支持多次執行（釋放資金、更新狀態）
2. **死信隊列**: Kafka DLQ（Dead Letter Queue）收集失敗事件
3. **人工介入**: 補償失敗 3 次後標記為 COMPENSATION_FAILED + 人工處理

### 風險 3: 性能瓶頸（JOIN 查詢）

**影響**: 中
**應對措施**:
1. **索引優化**: payment_order_id, approval_status, approver_id 複合索引
2. **緩存策略**: Redis 緩存待審批隊列（TTL 30s）
3. **分頁查詢**: 限制每頁 100 條（避免全表掃描）

---

## 📚 相關文檔 (Related Documentation)

- **業務需求**: [01-05_Withdrawal_Risk.md](../source-archive/01_Player_Center/01-05_Withdrawal_Risk.md)
- **支付設計**: [01-payment-design.md](./01-payment-design.md)
- **風控架構**: [03-risk-engine-design.md](./03-risk-engine-design.md)
- **現有遷移**: V9__payment_tables.sql（已合併至 V2__igaming_domain_tables.sql）

---

## ✅ 下一步行動 (Next Steps)

1. ✅ **確認評估結果**: 確認獨立 t_withdrawal_approval 表方案
2. ⏳ **創建 V23 遷移腳本**: 編寫完整 SQL 遷移檔案
3. ⏳ **實現 Java 類**: Entity, Dao, Manager, Service, Controller
4. ⏳ **整合 SAGA 工作流**: Spring State Machine 或 Temporal
5. ⏳ **編寫集成測試**: 完整審批流程測試（L1, L1+L2, L1+L2+L3）
6. ⏳ **創建 API 文檔**: Swagger/Knife4j 完整端點文檔

---

**文檔版本**: 1.0.0
**最後更新**: 2026-03-10
**維護者**: SmartAdmin iGaming Team
