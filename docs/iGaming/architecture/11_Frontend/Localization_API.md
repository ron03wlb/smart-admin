# Localization API Architecture

> **Business Requirements**: [Localization Requirements](../../requirements/11_Frontend_Experience/Localization_Requirements.md)
> **Canonical Source**: [source-archive/11_Frontend_CMS/11-10](../../source-archive/11_Frontend_CMS/11-10_Localization_API.md)
> **View Type**: Technical Architecture
> **Target Audience**: Architects, Backend Developers, DevOps

---

## 1. API Endpoint Overview

| Method | Endpoint | Description | Permission |
|--------|----------|-------------|------------|
| **GET** | `/api/v1/i18n/translations` | Get translation list | Public |
| **GET** | `/api/v1/i18n/translations/{key}` | Get single translation | Public |
| **POST** | `/api/v1/i18n/translations` | Create translation | Translator |
| **PUT** | `/api/v1/i18n/translations/{key}` | Update translation | Translator |
| **DELETE** | `/api/v1/i18n/translations/{key}` | Delete translation | Admin |
| **POST** | `/api/v1/i18n/translations/batch` | Batch import | Translator |
| **GET** | `/api/v1/i18n/export` | Export (JSON/CSV/XLIFF) | Translator |
| **POST** | `/api/v1/i18n/missing-keys` | Report missing key | Public |
| **POST** | `/api/v1/i18n/translations/{key}/publish` | Publish to CDN | Publisher |

## 2. API Specifications

### 2.1 Get Translation List

```http
GET /api/v1/i18n/translations?lang=zh-TW&namespace=game&status=published&page=1&page_size=50
```

**Response (success)**:
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

### 2.2 Batch Import

```http
POST /api/v1/i18n/translations/batch
Content-Type: application/json
Authorization: Bearer <jwt_token>

{
  "lang": "th",
  "namespace": "game",
  "mode": "upsert",
  "translations": {
    "game.slot.freespin_won": "คุณได้รับ {amount} ฟรีสปิน!",
    "game.slot.jackpot_hit": "แจ็คพอตแตก!",
    "game.table.bet_placed": "วางเดิมพันสำเร็จ"
  }
}
```

### 2.3 Publish to CDN

```http
POST /api/v1/i18n/translations/publish

{
  "lang": "zh-TW",
  "namespace": "game",
  "status_filter": "approved"
}

Response:
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

## 3. Error Code Specification

| Error Code | HTTP Status | Description | Scenario |
|------------|-------------|-------------|----------|
| **1000** | 200 | Success | Normal response |
| **4001** | 400 | Invalid Input | Wrong language code |
| **4003** | 403 | Forbidden | Insufficient permissions |
| **4004** | 404 | Not Found | Translation key missing |
| **4009** | 409 | Conflict | Duplicate key |
| **4029** | 429 | Too Many Requests | Rate limit exceeded |
| **5000** | 500 | Internal Server Error | System error |

## 4. Rate Limiting

| Client Type | Rate Limit | Description |
|-------------|-----------|-------------|
| **Anonymous** | 100 req/min | Public endpoints |
| **Logged-in Player** | 300 req/min | Authenticated users |
| **CMS Admin** | 1000 req/min | Backend operations |
| **Internal Service** | Unlimited | Microservice calls (IP whitelist) |

**Rate Limit Response Headers** (RFC 6585):
```http
HTTP/1.1 429 Too Many Requests
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1706356800
Retry-After: 60
```

## 5. Prometheus Monitoring

### PromQL Queries

**API Response Time (P99)**:
```promql
histogram_quantile(0.99,
  rate(translation_response_time_seconds_bucket[5m])
)
```

**Cache Hit Rate**:
```promql
(
  rate(translation_cache_hits_total[5m])
  /
  (rate(translation_cache_hits_total[5m]) + rate(translation_cache_misses_total[5m]))
) * 100
```

**API Error Rate**:
```promql
(
  rate(translation_requests_total{status_code=~"5.."}[5m])
  /
  rate(translation_requests_total[5m])
) * 100
```

### AlertManager Rules

```yaml
groups:
  - name: translation_api_alerts
    interval: 30s
    rules:
      - alert: TranslationAPISlowResponse
        expr: histogram_quantile(0.99, rate(translation_response_time_seconds_bucket[5m])) > 0.2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Translation API response time is too slow"
          description: "P99 response time is {{ $value }}s (threshold: 200ms)"

      - alert: TranslationCacheMissRateHigh
        expr: |
          (rate(translation_cache_misses_total[5m])
          / (rate(translation_cache_hits_total[5m]) + rate(translation_cache_misses_total[5m]))) > 0.15
        for: 10m
        labels:
          severity: warning

      - alert: MissingTranslationKeysHigh
        expr: increase(missing_translation_keys_total[1h]) > 100
        labels:
          severity: warning

      - alert: CDNPublishFailure
        expr: rate(cdn_publish_failures_total[10m]) > 0
        labels:
          severity: critical
```

## 6. Integration Points

| Module | Integration | Data Flow |
|--------|------------|-----------|
| **Frontend Layout Engine** | API Call | Layout Engine -> i18n API |
| **Banner Management** | Database JSONB | CMS -> translations table |
| **Activity System** | Database JSONB | Activity table -> translations |
| **Customer Service** | API Call + JSONB | CS Templates -> i18n API |
| **Notification Architecture** | API Call | Notification Service -> i18n API |
| **Audit Log** | Event Subscription | i18n API -> Audit Log |
