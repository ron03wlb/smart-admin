---
name: i18n-generator
description: [P2 - Productivity] Generate internationalization (i18n) support for SmartAdmin applications (backend MessageSource + frontend Vue I18n) with locale formatting, RTL layouts, translation key extraction, and translation management workflow. Use when implementing multi-language support, global deployment, or locale-specific features. Triggers when user mentions "i18n", "internationalization", "multi-language", "translation", "locale", "RTL", or "global deployment".
---

# Internationalization (i18n) Generator

**Priority:** P1 - Roadmap Priority #1
**Sprint:** 3 (Weeks 9-11)
**Status:** ✅ Production Ready

## Purpose

Enable global deployment by generating complete i18n infrastructure. Supports SmartAdmin deployment in 5+ languages with systematic translation management.

## Trigger Keywords

This skill is automatically activated when the user's request contains:

**Primary Keywords** (High confidence):
- "i18n" - Internationalization infrastructure
- "internationalization" - Multi-language support
- "multi-language" - Multiple language support
- "translation" - Translation management
- "locale" - Locale configuration

**Secondary Keywords** (Medium confidence):
- "RTL" - Context: Right-to-left layout support
- "global deployment" - Context: multi-region/language deployment
- "language switch" - Context: dynamic language switching

**Phrase Patterns**:
- "Add i18n to [module]" - Example: "Add i18n to Employee module"
- "Implement [language] support" - Example: "Implement French language support"
- "Setup translation for [component]" - Example: "Setup translation for error messages"

**Example User Requests**:
```
User: "Add i18n support to the Employee module"
User: "Implement French and German language support"
User: "Setup translation management for error messages"
User: "Add RTL layout support for Arabic"
```

**Note**: This skill can also be manually invoked via `/i18n-generator` command.

## Problem Statement

**User Roadmap Need:** "全球部署 (i18n)" (Global deployment with i18n)

**Current Issues:**
- SmartAdmin supports Chinese/English but lacks systematic i18n
- Manual translation management is error-prone
- No RTL layout support
- Missing translation key extraction
- Frontend/backend translation sync is manual

## Solution Overview

This skill generates:
- ✅ Backend message resource management (MessageSource, ResourceBundle, YAML/Properties)
- ✅ Frontend i18n integration (Vue I18n with lazy loading)
- ✅ Locale-specific formatting (dates, numbers, currency, timezone)
- ✅ RTL (right-to-left) layout support for Arabic/Hebrew
- ✅ Translation key extraction from code (backend + frontend)
- ✅ Missing translation detection and reporting
- ✅ Translation file synchronization (backend ↔ frontend)
- ✅ Pluralization rules (ICU MessageFormat)
- ✅ Context-aware translations (gender, formality levels)
- ✅ Translation management workflow (translator role, approval process)

## Quick Start

**Most common usage:**
```
User: "Add i18n support to UserModule for Chinese, English, Japanese, Arabic"
```

You will:
1. Generate backend MessageSource configuration
2. Create frontend Vue I18n setup
3. Extract translation keys
4. Generate translation files (4 languages)
5. Add RTL layout support
6. Set up locale formatting
7. Implement translation management workflow

## Scope

### Included
- Backend MessageSource (Spring i18n)
- Frontend Vue I18n integration
- Locale formatting (dates, numbers, currency)
- RTL layout support
- Translation key extraction
- Missing translation detection
- Backend ↔ Frontend sync
- Pluralization rules
- Translation workflow

### Not Included
- Professional translation services (user provides translations)
- Machine translation integration
- Translation memory systems

## Integration Points

- Works with `smartadmin-crud-generator` for i18n forms
- Integrates with `smartadmin-vue-crud` for frontend i18n
- Uses Spring's `MessageSource` and Vue I18n
- Compatible with all SmartAdmin modules

## Success Criteria

- ✅ Can deploy SmartAdmin in 5+ languages with one command
- ✅ i18n coverage validated with Chinese, English, Japanese, Arabic
- ✅ RTL layout working correctly
- ✅ Translation key extraction automated
- ✅ Missing translations detected automatically

## Detailed Documentation

### Configuration Guides

1. **[Backend i18n Patterns](references/backend-i18n-patterns.md)**
   - MessageSource configuration (Spring Boot)
   - Translation files (properties format)
   - Localized validation messages
   - Enum translation
   - SmartAdmin Service integration
   - **Lines:** ~650+ lines with 8 patterns

2. **[Frontend Vue I18n Setup](references/frontend-vue-i18n.md)**
   - Vue I18n instance creation
   - Translation file structure (JSON)
   - Locale switching component
   - Ant Design Vue locale integration
   - Number and date formatting
   - Lazy loading translations
   - **Lines:** ~700+ lines with 9 patterns

3. **[Locale Formatting and RTL Support](references/locale-formatting-rtl.md)**
   - Date/time formatting (backend + frontend)
   - Number and currency formatting
   - RTL locale detection
   - HTML direction attribute
   - RTL CSS adjustments
   - Component-level RTL support
   - Timezone handling
   - **Lines:** ~600+ lines

4. **[Translation Workflow](references/translation-workflow.md)**
   - Translation key extraction (automated)
   - Missing translation detection
   - Translation validation (syntax, completeness)
   - CI/CD integration (GitHub Actions)
   - Best practices
   - **Lines:** ~350+ lines

