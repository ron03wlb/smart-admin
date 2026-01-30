# 11-01 客服中台設計 (CS Platform Design)

## 1. 系統概述
客服是平台與玩家的第一線接觸點。本系統旨在提供 "全視角 (Single View)" 的玩家數據，賦能客服快速解決問題 (First Contact Resolution)。

## 2. 玩家 360 全視圖 (Player 360 View)

### 2.1 核心儀表板
整合以下模組至單一頁面：
1.  **Identity**: 
    - 基本資料 (Name/Phone/Email)。
    - KYC 狀態 (Approved/Pending/Rejected)。
    - 風控標籤 (Risk Tag: `Normal`, `HighRisk`, `BonusAbuser`)。
2.  **Wallet**: 
    - 總資產、可提現餘額、紅利餘額。
    - **流水進度 (Wagering Progress)**: 直觀顯示 "還差 $500 流水可提款"。
3.  **Activity**:
    - 最近 5 筆存款/提款狀態。
    - 最近 10 筆下注記錄 (顯示 Game Provider 與 Round ID)。
    - 當前參與的優惠活動。

### 2.2 操作面板 (Action Panel)
客服可執行的高頻操作（需權限控制）：
*   **補單 (Manual Credit)**: 手動給玩家加錢 (需附原因)。
*   **踢線 (Kickout)**: 強制玩家下線 (針對異常帳號)。
*   **重置密碼 (Reset Password)**: 發送重置郵件。
*   **解鎖 (Unlock)**: 解除登入失敗鎖定。

---

## 3. 工單系統 (Ticket System)

### 3.1 工單類型
1.  **Deposit Issue**: 存款未到帳 (需上傳憑證)。
2.  **Withdraw Issue**: 提款被拒/延遲。
3.  **Game Error**: 遊戲卡頓/派彩錯誤 (需提供 Round ID)。
    - **SOP**: 客服前端調用 `GameIntegration.CheckTransactionStatus(round_id)`。
    - **結果**:
      - 若 GP 返回 `Status: Settled, Win: 0` -> 回覆玩家 "未中獎"。
      - 若 GP 返回 `Record Not Found` -> 升級工單至技術部查核 "掉單"。
4.  **Bonus Inquiry**: 紅利領取問題。
5.  **Compliance**: 投訴/自我排除申請。

### 3.2 狀態流轉
`New` -> `Assigned` (分配給二線) -> `Investigating` (技術查詢中) -> `Resolved` -> `Closed` (玩家確認)。

### 3.3 SLA 管理
*   **P1 (Critical)**: VIP 存款問題 -> 響應時間 < 5分鐘。
*   **P2 (Normal)**: 普通遊戲諮詢 -> 響應時間 < 30分鐘。
*   **Escalation**: 超時未處理自動升級通知主管。

---

## 4. 知識庫管理系統 (Knowledge Base)

### 4.1 知識庫架構
**目標**：建立自助服務能力，減少重複性工單。

**內容分類**：
1. **FAQ（常見問題）**：
   - 如何存款/提款？
   - 紅利流水怎麼計算？
   - 為什麼我的提款被拒絕？
2. **遊戲指南**：
   - 各遊戲玩法說明
   - 新手教程
3. **合規政策**：
   - 責任博彩政策
   - 隱私條款
   - 自我排除流程

### 4.2 知識庫功能
**搜尋功能**：
- 使用 Elasticsearch 實現全文檢索
- 支持多語言搜尋（基於 08-05 多語言系統）
- 智能推薦相關文章

**內容管理**：
- CMS 後台編輯知識庫文章
- 版本控制：保留文章修改歷史
- 發佈審批工作流（參考 09-04 審批工作流系統）

**有效性追蹤**：

---

## 5. 智能客服機器人 (AI Chatbot)

### 5.1 機器人架構
**技術選型**：
- **NLU引擎**：Dialogflow / Rasa / Microsoft LUIS
- **整合方式**：WebSocket 即時對話
- **後備機制**：無法理解時自動轉人工客服

