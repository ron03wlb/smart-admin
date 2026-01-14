---
trigger: on_demand
description: Manager 层架构规范 - 事务边界与缓存管理
tags: [architecture, manager, transaction, cache, smart-admin]
positioning: current-standard
ai_role: code_reviewer_and_generator
auto_apply: true
ask_before_fix: false
archunit_test: ArchitectureTest#managerLayerRules
last_updated: 2025-01-13
---

# Manager 层架构规范

> **核心原则**: @Transactional 和缓存注解（@Cacheable/@CacheEvict/@CachePut）**只在 Manager 层使用**

---

## 🤖 AI 指令區塊

### 何時應用此規則
- ✅ 用戶要求生成跨多個 Mapper/DAO 的事務操作
- ✅ 用戶要求添加緩存功能
- ✅ Service 層方法變得複雜（調用多個 Mapper）
- ✅ Code Review 時發現 Service 層使用 @Transactional
- ✅ Code Review 時發現 Service 層使用 @Cacheable

### 強制執行檢查清單
- [ ] `@Transactional` 只出現在 Manager 類中
- [ ] `@Cacheable/@CacheEvict/@CachePut` 只出現在 Manager 類中
- [ ] Manager 類以 `Manager` 結尾（如 `EmployeeManager`、`DepartmentCacheManager`）
- [ ] Manager 類使用 `@Service` 注解
- [ ] Manager 類使用構造函數注入（`@RequiredArgsConstructor`）
- [ ] Service 層不包含事務或緩存注解

### AI 決策樹
```
檢測到代碼生成請求 → 判斷是否需要 Manager 層
  ├─ 需要跨多個 Mapper 事務? → 是 → 創建 Manager（事務管理）
  ├─ 需要緩存? → 是 → 創建 CacheManager（緩存管理）
  ├─ 複雜業務編排（調用多個 Service/Mapper）? → 是 → 創建 Manager
  └─ 單一 Mapper 操作? → 否 → 使用 Service 層即可
```

### 錯誤模式檢測與自動修正

#### 模式 1: Service 層使用 @Transactional（嚴重違規）
```java
// ❌ Service 層使用事務
@Service
public class EmployeeService {
    @Transactional(rollbackFor = Exception.class)  // ❌ 禁止
    public void saveEmployee(Employee e, List<Long> roleIds) { }
}

// ✅ 創建 EmployeeManager
@Service
@RequiredArgsConstructor
public class EmployeeManager {
    private final EmployeeMapper employeeMapper;
    private final RoleEmployeeMapper roleEmployeeMapper;

    @Transactional(rollbackFor = Throwable.class)  // ✅ Manager 層
    public void saveEmployee(Employee e, List<Long> roleIds) {
        employeeMapper.insert(e);
        if (CollectionUtils.isNotEmpty(roleIds)) {
            roleEmployeeMapper.batchInsert(roleIds, e.getId());
        }
    }
}
```

#### 模式 2: Service 層使用緩存注解（嚴重違規）
```java
// ❌ Service 層使用緩存
@Service
public class DepartmentService {
    @Cacheable("deptList")  // ❌ 禁止
    public List<Department> listAll() { }
}

// ✅ 創建 DepartmentCacheManager
@Service
@RequiredArgsConstructor
public class DepartmentCacheManager {
    private final DepartmentMapper mapper;

    @Cacheable(AdminCacheConst.Department.DEPARTMENT_LIST_CACHE)
    public List<DepartmentVO> getDepartmentList() {
        return mapper.listAll();
    }

    @CacheEvict(value = AdminCacheConst.Department.DEPARTMENT_LIST_CACHE, allEntries = true)
    public void clearCache() { }
}
```

---

## Manager 層架構定位

### 分層架構
```
Controller (控制層)
    ↓ 調用
Service (服務層) - 單一業務邏輯，返回 Option/Try
    ↓ 調用
Manager (管理層) - 事務邊界 + 緩存管理 + 業務編排
    ↓ 調用
Mapper (持久層) - MyBatis Plus 數據訪問
```

### Manager 層三大職責

#### 1. 事務管理（跨多個 Mapper 操作）
```java
@Service
@RequiredArgsConstructor
public class EmployeeManager {
    private final EmployeeMapper employeeMapper;
    private final RoleEmployeeMapper roleEmployeeMapper;

    @Transactional(rollbackFor = Throwable.class)  // 事務邊界
    public void updateEmployee(Employee employee, List<Long> roleIds) {
        employeeMapper.updateById(employee);

        // 跨表操作必須在同一事務
        roleEmployeeMapper.deleteByEmployeeId(employee.getId());
        if (CollectionUtils.isNotEmpty(roleIds)) {
            roleEmployeeMapper.batchInsert(roleIds, employee.getId());
        }
    }
}
```

#### 2. 緩存管理
```java
@Service
@RequiredArgsConstructor
public class LoginManager {
    private final EmployeeService employeeService;

    @Cacheable(AdminCacheConst.Login.REQUEST_EMPLOYEE)
    public RequestEmployee getRequestEmployee(Long employeeId) {
        EmployeeEntity entity = employeeService.getById(employeeId);
        return this.loadLoginInfo(entity);
    }

    @CachePut(value = AdminCacheConst.Login.REQUEST_EMPLOYEE, key = "#entity.employeeId")
    public RequestEmployee loadLoginInfo(EmployeeEntity entity) {
        // 複雜的登錄信息構建
    }

    @CacheEvict(value = AdminCacheConst.Login.REQUEST_EMPLOYEE)
    public void clearUserLoginInfo(Long employeeId) {
        // 清除緩存
    }
}
```

