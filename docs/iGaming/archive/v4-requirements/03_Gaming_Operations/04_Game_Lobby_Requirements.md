# 遊戲大廳需求（Game Lobby Requirements）

> **Canonical Source**: [03-02_Game_Lobby_Management.md](../../source-archive/03_Game_Center/03-02_Game_Lobby_Management.md)
> **Audience**: 高階主管、產品經理、營運主管、UX 設計師
> **Related Architecture**: [Game Lobby System (Architecture)](../../architecture/03_Game_Integration/05_Game_Lobby_System.md)
> **Last Synced**: 2026-02-08

---

## 1. 概述（Overview）

Game Lobby 負責管理面向玩家的遊戲列表，包括分類、排序、搜尋和個性化推薦。目標是提供**個性化大廳體驗**，針對個別玩家的偏好和行為進行客製化（「千人千面」）。

---

## 2. 遊戲元數據管理（Game Metadata Management）

### 2.1 必需的遊戲屬性（Required Game Attributes）

大廳中的每款遊戲必須維護以下元數據：

| 屬性類別 | 欄位 |
|--------------------|--------|
| **基本資訊** | 遊戲名稱（多語言）、圖示/縮圖、遊戲類型（Slot、Live、Sport）、供應商名稱 |
| **技術屬性** | RTP（Return to Player，玩家回報率）、波動性（Low/Medium/High）、支援裝置（Desktop/Mobile） |

### 2.2 自動同步流程（Auto-Sync Process）

供應商的新遊戲每 4 小時自動同步一次：

| 步驟 | 動作 | 詳細說明 |
|------|--------|--------|
| 1 | 獲取遊戲列表 | 呼叫供應商的 GetGameList API |
| 2 | 識別新遊戲 | 與本地資料庫比對，偵測新條目 |
| 3 | 下載素材 | 檢索遊戲圖片並上傳至平台 CDN |
| 4 | 設定初始狀態 | 預設為 **DISABLED**（待審核），防止未翻譯內容出現 |
| 5 | 通知營運團隊 | 發送「偵測到新遊戲」通知給營運團隊 |

---

## 3. 大廳組織與顯示規則（Lobby Organization and Display Rules）

### 3.1 遊戲分類（Game Categorization）

**系統分類**（預定義）：
- Hot（熱門遊戲）
- New（最近上線）
- Jackpot（累進獎池遊戲）

**自訂分類**（商戶可配置）：
- 商戶可建立自訂標籤，例如「本週精選」、「聖誕特輯」、「農曆新年」等
- 自訂分類由營運團隊針對每個商戶完全管理

### 3.2 排序規則（Sorting Rules）

每個分類內的遊戲使用以下優先級系統排序：

| 優先級 | 排序方法 | 說明 |
|----------|---------------|-------------|
| 1（最高） | 手動置頂 | 營運團隊可將特定遊戲置頂 |
| 2 | 基於人氣 | 按玩家數量和投注量自動排序 |
| 3（預設） | 基於權重 | 每款遊戲分配的預設權重 |

### 3.3 動態配置（Dynamic Configuration）

- **所有大廳元素**（橫幅、遊戲網格、選單、標籤）必須由配置驅動。不允許前端硬編碼。
- **熱更新**：配置變更透過輪詢或 WebSocket 推送，玩家無需重新整理頁面即可看到。

---

## 4. 遊戲標籤與分類（Game Tags and Classification）

### 4.1 多維度標籤系統（Multi-Dimensional Tag System）

**系統標籤**（自動生成）：

| 標籤 | 觸發條件 |
|-----|-------------------|
| NEW | 最近 7 天內上線 |
| HOT | 最近 24 小時內超過 100 名獨立投注者 |
| JACKPOT | 遊戲連接至累進獎池 |
| HIGH_RTP | RTP 為 97% 或更高 |
| EXCLUSIVE | 平台獨家遊戲 |

**營運標籤**（手動配置）：

| 標籤 | 使用範例 |
|-----|-------------|
| 每週推薦 | 每週更新的精選推薦 |
| 季節促銷 | 「聖誕特輯」、「農曆新年」 |
| 快速遊戲 | 單局時長低於 30 秒 |
| 高額投注 | 單注最大投注額超過 $1,000 |

**玩家標籤**（用戶生成）：
- 收藏數量（例如：1,200 名玩家已收藏）
- 玩家評分（例如：5 分中的 4.8 分）

