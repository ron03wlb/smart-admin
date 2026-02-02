# LiteFlow 使用示例

本文檔提供 LiteFlow 流程編排的實際使用示例，涵蓋常見業務場景。

---

## 目錄

- [示例 1: 訂單處理流程](#示例-1-訂單處理流程)
- [示例 2: 員工審批工作流](#示例-2-員工審批工作流)
- [示例 3: 數據同步流程](#示例-3-數據同步流程)
- [示例 4: 條件分支流程](#示例-4-條件分支流程)
- [示例 5: 並行執行流程](#示例-5-並行執行流程)
- [示例 6: 循環處理流程](#示例-6-循環處理流程)

---

## 示例 1: 訂單處理流程

### 業務場景

電商平台的訂單處理流程，包括訂單驗證、庫存檢查、折扣計算和支付處理。

### 流程設計

```
訂單提交 → 驗證訂單 → 檢查庫存 → 計算折扣 → 處理支付 → 創建訂單
```

### 步驟 1: 創建腳本節點

#### 1.1 驗證訂單

```bash
POST /liteflow/script/add
```

```json
{
  "scriptName": "驗證訂單",
  "scriptCode": "validateOrder",
  "scriptType": "qlexpress",
  "scriptData": "orderId = context.getData('orderId'); if (orderId == null || orderId <= 0) { context.setData('error', '訂單ID無效'); return false; } customerId = context.getData('customerId'); if (customerId == null) { context.setData('error', '客戶ID無效'); return false; } return true;",
  "remark": "驗證訂單必填字段"
}
```

#### 1.2 檢查庫存

```json
{
  "scriptName": "檢查庫存",
  "scriptCode": "checkInventory",
  "scriptType": "qlexpress",
  "scriptData": "productId = context.getData('productId'); quantity = context.getData('quantity'); availableStock = 100; if (quantity > availableStock) { context.setData('error', '庫存不足'); return false; } context.setData('inventoryChecked', true); return true;",
  "remark": "檢查商品庫存是否充足"
}
```

#### 1.3 計算折扣

```json
{
  "scriptName": "計算折扣",
  "scriptCode": "calculateDiscount",
  "scriptType": "qlexpress",
  "scriptData": "price = context.getData('price'); quantity = context.getData('quantity'); totalPrice = price * quantity; discount = 0; if (totalPrice > 500) { discount = totalPrice * 0.1; } else if (totalPrice > 1000) { discount = totalPrice * 0.15; } finalPrice = totalPrice - discount; context.setData('finalPrice', finalPrice); context.setData('discount', discount); return true;",
  "remark": "根據訂單總額計算折扣"
}
```

#### 1.4 處理支付

```json
{
  "scriptName": "處理支付",
  "scriptCode": "processPayment",
  "scriptType": "qlexpress",
  "scriptData": "finalPrice = context.getData('finalPrice'); paymentMethod = context.getData('paymentMethod'); if (paymentMethod == null) { context.setData('error', '未指定支付方式'); return false; } context.setData('paymentSuccess', true); context.setData('transactionId', 'TXN' + System.currentTimeMillis()); return true;",
  "remark": "處理支付邏輯"
}
```

### 步驟 2: 創建流程

```bash
POST /liteflow/chain/add
```

```json
{
  "chainName": "訂單處理流程",
  "chainCode": "order-processing",
  "chainType": 1,
  "chainData": "THEN(validateOrder, checkInventory, calculateDiscount, processPayment)",
  "remark": "電商訂單處理完整流程"
}
```

### 步驟 3: 執行流程

```bash
POST /liteflow/execution/execute
```

```json
{
  "chainCode": "order-processing",
  "inputParams": {
    "orderId": 12345,
    "customerId": 67890,
    "productId": 101,
    "quantity": 3,
    "price": 200.0,
    "paymentMethod": "credit-card"
  }
}
```

### 步驟 4: 查看執行結果

**響應示例**:

```json
{
  "ok": true,
  "code": 1,
  "msg": "success",
  "data": {
    "success": true,
    "chainCode": "order-processing",
    "executionTime": 125,
    "outputResult": {
      "orderId": 12345,
      "customerId": 67890,
      "inventoryChecked": true,
      "finalPrice": 540.0,
      "discount": 60.0,
      "paymentSuccess": true,
      "transactionId": "TXN1738404567890"
    }
  }
}
```

### 步驟 5: 查詢執行日誌

```bash
POST /liteflow/execution/queryLog
```

```json
{
  "chainCode": "order-processing",
  "pageNum": 1,
  "pageSize": 10
}
```

---

## 示例 2: 員工審批工作流

### 業務場景

員工請假申請的多級審批流程，包括直屬經理審批、HR 審批和總經理審批。

### 流程設計

```
請假申請 → 驗證申請 → 經理審批 → HR審批 → 總經理審批 → 通知員工
```

### 步驟 1: 創建腳本節點

#### 1.1 驗證申請

```json
{
  "scriptName": "驗證請假申請",
  "scriptCode": "validateLeaveRequest",
  "scriptType": "qlexpress",
  "scriptData": "employeeId = context.getData('employeeId'); leaveDays = context.getData('leaveDays'); leaveType = context.getData('leaveType'); if (employeeId == null || leaveDays == null || leaveType == null) { context.setData('error', '申請信息不完整'); return false; } if (leaveDays <= 0 || leaveDays > 30) { context.setData('error', '請假天數無效'); return false; } context.setData('validated', true); return true;",
  "remark": "驗證請假申請的有效性"
}
```

#### 1.2 經理審批

```json
{
  "scriptName": "經理審批",
  "scriptCode": "managerApproval",
  "scriptType": "qlexpress",
  "scriptData": "leaveDays = context.getData('leaveDays'); if (leaveDays <= 3) { context.setData('managerApproved', true); context.setData('skipHRApproval', true); } else { context.setData('managerApproved', true); context.setData('skipHRApproval', false); } return true;",
  "remark": "經理審批邏輯（3天以下自動通過）"
}
```

#### 1.3 HR 審批

```json
{
  "scriptName": "HR審批",
  "scriptCode": "hrApproval",
  "scriptType": "qlexpress",
  "scriptData": "skipHRApproval = context.getData('skipHRApproval'); if (skipHRApproval) { context.setData('hrApproved', true); return true; } leaveDays = context.getData('leaveDays'); if (leaveDays <= 7) { context.setData('hrApproved', true); } else { context.setData('hrApproved', false); context.setData('requireCEOApproval', true); } return true;",
  "remark": "HR審批邏輯（7天以上需CEO審批）"
}
```

#### 1.4 總經理審批

```json
{
  "scriptName": "總經理審批",
  "scriptCode": "ceoApproval",
  "scriptType": "qlexpress",
  "scriptData": "requireCEOApproval = context.getData('requireCEOApproval'); if (requireCEOApproval == null || !requireCEOApproval) { return true; } context.setData('ceoApproved', true); context.setData('finalApproved', true); return true;",
  "remark": "總經理最終審批"
}
```

### 步驟 2: 創建流程

```json
{
  "chainName": "員工請假審批流程",
  "chainCode": "leave-approval",
  "chainType": 1,
  "chainData": "THEN(validateLeaveRequest, managerApproval, hrApproval, ceoApproval)",
  "remark": "員工請假多級審批工作流"
}
```

### 步驟 3: 執行流程

```json
{
  "chainCode": "leave-approval",
  "inputParams": {
    "employeeId": 1001,
    "employeeName": "張三",
    "leaveDays": 5,
    "leaveType": "annual",
    "startDate": "2026-03-01",
    "endDate": "2026-03-05",
    "reason": "家庭事務"
  }
}
```

### 步驟 4: 查看執行結果

```json
{
  "ok": true,
  "code": 1,
  "data": {
    "success": true,
    "chainCode": "leave-approval",
    "executionTime": 98,
    "outputResult": {
      "validated": true,
      "managerApproved": true,
      "hrApproved": true,
      "skipHRApproval": false,
      "requireCEOApproval": false,
      "finalApproved": true
    }
  }
}
```

---

## 示例 3: 數據同步流程

### 業務場景

將客戶數據從源系統同步到目標系統，包括數據提取、轉換和加載（ETL）。

### 流程設計

```
觸發同步 → 提取源數據 → 數據轉換 → 驗證數據 → 加載目標系統 → 記錄日誌
```

### 步驟 1: 創建腳本節點

#### 1.1 提取源數據

```json
{
  "scriptName": "提取源數據",
  "scriptCode": "extractData",
  "scriptType": "qlexpress",
  "scriptData": "customerId = context.getData('customerId'); sourceData = new HashMap(); sourceData.put('id', customerId); sourceData.put('name', 'Customer' + customerId); sourceData.put('email', 'customer' + customerId + '@example.com'); sourceData.put('phone', '1234567890'); context.setData('sourceData', sourceData); return true;",
  "remark": "從源系統提取客戶數據"
}
```

#### 1.2 數據轉換

```json
{
  "scriptName": "數據轉換",
  "scriptCode": "transformData",
  "scriptType": "qlexpress",
  "scriptData": "sourceData = context.getData('sourceData'); targetData = new HashMap(); targetData.put('customer_id', sourceData.get('id')); targetData.put('full_name', sourceData.get('name')); targetData.put('email_address', sourceData.get('email')); targetData.put('phone_number', sourceData.get('phone')); context.setData('targetData', targetData); return true;",
  "remark": "轉換數據格式以符合目標系統"
}
```

#### 1.3 驗證數據

```json
{
  "scriptName": "驗證數據",
  "scriptCode": "validateData",
  "scriptType": "qlexpress",
  "scriptData": "targetData = context.getData('targetData'); if (targetData.get('customer_id') == null) { context.setData('error', '客戶ID不能為空'); return false; } if (targetData.get('email_address') == null) { context.setData('error', '郵箱地址不能為空'); return false; } context.setData('dataValid', true); return true;",
  "remark": "驗證轉換後的數據完整性"
}
```

#### 1.4 加載目標系統

```json
{
  "scriptName": "加載目標系統",
  "scriptCode": "loadData",
  "scriptType": "qlexpress",
  "scriptData": "targetData = context.getData('targetData'); context.setData('loadSuccess', true); context.setData('recordId', 'REC' + System.currentTimeMillis()); return true;",
  "remark": "將數據加載到目標系統"
}
```

### 步驟 2: 創建流程

```json
{
  "chainName": "客戶數據同步流程",
  "chainCode": "customer-data-sync",
  "chainType": 1,
  "chainData": "THEN(extractData, transformData, validateData, loadData)",
  "remark": "ETL 數據同步流程"
}
```

### 步驟 3: 執行流程

```json
{
  "chainCode": "customer-data-sync",
  "inputParams": {
    "customerId": 5001,
    "syncType": "full",
    "triggerSource": "scheduled-job"
  }
}
```

---

## 示例 4: 條件分支流程

### 業務場景

根據訂單金額決定不同的審批流程。

### 流程設計

```
IF(金額 > 10000, THEN(高額審批), ELSE(普通審批))
```

### 步驟 1: 創建腳本節點

#### 1.1 金額判斷

```json
{
  "scriptName": "判斷訂單金額",
  "scriptCode": "checkAmount",
  "scriptType": "qlexpress",
  "scriptData": "amount = context.getData('amount'); return amount > 10000;",
  "remark": "判斷訂單金額是否超過 10000"
}
```

#### 1.2 高額審批

```json
{
  "scriptName": "高額訂單審批",
  "scriptCode": "highAmountApproval",
  "scriptType": "qlexpress",
  "scriptData": "context.setData('approvalLevel', 'senior-manager'); context.setData('approved', true); return true;",
  "remark": "高額訂單需要高級經理審批"
}
```

#### 1.3 普通審批

```json
{
  "scriptName": "普通訂單審批",
  "scriptCode": "normalApproval",
  "scriptType": "qlexpress",
  "scriptData": "context.setData('approvalLevel', 'manager'); context.setData('approved', true); return true;",
  "remark": "普通訂單經理審批即可"
}
```

### 步驟 2: 創建條件流程

```json
{
  "chainName": "條件審批流程",
  "chainCode": "conditional-approval",
  "chainType": 2,
  "chainData": "IF(checkAmount, THEN(highAmountApproval), ELSE(normalApproval))",
  "remark": "根據金額選擇審批流程"
}
```

### 步驟 3: 執行流程

**場景 A: 高額訂單**

```json
{
  "chainCode": "conditional-approval",
  "inputParams": {
    "orderId": 20001,
    "amount": 15000
  }
}
```

**結果**: `approvalLevel = "senior-manager"`

**場景 B: 普通訂單**

```json
{
  "chainCode": "conditional-approval",
  "inputParams": {
    "orderId": 20002,
    "amount": 5000
  }
}
```

**結果**: `approvalLevel = "manager"`

---

## 示例 5: 並行執行流程

### 業務場景

用戶註冊時同時執行多個獨立任務：發送歡迎郵件、發送短信、記錄日誌。

### 流程設計

```
WHEN(發送郵件, 發送短信, 記錄日誌)
```

### 步驟 1: 創建腳本節點

#### 1.1 發送郵件

```json
{
  "scriptName": "發送歡迎郵件",
  "scriptCode": "sendEmail",
  "scriptType": "qlexpress",
  "scriptData": "email = context.getData('email'); context.setData('emailSent', true); return true;",
  "remark": "發送歡迎郵件"
}
```

#### 1.2 發送短信

```json
{
  "scriptName": "發送短信通知",
  "scriptCode": "sendSMS",
  "scriptType": "qlexpress",
  "scriptData": "phone = context.getData('phone'); context.setData('smsSent', true); return true;",
  "remark": "發送註冊短信"
}
```

#### 1.3 記錄日誌

```json
{
  "scriptName": "記錄註冊日誌",
  "scriptCode": "logRegistration",
  "scriptType": "qlexpress",
  "scriptData": "userId = context.getData('userId'); context.setData('logRecorded', true); return true;",
  "remark": "記錄用戶註冊日誌"
}
```

### 步驟 2: 創建並行流程

```json
{
  "chainName": "用戶註冊並行任務",
  "chainCode": "user-registration-parallel",
  "chainType": 1,
  "chainData": "WHEN(sendEmail, sendSMS, logRegistration)",
  "remark": "用戶註冊時並行執行多個任務"
}
```

### 步驟 3: 執行流程

```json
{
  "chainCode": "user-registration-parallel",
  "inputParams": {
    "userId": 3001,
    "email": "user3001@example.com",
    "phone": "13800138000"
  }
}
```

### 步驟 4: 查看結果

```json
{
  "ok": true,
  "data": {
    "success": true,
    "chainCode": "user-registration-parallel",
    "executionTime": 85,
    "outputResult": {
      "emailSent": true,
      "smsSent": true,
      "logRecorded": true
    }
  }
}
```

**優勢**: 並行執行，總執行時間遠小於順序執行的總和。

---

## 示例 6: 循環處理流程

### 業務場景

批量處理訂單，對每個訂單執行相同的處理邏輯。

### 流程設計

```
FOR(訂單列表).DO(THEN(驗證訂單, 更新狀態))
```

### 步驟 1: 創建腳本節點

#### 1.1 驗證訂單

```json
{
  "scriptName": "驗證單個訂單",
  "scriptCode": "validateSingleOrder",
  "scriptType": "qlexpress",
  "scriptData": "order = context.getData('currentOrder'); if (order.get('status') == 'pending') { context.setData('orderValid', true); } else { context.setData('orderValid', false); } return true;",
  "remark": "驗證單個訂單狀態"
}
```

#### 1.2 更新狀態

```json
{
  "scriptName": "更新訂單狀態",
  "scriptCode": "updateOrderStatus",
  "scriptType": "qlexpress",
  "scriptData": "order = context.getData('currentOrder'); order.put('status', 'processed'); context.setData('statusUpdated', true); return true;",
  "remark": "更新訂單為已處理狀態"
}
```

### 步驟 2: 創建循環流程

```json
{
  "chainName": "批量處理訂單",
  "chainCode": "batch-order-processing",
  "chainType": 3,
  "chainData": "FOR(orderList).DO(THEN(validateSingleOrder, updateOrderStatus))",
  "remark": "批量處理訂單列表"
}
```

### 步驟 3: 執行流程

```json
{
  "chainCode": "batch-order-processing",
  "inputParams": {
    "orderList": [
      {"orderId": 101, "status": "pending"},
      {"orderId": 102, "status": "pending"},
      {"orderId": 103, "status": "pending"}
    ]
  }
}
```

### 步驟 4: 查看結果

```json
{
  "ok": true,
  "data": {
    "success": true,
    "chainCode": "batch-order-processing",
    "executionTime": 156,
    "outputResult": {
      "processedCount": 3,
      "orderList": [
        {"orderId": 101, "status": "processed"},
        {"orderId": 102, "status": "processed"},
        {"orderId": 103, "status": "processed"}
      ]
    }
  }
}
```

---

## 最佳實踐

### 1. 流程設計原則

- ✅ **單一職責**: 每個腳本節點只做一件事
- ✅ **幂等性**: 流程可以安全重試
- ✅ **錯誤處理**: 捕獲並記錄所有異常
- ✅ **超時控制**: 設置合理的執行超時時間

### 2. 腳本編寫建議

- ✅ 使用 `context.getData()` 和 `context.setData()` 傳遞數據
- ✅ 返回 `true` 表示成功，`false` 表示失敗
- ✅ 設置 `context.setData('error', '錯誤信息')` 記錄錯誤
- ✅ 避免在腳本中進行複雜計算（委派給 Manager 層）

### 3. 性能優化

- ✅ 使用 `WHEN` 並行執行獨立任務
- ✅ 啟用緩存減少數據庫查詢
- ✅ 避免在循環中執行重複查詢
- ✅ 合理設置執行超時

### 4. 測試流程

在生產環境部署前，務必進行測試：

1. **單元測試**: 測試每個腳本節點
2. **集成測試**: 測試完整流程
3. **壓力測試**: 模擬高並發場景
4. **異常測試**: 測試錯誤處理邏輯

---

## 相關文檔

- [README.md](README.md) - 模塊概述和快速開始
- [HOT_RELOAD_INTEGRATION.md](HOT_RELOAD_INTEGRATION.md) - 熱重載指南
- [PERMISSION_SETUP.md](PERMISSION_SETUP.md) - 權限配置
- [API Specification](../../../docs/plans/liteflow/api-specification.md) - 完整 API 文檔

---

**Last Updated**: 2026-02-02
**Author**: SmartAdmin Team
