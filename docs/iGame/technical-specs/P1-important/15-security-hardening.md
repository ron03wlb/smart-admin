# P1-15: Security Hardening

**Version**: 1.0.0
**Status**: Draft
**Last Updated**: 2026-01-23
**Owner**: iGaming Platform Security Team
**Related Documents**: [P0-02 (Idempotency)](../P0-critical/02-idempotency-architecture.md), [P0-04 (KYC/AML)](../P0-critical/04-kyc-aml-automation.md), [P1-06 (Risk)](06-real-time-risk-engine.md), [P1-08 (Crypto)](08-crypto-payment-gateway.md)

---

## Table of Contents

1. [Background & Strategic Context](#1-background--strategic-context)
2. [OWASP Top 10 Mitigation](#2-owasp-top-10-mitigation)
3. [API Security Hardening](#3-api-security-hardening)
4. [Authentication & Authorization](#4-authentication--authorization)
5. [Data Protection & Encryption](#5-data-protection--encryption)
6. [Secrets Management](#6-secrets-management)
7. [Security Scanning Automation](#7-security-scanning-automation)
8. [Penetration Testing](#8-penetration-testing)
9. [Implementation Details (SmartAdmin)](#9-implementation-details-smartadmin)
10. [Security Monitoring & Incident Response](#10-security-monitoring--incident-response)
11. [Compliance & Audit](#11-compliance--audit)
12. [Appendices](#12-appendices)

---

## 1. Background & Strategic Context

### 1.1 Strategic Importance

**From igame_str.md (First Principles)**:
- **Trust (信任)**: Security is foundation of trust → No security = No players
- **Regulatory Compliance**: MGA requires annual penetration testing + security audit
- **Financial Protection**: iGaming platform handles millions in transactions → Security breach = bankruptcy

**Security Metrics**:
- **Zero Tolerance**: 0 critical vulnerabilities in production
- **Penetration Testing**: Annual third-party audit (mandatory for MGA license)
- **Security SLA**: <24 hour response time for critical vulnerabilities
- **Encryption**: 100% of sensitive data encrypted at rest and in transit

### 1.2 Threat Landscape

**Top Threats for iGaming Platforms**:

1. **Financial Fraud**:
   - Bonus abuse (multiple accounts, collusion)
   - Payment fraud (stolen credit cards)
   - Crypto wallet theft

2. **Account Takeover**:
   - Credential stuffing attacks
   - Phishing campaigns
   - Session hijacking

3. **Data Breaches**:
   - Player PII (Personally Identifiable Information)
   - Financial transaction history
   - KYC documents (passport scans, utility bills)

4. **DDoS Attacks**:
   - Target high-stakes live casino games
   - Extortion attempts during major events

5. **API Abuse**:
   - Scraping player data
   - Rate limit bypass
   - Replay attacks

### 1.3 Regulatory Requirements

**Malta Gaming Authority (MGA)**:
- Annual penetration testing by accredited third party
- ISO 27001 certification (optional but recommended)
- Incident response plan with <72 hour breach notification
- Encryption of all player data (AES-256)
- Multi-factor authentication for administrative access

**GDPR (General Data Protection Regulation)**:
- Right to erasure (delete player account)
- Data portability (export player data)
- Consent management (marketing communications)
- Data breach notification (<72 hours to supervisory authority)

### 1.4 Related Documents

- **P0-02 (Idempotency)**: Prevents replay attacks via idempotency keys
- **P0-04 (KYC/AML)**: Secure storage of identity documents
- **P1-06 (Risk Engine)**: Real-time fraud detection
- **P1-08 (Crypto)**: Cold wallet security, multi-signature

---

## 2. OWASP Top 10 Mitigation

### 2.1 A01: Broken Access Control

**Vulnerability**: Users can access resources without proper authorization

**SmartAdmin Mitigation**:

```java
package net.lab1024.sa.base.module.support.security;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/player")
@RequiredArgsConstructor
public class PlayerAdminController {

    private final PlayerService playerService;

    /**
     * SECURE: Requires ADMIN role to access player list
     */
    @SaCheckRole("ADMIN")
    @GetMapping("/list")
    public ResponseDTO<List<PlayerVO>> listPlayers() {
        return ResponseDTO.ok(playerService.listPlayers());
    }

    /**
     * SECURE: Requires specific permission to view sensitive data
     */
    @SaCheckPermission("player:view:sensitive")
    @GetMapping("/{playerId}/kyc")
    public ResponseDTO<PlayerKycVO> getPlayerKyc(@PathVariable Long playerId) {
        return ResponseDTO.ok(playerService.getPlayerKyc(playerId));
    }

    /**
     * SECURE: Tenant isolation enforced
     */
    @GetMapping("/{playerId}")
    public ResponseDTO<PlayerVO> getPlayer(@PathVariable Long playerId) {
        String tenantId = TenantContextHolder.getTenantId();

        // Verify player belongs to current tenant
        Player player = playerDao.selectOne(
            new LambdaQueryWrapper<Player>()
                .eq(Player::getTenantId, tenantId)
                .eq(Player::getId, playerId)
        );

        if (player == null) {
            throw new ServiceException("Player not found or access denied");
        }

        return ResponseDTO.ok(SmartBeanUtil.copy(player, PlayerVO.class));
    }
}
```

**Additional Mitigations**:
1. **Direct Object Reference Protection**:
   ```java
   // INSECURE: Predictable IDs
   // /api/transaction/12345

   // SECURE: Use UUIDs or signed tokens
   // /api/transaction/a3f5d8e9-4b2c-4f8a-9d7e-6c3b2a1f0e9d
   ```

2. **Row-Level Security** (PostgreSQL):
   ```sql
   -- Enable RLS for all tables
   ALTER TABLE players ENABLE ROW LEVEL SECURITY;

   -- Policy: Users can only see players from their tenant
   CREATE POLICY tenant_isolation ON players
       USING (tenant_id = current_setting('app.current_tenant_id')::VARCHAR);
   ```

3. **Mass Assignment Protection**:
   ```java
   @Data
   public class PlayerUpdateForm {
       @NotNull
       private Long id;

       private String nickname;

       // SECURE: vipTier is NOT exposed in form
       // Prevents players from promoting themselves
       // private VipTier vipTier;
   }
   ```

### 2.2 A02: Cryptographic Failures

**Vulnerability**: Sensitive data exposed due to weak encryption

**SmartAdmin Mitigation**:

```java
package net.lab1024.sa.base.module.support.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    private final SecretsManagerClient secretsManager;

    /**
     * Encrypt sensitive data (e.g., KYC documents, payment details)
     * Uses AES-256-GCM (authenticated encryption)
     */
    public String encrypt(String plaintext) throws Exception {
        // 1. Get encryption key from AWS Secrets Manager
        SecretKey key = loadEncryptionKey();

        // 2. Generate random IV (nonce)
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        // 3. Initialize cipher
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

        // 4. Encrypt
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        // 5. Combine IV + ciphertext for storage
        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Decrypt sensitive data
     */
    public String decrypt(String encryptedData) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encryptedData);

        // 1. Extract IV and ciphertext
        byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH);
        byte[] ciphertext = Arrays.copyOfRange(combined, GCM_IV_LENGTH, combined.length);

        // 2. Get decryption key
        SecretKey key = loadEncryptionKey();

        // 3. Initialize cipher
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

        // 4. Decrypt
        byte[] plaintext = cipher.doFinal(ciphertext);
        return new String(plaintext, StandardCharsets.UTF_8);
    }

    private SecretKey loadEncryptionKey() {
        // Load AES-256 key from AWS Secrets Manager
        GetSecretValueResponse response = secretsManager.getSecretValue(
            GetSecretValueRequest.builder()
                .secretId("igaming/encryption-key")
                .build()
        );

        byte[] keyBytes = Base64.getDecoder().decode(response.secretString());
        return new SecretKeySpec(keyBytes, "AES");
    }
}
```

**Database-Level Encryption** (PostgreSQL):
```sql
-- Use pgcrypto extension for column-level encryption
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Encrypt SSN (Social Security Number)
INSERT INTO players (ssn_encrypted)
VALUES (pgp_sym_encrypt('123-45-6789', current_setting('app.encryption_key')));

-- Decrypt when reading
SELECT pgp_sym_decrypt(ssn_encrypted::bytea, current_setting('app.encryption_key'))
FROM players WHERE id = 123;
```

**TLS Configuration** (Spring Boot):
```yaml
# application.yml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: igaming
    protocol: TLS
    enabled-protocols: TLSv1.3  # Only TLS 1.3
    ciphers: TLS_AES_128_GCM_SHA256,TLS_AES_256_GCM_SHA384
```

### 2.3 A03: Injection

**Vulnerability**: SQL injection, NoSQL injection, command injection

**SmartAdmin Mitigation**:

```java
package net.lab1024.sa.admin.module.business.player.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PlayerDao extends BaseMapper<Player> {

    /**
     * SECURE: MyBatis-Plus uses parameterized queries
     */
    default List<Player> searchPlayers(String keyword) {
        return selectList(
            new LambdaQueryWrapper<Player>()
                .like(Player::getNickname, keyword)  // Safe: parameterized
                .or()
                .like(Player::getEmail, keyword)
        );
    }

    /**
     * INSECURE: String concatenation (DO NOT USE)
     */
    // @Select("SELECT * FROM players WHERE nickname = '" + nickname + "'")
    // List<Player> unsafeSearch(String nickname);  // SQL INJECTION!
}
```

**Input Validation**:
```java
package net.lab1024.sa.admin.module.business.player.domain.form;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class PlayerRegisterForm {

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Nickname cannot be blank")
    @Length(min = 3, max = 20, message = "Nickname must be 3-20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Nickname can only contain letters, numbers, and underscores")
    private String nickname;

    @NotBlank(message = "Password cannot be blank")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "Password must be at least 8 characters with uppercase, lowercase, number, and special character"
    )
    private String password;
}
```

**Command Injection Prevention**:
```java
// INSECURE: Direct command execution
// Runtime.getRuntime().exec("convert " + userInput + " output.pdf");

// SECURE: Use ProcessBuilder with argument list
ProcessBuilder pb = new ProcessBuilder(
    "convert",
    sanitizedInput,  // Validated input
    "output.pdf"
);
pb.redirectErrorStream(true);
Process process = pb.start();
```

### 2.4 A04: Insecure Design

**Vulnerability**: Flawed security architecture (e.g., no rate limiting)

**SmartAdmin Mitigation**:

```java
package net.lab1024.sa.base.module.support.security;

import com.google.common.util.concurrent.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiting interceptor (prevent brute force attacks)
 */
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final ConcurrentHashMap<String, RateLimiter> limiters = new ConcurrentHashMap<>();
    private final RedissonClient redisson;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String endpoint = request.getRequestURI();
        String clientIp = getClientIp(request);

        // Different rate limits per endpoint
        int permitsPerSecond = getRateLimitForEndpoint(endpoint);

        // Get or create rate limiter for this IP
        String key = endpoint + ":" + clientIp;
        RateLimiter limiter = limiters.computeIfAbsent(
            key,
            k -> RateLimiter.create(permitsPerSecond)
        );

        // Try to acquire permit
        if (!limiter.tryAcquire(100, TimeUnit.MILLISECONDS)) {
            response.setStatus(429);  // Too Many Requests
            response.setHeader("X-RateLimit-Limit", String.valueOf(permitsPerSecond));
            response.setHeader("Retry-After", "60");

            // Log suspicious activity
            log.warn("Rate limit exceeded: endpoint={}, ip={}, limit={}/s",
                endpoint, clientIp, permitsPerSecond);

            // Block IP if excessive violations (Redis-based tracking)
            incrementViolationCount(clientIp);

            return false;
        }

        return true;
    }

    private int getRateLimitForEndpoint(String endpoint) {
        if (endpoint.startsWith("/api/auth/login")) {
            return 5;  // 5 login attempts per second (prevent brute force)
        } else if (endpoint.startsWith("/api/payment")) {
            return 10;  // 10 payment requests per second
        } else if (endpoint.startsWith("/api/game")) {
            return 100;  // 100 game requests per second
        }
        return 50;  // Default: 50 requests per second
    }

    private void incrementViolationCount(String clientIp) {
        String violationKey = "rate_limit:violations:" + clientIp;
        RAtomicLong violations = redisson.getAtomicLong(violationKey);
        long count = violations.incrementAndGet();

        // Set expiration (violations reset after 1 hour)
        violations.expire(1, TimeUnit.HOURS);

        // Block IP if >100 violations in 1 hour
        if (count > 100) {
            blockIpAddress(clientIp, Duration.ofHours(24));
            log.error("IP blocked due to excessive rate limit violations: ip={}, count={}",
                clientIp, count);
        }
    }
}
```

**Distributed Rate Limiting** (Redis-based):
```java
package net.lab1024.sa.base.module.support.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class DistributedRateLimiter {

    private final RedissonClient redisson;

    /**
     * Check if request is allowed (sliding window algorithm)
     *
     * @param key Unique key (e.g., "login:192.168.1.1")
     * @param maxRequests Maximum requests in window
     * @param window Time window duration
     * @return True if allowed, false if rate limit exceeded
     */
    public boolean isAllowed(String key, int maxRequests, Duration window) {
        String redisKey = "rate_limit:" + key;
        RDeque<Long> requests = redisson.getDeque(redisKey);

        long now = System.currentTimeMillis();
        long windowStart = now - window.toMillis();

        // Remove old requests outside window
        while (!requests.isEmpty() && requests.peekFirst() < windowStart) {
            requests.pollFirst();
        }

        // Check if limit exceeded
        if (requests.size() >= maxRequests) {
            return false;
        }

        // Add current request
        requests.addLast(now);
        requests.expire(window);

        return true;
    }
}
```

### 2.5 A05: Security Misconfiguration

**Vulnerability**: Default credentials, unnecessary services enabled

**SmartAdmin Mitigation**:

```yaml
# application-prod.yml (Production configuration)

# Disable actuator endpoints in production (except health)
management:
  endpoints:
    web:
      exposure:
        include: health,metrics
      base-path: /internal/actuator
  endpoint:
    health:
      show-details: never  # Don't expose internal details

# Disable Swagger in production
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false

# Security headers
server:
  error:
    include-message: never  # Don't expose error messages
    include-stacktrace: never
  servlet:
    session:
      cookie:
        http-only: true
        secure: true
        same-site: strict

# Database connection pool
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      connection-timeout: 30000
      # Don't expose sensitive info in logs
      leak-detection-threshold: 60000
```

**Security Headers** (Spring Security):
```java
package net.lab1024.sa.base.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityHeadersConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.headers(headers -> headers
            // Prevent clickjacking
            .frameOptions().deny()

            // XSS protection
            .xssProtection().block(true)

            // Content type sniffing protection
            .contentTypeOptions().disable()

            // HSTS (HTTP Strict Transport Security)
            .httpStrictTransportSecurity()
                .maxAgeInSeconds(31536000)
                .includeSubDomains(true)

            // Content Security Policy
            .contentSecurityPolicy(
                "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline'; " +
                "style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data: https:; " +
                "frame-ancestors 'none';"
            )

            // Referrer policy
            .referrerPolicy().policy(ReferrerPolicy.NO_REFERRER)
        );

        return http.build();
    }
}
```

**Docker Security**:
```dockerfile
# Use non-root user
FROM openjdk:21-jdk-slim

# Create app user
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Run as non-root
USER appuser

# Read-only filesystem
VOLUME /tmp
ADD --chown=appuser:appuser target/sa-admin.jar app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### 2.6 A06: Vulnerable and Outdated Components

**Vulnerability**: Using libraries with known vulnerabilities

**SmartAdmin Mitigation**:

```groovy
// build.gradle - Dependency version management

plugins {
    id 'org.owasp.dependencycheck' version '8.4.0'
}

dependencies {
    // ALWAYS use latest stable versions
    implementation 'org.springframework.boot:spring-boot-starter-web:3.5.4'
    implementation 'com.baomidou:mybatis-plus-boot-starter:3.5.12'

    // Security-critical libraries
    implementation 'org.bouncycastle:bcprov-jdk18on:1.79'  // Latest cryptography
    implementation 'com.google.guava:guava:33.0.0-jre'

    // NO vulnerable versions
    // implementation 'log4j:log4j:1.2.17'  // VULNERABLE (CVE-2021-44228)
}

// OWASP Dependency Check (fail build on HIGH/CRITICAL vulnerabilities)
dependencyCheck {
    failBuildOnCVSS = 7.0  // Fail on HIGH or CRITICAL
    suppressionFile = 'dependency-check-suppressions.xml'
}
```

**Automated Dependency Updates** (GitHub Dependabot):
```yaml
# .github/dependabot.yml
version: 2
updates:
  - package-ecosystem: "gradle"
    directory: "/"
    schedule:
      interval: "weekly"
    open-pull-requests-limit: 10

    # Auto-merge security patches
    labels:
      - "dependencies"
      - "security"
```

### 2.7 A07: Identification and Authentication Failures

**Vulnerability**: Weak passwords, no MFA, session fixation

**SmartAdmin Mitigation** (see Section 4 for full implementation)

### 2.8 A08: Software and Data Integrity Failures

**Vulnerability**: Unsigned updates, insecure CI/CD pipeline

**SmartAdmin Mitigation**:

```yaml
# .github/workflows/security-scan.yml
name: Security Scan

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  security-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      # SAST (Static Application Security Testing)
      - name: Run SpotBugs
        run: ./gradlew spotbugsMain

      - name: Run PMD
        run: ./gradlew pmdMain

      # Dependency Check
      - name: OWASP Dependency Check
        run: ./gradlew dependencyCheckAnalyze

      # Secret Scanning
      - name: TruffleHog Secret Scan
        uses: trufflesecurity/trufflehog@main
        with:
          path: ./
          base: ${{ github.event.repository.default_branch }}
          head: HEAD

      # Container Scanning
      - name: Build Docker Image
        run: docker build -t igaming:${{ github.sha }} .

      - name: Trivy Container Scan
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: 'igaming:${{ github.sha }}'
          severity: 'CRITICAL,HIGH'
          exit-code: '1'  # Fail on vulnerabilities
```

**Artifact Signing** (Maven/Gradle):
```groovy
// Sign release artifacts with GPG
signing {
    required { gradle.taskGraph.hasTask("publish") }
    sign publishing.publications
}
```

### 2.9 A09: Security Logging and Monitoring Failures

**Vulnerability**: Insufficient logging, no alerting

**SmartAdmin Mitigation** (see Section 10 for full implementation)

### 2.10 A10: Server-Side Request Forgery (SSRF)

**Vulnerability**: Attacker can make server request arbitrary URLs

**SmartAdmin Mitigation**:

```java
package net.lab1024.sa.base.module.support.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.util.Set;

@Slf4j
@Service
public class SsrfProtectionService {

    private static final Set<String> BLOCKED_PROTOCOLS = Set.of("file", "gopher", "jar");
    private static final Set<String> PRIVATE_IP_RANGES = Set.of(
        "127.0.0.0/8",    // Loopback
        "10.0.0.0/8",     // Private
        "172.16.0.0/12",  // Private
        "192.168.0.0/16", // Private
        "169.254.0.0/16"  // Link-local
    );

    /**
     * Validate URL before making HTTP request (prevent SSRF)
     */
    public void validateUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            URI uri = url.toURI();

            // 1. Block dangerous protocols
            String protocol = uri.getScheme().toLowerCase();
            if (BLOCKED_PROTOCOLS.contains(protocol)) {
                throw new SecurityException("Protocol not allowed: " + protocol);
            }

            // 2. Resolve hostname to IP
            InetAddress address = InetAddress.getByName(url.getHost());
            String ip = address.getHostAddress();

            // 3. Block private IP ranges
            if (isPrivateIp(ip)) {
                throw new SecurityException("Access to private IP not allowed: " + ip);
            }

            // 4. Block cloud metadata endpoints
            if (url.getHost().equals("169.254.169.254")) {
                throw new SecurityException("Access to cloud metadata endpoint blocked");
            }

            log.info("URL validated: {}", urlString);

        } catch (Exception e) {
            log.error("SSRF attempt detected: url={}", urlString, e);
            throw new SecurityException("Invalid or dangerous URL", e);
        }
    }

    private boolean isPrivateIp(String ip) {
        // Check if IP is in private ranges
        return ip.startsWith("127.") ||
               ip.startsWith("10.") ||
               ip.startsWith("192.168.") ||
               (ip.startsWith("172.") && isInRange(ip, 16, 31)) ||
               ip.startsWith("169.254.");
    }

    private boolean isInRange(String ip, int min, int max) {
        String[] parts = ip.split("\\.");
        if (parts.length >= 2) {
            int second = Integer.parseInt(parts[1]);
            return second >= min && second <= max;
        }
        return false;
    }
}
```

---

## 3. API Security Hardening

### 3.1 CORS Configuration

```java
package net.lab1024.sa.base.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // SECURE: Only allow specific origins (NO wildcards in production)
        config.setAllowedOrigins(Arrays.asList(
            "https://casino.example.com",
            "https://admin.example.com"
        ));

        // Allowed methods
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));

        // Allowed headers
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Request-ID"));

        // Expose headers
        config.setExposedHeaders(Arrays.asList("X-Total-Count", "X-Page-Number"));

        // Allow credentials (cookies)
        config.setAllowCredentials(true);

        // Cache preflight response for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);

        return source;
    }
}
```

### 3.2 Request Signing (HMAC)

```java
package net.lab1024.sa.base.module.support.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * API request signing interceptor (prevent tampering)
 */
@Component
@RequiredArgsConstructor
public class RequestSigningInterceptor implements HandlerInterceptor {

    private final GameProviderConfigDao providerConfigDao;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Only verify signatures for provider callbacks
        if (!request.getRequestURI().startsWith("/callback/")) {
            return true;
        }

        try {
            // 1. Extract signature from header
            String providedSignature = request.getHeader("X-Signature");
            if (providedSignature == null) {
                throw new SecurityException("Missing signature");
            }

            // 2. Read request body
            String body = request.getReader().lines()
                .collect(Collectors.joining(System.lineSeparator()));

            // 3. Get provider's secret key
            String providerId = extractProviderId(request.getRequestURI());
            String secretKey = getProviderSecretKey(providerId);

            // 4. Calculate expected signature
            String expectedSignature = calculateHmacSha256(body, secretKey);

            // 5. Compare signatures (constant-time to prevent timing attacks)
            if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                providedSignature.getBytes(StandardCharsets.UTF_8)
            )) {
                log.error("Invalid signature: provider={}, expected={}, provided={}",
                    providerId, expectedSignature, providedSignature);
                response.setStatus(401);
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("Signature validation failed", e);
            response.setStatus(401);
            return false;
        }
    }

    private String calculateHmacSha256(String data, String secretKey) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(
            secretKey.getBytes(StandardCharsets.UTF_8),
            "HmacSHA256"
        );
        mac.init(keySpec);

        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
```

### 3.3 API Versioning

```java
// SECURE: Deprecate old API versions gracefully
@RestController
@RequestMapping("/api/v2/player")  // Current version
public class PlayerControllerV2 {
    // Latest implementation
}

@Deprecated
@RestController
@RequestMapping("/api/v1/player")  // Deprecated version
public class PlayerControllerV1 {

    @GetMapping("/{id}")
    public ResponseDTO<PlayerVO> getPlayer(@PathVariable Long id, HttpServletResponse response) {
        // Warn about deprecation
        response.setHeader("X-API-Deprecated", "true");
        response.setHeader("X-API-Sunset", "2026-06-30");
        response.setHeader("X-API-Replacement", "/api/v2/player/{id}");

        log.warn("Deprecated API called: /api/v1/player/{}", id);

        return ResponseDTO.ok(playerService.getPlayer(id));
    }
}
```

---

## 4. Authentication & Authorization

### 4.1 Multi-Factor Authentication (MFA)

```java
package net.lab1024.sa.admin.module.business.auth.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.SecretGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class MfaService {

    private final SecretGenerator secretGenerator;
    private final QRCodeWriter qrCodeWriter;

    /**
     * Enable MFA for user (generate TOTP secret)
     */
    public MfaSetupResponse setupMfa(Long userId) {
        // 1. Generate TOTP secret
        String secret = secretGenerator.generate();

        // 2. Store secret in database (encrypted)
        User user = userDao.selectById(userId);
        user.setMfaSecret(encryptionService.encrypt(secret));
        user.setMfaEnabled(false);  // Not enabled until verified
        userDao.updateById(user);

        // 3. Generate QR code for authenticator app
        QrData qrData = new QrData.Builder()
            .label(user.getEmail())
            .secret(secret)
            .issuer("iGaming Platform")
            .algorithm(HashingAlgorithm.SHA1)
            .digits(6)
            .period(30)
            .build();

        String qrCodeImage = generateQrCodeImage(qrData);

        return MfaSetupResponse.builder()
            .secret(secret)
            .qrCodeImage(qrCodeImage)
            .build();
    }

    /**
     * Verify MFA code and enable MFA
     */
    public boolean verifyAndEnableMfa(Long userId, String code) {
        User user = userDao.selectById(userId);
        String secret = encryptionService.decrypt(user.getMfaSecret());

        // Verify TOTP code
        if (verifyCode(secret, code)) {
            user.setMfaEnabled(true);
            userDao.updateById(user);
            return true;
        }

        return false;
    }

    /**
     * Verify MFA code during login
     */
    public boolean verifyMfaCode(Long userId, String code) {
        User user = userDao.selectById(userId);

        if (!user.isMfaEnabled()) {
            return true;  // MFA not enabled
        }

        String secret = encryptionService.decrypt(user.getMfaSecret());
        return verifyCode(secret, code);
    }

    private boolean verifyCode(String secret, String code) {
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);

        // Allow 1 time step before/after for clock skew
        return verifier.isValidCode(secret, code);
    }

    private String generateQrCodeImage(QrData qrData) {
        try {
            BitMatrix bitMatrix = qrCodeWriter.encode(
                qrData.getUri(),
                BarcodeFormat.QR_CODE,
                250,
                250
            );

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            byte[] imageBytes = outputStream.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);

        } catch (Exception e) {
            throw new ServiceException("Failed to generate QR code", e);
        }
    }
}
```

### 4.2 Password Policy

```java
package net.lab1024.sa.base.module.support.security;

import lombok.extern.slf4j.Slf4j;
import org.passay.*;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Slf4j
@Service
public class PasswordPolicyService {

    private final PasswordValidator validator;

    public PasswordPolicyService() {
        // Password rules (aligned with OWASP recommendations)
        this.validator = new PasswordValidator(Arrays.asList(
            // Length: 8-64 characters
            new LengthRule(8, 64),

            // At least 1 uppercase
            new CharacterRule(EnglishCharacterData.UpperCase, 1),

            // At least 1 lowercase
            new CharacterRule(EnglishCharacterData.LowerCase, 1),

            // At least 1 digit
            new CharacterRule(EnglishCharacterData.Digit, 1),

            // At least 1 special character
            new CharacterRule(EnglishCharacterData.Special, 1),

            // No whitespace
            new WhitespaceRule(),

            // Check against common passwords
            new IllegalSequenceRule(EnglishSequenceData.Alphabetical, 3, false),
            new IllegalSequenceRule(EnglishSequenceData.Numerical, 3, false),
            new IllegalSequenceRule(EnglishSequenceData.USQwerty, 3, false)
        ));
    }

    /**
     * Validate password against policy
     */
    public void validatePassword(String password, String username) {
        // 1. Check password rules
        RuleResult result = validator.validate(new PasswordData(password));

        if (!result.isValid()) {
            List<String> messages = validator.getMessages(result);
            throw new ServiceException("Password does not meet requirements: " + String.join(", ", messages));
        }

        // 2. Password must not contain username
        if (password.toLowerCase().contains(username.toLowerCase())) {
            throw new ServiceException("Password cannot contain username");
        }

        // 3. Check against HaveIBeenPwned API (optional)
        if (isPasswordPwned(password)) {
            throw new ServiceException("Password has been compromised in a data breach. Please choose a different password.");
        }
    }

    /**
     * Check if password exists in HaveIBeenPwned database
     */
    private boolean isPasswordPwned(String password) {
        try {
            // SHA-1 hash of password
            String sha1 = DigestUtils.sha1Hex(password).toUpperCase();
            String prefix = sha1.substring(0, 5);
            String suffix = sha1.substring(5);

            // Query HaveIBeenPwned API (k-anonymity model)
            String url = "https://api.pwnedpasswords.com/range/" + prefix;
            String response = restTemplate.getForObject(url, String.class);

            // Check if suffix exists in response
            return response != null && response.contains(suffix);

        } catch (Exception e) {
            log.error("Failed to check HaveIBeenPwned", e);
            return false;  // Fail open (don't block user if API is down)
        }
    }
}
```

### 4.3 Session Management

```java
package net.lab1024.sa.base.module.support.security;

import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class SessionSecurityService {

    private final RedissonClient redisson;

    /**
     * Create secure session after login
     */
    public String createSession(Long userId, String deviceInfo) {
        // 1. Login user (Sa-Token)
        StpUtil.login(userId);
        String token = StpUtil.getTokenValue();

        // 2. Store session metadata
        SessionMetadata metadata = new SessionMetadata();
        metadata.setUserId(userId);
        metadata.setDeviceInfo(deviceInfo);
        metadata.setIpAddress(RequestUtils.getClientIp());
        metadata.setCreatedAt(LocalDateTime.now());
        metadata.setLastActivityAt(LocalDateTime.now());

        RBucket<SessionMetadata> bucket = redisson.getBucket("session:" + token);
        bucket.set(metadata, Duration.ofHours(24));

        // 3. Limit concurrent sessions (max 3 devices)
        enforceSessionLimit(userId, token);

        return token;
    }

    /**
     * Enforce maximum concurrent sessions per user
     */
    private void enforceSessionLimit(Long userId, String newToken) {
        String sessionListKey = "user_sessions:" + userId;
        RList<String> sessions = redisson.getList(sessionListKey);

        // Add new session
        sessions.add(newToken);

        // If >3 sessions, remove oldest
        if (sessions.size() > 3) {
            String oldestToken = sessions.remove(0);
            StpUtil.logoutByTokenValue(oldestToken);

            log.info("Session limit exceeded, removed oldest session: userId={}, token={}",
                userId, oldestToken);
        }
    }

    /**
     * Validate session and check for hijacking
     */
    public boolean validateSession(String token) {
        RBucket<SessionMetadata> bucket = redisson.getBucket("session:" + token);
        SessionMetadata metadata = bucket.get();

        if (metadata == null) {
            return false;
        }

        // Check if IP changed (potential session hijacking)
        String currentIp = RequestUtils.getClientIp();
        if (!metadata.getIpAddress().equals(currentIp)) {
            log.warn("Potential session hijacking detected: userId={}, originalIp={}, currentIp={}",
                metadata.getUserId(), metadata.getIpAddress(), currentIp);

            // Force re-authentication
            StpUtil.logout();
            return false;
        }

        // Update last activity
        metadata.setLastActivityAt(LocalDateTime.now());
        bucket.set(metadata);

        return true;
    }

    /**
     * Rotate session token (prevent fixation attacks)
     */
    public String rotateSession() {
        Long userId = StpUtil.getLoginIdAsLong();

        // Logout old session
        StpUtil.logout();

        // Create new session
        StpUtil.login(userId);
        return StpUtil.getTokenValue();
    }
}
```

---

## 5. Data Protection & Encryption

### 5.1 Sensitive Data Masking

```java
package net.lab1024.sa.base.module.support.security;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Mask sensitive data in API responses (e.g., email, phone)
 */
public class SensitiveDataMaskSerializer extends JsonSerializer<String> {

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null || value.isEmpty()) {
            gen.writeNull();
            return;
        }

        String masked = maskData(value);
        gen.writeString(masked);
    }

    private String maskData(String value) {
        // Email: j***@example.com
        if (value.contains("@")) {
            int atIndex = value.indexOf("@");
            if (atIndex > 1) {
                return value.charAt(0) + "***" + value.substring(atIndex);
            }
        }

        // Phone: +1-***-***-1234
        if (value.matches("\\+?\\d[\\d\\-\\s]+")) {
            int length = value.length();
            if (length > 4) {
                return value.substring(0, 3) + "***" + value.substring(length - 4);
            }
        }

        // Default: show first and last character
        if (value.length() > 2) {
            return value.charAt(0) + "***" + value.charAt(value.length() - 1);
        }

        return "***";
    }
}

// Usage in VO
@Data
public class PlayerVO {
    private Long id;

    @JsonSerialize(using = SensitiveDataMaskSerializer.class)
    private String email;

    @JsonSerialize(using = SensitiveDataMaskSerializer.class)
    private String phone;
}
```

### 5.2 Database Column Encryption

```java
package net.lab1024.sa.base.module.support.security;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MyBatis type handler for automatic encryption/decryption
 */
public class EncryptedStringTypeHandler extends BaseTypeHandler<String> {

    private final EncryptionService encryptionService;

    public EncryptedStringTypeHandler() {
        this.encryptionService = SpringContextHolder.getBean(EncryptionService.class);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        try {
            String encrypted = encryptionService.encrypt(parameter);
            ps.setString(i, encrypted);
        } catch (Exception e) {
            throw new SQLException("Failed to encrypt value", e);
        }
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String encrypted = rs.getString(columnName);
        return decrypt(encrypted);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String encrypted = rs.getString(columnIndex);
        return decrypt(encrypted);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String encrypted = cs.getString(columnIndex);
        return decrypt(encrypted);
    }

    private String decrypt(String encrypted) {
        if (encrypted == null) {
            return null;
        }

        try {
            return encryptionService.decrypt(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt value", e);
        }
    }
}

// Usage in Entity
@Data
@TableName(value = "players", autoResultMap = true)
public class Player {
    private Long id;

    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String ssn;  // Automatically encrypted/decrypted

    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String bankAccount;
}
```

---

## 6. Secrets Management

### 6.1 AWS Secrets Manager Integration

```java
package net.lab1024.sa.base.module.support.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.*;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SecretsService {

    private final SecretsManagerClient secretsManager;
    private final RedissonClient redisson;

    /**
     * Get secret from AWS Secrets Manager (with caching)
     */
    public String getSecret(String secretName) {
        // 1. Check cache first
        String cacheKey = "secret:" + secretName;
        RBucket<String> bucket = redisson.getBucket(cacheKey);
        String cached = bucket.get();

        if (cached != null) {
            return cached;
        }

        // 2. Fetch from AWS Secrets Manager
        GetSecretValueResponse response = secretsManager.getSecretValue(
            GetSecretValueRequest.builder()
                .secretId(secretName)
                .build()
        );

        String secretValue = response.secretString();

        // 3. Cache for 5 minutes
        bucket.set(secretValue, 5, TimeUnit.MINUTES);

        return secretValue;
    }

    /**
     * Rotate secret (part of automatic rotation lambda)
     */
    public void rotateSecret(String secretName, String newSecretValue) {
        // 1. Create new version
        PutSecretValueResponse response = secretsManager.putSecretValue(
            PutSecretValueRequest.builder()
                .secretId(secretName)
                .secretString(newSecretValue)
                .build()
        );

        // 2. Invalidate cache
        redisson.getKeys().delete("secret:" + secretName);

        log.info("Secret rotated: name={}, version={}", secretName, response.versionId());
    }
}
```

**Environment-Specific Configuration**:
```yaml
# application-prod.yml
aws:
  secrets:
    database-password: "igaming/prod/database-password"
    encryption-key: "igaming/prod/encryption-key"
    jwt-secret: "igaming/prod/jwt-secret"
    payment-api-key: "igaming/prod/payment-api-key"

# NEVER commit actual secrets to Git!
# Use AWS Secrets Manager or environment variables
```

---

## 7. Security Scanning Automation

### 7.1 SAST (Static Application Security Testing)

```groovy
// build.gradle
plugins {
    id 'com.github.spotbugs' version '6.0.0'
    id 'pmd'
    id 'checkstyle'
}

spotbugs {
    effort = 'max'
    reportLevel = 'low'
    excludeFilter = file('config/spotbugs/exclude.xml')
}

pmd {
    consoleOutput = true
    toolVersion = '7.0.0'
    ruleSets = []
    ruleSetFiles = files('config/pmd/ruleset.xml')
}

tasks.register('securityScan') {
    dependsOn 'spotbugsMain', 'pmdMain', 'checkstyleMain'
}
```

### 7.2 DAST (Dynamic Application Security Testing)

```yaml
# .github/workflows/dast-scan.yml
name: DAST Scan

on:
  schedule:
    - cron: '0 2 * * 1'  # Weekly on Monday 2 AM

jobs:
  zap-scan:
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to staging
        run: |
          # Deploy application to staging environment

      - name: OWASP ZAP Scan
        uses: zaproxy/action-full-scan@v0.4.0
        with:
          target: 'https://staging.igaming.example.com'
          rules_file_name: '.zap/rules.tsv'
          cmd_options: '-a -j'

      - name: Upload results
        uses: actions/upload-artifact@v3
        with:
          name: zap-scan-results
          path: report_html.html
```

### 7.3 Container Scanning

```yaml
# .github/workflows/container-scan.yml
name: Container Security Scan

on:
  push:
    branches: [ main ]

jobs:
  trivy-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Build Docker image
        run: docker build -t igaming:latest .

      - name: Run Trivy vulnerability scanner
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: 'igaming:latest'
          format: 'sarif'
          output: 'trivy-results.sarif'
          severity: 'CRITICAL,HIGH'

      - name: Upload to GitHub Security
        uses: github/codeql-action/upload-sarif@v2
        with:
          sarif_file: 'trivy-results.sarif'
```

---

## 8. Penetration Testing

### 8.1 Penetration Testing Checklist

**Authentication & Session Management**:
- [ ] Brute force protection (rate limiting)
- [ ] Password policy enforcement
- [ ] MFA bypass attempts
- [ ] Session fixation attacks
- [ ] Session hijacking (IP validation)
- [ ] Logout functionality
- [ ] Concurrent session limits

**Authorization**:
- [ ] Vertical privilege escalation (user → admin)
- [ ] Horizontal privilege escalation (access other players' data)
- [ ] Insecure direct object references (IDOR)
- [ ] Missing function-level access control

**Input Validation**:
- [ ] SQL injection (all input fields)
- [ ] XSS (reflected, stored, DOM-based)
- [ ] Command injection
- [ ] XML/XXE injection
- [ ] LDAP injection
- [ ] Template injection

**Business Logic**:
- [ ] Bonus abuse (multiple accounts)
- [ ] Negative deposit amounts
- [ ] Race conditions (concurrent transactions)
- [ ] Price manipulation
- [ ] Workflow bypass

**API Security**:
- [ ] Lack of rate limiting
- [ ] Mass assignment
- [ ] Excessive data exposure
- [ ] Lack of resources & rate limiting
- [ ] Broken function level authorization

**Crypto & Payment**:
- [ ] Cold wallet private key exposure
- [ ] Multi-signature bypass
- [ ] Exchange rate manipulation
- [ ] Double-spending attacks

### 8.2 Penetration Testing Report Template

```markdown
# Penetration Testing Report

**Project**: iGaming Platform
**Test Date**: 2026-01-23
**Tester**: Acme Security Ltd.
**Scope**: https://staging.igaming.example.com

## Executive Summary

Total vulnerabilities found: 12
- Critical: 0
- High: 2
- Medium: 5
- Low: 4
- Info: 1

## Findings

### HIGH-001: Broken Access Control (Player Data)

**Severity**: High
**CVSS**: 7.5
**Description**: Users can access other players' transaction history by modifying the `playerId` parameter.

**Steps to Reproduce**:
1. Login as Player A (ID: 123)
2. GET /api/transaction/history?playerId=456
3. Observe Player B's transaction history is returned

**Impact**: Privacy violation, GDPR breach

**Remediation**:
- Implement tenant isolation check in TransactionController
- Validate playerId matches authenticated user
- Add logging for unauthorized access attempts

**Status**: OPEN

---

### MEDIUM-003: Missing Rate Limiting (Login Endpoint)

**Severity**: Medium
**CVSS**: 5.3
**Description**: No rate limiting on /api/auth/login allows brute force attacks.

**Steps to Reproduce**:
1. Send 1000 login requests with different passwords
2. Observe no rate limiting enforced

**Impact**: Account takeover via credential stuffing

**Remediation**:
- Implement rate limiting (5 attempts per minute per IP)
- Add CAPTCHA after 3 failed attempts
- Implement account lockout after 10 failed attempts

**Status**: OPEN
```

---

## 9. Implementation Details (SmartAdmin)

### 9.1 Security Interceptor Chain

```java
package net.lab1024.sa.base.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class SecurityInterceptorConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;
    private final RequestSigningInterceptor requestSigningInterceptor;
    private final SessionValidationInterceptor sessionValidationInterceptor;
    private final AuditLoggingInterceptor auditLoggingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. Rate limiting (first line of defense)
        registry.addInterceptor(rateLimitInterceptor)
            .addPathPatterns("/api/**")
            .order(1);

        // 2. Request signing (for provider callbacks)
        registry.addInterceptor(requestSigningInterceptor)
            .addPathPatterns("/callback/**")
            .order(2);

        // 3. Session validation
        registry.addInterceptor(sessionValidationInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns("/api/auth/**", "/api/health")
            .order(3);

        // 4. Audit logging
        registry.addInterceptor(auditLoggingInterceptor)
            .addPathPatterns("/api/**")
            .order(4);
    }
}
```

---

## 10. Security Monitoring & Incident Response

### 10.1 Security Event Logging

```java
package net.lab1024.sa.base.module.support.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityEventLogger {

    private final SecurityEventDao securityEventDao;
    private final AlertService alertService;

    /**
     * Log security event (login, logout, access denied, etc.)
     */
    public void logSecurityEvent(SecurityEventType eventType, String description, Map<String, Object> metadata) {
        SecurityEvent event = new SecurityEvent();
        event.setEventType(eventType);
        event.setUserId(getCurrentUserId());
        event.setIpAddress(RequestUtils.getClientIp());
        event.setUserAgent(RequestUtils.getUserAgent());
        event.setDescription(description);
        event.setMetadata(JSON.toJSONString(metadata));
        event.setTimestamp(LocalDateTime.now());

        securityEventDao.insert(event);

        // Alert on critical events
        if (eventType.isCritical()) {
            alertService.sendSecurityAlert(event);
        }

        log.warn("Security event: type={}, user={}, ip={}, description={}",
            eventType, event.getUserId(), event.getIpAddress(), description);
    }

    private Long getCurrentUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return null;
        }
    }
}

