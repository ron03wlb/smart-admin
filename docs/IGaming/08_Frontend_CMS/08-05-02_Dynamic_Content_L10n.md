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

```sql
-- 活動表（支援多語言標題與描述）
CREATE TABLE promotions (
    promotion_id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,

    -- 多語言字段（JSONB）
    title JSONB NOT NULL,           -- {"en": "Welcome Bonus", "zh-TW": "歡迎禮金", "th": "โบนัสต้อนรับ"}
    description JSONB NOT NULL,     -- {"en": "Get 100% match...", "zh-TW": "獲得100%配對..."}
    terms JSONB,                    -- 條款與細則（多語言）

    -- 其他字段
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    bonus_type VARCHAR(20),
    created_at TIMESTAMP DEFAULT NOW(),

    -- JSONB 索引（加速語言查詢）
    INDEX idx_title_en ((title->>'en')),
    INDEX idx_title_zh ((title->>'zh-TW'))
);
```

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
```python
def get_user_language(request: Request, user: User = None) -> str:
    # 1. 用戶已登入 → 使用個人偏好
    if user and user.preferred_language:
        return user.preferred_language

    # 2. 讀取 Accept-Language Header
    accept_lang = request.headers.get('Accept-Language', '')
    if accept_lang:
        # 解析 "zh-TW,zh;q=0.9,en;q=0.8" 格式
        preferred_lang = accept_lang.split(',')[0].split(';')[0]
        if preferred_lang in SUPPORTED_LANGUAGES:
            return preferred_lang

    # 3. GeoIP 偵測（可選）
    client_ip = request.client.host
    country_code = geoip_lookup(client_ip)
    lang = COUNTRY_TO_LANGUAGE.get(country_code)
    if lang:
        return lang

    # 4. 預設回退
    return 'en'
```

### 3.2 JSONB 內容提取（SQL 層）

**方法 1：在 SQL 中提取語言**（推薦用於列表查詢）
```sql
-- 獲取所有活動（自動提取用戶語言的標題）
SELECT
    promotion_id,
    code,
    title->>'zh-TW' AS title,        -- 提取繁體中文標題
    description->>'zh-TW' AS description,
    start_time,
    end_time
FROM promotions
WHERE end_time > NOW()
  AND title ? 'zh-TW';  -- 確保該語言存在
```

**方法 2：返回完整 JSONB（應用層提取）**（推薦用於單筆查詢）
```sql
-- 返回完整的多語言 JSON
SELECT
    promotion_id,
    code,
    title,          -- 返回完整 JSONB: {"en": "...", "zh-TW": "..."}
    description,
    start_time,
    end_time
FROM promotions
WHERE promotion_id = 123;
```

**應用層提取**：
```python
def extract_localized_field(jsonb_field: dict, lang: str) -> str:
    """從 JSONB 字段提取指定語言的文字，支援回退"""
    if not jsonb_field:
        return ""

    # 1. 嘗試獲取指定語言
    if lang in jsonb_field:
        return jsonb_field[lang]

    # 2. 回退至主要語言（去除地區代碼）
    # 如 "zh-TW" → "zh"
    base_lang = lang.split('-')[0]
    for key in jsonb_field.keys():
        if key.startswith(base_lang):
            return jsonb_field[key]

    # 3. 回退至自定義 fallback
    if 'fallback' in jsonb_field:
        return jsonb_field['fallback']

    # 4. 回退至英文
    if 'en' in jsonb_field:
        return jsonb_field['en']

    # 5. 返回任意第一個語言
    return next(iter(jsonb_field.values()), "")
```

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
```python
@app.post("/api/v1/cms/promotions")
async def create_promotion(data: dict):
    # 前端提交格式
    # {
    #   "code": "WELCOME100",
    #   "title": {"en": "Welcome Bonus", "zh-TW": "歡迎禮金"},
    #   "description": {"en": "Get 100%...", "zh-TW": "獲得100%..."},
    #   ...
    # }

    # 驗證必填語言
    required_languages = ['en', 'zh-TW']
    for lang in required_languages:
        if lang not in data['title'] or not data['title'][lang]:
            raise HTTPException(400, f"Missing required language: {lang}")

    # 插入資料庫
    query = """
    INSERT INTO promotions (code, title, description, start_time, end_time)
    VALUES ($1, $2, $3, $4, $5)
    RETURNING promotion_id
    """

    promotion_id = await db.fetchval(
        query,
        data['code'],
        json.dumps(data['title']),        # JSONB 格式
        json.dumps(data['description']),
        data['start_time'],
        data['end_time']
    )

    return {"code": 1000, "promotion_id": promotion_id}
```

