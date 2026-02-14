# Backend i18n (MessageSource) Patterns Guide

**Skill:** i18n-generator
**Component:** Spring MessageSource / ResourceBundle
**Purpose:** Implement backend internationalization for SmartAdmin Java applications

---

## Why Backend i18n?

- ✅ Localized error messages and validation
- ✅ Multi-language email templates
- ✅ PDF/Excel reports in user's language
- ✅ Database enum translations
- ✅ Audit logs with localized descriptions

---

## Pattern 1: MessageSource Configuration

### Step 1: Dependencies

```gradle
dependencies {
    // Spring Boot includes i18n support by default
    implementation 'org.springframework.boot:spring-boot-starter-web'
}
```

### Step 2: Configuration

```java
package net.lab1024.sa.foundation.i18n.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.util.Locale;

@Configuration
public class I18nConfig {

    /**
     * MessageSource for loading i18n messages
     */
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();

        // Base name for message files (messages_zh_CN.properties, messages_en_US.properties)
        messageSource.setBasenames("i18n/messages", "i18n/validation", "i18n/enums");

        // UTF-8 encoding for Chinese/Japanese/Arabic
        messageSource.setDefaultEncoding("UTF-8");

        // Fallback to default locale if translation missing
        messageSource.setFallbackToSystemLocale(false);

        // Cache messages for performance (set to -1 for dev, 3600 for prod)
        messageSource.setCacheSeconds(3600);

        return messageSource;
    }

    /**
     * Locale resolver - determines user's locale from request
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();

        // Default locale
        resolver.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);

        // Supported locales
        resolver.setSupportedLocales(List.of(
            Locale.SIMPLIFIED_CHINESE,      // zh_CN
            Locale.US,                       // en_US
            Locale.JAPAN,                    // ja_JP
            new Locale("ar", "SA")           // ar_SA (Arabic - Saudi Arabia)
        ));

        return resolver;
    }

    /**
     * Interceptor to change locale via query parameter (?lang=en)
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }
}
```

### Step 3: Translation Files

```properties
# src/main/resources/i18n/messages_zh_CN.properties
common.success=操作成功
common.error=操作失败
user.not.found=用户不存在
user.create.success=用户创建成功
order.payment.success=订单支付成功：{0}元
order.shipped=订单已发货，追踪号：{0}
```

```properties
# src/main/resources/i18n/messages_en_US.properties
common.success=Operation successful
common.error=Operation failed
user.not.found=User not found
user.create.success=User created successfully
order.payment.success=Order paid successfully: ${0}
order.shipped=Order shipped, tracking number: {0}
```

```properties
# src/main/resources/i18n/messages_ja_JP.properties
common.success=操作が成功しました
common.error=操作が失敗しました
user.not.found=ユーザーが見つかりません
user.create.success=ユーザーが正常に作成されました
order.payment.success=注文が正常に支払われました：{0}円
order.shipped=注文が発送されました、追跡番号：{0}
```

```properties
# src/main/resources/i18n/messages_ar_SA.properties (RTL)
common.success=نجحت العملية
common.error=فشلت العملية
user.not.found=المستخدم غير موجود
user.create.success=تم إنشاء المستخدم بنجاح
order.payment.success=تم دفع الطلب بنجاح: {0} ريال
order.shipped=تم شحن الطلب، رقم التتبع: {0}
```

---

## Pattern 2: I18n Service

```java
package net.lab1024.sa.foundation.i18n.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class I18nService {

    private final MessageSource messageSource;

    /**
     * Get message for current user's locale
     */
    public String getMessage(String code, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, args, locale);
    }

    /**
     * Get message for specific locale
     */
    public String getMessage(String code, Locale locale, Object... args) {
        return messageSource.getMessage(code, args, locale);
    }

    /**
     * Get message with default fallback
     */
    public String getMessageWithDefault(String code, String defaultMessage, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, args, defaultMessage, locale);
    }

    /**
     * Check if translation exists
     */
    public boolean hasMessage(String code) {
        try {
            Locale locale = LocaleContextHolder.getLocale();
            messageSource.getMessage(code, null, locale);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get current user's locale
     */
    public Locale getCurrentLocale() {
        return LocaleContextHolder.getLocale();
    }

    /**
     * Get current locale language code (zh, en, ja, ar)
     */
    public String getCurrentLanguage() {
        return LocaleContextHolder.getLocale().getLanguage();
    }
}
```

---

## Pattern 3: ResponseDTO with i18n

