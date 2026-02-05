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

### 3. 多主體認證策略

iGaming 平台服務於四種不同的主體類型，每種主體有不同的認證需求和安全考量。本節概述差異化的認證策略。

#### 3.1 策略總覽

| 主體類型     | Token 類型                   | Access Token<br/>有效期 | Refresh Token<br/>有效期 | 主要安全措施                                  |
| -------- | -------------------------- | -------------------- | --------------------- | --------------------------------------- |
| **玩家**   | JWT (Redis Opaque)         | 15 分鐘                | 30 天                  | Device Fingerprint<br/>+ Token Rotation |
| **遊戲商**  | Opaque Token<br/>(API Key) | 永久有效                 | N/A                   | HMAC-SHA256<br/>+ IP 白名單                |
| **第三方**  | JWT (OAuth 2.0)            | 1 小時                 | 7 天                   | OAuth 2.0<br/>+ Webhook 簽名              |
| **後台用戶** | JWT (Redis Opaque)         | 30 分鐘                | 7 天                   | MFA (TOTP/SMS)<br/>+ IP 限制              |

**詳細設計文檔**：
- 📘 [07-03-02-02 多主體 Token 安全方案](./07-03-02-02_Multi_Actor_Token_Security.md) - 完整的設計文檔（架構圖、威脅分析、實施建議）
- 📘 [07-03-02-01 OAuth 2.0 Refresh Token 實施方案](./07-03-02-01_OAuth_Refresh_Token_Implementation.md) - 玩家專用（Production Ready）
- 📘 [05-06 後台用戶 MFA 實施方案](../../05_Platform_Governance/05-06_MFA_Implementation.md) - 後台 MFA 設計

#### 3.2 設計決策理由（Ultrathink 分析）

##### 為何玩家使用 JWT 而遊戲商使用 Opaque Token？

**分析過程**：

我們評估了三種方案：

**選項 A（全部使用 JWT）**：
- ✅ 優點：統一實現，單一代碼庫，易於維護
- ✅ 優點：JWT 自包含，無需查詢數據庫即可驗證
- ❌ 缺點：向遊戲商暴露內部 Token 結構（安全風險）
- ❌ 缺點：無法針對各主體優化 TTL（玩家需短 TTL，GP 需長 TTL）

**選項 B（差異化策略 - 已選擇）**：
- ✅ 優點：針對性安全設計（JWT 用於玩家、Opaque Token 用於 GP）
- ✅ 優點：最佳性能（HMAC 用於 GP server-to-server、JWT 用於玩家前端）
- ✅ 優點：靈活調整 TTL（玩家 15min、GP 永久有效）
- ❌ 缺點：實現複雜度較高，需維護多套認證流程

**選項 C（全部使用 OAuth 2.0）**：
- ✅ 優點：業界標準，互操作性佳
- ❌ 缺點：對 GP 的 server-to-server 通信過度設計
- ❌ 缺點：OAuth 握手開銷高（Authorization Code Flow 需 3 次往返）

**決策理由**：
- **玩家**：前端需要攜帶用戶上下文（user_id, tenant_id, roles）進行前端授權判斷 → JWT（自包含）
- **遊戲商**：Server-to-server 通信，不需要傳遞用戶上下文，opaque token 防止 token 檢查攻擊 → API Key + HMAC
- **第三方**：需與外部系統（支付網關、KYC 供應商）集成，業界標準 OAuth 2.0 降低集成成本 → OAuth 2.0
- **後台用戶**：高安全性需求，需要 JWT 攜帶角色信息 + MFA 額外驗證 → JWT + MFA

**Trade-off 權衡**：
**安全性** (⭐⭐⭐⭐⭐) vs **實現成本** (⭐⭐⭐) → **選擇選項 B（差異化策略）**

**結論**：雖然選項 B 的實現複雜度較高，但其針對性安全設計大幅提升了系統整體安全性，是 iGaming 平台的最佳選擇。

##### 為何後台用戶強制 MFA 而玩家可選？

**風險分析**：

| 主體類型 | 權限範圍 | 風險等級 | MFA 要求 | 理由 |
|---------|---------|---------|---------|------|
| **後台用戶** | 特權操作（提款批准、餘額調整、玩家封禁） | ⭐⭐⭐⭐⭐ 極高 | ✅ 強制 | 單一密碼洩露可導致大規模財務損失 |
| **玩家** | 自己的錢包和個人資料 | ⭐⭐⭐ 中等 | ⚠️ 可選 | Device Fingerprint 已提供基礎防護，MFA 影響用戶體驗 |

**結論**：
- **後台用戶**：合規要求（等保三級、MGA/UKGC）+ 高風險操作 → 強制 MFA
- **玩家**：用戶體驗優先 + Device Fingerprint 基礎防護 → MFA 可選（高價值玩家建議啟用）

---

### 4. 限流

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

### 5. 冪等性

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

