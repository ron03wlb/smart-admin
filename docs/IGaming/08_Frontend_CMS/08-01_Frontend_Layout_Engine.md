# 08-01 前台版面配置引擎 (Frontend Layout Engine)

## 1. 系統概述
無需工程師介入，運營人員即可透過後台 "拖拉拽" (Drag & Drop) 方式調整前台首頁佈局。
支援多終端 (Web, H5, App) 的自適應配置。

## 2. 核心功能需求

### 2.1 模板管理 (Template Management)
- **主題切換**：
  - 平台預置多套主題 (深色、亮色、節日限定)。
  - 商戶可一鍵切換整站配色 (CSS Variables)。
- **組件庫 (Component Library)**：
  - Banner 輪播模組
  - 跑馬燈 (Marquee)
  - 遊戲入口網格 (Game Grid) - 可設定 3列/4列/5列
  - 存款引導按鈕

### 2.2 頁面編輯器 (Page Editor)
- **首頁裝修**：
  - 上下拖動模組排序。
  - 設置模組標題 (如 "熱門遊戲" 改為 "TOP 10")。
- **導航欄配置 (Menu Config)**：
  - 自定義 Header/Footer 菜單順序。
  - 支援跳轉內部頁面或外部連結。

## 3. 審批與發布
- **預覽模式 (Preview)**：編輯完成後，生成一個 "預覽連結" (Preview URL) 供內部測試。
- **發布流程**：
  - 點擊 "發布 (Publish)" -> 生成版本號 (v1.0.1)。
  - 系統將配置推送至 CDN。
  - 前段透過 API 拉取最新 JSON Config 渲染頁面。

##### 📊 Diagram 1: 佈局配置完整流程 (Layout Configuration Complete Flow)

```mermaid
flowchart TD
    START[運營人員登入<br/>Layout CMS Admin] --> EDITOR[頁面編輯器<br/>Drag & Drop Interface]

    EDITOR --> COMP[組件庫選擇]
    COMP --> C1[Banner 輪播<br/>上傳圖片 + 鏈接]
    COMP --> C2[遊戲網格<br/>選擇遊戲 + 排序]
    COMP --> C3[跑馬燈<br/>輸入公告文字]
    COMP --> C4[存款按鈕<br/>設置樣式 + CTA]

    C1 --> ARRANGE[拖動排序<br/>調整組件位置]
    C2 --> ARRANGE
    C3 --> ARRANGE
    C4 --> ARRANGE

    ARRANGE --> CONFIG[配置參數<br/>Title, Display Rules]

    CONFIG --> TARGET{客群定向?}
    TARGET -->|All Users| PREVIEW
    TARGET -->|VIP Only| VIP_RULE[設置規則<br/>player.vip_level >= 3]
    TARGET -->|New Users Only| NEW_RULE[設置規則<br/>player.registration_days <= 7]

    VIP_RULE --> PREVIEW
    NEW_RULE --> PREVIEW[生成預覽<br/>Preview URL]

    PREVIEW --> TEST{測試驗收?}
    TEST -->|需修改| EDITOR
    TEST -->|通過| PUBLISH[點擊發布<br/>Publish Button]

    PUBLISH --> VERSION[生成版本<br/>v1.0.1<br/>timestamp: 2026-01-27T10:30:00Z]

    VERSION --> SAVE[保存至數據庫<br/>layout_configs table<br/>status: PUBLISHED]

    SAVE --> CDN[推送至 CDN<br/>CloudFront/Cloudflare<br/>JSON Config File]

    CDN --> CACHE[CDN 緩存<br/>TTL: 5 minutes<br/>Edge Locations: Global]

    CACHE --> API[前端 API 請求<br/>GET /api/v1/layout/config<br/>version: latest]

    API --> FETCH{版本檢查}
    FETCH -->|本地版本過期| DOWNLOAD[下載新配置<br/>JSON Config from CDN]
    FETCH -->|本地版本最新| RENDER

    DOWNLOAD --> RENDER[Vue 3 渲染引擎<br/>Dynamic Component Rendering]

    RENDER --> COMP_RENDER[組件渲染]
    COMP_RENDER --> R1[Banner: Swiper.js]
    COMP_RENDER --> R2[Game Grid: Virtual Scroll]
    COMP_RENDER --> R3[Marquee: CSS Animation]
    COMP_RENDER --> R4[Deposit Button: v-if]

    R1 --> RULE_CHECK{客群規則檢查}
    R2 --> RULE_CHECK
    R3 --> RULE_CHECK
    R4 --> RULE_CHECK

    RULE_CHECK -->|匹配用戶| SHOW[顯示組件]
    RULE_CHECK -->|不匹配| HIDE[隱藏組件]

    SHOW --> TRACKING[埋點追蹤<br/>Event: component_exposure<br/>component_id, user_id]
    HIDE --> END

    TRACKING --> END[前端頁面展示完成]

    style START fill:#E3F2FD,stroke:#1976D2,stroke-width:2px
    style EDITOR fill:#FFF9C4,stroke:#F57F17,stroke-width:2px
    style PUBLISH fill:#FFE082,stroke:#F57F00,stroke-width:2px
    style VERSION fill:#C8E6C9,stroke:#388E3C,stroke-width:2px
    style CDN fill:#B2DFDB,stroke:#00796B,stroke-width:2px
    style RENDER fill:#CE93D8,stroke:#7B1FA2,stroke-width:2px
    style SHOW fill:#A5D6A7,stroke:#388E3C,stroke-width:2px
    style HIDE fill:#E0E0E0,stroke:#757575,stroke-width:1px
    style END fill:#4CAF50,stroke:#1B5E20,stroke-width:3px,color:#FFF
```

