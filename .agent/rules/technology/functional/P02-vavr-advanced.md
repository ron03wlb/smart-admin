---
trigger: always_on
description: Vavr Advanced - Either, Collections, Pattern Matching
tags: [vavr, functional-programming, either, collections, pattern-matching]
positioning: ideal
prerequisites:
  - technology/functional/P01-vavr-fundamentals.md
last_updated: 2025-01-12
---

# Vavr Functional Programming Advanced - Either, Collections, Pattern Matching, Function Composition

> **TL;DR**: Use Either to express business logic branches, use immutable collections to ensure thread safety, use pattern matching to simplify conditional logic, use function composition to enhance readability.

**Positioning Statement**: This specification defines advanced functional programming patterns for the ideal target architecture.

**Prerequisites**: Must read [08-vavr-fundamentals.md](./08-vavr-fundamentals.md) first

---

## Table of Contents
- [Either - Business Logic Branches](#either---business-logic-branches)
- [Immutable Collections](#immutable-collections)
- [Pattern Matching](#pattern-matching)
- [Function Composition](#function-composition)

---

## Either - Business Logic Branches

### [Mandatory] Basic Usage

Either represents two possible values: Left (error) or Right (success).

```java
// ✅ Either<Error, Success> - Explicit error handling
public Either<ValidationError, User> validateAndCreateUser(UserCreateDTO dto) {
    return validateEmail(dto.getEmail())
        .flatMap(email -> validatePassword(dto.getPassword()))
        .flatMap(password -> createUser(dto));
}

private Either<ValidationError, String> validateEmail(String email) {
    if (!EMAIL_PATTERN.matcher(email).matches()) {
        return Either.left(new ValidationError("EMAIL_INVALID", "Invalid email format"));
    }
    return Either.right(email);
}

private Either<ValidationError, String> validatePassword(String password) {
    if (password.length() < 8) {
        return Either.left(new ValidationError("PASSWORD_TOO_SHORT", "Password must be at least 8 characters"));
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
        .mapLeft(ex -> new ValidationError("SAVE_FAILED", "Save failed: " + ex.getMessage()));
}
```

### [Mandatory] Either API

```java
// Create Either
Either<String, Integer> right = Either.right(42);
Either<String, Integer> left = Either.left("Error");

// Transform (only affects Right)
Either<String, String> mapped = right.map(Object::toString);

// Transform Left
Either<Integer, Integer> leftMapped = left.mapLeft(String::length);

// flatMap chaining
Either<String, Integer> result = Either.right(10)
    .flatMap(i -> i > 0 ? Either.right(i * 2) : Either.left("Negative number"));

// fold - handle both cases
String result = either.fold(
    error -> "Error: " + error,
    success -> "Success: " + success
);

// getOrElse
Integer value = right.getOrElse(0);

// Check
if (right.isRight()) { ... }
if (left.isLeft()) { ... }

// Swap Left and Right
Either<Integer, String> swapped = right.swap();
```

### [Recommended] Business Validation

```java
@Service
public class OrderService {

    // ✅ Either expresses business rules
    public Either<String, Order> createOrder(OrderCreateDTO dto) {
        return validateStock(dto.getItems())
            .flatMap(items -> validateUserCredit(dto.getUserId()))
            .flatMap(credit -> validatePaymentMethod(dto.getPaymentMethod()))
            .map(payment -> buildOrder(dto))
            .flatMap(this::saveOrder);
    }

    private Either<String, List<OrderItem>> validateStock(List<OrderItemDTO> items) {
        // Stock validation logic
        return Either.right(items.stream()
            .map(OrderItem::from)
            .collect(Collectors.toList()));
    }

    private Either<String, BigDecimal> validateUserCredit(Long userId) {
        BigDecimal credit = userMapper.selectById(userId).getCredit();
        return credit.compareTo(BigDecimal.ZERO) > 0
            ? Either.right(credit)
            : Either.left("Insufficient user credit");
    }
}
```

### [Recommended] Controller Usage

```java
@RestController
public class OrderController {

    // ✅ fold() handles Either
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

## Immutable Collections (Optional)

```java
// List - Immutable, thread-safe
io.vavr.collection.List<String> list = io.vavr.collection.List.of("a", "b", "c");
io.vavr.collection.List<Integer> lengths = list.map(String::length);

// Map
io.vavr.collection.Map<String, Integer> map =
    io.vavr.collection.HashMap.of("a", 1, "b", 2);
```

---

## Checklist

**Either**:
- [ ] Business validation chains use Either.flatMap
- [ ] Controller uses fold() to handle Either
- [ ] Error types use custom classes instead of String

**Immutable Collections**:
- [ ] Shared data uses Vavr collections to ensure thread safety
- [ ] Use map/filter/sortBy chaining operations
- [ ] Convert with toJavaList() when necessary

**Pattern Matching**:
- [ ] State machines use Match + Tuple matching
- [ ] Exception handling uses Match + instanceof
- [ ] Avoid overuse (keep simple if-else as is)

**Function Composition**:
- [ ] Complex processing flows split into small functions
- [ ] Use andThen for composition instead of nested calls
- [ ] Use currying/partial application when necessary

---

## Related Standards
- [08-vavr-fundamentals.md](./08-vavr-fundamentals.md) - Option, Try Fundamentals
- [08-vavr-mybatis-integration.md](./08-vavr-mybatis-integration.md) - MyBatis Plus Integration