```java
package net.lab1024.sa.common.core.domain.response;

import lombok.Data;
import net.lab1024.sa.foundation.i18n.service.I18nService;

@Data
public class ResponseDTO<T> {

    private Integer code;
    private String msg;
    private T data;
    private Boolean ok;

    /**
     * Success response with i18n message
     */
    public static <T> ResponseDTO<T> okWithI18n(String messageCode, Object... args) {
        I18nService i18nService = SpringUtil.getBean(I18nService.class);
        String message = i18nService.getMessage(messageCode, args);

        ResponseDTO<T> response = new ResponseDTO<>();
        response.setCode(0);
        response.setMsg(message);
        response.setOk(true);
        return response;
    }

    /**
     * Error response with i18n message
     */
    public static <T> ResponseDTO<T> errorWithI18n(String messageCode, Object... args) {
        I18nService i18nService = SpringUtil.getBean(I18nService.class);
        String message = i18nService.getMessage(messageCode, args);

        ResponseDTO<T> response = new ResponseDTO<>();
        response.setCode(-1);
        response.setMsg(message);
        response.setOk(false);
        return response;
    }
}
```

---

## Pattern 4: Localized Validation Messages

### Step 1: Validation Messages File

```properties
# src/main/resources/i18n/validation_zh_CN.properties
validation.user.username.required=用户名不能为空
validation.user.username.length=用户名长度必须在 {min} 到 {max} 之间
validation.user.email.invalid=邮箱格式不正确
validation.user.password.weak=密码强度不足
validation.order.amount.positive=订单金额必须大于0
```

```properties
# src/main/resources/i18n/validation_en_US.properties
validation.user.username.required=Username is required
validation.user.username.length=Username length must be between {min} and {max}
validation.user.email.invalid=Invalid email format
validation.user.password.weak=Password is too weak
validation.order.amount.positive=Order amount must be positive
```

### Step 2: Custom Validator with i18n

```java
package net.lab1024.sa.business.user.validator;

import lombok.RequiredArgsConstructor;
import net.lab1024.sa.foundation.i18n.service.I18nService;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Component
@RequiredArgsConstructor
public class UsernameValidator implements ConstraintValidator<ValidUsername, String> {

    private final I18nService i18nService;

    @Override
    public boolean isValid(String username, ConstraintValidatorContext context) {
        if (username == null || username.isEmpty()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                i18nService.getMessage("validation.user.username.required")
            ).addConstraintViolation();
            return false;
        }

        if (username.length() < 3 || username.length() > 20) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                i18nService.getMessage("validation.user.username.length", 3, 20)
            ).addConstraintViolation();
            return false;
        }

        return true;
    }
}
```

---

## Pattern 5: Enum Translation

### Step 1: Enum with i18n Keys

```java
package net.lab1024.sa.business.order.domain.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatus {

    CREATED("enum.order.status.created"),
    PAID("enum.order.status.paid"),
    SHIPPED("enum.order.status.shipped"),
    DELIVERED("enum.order.status.delivered"),
    CANCELLED("enum.order.status.cancelled");

    private final String i18nKey;
}
```

### Step 2: Enum Translation Files

```properties
# src/main/resources/i18n/enums_zh_CN.properties
enum.order.status.created=已创建
enum.order.status.paid=已支付
enum.order.status.shipped=已发货
enum.order.status.delivered=已送达
enum.order.status.cancelled=已取消
```

```properties
# src/main/resources/i18n/enums_en_US.properties
enum.order.status.created=Created
enum.order.status.paid=Paid
enum.order.status.shipped=Shipped
enum.order.status.delivered=Delivered
enum.order.status.cancelled=Cancelled
```

### Step 3: Enum Translation Service

```java
package net.lab1024.sa.foundation.i18n.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EnumI18nService {

    private final I18nService i18nService;

    /**
     * Get localized enum display name
     */
    public String getEnumDisplay(OrderStatus status) {
        return i18nService.getMessage(status.getI18nKey());
    }

    /**
     * Get all enum values with localized names
     */
    public Map<String, String> getAllOrderStatuses() {
        Map<String, String> statuses = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            statuses.put(status.name(), getEnumDisplay(status));
        }
        return statuses;
    }
}
```

---

## Pattern 6: SmartAdmin Service Integration

```java
package net.lab1024.sa.business.user.service;

import lombok.RequiredArgsConstructor;
import net.lab1024.sa.business.user.domain.form.UserAddForm;
import net.lab1024.sa.business.user.manager.UserManager;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.foundation.i18n.service.I18nService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserManager userManager;
    private final I18nService i18nService;

    public ResponseDTO<Long> createUser(UserAddForm form) {
        // Check if username exists
        if (userManager.existsByUsername(form.getUsername())) {
            return ResponseDTO.errorWithI18n("user.username.exists", form.getUsername());
        }

        // Create user
        Long userId = userManager.createUser(form);

        // Return success with localized message
        return ResponseDTO.okWithI18n("user.create.success");
    }

    public ResponseDTO<Void> deleteUser(Long userId) {
        UserEntity user = userManager.getById(userId);

        if (user == null) {
            return ResponseDTO.errorWithI18n("user.not.found");
        }

        userManager.deleteUser(userId);

        return ResponseDTO.okWithI18n("user.delete.success", user.getUsername());
    }
}
```

