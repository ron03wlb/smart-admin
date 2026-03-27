# SEO 與效能需求（SEO & Performance Requirements）

> **規範來源**: [source-archive/11_Frontend_CMS/11-03](../../source-archive/11_Frontend_CMS/11-03_SEO_and_Performance.md)
> **文件類型**: 業務需求
> **目標讀者**: 產品經理、SEO 專家、效能工程師
> **相關架構**: [SEO Performance Architecture](../../architecture/11_Frontend/04_SEO_Performance.md)
> **最後同步**: 2026-02-09

---

## 商業價值（Business Value）

此 SEO 與效能優化策略提供以下關鍵價值：
- **自然流量增長**：3000+ 遊戲的 SEO 友善 URL（`/games/{provider}/{game-slug}`）實現有效的搜尋引擎索引，與付費管道相比，可將獲客成本降低 40-60%
- **營運效率**：自動化 meta 模板引擎消除 3000+ 遊戲的手動 meta 標籤維護，將內容管理開銷減少 95%
- **使用者留存**：Core Web Vitals 合規（LCP < 2.5s、FID < 100ms、CLS < 0.1）將跳出率降低 25-35%，直接影響首次存款轉換
- **國際擴展**：多語言 SEO 配合 hreflang 標籤，防止重複內容處罰，並在 20+ 語言市場中實現排名，無 canonical 衝突
- **品質保證**：每次 pull request 的 Lighthouse CI 整合防止效能倒退，透過自動預算強制執行維持 Lighthouse 分數 ≥ 90

---

## 1. SEO 需求

### 1.1 自然流量優化

iGaming 產業高度依賴自然流量。平台必須最大化 SEO 效果，確保數千個老虎機/遊戲頁面被搜尋引擎有效索引。

**商業目標**：
- 每個遊戲必須擁有獨特、靜態、SEO 友善的 URL
- URL 結構：`/games/{provider}/{game-slug}`（例如：`/games/pg-soft/mahjong-ways-2`）
- 遊戲頁面嚴格禁止使用查詢參數路由

### 1.2 自動化 Meta 資料

擁有 3000+ 遊戲，手動維護 meta 標籤不可行。需要自動化 Meta 模板引擎。

**標題模板**：`"{GameName} Slot - Play Free Demo & RTP {RTP}% | {SiteName}"`

**描述模板**：`"Play {GameName} by {Provider}. Features: {Volatility} volatility, {MaxWin}x max win. Try the free demo now!"`

**結構化資料**：每個遊戲頁面必須包含 Schema.org JSON-LD 標記，包含 `aggregateRating`、`operatingSystem` 和 `applicationCategory`。

### 1.3 Sitemap 自動化

- 動態生成：每日同步遊戲庫，自動將活躍遊戲 URL 寫入 `sitemap-games.xml`
- 分塊：如果 URL 超過 50,000 個，自動拆分為多個 sitemap 檔案
- Sitemap 必須包含所有支援語言的 hreflang 註解

### 1.4 多語言 SEO（Hreflang）

對於多國營運，必須正確配置 hreflang 標籤以防止重複內容處罰。

**URL 策略**：建議使用子目錄模型（`casino.com/th/games`），平衡 SEO 友善性和實施成本。

| 模型 | SEO 友善性 | 實施複雜度 |
|------|-----------|-----------|
| 子網域（`th.casino.com`） | 最佳 | 高（多個 SSL 憑證） |
| 子目錄（`casino.com/th/`） | 建議 | 中（路由配置） |
| 查詢參數（`?lang=th`） | 不建議 | 低（SEO 不友善） |

---

## 2. 效能需求

### 2.1 Core Web Vitals 目標

| 指標 | 目標 | 說明 |
|------|------|------|
| **LCP**（Largest Contentful Paint） | < 2.5s | 最大元素渲染時間 |
| **FID**（First Input Delay） | < 100ms | 首次互動回應時間 |
| **CLS**（Cumulative Layout Shift） | < 0.1 | 視覺穩定性分數 |

### 2.2 Lighthouse 效能預算

- 效能分數必須 ≥ 90（錯誤閾值）
- First Contentful Paint 必須 < 2000ms
- LCP 必須 < 2500ms
- CLS 必須 < 0.1

### 2.3 資源載入標準

- 所有圖片必須使用 WebP/AVIF 格式
- 遊戲縮圖必須使用延遲載入配合模糊佔位符
- 遊戲預載入：滑鼠懸停時，預先建立到遊戲伺服器的 TCP 連線（preconnect）
- 點擊「開始遊戲」必須顯示骨架螢幕，絕不顯示空白頁面

### 2.4 CDN 需求

- 靜態資源（JS/CSS/圖片）必須透過全球 CDN 分發
- HTML 頁面（尤其是首頁和遊戲詳情頁面）必須在邊緣快取，TTL 60 秒
- 遊戲資訊更新時必須觸發快取失效

---

## 3. 行動裝置效能目標

- 冷啟動：< 2 秒
- 熱啟動：< 0.5 秒
- 圖片快取不得造成記憶體不足問題

---

## 4. 監控需求

### 4.1 真實使用者監控（RUM）

- Core Web Vitals 必須透過分析整合追蹤
- 效能資料必須在儀表板中提供以進行趨勢分析

### 4.2 自動化效能測試

- Lighthouse CI 必須在每次 pull request 時執行
- 效能預算違規必須封鎖合併

---

## 5. 驗收標準（Acceptance Criteria）

1. 所有遊戲頁面擁有獨特、SEO 友善的 URL 配合適當的 meta 標籤
2. Sitemap 每日自動生成並包含 hreflang 註解
3. Core Web Vitals 達成目標：LCP < 2.5s、FID < 100ms、CLS < 0.1
4. 所有關鍵頁面的 Lighthouse 分數 ≥ 90
5. 遊戲縮圖延遲載入配合模糊佔位符
6. CDN 邊緣快取配置：HTML 的 TTL 60s、靜態資源 7 天
7. 所有支援語言的 hreflang 標籤正確配置
