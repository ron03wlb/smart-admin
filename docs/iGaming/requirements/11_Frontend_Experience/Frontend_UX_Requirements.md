# 前端使用者體驗需求（Frontend UX Requirements）

> **Canonical Source**: [source-archive/11_Frontend_CMS/11-01](../../source-archive/11_Frontend_CMS/11-01_Frontend_Layout_Engine.md), [11-02](../../source-archive/11_Frontend_CMS/11-02_Banner_and_Announcement.md)
> **View Type**: Business Requirements
> **Target Audience**: Product Managers, UX Designers, Operations Team
> **Related Architecture**: [Frontend Layout Engine Architecture](../../architecture/11_Frontend/Frontend_Layout_Engine.md), [Banner Announcement Architecture](../../architecture/11_Frontend/Banner_Announcement.md)
> **Last Synced**: 2026-02-09

---

## Business Value

此功能提供以下價值：
- **縮短促銷活動上線時間（Reducing Time-to-Market for Promotions）**：無程式碼頁面編輯器消除對工程團隊的依賴，使營運團隊能在數小時而非數天內啟動新活動（部署速度提升 80%+）
- **提高轉換率（Increasing Conversion Rates）**：錢包模式 UI 適配（現金/信用/混合顯示）降低支付摩擦，通過提供清晰、情境化的餘額資訊提高存款轉換率
- **防止促銷錯誤造成的收益損失（Preventing Revenue Loss from Promotion Errors）**：強制審批流程（營運專員 → 營運主管 → CTO 緊急下架）防止財務損失的橫幅錯誤（例如「存 100 送 1000」打字錯誤）
- **提升玩家參與度（Improving Player Engagement）**：多語言橫幅支援（6+ 種語言加回退邏輯）以及按地理/VIP/時間的定向投放，相較通用活動提升 CTR 2-3 倍
- **優化行銷投資回報率（Optimizing Marketing ROI）**：橫幅分析（曝光、點擊、CTR、轉換、CVR）搭配 A/B 測試實現數據驅動的活動優化，降低獲客成本 30%+

## Acceptance Criteria

- [ ] 無程式碼頁面編輯器允許營運人員透過拖放調整首頁佈局，無需工程支援
- [ ] 主題切換實現一鍵切換整站配色方案（深色、淺色、節慶限定）
- [ ] 多終端支援：佈局配置在 Web、H5 和 App 之間自適應，並具有獨立元件可見性設定
- [ ] 錢包模式 UI 適配：Header 根據玩家配置自動切換現金模式（強調「存款」）、信用模式（顯示額度/結算倒計時）和混合模式（支付選擇器）
- [ ] 發佈前預覽：所有佈局變更產生預覽 URL 供內部測試，不影響生產用戶
- [ ] 審批流程：所有橫幅內容經過營運專員（提交）→ 營運主管（核准）→ CTO（緊急下架）才能發佈
- [ ] 多語言橫幅支援：橫幅圖片支援 6+ 種語言（zh-CN, en-US, vi-VN, th-TH, pt-BR, ja-JP）並具備回退邏輯（用戶語言 → 英文 → 簡體中文 → 預設圖片）
- [ ] 橫幅定向：系統正確按用戶區段（新/活躍/不活躍/高額玩家）、VIP 等級、裝置（桌面/行動/兩者）、地理位置和時間規則過濾橫幅
- [ ] 橫幅分析：儀表板顯示即時曝光、點擊、CTR、轉換和 CVR，並具備歸因邏輯與 A/B 測試支援
- [ ] 圖片優化：所有橫幅符合技術規格（桌面 ≤200KB @1920x600、行動 ≤150KB @750x400、彈窗 ≤100KB @600x600），使用 WebP/PNG 格式與懶加載

---

## 1. 佈局配置需求（Layout Configuration Requirements）

### 1.1 無程式碼頁面編輯器（No-Code Page Editor）

營運人員必須能夠透過拖放介面調整前端首頁佈局，無需工程介入。

