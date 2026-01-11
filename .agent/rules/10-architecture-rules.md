---
trigger: always_on
---

# 架構約束規範 - 分層架構與模塊化設計

定義 Spring Boot 應用的架構邊界、分層職責與模塊化約束。
所有規則可通過 ArchUnit 自動化驗證。

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

## 第二部分：各層職責與約束

### Controller 層

```java
// ✅ 正確 - 只負責 HTTP 處理
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
public class UserController {
    
    private final UserService userService;
    private final UserMapper userMapper;
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody UserCreateRequest request) {
        UserCreateDTO dto = userMapper.toDTO(request);
        User user = userService.createUser(dto);
        return userMapper.toResponse(user);
    }
    
    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable @Positive Long id) {
        return userService.findById(id)
            .map(userMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}

// ❌ 錯誤 - 業務邏輯洩漏到 Controller
@PostMapping
public User createUser(@RequestBody UserCreateRequest request) {
    // ❌ 不應直接訪問 Repository
    if (userRepository.existsByEmail(request.getEmail())) {
        throw new DuplicateEmailException();
    }
    User user = new User();
    user.setEmail(request.getEmail());
    user.setPassword(passwordEncoder.encode(request.getPassword())); // ❌
    return userRepository.save(user); // ❌
}
```

### Service 層

```java
// ✅ 正確 - 封裝所有業務邏輯
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public User createUser(UserCreateDTO dto) {
        validateUniqueEmail(dto.getEmail());
        
        User user = User.builder()
            .email(dto.getEmail())
            .password(passwordEncoder.encode(dto.getPassword()))
            .status(UserStatus.PENDING)
            .build();
        
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new UserCreatedEvent(saved));
        
        log.info("User created: id={}", saved.getId());
        return saved;
    }
    
    private void validateUniqueEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("Email exists: " + email);
        }
    }
}

// ❌ 錯誤 - 依賴 HTTP 層概念
@Service
public class BadUserService {
    @Autowired
    private HttpServletRequest request; // ❌ 依賴 HTTP
}
```

### Repository 層

```java
// ✅ 正確 - 只負責數據訪問
public interface UserRepository extends BaseMapper<User> {
    
    @Select("SELECT * FROM t_user WHERE email = #{email}")
    Optional<User> findByEmail(@Param("email") String email);
    
    boolean existsByEmail(String email);
    
    default List<User> findActiveByRole(String role) {
        return selectList(
            Wrappers.<User>lambdaQuery()
                .eq(User::getStatus, UserStatus.ACTIVE)
                .eq(User::getRole, role)
        );
    }
}

// ❌ 錯誤 - 包含業務邏輯
public interface BadUserRepository extends BaseMapper<User> {
    // ❌ 密碼驗證不應在 Repository
    default boolean authenticate(String email, String password) {
        User user = findByEmail(email);
        return passwordEncoder.matches(password, user.getPassword());
    }
}
```

### Domain 層

```java
// ✅ 正確 - 純 POJO
@Data @Builder @TableName("t_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String email;
    @JsonIgnore
    private String password;
    private UserStatus status;
    
    public boolean isActive() {
        return UserStatus.ACTIVE.equals(this.status);
    }
}

// ❌ 錯誤 - 實體依賴 Spring
public class BadUser {
    @Autowired private UserRepository repository; // ❌
}
```

---

## 第三部分：ArchUnit 自動化測試

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

## 第四部分：多模塊項目

### 模塊結構

```
my-application/
├── my-app-api/              # 接口 + DTO
├── my-app-domain/           # 純 POJO，無依賴
├── my-app-service/          # 業務實現
├── my-app-infrastructure/   # MyBatis Plus、外部服務
└── my-app-web/              # Controller + 啟動
```

### Maven 依賴約束

```
依賴方向：
web → infrastructure → service → api → domain

domain:        無依賴（純 POJO）
api:           依賴 domain
service:       依賴 api + domain
infrastructure: 依賴 service + MyBatis Plus
web:           聚合所有 + Spring Boot Web
```

---

## 第五部分：API 設計規範

### RESTful 風格

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    
    @GetMapping                    // GET /users - 列表
    @GetMapping("/{id}")           // GET /users/123 - 詳情
    @PostMapping                   // POST /users - 創建
    @PutMapping("/{id}")           // PUT /users/123 - 全量更新
    @PatchMapping("/{id}")         // PATCH /users/123 - 部分更新
    @DeleteMapping("/{id}")        // DELETE /users/123 - 刪除
    
    // 子資源
    @GetMapping("/{userId}/orders")
    @PostMapping("/{userId}/orders")
    
    // 動作
    @PostMapping("/{id}/activate")
}

// ❌ 錯誤
@GetMapping("/getUserById")        // 動詞在 URL
@PostMapping("/users/delete/{id}") // 用 POST 刪除
```

### 統一響應格式

```java
@Data
@Builder
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;
    private long timestamp;
    
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .code(200).message("Success").data(data)
            .timestamp(System.currentTimeMillis()).build();
    }
}
```

---

## 架構檢查清單

- [ ] ArchUnit 所有測試通過
- [ ] 無循環依賴
- [ ] Controller 不直接訪問 Repository
- [ ] Controller 使用 DTO 不暴露 Entity
- [ ] Service 使用構造函數注入
- [ ] @Transactional 只在 Service 層
- [ ] 自定義異常在 exception 包
- [ ] Domain 層無 Spring 依賴
- [ ] 命名規範符合約定
- [ ] RESTful API 設計規範