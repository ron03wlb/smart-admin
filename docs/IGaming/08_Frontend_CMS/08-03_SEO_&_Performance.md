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
- 針對多國營運站點，必須正確配置 `hreflang` 標籤，防止內容重複 (Duplicate Content) 懲罰。
- **範例**：
  ```html
  <link rel="alternate" hreflang="en" href="https://www.casino.com/games/slot1" />
  <link rel="alternate" hreflang="th" href="https://www.casino.com/th/games/slot1" />
  <link rel="alternate" hreflang="x-default" href="https://www.casino.com/games/slot1" />
  ```
