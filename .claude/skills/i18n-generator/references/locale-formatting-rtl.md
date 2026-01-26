# Locale Formatting and RTL Support Guide

**Skill:** i18n-generator
**Component:** Date/Number Formatting / RTL Layout
**Purpose:** Handle locale-specific formatting and right-to-left language support

---

## Part 1: Locale-Specific Formatting

### Date and Time Formatting

#### Backend (Java)

```java
package net.lab1024.sa.foundation.i18n.format;

import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class DateTimeFormatterService {

    /**
     * Format date for current locale
     */
    public String formatDate(LocalDateTime dateTime) {
        Locale locale = LocaleContextHolder.getLocale();
        DateTimeFormatter formatter = DateTimeFormatter
            .ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(locale);

        return dateTime.format(formatter);
    }

    /**
     * Format datetime for current locale
     */
    public String formatDateTime(LocalDateTime dateTime) {
        Locale locale = LocaleContextHolder.getLocale();
        DateTimeFormatter formatter = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(locale);

        return dateTime.format(formatter);
    }

    /**
     * Custom format patterns by locale
     */
    public String formatWithPattern(LocalDateTime dateTime) {
        Locale locale = LocaleContextHolder.getLocale();
        String pattern;

        if (locale.getLanguage().equals("zh")) {
            pattern = "yyyy年MM月dd日 HH:mm:ss";
        } else if (locale.getLanguage().equals("ja")) {
            pattern = "yyyy年MM月dd日 HH時mm分ss秒";
        } else if (locale.getLanguage().equals("ar")) {
            pattern = "yyyy/MM/dd HH:mm:ss";
        } else {
            pattern = "MMM dd, yyyy hh:mm:ss a";  // English
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern).withLocale(locale);
        return dateTime.format(formatter);
    }

    /**
     * Timezone conversion
     */
    public String formatWithTimezone(LocalDateTime dateTime, String timezone) {
        Locale locale = LocaleContextHolder.getLocale();
        ZoneId zoneId = ZoneId.of(timezone);

        DateTimeFormatter formatter = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withLocale(locale)
            .withZone(zoneId);

        return dateTime.atZone(ZoneId.systemDefault())
            .withZoneSameInstant(zoneId)
            .format(formatter);
    }
}
```

#### Frontend (Vue 3)

```javascript
// src/utils/date-formatter.js
import dayjs from 'dayjs';
import utc from 'dayjs/plugin/utc';
import timezone from 'dayjs/plugin/timezone';
import localizedFormat from 'dayjs/plugin/localizedFormat';
import 'dayjs/locale/zh-cn';
import 'dayjs/locale/ja';
import 'dayjs/locale/ar';

dayjs.extend(utc);
dayjs.extend(timezone);
dayjs.extend(localizedFormat);

export function formatDate(date, locale = 'zh-CN') {
  const localeMap = {
    'zh-CN': 'zh-cn',
    'en-US': 'en',
    'ja-JP': 'ja',
    'ar-SA': 'ar'
  };

  return dayjs(date).locale(localeMap[locale]).format('LL');  // Localized date
}

export function formatDateTime(date, locale = 'zh-CN') {
  const localeMap = {
    'zh-CN': 'zh-cn',
    'en-US': 'en',
    'ja-JP': 'ja',
    'ar-SA': 'ar'
  };

  return dayjs(date).locale(localeMap[locale]).format('LLL');  // Localized datetime
}

export function formatRelativeTime(date, locale = 'zh-CN') {
  const localeMap = {
    'zh-CN': 'zh-cn',
    'en-US': 'en',
    'ja-JP': 'ja',
    'ar-SA': 'ar'
  };

  return dayjs(date).locale(localeMap[locale]).fromNow();  // "2 hours ago"
}

export function formatWithTimezone(date, timezone, locale = 'zh-CN') {
  const localeMap = {
    'zh-CN': 'zh-cn',
    'en-US': 'en',
    'ja-JP': 'ja',
    'ar-SA': 'ar'
  };

  return dayjs(date).tz(timezone).locale(localeMap[locale]).format('LLL');
}
```

Usage in components:
```vue
<template>
  <div>
    <p>{{ formatDate(order.createdAt, currentLocale) }}</p>
    <p>{{ formatRelativeTime(order.createdAt, currentLocale) }}</p>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n';
import { formatDate, formatRelativeTime } from '@/utils/date-formatter';

const { locale } = useI18n();
const currentLocale = computed(() => locale.value);

const order = {
  createdAt: '2024-01-26T10:30:00'
};
</script>
```

