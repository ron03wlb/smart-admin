# 07-03-02 API 身份驗證與安全 (API Authentication & Security)

> **版本**: 1.0.0
> **最後更新**: 2026-02-04
> **來源**: 合併自 12-05_API_Design_Standard.md §7 + 12-05-01 Security Schemes

---

## 📋 目錄

- [安全規範](#安全規範)
- [JWT 認證實作](#jwt-認證實作)
- [OpenAPI Security Schemes](#openapi-security-schemes)

---
## 🔐 安全規範

### 1. 認證

**JWT認證（推薦）** ⭐：
```http
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

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
```

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
```

**權限頭（可選）**：
```http
X-Permission-Required: player:update
X-Resource-Owner: player:123456
```

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
```

**限流響應頭**：
```http
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1706346000
Retry-After: 60
```

---

### 4. 冪等性

**冪等性鍵（用於POST/PATCH）**：
```http
POST /api/v1/transactions
X-Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
```

**實現**：
```
1. 檢查Redis: GET idempotency:{key}
2. 如存在: 返回緩存響應
3. 如不存在: 執行請求 → 緩存響應(24小時)
```

---


---

## 🔐 OpenAPI Security Schemes 實作

# 全局安全配置
security:
  - bearerAuth: []

# 安全方案定義
components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
      description: |
        使用 Sa-Token 生成的 JWT Token

        **獲取方式**: POST /api/v1/auth/login

        **有效期**:
        - Access Token: 15 分鐘
        - Refresh Token: 7 天