---

## Pattern 7: Locale from RequestUser

```java
package net.lab1024.sa.foundation.i18n.interceptor;

import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.request.RequestUser;
import net.lab1024.sa.foundation.sa.token.SmartRequestUserService;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

@Slf4j
@Component
public class UserLocaleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            // Get current user
            RequestUser requestUser = SmartRequestUserService.getRequestUser();

            if (requestUser != null && requestUser.getLocale() != null) {
                // Set locale from user preferences
                Locale locale = Locale.forLanguageTag(requestUser.getLocale());
                LocaleContextHolder.setLocale(locale);
                log.debug("Locale set from user: userId={}, locale={}",
                    requestUser.getUserId(), locale);
            }
        } catch (Exception e) {
            log.warn("Failed to set user locale, using default", e);
        }

        return true;
    }
}
```

### User Entity with Locale

```java
@Data
@TableName("t_user")
public class UserEntity {

    @TableId(type = IdType.AUTO)
    private Long userId;

    private String username;

    private String email;

    /**
     * User's preferred locale (zh_CN, en_US, ja_JP, ar_SA)
     */
    private String locale;

    // Other fields...
}
```

---

## Pattern 8: Localized Email Templates

```java
package net.lab1024.sa.business.notification.service;

import lombok.RequiredArgsConstructor;
import net.lab1024.sa.foundation.i18n.service.I18nService;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.internet.MimeMessage;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class I18nEmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final I18nService i18nService;

    /**
     * Send order confirmation email in user's language
     */
    public void sendOrderConfirmation(UserEntity user, OrderEntity order) {
        Locale locale = Locale.forLanguageTag(user.getLocale());

        // Prepare template context
        Context context = new Context(locale);
        context.setVariable("userName", user.getUsername());
        context.setVariable("orderId", order.getOrderId());
        context.setVariable("totalAmount", order.getTotalAmount());
        context.setVariable("orderDate", order.getCreatedAt());

        // Render email template
        String emailContent = templateEngine.process("email/order-confirmation", context);

        // Get localized subject
        String subject = i18nService.getMessage("email.order.confirmation.subject", locale, order.getOrderId());

        // Send email
        sendEmail(user.getEmail(), subject, emailContent);
    }

    private void sendEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email: to={}", to, e);
        }
    }
}
```

### Email Template (Thymeleaf)

```html
<!-- src/main/resources/templates/email/order-confirmation.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title th:text="#{email.order.confirmation.title}">Order Confirmation</title>
</head>
<body>
    <h1 th:text="#{email.order.greeting(${userName})}">Hello {userName}</h1>

    <p th:text="#{email.order.confirmation.message}">
        Your order has been confirmed.
    </p>

    <ul>
        <li><strong th:text="#{email.order.id}">Order ID</strong>: <span th:text="${orderId}"></span></li>
        <li><strong th:text="#{email.order.amount}">Total Amount</strong>: <span th:text="${totalAmount}"></span></li>
        <li><strong th:text="#{email.order.date}">Order Date</strong>: <span th:text="${#temporals.format(orderDate, 'yyyy-MM-dd HH:mm:ss')}"></span></li>
    </ul>

    <p th:text="#{email.order.thank.you}">Thank you for your order!</p>
</body>
</html>
```

---

## Best Practices

1. **File Organization:**
   ```
   src/main/resources/i18n/
   ├── messages_zh_CN.properties
   ├── messages_en_US.properties
   ├── messages_ja_JP.properties
   ├── messages_ar_SA.properties
   ├── validation_zh_CN.properties
   ├── validation_en_US.properties
   └── enums_zh_CN.properties
   ```

2. **Naming Convention:**
   - Use dot notation: `module.action.result` (e.g., `user.create.success`)
   - Group by feature: `user.*`, `order.*`, `payment.*`
   - Consistent prefixes: `validation.*`, `enum.*`, `email.*`

3. **Parameter Placeholders:**
   - Use `{0}`, `{1}` for positional parameters
   - Use named parameters for clarity: `{userName}`, `{orderId}`

4. **Fallback Strategy:**
   - Always provide default English translation
   - Set `fallbackToSystemLocale=false` to avoid OS locale
   - Return key name if translation missing (for debugging)

5. **Performance:**
   - Cache messages in production (`cacheSeconds=3600`)
   - Reload messages in development (`cacheSeconds=-1`)
   - Use UTF-8 encoding for all files

---

**Next:** [Frontend Vue I18n Setup](frontend-vue-i18n.md)
**Version:** 1.0.0
**Last Updated:** 2026-01-26
