---
trigger: always_on
description: OWASP Top 10 Security Standards Part 1 (A01-A04)
tags: [security, owasp, spring-security, access-control, injection]
positioning: current-standard
last_updated: 2025-01-12
---

# OWASP Top 10 Part 1 - Access Control & Injection

Based on OWASP Top 10 (2021), mandatory security constraints for Spring Boot applications.

---

## A01 - Broken Access Control

### [Mandatory] Method-Level Authorization

```java
// ✅ Correct - @PreAuthorize method-level authorization
@Service
public class OrderService {

    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isOwner(#orderId, authentication)")
    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }
}

// ❌ Wrong - Relying only on URL-level security
@GetMapping("/orders/{id}")
public Order getOrder(@PathVariable Long id) {
    return orderRepository.findById(id).orElseThrow();
}
```

### [Mandatory] IDOR Protection

**Key Measures**:
- Verify resource ownership (user ID matching)
- Implement role-based access control
- Double-check sensitive operations

```java
// ✅ Correct - Verify resource ownership
@GetMapping("/users/{userId}/documents/{docId}")
public Document getDocument(@PathVariable Long userId, @PathVariable Long docId,
                           @AuthenticationPrincipal UserDetails user) {
    if (!user.getUserId().equals(userId) && !user.hasRole("ADMIN")) {
        throw new AccessDeniedException("Unauthorized access");
    }
    return documentService.findByIdAndUserId(docId, userId).orElseThrow();
}
```

### [Mandatory] URL Matcher Ordering

```java
// ✅ Correct - Specific rules first
http.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/public/**").permitAll()
    .requestMatchers(HttpMethod.DELETE, "/api/admin/**").hasRole("ADMIN")
    .requestMatchers("/api/admin/**").hasRole("ADMIN")
    .requestMatchers("/api/**").authenticated()
    .anyRequest().denyAll()
);
```

---

## A02 - Cryptographic Failures

### [Mandatory] Password Storage

```java
// ✅ Correct - BCrypt (strength 12)
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
}

// ❌ Forbidden
user.setPassword(dto.getPassword());                     // Plaintext
user.setPassword(DigestUtils.md5Hex(dto.getPassword())); // MD5 is broken
```

### [Mandatory] Sensitive Data Encryption

**Standard**: AES-256-GCM, random IV, use SecureRandom

```java
// ✅ Correct - AES-256-GCM
private static final String ALGORITHM = "AES/GCM/NoPadding";
private static final int GCM_TAG_LENGTH = 128;

public String encrypt(String plaintext) throws GeneralSecurityException {
    byte[] iv = new byte[12];
    SecureRandom.getInstanceStrong().nextBytes(iv);

    Cipher cipher = Cipher.getInstance(ALGORITHM);
    cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
    byte[] ciphertext = cipher.doFinal(plaintext.getBytes(UTF_8));

    // Store IV + Ciphertext together
    return Base64.getEncoder().encodeToString(concat(iv, ciphertext));
}
```

### [Mandatory] Prohibit Hardcoded Secrets

```java
// ❌ Forbidden
private static final String DB_PASSWORD = "mypassword123";

// ✅ Correct - Environment variable or Vault
@Value("${spring.datasource.password}")
private String dbPassword;
```

---

## A03 - Injection

### [Mandatory] SQL Injection Protection

**Mandatory Requirements**:
- MyBatis Plus uses Lambda Query
- JPA uses parameter binding (`@Param`)
- Prohibit string concatenation for SQL

```java
// ✅ Correct - MyBatis Plus Lambda
public List<User> findUsers(String name) {
    return userMapper.selectList(
        Wrappers.<User>lambdaQuery()
            .like(StringUtils.isNotBlank(name), User::getName, name)
    );
}

// ✅ Correct - JPA parameter binding
@Query("SELECT u FROM User u WHERE u.email = :email")
Optional<User> findByEmail(@Param("email") String email);

// ❌ Forbidden
String sql = "SELECT * FROM users WHERE name = '" + name + "'";
```

### [Mandatory] Command Injection Protection

**Protection Strategy**:
- Use whitelist to validate input
- Use ProcessBuilder instead of Runtime.exec()
- Use parameter arrays instead of strings

```java
// ✅ Correct - Whitelist + parameter array
public void processFile(String filename) {
    if (!filename.matches("^[a-zA-Z0-9_\\-\\.]+$")) {
        throw new IllegalArgumentException("Invalid filename");
    }
    ProcessBuilder pb = new ProcessBuilder("cat", filename);
    pb.directory(new File("/safe/directory"));
    pb.start();
}
```

### [Mandatory] XSS Protection

**Three-Layer Defense**:
1. Template auto-escaping (Thymeleaf)
2. Manual encoding of user input (OWASP Encoder)
3. CSP Header to limit script sources

```java
// ✅ Thymeleaf auto-escaping
<p th:text="${userInput}"></p>

// ✅ Manual encoding
import org.owasp.encoder.Encode;
model.addAttribute("query", Encode.forHtml(query));

// ✅ CSP Header
http.headers(h -> h.contentSecurityPolicy(csp ->
    csp.policyDirectives("default-src 'self'; script-src 'self'")));
```

---

## A04 - Insecure Design

### [Mandatory] Input Validation

**Validation Layers**:
- DTO Layer: `@Valid` + JSR-380 annotations
- Service Layer: Business logic validation
- Database Layer: Constraint checks

```java
@Data
public class UserCreateDTO {

    @NotBlank @Size(min = 3, max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9_]+$")
    private String username;

    @NotBlank @Email
    private String email;

    @NotBlank @Size(min = 8, max = 128)
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$")
    private String password;
}

@PostMapping("/users")
public ResponseEntity<User> createUser(@Valid @RequestBody UserCreateDTO dto) {
    return ResponseEntity.ok(userService.createUser(dto));
}
```

### [Mandatory] Rate Limiting

**Sensitive Endpoint Limits**:
- Login: 100 requests/minute (per IP)
- Registration: 10 requests/hour (per IP)
- API: 1000 requests/hour (per user)

```java
@Bean
public Bucket rateLimitBucket() {
    return Bucket.builder()
        .addLimit(Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1))))
        .build();
}

@PostMapping("/login")
@RateLimiter(name = "login", fallbackMethod = "loginRateLimitFallback")
public ResponseEntity<TokenDTO> login(@RequestBody LoginDTO dto) {
    return ResponseEntity.ok(authService.login(dto));
}
```

---

## Related Standards

- **Part 2**: `security/S02-owasp-top10-part2.md` - A05-A10 Security Configuration, Authentication, SSRF
- **MyBatis Plus**: `technology/database/09-mybatis-plus-core.md` - Lambda Query for SQL Injection Protection
- **CI/CD Pipeline**: `workflows/java-ci-cd-pipeline.md` - Integrate SpotBugs + FindSecBugs
