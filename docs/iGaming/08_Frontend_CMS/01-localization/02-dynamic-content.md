# 08-05-02 動態內容本地化 (Dynamic Content Localization)

## 1. 系統概述

靜態翻譯鍵值（如按鈕文字、菜單項）可通過 CDN 分發的 JSON 檔案解決，但動態內容（如活動標題、橫幅文案、遊戲描述）需要從資料庫中即時讀取並支援多語言。本文檔定義動態內容的本地化策略。

---

## 2. 資料庫設計：JSONB 多語言字段

### 2.1 核心設計原則

**使用 PostgreSQL JSONB 儲存多語言內容**：
- **優點**：Schema 靈活、查詢高效、支援索引
- **缺點**：需要應用層處理語言回退邏輯

**範例資料表設計**：


### 2.2 JSONB 字段結構規範

**標準格式**：
```json
{
  "en": "English text",
  "zh-TW": "繁體中文",
  "zh-CN": "简体中文",
  "th": "ข้อความภาษาไทย",
  "vi": "Văn bản tiếng Việt",
  "id": "Teks bahasa Indonesia",
  "pt-BR": "Texto em português",
  "ar": "نص عربي",
  "fallback": "Default text if language not found"
}
```

**必填字段**：
- `en`（英文）作為預設回退語言
- 平台主要語言（如 `zh-TW`）

**可選字段**：
- 其他支援語言
- `fallback`（自定義回退文字）

### 2.3 其他需要本地化的資料表

| 資料表 | 多語言字段 | 示例 |
|-------|----------|------|
| **games** | `name`, `description`, `rules` | 遊戲名稱、玩法說明 |
| **banners** | `title`, `subtitle`, `cta_text` | 橫幅標題、副標題、行動呼籲 |
| **notifications** | `title`, `body` | 站內通知標題與內容 |
| **faqs** | `question`, `answer` | 常見問題 |
| **vip_tiers** | `tier_name`, `benefits` | VIP等級名稱與權益 |
| **payment_methods** | `display_name`, `instructions` | 支付方式名稱與說明 |

---

## 3. API 響應策略

### 3.1 後端語言選擇邏輯

**優先級順序**（瀑布式回退）：
1. **用戶偏好語言**（從 JWT Token 或 User Profile 讀取）
2. **HTTP Header `Accept-Language`**
3. **GeoIP 偵測語言**（根據 IP 推斷）
4. **平台預設語言**（通常為 `en`）

**實現範例（Python/FastAPI）**：

### 3.2 JSONB 內容提取（SQL 層）

**方法 1：在 SQL 中提取語言**（推薦用於列表查詢）

**方法 2：返回完整 JSONB（應用層提取）**（推薦用於單筆查詢）

**應用層提取**：

### 3.3 API 響應格式設計

**Option A：單語言響應**（推薦用於移動應用）
```json
GET /api/v1/promotions?lang=zh-TW

Response:
{
  "code": 1000,
  "data": [
    {
      "promotion_id": 123,
      "code": "WELCOME100",
      "title": "歡迎禮金",              // 直接返回繁體中文
      "description": "首次存款獲得100%配對紅利...",
      "start_time": "2026-01-01T00:00:00Z",
      "end_time": "2026-12-31T23:59:59Z"
    }
  ]
}
```

**Option B：多語言響應**（推薦用於 CMS 後台）
```json
GET /api/v1/promotions/123?include_all_languages=true

Response:
{
  "code": 1000,
  "data": {
    "promotion_id": 123,
    "code": "WELCOME100",
    "title": {
      "en": "Welcome Bonus",
      "zh-TW": "歡迎禮金",
      "th": "โบนัสต้อนรับ"
    },
    "description": {
      "en": "Get 100% match on your first deposit...",
      "zh-TW": "首次存款獲得100%配對紅利...",
      "th": "รับโบนัสแมตช์ 100% ในการฝากครั้งแรก..."
    },
    "start_time": "2026-01-01T00:00:00Z",
    "end_time": "2026-12-31T23:59:59Z"
  }
}
```

---

## 4. CMS 動態內容整合

### 4.1 CMS 編輯器設計

**多語言內容編輯器 UI 設計**：

```html
<!-- 活動編輯表單 -->
<form>
  <!-- 基本信息 -->
  <input name="code" placeholder="Promotion Code (e.g., WELCOME100)" />

  <!-- 多語言標題編輯器（Tab 切換）-->
  <div class="multilingual-editor">
    <ul class="language-tabs">
      <li class="active">🇬🇧 English</li>
      <li>🇹🇼 繁體中文</li>
      <li>🇹🇭 ไทย</li>
      <li>🇻🇳 Tiếng Việt</li>
    </ul>

    <div class="tab-content">
      <!-- 英文標題 -->
      <div class="tab-pane active">
        <input name="title[en]" placeholder="Title in English" />
        <textarea name="description[en]" placeholder="Description..."></textarea>
      </div>

      <!-- 繁體中文標題 -->
      <div class="tab-pane">
        <input name="title[zh-TW]" placeholder="繁體中文標題" />
        <textarea name="description[zh-TW]" placeholder="活動描述..."></textarea>
      </div>

      <!-- 其他語言... -->
    </div>
  </div>

  <!-- 翻譯完整度指示器 -->
  <div class="translation-progress">
    <span>Translation Completeness: 75% (3/4 languages)</span>
    <span class="missing-languages">Missing: Vietnamese</span>
  </div>

  <button type="submit">Save Promotion</button>
</form>
```

