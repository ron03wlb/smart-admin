---
trigger: always_on
description: OWASP Top 10 Security Standards Part 2 (A05-A10)
tags: [security, owasp, authentication, ssrf, logging]
positioning: current-standard
prerequisites:
  - rules/07-owasp-top10-part1.md
last_updated: 2025-01-12
---

# OWASP Top 10 Part 2 - Configuration & Authentication

Based on OWASP Top 10 (2021), covering configuration, authentication, logging, and SSRF protection.

---

## A05 - Security Misconfiguration

### [Mandatory] Production Environment Configuration

**Key Requirements**:
- Only expose necessary endpoints (health, info, metrics)
- Disable error stack traces and detailed messages
- Turn off development tools (H2 Console, Swagger UI)

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

### [Mandatory] Security Headers

**Must Enable**:
- `X-Frame-Options: DENY`
- `Strict-Transport-Security: max-age=31536000; includeSubDomains`
- `X-Content-Type-Options: nosniff`

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

## A06 - Vulnerable Components

### [Mandatory] Dependency Vulnerability Scanning

**CI/CD Integration**:
- Maven: OWASP Dependency-Check
- Daily automatic scanning
- CVSS ≥ 7.0 fails the build

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

**Execution Command**:
```bash
mvn dependency-check:check
```

---

## A07 - Authentication Failures

### [Mandatory] JWT Security Configuration

**Standard Requirements**:
- Algorithm: HS512 (minimum 512 bits key)
- Expiration: ≤ 1 hour
- JTI (JWT ID) for replay attack prevention
- Key Storage: Environment variable or Vault

```java
@PostConstruct
protected void init() {
    // HS512 requires at least 512 bits key
    this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
}

public String createToken(String username, List<String> roles) {
    return Jwts.builder()
        .setSubject(username)
        .claim("roles", roles)
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + 3600000))
        .setId(UUID.randomUUID().toString()) // JTI for replay prevention
        .signWith(key, SignatureAlgorithm.HS512)
        .compact();
}

// ❌ Wrong
.signWith(SignatureAlgorithm.HS256, "weak-key");
.signWith(SignatureAlgorithm.NONE);
```

### [Mandatory] Session Security

**Cookie Attributes**:
- `HttpOnly=true` (prevent XSS)
- `Secure=true` (HTTPS only)
- `SameSite=Strict` (prevent CSRF)
- `Max-Age=3600` (1 hour)

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

## A08 - Data Integrity Failures

### [Mandatory] Deserialization Security

**Protection Measures**:
- Prohibit ObjectInputStream from untrusted sources
- Use whitelist to validate class names
- Prefer JSON over Java serialization

```java
// ❌ Forbidden
ObjectInputStream ois = new ObjectInputStream(untrustedInput);
Object obj = ois.readObject(); // RCE risk

// ✅ Correct - Whitelist
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

## A09 - Logging Failures

### [Mandatory] Security Event Logging

**Must Log Events**:
- Authentication success/failure
- Authorization failure
- Input validation failure
- Sensitive data access (query, modify, delete)
- Configuration changes

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

**Log Format Requirements**:
- Structured logs (JSON or fixed format)
- Include: Timestamp, user ID, IP, action, result
- Prohibit logging sensitive data (passwords, tokens, credit cards)

---

## A10 - SSRF (Server-Side Request Forgery)

### [Mandatory] SSRF Protection

**Three-Layer Defense Strategy**:
1. Protocol whitelist (http/https only)
2. Host whitelist (trusted domains)
3. Block internal addresses (Loopback, Site-Local)

```java
private static final Set<String> ALLOWED_HOSTS = Set.of("api.trusted.com");

public String fetchUrl(String urlString) throws IOException {
    URL url = new URL(urlString);

    // 1. Protocol whitelist
    if (!Set.of("http", "https").contains(url.getProtocol())) {
        throw new SecurityException("Blocked scheme");
    }

    // 2. Host whitelist
    if (!ALLOWED_HOSTS.contains(url.getHost())) {
        throw new SecurityException("Host not allowed");
    }

    // 3. Block internal addresses
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

**Additional Protection**:
- Disable HTTP redirects (`setInstanceFollowRedirects(false)`)
- Set connection timeout (≤ 5 seconds)
- Validate Content-Type

---

## FindSecBugs Required Rules

| Rule ID                 | Name               | Severity |
| ----------------------- | ------------------ | -------- |
| SQL_INJECTION           | SQL Injection      | Critical |
| COMMAND_INJECTION       | Command Injection  | Critical |
| PATH_TRAVERSAL_IN       | Path Traversal     | Critical |
| XXE_DOCUMENT            | XXE Attack         | Critical |
| XSS_REQUEST_WRAPPER     | XSS                | High     |
| WEAK_MESSAGE_DIGEST_MD5 | Weak Hash          | High     |
| HARD_CODE_PASSWORD      | Hardcoded Password | High     |

**Integration Configuration**:
```xml
<plugin>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-maven-plugin</artifactId>
    <configuration>
        <plugins>
            <plugin>
                <groupId>com.h3xstream.findsecbugs</groupId>
                <artifactId>findsecbugs-plugin</artifactId>
                <version>1.13.0</version>
            </plugin>
        </plugins>
    </configuration>
</plugin>
```

---

## Security Checklist

**Development Phase**:
- [ ] No SQL/Command injection risks (Lambda Query, parameter binding)
- [ ] All input validated with @Valid
- [ ] Passwords use BCrypt, sensitive data uses AES-GCM
- [ ] No hardcoded secrets (keys, passwords, tokens)
- [ ] Implement method-level authorization (@PreAuthorize)

**Pre-Deployment**:
- [ ] SpotBugs + FindSecBugs: No Critical/High warnings
- [ ] Dependency Check: No CVSS ≥ 7.0 vulnerabilities
- [ ] Production configuration reviewed (disable dev tools, error details)
- [ ] Security Headers enabled (HSTS, CSP, X-Frame-Options)
- [ ] Security event logging configured (authentication, authorization, sensitive operations)

---

## Related Standards

- **Part 1**: `rules/07-owasp-top10-part1.md` - A01-A04 Access Control, Encryption, Injection
- **CI/CD Pipeline**: `workflows/java-ci-cd-pipeline.md` - Automated Security Scanning
- **MyBatis Plus**: `rules/09-mybatis-plus.md` - SQL Injection Protection