### 5.2 對話流程設計

**Intent 識別範例**：
```yaml
Intent: deposit_not_received
  - "我存款沒到帳"
  - "錢已經轉了但是沒收到"
  - "deposit not credited"

Response:
  1. 請玩家提供交易ID或憑證截圖
  2. 查詢支付系統 (02-02 支付網關)
  3. 若找不到記錄 -> 創建工單並轉人工
```

**多輪對話範例**：
```
Bot: 您好，請問需要什麼幫助？
User: 我想提款
Bot: 請問您是遇到以下哪種問題？
     1. 不知道如何提款
     2. 提款被拒絕
     3. 提款金額未到帳
User: 2
Bot: 您的提款被拒原因是：【餘額不足完成流水要求】
     當前流水進度：$4,500 / $5,000（還差 $500）
     建議：請繼續遊戲完成流水後再提款
```

### 5.3 機器人訓練與優化
**持續學習**：
- 收集「轉人工」的對話記錄
- 分析未識別的 Intent
- 每月更新訓練模型

**A/B 測試**：
- 測試不同回覆話術的滿意度
- 優化轉人工的觸發條件

---

## 6. 工單路由算法 (Ticket Routing)

### 6.1 自動分配規則

**優先級計算公式**：

**路由規則表**：
| 工單類型 | VIP等級 | 分配規則 |
|---------|--------|---------|
| Deposit Issue | VIP 3+ | → 優先隊列 → VIP專屬客服 |
| Game Error | 任意 | → 技術支持隊列 |
| Bonus Inquiry | 任意 | → 活動專員隊列 |
| Compliance | 任意 | → 合規團隊（需主管審核）|

### 6.2 負載均衡策略

**Round-Robin + 負載感知**：

**技能匹配**：
- 存款問題 → 需要 `payment_expert` 技能標籤
- 遊戲技術問題 → 需要 `game_integration` 技能標籤

---

## 7. SLA 自動化監控

### 7.1 SLA 指標定義

| 優先級 | 首次響應時間 | 解決時間 | 適用場景 |
|--------|------------|---------|---------|
| **P0 (Critical)** | < 5分鐘 | < 2小時 | VIP存款未到帳、重大遊戲故障 |
| **P1 (High)** | < 15分鐘 | < 4小時 | 普通存款問題、提款被拒 |
| **P2 (Normal)** | < 30分鐘 | < 24小時 | 一般諮詢、紅利問題 |
| **P3 (Low)** | < 2小時 | < 72小時 | 功能建議、非緊急問題 |

### 7.2 自動升級機制

**超時檢測定時任務**（每5分鐘執行）：

**通知機制**：
- **Level 1 升級**：通知團隊主管（Email + Slack）
- **Level 2 升級**：通知客服總監
- **Level 3 升級**：進入危機管理流程

### 7.3 SLA 儀表板

**實時監控指標**：
```
當前待處理工單：125
  - P0: 2（超時預警：1）
  - P1: 18（平均等待：8分鐘）
  - P2: 85
  - P3: 20

今日 SLA 達成率：
  - P0: 95%（目標 >90%）
  - P1: 88%（目標 >85%）
  - P2: 92%（目標 >80%）
```

---

## 8. 客服績效報表 (Performance Analytics)

### 8.1 個人績效指標 (KPI)

**核心指標**：
1. **First Contact Resolution (FCR)**：
   - 定義：首次接觸即解決問題的比率
   - 公式：`FCR = 首次解決工單數 / 總工單數`
   - 目標：> 70%

2. **Average Handle Time (AHT)**：
   - 平均處理時間 = (總通話時長 + 總後續處理時長) / 工單數
   - 目標：< 10分鐘（Live Chat）、< 15分鐘（工單）

3. **Customer Satisfaction Score (CSAT)**：
   - 玩家滿意度評分（1-5星）
   - 目標：平均 > 4.0

### 8.2 績效報表設計

