# I18n Generator - Quick Reference

## 後端 MessageSource 配置

```java
@Configuration
public class MessageSourceConfig {

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasename("classpath:i18n/messages");
        source.setDefaultEncoding("UTF-8");
        source.setCacheSeconds(3600);
        return source;
    }

    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver();
        resolver.setDefaultLocale(Locale.TAIWAN);
        resolver.setCookieName("lang");
        return resolver;
    }
}
```

## 翻譯文件結構

```
resources/i18n/
├── messages.properties           # 預設
├── messages_zh_TW.properties    # 繁體中文
├── messages_en.properties        # English
└── messages_ja.properties        # 日本語
```

## 翻譯文件範例

```properties
# messages_zh_TW.properties
employee.title=員工管理
employee.add=新增員工
employee.edit=編輯員工
employee.delete.confirm=確定要刪除此員工嗎？
validation.required={0}為必填項目
validation.email.invalid=電子郵件格式不正確
```

```properties
# messages_en.properties
employee.title=Employee Management
employee.add=Add Employee
employee.edit=Edit Employee
employee.delete.confirm=Are you sure you want to delete this employee?
validation.required={0} is required
validation.email.invalid=Invalid email format
```

## Vue I18n 配置

```javascript
// src/i18n/index.js
import { createI18n } from 'vue-i18n'
import zhTW from './locales/zh-TW.json'
import en from './locales/en.json'

export default createI18n({
  legacy: false,
  locale: 'zh-TW',
  fallbackLocale: 'en',
  messages: { 'zh-TW': zhTW, en },
})
```

## 使用方式

```java
// 後端
@RequiredArgsConstructor
public class EmployeeService {
    private final MessageSource messageSource;

    public String getMessage(String key) {
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
    }
}
```

```vue
<!-- 前端 -->
<template>
  <h1>{{ $t('employee.title') }}</h1>
  <a-button>{{ $t('employee.add') }}</a-button>
</template>
```
