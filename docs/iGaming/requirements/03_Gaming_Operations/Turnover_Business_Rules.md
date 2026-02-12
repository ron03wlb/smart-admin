# 有效投注額業務規則（Turnover Business Rules）

> **Canonical Source**: [source-archive/03_Game_Center/03-04_Turnover_Calculation.md](../../source-archive/03_Game_Center/03-04_Turnover_Calculation.md)
> **Audience**: 高階主管、產品經理
> **Related Architecture**: [Turnover_Calculation_Logic.md](../../architecture/03_Game_Integration/Turnover_Calculation_Logic.md)
> **Last Synced**: 2026-02-08
> **Source Version**: 4.0.0

---

## 1. 執行摘要（Executive Summary）

本文件定義 iGaming 平台中有效投注額（Valid Turnover，wagering）計算的業務規則。有效投注額計算對以下方面至關重要：

- **玩家投注進度**：追蹤紅利解鎖要求
- **返水計算**：決定現金回饋金額
- **VIP 等級晉升**：衡量玩家活躍度
- **監管合規**：準確的 GGR（Gross Gaming Revenue，總博弈收入）報告

**核心原則**：只有產生輸贏結果、具有實際風險且通過風控驗證的投注，才計入有效投注額。

---

## 2. 術語定義（Terminology Definitions）

| 術語 | 定義 | 範圍 |
|------|------------|-------|
| **Bet Amount** | 單筆投注的原始賭注 | 每筆投注 |
| **Valid Turnover（有效投注額）** | 累積的投注金額總和（也稱為「Turnover」） | 累積 |
| **Valid Bet** | 風控篩選後的單筆投注金額 | 每筆投注 |
| **Wagering Requirement（投注要求）** | 解鎖紅利所需的總有效投注額 | 累積 |

---

## 3. 三層驗證架構（Three-Layer Validation Architecture）

有效投注額計算遵循嚴格的三層驗證模型，確保關注點分離和清晰的職責劃分。

### 3.1 層級概述（Layer Overview）

| 層級 | 模組 | 職責 | 輸出 |
|-------|--------|----------------|--------|
| **Layer 1** | Risk Engine | 拒絕決策（BLOCK/FLAG/PASS） | `effective_turnover_base` |
| **Layer 2** | Finance Center | 結算狀態記錄 | `valid_turnover_finance` |
| **Layer 3** | Activity System | 遊戲權重應用 | `activity_valid_turnover` |

### 3.2 核心原則（Key Principle）

**Layer 1 是唯一負責拒絕決策的層級**。後續層級（Layer 2/3）僅執行數值調整，不執行拒絕。

### 3.3 層級職責矩陣（Layer Responsibilities Matrix）

| 職責 | Layer 1 (Risk) | Layer 2 (Finance) | Layer 3 (Activity) |
|----------------|----------------|-------------------|-------------------|
| **拒絕決策** | 是（唯一） | 否 | 否 |
| **狀態係數調整** | 否 | 是（唯一） | 否 |
| **遊戲權重應用** | 否 | 否 | 是（唯一） |
| **Block 時提前返回** | 是 | 否 | 否 |

---

## 4. 狀態係數定義（Status Factor Definitions）

### 4.1 標準本金法（Standard Principal Method）

**核心規則**：Valid Bet = Bet Amount（不論結算狀態）

「標準本金法」根據投注時承擔的風險平等對待所有投注金額，而非結果。

### 4.2 狀態係數表（Status Factor Table）

| 狀態 | 係數 | 說明 |
|--------|--------|-------------|
| **WIN** | 100% | 正常計算 |
| **LOSS** | 100% | 正常計算 |
| **HALF_WIN** | 100% | 標準本金法（v2.0.0） |
| **HALF_LOSS** | 100% | 標準本金法（v2.0.0） |
| **DRAW / TIE** | 0% | 無風險暴露，無有效投注額 |
| **VOID / CANCEL** | 0% | 無效投注 |
| **RUNNING** | 0% | 未結算，待結果 |

### 4.3 HALF_WIN/HALF_LOSS 的業務理由（Business Rationale for HALF_WIN/HALF_LOSS）

**為何是 100%（而非 50%）？**

考慮兩名玩家都對亞洲讓分 -0.25 投注 $100：
- 玩家 A：全贏結果 → Valid Bet = $100
- 玩家 B：半輸結果 → Valid Bet 也應為 $100

**理由**：
1. 兩名玩家採取相同的投注行為
2. 兩者承擔相同的風險金額（$100）
3. Valid Bet 反映投注行為，而非結算結果
4. 簡化計算，無需等待結算

---

## 5. 遊戲權重政策（Game Weight Policies）

### 5.1 權重配置表（Weight Configuration Table）

