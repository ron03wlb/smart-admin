---
trigger: always_on
description: Vavr 進階 - Either、集合、模式匹配
tags: [vavr, functional-programming, either, collections, pattern-matching]
positioning: ideal
prerequisites: [rules/08-vavr-fundamentals.md]
last_updated: 2025-01-12
---

# Vavr 函數式編程進階 - Either、集合、模式匹配、函數組合

> **TL;DR**: 使用 Either 表達業務邏輯分支，使用不可變集合保證線程安全，使用模式匹配簡化條件邏輯，使用函數組合提升可讀性。

**定位說明**: 本規範定義理想目標架構的進階函數式編程模式。

**前置依賴**: 需先閱讀 [08-vavr-fundamentals.md](./08-vavr-fundamentals.md)

---

## 目錄
- [Either - 業務邏輯分支](#either---業務邏輯分支)
- [不可變集合](#不可變集合)
- [模式匹配](#模式匹配)
- [函數組合](#函數組合)

---

## Either - 業務邏輯分支

### 【強制】基本使用

Either 表示兩種可能的值：Left（錯誤）或 Right（成功）。

```java
// ✅ Either<Error, Success> - 明確的錯誤處理
public Either<ValidationError, User> validateAndCreateUser(UserCreateDTO dto) {
    return validateEmail(dto.getEmail())
        .flatMap(email -> validatePassword(dto.getPassword()))
        .flatMap(password -> createUser(dto));
}

private Either<ValidationError, String> validateEmail(String email) {
    if (!EMAIL_PATTERN.matcher(email).matches()) {
        return Either.left(new ValidationError("EMAIL_INVALID", "郵箱格式錯誤"));
    }
    return Either.right(email);
}

private Either<ValidationError, String> validatePassword(String password) {
    if (password.length() < 8) {
        return Either.left(new ValidationError("PASSWORD_TOO_SHORT", "密碼至少8位"));
    }
    return Either.right(password);
}

private Either<ValidationError, User> createUser(UserCreateDTO dto) {
    return Try.of(() -> {
            User user = User.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .build();
            userRepository.save(user);
            return user;
        })
        .toEither()
        .mapLeft(ex -> new ValidationError("SAVE_FAILED", "保存失敗: " + ex.getMessage()));
}
```

### 【強制】Either API

```java
// 創建 Either
Either<String, Integer> right = Either.right(42);
Either<String, Integer> left = Either.left("Error");

// 轉換（僅作用於 Right）
Either<String, String> mapped = right.map(Object::toString);

// 轉換 Left
Either<Integer, Integer> leftMapped = left.mapLeft(String::length);

// flatMap 鏈式處理
Either<String, Integer> result = Either.right(10)
    .flatMap(i -> i > 0 ? Either.right(i * 2) : Either.left("負數"));

// fold - 處理兩種情況
String result = either.fold(
    error -> "錯誤: " + error,
    success -> "成功: " + success
);

// getOrElse
Integer value = right.getOrElse(0);

// 判斷
if (right.isRight()) { ... }
if (left.isLeft()) { ... }

// 交換 Left 和 Right
Either<Integer, String> swapped = right.swap();
```

### 【推薦】業務驗證

```java
@Service
public class OrderService {

    // ✅ Either 表達業務規則
    public Either<String, Order> createOrder(OrderCreateDTO dto) {
        return validateStock(dto.getItems())
            .flatMap(items -> validateUserCredit(dto.getUserId()))
            .flatMap(credit -> validatePaymentMethod(dto.getPaymentMethod()))
            .map(payment -> buildOrder(dto))
            .flatMap(this::saveOrder);
    }

    private Either<String, List<OrderItem>> validateStock(List<OrderItemDTO> items) {
        // 驗證庫存邏輯
        return Either.right(items.stream()
            .map(OrderItem::from)
            .collect(Collectors.toList()));
    }

    private Either<String, BigDecimal> validateUserCredit(Long userId) {
        BigDecimal credit = userMapper.selectById(userId).getCredit();
        return credit.compareTo(BigDecimal.ZERO) > 0
            ? Either.right(credit)
            : Either.left("用戶信用額度不足");
    }
}
```

### 【推薦】Controller 使用

```java
@RestController
public class OrderController {

    // ✅ fold() 處理 Either
    @PostMapping
    public ResponseDTO<OrderVO> createOrder(@Valid @RequestBody OrderCreateDTO dto) {
        return orderService.createOrder(dto)
            .fold(
                error -> ResponseDTO.error(error),
                order -> ResponseDTO.ok(OrderVO.from(order))
            );
    }
}
```

---

## 不可變集合

### 【強制】基本使用

Vavr 集合都是不可變的、線程安全的。

```java
// List
io.vavr.collection.List<String> list = io.vavr.collection.List.of("a", "b", "c");
io.vavr.collection.List<String> list2 = io.vavr.collection.List.ofAll(javaList);

// 轉換（返回新 List）
io.vavr.collection.List<Integer> lengths = list.map(String::length);
io.vavr.collection.List<String> filtered = list.filter(s -> s.length() > 1);
io.vavr.collection.List<String> sorted = list.sortBy(String::length);

// Map
io.vavr.collection.Map<String, Integer> map = io.vavr.collection.HashMap.of(
    "a", 1,
    "b", 2,
    "c", 3
);

// 添加（返回新 Map）
io.vavr.collection.Map<String, Integer> updated = map.put("d", 4);

// Set
io.vavr.collection.Set<String> set = io.vavr.collection.HashSet.of("a", "b", "c");
```

### 【推薦】Service 層使用

```java
@Service
public class UserService {

    // ✅ 返回不可變 List
    public io.vavr.collection.List<UserVO> getActiveUsers() {
        return io.vavr.collection.List.ofAll(
                userMapper.selectList(
                    Wrappers.<User>lambdaQuery().eq(User::getStatus, "ACTIVE")
                )
            )
            .filter(User::isVerified)
            .sortBy(User::getCreatedAt)
            .map(UserVO::from);
    }

    // ✅ groupBy 分組
    public io.vavr.collection.Map<String, io.vavr.collection.List<UserVO>>
            getUsersByDepartment() {
        return io.vavr.collection.List.ofAll(userMapper.selectAll())
            .groupBy(User::getDepartment)
            .mapValues(users -> users.map(UserVO::from));
    }
}
```

### 【推薦】線程安全場景

```java
@Component
public class CategoryCache {

    // ✅ 不可變集合，線程安全
    private volatile io.vavr.collection.List<Category> cache =
        io.vavr.collection.List.empty();

    @Scheduled(fixedRate = 300000) // 5分鐘刷新
    public void refresh() {
        io.vavr.collection.List<Category> newCache =
            io.vavr.collection.List.ofAll(categoryRepository.findAll())
                .filter(Category::isActive)
                .sortBy(Category::getOrder);

        // 原子替換
        this.cache = newCache;
    }

    public io.vavr.collection.List<CategoryVO> getByType(String type) {
        return cache
            .filter(c -> c.getType().equals(type))
            .map(CategoryVO::from);
    }

    public Option<Category> findById(Long id) {
        return cache.find(c -> c.getId().equals(id));
    }
}
```

---

## 模式匹配

### 【強制】基本使用

```java
import static io.vavr.API.*;
import static io.vavr.Predicates.*;

// ✅ 值匹配
public String processOrderStatus(OrderStatus status) {
    return Match(status).of(
        Case($(OrderStatus.PENDING), "等待支付"),
        Case($(OrderStatus.PAID), "已支付"),
        Case($(OrderStatus.SHIPPED), "已發貨"),
        Case($(OrderStatus.COMPLETED), "已完成"),
        Case($(OrderStatus.CANCELLED), "已取消"),
        Case($(), s -> {
            log.warn("未知訂單狀態: {}", s);
            return "未知狀態";
        })
    );
}

// ✅ 類型匹配
public String handleResult(Object result) {
    return Match(result).of(
        Case($(instanceOf(String.class)), s -> "字符串: " + s),
        Case($(instanceOf(Integer.class)), i -> "數字: " + i),
        Case($(instanceOf(List.class)), list -> "列表大小: " + list.size()),
        Case($(), obj -> "未知類型: " + obj.getClass().getSimpleName())
    );
}

// ✅ 條件匹配
public String classifyAge(int age) {
    return Match(age).of(
        Case($(n -> n < 18), "未成年"),
        Case($(n -> n >= 18 && n < 60), "成年"),
        Case($(n -> n >= 60), "老年"),
        Case($(), "無效年齡")
    );
}
```

### 【推薦】業務邏輯

```java
@Service
public class OrderStateMachine {

    // ✅ 狀態轉換
    public OrderVO processAction(Order order, OrderAction action) {
        Order updated = Match(Tuple.of(order.getStatus(), action)).of(
            Case($Tuple2($(OrderStatus.PENDING), $(OrderAction.PAY)),
                () -> handlePayment(order)),
            Case($Tuple2($(OrderStatus.PAID), $(OrderAction.SHIP)),
                () -> handleShipping(order)),
            Case($Tuple2($(isIn(OrderStatus.PENDING, OrderStatus.PAID)), $(OrderAction.CANCEL)),
                () -> handleCancellation(order)),
            Case($(), () -> {
                throw new IllegalStateException("無效的狀態轉換");
            })
        );
        return OrderVO.from(updated);
    }
}
```

### 【推薦】異常處理

```java
public ResponseDTO<?> handleException(Exception ex) {
    return Match(ex).of(
        Case($(instanceOf(UserNotFoundException.class)),
            e -> ResponseDTO.error(404, "用戶不存在")),

        Case($(instanceOf(ValidationException.class)),
            e -> ResponseDTO.error(400, e.getMessage())),

        Case($(instanceOf(DataIntegrityViolationException.class)),
            e -> ResponseDTO.error(409, "數據衝突")),

        Case($(instanceOf(Exception.class)),
            e -> {
                log.error("未處理的異常", e);
                return ResponseDTO.error(500, "服務器錯誤");
            })
    );
}
```

---

## 函數組合

### 【強制】Function 接口

```java
import io.vavr.Function1;
import io.vavr.Function2;

// Function1: 一個參數
Function1<String, String> normalizeEmail =
    email -> email.toLowerCase().trim();

Function1<String, Option<String>> validateEmail =
    email -> EMAIL_PATTERN.matcher(email).matches()
        ? Option.of(email)
        : Option.none();

Function1<String, Try<User>> findUserByEmail =
    email -> Try.of(() -> userRepository.findByEmail(email)
        .orElseThrow(() -> new UserNotFoundException(email)));

// Function2: 兩個參數
Function2<String, String, String> concat =
    (a, b) -> a + b;

Function2<Integer, Integer, Integer> add =
    (a, b) -> a + b;
```

### 【推薦】函數組合

```java
@Service
public class UserProcessor {

    private final Function1<String, String> normalizeEmail =
        email -> email.toLowerCase().trim();

    private final Function1<String, Option<String>> validateEmail =
        email -> EMAIL_PATTERN.matcher(email).matches()
            ? Option.of(email)
            : Option.none();

    // ✅ andThen 組合
    public Try<User> processUserLogin(String rawEmail) {
        return normalizeEmail
            .andThen(validateEmail)
            .andThen(opt -> opt.toTry(() -> new IllegalArgumentException("郵箱無效")))
            .apply(rawEmail);
    }
}
```

### 【推薦】偏函數應用

```java
// ✅ 柯里化
Function2<Integer, Integer, Integer> add = (a, b) -> a + b;
Function1<Integer, Integer> add5 = add.curried().apply(5);

int result = add5.apply(10); // 15

// ✅ 部分應用
Function1<Integer, Integer> increment = add.apply(1);
int result2 = increment.apply(10); // 11
```

---

## 檢查清單

**Either**:
- [ ] 業務驗證鏈使用 Either.flatMap
- [ ] Controller 使用 fold() 處理 Either
- [ ] 錯誤類型使用自定義類別而非 String

**不可變集合**:
- [ ] 共享數據使用 Vavr 集合保證線程安全
- [ ] 使用 map/filter/sortBy 鏈式操作
- [ ] 必要時使用 toJavaList() 轉換

**模式匹配**:
- [ ] 狀態機使用 Match + Tuple 匹配
- [ ] 異常處理使用 Match + instanceOf
- [ ] 避免過度使用（簡單 if-else 保持原樣）

**函數組合**:
- [ ] 複雜處理流程拆分為小函數
- [ ] 使用 andThen 組合而非嵌套調用
- [ ] 必要時使用柯里化/偏應用

---

## 相關規範
- [08-vavr-fundamentals.md](./08-vavr-fundamentals.md) - Option、Try 基礎
- [08-vavr-mybatis-integration.md](./08-vavr-mybatis-integration.md) - MyBatis Plus 整合
