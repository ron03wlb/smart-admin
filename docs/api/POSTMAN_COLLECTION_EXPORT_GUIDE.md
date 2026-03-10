# Postman Collection 導出指南

**版本**: 1.0.0
**適用於**: SmartAdmin v4.1.0
**生成日期**: 2026-03-10

---

## 📋 概述

本指南介紹如何從 SmartAdmin Knife4j UI 導出 Postman Collection，並在 Postman 中使用。

---

## 🚀 快速開始

### 前置要求

- ✅ SmartAdmin 應用程序已啟動（端口 1024）
- ✅ 已安裝 Postman Desktop 或 Postman Web

### 步驟 1: 啟動應用程序

```bash
cd smart-admin-api-java21-springboot3
./gradlew :smartadmin-app:bootRun
```

**確認啟動成功**:
```bash
curl http://localhost:1024/actuator/health
# 預期響應: {"status":"UP"}
```

---

## 方法 1: 從 Knife4j UI 導出（推薦）

### 1.1 訪問 Knife4j UI

在瀏覽器中打開: [http://localhost:1024/doc.html](http://localhost:1024/doc.html)

![Knife4j UI](https://via.placeholder.com/800x400?text=Knife4j+UI+Screenshot)

### 1.2 導出 OpenAPI 規範

1. 點擊頁面右上角的 **"OpenAPI 規範"** 按鈕
2. 選擇以下選項之一：
   - **下載 JSON** (推薦) - 適用於 Postman
   - **下載 YAML** - 適用於 OpenAPI 工具鏈

**示例檔名**: `smartadmin-openapi-v3.json`

### 1.3 在 Postman 中導入

**步驟**:
1. 打開 Postman Desktop 或 Postman Web
2. 點擊 **File → Import** (或使用快捷鍵 `Ctrl+O`)
3. 選擇 **Upload Files** 標籤
4. 選擇剛才下載的 `smartadmin-openapi-v3.json`
5. 點擊 **Import**

**導入配置（可選）**:
- ✅ **Generate Collection from Imported API**: 啟用
- ✅ **Generate a Postman Collection**: 啟用
- ⚠️ **Folder Organization**: 選擇 "Tags" (按 API Tag 分組)
- ⚠️ **Parameter Generation**: 選擇 "Example" (使用示例值)

### 1.4 驗證導入結果

導入成功後，Postman 左側應顯示新的 Collection:

```
SmartAdmin API
├── System.SYSTEM_MFA (13 個請求)
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

## 方法 2: 直接下載 OpenAPI JSON

### 2.1 使用 curl 下載

```bash
curl http://localhost:1024/v3/api-docs -o smartadmin-openapi.json
```

### 2.2 使用瀏覽器下載

1. 訪問: [http://localhost:1024/v3/api-docs](http://localhost:1024/v3/api-docs)
2. 右鍵 → **另存為...** → 保存為 `smartadmin-openapi.json`

### 2.3 在 Postman 中導入

與方法 1.3 相同。

---

## 方法 3: 使用 Postman CLI

### 3.1 安裝 Postman CLI

```bash
# macOS/Linux
curl -o- "https://dl.pstmn.io/install/linux64.tar.gz" | tar -xz

# Windows (PowerShell)
Invoke-WebRequest -Uri "https://dl.pstmn.io/install/win64.zip" -OutFile "postman-cli.zip"
Expand-Archive -Path "postman-cli.zip" -DestinationPath "C:\Program Files\Postman CLI"
```

### 3.2 登入 Postman

```bash
postman login
```

### 3.3 導入 Collection

```bash
# 下載 OpenAPI 規範
curl http://localhost:1024/v3/api-docs -o smartadmin-openapi.json

# 導入到 Postman
postman collection import smartadmin-openapi.json
```

---

## 🔧 配置 Postman Collection

### 1. 設置環境變量

**創建新環境**:
1. 點擊 Postman 左上角的 **Environments** 標籤
2. 點擊 **Create Environment**
3. 輸入環境名稱: `SmartAdmin - Dev`

**添加變量**:

| Variable Name | Initial Value | Current Value | Type |
|---------------|---------------|---------------|------|
| `base_url` | `http://localhost:1024` | `http://localhost:1024` | default |
| `access_token` | `` | `<動態設置>` | secret |
| `refresh_token` | `` | `<動態設置>` | secret |
| `employee_id` | `1` | `<動態設置>` | default |

### 2. 配置 Collection 認證

**步驟**:
1. 在 Collection 上右鍵 → **Edit**
2. 選擇 **Authorization** 標籤
3. 選擇 **Type**: `Bearer Token`
4. 在 **Token** 欄位輸入: `{{access_token}}`

**替代方案**（使用 Pre-request Script）:
```javascript
// 在 Collection 的 Pre-request Script 中添加
pm.request.headers.add({
    key: "x-access-token",
    value: pm.environment.get("access_token")
});

pm.request.headers.add({
    key: "x-refresh-token",
    value: pm.environment.get("refresh_token")
});
```

### 3. 添加自動認證腳本

**登入請求（POST /login）的 Tests 腳本**:
```javascript
// 解析響應
var jsonData = pm.response.json();

// 檢查登入成功
pm.test("Login successful", function () {
    pm.expect(jsonData.code).to.eql(1);
});

// 保存 tokens 到環境變量
if (jsonData.code === 1 && jsonData.data) {
    pm.environment.set("access_token", jsonData.data.accessToken);
    pm.environment.set("refresh_token", jsonData.data.refreshToken);
    pm.environment.set("employee_id", jsonData.data.employeeId);

    console.log("Tokens saved to environment variables");
}
```

---

## 📝 MFA API 測試流程

### 測試場景 1: 完整 MFA 啟用流程

**步驟**:

**1. POST /mfa/setup/init**
```javascript
// Tests
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response contains secret", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.secret).to.exist;

    // 保存 secret 到環境變量
    pm.environment.set("mfa_secret", jsonData.data.secret);
    console.log("MFA Secret:", jsonData.data.secret);
});
```

**2. 掃描 QR 碼（手動）**
- 複製響應中的 `qrCodeDataUrl`
- 在瀏覽器中打開（Base64 圖片）
- 使用 Google Authenticator 掃描 QR 碼

**3. POST /mfa/setup/enable**

**Request Body**:
```json
{
  "mfaToken": "123456",  // ⚠️ 手動輸入 Google Authenticator 顯示的 6 位數字
  "trustDevice": true,
  "deviceName": "Postman Test Device"
}
```

**Tests**:
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response contains 10 backup codes", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.backupCodes).to.have.lengthOf(10);

    // 保存第一個備份碼到環境變量（測試用）
    pm.environment.set("backup_code", jsonData.data.backupCodes[0]);
    console.log("Backup Codes:", jsonData.data.backupCodes.join(", "));
});
```

**4. GET /mfa/status**

**Tests**:
```javascript
pm.test("MFA is enabled", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.mfaEnabled).to.be.true;
    pm.expect(jsonData.data.backupCodesRemaining).to.eql(10);
});
```

---

### 測試場景 2: MFA 驗證流程

**1. POST /mfa/verify (使用 TOTP)**

**Request Body**:
```json
{
  "mfaToken": "{{totp_from_authenticator}}",  // ⚠️ 手動輸入
  "trustDevice": false
}
```

**2. POST /mfa/verify (使用備份碼)**

**Request Body**:
```json
{
  "mfaToken": "{{backup_code}}",  // 使用環境變量
  "trustDevice": false
}
```

**Tests**:
```javascript
pm.test("Backup code verification successful", function () {
    pm.response.to.have.status(200);
});