**JSON Config 範例**:

```json
{
  "version": "v1.0.1",
  "published_at": "2026-01-27T10:30:00Z",
  "experiment_id": null,
  "components": [
    {
      "component_id": "banner-hero",
      "type": "BANNER",
      "order": 1,
      "config": {
        "images": [
          {
            "url": "https://cdn.example.com/banner1.jpg",
            "link": "/promotions/welcome-bonus",
            "alt": "Welcome Bonus 100%"
          }
        ],
        "autoplay": true,
        "interval": 5000
      },
      "display_rules": {
        "target_users": ["ALL"]
      }
    },
    {
      "component_id": "game-grid-hot",
      "type": "GAME_GRID",
      "order": 2,
      "config": {
        "title": "熱門遊戲",
        "game_ids": [101, 102, 103, 104, 105],
        "columns": 5,
        "show_jackpot": true
      },
      "display_rules": {
        "target_users": ["ALL"]
      }
    },
    {
      "component_id": "vip-exclusive-banner",
      "type": "BANNER",
      "order": 3,
      "config": {
        "images": [
          {
            "url": "https://cdn.example.com/vip-banner.jpg",
            "link": "/vip/benefits"
          }
        ]
      },
      "display_rules": {
        "target_users": ["VIP"],
        "conditions": {
          "vip_level": {
            "operator": ">=",
            "value": 3
          }
        }
      }
    }
  ]
}
```

## 4. 實驗與優化 (Experimentation)
為提升轉換率 (CTR)，佈局引擎需支援 A/B 測試：
- **實驗配置**：
  - 在 Layout JSON 中添加 `experiment_id: "EXP_HOME_V2"`.
  - **Variant A**: Control Group (原版).
  - **Variant B**: Test Group (如：將 "熱門遊戲" 移至最頂部).
- **分流邏輯 (Traffic Splitting)**：
  - 基於 `DeviceID` 或 `PlayerID` 進行 Hash 取模：`hash(id) % 100 < 50 ? A : B`.
  - 確保同一用戶始終看到相同版本 (Consistency).
- **數據追蹤**：
  - 前端渲染時自動上報 `Exposure` 事件 (包含 `experiment_id`, `variant`).

##### 📊 Diagram 2: A/B 測試分流決策機制 (A/B Testing Traffic Splitting Mechanism)

