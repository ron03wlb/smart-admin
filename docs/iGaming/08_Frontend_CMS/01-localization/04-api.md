# 08-05-04 API 規格與監控 (API Specification & Monitoring)

## 1. 系統概述

本文檔定義翻譯服務的完整 API 規格、請求/響應格式、錯誤處理、效能監控與 SLA 指標。

---

## 2. API 端點總覽

### 2.1 端點清單

| 方法 | 端點 | 說明 | 權限 |
|------|------|------|------|
| **GET** | `/api/v1/i18n/translations` | 獲取翻譯列表 | Public |
| **GET** | `/api/v1/i18n/translations/{key}` | 獲取單個翻譯 | Public |
| **POST** | `/api/v1/i18n/translations` | 創建新翻譯 | Translator |
| **PUT** | `/api/v1/i18n/translations/{key}` | 更新翻譯 | Translator |
| **DELETE** | `/api/v1/i18n/translations/{key}` | 刪除翻譯 | Admin |
| **POST** | `/api/v1/i18n/translations/batch` | 批量導入翻譯 | Translator |
| **GET** | `/api/v1/i18n/export` | 導出翻譯（JSON/CSV/XLIFF）| Translator |
| **POST** | `/api/v1/i18n/missing-keys` | 上報缺失鍵值 | Public |
| **GET** | `/api/v1/i18n/namespaces` | 獲取命名空間列表 | Public |
| **POST** | `/api/v1/i18n/translations/{key}/publish` | 發佈翻譯至 CDN | Publisher |

---

## 3. 詳細 API 規格

### 3.1 獲取翻譯列表

**請求**：
```http
GET /api/v1/i18n/translations?lang=zh-TW&namespace=game&status=published&page=1&page_size=50

Headers:
Accept-Language: zh-TW
Authorization: Bearer <optional_jwt_token>
```

**查詢參數**：
| 參數 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `lang` | string | 是 | 語言代碼（如 `zh-TW`, `en`）|
| `namespace` | string | 否 | 命名空間過濾（如 `game`, `player`）|
| `status` | string | 否 | 狀態過濾（`draft`, `published`）|
| `page` | int | 否 | 頁碼（預設 1）|
| `page_size` | int | 否 | 每頁數量（預設 50，最大 200）|
| `search` | string | 否 | 關鍵字搜尋（搜尋 key 或 value）|

**響應**（成功）：
```json
{
  "code": 1000,
  "message": "Success",
  "data": {
    "translations": [
      {
        "translation_id": 12345,
        "key": "game.slot.freespin_won",
        "lang": "zh-TW",
        "namespace": "game",
        "value": "您贏得了 {amount} 次免費旋轉！",
        "context": "Slot game win notification",
        "status": "published",
        "version": 3,
        "created_at": "2026-01-20T10:00:00Z",
        "updated_at": "2026-01-27T14:30:00Z"
      },
      {
        "translation_id": 12346,
        "key": "game.table.bet_placed",
        "lang": "zh-TW",
        "value": "下注成功",
        "status": "published",
        ...
      }
    ],
    "pagination": {
      "current_page": 1,
      "page_size": 50,
      "total_pages": 5,
      "total_count": 234
    }
  }
}
```

**響應**（錯誤）：
```json
{
  "code": 4001,
  "message": "Invalid language code",
  "details": {
    "lang": "Language 'xyz' is not supported. Supported languages: en, zh-TW, zh-CN, th, vi, id, pt-BR, ar, he"
  }
}
```

### 3.2 獲取單個翻譯

**請求**：
```http
GET /api/v1/i18n/translations/game.slot.freespin_won?lang=zh-TW
```

**響應**：
```json
{
  "code": 1000,
  "data": {
    "translation_id": 12345,
    "key": "game.slot.freespin_won",
    "lang": "zh-TW",
    "namespace": "game",
    "value": "您贏得了 {amount} 次免費旋轉！",
    "context": "Slot game win notification",
    "status": "published",
    "version": 3,
    "created_at": "2026-01-20T10:00:00Z",
    "updated_at": "2026-01-27T14:30:00Z",
    "created_by": "alice@casino.com",
    "updated_by": "bob@casino.com"
  }
}
```