### 4.2 CMS 後端保存邏輯

**接收前端提交的多語言數據**：

### 4.3 翻譯完整度追蹤

**自動檢測缺失語言**：

**CMS 儀表板顯示**：
```
翻譯完整度報告（過去 7 天創建的內容）
┌──────────────┬────────────┬──────────────┐
│ 內容類型     │ 完整度     │ 缺失語言     │
├──────────────┼────────────┼──────────────┤
│ 活動（25筆） │ 88% (22/25)│ 3筆缺泰文    │
│ 橫幅（10筆） │ 100%       │ 無           │
│ FAQ（50筆）  │ 76% (38/50)│ 12筆缺越南文 │
└──────────────┴────────────┴──────────────┘
```

---

## 5. Email/SMS 模板本地化

### 5.1 模板資料表設計


### 5.2 模板渲染引擎

**使用 Jinja2/Mustache 進行變量替換**：

### 5.3 模板版本控制

**記錄模板修改歷史**：

---

## 6. 圖片與媒體資源本地化

### 6.1 本地化圖片命名規範

**檔案命名格式**：`{resource_name}_{lang}.{ext}`

**範例**：
```
/cdn/banners/
  ├── new_year_promo_en.jpg      // 英文版橫幅
  ├── new_year_promo_zh-TW.jpg   // 繁體中文版橫幅
  ├── new_year_promo_th.jpg      // 泰文版橫幅
  └── new_year_promo_default.jpg // 預設回退圖片
```

### 6.2 CDN URL 動態組合

**後端邏輯**：

### 6.3 圖片內嵌文字檢測（QA 流程）

**使用 OCR 檢測圖片中的硬編碼文字**（CI/CD 自動化檢查）：

**最佳實踐**：
- ✅ 使用純圖形背景 + CSS/HTML 疊加文字（可翻譯）
- ❌ 避免在圖片中直接嵌入文字（無法翻譯）

---

## 7. 前端動態內容渲染

### 7.1 React 組件範例

```jsx
import { useTranslation } from 'react-i18next';
import { useLanguage } from '@/hooks/useLanguage';

function PromotionCard({ promotion }) {
  const { currentLanguage } = useLanguage();

  // 從 JSONB 字段提取當前語言的文字
  const title = promotion.title[currentLanguage] || promotion.title['en'];
  const description = promotion.description[currentLanguage] || promotion.description['en'];

  return (
    <div className="promotion-card">
      <h3>{title}</h3>
      <p>{description}</p>
      <button>{t('common.button.claim')}</button>  {/* 靜態翻譯 */}
    </div>
  );
}
```

### 7.2 Vue 組件範例

```vue
<template>
  <div class="promotion-card">
    <h3>{{ localizedTitle }}</h3>
    <p>{{ localizedDescription }}</p>
    <button>{{ $t('common.button.claim') }}</button>
  </div>
</template>

<script>
export default {
  props: ['promotion'],
  computed: {
    currentLang() {
      return this.$i18n.locale;
    },
    localizedTitle() {
      return this.promotion.title[this.currentLang] || this.promotion.title['en'];
    },
    localizedDescription() {
      return this.promotion.description[this.currentLang] || this.promotion.description['en'];
    }
  }
}
</script>
```

---

## 8. 效能優化

### 8.1 JSONB 索引優化

**GIN 索引加速 JSONB 查詢**：

### 8.2 應用層快取

**Redis 快取動態內容**：

---

## 📚 相關文檔

### 系列文檔
- [08-05-01 i18n 架構與服務設計](./08-05-01_i18n_Architecture.md) - 翻譯服務架構、CDN分發
- [08-05-03 翻譯工作流](./08-05-03_Translation_Workflow.md) - 狀態機、Crowdin整合
- [08-05-04 API規格](./08-05-04_API_Specification.md) - 完整API文檔、監控

### 業務邏輯參考
- [04-01 活動系統設計](../03_Player_Journey/03-03_Activity_Bonus.md) - 活動多語言內容
- [08-02 Banner 管理](./08-02_Banner_&_Announcement.md) - 橫幅多語言
- [11-01 客服中台設計](../06_Analytics_Operations_NEW/06-02_Customer_Service.md) - 通知模板本地化

### 技術架構參考
- [12-05 API設計標準](../12_Technical_Operations/12-05_API_Design_Standard.md) - API規範
- [09-03 數據安全標準](../09_System_Security/09-03_Data_Security_Standard.md) - JSONB 加密

---

**文檔版本**: 1.0.0
**最後更新**: 2026-01-27
**維護團隊**: Frontend Team & Backend Team
