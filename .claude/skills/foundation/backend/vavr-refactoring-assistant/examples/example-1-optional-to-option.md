# 範例 1: Optional → Option 重構

**技能**: vavr-refactoring-assistant
**難度**: ⭐⭐☆☆☆（中等）
**預估時間**: 10-15 分鐘

---

## 場景描述

將 Service 層的 `java.util.Optional` 重構為 `io.vavr.control.Option`，符合 SmartAdmin 架構標準（serviceUsesVavrOption ArchUnit 測試）。

---

## ❌ 違規代碼（使用 java.util.Optional）

```java
package net.lab1024.sa.admin.module.business.employee.service;

import java.util.Optional; // ❌ Service 層禁止使用 java.util.Optional

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeDao employeeDao;

    public EmployeeVO getById(Long employeeId) {
        // ❌ 使用 java.util.Optional
        Optional<EmployeeEntity> optional = Optional.ofNullable(
            employeeDao.selectById(employeeId)
        );

        // ❌ 複雜的 Optional 鏈式調用
        return optional
            .map(entity -> SmartBeanUtil.copy(entity, EmployeeVO.class))
            .orElseThrow(() -> new BusinessException(EmployeeErrorCode.DATA_NOT_EXIST));
    }
}
```

**ArchUnit 測試失敗**:
```
Rule 'Service classes should use Vavr Option instead of java.util.Optional' was violated (1 times):
Method <EmployeeService.getById(Long)> uses java.util.Optional in (EmployeeService.java:15)
```

---

## ✅ 正確代碼（使用 Vavr Option）

```java
package net.lab1024.sa.admin.module.business.employee.service;

import io.vavr.control.Option; // ✅ Service 層使用 Vavr Option

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeDao employeeDao;

    public EmployeeVO getById(Long employeeId) {
        // ✅ 使用 Vavr Option
        return Option.of(employeeDao.selectById(employeeId))
            .map(entity -> SmartBeanUtil.copy(entity, EmployeeVO.class))
            .getOrElseThrow(() -> new BusinessException(EmployeeErrorCode.DATA_NOT_EXIST));
    }
}
```

---

## 關鍵差異對照

| 操作 | java.util.Optional | io.vavr.control.Option |
|------|-------------------|------------------------|
| 創建 | `Optional.ofNullable(value)` | `Option.of(value)` |
| 空值 | `Optional.empty()` | `Option.none()` |
| 映射 | `.map(func)` | `.map(func)` （相同） |
| 取值 | `.orElseThrow()` | `.getOrElseThrow()` |
| 預設值 | `.orElse(default)` | `.getOrElse(default)` |

---

## 驗證測試通過

```bash
./gradlew :sa-admin:test --tests ArchitectureTest.serviceUsesVavrOption
```

**輸出**:
```
ArchitectureTest > serviceUsesVavrOption() PASSED ✅
BUILD SUCCESSFUL
```

---

## 相關規則

- **[Architecture Rules - Service Layer](../../../../../.agent/rules/foundation/10-architecture-rules.md#serviceusesvavroption)**
  - Service 層必須使用 `io.vavr.control.Option`（禁止 `java.util.Optional`）
  - ArchUnit 測試驗證: `serviceUsesVavrOption()`

- **[Vavr 官方文檔](https://docs.vavr.io/#_option)**
  - Option vs Optional 詳細比較