**錯誤響應**（404）：
```json
{
  "code": 4004,
  "message": "Translation not found",
  "details": {
    "key": "game.slot.freespin_won",
    "lang": "zh-TW"
  }
}
```

### 3.3 創建新翻譯

**請求**：
```http
POST /api/v1/i18n/translations
Content-Type: application/json
Authorization: Bearer <jwt_token>

{
  "key": "game.slot.bonus_round_start",
  "lang": "zh-TW",
  "namespace": "game",
  "value": "紅利回合開始！",
  "context": "Triggered when player enters bonus round",
  "status": "draft"
}
```

**響應**（成功）：
```json
{
  "code": 1000,
  "message": "Translation created successfully",
  "data": {
    "translation_id": 12347,
    "key": "game.slot.bonus_round_start",
    "lang": "zh-TW",
    "value": "紅利回合開始！",
    "status": "draft",
    "created_at": "2026-01-27T15:00:00Z"
  }
}
```

**錯誤響應**（409 - 鍵值已存在）：
```json
{
  "code": 4009,
  "message": "Translation key already exists",
  "details": {
    "key": "game.slot.bonus_round_start",
    "lang": "zh-TW",
    "existing_translation_id": 12300
  },
  "suggestion": "Use PUT /api/v1/i18n/translations/{key} to update existing translation"
}
```

### 3.4 更新翻譯

**請求**：
```http
PUT /api/v1/i18n/translations/game.slot.freespin_won?lang=zh-TW
Content-Type: application/json
Authorization: Bearer <jwt_token>

{
  "value": "恭喜！您贏得了 {amount} 次免費旋轉！",
  "change_reason": "添加祝賀語以提升玩家體驗"
}
```

**響應**：
```json
{
  "code": 1000,
  "message": "Translation updated successfully",
  "data": {
    "translation_id": 12345,
    "key": "game.slot.freespin_won",
    "lang": "zh-TW",
    "value": "恭喜！您贏得了 {amount} 次免費旋轉！",
    "version": 4,
    "updated_at": "2026-01-27T16:00:00Z"
  }
}
```

### 3.5 批量導入翻譯

**請求**：
```http
POST /api/v1/i18n/translations/batch
Content-Type: application/json
Authorization: Bearer <jwt_token>

{
  "lang": "th",
  "namespace": "game",
  "mode": "upsert",  // "upsert" 或 "overwrite"
  "translations": {
    "game.slot.freespin_won": "คุณได้รับ {amount} ฟรีสปิน!",
    "game.slot.jackpot_hit": "แจ็คพอตแตก!",
    "game.table.bet_placed": "วางเดิมพันสำเร็จ"
  }
}
```

**響應**：
```json
{
  "code": 1000,
  "message": "Batch import completed",
  "data": {
    "total_keys": 3,
    "inserted": 1,
    "updated": 2,
    "errors": 0,
    "failed_keys": []
  }
}
```

### 3.6 導出翻譯

**請求**（JSON 格式）：
```http
GET /api/v1/i18n/export?lang=zh-TW&namespace=game&format=json
Authorization: Bearer <jwt_token>
```

**響應**：
```json
{
  "game.slot.freespin_won": "您贏得了 {amount} 次免費旋轉！",
  "game.slot.jackpot_hit": "恭喜中大獎！",
  "game.table.bet_placed": "下注成功"
}
```

**請求**（CSV 格式）：
```http
GET /api/v1/i18n/export?lang=zh-TW&format=csv
```

**響應**：
```csv
Key,Namespace,Chinese (Traditional),English (Fallback),Context,Status
game.slot.freespin_won,game,您贏得了 {amount} 次免費旋轉！,You won {amount} Free Spins!,Slot game win notification,published
game.slot.jackpot_hit,game,恭喜中大獎！,Jackpot Hit!,,published
```

### 3.7 上報缺失鍵值

**請求**（前端自動上報）：
```http
POST /api/v1/i18n/missing-keys
Content-Type: application/json

{
  "key": "game.crash.multiplier_hit",
  "requested_languages": ["zh-TW", "th"],
  "fallback_value": "Multiplier Hit!",
  "page_url": "https://casino.com/games/crash",
  "namespace": "game"
}
```

