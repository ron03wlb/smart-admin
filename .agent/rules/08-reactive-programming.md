---
trigger: always_on
---

# 函数式编程规范 - Vavr 最佳实践

基于 Vavr 库实现函数式编程范式，提升代码健壮性、可读性与可维护性。
适用于传统 Spring Web MVC 架构。

---

## 第一部分：Vavr 核心概念

### 【强制】Maven 依赖

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

### 为什么使用 Vavr？

- ✅ 不可变集合，线程安全
- ✅ Try/Option/Either 替代 try-catch 和 null
- ✅ 函数组合与链式调用
- ✅ 模式匹配增强可读性
- ✅ 与 Spring MVC 完美兼容

---

## 第二部分：Option - null 安全

### 【强制】基本使用

```java
// ❌ 传统方式 - null 检查繁琐
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

// ✅ Vavr Option - 优雅链式调用
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

### 【强制】Option API

```java
// 创建 Option
Option<String> some = Option.of("value");
Option<String> none = Option.of(null);          // None
Option<String> fromOptional = Option.ofOptional(optional);

// 转换
Option<Integer> length = some.map(String::length);
Option<String> upper = some.map(String::toUpperCase);

// 过滤
Option<String> filtered = some.filter(s -> s.length() > 5);

// 获取值
String value = some.getOrElse("default");
String value2 = some.getOrElse(() -> computeDefault());
String value3 = some.getOrElseThrow(() -> new RuntimeException());

// 判断
if (some.isDefined()) { ... }
if (some.isEmpty()) { ... }

// flatMap 处理嵌套
Option<String> city = Option.of(user)
    .flatMap(u -> Option.of(u.getAddress()))
    .map(Address::getCity);
```

### 【推荐】在 Service 层使用

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    // ✅ 返回 Option 代替 Optional
    public Option<User> findById(Long id) {
        return Option.of(userMapper.selectById(id));
    }

    // ✅ 链式处理
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

### 【推荐】Controller 集成

```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ✅ fold() 处理 Option
    @GetMapping("/{id}")
    public ResponseDTO<UserVO> getUser(@PathVariable Long id) {
        return userService.findById(id)
            .map(UserVO::from)
            .fold(
                () -> ResponseDTO.error("用户不存在"),
                user -> ResponseDTO.ok(user)
            );
    }

    // ✅ peek() 执行副作用
    @GetMapping("/{id}/profile")
    public ResponseDTO<UserProfileVO> getProfile(@PathVariable Long id) {
        return userService.findById(id)
            .peek(user -> log.info("访问用户档案: {}", user.getId()))
            .map(UserProfileVO::from)
            .map(ResponseDTO::ok)
            .getOrElse(() -> ResponseDTO.error("用户不存在"));
    }
}
```

---

## 第三部分：Try - 异常处理

### 【强制】基本使用

```java
// ❌ 传统方式 - 异常处理冗长
public OrderVO createOrder(OrderCreateDTO dto) {
    try {
        Order order = orderService.create(dto);
        try {
            paymentService.processPayment(order.getId());
            return OrderVO.from(order);
        } catch (PaymentException e) {
            orderService.markAsFailed(order.getId());
            throw new BusinessException("支付失败", e);
        }
    } catch (Exception e) {
        log.error("订单创建失败", e);
        throw new BusinessException("订单创建失败", e);
    }
}

// ✅ Vavr Try - 函数式异常处理
public OrderVO createOrder(OrderCreateDTO dto) {
    return Try.of(() -> orderService.create(dto))
        .andThen(order -> paymentService.processPayment(order.getId()))
        .map(OrderVO::from)
        .recover(PaymentException.class, ex -> {
            log.warn("支付失败，订单已标记失败", ex);
            throw new BusinessException("支付失败", ex);
        })
        .getOrElseThrow(ex -> new BusinessException("订单创建失败", ex));
}
```

### 【强制】Try API

```java
// 创建 Try
Try<Integer> success = Try.of(() -> 42);
Try<Integer> failure = Try.of(() -> 1 / 0);

