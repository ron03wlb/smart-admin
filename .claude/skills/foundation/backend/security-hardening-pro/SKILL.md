---
name: security-hardening-pro
description: [P0 - Critical] Implement security best practices automatically including SM2/SM3/SM4 encryption, data masking for PII, SQL injection prevention, XSS/CSRF protection, rate limiting, and audit logging. Use when securing APIs, implementing encryption, masking sensitive data, or meeting compliance requirements (KYC/AML for iGaming). Automatically triggered when user mentions "security", "encryption", "data masking", "SQL injection", "XSS", "CSRF", "audit log", "compliance", or "hardening".
---

# Security Hardening Automation

Implement security best practices for SmartAdmin applications, with focus on iGaming compliance requirements (KYC/AML), data protection, and attack prevention.

## Quick Start

**Most common usage:**
```
User: "Add encryption to the payment API"
User: "Implement data masking for phone numbers"
User: "Secure this endpoint against SQL injection"
User: "Add audit logging for financial operations"
User: "Implement rate limiting to prevent DDoS"
```

You will:
1. Identify security requirements (encryption, masking, auth, audit)
2. Apply SmartAdmin foundation modules (api-encrypt, data-masking, etc.)
3. Configure security filters and interceptors
4. Add validation and sanitization
5. Implement audit trails

## Trigger Keywords

This skill is automatically activated when the user's request contains:

**Primary Keywords** (High confidence):
- "security" - Security hardening or compliance implementation
- "encryption" - API encryption (SM2/SM3/SM4)
- "data masking" - PII protection (phone, email, ID card)
- "audit log" - Financial operation audit trails
- "secure endpoint" - Endpoint security hardening

**Secondary Keywords** (Medium confidence):
- "SQL injection" - Context: input validation and sanitization
- "XSS" - Context: cross-site scripting protection
- "CSRF" - Context: cross-site request forgery protection
- "rate limiting" - Context: DDoS prevention
- "compliance" - Context: KYC/AML/iGaming compliance
- "KYC" - Context: Know Your Customer implementation
- "AML" - Context: Anti-Money Laundering implementation
- "hardening" - General security hardening request

**Phrase Patterns**:
- "Add [security feature] to [component]" - Example: "Add encryption to the payment API"
- "Implement [protection] for [data]" - Example: "Implement data masking for phone numbers"
- "Secure [component] against [threat]" - Example: "Secure this endpoint against SQL injection"

**Example User Requests**:
```
User: "Add encryption to the payment API"
User: "Implement data masking for phone numbers in user profile"
User: "Secure this endpoint against SQL injection and XSS"
User: "Add audit logging for financial operations"
User: "Implement rate limiting to prevent DDoS attacks"
```

**Note**: This skill can also be manually invoked via `/security-hardening-pro` command.

## Core Capabilities

### 1. API Encryption (SM2/SM3/SM4)

**SmartAdmin api-encrypt foundation module:**

**Enable encryption:**
```java
@RestController
@RequestMapping("/api/payment")
public class PaymentController {
    
    // Request/Response encryption
    @PostMapping("/withdraw")
    @ApiEncrypt  // Automatically encrypts request and response
    public ResponseDTO<String> withdraw(@RequestBody @Valid WithdrawalForm form) {
        // Form is auto-decrypted by ApiEncryptInterceptor
        return paymentService.processWithdrawal(form);
        // Response is auto-encrypted before sending
    }
}
```

**Configuration (application.yml):**
```yaml
sa:
  api-encrypt:
    enabled: true
    algorithm: SM4  # SM2, SM3, SM4 (Chinese encryption standards)
    secret-key: ${ENCRYPT_SECRET_KEY}  # From environment variable
    exclude-paths:
      - /api/public/**
      - /api/health
```

**Custom encryption for specific fields:**
```java
@Service
public class PaymentService {
    private final SmartEncryptUtil encryptUtil;
    
    public void saveCardInfo(CardForm form) {
        // Encrypt credit card number before saving
        String encryptedCard = encryptUtil.encrypt(form.getCardNumber());
        
        CardEntity entity = new CardEntity();
        entity.setCardNumber(encryptedCard);
        cardDao.insert(entity);
    }
    
    public CardVO getCardInfo(Long cardId) {
        CardEntity entity = cardDao.selectById(cardId);
        
        // Decrypt when retrieving
        String decryptedCard = encryptUtil.decrypt(entity.getCardNumber());
        
        CardVO vo = SmartBeanUtil.copy(entity, CardVO.class);
        vo.setCardNumber(decryptedCard);
        return vo;
    }
}
```

---

### 2. Data Masking (PII Protection)

**SmartAdmin data-masking foundation module:**