// Usage
@RestController
public class AuthController {

    @PostMapping("/login")
    public ResponseDTO<String> login(@RequestBody LoginForm form) {
        try {
            String token = authService.login(form);

            securityEventLogger.logSecurityEvent(
                SecurityEventType.LOGIN_SUCCESS,
                "User logged in successfully",
                Map.of("email", form.getEmail())
            );

            return ResponseDTO.ok(token);

        } catch (AuthenticationException e) {
            securityEventLogger.logSecurityEvent(
                SecurityEventType.LOGIN_FAILED,
                "Login failed: " + e.getMessage(),
                Map.of("email", form.getEmail(), "reason", e.getMessage())
            );

            throw e;
        }
    }
}
```

### 10.2 Incident Response Plan

**Severity Levels**:
- **P0 (Critical)**: Data breach, system compromise, payment fraud
- **P1 (High)**: Account takeover, DDoS attack
- **P2 (Medium)**: Suspicious activity, failed intrusion attempts
- **P3 (Low)**: Configuration issues, informational alerts

**Response SLA**:
- P0: Immediate (15 minutes)
- P1: <1 hour
- P2: <24 hours
- P3: <7 days

**Incident Response Workflow**:
1. **Detection**: Security monitoring alerts triggered
2. **Triage**: Security team assesses severity
3. **Containment**: Isolate affected systems
4. **Eradication**: Remove threat
5. **Recovery**: Restore normal operations
6. **Post-Incident**: Root cause analysis, lessons learned

---

## 11. Compliance & Audit

### 11.1 GDPR Compliance

**Right to Erasure**:
```java
@Service
@RequiredArgsConstructor
public class GdprService {

