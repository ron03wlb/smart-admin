# OWASP Top 10 Vulnerabilities and SmartAdmin Mitigations

## 1. Broken Access Control (A01:2021)

**Vulnerability:** Users can access resources they shouldn't have access to.

**SmartAdmin Protection:**
```java
// Use Sa-Token permission checks
@PostMapping("/delete")
@SaCheckPermission("employee:delete")  // Only users with permission can access
public ResponseDTO<String> deleteEmployee(@RequestBody Long employeeId) {
    return employeeService.deleteEmployee(employeeId);
}

// Verify resource ownership
@GetMapping("/profile/{userId}")
public ResponseDTO<UserVO> getUserProfile(@PathVariable Long userId) {
    // Check if user is accessing their own profile or is admin
    if (!userId.equals(RequestContext.getUserId()) && !StpUtil.hasRole("admin")) {
        return ResponseDTO.error(ErrorCode.PERMISSION_DENIED);
    }
    return userService.getUserProfile(userId);
}
```

## 2. Cryptographic Failures (A02:2021)

**Vulnerability:** Sensitive data exposed due to weak or missing encryption.

**SmartAdmin Protection:**
```java
// Use api-encrypt foundation module
@PostMapping("/payment")
@ApiEncrypt  // Encrypts request and response
public ResponseDTO<String> processPayment(@RequestBody PaymentForm form) {
    // Sensitive data is auto-encrypted
}

// Hash passwords with BCrypt
String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));

// Encrypt sensitive data before storage
String encrypted = encryptUtil.encrypt(sensitiveData);
```

## 3. Injection (A03:2021)

**Vulnerability:** Untrusted data sent to interpreter (SQL, OS, LDAP).

**SmartAdmin Protection:**
```java
// ALWAYS use parameterized queries
LambdaQueryWrapper<EmployeeEntity> query = Wrappers.<EmployeeEntity>lambdaQuery()
    .eq(EmployeeEntity::getLoginName, loginName)  // Parameterized - SAFE
    .like(EmployeeEntity::getActualName, keyword);

// NEVER use string concatenation
// String sql = "SELECT * FROM t_employee WHERE login_name = '" + loginName + "'";  // VULNERABLE!

// MyBatis: Use #{} not ${}
@Select("SELECT * FROM t_employee WHERE login_name = #{loginName}")  // SAFE
EmployeeEntity selectByLoginName(@Param("loginName") String loginName);
```

## 4. Insecure Design (A04:2021)

**Vulnerability:** Missing or ineffective control design.

**SmartAdmin Protection:**
```java
// Implement defense in depth
@PostMapping("/withdraw")
@SaCheckLogin                    // Layer 1: Authentication
@SaCheckPermission("wallet:withdraw")  // Layer 2: Authorization
@RepeatSubmit(interval = 5000)   // Layer 3: Rate limiting
public ResponseDTO<String> withdraw(@RequestBody @Valid WithdrawalForm form) {
    // Layer 4: Business validation
    if (!kycService.isVerified(form.getPlayerId())) {
        return ResponseDTO.userErrorParam("KYC verification required");
    }
    
    // Layer 5: Transaction limits
    if (!riskService.validateWithdrawal(form)) {
        return ResponseDTO.userErrorParam("Withdrawal limit exceeded");
    }
    
    return walletService.processWithdrawal(form);
}
```

## 5. Security Misconfiguration (A05:2021)

**Vulnerability:** Insecure default configurations, incomplete setups.

**SmartAdmin Protection:**
```yaml
# application.yml - Secure defaults
spring:
  datasource:
    url: ${DB_URL}  # Use environment variables
    username: ${DB_USER}
    password: ${DB_PASSWORD}

sa:
  config:
    show-sql: false  # Disable in production
    stacktrace: false  # Don't expose stack traces
  
server:
  error:
    include-message: never  # Don't expose error details
    include-stacktrace: never
```

## 6. Vulnerable and Outdated Components (A06:2021)

**Vulnerability:** Using libraries with known vulnerabilities.

**SmartAdmin Protection:**
```bash
# Regularly update dependencies
./gradlew dependencyUpdates

# Check for vulnerabilities
./gradlew dependencyCheckAnalyze

# Use Dependabot or Renovate for automated PRs
```

```kotlin
// build.gradle.kts - Keep updated
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web:3.5.4")
    implementation("com.baomidou:mybatis-plus-spring-boot3-starter:3.5.12")
    implementation("cn.dev33:sa-token-spring-boot3-starter:1.44.0")
}
```

## 7. Identification and Authentication Failures (A07:2021)

**Vulnerability:** Weak authentication, session management issues.

**SmartAdmin Protection:**
```java
// Strong password validation
@Pattern(
    regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
    message = "Password must be 8-20 chars with uppercase, lowercase, digit, and special character"
)
private String password;

// Sa-Token session management (auto-expires)
StpUtil.login(userId, 
    SaLoginConfig.setTimeout(7200)  // 2-hour timeout
);

// Multi-factor authentication
if (!mfaService.verifyCode(form.getMfaCode())) {
    return ResponseDTO.error(ErrorCode.MFA_INVALID);
}
```