// 转换（仅 Success 执行）
Try<String> result = success.map(Object::toString);
Try<Integer> doubled = success.map(i -> i * 2);

// mapTry 处理可能抛异常的函数
Try<String> content = Try.of(() -> Paths.get("file.txt"))
    .mapTry(Files::readString);

// 链式调用
Try<Result> result = Try.of(() -> step1())
    .mapTry(this::step2)
    .mapTry(this::step3);

// 错误恢复
Try<Integer> recovered = failure
    .recover(ArithmeticException.class, 0)
    .recover(ex -> -1);

// 错误转换
Try<Integer> mapped = failure
    .recoverWith(ex -> Try.of(() -> fallbackComputation()));

// 获取值
Integer value = success.get();                        // 成功返回值，失败抛异常
Integer value2 = success.getOrElse(0);
Integer value3 = success.getOrElseGet(ex -> computeDefault());

// 转换为 Either
Either<Throwable, Integer> either = success.toEither();

// 判断
if (success.isSuccess()) { ... }
if (failure.isFailure()) { ... }
```

### 【推荐】文件操作

```java
@Service
public class FileService {

    // ✅ Try 处理 IO 异常
    public Try<String> readFile(String path) {
        return Try.of(() -> Paths.get(path))
            .mapTry(Files::readString)
            .mapTry(String::trim);
    }

    // ✅ andThen 执行副作用
    public Try<Void> writeFile(String path, String content) {
        return Try.of(() -> Paths.get(path))
            .andThenTry(p -> Files.writeString(p, content))
            .andThen(() -> log.info("文件写入成功: {}", path));
    }

    // ✅ 组合多个操作
    public Try<FileProcessResult> processFile(String inputPath, String outputPath) {
        return readFile(inputPath)
            .mapTry(this::transform)
            .flatMapTry(content -> writeFile(outputPath, content)
                .map(v -> new FileProcessResult(inputPath, outputPath)));
    }
}
```

### 【推荐】外部 API 调用

```java
@Service
@RequiredArgsConstructor
public class ThirdPartyService {

    private final RestTemplate restTemplate;

    // ✅ Try 包装 HTTP 调用
    public Try<ApiResponse> callExternalApi(String endpoint, Object request) {
        return Try.of(() -> restTemplate.postForObject(
                endpoint,
                request,
                ApiResponse.class
            ))
            .recover(HttpClientErrorException.class, ex -> {
                log.error("API 调用失败: {} - {}", ex.getStatusCode(), ex.getMessage());
                throw new ExternalApiException("第三方服务异常", ex);
            })
            .recover(ResourceAccessException.class, ex -> {
                log.error("API 连接超时", ex);
                throw new ExternalApiException("连接超时", ex);
            });
    }

    // ✅ 重试逻辑
    public Try<ApiResponse> callWithRetry(String endpoint, Object request, int maxAttempts) {
        return retry(maxAttempts, () -> callExternalApi(endpoint, request));
    }

    private <T> Try<T> retry(int maxAttempts, Supplier<Try<T>> operation) {
        Try<T> result = operation.get();
        for (int i = 1; i < maxAttempts && result.isFailure(); i++) {
            log.info("重试第 {} 次", i);
            result = operation.get();
        }
        return result;
    }
}
```

---

## 第四部分：Either - 业务逻辑分支

### 【强制】基本使用

Either 表示两种可能的值：Left（错误）或 Right（成功）。

```java
// ✅ Either<Error, Success> - 明确的错误处理
public Either<ValidationError, User> validateAndCreateUser(UserCreateDTO dto) {
    return validateEmail(dto.getEmail())
        .flatMap(email -> validatePassword(dto.getPassword()))
        .flatMap(password -> createUser(dto));
}

