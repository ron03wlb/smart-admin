---
trigger: always_on
---

# OWASP 安全規則 - Java/Spring Boot 防護指南

基於 OWASP Top 10 (2021)，針對 Spring Boot 應用的強制性安全約束。
所有規則可通過 SpotBugs + FindSecBugs、SonarQube 自動檢測。

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
    return orderRepository.findById(id).orElseThrow(); // 無授權檢查
}
```

### 【強制】IDOR 防護

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

// ❌ 錯誤 - 通配符在前會覆蓋後續規則
http.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/**").authenticated()
    .requestMatchers("/api/admin/**").hasRole("ADMIN") // 永遠不會到達
);
```

---

## A02 - Cryptographic Failures（加密失效）

### 【強制】密碼存儲

```java
// ✅ 正確 - BCrypt
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
}

// ❌ 嚴禁
user.setPassword(dto.getPassword());                     // 明文
user.setPassword(DigestUtils.md5Hex(dto.getPassword())); // MD5 已破解
```

### 【強制】敏感數據加密

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

// ❌ 嚴禁 - ECB 模式或固定 IV
Cipher.getInstance("AES/ECB/PKCS5Padding");
```

### 【強制】禁止硬編碼機密

```java
// ❌ 嚴禁
private static final String DB_PASSWORD = "mypassword123";
private static final String API_KEY = "sk-1234567890abcdef";

// ✅ 正確 - 環境變量或 Vault
@Value("${spring.datasource.password}")
private String dbPassword;
```

---

## A03 - Injection（注入攻擊）

### 【強制】SQL 注入防護

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

// ❌ 嚴禁 - 字符串拼接
String sql = "SELECT * FROM users WHERE name = '" + name + "'";
```

### 【強制】命令注入防護

```java
// ❌ 嚴禁
Runtime.getRuntime().exec("cat " + filename);

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

```java
// ✅ Thymeleaf 自動轉義
<p th:text="${userInput}"></p>

// ✅ 手動編碼
import org.owasp.encoder.Encode;
model.addAttribute("query", Encode.forHtml(query));

// ✅ CSP Header
http.headers(h -> h.contentSecurityPolicy(csp -> 
    csp.policyDirectives("default-src 'self'; script-src 'self'")));

// ❌ 危險 - utext 不轉義
<p th:utext="${userInput}"></p>
```

---

## A04 - Insecure Design（不安全設計）

### 【強制】輸入驗證

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

## A05 - Security Misconfiguration（安全配置錯誤）

### 【強制】生產環境配置

```yaml
# application-prod.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: never

server:
  error:
    include-stacktrace: never
    include-message: never

spring:
  h2.console.enabled: false

springdoc:
  api-docs.enabled: false
  swagger-ui.enabled: false
```

### 【強制】安全 Headers

```java
http.headers(headers -> headers
    .frameOptions(frame -> frame.deny())
    .httpStrictTransportSecurity(hsts -> hsts
        .includeSubDomains(true)
        .maxAgeInSeconds(31536000))
    .contentTypeOptions(Customizer.withDefaults())
);
```

---

## A06 - Vulnerable Components（易受攻擊組件）

### 【強制】依賴漏洞掃描

```xml
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>10.0.3</version>
    <configuration>
        <failBuildOnCVSS>7</failBuildOnCVSS>
    </configuration>
</plugin>
```

---

## A07 - Authentication Failures（身份驗證失效）

### 【強制】JWT 安全配置

```java
@PostConstruct
protected void init() {
    // HS512 需要至少 512 bits 密鑰
    this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
}

public String createToken(String username, List<String> roles) {
    return Jwts.builder()
        .setSubject(username)
        .claim("roles", roles)
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + 3600000))
        .setId(UUID.randomUUID().toString()) // JTI 防重放
        .signWith(key, SignatureAlgorithm.HS512)
        .compact();
}

