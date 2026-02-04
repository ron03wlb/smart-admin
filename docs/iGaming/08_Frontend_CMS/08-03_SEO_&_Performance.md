# 08-03 遊戲 SEO 與效能標準 (SEO & Performance Standards)

## 1. 系統概述
iGaming 產業高度依賴有機流量 (Organic Traffic)，本模組定義如何透過技術手段最大化 SEO 效益，確保數千款老虎機/遊戲頁面能被搜尋引擎有效索引，同時維持極致的加載效能。

## 2. 搜尋引擎優化 (SEO) 架構

### 2.1 渲染策略 (Rendering Strategy)
傳統 SPA (Single Page Application) 對爬蟲不友善，必須採用 **SSR (Server-Side Rendering)** 或 **ISR (Incremental Static Regeneration)**。

- **框架選型**：Next.js (React) 或 Nuxt.js (Vue)。
- **動態路由 (Dynamic Routing)**：
  - URL 結構：`/games/{provider}/{game-slug}` (例: `/games/pg-soft/mahjong-ways-2`)。
  - 每個遊戲獨立一個靜態 URL，而非透過 Query Params 切換。

### 2.2 自動化 Meta Data 生成
針對 3000+ 款遊戲，不可能人工維護 Meta Tags。需建立 **"Meta Template Engine"**：

- **Title Template**: 
  - `"{GameName} Slot - Play Free Demo & RPT {RTP}% | {SiteName}"`
- **Description Template**:
  - `"Play {GameName} by {Provider}. Features: {Volatility} volatility, {MaxWin}x max win. Try the free demo now!"`
- **Structured Data (Schema.org)**：
  - 自動注入 `VideoGame` 或 `SoftwareApplication` JSON-LD。
  - 標記 `aggregateRating`, `operatingSystem`, `applicationCategory`。

### 2.3 Sitemap 自動化
- **動態生成**：每日同步遊戲庫，自動將 `Active` 狀態的遊戲 URL 寫入 `sitemap-games.xml`。
- **分片 (Chunking)**：若 URL 超過 50,000 筆，自動分割為 `sitemap-games-1.xml`, `sitemap-games-2.xml`。

## 3. 效能優化標準 (Performance Standards)

### 3.1 Core Web Vitals 指標
- **LCP (Largest Contentful Paint)**: < 2.5s
- **FID (First Input Delay)**: < 100ms
- **CLS (Cumulative Layout Shift)**: < 0.1

### 3.2 資源加載優化
- **圖片優化**：
  - 全部採用 **WebP / AVIF** 格式。
  - 遊戲縮圖 (Thumbnail) 需採用 `Lazylad` + `BlurPlaceholder`。
- **遊戲預加載 (Pre-flight Loading)**：
  - 當滑鼠 Hover 到遊戲入口時，預先建立與遊戲伺服器的 TCP 連接 (Preconnect)。
  - 點擊 "開始遊戲" 後，顯示骨架屏 (Skeleton)，並非空白畫面。

### 3.3 CDN 策略
- **靜態資源**：JS/CSS/Images 透過 Cloudflare/Cloudfront 分發。
- **邊緣緩存 (Edge Cache)**：HTML 頁面 (尤其是首頁與遊戲詳情頁) 應在 CDN Edge 緩存 TTL 60秒，降低 Origin Server 負載。

## 4. 多語系 SEO (Hreflang)

### 4.1 Hreflang 配置

針對多國營運站點，必須正確配置 `hreflang` 標籤，防止內容重複 (Duplicate Content) 懲罰。

**HTML Head 標籤範例**：
```html
<link rel="alternate" hreflang="en" href="https://www.casino.com/games/slot1" />
<link rel="alternate" hreflang="th" href="https://www.casino.com/th/games/slot1" />
<link rel="alternate" hreflang="vi" href="https://www.casino.com/vi/games/slot1" />
<link rel="alternate" hreflang="x-default" href="https://www.casino.com/games/slot1" />
```