---

## 5. 搜尋與篩選需求（Search and Filter Requirements）

### 5.1 篩選維度（Filter Dimensions）

玩家必須能夠按以下條件篩選遊戲：

| 篩選器 | 選項 |
|--------|---------|
| 遊戲供應商 | PG Soft、Pragmatic Play、Evolution 等 |
| 遊戲類型 | Slots、Live Casino、Sports、Lottery |
| RTP 範圍 | 95-96%、96-97%、97%+ |
| 波動性 | Low、Medium、High |
| 投注範圍 | 最小和最大投注金額 |

### 5.2 搜尋能力（Search Capabilities）

| 功能 | 說明 |
|---------|-------------|
| 全文搜尋 | 搜尋遊戲名稱（多語言）、標籤和供應商名稱 |
| 拼音支援 | 輸入「shuiguoji」可匹配「水果機」的中文遊戲名稱 |
| 同義詞匹配 | 「Slots」=「水果機」= 其他區域等效詞 |
| 搜尋歷史 | 記錄玩家搜尋詞彙，用於個性化熱搜推薦 |
| 加權結果 | 遊戲名稱匹配優先於標籤匹配 |

---

## 6. 個性化推薦策略（Personalized Recommendation Strategy）

### 6.1 玩家分群推薦矩陣（Recommendation Matrix by Player Segment）

| 玩家分群 | 推薦策略 | 權重分配 |
|----------------|------------------------|---------------------|
| **新玩家** | 熱門遊戲 + 高 RTP 遊戲 | 60% 人氣 + 40% RTP |
| **活躍玩家** | 歷史偏好 + 相似遊戲 | 70% 協同過濾 + 30% 人氣 |
| **VIP 玩家** | 高額投注遊戲 + 獨家遊戲 | 50% 高投注 + 30% 獨家 + 20% 新遊戲 |
| **流失中玩家** | 先前喜愛的遊戲 + 新促銷 | 60% 歷史偏好 + 40% 新活動 |

### 6.2 行為評分（Behavioral Scoring）

玩家與遊戲的互動會被評分以驅動推薦：

| 動作 | 分數 |
|--------|-------|
| 點擊遊戲 | +1 |
| 試玩模式遊玩 | +3 |
| 進行真錢投注 | +10 |
| 加入收藏 | +5 |

---

## 7. 商戶差異化（Merchant Differentiation）

| 功能 | 說明 |
|---------|-------------|
| **遊戲屏蔽** | 商戶 A 可隱藏所有 RTP 高於 98% 的遊戲；商戶 B 可保留 |
| **獨家遊戲** | 某些遊戲僅限特定商戶使用 |

---

## 8. 審批工作流程（Approval Workflows）

### 8.1 緊急遊戲下架（Emergency Game Takedown）

| 步驟 | 角色 | 動作 |
|------|-------|--------|
| 1 | 營運團隊 | 發起「維護請求」（例如：偵測到異常 RTP） |
| 2 | 風控經理 | 批准請求 |
| 3 | 系統 | 立即生效 -- 遊戲被隱藏或停用 |

### 8.2 新遊戲上線審批（New Game Launch Approval）

| 步驟 | 角色 | 動作 |
|------|-------|--------|
| 1 | 系統 | 從供應商自動同步新遊戲 |
| 2 | 營運團隊 | 配置圖片、翻譯和標籤 |
| 3 | 營運團隊 | 提交審核 |
| 4 | 審核人員 | 批准上線 |

**目的**：防止未測試或部分翻譯的遊戲暴露給玩家。

---

## 9. UX 需求（UX Requirements）

### 9.1 效能目標（Performance Targets）

| 指標 | 目標 |
|--------|--------|
| API 回應時間（P50） | 低於 100ms |
| API 回應時間（P99） | 低於 500ms |
| 搜尋查詢延遲 | 低於 50ms |
| 圖片 CDN 命中率 | 高於 98% |

### 9.2 分頁與滾動（Pagination and Scrolling）

- 遊戲預設以每頁 30 項載入
- 前端使用虛擬滾動，僅渲染可見的遊戲卡片加上緩衝區，確保即使有數千款遊戲也能順暢滾動

---

## 10. 商業價值（Business Value）

Game Lobby 透過以下方式提供可衡量的商業價值：