**業務規則**：
- 主題切換：平台提供多個預建主題（深色、淺色、節慶限定）
- 商家可一鍵切換整站配色方案
- 元件庫必須包含：Banner 輪播、跑馬燈、遊戲網格（3/4/5 欄可配置）、存款引導按鈕
- 頁面編輯器支援拖放重新排序模組與自訂模組標題
- 導航列（Header/Footer）支援自訂選單排序，可連結內部頁面或外部連結

### 1.2 多終端支援（Multi-Terminal Support）

- 佈局配置必須在 Web、H5 和 App 終端之間自適應
- 每個終端可有獨立元件可見性設定

### 1.3 錢包模式 UI 適配（Wallet Mode UI Adaptation）

前端必須根據玩家錢包模式自動切換 Header 資訊顯示：

| 錢包模式 | 顯示內容 | 關鍵功能 |
|-------------|----------------|--------------|
| **現金模式（Cash Mode）** | 餘額（總現金 + 紅利） | 強調「存款」按鈕 |
| **信用模式（Credit Mode）** | 信用額度、已用、可用 | 隱藏存款按鈕；顯示「額度」詳情頁；顯示結算倒計時 |
| **混合模式（Hybrid Mode）** | 現金餘額與可用信用 | 支付選擇器：「優先扣現金」或「使用信用」 |

---

## 2. 審批與發佈需求（Approval and Publishing Requirements）

### 2.1 發佈前預覽（Preview Before Publish）

- 編輯完成必須產生預覽 URL 供內部測試
- 預覽必須可查看但不影響生產用戶

### 2.2 發佈流程（Publishing Workflow）

- 「發佈」動作產生版本號（例如 v1.0.1）
- 配置推送至 CDN 進行全球分發
- 前端透過 API 拉取最新配置
- 版本檢查：僅在本地版本過期時下載新配置

### 2.3 強制審批（Approval Mandate）

所有橫幅內容必須經過強制審批：
- **營運專員（Operations Specialist）**：提交橫幅、編輯草稿
- **營運主管（Operations Supervisor）**：審查橫幅內容與目標連結、核准發佈
- **CTO**：嚴重錯誤的緊急下架權限

**理由**：錯誤的促銷內容（例如「存 100 送 1000」）可能導致巨大財務損失。

---

## 3. 橫幅與公告需求（Banner and Announcement Requirements）

### 3.1 橫幅類型與屬性（Banner Types and Attributes）

| 橫幅類型 | 屬性 |
|------------|------------|
| PC 首頁輪播 | 圖片（多語言）、跳轉連結（Deep Link）、有效時間範圍、排序順序 |
| H5 首頁輪播 | 同 PC，具備行動響應式尺寸 |
| 彈窗廣告 | 同上屬性加顯示頻率控制 |

### 3.2 多語言橫幅管理（Multi-Language Banner Management）

**語言矩陣**：

| 語言 | 必需 | 優先級 |
|----------|----------|----------|
| zh-CN（簡體中文） | 必須 | P0 |
| en-US（英文） | 必須 | P0 |
| vi-VN（越南文） | 必須 | P1 |
| th-TH（泰文） | 必須 | P1 |
| pt-BR（葡萄牙文） | 可選 | P2 |
| ja-JP（日文） | 可選 | P2 |

**回退邏輯**：用戶語言 → 英文 → 簡體中文（平台預設）→ 預設圖片

### 3.3 跑馬燈（Marquee，Scrolling Announcements）

- **系統自動產生**：祝賀訊息（例如「玩家 xxx 在遊戲 yyy 中贏得 $10,000」）
- **手動發佈**：平台維護通知、新支付通道公告
- **播放配置**：速度、顏色、循環次數均可配置

### 3.4 信箱訊息（Inbox Messages，Station Mail）

- **目標受眾**：廣播（所有用戶）、區段（例如「所有 VIP3+ 玩家」）、個人
- **模板支援**：HTML 格式，可插入圖片與按鈕

---

## 4. 橫幅顯示規則（定向投放，Banner Display Rules, Targeting）

