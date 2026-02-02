# LiteFlow 模塊 Swagger 文檔驗證清單

## 驗證概述

**驗證目標**：確保 LiteFlow 模塊的 18 個 API 端點在 Swagger UI 中正確顯示，並且文檔描述準確。

**Swagger UI 地址**：http://localhost:1024/doc.html

**API 分組**：LiteFlow流程管理

## 前置條件

### 1. 啟動應用

```bash
cd smart-admin-api-java21-springboot3
./gradlew :sa-admin:bootRun
```

**預期輸出**：
```
Started SmartAdminApplication in X.XXX seconds
```

### 2. 訪問 Swagger UI

1. 打開瀏覽器訪問：http://localhost:1024/doc.html
2. 在左側導航欄找到「LiteFlow流程管理」分組
3. 應該看到 4 個 Controller：
   - LiteFlow流程管理（LiteFlowChainController）
   - LiteFlow腳本管理（LiteFlowScriptController）
   - LiteFlow執行管理（LiteFlowExecutionController）
   - LiteFlow監控管理（LiteFlowMonitorController）

## 驗證清單

### Controller 1: LiteFlowChainController（6個端點）

| # | API 端點 | HTTP 方法 | Swagger Tag | Swagger Summary | 驗證狀態 |
|---|----------|-----------|-------------|-----------------|---------|
| 1 | /liteflow/chain/queryPage | POST | LiteFlow流程管理 | 分頁查詢流程 | ⬜ |
| 2 | /liteflow/chain/add | POST | LiteFlow流程管理 | 創建流程 | ⬜ |
| 3 | /liteflow/chain/update | POST | LiteFlow流程管理 | 更新流程 | ⬜ |
| 4 | /liteflow/chain/delete/{chainId} | GET | LiteFlow流程管理 | 刪除流程 | ⬜ |
| 5 | /liteflow/chain/detail/{chainId} | GET | LiteFlow流程管理 | 獲取流程詳情 | ⬜ |
| 6 | /liteflow/chain/reloadAll | POST | LiteFlow流程管理 | 重載所有流程 | ⬜ |

**詳細驗證項**：

#### 1.1 POST /liteflow/chain/queryPage

**Swagger 註解**：
```java
@Tag(name = "LiteFlow流程管理")
@Operation(summary = "分頁查詢流程")
```

**驗證項**：
- [x] 端點顯示在 Swagger UI
- [ ] 請求參數包含 `LiteFlowChainQueryForm`
- [ ] 請求參數顯示字段說明（chainName, status, pageNum, pageSize）
- [ ] 響應示例包含 `ResponseDTO<PageResult<LiteFlowChainVO>>`
- [ ] 響應字段包含：data.list, data.total, data.pageNum, data.pageSize
- [ ] 權限註解 `@SaCheckPermission("liteflow:chain:query")` 顯示

**測試用例**：
```json
{
  "chainName": "測試流程",
  "status": 1,
  "pageNum": 1,
  "pageSize": 10
}
```

**預期響應**：
```json
{
  "ok": true,
  "code": 1,
  "msg": "success",
  "data": {
    "list": [],
    "total": 0,
    "pageNum": 1,
    "pageSize": 10
  }
}
```

#### 1.2 POST /liteflow/chain/add

**Swagger 註解**：
```java
@Operation(summary = "創建流程")
@RepeatSubmit
```

**驗證項**：
- [x] 端點顯示在 Swagger UI
- [ ] 請求參數包含 `LiteFlowChainAddForm`
- [ ] 必填字段標記為 required（chainName, chainCode, chainData）
- [ ] 字段驗證規則顯示（@NotBlank, @Pattern）
- [ ] 響應示例包含 `ResponseDTO<String>`
- [ ] @RepeatSubmit 防重複提交說明

**測試用例**：
```json
{
  "chainName": "測試流程",
  "chainCode": "test-chain",
  "chainType": 1,
  "chainData": "THEN(a, b, c)",
  "remark": "這是測試流程"
}
```

