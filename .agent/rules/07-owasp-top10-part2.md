---
trigger: always_on
description: OWASP Top 10 安全規範 Part 2 (A05-A10)
tags: [security, owasp, authentication, ssrf, logging]
positioning: current-standard
prerequisites: [rules/07-owasp-top10-part1.md]
last_updated: 2025-01-12
---

# OWASP Top 10 Part 2 - Configuration & Authentication

基於 OWASP Top 10 (2021)，涵蓋配置、認證、日誌及 SSRF 防護。

---

## A05 - Security Misconfiguration（安全配置錯誤）

### 【強制】生產環境配置

**關鍵要求**：
- 僅暴露必要的端點（health, info, metrics）
- 禁用錯誤堆疊追蹤和詳細訊息
- 關閉開發工具（H2 Console, Swagger UI）

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

**必須啟用**：
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

## A06 - Vulnerable Components（易受攻擊組件）

### 【強制】依賴漏洞掃描

**CI/CD 集成**：
- Maven：OWASP Dependency-Check
- 每日自動掃描
- CVSS ≥ 7.0 中斷構建

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

**執行命令**：
```bash
mvn dependency-check:check
```

---

## A07 - Authentication Failures（身份驗證失效）

### 【強制】JWT 安全配置

**標準要求**：
- 算法：HS512（至少 512 bits 密鑰）
- 過期時間：≤ 1 小時
- JTI（JWT ID）防重放攻擊
- 密鑰存儲：環境變量或 Vault

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

**Cookie 屬性**：
- `HttpOnly=true`（防 XSS）
- `Secure=true`（僅 HTTPS）
- `SameSite=Strict`（防 CSRF）
- `Max-Age=3600`（1 小時）

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

**防護措施**：
- 禁用不受信任來源的 ObjectInputStream
- 使用白名單驗證類名
- 優先使用 JSON 而非 Java 序列化

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

**必須記錄事件**：
- 認證成功/失敗
- 授權失敗
- 輸入驗證失敗
- 敏感數據訪問（查詢、修改、刪除）
- 配置變更

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

**日誌格式要求**：
- 結構化日誌（JSON 或固定格式）
- 包含：時間戳、用戶 ID、IP、操作、結果
- 禁止記錄敏感數據（密碼、Token、信用卡號）

---

## A10 - SSRF（服務端請求偽造）

### 【強制】SSRF 防護

**三層防護策略**：
1. 協議白名單（僅 http/https）
2. 主機白名單（信任域名）
3. 禁止內網地址（Loopback、Site-Local）

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

**額外保護**：
- 禁用 HTTP 重定向（`setInstanceFollowRedirects(false)`）
- 設置連接超時（≤ 5 秒）
- 驗證 Content-Type

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

**集成配置**：
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

## 安全檢查清單

**開發階段**：
- [ ] 無 SQL/Command 注入風險（Lambda 查詢、參數綁定）
- [ ] 所有輸入經過 @Valid 驗證
- [ ] 密碼使用 BCrypt，敏感數據 AES-GCM
- [ ] 無硬編碼機密（密鑰、密碼、Token）
- [ ] 實現方法級授權（@PreAuthorize）

**部署前**：
- [ ] SpotBugs + FindSecBugs 無 Critical/High 警告
- [ ] Dependency Check 無 CVSS ≥ 7.0 漏洞
- [ ] 生產配置已審查（關閉開發工具、錯誤詳情）
- [ ] Security Headers 已啟用（HSTS, CSP, X-Frame-Options）
- [ ] 安全事件日誌已配置（認證、授權、敏感操作）

---

## 相關規範

- **Part 1**：`rules/07-owasp-top10-part1.md` - A01-A04 訪問控制、加密、注入
- **CI/CD Pipeline**：`workflows/java-ci-cd-pipeline.md` - 自動化安全掃描
- **MyBatis Plus**：`rules/09-mybatis-plus.md` - SQL 注入防護
