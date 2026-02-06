# 風控系統改進方案

**版本**: v1.0.0
**日期**: 2026-02-05
**狀態**: ✅ 待實施

---

## 📋 改進背景

根據投注、風控、審核出金邏輯衝突分析，統一以下決策：

| # | 決策項目 | 確認方案 |
|---|---------|---------|
| 1 | 決策模式 | ✅ 使用優先級決策（移除 0-100 分數累加） |
| 2 | SLA 策略 | ✅ 按優先級設定不同 SLA，超時自動拒絕出金 |
| 3 | 優先級計算 | ✅ 在規則配置中決定優先級計算邏輯 |

---

## 1. 優先級決策模式

### 1.1 取代舊版分數邏輯

| 舊版 (分數) | 新版 (優先級) |
|-----------|-------------|
| 86-100 → 阻擋 | URGENT → 立即阻斷 |
| 61-85 → 人工審核 | HIGH → 立即阻斷 |
| 31-60 → 監控 | MEDIUM → 人工審核 |
| 0-30 → 放行 | LOW → 正常放行 |

### 1.2 取款決策邏輯

```java
public String evaluateWithdrawal(Long playerId) {
    List<RiskProposal> proposals = getRelatedProposals(playerId, 30);
    
    if (hasProposalWithPriority(proposals, "URGENT")) return "BLOCKED";
    if (hasProposalWithPriority(proposals, "HIGH")) return "BLOCKED";
    if (hasProposalWithPriority(proposals, "MEDIUM")) return "MANUAL_REVIEW";
    return "APPROVED";
}
```

---

## 2. SLA 精細化配置

### 2.1 SLA 與超時行為

| 優先級        | SLA | 超時行為    | 適用場景     |
| ---------- | --- | ------- | -------- |
| **URGENT** | 1h  | 🔴 自動拒絕 | 黑名單/IP封禁 |
| **HIGH**   | 2h  | 🔴 自動拒絕 | 機器人檢測    |
| **MEDIUM** | 24h | 🔴 自動拒絕 | 異常投注     |
| **LOW**    | 48h | 🟢 自動放行 | 數據收集     |

### 2.2 超時處理服務

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

---

## 3. 優先級計算配置

### 3.1 三種計算模式

| 模式 | 說明 | 使用場景 |
|------|------|---------|
| **FIXED** | 固定優先級 | 黑名單、IP封禁 |
| **AMOUNT_BASED** | 基於金額計算 | 可疑取款金額 |
| **CUSTOM** | 自定義表達式 | 複雜欺詐模式 |

### 3.2 規則配置表擴展

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

### 3.3 優先級計算服務

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

---

## 4. 流水驗證與活動風控方案 (新增)

### 4.1 解決長週期流水問題
採用 **方案 A: 存取款快照法 (Checkpoint Snapshot)**，解決數年未登錄玩家的流水驗證性能問題。

- **核心機制**: 在每次取款成功時，記錄當前總流水快照。
- **驗證公式**: `本次有效流水 = 當前總流水(實時) - 上次快照總流水`。
- **優勢**: 驗證耗時 O(1)，與時間跨度無關。

### 4.2 活動風控整合 (Activity Integration)
- **雙層防護**:
    - **Prevention (前置)**: 領獎攔截 (IP/設備檢查) + 投注攔截 (低賠率/對沖 → 流水貢獻為 0)。
    - **Detection (後置)**: 取款前掃描異常行為並生成風控提案。
- **UI 警示**:
    - **審核頁 (Proposal)**: 高亮 "關聯風險"、"活動套利" 等標籤。
    - **注單頁 (Details)**: 標記無效流水注單 (e.g., "[低賠率] 流水: 0.00")。

### 4.3 關聯文檔
- 詳見 **[08-turnover-validation-scheme](../technical-specs/P1-important/08-turnover-validation-scheme.md)**

---

## 5. 文檔一致性修正 (Documentation Harmonization)

