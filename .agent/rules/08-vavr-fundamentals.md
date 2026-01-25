---
trigger: always_on
description: Vavr Fundamentals - Option and Try
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

# Vavr Functional Programming Fundamentals - Option and Try

> **TL;DR**: Use Vavr Option to replace null checks, use Try to replace try-catch, achieving elegant functional exception handling.

**Positioning Statement**: This specification defines the ideal target architecture, guiding the project from traditional Java null/exception handling to Vavr functional patterns.

---

## 🤖 AI Instructions Block

### When to Apply This Rule
- ✅ User requests to generate Service layer code
- ✅ Service method needs to return potentially null objects
- ✅ Code needs to handle operations that may throw exceptions (IO, network, external API)
- ✅ Code review finds `Optional` or `try-catch` usage
- ✅ User asks how to handle null or exceptions

### Mandatory Enforcement Checklist
When generating or reviewing Service layer code, must confirm:
- [ ] Service method return type is `Option<T>` not `Optional<T>` or `T`
- [ ] No explicit null checks (`if (obj == null)` or `obj != null`)
- [ ] Exception handling uses `Try.of()` instead of `try-catch` blocks
- [ ] No using Option in Controller parameters (should use primitive types)
- [ ] Use `map()`/`flatMap()` for method chaining instead of nested if

### AI Decision Tree
```
User request: "query user" / "get order"
  ├─ Return single object?
  │   └─ YES → Use Option<Entity>
  │       ├─ Repository returns possibly null?
  │       │   └─ Use: Option.of(repository.selectById(id))
  │       └─ Repository returns Optional?
  │           └─ Use: Option.ofOptional(repository.findById(id))
  │
  ├─ May throw exception?
  │   └─ YES → Use Try<Entity>
  │       ├─ IO operation (file, network)?
  │       │   └─ Use: Try.of(() -> ...).mapTry(...)
  │       ├─ External API call?
  │       │   └─ Use: Try.of(() -> ...).recover(...)
  │       └─ Database operation?
  │           └─ Use: Try.of(() -> ...).onFailure(log::error)
  │
  └─ Need nested checks? (user → address → city)
      └─ Use flatMap chaining
          Option.of(user)
            .flatMap(u -> Option.of(u.getAddress()))
            .map(Address::getCity)
```

### Error Pattern Detection and Auto-Fix

#### Pattern 1: Detect Optional return type
```java
// ❌ Detected error
public Optional<User> findById(Long id) {
    return userMapper.findById(id);
}

// ✅ Auto-fix to
public Option<User> findById(Long id) {
    return Option.ofOptional(userMapper.findById(id));
}
// Or (if Mapper returns possibly null)
public Option<User> findById(Long id) {
    return Option.of(userMapper.selectById(id));
}
```

#### Pattern 2: Detect null checks
```java
// ❌ Detected error
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

// ✅ Auto-fix to
public UserVO getUserCity(Long id) {
    return Option.of(userRepository.findById(id))
        .map(user -> new UserVO(
            user.getName(),
            Option.of(user.getAddress())
                .map(Address::getCity)
                .getOrElse("Unknown")
        ))
        .getOrElseThrow(() -> new NotFoundException("User not found"));
}
```

#### Pattern 3: Detect try-catch
```java
// ❌ Detected error
public String readConfig(String path) {
    try {
        return Files.readString(Paths.get(path));
    } catch (IOException e) {
        log.error("Config read failed", e);
        return "default-config";
    }
}

// ✅ Auto-fix to
public Try<String> readConfig(String path) {
    return Try.of(() -> Files.readString(Paths.get(path)))
        .onFailure(e -> log.error("Config read failed", e));
}
// Controller layer handles:
// readConfig(path).getOrElse("default-config")
```

#### Pattern 4: Detect Controller parameter using Option (wrong)
```java
// ❌ Detected error
@GetMapping("/{id}")
public ResponseDTO<UserVO> getUser(Option<Long> id) { // ❌ Wrong usage
    ...
}

// ✅ Fix to
@GetMapping("/{id}")
public ResponseDTO<UserVO> getUser(@PathVariable Long id) { // ✅ Correct
    return userService.findById(id)
        .map(UserVO::from)
        .map(ResponseDTO::ok)
        .getOrElse(() -> ResponseDTO.error("User not found"));
}
```

### Code Generation Standard Templates

