# 客服平台需求（Customer Service Platform Requirements）

> **規範來源**: [13-01_CS_Platform_Design.md](../../source-archive/13_Customer_Service/13-01_CS_Platform_Design.md)
> **文件類型**: 業務需求
> **目標讀者**: 產品經理、客戶服務經理
> **相關架構**: [CS_Platform_Architecture.md](../../architecture/13_Customer_Service/CS_Platform_Architecture.md)
> **最後同步**: 2026-02-09

---

## 商業價值（Business Value）

此客服平台提供的關鍵價值：
- **玩家留存與滿意度（Player Retention & Satisfaction）**：玩家 360 度視圖整合財務/投注/風險資料，提供個人化服務，推動 CSAT > 90%、透過快速問題解決（AFR < 5 分鐘）降低流失率 40%
- **營運成本降低（Operational Cost Reduction）**：AI 聊天機器人具備意圖分類（90% 準確度、40% 自動解決率）可降低客服人力成本 50%，同時處理 3 倍工單量（145+ FAQ 文章涵蓋 80% 常見問題）
- **VIP 收入保護（VIP Revenue Protection）**：分層 SLA 管理（Diamond: 5 分鐘 / 2 小時、Platinum: 15 分鐘 / 4 小時）與自動升級機制可防止 VIP 流失，保護平台 70-80% 收入來自前 5% 玩家
- **品質保證（Quality Assurance）**：知識庫具備版本控制、審批流程與多語言支援（6 種語言）可確保客服回應一致性，降低工單重開率 < 5%、提升首次聯繫解決率（FCR）> 80%
- **風險控管（Risk Mitigation）**：即時風險標籤（BONUS_HUNTER、ARBITRAGE、MULTI_ACCOUNT）與 AML 狀態整合，讓 CS 專員能早期識別詐欺，防止每月 $200K-$500K 詐欺損失

---

## 1. 概述（Overview）

客服平台是 iGaming 平台核心營運支援系統，提供全渠道工單管理、AI 輔助回應、玩家 360 度視圖與 SLA 自動化。

### 1.1 核心目標（Core Objectives）

| 目標 | 目標值 | 描述 |
|-----------|--------|-------------|
| 降低回應時間 | AFR < 5 分鐘 | 平均首次回應時間（Average First Response） |
| 提升解決率 | FCR > 80% | 首次聯繫解決率（First Contact Resolution） |
| 提高滿意度 | CSAT > 90% | 客戶滿意度（Customer Satisfaction Score） |
| 優化成本 | 自動化 > 40% | FAQ 與簡單查詢自動化 |

---

## 2. 玩家 360 度視圖（Player 360-Degree View）

### 2.1 資料分類（Data Categories）

CS 專員需要統一視圖整合多資料來源，快速定位問題並提供個人化服務。

#### A. 身份與基本資訊（Identity & Basic Information）
- 玩家 ID、用戶名、註冊時間、KYC 狀態
- 國家、語言偏好、時區
- 聯繫方式（Email、電話、Telegram ID）
- 裝置指紋、最後登入時間

#### B. 財務資料（Financial Data）
- 錢包餘額（現金餘額、優惠餘額、鎖定金額）
- 存款記錄（總存款、最後存款時間、主要支付方式）
- 提款記錄（總提款、待處理提款、提款方式）
- 交易異常標記（退款、拒付、重複交易）

#### C. 投注與遊戲行為（Betting & Gaming Behavior）
- 總投注額、總輸贏、遊戲偏好（老虎機、真人娱樂城、體育）
- 最近 10 筆投注記錄
- 活躍遊戲供應商（Pragmatic Play、Evolution 等）
- 異常投注模式標記（對沖、套利）

#### D. 玩家分群與標籤（Player Segmentation & Tags）
- **生命週期階段**：新玩家（New）/ 活躍（Active）/ 休眠（Dormant）/ 流失（Lost）
- **RFM 分群**：Champions / At Risk / Lost
- **價值標籤**：VIP_WHALE / HIGH_ROLLER / REGULAR
- **風險標籤**：BONUS_HUNTER / ARBITRAGE / MULTI_ACCOUNT

#### E. CS 專屬標籤（CS-Specific Tags）
- `VIP_CONCIERGE` - 需要 VIP 經理接手
- `COMPLAINT_ESCALATED` - 投訴已升級至管理層
- `SELF_EXCLUSION_REQUESTED` - 玩家要求自我排除
- `PAYMENT_ISSUE_HISTORY` - 歷史支付問題記錄

#### F. 工單歷史（Ticket History）
- 最近 20 張工單（標題、狀態、指派專員、解決時間）
- 高頻問題類型（存款失敗、遊戲延遲、優惠查詢）
- 投訴記錄（嚴重程度、補償、解決方案）
- NPS 分數（玩家滿意度評分）