**日報表範例**：
```markdown
# 客服日報 - 2026-01-27

## 團隊概況
- 在線客服：12人
- 處理工單：156筆
- 平均響應時間：8分鐘
- SLA達成率：91%

## Top 表現客服
1. Alice (agent_001) - FCR: 85%, CSAT: 4.7, 處理量: 25
2. Bob (agent_002) - FCR: 78%, CSAT: 4.5, 處理量: 22
3. Carol (agent_003) - FCR: 82%, CSAT: 4.6, 處理量: 20

## 需改進
- 存款問題平均處理時間偏高（18分鐘），需加強培訓
```

### 8.3 質量檢查 (QA)

**抽查機制**：
- 每日隨機抽查 10% 工單
- 評分維度：
  - 專業性（是否準確引用政策）
  - 同理心（語氣是否友善）
  - 效率（是否快速解決）

**改進計劃**：
- 低分工單定期回顧
- 組織案例分享會議

---

## 9. 多渠道整合 (Omnichannel Integration)

### 9.1 支持渠道

| 渠道 | 使用場景 | 技術方案 |
|------|---------|---------|
| **Live Chat** | 即時諮詢 | Zendesk / Intercom |
| **Email** | 非緊急問題、正式回覆 | SendGrid / AWS SES |
| **SMS** | 驗證碼、提款通知 | Twilio / Vonage |
| **電話** | VIP專線 | Call Center 系統 |
| **社交媒體** | Facebook / Telegram | 整合社群管理工具 |

### 9.2 統一對話歷史

**跨渠道追蹤**：

**360度對話視圖**：
- 客服打開玩家檔案時，自動顯示所有渠道的歷史對話
- 避免玩家重複描述問題

### 9.3 Context Switching（渠道切換）

**場景**：玩家在 Live Chat 中提供敏感資料（銀行帳號）

**處理流程**：
1. 機器人/客服檢測到敏感信息
2. 自動提示：「建議您透過 Email 提供此資訊，以確保安全」
3. 系統自動創建 Email 工單並關聯當前對話
4. 玩家回覆 Email 後，客服在同一工單中繼續處理

---

## 10. Live Chat 集成

### 10.1 技術集成
*   **Vendor**: Zendesk / LiveChat / Intercom。
*   **Context Passing**: 當玩家發起對話時，自動將 `player_id` 與 `current_url` 傳送給客服端，讓客服知道 "他是誰" 以及 "他在看什麼頁面"。

### 10.2 即時數據推送
**WebSocket 集成**：
```javascript
// 前端發起對話時傳遞上下文
const livechat = new LiveChatClient({
  playerId: currentUser.id,
  playerVIP: currentUser.vipLevel,
  currentPage: window.location.pathname,
  walletBalance: currentUser.wallet.playableBalance,
  wageringProgress: currentUser.bonusWageringProgress
});
```

**客服端自動顯示**：
- 玩家 VIP 等級（決定優先級）
- 當前頁面（知道玩家在哪裡卡住）
- 錢包餘額（快速判斷存款問題）
- 流水進度（提款問題的關鍵資訊）

---

## 📚 相關文檔

### 業務邏輯參考
- [01-01 玩家賬戶系統](../01_Player_Center/01-01_Player_Account_System.md) - 玩家資料來源
- [02-06 統一錢包模型](../02_Finance_Center/02-06_Unified_Wallet_Model.md) - 錢包數據展示
- [04-01 活動系統設計](../04_Activity_Center/04-01_Activity_System_Design.md) - 紅利問題處理

### 技術架構參考
- [08-05 多語言系統](../08_Frontend_CMS/08-05_Localization_System.md) - 知識庫多語言支持
- [09-02 審計日誌系統](../09_System_Security/09-02_Audit_Log_System.md) - 客服操作審計
- [07-03 通知架構](../07_Platform_Management/07-03_Notification_Architecture.md) - 多渠道通知整合

---

**文檔版本**: 1.1.0
**最後更新**: 2026-01-27
**維護團隊**: Customer Service Team & Product Team