### 4.3 翻譯完整度追蹤

**自動檢測缺失語言**：
```sql
-- 查詢翻譯不完整的活動
SELECT
    promotion_id,
    code,
    title,
    ARRAY(SELECT jsonb_object_keys(title)) AS available_languages,
    ARRAY['en', 'zh-TW', 'th', 'vi'] AS required_languages,
    ARRAY['en', 'zh-TW', 'th', 'vi'] - ARRAY(SELECT jsonb_object_keys(title)) AS missing_languages
FROM promotions
WHERE NOT (title ?& ARRAY['en', 'zh-TW']);  -- 缺少必填語言
```

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

```sql
CREATE TABLE notification_templates (
    template_id BIGSERIAL PRIMARY KEY,
    template_code VARCHAR(50) UNIQUE NOT NULL,  -- e.g., "deposit_success"
    channel VARCHAR(20) NOT NULL,               -- 'email', 'sms', 'push'

    -- 多語言內容
    subject JSONB,      -- Email 主旨（僅 Email 使用）
    body JSONB NOT NULL,-- 內容主體

    -- 變量定義（JSON Schema）
    variables JSONB,    -- {"player_name": "string", "amount": "number"}

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 範例數據
INSERT INTO notification_templates (template_code, channel, subject, body, variables) VALUES
('deposit_success', 'email',
 '{"en": "Deposit Successful", "zh-TW": "存款成功", "th": "ฝากเงินสำเร็จ"}',
 '{"en": "Dear {player_name}, your deposit of {amount} {currency} has been credited.",
   "zh-TW": "親愛的 {player_name}，您的 {amount} {currency} 存款已到賬。",
   "th": "เรียน {player_name} การฝากเงินจำนวน {amount} {currency} เสร็จสมบูรณ์"}',
 '{"player_name": "string", "amount": "number", "currency": "string"}'
);
```

### 5.2 模板渲染引擎

**使用 Jinja2/Mustache 進行變量替換**：
```python
from jinja2 import Template

def render_notification(template_code: str, lang: str, variables: dict) -> dict:
    # 1. 從資料庫讀取模板
    template = await db.fetchrow(
        "SELECT subject, body FROM notification_templates WHERE template_code = $1",
        template_code
    )

    # 2. 提取指定語言的內容
    subject_text = extract_localized_field(template['subject'], lang)
    body_text = extract_localized_field(template['body'], lang)

    # 3. 渲染變量
    subject_rendered = Template(subject_text).render(**variables)
    body_rendered = Template(body_text).render(**variables)

    return {
        "subject": subject_rendered,
        "body": body_rendered
    }

# 使用範例
notification = await render_notification(
    template_code='deposit_success',
    lang='zh-TW',
    variables={
        'player_name': '張三',
        'amount': 1000,
        'currency': 'TWD'
    }
)
# 結果：
# {
#   "subject": "存款成功",
#   "body": "親愛的 張三，您的 1000 TWD 存款已到賬。"
# }
```

### 5.3 模板版本控制

**記錄模板修改歷史**：
```sql
CREATE TABLE notification_template_versions (
    version_id BIGSERIAL PRIMARY KEY,
    template_id BIGINT REFERENCES notification_templates(template_id),
    version INT NOT NULL,
    subject JSONB,
    body JSONB,
    modified_by VARCHAR(100),
    modified_at TIMESTAMP DEFAULT NOW(),

    UNIQUE (template_id, version)
);

-- 觸發器：每次更新模板時自動創建新版本
CREATE OR REPLACE FUNCTION save_template_version()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO notification_template_versions (template_id, version, subject, body, modified_by)
    VALUES (
        OLD.template_id,
        (SELECT COALESCE(MAX(version), 0) + 1 FROM notification_template_versions WHERE template_id = OLD.template_id),
        OLD.subject,
        OLD.body,
        current_user
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER template_version_trigger
BEFORE UPDATE ON notification_templates
FOR EACH ROW EXECUTE FUNCTION save_template_version();
```

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
```python
def get_localized_image_url(base_name: str, lang: str) -> str:
    cdn_base = "https://cdn.casino.com"

    # 1. 嘗試獲取指定語言的圖片
    localized_path = f"/banners/{base_name}_{lang}.jpg"
    if check_file_exists(cdn_base + localized_path):
        return cdn_base + localized_path

    # 2. 回退至預設圖片
    default_path = f"/banners/{base_name}_default.jpg"
    return cdn_base + default_path

# API 響應範例
{
  "banner_id": 101,
  "image_url": "https://cdn.casino.com/banners/new_year_promo_zh-TW.jpg",
  "fallback_url": "https://cdn.casino.com/banners/new_year_promo_default.jpg"
}
```

