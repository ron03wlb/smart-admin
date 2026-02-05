# Naming Convention Checker - Quick Reference

## 類別命名

| 類型 | 正確 ✅ | 錯誤 ❌ |
|------|---------|---------|
| Entity | `EmployeeEntity` | `Employee`, `EmployeePO` |
| Dao | `EmployeeDao` | `EmployeeMapper`, `EmployeeRepository` |
| Service | `EmployeeService` | `EmployeeSvc` |
| Manager | `EmployeeManager` | `EmployeeMgr` |
| Controller | `EmployeeController` | `EmployeeCtrl` |
| VO | `EmployeeVO` | `EmployeeDTO`, `EmployeeResponse` |

## 表名規則

```sql
-- ✅ 正確: 單數形式
CREATE TABLE t_employee (...);
CREATE TABLE t_department (...);
CREATE TABLE t_order (...);

-- ❌ 錯誤: 複數形式
CREATE TABLE t_employees (...);
CREATE TABLE t_orders (...);
```

## Boolean 欄位

```java
// ✅ 正確
private Boolean deleted;
private Boolean enabled;

// ❌ 錯誤
private Boolean isDeleted;
private Boolean isEnabled;
```

## 檢查範例

```java
// 全部正確的類別定義
@TableName("t_employee")
public class EmployeeEntity {
    private Long employeeId;
    private String firstName;
    private Boolean deleted;  // ✅ 無 is 前綴
}

public class EmployeeAddForm { }
public class EmployeeUpdateForm { }
public class EmployeeQueryForm extends PageParam { }
public class EmployeeVO { }
public interface EmployeeDao extends BaseMapper<EmployeeEntity> { }
public class EmployeeService { }
public class EmployeeManager { }
public class EmployeeController { }
```

## 相關規則

- [F01-naming-conventions.md](../../../../rules/foundation/F01-naming-conventions.md)
