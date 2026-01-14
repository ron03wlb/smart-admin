---
trigger: always_on
description: 分層架構規範 - Controller/Service/Manager/Dao/Entity
tags: [architecture, layering, spring-mvc, dto-vo-pattern]
positioning: current-standard
ai_role: code_reviewer_and_generator
auto_apply: true
ask_before_fix: true
prerequisites:
  - rules/01-naming-conventions.md
  - rules/02-oop-principles.md
conflicts_with: []
related_rules:
  - rules/08-vavr-fundamentals.md
  - rules/09-mybatis-plus-core.md
archunit_test: ArchitectureTest#layerDependencies
checkstyle_rule: none
spotbugs_rule: none
last_updated: 2025-01-13
---

# 架構約束規範 - 分層架構與模塊化設計

定義 Spring Boot 應用的架構邊界、分層職責與模塊化約束。
所有規則可通過 ArchUnit 自動化驗證。

---

## 🤖 AI 指令區塊

### 何時應用此規則
- ✅ **永遠**: 生成任何 Java 代碼時都必須遵守架構規範
- ✅ 用戶要求創建 Controller/Service/Repository/Entity
- ✅ Code Review 時檢查分層正確性
- ✅ 檢測到跨層訪問（如 Controller 直接訪問 Repository）
- ✅ 檢測到字段注入（應該用構造函數注入）

### 強制執行檢查清單
生成或審查代碼時，必須確認：
- [ ] **依賴方向正確**: Controller → Service → Manager → Mapper → Domain（禁止反向）
- [ ] **Controller 不直接訪問 Mapper**（必須通過 Service）
- [ ] **使用構造函數注入**（禁止 @Autowired 字段注入）
- [ ] **@Transactional/@Cacheable 只在 Manager 層**（參考 09-manager-layer.md）
- [ ] **類命名規範**: XxxController, XxxService, XxxManager, XxxMapper, XxxEntity
- [ ] **Controller 使用 DTO/VO**（不直接暴露 Entity）
- [ ] **Domain 層無 Spring 依賴**（純 POJO）

### AI 決策樹
```
用戶要求: "創建用戶管理功能"
  ├─ 1️⃣ 確定需要的層次
  │   ├─ Controller? → YES（需要 HTTP 接口）
  │   ├─ Service? → YES（業務邏輯）
  │   ├─ Repository? → YES（數據訪問）
  │   └─ Entity? → 檢查是否已存在
  │
  ├─ 2️⃣ 按順序生成（從內層到外層）
  │   Step 1: Entity（如果不存在）
  │   Step 2: Repository extends BaseMapper
  │   Step 3: Service implements XxxService
  │   Step 4: Controller 注入 Service
  │
  ├─ 3️⃣ 確認依賴注入方式
  │   └─ 使用 @RequiredArgsConstructor + private final
  │       ❌ 不使用 @Autowired 字段注入
  │
  └─ 4️⃣ 確認職責分配
      ├─ Controller: HTTP 處理、DTO 轉換、參數校驗
      ├─ Service: 業務邏輯、事務管理、調用 Repository
      └─ Repository: 數據訪問、類型安全查詢
```

### 錯誤模式檢測與修正

#### 模式 1: Controller 直接訪問 Repository（嚴重錯誤）
```java
// ❌ 檢測到架構違規
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserRepository userRepository; // ❌ 錯誤！

    @GetMapping("/{id}")
    public ResponseDTO<User> getUser(@PathVariable Long id) {
        return ResponseDTO.ok(userRepository.findById(id)); // ❌
    }
}

// 🔧 需要詢問用戶後修正
// AI: "檢測到 Controller 直接訪問 Repository，這違反了分層架構。我將創建 UserService 作為中間層，是否繼續？"

// ✅ 修正為
// 1. 創建 Service
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;

    public Option<User> findById(Long id) {
        return Option.of(userMapper.selectById(id));
    }
}

// 2. 修改 Controller
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService; // ✅ 正確

    @GetMapping("/{id}")
    public ResponseDTO<UserVO> getUser(@PathVariable Long id) {
        return userService.findById(id)
            .map(UserVO::from)
            .map(ResponseDTO::ok)
            .getOrElse(() -> ResponseDTO.error("用戶不存在"));
    }
}
```

#### 模式 2: 字段注入（需要修正）
```java
// ❌ 檢測到錯誤
@Service
public class UserService {
    @Autowired // ❌ 字段注入
    private UserRepository userRepository;

    @Autowired // ❌ 字段注入
    private EmailService emailService;
}

// ✅ 自動修正為
@Service
@RequiredArgsConstructor // ✅ Lombok 構造函數
public class UserService {
    private final UserMapper userMapper; // ✅ final 字段
    private final EmailService emailService; // ✅ final 字段
}
```