#### 3. 複雜業務編排（調用多個 Service/Mapper）
```java
@Service
@RequiredArgsConstructor
public class OrderManager {
    private final OrderMapper orderMapper;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;

    @Transactional(rollbackFor = Throwable.class)
    public void createOrder(Order order) {
        inventoryService.checkStock(order.getProductId(), order.getQuantity());
        orderMapper.insert(order);
        inventoryService.deductStock(order.getProductId(), order.getQuantity());
        paymentService.createPayment(order.getId(), order.getAmount());
    }
}
```

---

## Manager 命名規範

### 命名模式
- **事務管理**: `{Entity}Manager`（如 `EmployeeManager`、`OrderManager`）
- **緩存管理**: `{Entity}CacheManager`（如 `DepartmentCacheManager`、`MenuCacheManager`）
- **混合功能**: `{Domain}Manager`（如 `LoginManager`、`ReloadManager`）

### 示例對比
```java
// ✅ 正確命名
EmployeeManager          // 員工事務管理
DepartmentCacheManager   // 部門緩存管理
LoginManager             // 登錄業務編排

// ❌ 錯誤命名
EmployeeTransactionManager   // 冗余（Manager 已暗示事務）
DeptManager                  // 應使用完整單詞 Department
EmployeeManagerImpl          // 不需要 Impl 後綴
```

---

## 事務注解規範

### 強制規範
```java
// ✅ 正確：rollbackFor = Throwable.class（捕獲所有異常）
@Transactional(rollbackFor = Throwable.class)
public void saveEmployee(Employee employee) { }

// ❌ 錯誤：rollbackFor = Exception.class（無法捕獲 Error）
@Transactional(rollbackFor = Exception.class)  // ❌
public void saveEmployee(Employee employee) { }

// ❌ 錯誤：不指定 rollbackFor（只回滾 RuntimeException）
@Transactional  // ❌
public void saveEmployee(Employee employee) { }
```

### 只讀事務（可選優化）
```java
@Transactional(readOnly = true)  // 查詢場景可使用
public List<Employee> listByDepartment(Long deptId) {
    return employeeMapper.selectByDepartmentId(deptId);
}
```

---

## 緩存注解規範

### 緩存常量定義
```java
public interface AdminCacheConst {
    interface Department {
        String DEPARTMENT_LIST_CACHE = "sa:department:list";
        String DEPARTMENT_TREE_CACHE = "sa:department:tree";
    }

    interface Login {
        String REQUEST_EMPLOYEE = "sa:login:employee";
        String USER_PERMISSION = "sa:login:permission";
    }
}
```

### 緩存注解使用
```java
@Service
public class DepartmentCacheManager {

    // 查詢時緩存
    @Cacheable(AdminCacheConst.Department.DEPARTMENT_LIST_CACHE)
    public List<DepartmentVO> getDepartmentList() { }

    // 更新時刷新緩存
    @CachePut(value = AdminCacheConst.Department.DEPARTMENT_LIST_CACHE, key = "#dept.id")
    public DepartmentVO updateDepartment(Department dept) { }

    // 刪除時清空緩存
    @CacheEvict(value = AdminCacheConst.Department.DEPARTMENT_LIST_CACHE, allEntries = true)
    public void clearCache() { }
}
```

---

## ArchUnit 測試規則

```java
@ArchTest
static final ArchRule transactionalOnlyInManager = methods()
    .that().areAnnotatedWith(Transactional.class)
    .should().beDeclaredInClassesThat().haveSimpleNameEndingWith("Manager")
    .because("@Transactional 只能在 Manager 層使用（規則：09-manager-layer.md）");

@ArchTest
static final ArchRule cacheableOnlyInManager = methods()
    .that().areAnnotatedWith(Cacheable.class)
    .or().areAnnotatedWith(CacheEvict.class)
    .or().areAnnotatedWith(CachePut.class)
    .should().beDeclaredInClassesThat().haveSimpleNameEndingWith("Manager")
    .because("緩存注解只能在 Manager 層使用（規則：09-manager-layer.md）");
```

---

## 示例：Service 調用 Manager

```java
// Manager 層（事務邊界）
@Service
@RequiredArgsConstructor
public class EmployeeManager {
    private final EmployeeMapper employeeMapper;
    private final RoleEmployeeMapper roleEmployeeMapper;

    @Transactional(rollbackFor = Throwable.class)
    public void saveEmployee(EmployeeEntity e, List<Long> roleIds) {
        employeeMapper.insert(e);
        if (CollectionUtils.isNotEmpty(roleIds)) {
            roleEmployeeMapper.batchInsert(roleIds, e.getEmployeeId());
        }
    }
}

// Service 層（業務邏輯 + Vavr）
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeManager manager;
    private final EmployeeMapper mapper;

    public Option<EmployeeVO> findById(Long id) {
        return Option.of(mapper.selectById(id))
            .map(EmployeeMapper.INSTANCE::toVO);
    }

    public void createEmployee(EmployeeCreateDTO dto) {
        manager.saveEmployee(toEntity(dto), dto.getRoleIds());
    }
}
```

---

**最後更新**: 2025-01-13
**規範來源**: smart-admin 實際架構
**強制級別**: 🚫 ArchUnit 阻止 PR 合併