### 6.3 圖片內嵌文字檢測（QA 流程）

**使用 OCR 檢測圖片中的硬編碼文字**（CI/CD 自動化檢查）：
```python
import pytesseract
from PIL import Image

def detect_hardcoded_text_in_image(image_path: str) -> bool:
    """檢測圖片中是否包含文字（應避免）"""
    img = Image.open(image_path)
    text = pytesseract.image_to_string(img)

    if len(text.strip()) > 10:  # 超過 10 個字符視為包含文字
        print(f"⚠️ Warning: Image {image_path} contains hardcoded text: {text[:50]}")
        return True
    return False

# 在 CI/CD 中執行
for img in glob.glob('/cdn/banners/*.jpg'):
    if detect_hardcoded_text_in_image(img):
        raise Exception(f"Image {img} should not contain hardcoded text. Use CSS overlays instead.")
```

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
```sql
-- 為常用語言創建表達式索引
CREATE INDEX idx_promotions_title_en ON promotions ((title->>'en'));
CREATE INDEX idx_promotions_title_zh ON promotions ((title->>'zh-TW'));
CREATE INDEX idx_promotions_title_th ON promotions ((title->>'th'));

-- 查詢效能對比
EXPLAIN ANALYZE
SELECT * FROM promotions WHERE title->>'zh-TW' LIKE '%歡迎%';
-- 使用索引後：Execution Time: 5ms（vs 未索引：150ms）
```

### 8.2 應用層快取

**Redis 快取動態內容**：
```python
import redis
import json

redis_client = redis.Redis(host='localhost', port=6379)

async def get_promotion_localized(promotion_id: int, lang: str):
    # 1. 嘗試從 Redis 讀取
    cache_key = f"promotion:{promotion_id}:{lang}"
    cached = redis_client.get(cache_key)
    if cached:
        return json.loads(cached)

    # 2. 從資料庫讀取
    promotion = await db.fetchrow(
        "SELECT promotion_id, code, title, description FROM promotions WHERE promotion_id = $1",
        promotion_id
    )

    # 3. 提取語言並快取
    localized_data = {
        "promotion_id": promotion['promotion_id'],
        "code": promotion['code'],
        "title": extract_localized_field(promotion['title'], lang),
        "description": extract_localized_field(promotion['description'], lang)
    }

    redis_client.setex(cache_key, 3600, json.dumps(localized_data))  # 1 小時 TTL
    return localized_data
```

---

## 9. 相關文檔

### 系列文檔
- [08-05-01 i18n 架構與服務設計](./08-05-01_i18n_Architecture.md) - 翻譯服務架構、CDN分發
- [08-05-03 翻譯工作流](./08-05-03_Translation_Workflow.md) - 狀態機、Crowdin整合
- [08-05-04 API規格](./08-05-04_API_Specification.md) - 完整API文檔、監控

### 業務邏輯參考
- [04-01 活動系統設計](../04_Activity_Center/04-01_Activity_System_Design.md) - 活動多語言內容
- [08-02 Banner 管理](./08-02_Banner_&_Announcement.md) - 橫幅多語言
- [11-01 客服中台設計](../11_Customer_Service/11-01_CS_Platform_Design.md) - 通知模板本地化

### 技術架構參考
- [12-05 API設計標準](../12_Technical_Operations/12-05_API_Design_Standard.md) - API規範
- [09-03 數據安全標準](../09_System_Security/09-03_Data_Security_Standard.md) - JSONB 加密

---

**文檔版本**: 1.0.0
**最後更新**: 2026-01-27
**維護團隊**: Frontend Team & Backend Team
