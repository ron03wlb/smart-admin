---
trigger: always_on
description: Layered Architecture Rules - Controller/Service/Manager/Dao/Entity
tags: [architecture, layering, spring-mvc, dto-vo-pattern]
positioning: current-standard
ai_role: code_reviewer_and_generator
auto_apply: true
ask_before_fix: true
prerequisites:
  - foundation/01-naming-conventions.md
  - foundation/02-oop-principles.md
related_rules:
  - technology/functional/08-vavr-fundamentals.md
  - technology/database/09-mybatis-plus-core.md
  - foundation/09-manager-layer.md
archunit_test: ArchitectureTest#layerDependencies
last_updated: 2025-01-17
---

# Architecture Constraint Rules - Layered Architecture and Modular Design

Defines architecture boundaries, layer responsibilities, and modular constraints for Spring Boot applications.
All rules can be automatically validated through ArchUnit.

---

## 🤖 AI Instructions Block

### When to Apply This Rule
- ✅ **Always**: Must follow architecture rules when generating any Java code
- ✅ User requests to create Controller/Service/Repository/Entity
- ✅ Check layer correctness during Code Review
- ✅ Detected cross-layer access (e.g., Controller directly accessing Repository)
- ✅ Detected field injection (should use constructor injection)

### Mandatory Enforcement Checklist
- [ ] **Dependency direction correct**: Controller → Service → Manager → Mapper → Domain
- [ ] **Controller does not directly access Mapper**
- [ ] **Use Constructor Injection** (prohibit @Autowired field injection)
- [ ] **@Transactional/@Cacheable only in Manager layer** (refer to [09-manager-layer.md](./09-manager-layer.md))
- [ ] **Class naming convention**: XxxController, XxxService, XxxManager, XxxMapper
- [ ] **Controller uses DTO/VO** (do not directly expose Entity)
- [ ] **Domain layer has no Spring dependencies** (pure POJO)

### AI Decision Tree
```
User request: "Create user management functionality"
  ├─ 1️⃣ Determine required layers
  │   ├─ Controller? → YES (need HTTP interface)
  │   ├─ Service? → YES (business logic)
  │   ├─ Repository? → YES (data access)
  │   └─ Entity? → Check if already exists
  │
  ├─ 2️⃣ Generate in order (inner layer to outer layer)
  │   Step 1: Entity → Step 2: Mapper → Step 3: Service → Step 4: Controller
  │
  └─ 3️⃣ Confirm dependency injection
      └─ Use @RequiredArgsConstructor + private final
```

---

## Layered Architecture

### Standard Directory Structure
```
com.example.myapp/
├── controller/          # Presentation Layer - HTTP, DTO
├── service/             # Business Layer - Core logic
├── manager/             # Manager Layer - Transactions, Cache
├── mapper/              # Persistence Layer - Data Access
├── domain/entity/       # Domain Layer - Entities
└── common/              # exception, util
```

### 【Mandatory】Layer Dependency Direction
```
Controller → Service → Manager → Mapper/DAO → Domain

✅ Allowed: Upper layer depends on lower layer
✅ Allowed: Service → Dao/Mapper (for single CRUD operations without @Transactional)
✅ Allowed: Service → Manager (when @Transactional/@Cacheable needed)
❌ Prohibited: Reverse dependency (Manager → Service)
❌ Prohibited: Cross-layer access (Controller → Manager/Mapper)
❌ Prohibited: Manager lateral invocation (ManagerA → ManagerB)
```

> **Manager Layer Detailed Constraints**: Refer to [09-manager-layer.md](./09-manager-layer.md)

### 【Critical】Service Layer Can Directly Access Dao/Mapper

**Service layer is allowed to directly call Dao/Mapper** for simple single-table CRUD operations:

