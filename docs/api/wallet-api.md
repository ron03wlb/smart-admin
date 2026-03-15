# 錢包 API 文檔

**版本**: v4.1.0
**基礎路徑**: `/igaming/wallet`
**認證**: 需要 Access Token (Header: `Authorization`)

---

## 1. 創建錢包

**端點**: `POST /igaming/wallet/create`
**描述**: 為玩家創建新錢包（CASH, BONUS, CREDIT 類型）

### 請求體 (WalletCreateForm)
```json
{
  "playerId": 10001,
  "walletType": "CASH",
  "currency": "CNY"
}
```

### 參數說明
| 字段 | 類型 | 必填 | 說明 |
|------|------|------|------|
| playerId | Long | ✅ | 玩家 ID |
| walletType | String | ✅ | 錢包類型：CASH, BONUS, CREDIT |
| currency | String | ✅ | 貨幣代碼（CNY, USD, EUR 等）|

### 響應
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "walletId": 100001,
    "playerId": 10001,
    "walletType": "CASH",
    "currency": "CNY",
    "balance": "0.0000",
    "lockedAmount": "0.0000",
    "deleted": false,
    "createTime": "2026-03-11T10:00:00",
    "updateTime": "2026-03-11T10:00:00"
  }
}
```

---

## 2. 查詢錢包詳情

**端點**: `GET /igaming/wallet/get/{walletId}`
**描述**: 根據錢包 ID 查詢錢包詳情

### 路徑參數
| 參數 | 類型 | 說明 |
|------|------|------|
| walletId | Long | 錢包 ID |

### 響應
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "walletId": 100001,
    "playerId": 10001,
    "walletType": "CASH",
    "currency": "CNY",
    "balance": "1000.5000",
    "lockedAmount": "100.0000",
    "deleted": false,
    "createTime": "2026-03-11T10:00:00",
    "updateTime": "2026-03-11T10:30:00"
  }
}
```

---

## 3. 分頁查詢錢包

**端點**: `POST /igaming/wallet/query`
**描述**: 根據條件分頁查詢錢包列表

### 請求體 (WalletQueryForm)
```json
{
  "playerId": 10001,
  "walletType": "CASH",
  "pageNum": 1,
  "pageSize": 10,
  "searchCount": true
}
```

### 參數說明
| 字段 | 類型 | 必填 | 說明 |
|------|------|------|------|
| playerId | Long | ❌ | 玩家 ID（篩選條件）|
| walletType | String | ❌ | 錢包類型（篩選條件）|
| pageNum | Integer | ✅ | 頁碼（從 1 開始）|
| pageSize | Integer | ✅ | 每頁數量（最大 100）|
| searchCount | Boolean | ✅ | 是否查詢總數 |

### 響應
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "total": 3,
    "list": [
      {
        "walletId": 100001,
        "playerId": 10001,
        "walletType": "CASH",
        "currency": "CNY",
        "balance": "1000.5000",
        "lockedAmount": "100.0000"
      }
    ],
    "pageNum": 1,
    "pageSize": 10
  }
}
```

---

## 4. 存款（Credit）

**端點**: `POST /igaming/wallet/credit`
**描述**: 增加錢包餘額（存款、中獎等）

### 請求體 (WalletCreditForm)
```json
{
  "walletId": 100001,
  "amount": "500.00",
  "requestId": "deposit_20260311_1234567890",
  "referenceType": "DEPOSIT",
  "referenceId": "ORDER_20260311_001",
  "description": "銀行卡存款"
}
```

### 參數說明
| 字段 | 類型 | 必填 | 說明 |
|------|------|------|------|
| walletId | Long | ✅ | 錢包 ID |
| amount | BigDecimal | ✅ | 存款金額（必須 > 0）|
| requestId | String | ✅ | 請求唯一標識（冪等性保證）|
| referenceType | String | ✅ | 參考類型（DEPOSIT, GAME_WIN 等）|
| referenceId | String | ✅ | 參考 ID（如訂單號）|
| description | String | ❌ | 描述信息 |

### 響應
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "transactionId": 500001,
    "walletId": 100001,
    "transactionType": "CREDIT",
    "amount": "500.0000",
    "balanceBefore": "1000.5000",
    "balanceAfter": "1500.5000",
    "requestId": "deposit_20260311_1234567890",
    "createTime": "2026-03-11T10:45:00"
  }
}
```

