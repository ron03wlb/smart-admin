# Vavr Refactoring Assistant - Quick Reference

**Version**: 1.0.0  
**Last Updated**: 2026-02-02  
**Skill**: vavr-refactoring-assistant (P0 - Critical)

---

## Quick Patterns

### Option vs Optional

// ❌ OLD - java.util.Optional
public Optional<User> findById(Long id) {
    return userDao.selectById(id);
}

// ✅ NEW - io.vavr.control.Option
public Option<User> findById(Long id) {
    return Option.ofOptional(userDao.selectById(id));
}

### Try for Exceptions

// ❌ OLD - try-catch
try {
    return processPayment(form);
} catch (Exception e) {
    log.error("Payment failed", e);
    return ResponseDTO.error(ErrorCode.PAYMENT_FAILED);
}

// ✅ NEW - io.vavr.control.Try
return Try.of(() -> processPayment(form))
    .onFailure(e -> log.error("Payment failed", e))
    .fold(
        error -> ResponseDTO.error(ErrorCode.PAYMENT_FAILED),
        result -> ResponseDTO.ok(result)
    );

### Either for Business Errors

// ❌ OLD - throw exceptions
if (!hasPermission) {
    throw new BusinessException("No permission");
}

// ✅ NEW - io.vavr.control.Either
return hasPermission
    ? Either.right(user)
    : Either.left(ErrorCode.NO_PERMISSION);

---

## 3 Core Refactoring Patterns

### 1. Option (Null Safety)
- Replace Optional<T> with Option<T>
- Use .map(), .flatMap(), .getOrElse()

### 2. Try (Exception Handling)
- Replace try-catch with Try.of()
- Use .fold() for result handling

### 3. Either (Business Logic)
- Left = Error, Right = Success
- Replace custom Result<T> with Either<L, R>

---

## Command Reference

### Dependency

<dependency>
    <groupId>io.vavr</groupId>
    <artifactId>vavr</artifactId>
    <version>0.10.4</version>
</dependency>

### Imports

import io.vavr.control.Option;
import io.vavr.control.Try;
import io.vavr.control.Either;

---

## Common Refactoring

### Refactor 1: Service Layer Optional → Option
Find: Optional<User>
Replace: Option<User>

### Refactor 2: Try-catch → Try
Find: try { ... } catch
Replace: Try.of(() -> ...)

### Refactor 3: Custom Result → Either
Find: Result<T>
Replace: Either<ErrorCode, T>

---

See SKILL.md for complete refactoring guide.