```mermaid
flowchart TD
    START[用戶訪問首頁<br/>User visits homepage] --> ID{用戶識別?}

    ID -->|已登入| PLAYER_ID[使用 player_id<br/>穩定識別]
    ID -->|未登入| DEVICE_ID[使用 device_id<br/>瀏覽器指紋]

    PLAYER_ID --> HASH1[計算 Hash<br/>hash = murmur3 - player_id]
    DEVICE_ID --> HASH2[計算 Hash<br/>hash = murmur3 - device_id]

    HASH1 --> MOD[取模運算<br/>variant = hash % 100]
    HASH2 --> MOD

    MOD --> SPLIT{分流決策<br/>variant 值?}

    SPLIT -->|0-49 - 50% 流量| VARIANT_A[Variant A<br/>Control Group<br/>原版佈局]
    SPLIT -->|50-99 - 50% 流量| VARIANT_B[Variant B<br/>Test Group<br/>實驗佈局]

    VARIANT_A --> FETCH_A[拉取配置<br/>GET /api/v1/layout/config<br/>variant=A]
    VARIANT_B --> FETCH_B[拉取配置<br/>GET /api/v1/layout/config<br/>variant=B]

    FETCH_A --> CONFIG_A[Variant A 配置<br/>Banner → Game Grid - Hot → Deposit]
    FETCH_B --> CONFIG_B[Variant B 配置<br/>Game Grid - Hot → Banner → Deposit]

    CONFIG_A --> RENDER_A[渲染 Variant A<br/>組件順序: 1, 2, 3]
    CONFIG_B --> RENDER_B[渲染 Variant B<br/>組件順序: 2, 1, 3]

    RENDER_A --> EXPOSURE_A[上報曝光事件<br/>Event: experiment_exposure<br/>experiment_id: EXP_HOME_V2<br/>variant: A<br/>user_id: 12345<br/>timestamp: 2026-01-27T10:30:00Z]

    RENDER_B --> EXPOSURE_B[上報曝光事件<br/>Event: experiment_exposure<br/>experiment_id: EXP_HOME_V2<br/>variant: B<br/>user_id: 67890<br/>timestamp: 2026-01-27T10:30:05Z]

    EXPOSURE_A --> TRACK_A[追蹤轉換指標<br/>- Click on Game<br/>- Deposit Button Click<br/>- First Deposit Amount]

    EXPOSURE_B --> TRACK_B[追蹤轉換指標<br/>- Click on Game<br/>- Deposit Button Click<br/>- First Deposit Amount]

    TRACK_A --> ANALYTICS[數據分析<br/>Conversion Rate Comparison]
    TRACK_B --> ANALYTICS

    ANALYTICS --> COMPARE{統計顯著性?<br/>p-value < 0.05}

    COMPARE -->|是 + Variant B 更好| WINNER_B[🏆 Variant B 獲勝<br/>CTR: 8.5% vs 6.2% - A<br/>提升: +37%]
    COMPARE -->|是 + Variant A 更好| WINNER_A[🏆 Variant A 獲勝<br/>保持原版佈局]
    COMPARE -->|否 - 無顯著差異| NO_WINNER[⚠️ 無明顯差異<br/>需延長實驗時間]

    WINNER_B --> ROLLOUT[全量推廣<br/>100% 流量使用 Variant B]
    WINNER_A --> KEEP[維持現狀<br/>100% 流量使用 Variant A]
    NO_WINNER --> CONTINUE[繼續實驗<br/>擴大樣本量]

    ROLLOUT --> END[實驗結束<br/>發布新版佈局]
    KEEP --> END
    CONTINUE --> SPLIT

    style START fill:#E3F2FD,stroke:#1976D2,stroke-width:2px
    style SPLIT fill:#FFF9C4,stroke:#F57F17,stroke-width:2px
    style VARIANT_A fill:#B2DFDB,stroke:#00796B,stroke-width:2px
    style VARIANT_B fill:#FFE082,stroke:#F57F00,stroke-width:2px
    style EXPOSURE_A fill:#C8E6C9,stroke:#388E3C,stroke-width:2px
    style EXPOSURE_B fill:#FFE0B2,stroke:#E65100,stroke-width:2px
    style ANALYTICS fill:#CE93D8,stroke:#7B1FA2,stroke-width:2px
    style WINNER_B fill:#4CAF50,stroke:#1B5E20,stroke-width:3px,color:#FFF
    style WINNER_A fill:#4CAF50,stroke:#1B5E20,stroke-width:3px,color:#FFF
    style NO_WINNER fill:#FFC107,stroke:#F57F00,stroke-width:2px
    style END fill:#90A4AE,stroke:#455A64,stroke-width:2px
```

**A/B 測試實施細節**:

### 4.1 分流算法實現

```javascript
// murmur3 hash function (simplified)
function murmurHash3(key, seed = 0) {
  let hash = seed;
  for (let i = 0; i < key.length; i++) {
    hash ^= key.charCodeAt(i);
    hash += (hash << 10);
    hash ^= (hash >> 6);
  }
  hash += (hash << 3);
  hash ^= (hash >> 11);
  hash += (hash << 15);
  return hash >>> 0;  // Convert to unsigned 32-bit int
}

// 分流決策函數
function assignVariant(userId, experimentId) {
  const key = `${experimentId}:${userId}`;
  const hash = murmurHash3(key);
  const bucket = hash % 100;

  if (bucket < 50) {
    return 'A';  // Control Group (50%)
  } else {
    return 'B';  // Test Group (50%)
  }
}

// 使用範例
const userId = '12345';
const experimentId = 'EXP_HOME_V2';
const variant = assignVariant(userId, experimentId);
console.log(`User ${userId} assigned to Variant ${variant}`);
```