**響應**：
```json
{
  "code": 1000,
  "message": "Missing key recorded",
  "data": {
    "missing_key_id": 567,
    "key": "game.crash.multiplier_hit",
    "status": "pending",
    "created_at": "2026-01-27T17:00:00Z"
  }
}
```

### 3.8 發佈翻譯至 CDN

**請求**：
```http
POST /api/v1/i18n/translations/publish
Content-Type: application/json
Authorization: Bearer <jwt_token>

{
  "lang": "zh-TW",
  "namespace": "game",  // 或 "*" 表示所有命名空間
  "status_filter": "approved"  // 只發佈已批准的翻譯
}
```

**響應**：
```json
{
  "code": 1000,
  "message": "Published 128 translations to CDN",
  "data": {
    "lang": "zh-TW",
    "namespace": "game",
    "published_count": 128,
    "cdn_url": "https://cdn.casino.com/i18n/zh-TW/game.v7.json",
    "version": 7,
    "published_at": "2026-01-27T18:00:00Z",
    "cache_purged": true
  }
}
```

---

## 4. 錯誤碼規範

### 4.1 標準錯誤碼

| 錯誤碼 | HTTP狀態 | 說明 | 示例場景 |
|-------|---------|------|---------|
| **1000** | 200 | Success | 正常響應 |
| **4001** | 400 | Invalid Input | 語言代碼錯誤、參數格式錯誤 |
| **4003** | 403 | Forbidden | 權限不足（非 Translator 嘗試修改）|
| **4004** | 404 | Not Found | 翻譯鍵值不存在 |
| **4009** | 409 | Conflict | 鍵值重複（已存在）|
| **4010** | 400 | Business Rule Violation | 違反業務規則（如缺少必填語言）|
| **4029** | 429 | Too Many Requests | 超過 API 限流 |
| **5000** | 500 | Internal Server Error | 系統異常 |

### 4.2 錯誤響應格式

**標準錯誤響應結構**：
```json
{
  "code": 4001,
  "message": "Invalid language code",
  "details": {
    "field": "lang",
    "provided_value": "xyz",
    "expected_format": "ISO 639-1 language code (e.g., en, zh-TW)"
  },
  "timestamp": "2026-01-27T18:30:00Z",
  "request_id": "req_abc123def456"
}
```

---

## 5. API 限流 (Rate Limiting)

### 5.1 限流策略

**分層限流**：
| 客戶端類型 | 限流規則 | 說明 |
|-----------|---------|------|
| **匿名用戶** | 100 req/min | 公開端點（如前端獲取翻譯）|
| **已登入玩家** | 300 req/min | 已驗證用戶 |
| **CMS 管理員** | 1000 req/min | 後台操作 |
| **內部服務** | 無限制 | 微服務間調用（使用內部 IP 白名單）|

### 5.2 限流實現

**使用 Redis Token Bucket 算法**：

### 5.3 限流響應頭

**標準響應頭**（符合 RFC 6585）：
```http
HTTP/1.1 429 Too Many Requests
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1706356800  # Unix timestamp
Retry-After: 60  # 秒數

{
  "code": 4029,
  "message": "Too many requests",
  "details": {
    "limit": 100,
    "window": "60 seconds",
    "retry_after": 60
  }
}
```

---

## 6. 監控與指標

### 6.1 核心監控指標

| 指標名稱 | 類型 | 目標值 | Alert 閾值 | 說明 |
|---------|------|--------|-----------|------|
| **API Response Time (P99)** | Histogram | < 50ms | > 200ms | 99% 請求響應時間 |
| **Cache Hit Rate** | Gauge | > 95% | < 85% | Redis 快取命中率 |
| **Translation Coverage** | Gauge | 100% | < 95% | 已翻譯鍵值覆蓋率 |
| **Missing Key Rate** | Counter | 0% | > 1% | 缺失鍵值上報率 |
| **API Error Rate** | Counter | < 0.1% | > 1% | API 錯誤率 |
| **CDN Publish Success Rate** | Gauge | 100% | < 99% | CDN 發佈成功率 |

### 6.2 Prometheus 監控實現

**Metrics 定義**：