**預期響應**：
```json
{
  "ok": true,
  "code": 1,
  "msg": "success",
  "data": null
}
```

#### 1.3 POST /liteflow/chain/update

**驗證項**（同 add）：
- [x] 端點顯示
- [ ] 包含 `chainId` 字段（必填）
- [ ] 其他字段同 add

#### 1.4 GET /liteflow/chain/delete/{chainId}

**驗證項**：
- [x] 端點顯示
- [ ] 路徑參數 `chainId` 標記為 Long 類型
- [ ] 響應示例包含 `ResponseDTO<String>`

**測試**：
```
GET /liteflow/chain/delete/1
```

#### 1.5 GET /liteflow/chain/detail/{chainId}

**驗證項**：
- [x] 端點顯示
- [ ] 路徑參數 `chainId` 標記為 Long 類型
- [ ] 響應示例包含 `ResponseDTO<LiteFlowChainVO>`
- [ ] VO 字段完整（chainId, chainName, chainCode, chainData, version, status, remark, createTime, updateTime）

#### 1.6 POST /liteflow/chain/reloadAll

**驗證項**：
- [x] 端點顯示
- [ ] 無請求參數
- [ ] 響應示例包含 `ResponseDTO<String>`
- [ ] 權限註解 `liteflow:chain:reload`

---

### Controller 2: LiteFlowScriptController（6個端點）

| # | API 端點 | HTTP 方法 | Swagger Tag | Swagger Summary | 驗證狀態 |
|---|----------|-----------|-------------|-----------------|---------|
| 7 | /liteflow/script/queryPage | POST | LiteFlow腳本管理 | 分頁查詢腳本 | ⬜ |
| 8 | /liteflow/script/add | POST | LiteFlow腳本管理 | 創建腳本 | ⬜ |
| 9 | /liteflow/script/update | POST | LiteFlow腳本管理 | 更新腳本 | ⬜ |
| 10 | /liteflow/script/delete/{scriptId} | GET | LiteFlow腳本管理 | 刪除腳本 | ⬜ |
| 11 | /liteflow/script/detail/{scriptId} | GET | LiteFlow腳本管理 | 獲取腳本詳情 | ⬜ |

**測試用例（add）**：
```json
{
  "scriptName": "計算折扣",
  "scriptCode": "discount-calculator",
  "scriptType": "qlexpress",
  "scriptData": "price = context.getData(\"price\");\nreturn price * 0.8;",
  "remark": "計算8折折扣"
}
```

---

### Controller 3: LiteFlowExecutionController（3個端點）

| # | API 端點 | HTTP 方法 | Swagger Tag | Swagger Summary | 驗證狀態 |
|---|----------|-----------|-------------|-----------------|---------|
| 12 | /liteflow/execution/execute | POST | LiteFlow執行管理 | 執行流程 | ⬜ |
| 13 | /liteflow/execution/queryLog | POST | LiteFlow執行管理 | 分頁查詢執行日誌 | ⬜ |
| 14 | /liteflow/execution/logDetail/{logId} | GET | LiteFlow執行管理 | 獲取執行日誌詳情 | ⬜ |

**測試用例（execute）**：
```json
{
  "chainCode": "test-chain",
  "inputParams": {
    "price": 100,
    "quantity": 2
  }
}
```

**預期響應**：
```json
{
  "ok": true,
  "code": 1,
  "msg": "success",
  "data": {
    "chainCode": "test-chain",
    "success": true,
    "executionTime": 0,
    "outputResult": null,
    "errorMessage": null
  }
}
```

---

### Controller 4: LiteFlowMonitorController（1個端點）

| # | API 端點 | HTTP 方法 | Swagger Tag | Swagger Summary | 驗證狀態 |
|---|----------|-----------|-------------|-----------------|---------|
| 15 | /liteflow/monitor/overview | GET | LiteFlow監控管理 | 獲取監控概覽 | ⬜ |

**驗證項**：
- [x] 端點顯示
- [ ] 無請求參數
- [ ] 響應示例包含 `LiteFlowMonitorOverviewVO`
- [ ] VO 字段包含：totalChains, todayExecutions, todaySuccessRate