```java
// Service layer query
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

// Controller layer handles Option
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService service;

    @GetMapping("/{id}")
    public ResponseDTO<UserVO> getUser(@PathVariable Long id) {
        return service.findById(id)
            .map(UserVO::from)
            .fold(() -> ResponseDTO.error("Not found"), ResponseDTO::ok);
    }
}

// Try exception handling
public Try<String> readFile(String path) {
    return Try.of(() -> Files.readString(Paths.get(path)))
        .onFailure(e -> log.error("File read failed", e));
}
```

### Validation Commands
```bash
mvn test -Dtest=ArchitectureTest#serviceUsesVavrOption
```

---

---

## Maven Dependencies

```xml
<dependency>
    <groupId>io.vavr</groupId>
    <artifactId>vavr</artifactId>
    <version>0.10.4</version>
</dependency>

<!-- JSON serialization support -->
<dependency>
    <groupId>io.vavr</groupId>
    <artifactId>vavr-jackson</artifactId>
    <version>0.10.4</version>
</dependency>
```

---

## Why Use Vavr?

- ✅ Immutable collections, thread-safe
- ✅ Try/Option/Either replace try-catch and null
- ✅ Function composition and method chaining
- ✅ Pattern matching enhances readability
- ✅ Perfect compatibility with Spring MVC

---

## Option - Null Safety

### [Mandatory] Basic Usage

```java
// ❌ Traditional way - verbose null checks
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

// ✅ Vavr Option - elegant method chaining
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

### [Mandatory] Option API

```java
// Create Option
Option<String> some = Option.of("value");
Option<String> none = Option.of(null);          // None
Option<String> fromOptional = Option.ofOptional(optional);

// Transform
Option<Integer> length = some.map(String::length);
Option<String> upper = some.map(String::toUpperCase);

// Filter
Option<String> filtered = some.filter(s -> s.length() > 5);

// Get value
String value = some.getOrElse("default");
String value2 = some.getOrElse(() -> computeDefault());
String value3 = some.getOrElseThrow(() -> new RuntimeException());

// Check
if (some.isDefined()) { ... }
if (some.isEmpty()) { ... }

// flatMap for nested handling
Option<String> city = Option.of(user)
    .flatMap(u -> Option.of(u.getAddress()))
    .map(Address::getCity);
```

### [Recommended] Service Layer Method Chaining

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

## Try - Exception Handling

### [Mandatory] Basic Usage

```java
// ❌ Traditional try-catch
public String readFile(String path) {
    try {
        return Files.readString(Paths.get(path));
    } catch (IOException e) {
        log.error("File read failed", e);
        throw new RuntimeException(e);
    }
}

// ✅ Vavr Try
public Try<String> readFile(String path) {
    return Try.of(() -> Files.readString(Paths.get(path)))
        .onFailure(e -> log.error("File read failed", e));
}
```

### [Mandatory] Try API

```java
// Create and transform
Try<Integer> result = Try.of(() -> 42).map(i -> i * 2);
Try<String> content = Try.of(() -> Paths.get("f.txt")).mapTry(Files::readString);

// Error recovery
Try<Integer> recovered = Try.of(() -> 1 / 0)
    .recover(ArithmeticException.class, 0);

// Get value
Integer value = result.getOrElse(0);
```

### [Recommended] External API Calls

```java
public Try<ApiResponse> callApi(String endpoint, Object req) {
    return Try.of(() -> restTemplate.postForObject(endpoint, req, ApiResponse.class))
        .recover(HttpClientErrorException.class, ex -> {
            log.error("API failed: {}", ex.getStatusCode());
            throw new ExternalApiException("Third-party service error", ex);
        });
}
```

---

## Checklist

**Option Usage**:
- [ ] Service layer methods return Option instead of null
- [ ] Use map/flatMap chaining instead of if-null checks
- [ ] Controller uses fold() or getOrElse() to handle Option

**Try Usage**:
- [ ] File operations use Try.mapTry() to handle IOException
- [ ] External API calls wrapped with Try
- [ ] Use recover() to handle specific exceptions instead of catch

**General Principles**:
- [ ] Avoid throwing exceptions inside Option/Try
- [ ] Prefer method references for better readability
- [ ] Compatible with Spring MVC traditional architecture

---

## Related Standards
- [08-vavr-advanced.md](./08-vavr-advanced.md) - Either, Collections, Pattern Matching
- [08-vavr-mybatis-integration.md](./08-vavr-mybatis-integration.md) - MyBatis Plus Integration
