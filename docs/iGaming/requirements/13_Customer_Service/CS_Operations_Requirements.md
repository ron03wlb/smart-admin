# 客服營運需求（Customer Service Operations Requirements）

> **Canonical Source**: [13-02_Customer_Service_Operations.md](../../source-archive/13_Customer_Service/13-02_Customer_Service_Operations.md)
> **View Type**: Business Requirements
> **Target Audience**: Product Managers, Customer Service Managers, Operations Directors
> **Related Architecture**: [CS_Operations_Architecture.md](../../architecture/13_Customer_Service/CS_Operations_Architecture.md)
> **Last Synced**: 2026-02-09

---

## 商業價值（Business Value）

此客服營運框架提供的關鍵價值：
- **玩家留存（Player Retention）**：首次聯繫解決率（First Contact Resolution, FCR）> 80%、客戶滿意度（Customer Satisfaction, CSAT）> 90%，可減少流失率 35-45%，直接影響玩家終身價值與重複存款
- **營運效率（Operational Efficiency）**：多渠道整合與 AI 自動化（40% 即時聊天、30% Telegram、25% WhatsApp）可降低客服人力成本 50%，同時處理 3 倍工單量
- **VIP 體驗（VIP Experience）**：Diamond/Platinum 等級專屬路由與即時電話支援，可提升 VIP 存款頻率 25%、留存率 60%
- **SLA 合規（SLA Compliance）**：即時監控與自動升級機制（Level 1 @ 50%、Level 2 @ 75%、Level 3 @ 90%）可維持 > 95% SLA 達成率，避免玩家挫折與監管投訴
- **人力優化（Workforce Optimization）**：技能路由演算法（Agent Level × Language Match × Load Inverse）可降低平均處理時間（Average Handle Time, AHT）30%、提升專員生產力 40%
- **責任博弈（Responsible Gambling）**：整合自我排除（Self-Exclusion）處理與問題博弈偵測，可避免監管罰款、保護品牌聲譽，並提供即時介入

---

## 1. 概述（Overview）

客服營運定義了運作 CS 平台的營運框架，包括工單路由演算法、多渠道整合策略、人力管理、績效分析與品質保證流程。

---

## 2. 工單路由與指派（Ticket Routing & Assignment）

### 2.1 路由策略（Routing Strategy - 優先順序）

1. **VIP 玩家優先路由**
   - Diamond/Platinum → 專屬 VIP 經理
   - Gold → VIP 團隊（輪詢分配）

2. **技能匹配（Skill Matching）**
   - 金融工單 → 金融專員
   - 遊戲技術問題 → 技術支援
   - 優惠爭議 → 優惠專員
   - 投訴 → Team Lead/Manager

3. **語言匹配（Language Matching）**
   - 玩家語言偏好 = 專員語言能力

4. **工作量平衡（Workload Balancing）**
   - 指派給當前工單數最少的專員
   - 防止任何專員超過 20 張工單

### 2.2 負載平衡演算法（Load Balancing Algorithm）

```
Weight = (Agent Level Weight) x (Language Match Score) x (Current Ticket Load Inverse)

Agent Level Weight:
- Senior: 1.5
- Regular: 1.0
- Junior: 0.7

Language Match Score:
- Native match: 1.0
- Fluent: 0.8
- Basic: 0.5

Current Ticket Load Inverse:
- 1 / (Current ticket count + 1)
```

---

## 3. 多渠道整合（Multi-Channel Integration）

### 3.1 支援渠道（Supported Channels）

| 渠道 | 優先級 | SLA | 自動化程度 |
|---------|----------|-----|-----------------|
| **即時聊天（Live Chat）** | 最高 | 5 分鐘 | AI 自動回覆 40% |
| **Telegram** | 高 | 15 分鐘 | AI 自動回覆 30% |
| **電子郵件（Email）** | 中等 | 2 小時 | 自動分類 100% |
| **WhatsApp** | 中等 | 15 分鐘 | AI 自動回覆 25% |
| **電話（Phone）** | 僅 VIP | 立即 | 0%（人工） |