### 6.3 監控儀表板

**Grafana 儀表板配置**（PromQL 查詢）：

**1. API 響應時間（P99）**：
```promql
histogram_quantile(0.99,
  rate(translation_response_time_seconds_bucket[5m])
)
```

**2. 快取命中率**：
```promql
(
  rate(translation_cache_hits_total[5m])
  /
  (rate(translation_cache_hits_total[5m]) + rate(translation_cache_misses_total[5m]))
) * 100
```

**3. API 錯誤率**：
```promql
(
  rate(translation_requests_total{status_code=~"5.."}[5m])
  /
  rate(translation_requests_total[5m])
) * 100
```

**4. 每分鐘請求數（QPS）**：
```promql
sum(rate(translation_requests_total[1m])) by (endpoint)
```

### 6.4 告警規則

**Prometheus AlertManager 配置**：
```yaml
groups:
  - name: translation_api_alerts
    interval: 30s
    rules:
      # 響應時間過慢
      - alert: TranslationAPISlowResponse
        expr: histogram_quantile(0.99, rate(translation_response_time_seconds_bucket[5m])) > 0.2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Translation API response time is too slow"
          description: "P99 response time is {{ $value }}s (threshold: 200ms)"

      # 快取命中率過低
      - alert: TranslationCacheMissRateHigh
        expr: |
          (rate(translation_cache_misses_total[5m])
          / (rate(translation_cache_hits_total[5m]) + rate(translation_cache_misses_total[5m]))) > 0.15
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Translation cache miss rate is too high"
          description: "Cache miss rate is {{ $value | humanizePercentage }}"

      # 缺失鍵值過多
      - alert: MissingTranslationKeysHigh
        expr: increase(missing_translation_keys_total[1h]) > 100
        labels:
          severity: warning
        annotations:
          summary: "Too many missing translation keys reported"
          description: "{{ $value }} missing keys reported in the last hour"

      # CDN 發佈失敗
      - alert: CDNPublishFailure
        expr: rate(cdn_publish_failures_total[10m]) > 0
        labels:
          severity: critical
        annotations:
          summary: "CDN publish failures detected"
          description: "CDN publish failed {{ $value }} times in the last 10 minutes"
```

---

## 7. 與其他模組整合

### 7.1 整合點清單

| 模組 | 整合方式 | 數據流向 | 說明 |
|------|---------|---------|------|
| **08-01 前端佈局引擎** | API 調用 | 佈局引擎 → i18n API | UI 元素多語言文字 |
| **08-02 Banner 管理** | 資料庫 JSONB | CMS → translations 表 | 橫幅多語言標題與描述 |
| **04-01 活動系統** | 資料庫 JSONB | 活動表 → translations 表 | 活動多語言內容 |
| **11-01 客服中台** | API 調用 + JSONB | 客服模板 → i18n API | 通知模板多語言 |
| **07-03 通知架構** | API 調用 | 通知服務 → i18n API | Email/SMS 模板翻譯 |
| **09-02 審計日誌** | 事件訂閱 | i18n API → 審計日誌 | 記錄翻譯變更 |

### 7.2 事件發佈

**當翻譯變更時發佈事件**（引用 07-03 通知架構）：

---

## 📚 相關文檔

### 系列文檔
- [08-05-01 i18n 架構與服務設計](./08-05-01_i18n_Architecture.md) - 翻譯服務架構、CDN分發
- [08-05-02 動態內容本地化](./08-05-02_Dynamic_Content_L10n.md) - JSONB多語言字段、API響應
- [08-05-03 翻譯工作流](./08-05-03_Translation_Workflow.md) - 狀態機、Crowdin整合

### 技術架構參考
- [12-05 API設計標準](../07_Technical_Infrastructure_NEW/07-03-01_Design_Principles.md) - API 規範
- [12-04 監控體系](../12_Technical_Operations/12-04_Monitoring_System.md) - Prometheus/Grafana 監控
- [07-03 通知架構](../07_Platform_Management/07-03_Notification_Architecture.md) - 事件驅動整合

---

**文檔版本**: 1.0.0
**最後更新**: 2026-01-27
**維護團隊**: Backend Team & DevOps Team