#### 模式 3: Entity 依賴 Spring（錯誤）
```java
// ❌ 檢測到錯誤
@TableName("t_user")
public class User {
    @Autowired // ❌ Entity 不應該有依賴注入
    private UserRepository repository;
}

// ✅ 修正為（Domain 應該是純 POJO）
@Data
@Builder
@TableName("t_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String email;
    // ... 只有數據字段，無依賴
}
```

### 生成代碼標準模板

#### 模板 1: 完整的 CRUD 功能（推薦順序）

**Step 1: Entity（如果不存在）**
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long userId;

    private String username;
    private String email;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
```

**Step 2: Repository/Mapper**
```java
@Mapper
public interface UserMapper extends BaseMapper<User> {

    // 使用 default 方法添加自定義查詢
    default Option<User> findByEmail(String email) {
        return Option.of(selectOne(
            Wrappers.<User>lambdaQuery()
                .eq(User::getEmail, email)
        ));
    }
}
```

**Step 3: Service（業務邏輯）**
```java
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;

    public Option<User> findById(Long id) {
        return Option.of(userMapper.selectById(id));
    }

    public void createUser(UserCreateDTO dto) {
        User user = User.builder()
            .username(dto.getUsername())
            .email(dto.getEmail())
            .build();
        userMapper.insert(user);
    }
}
```

**Step 4: Controller（HTTP 接口）**
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
            .fold(() -> ResponseDTO.error("用戶不存在"), ResponseDTO::ok);
    }

    @PostMapping
    public ResponseDTO<Void> createUser(@Valid @RequestBody UserCreateDTO dto) {
        userService.createUser(dto);
        return ResponseDTO.ok();
    }
}
```

### 驗證命令
```bash
mvn test -Dtest=ArchitectureTest  # 完整架構測試
mvn test -Dtest=ArchitectureTest#layerDependencies  # 分層依賴檢查
```

---

---

## 第一部分：分層架構

### 標準目錄結構

```
com.example.myapp/
├── controller/          # 表現層 - HTTP、DTO
├── service/             # 業務層 - 核心邏輯
│   └── impl/
├── repository/          # 持久層 - 數據訪問
├── domain/entity/       # 領域層 - 實體
├── infrastructure/      # 基礎設施 - config、external
└── common/              # exception、util
```

### 【強制】層級依賴方向

```
Controller → Service → Repository → Domain

✅ 允許：上層依賴下層
❌ 禁止：反向依賴、跨層訪問（Controller → Repository）
```

---

## ArchUnit 自動化測試

```java
@AnalyzeClasses(packages = "com.example", 
    importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    // 分層約束
    @ArchTest
    static final ArchRule layerDependencies = layeredArchitecture()
        .consideringAllDependencies()
        .layer("Controller").definedBy("..controller..")
        .layer("Service").definedBy("..service..")
        .layer("Repository").definedBy("..repository..")
        .layer("Domain").definedBy("..domain..", "..entity..")
        .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
        .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller", "Infrastructure")
        .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service");

    // 命名規範
    @ArchTest
    static final ArchRule controllerNaming = classes()
        .that().resideInAPackage("..controller..")
        .and().areAnnotatedWith(RestController.class)
        .should().haveSimpleNameEndingWith("Controller");

    @ArchTest
    static final ArchRule serviceImplNaming = classes()
        .that().resideInAPackage("..service.impl..")
        .should().haveSimpleNameEndingWith("ServiceImpl");

    // 禁止字段注入
    @ArchTest
    static final ArchRule noFieldInjection = fields()
        .that().areDeclaredInClassesThat()
        .resideInAnyPackage("..controller..", "..service..")
        .should().notBeAnnotatedWith(Autowired.class);

    // Controller 不能直接訪問 Repository
    @ArchTest
    static final ArchRule controllerNotAccessRepository = noClasses()
        .that().resideInAPackage("..controller..")
        .should().dependOnClassesThat().resideInAPackage("..repository..");

    // Domain 層無 Spring 依賴
    @ArchTest
    static final ArchRule domainPure = noClasses()
        .that().resideInAPackage("..domain..", "..entity..")
        .should().dependOnClassesThat()
        .resideInAnyPackage("org.springframework..");

    // 無循環依賴
    @ArchTest
    static final ArchRule noCycles = slices()
        .matching("com.example.(*)..")
        .should().beFreeOfCycles();

    // @Transactional 只在 Service 層
    @ArchTest
    static final ArchRule transactionalInService = methods()
        .that().areDeclaredInClassesThat()
        .resideOutsideOfPackage("..service..")
        .should().notBeAnnotatedWith(Transactional.class);
}
```

---

## 架構檢查清單

- [ ] ArchUnit 所有測試通過
- [ ] 無循環依賴
- [ ] Controller 不直接訪問 Mapper
- [ ] Controller 使用 DTO/VO 不暴露 Entity
- [ ] Service 使用構造函數注入
- [ ] @Transactional/@Cacheable 只在 Manager 層（參考 09-manager-layer.md）
- [ ] Domain 層無 Spring 依賴
- [ ] 命名規範符合約定