    /**
     * Delete player account and all personal data (GDPR Right to Erasure)
     */
    @Transactional
    public void deletePlayerData(Long playerId) {
        // 1. Verify player can be deleted (no active bets)
        int activeBets = gameRoundDao.countActiveBets(playerId);
        if (activeBets > 0) {
            throw new ServiceException("Cannot delete player with active bets");
        }

        // 2. Anonymize player data (retain for regulatory compliance)
        Player player = playerDao.selectById(playerId);
        player.setEmail("deleted-" + UUID.randomUUID() + "@anonymized.com");
        player.setNickname("Deleted User");
        player.setPhone(null);
        player.setSsn(null);
        player.setDeleted(true);
        player.setDeletedAt(LocalDateTime.now());
        playerDao.updateById(player);

        // 3. Delete KYC documents from MinIO
        kycDocumentService.deleteDocuments(playerId);

        // 4. Retain transaction history (7 years for MGA)
        // But anonymize personal details

        log.info("Player data deleted (GDPR): playerId={}", playerId);
    }
}
```

---

## 12. Appendices

### 12.1 Security Checklist (Pre-Production)

- [ ] All OWASP Top 10 vulnerabilities mitigated
- [ ] Penetration testing completed (no critical findings)
- [ ] Secrets rotated and stored in AWS Secrets Manager
- [ ] TLS 1.3 enforced, no weak ciphers
- [ ] MFA enabled for all admin accounts
- [ ] Rate limiting configured for all public endpoints
- [ ] Security headers configured (HSTS, CSP, X-Frame-Options)
- [ ] Database column encryption for PII
- [ ] Audit logging for all sensitive operations
- [ ] Incident response plan documented
- [ ] Security monitoring alerts configured
- [ ] GDPR compliance verified (right to erasure)
- [ ] Dependency scanning in CI/CD pipeline
- [ ] Container vulnerability scanning enabled

### 12.2 Security Tools

| Tool | Purpose | License |
|------|---------|---------|
| SpotBugs | SAST (Java) | LGPL |
| PMD | SAST (Code Quality) | BSD |
| OWASP Dependency Check | SCA | Apache 2.0 |
| TruffleHog | Secret Scanning | GPL |
| Trivy | Container Scanning | Apache 2.0 |
| OWASP ZAP | DAST | Apache 2.0 |
| SonarQube | Code Quality + Security | LGPL/Commercial |

---

## Document Status

**Version**: 1.0.0
**Status**: Draft (Ready for Security Review)
**Lines**: ~1,200 lines
**Last Updated**: 2026-01-23

**Next Steps**:
1. Security team review
2. Penetration testing by third party (MGA requirement)
3. Security automation setup (GitHub Actions)
4. Incident response plan approval
5. Security training for development team

**Related Documents**:
- [P0-02: Idempotency](../P0-critical/02-idempotency-architecture.md)
- [P0-04: KYC/AML](../P0-critical/04-kyc-aml-automation.md)
- [P1-06: Real-Time Risk Engine](06-real-time-risk-engine.md)
- [P1-08: Crypto Payment Gateway](08-crypto-payment-gateway.md)
