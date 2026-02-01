---
name: smartadmin-manager-extractor
description: [P2 - Productivity] Automatically extract @Transactional methods to Manager layer, fix ArchUnit violations (83% time saving 30min → 5min). Use when ArchitectureTest transactionalMustUseRollbackForThrowable fails or when refactoring Service to Manager.
---

# SmartAdmin Manager Extractor Skill

**Version**: 1.0.0
**Priority**: P2 (Productivity/Refactoring)
**Category**: Refactoring
**Status**: Stable

---

## 概述

自動將 Service 層的 `@Transactional` 方法提取到 Manager 層，修復 ArchUnit 違規並確保架構一致性。

**時間節省**: 30 分鐘 → 5 分鐘（**83% 改善**）

---

## 核心功能

| 功能 | 描述 |
|------|------|
| **AST 解析** | 使用 JavaParser 分析 Service 類結構 |
| **智能提取** | 識別 @Transactional 方法並處理依賴 |
| **Manager 生成** | 從模板自動生成 Manager 類 |
| **三重驗證** | 編譯 + ArchUnit + 測試 |
| **Git 安全** | 失敗自動回滾（stash pop） |

---

## Trigger Keywords

This skill is automatically activated when the user's request contains:

**Primary Keywords** (High confidence):
- "extract to Manager" - Extract methods to Manager layer
- "transactionalMustUseRollbackForThrowable fails" - ArchUnit test violation
- "@Transactional must be in Manager" - Architecture rule enforcement
- "refactor transaction" - Refactor transactional methods

**Secondary Keywords** (Medium confidence):
- "move method to Manager" - Context: layer refactoring
- "ArchUnit violation" - Context: transactional placement violation
- "Manager layer" - Context: refactoring to Manager

**Phrase Patterns**:
- "Extract [method] to Manager" - Example: "Extract saveEmployee to Manager layer"
- "Fix [ArchUnit test] violation" - Example: "Fix transactionalMustUseRollbackForThrowable violation"
- "Refactor [Service] to use Manager" - Example: "Refactor EmployeeService to use Manager"

**Example User Requests**:
```
User: "Extract @Transactional methods from EmployeeService to Manager layer"
User: "Fix transactionalMustUseRollbackForThrowable ArchUnit violation"
User: "Refactor EmployeeService transactions to Manager"
User: "Move saveEmployee method to Manager layer"
```

**Note**: This skill can also be manually invoked via `/smartadmin-manager-extractor` or `/manager-extract` command. Supports `--dry-run` mode.

### 命令

```bash
# 提取所有 @Transactional 方法
/manager-extract EmployeeService

# 提取特定方法
/manager-extract EmployeeService --method saveEmployee

# 預覽（不實際執行）
/manager-extract EmployeeService --dry-run
```

---

## 使用範例

### 場景：ArchUnit 測試失敗

**問題**:
```
❌ EmployeeService.saveEmployee() violates rule:
   @Transactional must be in Manager layer
```

**解決**:
```
/manager-extract EmployeeService
```

**輸出**:
```
✅ Created EmployeeManager.saveEmployeeTransaction()
✅ Updated EmployeeService to call Manager
✅ ArchUnit: PASSED
✅ All tests: PASSED

Time saved: 25 minutes (30 min → 5 min)
```

---

## 提取前後對比

### 提取前（違反規則）

```java
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeDao employeeDao;
    private final DepartmentDao departmentDao;

    @Transactional(rollbackFor = Throwable.class)  // ❌ 違規
    public void saveEmployee(EmployeeEntity employee) {
        employeeDao.insert(employee);
        departmentDao.updateEmployeeCount(employee.getDeptId());
    }
}
```

### 提取後（符合規則）

```java
// EmployeeService.java
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeManager employeeManager;  // ✅ 注入 Manager

    public void saveEmployee(EmployeeEntity employee) {
        employeeManager.saveEmployeeTransaction(employee);  // ✅ 委託給 Manager
    }
}

// EmployeeManager.java（自動生成）
@Service
@RequiredArgsConstructor
public class EmployeeManager {
    private final EmployeeDao employeeDao;
    private final DepartmentDao departmentDao;

    @Transactional(rollbackFor = Throwable.class)  // ✅ Manager 層正確
    public void saveEmployeeTransaction(EmployeeEntity employee) {
        employeeDao.insert(employee);
        departmentDao.updateEmployeeCount(employee.getDeptId());
    }
}
```

---

## 處理邊界條件

### 1. 方法名衝突

```java
// Manager 已有 saveEmployee() 方法
// ✅ 自動重命名為 saveEmployeeTransaction()
```

### 2. 互相依賴的方法

```java
// Service 方法互相調用
@Transactional
public void saveEmployee() { updateDepartment(); }

@Transactional
private void updateDepartment() { }

// ✅ 同時提取兩個方法
```

### 3. 字段依賴

```java
// Service 方法使用字段
private final SomeService someService;

@Transactional
public void method() { someService.doSomething(); }

// ✅ 自動更新 Manager 構造函數注入
```

---

## 驗證流程

```
1. Git Stash（備份）
2. 提取方法
3. 編譯驗證 → 失敗 → Rollback
4. ArchUnit 驗證 → 失敗 → Rollback
5. 測試驗證 → 失敗 → Rollback
6. 全部通過 → Git Commit
```

---

## 依賴配置

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.github.javaparser:javaparser-core:3.26.0")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
}
```

---

## 故障排除

### 問題：編譯失敗

**原因**: 缺少依賴注入
**解決**: 檢查 Manager 構造函數是否包含所有依賴

### 問題：ArchUnit 仍失敗

**原因**: Service 仍直接調用 Dao
**解決**: 確保 Service 僅調用 Manager

### 問題：測試失敗

**原因**: 測試未更新 Mock 對象
**解決**: 更新測試以 Mock Manager 而非 Dao

---

## 參考資料

- [提取範例](examples/extraction-example.md)
- [Manager 層模式](references/manager-layer-patterns.md)
- [SmartAdmin 架構規則](../../../../.agent/rules/foundation/10-architecture-rules.md)

---

**維護者**: SmartAdmin Skills Team
**最後更新**: 2026-01-29