### 錯誤響應
```json
{
  "code": 30001,
  "msg": "錢包不存在或已凍結",
  "data": null
}
```

---

## 5. 扣款（Debit）

**端點**: `POST /igaming/wallet/debit`
**描述**: 減少錢包餘額（下注、提款等）

### 請求體 (WalletDebitForm)
```json
{
  "walletId": 100001,
  "amount": "200.00",
  "requestId": "bet_20260311_9876543210",
  "referenceType": "BET",
  "referenceId": "GAME_ROUND_123456",
  "description": "老虎機下注"
}
```

### 參數說明
| 字段 | 類型 | 必填 | 說明 |
|------|------|------|------|
| walletId | Long | ✅ | 錢包 ID |
| amount | BigDecimal | ✅ | 扣款金額（必須 > 0）|
| requestId | String | ✅ | 請求唯一標識（冪等性保證）|
| referenceType | String | ✅ | 參考類型（BET, WITHDRAW 等）|
| referenceId | String | ✅ | 參考 ID（如遊戲回合 ID）|
| description | String | ❌ | 描述信息 |

### 響應
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "transactionId": 500002,
    "walletId": 100001,
    "transactionType": "DEBIT",
    "amount": "-200.0000",
    "balanceBefore": "1500.5000",
    "balanceAfter": "1300.5000",
    "requestId": "bet_20260311_9876543210",
    "createTime": "2026-03-11T11:00:00"
  }
}
```

### 錯誤響應
```json
{
  "code": 30002,
  "msg": "餘額不足",
  "data": null
}
```

---

## 6. 鎖定資金

**端點**: `POST /igaming/wallet/lock`
**描述**: 鎖定錢包部分資金（用於提款審核等）

### 請求體 (WalletLockForm)
```json
{
  "walletId": 100001,
  "lockAmount": "300.00",
  "lockReason": "提款審核中",
  "referenceType": "WITHDRAWAL",
  "referenceId": "WITHDRAW_20260311_001"
}
```

### 參數說明
| 字段 | 類型 | 必填 | 說明 |
|------|------|------|------|
| walletId | Long | ✅ | 錢包 ID |
| lockAmount | BigDecimal | ✅ | 鎖定金額（必須 > 0）|
| lockReason | String | ✅ | 鎖定原因 |
| referenceType | String | ✅ | 參考類型 |
| referenceId | String | ✅ | 參考 ID |

### 響應
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "lockId": 700001,
    "walletId": 100001,
    "lockAmount": "300.0000",
    "lockReason": "提款審核中",
    "locked": true,
    "createTime": "2026-03-11T11:15:00"
  }
}
```

---

## 7. 解鎖資金

**端點**: `DELETE /igaming/wallet/unlock/{lockId}`
**描述**: 解鎖已鎖定的資金

### 路徑參數
| 參數 | 類型 | 說明 |
|------|------|------|
| lockId | Long | 鎖定記錄 ID |

### 響應
```json
{
  "code": 0,
  "msg": "success",
  "data": "資金解鎖成功"
}
```

---

## 8. 交易記錄查詢

**端點**: `POST /igaming/wallet/transaction/query`
**描述**: 分頁查詢錢包交易記錄

### 請求體 (WalletTransactionQueryForm)
```json
{
  "walletId": 100001,
  "transactionType": "CREDIT",
  "startTime": "2026-03-11T00:00:00",
  "endTime": "2026-03-11T23:59:59",
  "pageNum": 1,
  "pageSize": 20,
  "searchCount": true
}
```

### 參數說明
| 字段 | 類型 | 必填 | 說明 |
|------|------|------|------|
| walletId | Long | ❌ | 錢包 ID（篩選條件）|
| transactionType | String | ❌ | 交易類型（CREDIT, DEBIT）|
| startTime | LocalDateTime | ❌ | 開始時間 |
| endTime | LocalDateTime | ❌ | 結束時間 |
| pageNum | Integer | ✅ | 頁碼 |
| pageSize | Integer | ✅ | 每頁數量 |

