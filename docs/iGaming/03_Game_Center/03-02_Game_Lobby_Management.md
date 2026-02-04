# 03-02 遊戲大廳管理 (Game Lobby Management)

## 1. 系統概述
負責管理前台展示的遊戲列表、分類、排序與推薦。
目標是提供 "千人千面" 的個性化大廳體驗。

## 2. 核心功能需求

### 2.1 遊戲元數據管理 (Game Metadata)
- **基本屬性**：遊戲名稱 (多語言)、圖標 (Icon)、類型 (Slot, Live, Sport)、供應商 (PG, Evolution)。
- **技術屬性**：RTP (玩家回報率)、Volatility (波動率)、支援裝置 (Desktop/Mobile)。
- **同步機制 (Auto-Sync)**：
  - **頻率**: 每 4 小時執行一次 `GameDiscoveryJob`。
  - **流程**: 
    1. 調用 GP 的 `GetGameList` API。
    2. 比對本地 DB，識別 **[NEW]** 遊戲。
    3. 自動下載圖片資源並上傳至平台 CDN。
    4. 預設狀態設為 `DISABLED` (待審核)，防止未翻譯內容直接曝光。
    5. 發送 "New Game Detected" 通知給運營團隊。

### 2.2 大廳配置 (Lobby Config)
- **分類管理 (Categorization)**：
  - 系統預設：熱門 (Hot)、新遊戲 (New)、彩金池 (Jackpot)。
  - 自定義標籤：商戶可建立 "本週推薦"、"聖誕特輯" 等標籤。
- **排序邏輯**：
  - 預設權重
  - 依熱度 (投注人數/金額) 自動排序
  - 人工置頂 (Pin)

### 2.3 搜尋與過濾

**基礎篩選維度**：
- 遊戲供應商（PG Soft, Pragmatic Play, Evolution 等）
- 遊戲類型（老虎機, 真人, 體育, 彩票）
- RTP 區間（95-96%, 96-97%, 97%+）
- 波動率（低, 中, 高）
- 最小/最大下注額

**全文檢索實作** (Elasticsearch):
```json
{
  "query": {
    "multi_match": {
      "query": "水果機",
      "fields": ["game_name_zh^3", "game_name_en", "tags^2", "provider"],
      "fuzziness": "AUTO"
    }
  }
}
```

**搜尋優化策略**：
- **拼音搜尋**：支援 "shuiguoji" 匹配 "水果機"
- **同義詞**："老虎機" = "Slot" = "角子機"
- **搜尋歷史**：記錄用戶搜尋詞，優化熱門搜尋推薦

### 2.4 個性化推薦算法

**推薦策略矩陣**：

| 用戶類型 | 推薦策略 | 權重分配 |
|---------|---------|---------|
| **新用戶** | 熱門遊戲 + 高 RTP 遊戲 | 60% 熱度 + 40% RTP |
| **活躍玩家** | 歷史偏好 + 同類遊戲 | 70% 協同過濾 + 30% 熱度 |
| **VIP 用戶** | 高額投注遊戲 + 獨家遊戲 | 50% 高投注 + 30% 獨家 + 20% 新遊戲 |
| **流失用戶** | 曾經喜愛的遊戲 + 新活動 | 60% 歷史偏好 + 40% 新活動 |


**實時行為追蹤**：
- 點擊遊戲（+1 分）
- 試玩遊戲（+3 分）
- 投注遊戲（+10 分）
- 加入收藏（+5 分）

## 3. 商戶差異化
- **屏蔽遊戲**：商戶 A 可屏蔽 RTP > 98% 的遊戲，商戶 B 可保留。
- **專屬遊戲**：支援僅特定商戶可見的獨家遊戲 (Exclusive Games)。

## 4. 動態配置與審批 (Dynamic Config & Approval)

### 4.1 動態配置項
- **全動態化**：大廳的所有元素（Banner, Game Grid, Menu, Tags）必須透過 JSON 配置驅動，嚴禁前端硬編碼（Hardcode）。
- **熱更**：修改配置後，前端透過輪詢或 WebSocket 接收更新，無需刷新頁面即可看到新排序。

### 4.2 審批工作流
- **遊戲開關審批**：
  - **場景**：發現某遊戲 RTP 異常，需緊急下架。
  - **流程**：運營發起 "維護申請" -> 風控主管批准 -> 立即生效。
- **上架新遊戲審批**：
  - **流程**：平台同步新遊戲 -> 運營配置圖片與標籤 -> 提交審核 -> 允許上架。
  - **目的**：防止未經測試或翻譯不全的遊戲直接暴露給玩家。

---

## 5. 遊戲標籤與分類策略

### 5.1 多維度標籤體系

**系統標籤** (自動生成):
- `[NEW]` - 上線 7 天內
- `[HOT]` - 近 24 小時投注人數 > 100
- `[JACKPOT]` - 累積彩金池遊戲
- `[HIGH_RTP]` - RTP ≥ 97%
- `[EXCLUSIVE]` - 平台獨家

**運營標籤** (人工配置):
- `[周推薦]`, `[聖誕特輯]`, `[農曆新年]`
- `[快速遊戲]` - 單局 < 30 秒
- `[高額投注]` - 單注上限 > $1000

**玩家標籤** (用戶生成):
- 收藏數量 (❤️ 1.2K 人收藏)
- 評分 (⭐ 4.8 / 5.0)