// ❌ 錯誤
.signWith(SignatureAlgorithm.HS256, "weak-key");
.signWith(SignatureAlgorithm.NONE);
```

### 【強制】Session 安全

```yaml
server:
  servlet:
    session:
      cookie:
        http-only: true
        secure: true
        same-site: strict
        max-age: 3600
      timeout: 30m
```

---

## A08 - Data Integrity Failures（數據完整性失效）

### 【強制】反序列化安全

```java
// ❌ 嚴禁
ObjectInputStream ois = new ObjectInputStream(untrustedInput);
Object obj = ois.readObject(); // RCE 風險

// ✅ 正確 - 白名單
public class SafeObjectInputStream extends ObjectInputStream {
    private static final Set<String> ALLOWED = Set.of(
        "com.example.dto.UserDTO",
        "com.example.dto.OrderDTO"
    );
    
    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) 
            throws IOException, ClassNotFoundException {
        if (!ALLOWED.contains(desc.getName())) {
            throw new InvalidClassException("Unauthorized: " + desc.getName());
        }
        return super.resolveClass(desc);
    }
}
```

---

## A09 - Logging Failures（日誌監控失效）

### 【強制】安全事件日誌

```java
@Aspect
@Component
@Slf4j
public class SecurityAuditAspect {
    
    @AfterReturning("@annotation(audited)")
    public void logSecurityEvent(JoinPoint jp, Audited audited) {
        log.info("SECURITY_AUDIT | action={} | user={} | resource={} | ip={} | time={}",
            audited.action(),
            SecurityContextHolder.getContext().getAuthentication().getName(),
            jp.getSignature().getName(),
            getClientIp(),
            Instant.now()
        );
    }
}
```

**必須記錄**：認證成功/失敗、授權失敗、輸入驗證失敗、敏感數據訪問、配置變更

---

## A10 - SSRF（服務端請求偽造）

### 【強制】SSRF 防護

```java
private static final Set<String> ALLOWED_HOSTS = Set.of("api.trusted.com");

public String fetchUrl(String urlString) throws IOException {
    URL url = new URL(urlString);
    
    // 1. 協議白名單
    if (!Set.of("http", "https").contains(url.getProtocol())) {
        throw new SecurityException("Blocked scheme");
    }
    
    // 2. 主機白名單
    if (!ALLOWED_HOSTS.contains(url.getHost())) {
        throw new SecurityException("Host not allowed");
    }
    
    // 3. 禁止內網
    InetAddress addr = InetAddress.getByName(url.getHost());
    if (addr.isLoopbackAddress() || addr.isSiteLocalAddress()) {
        throw new SecurityException("Internal addresses blocked");
    }
    
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setConnectTimeout(5000);
    conn.setInstanceFollowRedirects(false);
    return new String(conn.getInputStream().readAllBytes(), UTF_8);
}
```

---

## FindSecBugs 必須啟用規則

| 規則 ID                 | 名稱       | 嚴重程度 |
| ----------------------- | ---------- | -------- |
| SQL_INJECTION           | SQL 注入   | Critical |
| COMMAND_INJECTION       | 命令注入   | Critical |
| PATH_TRAVERSAL_IN       | 路徑遍歷   | Critical |
| XXE_DOCUMENT            | XXE 攻擊   | Critical |
| XSS_REQUEST_WRAPPER     | XSS        | High     |
| WEAK_MESSAGE_DIGEST_MD5 | 弱哈希     | High     |
| HARD_CODE_PASSWORD      | 硬編碼密碼 | High     |

---

## 安全檢查清單

- [ ] 無 SQL/Command 注入風險
- [ ] 所有輸入經過 @Valid 驗證
- [ ] 密碼使用 BCrypt，敏感數據 AES-GCM
- [ ] 無硬編碼機密
- [ ] 實現方法級授權
- [ ] SpotBugs + FindSecBugs 無警告
- [ ] Dependency Check 無高危漏洞