---

### Number and Currency Formatting

#### Backend (Java)

```java
package net.lab1024.sa.foundation.i18n.format;

import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

@Component
public class NumberFormatterService {

    /**
     * Format currency for current locale
     */
    public String formatCurrency(BigDecimal amount) {
        Locale locale = LocaleContextHolder.getLocale();
        Currency currency = getCurrencyForLocale(locale);

        NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
        formatter.setCurrency(currency);

        return formatter.format(amount);
    }

    /**
     * Format decimal number
     */
    public String formatDecimal(BigDecimal number, int fractionDigits) {
        Locale locale = LocaleContextHolder.getLocale();

        NumberFormat formatter = NumberFormat.getNumberInstance(locale);
        formatter.setMinimumFractionDigits(fractionDigits);
        formatter.setMaximumFractionDigits(fractionDigits);

        return formatter.format(number);
    }

    /**
     * Format percentage
     */
    public String formatPercentage(double value) {
        Locale locale = LocaleContextHolder.getLocale();
        NumberFormat formatter = NumberFormat.getPercentInstance(locale);
        formatter.setMinimumFractionDigits(1);

        return formatter.format(value);
    }

    private Currency getCurrencyForLocale(Locale locale) {
        if (locale.getLanguage().equals("zh")) {
            return Currency.getInstance("CNY");
        } else if (locale.getLanguage().equals("ja")) {
            return Currency.getInstance("JPY");
        } else if (locale.getLanguage().equals("ar")) {
            return Currency.getInstance("SAR");
        } else {
            return Currency.getInstance("USD");
        }
    }
}
```

#### Frontend (Vue 3)

Use Vue I18n number formats (see frontend-vue-i18n.md Pattern 6)

---

## Part 2: RTL (Right-to-Left) Layout Support

### What is RTL?

**RTL languages:** Arabic (ar), Hebrew (he), Persian (fa), Urdu (ur)
**Challenge:** UI must mirror horizontally (text, icons, layout flow)

---

### Pattern 1: Detect RTL Locale

```javascript
// src/utils/rtl-helper.js
export function isRTLLocale(locale) {
  const rtlLocales = ['ar', 'ar-SA', 'he', 'he-IL', 'fa', 'fa-IR', 'ur', 'ur-PK'];
  return rtlLocales.includes(locale) || locale.startsWith('ar-') || locale.startsWith('he-');
}

export function getDirection(locale) {
  return isRTLLocale(locale) ? 'rtl' : 'ltr';
}
```

---

### Pattern 2: HTML Direction Attribute

```javascript
// src/i18n/index.js (add to locale change logic)
import { isRTLLocale } from '@/utils/rtl-helper';

export function setDocumentDirection(locale) {
  const direction = isRTLLocale(locale) ? 'rtl' : 'ltr';
  document.documentElement.setAttribute('dir', direction);
  document.documentElement.setAttribute('lang', locale);
}

// Call when locale changes
export async function setLocale(locale) {
  await loadLocaleMessages(locale);
  i18n.global.locale.value = locale;
  setDocumentDirection(locale);
}
```

---

### Pattern 3: RTL CSS

```css
/* src/styles/rtl.css */

/* Apply RTL-specific styles when dir="rtl" */
[dir="rtl"] .text-left {
  text-align: right;
}

[dir="rtl"] .text-right {
  text-align: left;
}

[dir="rtl"] .float-left {
  float: right;
}

[dir="rtl"] .float-right {
  float: left;
}

/* Margin/Padding adjustments */
[dir="rtl"] .ml-2 {
  margin-left: 0;
  margin-right: 0.5rem;
}

[dir="rtl"] .mr-2 {
  margin-right: 0;
  margin-left: 0.5rem;
}

[dir="rtl"] .pl-4 {
  padding-left: 0;
  padding-right: 1rem;
}

[dir="rtl"] .pr-4 {
  padding-right: 0;
  padding-left: 1rem;
}

/* Flexbox adjustments */
[dir="rtl"] .flex-row {
  flex-direction: row-reverse;
}

/* Border adjustments */
[dir="rtl"] .border-left {
  border-left: none;
  border-right: 1px solid #e0e0e0;
}

[dir="rtl"] .border-right {
  border-right: none;
  border-left: 1px solid #e0e0e0;
}

/* Icon adjustments (flip horizontal) */
[dir="rtl"] .icon-arrow-left::before {
  content: '\f054';  /* arrow-right icon code */
}

[dir="rtl"] .icon-arrow-right::before {
  content: '\f053';  /* arrow-left icon code */
}
```