### 5.2 智能分類引擎

**基於內容的自動分類**：

---

## 6. 性能優化策略

### 6.1 多級快取架構

```
┌─────────────────────────────────────────────┐
│  L1: CDN 快取 (遊戲圖標、Banner)             │
│  TTL: 7 天                                  │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│  L2: Redis 快取 (遊戲列表、排序結果)         │
│  TTL: 15 分鐘                               │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│  L3: Application Cache (JVM Caffeine)       │
│  TTL: 5 分鐘                                │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│  Database (MySQL - 遊戲元數據主庫)           │
└─────────────────────────────────────────────┘
```

**快取更新策略**：
- **遊戲上下架**：立即清除所有快取（Pub/Sub 廣播）
- **熱度排序**：每 15 分鐘後台任務重新計算
- **新遊戲同步**：增量更新，僅清除相關分類快取

### 6.2 分頁與虛擬捲動

**後端分頁 API**：
```json
GET /api/v1/game-lobby/games?category=slot&page=1&size=30

Response:
{
  "games": [...],
  "pagination": {
    "current_page": 1,
    "total_pages": 45,
    "total_games": 1340
  }
}
```

**前端虛擬捲動**：
- 使用 `react-window` / `react-virtualized` 渲染大量遊戲卡片
- 僅渲染可見區域 + 上下緩衝區（提升滾動流暢度）

---

## 7. 數據模型設計

### 7.1 核心表結構



### 7.2 遊戲熱度計算表


---

## 8. API 設計規範

### 8.1 遊戲列表 API

```http
GET /api/v1/game-lobby/games?category={category}&provider={provider}&page={page}

Headers:
  Authorization: Bearer {token}
  X-Tenant-ID: {tenantId}

Query Parameters:
  - category (optional): 'HOT', 'NEW', 'JACKPOT'
  - provider (optional): 'PG', 'PRAGMATIC', 'EVOLUTION'
  - search (optional): 搜尋關鍵字
  - page (required): 頁碼
  - size (optional): 每頁數量 (預設 30)

Response (200 OK):
{
  "code": 0,
  "msg": "success",
  "data": {
    "games": [
      {
        "game_id": "pg_fortune_tiger",
        "game_name": "Fortune Tiger",
        "provider": "PG Soft",
        "rtp": 96.81,
        "thumbnail": "https://cdn.example.com/games/fortune_tiger.jpg",
        "tags": ["HOT", "HIGH_RTP"],
        "popularity_score": 9.2
      }
    ],
    "pagination": {
      "current_page": 1,
      "total_pages": 15,
      "total_count": 450
    }
  }
}
```

### 8.2 遊戲詳情 API

```http
GET /api/v1/game-lobby/games/{gameId}

Response:
{
  "code": 0,
  "data": {
    "game_id": "pg_fortune_tiger",
    "game_name": "Fortune Tiger (招財虎)",
    "provider": "PG Soft",
    "rtp": 96.81,
    "volatility": "MEDIUM",
    "min_bet": 0.10,
    "max_bet": 250.00,
    "description": "亞洲主題老虎機...",
    "features": ["Free Spins", "Multipliers", "Re-Spins"],
    "stats": {
      "total_players_today": 1234,
      "total_bets_today": 45678,
      "avg_rating": 4.7,
      "favorite_count": 890
    }
  }
}
```

---

## 9. 監控指標 (KPIs)

### 9.1 業務指標
- **遊戲點擊率 (CTR)**: (點擊遊戲數 / 展示次數) × 100%
- **遊戲轉換率**: (實際投注數 / 點擊遊戲數) × 100%
- **平均遊戲時長**: AVG(session_duration)
- **熱門遊戲 TOP 10**: 依投注金額排序

### 9.2 技術指標
- **API 響應時間**: P50 < 100ms, P99 < 500ms
- **快取命中率**: Redis > 95%
- **搜尋查詢延遲**: Elasticsearch < 50ms
- **圖片 CDN 命中率**: > 98%

### 9.3 告警規則
- ⚠️ 某遊戲 RTP 異常（> 105% 或 < 90%）
- ⚠️ 遊戲同步失敗（連續 3 次失敗）
- ⚠️ API 響應時間 P99 > 1s
- ⚠️ 快取命中率 < 80%

---

## 📚 相關文檔

### 核心依賴
- [03-01 遊戲集成標準](./03-01_Game_Integration_Standard.md) - GP API 規格
- [03-03 無縫錢包對接分析](./03-03_Seamless_Wallet_Analysis.md) - 遊戲啟動流程

### 技術架構
- [08-01 前端佈局引擎](../08_Frontend_CMS/08-01_Frontend_Layout_Engine.md) - 大廳頁面設計
- [12-03 網關架構](../07_Technical_Infrastructure/07-02-01_Gateway_Core.md) - API 限流

### 業務整合
- [04-01 活動系統設計](../03_Player_Journey/03-03_Activity_Bonus.md) - 活動遊戲推薦
- [01-02 VIP 系統](../03_Player_Journey/03-02_VIP_Loyalty.md) - VIP 獨家遊戲

---

**最後更新**: 2026-01-27
**維護團隊**: Game Team

---

**文檔版本**: 1.0.0
**最後更新**: 2026-01-28
**維護團隊**: Integration Team & Backend Team