### 4.2 曝光事件上報

```javascript
// 前端追蹤代碼
function trackExperimentExposure(experimentId, variant, userId) {
  const event = {
    event_type: 'experiment_exposure',
    experiment_id: experimentId,
    variant: variant,
    user_id: userId,
    timestamp: new Date().toISOString(),
    page_url: window.location.href,
    user_agent: navigator.userAgent
  };

  // 發送至數據分析平台
  fetch('/api/v1/analytics/track', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify(event)
  });

  // 同時發送至 Google Analytics / Mixpanel
  gtag('event', 'experiment_exposure', {
    experiment_id: experimentId,
    variant: variant
  });
}

// 在組件渲染後自動上報
onMounted(() => {
  trackExperimentExposure('EXP_HOME_V2', variant, userId);
});
```

### 4.3 轉換指標追蹤

| 指標類型 | 指標名稱 | 定義 | 計算方式 |
|----------|----------|------|----------|
| **曝光指標** | Exposure Rate | 實驗頁面曝光率 | Exposed Users / Total Users |
| **點擊指標** | Click-through Rate (CTR) | 點擊遊戲按鈕比例 | Game Clicks / Exposed Users |
| **轉換指標** | Deposit Conversion Rate | 首次存款轉換率 | First Deposits / Exposed Users |
| **收入指標** | Average Revenue per User (ARPU) | 平均用戶收入 | Total Revenue / Exposed Users |
| **留存指標** | Day 7 Retention | 7 日留存率 | Active on Day 7 / Exposed Users |

### 4.4 實驗結果分析範例

```
實驗名稱: EXP_HOME_V2 - 首頁佈局優化
實驗時長: 2026-01-20 ~ 2026-01-27 (7 days)
曝光用戶: 50,000 人 (Variant A: 25,000, Variant B: 25,000)

指標對比:

| 指標 | Variant A (Control) | Variant B (Test) | 提升幅度 | P-value | 統計顯著性 |
|------|---------------------|------------------|----------|---------|-----------|
| CTR | 6.2% (1,550 clicks) | 8.5% (2,125 clicks) | +37% | 0.001 | ✅ 顯著 |
| Deposit Conversion | 2.1% (525 deposits) | 2.8% (700 deposits) | +33% | 0.003 | ✅ 顯著 |
| ARPU | $12.50 | $16.20 | +30% | 0.012 | ✅ 顯著 |
| Avg Session Time | 3.2 min | 4.1 min | +28% | 0.008 | ✅ 顯著 |

結論:
🏆 Variant B 在所有核心指標上均顯著優於 Variant A
💡 建議: 全量推廣 Variant B 至所有用戶
📈 預估影響: 月度存款額提升 +30% (~$150,000)
```

## 5. 動態規則
- **客群定向**：
  - 可設定 "僅 VIP 可見" 的專屬 Banner。
  - 可設定 "僅新註冊用戶可見" 的首存優惠廣告。

## 5. 錢包模式 UI 適配 (Wallet Mode UI Adaptation)
前端需根據 `player.wallet_mode` 自動切換 Header 資訊顯示：

### 5.1 Cash Mode (現金模式)
*   **顯示內容**：`Balance` (Total Cash + Bonus)
*   **特徵**：強調 "充值 (Deposit)" 按鈕。

### 5.2 Credit Mode (信用模式)
*   **顯示內容**：
    *   `Credit`: 信用額度 (Limit)。
    *   `Used`: 已用額度。
    *   `Available`: 可用額度 (重點顯示)。
*   **特徵**：
    *   隱藏 "充值" 按鈕，改為 "額度 (Quota)" 詳情頁。
    *   顯示 **"週結倒數 (Settlement Countdown)"**，提醒玩家結算日。

### 5.3 Hybrid Mode (混合模式)
*   **顯示內容**：同時顯示 Cash Balance 與 Available Credit。
*   **支付選擇器**：在下注或購買道具時，允許玩家選擇 "優先扣除 Cash" 或 "使用 Credit"。


---

**文檔版本**: 1.0.0
**最後更新**: 2026-01-28
**維護團隊**: Frontend Team & Product Team
