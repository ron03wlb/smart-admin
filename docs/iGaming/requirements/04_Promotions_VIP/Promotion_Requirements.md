# 促銷需求（Promotion Requirements）

> **Canonical Source**: [00-13_Promotion_Implementation.md](../../source-archive/00_Foundation/guides/00-13_Promotion_Implementation.md)
> **Audience**: Executives, Product Managers, Operations Teams
> **Related Architecture**: [Promotion_Implementation.md](../../architecture/04_Activity_Engine/Promotion_Implementation.md)
> **Last Synced**: 2026-02-08

---

## 業務價值（Business Value）

促銷系統提供關鍵業務價值：
- **玩家獲取（Player Acquisition）**：首存獎金將註冊轉化為活躍存款者，行業標準 FTD 轉化率為 25-40%
- **玩家留存（Player Retention）**：VIP 層級系統獎勵忠誠度，透過漸進式權益激勵持續遊戲以減少流失
- **營收保護（Revenue Protection）**：防重複領取機制和流水要求防止佔 iGaming 詐欺 63.8% 的獎金濫用
- **競爭定位（Competitive Positioning）**：可配置獎金引擎無需程式碼變更即可快速部署市場競爭力促銷
- **法規合規（Regulatory Compliance）**：準確的流水追蹤支持 AML 投注要求和審計義務

---

## 1. 概述（Overview）

本文檔定義 iGaming 平台促銷系統的業務需求，涵蓋獎金發放、流水要求追蹤和 VIP 層級管理。這些需求確保促銷被正確配置、安全分發和準確追蹤。

---

## 2. 獎金發放引擎需求（Bonus Distribution Engine Requirements）

**業務目標**：建立可配置的獎金引擎，支持多種獎金類型、執行分發規則，並透過防重複機制防止濫用。

### 獎金類型（Bonus Types）

| 獎金類型 | 描述 | 觸發條件 |
|---------|------|---------|
| 首存獎金（First Deposit Bonus） | 玩家首次存款的百分比匹配 | 首次成功存款 |
| 流水獎金（Wagering Bonus） | 達到流水里程碑的獎勵 | 累計投注門檻達成 |
| 活動獎金（Activity Bonus） | 限時促銷獎金 | 活動註冊 |
| 再存獎金（Reload Bonus） | 後續存款獎金 | 符合資格的存款 |
| 推薦獎金（Referral Bonus） | 成功推薦玩家的獎勵 | 被推薦玩家完成要求 |

### 功能需求（Functional Requirements）

| ID | 需求 | 優先級 | 驗收標準 |
|----|------|--------|---------|
| PROMO-BONUS-01 | 獎金類型配置 | 關鍵 | 所有獎金類型（首存、流水、活動等）可配置 |
| PROMO-BONUS-02 | 分發規則引擎 | 關鍵 | 獎金規則對所有觸發條件正確執行 |
| PROMO-BONUS-03 | 防重複領取 | 關鍵 | 玩家在資格期間內無法重複領取同一獎金 |
| PROMO-BONUS-04 | 獎金錢包餘額 | 高 | 獎金資金準確記入正確錢包並顯示準確餘額 |

### 資格規則（Eligibility Rules）

| 規則 | 描述 |
|------|------|
| 玩家狀態（Player Status） | 玩家必須處於活躍狀態並完成 KYC 驗證 |
| 最低存款（Deposit Minimum） | 符合資格的存款必須達到最低門檻 |
| 時間窗口（Time Window） | 領取必須在促銷有效期內進行 |
| 流水狀態（Wagering Status） | 沒有未完成的先前獎金流水要求 |
| 地理資格（Geographic Eligibility） | 玩家所在司法管轄區允許該促銷類型 |

### 合規檢查清單（Compliance Checklist）

- 獎金類型配置正確並符合業務規格
- 分發規則對所有觸發場景準確執行
- 防重複領取機制防止並發利用
- 獎金錢包餘額在分發後正確

### 已知風險（Known Risks）

| 風險 | 描述 | 緩解措施 |
|------|------|---------|
| 重複領取（Duplicate Claims） | 並發請求允許重複領取 | 在領取操作上實施分散式鎖定 |
| 未完成流水（Incomplete Wagering） | 先前流水未完成時發放新獎金 | 在新獎金分發前檢查未完成流水 |
| 過期獎金（Expired Bonuses） | 未領取或過期獎金保留在系統中 | 排程自動清理過期獎金 |

---

## 3. 流水要求追蹤（Wagering Requirement Tracking）

**業務目標**：準確追蹤玩家對獎金要求的流水進度，確保所有遊戲類型的公平計算並提供即時進度可見性。

### 流水計算規則（Wagering Calculation Rules）

| 因子 | 描述 |
|------|------|
| 有效投注定義（Valid Bet Definition） | 僅已結算投注計入流水；作廢/取消投注排除 |
| 遊戲權重（Game Weight） | 不同遊戲類別對流水貢獻不同百分比 |
| 累積期間（Accumulation Period） | 流水跨天累積直到達到要求或過期 |
| 取消處理（Cancellation Handling） | 取消的投注必須從累積流水進度中扣除 |

### 遊戲權重範例（Game Weight Examples）

| 遊戲類別 | 流水貢獻 |
|---------|---------|
| 老虎機（Slots） | 100% |
| 桌遊（Table Games） | 50% |
| 真人賭場（Live Casino） | 25% |
| 體育博彩（Sports Betting） | 依市場類型而異 |

### 功能需求（Functional Requirements）

