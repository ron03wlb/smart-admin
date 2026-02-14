# Security Hardening Pro - Examples

## 範例 1: 資料遮罩實作

**VO 使用遮罩註解:**
```java
public class UserVO {

    private Long userId;

    private String name;

    @DataMask(type = MaskType.PHONE)
    private String phone;  // 輸出: 138****1234

    @DataMask(type = MaskType.ID_CARD)
    private String idCard;  // 輸出: 310***********1234

    @DataMask(type = MaskType.EMAIL)
    private String email;  // 輸出: a***@example.com
}
```

**遮罩序列化器:**
```java
public class DataMaskSerializer extends JsonSerializer<String> {

    private MaskType maskType;

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) {
        if (value == null) {
            gen.writeNull();
            return;
        }

        String masked = switch (maskType) {
            case PHONE -> maskPhone(value);
            case ID_CARD -> maskIdCard(value);
            case EMAIL -> maskEmail(value);
            default -> maskDefault(value);
        };
        gen.writeString(masked);
    }

    private String maskPhone(String phone) {
        return phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    }
}
```

## 範例 2: SM4 加密服務

```java
@Service
@RequiredArgsConstructor
public class EncryptionService {

    @Value("${security.sm4.key}")
    private String sm4Key;

    public String encryptSensitiveData(String plainText) {
        SM4 sm4 = SmUtil.sm4(sm4Key.getBytes(StandardCharsets.UTF_8));
        return sm4.encryptHex(plainText);
    }

    public String decryptSensitiveData(String cipherText) {
        SM4 sm4 = SmUtil.sm4(sm4Key.getBytes(StandardCharsets.UTF_8));
        return sm4.decryptStr(cipherText);
    }
}
```

## 範例 3: XSS 過濾器

```java
@Component
public class XssFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        XssHttpServletRequestWrapper wrappedRequest =
            new XssHttpServletRequestWrapper((HttpServletRequest) request);
        chain.doFilter(wrappedRequest, response);
    }
}

public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return cleanXss(value);
    }

    private String cleanXss(String value) {
        if (value == null) return null;
        return HtmlUtil.cleanHtmlTag(value);
    }
}
```

## 範例 4: 審計日誌註解

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {
    String action();
    String module() default "";
}

// 使用
@AuditLog(action = "新增員工", module = "員工管理")
public ResponseDTO<String> addEmployee(EmployeeAddForm form) {
    // ...
}
```

## 常見安全問題修復

### SQL 注入防護

```java
// ❌ 危險: 字串拼接
String sql = "SELECT * FROM user WHERE name = '" + name + "'";

// ✅ 安全: 使用參數化查詢
@Select("SELECT * FROM user WHERE name = #{name}")
User findByName(@Param("name") String name);
```

### 敏感資訊日誌

```java
// ❌ 危險: 記錄敏感資訊
log.info("User login: password={}", password);

// ✅ 安全: 遮罩敏感資訊
log.info("User login: password=***");
```