### 響應
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "total": 150,
    "list": [
      {
        "transactionId": 500002,
        "walletId": 100001,
        "transactionType": "DEBIT",
        "amount": "-200.0000",
        "balanceBefore": "1500.5000",
        "balanceAfter": "1300.5000",
        "requestId": "bet_20260311_9876543210",
        "referenceType": "BET",
        "referenceId": "GAME_ROUND_123456",
        "description": "老虎機下注",
        "createTime": "2026-03-11T11:00:00"
      }
    ],
    "pageNum": 1,
    "pageSize": 20
  }
}
```

---

## 9. 錢包匯總報表

**端點**: `GET /igaming/wallet/report/summary`
**描述**: 獲取錢包匯總統計數據（總餘額、總鎖定金額等）

### 查詢參數
| 參數 | 類型 | 必填 | 說明 |
|------|------|------|------|
| playerId | Long | ❌ | 玩家 ID（不傳則查詢所有）|
| walletType | String | ❌ | 錢包類型（不傳則所有類型）|

### 響應
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "totalBalance": "15000.5000",
    "totalLockedAmount": "500.0000",
    "availableBalance": "14500.5000",
    "walletCount": 3,
    "breakdown": [
      {
        "walletType": "CASH",
        "balance": "10000.5000",
        "lockedAmount": "300.0000",
        "count": 1
      },
      {
        "walletType": "BONUS",
        "balance": "5000.0000",
        "lockedAmount": "200.0000",
        "count": 2
      }
    ]
  }
}
```

---

## 錯誤碼參考

| 錯誤碼 | 錯誤訊息 | 說明 |
|-------|---------|------|
| 30001 | Wallet not found or frozen | 錢包不存在或已凍結 |
| 30002 | Insufficient balance | 餘額不足 |
| 30003 | Invalid amount | 金額無效（≤ 0 或格式錯誤）|
| 30004 | Duplicate request ID | 請求 ID 重複（冪等性檢查）|
| 30005 | Lock record not found | 鎖定記錄不存在 |
| 30006 | Lock amount exceeds available balance | 鎖定金額超過可用餘額 |
| 30007 | Wallet type mismatch | 錢包類型不匹配 |
| 30008 | Currency not supported | 不支持的貨幣 |

---

## 安全機制

### 1. 三層防護機制
- **Layer 1**: Redisson 分布式鎖（防止並發衝突）
- **Layer 2**: MyBatis Plus 樂觀鎖（`@Version` 自動檢查）
- **Layer 3**: Request ID 去重（唯一約束保證冪等性）

### 2. 冪等性保證
所有涉及餘額變動的操作都需要提供唯一的 `requestId`。相同的 `requestId` 重複請求將返回原有結果，不會重複扣款或存款。

**示例**：
```json
// 第一次請求
POST /igaming/wallet/debit
{ "requestId": "bet_123", "amount": "100.00", ... }
→ 扣款成功，返回交易記錄

// 重複請求（相同 requestId）
POST /igaming/wallet/debit
{ "requestId": "bet_123", "amount": "100.00", ... }
→ 返回已存在的交易記錄，不會再次扣款
```

### 3. 餘額精度
- 所有金額使用 `DECIMAL(19,4)` 精度（最多 4 位小數）
- 無浮點數舍入誤差
- 支持貨幣：CNY, USD, EUR, GBP, JPY 等

---

## 測試數據

測試環境已創建 100 個測試錢包（Wallet ID: 1-100），可使用以下測試數據：

**測試玩家 ID**: 1-20
**測試錢包類型**: CASH, BONUS, CREDIT
**總測試餘額**: 1,000,000 CNY

**測試 Access Token**:
```
7e775f88a6fd47be91f8854a43ff4eac
```

**測試請求示例**（使用 curl）：
```bash
# 查詢錢包詳情
curl -X GET "http://localhost:1024/igaming/wallet/get/1" \
  -H "Authorization: 7e775f88a6fd47be91f8854a43ff4eac"

# 存款 100 元
curl -X POST "http://localhost:1024/igaming/wallet/credit" \
  -H "Authorization: 7e775f88a6fd47be91f8854a43ff4eac" \
  -H "Content-Type: application/json" \
  -d '{"walletId":1,"amount":"100.00","requestId":"test_credit_001","referenceType":"TEST","referenceId":"TEST_001"}'
```

---

## 性能指標

**k6 性能測試結果**（2026-03-11）：
- **總請求數**: 20,206
- **HTTP 成功率**: 100%
- **業務邏輯成功率**: 100%
- **TPS**: 168 transactions/s
- **P95 響應時間**: 68.04ms < 100ms ✓
- **平均響應時間**: 31.21ms
- **餘額一致性**: 100%（0 個重複扣款）

---

**文檔版本**: v1.0.0
**最後更新**: 2026-03-11
**維護者**: iGaming Team