**Sitemap 整合**：
```xml
<url>
  <loc>https://www.casino.com/games/slot1</loc>
  <xhtml:link rel="alternate" hreflang="en" href="https://www.casino.com/games/slot1" />
  <xhtml:link rel="alternate" hreflang="th" href="https://www.casino.com/th/games/slot1" />
  <xhtml:link rel="alternate" hreflang="vi" href="https://www.casino.com/vi/games/slot1" />
</url>
```

### 4.2 URL 結構策略

**多語言 URL 模式選擇**：

| 模式 | URL 範例 | SEO 友好度 | 實作複雜度 |
|------|---------|-----------|-----------|
| **子域名** | `th.casino.com/games` | ⭐⭐⭐⭐⭐ 最佳 | 高（需多個 SSL 證書） |
| **子目錄** | `casino.com/th/games` | ⭐⭐⭐⭐ 推薦 | 中（路由配置） |
| **Query 參數** | `casino.com/games?lang=th` | ⭐⭐ 不推薦 | 低（SEO 不友善） |

**推薦方案**：使用子目錄模式（兼顧 SEO 與實作成本）

---

## 5. SSR/ISR 實作細節

### 5.1 Next.js ISR 配置

**遊戲詳情頁面範例**：
```javascript
// pages/games/[provider]/[slug].tsx
export async function getStaticPaths() {
  // 預渲染前 100 個熱門遊戲
  const topGames = await fetchTopGames(100);

  return {
    paths: topGames.map(game => ({
      params: { provider: game.provider, slug: game.slug }
    })),
    fallback: 'blocking' // 其他遊戲首次訪問時 SSR
  };
}

export async function getStaticProps({ params }) {
  const game = await fetchGameBySlug(params.provider, params.slug);

  return {
    props: { game },
    revalidate: 3600 // 每小時重新生成一次
  };
}
```

### 5.2 動態路由 SEO 最佳化

**Canonical URL 處理**：
```html
<!-- 防止參數污染 SEO -->
<link rel="canonical" href="https://www.casino.com/games/pg-soft/mahjong-ways-2" />
```

**Open Graph 標籤**：
```html
<meta property="og:title" content="Mahjong Ways 2 - Play Free Demo | Casino" />
<meta property="og:description" content="Play Mahjong Ways 2 by PG Soft. 96.81% RTP, Medium volatility." />
<meta property="og:image" content="https://cdn.casino.com/games/mahjong-ways-2.jpg" />
<meta property="og:url" content="https://www.casino.com/games/pg-soft/mahjong-ways-2" />
<meta property="og:type" content="website" />
```

---

## 6. CDN 與快取策略

### 6.1 多層快取架構

```
┌────────────────────────────────────────────┐
│  Cloudflare CDN (Edge Cache)               │
│  - HTML: TTL 60s                           │
│  - JS/CSS: TTL 7 days (immutable)          │
│  - Images: TTL 30 days                     │
└────────────────┬───────────────────────────┘
                 │
┌────────────────▼───────────────────────────┐
│  Origin Server (Next.js)                   │
│  - ISR 快取: 3600s                         │
│  - API 快取: Redis 300s                    │
└────────────────┬───────────────────────────┘
                 │
┌────────────────▼───────────────────────────┐
│  Database (MySQL)                          │
└────────────────────────────────────────────┘
```

### 6.2 快取失效策略

