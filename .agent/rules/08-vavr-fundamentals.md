---
trigger: always_on
description: Vavr 基礎 - Option 與 Try
tags: [vavr, functional-programming, option, try]
positioning: ideal
ai_role: code_reviewer_and_generator
auto_apply: true
ask_before_fix: false
prerequisites:
  - rules/01-naming-conventions.md
  - rules/10-architecture-rules.md
conflicts_with: []
related_rules:
  - rules/08-vavr-advanced.md
  - rules/08-vavr-mybatis-integration.md
archunit_test: ArchitectureTest#serviceUsesVavrOption
checkstyle_rule: none
spotbugs_rule: none
last_updated: 2025-01-13
---

# Vavr 函數式編程基礎 - Option 與 Try

> **TL;DR**: 使用 Vavr Option 替代 null 檢查，使用 Try 替代 try-catch，實現優雅的函數式異常處理。

**定位說明**: 本規範定義理想目標架構，引導專案從傳統 Java null/異常處理遷移至 Vavr 函數式模式。

---

## 🤖 AI 指令區塊

### 何時應用此規則
- ✅ 用戶要求生成 Service 層代碼
- ✅ Service 方法需要返回可能為 null 的對象
- ✅ 代碼中需要處理可能拋異常的操作（IO、網絡、外部 API）
- ✅ Code Review 時發現 `Optional` 或 `try-catch` 使用
- ✅ 用戶詢問如何處理 null 或異常

### 強制執行檢查清單
當生成或審查 Service 層代碼時，必須確認：
- [ ] Service 方法返回類型是 `Option<T>` 而非 `Optional<T>` 或 `T`
- [ ] 沒有顯式的 null 檢查（`if (obj == null)` 或 `obj != null`）
- [ ] 異常處理使用 `Try.of()` 而非 `try-catch` 塊
- [ ] 沒有在 Controller 參數中使用 Option（應該用基本類型）
- [ ] 使用 `map()`/`flatMap()` 進行鏈式調用而非嵌套 if

### AI 決策樹
```
用戶要求: "查詢用戶" / "獲取訂單"
  ├─ 返回單個對象？
  │   └─ YES → 使用 Option<Entity>
  │       ├─ Repository 返回可能為 null？
  │       │   └─ 使用: Option.of(repository.selectById(id))
  │       └─ Repository 返回 Optional？
  │           └─ 使用: Option.ofOptional(repository.findById(id))
  │
  ├─ 可能拋異常？
  │   └─ YES → 使用 Try<Entity>
  │       ├─ IO 操作（文件、網絡）？
  │       │   └─ 使用: Try.of(() -> ...).mapTry(...)
  │       ├─ 外部 API 調用？
  │       │   └─ 使用: Try.of(() -> ...).recover(...)
  │       └─ 數據庫操作？
  │           └─ 使用: Try.of(() -> ...).onFailure(log::error)
  │
  └─ 需要嵌套檢查？（user → address → city）
      └─ 使用 flatMap 鏈式調用
          Option.of(user)
            .flatMap(u -> Option.of(u.getAddress()))
            .map(Address::getCity)
```

### 錯誤模式檢測與自動修正

#### 模式 1: 檢測到 Optional 返回類型
```java
// ❌ 檢測到錯誤
public Optional<User> findById(Long id) {
    return userMapper.findById(id);
}

// ✅ 自動修正為
public Option<User> findById(Long id) {
    return Option.ofOptional(userMapper.findById(id));
}
// 或者（如果 Mapper 返回可能為 null）
public Option<User> findById(Long id) {
    return Option.of(userMapper.selectById(id));
}
```

#### 模式 2: 檢測到 null 檢查
```java
// ❌ 檢測到錯誤
public UserVO getUserCity(Long id) {
    User user = userRepository.findById(id);
    if (user == null) {
        throw new NotFoundException();
    }
    Address address = user.getAddress();
    if (address == null) {
        return new UserVO(user.getName(), "Unknown");
    }
    return new UserVO(user.getName(), address.getCity());
}

// ✅ 自動修正為
public UserVO getUserCity(Long id) {
    return Option.of(userRepository.findById(id))
        .map(user -> new UserVO(
            user.getName(),
            Option.of(user.getAddress())
                .map(Address::getCity)
                .getOrElse("Unknown")
        ))
        .getOrElseThrow(() -> new NotFoundException("用戶不存在"));
}
```

#### 模式 3: 檢測到 try-catch
```java
// ❌ 檢測到錯誤
public String readConfig(String path) {
    try {
        return Files.readString(Paths.get(path));
    } catch (IOException e) {
        log.error("配置讀取失敗", e);
        return "default-config";
    }
}

// ✅ 自動修正為
public Try<String> readConfig(String path) {
    return Try.of(() -> Files.readString(Paths.get(path)))
        .onFailure(e -> log.error("配置讀取失敗", e));
}
// Controller 層處理:
// readConfig(path).getOrElse("default-config")
```

#### 模式 4: 檢測到 Controller 參數使用 Option（錯誤）
```java
// ❌ 檢測到錯誤
@GetMapping("/{id}")
public ResponseDTO<UserVO> getUser(Option<Long> id) { // ❌ 錯誤用法
    ...
}

// ✅ 修正為
@GetMapping("/{id}")
public ResponseDTO<UserVO> getUser(@PathVariable Long id) { // ✅ 正確
    return userService.findById(id)
        .map(UserVO::from)
        .map(ResponseDTO::ok)
        .getOrElse(() -> ResponseDTO.error("用戶不存在"));
}
```

### 生成代碼標準模板