**Apply masking to VO fields:**
```java
public class EmployeeVO {
    private Long employeeId;
    private String actualName;
    
    @MaskPhone  // Masks phone: 138****1234
    private String phone;
    
    @MaskEmail  // Masks email: ab***@example.com
    private String email;
    
    @MaskIdCard  // Masks ID card: 110***********1234
    private String idCard;
    
    @MaskBankCard  // Masks bank card: 6222****1234
    private String bankCard;
    
    @MaskAddress  // Masks address: 北京市***
    private String address;
}
```

**Custom masking rules:**
```java
@MaskCustom(maskFunc = "customMask")
private String sensitiveData;

// Masking function
public static String customMask(String data) {
    if (data == null || data.length() <= 4) {
        return "****";
    }
    return data.substring(0, 2) + "****" + data.substring(data.length() - 2);
}
```

**Configuration:**
```yaml
sa:
  data-masking:
    enabled: true
    mask-patterns:
      phone: "^(\\d{3})\\d{4}(\\d{4})$"  # 138****1234
      email: "^(\\w{2})\\w+(@.+)$"        # ab***@example.com
      id-card: "^(\\d{3})\\d{11}(\\d{4})$"  # 110***********1234
```

---

### 3. SQL Injection Prevention

**MyBatis Plus parameterized queries (ALWAYS use):**
```java
// GOOD: Parameterized query (SQL injection safe)
LambdaQueryWrapper<EmployeeEntity> query = Wrappers.<EmployeeEntity>lambdaQuery()
    .eq(EmployeeEntity::getLoginName, loginName)  // Parameterized
    .like(EmployeeEntity::getActualName, keyword); // Parameterized

List<EmployeeEntity> employees = employeeDao.selectList(query);
```

**NEVER use string concatenation:**
```java
// BAD: String concatenation (SQL INJECTION VULNERABLE!)
String sql = "SELECT * FROM t_employee WHERE login_name = '" + loginName + "'";
// Attacker input: admin' OR '1'='1
// Resulting SQL: SELECT * FROM t_employee WHERE login_name = 'admin' OR '1'='1'

// NEVER DO THIS!
```

**MyBatis Mapper with parameterization:**
```java
@Mapper
public interface EmployeeDao extends BaseMapper<EmployeeEntity> {
    
    // GOOD: #{} uses PreparedStatement (safe)
    @Select("SELECT * FROM t_employee WHERE login_name = #{loginName}")
    EmployeeEntity selectByLoginName(@Param("loginName") String loginName);
    
    // BAD: ${} uses string substitution (VULNERABLE!)
    @Select("SELECT * FROM t_employee WHERE login_name = '${loginName}'")
    EmployeeEntity selectByLoginNameUnsafe(@Param("loginName") String loginName);
}
```

**Input validation:**
```java
public class EmployeeAddForm {
    
    @NotBlank(message = "Login name cannot be empty")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "Login name: 4-20 alphanumeric characters")
    private String loginName;
    
    @NotBlank(message = "Name cannot be empty")
    @Size(max = 50, message = "Name cannot exceed 50 characters")
    private String actualName;
}
```

---

### 4. XSS (Cross-Site Scripting) Prevention

**Output encoding in Vue templates:**
```vue
<template>
  <!-- GOOD: Vue auto-escapes HTML -->
  <div>{{ employee.actualName }}</div>
  
  <!-- BAD: v-html renders raw HTML (XSS vulnerable) -->
  <div v-html="employee.actualName"></div>
  
  <!-- Use v-html only for trusted content -->
  <div v-html="trustedHtmlContent" v-if="isTrusted"></div>
</template>
```

**Input sanitization:**
```java
@Service
public class XssProtectionService {
    
    public String sanitizeHtml(String input) {
        if (input == null) {
            return null;
        }
        
        // Remove HTML tags
        return input.replaceAll("<[^>]*>", "")
                    .replaceAll("&", "&amp;")
                    .replaceAll("<", "&lt;")
                    .replaceAll(">", "&gt;")
                    .replaceAll("\"", "&quot;")
                    .replaceAll("'", "&#x27;");
    }
}
```

**Content Security Policy (CSP):**
```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public FilterRegistrationBean<CspFilter> cspFilter() {
        FilterRegistrationBean<CspFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CspFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }
}

public class CspFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setHeader("Content-Security-Policy", 
            "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';");
        chain.doFilter(request, response);
    }
}
```

---

### 5. CSRF (Cross-Site Request Forgery) Prevention

**Sa-Token automatic CSRF protection:**
```java
@Configuration
public class SaTokenConfig {
    @Bean
    public StpInterface stpInterface() {
        return new StpInterface() {
            // Sa-Token automatically validates CSRF token in requests
        };
    }
}
```