### 4.1 用戶區段定向（User Segment Targeting）

| 區段 | 條件 | 橫幅類型 |
|---------|----------|-------------|
| 新玩家 | 註冊 < 7 天 | 首存促銷 |
| 活躍玩家 | 最近 7 天登入 | 遊戲推薦 |
| 不活躍玩家 | 30 天未登入 | 回歸紅利 |
| 高額玩家 | 月存款 > $10,000 | VIP 專屬活動 |

### 4.2 VIP 等級定向（VIP Level Targeting）

| VIP 等級 | 可見橫幅 |
|-----------|----------------|
| Bronze | 基本活動橫幅 |
| Silver | 標準活動 + 每週返水 |
| Gold | VIP 活動 + 生日紅利 |
| Platinum | VIP 專屬 + 客製化服務 |
| Diamond | 頂級活動 + 專屬客戶經理 |

### 4.3 裝置定向（Device Targeting）

- **僅桌面（Desktop Only）**：僅桌面站點的大型橫幅
- **僅行動（Mobile Only）**：行動適配的小螢幕橫幅
- **兩者（Both）**：所有平台的響應式設計

### 4.4 地理定向（Geo Targeting）

- 橫幅可按國家/地區限制
- 按地理位置的支付專屬橫幅（例如中國的銀聯、泰國的 PromptPay）

### 4.5 時間規則（Time-Based Rules）

- **固定時間窗口**：例如「夜間存款紅利」每日 18:00-23:00 顯示
- **週末專屬**：僅週六/週日顯示
- **節慶活動**：農曆新年、聖誕節期間專屬橫幅

---

## 5. 橫幅分析需求（Banner Analytics Requirements）

### 5.1 核心指標（Core Metrics）

| 指標 | 定義 |
|--------|-----------|
| 曝光（Impressions） | 橫幅被載入的次數 |
| 點擊（Clicks） | 橫幅被點擊的次數 |
| CTR（Click-Through Rate，點擊率） | 點擊 / 曝光 × 100% |
| 轉換（Conversions） | 點擊後完成目標動作（例如存款） |
| CVR（Conversion Rate，轉換率） | 轉換 / 點擊 × 100% |

### 5.2 歸因邏輯（Attribution Logic）

- 橫幅點擊必須追蹤轉換歸因
- 支援 A/B 測試與變體比較

---

## 6. 圖片需求（Image Requirements）

### 6.1 技術規格（Technical Specifications）

| 裝置類型 | 最大檔案大小 | 建議尺寸（px） |
|------------|---------------|----------------------|
| 桌面橫幅 | 200 KB | 1920x600 |
| 行動橫幅 | 150 KB | 750x400 |
| 彈窗廣告 | 100 KB | 600x600 |
| 跑馬燈圖示 | 20 KB | 32x32 |

### 6.2 格式需求（Format Requirements）

- WebP/PNG 首選格式
- 多裝置支援需要響應式圖片
- 所有非關鍵圖片懶加載

---

## 7. 驗收標準（Acceptance Criteria）

- [ ] 營運人員可在無開發人員協助下建立、編輯、預覽和發佈頁面佈局
- [ ] 橫幅系統支援多語言並具備適當回退（用戶語言 → 英文 → zh-CN → 預設圖片）
- [ ] 所有橫幅在發佈前經過強制審批流程（專員 → 主管）
- [ ] 定向規則正確按用戶區段、VIP 等級、裝置、地理位置和時間過濾橫幅
- [ ] 分析儀表板顯示即時 CTR 與轉換指標，並具備歸因追蹤
- [ ] 錢包模式 UI 根據玩家配置（現金/信用/混合）正確適配 Header 顯示
- [ ] 主題切換允許一鍵切換所有頁面元件的整站配色方案
- [ ] 多終端支援為 Web、H5 和 App 提供自適應佈局，並具備獨立元件可見性
- [ ] 預覽 URL 產生允許內部測試而不影響生產用戶
- [ ] 版本控制在發佈時產生版本號並將配置推送至 CDN
