# Example 1: @Transactional Validation

## Scenario
檢查所有 @Transactional 使用是否符合 SmartAdmin 規範

## Input
```bash
User: "Validate @Transactional usage across the project"
```

## Generated Checks
```java
// Check 1: @Transactional only in Manager layer
✅ PASS: EmployeeManager.saveEmployeeTransaction()
❌ FAIL: EmployeeService.saveEmployee() (should be in Manager)

// Check 2: rollbackFor = Throwable.class
✅ PASS: EmployeeManager.saveEmployeeTransaction(rollbackFor = Throwable.class)
❌ FAIL: DepartmentManager.saveDepartment(no rollbackFor specified)

// Check 3: No @Transactional on private methods
✅ PASS: All @Transactional methods are public/protected
```

## Expected Result
```
Spring Pattern Check Report:
- Total @Transactional methods: 45
- Violations found: 3
  1. EmployeeService.saveEmployee() (wrong layer)
  2. DepartmentManager.saveDepartment() (missing rollbackFor)
  3. OrderService.createOrder() (wrong layer)

Action Required: Fix 3 violations before merge
```