**主動清除快取**：
```javascript
// 遊戲資訊更新時，清除 CDN 快取
async function purgeGameCache(gameSlug) {
  await cloudflarePurge([
    `/games/*/${gameSlug}`,
    `/api/games/${gameSlug}`
  ]);

  await redisDel(`game:${gameSlug}`);
}
```

**Cache-Control Headers**：
```nginx
# 靜態資源 (帶版本號)
location /static/ {
    add_header Cache-Control "public, max-age=31536000, immutable";
}

# HTML 頁面
location / {
    add_header Cache-Control "public, max-age=60, s-maxage=60, stale-while-revalidate=120";
}
```

---

## 7. 圖片優化策略

### 7.1 現代圖片格式

**WebP 自動轉換**：
```html
<picture>
  <source srcset="/games/slot1.avif" type="image/avif" />
  <source srcset="/games/slot1.webp" type="image/webp" />
  <img src="/games/slot1.jpg" alt="Fortune Tiger Slot" loading="lazy" />
</picture>
```

**響應式圖片**：
```html
<img
  srcset="
    /games/slot1-320w.webp 320w,
    /games/slot1-640w.webp 640w,
    /games/slot1-1280w.webp 1280w
  "
  sizes="(max-width: 640px) 100vw, 50vw"
  src="/games/slot1-640w.webp"
  alt="Fortune Tiger"
  loading="lazy"
  decoding="async"
/>
```

### 7.2 圖片 CDN 優化

**Cloudflare Images / Imgix 整合**：
```
https://cdn.casino.com/games/slot1.jpg?w=400&h=300&fit=cover&fm=webp&q=85
```

**參數說明**：
- `w=400&h=400`: 指定尺寸（避免傳輸過大圖片）
- `fit=cover`: 裁切模式
- `fm=webp`: 自動轉 WebP（支援瀏覽器）
- `q=85`: 品質（85% 為最佳平衡點）

---

## 8. 性能監控

### 8.1 Real User Monitoring (RUM)

**Google Analytics 4 整合**：
```javascript
// 追蹤 Core Web Vitals
import { getCLS, getFID, getLCP } from 'web-vitals';

function sendToAnalytics({ name, value, id }) {
  gtag('event', name, {
    event_category: 'Web Vitals',
    value: Math.round(name === 'CLS' ? value * 1000 : value),
    event_label: id,
    non_interaction: true,
  });
}

getCLS(sendToAnalytics);
getFID(sendToAnalytics);
getLCP(sendToAnalytics);
```

### 8.2 Lighthouse CI

**自動化性能測試**：
```
# .github/workflows/lighthouse.yml
name: Lighthouse CI
on: [pull_request]

jobs:
  lighthouse:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - run: npm install && npm run build
      - run: |
          npx lhci autorun --config=lighthouserc.json
```

**性能預算配置** (lighthouserc.json):
```json
{
  "ci": {
    "assert": {
      "assertions": {
        "categories:performance": ["error", {"minScore": 0.9}],
        "first-contentful-paint": ["error", {"maxNumericValue": 2000}],
        "largest-contentful-paint": ["error", {"maxNumericValue": 2500}],
        "cumulative-layout-shift": ["error", {"maxNumericValue": 0.1}]
      }
    }
  }
}
```

---

## 9. 結構化數據 (Schema.org)

### 9.1 VideoGame Schema

```html
<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "VideoGame",
  "name": "Mahjong Ways 2",
  "description": "Exciting slot game with Mahjong theme",
  "gamePlatform": ["Web Browser", "iOS", "Android"],
  "applicationCategory": "Casino Game",
  "offers": {
    "@type": "Offer",
    "price": "0",
    "priceCurrency": "USD",
    "availability": "https://schema.org/InStock"
  },
  "aggregateRating": {
    "@type": "AggregateRating",
    "ratingValue": "4.7",
    "ratingCount": "1234",
    "bestRating": "5",
    "worstRating": "1"
  },
  "provider": {
    "@type": "Organization",
    "name": "PG Soft"
  }
}
</script>
```

---

## 📚 相關文檔

### 前置依賴
- [08-01 前端佈局引擎](./08-01_Frontend_Layout_Engine.md) - 頁面組件設計
- [08-05 本地化系統](./08-05_Localization_System.md) - 多語言 SEO

### 技術參考
- [12-03 網關架構](../07_Technical_Infrastructure_NEW/07-02-01_Gateway_Core.md) - CDN 配置
- [03-02 遊戲大廳管理](../03_Game_Center/03-02_Game_Lobby_Management.md) - 遊戲列表渲染

### 業務整合
- [01-01 玩家賬戶系統](../03_Player_Journey/03-01_Player_Lifecycle.md) - 用戶體驗優化
- [07-04 數據管道架構](../07_Platform_Management/07-04_Data_Pipeline_Architecture.md) - 性能數據分析

---

**最後更新**: 2026-01-27
**維護團隊**: Frontend Team

---

**文檔版本**: 1.0.0
**最後更新**: 2026-01-28
**維護團隊**: Frontend Team & Product Team
