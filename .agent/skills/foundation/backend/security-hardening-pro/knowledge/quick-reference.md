# Security Hardening Pro - Quick Reference

## SM 系列加密

| 算法 | 類型 | 用途 |
|------|------|------|
| SM2 | 非對稱 | 數位簽名、密鑰交換 |
| SM3 | 雜湊 | 密碼儲存、資料完整性 |
| SM4 | 對稱 | 資料加密 |

## SM4 加密範例

```java
@Component
public class Sm4Util {

    private static final String KEY = "your-16-byte-key";

    public static String encrypt(String plainText) {
        SM4 sm4 = SmUtil.sm4(KEY.getBytes());
        return sm4.encryptHex(plainText);
    }

    public static String decrypt(String cipherText) {
        SM4 sm4 = SmUtil.sm4(KEY.getBytes());
        return sm4.decryptStr(cipherText);
    }
}
```

## 資料遮罩註解

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = DataMaskSerializer.class)
public @interface DataMask {
    MaskType type() default MaskType.DEFAULT;
}

public enum MaskType {
    PHONE,      // 138****1234
    ID_CARD,    // 310***********1234
    EMAIL,      // a***@example.com
    DEFAULT     // 前3後4
}
```

## XSS 防護

```java
// 使用 Hutool 清理 HTML
String safe = HtmlUtil.cleanHtmlTag(userInput);

// 或使用 OWASP Java Encoder
String safe = Encode.forHtml(userInput);
```

## CSRF 防護

```java
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http.csrf()
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
        return http.build();
    }
}
```

## 審計日誌

```java
@Aspect
@Component
public class AuditLogAspect {

    @Around("@annotation(auditLog)")
    public Object logOperation(ProceedingJoinPoint pjp, AuditLog auditLog) {
        // 記錄操作人、時間、操作內容
        log.info("User: {}, Action: {}, Time: {}",
            getCurrentUser(), auditLog.action(), LocalDateTime.now());
        return pjp.proceed();
    }
}
```

## 相關規則

- [S01-owasp-top10-part1.md](../../../../rules/security/S01-owasp-top10-part1.md)
- [S02-owasp-top10-part2.md](../../../../rules/security/S02-owasp-top10-part2.md)