## 8. Software and Data Integrity Failures (A08:2021)

**Vulnerability:** Unsigned updates, insecure CI/CD pipelines.

**SmartAdmin Protection:**
```yaml
# GitHub Actions - Dependency verification
- name: Verify dependencies
  run: ./gradlew verifyDependencies

# Use checksums for artifacts
- name: Generate checksum
  run: sha256sum smart-admin.jar > smart-admin.jar.sha256

# Sign releases
- name: Sign artifact
  run: gpg --sign smart-admin.jar
```

## 9. Security Logging and Monitoring Failures (A09:2021)

**Vulnerability:** Insufficient logging, no monitoring.

**SmartAdmin Protection:**
```java
// Audit log for all critical operations
@Manager
public class WalletManager {
    private final AuditLogService auditLogService;
    
    @Transactional
    public void processWithdrawal(WithdrawalForm form) {
        // Process withdrawal
        walletDao.updateBalance(form.getPlayerId(), form.getAmount());
        
        // MANDATORY: Audit log
        auditLogService.log("WALLET_WITHDRAWAL", 
            form.getPlayerId(), 
            form.getAmount(), 
            RequestContext.getIpAddress(),
            RequestContext.getUserAgent());
    }
}

// Monitor failed login attempts
@Service
public class LoginService {
    
    public ResponseDTO<String> login(LoginForm form) {
        EmployeeEntity employee = employeeDao.selectByLoginName(form.getLoginName());
        
        if (employee == null || !passwordService.verify(form.getPassword(), employee.getPassword())) {
            // Log failed attempt
            auditLogService.log("LOGIN_FAILED", form.getLoginName(), RequestContext.getIpAddress());
            
            // Rate limit after 5 failed attempts
            if (getFailedAttempts(form.getLoginName()) > 5) {
                return ResponseDTO.error(ErrorCode.ACCOUNT_LOCKED);
            }
            
            return ResponseDTO.error(ErrorCode.INVALID_CREDENTIALS);
        }
        
        // Successful login
        auditLogService.log("LOGIN_SUCCESS", employee.getEmployeeId(), RequestContext.getIpAddress());
        return ResponseDTO.ok(StpUtil.getTokenValue());
    }
}
```

## 10. Server-Side Request Forgery (SSRF) (A10:2021)

**Vulnerability:** Application fetches remote resources without validating user-supplied URLs.

**SmartAdmin Protection:**
```java
@Service
public class WebhookService {
    
    private static final List<String> ALLOWED_DOMAINS = 
        Arrays.asList("api.partner.com", "webhook.example.com");
    
    public ResponseDTO<String> sendWebhook(String url, Object payload) {
        // Validate URL domain
        try {
            URL parsedUrl = new URL(url);
            String host = parsedUrl.getHost();
            
            if (!ALLOWED_DOMAINS.contains(host)) {
                return ResponseDTO.userErrorParam("Domain not in allowlist");
            }
            
            // Prevent internal network access
            InetAddress address = InetAddress.getByName(host);
            if (address.isLoopbackAddress() || address.isSiteLocalAddress()) {
                return ResponseDTO.userErrorParam("Internal network access denied");
            }
            
            // Make request
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.toJson(payload)))
                .build();
            
            client.send(request, HttpResponse.BodyHandlers.ofString());
            return ResponseDTO.ok();
            
        } catch (Exception e) {
            log.error("Webhook failed", e);
            return ResponseDTO.error(ErrorCode.SYSTEM_ERROR);
        }
    }
}
```

## Security Testing Checklist

- [ ] **A01 - Access Control**: Test IDOR, privilege escalation, forced browsing
- [ ] **A02 - Crypto**: Verify encryption, check password storage, test TLS
- [ ] **A03 - Injection**: Test SQL injection, OS command injection, LDAP injection
- [ ] **A04 - Design**: Review threat model, check security requirements
- [ ] **A05 - Misconfiguration**: Check default credentials, error messages, CORS
- [ ] **A06 - Components**: Run dependency scan, check CVE database
- [ ] **A07 - Authentication**: Test weak passwords, session fixation, brute force
- [ ] **A08 - Integrity**: Verify checksums, review CI/CD pipeline
- [ ] **A09 - Logging**: Check audit logs, test monitoring alerts
- [ ] **A10 - SSRF**: Test URL validation, internal network access

## Automated Security Scanning

```bash
# OWASP Dependency Check
./gradlew dependencyCheckAnalyze

# SpotBugs (includes security bugs)
./gradlew spotbugsMain

# PMD (includes security rules)
./gradlew pmdMain

# Checkstyle (coding standards)
./gradlew checkstyleMain
```