### 3.2 即時聊天需求（Live Chat Requirements）

- 即時雙向通訊（透過即時訊息）
- 顯示「正在輸入...」指示器
- 專員可同時處理 3-5 個對話（依經驗等級而定）
- 快速回覆範本（50+ 預設回覆）
- 檔案傳輸（截圖、支付憑證）
- 對話轉移（轉給其他專員或升級）

### 3.3 上下文傳遞（Context Passing）

當玩家發起對話時，自動傳遞以下資訊給 CS 專員：
- 玩家 ID 與 VIP 等級（決定優先級）
- 當前頁面（知道玩家卡在哪裡）
- 錢包餘額（快速判斷存款問題）
- 投注進度（提款問題關鍵資訊）

### 3.4 統一對話歷史（Unified Conversation History）

- CS 專員打開玩家檔案時，自動顯示所有渠道歷史
- 避免玩家重複描述問題
- 跨渠道工單關聯

### 3.5 渠道切換（Channel Switching - 敏感資料）

當玩家在即時聊天中分享敏感資訊（銀行帳號）時：
1. Bot/專員偵測到敏感資訊
2. 自動提示：「為了安全，建議透過電子郵件提供此資訊」
3. 系統自動建立電子郵件工單，並關聯到當前對話
4. 玩家回覆電子郵件，專員在同一工單中繼續處理

---

## 4. SLA 自動監控（SLA Automated Monitoring）

### 4.1 即時儀表板（Real-Time Dashboard）

顯示關鍵指標：
- 當前待處理工單（依優先級 P0/P1/P2/P3）
- SLA 達成率（最近 24 小時）
- 超時工單數量
- SLA 警告數量

### 4.2 自動告警機制（Automated Alert Mechanism）

| 通知等級 | 觸發條件 | 通知渠道 |
|-------------------|-------------------|---------------------|
| **Level 1** | SLA 超過 50%，無處理動作 | Slack: 專員 + Team Lead |
| **Level 2** | SLA 超過 75% | Email + SMS: Manager |
| **Level 3** | SLA 達成率 < 90%，持續 2 小時 | 緊急告警：所有渠道 |

### 4.3 超時偵測（Timeout Detection）

- 排程任務每 5 分鐘執行一次
- 檢查所有開放工單的 SLA 截止時間
- 依升級規則自動升級

---

## 5. 專員績效分析（Agent Performance Analytics）

### 5.1 個人 KPI（Individual KPIs）

| 指標 | 定義 | 目標 | 計算公式 |
|--------|-----------|--------|---------|
| **FCR** | 首次聯繫解決率（First Contact Resolution） | > 70% | 首次解決 / 總工單數 |
| **AHT** | 平均處理時間（Average Handle Time） | < 10 分鐘（聊天）、< 15 分鐘（工單） | 總時間 / 工單數 |
| **CSAT** | 客戶滿意度（Customer Satisfaction Score） | 平均 > 4.0（1-5 分制） | 玩家評分 |
| **Ticket Volume** | 日工單處理量 | 監控 | 每日處理總數 |

### 5.2 每日報告內容（Daily Report Content）

- 線上專員數量
- 處理工單數
- 平均回應時間
- SLA 達成率
- 表現最佳專員（FCR、CSAT、處理量）
- 改善領域

### 5.3 績效排名系統（Performance Ranking System）

- 每週績效 Top 5 顯示在 CS 系統首頁
- 月度 Top 3 獲得獎金（$200 / $150 / $100）
- 連續 3 個月進入 Top 10 → 晉升 Senior 資格

---

## 6. 品質保證流程（Quality Assurance Process）

### 6.1 隨機檢查機制（Random Inspection Mechanism）

- 每日隨機檢查 10% 的工單
- 評分維度：
  - **專業度（Professionalism）**：準確引用政策
  - **同理心（Empathy）**：友善且理解的語氣
  - **效率（Efficiency）**：解決速度

### 6.2 持續改進（Continuous Improvement）

- 定期檢視低分工單
- 定期組織案例分享會
- 從品質評分中識別培訓需求