**Manual CSRF token validation:**
```java
@PostMapping("/update")
public ResponseDTO<String> updateEmployee(
    @RequestBody @Valid EmployeeUpdateForm form,
    @RequestHeader("X-CSRF-Token") String csrfToken
) {
    // Validate CSRF token
    if (!csrfService.validateToken(csrfToken)) {
        return ResponseDTO.error(ErrorCode.CSRF_TOKEN_INVALID);
    }
    
    return employeeService.updateEmployee(form);
}
```

**Vue CSRF token setup:**
```typescript
// Add CSRF token to Axios headers
axios.interceptors.request.use(config => {
    const csrfToken = localStorage.getItem('csrf-token');
    if (csrfToken) {
        config.headers['X-CSRF-Token'] = csrfToken;
    }
    return config;
});
```

---

### 6. Rate Limiting (DDoS Protection)

**SmartAdmin repeat-submit foundation module:**
```java
@RestController
public class LoginController {
    
    @PostMapping("/login")
    @RepeatSubmit(interval = 5000)  // Prevent repeated submissions within 5 seconds
    public ResponseDTO<String> login(@RequestBody LoginForm form) {
        return loginService.login(form);
    }
}
```

**Redis-based rate limiting:**
```java
@Component
public class RateLimiter {
    private final RedissonClient redissonClient;
    
    public boolean allowRequest(String key, int maxRequests, int windowSeconds) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        rateLimiter.trySetRate(RateType.OVERALL, maxRequests, windowSeconds, RateIntervalUnit.SECONDS);
        
        return rateLimiter.tryAcquire(1);
    }
}

@Service
public class EmployeeService {
    private final RateLimiter rateLimiter;
    
    public ResponseDTO<PageResult<EmployeeVO>> queryEmployee(EmployeeQueryForm form) {
        String key = "employee:query:" + RequestContext.getUserId();
        
        // Limit to 10 requests per minute
        if (!rateLimiter.allowRequest(key, 10, 60)) {
            return ResponseDTO.error(ErrorCode.RATE_LIMIT_EXCEEDED);
        }
        
        // Process query
        return ResponseDTO.ok(employeeDao.queryEmployee(form));
    }
}
```

**Configuration:**
```yaml
sa:
  rate-limit:
    enabled: true
    default-rate: 100  # 100 requests per minute
    burst-capacity: 200  # Allow burst up to 200
    endpoints:
      "/api/login": 5       # 5 requests per minute for login
      "/api/payment/**": 10  # 10 requests per minute for payment
```

---

### 7. Audit Logging (Compliance)

**SmartAdmin audit logging:**
```java
@Service
public class AuditLogService {
    private final AuditLogDao auditLogDao;
    
    public void log(String operation, Object... params) {
        AuditLogEntity log = new AuditLogEntity();
        log.setOperation(operation);
        log.setParams(JsonUtil.toJson(params));
        log.setOperatorId(RequestContext.getUserId());
        log.setIpAddress(RequestContext.getIpAddress());
        log.setUserAgent(RequestContext.getUserAgent());
        log.setTimestamp(LocalDateTime.now());
        
        auditLogDao.insert(log);
    }
}

@Manager
public class WalletManager {
    private final AuditLogService auditLogService;
    
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void processDeposit(TransactionEntity txn) {
        // Process deposit
        WalletEntity wallet = walletDao.selectByPlayerIdForUpdate(txn.getPlayerId());
        BigDecimal newBalance = wallet.getBalance().add(txn.getAmount());
        wallet.setBalance(newBalance);
        walletDao.updateById(wallet);
        
        // MANDATORY: Audit log for financial operation
        auditLogService.log("WALLET_DEPOSIT", 
            txn.getPlayerId(), 
            txn.getAmount(), 
            newBalance, 
            txn.getTransactionId());
    }
}
```

**Audit log query:**
```java
@Service
public class AuditLogService {
    
    public List<AuditLogVO> queryAuditLogs(String operation, Long userId, LocalDateTime startTime) {
        LambdaQueryWrapper<AuditLogEntity> query = Wrappers.<AuditLogEntity>lambdaQuery()
            .eq(operation != null, AuditLogEntity::getOperation, operation)
            .eq(userId != null, AuditLogEntity::getOperatorId, userId)
            .ge(startTime != null, AuditLogEntity::getTimestamp, startTime)
            .orderByDesc(AuditLogEntity::getTimestamp);
        
        List<AuditLogEntity> logs = auditLogDao.selectList(query);
        return SmartBeanUtil.copyList(logs, AuditLogVO.class);
    }
}
```

---

### 8. Authentication & Authorization

