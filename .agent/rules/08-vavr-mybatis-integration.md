---
trigger: always_on
description: Vavr 與 MyBatis Plus 整合
tags: [vavr, mybatis-plus, integration, functional-programming]
positioning: ideal
prerequisites: [rules/08-vavr-fundamentals.md, rules/09-mybatis-plus-core.md]
related_rules: [rules/05-postgresql-mybatis-integration.md, rules/09-mybatis-plus-postgresql.md]
last_updated: 2025-01-12
---

# Vavr 與 MyBatis Plus 整合

> **TL;DR**: 在 MyBatis Plus 數據訪問層整合 Vavr，使用 Option 包裝查詢、Try 處理事務、Either 表達業務邏輯，提升代碼健壯性。

**定位說明**: 本規範定義 Vavr 函數式編程與 MyBatis Plus 整合的理想模式，以及從 Java Stream API 遷移的指南。

**前置依賴**:
- [08-vavr-fundamentals.md](./08-vavr-fundamentals.md) - Vavr Option/Try 基礎
- [09-mybatis-plus-core.md](./09-mybatis-plus-core.md) - MyBatis Plus 核心用法

---

## 目錄
- [Mapper 查詢包裝](#mapper-查詢包裝)
- [Service 層集成](#service-層集成)
- [事務處理](#事務處理)
- [Java Stream API 規範](#java-stream-api-規範)

---

## Mapper 查詢包裝

### 【強制】默認方法返回 Option

```java
@Mapper
public interface UserMapper extends BaseMapper<User> {

    // ✅ 默認方法返回 Option
    default Option<User> findByIdSafe(Long id) {
        return Option.of(selectById(id));
    }

    default Option<User> findByEmail(String email) {
        return Option.of(selectOne(
            Wrappers.<User>lambdaQuery()
                .eq(User::getEmail, email)
        ));
    }

    // ✅ 返回不可變 List
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

## Service 層集成

### 【推薦】Option 包裝查詢

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    // ✅ Option 包裝查詢
    public Option<User> findById(Long id) {
        return userMapper.findByIdSafe(id);
    }

    // ✅ Try 包裝寫操作
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

    // ✅ Either 業務驗證 + 寫操作
    public Either<String, User> registerUser(UserRegisterDTO dto) {
        return validateEmail(dto.getEmail())
            .flatMap(email -> validatePassword(dto.getPassword()))
            .flatMap(password -> checkEmailNotExists(dto.getEmail()))
            .flatMap(email -> createUser(dto).toEither()
                .mapLeft(ex -> "註冊失敗: " + ex.getMessage()));
    }

    // ✅ 批量操作
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
            : Either.left("郵箱格式錯誤");
    }

    private Either<String, String> validatePassword(String password) {
        return password.length() >= 8
            ? Either.right(password)
            : Either.left("密碼至少8位");
    }

    private Either<String, String> checkEmailNotExists(String email) {
        return userMapper.findByEmail(email).isDefined()
            ? Either.left("郵箱已存在")
            : Either.right(email);
    }
}
```

---

## 事務處理

### 【推薦】Try + @Transactional

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
        // 驗證邏輯
        return Either.right(dto);
    }

    private Either<String, OrderCreateDTO> reserveInventory(OrderCreateDTO dto) {
        // 庫存預留
        return Either.right(dto);
    }

    private Either<String, Order> saveOrder(OrderCreateDTO dto) {
        return Try.of(() -> {
                Order order = buildOrder(dto);
                orderMapper.insert(order);
                return order;
            })
            .toEither()
            .mapLeft(ex -> "訂單保存失敗: " + ex.getMessage());
    }

    private Either<String, Order> processPayment(Order order) {
        return Try.of(() -> {
                paymentService.process(order.getId());
                order.setStatus(OrderStatus.PAID);
                orderMapper.updateById(order);
                return order;
            })
            .toEither()
            .mapLeft(ex -> "支付處理失敗: " + ex.getMessage());
    }
}
```

---

## Java Stream API 規範

### 【強制】基本使用原則

#### 1. Stream 只能消費一次

```java
// ❌ 錯誤 - IllegalStateException
Stream<String> stream = list.stream();
long count = stream.count();
List<String> result = stream.collect(toList()); // 異常！

// ✅ 正確 - 每次創建新 Stream
long count = list.stream().count();
List<String> result = list.stream().collect(toList());
```

#### 2. 禁止修改源集合

```java
// ❌ 嚴禁 - ConcurrentModificationException
list.stream()
    .filter(s -> s.equals("b"))
    .forEach(s -> list.remove(s));

// ✅ 正確 - 收集到新集合
List<String> filtered = list.stream()
    .filter(s -> !s.equals("b"))
    .collect(toList());
```

#### 3. 優先使用方法引用

```java
// ✅ 推薦
list.stream()
    .map(User::getName)
    .filter(Objects::nonNull)
    .forEach(System.out::println);
```

### 【強制】性能優化

#### 1. 使用原始類型 Stream 避免 Boxing

```java
// ❌ 低效 - 自動裝箱拆箱
int sum = list.stream()
    .map(User::getAge)
    .reduce(0, Integer::sum);

// ✅ 高效 - IntStream
int sum = list.stream()
    .mapToInt(User::getAge)
    .sum();
```

#### 2. 短路操作優先

```java
// ✅ 正確 - 找到即停止
Optional<User> admin = users.stream()
    .filter(u -> "ADMIN".equals(u.getRole()))
    .findFirst();

// ✅ 正確 - anyMatch 短路
boolean hasAdmin = users.stream()
    .anyMatch(u -> "ADMIN".equals(u.getRole()));
```

#### 3. 並行流謹慎使用

```java
// ✅ 正確 - 線程安全收集器
List<Integer> results = IntStream.range(0, 1000)
    .parallel()
    .boxed()
    .collect(toList());
```

**並行流適用條件**:
- 數據量大（> 10000）
- 無共享可變狀態
- CPU 密集型操作

### 【推薦】從 Stream API 遷移至 Vavr

```java
// Java Stream API (傳統方式)
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

// Vavr (理想架構)
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

**Vavr 優勢**:
- 不可變集合，線程安全
- 鏈式操作更自然
- 與 Option/Try/Either 無縫整合
- 無需 collect() 終止操作

---

## 檢查清單

**Mapper 層**:
- [ ] 自定義 default 方法返回 Option
- [ ] 批量查詢返回 Vavr List
- [ ] 避免在 Mapper 內部使用 Try/Either

**Service 層**:
- [ ] 查詢操作使用 Option 包裝
- [ ] 寫操作使用 Try 包裝
- [ ] 複雜業務驗證使用 Either
- [ ] 批量操作使用 Vavr 集合 map/filter

**事務處理**:
- [ ] @Transactional 與 Try 配合使用
- [ ] 確保 rollbackFor = Exception.class
- [ ] Either 錯誤訊息明確具體

**遷移策略**:
- [ ] 新代碼優先使用 Vavr
- [ ] 現有 Stream API 代碼逐步重構
- [ ] 保持向後兼容（必要時使用 toJavaList()）

---

## 相關規範
- [08-vavr-fundamentals.md](./08-vavr-fundamentals.md) - Option、Try 基礎
- [08-vavr-advanced.md](./08-vavr-advanced.md) - Either、集合、模式匹配
- [09-mybatis-plus-core.md](./09-mybatis-plus-core.md) - MyBatis Plus 核心用法
- [09-mybatis-plus-postgresql.md](./09-mybatis-plus-postgresql.md) - PostgreSQL 整合
- [05-postgresql-mybatis-integration.md](./05-postgresql-mybatis-integration.md) - SQL 優化
