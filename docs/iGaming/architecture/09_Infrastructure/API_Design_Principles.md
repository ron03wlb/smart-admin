# API 設計原則 (API Design Principles)

> **Canonical Source**: [09-03-01 Design Principles](../../source-archive/09_Technical_Infrastructure/09-03-01_Design_Principles.md)
> **View**: Technical Architecture (Development & DevOps)

---

## 1. RESTful 核心原則

### 1.1 資源導向 (Resource-Oriented)

```
GET /api/v1/players/{id}          # 查詢單個玩家
GET /api/v1/players               # 查詢玩家列表
POST /api/v1/players              # 創建玩家
PATCH /api/v1/players/{id}        # 更新玩家
DELETE /api/v1/players/{id}       # 刪除玩家

GET /api/v1/players/{id}/wallets       # 子資源: 玩家錢包
GET /api/v1/players/{id}/transactions  # 子資源: 玩家交易
POST /api/v1/players/{id}/bonuses      # 子資源: 發放紅利
```

### 1.2 HTTP 方法語義

| 方法 | 語義 | 冪等性 | 安全性 | 使用場景 |
|------|------|-------|-------|---------|
| **GET** | 查詢資源 | Yes | Yes | 獲取單個/列表資源 |
| **POST** | 創建資源 | No | No | 新增玩家、發起交易 |
| **PUT** | 完全替換 | Yes | No | 替換整個資源 |
| **PATCH** | 部分更新 | No | No | 更新部分字段 |
| **DELETE** | 刪除資源 | Yes | No | 刪除玩家、取消訂單 |

### 1.3 路徑命名規範

```
/api/v{version}/{resource}
/api/v{version}/{resource}/{id}
/api/v{version}/{resource}/{id}/{sub-resource}
```

**命名規則**:
- 使用複數名詞: `players` not `player`
- 使用小寫字母 + 連字符: `game-sessions` not `gameSessions`
- 避免動詞: `/players` not `/getPlayers`
- 最多 3 層層級

---

## 2. 查詢參數規範

### 2.1 過濾 (Filtering)

```http
GET /api/v1/players?status=active&kyc_status=verified
GET /api/v1/transactions?created_at_gte=2026-01-01&created_at_lte=2026-01-31
GET /api/v1/players?balance_gt=1000&balance_lte=10000
GET /api/v1/players?status_in=active,suspended
```

### 2.2 排序 (Sorting)

```http
GET /api/v1/players?sort=created_at:desc
GET /api/v1/players?sort=vip_level:desc,created_at:desc
```

### 2.3 分頁 (Pagination)

```http
GET /api/v1/players?page=1&page_size=20
```

### 2.4 字段選擇 (Field Selection)

```http
GET /api/v1/players?fields=id,username,email
```

---

## 3. HTTP 狀態碼

### 3.1 成功響應 (2xx)

| 狀態碼 | 說明 | 使用場景 |
|--------|------|---------|
| **200 OK** | 成功 | GET, PUT, PATCH 成功 |
| **201 Created** | 已創建 | POST 成功創建 |
| **202 Accepted** | 已接受 | 異步處理已提交 |
| **204 No Content** | 無內容 | DELETE 成功 |

### 3.2 客戶端錯誤 (4xx)

| 狀態碼 | 說明 | 使用場景 | 錯誤碼 |
|--------|------|---------|--------|
| **400** | 請求錯誤 | 參數驗證失敗 | 4009 |
| **401** | 未認證 | Token 無效/過期 | 4001 |
| **403** | 無權限 | 權限不足 | 4003 |
| **404** | 未找到 | 資源不存在 | 4004 |
| **409** | 衝突 | 重複創建、並發衝突 | 4091 |
| **422** | 無法處理 | 業務規則違反 | 4010 |
| **429** | 限流 | 超過請求限制 | 4029 |

### 3.3 服務端錯誤 (5xx)

| 狀態碼 | 說明 | 使用場景 | 錯誤碼 |
|--------|------|---------|--------|
| **500** | 服務器錯誤 | 未知異常 | 5000 |
| **502** | 網關錯誤 | 上游服務異常 | 5002 |
| **503** | 服務不可用 | 維護模式 | 5003 |
| **504** | 網關超時 | 上游超時 | 5004 |

---

## 4. 統一響應格式

### 4.1 成功響應

```json
{
  "code": 1000,
  "message": "Success",
  "data": {
    "player_id": 123456,
    "username": "john_doe",
    "email": "john@example.com"
  },
  "timestamp": "2026-01-27T10:00:00Z",
  "trace_id": "abc-123-def-456"
}
```

### 4.2 錯誤響應

```json
{
  "code": 4201,
  "message": "餘額不足",
  "error_detail": {
    "required_amount": 10000,
    "available_balance": 5000,
    "currency": "USD"
  },
  "timestamp": "2026-01-27T10:00:00Z",
  "trace_id": "abc-123-def-456"
}
```

### 4.3 字段說明

| 字段 | 類型 | 說明 |
|------|------|------|
| `code` | Integer | 業務錯誤碼 (4 位數字) |
| `message` | String | 人類可讀的消息 |
| `data` | Object | 成功時的響應資料 |
| `error_detail` | Object | 錯誤詳情 |
| `timestamp` | String | ISO 8601 格式 |
| `trace_id` | String | 分佈式追蹤 ID |

---

## 5. 請求頭規範

### 5.1 必須包含

```http
Content-Type: application/json
Authorization: Bearer {jwt_token}
X-Request-ID: {uuid}
X-Tenant-ID: {tenant_id}
Accept-Language: en-US,zh-TW
```

### 5.2 可選包含

```http
X-Device-ID: {device_fingerprint}
X-IP-Address: {client_ip}
X-Idempotency-Key: {uuid}  # 用於 POST/PATCH
```

---

## 6. API 版本策略

### 6.1 URL 版本（推薦）

```
/api/v1/players
/api/v2/players
```

### 6.2 版本兼容性規則

**Breaking Changes（需要新版本）**:
- 刪除字段
- 修改字段類型 / 語義
- 修改錯誤碼 / HTTP 狀態碼

**Non-Breaking Changes（無需新版本）**:
- 添加新字段 / 新端點
- 添加可選參數
- 放寬驗證規則

### 6.3 版本生命週期

```
v1 Released: 2025-01-01
v2 Released: 2026-01-01
v1 Deprecated: 2026-01-01 (6 months notice)
v1 Sunset: 2026-07-01 (停止服務)
```

**廢棄通知 Header**:

```http
Deprecation: true
Sunset: Wed, 01 Jul 2026 00:00:00 GMT
Link: </api/v2/players>; rel="alternate"
```

---

## 相關文檔

- [Authentication Architecture](./Authentication_Architecture.md) - 認證與授權
- [Common Patterns](./Common_Patterns.md) - 通用 API 模式
- [Domain APIs](./Domain_APIs.md) - 領域 API 設計