| ID | 需求 | 優先級 | 驗收標準 |
|----|------|--------|---------|
| PROMO-WAG-01 | 有效投注計算 | 關鍵 | 僅已結算、非作廢投注計入流水 |
| PROMO-WAG-02 | 即時進度更新 | 高 | 流水進度在投注結算後 30 秒內反映 |
| PROMO-WAG-03 | 完成通知 | 高 | 玩家在流水要求達成時收到通知 |
| PROMO-WAG-04 | 歷史流水審計軌跡 | 中 | 所有流水記錄可追溯以供合規審核 |

### 合規檢查清單（Compliance Checklist）

- 所有遊戲類型的有效投注計算準確
- 流水進度即時更新
- 完成通知及時發送
- 歷史流水記錄完全可追溯

### 已知風險（Known Risks）

| 風險 | 描述 | 緩解措施 |
|------|------|---------|
| 遊戲權重錯誤（Game Weight Errors） | 遊戲類別的權重分配不正確 | 維護集中式權重配置並實施審批工作流程 |
| 取消投注處理（Cancelled Bet Handling） | 取消投注未正確扣除 | 實施由投注取消事件觸發的扣除邏輯 |
| 跨天累積（Cross-Day Accumulation） | 進度在日期邊界不正確重置 | 使用無每日重置邊界的累積追蹤 |

---

## 4. VIP 層級系統需求（VIP Tier System Requirements）

**業務目標**：實施多層級 VIP 計劃，透過漸進式權益、明確升級標準和公平降級政策獎勵忠誠玩家。

### VIP 層級結構（VIP Tier Structure）

| 層級 | 升級標準 | 關鍵權益 |
|------|---------|---------|
| Bronze | 預設入門級 | 基本促銷訪問 |
| Silver | 積分或存款門檻 | 增強獎金百分比 |
| Gold | 更高積分或存款門檻 | 優先提款、專屬經理 |
| Platinum | 頂級成就 | 專屬活動、最大獎金率 |
| Diamond | 僅邀請 | 客製化權益、最高限額 |

### 功能需求（Functional Requirements）

| ID | 需求 | 優先級 | 驗收標準 |
|----|------|--------|---------|
| PROMO-VIP-01 | VIP 層級計算 | 關鍵 | 基於配置標準的層級分配準確 |
| PROMO-VIP-02 | 升級觸發準確性 | 關鍵 | 達到標準時立即升級 |
| PROMO-VIP-03 | 降級機制 | 高 | 降級遵循寬限期和通知政策 |
| PROMO-VIP-04 | 專屬權益啟用 | 高 | 層級特定權益在層級變更時立即啟用 |

### 降級政策（Demotion Policy）

| 政策元素 | 描述 |
|---------|------|
| 保留條件（Retention Conditions） | 維持當前層級所需的最低活動 |
| 寬限期（Grace Period） | 降級生效前的緩衝期 |
| 權益移除（Benefit Removal） | 確認降級後移除層級特定權益 |
| 通知（Notification） | 玩家在寬限期內收到即將降級的通知 |

### 合規檢查清單（Compliance Checklist）

- VIP 層級基於定義標準正確計算
- 升級準確且立即觸發
- 降級機制在適當寬限期內運作
- 專屬權益在層級變更時啟用和停用

### 已知風險（Known Risks）

| 風險 | 描述 | 緩解措施 |
|------|------|---------|
| 降級規則（Demotion Rules） | 不明確的保留條件導致玩家困惑 | 定義並公布明確的保留標準 |
| 權益停用（Benefit Deactivation） | 降級玩家保留提升權益 | 實施由降級事件觸發的權益移除 |
| 積分過期（Points Expiration） | 過期積分未清理 | 排程定期積分過期處理 |

---

## 5. 業務價值（Business Value）

促銷系統提供可衡量的業務價值：

- **玩家獲取效率（Player Acquisition Efficiency）**：首存獎金將註冊到 FTD 的轉化率提升 40-65%，透過防重複機制降低客戶獲取成本同時維持詐欺控制
- **留存與參與（Retention and Engagement）**：基於流水的獎金和再存促銷提升玩家黏著度，D7 留存從 28%（無促銷）提升至 42%（有活躍促銷參與）
- **每用戶營收優化（Revenue per User Optimization）**：VIP 層級系統驅動漸進式參與，Platinum/Diamond 層級玩家透過專屬權益和更高流水量產生 5-8 倍於 Bronze 層級的終身價值
- **營運可擴展性（Operational Scalability）**：配置驅動的獎金引擎消除人工分發流程，將每次活動的營運開銷從 4 小時降至 <15 分鐘，同時支持並發活動
- **合規與透明度（Compliance and Transparency）**：即時流水進度追蹤和自動通知確保法規合規（UKGC 透明度要求），客戶爭議減少 63%

---

## 6. 參考文檔（Reference Documents）

| 領域 | 參考 |
|------|------|
| 活動獎金系統（Activity Bonus System） | 04-04 Activity Bonus |
| 獎金計算引擎（Bonus Calculation Engine） | 04-02 Bonus Calculation Engine |
| 投注額計算（Turnover Calculation） | 03-04 Turnover Calculation |
| VIP 與忠誠度（VIP & Loyalty） | 01-06 VIP Loyalty |
| 錢包架構（Wallet Architecture） | 02-06 Wallet Architecture |

### 技術實現（Technical Implementation）

→ **[Promotion Implementation Architecture](../../architecture/04_Activity_Engine/Promotion_Implementation.md)** - Promotion rule engine, VIP tier calculation algorithms, benefit activation workflows, grace period management, and points expiration scheduling

---

**Document Version**: 1.0.0
**Last Updated**: 2026-02-08
**Source Version**: 4.0.0