| 遊戲類型 | 權重 | 業務理由 |
|-----------|--------|-------------------|
| **Slots** | 100% | 純機率，高平台優勢 |
| **Sports Betting** | 100% | 高風險，不可預測結果 |
| **E-Sports** | 100% | 類似體育博彩的風險特徵 |
| **Roulette** | 20% | 中等風險，莊家優勢 ~2.7% |
| **Baccarat** | 15% | 高 RTP（~98.9%），低平台風險 |
| **Live Casino** | 15% | 依營運商策略而異 |
| **Blackjack** | 10% | 基於技巧，低莊家優勢 |
| **Video Poker** | 5% | 高技巧因子 |
| **Poker** | 5% | 基於技巧的遊戲 |
| **Lottery** | 10-20% | 雙邊投注（大/小）易對沖 |
| **PVP Games** | 0% | 玩家對玩家轉移，無平台風險 |

### 5.2 權重調整考量（Weight Adjustment Considerations）

遊戲權重依促銷活動可配置。考量因素：
- **平台 RTP**：RTP 越高 = 權重越低
- **濫用潛力**：易對沖的遊戲權重較低
- **技巧因子**：高技巧遊戲權重較低
- **市場競爭**：可能根據競爭對手的優惠調整

---

## 6. 投注要求規則（Wagering Requirement Rules）

### 6.1 核心決策：提款時驗證（Withdrawal-Time Verification）

**業務規則**：投注進度在遊玩期間累積，但驗證僅在提款時執行。

### 6.2 方法比較（Comparison of Approaches）

| 方面 | 投注時自動解鎖（錯誤） | 提款時驗證（正確） |
|--------|---------------------------|----------------------------------|
| **玩家達標後輸光** | 紅利已解鎖，營運商損失 | 紅利仍鎖定，風險受保護 |
| **玩家體驗** | 隱藏風險 | 提款時透明 |
| **風險控制** | 低保護 | 高保護 |
| **業界標準** | 不合規 | 合規 |

### 6.3 範例場景（Example Scenario）

1. 玩家獲得 $1,000 存款 + $1,000 紅利（100% 匹配）
2. 投注要求：($1,000 + $1,000) × 5 = $10,000
3. 玩家完成 $10,000 有效投注額
4. 玩家繼續遊玩並損失 $500
5. 提款請求時：系統驗證投注完成，但可用餘額反映損失

**結果**：營運商免於釋放後續損失的紅利資金。

---

## 7. Free Spins 有效投注額規則（Free Spins Turnover Rules）

### 7.1 核心原則（Core Principle）

| 指標 | 定義 | 計算 | 目的 |
|--------|------------|-------------|---------|
| **Turnover** | 總遊戲流量金額 | Free spin 面值總和 | 財務報告、GGR |
| **Valid Bet** | 投注貢獻 | 0（不計入） | 紅利進度、返水 |

### 7.2 業務理由（Business Rationale）

**為何 Turnover = 面值（而非 0）？**

範例：玩家獲得 10 次 free spins，每次 $1，贏得 $8.50

| 計算方法 | Turnover | Payout | GGR | 解讀 |
|-------------------|----------|--------|-----|----------------|
| Turnover = 0（錯誤） | $0 | $8.50 | -$8.50 | 顯示為損失 |
| Turnover = 面值（正確） | $10.00 | $8.50 | $1.50 | 反映促銷成本 |

### 7.3 業界標準（Industry Standard）

所有主要遊戲供應商使用此邏輯：

| 供應商 | Turnover | Valid Bet |
|----------|----------|-----------|
| Evolution Gaming | 面值總和 | 0 |
| Pragmatic Play | 面值總和 | 0 |
| Hub88 (Aggregator) | 面值總和 | 0 |

---

## 8. 風控動作（Risk Control Actions）

### 8.1 動作類型（v2.1.0）（Action Types）

| 動作 | 說明 | Valid Bet | Turnover | Risk Proposal |
|--------|-------------|-----------|----------|---------------|
| **BLOCK** | 即時封鎖 | 0 | 不計入 | 不生成 |
| **FLAG** | 標記但允許 | bet_amount | 正常 | 生成 |
| **PASS** | 正常通過 | bet_amount | 正常 | 不生成 |

### 8.2 偵測類型（Detection Types）

- **對沖偵測**：同一玩家、同一輪、相反投注
- **套利偵測**：跨平台/跨市場套利
- **低賠率過濾**：低於最低賠率門檻的投注（預設：1.5）
- **同 IP 對沖**：來自同一 IP 的多個帳戶投注相反方向

---

## 9. 合規要求（Compliance Requirements）

### 9.1 監控 SLA（Monitoring SLA）

| 指標 | 目標 | 告警門檻 |
|--------|--------|-----------------|
| 有效投注額計算延遲（P99） | < 100ms | > 500ms |
| Risk engine 呼叫成功率 | > 99.9% | < 99% |
| 每日對帳偏差 | < 0.01% | > 0.01% |
| 事件發布成功率 | > 99.99% | < 99.9% |

