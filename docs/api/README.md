# SmartAdmin iGaming API 文檔

**版本**: v4.1.0
**最後更新**: 2026-03-11

---

## 📚 文檔索引

### API 文檔
- [錢包 API](wallet-api.md) - 錢包管理、存款、扣款、資金鎖定/解鎖、交易查詢
- [支付 API](payment-api.md) - 存款、提款、訂單查詢、Webhook 回調

### OpenAPI 規格
- [openapi.json](openapi.json) - OpenAPI 3.1.0 完整規格（377KB，所有模塊）

### Postman Collection
- [SmartAdmin-iGaming-API.postman_collection.json](SmartAdmin-iGaming-API.postman_collection.json) - Postman 測試集合（v2.1.0 格式）

---

## 🚀 快速開始

### 前置條件
- SmartAdmin 應用程式正在運行（`http://localhost:1024`）
- 已獲取 Access Token（預設測試 Token: `7e775f88a6fd47be91f8854a43ff4eac`）

### 使用 Postman

**步驟 1: 導入 Collection**
1. 打開 Postman
2. 點擊 `Import` 按鈕
3. 選擇 `SmartAdmin-iGaming-API.postman_collection.json`
4. 點擊 `Import`

**步驟 2: 配置環境變數**
Collection 已預設以下變數（Collection Variables）：
- `base_url`: `http://localhost:1024`
- `access_token`: `7e775f88a6fd47be91f8854a43ff4eac`

如需修改，請點擊 Collection → Variables 標籤頁。

**步驟 3: 測試 API**
1. 展開 `Wallet API` 或 `Payment API` 資料夾
2. 選擇任意請求（如 `Create Wallet`）
3. 點擊 `Send` 按鈕
4. 查看響應結果

### 使用 curl

**查詢錢包詳情**:
```bash
curl -X GET "http://localhost:1024/igaming/wallet/get/1" \
  -H "Authorization: 7e775f88a6fd47be91f8854a43ff4eac"
```

**存款 100 元**:
```bash
curl -X POST "http://localhost:1024/igaming/wallet/credit" \
  -H "Authorization: 7e775f88a6fd47be91f8854a43ff4eac" \
  -H "Content-Type: application/json" \
  -d '{
    "walletId": 1,
    "amount": "100.00",
    "requestId": "test_credit_001",
    "referenceType": "TEST",
    "referenceId": "TEST_001"
  }'
```

**發起存款（Mock PSP 成功案例）**:
```bash
curl -X POST "http://localhost:1024/igaming/payment/deposit" \
  -H "Authorization: 7e775f88a6fd47be91f8854a43ff4eac" \
  -H "Content-Type: application/json" \
  -d '{
    "walletId": 1,
    "amount": "100.00",
    "pspCode": "mock",
    "paymentMethod": "bank_card",
    "returnUrl": "https://example.com/return",
    "callbackUrl": "https://example.com/callback"
  }'
```

---

## 🔑 認證

所有 API 端點都需要 Access Token 認證（除了 Swagger UI 和 Health Check）。

### 獲取 Access Token

**步驟 1: 獲取驗證碼**
```bash
curl -X GET "http://localhost:1024/support/captcha"
```

響應示例:
```json
{
  "code": 0,
  "data": {
    "captchaUuid": "abc123-def456-ghi789",
    "captchaText": "1234",
    "captchaBase64": "data:image/png;base64,..."
  }
}
```

**步驟 2: 登入獲取 Token**
```bash
curl -X POST "http://localhost:1024/login" \
  -H "Content-Type: application/json" \
  -d '{
    "loginName": "admin",
    "password": "123456",
    "captchaCode": "1234",
    "captchaUuid": "abc123-def456-ghi789",
    "loginDevice": 1
  }'
```

響應示例:
```json
{
  "code": 0,
  "data": {
    "token": "7e775f88a6fd47be91f8854a43ff4eac",
    "employeeId": 1,
    "employeeName": "admin"
  }
}
```

**步驟 3: 使用 Token**
在所有後續請求中添加 Header:
```
Authorization: 7e775f88a6fd47be91f8854a43ff4eac
```

---

## 📊 API 端點摘要

### 錢包 API (`/igaming/wallet`)
| 端點 | 方法 | 描述 |
|------|------|------|
| `/create` | POST | 創建錢包 |
| `/get/{walletId}` | GET | 查詢錢包詳情 |
| `/query` | POST | 分頁查詢錢包 |
| `/credit` | POST | 存款（增加餘額）|
| `/debit` | POST | 扣款（減少餘額）|
| `/lock` | POST | 鎖定資金 |
| `/unlock/{lockId}` | DELETE | 解鎖資金 |
| `/transaction/query` | POST | 查詢交易記錄 |
| `/report/summary` | GET | 錢包匯總報表 |

