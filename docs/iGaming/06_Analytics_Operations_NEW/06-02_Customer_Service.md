# 06-02 客戶服務平台設計 (Customer Service Platform Design)

> **MIGRATED FROM**: 11-01_CS_Platform_Design.md (Phase 4 Module Merge)
> **Version**: 2.0.0
> **Last Updated**: 2026-02-04

## 📋 目錄

1. [系統概述](#1-系統概述)
2. [Player 360° 視圖](#2-player-360-視圖)
3. [工單系統 (Ticket System)](#3-工單系統-ticket-system)
4. [知識庫系統 (Knowledge Base)](#4-知識庫系統-knowledge-base)
5. [AI 客服機器人](#5-ai-客服機器人)
6. [工單路由與分配](#6-工單路由與分配)
7. [SLA 管理與自動化](#7-sla-管理與自動化)
8. [客服績效分析](#8-客服績效分析)
9. [多渠道整合](#9-多渠道整合)
10. [SmartAdmin 架構映射](#10-smartadmin-架構映射)
11. [相關文檔](#11-相關文檔)

---

## 1. 系統概述

客戶服務平台是 iGaming 運營的核心支撐系統，提供全渠道工單管理、AI 輔助回覆、玩家 360° 視圖、SLA 自動化等功能。

**核心目標**：
- **降低響應時間**: 平均首次響應時間 (AFR) < 5 分鐘
- **提升解決率**: 首次聯繫解決率 (FCR) > 80%
- **增強滿意度**: 客戶滿意度 (CSAT) > 90%
- **優化成本**: 自動化處理率 > 40%（FAQ、簡單查詢）

**系統能力**：
- 統一工單系統（Email、Live Chat、Telegram、WhatsApp）
- 玩家 360° 數據視圖（投注、存提款、風控標籤）
- AI 智能分類與自動回覆
- 知識庫管理（FAQ、遊戲指南、合規政策）
- SLA 自動化與告警

---

## 2. Player 360° 視圖

### 2.1 視圖設計原則

<!-- SSOT Marker: 玩家分群邏輯詳見 01-03 §2-4 -->

客服代表需要完整的玩家信息以快速定位問題並提供個性化服務。Player 360° 視圖整合多個數據源，提供統一視圖。

**數據分類**：

#### A. 身份與基本信息
- 玩家 ID、用戶名、註冊時間、KYC 狀態
- 國家、語言偏好、時區
- 聯繫方式（Email、手機、Telegram ID）
- 設備指紋、最後登入時間

#### B. 財務數據
- 錢包餘額（現金餘額、紅利餘額、鎖定金額）
- 存款記錄（總存款、最後存款時間、主要支付方式）
- 提款記錄（總提款、待處理提款、提款方式）
- 交易異常標記（拒付、退款、重複交易）

#### C. 投注與遊戲行為
- 總投注額、總輸贏、遊戲偏好（老虎機、真人、體育）
- 最近 10 筆投注記錄
- 活躍遊戲供應商（Pragmatic Play、Evolution 等）
- 異常投注模式標記（對沖、套利）

#### D. 玩家分群與標籤

💡 **SSOT Marker**: 玩家分群詳細算法參見 **[01-03 玩家分群與標籤](../01_Player_Center/01-03_Player_Segmentation.md)**

**快速分群視圖**（顯示在 CS 界面頂部）：
- **生命週期階段**: 新用戶 / 活躍 / 沉睡 / 流失（定義參見 `[01-03 §2.1]`）
- **RFM 分群**: Champions / At Risk / Lost（計算邏輯參見 `[01-03 §3.2]`）
- **價值標籤**: VIP_WHALE / HIGH_ROLLER / REGULAR（標準參見 `[01-03 §4.1]`）
- **風控標籤**: BONUS_HUNTER / ARBITRAGE / MULTI_ACCOUNT（檢測規則參見 `[01-03 §5]`）

**CS 專屬標籤**（客服系統維護）：
- `VIP_CONCIERGE` - 需 VIP 經理接管
- `COMPLAINT_ESCALATED` - 投訴已升級至管理層
- `SELF_EXCLUSION_REQUESTED` - 玩家要求自我排除
- `PAYMENT_ISSUE_HISTORY` - 歷史支付問題記錄

#### E. 工單歷史
- 最近 20 張工單（標題、狀態、處理客服、解決時間）
- 高頻問題類型（存款失敗、遊戲卡頓、紅利查詢）
- 投訴記錄（嚴重度、是否賠償、解決方案）
- NPS 評分（玩家對客服的滿意度評分）

#### F. 風控與合規信息

💡 **SSOT Marker**: 風險評分邏輯詳見 **[05-01 風控系統](../04_Risk_Control/04-01_Risk_Framework.md §4.2)**

- **風險評分**: 實時風險分數（0-100）
- **AML 狀態**: 待審核 / 通過 / 可疑交易標記
- **多賬戶關聯**: 關聯玩家列表（設備指紋、IP、支付方式）
- **限制措施**: 提款限額、紅利禁用、投注限制

### 2.2 API 整合與數據來源

**數據來源整合**：
```
Player 360° View
├─ Player Service      - 基本信息、KYC 狀態
├─ Wallet Service      - 錢包餘額、交易記錄
├─ Bet Service         - 投注歷史、遊戲偏好
├─ Segmentation Service - 玩家分群、RFM 標籤（參見 01-03）
├─ Risk Service        - 風險評分、風控標籤
└─ Ticket Service      - 工單歷史、投訴記錄
```

**響應時間優化**：
- Redis 緩存（5 分鐘 TTL）
- 異步加載非關鍵數據（投注歷史、工單歷史）
- GraphQL 按需查詢（避免過度獲取）

---

## 3. 工單系統 (Ticket System)

### 3.1 工單類型分類

| 類型 | 子類型 | 優先級 | SLA (首次響應) | 平均處理時間 |
|------|--------|--------|---------------|-------------|
| **財務類** | 存款失敗 | 緊急 | 5 分鐘 | 15 分鐘 |
| | 提款延遲 | 緊急 | 5 分鐘 | 30 分鐘 |
| | 餘額錯誤 | 高 | 15 分鐘 | 1 小時 |
| **遊戲類** | 遊戲卡頓 | 中 | 30 分鐘 | 2 小時 |
| | 投注爭議 | 高 | 15 分鐘 | 1 小時 |
| **帳戶類** | 登入問題 | 高 | 15 分鐘 | 30 分鐘 |
| | 密碼重置 | 中 | 30 分鐘 | 5 分鐘 |
| **紅利類** | 紅利未到賬 | 中 | 30 分鐘 | 1 小時 |
| | 流水計算查詢 | 低 | 2 小時 | 30 分鐘 |
| **投訴類** | 服務投訴 | 緊急 | 5 分鐘 | 4 小時 |
| | 欺詐指控 | 緊急 | 5 分鐘 | 24 小時 |

### 3.2 工單生命週期

```
新建 (New) → 已分配 (Assigned) → 處理中 (In Progress)
                                    ↓
                           等待玩家回覆 (Pending Customer)
                                    ↓
                           等待內部處理 (Pending Internal)
                                    ↓
                           已解決 (Resolved) → 已關閉 (Closed)
                                    ↓
                           重新開啟 (Reopened)
```

**自動關閉規則**：
- 狀態 = `Pending Customer` 且 72 小時無回覆 → 自動關閉
- 狀態 = `Resolved` 且 24 小時無玩家異議 → 自動關閉

### 3.3 SLA 管理

**SLA 等級定義**（基於玩家 VIP 等級）：

| VIP 等級 | 首次響應 SLA | 解決 SLA | 升級條件 |
|---------|-------------|---------|---------|
| Diamond | 5 分鐘 | 2 小時 | 超過 SLA 50% → 升級至 Manager |
| Platinum | 15 分鐘 | 4 小時 | 超過 SLA 50% → 升級至 Senior |
| Gold | 30 分鐘 | 8 小時 | 超過 SLA 75% → 升級至 Senior |
| Silver/Bronze | 2 小時 | 24 小時 | 超過 SLA 100% → 升級至 Team Lead |

---

## 4. 知識庫系統 (Knowledge Base)

### 4.1 知識庫分類體系

**4 大類別知識庫**：

#### A. FAQ（常見問題）
- **存款與提款** (50+ 文章)
  - "存款失敗怎麼辦？"
  - "提款需要多久到賬？"
  - "支持哪些支付方式？"
- **紅利與活動** (40+ 文章)
  - "紅利流水如何計算？"
  - "如何領取首存紅利？"
  - "生日紅利如何獲得？"
- **遊戲問題** (30+ 文章)
  - "遊戲加載失敗怎麼辦？"
  - "投注記錄在哪裡查看？"
  - "遊戲 RTP 是多少？"
- **帳戶安全** (25+ 文章)
  - "如何啟用兩步驗證？"
  - "忘記密碼怎麼辦？"
  - "如何更新個人信息？"

#### B. 遊戲指南
- **老虎機規則** (100+ 遊戲)
  - Pragmatic Play - Sweet Bonanza 玩法
  - NetEnt - Starburst 規則
  - Play'n GO - Book of Dead 指南
- **真人娛樂規則**
  - 百家樂規則與賠率
  - 輪盤玩法（歐洲輪盤 vs 美式輪盤）
  - 骰寶規則詳解
- **體育投注指南**
  - 讓球盤解釋
  - 大小球玩法
  - 混合過關規則

#### C. 合規政策
- **隱私政策** - GDPR、個人信息保護法
- **負責任博彩** - 自我排除、存款限額、冷靜期
- **反洗錢 (AML)** - KYC 要求、資金來源證明
- **條款與條件** - 紅利條款、提款規則

#### D. 內部操作手冊（僅客服可見）
- **財務問題處理流程** - 存款失敗、提款延遲、餘額調整
- **遊戲爭議處理** - 投注取消、遊戲故障賠償
- **升級與賠償標準** - 投訴升級流程、賠償額度指南
- **欺詐檢測** - 多賬戶識別、套利檢測、獎金獵人處理

### 4.2 知識庫版本控制

**審批流程**：
```
內容創建 → 同行評審 (Peer Review) → Team Lead 批准 → 發布
                                    ↓
                           定期審閱（季度更新）
```

**版本歷史**：
- 每篇文章保留完整版本歷史
- 顯示最後更新時間、更新者
- 支持版本對比（diff view）

### 4.3 多語言支持

**支持語言**：英語、繁體中文、簡體中文、泰語、越南語、印尼語

**i18n 架構**：
```
Knowledge Base
├─ en/    - 英語版本（主版本）
├─ zh-TW/ - 繁體中文（翻譯）
├─ zh-CN/ - 簡體中文（翻譯）
├─ th/    - 泰語（翻譯）
├─ vi/    - 越南語（翻譯）
└─ id/    - 印尼語（翻譯）
```

**同步機制**：
- 主版本（英語）更新後，自動觸發翻譯任務
- 翻譯完成前，顯示英語版本 + "翻譯中" 標記
- 使用 AI 翻譯 + 人工審校

### 4.4 知識庫搜索優化

**Elasticsearch 整合**：
- 全文檢索（支持同義詞、模糊匹配）
- 搜索建議（autocomplete）
- 搜索結果高亮
- 點擊率追蹤（優化排名）

**搜索質量指標**：
- 搜索成功率（找到答案並關閉工單）> 70%
- 平均搜索時間 < 5 秒
- 零結果搜索率 < 5%

### 4.5 知識庫權限控制

| 角色 | 可見範圍 | 編輯權限 | 審批權限 |
|------|---------|---------|---------|
| **玩家** | FAQ + 遊戲指南 + 合規政策 | ❌ | ❌ |
| **客服代表** | 所有（含內部手冊） | ✅ 草稿 | ❌ |
| **Team Lead** | 所有 | ✅ 草稿 + 編輯 | ✅ 審批 |
| **Manager** | 所有 | ✅ 全部 | ✅ 終審 |

---

## 5. AI 客服機器人

### 5.1 智能分類引擎

**NLU (Natural Language Understanding) 引擎**：
- 意圖識別（Intent Classification）- 13 種意圖類別
- 實體提取（Entity Extraction）- 提款金額、遊戲名稱、帳戶 ID

**意圖分類列表**：

| 意圖 | 示例問題 | 信心閾值 | 處理方式 |
|------|---------|---------|---------|
| `DEPOSIT_ISSUE` | "我的存款沒到賬" | > 0.8 | 自動回覆 + 創建工單 |
| `WITHDRAWAL_QUERY` | "提款需要多久？" | > 0.9 | 返回 FAQ 文章 |
| `BONUS_INQUIRY` | "我的紅利在哪裡？" | > 0.85 | 檢查 Bonus Wallet + 回覆 |
| `GAME_MALFUNCTION` | "遊戲卡住了" | > 0.75 | 創建工單 + 轉人工 |
| `PASSWORD_RESET` | "忘記密碼了" | > 0.95 | 發送重置鏈接 |
| `KYC_VERIFICATION` | "如何完成 KYC？" | > 0.9 | 返回 KYC 指南 |
| `COMPLAINT` | "我要投訴" | > 0.7 | 立即轉人工 |

### 5.2 自動回覆規則引擎

**規則庫架構**（基於知識庫）：

```
Rule Engine
├─ FAQ 匹配規則
│  └─ 關鍵字匹配 + 相似度計算（> 85% 觸發自動回覆）
├─ 動態數據查詢規則
│  └─ 餘額查詢、提款狀態、紅利流水進度
└─ 操作指令規則
   └─ 密碼重置、設備解綁、郵件驗證重發
```

**自動回覆範例**：

**場景 1**: 玩家詢問 "我的提款狀態"
```
AI 動作：
1. 意圖識別 → WITHDRAWAL_QUERY (信心度 0.92)
2. 實體提取 → player_id: 123456
3. 查詢提款記錄 → Withdrawal API
4. 自動回覆：
   "您好！您的提款申請（$500）目前狀態為「審核中」，預計 24 小時內處理完成。
   如有疑問可繼續詢問或轉人工客服。"
```

**場景 2**: 玩家詢問 "為什麼紅利沒到賬？"
```
AI 動作：
1. 意圖識別 → BONUS_INQUIRY (信心度 0.88)
2. 查詢 Bonus Wallet → Bonus API
3. 判斷：
   - 紅利已到賬 → 回覆 "您的紅利已到賬，餘額：$50"
   - 紅利未到賬 → 檢查資格條件 → 回覆 "您需要完成首存 $100 才能領取"
   - 不確定 → 創建工單 + 轉人工
```

### 5.3 人工接管邏輯

**自動轉人工條件**：
- 意圖信心度 < 0.7（不確定玩家問題）
- 玩家明確要求人工客服（"轉人工"、"Talk to agent"）
- 敏感問題（投訴、欺詐指控、帳戶異常）
- 連續 3 次自動回覆無法解決問題
- VIP Diamond 玩家（自動轉 VIP 經理）

**接管流程**：
```
AI 偵測到轉人工條件
    ↓
創建工單 + 附加對話歷史
    ↓
分配至合適客服（基於工作量 + 技能匹配）
    ↓
客服接管 + 顯示 AI 分析結果（意圖、玩家情緒）
```

### 5.4 訓練數據管道

**數據來源**：
- 歷史工單（100,000+ 張）
- 玩家 FAQ 點擊率（哪些問題最常見）
- 客服標註數據（人工標註意圖與實體）

**持續學習流程**：
```
新工單 → 客服處理 → 標註意圖與實體 → 每週模型重訓 → 部署更新
```

**質量指標**：
- 意圖分類準確率 > 90%
- 自動回覆解決率 > 40%
- 玩家滿意度（AI 回覆）> 75%

---

## 6. 工單路由與分配

### 6.1 自動分配規則

**路由策略**（優先級從高到低）：

1. **VIP 玩家優先路由**
   - Diamond/Platinum → 專屬 VIP 經理
   - Gold → VIP 團隊（輪詢分配）

2. **技能匹配**
   - 財務類工單 → 財務專員
   - 遊戲技術問題 → 技術支持
   - 紅利爭議 → 紅利專員
   - 投訴 → Team Lead/Manager

3. **語言匹配**
   - 玩家語言偏好 = 客服語言能力

4. **工作量平衡**
   - 當前工單數最少的客服
   - 避免單一客服工單數 > 20

### 6.2 負載均衡算法

**加權輪詢算法**：
```
Weight = (客服等級權重) × (語言匹配度) × (當前工單負載倒數)

客服等級權重：
- Senior: 1.5
- Regular: 1.0
- Junior: 0.7

語言匹配度：
- 母語匹配: 1.0
- 流利: 0.8
- 基礎: 0.5

當前工單負載倒數：
- 1 / (當前工單數 + 1)
```

---

## 7. SLA 管理與自動化

### 7.1 SLA 監控與告警

**實時監控儀表板**：
```
┌─────────────────────────────────────┐
│  SLA 達成率（最近 24 小時）           │
│  首次響應 SLA: 92.5% ✅              │
│  解決 SLA: 85.3% ⚠️                  │
│  超 SLA 工單數: 23 張                │
└─────────────────────────────────────┘
```

**自動告警**：
- SLA 超時 50% → Slack 通知客服 + Team Lead
- SLA 超時 75% → Email + SMS 通知 Manager
- SLA 達成率 < 90%（連續 2 小時）→ 緊急告警

### 7.2 自動升級機制

**升級規則**：

| 條件 | 升級動作 |
|------|---------|
| 超 SLA 50% 且無處理記錄 | 升級至 Team Lead |
| 超 SLA 100% 且無解決方案 | 升級至 Manager |
| 玩家投訴 + Diamond VIP | 立即升級至 VIP Manager |
| 工單重開 3 次以上 | 升級至 Senior 客服 |

---

## 8. 客服績效分析

### 8.1 關鍵績效指標 (KPI)

| 指標 | 定義 | 目標值 | 計算公式 |
|------|------|--------|----------|
| **FCR** (First Contact Resolution) | 首次聯繫解決率 | > 80% | 首次解決工單數 / 總工單數 |
| **AHT** (Average Handling Time) | 平均處理時間 | < 15 分鐘 | 總處理時間 / 工單數 |
| **CSAT** (Customer Satisfaction) | 客戶滿意度 | > 90% | 滿意工單數 / 評分工單數 |
| **SLA 達成率** | SLA 合規率 | > 95% | SLA 內完成工單 / 總工單數 |
| **重開率** | 工單重新開啟率 | < 5% | 重開工單數 / 已關閉工單數 |

### 8.2 客服排行榜

**每週績效排名**（顯示在客服系統首頁）：

```
┌────────────────────────────────────┐
│  本週績效 Top 5 客服                │
├────────────────────────────────────┤
│  🥇 Alice    - FCR: 92%, CSAT: 96% │
│  🥈 Bob      - FCR: 88%, CSAT: 94% │
│  🥉 Charlie  - FCR: 85%, CSAT: 92% │
│  4️⃣  David    - FCR: 83%, CSAT: 91% │
│  5️⃣  Emma     - FCR: 81%, CSAT: 90% │
└────────────────────────────────────┘
```

**獎勵機制**：
- 月度 Top 3 → 獎金 $200 / $150 / $100
- 連續 3 個月 Top 10 → 晉升 Senior

---

## 9. 多渠道整合

### 9.1 支持渠道

| 渠道 | 優先級 | SLA | 自動化程度 |
|------|--------|-----|-----------|
| **Live Chat** | 最高 | 5 分鐘 | AI 自動回覆 40% |
| **Telegram** | 高 | 15 分鐘 | AI 自動回覆 30% |
| **Email** | 中 | 2 小時 | 自動分類 100% |
| **WhatsApp** | 中 | 15 分鐘 | AI 自動回覆 25% |
| **電話** | VIP 專屬 | 即時接聽 | 0% (人工) |

### 9.2 Live Chat 整合

**WebSocket 即時通訊**：
- 客服與玩家實時對話
- 顯示"正在輸入..."狀態
- 客服可同時處理 3-5 個對話（基於經驗等級）

**功能**：
- 快速回覆模板（50+ 預設回覆）
- 文件傳送（截圖、支付憑證）
- 表情符號支持
- 對話轉移（轉其他客服或升級）

---

## 10. SmartAdmin 架構映射

### 10.1 核心類別設計

#### Entity 層

```java
@Entity
@Table(name = "t_customer_service_ticket")
@Data
public class TicketEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ticketId;

    private Long playerId;

    @Enumerated(EnumType.STRING)
    private TicketType type; // DEPOSIT_ISSUE, WITHDRAWAL_QUERY, etc.

    @Enumerated(EnumType.STRING)
    private TicketPriority priority; // URGENT, HIGH, MEDIUM, LOW

    @Enumerated(EnumType.STRING)
    private TicketStatus status; // NEW, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED

    private Long assignedAgentId;

    private String subject;

    private String description;

    private LocalDateTime firstResponseTime;

    private LocalDateTime resolvedTime;

    private Boolean deleted;
}
```

#### Manager 層

```java
@Manager
@RequiredArgsConstructor
public class TicketAssignmentManager {
    private final TicketDao ticketDao;
    private final AgentDao agentDao;
    private final RedissonClient redissonClient;

    @Transactional(rollbackFor = Throwable.class)
    public Long assignTicketToAgent(Long ticketId, Long playerId) {
        // Lock to prevent concurrent assignment
        RLock lock = redissonClient.getLock("ticket:assign:" + ticketId);
        try {
            lock.lock(10, TimeUnit.SECONDS);

            // Find best agent using weighted round-robin
            AgentEntity agent = findBestAvailableAgent(playerId);
            if (agent == null) {
                throw new BusinessException("No available agents");
            }

            // Assign ticket
            TicketEntity ticket = ticketDao.selectById(ticketId);
            ticket.setAssignedAgentId(agent.getAgentId());
            ticket.setStatus(TicketStatus.ASSIGNED);
            ticketDao.updateById(ticket);

            return agent.getAgentId();
        } finally {
            lock.unlock();
        }
    }

    private AgentEntity findBestAvailableAgent(Long playerId) {
        // Implementation of weighted round-robin algorithm
        // (See §6.2 for logic)
        return null; // placeholder
    }
}
```

#### Service 層

```java
@Service
@RequiredArgsConstructor
public class TicketService {
    private final TicketDao ticketDao;
    private final TicketAssignmentManager ticketAssignmentManager;

    public Option<TicketVO> createTicket(TicketCreateForm form) {
        return Try.of(() -> {
            // Validate form
            if (form.getPlayerId() == null || form.getSubject() == null) {
                throw new BusinessException("Invalid ticket form");
            }

            // Create ticket entity
            TicketEntity ticket = new TicketEntity();
            ticket.setPlayerId(form.getPlayerId());
            ticket.setType(form.getType());
            ticket.setPriority(calculatePriority(form));
            ticket.setStatus(TicketStatus.NEW);
            ticket.setSubject(form.getSubject());
            ticket.setDescription(form.getDescription());

            ticketDao.insert(ticket);

            // Auto-assign to agent
            ticketAssignmentManager.assignTicketToAgent(ticket.getTicketId(), form.getPlayerId());

            return SmartBeanUtil.copy(ticket, TicketVO.class);
        }).toOption();
    }

    private TicketPriority calculatePriority(TicketCreateForm form) {
        // Logic to determine priority based on type and player VIP level
        return TicketPriority.MEDIUM; // placeholder
    }
}
```

### 10.2 Foundation 模組依賴

**依賴項**：
- `foundation.redis-lock` - 工單分配防並發
- `foundation.websocket` - Live Chat 即時通訊
- `foundation.mq` - SLA 告警事件發布
- `foundation.cache` - 玩家 360° 視圖緩存
- `foundation.elasticsearch` - 知識庫全文檢索

---

## 11. 相關文檔

### 核心依賴
- **[01-03 玩家分群與標籤](../01_Player_Center/01-03_Player_Segmentation.md)** - 玩家生命週期、RFM 模型、風控標籤
- **[05-01 風控系統](../04_Risk_Control/04-01_Risk_Framework.md)** - 風險評分、異常檢測

### 技術基礎設施
- **[07-02 網關架構](../07_Technical_Infrastructure/07-02_Gateway_Architecture.md)** - WebSocket 即時通訊
- **[07-03 API 設計標準](../07_Technical_Infrastructure/07-03_API_Design_Standard.md)** - RESTful API 規範

### Analytics & Operations
- **[06-01 報表與 BI](./06-01_Reporting_BI.md)** - 客服績效分析報表
- **[06-03 第三方整合](./06-03_Third_Party_Integration.md)** - Telegram/WhatsApp 整合

---

**文檔版本**: 2.0.0
**最後更新**: 2026-02-04
**維護團隊**: Customer Service Team & Product Team
**遷移歷史**: 從 11-01 遷移至 06-02（Phase 4 Module Merge）