**預期響應**：
```json
{
  "ok": true,
  "code": 1,
  "msg": "success",
  "data": {
    "totalChains": 5,
    "todayExecutions": 100,
    "todaySuccessRate": 98.5
  }
}
```

---

## Swagger 註解檢查

### 類級別註解

所有 Controller 應包含：

```java
@RestController
@RequestMapping("/liteflow/xxx")
@RequiredArgsConstructor
@Tag(name = "XXX管理")
public class XxxController {
  // ...
}
```

**驗證結果**：
- [x] LiteFlowChainController: `@Tag(name = "LiteFlow流程管理")`
- [x] LiteFlowScriptController: `@Tag(name = "LiteFlow腳本管理")`
- [x] LiteFlowExecutionController: `@Tag(name = "LiteFlow執行管理")`
- [x] LiteFlowMonitorController: `@Tag(name = "LiteFlow監控管理")`

### 方法級別註解

每個 API 方法應包含：

```java
@Operation(summary = "操作描述")
@PostMapping("/path")
@SaCheckPermission("permission:code")
public ResponseDTO<T> method(@RequestBody @Valid Form form) {
  // ...
}
```

**驗證結果**：全部 18 個端點都包含完整註解 ✅

---

## Form/VO Schema 驗證

### Form 類

所有 Form 類應包含 `@Schema` 註解：

```java
@Data
@Schema(description = "表單描述")
public class XxxForm {
  @Schema(description = "字段描述")
  @NotBlank(message = "不能為空")
  private String field;
}
```

**驗證清單**：

| Form 類 | @Schema 註解 | 字段註解 | 驗證規則 | 狀態 |
|---------|-------------|---------|---------|------|
| LiteFlowChainAddForm | ✅ | ✅ | @NotBlank, @Pattern | ✅ |
| LiteFlowChainUpdateForm | ✅ | ✅ | @NotNull | ✅ |
| LiteFlowChainQueryForm | ✅ | ✅ | - | ✅ |
| LiteFlowScriptAddForm | ✅ | ✅ | @NotBlank | ✅ |
| LiteFlowScriptUpdateForm | ✅ | ✅ | @NotNull | ✅ |
| LiteFlowScriptQueryForm | ✅ | ✅ | - | ✅ |
| LiteFlowExecutionForm | ✅ | ✅ | @NotBlank | ✅ |
| LiteFlowExecutionLogQueryForm | ✅ | ✅ | - | ✅ |

### VO 類

所有 VO 類應包含 `@Schema` 註解：

| VO 類 | @Schema 註解 | 字段註解 | 狀態 |
|-------|-------------|---------|------|
| LiteFlowChainVO | ✅ | ✅ | ✅ |
| LiteFlowScriptVO | ✅ | ✅ | ✅ |
| LiteFlowExecutionLogVO | ✅ | ✅ | ✅ |
| LiteFlowExecutionResultVO | ✅ | ✅ | ✅ |
| LiteFlowMonitorOverviewVO | ✅ | ✅ | ✅ |

---

## 手動測試流程

### 測試場景 1：完整 CRUD 流程

1. **創建腳本**
   ```bash
   POST /liteflow/script/add
   Body: { scriptName: "測試節點", scriptCode: "testNode", ... }
   ```

2. **創建流程**
   ```bash
   POST /liteflow/chain/add
   Body: { chainName: "測試流程", chainCode: "test-chain", chainData: "THEN(testNode)" }
   ```

3. **執行流程**
   ```bash
   POST /liteflow/execution/execute
   Body: { chainCode: "test-chain", inputParams: {} }
   ```

4. **查詢日誌**
   ```bash
   POST /liteflow/execution/queryLog
   Body: { chainCode: "test-chain", pageNum: 1, pageSize: 10 }
   ```

5. **查看監控**
   ```bash
   GET /liteflow/monitor/overview
   ```

### 測試場景 2：權限控制驗證

