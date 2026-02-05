# SmartAdmin Manager Extractor - Quick Reference

## SmartAdmin 架構規則

```
Controller → Service → Manager → Dao
                ↓
            (事務邏輯在 Manager)
```

**關鍵規則**:
- `@Transactional` 只能在 Manager 層
- `@Cacheable` 只能在 Manager 層
- Service 可直接調用 Dao (單表 CRUD)
- Service 需要事務時，委派給 Manager

## 提取步驟

### 1. 識別違規

```java
// ❌ 錯誤: Service 層有 @Transactional
@Service
public class EmployeeService {
    @Transactional(rollbackFor = Throwable.class)
    public void createEmployee(EmployeeForm form) {
        // ...
    }
}
```

### 2. 創建 Manager

```java
// ✅ 正確: Manager 層處理事務
@Service
@RequiredArgsConstructor
public class EmployeeManager {
    private final EmployeeDao employeeDao;
    private final DepartmentDao departmentDao;

    @Transactional(rollbackFor = Throwable.class)
    public void createEmployee(EmployeeForm form) {
        // 多表操作或需要事務的邏輯
        EmployeeEntity entity = SmartBeanUtil.copy(form, EmployeeEntity.class);
        employeeDao.insert(entity);
        departmentDao.incrementEmployeeCount(form.getDepartmentId());
    }
}
```

### 3. 更新 Service

```java
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeManager employeeManager;
    private final EmployeeDao employeeDao;

    // 委派給 Manager
    public void createEmployee(EmployeeForm form) {
        employeeManager.createEmployee(form);
    }

    // 單表查詢可直接用 Dao
    public Option<EmployeeVO> getById(Long id) {
        return Option.of(employeeDao.selectById(id))
            .map(e -> SmartBeanUtil.copy(e, EmployeeVO.class));
    }
}
```

## 驗證命令

```bash
# 編譯檢查
./gradlew :smartadmin-app:compileJava

# ArchUnit 測試
./gradlew :smartadmin-app:test --tests ArchitectureTest

# 完整測試
./gradlew :smartadmin-app:test
```

## ArchUnit 規則源碼

```java
@ArchTest
static final ArchRule transactionalMustUseRollbackForThrowable =
    methods()
        .that().areAnnotatedWith(Transactional.class)
        .should().beAnnotatedWith(
            Transactional.class,
            annotation -> annotation.rollbackFor().contains(Throwable.class)
        )
        .andShould().beDeclaredInClassesThat()
        .haveSimpleNameEndingWith("Manager");
```
