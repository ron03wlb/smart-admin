# LiteFlow 模塊權限碼配置指南

## 概述

LiteFlow 模塊共需配置 **13 個權限碼**，分佈在 4 個控制器中。所有權限碼需通過 **系統管理 > 菜單管理** 界面配置。

## 權限碼命名規範

格式：`liteflow:{模塊}:{操作}`

- **模塊**：chain（流程）、script（腳本）、execution（執行）、monitor（監控）
- **操作**：query（查詢）、add（新增）、update（更新）、delete（刪除）、execute（執行）、reload（重載）

## 配置步驟

### 1. 創建 LiteFlow 主菜單

在菜單管理中創建頂層菜單：

```yaml
菜單名稱: LiteFlow流程管理
菜單類型: 目錄
路由地址: /liteflow
菜單圖標: workflow
排序: 100
顯示狀態: 是
```

### 2. 創建子菜單和權限碼

#### 2.1 流程管理（Chain）

**菜單配置**：
```yaml
菜單名稱: 流程管理
父級菜單: LiteFlow流程管理
菜單類型: 菜單
路由地址: chain
組件路徑: liteflow/chain/index
顯示狀態: 是
```

**功能點權限碼（6個）**：

| 功能點名稱 | 權限碼 | 請求方式 | 後端地址 | Controller 方法 |
|-----------|--------|---------|----------|----------------|
| 查詢流程 | `liteflow:chain:query` | POST | /liteflow/chain/queryPage | queryPage() |
| 新增流程 | `liteflow:chain:add` | POST | /liteflow/chain/add | add() |
| 更新流程 | `liteflow:chain:update` | POST | /liteflow/chain/update | update() |
| 刪除流程 | `liteflow:chain:delete` | GET | /liteflow/chain/delete/{chainId} | delete() |
| 查看詳情 | `liteflow:chain:query` | GET | /liteflow/chain/detail/{chainId} | getDetail() |
| 重載流程 | `liteflow:chain:reload` | POST | /liteflow/chain/reloadAll | reloadAll() |

#### 2.2 腳本管理（Script）

**菜單配置**：
```yaml
菜單名稱: 腳本管理
父級菜單: LiteFlow流程管理
菜單類型: 菜單
路由地址: script
組件路徑: liteflow/script/index
顯示狀態: 是
```

**功能點權限碼（5個）**：

| 功能點名稱 | 權限碼 | 請求方式 | 後端地址 | Controller 方法 |
|-----------|--------|---------|----------|----------------|
| 查詢腳本 | `liteflow:script:query` | POST | /liteflow/script/queryPage | queryPage() |
| 新增腳本 | `liteflow:script:add` | POST | /liteflow/script/add | add() |
| 更新腳本 | `liteflow:script:update` | POST | /liteflow/script/update | update() |
| 刪除腳本 | `liteflow:script:delete` | GET | /liteflow/script/delete/{scriptId} | delete() |
| 查看詳情 | `liteflow:script:query` | GET | /liteflow/script/detail/{scriptId} | getDetail() |

#### 2.3 執行管理（Execution）

**菜單配置**：
```yaml
菜單名稱: 執行管理
父級菜單: LiteFlow流程管理
菜單類型: 菜單
路由地址: execution
組件路徑: liteflow/execution/index
顯示狀態: 是
```

**功能點權限碼（3個）**：

| 功能點名稱 | 權限碼 | 請求方式 | 後端地址 | Controller 方法 |
|-----------|--------|---------|----------|----------------|
| 執行流程 | `liteflow:execution:execute` | POST | /liteflow/execution/execute | execute() |
| 查詢日誌 | `liteflow:execution:query` | POST | /liteflow/execution/queryLog | queryLog() |
| 日誌詳情 | `liteflow:execution:query` | GET | /liteflow/execution/logDetail/{logId} | getLogDetail() |

#### 2.4 監控管理（Monitor）

**菜單配置**：
```yaml
菜單名稱: 監控管理
父級菜單: LiteFlow流程管理
菜單類型: 菜單
路由地址: monitor
組件路徑: liteflow/monitor/index
顯示狀態: 是
```

**功能點權限碼（1個）**：

| 功能點名稱 | 權限碼 | 請求方式 | 後端地址 | Controller 方法 |
|-----------|--------|---------|----------|----------------|
| 查詢監控 | `liteflow:monitor:query` | GET | /liteflow/monitor/overview | getOverview() |

## 權限碼彙總

### 完整權限碼列表（13個）

```text
流程管理（6個）：
  liteflow:chain:query      - 查詢流程
  liteflow:chain:add        - 新增流程
  liteflow:chain:update     - 更新流程
  liteflow:chain:delete     - 刪除流程
  liteflow:chain:reload     - 重載流程

腳本管理（5個）：
  liteflow:script:query     - 查詢腳本
  liteflow:script:add       - 新增腳本
  liteflow:script:update    - 更新腳本
  liteflow:script:delete    - 刪除腳本

執行管理（2個）：
  liteflow:execution:execute - 執行流程
  liteflow:execution:query   - 查詢日誌

監控管理（1個）：
  liteflow:monitor:query     - 查詢監控
```

## Controller 權限碼映射

### LiteFlowChainController.java

```java
@PostMapping("/queryPage")
@SaCheckPermission("liteflow:chain:query")  // 1

@PostMapping("/add")
@SaCheckPermission("liteflow:chain:add")    // 2

@PostMapping("/update")
@SaCheckPermission("liteflow:chain:update") // 3

@GetMapping("/delete/{chainId}")
@SaCheckPermission("liteflow:chain:delete") // 4

@GetMapping("/detail/{chainId}")
@SaCheckPermission("liteflow:chain:query")  // 重用 query

@PostMapping("/reloadAll")
@SaCheckPermission("liteflow:chain:reload") // 5
```

