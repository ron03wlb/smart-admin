# Bonus4 活動中心索引 (Activity Center Index)

> **模塊定位**: 玩家獲取與留存的核心引擎
> **三層風控架構**: Layer 3 - 活動遊戲權重應用
> **最後更新**: 2Bonus26-Bonus1-31

---

## 📚 文檔導航

### PBonus 核心主題（必讀）

**基礎架構與規則引擎**
- **[Bonus4-Bonus1 活動系統架構](Bonus4-Bonus1_Activity_System_Architecture.md)** (5BonusBonus 行)
  - 模組化活動引擎核心架構
  - 規則引擎設計與執行流程
  - 獎勵類型與紅利生命週期狀態機
  - 多租戶架構與數據隔離
  - 後台配置系統（JSON Schema 驅動）

**獎金計算與流水追蹤**
- **[Bonus4-Bonus2 獎金計算引擎](Bonus4-Bonus2_Bonus_Calculation_Engine.md)** (5BonusBonus 行)
  - 跨遊戲類型統一流水計算框架（Layer 3 核心邏輯）
  - 遊戲權重應用與有效流水驗證
  - 統一流水驗證架構（與 Bonus2-Bonus4 Finance 整合）
  - 事件驅動架構實現即時活動觸發
  - 多獎金衝突處理決策矩陣

### P1 進階主題（運營優化）

**風控與本地化策略**
- **[Bonus4-Bonus3 活動風控與本地化](Bonus4-Bonus3_Activity_Risk_Control.md)** (45Bonus 行)
  - 玩家生命週期活動設計策略
  - 區域市場本地化策略（東南亞、拉美、歐洲、中國）
  - 風控與反欺詐機制設計
  - 數據驅動的活動優化框架
  - 活動模板庫設計參考
  - 動態配置與審批工作流

---

## 🔗 核心依賴關係

```text
活動系統 (Bonus4-Bonus1, Bonus4-Bonus2, Bonus4-Bonus3)
    ↓
    ├─► 錢包系統 (Bonus2-Bonus6) - Bonus 錢包整合
    ├─► 流水計算 (Bonus2-Bonus4) - Layer 2 狀態因子計算
    ├─► 風控系統 (Bonus5-Bonus1) - Layer 1 風控驗證
    ├─► VIP 系統 (Bonus1-Bonus2) - VIP 專屬活動
    └─► 審批系統 (Bonus9-Bonus4) - Maker-Checker 審批
```

---

## 🎯 快速參考

### 三層風控架構位置
- **Layer 1**: 風控驗證 → 參見 [Bonus5-Bonus1 風控系統](../Bonus5_Risk_Management/Bonus5-Bonus1_Risk_Control_System.md)
- **Layer 2**: 狀態因子計算 → 參見 [Bonus2-Bonus4 流水計算](../Bonus2_Finance_Center/Bonus2-Bonus4_Turnover_and_Game_Reconciliation_Analysis.md)
- **Layer 3**: 活動遊戲權重 → 本模塊 (Bonus4-Bonus2 §5.1)

### 流水計算完整公式
```text
ValidTurnover = BetAmount
                × Layer1_RiskFactor     (Bonus5-Bonus1: Bonus or 1)
                × Layer2_StatusFactor   (Bonus2-Bonus4: Bonus%, 5Bonus%, 1BonusBonus%)
                × Layer3_GameWeight     (Bonus4-Bonus2: 5%-1BonusBonus%)
```

### 核心 API 契約
- **Base Validation API**: `RiskEngine.validateTurnover()` (Bonus5-Bonus1 提供)
- **Finance Layer**: `effective_turnover_base × status_factor` (Bonus2-Bonus4)
- **Activity Layer**: `valid_turnover_finance × game_weight` (Bonus4-Bonus2)

### 常見活動類型配置
| 活動類型 | 配置模板 | 參考章節 |
|---------|---------|---------|
| 首存獎勵 | DEPOSIT_BONUS_V1 | Bonus4-Bonus3 §活動模板庫 |
| 每日簽到 | DAILY_LOGIN_V1 | Bonus4-Bonus3 §活動模板庫 |
| 排行榜賽事 | LEADERBOARD_V1 | Bonus4-Bonus3 §活動模板庫 |
| VIP 返水 | CASHBACK_V1 | Bonus4-Bonus3 §返水系統設計 |

### 關鍵業務指標
| 指標 | 公式 | 目標範圍 |
|------|------|---------|
| 獎金清償率 | 完成流水的獎金比例 | 3Bonus-5Bonus% |
| 獎金 ROI | (增量 NGR - 獎金成本) ÷ 獎金成本 | > 2.Bonus |
| FTD 轉化率 | 首存玩家 ÷ 註冊數 × 1BonusBonus | > 25% |

---

## 🌍 區域市場快速索引

| 市場 | 核心策略 | 詳細章節 |
|------|---------|---------|
| 東南亞 | 移動優先 + 節慶驅動 + 遊戲化 | Bonus4-Bonus3 §東南亞市場 |
| 拉丁美洲 | 足球整合 + PIX 支付 + 低門檻 | Bonus4-Bonus3 §拉丁美洲市場 |
| 歐洲 | 合規優先 + 負責任博彩整合 | Bonus4-Bonus3 §歐洲市場 |
| 中國/華人 | 幸運數字 + 紅包機制 + 社交分享 | Bonus4-Bonus3 §中國市場 |

