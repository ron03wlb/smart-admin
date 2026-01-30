# Frontend Vue I18n Setup Guide

**Skill:** i18n-generator
**Component:** Vue I18n / Ant Design Vue
**Purpose:** Implement frontend internationalization for SmartAdmin Vue 3 applications

---

## Why Frontend i18n?

- ✅ Localized UI text and labels
- ✅ Multi-language forms and validation
- ✅ Date/time/number formatting
- ✅ RTL layout support
- ✅ Dynamic locale switching

---

## Dependencies

```json
// package.json
{
  "dependencies": {
    "vue": "^3.4.0",
    "vue-i18n": "^9.15.0",
    "ant-design-vue": "^4.2.0",
    "dayjs": "^1.11.13"
  }
}
```

---

## Pattern 1: Vue I18n Setup

### Step 1: Install

```bash
npm install vue-i18n@9
```

### Step 2: Create i18n Instance

```javascript
// src/i18n/index.js
import { createI18n } from 'vue-i18n';
import zhCN from './locales/zh-CN.json';
import enUS from './locales/en-US.json';
import jaJP from './locales/ja-JP.json';
import arSA from './locales/ar-SA.json';

// Load locale messages
const messages = {
  'zh-CN': zhCN,
  'en-US': enUS,
  'ja-JP': jaJP,
  'ar-SA': arSA
};

// Create i18n instance
const i18n = createI18n({
  legacy: false,              // Use Composition API mode
  locale: 'zh-CN',            // Default locale
  fallbackLocale: 'en-US',    // Fallback locale
  messages,
  globalInjection: true,      // Inject $t function globally
  missingWarn: false,         // Disable missing translation warnings in prod
  fallbackWarn: false
});

export default i18n;
```

### Step 3: Register i18n in App

```javascript
// src/main.js
import { createApp } from 'vue';
import App from './App.vue';
import i18n from './i18n';

const app = createApp(App);

app.use(i18n);
app.mount('#app');
```

---

## Pattern 2: Translation Files

### Translation File Structure

```
src/i18n/
├── index.js
└── locales/
    ├── zh-CN.json
    ├── en-US.json
    ├── ja-JP.json
    └── ar-SA.json
```

### Translation JSON (Chinese)

```json
{
  "common": {
    "confirm": "确认",
    "cancel": "取消",
    "submit": "提交",
    "reset": "重置",
    "search": "搜索",
    "add": "新增",
    "edit": "编辑",
    "delete": "删除",
    "save": "保存",
    "success": "操作成功",
    "error": "操作失败",
    "loading": "加载中..."
  },
  "user": {
    "title": "用户管理",
    "username": "用户名",
    "email": "邮箱",
    "phone": "手机号",
    "status": "状态",
    "create": "创建用户",
    "createSuccess": "用户创建成功",
    "validation": {
      "usernameRequired": "请输入用户名",
      "usernameLength": "用户名长度必须在 {min} 到 {max} 之间",
      "emailInvalid": "邮箱格式不正确",
      "phoneInvalid": "手机号格式不正确"
    }
  },
  "order": {
    "title": "订单管理",
    "orderId": "订单号",
    "totalAmount": "订单金额",
    "orderStatus": "订单状态",
    "createdAt": "创建时间",
    "status": {
      "created": "已创建",
      "paid": "已支付",
      "shipped": "已发货",
      "delivered": "已送达",
      "cancelled": "已取消"
    }
  }
}
```

### Translation JSON (English)

```json
{
  "common": {
    "confirm": "Confirm",
    "cancel": "Cancel",
    "submit": "Submit",
    "reset": "Reset",
    "search": "Search",
    "add": "Add",
    "edit": "Edit",
    "delete": "Delete",
    "save": "Save",
    "success": "Operation successful",
    "error": "Operation failed",
    "loading": "Loading..."
  },
  "user": {
    "title": "User Management",
    "username": "Username",
    "email": "Email",
    "phone": "Phone",
    "status": "Status",
    "create": "Create User",
    "createSuccess": "User created successfully",
    "validation": {
      "usernameRequired": "Username is required",
      "usernameLength": "Username length must be between {min} and {max}",
      "emailInvalid": "Invalid email format",
      "phoneInvalid": "Invalid phone format"
    }
  },
  "order": {
    "title": "Order Management",
    "orderId": "Order ID",
    "totalAmount": "Total Amount",
    "orderStatus": "Order Status",
    "createdAt": "Created At",
    "status": {
      "created": "Created",
      "paid": "Paid",
      "shipped": "Shipped",
      "delivered": "Delivered",
      "cancelled": "Cancelled"
    }
  }
}
```

