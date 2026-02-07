# 風控系統改進方案

**版本**: v1.1.0
**日期**: 2026-02-07
**狀態**: 🟢 Phase 1 執行中

---

## 階段進度

| Phase | 名稱 | 狀態 | 預計完成 |
|-------|------|------|---------|
| 1 | 優先級決策重構 | 🟢 進行中 | Day 1-4 |
| 1.5 | 流水驗證整合 | ⏳ 待開始 | Day 5-6 |
| 2+ | [見獨立文檔](#相關文檔) | 📋 待前置依賴 | TBD |

---

## 背景與問題分析

### 核心問題 (Priority 1)

| # | 問題 | 說明 | 影響 |
|---|------|------|------|
| 1 | **決策模式不統一** | 04-01 使用分數 (0-100)，05-04 使用優先級 (URGENT/HIGH) | 邏輯衝突 |
| 2 | **SLA 策略粗糙** | 統一 24h 無法區分緊急程度 | 響應不及時 |
| 3 | **優先級計算硬編碼** | 無配置化支持，修改需重新發版 | 靈活性差 |

### 組織問題 (Priority 2)

| # | 問題 | 說明 |
|---|------|------|
| 4 | **流水驗證分離** | §4 與風控決策缺乏整合 |
| 5 | **文檔術語不一致** | "分數"/"優先級" 混用 |

---

## 決策確認 ✅

| # | 決策項目 | 確認方案 |
|---|---------|---------|
| 1 | 決策模式 | ✅ 使用優先級決策（移除 0-100 分數累加） |
| 2 | SLA 策略 | ✅ 按優先級設定不同 SLA，超時自動拒絕出金 |
| 3 | 優先級計算 | ✅ 在規則配置中決定優先級計算邏輯 |

---

## Phase 1: 優先級決策重構 (Day 1-4)

### Step 1.1: 取款決策邏輯 (Day 1)

**目標**: 建立統一的優先級決策模型，取代舊版 0-100 分數機制。

**優先級映射**:

| 舊版 (分數) | 新版 (優先級) | 行為 |
|-----------|-------------|------|
| 86-100 → 阻擋 | URGENT | 立即阻斷 |
| 61-85 → 人工審核 | HIGH | 立即阻斷 |
| 31-60 → 監控 | MEDIUM | 人工審核 |
| 0-30 → 放行 | LOW | 正常放行 |

**實現**:

```java
public String evaluateWithdrawal(Long playerId) {
    List<RiskProposal> proposals = getRelatedProposals(playerId, 30);

    if (hasProposalWithPriority(proposals, "URGENT")) return "BLOCKED";
    if (hasProposalWithPriority(proposals, "HIGH")) return "BLOCKED";
    if (hasProposalWithPriority(proposals, "MEDIUM")) return "MANUAL_REVIEW";
    return "APPROVED";
}
```

**驗證檢查點**:
- [ ] URGENT 提案 → BLOCKED
- [ ] HIGH 提案 → BLOCKED
- [ ] MEDIUM 提案 → MANUAL_REVIEW
- [ ] 無提案 → APPROVED

---

### Step 1.2: SLA 精細化配置 (Day 1-2)

**目標**: 按優先級設定不同 SLA，提升緊急事件響應效率。

**SLA 配置表**:

| 優先級 | SLA | 超時行為 | 適用場景 |
|--------|-----|---------|---------|
| **URGENT** | 1h | 🔴 自動拒絕 | 黑名單/IP封禁 |
| **HIGH** | 2h | 🔴 自動拒絕 | 機器人檢測 |
| **MEDIUM** | 24h | 🔴 自動拒絕 | 異常投注 |
| **LOW** | 48h | 🟢 自動放行 | 數據收集 |

**驗證檢查點**:
- [ ] URGENT SLA = 1h
- [ ] HIGH SLA = 2h
- [ ] MEDIUM SLA = 24h
- [ ] LOW SLA = 48h

---

### Step 1.3: 超時處理服務 (Day 2-3)

**目標**: 實現超時自動處理機制。

**實現**:

```java
@Scheduled(fixedDelay = 300_000)  // 每 5 分鐘
public void processExpiredProposals() {
    List<RiskProposalEntity> expired = findExpiredProposals();

    for (RiskProposalEntity p : expired) {
        if ("URGENT/HIGH/MEDIUM".contains(p.getPriority())) {
            // 自動拒絕
            rejectWithdrawal(p.getWithdrawalRequestId());
            p.setStatus("REJECTED");
        } else {
            // LOW: 自動放行
            approveWithdrawal(p.getWithdrawalRequestId());
            p.setStatus("APPROVED");
        }
    }
}
```

**驗證檢查點**:
- [ ] URGENT/HIGH/MEDIUM 超時 → 自動拒絕
- [ ] LOW 超時 → 自動放行
- [ ] 定時任務正常執行

---

### Step 1.4: 優先級計算配置 (Day 3-4)

**目標**: 支持三種優先級計算模式，實現配置化。

**計算模式**:

| 模式 | 說明 | 使用場景 |
|------|------|---------|
| **FIXED** | 固定優先級 | 黑名單、IP封禁 |
| **AMOUNT_BASED** | 基於金額計算 | 可疑取款金額 |
| **CUSTOM** | 自定義表達式 | 複雜欺詐模式 |

**資料庫變更**:

```sql
ALTER TABLE t_risk_rule_config
ADD COLUMN priority_calculation_mode VARCHAR(30) DEFAULT 'FIXED',
ADD COLUMN amount_thresholds JSONB;

-- FIXED 模式示例
INSERT INTO t_risk_rule_config (rule_code, trigger_priority, priority_calculation_mode)
VALUES ('BLACKLIST', 'URGENT', 'FIXED');

-- AMOUNT_BASED 模式示例
INSERT INTO t_risk_rule_config (rule_code, priority_calculation_mode, amount_thresholds)
VALUES ('SUSPICIOUS_AMOUNT', 'AMOUNT_BASED',
        '{"URGENT": 10000, "HIGH": 5000, "MEDIUM": 1000}');
```

**服務實現**:

```java
public String calculatePriority(RiskRuleConfig rule, RiskContext ctx) {
    return switch (rule.getPriorityCalculationMode()) {
        case "FIXED" -> rule.getTriggerPriority();
        case "AMOUNT_BASED" -> calculateByAmount(ctx.getAmount(), rule.getAmountThresholds());
        case "CUSTOM" -> expressionEngine.execute(rule.getPriorityExpression(), ctx);
        default -> "MEDIUM";
    };
}

private String calculateByAmount(BigDecimal amount, Map<String, Integer> thresholds) {
    if (amount.compareTo(BigDecimal.valueOf(thresholds.get("URGENT"))) >= 0) return "URGENT";
    if (amount.compareTo(BigDecimal.valueOf(thresholds.get("HIGH"))) >= 0) return "HIGH";
    if (amount.compareTo(BigDecimal.valueOf(thresholds.get("MEDIUM"))) >= 0) return "MEDIUM";
    return "LOW";
}
```

**驗證檢查點**:
- [ ] FIXED 模式正常工作
- [ ] AMOUNT_BASED 模式正常工作
- [ ] CUSTOM 模式正常工作

---

### Step 1.5: 文檔一致性修正 (Day 4)

**目標**: 確保所有文檔使用統一術語。

**修正清單**:

| 文檔 | 修改內容 | 狀態 |
|------|---------|------|
| `04-01_Risk_Framework.md` | 移除 0-100 分數邏輯，插入優先級矩陣 | ⏳ |
| `05-04_Risk_Proposal_Workflow.md` | 統一 SLA 定義，添加超時處理 | ⏳ |
| `風控系統架構.md` | 確認 v3.0.0 超時行為一致 | ⏳ |

**驗證檢查點**:
- [ ] 所有文檔使用 "優先級" 而非 "分數"
- [ ] SLA 定義一致
- [ ] 無遺留的 "0-100 分" 描述

---

## Phase 1.5: 流水驗證整合 (Day 5-6)

### Step 1.5.1: 存取款快照機制 (Day 5)

**目標**: 解決長週期流水驗證的性能問題。

**方案**: 採用 **存取款快照法 (Checkpoint Snapshot)**

- **核心機制**: 在每次取款成功時，記錄當前總流水快照
- **驗證公式**: `本次有效流水 = 當前總流水(實時) - 上次快照總流水`
- **優勢**: 驗證耗時 O(1)，與時間跨度無關

**資料庫設計**:

```sql
CREATE TABLE t_player_turnover_snapshot (
    id BIGINT PRIMARY KEY,
    player_id BIGINT NOT NULL,
    withdrawal_id BIGINT NOT NULL,
    snapshot_turnover DECIMAL(18,2) NOT NULL,
    snapshot_time TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**驗證檢查點**:
- [ ] 快照表創建成功
- [ ] 取款成功時記錄快照
- [ ] 流水驗證使用快照差值

---

### Step 1.5.2: 活動風控整合 (Day 5-6)

**目標**: 建立雙層防護機制。

**雙層防護**:

| 層級 | 類型 | 機制 |
|------|------|------|
| **Prevention** | 前置 | 領獎攔截 (IP/設備檢查) + 投注攔截 (低賠率/對沖 → 流水貢獻為 0) |
| **Detection** | 後置 | 取款前掃描異常行為並生成風控提案 |

**驗證檢查點**:
- [ ] 低賠率投注流水貢獻 = 0
- [ ] 對沖投注流水貢獻 = 0
- [ ] 異常行為生成風控提案

---

### Step 1.5.3: UI 警示標記 (Day 6)

**目標**: 在審核頁面提供清晰的風險標識。

**UI 標記規範**:

| 頁面 | 標記內容 |
|------|---------|
| **審核頁 (Proposal)** | 高亮 "關聯風險"、"活動套利" 等標籤 |
| **注單頁 (Details)** | 標記無效流水注單 (e.g., "[低賠率] 流水: 0.00") |

**驗證檢查點**:
- [ ] 審核頁顯示風險標籤
- [ ] 注單頁標記無效流水

---

## 風險評估

| 風險類別 | 可能性 | 影響 | 風險等級 | 緩解措施 |
|---------|-------|------|---------|---------|
| **舊邏輯殘留** | 🟡 中 | 🔴 高 | 🔴 高 | 全面搜索 "score" 關鍵字 |
| **SLA 超時誤殺** | 🟡 中 | 🔴 高 | 🔴 高 | 先灰度 LOW 優先級規則 |
| **文檔不同步** | 🔴 高 | 🟡 中 | 🔴 高 | Pre-commit Hook 檢查術語一致性 |
| **並行編輯衝突** | 🟢 低 | 🟡 中 | 🟡 中 | 文檔凍結期通知 |

**回滾策略**:
- **Level 1**: 單 Step 回滾（`git revert <commit>`）
- **Level 2**: 整個 Phase 回滾（`git reset --hard phase1-baseline`）

---

## 預期成果

| 指標 | 重構前 | 重構後 | 改進 |
|------|--------|--------|------|
| **決策模式** | Score (0-100) | Priority (4 級) | 更清晰 |
| **SLA 區分** | 統一 24h | 1h/2h/24h/48h | 更精細 |
| **配置化程度** | 硬編碼 | 3 種模式 (FIXED/AMOUNT/CUSTOM) | 更靈活 |
| **文檔一致性** | ~50% | 100% | +50% |
| **流水驗證效率** | O(n) 全量查詢 | O(1) 快照法 | 性能提升 |

---

## 關鍵文件清單

| 文件 | 操作 | 說明 |
|------|------|------|
| `04-01_Risk_Framework.md` | 編輯 | 移除 0-100 分數邏輯 |
| `05-04_Risk_Proposal_Workflow.md` | 編輯 | 統一 SLA，添加超時處理 |
| `風控系統架構.md` | 確認 | 確認 v3.0.0 超時行為 |
| `RiskProposalService.java` | 新增 | 優先級決策方法 |
| `t_risk_rule_config` | ALTER | 添加優先級計算欄位 |
| `t_player_turnover_snapshot` | CREATE | 流水快照表 |

---

## 相關文檔

本改進方案拆分為多個獨立文檔，按前置依賴分階段實施：

| 文檔 | 主題 | 狀態 | 前置依賴 |
|------|------|------|---------|
| **本文檔** | Phase 1 + 1.5（優先級決策 + 流水驗證） | 🟢 執行中 | 無 |
| [000-01-A](./000-01-A_Provider_Game_Risk.md) | 供應商與遊戲風控 | 📋 待定 | 000-03 |
| [000-01-B](./000-01-B_Financial_Reconciliation.md) | 財務整合與對帳 | 📋 待定 | 000-04 |
| [000-01-C](./000-01-C_Compliance_Resilience.md) | 合規與運營韌性 | 📋 待定 | 000-05 |

**前置依賴文檔（需先建立）**:
- [000-03_Gap_Analysis_Provider_Game_Risk.md](./000-03_Gap_Analysis_Provider_Game_Risk.md)
- [000-04_Gap_Analysis_Reconciliation.md](./000-04_Gap_Analysis_Reconciliation.md)
- [000-05_Final_Gap_Analysis_Compliance_Infra.md](./000-05_Final_Gap_Analysis_Compliance_Infra.md)

**參考文檔**:
- [08-turnover-validation-scheme](../technical-specs/P1-important/08-turnover-validation-scheme.md) - 流水驗證詳細方案

---

## 附錄 A: 資料庫變更腳本

```sql
-- A.1 規則配置表擴展
ALTER TABLE t_risk_rule_config
ADD COLUMN priority_calculation_mode VARCHAR(30) DEFAULT 'FIXED',
ADD COLUMN amount_thresholds JSONB;

-- A.2 流水快照表
CREATE TABLE t_player_turnover_snapshot (
    id BIGINT PRIMARY KEY,
    player_id BIGINT NOT NULL,
    withdrawal_id BIGINT NOT NULL,
    snapshot_turnover DECIMAL(18,2) NOT NULL,
    snapshot_time TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_snapshot_player ON t_player_turnover_snapshot(player_id);
CREATE INDEX idx_snapshot_time ON t_player_turnover_snapshot(snapshot_time);
```

---

## 附錄 B: 驗證命令

```bash
# B.1 搜索殘留的分數邏輯
grep -r "score" docs/iGaming/04_Risk_Control --include="*.md"
grep -r "0-100" docs/iGaming --include="*.md"

# B.2 驗證 SLA 一致性
grep -r "SLA\|超時" docs/iGaming/05_Risk_Management --include="*.md"

# B.3 檢查文檔術語統一
grep -rn "分數" docs/iGaming --include="*.md" | grep -v "archive"
```

---

## 變更日誌

| 版本 | 日期 | 變更 |
|------|------|------|
| v1.1.0 | 2026-02-07 | 結構重組：Phase 2/3/4 分離為獨立文檔，新增背景分析/風險評估/預期成果 |
| v1.0.0 | 2026-02-05 | 初始版本：統一優先級決策、SLA 精細化、配置驅動 |