```java
// ✅ Allowed: Service directly calls Dao (single-table read)
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeDao employeeDao;

    public EmployeeEntity getById(Long id) {
        return employeeDao.selectById(id);  // ✅ Single query, no transaction
    }

    public List<EmployeeVO> listAll() {
        return employeeDao.selectList(null);  // ✅ Single query
    }
}

// ✅ Allowed: Service directly calls Dao (single-table write)
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeDao employeeDao;

    public void updateAvatar(Long id, String avatar) {
        EmployeeEntity entity = new EmployeeEntity();
        entity.setEmployeeId(id);
        entity.setAvatar(avatar);
        employeeDao.updateById(entity);  // ✅ Single update, no transaction
    }
}

// ❌ Prohibited: Service uses @Transactional for multi-table operations
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeDao employeeDao;
    private final RoleEmployeeDao roleEmployeeDao;

    @Transactional  // ❌ Service cannot use @Transactional!
    public void updateEmployee(EmployeeEntity employee, List<Long> roleIds) {
        employeeDao.updateById(employee);
        roleEmployeeDao.deleteByEmployeeId(employee.getId());
        roleEmployeeDao.batchInsert(roleIds, employee.getId());
    }
}

// ✅ Correct: Delegate to Manager for multi-table operations with @Transactional
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeDao employeeDao;
    private final EmployeeManager employeeManager;

    public void updateEmployee(EmployeeUpdateForm form) {
        // Business validation
        EmployeeEntity existing = employeeDao.selectById(form.getId());
        if (existing == null) {
            return ResponseDTO.error("Employee not found");
        }

        // Delegate to Manager for transactional multi-table operation
        EmployeeEntity entity = SmartBeanUtil.copy(form, EmployeeEntity.class);
        employeeManager.updateEmployeeTransaction(entity, form.getRoleIds());
        return ResponseDTO.ok();
    }
}
```

**Decision Rule**:
- **Service → Dao directly**: Single-table CRUD, no @Transactional, no @Cacheable
- **Service → Manager**: Multi-table operations, requires @Transactional/@Cacheable

---

## Error Pattern Detection

### Pattern 1: Controller Directly Accessing Repository
```java
// ❌ Architecture Violation
@RestController
public class UserController {
    private final UserMapper userMapper; // ❌ Incorrect!
}

// ✅ Fix to
@RestController
public class UserController {
    private final UserService userService; // ✅ Correct
}
```

### Pattern 2: Field Injection
```java
// ❌ Field Injection
@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;
}

// ✅ Constructor Injection
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
}
```

---

## Code Generation Templates

### Entity
```java
@Data
@Builder
@TableName("t_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long userId;
    private String username;
    private String email;
}
```

### Mapper
```java
@Mapper
public interface UserMapper extends BaseMapper<User> {
    default Option<User> findByEmail(String email) {
        return Option.of(selectOne(
            Wrappers.<User>lambdaQuery().eq(User::getEmail, email)));
    }
}
```

### Service
```java
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;

    public Option<User> findById(Long id) {
        return Option.of(userMapper.selectById(id));
    }
}
```

### Controller
```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseDTO<UserVO> getUser(@PathVariable Long id) {
        return userService.findById(id)
            .map(UserVO::from)
            .fold(() -> ResponseDTO.error("User not found"), ResponseDTO::ok);
    }
}
```

---

## ArchUnit Automated Testing

```java
@AnalyzeClasses(packages = "com.example")
public class ArchitectureTest {

    @ArchTest
    static final ArchRule layerDependencies = layeredArchitecture()
        .layer("Controller").definedBy("..controller..")
        .layer("Service").definedBy("..service..")
        .layer("Repository").definedBy("..mapper..", "..repository..")
        .layer("Domain").definedBy("..domain..", "..entity..")
        .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
        .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
        .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service", "Manager");

    @ArchTest
    static final ArchRule noFieldInjection = fields()
        .that().areDeclaredInClassesThat()
        .resideInAnyPackage("..controller..", "..service..")
        .should().notBeAnnotatedWith(Autowired.class);

    @ArchTest
    static final ArchRule controllerNotAccessRepository = noClasses()
        .that().resideInAPackage("..controller..")
        .should().dependOnClassesThat().resideInAPackage("..mapper..");
}
```

---

## Architecture Checklist

- [ ] ArchUnit all tests passing
- [ ] No circular dependencies
- [ ] Controller does not directly access Mapper
- [ ] Controller uses DTO/VO, does not expose Entity
- [ ] Service uses Constructor Injection
- [ ] @Transactional/@Cacheable only in Manager layer
- [ ] Domain layer has no Spring dependencies
- [ ] Naming convention follows agreement

### Validation Commands
```bash
./gradlew check
mvn test -Dtest=ArchitectureTest
```