---

## Pattern 3: Using i18n in Components

### Composition API (Recommended)

```vue
<!-- src/views/user/user-list.vue -->
<template>
  <div>
    <a-page-header :title="t('user.title')" />

    <a-form>
      <a-form-item :label="t('user.username')">
        <a-input v-model:value="form.username" :placeholder="t('user.validation.usernameRequired')" />
      </a-form-item>

      <a-form-item :label="t('user.email')">
        <a-input v-model:value="form.email" />
      </a-form-item>

      <a-form-item>
        <a-button type="primary" @click="submit">
          {{ t('common.submit') }}
        </a-button>
        <a-button @click="reset">
          {{ t('common.reset') }}
        </a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n';
import { reactive } from 'vue';
import { message } from 'ant-design-vue';

const { t } = useI18n();

const form = reactive({
  username: '',
  email: ''
});

const submit = () => {
  // Form submission logic
  message.success(t('user.createSuccess'));
};

const reset = () => {
  form.username = '';
  form.email = '';
};
</script>
```

### With Parameters

```vue
<template>
  <div>
    <!-- Static parameter -->
    <p>{{ t('user.validation.usernameLength', { min: 3, max: 20 }) }}</p>

    <!-- Dynamic parameter -->
    <p>{{ t('order.totalAmount') }}: {{ formatCurrency(order.totalAmount) }}</p>

    <!-- Pluralization -->
    <p>{{ t('order.itemCount', { count: order.items.length }) }}</p>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n';

const { t, n } = useI18n();

const formatCurrency = (amount) => {
  return n(amount, 'currency');
};
</script>
```

---

## Pattern 4: Locale Switching

### Locale Switcher Component

```vue
<!-- src/components/LocaleSwitcher.vue -->
<template>
  <a-dropdown>
    <a-button>
      <GlobalOutlined />
      {{ currentLocaleLabel }}
    </a-button>

    <template #overlay>
      <a-menu @click="handleLocaleChange">
        <a-menu-item v-for="locale in supportedLocales" :key="locale.value">
          <span :class="{ 'font-bold': locale.value === currentLocale }">
            {{ locale.label }}
          </span>
        </a-menu-item>
      </a-menu>
    </template>
  </a-dropdown>
</template>

<script setup>
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { GlobalOutlined } from '@ant-design/icons-vue';
import { updateUserLocale } from '@/api/user-api';

const { locale } = useI18n();

const supportedLocales = [
  { value: 'zh-CN', label: '简体中文' },
  { value: 'en-US', label: 'English' },
  { value: 'ja-JP', label: '日本語' },
  { value: 'ar-SA', label: 'العربية' }
];

const currentLocale = computed(() => locale.value);

const currentLocaleLabel = computed(() => {
  return supportedLocales.find(l => l.value === currentLocale.value)?.label;
});

const handleLocaleChange = async ({ key }) => {
  // Update Vue I18n locale
  locale.value = key;

  // Persist to localStorage
  localStorage.setItem('locale', key);

  // Update Ant Design locale
  updateAntdLocale(key);

  // Update dayjs locale
  updateDayjsLocale(key);

  // Sync to backend (save user preference)
  try {
    await updateUserLocale(key);
  } catch (error) {
    console.error('Failed to update user locale:', error);
  }

  // Reload page to apply locale changes
  location.reload();
};

const updateAntdLocale = (locale) => {
  // See Pattern 5 for Ant Design locale integration
};

const updateDayjsLocale = (locale) => {
  const localeMap = {
    'zh-CN': 'zh-cn',
    'en-US': 'en',
    'ja-JP': 'ja',
    'ar-SA': 'ar'
  };
  import(`dayjs/locale/${localeMap[locale]}`);
};
</script>
```

---

## Pattern 5: Ant Design Vue Locale Integration

