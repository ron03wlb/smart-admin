# MFA API 文檔

**版本**: 1.0.0
**生成日期**: 2026-03-10
**文檔狀態**: ✅ Production Ready

---

## 📋 概述

SmartAdmin MFA (Multi-Factor Authentication) 模塊提供完整的雙因素認證功能，包含 TOTP、備份碼、信任設備管理和設備遺失恢復流程。

**關鍵特性**:
- ✅ **TOTP 認證**: 支持 Google Authenticator、Microsoft Authenticator 等標準 TOTP 應用
- ✅ **備份碼系統**: 10 個一次性 8 位數字備份碼，使用 Argon2id 加密
- ✅ **信任設備管理**: 30 天信任期限，基於 SHA256 設備指紋
- ✅ **設備遺失恢復**: 管理員審批工作流，24 小時有效期恢復碼
- ✅ **完整審計日誌**: 所有 MFA 操作記錄到審計日誌（INFO/WARNING/CRITICAL 級別）

---

## 🚀 快速開始

### 1. 啟動應用程序

```bash
cd smart-admin-api-java21-springboot3
./gradlew :smartadmin-app:bootRun
```

**默認端口**: `1024`
**啟動環境**: `dev` (開發環境)

### 2. 訪問 Knife4j API 文檔

**Knife4j UI**: [http://localhost:1024/doc.html](http://localhost:1024/doc.html)

**配置信息** (`dev/application.yaml`):
```yaml
# SpringDoc/OpenAPI 配置
springdoc:
  swagger-ui:
    enabled: true
    doc-expansion: none
    tags-sorter: alpha
    server-base-url: http://localhost:1024
  api-docs:
    enabled: true

# Knife4j API 文檔配置
knife4j:
  enable: true
  setting:
    language: zh-CN
  basic:
    enable: false  # 開發環境無需認證
```

**替代訪問方式**:
- OpenAPI JSON: [http://localhost:1024/v3/api-docs](http://localhost:1024/v3/api-docs)
- Swagger UI: [http://localhost:1024/swagger-ui.html](http://localhost:1024/swagger-ui.html)

---

## 📚 API 端點總覽

### MFA 主控制器 (MfaController)

**Tag**: `System.SYSTEM_MFA`
**路徑**: `net.lab1024.sa.system.mfa.controller.MfaController`

| HTTP Method | Endpoint | Summary | 權限 |
|-------------|----------|---------|------|
| POST | `/mfa/setup/init` | 初始化 MFA 設定 | 已登入用戶 |
| POST | `/mfa/setup/enable` | 啟用 MFA | 已登入用戶 |
| POST | `/mfa/setup/disable` | 禁用 MFA（需 TOTP 驗證） | 已登入用戶 |
| GET | `/mfa/status` | 查詢 MFA 狀態 | 已登入用戶 |
| POST | `/mfa/verify` | 驗證 MFA（TOTP/備份碼） | 已登入用戶 |
| POST | `/mfa/backup-codes/regenerate` | 重新生成備份碼（需 TOTP 驗證） | 已登入用戶 |
| GET | `/mfa/backup-codes/count` | 查詢剩餘備份碼數量 | 已登入用戶 |
| POST | `/mfa/trusted-devices/add` | 添加信任設備 | 已登入用戶 |

### MFA 恢復控制器 (MfaRecoveryController)

**Tag**: `System.SYSTEM_MFA`
**路徑**: `net.lab1024.sa.system.mfa.controller.MfaRecoveryController`

| HTTP Method | Endpoint | Summary | 權限 |
|-------------|----------|---------|------|
| POST | `/mfa/recovery/request` | 創建 MFA 恢復請求 | 已登入用戶 |
| GET | `/mfa/recovery/pending` | 查詢待審核恢復請求 | `mfa:recovery:admin` |
| POST | `/mfa/recovery/approve` | 批准恢復請求 | `mfa:recovery:admin` |
| POST | `/mfa/recovery/reject` | 拒絕恢復請求 | `mfa:recovery:admin` |
| POST | `/mfa/recovery/reset` | 使用恢復碼重置 MFA | 已登入用戶 |

---

## 🔐 認證與授權

### 認證要求

所有 MFA API 端點均需要用戶已登入（Sa-Token 認證）。

**請求頭**:
```http
x-access-token: <JWT Token>
x-refresh-token: <Refresh Token>
```

### 權限要求

| 權限代碼 | 說明 | 適用端點 |
|---------|------|---------|
| 無特殊權限 | 已登入用戶即可訪問 | 大部分 MFA 端點 |
| `mfa:recovery:admin` | Super Admin 權限 | `/mfa/recovery/pending`<br/>`/mfa/recovery/approve`<br/>`/mfa/recovery/reject` |

---

## 📖 詳細 API 文檔

### 1. 初始化 MFA 設定

**POST** `/mfa/setup/init`

**功能**: 生成 TOTP secret 和 QR 碼，供用戶掃描綁定 Authenticator。

**請求**:
```http
POST /mfa/setup/init HTTP/1.1
x-access-token: <JWT Token>
```

**響應** (`MfaSetupInitVO`):
```json
{
  "code": 1,
  "msg": "成功",
  "data": {
    "secret": "JBSWY3DPEHPK3PXP",  // Base32 編碼的 TOTP secret
    "qrCodeUrl": "otpauth://totp/SmartAdmin:user@example.com?secret=JBSWY3DPEHPK3PXP&issuer=SmartAdmin",
    "qrCodeDataUrl": "data:image/png;base64,iVBORw0KGgoAAAANS...",  // Base64 QR 碼圖片
    "backupCodes": null  // 初始化階段無備份碼
  }
}
```

---

### 2. 啟用 MFA

**POST** `/mfa/setup/enable`

**功能**: 用戶掃描 QR 碼後，輸入 TOTP token 驗證綁定，系統生成 10 個備份碼。

**請求** (`MfaEnableForm`):
```json
{
  "mfaToken": "123456",  // 6 位 TOTP token
  "trustDevice": true,    // 可選：是否信任當前設備
  "deviceName": "My MacBook Pro"  // trustDevice=true 時必填
}
```

**響應** (`MfaSetupInitVO`):
```json
{
  "code": 1,
  "msg": "成功",
  "data": {
    "secret": "JBSWY3DPEHPK3PXP",
    "qrCodeUrl": null,  // 已啟用後無需 QR 碼
    "qrCodeDataUrl": null,
    "backupCodes": [  // ⚠️ 僅顯示一次，請妥善保存
      "12345678",
      "23456789",
      "34567890",
      "45678901",
      "56789012",
      "67890123",
      "78901234",
      "89012345",
      "90123456",
      "01234567"
    ]
  }
}
```

---

### 3. 禁用 MFA

**POST** `/mfa/setup/disable`

**功能**: 禁用 MFA（需 TOTP 驗證）。角色強制要求 MFA 的用戶無法禁用。

**請求**:
```http
POST /mfa/setup/disable?totpToken=123456 HTTP/1.1
x-access-token: <JWT Token>
```

**響應**:
```json
{
  "code": 1,
  "msg": "MFA 已成功禁用",
  "data": null
}
```

**錯誤響應示例**:
```json
{
  "code": 30001,
  "level": "warning",
  "msg": "MFA 驗證碼錯誤",
  "data": null
}
```

---

### 4. 查詢 MFA 狀態

**GET** `/mfa/status`

**功能**: 返回當前用戶的 MFA 配置狀態。

**請求**:
```http
GET /mfa/status HTTP/1.1
x-access-token: <JWT Token>
```

**響應** (`MfaStatusVO`):
```json
{
  "code": 1,
  "msg": "成功",
  "data": {
    "mfaEnabled": true,  // MFA 是否已啟用
    "mfaType": "TOTP",  // MFA 類型
    "backupCodesGenerated": true,  // 備份碼是否已生成
    "backupCodesRemaining": 8,  // 剩餘備份碼數量
    "lastVerifiedAt": "2026-03-10T10:30:00Z",  // 最後驗證時間
    "enforcedByRole": false  // 是否由角色強制要求 MFA
  }
}
```

---

### 5. 驗證 MFA

**POST** `/mfa/verify`

**功能**: 登入流程或敏感操作前的 MFA 驗證。支持 6 位 TOTP token 或 8 位備份碼。

**請求** (`MfaVerifyForm`):
```json
{
  "mfaToken": "123456",  // 6 位 TOTP 或 8 位備份碼
  "trustDevice": false,  // 可選：是否信任當前設備
  "deviceName": null  // trustDevice=true 時必填
}
```

**響應**:
```json
{
  "code": 1,
  "msg": "MFA 驗證成功",
  "data": null
}
```

---

### 6. 重新生成備份碼

**POST** `/mfa/backup-codes/regenerate`

**功能**: 重新生成 10 個新備份碼（舊備份碼作廢）。需 TOTP 驗證。

**請求**:
```http
POST /mfa/backup-codes/regenerate?totpToken=123456 HTTP/1.1
x-access-token: <JWT Token>
```

**響應** (`MfaSetupInitVO`):
```json
{
  "code": 1,
  "msg": "成功",
  "data": {
    "backupCodes": [  // 新的 10 個備份碼
      "11111111",
      "22222222",
      "..."
    ]
  }
}
```

---

### 7. 查詢剩餘備份碼數量

**GET** `/mfa/backup-codes/count`

**功能**: 返回剩餘可用備份碼數量（0-10）。

**請求**:
```http
GET /mfa/backup-codes/count HTTP/1.1
x-access-token: <JWT Token>
```

**響應**:
```json
{
  "code": 1,
  "msg": "成功",
  "data": 8  // 剩餘 8 個備份碼
}
```

---

### 8. 添加信任設備

**POST** `/mfa/trusted-devices/add`

**功能**: 將當前設備添加到信任清單，30 天內無需 MFA 驗證。

**請求**:
```http
POST /mfa/trusted-devices/add?deviceName=My%20iPhone%2015 HTTP/1.1
x-access-token: <JWT Token>
```

**設備指紋生成邏輯**:
```java
String fingerprint = SHA256(ipAddress + userAgent + deviceUuid);
```

**響應**:
```json
{
  "code": 1,
  "msg": "信任設備已添加",
  "data": null
}
```

---

### 9. 創建 MFA 恢復請求

**POST** `/mfa/recovery/request`

**功能**: 用戶遺失 MFA 設備和備份碼時，發起恢復請求。

**請求** (`MfaRecoveryRequestForm`):
```json
{
  "email": "user@example.com",  // 用戶郵箱
  "reason": "手機遺失，無法使用 Google Authenticator"  // 恢復原因
}
```

**響應**:
```json
{
  "code": 1,
  "msg": "恢復請求已提交，請等待管理員審核",
  "data": null
}
```

**副作用**:
- 創建恢復請求（狀態 = PENDING）
- 發送郵件通知 Super Admin
- 記錄 CRITICAL 級別審計日誌

---

### 10. 查詢待審核恢復請求（Super Admin）

**GET** `/mfa/recovery/pending`

**權限**: `mfa:recovery:admin`

**請求**:
```http
GET /mfa/recovery/pending HTTP/1.1
x-access-token: <JWT Token (Super Admin)>
```

**響應** (`List<MfaRecoveryRequestVO>`):
```json
{
  "code": 1,
  "msg": "成功",
  "data": [
    {
      "recoveryId": 1001,
      "employeeId": 123,
      "employeeName": "張三",
      "email": "user@example.com",
      "reason": "手機遺失，無法使用 Google Authenticator",
      "status": "PENDING",
      "requestedAt": "2026-03-10T10:00:00Z",
      "ipAddress": "192.168.1.100"
    }
  ]
}
```

---

### 11. 批准恢復請求（Super Admin）

**POST** `/mfa/recovery/approve`

**權限**: `mfa:recovery:admin`

**請求**:
```http
POST /mfa/recovery/approve?recoveryId=1001 HTTP/1.1
x-access-token: <JWT Token (Super Admin)>
```

**響應**:
```json
{
  "code": 1,
  "msg": "恢復請求已批准，恢復碼已發送到用戶郵箱",
  "data": null
}
```

**副作用**:
- 生成 8 位隨機恢復碼（Argon2id 加密）
- 設置 24 小時有效期
- 發送郵件給用戶（包含恢復碼）
- 更新請求狀態為 APPROVED
- 記錄 CRITICAL 審計日誌

---

### 12. 拒絕恢復請求（Super Admin）

**POST** `/mfa/recovery/reject`

**權限**: `mfa:recovery:admin`

**請求** (`MfaRecoveryRejectForm`):
```json
{
  "recoveryId": 1001,
  "rejectionReason": "身份驗證不通過"
}
```

**響應**:
```json
{
  "code": 1,
  "msg": "恢復請求已拒絕",
  "data": null
}
```

**副作用**:
- 更新請求狀態為 REJECTED
- 記錄拒絕原因
- 發送郵件通知用戶
- 記錄 WARNING 審計日誌

---

### 13. 使用恢復碼重置 MFA

**POST** `/mfa/recovery/reset`

**功能**: 用戶使用 Super Admin 批准後收到的 8 位恢復碼重置 MFA。

**請求** (`MfaRecoveryResetForm`):
```json
{
  "recoveryCode": "12345678"  // 8 位恢復碼
}
```

**響應**:
```json
{
  "code": 1,
  "msg": "MFA 已成功重置",
  "data": null
}
```

**副作用**:
- 禁用 MFA 配置（mfa_enabled = false）
- 軟刪除所有備份碼（deleted = true）
- 軟刪除所有信任設備（deleted = true）
- 標記恢復碼為已使用
- 記錄 CRITICAL 審計日誌

**錯誤響應示例**:
```json
{
  "code": 30002,
  "level": "error",
  "msg": "恢復碼已過期或無效",
  "data": null
}
```

---

## 🛠️ 導出 Postman Collection

### 方法 1: 從 Knife4j UI 導出（推薦）

1. 訪問 Knife4j UI: [http://localhost:1024/doc.html](http://localhost:1024/doc.html)
2. 點擊右上角 **"OpenAPI 規範"** 按鈕
3. 選擇 **"下載 JSON"** 或 **"下載 YAML"**
4. 在 Postman 中導入：
   - File → Import → Upload Files
   - 選擇下載的 OpenAPI JSON/YAML 文件
   - Postman 會自動生成 Collection

### 方法 2: 直接訪問 OpenAPI JSON

1. 訪問 OpenAPI JSON: [http://localhost:1024/v3/api-docs](http://localhost:1024/v3/api-docs)
2. 複製 JSON 內容
3. 在 Postman 中導入：
   - File → Import → Raw Text
   - 粘貼 JSON 內容
   - Generate Collection

### 方法 3: 使用 Postman OpenAPI 3.0 Generator

```bash
# 下載 OpenAPI 規範
curl http://localhost:1024/v3/api-docs -o smartadmin-openapi.json

# 使用 Postman CLI 導入
postman collection import smartadmin-openapi.json
```

### Postman Collection 結構預覽

導入後的 Collection 將包含以下結構：

```
SmartAdmin API
├── System.SYSTEM_MFA
│   ├── POST /mfa/setup/init
│   ├── POST /mfa/setup/enable
│   ├── POST /mfa/setup/disable
│   ├── GET /mfa/status
│   ├── POST /mfa/verify
│   ├── POST /mfa/backup-codes/regenerate
│   ├── GET /mfa/backup-codes/count
│   ├── POST /mfa/trusted-devices/add
│   ├── POST /mfa/recovery/request
│   ├── GET /mfa/recovery/pending
│   ├── POST /mfa/recovery/approve
│   ├── POST /mfa/recovery/reject
│   └── POST /mfa/recovery/reset
└── ...其他模塊
```

---

## 📊 響應碼說明

### 成功響應 (code = 1)

```json
{
  "code": 1,
  "msg": "成功",
  "data": <響應數據>
}
```

### 錯誤響應 (code != 1)

| Code | Level | Message | 說明 |
|------|-------|---------|------|
| 30001 | warning | MFA 驗證碼錯誤 | TOTP token 或備份碼錯誤 |
| 30002 | error | 恢復碼已過期或無效 | 恢復碼過期（>24h）或已使用 |
| 30003 | warning | MFA 未啟用 | 用戶尚未啟用 MFA |
| 30004 | error | MFA 已啟用 | 用戶已經啟用 MFA（重複啟用） |
| 30005 | error | 恢復請求已存在 | 用戶已有 PENDING 狀態的恢復請求 |
| 30006 | error | 角色強制要求 MFA | 無法禁用 MFA（enforcedByRole=true） |

---

## 🧪 測試指南

### 單元測試

```bash
cd smart-admin-api-java21-springboot3
./gradlew :smartadmin-app:test --tests "net.lab1024.sa.system.mfa.*Test"
```

**測試覆蓋率**: 676 tests passed (100%)

### 集成測試

```bash
./gradlew :smartadmin-app:test --tests "*IntegrationTest"
```

**關鍵集成測試**:
- `MfaSetupIntegrationTest` - 完整 MFA 設定流程
- `MfaVerificationIntegrationTest` - TOTP + 備份碼驗證流程
- `MfaRecoveryIntegrationTest` - 設備遺失恢復流程

### API 測試腳本（Postman）

導入 Postman Collection 後，使用以下測試腳本：

**1. 完整 MFA 啟用流程**:
```javascript
// Test 1: POST /mfa/setup/init
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});
pm.test("Response contains secret", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.secret).to.exist;
    pm.collectionVariables.set("mfa_secret", jsonData.data.secret);
});

// Test 2: POST /mfa/setup/enable
// (需手動輸入 6 位 TOTP token)
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});
pm.test("Response contains 10 backup codes", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.backupCodes).to.have.lengthOf(10);
});
```

---

## 🔒 安全建議

### 1. 生產環境配置

**啟用 Knife4j Basic Auth**:
```yaml
knife4j:
  enable: true
  basic:
    enable: true  # ⚠️ 生產環境必須啟用
    username: api_admin
    password: <強密碼>
```

### 2. HTTPS 強制

生產環境必須使用 HTTPS：

```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: <密碼>
    key-store-type: PKCS12
```

### 3. CORS 配置

限制允許的來源：

```yaml
spring:
  mvc:
    cors:
      allowed-origins:
        - https://admin.yourdomain.com
      allowed-methods:
        - GET
        - POST
      allowed-headers:
        - x-access-token
        - x-refresh-token
```

### 4. Rate Limiting

使用 Sentinel 限流：

```java
@SentinelResource(
    value = "mfa-verify",
    blockHandler = "handleRateLimitException",
    fallback = "handleFallback"
)
public ResponseDTO<String> verifyMfa(...) {
    // ...
}
```

**限流規則建議**:
- `/mfa/verify`: 每分鐘最多 5 次（防止暴力破解）
- `/mfa/setup/init`: 每小時最多 3 次（防止濫用）
- `/mfa/recovery/request`: 每天最多 1 次（防止濫用）

---

## 📞 技術支持

**問題反饋**: [GitHub Issues](https://github.com/lab1024/smart-admin/issues)
**文檔版本**: 1.0.0
**最後更新**: 2026-03-10

---

**文檔維護者**: SmartAdmin MFA Team
**審閱者**: SmartAdmin Architecture Team
**批准者**: Project Lead