private Either<ValidationError, String> validateEmail(String email) {
    if (!EMAIL_PATTERN.matcher(email).matches()) {
        return Either.left(new ValidationError("EMAIL_INVALID", "邮箱格式错误"));
    }
    return Either.right(email);
}

private Either<ValidationError, String> validatePassword(String password) {
    if (password.length() < 8) {
        return Either.left(new ValidationError("PASSWORD_TOO_SHORT", "密码至少8位"));
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
        .mapLeft(ex -> new ValidationError("SAVE_FAILED", "保存失败: " + ex.getMessage()));
}
```

### 【强制】Either API

```java
// 创建 Either
Either<String, Integer> right = Either.right(42);
Either<String, Integer> left = Either.left("Error");

// 转换（仅作用于 Right）
Either<String, String> mapped = right.map(Object::toString);

// 转换 Left
Either<Integer, Integer> leftMapped = left.mapLeft(String::length);

// flatMap 链式处理
Either<String, Integer> result = Either.right(10)
    .flatMap(i -> i > 0 ? Either.right(i * 2) : Either.left("负数"));

// fold - 处理两种情况
String result = either.fold(
    error -> "错误: " + error,
    success -> "成功: " + success
);

// getOrElse
Integer value = right.getOrElse(0);

// 判断
if (right.isRight()) { ... }
if (left.isLeft()) { ... }

// 交换 Left 和 Right
Either<Integer, String> swapped = right.swap();
```

### 【推荐】业务验证

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final InventoryService inventoryService;

    // ✅ Either 表达业务规则
    public Either<String, Order> createOrder(OrderCreateDTO dto) {
        return validateStock(dto.getItems())
            .flatMap(items -> validateUserCredit(dto.getUserId()))
            .flatMap(credit -> validatePaymentMethod(dto.getPaymentMethod()))
            .map(payment -> buildOrder(dto))
            .flatMap(this::saveOrder);
    }

    private Either<String, List<OrderItem>> validateStock(List<OrderItemDTO> items) {
        for (OrderItemDTO item : items) {
            if (!inventoryService.hasStock(item.getProductId(), item.getQuantity())) {
                return Either.left("商品 " + item.getProductId() + " 库存不足");
            }
        }
        return Either.right(items.stream()
            .map(OrderItem::from)
            .collect(Collectors.toList()));
    }

    private Either<String, BigDecimal> validateUserCredit(Long userId) {
        BigDecimal credit = userMapper.selectById(userId).getCredit();
        return credit.compareTo(BigDecimal.ZERO) > 0
            ? Either.right(credit)
            : Either.left("用户信用额度不足");
    }

    private Either<String, PaymentMethod> validatePaymentMethod(String method) {
        return Try.of(() -> PaymentMethod.valueOf(method))
            .toEither()
            .mapLeft(ex -> "不支持的支付方式: " + method);
    }

    private Either<String, Order> saveOrder(Order order) {
        return Try.of(() -> {
                orderMapper.insert(order);
                return order;
            })
            .toEither()
            .mapLeft(ex -> "订单保存失败: " + ex.getMessage());
    }
}
```

### 【推荐】Controller 使用

```java
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // ✅ fold() 处理 Either
    @PostMapping
    public ResponseDTO<OrderVO> createOrder(@Valid @RequestBody OrderCreateDTO dto) {
        return orderService.createOrder(dto)
            .fold(
                error -> ResponseDTO.error(error),
                order -> ResponseDTO.ok(OrderVO.from(order))
            );
    }

    // ✅ map() 转换成功值
    @PostMapping("/batch")
    public ResponseDTO<List<OrderVO>> batchCreateOrders(
            @Valid @RequestBody List<OrderCreateDTO> dtos) {

        List<OrderVO> successOrders = dtos.stream()
            .map(orderService::createOrder)
            .filter(Either::isRight)
            .map(Either::get)
            .map(OrderVO::from)
            .collect(Collectors.toList());

        return ResponseDTO.ok(successOrders);
    }
}
```

---

## 第五部分：不可变集合

### 【强制】基本使用

Vavr 集合都是不可变的、线程安全的。

```java
// List
io.vavr.collection.List<String> list = io.vavr.collection.List.of("a", "b", "c");
io.vavr.collection.List<String> list2 = io.vavr.collection.List.ofAll(javaList);

// 转换（返回新 List）
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

### 【推荐】Service 层使用

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    // ✅ 返回不可变 List
    public io.vavr.collection.List<UserVO> getActiveUsers() {
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

    // ✅ groupBy 分组
    public io.vavr.collection.Map<String, io.vavr.collection.List<UserVO>>
            getUsersByDepartment() {
        return io.vavr.collection.List.ofAll(userMapper.selectAll())
            .groupBy(User::getDepartment)
            .mapValues(users -> users.map(UserVO::from));
    }

    // ✅ 去重
    public io.vavr.collection.Set<String> getUniqueRoles() {
        return io.vavr.collection.List.ofAll(userMapper.selectAll())
            .flatMap(user -> io.vavr.collection.List.ofAll(user.getRoles()))
            .toSet();
    }
}
```

### 【推荐】线程安全场景

```java
@Component
public class CategoryCache {

    // ✅ 不可变集合，线程安全
    private volatile io.vavr.collection.List<Category> cache =
        io.vavr.collection.List.empty();

    @Scheduled(fixedRate = 300000) // 5分钟刷新
    public void refresh() {
        io.vavr.collection.List<Category> newCache =
            io.vavr.collection.List.ofAll(categoryRepository.findAll())
                .filter(Category::isActive)
                .sortBy(Category::getOrder);

        // 原子替换
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

## 第六部分：模式匹配

### 【强制】基本使用

```java
import static io.vavr.API.*;
import static io.vavr.Predicates.*;

// ✅ 值匹配
public String processOrderStatus(OrderStatus status) {
    return Match(status).of(
        Case($(OrderStatus.PENDING), "等待支付"),
        Case($(OrderStatus.PAID), "已支付"),
        Case($(OrderStatus.SHIPPED), "已发货"),
        Case($(OrderStatus.COMPLETED), "已完成"),
        Case($(OrderStatus.CANCELLED), "已取消"),
        Case($(), s -> {
            log.warn("未知订单状态: {}", s);
            return "未知状态";
        })
    );
}

// ✅ 类型匹配
public String handleResult(Object result) {
    return Match(result).of(
        Case($(instanceOf(String.class)), s -> "字符串: " + s),
        Case($(instanceOf(Integer.class)), i -> "数字: " + i),
        Case($(instanceOf(List.class)), list -> "列表大小: " + list.size()),
        Case($(), obj -> "未知类型: " + obj.getClass().getSimpleName())
    );
}

// ✅ 条件匹配
public String classifyAge(int age) {
    return Match(age).of(
        Case($(n -> n < 18), "未成年"),
        Case($(n -> n >= 18 && n < 60), "成年"),
        Case($(n -> n >= 60), "老年"),
        Case($(), "无效年龄")
    );
}
```

### 【推荐】业务逻辑

```java
@Service
public class OrderStateMachine {

    // ✅ 状态转换
    public OrderVO processAction(Order order, OrderAction action) {
        Order updated = Match(Tuple.of(order.getStatus(), action)).of(
            // PENDING 状态可支付
            Case($Tuple2($(OrderStatus.PENDING), $(OrderAction.PAY)),
                () -> handlePayment(order)),

            // PAID 状态可发货
            Case($Tuple2($(OrderStatus.PAID), $(OrderAction.SHIP)),
                () -> handleShipping(order)),

            // SHIPPED 状态可确认收货
            Case($Tuple2($(OrderStatus.SHIPPED), $(OrderAction.CONFIRM)),
                () -> handleConfirmation(order)),

            // PENDING/PAID 可取消
            Case($Tuple2($(isIn(OrderStatus.PENDING, OrderStatus.PAID)), $(OrderAction.CANCEL)),
                () -> handleCancellation(order)),

            // 默认：无效转换
            Case($(), () -> {
                throw new IllegalStateException(
                    String.format("无效的状态转换: %s -> %s", order.getStatus(), action)
                );
            })
        );

        return OrderVO.from(updated);
    }

    private Order handlePayment(Order order) {
        // 支付逻辑
        order.setStatus(OrderStatus.PAID);
        orderMapper.updateById(order);
        return order;
    }

    // ... 其他处理方法
}
```

### 【推荐】异常处理

```java
public ResponseDTO<?> handleException(Exception ex) {
    return Match(ex).of(
        Case($(instanceOf(UserNotFoundException.class)),
            e -> ResponseDTO.error(404, "用户不存在")),

        Case($(instanceOf(ValidationException.class)),
            e -> ResponseDTO.error(400, e.getMessage())),

        Case($(instanceOf(DataIntegrityViolationException.class)),
            e -> ResponseDTO.error(409, "数据冲突")),

        Case($(instanceOf(Exception.class)),
            e -> {
                log.error("未处理的异常", e);
                return ResponseDTO.error(500, "服务器错误");
            })
    );
}
```

---

## 第七部分：函数组合

### 【强制】Function 接口

```java
import io.vavr.Function1;
import io.vavr.Function2;

// Function1: 一个参数
Function1<String, String> normalizeEmail =
    email -> email.toLowerCase().trim();

Function1<String, Option<String>> validateEmail =
    email -> EMAIL_PATTERN.matcher(email).matches()
        ? Option.of(email)
        : Option.none();

Function1<String, Try<User>> findUserByEmail =
    email -> Try.of(() -> userRepository.findByEmail(email)
        .orElseThrow(() -> new UserNotFoundException(email)));

// Function2: 两个参数
Function2<String, String, String> concat =
    (a, b) -> a + b;

Function2<Integer, Integer, Integer> add =
    (a, b) -> a + b;
```

### 【推荐】函数组合

```java
@Service
public class UserProcessor {

    private final Function1<String, String> normalizeEmail =
        email -> email.toLowerCase().trim();

    private final Function1<String, Option<String>> validateEmail =
        email -> EMAIL_PATTERN.matcher(email).matches()
            ? Option.of(email)
            : Option.none();

    private final Function1<String, Try<User>> findUserByEmail =
        email -> Try.of(() -> userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException(email)));

    // ✅ andThen 组合
    public Try<User> processUserLogin(String rawEmail) {
        return normalizeEmail
            .andThen(validateEmail)
            .andThen(opt -> opt.toTry(() -> new IllegalArgumentException("邮箱无效")))
            .andThen(findUserByEmail)
            .apply(rawEmail);
    }

    // ✅ compose 反向组合
    Function1<String, Integer> length = String::length;
    Function1<Integer, String> format = i -> "长度: " + i;
    Function1<String, String> combined = format.compose(length);
    // combined.apply("hello") => "长度: 5"
}
```

### 【推荐】偏函数应用

```java
// ✅ 柯里化
Function2<Integer, Integer, Integer> add = (a, b) -> a + b;
Function1<Integer, Integer> add5 = add.curried().apply(5);

