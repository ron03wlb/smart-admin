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
related_rules:
  - rules/08-vavr-fundamentals.md
  - rules/09-mybatis-plus-core.md
  - rules/09-manager-layer.md
archunit_test: ArchitectureTest#layerDependencies
last_updated: 2025-01-17
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
- [ ] **依賴方向正確**: Controller → Service → Manager → Mapper → Domain
- [ ] **Controller 不直接訪問 Mapper**
- [ ] **使用構造函數注入**（禁止 @Autowired 字段注入）
- [ ] **@Transactional/@Cacheable 只在 Manager 層**（參考 [09-manager-layer.md](./09-manager-layer.md)）
- [ ] **類命名規範**: XxxController, XxxService, XxxManager, XxxMapper
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
  │   Step 1: Entity → Step 2: Mapper → Step 3: Service → Step 4: Controller
  │
  └─ 3️⃣ 確認依賴注入
      └─ 使用 @RequiredArgsConstructor + private final
```

---

## 分層架構

### 標準目錄結構
```
com.example.myapp/
├── controller/          # 表現層 - HTTP、DTO
├── service/             # 業務層 - 核心邏輯
├── manager/             # 管理層 - 事務、緩存
├── mapper/              # 持久層 - 數據訪問
├── domain/entity/       # 領域層 - 實體
└── common/              # exception、util
```

### 【強制】層級依賴方向
```
Controller → Service → Manager → Mapper/DAO → Domain

✅ 允許：上層依賴下層
✅ 允許：Service → Mapper（簡單場景可跳過 Manager）
❌ 禁止：反向依賴（Manager → Service）
❌ 禁止：跨層訪問（Controller → Manager/Mapper）
❌ 禁止：Manager 橫向調用（ManagerA → ManagerB）
```

> **Manager 層詳細約束**: 參考 [09-manager-layer.md](./09-manager-layer.md)

---

## 錯誤模式檢測

### 模式 1: Controller 直接訪問 Repository
```java
// ❌ 架構違規
@RestController
public class UserController {
    private final UserMapper userMapper; // ❌ 錯誤！
}

// ✅ 修正為
@RestController
public class UserController {
    private final UserService userService; // ✅ 正確
}
```

### 模式 2: 字段注入
```java
// ❌ 字段注入
@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;
}

// ✅ 構造函數注入
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
}
```

---

## 生成代碼模板

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
            .fold(() -> ResponseDTO.error("用戶不存在"), ResponseDTO::ok);
    }
}
```

---

## ArchUnit 自動化測試

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

## 架構檢查清單

- [ ] ArchUnit 所有測試通過
- [ ] 無循環依賴
- [ ] Controller 不直接訪問 Mapper
- [ ] Controller 使用 DTO/VO 不暴露 Entity
- [ ] Service 使用構造函數注入
- [ ] @Transactional/@Cacheable 只在 Manager 層
- [ ] Domain 層無 Spring 依賴
- [ ] 命名規範符合約定

### 驗證命令
```bash
./gradlew check
mvn test -Dtest=ArchitectureTest
```