---

### Pattern 4: Ant Design Vue RTL Configuration

```javascript
// src/main.js (add RTL support)
import { ConfigProvider } from 'ant-design-vue';
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { isRTLLocale } from '@/utils/rtl-helper';

const App = {
  setup() {
    const { locale } = useI18n();

    const direction = computed(() => {
      return isRTLLocale(locale.value) ? 'rtl' : 'ltr';
    });

    return { direction };
  },
  template: `
    <a-config-provider :direction="direction">
      <router-view />
    </a-config-provider>
  `
};
```

---

### Pattern 5: Component-Level RTL Support

```vue
<!-- src/components/UserCard.vue -->
<template>
  <div class="user-card" :class="{ 'rtl': isRTL }">
    <a-avatar :src="user.avatar" />

    <div class="user-info">
      <h3>{{ user.username }}</h3>
      <p>{{ user.email }}</p>
    </div>

    <a-button :icon="isRTL ? 'arrow-left' : 'arrow-right'" />
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { isRTLLocale } from '@/utils/rtl-helper';

const { locale } = useI18n();

const isRTL = computed(() => isRTLLocale(locale.value));
</script>

<style scoped>
.user-card {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.user-card .user-info {
  text-align: left;
}

/* RTL overrides */
.user-card.rtl {
  flex-direction: row-reverse;
}

.user-card.rtl .user-info {
  text-align: right;
}
</style>
```

---

### Pattern 6: Navigation Menu RTL

```vue
<!-- src/components/SideMenu.vue -->
<template>
  <a-menu mode="inline" :class="{ 'rtl-menu': isRTL }">
    <a-menu-item v-for="item in menuItems" :key="item.key">
      <component :is="item.icon" :class="{ 'flip': isRTL && item.flipIcon }" />
      <span>{{ t(item.label) }}</span>
    </a-menu-item>
  </a-menu>
</template>

<script setup>
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { isRTLLocale } from '@/utils/rtl-helper';
import { UserOutlined, SettingOutlined, ArrowLeftOutlined } from '@ant-design/icons-vue';

const { locale, t } = useI18n();

const isRTL = computed(() => isRTLLocale(locale.value));

const menuItems = [
  { key: 'users', icon: UserOutlined, label: 'menu.users', flipIcon: false },
  { key: 'settings', icon: SettingOutlined, label: 'menu.settings', flipIcon: false },
  { key: 'logout', icon: ArrowLeftOutlined, label: 'menu.logout', flipIcon: true }
];
</script>

<style scoped>
.rtl-menu .icon.flip {
  transform: scaleX(-1);  /* Flip icon horizontally */
}

.rtl-menu {
  text-align: right;
}
</style>
```

---

### Pattern 7: Table RTL Support

```vue
<!-- src/views/user/user-list.vue -->
<template>
  <a-table
    :columns="columns"
    :data-source="users"
    :class="{ 'rtl-table': isRTL }"
  />
</template>

<script setup>
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { isRTLLocale } from '@/utils/rtl-helper';

const { locale, t } = useI18n();

const isRTL = computed(() => isRTLLocale(locale.value));

const columns = computed(() => [
  {
    title: t('user.username'),
    dataIndex: 'username',
    key: 'username',
    align: isRTL.value ? 'right' : 'left'
  },
  {
    title: t('user.email'),
    dataIndex: 'email',
    key: 'email',
    align: isRTL.value ? 'right' : 'left'
  },
  {
    title: t('user.status'),
    dataIndex: 'status',
    key: 'status',
    align: 'center'
  }
]);
</script>

<style scoped>
.rtl-table :deep(.ant-table-thead > tr > th) {
  text-align: right;
}

.rtl-table :deep(.ant-table-tbody > tr > td) {
  text-align: right;
}
</style>
```

---

### Pattern 8: Form RTL Support