```javascript
// src/components/ConfigProvider.vue
<template>
  <a-config-provider :locale="antdLocale">
    <router-view />
  </a-config-provider>
</template>

<script setup>
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import zhCN from 'ant-design-vue/es/locale/zh_CN';
import enUS from 'ant-design-vue/es/locale/en_US';
import jaJP from 'ant-design-vue/es/locale/ja_JP';
import arEG from 'ant-design-vue/es/locale/ar_EG';  // Arabic (closest match)
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';
import 'dayjs/locale/ja';
import 'dayjs/locale/ar';

const { locale } = useI18n();

const antdLocaleMap = {
  'zh-CN': zhCN,
  'en-US': enUS,
  'ja-JP': jaJP,
  'ar-SA': arEG
};

const antdLocale = computed(() => {
  const currentLocale = locale.value;

  // Set dayjs locale
  const dayjsLocaleMap = {
    'zh-CN': 'zh-cn',
    'en-US': 'en',
    'ja-JP': 'ja',
    'ar-SA': 'ar'
  };
  dayjs.locale(dayjsLocaleMap[currentLocale]);

  return antdLocaleMap[currentLocale] || enUS;
});
</script>
```

---

## Pattern 6: Number and Date Formatting

### Number Formats

```javascript
// src/i18n/index.js (add to i18n config)
const numberFormats = {
  'zh-CN': {
    currency: {
      style: 'currency',
      currency: 'CNY',
      notation: 'standard'
    },
    decimal: {
      style: 'decimal',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    },
    percent: {
      style: 'percent',
      useGrouping: false
    }
  },
  'en-US': {
    currency: {
      style: 'currency',
      currency: 'USD',
      notation: 'standard'
    },
    decimal: {
      style: 'decimal',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    },
    percent: {
      style: 'percent',
      useGrouping: false
    }
  },
  'ja-JP': {
    currency: {
      style: 'currency',
      currency: 'JPY',
      notation: 'standard'
    }
  },
  'ar-SA': {
    currency: {
      style: 'currency',
      currency: 'SAR',
      notation: 'standard'
    }
  }
};

const i18n = createI18n({
  // ... other config
  numberFormats
});
```

### Date Formats

```javascript
// src/i18n/index.js (add to i18n config)
const datetimeFormats = {
  'zh-CN': {
    short: {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    },
    long: {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }
  },
  'en-US': {
    short: {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    },
    long: {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      hour12: true
    }
  },
  'ja-JP': {
    short: {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    },
    long: {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false
    }
  },
  'ar-SA': {
    short: {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    },
    long: {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false
    }
  }
};

const i18n = createI18n({
  // ... other config
  datetimeFormats
});
```

### Using Formats in Components

```vue
<template>
  <div>
    <!-- Currency formatting -->
    <p>{{ t('order.totalAmount') }}: {{ n(order.totalAmount, 'currency') }}</p>

    <!-- Date formatting -->
    <p>{{ t('order.createdAt') }}: {{ d(order.createdAt, 'long') }}</p>

    <!-- Percent formatting -->
    <p>{{ t('order.discountRate') }}: {{ n(order.discountRate / 100, 'percent') }}</p>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n';

const { t, n, d } = useI18n();

const order = {
  totalAmount: 99.99,
  createdAt: new Date('2024-01-26T10:30:00'),
  discountRate: 15
};
</script>
```

---

## Pattern 7: Lazy Loading Translations

```javascript
// src/i18n/index.js (optimized for large apps)
import { createI18n } from 'vue-i18n';

const i18n = createI18n({
  legacy: false,
  locale: 'zh-CN',
  fallbackLocale: 'en-US',
  messages: {}  // Empty, load on demand
});

// Lazy load locale messages
export async function loadLocaleMessages(locale) {
  // Check if already loaded
  if (i18n.global.availableLocales.includes(locale)) {
    return;
  }

  // Load locale file dynamically
  const messages = await import(`./locales/${locale}.json`);

  // Set locale messages
  i18n.global.setLocaleMessage(locale, messages.default);
}

export async function setLocale(locale) {
  // Load locale messages if not already loaded
  await loadLocaleMessages(locale);

  // Set active locale
  i18n.global.locale.value = locale;
}

export default i18n;
```

---

## Pattern 8: SmartAdmin CRUD Form with i18n

