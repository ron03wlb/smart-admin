# Naming Convention Checker - Examples

## 範例 1: 命名檢查報告

```markdown
# Naming Convention Check Report

## Summary
- Files Checked: 24
- Violations: 3

## Violations

### 1. Entity 命名錯誤
**File**: Employee.java
**Problem**: Entity class should end with "Entity"
**Fix**: Rename to `EmployeeEntity`

### 2. Boolean 欄位使用 is 前綴
**File**: EmployeeEntity.java:15
**Problem**: Boolean field uses "is" prefix
**Before**: `private Boolean isDeleted;`
**After**: `private Boolean deleted;`

### 3. 表名使用複數
**File**: EmployeeEntity.java
**Problem**: Table name should be singular
**Before**: `@TableName("t_employees")`
**After**: `@TableName("t_employee")`
```

## 範例 2: 修復 Boolean 欄位

**Before:**
```java
@Data
@TableName("t_employee")
public class EmployeeEntity {
    private Long employeeId;
    private String name;
    private Boolean isDeleted;   // ❌
    private Boolean isActive;    // ❌
}
```

**After:**
```java
@Data
@TableName("t_employee")
public class EmployeeEntity {
    private Long employeeId;
    private String name;
    private Boolean deleted;     // ✅
    private Boolean active;      // ✅
}
```

## 範例 3: VO/DTO 命名修正

**Before:**
```java
public class EmployeeDTO { }      // ❌ 使用 DTO
public class EmployeeResponse { } // ❌ 使用 Response
```

**After:**
```java
public class EmployeeVO { }       // ✅ 使用 VO
```
