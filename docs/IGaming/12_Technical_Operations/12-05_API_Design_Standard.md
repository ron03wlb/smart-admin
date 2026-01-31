# API 設計標準 (API Design Standard)

> **版本**: 1.0.0
> **最後更新**: 2026-01-27
> **目的**: 統一IGaming平台所有API的設計規範

---

## 📋 目錄

- [設計原則](#設計原則)
- [RESTful API規範](#restful-api規範)
- [統一錯誤碼體系](#統一錯誤碼體系)
- [請求與響應格式](#請求與響應格式)
- [分頁排序過濾](#分頁排序過濾)
- [API版本策略](#api版本策略)
- [安全規範](#安全規範)
- [性能優化](#性能優化)

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
```markdown

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
```markdown

**命名規則**：
- ✅ 使用複數名詞（`players` not `player`）
- ✅ 使用小寫字母 + 連字符（`game-sessions` not `gameSessions`）
- ✅ 避免動詞（`/players` not `/getPlayers`）
- ✅ 保持層級簡單（最多3層）

**示例**：
```sql
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
```text

---

### 2. 查詢參數規範

**過濾 (Filtering)**：
```http
GET /api/v1/players?status=active&kyc_status=verified
```text

**排序 (Sorting)**：
```http
GET /api/v1/players?sort=created_at:desc
GET /api/v1/players?sort=created_at:desc,username:asc
```text

**分頁 (Pagination)**：
```http
GET /api/v1/players?page=1&page_size=20
```text

**字段選擇 (Field Selection)**：
```http
GET /api/v1/players?fields=id,username,email
```text

**搜索 (Search)**：
```http
GET /api/v1/players?search=john
GET /api/v1/games?q=blackjack
```markdown

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

## 🔢 統一錯誤碼體系

### 1. 錯誤碼設計

**格式**: `XYZZ`
- **X**: 錯誤類別（1=成功, 4=客戶端錯誤, 5=服務端錯誤）
- **Y**: 子類別
- **ZZ**: 具體錯誤

### 2. 標準錯誤碼

#### 成功 (1xxx)

| Code | 說明 | HTTP Status |
|------|------|-------------|
| **1000** | 成功 | 200 OK |
| **1001** | 已創建 | 201 Created |
| **1002** | 已接受 | 202 Accepted |

#### 認證與授權錯誤 (40xx)

| Code | 說明 | HTTP Status | 場景 |
|------|------|-------------|------|
| **4001** | 未認證 | 401 | Token無效/過期 |
| **4002** | Token過期 | 401 | JWT過期 |
| **4003** | 無權限 | 403 | RBAC權限不足 |
| **4004** | 未找到 | 404 | 資源不存在 |
| **4005** | MFA必需 | 403 | 需要二次驗證 |

#### 請求錯誤 (400x-402x)

| Code | 說明 | HTTP Status | 場景 |
|------|------|-------------|------|
| **4009** | 參數驗證失敗 | 400 | 必填字段缺失、格式錯誤 |
| **4010** | 業務規則違反 | 422 | 餘額不足、流水未達標 |
| **4011** | 重複請求 | 409 | 冪等性檢查失敗 |
| **4012** | 並發衝突 | 409 | 樂觀鎖版本衝突 |
| **4013** | 資源已存在 | 409 | 用戶名重複、郵箱重複 |
| **4029** | 請求過多 | 429 | 超過限流閾值 |

#### 業務錯誤 (41xx-49xx)

##### 玩家相關 (41xx)

| Code | 說明 | HTTP Status |
|------|------|-------------|
| **4101** | 玩家未找到 | 404 |
| **4102** | 玩家已禁用 | 403 |
| **4103** | KYC未驗證 | 403 |
| **4104** | 玩家自我排除 | 403 |

##### 錢包相關 (42xx)

| Code | 說明 | HTTP Status |
|------|------|-------------|
| **4201** | 餘額不足 | 422 |
| **4202** | 錢包未找到 | 404 |
| **4203** | 錢包已鎖定 | 403 |
| **4204** | 信用額度不足 | 422 |

##### 交易相關 (43xx)

| Code | 說明 | HTTP Status |
|------|------|-------------|
| **4301** | 交易失敗 | 422 |
| **4302** | 支付渠道不可用 | 503 |
| **4303** | 超過交易限額 | 422 |
| **4304** | 交易已取消 | 409 |

##### 遊戲相關 (44xx)

| Code | 說明 | HTTP Status |
|------|------|-------------|
| **4401** | 遊戲不可用 | 503 |
| **4402** | 遊戲會話無效 | 400 |
| **4403** | 下注金額無效 | 422 |
| **4404** | GP通信失敗 | 502 |

##### 活動相關 (45xx)

| Code | 說明 | HTTP Status |
|------|------|-------------|
| **4501** | 活動已過期 | 410 |
| **4502** | 不符合活動條件 | 422 |
| **4503** | 紅利已用盡 | 422 |
| **4504** | 流水未達標 | 422 |

#### 服務端錯誤 (50xx)

| Code | 說明 | HTTP Status | 場景 |
|------|------|-------------|------|
| **5000** | 服務器內部錯誤 | 500 | 未知異常 |
| **5001** | 數據庫錯誤 | 500 | SQL執行失敗 |
| **5002** | 上游服務錯誤 | 502 | GP/PSP異常 |
| **5003** | 服務不可用 | 503 | 維護模式 |
| **5004** | 上游超時 | 504 | GP/PSP超時 |

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
```text

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
```markdown

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
```text

**可選包含**：
```http
X-Device-ID: {device_fingerprint}
X-IP-Address: {client_ip}
User-Agent: {client_user_agent}
X-Idempotency-Key: {uuid}  # 冪等性鍵（用於POST/PATCH）
```text

---

## 📄 分頁排序過濾

### 1. 分頁標準

**Offset分頁（推薦用於小數據集）**：
```
GET /api/v1/players?page=1&page_size=20
```text

**響應格式**：
```json
{
  "code": 1000,
  "message": "Success",
  "data": {
    "items": [...],
    "pagination": {
      "page": 1,
      "page_size": 20,
      "total_count": 1543,
      "total_pages": 78,
      "has_next": true,
      "has_previous": false
    }
  }
}
```text

**Cursor分頁（推薦用於大數據集/實時流）**：
```
GET /api/v1/transactions?cursor=eyJpZCI6MTIzNDU2fQ&limit=50
```text

**響應格式**：
```json
{
  "code": 1000,
  "message": "Success",
  "data": {
    "items": [...],
    "pagination": {
      "next_cursor": "eyJpZCI6MTIzNTA2fQ",
      "has_more": true
    }
  }
}
```text

---

### 2. 排序標準

**語法**: `sort={field}:{order}`

**示例**：
```
# 單字段排序
GET /api/v1/players?sort=created_at:desc

# 多字段排序
GET /api/v1/players?sort=vip_level:desc,created_at:desc

# 默認排序
GET /api/v1/players  # 默認: created_at:desc
```markdown

**排序方向**：
- `asc`: 升序
- `desc`: 降序（默認）

---

### 3. 過濾標準

**精確匹配**：
```
GET /api/v1/players?status=active&kyc_status=verified
```text

**範圍查詢**：
```
GET /api/v1/transactions?created_at_gte=2026-01-01&created_at_lte=2026-01-31
GET /api/v1/players?balance_gt=1000&balance_lte=10000
```text

**模糊搜索**：
```
GET /api/v1/players?username_like=john
GET /api/v1/games?name_contains=poker
```text

**IN查詢**：
```
GET /api/v1/players?status_in=active,suspended
GET /api/v1/games?provider_id_in=1,2,3
```text

**複雜過濾（使用JSON）**（可選高級功能）：
```
GET /api/v1/players?filter={"and":[{"field":"status","op":"eq","value":"active"},{"field":"balance","op":"gt","value":1000}]}
```text

---

## 🔄 API版本策略

### 1. 版本命名

**URL版本（推薦）** ⭐：
```
/api/v1/players
/api/v2/players
```markdown

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
```markdown

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
```text

**廢棄通知**（Response Header）：
```http
Deprecation: true
Sunset: Wed, 01 Jul 2026 00:00:00 GMT
Link: </api/v2/players>; rel="alternate"
```text

---

## 🔐 安全規範

### 1. 認證

**JWT認證（推薦）** ⭐：
```http
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```text

**JWT結構**：
```json
{
  "header": {
    "alg": "RS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "player:123456",
    "tenant_id": "tenant_1",
    "roles": ["player"],
    "iat": 1706342400,
    "exp": 1706346000
  }
}
```markdown

**Token類型**：
- **Access Token**: 15分鐘有效期
- **Refresh Token**: 7天有效期（存儲在HttpOnly Cookie）

---

### 2. 授權

**RBAC權限檢查**：
```
1. 驗證Token有效性
2. 檢查租戶隔離（tenant_id）
3. 檢查角色權限（roles）
4. 檢查資源所有權（player_id）
```text

**權限頭（可選）**：
```http
X-Permission-Required: player:update
X-Resource-Owner: player:123456
```text

---

### 3. 限流

**限流策略**：
```
# 全局限流
100 req/min per IP

# 用戶限流
1000 req/min per Player

# 端點限流
POST /api/v1/withdrawals: 10 req/hour per Player
```text

**限流響應頭**：
```http
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1706346000
Retry-After: 60
```text

---

### 4. 冪等性

**冪等性鍵（用於POST/PATCH）**：
```http
POST /api/v1/transactions
X-Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
```text

**實現**：
```
1. 檢查Redis: GET idempotency:{key}
2. 如存在: 返回緩存響應
3. 如不存在: 執行請求 → 緩存響應(24小時)
```text

---

## ⚡ 性能優化

### 1. 緩存策略

**HTTP緩存頭**：
```http
# 靜態資源（遊戲圖片、前端資源）
Cache-Control: public, max-age=31536000, immutable

# 準靜態資源（遊戲列表、VIP等級）
Cache-Control: public, max-age=3600, must-revalidate
ETag: "33a64df551425fcc55e4d42a148795d9f25f89d4"

# 動態資源（玩家餘額、交易記錄）
Cache-Control: no-cache, no-store, must-revalidate
```text

---

### 2. 壓縮

**Gzip壓縮（推薦）**：
```http
Accept-Encoding: gzip, deflate
Content-Encoding: gzip
```yaml

**壓縮率**：
- JSON: ~70-80%壓縮率
- 建議閾值: >1KB才壓縮

---

### 3. 字段選擇

**Sparse Fieldsets**：
```text
GET /api/v1/players/{id}?fields=id,username,balance
```

**響應**：
```json
{
  "data": {
    "id": 123456,
    "username": "john_doe",
    "balance": 50000
  }
}
```text

---

### 4. 批量操作

**批量查詢**：
```
POST /api/v1/players/batch-get
{
  "player_ids": [123, 456, 789]
}
```text

**批量創建**：
```
POST /api/v1/bonuses/batch-create
{
  "bonuses": [
    {"player_id": 123, "amount": 1000},
    {"player_id": 456, "amount": 2000}
  ]
}
```

---

## 📚 相關文檔

### 技術參考
- [00-04 技術選型標準](../00_Concept_&_Analysis/00-04_Technology_Stack.md) - 技術棧
- [12-03 網關架構](./12-03_Gateway_Architecture.md) - API網關設計
- [09-01 管理後台RBAC](../09_System_Security/09-01_Admin_RBAC.md) - 權限設計

### 業務參考
- [02-06 統一錢包模型](../02_Finance_Center/02-06_Unified_Wallet_Model.md) - 錢包API
- [03-01 遊戲集成標準](../03_Game_Center/03-01_Game_Integration_Standard.md) - GP API

---

**文檔版本**: 1.0.0
**維護團隊**: Architecture Team & Backend Team
**下次審閱**: 2026-04-27（每季度審閱）
