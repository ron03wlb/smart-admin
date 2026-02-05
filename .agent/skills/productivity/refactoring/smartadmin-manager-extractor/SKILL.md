---
name: smartadmin-manager-extractor
description: "自動提取 @Transactional 到 Manager 層"
priority: P2
category: refactoring
---

# SmartAdmin Manager Extractor

自動將 Service 層的 @Transactional 方法提取到 Manager 層，修復 ArchUnit 架構違規。

## Usage

```
User: "Extract @Transactional methods from EmployeeService to Manager"
AI: [Create Manager class, move methods, update Service to delegate]
```

## When to Use

- ArchUnit 測試失敗: "@Transactional not in Manager layer"
- Service 層包含 @Transactional 註解
- 需要重構符合 SmartAdmin 架構規範
- 批量修復多個違規

## Generated Output

- 新建 Manager 類 (如 EmployeeManager)
- 移動 @Transactional 方法
- 更新 Service 層委派調用
- ArchUnit 驗證確認

## Workflow

1. **識別違規方法**
   - 掃描 Service 層的 @Transactional
   - 列出需要提取的方法

2. **創建 Manager 類**
   - 命名: {Entity}Manager
   - 添加 @Service 和 @RequiredArgsConstructor

3. **移動方法**
   - 將 @Transactional 方法移至 Manager
   - 確保 `@Transactional(rollbackFor = Throwable.class)`

4. **更新 Service**
   - 注入 Manager
   - 委派調用 Manager 方法

5. **三重驗證**
   - 編譯通過
   - ArchUnit 測試通過
   - 業務測試通過

## Related Rules

- [F03-manager-layer.md](../../../rules/foundation/F03-manager-layer.md)
- [F04-architecture-rules.md](../../../rules/foundation/F04-architecture-rules.md)

## Safety

- 執行前自動創建 Git stash
- 失敗時自動回滾
- 支援增量重構

## Example Session

**User:** ArchUnit 測試失敗，EmployeeService 有 @Transactional

**AI Agent Actions:**
1. 識別 `EmployeeService.createEmployee()` 有 @Transactional
2. 創建 `EmployeeManager` 類
3. 移動 `createEmployee()` 方法到 Manager
4. 更新 Service 委派: `employeeManager.createEmployee(...)`
5. 運行 `./gradlew :smartadmin-app:test --tests ArchitectureTest`
6. 確認測試通過