```java
// Service 層查詢
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper mapper;

    public Option<User> findById(Long id) {
        return Option.of(mapper.selectById(id));
    }

    public Option<User> findActiveById(Long id) {
        return findById(id).filter(User::isActive);
    }
}

// Controller 層處理 Option
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService service;

    @GetMapping("/{id}")
    public ResponseDTO<UserVO> getUser(@PathVariable Long id) {
        return service.findById(id)
            .map(UserVO::from)
            .fold(() -> ResponseDTO.error("不存在"), ResponseDTO::ok);
    }
}

// Try 異常處理
public Try<String> readFile(String path) {
    return Try.of(() -> Files.readString(Paths.get(path)))
        .onFailure(e -> log.error("文件讀取失敗", e));
}
```

### 驗證命令
```bash
mvn test -Dtest=ArchitectureTest#serviceUsesVavrOption
```

---

---

## Maven 依賴

```xml
<dependency>
    <groupId>io.vavr</groupId>
    <artifactId>vavr</artifactId>
    <version>0.10.4</version>
</dependency>

<!-- JSON 序列化支持 -->
<dependency>
    <groupId>io.vavr</groupId>
    <artifactId>vavr-jackson</artifactId>
    <version>0.10.4</version>
</dependency>
```

---

## 為什麼使用 Vavr？

- ✅ 不可變集合，線程安全
- ✅ Try/Option/Either 替代 try-catch 和 null
- ✅ 函數組合與鏈式調用
- ✅ 模式匹配增強可讀性
- ✅ 與 Spring MVC 完美兼容

---

## Option - null 安全

### 【強制】基本使用

```java
// ❌ 傳統方式 - null 檢查繁瑣
public UserVO getUserById(Long id) {
    User user = userRepository.findById(id);
    if (user == null) {
        throw new UserNotFoundException(id);
    }
    Address address = user.getAddress();
    if (address == null) {
        return new UserVO(user.getName(), "Unknown");
    }
    return new UserVO(user.getName(), address.getCity());
}

// ✅ Vavr Option - 優雅鏈式調用
public UserVO getUserById(Long id) {
    return Option.ofOptional(userRepository.findById(id))
        .map(user -> new UserVO(
            user.getName(),
            Option.of(user.getAddress())
                .map(Address::getCity)
                .getOrElse("Unknown")
        ))
        .getOrElseThrow(() -> new UserNotFoundException(id));
}
```

### 【強制】Option API

```java
// 創建 Option
Option<String> some = Option.of("value");
Option<String> none = Option.of(null);          // None
Option<String> fromOptional = Option.ofOptional(optional);

// 轉換
Option<Integer> length = some.map(String::length);
Option<String> upper = some.map(String::toUpperCase);

// 過濾
Option<String> filtered = some.filter(s -> s.length() > 5);

// 獲取值
String value = some.getOrElse("default");
String value2 = some.getOrElse(() -> computeDefault());
String value3 = some.getOrElseThrow(() -> new RuntimeException());

// 判斷
if (some.isDefined()) { ... }
if (some.isEmpty()) { ... }

// flatMap 處理嵌套
Option<String> city = Option.of(user)
    .flatMap(u -> Option.of(u.getAddress()))
    .map(Address::getCity);
```

### 【推薦】Service 層鏈式處理

```java
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper mapper;

    public Option<UserDetailVO> getUserDetail(Long id) {
        return Option.of(mapper.selectById(id))
            .filter(User::isActive)
            .map(UserDetailVO::from);
    }
}
```

---

## Try - 異常處理

### 【強制】基本使用

```java
// ❌ 傳統 try-catch
public String readFile(String path) {
    try {
        return Files.readString(Paths.get(path));
    } catch (IOException e) {
        log.error("文件讀取失敗", e);
        throw new RuntimeException(e);
    }
}

// ✅ Vavr Try
public Try<String> readFile(String path) {
    return Try.of(() -> Files.readString(Paths.get(path)))
        .onFailure(e -> log.error("文件讀取失敗", e));
}
```

### 【強制】Try API

```java
// 創建與轉換
Try<Integer> result = Try.of(() -> 42).map(i -> i * 2);
Try<String> content = Try.of(() -> Paths.get("f.txt")).mapTry(Files::readString);

// 錯誤恢復
Try<Integer> recovered = Try.of(() -> 1 / 0)
    .recover(ArithmeticException.class, 0);

// 獲取值
Integer value = result.getOrElse(0);
```

### 【推薦】外部 API 調用

```java
public Try<ApiResponse> callApi(String endpoint, Object req) {
    return Try.of(() -> restTemplate.postForObject(endpoint, req, ApiResponse.class))
        .recover(HttpClientErrorException.class, ex -> {
            log.error("API 失敗: {}", ex.getStatusCode());
            throw new ExternalApiException("第三方服務異常", ex);
        });
}
```

---

## 檢查清單

**Option 使用**:
- [ ] Service 層方法返回 Option 而非 null
- [ ] 使用 map/flatMap 鏈式處理而非 if-null 檢查
- [ ] Controller 使用 fold() 或 getOrElse() 處理 Option

**Try 使用**:
- [ ] 文件操作使用 Try.mapTry() 處理 IOException
- [ ] 外部 API 調用使用 Try 包裝
- [ ] 使用 recover() 處理特定異常而非 catch

**一般原則**:
- [ ] 避免在 Option/Try 內部拋異常
- [ ] 優先使用方法引用提升可讀性
- [ ] 與 Spring MVC 傳統架構兼容

---

## 相關規範
- [08-vavr-advanced.md](./08-vavr-advanced.md) - Either、集合、模式匹配
- [08-vavr-mybatis-integration.md](./08-vavr-mybatis-integration.md) - MyBatis Plus 整合