```vue
<!-- src/views/user/user-form-modal.vue -->
<template>
  <a-modal
    :visible="visible"
    :title="t(modalTitle)"
    @ok="handleSubmit"
    @cancel="handleCancel"
  >
    <a-form ref="formRef" :model="form" :rules="rules">
      <a-form-item name="username" :label="t('user.username')">
        <a-input v-model:value="form.username" :placeholder="t('user.validation.usernameRequired')" />
      </a-form-item>

      <a-form-item name="email" :label="t('user.email')">
        <a-input v-model:value="form.email" :placeholder="t('user.validation.emailInvalid')" />
      </a-form-item>

      <a-form-item name="phone" :label="t('user.phone')">
        <a-input v-model:value="form.phone" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { reactive, computed, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { message } from 'ant-design-vue';
import { createUserApi, updateUserApi } from '@/api/user-api';

const { t } = useI18n();

const props = defineProps({
  visible: Boolean,
  userId: Number
});

const emit = defineEmits(['success', 'cancel']);

const formRef = ref();
const form = reactive({
  username: '',
  email: '',
  phone: ''
});

// i18n validation rules
const rules = computed(() => ({
  username: [
    {
      required: true,
      message: t('user.validation.usernameRequired')
    },
    {
      min: 3,
      max: 20,
      message: t('user.validation.usernameLength', { min: 3, max: 20 })
    }
  ],
  email: [
    {
      required: true,
      type: 'email',
      message: t('user.validation.emailInvalid')
    }
  ],
  phone: [
    {
      pattern: /^1[3-9]\d{9}$/,
      message: t('user.validation.phoneInvalid')
    }
  ]
}));

const modalTitle = computed(() => {
  return props.userId ? 'user.edit' : 'user.create';
});

const handleSubmit = async () => {
  try {
    await formRef.value.validate();

    const apiCall = props.userId ? updateUserApi : createUserApi;
    await apiCall(form);

    message.success(t('common.success'));
    emit('success');
  } catch (error) {
    if (error.errorFields) {
      // Validation error - handled by form
      return;
    }
    message.error(t('common.error'));
  }
};

const handleCancel = () => {
  formRef.value.resetFields();
  emit('cancel');
};
</script>
```

---

## Pattern 9: Extracting Translation Keys

### Script to Extract Keys from Vue Files

```javascript
// scripts/extract-i18n-keys.js
const fs = require('fs');
const path = require('path');
const { parse } = require('@vue/compiler-sfc');

const extractedKeys = new Set();

function extractKeysFromFile(filePath) {
  const content = fs.readFileSync(filePath, 'utf-8');

  // Match t('key') and t("key")
  const regex = /\bt\(['"]([a-zA-Z0-9._-]+)['"]\)/g;
  let match;

  while ((match = regex.exec(content)) !== null) {
    extractedKeys.add(match[1]);
  }
}

function scanDirectory(dir) {
  const files = fs.readdirSync(dir);

  for (const file of files) {
    const filePath = path.join(dir, file);
    const stat = fs.statSync(filePath);

    if (stat.isDirectory()) {
      scanDirectory(filePath);
    } else if (file.endsWith('.vue') || file.endsWith('.js')) {
      extractKeysFromFile(filePath);
    }
  }
}

// Scan src directory
scanDirectory('./src');

// Output extracted keys
console.log('Extracted translation keys:');
console.log(Array.from(extractedKeys).sort().join('\n'));

// Check for missing translations
const zhCN = require('../src/i18n/locales/zh-CN.json');
const missingKeys = [];

for (const key of extractedKeys) {
  const keys = key.split('.');
  let obj = zhCN;

  for (const k of keys) {
    if (obj[k] === undefined) {
      missingKeys.push(key);
      break;
    }
    obj = obj[k];
  }
}

if (missingKeys.length > 0) {
  console.log('\nMissing translations:');
  console.log(missingKeys.join('\n'));
}
```

Run script:
```bash
node scripts/extract-i18n-keys.js
```

---

## Best Practices

1. **Consistent Key Structure:**
   - Use dot notation: `module.component.action`
   - Group by feature: `user.*`, `order.*`
   - Keep keys shallow (max 3 levels)

2. **Translation File Organization:**
   - Split by module for large apps
   - Use JSON for simple translations
   - Use JavaScript for complex translations with logic

3. **Performance:**
   - Use lazy loading for large translation files
   - Cache loaded locales
   - Minimize translation file size

4. **Development Workflow:**
   - Extract keys automatically
   - Detect missing translations in CI/CD
   - Use fallback locale for untranslated keys
   - Never hard-code text in components

5. **Component Design:**
   - Always use `t()` function for UI text
   - Support dynamic parameters
   - Test with all supported locales
   - Handle RTL layouts (see next section)

---

**Next:** [Locale Formatting and RTL Support](locale-formatting-rtl.md)
**Version:** 1.0.0
**Last Updated:** 2026-01-26