- **提升玩家參與度**：個性化推薦相較於靜態遊戲列表，點擊率提高 25%，增加平台停留時間和終身價值
- **降低玩家流失**：「千人千面」策略確保每位玩家看到相關內容，降低因不相關遊戲顯示造成的跳出率
- **加速新遊戲採用**：自動同步和促銷標籤使新遊戲快速上市，獲得先發優勢
- **優化營運效率**：配置驅動的大廳消除前端硬編碼變更，將部署週期從數天縮短至數分鐘
- **實現商戶差異化**：每商戶的遊戲屏蔽和獨家遊戲分配支援多品牌組合策略，無需重複程式碼

---

## 11. 成功指標（Success Metrics）

| 指標 | 目標 | 測量方法 |
|--------|--------|-------------|
| 遊戲點擊率（CTR） | ≥8% | （遊戲點擊數 / 曝光數）× 100% |
| 遊戲轉換率 | ≥12% | （實際投注數 / 遊戲點擊數）× 100% |
| 搜尋結果相關性 | ≥90% | 玩家在前 5 個結果內找到目標遊戲 |
| Lobby API 回應時間（P99） | <500ms | 99th 百分位 API 延遲監控 |
| 個性化參與度 | ≥25% | 推薦遊戲點擊數 vs 總遊戲點擊數 |
| CDN 快取命中率 | ≥98% | 遊戲素材 CDN 快取效能 |

---

## 12. 業務 KPI（Business KPIs）

### 12.1 核心指標（Core Metrics）

| KPI | 公式 |
|-----|---------|
| 遊戲點擊率（CTR） | （遊戲點擊數 / 曝光數）× 100% |
| 遊戲轉換率 | （實際投注數 / 遊戲點擊數）× 100% |
| 平均遊戲時長 | AVG(session_duration) 每款遊戲 |
| 前 10 熱門遊戲 | 按總投注量排名 |

### 12.2 告警規則（Alert Rules）

| 告警條件 | 嚴重程度 |
|-----------------|----------|
| 遊戲 RTP 異常（高於 105% 或低於 90%） | Warning |
| 遊戲同步失敗（連續 3 次失敗） | Warning |
| API 回應時間 P99 超過 1 秒 | Warning |
| 快取命中率低於 80% | Warning |

---

## 11. 成功指標（Success Metrics）

| 指標 | 目標 | 測量方法 |
|--------|--------|-------------------|
| 遊戲點擊率（CTR） | ≥15% | （遊戲點擊數 / 曝光數）× 100% |
| 遊戲轉換率 | ≥40% | （實際投注數 / 遊戲點擊數）× 100% |
| API 回應時間（P99） | <500ms | Gateway 層級的延遲監控 |
| 搜尋查詢延遲 | <50ms | 全文搜尋引擎查詢時間追蹤 |
| 圖片 CDN 命中率 | ≥98% | CDN 快取命中比例日誌 |
| 新遊戲同步成功率 | ≥99% | 自動同步作業成功/失敗比例 |
| 玩家個性化匹配率 | ≥70% | A/B 測試：推薦 vs 隨機遊戲的轉換提升 |

---

## 相關文件（Related Documents）

### 核心依賴（Core Dependencies）
- [Game Integration Standards](../../source-archive/03_Game_Center/03-01_Game_Integration_Standard.md) - GP API 規格
- [Seamless Wallet Analysis](../../source-archive/03_Game_Center/03-03_Seamless_Wallet_Analysis.md) - 遊戲啟動流程

### 業務整合（Business Integration）
- [Activity Bonus](../../source-archive/04_Activity_Center/04-04_Activity_Bonus.md) - 基於活動的遊戲推薦
- [VIP Loyalty](../../source-archive/01_Player_Center/01-06_VIP_Loyalty.md) - VIP 獨家遊戲

### UX 與設計（UX and Design）
- [Frontend Layout Engine](../../source-archive/11_Frontend_CMS/11-01_Frontend_Layout_Engine.md) - 大廳頁面設計
- [Gateway Architecture](../../source-archive/09_Technical_Infrastructure/09-02-01_Gateway_Core.md) - API 限流

### 技術實作（Technical Implementation）

→ **[Game Lobby System Architecture](../../architecture/03_Game_Integration/05_Game_Lobby_System.md)** - 遊戲目錄管理、篩選演算法、延遲載入實作、CDN 素材優化、個性化引擎

---

**Document Version**: 1.0.0
**Last Updated**: 2026-02-08
**Maintainer**: Game Team
