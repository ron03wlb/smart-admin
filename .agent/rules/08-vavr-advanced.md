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

## 不可變集合（可選）

```java
// List - 不可變、線程安全
io.vavr.collection.List<String> list = io.vavr.collection.List.of("a", "b", "c");
io.vavr.collection.List<Integer> lengths = list.map(String::length);

// Map
io.vavr.collection.Map<String, Integer> map =
    io.vavr.collection.HashMap.of("a", 1, "b", 2);
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
