---
trigger: on_demand
description: Manager 层架构规范 - 事务边界与缓存管理
tags: [architecture, manager, transaction, cache, smart-admin]
positioning: current-standard
ai_role: code_reviewer_and_generator
auto_apply: true
archunit_test: ArchitectureTest#managerLayerRules
last_updated: 2025-01-17
---

# Manager 层架构规范

> **核心原则**: @Transactional 和缓存注解（@Cacheable/@CacheEvict/@CachePut）**只在 Manager 层使用**

---

## 🤖 AI 指令區塊

### 何時應用此規則
- ✅ 用戶要求生成跨多個 Mapper/DAO 的事務操作
- ✅ 用戶要求添加緩存功能
- ✅ Service 層方法變得複雜（調用多個 Mapper）
- ✅ Code Review 時發現 Service 層使用 @Transactional/@Cacheable

### 強制執行檢查清單
- [ ] `@Transactional` 只出現在 Manager 類中
- [ ] `@Cacheable/@CacheEvict/@CachePut` 只出現在 Manager 類中
- [ ] Manager 類以 `Manager` 結尾
- [ ] Manager 類使用構造函數注入（`@RequiredArgsConstructor`）
- [ ] **Manager 層禁止調用 Service 層（嚴格執行）**
- [ ] **Manager 層禁止調用其他業務 Manager（避免事務嵌套）**

### AI 決策樹
```
檢測到代碼生成請求 → 判斷是否需要 Manager 層
  ├─ 需要跨多個 Mapper 事務? → 是 → 創建 Manager
  ├─ 需要緩存? → 是 → 創建 CacheManager
  └─ 單一 Mapper 操作? → 否 → 使用 Service 層即可
```

---

## 錯誤模式檢測與自動修正

### 模式 1: Service 層使用 @Transactional
```java
// ❌ Service 層使用事務
@Service
public class EmployeeService {
    @Transactional(rollbackFor = Exception.class)  // ❌ 禁止
    public void saveEmployee(Employee e) { }
}

// ✅ 創建 EmployeeManager
@Service
@RequiredArgsConstructor
public class EmployeeManager {
    @Transactional(rollbackFor = Throwable.class)  // ✅ Manager 層
    public void saveEmployee(Employee e, List<Long> roleIds) { }
}
```

### 模式 2: Service 層使用緩存注解
```java
// ❌ Service 層使用緩存
@Service
public class DepartmentService {
    @Cacheable("deptList")  // ❌ 禁止
    public List<Department> listAll() { }
}

// ✅ 創建 DepartmentCacheManager
@Service
public class DepartmentCacheManager {
    @Cacheable(AdminCacheConst.Department.DEPARTMENT_LIST_CACHE)
    public List<DepartmentVO> getDepartmentList() { }
}
```

---

## 調用約束（嚴格執行）

> 詳細分層架構參考 [10-architecture-rules.md](./10-architecture-rules.md)

| 約束                       | 說明                         |
| -------------------------- | ---------------------------- |
| **✗ Manager → Service**    | 禁止向上調用                 |
| **✗ ManagerA → ManagerB**  | 禁止橫向調用（避免事務嵌套） |
| **✓ Manager → DAO/Mapper** | 允許調用 DAO 層              |
| **✓ Service → Manager**    | 允許 Service 調用 Manager    |

---

## Manager 層三大職責

### 1. 事務管理（跨多個 Mapper 操作）
```java
@Service
@RequiredArgsConstructor
public class EmployeeManager {
    private final EmployeeMapper employeeMapper;
    private final RoleEmployeeMapper roleEmployeeMapper;

    @Transactional(rollbackFor = Throwable.class)
    public void updateEmployee(Employee employee, List<Long> roleIds) {
        employeeMapper.updateById(employee);
        roleEmployeeMapper.deleteByEmployeeId(employee.getId());
        if (CollectionUtils.isNotEmpty(roleIds)) {
            roleEmployeeMapper.batchInsert(roleIds, employee.getId());
        }
    }
}
```

### 2. 緩存管理
```java
@Service
public class DepartmentCacheManager {
    @Cacheable(AdminCacheConst.Department.DEPARTMENT_LIST_CACHE)
    public List<DepartmentVO> getDepartmentList() { }

    @CacheEvict(value = AdminCacheConst.Department.DEPARTMENT_LIST_CACHE, allEntries = true)
    public void clearCache() { }
}
```

### 3. 複雜業務編排（只調用 DAO/Mapper）
```java
@Service
@RequiredArgsConstructor
public class OrderManager {
    private final OrderMapper orderMapper;
    private final InventoryMapper inventoryMapper;  // ✅ 只調用 Mapper
    // ❌ private final InventoryService inventoryService;  // 禁止

    @Transactional(rollbackFor = Throwable.class)
    public void createOrder(Order order, Integer quantity) {
        inventoryMapper.deductStock(order.getProductId(), quantity);
        orderMapper.insert(order);
    }
}
```

---

## 事務注解規範

```java
// ✅ 正確：rollbackFor = Throwable.class
@Transactional(rollbackFor = Throwable.class)
public void saveEmployee(Employee employee) { }

// ❌ 錯誤：rollbackFor = Exception.class（無法捕獲 Error）
@Transactional(rollbackFor = Exception.class)

// ❌ 錯誤：不指定 rollbackFor
@Transactional
```

---

## 命名規範

- **事務管理**: `{Entity}Manager`（如 `EmployeeManager`）
- **緩存管理**: `{Entity}CacheManager`（如 `DepartmentCacheManager`）
- **混合功能**: `{Domain}Manager`（如 `LoginManager`）

---

## ArchUnit 測試規則

```java
@ArchTest
static final ArchRule transactionalOnlyInManager = methods()
    .that().areAnnotatedWith(Transactional.class)
    .should().beDeclaredInClassesThat().haveSimpleNameEndingWith("Manager")
    .because("@Transactional 只能在 Manager 層使用");

@ArchTest
static final ArchRule cacheableOnlyInManager = methods()
    .that().areAnnotatedWith(Cacheable.class)
    .should().beDeclaredInClassesThat().haveSimpleNameEndingWith("Manager")
    .because("緩存注解只能在 Manager 層使用");
```

---

**最後更新**: 2025-01-17
**強制級別**: 🚫 ArchUnit 阻止 PR 合併