```vue
<!-- src/views/user/user-form.vue -->
<template>
  <a-form
    :model="form"
    :label-col="labelCol"
    :wrapper-col="wrapperCol"
    :class="{ 'rtl-form': isRTL }"
  >
    <a-form-item :label="t('user.username')" name="username">
      <a-input v-model:value="form.username" />
    </a-form-item>

    <a-form-item :label="t('user.email')" name="email">
      <a-input v-model:value="form.email" />
    </a-form-item>

    <a-form-item :wrapper-col="{ offset: isRTL ? 0 : 8, span: 16 }">
      <a-button type="primary" @click="submit">
        {{ t('common.submit') }}
      </a-button>
    </a-form-item>
  </a-form>
</template>

<script setup>
import { reactive, computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { isRTLLocale } from '@/utils/rtl-helper';

const { locale, t } = useI18n();

const isRTL = computed(() => isRTLLocale(locale.value));

const labelCol = computed(() => ({
  span: 8,
  style: { textAlign: isRTL.value ? 'left' : 'right' }
}));

const wrapperCol = computed(() => ({
  span: 16
}));

const form = reactive({
  username: '',
  email: ''
});

const submit = () => {
  // Submit logic
};
</script>

<style scoped>
.rtl-form :deep(.ant-form-item-label) {
  text-align: left;
}
</style>
```

---

## Part 3: Timezone Handling

### User Timezone Preference

```java
// Backend: Store user's timezone
@Data
@TableName("t_user")
public class UserEntity {

    @TableId(type = IdType.AUTO)
    private Long userId;

    private String username;

    private String locale;       // zh_CN, en_US, ja_JP, ar_SA

    private String timezone;     // Asia/Shanghai, America/New_York, etc.
}
```

### Convert to User Timezone

```java
package net.lab1024.sa.foundation.i18n.format;

import lombok.RequiredArgsConstructor;
import net.lab1024.sa.foundation.domain.request.RequestUser;
import net.lab1024.sa.foundation.sa.token.SmartRequestUserService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class TimezoneConverter {

    /**
     * Convert datetime to user's timezone
     */
    public ZonedDateTime convertToUserTimezone(LocalDateTime dateTime) {
        RequestUser requestUser = SmartRequestUserService.getRequestUser();

        if (requestUser == null || requestUser.getTimezone() == null) {
            return dateTime.atZone(ZoneId.systemDefault());
        }

        ZoneId userZone = ZoneId.of(requestUser.getTimezone());

        return dateTime.atZone(ZoneId.systemDefault())
            .withZoneSameInstant(userZone);
    }

    /**
     * Convert from user's timezone to system timezone
     */
    public LocalDateTime convertFromUserTimezone(LocalDateTime userDateTime) {
        RequestUser requestUser = SmartRequestUserService.getRequestUser();

        if (requestUser == null || requestUser.getTimezone() == null) {
            return userDateTime;
        }

        ZoneId userZone = ZoneId.of(requestUser.getTimezone());
        ZoneId systemZone = ZoneId.systemDefault();

        return userDateTime.atZone(userZone)
            .withZoneSameInstant(systemZone)
            .toLocalDateTime();
    }
}
```

---

## Best Practices

### Date/Time Formatting:
1. ✅ Always use locale-aware formatters
2. ✅ Store timestamps in UTC in database
3. ✅ Convert to user's timezone for display
4. ✅ Use ISO 8601 format for API transfers
5. ✅ Test with multiple locales and timezones

### Number/Currency Formatting:
1. ✅ Use locale-specific decimal separators (. vs ,)
2. ✅ Format currency with correct symbol and position
3. ✅ Handle currency conversion rates if needed
4. ✅ Display prices in user's preferred currency

### RTL Support:
1. ✅ Use logical CSS properties (start/end instead of left/right)
2. ✅ Flip directional icons (arrows, chevrons)
3. ✅ Test layout with Arabic/Hebrew text
4. ✅ Use `dir="auto"` for mixed content
5. ✅ Ensure proper text alignment (right for RTL)

### Testing:
1. ✅ Test with all supported locales
2. ✅ Verify date formats match locale conventions
3. ✅ Check currency symbols and positions
4. ✅ Validate RTL layout with actual Arabic text
5. ✅ Test timezone conversions across regions

---

**Next:** [Translation Management Workflow](translation-workflow.md)
**Version:** 1.0.0
**Last Updated:** 2026-01-26