#### G. 風險與合規資訊（Risk & Compliance Information）
- 即時風險分數（0-100）
- AML 狀態：待處理（Pending）/ 通過（Passed）/ 可疑交易標記
- 多帳號關聯（裝置指紋、IP、支付方式）
- 限制：提款限制、優惠停用、投注限制

### 2.2 操作面板（Action Panel）

CS 專員可執行的高頻操作（需要權限控制）：

| 操作 | 描述 | 所需權限 |
|--------|-------------|-------------------|
| **手動加款（Manual Credit）** | 向玩家帳戶添加資金（需提供理由） | Senior+ |
| **強制登出（Kickout）** | 強制玩家登出（用於異常帳號） | Agent+ |
| **重置密碼（Reset Password）** | 發送密碼重置郵件 | Agent+ |
| **解鎖（Unlock）** | 移除登入失敗鎖定 | Agent+ |

---

## 3. 工單系統（Ticket System）

### 3.1 工單類型（Ticket Types）

| 類別 | 子類型 | 優先級 | SLA（首次回應） | 平均處理時間 |
|----------|----------|----------|---------------------|------------------|
| **財務（Financial）** | 存款失敗 | 緊急 | 5 分鐘 | 15 分鐘 |
| | 提款延遲 | 緊急 | 5 分鐘 | 30 分鐘 |
| | 餘額錯誤 | 高 | 15 分鐘 | 1 小時 |
| **遊戲（Gaming）** | 遊戲延遲 | 中等 | 30 分鐘 | 2 小時 |
| | 投注爭議 | 高 | 15 分鐘 | 1 小時 |
| **帳號（Account）** | 登入問題 | 高 | 15 分鐘 | 30 分鐘 |
| | 密碼重置 | 中等 | 30 分鐘 | 5 分鐘 |
| **優惠（Bonus）** | 優惠未到帳 | 中等 | 30 分鐘 | 1 小時 |
| | 投注計算查詢 | 低 | 2 小時 | 30 分鐘 |
| **投訴（Complaint）** | 服務投訴 | 緊急 | 5 分鐘 | 4 小時 |
| | 詐欺指控 | 緊急 | 5 分鐘 | 24 小時 |

### 3.2 工單生命週期（Ticket Lifecycle）

```
新建（New）-> 已指派（Assigned）-> 處理中（In Progress）
                     |
              待玩家回覆（Pending Customer）- 等待玩家回覆
                     |
              待內部處理（Pending Internal）- 等待內部處理
                     |
              已解決（Resolved）-> 已關閉（Closed）
                     |
              重新開啟（Reopened）
```

### 3.3 自動關閉規則（Auto-Close Rules）

- 狀態 = `Pending Customer` 且 72 小時無回覆 → 自動關閉
- 狀態 = `Resolved` 且 24 小時玩家無異議 → 自動關閉

### 3.4 遊戲錯誤處理 SOP（Game Error Handling SOP）

1. CS 專員使用 Round ID 呼叫遊戲供應商交易狀態查詢
2. 若供應商回傳 `Status: Settled, Win: 0` → 回覆玩家「無獲勝」
3. 若供應商回傳 `Record Not Found` → 升級工單至技術團隊進行「掉單」調查

---

## 4. 知識庫（Knowledge Base）

### 4.1 內容分類（Content Categories）

#### A. FAQ（常見問題 - 145+ 篇文章）
- **存款與提款（Deposits & Withdrawals）**（50+ 篇文章）
- **優惠與促銷（Bonuses & Promotions）**（40+ 篇文章）
- **遊戲問題（Game Issues）**（30+ 篇文章）
- **帳號安全（Account Security）**（25+ 篇文章）

#### B. 遊戲指南（Game Guides）
- 老虎機規則（100+ 款遊戲）
- 真人娱樂城規則（百家樂、輪盤、骰寶）
- 體育博弈指南（讓分、大小、串關）

#### C. 合規政策（Compliance Policies）
- 隱私政策（GDPR）
- 責任博弈（自我排除、存款限制、冷靜期）
- AML（KYC 要求、資金來源）
- 條款與條件

#### D. 內部營運手冊（Internal Operations Manual - 僅 CS）
- 財務問題處理程序
- 遊戲爭議處理
- 升級與補償標準
- 詐欺偵測程序

### 4.2 版本控制（Version Control）
- 每篇文章保留完整版本歷史
- 顯示最後更新時間與更新者
- 支援版本比較（diff view）
- 審批流程：建立 → 同儕審查 → Team Lead 審批 → 發布

### 4.3 多語言支援（Multi-Language Support）

| 語言 | 狀態 |
|----------|--------|
| English | 主要版本 |
| Traditional Chinese（繁體中文） | 翻譯 |
| Simplified Chinese（簡體中文） | 翻譯 |
| Thai（泰文） | 翻譯 |
| Vietnamese（越南文） | 翻譯 |
| Indonesian（印尼文） | 翻譯 |

### 4.4 權限控制（Permission Control）

