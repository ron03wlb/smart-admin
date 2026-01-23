# P2-20: Localization & Internationalization (i18n)

**Version**: 1.0
**Last Updated**: 2026-01-23
**Status**: Draft
**Priority**: P2 (Enhancement)

---

## Table of Contents

1. [Background & Strategic Context](#1-background--strategic-context)
2. [Requirements](#2-requirements)
3. [Architecture Design](#3-architecture-design)
4. [Supported Languages & Locales](#4-supported-languages--locales)
5. [Backend Internationalization](#5-backend-internationalization)
6. [Frontend Internationalization](#6-frontend-internationalization)
7. [Database Schema Design](#7-database-schema-design)
8. [Translation Management](#8-translation-management)
9. [Locale Detection & Resolution](#9-locale-detection--resolution)
10. [Number, Currency & Date Formatting](#10-number-currency--date-formatting)
11. [Integration Points](#11-integration-points)
12. [SmartAdmin Implementation](#12-smartadmin-implementation)
13. [Testing Strategy](#13-testing-strategy)
14. [Operations & Monitoring](#14-operations--monitoring)
15. [Appendices](#15-appendices)

---

## 1. Background & Strategic Context

### 1.1 Business Context

**From [backend_project.md](../../backend_project.md) Section 3.9**:
> "Multi-tenant white-label frontend enables 100+ merchants to operate with different branding, languages, and currencies while sharing the same backend infrastructure."

**From [igame_str.md](../../igame_str.md) - Zero Marginal Cost Scaling**:
> "Code leverage: One codebase serves 100 merchants. Automation replaces 70% manual work. Data leverage enables intelligent decisions."

### 1.2 Problem Statement

**Current Limitation**:
- No systematic i18n framework for backend error messages, validation messages, or API responses
- Frontend uses hardcoded English strings, limiting market expansion
- Notification templates (P2-17) created in single language per tenant
- CMS content (P1-10) supports multi-language but lacks locale fallback strategy

**Business Impact**:
- **Market Penetration**: Cannot launch in non-English markets (LatAm, Asia, Europe) without manual translation
- **Player Experience**: 68% of players abandon registration if interface is not in native language
- **Operational Overhead**: Each new language requires code changes instead of configuration

### 1.3 Success Criteria

| Metric | Target | Measurement |
|--------|--------|-------------|
| **Supported Languages** | 20+ languages | Initial launch with 5 languages, add 3/quarter |
| **Translation Coverage** | >95% for core flows | Automated missing key detection |
| **Fallback Latency** | <5ms for locale resolution | 3-tier fallback (zh-CN → zh → en) |
| **RTL Support** | Full support for Arabic/Hebrew | Layout auto-flip, no hardcoded LTR |
| **Time to Add Language** | <2 days | Translation file + QA only, no code changes |

---

## 2. Requirements

### 2.1 Functional Requirements

**FR-LOC-001: Multi-Language Support**
- Support 20+ languages across backend and frontend
- Initial launch: English (en), Spanish (es), Portuguese (pt-BR), Chinese (zh-CN), Japanese (ja)
- Graceful fallback: specific locale → language → English (e.g., zh-CN → zh → en)

**FR-LOC-002: Backend Internationalization**
- Error messages, validation messages, and API responses in player's locale
- Exception messages use i18n keys (e.g., `error.wallet.insufficient_funds`) instead of hardcoded strings
- ResponseDTO supports `message_key` for client-side rendering

**FR-LOC-003: Frontend Internationalization**
- All UI labels, buttons, notifications use Vue I18n
- Dynamic locale switching without page reload
- Lazy-loading of translation files (load on demand, not all upfront)

**FR-LOC-004: Database Content Localization**
- Multi-language support for CMS content (from P1-10)
- Notification templates (from P2-17) in multiple languages
- VIP tier names, bonus terms, and promotional content

**FR-LOC-005: Number & Date Formatting**
- Currency display per locale (e.g., $1,234.56 vs €1.234,56)
- Date/time formatting (MM/DD/YYYY vs DD/MM/YYYY vs YYYY-MM-DD)
- Number formatting (1,000,000.00 vs 1.000.000,00)

**FR-LOC-006: Right-to-Left (RTL) Support**
- Automatic layout flip for Arabic (ar), Hebrew (he), Persian (fa)
- Mirrored icons and directional components
- Bi-directional (BiDi) text handling

### 2.2 Non-Functional Requirements

**NFR-LOC-001: Performance**
- Locale resolution: <5ms p95 latency
- Translation lookup: <2ms p95 latency (in-memory cache)
- Frontend bundle size: <50KB per language file

**NFR-LOC-002: Scalability**
- Support 100+ tenants with different default locales
- Handle 10,000+ translation keys without performance degradation

**NFR-LOC-003: Maintainability**
- Translation files in JSON format (frontend) and properties/YAML (backend)
- Automated missing key detection via CI/CD
- Centralized translation management (no scattered i18n strings)

---

## 3. Architecture Design

### 3.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      Player Request                              │
│  Accept-Language: zh-CN,zh;q=0.9,en;q=0.8                       │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│               LocaleInterceptor (Spring MVC)                     │
│  - Parse Accept-Language header                                 │
│  - Check user preference (UserEntity.locale)                    │
│  - Set LocaleContextHolder.setLocale(locale)                    │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                  Backend Processing                              │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Controller Layer                                         │  │
│  │  - Validates input with i18n error messages              │  │
│  │  - Returns ResponseDTO with message_key                  │  │
│  └────────────────┬─────────────────────────────────────────┘  │
│                   │                                              │
│  ┌────────────────▼─────────────────────────────────────────┐  │
│  │ Service/Manager Layer                                    │  │
│  │  - Business logic throws ServiceException(i18nKey)       │  │
│  │  - Uses MessageSource for log messages                   │  │
│  └────────────────┬─────────────────────────────────────────┘  │
│                   │                                              │
│  ┌────────────────▼─────────────────────────────────────────┐  │
│  │ GlobalExceptionHandler                                   │  │
│  │  - Catches exceptions                                    │  │
│  │  - Resolves i18n message via MessageSource               │  │
│  │  - Returns ResponseDTO with both message & message_key   │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                   API Response (JSON)                            │
│  {                                                               │
│    "code": 30001,                                                │
│    "message": "余额不足",  // Pre-rendered in zh-CN              │
│    "message_key": "error.wallet.insufficient_funds",             │
│    "data": null                                                  │
│  }                                                               │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Frontend (Vue 3)                              │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Vue I18n Plugin                                          │  │
│  │  - Detects browser locale (navigator.language)           │  │
│  │  - Loads translation file (lazy-loading)                 │  │
│  │  - Provides $t() helper for component templates          │  │
│  └────────────────┬─────────────────────────────────────────┘  │
│                   │                                              │
│  ┌────────────────▼─────────────────────────────────────────┐  │
│  │ UI Components                                            │  │
│  │  <template>                                              │  │
│  │    <a-button>{{ $t('common.submit') }}</a-button>       │  │
│  │    <span>{{ $n(amount, 'currency') }}</span>            │  │
│  │    <span>{{ $d(date, 'long') }}</span>                  │  │
│  │  </template>                                             │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 Key Design Decisions

**Decision 1: Dual Rendering Strategy**
- **Backend**: Pre-render messages in player's locale for backward compatibility
- **Frontend**: Optionally re-render using `message_key` if available
- **Rationale**: Gradual migration path; legacy clients continue working

**Decision 2: In-Memory Translation Cache**
- **Backend**: Spring MessageSource with `ConcurrentMapMessageSource` (reload without restart)
- **Frontend**: Vue I18n with lazy-loaded chunks (split by module)
- **Rationale**: <2ms lookup latency vs database query (50-100ms)

**Decision 3: Locale Fallback Chain**
- **Pattern**: `zh-CN` (specific) → `zh` (language) → `en` (default)
- **Rationale**: Provide best-effort translation even if specific locale missing

**Decision 4: Translation File Format**
- **Backend**: YAML for readability, properties for compatibility
- **Frontend**: JSON for Vue I18n native support
- **Rationale**: Developer-friendly editing, no compilation step

---

## 4. Supported Languages & Locales

### 4.1 Initial Launch Languages (Phase 1)

| Language | Locale Code | Market | Priority | RTL |
|----------|-------------|--------|----------|-----|
| English | en | Global | P0 | No |
| Spanish | es | LatAm, Spain | P0 | No |
| Portuguese | pt-BR | Brazil | P0 | No |
| Chinese (Simplified) | zh-CN | China | P0 | No |
| Japanese | ja | Japan | P1 | No |

### 4.2 Phase 2 Expansion (Quarters 2-3)

| Language | Locale Code | Market | Priority | RTL |
|----------|-------------|--------|----------|-----|
| German | de | Germany, Austria | P1 | No |
| French | fr | France, Canada | P1 | No |
| Korean | ko | South Korea | P1 | No |
| Russian | ru | Russia, CIS | P1 | No |
| Turkish | tr | Turkey | P1 | No |
| Thai | th | Thailand | P1 | No |
| Vietnamese | vi | Vietnam | P1 | No |

### 4.3 Phase 3 RTL & Niche Markets (Quarters 4+)

| Language | Locale Code | Market | Priority | RTL |
|----------|-------------|--------|----------|-----|
| Arabic | ar | MENA | P2 | **Yes** |
| Hebrew | he | Israel | P2 | **Yes** |
| Persian | fa | Iran | P2 | **Yes** |
| Hindi | hi | India | P2 | No |
| Indonesian | id | Indonesia | P2 | No |
| Polish | pl | Poland | P2 | No |
| Italian | it | Italy | P2 | No |
| Dutch | nl | Netherlands | P2 | No |

### 4.4 Regional Variants

**Differentiated by Currency/Date Format**:
- `en-US` (United States): $ MM/DD/YYYY
- `en-GB` (United Kingdom): £ DD/MM/YYYY
- `en-CA` (Canada): CAD$ YYYY-MM-DD
- `es-ES` (Spain): € DD/MM/YYYY
- `es-MX` (Mexico): MX$ DD/MM/YYYY
- `pt-BR` (Brazil): R$ DD/MM/YYYY
- `pt-PT` (Portugal): € DD/MM/YYYY
- `zh-CN` (China Simplified): ¥ YYYY-MM-DD
- `zh-TW` (Taiwan Traditional): NT$ YYYY/MM/DD

---

## 5. Backend Internationalization

### 5.1 Spring MessageSource Configuration

**File**: `sa-base/infrastructure/web/src/main/java/net/lab1024/sa/base/config/I18nConfig.java`

```java
package net.lab1024.sa.base.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Internationalization Configuration
 *
 * @author lab1024
 * @since 2026-01-23
 */
@Configuration
@RequiredArgsConstructor
public class I18nConfig {

    /**
     * MessageSource for backend i18n messages
     *
     * Translation files located at:
     * - classpath:i18n/messages_en.properties (default)
     * - classpath:i18n/messages_zh_CN.properties
     * - classpath:i18n/messages_es.properties
     */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource =
            new ReloadableResourceBundleMessageSource();

        messageSource.setBasename("classpath:i18n/messages");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messageSource.setDefaultLocale(Locale.ENGLISH);
        messageSource.setFallbackToSystemLocale(false);

        // Cache for 1 hour in production, reload every 5 seconds in dev
        messageSource.setCacheSeconds(3600);

        return messageSource;
    }

    /**
     * LocaleResolver determines player's locale from Accept-Language header
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);

        // Supported locales (fallback to en if unsupported locale requested)
        resolver.setSupportedLocales(List.of(
            Locale.ENGLISH,
            Locale.forLanguageTag("es"),
            Locale.forLanguageTag("pt-BR"),
            Locale.SIMPLIFIED_CHINESE,
            Locale.JAPANESE
        ));

        return resolver;
    }
}
```

### 5.2 LocaleInterceptor

**File**: `sa-base/infrastructure/web/src/main/java/net/lab1024/sa/base/interceptor/LocaleInterceptor.java`

```java
package net.lab1024.sa.base.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.base.module.support.login.domain.RequestEmployee;
import net.lab1024.sa.base.module.support.login.service.LoginService;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

/**
 * Locale Interceptor
 *
 * Resolves player's locale from:
 * 1. User preference (UserEntity.locale) - highest priority
 * 2. Accept-Language header
 * 3. Default locale (en)
 *
 * @author lab1024
 * @since 2026-01-23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocaleInterceptor implements HandlerInterceptor {

    private final LocaleResolver localeResolver;
    private final LoginService loginService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                            Object handler) {
        Locale locale;

        // 1. Check if player is logged in and has locale preference
        try {
            RequestEmployee employee = loginService.getRequestEmployee();
            if (employee != null && employee.getLocale() != null) {
                locale = Locale.forLanguageTag(employee.getLocale());
                log.debug("Using user preference locale: {}", locale);
            } else {
                // 2. Use Accept-Language header
                locale = localeResolver.resolveLocale(request);
                log.debug("Using Accept-Language locale: {}", locale);
            }
        } catch (Exception e) {
            // Player not logged in, use Accept-Language
            locale = localeResolver.resolveLocale(request);
        }

        // Set locale in thread-local context
        LocaleContextHolder.setLocale(locale);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // Clear thread-local to prevent memory leak
        LocaleContextHolder.resetLocaleContext();
    }
}
```

### 5.3 Translation Files (Backend)

**File**: `sa-base/infrastructure/web/src/main/resources/i18n/messages_en.properties`

```properties
# Common Messages
common.success=Operation successful
common.failed=Operation failed
common.invalid_parameter=Invalid parameter: {0}
common.not_found=Resource not found: {0}
common.permission_denied=Permission denied

# Wallet Errors
error.wallet.insufficient_funds=Insufficient balance. Available: {0}, Required: {1}
error.wallet.invalid_currency=Invalid currency: {0}
error.wallet.account_not_found=Wallet account not found for currency {0}
error.wallet.transaction_failed=Transaction failed: {0}

# Bonus Errors
error.bonus.expired=Bonus has expired
error.bonus.already_claimed=Bonus already claimed
error.bonus.wagering_incomplete=Wagering requirement not met. Remaining: {0}
error.bonus.max_bonus_exceeded=Maximum bonus limit exceeded

# VIP Messages
vip.tier.upgraded=Congratulations! You've been upgraded to {0} tier
vip.tier.downgraded=Your VIP tier has been downgraded to {0}
vip.tier.grace_period=Grace period started. Expires on {0}

# Validation Messages
validation.required={0} is required
validation.min={0} must be at least {1}
validation.max={0} must not exceed {1}
validation.email=Invalid email format
validation.phone=Invalid phone number format
```

**File**: `sa-base/infrastructure/web/src/main/resources/i18n/messages_zh_CN.properties`

```properties
# Common Messages
common.success=操作成功
common.failed=操作失败
common.invalid_parameter=参数无效：{0}
common.not_found=资源未找到：{0}
common.permission_denied=权限不足

# Wallet Errors
error.wallet.insufficient_funds=余额不足。可用：{0}，需要：{1}
error.wallet.invalid_currency=货币无效：{0}
error.wallet.account_not_found=未找到货币 {0} 的钱包账户
error.wallet.transaction_failed=交易失败：{0}

# Bonus Errors
error.bonus.expired=奖金已过期
error.bonus.already_claimed=奖金已领取
error.bonus.wagering_incomplete=流水要求未完成。剩余：{0}
error.bonus.max_bonus_exceeded=超过最大奖金限制

# VIP Messages
vip.tier.upgraded=恭喜！您已升级至 {0} 等级
vip.tier.downgraded=您的VIP等级已降至 {0}
vip.tier.grace_period=宽限期已开始。到期时间：{0}

# Validation Messages
validation.required={0} 为必填项
validation.min={0} 最小值为 {1}
validation.max={0} 最大值为 {1}
validation.email=邮箱格式无效
validation.phone=手机号格式无效
```

### 5.4 I18nService for Programmatic Access

**File**: `sa-base/infrastructure/web/src/main/java/net/lab1024/sa/base/service/I18nService.java`

```java
package net.lab1024.sa.base.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * I18n Service
 *
 * Provides programmatic access to translated messages
 *
 * @author lab1024
 * @since 2026-01-23
 */
@Service
@RequiredArgsConstructor
public class I18nService {

    private final MessageSource messageSource;

    /**
     * Get translated message using current thread's locale
     *
     * @param key Message key (e.g., "error.wallet.insufficient_funds")
     * @param args Placeholder values
     * @return Translated message
     */
    public String getMessage(String key, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(key, args, locale);
    }

    /**
     * Get translated message with explicit locale
     */
    public String getMessage(String key, Locale locale, Object... args) {
        return messageSource.getMessage(key, args, locale);
    }

    /**
     * Get translated message with fallback if key not found
     */
    public String getMessageOrDefault(String key, String defaultMessage, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(key, args, defaultMessage, locale);
    }
}
```

### 5.5 Enhanced ResponseDTO with message_key

**File**: `sa-base/foundation/core/src/main/java/net/lab1024/sa/common/core/domain/ResponseDTO.java`

```java
// Add new field to existing ResponseDTO class
@Schema(description = "I18n message key for client-side rendering")
private String messageKey;

// Add new builder method
public static <T> ResponseDTO<T> error(UserErrorCode errorCode, String messageKey, Object... args) {
    ResponseDTO<T> response = new ResponseDTO<>();
    response.setCode(errorCode.getCode());

    // Pre-render message in current locale
    String message = I18nHolder.getMessage(messageKey, args);
    response.setMessage(message);

    // Also provide key for client-side re-rendering
    response.setMessageKey(messageKey);
    response.setData(null);
    response.setSuccess(false);
    return response;
}
```

### 5.6 GlobalExceptionHandler with I18n

**File**: `sa-base/infrastructure/web/src/main/java/net/lab1024/sa/base/handler/GlobalExceptionHandler.java`

```java
@ExceptionHandler(ServiceException.class)
public ResponseDTO<String> handleServiceException(ServiceException ex) {
    log.warn("ServiceException: code={}, messageKey={}",
             ex.getCode(), ex.getMessageKey(), ex);

    // Get translated message
    String message = i18nService.getMessage(ex.getMessageKey(), ex.getArgs());

    ResponseDTO<String> response = new ResponseDTO<>();
    response.setCode(ex.getCode());
    response.setMessage(message);
    response.setMessageKey(ex.getMessageKey());  // For client-side re-rendering
    response.setSuccess(false);

    return response;
}
```

---

## 6. Frontend Internationalization

### 6.1 Vue I18n Setup

**File**: `smart-admin-web/src/i18n/index.ts`

```typescript
import { createI18n } from 'vue-i18n';
import type { I18nOptions } from 'vue-i18n';

// Import default locale (English)
import enUS from './locales/en-US.json';

// Supported locales
export const SUPPORT_LOCALES = [
  { value: 'en-US', label: 'English', flag: '🇺🇸' },
  { value: 'es', label: 'Español', flag: '🇪🇸' },
  { value: 'pt-BR', label: 'Português', flag: '🇧🇷' },
  { value: 'zh-CN', label: '简体中文', flag: '🇨🇳' },
  { value: 'ja', label: '日本語', flag: '🇯🇵' },
];

// Detect browser locale
function getBrowserLocale(): string {
  const browserLocale = navigator.language || 'en-US';

  // Match exact locale (e.g., en-US)
  if (SUPPORT_LOCALES.some(l => l.value === browserLocale)) {
    return browserLocale;
  }

  // Match language only (e.g., en from en-GB)
  const language = browserLocale.split('-')[0];
  const match = SUPPORT_LOCALES.find(l => l.value.startsWith(language));
  return match?.value || 'en-US';
}

// Create i18n instance
const i18n = createI18n({
  legacy: false,  // Use Composition API mode
  locale: getBrowserLocale(),
  fallbackLocale: 'en-US',
  messages: {
    'en-US': enUS,  // Eagerly load default locale
  },
  globalInjection: true,  // Inject $t, $n, $d globally
  missingWarn: false,  // Disable warnings in production
  fallbackWarn: false,
});

export default i18n;
```

### 6.2 Lazy-Loading Translation Files

**File**: `smart-admin-web/src/i18n/loader.ts`

```typescript
import type { I18n } from 'vue-i18n';

// Locale file loaders (dynamic imports for code splitting)
const localeLoaders: Record<string, () => Promise<any>> = {
  'en-US': () => import('./locales/en-US.json'),
  'es': () => import('./locales/es.json'),
  'pt-BR': () => import('./locales/pt-BR.json'),
  'zh-CN': () => import('./locales/zh-CN.json'),
  'ja': () => import('./locales/ja.json'),
};

// Track loaded locales
const loadedLocales = new Set<string>(['en-US']);  // en-US loaded by default

/**
 * Lazy-load translation file for given locale
 */
export async function loadLocale(i18n: I18n, locale: string): Promise<void> {
  // Already loaded
  if (loadedLocales.has(locale)) {
    return;
  }

  // Loader not found
  if (!localeLoaders[locale]) {
    console.warn(`Locale loader not found for: ${locale}`);
    return;
  }

  try {
    const messages = await localeLoaders[locale]();
    i18n.global.setLocaleMessage(locale, messages.default);
    loadedLocales.add(locale);
    console.log(`Locale loaded: ${locale}`);
  } catch (error) {
    console.error(`Failed to load locale ${locale}:`, error);
  }
}

/**
 * Set locale with lazy-loading
 */
export async function setLocale(i18n: I18n, locale: string): Promise<void> {
  await loadLocale(i18n, locale);
  i18n.global.locale.value = locale;

  // Save to localStorage for persistence
  localStorage.setItem('user-locale', locale);

  // Update HTML lang attribute for accessibility
  document.documentElement.setAttribute('lang', locale);

  // Update direction for RTL languages
  const rtlLocales = ['ar', 'he', 'fa'];
  const direction = rtlLocales.includes(locale) ? 'rtl' : 'ltr';
  document.documentElement.setAttribute('dir', direction);
}
```

### 6.3 Translation File Structure (Frontend)

**File**: `smart-admin-web/src/i18n/locales/en-US.json`

```json
{
  "common": {
    "submit": "Submit",
    "cancel": "Cancel",
    "confirm": "Confirm",
    "delete": "Delete",
    "edit": "Edit",
    "save": "Save",
    "search": "Search",
    "reset": "Reset",
    "loading": "Loading...",
    "no_data": "No data available"
  },
  "auth": {
    "login": "Login",
    "register": "Register",
    "logout": "Logout",
    "username": "Username",
    "password": "Password",
    "email": "Email",
    "phone": "Phone Number",
    "login_success": "Login successful",
    "logout_success": "Logout successful"
  },
  "wallet": {
    "balance": "Balance",
    "deposit": "Deposit",
    "withdraw": "Withdraw",
    "transaction_history": "Transaction History",
    "insufficient_funds": "Insufficient balance. Available: {available}, Required: {required}",
    "deposit_success": "Deposit successful: {amount}",
    "withdraw_success": "Withdrawal successful: {amount}"
  },
  "bonus": {
    "available_bonuses": "Available Bonuses",
    "active_bonuses": "Active Bonuses",
    "claim": "Claim",
    "wagering_progress": "Wagering Progress: {current} / {required}",
    "expires_in": "Expires in {days} days"
  },
  "vip": {
    "current_tier": "Current VIP Tier",
    "next_tier": "Next Tier",
    "progress_to_next": "Progress to {tier}",
    "tier_benefits": "Tier Benefits",
    "upgrade_notification": "Congratulations! You've been upgraded to {tier} tier"
  }
}
```

**File**: `smart-admin-web/src/i18n/locales/zh-CN.json`

```json
{
  "common": {
    "submit": "提交",
    "cancel": "取消",
    "confirm": "确认",
    "delete": "删除",
    "edit": "编辑",
    "save": "保存",
    "search": "搜索",
    "reset": "重置",
    "loading": "加载中...",
    "no_data": "暂无数据"
  },
  "auth": {
    "login": "登录",
    "register": "注册",
    "logout": "退出",
    "username": "用户名",
    "password": "密码",
    "email": "邮箱",
    "phone": "手机号",
    "login_success": "登录成功",
    "logout_success": "退出成功"
  },
  "wallet": {
    "balance": "余额",
    "deposit": "充值",
    "withdraw": "提现",
    "transaction_history": "交易记录",
    "insufficient_funds": "余额不足。可用：{available}，需要：{required}",
    "deposit_success": "充值成功：{amount}",
    "withdraw_success": "提现成功：{amount}"
  },
  "bonus": {
    "available_bonuses": "可用奖金",
    "active_bonuses": "激活奖金",
    "claim": "领取",
    "wagering_progress": "流水进度：{current} / {required}",
    "expires_in": "{days}天后过期"
  },
  "vip": {
    "current_tier": "当前VIP等级",
    "next_tier": "下一等级",
    "progress_to_next": "升级至{tier}进度",
    "tier_benefits": "等级特权",
    "upgrade_notification": "恭喜！您已升级至{tier}等级"
  }
}
```

### 6.4 Using I18n in Vue Components

**Example**: Wallet Component

```vue
<template>
  <div class="wallet-page">
    <h1>{{ $t('wallet.balance') }}</h1>

    <!-- Currency formatting with $n() -->
    <div class="balance-amount">
      {{ $n(walletBalance, 'currency') }}
    </div>

    <!-- Date formatting with $d() -->
    <div class="last-update">
      {{ $t('wallet.last_updated') }}: {{ $d(lastUpdatedAt, 'long') }}
    </div>

    <!-- Error message from backend -->
    <a-alert
      v-if="errorMessage"
      type="error"
      :message="errorMessage"
    />

    <!-- Buttons -->
    <a-space>
      <a-button type="primary" @click="showDepositModal">
        {{ $t('wallet.deposit') }}
      </a-button>
      <a-button @click="showWithdrawModal">
        {{ $t('wallet.withdraw') }}
      </a-button>
    </a-space>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { walletApi } from '@/api/wallet';

const { t, n, d } = useI18n();

const walletBalance = ref<number>(0);
const lastUpdatedAt = ref<Date>(new Date());
const errorMessage = ref<string>('');

async function loadWallet() {
  try {
    const response = await walletApi.getBalance();

    if (!response.success) {
      // Re-render backend message using message_key if available
      if (response.messageKey) {
        errorMessage.value = t(response.messageKey, {
          available: n(response.data.available, 'currency'),
          required: n(response.data.required, 'currency'),
        });
      } else {
        // Fallback to pre-rendered message from backend
        errorMessage.value = response.message;
      }
    } else {
      walletBalance.value = response.data.balance;
    }
  } catch (error) {
    errorMessage.value = t('common.network_error');
  }
}
</script>
```

---

## 7. Database Schema Design

### 7.1 UserEntity.locale Field

**File**: `sa-admin/src/main/java/net/lab1024/sa/admin/module/system/employee/domain/entity/EmployeeEntity.java`

```java
/**
 * User's preferred locale (e.g., "zh-CN", "en-US")
 * If null, use Accept-Language header
 */
@Schema(description = "Preferred locale")
@TableField("locale")
private String locale;
```

**Migration**:

```sql
ALTER TABLE t_employee ADD COLUMN locale VARCHAR(10);
CREATE INDEX idx_employee_locale ON t_employee(locale);

COMMENT ON COLUMN t_employee.locale IS 'User preferred locale (e.g., zh-CN, en-US)';
```

### 7.2 Notification Template Localization

**From P2-17: Notification System**

```sql
CREATE TABLE t_notification_template (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    template_code VARCHAR(64) NOT NULL,
    channel VARCHAR(20) NOT NULL,  -- EMAIL, SMS, PUSH, IN_APP
    locale VARCHAR(10) NOT NULL,   -- en, zh-CN, es, pt-BR, ja
    subject TEXT,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(tenant_id, template_code, channel, locale)
);

CREATE INDEX idx_notification_template_lookup
    ON t_notification_template(tenant_id, template_code, channel, locale);
```

**Locale Fallback Query**:

```java
// Try exact match: zh-CN
NotificationTemplate template = dao.selectOne(
    new LambdaQueryWrapper<NotificationTemplate>()
        .eq(NotificationTemplate::getTenantId, tenantId)
        .eq(NotificationTemplate::getTemplateCode, templateCode)
        .eq(NotificationTemplate::getChannel, channel)
        .eq(NotificationTemplate::getLocale, "zh-CN")
);

if (template == null) {
    // Fallback to language: zh
    template = dao.selectOne(/* ... .eq(locale, "zh") */);
}

if (template == null) {
    // Fallback to default: en
    template = dao.selectOne(/* ... .eq(locale, "en") */);
}
```

### 7.3 CMS Content Localization

**From P1-10: Headless CMS Integration**

```sql
-- Strapi auto-generates localized content tables
-- Example: t_cms_banner with locale field

CREATE TABLE t_cms_banner (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    locale VARCHAR(10) NOT NULL,
    title VARCHAR(255) NOT NULL,
    image_url TEXT NOT NULL,
    link_url TEXT,
    display_order INT DEFAULT 0,
    published_at TIMESTAMP,

    UNIQUE(tenant_id, id, locale)  -- Same banner, multiple locales
);
```

---

## 8. Translation Management

### 8.1 Translation Workflow

```
┌────────────────────────────────────────────────────────────┐
│  Step 1: Developer adds i18n keys to code                  │
│  - Backend: i18nService.getMessage("error.wallet.xxx")     │
│  - Frontend: $t('wallet.xxx')                              │
└────────────────┬───────────────────────────────────────────┘
                 │
                 ▼
┌────────────────────────────────────────────────────────────┐
│  Step 2: Extract missing keys (CI/CD)                      │
│  - Run i18n-check.sh script                                │
│  - Detect keys in code not present in translation files    │
│  - Fail build if missing keys found                        │
└────────────────┬───────────────────────────────────────────┘
                 │
                 ▼
┌────────────────────────────────────────────────────────────┐
│  Step 3: Add English translations (baseline)               │
│  - Developer adds key + value to messages_en.properties    │
│  - Developer adds key + value to en-US.json                │
└────────────────┬───────────────────────────────────────────┘
                 │
                 ▼
┌────────────────────────────────────────────────────────────┐
│  Step 4: Professional translation (optional)               │
│  - Export translation files to Lokalise/Crowdin            │
│  - Professional translators translate to other languages   │
│  - Import translated files back                            │
└────────────────┬───────────────────────────────────────────┘
                 │
                 ▼
┌────────────────────────────────────────────────────────────┐
│  Step 5: QA validation                                     │
│  - Native speakers review translations                     │
│  - Check for cultural appropriateness                      │
│  - Test in actual UI/UX context                            │
└────────────────────────────────────────────────────────────┘
```

### 8.2 Missing Key Detection Script

**File**: `scripts/i18n-check.sh`

```bash
#!/bin/bash

echo "Checking for missing i18n keys..."

# Extract all i18n keys from backend code
backend_keys=$(grep -roh 'i18nService.getMessage("\K[^"]+' \
    sa-admin/src sa-base/src \
    | sort -u)

# Extract all keys from frontend code
frontend_keys=$(grep -roh '\$t('\''\K[^'\'']+' \
    smart-admin-web/src \
    | sort -u)

# Check backend keys against English properties file
missing_backend=0
for key in $backend_keys; do
    if ! grep -q "^${key}=" sa-base/infrastructure/web/src/main/resources/i18n/messages_en.properties; then
        echo "❌ Missing backend key: $key"
        missing_backend=$((missing_backend + 1))
    fi
done

# Check frontend keys against English JSON file
missing_frontend=0
for key in $frontend_keys; do
    if ! grep -q "\"${key}\"" smart-admin-web/src/i18n/locales/en-US.json; then
        echo "❌ Missing frontend key: $key"
        missing_frontend=$((missing_frontend + 1))
    fi
done

if [ $missing_backend -eq 0 ] && [ $missing_frontend -eq 0 ]; then
    echo "✅ All i18n keys are present"
    exit 0
else
    echo "❌ Found $missing_backend missing backend keys and $missing_frontend missing frontend keys"
    exit 1
fi
```

---

## 9. Locale Detection & Resolution

### 9.1 Locale Resolution Priority

**Priority Order**:

1. **User Preference** (EmployeeEntity.locale) - Highest priority
2. **Accept-Language Header** - Browser/client preference
3. **Tenant Default Locale** (TenantEntity.defaultLocale) - Merchant setting
4. **System Default** (en) - Fallback

### 9.2 Locale Fallback Algorithm

**Example**: Player requests `zh-TW` (Traditional Chinese)

```
Request: locale=zh-TW
  ↓
Check: messages_zh_TW.properties exists?
  ├─ YES → Use zh-TW
  └─ NO  → Fallback to language
      ↓
    Check: messages_zh.properties exists?
      ├─ YES → Use zh
      └─ NO  → Fallback to default
          ↓
        Use: en (default)
```

---

## 10. Number, Currency & Date Formatting

### 10.1 Backend Formatting (Java)

```java
// Currency formatting
NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(locale);
String formatted = currencyFormat.format(1234.56);
// en-US: $1,234.56
// zh-CN: ¥1,234.56
// es-ES: 1.234,56 €

// Date formatting
DateTimeFormatter dateFormatter = DateTimeFormatter
    .ofLocalizedDate(FormatStyle.LONG)
    .withLocale(locale);
String formatted = LocalDate.now().format(dateFormatter);
// en-US: January 23, 2026
// zh-CN: 2026年1月23日
// es-ES: 23 de enero de 2026
```

### 10.2 Frontend Formatting (Vue I18n)

**File**: `smart-admin-web/src/i18n/index.ts` (add number/date formats)

```typescript
const i18n = createI18n({
  // ... existing config

  numberFormats: {
    'en-US': {
      currency: {
        style: 'currency',
        currency: 'USD',
      },
      decimal: {
        style: 'decimal',
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      },
    },
    'zh-CN': {
      currency: {
        style: 'currency',
        currency: 'CNY',
      },
      decimal: {
        style: 'decimal',
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      },
    },
  },

  datetimeFormats: {
    'en-US': {
      short: {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
      },
      long: {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: 'numeric',
        minute: 'numeric',
      },
    },
    'zh-CN': {
      short: {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
      },
      long: {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: 'numeric',
        minute: 'numeric',
        hour12: false,
      },
    },
  },
});
```

**Usage in Components**:

```vue
<template>
  <!-- Currency -->
  <span>{{ $n(amount, 'currency') }}</span>
  <!-- en-US: $1,234.56 -->
  <!-- zh-CN: ¥1,234.56 -->

  <!-- Decimal -->
  <span>{{ $n(percentage, 'decimal') }}%</span>
  <!-- 95.50% -->

  <!-- Date short -->
  <span>{{ $d(date, 'short') }}</span>
  <!-- en-US: Jan 23, 2026 -->
  <!-- zh-CN: 1月23日, 2026 -->

  <!-- Date long -->
  <span>{{ $d(date, 'long') }}</span>
  <!-- en-US: January 23, 2026 at 2:30 PM -->
  <!-- zh-CN: 2026年1月23日 14:30 -->
</template>
```

---

## 11. Integration Points

### 11.1 Notification System (P2-17)

**Integration**: Multi-language notification templates

```java
@Service
@RequiredArgsConstructor
public class NotificationManager {

    public void sendNotification(Long playerId, String templateCode,
                                NotificationChannel channel, Map<String, Object> variables) {
        // Get player's locale
        String locale = getPlayerLocale(playerId);

        // Resolve template with fallback
        NotificationTemplate template = resolveTemplate(templateCode, channel, locale);

        // Render with locale-specific formatting
        String content = renderTemplate(template.getContent(), variables, locale);

        // Send via channel
        sendViaChannel(channel, getRecipient(playerId, channel), content);
    }

    private NotificationTemplate resolveTemplate(String code, NotificationChannel channel, String locale) {
        // Try exact locale (zh-CN)
        NotificationTemplate template = dao.selectByCodeChannelLocale(code, channel, locale);
        if (template != null) return template;

        // Fallback to language (zh)
        String language = locale.split("-")[0];
        template = dao.selectByCodeChannelLocale(code, channel, language);
        if (template != null) return template;

        // Fallback to English
        return dao.selectByCodeChannelLocale(code, channel, "en");
    }
}
```

### 11.2 CMS Integration (P1-10)

**Integration**: Multi-language content delivery

```java
@Service
@RequiredArgsConstructor
public class CmsContentManager {

    public List<BannerVO> getTenantBanners() {
        String tenantId = TenantContextHolder.getTenantId();
        String locale = LocaleContextHolder.getLocale().toLanguageTag();

        // Fetch from cache with locale key
        String cacheKey = String.format("cms:banners:%s:%s", tenantId, locale);
        List<BannerVO> cached = redisService.get(cacheKey);
        if (cached != null) return cached;

        // Fetch from Strapi with locale filter
        List<BannerVO> banners = strapiClient.getBanners(tenantId, locale);

        // Cache for 5 minutes
        redisService.set(cacheKey, banners, Duration.ofMinutes(5));

        return banners;
    }
}
```

### 11.3 VIP System (P1-11)

**Integration**: Localized VIP tier names and benefits

```java
@Service
@RequiredArgsConstructor
public class VipTierManager {

    public VipTierDetailVO getPlayerVipTierDetail(Long playerId) {
        PlayerVipTier tier = getPlayerTier(playerId);

        // Get localized tier name and benefits
        String locale = LocaleContextHolder.getLocale().toLanguageTag();

        VipTierDetailVO detail = new VipTierDetailVO();
        detail.setTierName(i18nService.getMessage("vip.tier." + tier.getTier().name().toLowerCase()));
        detail.setBenefits(getLocalizedBenefits(tier.getTier(), locale));

        return detail;
    }

    private List<String> getLocalizedBenefits(VipTier tier, String locale) {
        // Benefits stored as i18n keys in database
        return tier.getBenefitKeys().stream()
            .map(key -> i18nService.getMessage(key))
            .collect(Collectors.toList());
    }
}
```

---

## 12. SmartAdmin Implementation

### 12.1 Layer Responsibilities

| Layer | Responsibility | I18n Usage |
|-------|----------------|------------|
| **Controller** | Validate input with i18n error messages | `@Valid` annotations with i18n messages |
| **Service** | Business logic with localized logging | Log messages use i18nService |
| **Manager** | Throw ServiceException with i18n keys | `throw new ServiceException("error.wallet.xxx")` |
| **Dao** | Database queries (no i18n) | N/A |

### 12.2 Example: WalletController with I18n

```java
@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final I18nService i18nService;

    @PostMapping("/withdraw")
    public ResponseDTO<WithdrawResult> withdraw(@RequestBody @Valid WithdrawForm form) {
        try {
            WithdrawResult result = walletService.withdraw(
                form.getPlayerId(),
                form.getAmount(),
                form.getCurrency()
            );

            // Success message with i18n key
            return ResponseDTO.ok(
                result,
                i18nService.getMessage("wallet.withdraw_success", form.getAmount())
            );
        } catch (ServiceException e) {
            // Exception handler will translate message
            throw e;
        }
    }
}
```

### 12.3 Example: WalletManager with I18n Exceptions

```java
@Service
@RequiredArgsConstructor
public class WalletManager {

    @Transactional(rollbackFor = Exception.class)
    public void debit(Long playerId, BigDecimal amount, String currency) {
        WalletAccount account = walletAccountDao.selectOne(
            new LambdaQueryWrapper<WalletAccount>()
                .eq(WalletAccount::getPlayerId, playerId)
                .eq(WalletAccount::getCurrency, currency)
        );

        if (account == null) {
            throw new ServiceException(
                WalletErrorCode.ACCOUNT_NOT_FOUND.getCode(),
                "error.wallet.account_not_found",
                currency
            );
        }

        if (account.getBalance().compareTo(amount) < 0) {
            throw new ServiceException(
                WalletErrorCode.INSUFFICIENT_FUNDS.getCode(),
                "error.wallet.insufficient_funds",
                account.getBalance(),
                amount
            );
        }

        // Debit logic...
    }
}
```

---

## 13. Testing Strategy

### 13.1 Backend I18n Tests

**File**: `sa-base/infrastructure/web/src/test/java/net/lab1024/sa/base/I18nServiceTest.java`

```java
@SpringBootTest
class I18nServiceTest {

    @Autowired
    private I18nService i18nService;

    @Test
    void testGetMessage_English() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        String message = i18nService.getMessage("error.wallet.insufficient_funds", 100, 200);

        assertThat(message).isEqualTo("Insufficient balance. Available: 100, Required: 200");
    }

    @Test
    void testGetMessage_Chinese() {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);

        String message = i18nService.getMessage("error.wallet.insufficient_funds", 100, 200);

        assertThat(message).isEqualTo("余额不足。可用：100，需要：200");
    }

    @Test
    void testFallbackToEnglish() {
        // Use unsupported locale
        LocaleContextHolder.setLocale(Locale.forLanguageTag("xx"));

        String message = i18nService.getMessage("common.success");

        // Should fallback to English
        assertThat(message).isEqualTo("Operation successful");
    }
}
```

### 13.2 Frontend I18n Tests

**File**: `smart-admin-web/src/i18n/__tests__/i18n.test.ts`

```typescript
import { describe, it, expect } from 'vitest';
import { createI18n } from 'vue-i18n';
import enUS from '../locales/en-US.json';
import zhCN from '../locales/zh-CN.json';

describe('i18n', () => {
  const i18n = createI18n({
    legacy: false,
    locale: 'en-US',
    messages: {
      'en-US': enUS,
      'zh-CN': zhCN,
    },
  });

  it('should translate English messages', () => {
    i18n.global.locale.value = 'en-US';

    expect(i18n.global.t('common.submit')).toBe('Submit');
    expect(i18n.global.t('wallet.balance')).toBe('Balance');
  });

  it('should translate Chinese messages', () => {
    i18n.global.locale.value = 'zh-CN';

    expect(i18n.global.t('common.submit')).toBe('提交');
    expect(i18n.global.t('wallet.balance')).toBe('余额');
  });

  it('should handle parameterized messages', () => {
    i18n.global.locale.value = 'en-US';

    const message = i18n.global.t('wallet.insufficient_funds', {
      available: '$100.00',
      required: '$200.00',
    });

    expect(message).toContain('$100.00');
    expect(message).toContain('$200.00');
  });

  it('should format currency', () => {
    i18n.global.locale.value = 'en-US';
    const formatted = i18n.global.n(1234.56, 'currency');
    expect(formatted).toBe('$1,234.56');
  });
});
```

### 13.3 Missing Key Detection Test

```java
@Test
void testAllBackendKeysHaveTranslations() throws IOException {
    // Extract all keys from code
    Set<String> keysInCode = extractI18nKeysFromCode("sa-admin/src", "sa-base/src");

    // Load English properties file
    Properties properties = new Properties();
    properties.load(new FileInputStream("sa-base/infrastructure/web/src/main/resources/i18n/messages_en.properties"));

    // Check all keys exist
    Set<String> missingKeys = keysInCode.stream()
        .filter(key -> !properties.containsKey(key))
        .collect(Collectors.toSet());

    assertThat(missingKeys)
        .as("Missing i18n keys in messages_en.properties")
        .isEmpty();
}
```

---

## 14. Operations & Monitoring

### 14.1 Metrics

**Prometheus Metrics**:

```java
@Component
@RequiredArgsConstructor
public class I18nMetrics {

    private final MeterRegistry meterRegistry;

    @PostConstruct
    public void init() {
        // Counter for missing keys
        meterRegistry.counter("i18n.missing_keys.total");

        // Gauge for loaded locales
        meterRegistry.gauge("i18n.loaded_locales", loadedLocales.size());

        // Histogram for message resolution time
        meterRegistry.timer("i18n.message_resolution.duration");
    }

    public void recordMissingKey(String key, String locale) {
        meterRegistry.counter("i18n.missing_keys.total",
            "key", key,
            "locale", locale
        ).increment();

        log.warn("Missing i18n key: {} for locale: {}", key, locale);
    }
}
```

### 14.2 Logging

```java
@Slf4j
@Component
public class I18nAuditor {

    @EventListener
    public void onLocaleChange(LocaleChangeEvent event) {
        log.info("Locale changed: old={}, new={}, user={}",
            event.getOldLocale(),
            event.getNewLocale(),
            RequestUser.getUserId()
        );
    }

    @EventListener
    public void onMissingKey(MissingKeyEvent event) {
        log.warn("Missing i18n key: key={}, locale={}, requestPath={}",
            event.getKey(),
            event.getLocale(),
            event.getRequestPath()
        );
    }
}
```

### 14.3 Grafana Dashboard

**Queries**:

```promql
# Missing key rate
rate(i18n_missing_keys_total[5m])

# Top 10 missing keys
topk(10, sum by (key) (increase(i18n_missing_keys_total[1h])))

# Locale distribution
sum by (locale) (i18n_locale_requests_total)

# Message resolution latency p95
histogram_quantile(0.95,
  sum(rate(i18n_message_resolution_duration_bucket[5m])) by (le)
)
```

---

## 15. Appendices

### 15.1 Translation File Size Budget

| Locale | Frontend (JSON) | Backend (Properties) | Total |
|--------|-----------------|----------------------|-------|
| en-US | 45 KB | 12 KB | 57 KB |
| zh-CN | 38 KB | 10 KB | 48 KB |
| es | 42 KB | 11 KB | 53 KB |
| pt-BR | 43 KB | 11 KB | 54 KB |
| ja | 41 KB | 11 KB | 52 KB |

**Total Bundle Size**: ~260 KB for 5 languages (lazy-loaded, only 1 loaded at a time)

### 15.2 RTL Language Support Checklist

**When Adding RTL Language (ar, he, fa)**:

- [ ] Add `dir="rtl"` to `<html>` element via JavaScript
- [ ] Use logical CSS properties: `margin-inline-start` instead of `margin-left`
- [ ] Mirror directional icons (arrows, chevrons)
- [ ] Test form layouts (labels should be right-aligned)
- [ ] Test table layouts (columns should flow right-to-left)
- [ ] Verify number formatting (numbers still LTR even in RTL context)
- [ ] Test mixed LTR/RTL content (bi-directional text)

### 15.3 Cross-References

**Related Documents**:
- [P2-17: Notification System](./17-notification-system.md) - Multi-language notification templates
- [P1-10: Headless CMS Integration](../P1-important/10-headless-cms-integration.md) - Multi-language CMS content
- [P1-11: VIP System Design](../P1-important/11-vip-system-design.md) - Localized VIP tier names
- [backend_project.md](../../backend_project.md) - Overall architecture

**Foundation Modules**:
- `.claude/shared/knowledge/smartadmin-patterns.md` - ResponseDTO patterns
- `.agent/rules/01-naming-conventions.md` - Naming standards

### 15.4 Glossary

| Term | Definition |
|------|------------|
| **i18n** | Internationalization (18 letters between 'i' and 'n') |
| **l10n** | Localization (10 letters between 'l' and 'n') |
| **Locale** | Language + Region (e.g., zh-CN, en-US) |
| **RTL** | Right-to-Left (writing direction for Arabic, Hebrew) |
| **BiDi** | Bi-Directional (mixing LTR and RTL text) |
| **ICU** | International Components for Unicode (formatting standard) |

---

**Document End**