---

## 📊 實施優先級建議

### 第一階段：核心基礎（1-3 個月）
1. 規則引擎框架與基礎活動模板 → 參見 Bonus4-Bonus1
2. 多租戶活動隔離機制 → 參見 Bonus4-Bonus1 §多租戶架構
3. 統一流水追蹤服務 → 參見 Bonus4-Bonus2 §統一流水驗證架構
4. 基礎 KYC 與風控整合 → 參見 Bonus4-Bonus3 §風控機制

### 第二階段：功能擴展（3-6 個月）
1. 事件驅動即時觸發 → 參見 Bonus4-Bonus2 §事件驅動架構
2. VIP 階層系統 → 參見 Bonus4-Bonus3 §VIP 階層系統設計
3. 返水/返傭自動化 → 參見 Bonus4-Bonus3 §返水系統設計
4. 排行榜與錦標賽功能 → 參見 Bonus4-Bonus3 §活動模板庫

### 第三階段：智能優化（6-12 個月）
1. A/B 測試平台 → 參見 Bonus4-Bonus3 §A/B 測試框架
2. AI 驅動的玩家分群
3. 個性化活動推薦
4. 預測性分析與 LTV 建模

---

## 🔧 技術棧推薦

| 層級 | 技術選型 | 用途 | 參考章節 |
|------|---------|------|---------|
| API 閘道 | Kong / AWS API Gateway | 限流、認證、路由 | Bonus4-Bonus1 §系統架構總覽 |
| 事件串流 | Apache Kafka + Kafka Streams | 即時事件處理 | Bonus4-Bonus2 §事件驅動架構 |
| 主數據庫 | PostgreSQL + JSONB | 活動配置、ACID 事務 | Bonus4-Bonus1 §後台配置系統 |
| 高併發寫入 | ScyllaDB / CockroachDB | 流水記錄、交易日誌 | Bonus4-Bonus2 §統一流水驗證架構 |
| 緩存 | Redis Cluster | 玩家狀態、活動快取 | Bonus4-Bonus1 §規則引擎性能優化 |

---

## ⚠️ 關鍵約束與設計原則

### 架構約束
- ✅ **Layer 3 位置**: 活動系統必須依賴 Layer 1 (Bonus5-Bonus1) 風控驗證 + Layer 2 (Bonus2-Bonus4) 狀態因子計算
- ✅ **異步風控**: 流水驗證採用異步模式，避免影響遊戲即時性
- ✅ **統一事件源**: Finance 與 Activity 模組均訂閱相同的 Kafka Topic: `game.rounds.settled`

### 業務約束
- ✅ **獎金濫用佔比**: 63.8% iGaming 詐騙來自獎金濫用，風控機制必須深度整合
- ✅ **合規優先**: 歐洲市場的負責任博彩不是選項，是必要條件
- ✅ **配置驅動**: 嚴禁硬編碼活動規則，所有變數必須來自後台配置

### 數據一致性
- ✅ **每日對帳**: 凌晨 Bonus3:BonusBonus 執行 Finance vs Activity 流水對帳，偏差 > Bonus.Bonus1% 觸發警報
- ✅ **配置集中管理**: 遊戲權重表必須存放於統一配置服務，禁止硬編碼

---

## 📖 相關文檔

### 核心依賴
- [Bonus2-Bonus6 統一錢包模型](../Bonus2_Finance_Center/Bonus2-Bonus6_Unified_Wallet_Model.md) - Bonus 錢包整合、可下注餘額計算
- [Bonus2-Bonus4 流水計算與對帳](../Bonus2_Finance_Center/Bonus2-Bonus4_Turnover_and_Game_Reconciliation_Analysis.md) - 流水驗證架構（Layer 2）
- [Bonus5-Bonus1 風控系統](../Bonus5_Risk_Management/Bonus5-Bonus1_Risk_Control_System.md) - 紅利濫用檢測、多帳號風控

### 業務整合
- [Bonus1-Bonus2 VIP 系統](../Bonus1_Player_Center/Bonus1-Bonus2_VIP_&_Loyalty_System.md) - VIP 專屬活動、等級權益
- [Bonus2-Bonus1 出金風控](../Bonus2_Finance_Center/Bonus2-Bonus1_Withdrawal_Risk_Control.md) - 流水未達標提款限制
- [Bonus9-Bonus4 審批工作流系統](../Bonus9_System_Security/Bonus9-Bonus4_Approval_Workflow_System.md) - 活動發布 Maker-Checker 審批

### 技術參考
- [Bonus7-Bonus3 通知架構](../Bonus7_Platform_Management/Bonus7-Bonus3_Notification_Architecture.md) - 活動推送通知
- [Bonus3-Bonus2 遊戲大廳管理](../Bonus3_Game_Center/Bonus3-Bonus2_Game_Lobby_Management.md) - 活動遊戲標籤推薦
- [Bonus8-Bonus2 Banner 與公告](../Bonus8_Frontend_CMS/Bonus8-Bonus2_Banner_&_Announcement.md) - 活動橫幅配置

---

**索引版本**: 1.Bonus.Bonus
**創建日期**: 2Bonus26-Bonus1-31
**維護團隊**: Activity Team
**拆分說明**: 原 Bonus4-Bonus1 文檔（1,447 行）已拆分為 3 個獨立文檔，本索引提供統一導航