### 支付 API (`/igaming/payment`)
| 端點 | 方法 | 描述 |
|------|------|------|
| `/deposit` | POST | 發起存款 |
| `/withdraw` | POST | 發起提款 |
| `/get/{paymentOrderId}` | GET | 查詢訂單詳情 |
| `/query` | POST | 分頁查詢訂單 |
| `/callback/{pspCode}` | POST | Webhook 回調（PSP 調用）|

---

## 🧪 測試數據

### 測試錢包
- **Wallet ID**: 1-100（已創建 100 個測試錢包）
- **Player ID**: 1-20（測試玩家）
- **總測試餘額**: 1,000,000 CNY
- **Wallet Type**: CASH, BONUS, CREDIT

### 測試場景

**場景 1: 完整存款流程**
1. 查詢錢包餘額（`GET /wallet/get/1`）
2. 發起存款（`POST /payment/deposit`, amount: `100.00`）
3. Mock PSP 自動完成支付（Webhook 回調）
4. 再次查詢錢包餘額（確認已增加 100 元）

**場景 2: Mock PSP 測試**
- **成功**: `amount: "100.00"` → SUCCESS
- **失敗**: `amount: "100.01"` → FAILED
- **超時**: `amount: "100.02"` → TIMEOUT

**場景 3: 冪等性測試**
1. 使用相同 `requestId` 調用 2 次 `/wallet/credit`
2. 第一次返回新交易記錄
3. 第二次返回已存在的交易記錄（不會重複存款）

---

## 🔒 安全機制

### 錢包三層防護
- **Layer 1**: Redisson 分布式鎖（防止並發衝突）
- **Layer 2**: MyBatis Plus 樂觀鎖（`@Version` 自動檢查）
- **Layer 3**: Request ID 去重（唯一約束保證冪等性）

### Webhook 四層防護
- **Layer 1**: HMAC-SHA256 簽名驗證
- **Layer 2**: 時間戳驗證（5 分鐘容忍度）
- **Layer 3**: 常數時間比較（防止時序攻擊）
- **Layer 4**: 冪等性檢查（狀態驗證）

---

## 📈 性能指標

### k6 性能測試結果（2026-03-11）
- **總請求數**: 20,206
- **HTTP 成功率**: 100%
- **業務邏輯成功率**: 100%
- **TPS**: 168 transactions/s
- **P95 響應時間**: 68.04ms < 100ms ✓
- **平均響應時間**: 31.21ms
- **餘額一致性**: 100%（0 個重複扣款）

### 性能目標
- **錢包 Credit/Debit**: P95 < 100ms
- **支付存款**: P95 < 200ms（含 PSP API）
- **支付提款審核**: 自動審核 < 5 分鐘
- **訂單查詢**: P95 < 30ms
- **對帳準確率**: 99.9%（偏差 < 0.1%）

---

## 🐛 錯誤碼參考

### 錢包錯誤碼（30xxx）
| 錯誤碼 | 錯誤訊息 |
|-------|---------|
| 30001 | Wallet not found or frozen |
| 30002 | Insufficient balance |
| 30003 | Invalid amount |
| 30004 | Duplicate request ID |
| 30005 | Lock record not found |
| 30006 | Lock amount exceeds available balance |

### 支付錯誤碼（40xxx）
| 錯誤碼 | 錯誤訊息 |
|-------|---------|
| 40001 | Payment order not found |
| 40002 | PSP not supported |
| 40003 | Invalid payment method |
| 40006 | Insufficient balance for withdrawal |
| 40007 | Withdrawal blocked by risk check |
| 40009 | Invalid signature (Webhook) |

---

## 📖 相關文檔

- [SmartAdmin 架構規則](.agent/rules/foundation/F04-architecture-rules.md) - SmartAdmin 分層架構規範
- [SmartAdmin 模式](.claude/shared/knowledge/smartadmin-patterns.md) - ResponseDTO, Pagination, Bean Conversion 等
- [Phase 1.5 實施計劃](C:\Users\ron.chang\.claude\plans\memoized-beaming-pretzel.md) - iGaming 後端基礎設施實施計劃

---

## 🆘 支持

- **GitHub Issues**: [https://github.com/1024-lab/smart-admin/issues](https://github.com/1024-lab/smart-admin/issues)
- **文檔**: [https://1024lab.net](https://1024lab.net)
- **Email**: lab1024@163.com

---

**維護者**: iGaming Team
**版本**: v1.0.0
**最後更新**: 2026-03-11
