---
trigger: always_on
description: OWASP Top 10 安全規範 Part 1 (A01-A04)
tags: [security, owasp, spring-security, access-control, injection]
positioning: current-standard
last_updated: 2025-01-12
---

# OWASP Top 10 Part 1 - Access Control & Injection

基於 OWASP Top 10 (2021)，針對 Spring Boot 應用的強制性安全約束。

---

## A01 - Broken Access Control（存取控制失效）

### 【強制】方法級別授權

```java
// ✅ 正確 - @PreAuthorize 方法級授權
@Service
public class OrderService {

    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isOwner(#orderId, authentication)")
    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }
}

// ❌ 錯誤 - 僅依賴 URL 級別安全
@GetMapping("/orders/{id}")
public Order getOrder(@PathVariable Long id) {
    return orderRepository.findById(id).orElseThrow();
}
```

### 【強制】IDOR 防護

**關鍵措施**：
- 驗證資源所有權（用戶 ID 匹配）
- 實現基於角色的訪問控制
- 對敏感操作進行雙重檢查

```java
// ✅ 正確 - 驗證資源所有權
@GetMapping("/users/{userId}/documents/{docId}")
public Document getDocument(@PathVariable Long userId, @PathVariable Long docId,
                           @AuthenticationPrincipal UserDetails user) {
    if (!user.getUserId().equals(userId) && !user.hasRole("ADMIN")) {
        throw new AccessDeniedException("無權訪問");
    }
    return documentService.findByIdAndUserId(docId, userId).orElseThrow();
}
```

### 【強制】URL Matcher 順序

```java
// ✅ 正確 - 具體規則在前
http.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/public/**").permitAll()
    .requestMatchers(HttpMethod.DELETE, "/api/admin/**").hasRole("ADMIN")
    .requestMatchers("/api/admin/**").hasRole("ADMIN")
    .requestMatchers("/api/**").authenticated()
    .anyRequest().denyAll()
);
```

---

## A02 - Cryptographic Failures（加密失效）

### 【強制】密碼存儲

```java
// ✅ 正確 - BCrypt (強度 12)
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
}

// ❌ 嚴禁
user.setPassword(dto.getPassword());                     // 明文
user.setPassword(DigestUtils.md5Hex(dto.getPassword())); // MD5 已破解
```

### 【強制】敏感數據加密

**標準**：AES-256-GCM，隨機 IV，使用 SecureRandom

```java
// ✅ 正確 - AES-256-GCM
private static final String ALGORITHM = "AES/GCM/NoPadding";
private static final int GCM_TAG_LENGTH = 128;

public String encrypt(String plaintext) throws GeneralSecurityException {
    byte[] iv = new byte[12];
    SecureRandom.getInstanceStrong().nextBytes(iv);

    Cipher cipher = Cipher.getInstance(ALGORITHM);
    cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
    byte[] ciphertext = cipher.doFinal(plaintext.getBytes(UTF_8));

    // IV + Ciphertext 一起存儲
    return Base64.getEncoder().encodeToString(concat(iv, ciphertext));
}
```

### 【強制】禁止硬編碼機密

```java
// ❌ 嚴禁
private static final String DB_PASSWORD = "mypassword123";

// ✅ 正確 - 環境變量或 Vault
@Value("${spring.datasource.password}")
private String dbPassword;
```

---

## A03 - Injection（注入攻擊）

### 【強制】SQL 注入防護

**強制要求**：
- MyBatis Plus 使用 Lambda 查詢
- JPA 使用參數綁定（`@Param`）
- 禁止字符串拼接 SQL

```java
// ✅ 正確 - MyBatis Plus Lambda
public List<User> findUsers(String name) {
    return userMapper.selectList(
        Wrappers.<User>lambdaQuery()
            .like(StringUtils.isNotBlank(name), User::getName, name)
    );
}

// ✅ 正確 - JPA 參數綁定
@Query("SELECT u FROM User u WHERE u.email = :email")
Optional<User> findByEmail(@Param("email") String email);

// ❌ 嚴禁
String sql = "SELECT * FROM users WHERE name = '" + name + "'";
```

### 【強制】命令注入防護

**防護策略**：
- 使用白名單驗證輸入
- ProcessBuilder 取代 Runtime.exec()
- 使用參數數組而非字符串

```java
// ✅ 正確 - 白名單 + 參數數組
public void processFile(String filename) {
    if (!filename.matches("^[a-zA-Z0-9_\\-\\.]+$")) {
        throw new IllegalArgumentException("Invalid filename");
    }
    ProcessBuilder pb = new ProcessBuilder("cat", filename);
    pb.directory(new File("/safe/directory"));
    pb.start();
}
```

### 【強制】XSS 防護

**三層防護**：
1. 模板自動轉義（Thymeleaf）
2. 手動編碼用戶輸入（OWASP Encoder）
3. CSP Header 限制腳本來源

```java
// ✅ Thymeleaf 自動轉義
<p th:text="${userInput}"></p>

// ✅ 手動編碼
import org.owasp.encoder.Encode;
model.addAttribute("query", Encode.forHtml(query));

// ✅ CSP Header
http.headers(h -> h.contentSecurityPolicy(csp ->
    csp.policyDirectives("default-src 'self'; script-src 'self'")));
```

---

## A04 - Insecure Design（不安全設計）

### 【強制】輸入驗證

**驗證層級**：
- DTO 層：`@Valid` + JSR-380 註解
- Service 層：業務邏輯驗證
- 資料庫層：約束檢查

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

### 【強制】速率限制

**敏感端點限制**：
- 登入：100 次/分鐘（IP）
- 註冊：10 次/小時（IP）
- API：1000 次/小時（用戶）

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

## 相關規範

- **Part 2**：`rules/07-owasp-top10-part2.md` - A05-A10 安全配置、認證、SSRF
- **MyBatis Plus**：`rules/09-mybatis-plus.md` - Lambda 查詢防護 SQL 注入
- **CI/CD Pipeline**：`workflows/java-ci-cd-pipeline.md` - 集成 SpotBugs + FindSecBugs
