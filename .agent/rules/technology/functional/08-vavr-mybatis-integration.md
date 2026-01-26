---
trigger: always_on
description: Vavr and MyBatis Plus Integration
tags: [vavr, mybatis-plus, integration, functional-programming]
positioning: ideal
prerequisites:
  - technology/functional/08-vavr-fundamentals.md
  - technology/database/09-mybatis-plus-core.md
related_rules:
  - technology/database/05-postgresql-mybatis.md
last_updated: 2025-01-12
---

# Vavr and MyBatis Plus Integration

> **TL;DR**: Integrate Vavr into MyBatis Plus data access layer using Option to wrap queries, Try to handle transactions, and Either to express business logic, improving code robustness.

**Positioning**: This specification defines ideal patterns for integrating Vavr functional programming with MyBatis Plus, and provides migration guidance from Java Stream API.

**Prerequisites**:
- [08-vavr-fundamentals.md](./08-vavr-fundamentals.md) - Vavr Option/Try fundamentals
- [09-mybatis-plus-core.md](./09-mybatis-plus-core.md) - MyBatis Plus core usage

---

## Table of Contents
- [Mapper Query Wrapping](#mapper-query-wrapping)
- [Service Layer Integration](#service-layer-integration)
- [Transaction Handling](#transaction-handling)
- [Java Stream API Standards](#java-stream-api-standards)

---

## Mapper Query Wrapping

### [Mandatory] Default Methods Return Option

```java
@Mapper
public interface UserMapper extends BaseMapper<User> {

    // ✅ Default methods return Option
    default Option<User> findByIdSafe(Long id) {
        return Option.of(selectById(id));
    }

    default Option<User> findByEmail(String email) {
        return Option.of(selectOne(
            Wrappers.<User>lambdaQuery()
                .eq(User::getEmail, email)
        ));
    }

    // ✅ Return immutable List
    default io.vavr.collection.List<User> findAllActive() {
        return io.vavr.collection.List.ofAll(
            selectList(
                Wrappers.<User>lambdaQuery()
                    .eq(User::getStatus, "ACTIVE")
            )
        );
    }
}
```

---

## Service Layer Integration

### [Recommended] Option Wrapping Queries

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    // ✅ Option wrapping queries
    public Option<User> findById(Long id) {
        return userMapper.findByIdSafe(id);
    }

    // ✅ Try wrapping write operations
    public Try<User> createUser(UserCreateDTO dto) {
        return Try.of(() -> {
            User user = User.builder()
                .email(dto.getEmail())
                .name(dto.getName())
                .password(passwordEncoder.encode(dto.getPassword()))
                .build();
            userMapper.insert(user);
            return user;
        });
    }

    // ✅ Either for business validation + write operations
    public Either<String, User> registerUser(UserRegisterDTO dto) {
        return validateEmail(dto.getEmail())
            .flatMap(email -> validatePassword(dto.getPassword()))
            .flatMap(password -> checkEmailNotExists(dto.getEmail()))
            .flatMap(email -> createUser(dto).toEither()
                .mapLeft(ex -> "Registration failed: " + ex.getMessage()));
    }

    // ✅ Batch operations
    public io.vavr.collection.List<User> batchCreate(
            List<UserCreateDTO> dtos) {
        return io.vavr.collection.List.ofAll(dtos)
            .map(this::createUser)
            .filter(Try::isSuccess)
            .map(Try::get);
    }

    private Either<String, String> validateEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches()
            ? Either.right(email)
            : Either.left("Invalid email format");
    }

    private Either<String, String> validatePassword(String password) {
        return password.length() >= 8
            ? Either.right(password)
            : Either.left("Password must be at least 8 characters");
    }

    private Either<String, String> checkEmailNotExists(String email) {
        return userMapper.findByEmail(email).isDefined()
            ? Either.left("Email already exists")
            : Either.right(email);
    }
}
```

---

## Transaction Handling

### [Recommended] Try + @Transactional

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final PaymentService paymentService;

    // ✅ Try + @Transactional
    @Transactional(rollbackFor = Exception.class)
    public Try<Order> createOrderWithPayment(OrderCreateDTO dto) {
        return Try.of(() -> buildOrder(dto))
            .andThenTry(order -> orderMapper.insert(order))
            .flatMapTry(order ->
                paymentService.processPayment(order.getId())
                    .map(payment -> {
                        order.setStatus(OrderStatus.PAID);
                        orderMapper.updateById(order);
                        return order;
                    })
            );
    }

    // ✅ Either + @Transactional
    @Transactional(rollbackFor = Exception.class)
    public Either<String, Order> placeOrder(OrderCreateDTO dto) {
        return validateOrder(dto)
            .flatMap(this::reserveInventory)
            .flatMap(this::saveOrder)
            .flatMap(this::processPayment);
    }

    private Either<String, OrderCreateDTO> validateOrder(OrderCreateDTO dto) {
        // Validation logic
        return Either.right(dto);
    }

    private Either<String, OrderCreateDTO> reserveInventory(OrderCreateDTO dto) {
        // Inventory reservation
        return Either.right(dto);
    }

    private Either<String, Order> saveOrder(OrderCreateDTO dto) {
        return Try.of(() -> {
                Order order = buildOrder(dto);
                orderMapper.insert(order);
                return order;
            })
            .toEither()
            .mapLeft(ex -> "Order save failed: " + ex.getMessage());
    }

    private Either<String, Order> processPayment(Order order) {
        return Try.of(() -> {
                paymentService.process(order.getId());
                order.setStatus(OrderStatus.PAID);
                orderMapper.updateById(order);
                return order;
            })
            .toEither()
            .mapLeft(ex -> "Payment processing failed: " + ex.getMessage());
    }
}
```

---

## Java Stream API Standards

### [Mandatory] Basic Usage Principles

#### 1. Stream Can Only Be Consumed Once

```java
// ❌ Wrong - IllegalStateException
Stream<String> stream = list.stream();
long count = stream.count();
List<String> result = stream.collect(toList()); // Exception!

// ✅ Correct - Create new Stream each time
long count = list.stream().count();
List<String> result = list.stream().collect(toList());
```

#### 2. Prohibit Modifying Source Collection

```java
// ❌ Forbidden - ConcurrentModificationException
list.stream()
    .filter(s -> s.equals("b"))
    .forEach(s -> list.remove(s));

// ✅ Correct - Collect to new collection
List<String> filtered = list.stream()
    .filter(s -> !s.equals("b"))
    .collect(toList());
```

#### 3. Prefer Method References

```java
// ✅ Recommended
list.stream()
    .map(User::getName)
    .filter(Objects::nonNull)
    .forEach(System.out::println);
```

### [Mandatory] Performance Optimization

#### 1. Use Primitive Type Streams to Avoid Boxing

```java
// ❌ Inefficient - Auto-boxing/unboxing
int sum = list.stream()
    .map(User::getAge)
    .reduce(0, Integer::sum);

// ✅ Efficient - IntStream
int sum = list.stream()
    .mapToInt(User::getAge)
    .sum();
```

#### 2. Short-Circuit Operations First

```java
// ✅ Correct - Stop when found
Optional<User> admin = users.stream()
    .filter(u -> "ADMIN".equals(u.getRole()))
    .findFirst();

// ✅ Correct - anyMatch short-circuits
boolean hasAdmin = users.stream()
    .anyMatch(u -> "ADMIN".equals(u.getRole()));
```

#### 3. Use Parallel Streams Cautiously

```java
// ✅ Correct - Thread-safe collector
List<Integer> results = IntStream.range(0, 1000)
    .parallel()
    .boxed()
    .collect(toList());
```

**Parallel Stream Applicable Conditions**:
- Large data volume (> 10000)
- No shared mutable state
- CPU-intensive operations

### [Recommended] Migrating from Stream API to Vavr

```java
// Java Stream API (traditional approach)
public List<UserVO> getActiveUsers_StreamAPI() {
    return userMapper.selectList(
            Wrappers.<User>lambdaQuery()
                .eq(User::getStatus, "ACTIVE")
        )
        .stream()
        .filter(User::isVerified)
        .sorted(Comparator.comparing(User::getCreatedAt))
        .map(UserVO::from)
        .collect(Collectors.toList());
}

// Vavr (ideal architecture)
public io.vavr.collection.List<UserVO> getActiveUsers_Vavr() {
    return io.vavr.collection.List.ofAll(
            userMapper.selectList(
                Wrappers.<User>lambdaQuery()
                    .eq(User::getStatus, "ACTIVE")
            )
        )
        .filter(User::isVerified)
        .sortBy(User::getCreatedAt)
        .map(UserVO::from);
}
```

**Vavr Advantages**:
- Immutable collections, thread-safe
- More natural method chaining
- Seamless integration with Option/Try/Either
- No need for collect() terminal operation

---

## Checklist

**Mapper Layer**:
- [ ] Custom default methods return Option
- [ ] Batch queries return Vavr List
- [ ] Avoid using Try/Either inside Mapper

**Service Layer**:
- [ ] Query operations use Option wrapping
- [ ] Write operations use Try wrapping
- [ ] Complex business validation uses Either
- [ ] Batch operations use Vavr collection map/filter

**Transaction Handling**:
- [ ] @Transactional works with Try
- [ ] Ensure rollbackFor = Exception.class
- [ ] Either error messages are clear and specific

**Migration Strategy**:
- [ ] New code prioritizes Vavr
- [ ] Gradually refactor existing Stream API code
- [ ] Maintain backward compatibility (use toJavaList() when necessary)

---

## Related Specifications
- [08-vavr-fundamentals.md](./08-vavr-fundamentals.md) - Option, Try fundamentals
- [08-vavr-advanced.md](./08-vavr-advanced.md) - Either, collections, pattern matching
- [09-mybatis-plus-core.md](../database/09-mybatis-plus-core.md) - MyBatis Plus core usage
- [05-postgresql-mybatis.md](../database/05-postgresql-mybatis.md) - Complete PostgreSQL + MyBatis Plus Integration