### LiteFlowScriptController.java

```java
@PostMapping("/queryPage")
@SaCheckPermission("liteflow:script:query")  // 6

@PostMapping("/add")
@SaCheckPermission("liteflow:script:add")    // 7

@PostMapping("/update")
@SaCheckPermission("liteflow:script:update") // 8

@GetMapping("/delete/{scriptId}")
@SaCheckPermission("liteflow:script:delete") // 9

@GetMapping("/detail/{scriptId}")
@SaCheckPermission("liteflow:script:query")  // 重用 query
```

### LiteFlowExecutionController.java

```java
@PostMapping("/execute")
@SaCheckPermission("liteflow:execution:execute") // 10

@PostMapping("/queryLog")
@SaCheckPermission("liteflow:execution:query")   // 11

@GetMapping("/logDetail/{logId}")
@SaCheckPermission("liteflow:execution:query")   // 重用 query
```

### LiteFlowMonitorController.java

```java
@GetMapping("/overview")
@SaCheckPermission("liteflow:monitor:query")     // 13
```

## SQL 示例（可選）

如果需要通過 SQL 腳本批量創建權限碼，可以參考以下示例（**僅供參考，推薦使用界面配置**）：

```sql
-- 1. 創建 LiteFlow 主菜單（目錄）
INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, icon, visible_flag, disabled_flag, deleted_flag, create_time, create_user_id)
VALUES ('LiteFlow流程管理', 1, 0, 100, '/liteflow', 'workflow', TRUE, FALSE, FALSE, NOW(), 1);

-- 2. 創建子菜單和功能點（示例）
-- 流程管理菜單
INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, visible_flag, disabled_flag, deleted_flag, create_time, create_user_id)
VALUES ('流程管理', 2, (SELECT menu_id FROM t_menu WHERE menu_name = 'LiteFlow流程管理'), 1, 'chain', 'liteflow/chain/index', TRUE, FALSE, FALSE, NOW(), 1);

-- 功能點：查詢流程
INSERT INTO t_menu (menu_name, menu_type, parent_id, api_perms, visible_flag, disabled_flag, deleted_flag, create_time, create_user_id)
VALUES ('查詢流程', 3, (SELECT menu_id FROM t_menu WHERE menu_name = '流程管理' AND parent_id > 0), 'liteflow:chain:query', TRUE, FALSE, FALSE, NOW(), 1);

-- ... 其他功能點類似
```

## 角色授權

配置完權限碼後，需在 **系統管理 > 角色管理** 中為相應角色分配權限：

1. 選擇目標角色（如：系統管理員、LiteFlow 管理員）
2. 點擊「菜單權限」
3. 勾選 LiteFlow 相關菜單和功能點
4. 保存

## 驗證方法

### 1. 通過 Swagger UI 驗證

```bash
# 1. 啟動應用
./gradlew :sa-admin:bootRun

# 2. 訪問 Swagger UI
http://localhost:1024/doc.html

# 3. 使用有權限的賬號登錄
# 4. 測試各個 API 端點，驗證權限控制是否生效
```

### 2. 通過前端界面驗證

1. 使用有權限的賬號登錄前端
2. 檢查 LiteFlow 菜單是否顯示
3. 測試各個功能按鈕是否可見和可用

### 3. 通過日誌驗證

```bash
# 無權限時會看到 Sa-Token 拋出的異常
grep "NotPermissionException" logs/smart-admin.log
```

## 常見問題

### Q1: 配置權限碼後，API 仍然返回 403 錯誤？

**A**: 檢查以下幾點：
1. 確認菜單表中 `api_perms` 字段值是否正確
2. 確認角色已分配該權限
3. 確認用戶已分配該角色
4. 清除緩存並重新登錄

### Q2: 如何批量導入權限碼？

**A**: 推薦使用以下方法：
1. **方法1**：通過菜單管理界面逐個配置（推薦，最安全）
2. **方法2**：編寫 SQL 腳本批量插入（需謹慎測試）
3. **方法3**：使用系統的「導入導出」功能（如果支持）

### Q3: 權限碼命名有什麼注意事項？

**A**:
1. 使用小寫字母和冒號
2. 格式：`模塊:子模塊:操作`
3. 保持命名一致性（如：query 統一用於查詢類操作）
4. 避免使用中文或特殊字符

## 下一步

配置完權限碼後，建議：
1. 創建測試角色驗證權限控制
2. 編寫前端頁面並集成權限指令（v-privilege）
3. 進行 Swagger 文檔驗證（下一任務）

## 參考資料

- SmartAdmin 菜單管理文檔
- Sa-Token 權限註解文檔：https://sa-token.cc/doc.html#/use/jur-auth
- LiteFlow Controller 源碼：
  - [LiteFlowChainController.java](src/main/java/net/lab1024/sa/base/module/support/liteflow/controller/LiteFlowChainController.java)
  - [LiteFlowScriptController.java](src/main/java/net/lab1024/sa/base/module/support/liteflow/controller/LiteFlowScriptController.java)
  - [LiteFlowExecutionController.java](src/main/java/net/lab1024/sa/base/module/support/liteflow/controller/LiteFlowExecutionController.java)
  - [LiteFlowMonitorController.java](src/main/java/net/lab1024/sa/base/module/support/liteflow/controller/LiteFlowMonitorController.java)