### 9.2 對帳門檻（Reconciliation Thresholds）

| 偏差範圍 | 門檻 | 業務影響 |
|-----------------|-----------|-----------------|
| **< 0.01%** | 可接受 | 最小（浮點精度） |
| **0.01% - 1%** | 警告 | 可能的配置錯誤 |
| **> 1%** | 嚴重 | 潛在資金風險 |

### 9.3 每日對帳排程（Daily Reconciliation Schedule）

- **執行時間**：每日 03:00 UTC+8
- **偏差門檻**：0.01%（比業界標準 0.1% 更嚴格）
- **告警渠道**：Slack + Email（警告），PagerDuty（嚴重）

---

## 10. 重新計算政策（Recalculation Policy）

### 10.1 觸發場景（Trigger Scenarios）

| 場景 | 優先級 | 重新計算範圍 | SLA |
|----------|----------|---------------------|-----|
| 狀態係數配置錯誤 | P0 | 受影響的交易 | 立即 |
| 遊戲權重調整 | P1 | 指定遊戲類型 | 24 小時 |
| GP 結算差異 | P1 | 單筆或批次 | 4 小時 |
| 促銷規則變更 | P2 | 促銷交易 | 批次作業 |

### 10.2 審計要求（Audit Requirements）

所有重新計算必須：
1. 執行前需要批准
2. 建立原始資料備份
3. 記錄所有變更的前後值
4. 完成後通知下游系統

---

## 11. 核心業務決策摘要（Key Business Decisions Summary）

### 決策 1：HALF_WIN/HALF_LOSS 使用標準本金法
- **選擇**：100% 有效投注額（非 50%）
- **理由**：相同的投注行為應得相同的有效投注額貢獻

### 決策 2：Free Spins 有效投注額計算
- **選擇**：Turnover = 面值總和，Valid Bet = 0
- **理由**：業界標準，準確的 GGR 報告

### 決策 3：提款時投注要求驗證
- **選擇**：在提款時驗證，而非投注時自動解鎖
- **理由**：保護營運商免於紅利濫用

### 決策 4：Layer 1 配置驅動的風控（v2.1.0）
- **選擇**：支援 BLOCK/FLAG/PASS 動作類型
- **理由**：靈活的風險回應，無需程式碼變更

---

## 12. 驗收標準（Acceptance Criteria）

有效投注額計算系統必須滿足以下驗收標準：

- [ ] **Layer 1 風控驗證**：Risk engine 在 50ms P99 延遲內執行 BLOCK/FLAG/PASS 決策，拒絕決策僅在 Layer 1 做出
- [ ] **狀態係數處理**：HALF_WIN 和 HALF_LOSS 投注使用標準本金法計算為 100% 有效投注額貢獻
- [ ] **遊戲權重應用**：所有遊戲類型應用正確的權重百分比（Slots 100%、Baccarat 15%、Blackjack 10% 等），無需手動干預
- [ ] **Free Spins 會計**：Free spin 有效投注額等於面值總和，Valid Bet 等於 0，確保準確的 GGR 報告
- [ ] **投注要求驗證**：投注要求完成在提款時驗證，而非在投注時自動解鎖
- [ ] **風控動作**：對沖偵測、套利偵測、低賠率過濾和同 IP 對沖即時運作，延遲 <100ms
- [ ] **對帳準確性**：每日有效投注額對帳偏差保持在 0.01% 門檻以下，偏差 ≥0.01% 時自動告警
- [ ] **重新計算支援**：狀態係數錯誤、遊戲權重調整和 GP 結算差異觸發自動重新計算，附完整審計軌跡
- [ ] **合規監控**：所有 SLA 目標（P99 延遲 <100ms、成功率 >99.9%、事件發布 >99.99%）持續監控並告警

---

## 13. 相關文件（Related Documents）

### 前置條件（Prerequisites）
- 00-03 Terminology Standards *(planned - source-archive/00_Foundation/00-03_Terminology_Standards)* - 必讀

### 技術實作（Technical Implementation）

→ **[Turnover Calculation Logic - Technical Architecture](../../architecture/03_Game_Integration/Turnover_Calculation_Logic.md)** - 完整的有效投注額計算演算法、投注狀態處理矩陣、遊戲權重表、三層驗證實作

### 依賴項（Dependencies）
- [05-01 Risk Framework](../../source-archive/05_Risk_Control/05-01_Risk_Framework.md) - Layer 1 risk engine
- [04-04 Activity Bonus](../../source-archive/04_Activity_Center/04-04_Activity_Bonus.md) - Layer 3 activity system

---

**Document Version**: 1.0.0 (derived from source v4.0.0)
**Last Updated**: 2026-02-08
**Maintainers**: Finance Team, Product Team
