# 07-03-01 API 設計原則 (API Design Principles)

> **版本**: 1.0.0
> **最後更新**: 2026-02-04
> **來源**: 合併自 12-05_API_Design_Standard.md §1-2, §4, §6

---

## 📋 目錄

- [設計原則](#設計原則)
- [RESTful API規範](#restful-api規範)
- [請求與響應格式](#請求與響應格式)
- [API版本策略](#api版本策略)

---
## 🎯 設計原則

### 1. RESTful 核心原則

**資源導向 (Resource-Oriented)**：
- ✅ 使用名詞表示資源（不是動詞）
- ✅ 路徑表示層級關係
- ✅ HTTP方法表示操作

**範例**：
```sql
✅ 正確: GET /api/v1/players/{id}
❌ 錯誤: GET /api/v1/getPlayer?id={id}

✅ 正確: POST /api/v1/players
❌ 錯誤: POST /api/v1/createPlayer

✅ 正確: DELETE /api/v1/players/{id}
❌ 錯誤: POST /api/v1/deletePlayer
```

---

### 2. HTTP 方法語義

| 方法 | 語義 | 冪等性 | 安全性 | 使用場景 |
|------|------|-------|-------|---------|
| **GET** | 查詢資源 | ✅ | ✅ | 獲取單個/列表資源 |
| **POST** | 創建資源 | ❌ | ❌ | 新增玩家、發起交易 |
| **PUT** | 完全替換 | ✅ | ❌ | 替換整個資源 |
| **PATCH** | 部分更新 | ❌ | ❌ | 更新部分字段 |
| **DELETE** | 刪除資源 | ✅ | ❌ | 刪除玩家、取消訂單 |

**冪等性說明**：
- **冪等**：多次相同請求結果一致（GET、PUT、DELETE）
- **非冪等**：每次請求產生新結果（POST、PATCH）

---

## 🛣️ RESTful API規範

### 1. 路徑命名規範

**基礎格式**：
```
/api/v{version}/{resource}
/api/v{version}/{resource}/{id}
/api/v{version}/{resource}/{id}/{sub-resource}
```

**命名規則**：
- ✅ 使用複數名詞（`players` not `player`）
- ✅ 使用小寫字母 + 連字符（`game-sessions` not `gameSessions`）
- ✅ 避免動詞（`/players` not `/getPlayers`）
- ✅ 保持層級簡單（最多3層）

**示例**：
```
# 玩家相關
GET    /api/v1/players                    # 獲取玩家列表
GET    /api/v1/players/{id}               # 獲取單個玩家
POST   /api/v1/players                    # 創建玩家
PATCH  /api/v1/players/{id}               # 更新玩家
DELETE /api/v1/players/{id}               # 刪除玩家

# 子資源
GET    /api/v1/players/{id}/wallets       # 獲取玩家錢包
GET    /api/v1/players/{id}/transactions  # 獲取玩家交易記錄
POST   /api/v1/players/{id}/bonuses       # 給玩家發放紅利

# 遊戲相關
GET    /api/v1/games                      # 獲取遊戲列表
GET    /api/v1/games/{id}/sessions        # 獲取遊戲會話列表
POST   /api/v1/game-sessions              # 創建遊戲會話（不依賴遊戲ID）
```

---

### 2. 查詢參數規範

**過濾 (Filtering)**：
```http
GET /api/v1/players?status=active&kyc_status=verified
```

**排序 (Sorting)**：
```http
GET /api/v1/players?sort=created_at:desc
GET /api/v1/players?sort=created_at:desc,username:asc
```

**分頁 (Pagination)**：
```http
GET /api/v1/players?page=1&page_size=20
```

**字段選擇 (Field Selection)**：
```http
GET /api/v1/players?fields=id,username,email
```

**搜索 (Search)**：
```http
GET /api/v1/players?search=john
GET /api/v1/games?q=blackjack
```

---

### 3. HTTP 狀態碼使用

#### 成功響應 (2xx)

| 狀態碼 | 說明 | 使用場景 |
|--------|------|---------|
| **200 OK** | 成功 | GET、PUT、PATCH 成功 |
| **201 Created** | 已創建 | POST 成功創建資源 |
| **202 Accepted** | 已接受 | 異步處理任務已提交 |
| **204 No Content** | 無內容 | DELETE 成功（無需返回數據）|

#### 客戶端錯誤 (4xx)

| 狀態碼 | 說明 | 使用場景 | 錯誤碼 |
|--------|------|---------|--------|
| **400 Bad Request** | 請求錯誤 | 參數驗證失敗 | 4009 |
| **401 Unauthorized** | 未認證 | Token無效/過期 | 4001 |
| **403 Forbidden** | 無權限 | 權限不足 | 4003 |
| **404 Not Found** | 未找到 | 資源不存在 | 4004 |
| **409 Conflict** | 衝突 | 重複創建、並發衝突 | 4091 |
| **422 Unprocessable Entity** | 無法處理 | 業務規則違反 | 4010 |
| **429 Too Many Requests** | 限流 | 超過請求限制 | 4029 |

#### 服務端錯誤 (5xx)

| 狀態碼 | 說明 | 使用場景 | 錯誤碼 |
|--------|------|---------|--------|
| **500 Internal Server Error** | 服務器錯誤 | 未知異常 | 5000 |
| **502 Bad Gateway** | 網關錯誤 | 上游服務異常 | 5002 |
| **503 Service Unavailable** | 服務不可用 | 維護模式 | 5003 |
| **504 Gateway Timeout** | 網關超時 | 上游超時 | 5004 |

---

## 📦 請求與響應格式

### 1. 統一響應格式

**成功響應**：
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

**錯誤響應**：
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

**字段說明**：
- `code`: 業務錯誤碼（4位數字）
- `message`: 人類可讀的錯誤消息
- `data`: 成功時的響應數據（可選）
- `error_detail`: 錯誤詳情（可選）
- `timestamp`: ISO 8601格式時間戳
- `trace_id`: 分佈式追蹤ID（用於日誌查詢）

---

### 2. 請求頭規範

**必須包含**：
```http
Content-Type: application/json
Authorization: Bearer {jwt_token}
X-Request-ID: {uuid}
X-Tenant-ID: {tenant_id}
Accept-Language: en-US,zh-TW
```

**可選包含**：
```http
X-Device-ID: {device_fingerprint}
X-IP-Address: {client_ip}
User-Agent: {client_user_agent}
X-Idempotency-Key: {uuid}  # 冪等性鍵（用於POST/PATCH）
```

---

## 🔄 API版本策略

### 1. 版本命名

**URL版本（推薦）** ⭐：
```
/api/v1/players
/api/v2/players
```

**優點**：
- ✅ 清晰直觀
- ✅ 緩存友好
- ✅ 路由簡單

**缺點**：
- ❌ URL變更

---

**Header版本（備選）**：
```
GET /api/players
Accept: application/vnd.igaming.v1+json
```

**優點**：
- ✅ URL不變
- ✅ RESTful純粹性

**缺點**：
- ❌ 調試困難
- ❌ 緩存複雜

**推薦**：URL版本 ⭐

---

### 2. 版本兼容性

**Breaking Changes（需要新版本）**：
- ❌ 刪除字段
- ❌ 修改字段類型
- ❌ 修改字段語義
- ❌ 修改錯誤碼
- ❌ 修改HTTP狀態碼

**Non-Breaking Changes（無需新版本）**：
- ✅ 添加新字段
- ✅ 添加新端點
- ✅ 添加可選參數
- ✅ 放寬驗證規則

---

### 3. 版本生命週期

**版本支持策略**：
```
v1 Released: 2025-01-01
v2 Released: 2026-01-01
    ↓
v1 Deprecated: 2026-01-01 (6個月通知期)
    ↓
v1 Sunset: 2026-07-01 (停止服務)
```

**廢棄通知**（Response Header）：
```http
Deprecation: true
Sunset: Wed, 01 Jul 2026 00:00:00 GMT
Link: </api/v2/players>; rel="alternate"
```

---

