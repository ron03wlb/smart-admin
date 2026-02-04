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

## 🔐 Refresh Token 詳細實施方案

**⚠️ 重要安全提示**：

如果您的系統中存在「根據交易 ID 生成 token」的邏輯，請立即停止使用並遷移到 OAuth 2.0 Refresh Token 機制。

**問題說明**：
- ❌ **Critical 級別漏洞**：任何人知道交易 ID 即可冒充用戶
- ✅ **正確方案**：使用 OAuth 2.0 Refresh Token + Device Fingerprint 驗證

**完整實施方案**：
- 📘 [07-03-02-01 OAuth 2.0 Refresh Token 實施方案](./07-03-02-01_OAuth_Refresh_Token_Implementation.md)
  - 包含完整的代碼設計、測試計劃、實施路線圖
  - ROI 分析：$24k 投入 vs $650k-$2.3M 損失避免（1448% ROI）
  - 符合 GDPR、等保三級、MGA/UKGC 合規要求

**快速參考**：

| Token 類型 | 有效期 | 存儲位置 | 用途 | 刷新機制 |
|-----------|--------|---------|------|---------|
| **Access Token** | 15 分鐘 | LocalStorage | API 訪問 | 通過 Refresh Token 刷新 |
| **Refresh Token** | 30 天 | Redis (HttpOnly) | 刷新 Access Token | Token Rotation（舊 Token 失效） |

**核心安全特性**：
- ✅ Token Rotation：刷新後舊 Refresh Token 立即失效
- ✅ Device Fingerprint：FingerprintJS 設備識別（99.5% 準確率）
- ✅ 輪換次數限制：最多刷新 10 次後強制重新登入
- ✅ Redis 高可用：主從 + Sentinel 架構

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

