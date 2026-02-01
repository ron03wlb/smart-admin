# Example 1: Entity Test Fixture

## Scenario
生成 Employee 實體的測試數據構建器

## Input
```bash
User: "Generate test fixture for Employee entity"
```

## Generated Output
```java
public class EmployeeFixture {
    public static Employee.EmployeeBuilder defaultEmployee() {
        return Employee.builder()
            .employeeId(1001L)
            .employeeName("張三")
            .phone("13800138000")
            .email("zhangsan@example.com")
            .departmentId(10L)
            .deleted(false);
    }

    public static Employee createTestEmployee() {
        return defaultEmployee().build();
    }

    public static Employee createEmployeeWithDepartment(Long deptId) {
        return defaultEmployee()
            .departmentId(deptId)
            .build();
    }
}
```

## Expected Result
- 簡化測試數據準備
- 支持自定義字段覆蓋
- 提高測試可讀性
