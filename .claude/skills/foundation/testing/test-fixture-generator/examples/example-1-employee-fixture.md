# 範例 1: 員工測試數據 Fixture Builder

**技能**: test-fixture-generator
**難度**: ⭐⭐☆☆☆（中等）
**預估時間**: 10-15 分鐘

---

## 場景描述

為複雜的員工實體創建測試數據構建器（Fixture Builder），提供可重用、可定制的測試數據生成方式。

---

## 問題（手動創建測試數據）

### ❌ 重複的測試數據創建

```java
@Test
void testAddEmployee() {
    // ❌ 每個測試都要重複創建
    EmployeeEntity employee = new EmployeeEntity();
    employee.setEmployeeName("張三");
    employee.setDepartmentId(1L);
    employee.setPosition("工程師");
    employee.setJoinDate(LocalDate.of(2024, 1, 1));
    employee.setStatus(1);
    employee.setSalary(new BigDecimal("80000"));
    employee.setEmail("zhangsan@example.com");
    // ... 10+ 個欄位

    employeeDao.insert(employee);
}

@Test
void testQueryEmployee() {
    // ❌ 又要重複創建相同結構
    EmployeeEntity employee = new EmployeeEntity();
    employee.setEmployeeName("李四");
    // ... 又是一堆重複代碼
}
```

---

## 解決方案: Fixture Builder

### EmployeeFixture.java

```java
public class EmployeeFixture {

    public static EmployeeBuilder aValidEmployee() {
        return new EmployeeBuilder()
            .withEmployeeName("測試員工")
            .withDepartmentId(1L)
            .withPosition("工程師")
            .withJoinDate(LocalDate.of(2024, 1, 1))
            .withStatus(1)
            .withSalary(new BigDecimal("80000"))
            .withEmail("test@example.com")
            .withPhoneNumber("13800138000");
    }

    public static class EmployeeBuilder {
        private Long employeeId;
        private String employeeName = "默認員工";
        private Long departmentId = 1L;
        private String position = "員工";
        private LocalDate joinDate = LocalDate.now();
        private Integer status = 1;
        private BigDecimal salary = new BigDecimal("50000");
        private String email = "default@example.com";
        private String phoneNumber = "13800138000";

        public EmployeeBuilder withEmployeeId(Long employeeId) {
            this.employeeId = employeeId;
            return this;
        }

        public EmployeeBuilder withEmployeeName(String employeeName) {
            this.employeeName = employeeName;
            return this;
        }

        public EmployeeBuilder withDepartmentId(Long departmentId) {
            this.departmentId = departmentId;
            return this;
        }

        public EmployeeBuilder withPosition(String position) {
            this.position = position;
            return this;
        }

        public EmployeeBuilder withJoinDate(LocalDate joinDate) {
            this.joinDate = joinDate;
            return this;
        }

        public EmployeeBuilder withStatus(Integer status) {
            this.status = status;
            return this;
        }

        public EmployeeBuilder withSalary(BigDecimal salary) {
            this.salary = salary;
            return this;
        }

        public EmployeeBuilder withEmail(String email) {
            this.email = email;
            return this;
        }

        public EmployeeBuilder withPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public EmployeeEntity build() {
            EmployeeEntity entity = new EmployeeEntity();
            entity.setEmployeeId(employeeId);
            entity.setEmployeeName(employeeName);
            entity.setDepartmentId(departmentId);
            entity.setPosition(position);
            entity.setJoinDate(joinDate);
            entity.setStatus(status);
            entity.setSalary(salary);
            entity.setEmail(email);
            entity.setPhoneNumber(phoneNumber);
            return entity;
        }
    }
}
```

---

## 使用範例

### ✅ 簡潔的測試代碼

```java
@Test
void testAddEmployee() {
    // ✅ 一行創建有效員工
    EmployeeEntity employee = EmployeeFixture.aValidEmployee().build();
    employeeDao.insert(employee);

    assertThat(employee.getEmployeeId()).isNotNull();
}

@Test
void testHighSalaryEmployee() {
    // ✅ 定制特定欄位
    EmployeeEntity highPaid = EmployeeFixture.aValidEmployee()
        .withSalary(new BigDecimal("200000"))
        .withPosition("高級工程師")
        .build();

    employeeDao.insert(highPaid);

    assertThat(highPaid.getSalary()).isGreaterThan(new BigDecimal("150000"));
}

@Test
void testMultipleEmployees() {
    // ✅ 批量創建
    List<EmployeeEntity> employees = Stream.of("張三", "李四", "王五")
        .map(name -> EmployeeFixture.aValidEmployee()
            .withEmployeeName(name)
            .build())
        .toList();

    employees.forEach(employeeDao::insert);

    assertThat(employees).hasSize(3);
}
```

---

## 優勢

✅ **可重用**: 所有測試共享相同 Fixture
✅ **可定制**: Fluent API 隨意覆蓋欄位
✅ **可讀性**: 測試意圖清晰
✅ **維護性**: 欄位變更只需修改 Fixture

---

## 進階: 預設場景

```java
public class EmployeeFixture {

    public static EmployeeBuilder aValidEmployee() { /* ... */ }

    public static EmployeeBuilder aSeniorEngineer() {
        return aValidEmployee()
            .withPosition("高級工程師")
            .withSalary(new BigDecimal("150000"))
            .withJoinDate(LocalDate.now().minusYears(3));
    }

    public static EmployeeBuilder aResignedEmployee() {
        return aValidEmployee()
            .withStatus(2) // 離職
            .withJoinDate(LocalDate.now().minusYears(2));
    }
}
```

**使用**:
```java
EmployeeEntity senior = EmployeeFixture.aSeniorEngineer().build();
EmployeeEntity resigned = EmployeeFixture.aResignedEmployee().build();
```

---

## 參考資料

- [Test Data Builders Pattern](https://www.natpryce.com/articles/000714.html)
- [Fixture Pattern in Testing](https://martinfowler.com/bliki/ObjectMother.html)
