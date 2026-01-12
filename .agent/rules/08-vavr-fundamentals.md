---
trigger: always_on
description: Vavr 基礎 - Option 與 Try
tags: [vavr, functional-programming, option, try]
positioning: ideal
last_updated: 2025-01-12
---

# Vavr 函數式編程基礎 - Option 與 Try

> **TL;DR**: 使用 Vavr Option 替代 null 檢查，使用 Try 替代 try-catch，實現優雅的函數式異常處理。

**定位說明**: 本規範定義理想目標架構，引導專案從傳統 Java null/異常處理遷移至 Vavr 函數式模式。

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

### 【推薦】在 Service 層使用

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    // ✅ 返回 Option 代替 Optional
    public Option<User> findById(Long id) {
        return Option.of(userMapper.selectById(id));
    }

    // ✅ 鏈式處理
    public Option<UserDetailVO> getUserDetail(Long id) {
        return findById(id)
            .filter(User::isActive)
            .map(this::enrichWithOrders)
            .map(UserDetailVO::from);
    }

    private User enrichWithOrders(User user) {
        List<Order> orders = orderMapper.selectByUserId(user.getId());
        user.setOrders(orders);
        return user;
    }
}
```

### 【推薦】Controller 集成

```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ✅ fold() 處理 Option
    @GetMapping("/{id}")
    public ResponseDTO<UserVO> getUser(@PathVariable Long id) {
        return userService.findById(id)
            .map(UserVO::from)
            .fold(
                () -> ResponseDTO.error("用戶不存在"),
                user -> ResponseDTO.ok(user)
            );
    }

    // ✅ peek() 執行副作用
    @GetMapping("/{id}/profile")
    public ResponseDTO<UserProfileVO> getProfile(@PathVariable Long id) {
        return userService.findById(id)
            .peek(user -> log.info("訪問用戶檔案: {}", user.getId()))
            .map(UserProfileVO::from)
            .map(ResponseDTO::ok)
            .getOrElse(() -> ResponseDTO.error("用戶不存在"));
    }
}
```

---

## Try - 異常處理

### 【強制】基本使用

```java
// ❌ 傳統方式 - 異常處理冗長
public OrderVO createOrder(OrderCreateDTO dto) {
    try {
        Order order = orderService.create(dto);
        try {
            paymentService.processPayment(order.getId());
            return OrderVO.from(order);
        } catch (PaymentException e) {
            orderService.markAsFailed(order.getId());
            throw new BusinessException("支付失敗", e);
        }
    } catch (Exception e) {
        log.error("訂單創建失敗", e);
        throw new BusinessException("訂單創建失敗", e);
    }
}

// ✅ Vavr Try - 函數式異常處理
public OrderVO createOrder(OrderCreateDTO dto) {
    return Try.of(() -> orderService.create(dto))
        .andThen(order -> paymentService.processPayment(order.getId()))
        .map(OrderVO::from)
        .recover(PaymentException.class, ex -> {
            log.warn("支付失敗，訂單已標記失敗", ex);
            throw new BusinessException("支付失敗", ex);
        })
        .getOrElseThrow(ex -> new BusinessException("訂單創建失敗", ex));
}
```

### 【強制】Try API

```java
// 創建 Try
Try<Integer> success = Try.of(() -> 42);
Try<Integer> failure = Try.of(() -> 1 / 0);

// 轉換（僅 Success 執行）
Try<String> result = success.map(Object::toString);
Try<Integer> doubled = success.map(i -> i * 2);

// mapTry 處理可能拋異常的函數
Try<String> content = Try.of(() -> Paths.get("file.txt"))
    .mapTry(Files::readString);

// 鏈式調用
Try<Result> result = Try.of(() -> step1())
    .mapTry(this::step2)
    .mapTry(this::step3);

// 錯誤恢復
Try<Integer> recovered = failure
    .recover(ArithmeticException.class, 0)
    .recover(ex -> -1);

// 錯誤轉換
Try<Integer> mapped = failure
    .recoverWith(ex -> Try.of(() -> fallbackComputation()));

// 獲取值
Integer value = success.get();                        // 成功返回值，失敗拋異常
Integer value2 = success.getOrElse(0);
Integer value3 = success.getOrElseGet(ex -> computeDefault());

// 轉換為 Either
Either<Throwable, Integer> either = success.toEither();

// 判斷
if (success.isSuccess()) { ... }
if (failure.isFailure()) { ... }
```

### 【推薦】文件操作

```java
@Service
public class FileService {

    // ✅ Try 處理 IO 異常
    public Try<String> readFile(String path) {
        return Try.of(() -> Paths.get(path))
            .mapTry(Files::readString)
            .mapTry(String::trim);
    }

    // ✅ andThen 執行副作用
    public Try<Void> writeFile(String path, String content) {
        return Try.of(() -> Paths.get(path))
            .andThenTry(p -> Files.writeString(p, content))
            .andThen(() -> log.info("文件寫入成功: {}", path));
    }

    // ✅ 組合多個操作
    public Try<FileProcessResult> processFile(String inputPath, String outputPath) {
        return readFile(inputPath)
            .mapTry(this::transform)
            .flatMapTry(content -> writeFile(outputPath, content)
                .map(v -> new FileProcessResult(inputPath, outputPath)));
    }
}
```

### 【推薦】外部 API 調用

```java
@Service
@RequiredArgsConstructor
public class ThirdPartyService {

    private final RestTemplate restTemplate;

    // ✅ Try 包裝 HTTP 調用
    public Try<ApiResponse> callExternalApi(String endpoint, Object request) {
        return Try.of(() -> restTemplate.postForObject(
                endpoint,
                request,
                ApiResponse.class
            ))
            .recover(HttpClientErrorException.class, ex -> {
                log.error("API 調用失敗: {} - {}", ex.getStatusCode(), ex.getMessage());
                throw new ExternalApiException("第三方服務異常", ex);
            })
            .recover(ResourceAccessException.class, ex -> {
                log.error("API 連接超時", ex);
                throw new ExternalApiException("連接超時", ex);
            });
    }

    // ✅ 重試邏輯
    public Try<ApiResponse> callWithRetry(String endpoint, Object request, int maxAttempts) {
        return retry(maxAttempts, () -> callExternalApi(endpoint, request));
    }

    private <T> Try<T> retry(int maxAttempts, Supplier<Try<T>> operation) {
        Try<T> result = operation.get();
        for (int i = 1; i < maxAttempts && result.isFailure(); i++) {
            log.info("重試第 {} 次", i);
            result = operation.get();
        }
        return result;
    }
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