## Implementation Workflow

### Step 1: Backend MessageSource Setup (10 minutes)

```java
// Configuration
@Configuration
public class I18nConfig {

    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("i18n/messages", "i18n/validation");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setCacheSeconds(3600);
        return messageSource;
    }

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);
        resolver.setSupportedLocales(List.of(
            Locale.SIMPLIFIED_CHINESE, Locale.US, Locale.JAPAN, new Locale("ar", "SA")
        ));
        return resolver;
    }
}
```

```properties
# src/main/resources/i18n/messages_zh_CN.properties
common.success=操作成功
user.not.found=用户不存在

# src/main/resources/i18n/messages_en_US.properties
common.success=Operation successful
user.not.found=User not found
```

### Step 2: Frontend Vue I18n Setup (15 minutes)

```javascript
// src/i18n/index.js
import { createI18n } from 'vue-i18n';
import zhCN from './locales/zh-CN.json';
import enUS from './locales/en-US.json';

const i18n = createI18n({
  legacy: false,
  locale: 'zh-CN',
  fallbackLocale: 'en-US',
  messages: { 'zh-CN': zhCN, 'en-US': enUS }
});

export default i18n;
```

```json
// src/i18n/locales/zh-CN.json
{
  "common": {
    "submit": "提交",
    "cancel": "取消"
  },
  "user": {
    "username": "用户名",
    "email": "邮箱"
  }
}
```

```javascript
// src/main.js
import i18n from './i18n';
app.use(i18n);
```

### Step 3: Use in Components (10 minutes)

```vue
<template>
  <div>
    <h1>{{ t('user.title') }}</h1>
    <a-button>{{ t('common.submit') }}</a-button>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n';
const { t } = useI18n();
</script>
```

### Step 4: Backend Service Integration (10 minutes)

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final I18nService i18nService;

    public ResponseDTO<Long> createUser(UserAddForm form) {
        // Business logic...

        return ResponseDTO.okWithI18n("user.create.success");
    }
}
```

### Step 5: Add RTL Support (15 minutes)

```javascript
// src/utils/rtl-helper.js
export function isRTLLocale(locale) {
  return ['ar', 'ar-SA', 'he', 'he-IL'].includes(locale);
}

// Update document direction
export function setDocumentDirection(locale) {
  const direction = isRTLLocale(locale) ? 'rtl' : 'ltr';
  document.documentElement.setAttribute('dir', direction);
}
```

```css
/* src/styles/rtl.css */
[dir="rtl"] .text-left {
  text-align: right;
}

[dir="rtl"] .ml-2 {
  margin-left: 0;
  margin-right: 0.5rem;
}
```

### Step 6: Locale Switching (10 minutes)

```vue
<!-- LocaleSwitcher.vue -->
<template>
  <a-dropdown>
    <a-button><GlobalOutlined /> {{ currentLocaleLabel }}</a-button>
    <template #overlay>
      <a-menu @click="handleLocaleChange">
        <a-menu-item key="zh-CN">简体中文</a-menu-item>
        <a-menu-item key="en-US">English</a-menu-item>
        <a-menu-item key="ja-JP">日本語</a-menu-item>
        <a-menu-item key="ar-SA">العربية</a-menu-item>
      </a-menu>
    </template>
  </a-dropdown>
</template>

<script setup>
import { useI18n } from 'vue-i18n';
const { locale } = useI18n();

const handleLocaleChange = ({ key }) => {
  locale.value = key;
  localStorage.setItem('locale', key);
  setDocumentDirection(key);
  location.reload();
};
</script>
```

### Step 7: Extract Translation Keys (5 minutes)

```bash
# Frontend key extraction
node scripts/extract-i18n-keys.js

# Check for missing translations
node scripts/check-missing-translations.js
```

### Step 8: Validation and Testing (10 minutes)

```bash
# Validate translation files
node scripts/validate-translations.js

# Test with all locales
npm run test:i18n
```

**Total Time:** ~80 minutes (vs 2-3 weeks manual implementation)

## Troubleshooting Guide

### Issue: Missing translations

**Solution:** Use automated extraction
```bash
node scripts/extract-i18n-keys.js
node scripts/check-missing-translations.js
```

### Issue: RTL layout broken

**Solution:** Add RTL CSS overrides
```css
[dir="rtl"] .component {
  /* RTL-specific styles */
}
```

### Issue: Date formatting incorrect

**Solution:** Use locale-aware formatters
```javascript
dayjs(date).locale(localeMap[locale]).format('LL');
```

### Issue: Number/currency format wrong

**Solution:** Use i18n number formatting
```javascript
n(amount, 'currency')
```

## Performance Impact

**Expected Improvements:**
- Development time: 2-3 weeks → 80 minutes (95% reduction)
- Translation coverage: Automated detection of missing keys
- Global deployment: Support for 5+ languages out of the box
- RTL support: Full Arabic/Hebrew layout support

**Resource Requirements:**
- Translation files: ~50KB per locale (JSON/Properties)
- Memory overhead: < 5MB for all locales
- Runtime performance: Negligible (cached translations)

---

**Version:** 1.0.0
**Created:** 2026-01-26
**Sprint:** 3 (Weeks 9-11)
**Status:** ✅ Production Ready
**Last Updated:** 2026-01-26