| 角色 | 可見範圍 | 編輯 | 審批 |
|------|--------------|------|----------|
| **玩家（Player）** | FAQ + 遊戲指南 + 合規 | 否 | 否 |
| **CS 專員（CS Agent）** | 全部（包含內部手冊） | 僅草稿 | 否 |
| **Team Lead** | 全部 | 草稿 + 編輯 | 審批 |
| **Manager** | 全部 | 全部 | 最終審批 |

---

## 5. AI 聊天機器人（AI Chatbot）

### 5.1 意圖分類（Intent Classification）

| 意圖 | 範例 | 信心閾值 | 處理方式 |
|--------|---------|---------------------|----------|
| `DEPOSIT_ISSUE` | "我的存款沒到" | > 0.8 | 自動回覆 + 建立工單 |
| `WITHDRAWAL_QUERY` | "提款要多久？" | > 0.9 | 回傳 FAQ 文章 |
| `BONUS_INQUIRY` | "我的優惠在哪？" | > 0.85 | 檢查 Bonus Wallet + 回覆 |
| `GAME_MALFUNCTION` | "遊戲卡住了" | > 0.75 | 建立工單 + 轉人工 |
| `PASSWORD_RESET` | "忘記密碼" | > 0.95 | 發送重置連結 |
| `KYC_VERIFICATION` | "如何完成 KYC？" | > 0.9 | 回傳 KYC 指南 |
| `COMPLAINT` | "我要投訴" | > 0.7 | 立即轉人工 |

### 5.2 自動轉人工條件（Auto-Transfer to Human Conditions）

- 意圖信心度 < 0.7
- 玩家明確要求人工專員
- 敏感問題（投訴、詐欺指控、帳號異常）
- 連續 3 次自動回覆未解決
- VIP Diamond 玩家（自動轉 VIP 經理）

### 5.3 品質指標（Quality Metrics）

| 指標 | 目標 |
|--------|--------|
| 意圖分類準確度 | > 90% |
| 自動回覆解決率 | > 40% |
| 玩家滿意度（AI 回覆） | > 75% |

---

## 6. SLA 管理（SLA Management）

### 6.1 依 VIP 等級的 SLA 標準（SLA Levels by VIP Tier）

| VIP 等級 | 首次回應 SLA | 解決 SLA | 升級條件 |
|-----------|-------------------|----------------|---------------------|
| Diamond | 5 分鐘 | 2 小時 | 超過 SLA 50% → Manager |
| Platinum | 15 分鐘 | 4 小時 | 超過 SLA 50% → Senior |
| Gold | 30 分鐘 | 8 小時 | 超過 SLA 75% → Senior |
| Silver/Bronze | 2 小時 | 24 小時 | 超過 SLA 100% → Team Lead |

### 6.2 升級規則（Escalation Rules）

| 條件 | 升級動作 |
|-----------|------------------|
| 超過 SLA 50%，無處理記錄 | 升級至 Team Lead |
| 超過 SLA 100%，無解決方案 | 升級至 Manager |
| 玩家投訴 + Diamond VIP | 立即升級至 VIP Manager |
| 工單重開 3+ 次 | 升級至 Senior 專員 |

### 6.3 告警渠道（Alert Channels）

- SLA 超過 50%：Slack 通知專員 + Team Lead
- SLA 超過 75%：Email + SMS 給 Manager
- SLA 達成率 < 90%（連續 2 小時）：緊急告警

---

## 7. 效能指標（Effectiveness Metrics - KPIs）

| 指標 | 定義 | 目標 | 計算公式 |
|--------|-----------|--------|---------|
| **FCR** | 首次聯繫解決率（First Contact Resolution） | > 80% | 首次解決工單 / 總工單數 |
| **AHT** | 平均處理時間（Average Handling Time） | < 15 分鐘 | 總處理時間 / 工單數 |
| **CSAT** | 客戶滿意度（Customer Satisfaction） | > 90% | 滿意工單 / 評分工單數 |
| **SLA Achievement** | SLA 合規率 | > 95% | 符合 SLA 工單 / 總工單數 |
| **Reopen Rate** | 工單重開率 | < 5% | 重開工單 / 已關閉工單數 |

---

## 8. 品質保證（Quality Assurance）

### 8.1 隨機檢查（Random Inspection）
- 每日隨機檢查 10% 的工單
- 評分維度：專業度、同理心、效率

### 8.2 績效排名（Performance Ranking）
- 每週績效排名顯示在 CS 系統首頁
- 月度 Top 3 獲得獎金（$200 / $150 / $100）
- 連續 3 個月進入 Top 10 可晉升 Senior

---

## 相關文件（Related Documents）

- [CS_Operations_Requirements.md](CS_Operations_Requirements.md) - CS 營運需求
- [CS_Platform_Architecture.md](../../architecture/13_Customer_Service/CS_Platform_Architecture.md) - 技術架構

---

**Return**: [Customer Service Module](../../source-archive/13_Customer_Service/README.md) | [iGaming Home](../../source-archive/README.md)