---

## 7. 人力管理（Workforce Management）

### 7.1 排班規則（Scheduling Rules）

| 班次類型 | 時段 | 專員人數 | 重點 |
|-----------|-------|-------------|-------|
| 尖峰時段（Peak hours） | 18:00-02:00 UTC+8 | 最大人力配置 | 遊戲高峰期 |
| 標準時段（Standard hours） | 10:00-18:00 UTC+8 | 標準人力配置 | 正常營運 |
| 離峰時段（Off-peak hours） | 02:00-10:00 UTC+8 | 最小人力配置 | AI 處理大多數查詢 |

### 7.2 專員技能矩陣（Agent Skill Matrix）

| 技能標籤 | 描述 | 培訓要求 |
|-----------|------------|---------------------|
| `payment_expert` | 存款/提款問題 | 支付系統培訓 |
| `game_integration` | 遊戲技術問題 | 遊戲供應商培訓 |
| `bonus_specialist` | 優惠與投注問題 | 促銷規則培訓 |
| `compliance_officer` | 投訴與監管 | 合規認證 |
| `vip_manager` | VIP 玩家處理 | VIP 計畫培訓 |

---

## 8. 責任博弈整合（Responsible Gambling Integration）

客服在責任博弈中扮演關鍵角色：

### 8.1 自我排除處理（Self-Exclusion Handling）

- CS 專員必須接受自我排除請求處理培訓
- 請求必須立即處理（不得延遲）
- 確認前必須告知玩家後果
- CS 不得鼓勵玩家重新考慮自我排除

### 8.2 問題博弈跡象（Problem Gambling Signs）

CS 專員應識別並升級以下情況：
- 玩家表達博弈損失的痛苦
- 請求協助控制博弈行為
- 提及財務困難
- 因損失而顯示攻擊性行為
- 過度聯繫 CS 詢問優惠/投注問題

---

## 9. 效能指標（Effectiveness Metrics）

| 指標 | 目標 | 描述 |
|--------|--------|-------------|
| SLA 達成率（SLA achievement rate） | > 95% | 整體 SLA 合規 |
| 首次聯繫解決率（First Contact Resolution） | > 80% | 首次聯繫解決的問題 |
| 客戶滿意度（Customer Satisfaction） | > 90% | CSAT 分數 |
| 專員利用率（Agent utilization） | 70-85% | 最佳專員工作量 |
| 工單重開率（Ticket reopen rate） | < 5% | 解決品質 |
| AI 自動化率（AI automation rate） | > 40% | AI 解決的工單 |
| 平均處理時間（Average handling time） | < 15 分鐘 | 解決時間 |
| 知識庫搜尋成功率（Knowledge base search success） | > 70% | 自助服務效能 |

---

## 成功指標（Success Metrics）

| 指標 | 目標 | 衡量方式 |
|--------|--------|-------------|
| VIP 優先路由準確度 | 100% | Diamond/Platinum 玩家路由到 VIP 經理/團隊 |
| 技能匹配準確度 | > 95% | 工單路由到具有匹配技能標籤的專員 |
| 語言匹配率 | > 98% | 工單指派給說玩家語言的專員 |
| 負載平衡效率 | 最多 20 張工單/專員 | 尖峰時段無專員超過工單上限 |
| 多渠道上下文保留 | 100% | CS 專員可看到完整跨渠道對話歷史 |
| SLA 告警回應時間 | < 5 分鐘 | Level 1 告警到專員確認的時間 |
| 品質檢查覆蓋率 | 每日 10% | 達成隨機檢查配額 |
| 責任博弈回應 | 100% 立即 | 自我排除請求無延遲處理 |

---

## 相關文件（Related Documents）

- [CS_Platform_Requirements.md](CS_Platform_Requirements.md) - CS 平台需求
- [CS_Operations_Architecture.md](../../architecture/13_Customer_Service/CS_Operations_Architecture.md) - 技術架構

---

**Return**: [Customer Service Module](../../source-archive/13_Customer_Service/README.md) | [iGaming Home](../../source-archive/README.md)