// 驗證備份碼數量減少
pm.sendRequest({
    url: pm.environment.get("base_url") + "/mfa/backup-codes/count",
    method: "GET",
    header: {
        "x-access-token": pm.environment.get("access_token")
    }
}, function (err, res) {
    pm.test("Backup codes count decreased", function () {
        var count = res.json().data;
        pm.expect(count).to.eql(9);  // 使用一個後剩餘 9 個
    });
});
```

---

### 測試場景 3: MFA 恢復流程（需 Super Admin）

**1. POST /mfa/recovery/request**

**Request Body**:
```json
{
  "email": "user@example.com",
  "reason": "手機遺失，無法使用 Google Authenticator"
}
```

**2. GET /mfa/recovery/pending (Super Admin)**

**Tests**:
```javascript
pm.test("Pending request exists", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data).to.be.an("array");
    pm.expect(jsonData.data.length).to.be.greaterThan(0);

    // 保存 recoveryId 到環境變量
    pm.environment.set("recovery_id", jsonData.data[0].recoveryId);
});
```

**3. POST /mfa/recovery/approve (Super Admin)**

**Query Params**:
- `recoveryId`: `{{recovery_id}}`

**4. POST /mfa/recovery/reset**

**Request Body**:
```json
{
  "recoveryCode": "12345678"  // ⚠️ 從郵件中獲取
}
```

---

## 🔍 故障排查

### 問題 1: 導入後無請求

**原因**: OpenAPI 規範格式不正確或版本不兼容。

**解決方案**:
1. 確認 OpenAPI 版本: `3.0.x`
2. 驗證 JSON 格式: [https://editor.swagger.io/](https://editor.swagger.io/)
3. 更新 Postman 到最新版本

### 問題 2: 所有請求返回 401 Unauthorized

**原因**: 未設置認證 token。

**解決方案**:
1. 確認環境變量 `access_token` 已設置
2. 檢查 Collection 的 Authorization 配置
3. 執行登入請求並運行 Tests 腳本保存 token

### 問題 3: 請求返回 404 Not Found

**原因**: 應用程序未啟動或端口不正確。

**解決方案**:
```bash
# 檢查應用程序是否運行
curl http://localhost:1024/actuator/health

# 檢查端口占用
netstat -ano | findstr :1024  # Windows
lsof -i :1024  # macOS/Linux
```

### 問題 4: MFA 驗證失敗（TOTP token 錯誤）

**原因**: 時間不同步或 secret 錯誤。

**解決方案**:
1. 同步系統時間（TOTP 基於時間）
2. 重新執行 `/mfa/setup/init` 並掃描新的 QR 碼
3. 確認使用的 secret 是最新的

---

## 📚 相關資源

- **完整 API 文檔**: [MFA_API_DOCUMENTATION.md](./MFA_API_DOCUMENTATION.md)
- **SmartAdmin 官方文檔**: [README.md](../../README.md)
- **Postman 官方文檔**: [https://learning.postman.com/](https://learning.postman.com/)
- **OpenAPI 3.0 規範**: [https://spec.openapis.org/oas/v3.0.3](https://spec.openapis.org/oas/v3.0.3)

---

## 📞 技術支持

**問題反饋**: [GitHub Issues](https://github.com/lab1024/smart-admin/issues)
**文檔版本**: 1.0.0
**最後更新**: 2026-03-10

---

**文檔維護者**: SmartAdmin MFA Team
**審閱者**: SmartAdmin QA Team
