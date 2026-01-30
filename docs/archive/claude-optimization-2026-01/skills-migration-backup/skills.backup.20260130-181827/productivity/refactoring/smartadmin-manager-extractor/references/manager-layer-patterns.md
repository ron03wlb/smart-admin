# Manager Layer Patterns - SmartAdmin 架構參考

## SmartAdmin 分層架構

```
Controller → Service → Manager → Dao → Entity
              ↓         ↓
            (單表)    (多表 + @Transactional)
```

---

## Manager 層核心原則

### 1. @Transactional 專屬層

**規則**: `@Transactional` 註解 **僅允許** 出現在 Manager 層

```java
// ❌ 錯誤：Service 層使用 @Transactional
@Service
public class EmployeeService {
    @Transactional(rollbackFor = Throwable.class)  // ❌ 違規
    public void saveEmployee(EmployeeEntity employee) { }
}

// ✅ 正確：Manager 層使用 @Transactional
@Service
public class EmployeeManager {
    @Transactional(rollbackFor = Throwable.class)  // ✅ 正確
    public void saveEmployeeTransaction(EmployeeEntity employee) { }
}
```

### 2. Service 委託模式

```java
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeDao employeeDao;        // 單表操作
    private final EmployeeManager employeeManager;  // 多表/事務操作

    // 單表查詢：Service → Dao
    public EmployeeEntity getEmployee(Long id) {
        return employeeDao.selectById(id);  // ✅ 直接調用 Dao
    }

    // 多表操作：Service → Manager → Dao
    public void saveEmployee(EmployeeEntity employee) {
        employeeManager.saveEmployeeTransaction(employee);  // ✅ 委託給 Manager
    }
}
```

### 3. 構造函數注入

```java
@Service
@RequiredArgsConstructor  // Lombok 自動生成構造函數
public class EmployeeManager {
    private final EmployeeDao employeeDao;
    private final DepartmentDao departmentDao;
    // ✅ 使用 @RequiredArgsConstructor + private final
}

// ❌ 錯誤：字段注入
@Service
public class EmployeeManager {
    @Autowired  // ❌ 禁止使用 @Autowired
    private EmployeeDao employeeDao;
}
```

---

## Manager 層典型使用場景

### 場景 1: 多表操作

```java
@Service
@RequiredArgsConstructor
public class EmployeeManager {
    private final EmployeeDao employeeDao;
    private final DepartmentDao departmentDao;

    @Transactional(rollbackFor = Throwable.class)
    public void saveEmployeeTransaction(EmployeeEntity employee) {
        // 1. 插入員工表
        employeeDao.insert(employee);

        // 2. 更新部門表的員工計數
        departmentDao.updateEmployeeCount(employee.getDeptId(), 1);
    }
}
```

### 場景 2: 複雜業務邏輯

```java
@Service
@RequiredArgsConstructor
public class OrderManager {
    private final OrderDao orderDao;
    private final InventoryDao inventoryDao;
    private final PaymentDao paymentDao;

    @Transactional(rollbackFor = Throwable.class)
    public void createOrderTransaction(Order order) {
        // 1. 創建訂單
        orderDao.insert(order);

        // 2. 扣減庫存
        inventoryDao.decreaseStock(order.getProductId(), order.getQuantity());

        // 3. 創建支付記錄
        Payment payment = new Payment(order);
        paymentDao.insert(payment);
    }
}
```

### 場景 3: @Cacheable + @Transactional

```java
@Service
@RequiredArgsConstructor
public class EmployeeManager {
    private final EmployeeDao employeeDao;

    // ✅ @Cacheable 優先級高於 @Transactional
    @Cacheable(value = "employee", key = "#employeeId")
    @Transactional(rollbackFor = Throwable.class, readOnly = true)
    public EmployeeEntity getEmployeeTransaction(Long employeeId) {
        return employeeDao.selectById(employeeId);
    }
}
```

---

## ArchUnit 強制規則

### 規則 1: @Transactional 僅在 Manager

```java
// ArchitectureTest.java
@ArchTest
static final ArchRule transactionalMustBeInManagerLayer =
    methods().that()
        .areAnnotatedWith(Transactional.class)
    .should().beDeclaredInClassesThat()
        .haveSimpleNameEndingWith("Manager")
    .because("@Transactional must be in Manager layer");
```

### 規則 2: @Transactional 必須使用 rollbackFor

```java
@ArchTest
static final ArchRule transactionalMustUseRollbackForThrowable =
    methods().that()
        .areAnnotatedWith(Transactional.class)
    .should().beAnnotatedWith(
        ArchConditions.haveAnnotationParameter(
            Transactional.class, "rollbackFor", Throwable.class
        )
    )
    .because("@Transactional must use rollbackFor = Throwable.class");
```

### 規則 3: Service 不得直接調用多個 Dao

```java
@ArchTest
static final ArchRule serviceMustNotCallMultipleDaos =
    classes().that()
        .haveSimpleNameEndingWith("Service")
        .and().doNotHaveSimpleName("BaseService")
    .should().dependOnClassesThat()
        .haveSimpleNameEndingWith("Manager")
        .forSubclass()  // 當需要多表操作時
    .because("Service must delegate to Manager for multi-table operations");
```

---

## 命名慣例

| 類型 | 命名規則 | 範例 |
|------|---------|------|
| Manager 類 | `{Entity}Manager` | `EmployeeManager` |
| 事務方法 | `{operation}Transaction()` | `saveEmployeeTransaction()` |
| Service 委託方法 | 與原方法同名 | `saveEmployee()` |

---

## 性能考量

### 事務傳播

```java
@Transactional(propagation = Propagation.REQUIRED)  // 默認
public void outerTransaction() {
    innerTransaction();  // 加入外部事務
}

@Transactional(propagation = Propagation.REQUIRES_NEW)
public void independentTransaction() {
    // 獨立事務（即使外部事務失敗也會提交）
}
```

### 只讀事務優化

```java
@Transactional(rollbackFor = Throwable.class, readOnly = true)
public EmployeeEntity getEmployeeTransaction(Long id) {
    return employeeDao.selectById(id);  // 只讀優化
}
```

---

## 參考資料

- [SmartAdmin 架構規則](../../../../.agent/rules/foundation/10-architecture-rules.md)
- [Manager 層規範](../../../../.agent/rules/foundation/09-manager-layer.md)
- [事務管理模式](../../../../.claude/shared/knowledge/smartadmin-patterns.md#transaction-management)

---

**維護者**: SmartAdmin Skills Team
**最後更新**: 2026-01-29
**適用版本**: SmartAdmin v3.0.0+