根據 `000-02_Conflict_Audit_Report.md`，需執行以下修正以確保邏輯統一：

### 5.1 決策模型統一 (Score to Priority)
- **目標**: 廢棄 `04-01` 中的 "0-100 分" 機制，全面對齊 `05-04` 的 "優先級 (URGENT/HIGH)" 機制。
- **修正**: 更新 Layer 1 規則引擎輸出，不再返回 `score`，而是返回 `risk_level`。

### 5.2 時間範圍統一 (Time Range Alignment)
- **目標**: 消除 `07` (30天) 與 `08` (快照法) 的邏輯衝突。
- **決策**: 採用 **"全量一致性 (Full Consistency)"** 原則。
    - **規則**: 風控提案掃描範圍 = 流水驗證範圍 = `Last Snapshot Time` 至今。
    - **長週期**: 即使是 5 年未取款的老用戶，也必須掃描 5 年內所有 "未結案的嚴重提案"。

### 5.3 關聯修正
- `04-01`: 移除分數閾值表，插入優先級矩陣。
- `07`: 修改時間範圍規則，引用 `t_player_turnover_snapshot`。
- `02-04`: 增加對方案 A 快照法的引用。

---

## 6. 需修改的文檔

| 文檔 | 修改內容 | 優先級 |
|------|---------|--------|
| `04-01_Risk_Framework.md` | 移除 0-100 分數邏輯 | P0 |
| `05-04_Risk_Proposal_Workflow.md` | 統一 SLA，添加超時處理 | P0 |
| `風控系統架構.md` | 確認 v3.0.0 超時行為 | P0 |

---

## 7. 實施計劃 (Phase 1)

| Phase | 工作內容 | 時間 |
|-------|---------|------|
| 1 | 文檔統一 | 1 天 |
| 2 | 資料庫變更 (含快照表) | 1 天 |
| 3 | 服務實現 (含流水快照) | 3 天 |
| 4 | 測試驗證 | 1.5 天 |

---

## 8. 驗收標準 (Phase 1)

- [ ] 優先級決策：取款時按最高優先級提案決策
- [ ] SLA 精細化：URGENT=1h, HIGH=2h, MEDIUM=24h, LOW=48h
- [ ] 超時自動拒絕：URGENT/HIGH/MEDIUM 超時後拒絕出金
- [ ] 配置驅動：支持 FIXED/AMOUNT_BASED/CUSTOM 三種模式
- [ ] 文檔一致：所有文檔使用統一術語

---

## 9. 第二階段：供應商與遊戲風控 (Phase 2: Provider & Game Risk)

根據 [000-03_Gap_Analysis_Provider_Game_Risk.md](./000-03_Gap_Analysis_Provider_Game_Risk.md)，填補 "重玩家、輕平台" 的風控盲區。

### 9.1 問題陳述
現有系統缺乏對 **遊戲商異常 (Provider Anomalies)** 和 **遊戲漏洞 (Game Glitches)** 的防禦能力。
- **RTP 異常**: 若某款 Slot 遊戲因 Bug 導致 RTP > 100%，系統目前無法自動熔斷。
- **延遲套利**: 玩家利用不同平台間的 API 延遲 (Latency) 進行無風險套利。
- **數據完整性**: 缺乏 Provider 端的 Round ID 連續性檢查，易被 "吃單"。

### 9.2 擬新增模組
1.  **[04-05_Game_Anomaly_Monitor.md]**: 遊戲異常監控
    - **RTP 熔斷**: 實時監控 Lobby/Game 維度的 RTP，異常自動下架。
    - **特徵**: `WinAmount > BetAmount * 10` 且 `Frequency > 5/min`。
2.  **[02-08_Provider_Reconciliation.md]**: 遊戲商對帳
    - **完整性檢查**: `Platform.TotalBet` vs `Provider.TotalBet`。
    - **延遲監控**: 報警若 `BetTime - ReceiveTime > 2s`。