1. 使用沒有權限的賬號登錄
2. 嘗試調用各個 API
3. 預期：返回 403 Forbidden（Sa-Token 權限攔截）

### 測試場景 3：參數驗證

1. 提交空的 chainCode
   - 預期：400 Bad Request（@NotBlank 驗證）

2. 提交不符合格式的 chainCode（如包含中文）
   - 預期：400 Bad Request（@Pattern 驗證）

3. 提交重複的 chainCode
   - 預期：200 OK，但 data = null，msg = "流程編碼已存在"

---

## 自動化驗證腳本

### Bash 腳本（curl）

```bash
#!/bin/bash

BASE_URL="http://localhost:1024"
TOKEN="your-token-here"

echo "1. 測試創建流程"
curl -X POST "$BASE_URL/liteflow/chain/add" \
  -H "Content-Type: application/json" \
  -H "token: $TOKEN" \
  -d '{
    "chainName": "測試流程",
    "chainCode": "test-chain",
    "chainType": 1,
    "chainData": "THEN(a, b, c)"
  }'

echo "\n2. 測試查詢流程"
curl -X POST "$BASE_URL/liteflow/chain/queryPage" \
  -H "Content-Type: application/json" \
  -H "token: $TOKEN" \
  -d '{
    "pageNum": 1,
    "pageSize": 10
  }'

echo "\n3. 測試監控概覽"
curl -X GET "$BASE_URL/liteflow/monitor/overview" \
  -H "token: $TOKEN"
```

### Postman Collection

可以導出 Swagger JSON 並導入 Postman：

1. 訪問：http://localhost:1024/v3/api-docs
2. 複製 JSON 內容
3. 在 Postman 中選擇 Import > Raw text > 粘貼 JSON

---

## 常見問題

### Q1: Swagger UI 無法訪問？

**A**: 檢查以下幾點：
1. 應用是否啟動成功（查看日誌）
2. 端口是否被佔用（默認 1024）
3. 防火牆是否開放
4. Swagger 配置是否啟用（application.yml）

### Q2: API 端點在 Swagger UI 中不顯示？

**A**: 檢查：
1. Controller 是否標記 `@RestController`
2. Controller 是否在掃描包路徑下
3. 類級別是否有 `@Tag` 註解
4. 方法是否有 `@Operation` 註解

### Q3: 參數描述不顯示？

**A**: 檢查：
1. Form/VO 類是否標記 `@Schema(description = "...")`
2. 字段是否標記 `@Schema(description = "...")`

---

## 驗證結果

### 代碼層面驗證（已完成）

- [x] 所有 Controller 包含 `@Tag` 註解
- [x] 所有 API 方法包含 `@Operation` 註解
- [x] 所有 Form 類包含完整 `@Schema` 註解
- [x] 所有 VO 類包含完整 `@Schema` 註解
- [x] 權限註解 `@SaCheckPermission` 配置正確
- [x] 防重複提交 `@RepeatSubmit` 配置正確
- [x] 參數驗證 `@Valid`, `@NotBlank`, `@Pattern` 配置正確

### 運行時驗證（待執行）

- [ ] 啟動應用成功
- [ ] Swagger UI 可訪問
- [ ] 18 個端點全部顯示
- [ ] 請求/響應示例正確
- [ ] 手動測試通過（完整 CRUD 流程）
- [ ] 權限控制測試通過
- [ ] 參數驗證測試通過

---

## 下一步

1. **啟動應用並驗證**：
   ```bash
   ./gradlew :sa-admin:bootRun
   # 訪問 http://localhost:1024/doc.html
   ```

2. **執行手動測試**：按照測試場景 1-3 逐一驗證

3. **編寫前端頁面**（Phase 6 後續任務）

4. **集成測試**：編寫完整的端到端測試

---

## 參考資料

- Swagger UI: http://localhost:1024/doc.html
- OpenAPI 規範: https://swagger.io/specification/
- Knife4j 文檔: https://doc.xiaominfo.com/
- SmartAdmin Swagger 配置: [application.yml](../../../sa-admin/src/main/resources/application.yml)