int result = add5.apply(10); // 15

// ✅ 部分应用
Function1<Integer, Integer> increment = add.apply(1);
int result2 = increment.apply(10); // 11
```

---

## 第八部分：与 MyBatis Plus 整合

### 【强制】Mapper 查询包装

```java
@Mapper
public interface UserMapper extends BaseMapper<User> {

    // ✅ 默认方法返回 Option
    default Option<User> findByIdSafe(Long id) {
        return Option.of(selectById(id));
    }

    default Option<User> findByEmail(String email) {
        return Option.of(selectOne(
            Wrappers.<User>lambdaQuery()
                .eq(User::getEmail, email)
        ));
    }

    // ✅ 返回不可变 List
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

### 【推荐】Service 层集成

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    // ✅ Option 包装查询
    public Option<User> findById(Long id) {
        return userMapper.findByIdSafe(id);
    }

    // ✅ Try 包装写操作
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

    // ✅ Either 业务验证 + 写操作
    public Either<String, User> registerUser(UserRegisterDTO dto) {
        return validateEmail(dto.getEmail())
            .flatMap(email -> validatePassword(dto.getPassword()))
            .flatMap(password -> checkEmailNotExists(dto.getEmail()))
            .flatMap(email -> createUser(dto).toEither()
                .mapLeft(ex -> "注册失败: " + ex.getMessage()));
    }