### 9.3 實施優先級
- **P1**: 遊戲異常監控 (RTP Monitor) - 直接防止巨額虧損。
- **P2**: 延遲套利檢測 (Latency Arbitrage) - 提升平台公平性。
- **P3**: 遊戲商對帳 (Provider Recon) - 財務合規。

---

## 10. 第三階段：財務整合與對帳深化 (Phase 3: Financial Integrity & Reconciliation)

根據 [000-04_Gap_Analysis_Reconciliation.md](./000-04_Gap_Analysis_Reconciliation.md)，強化 "內部總帳" 與 "佣金" 的精確性。

### 10.1 問題陳述
現有對帳側重於 **外部 (PSP/Game)**，缺乏 **內部審計 (Internal Audit)** 與 **佣金核算 (Commission)** 的監督機制。
- **內部總帳風險**: 若數據庫被 DB Admin 篡改或系統出現並發扣款 Bug，缺乏每日總帳試算 (Trial Balance) 難以發現。
- **佣金超發風險**: NGR 計算缺乏獨立檢核，可能因未扣除 Bonus Cost 導致代理佣金超發。
- **對帳精度問題**: 缺乏時區與幣種精度的明確標準。

### 10.2 擬新增模組
1.  **[02-09_Internal_Wallet_Audit.md]**: 內部錢包審計
    - **日結試算**: `Total Deposits - Withdrawals + Wins - Losses == Sum(User Balances)`。
    - **監控**: 餘額不平時觸發 P0 告警。
2.  **[06-05_Affiliate_Commission_Reconciliation.md]**: 代理佣金對帳
    - **NGR 審計**: 獨立驗證 `NGR = Bet - Win - Bonus - Tax` 的計算過程。
3.  **[02-04] 增強**: 補充 Provider 時區 (UTC/UTC+8) 與幣種精度處理邏輯。

### 10.3 實施優先級
- **P1**: 內部錢包審計 (Internal Audit) - 防止內部欺詐與系統崩潰。
- **P2**: 代理佣金對帳 (Commission Recon) - 防止資金流失。
- **P3**: 遊戲商時區精度優化 - 提升對帳效率。

---

## 11. 第四階段：合規與運營韌性 (Phase 4: Compliance & Operational Resilience)

根據 [000-05_Final_Gap_Analysis_Compliance_Infra.md](./000-05_Final_Gap_Analysis_Compliance_Infra.md)，補全 "責任博彩" 與 "基礎設施高可用"。

### 11.1 問題陳述
現有文檔在 **合規性 (Regulatory)** 與 **穩定性 (Stability)** 方面存在致命缺失。
- **合規紅線**: 缺乏責任博彩 (RG) 模組 (如自我隔離、存款限制)，無法通過 GLI/MGA 認證。
- **流量欺詐**: 現金網缺乏對 Affiliate Traffic 的防作弊機制 (Click Injection)，導致廣告費被刷。
- **單點故障**: 錢包服務缺乏明確的 "降級策略" (如 DB 掛掉時如何處理)。

### 11.2 擬新增模組
1.  **[01-06_Responsible_Gaming_Module.md]**: 責任博彩引擎
    - **功能**: Deposit Limit, Loss Limit, Self-Exclusion (不可撤銷), Reality Check。
    - **強制性**: 這是所有正規市場 (UK, US, EU) 的入場券。
2.  **[06-03_Affiliate_Tracking_Integrity.md]**: 代理流量完整性
    - **防護**: CTIT (Click-To-Install Time) 檢查，防止歸因劫持。
3.  **[07-05_High_Availability_Wallet.md]**: 錢包高可用
    - **降級**: 定義 "Read-Only Mode"，在主庫宕機時允許遊玩（扣款記入 Redis 隊列），但禁止充提。

### 11.3 實施優先級
- **P1**: 責任博彩 (RG) - 合規剛需，優先級最高。
- **P2**: 錢包高可用 (HA Wallet) - 防止重大事故。
- **P3**: 代理流量防護 - 優化運營成本。