**Sa-Token permission checks:**
```java
@RestController
@RequestMapping("/api/employee")
public class EmployeeController {
    
    @PostMapping("/add")
    @SaCheckPermission("employee:add")  // Requires permission
    public ResponseDTO<String> addEmployee(@RequestBody @Valid EmployeeAddForm form) {
        return employeeService.addEmployee(form);
    }
    
    @PostMapping("/query")
    @SaCheckLogin  // Requires login only
    public ResponseDTO<PageResult<EmployeeVO>> queryEmployee(@RequestBody EmployeeQueryForm form) {
        return employeeService.queryEmployee(form);
    }
    
    @GetMapping("/public/info")
    @NoNeedLogin  // Public endpoint (no auth required)
    public ResponseDTO<String> getPublicInfo() {
        return ResponseDTO.ok("Public information");
    }
}
```

**Role-based access control:**
```java
@Service
public class EmployeeService {
    
    public ResponseDTO<String> deleteEmployee(Long employeeId) {
        // Check if user has admin role
        if (!StpUtil.hasRole("admin")) {
            return ResponseDTO.error(ErrorCode.PERMISSION_DENIED);
        }
        
        employeeManager.deleteEmployee(employeeId);
        return ResponseDTO.ok();
    }
}
```

---

### 9. Secure Password Handling

**Password hashing with BCrypt:**
```java
@Service
public class PasswordService {
    
    public String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }
    
    public boolean verifyPassword(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}

@Service
public class EmployeeService {
    private final PasswordService passwordService;
    
    public ResponseDTO<String> addEmployee(EmployeeAddForm form) {
        // Hash password before saving
        String hashedPassword = passwordService.hashPassword(form.getPassword());
        
        EmployeeEntity entity = SmartBeanUtil.copy(form, EmployeeEntity.class);
        entity.setPassword(hashedPassword);
        employeeDao.insert(entity);
        
        return ResponseDTO.ok();
    }
}
```

**Password validation rules:**
```java
public class EmployeeAddForm {
    
    @NotBlank(message = "Password cannot be empty")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
        message = "Password: 8-20 chars, must contain uppercase, lowercase, digit, and special character"
    )
    private String password;
}
```

---

### 10. Secure File Upload

**File validation:**
```java
@Service
public class FileUploadService {
    
    private static final List<String> ALLOWED_EXTENSIONS = 
        Arrays.asList("jpg", "jpeg", "png", "pdf", "doc", "docx");
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    
    public ResponseDTO<String> uploadFile(MultipartFile file) {
        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            return ResponseDTO.userErrorParam("File size exceeds 10 MB limit");
        }
        
        // Validate file extension
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return ResponseDTO.userErrorParam("File type not allowed");
        }
        
        // Validate content type
        String contentType = file.getContentType();
        if (!contentType.startsWith("image/") && !contentType.equals("application/pdf")) {
            return ResponseDTO.userErrorParam("Invalid content type");
        }
        
        // Generate safe filename
        String safeFilename = UUID.randomUUID().toString() + "." + extension;
        
        // Save file
        fileStorageService.save(file, safeFilename);
        
        return ResponseDTO.ok(safeFilename);
    }
}
```

---

## Security Checklist

**API Security:**
- [ ] Authentication enabled (@SaCheckLogin)
- [ ] Authorization checks (@SaCheckPermission)
- [ ] Request/response encryption (@ApiEncrypt)
- [ ] CSRF token validation
- [ ] Rate limiting configured
- [ ] Input validation (@Valid)

**Data Security:**
- [ ] Sensitive fields masked (@MaskPhone, @MaskEmail)
- [ ] Passwords hashed with BCrypt
- [ ] Database credentials in environment variables
- [ ] PII encrypted at rest

**Attack Prevention:**
- [ ] SQL injection: Use parameterized queries (#{})
- [ ] XSS: Vue auto-escapes, avoid v-html
- [ ] CSRF: Token validation enabled
- [ ] DDoS: Rate limiting configured
- [ ] File upload: Validation and size limits

**Compliance (iGaming):**
- [ ] Audit logs for all financial operations
- [ ] KYC verification before withdrawals
- [ ] AML checks for suspicious transactions
- [ ] Transaction limits enforced
- [ ] Data retention policy implemented

---

## References

Detailed security guides:
- [references/api-encryption-guide.md](references/api-encryption-guide.md) - SM2/SM3/SM4 encryption patterns
- [references/owasp-top-10.md](references/owasp-top-10.md) - OWASP Top 10 vulnerabilities and fixes
- [references/igaming-compliance.md](references/igaming-compliance.md) - iGaming compliance requirements

## Time Savings

**Manual Security Implementation:** Varies by feature (4-12 hours)
**Skill-Guided Implementation:** 1-3 hours
**Time Saved: 60-75% reduction**

**Quality Improvements:**
- ✅ Comprehensive security coverage
- ✅ Compliance-ready audit trails
- ✅ Attack prevention best practices
- ✅ SmartAdmin foundation modules leveraged