    // ✅ 批量操作
    public io.vavr.collection.List<User> batchCreate(
            List<UserCreateDTO> dtos) {
        return io.vavr.collection.List.ofAll(dtos)
            .map(this::createUser)
            .filter(Try::isSuccess)
            .map(Try::get);
    }

    private Either<String, String> checkEmailNotExists(String email) {
        return userMapper.findByEmail(email).isDefined()
            ? Either.left("邮箱已存在")
            : Either.right(email);
    }
}
```

### 【推荐】事务处理

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
}
```

---

## 第九部分：Java Stream API 规范（保留）

### 【强制】基本使用原则

#### 1. Stream 只能消费一次

```java
// ❌ 错误 - IllegalStateException
Stream<String> stream = list.stream();
long count = stream.count();
List<String> result = stream.collect(toList()); // 异常！

// ✅ 正确 - 每次创建新 Stream
long count = list.stream().count();
List<String> result = list.stream().collect(toList());
```

#### 2. 禁止修改源集合

```java
// ❌ 严禁 - ConcurrentModificationException
list.stream()
    .filter(s -> s.equals("b"))
    .forEach(s -> list.remove(s));

// ✅ 正确 - 收集到新集合
List<String> filtered = list.stream()
    .filter(s -> !s.equals("b"))
    .collect(toList());
```

