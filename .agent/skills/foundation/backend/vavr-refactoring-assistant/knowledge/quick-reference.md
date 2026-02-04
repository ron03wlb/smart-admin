# Vavr Refactoring Assistant - Quick Reference

## Option 基本用法

```java
import io.vavr.control.Option;

// 創建
Option<String> some = Option.some("value");
Option<String> none = Option.none();
Option<String> of = Option.of(nullableValue);  // null → None

// 取值
String value = option.getOrElse("default");
String value = option.getOrNull();

// 轉換
Option<Integer> length = option.map(String::length);
Option<User> user = option.flatMap(this::findUser);

// 判斷
if (option.isDefined()) { ... }
if (option.isEmpty()) { ... }
```

## Optional → Option 遷移

```java
// ❌ Before (java.util.Optional)
import java.util.Optional;

public Optional<EmployeeVO> getById(Long id) {
    EmployeeEntity entity = employeeDao.selectById(id);
    return Optional.ofNullable(entity)
        .map(e -> SmartBeanUtil.copy(e, EmployeeVO.class));
}

// ✅ After (io.vavr.control.Option)
import io.vavr.control.Option;

public Option<EmployeeVO> getById(Long id) {
    EmployeeEntity entity = employeeDao.selectById(id);
    return Option.of(entity)
        .map(e -> SmartBeanUtil.copy(e, EmployeeVO.class));
}
```

## Try 用法

```java
import io.vavr.control.Try;

// 包裝可能失敗的操作
Try<String> result = Try.of(() -> riskyOperation());

// 處理結果
result.onSuccess(value -> log.info("Success: {}", value))
      .onFailure(ex -> log.error("Failed", ex));

// 取值
String value = result.getOrElse("default");
String value = result.getOrElseThrow(ex -> new RuntimeException(ex));
```

## Either 用法

```java
import io.vavr.control.Either;

// 左邊是錯誤，右邊是成功
public Either<String, EmployeeVO> validateAndGet(Long id) {
    if (id == null) {
        return Either.left("ID cannot be null");
    }
    return Option.of(employeeDao.selectById(id))
        .map(e -> SmartBeanUtil.copy(e, EmployeeVO.class))
        .toEither("Employee not found");
}

// 使用
either.fold(
    error -> ResponseDTO.error(error),
    success -> ResponseDTO.ok(success)
);
```

## 常見遷移模式

| Java | Vavr |
|------|------|
| `Optional.empty()` | `Option.none()` |
| `Optional.of(v)` | `Option.some(v)` |
| `Optional.ofNullable(v)` | `Option.of(v)` |
| `opt.isPresent()` | `opt.isDefined()` |
| `opt.isEmpty()` | `opt.isEmpty()` |
| `opt.orElse(d)` | `opt.getOrElse(d)` |
| `opt.orElseGet(s)` | `opt.getOrElse(s)` |

## 相關規則

- [P01-vavr-fundamentals.md](../../../../rules/technology/functional/P01-vavr-fundamentals.md)
- [P02-vavr-advanced.md](../../../../rules/technology/functional/P02-vavr-advanced.md)