#### 3. 优先使用方法引用

```java
// ✅ 推荐
list.stream()
    .map(User::getName)
    .filter(Objects::nonNull)
    .forEach(System.out::println);
```

### 【强制】性能优化

#### 1. 使用原始类型 Stream 避免 Boxing

```java
// ❌ 低效 - 自动装箱拆箱
int sum = list.stream()
    .map(User::getAge)
    .reduce(0, Integer::sum);

// ✅ 高效 - IntStream
int sum = list.stream()
    .mapToInt(User::getAge)
    .sum();
```

#### 2. 短路操作优先

```java
// ✅ 正确 - 找到即停止
Optional<User> admin = users.stream()
    .filter(u -> "ADMIN".equals(u.getRole()))
    .findFirst();

// ✅ 正确 - anyMatch 短路
boolean hasAdmin = users.stream()
    .anyMatch(u -> "ADMIN".equals(u.getRole()));
```

#### 3. 并行流谨慎使用

```java
// ✅ 正确 - 线程安全收集器
List<Integer> results = IntStream.range(0, 1000)
    .parallel()
    .boxed()
    .collect(toList());
```

**并行流适用条件**:
- 数据量大（> 10000）
- 无共享可变状态
- CPU 密集型操作

---

## 检查清单

**Vavr 核心**:
- ✅ Option 替代 null 检查
- ✅ Try 替代 try-catch
- ✅ Either 表达业务分支
- ✅ 不可变集合保证线程安全
- ✅ 模式匹配简化条件逻辑
- ✅ 函数组合提升可读性

**MyBatis Plus 整合**:
- ✅ Mapper 返回 Option/List
- ✅ Service 使用 Try/Either
- ✅ @Transactional 与 Try 配合

**Stream API**:
- ✅ Stream 只消费一次
- ✅ 不修改源集合
- ✅ mapToInt 避免 Boxing
- ✅ anyMatch/findFirst 短路操作
