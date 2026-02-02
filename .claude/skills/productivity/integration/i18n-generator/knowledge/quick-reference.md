# i18n Generator - Quick Reference

**Version**: 1.0.0
**Last Updated**: 2026-02-02
**Skill**: i18n-generator (P2 - Productivity/Integration)

---

## Command Quick Reference

| Command | Purpose | Duration |
|---------|---------|----------|
| Backend i18n (MessageSource) | Setup Spring i18n | ~8 min |
| Frontend i18n (Vue I18n) | Setup Vue i18n | ~10 min |
| API Error i18n | Internationalize error messages | ~5 min |
| Dynamic Language Switch | Runtime language switching | ~8 min |
| Extract i18n Keys | Auto-extract hardcoded strings | ~12 min |

---

## i18n Architecture

### Full-Stack i18n Pattern

```
Frontend (Vue I18n)         Backend (MessageSource)
┌─────────────────┐         ┌───────────────────┐
│ UI Labels       │ ←───────│ API Error Messages│
│ Button Text     │  JSON   │ Validation Messages│
│ Form Labels     │  API    │ Business Messages │
└─────────────────┘         └───────────────────┘
         │                            │
         └────────────────┬───────────┘
                     Language Code
                    (zh-CN, en-US)
```

**Key Principles**:
- Frontend: UI text (labels, buttons, static content)
- Backend: Error messages, validation messages, business logic messages
- Shared: Language code synchronization via Accept-Language header

---

## Pattern 1: Backend i18n (Spring MessageSource)

### Setup MessageSource

```java
@Configuration
public class I18nConfig {

    /**
     * Configure MessageSource for i18n
     */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource =
            new ReloadableResourceBundleMessageSource();

        messageSource.setBasenames(
            "classpath:i18n/messages",
            "classpath:i18n/validation",
            "classpath:i18n/business"
        );
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setCacheSeconds(3600);  // Cache for 1 hour

        return messageSource;
    }

    /**
     * Locale resolver - from Accept-Language header
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);  // zh-CN default
        resolver.setSupportedLocales(Arrays.asList(
            Locale.SIMPLIFIED_CHINESE,  // zh-CN
            Locale.US,                  // en-US
            Locale.TRADITIONAL_CHINESE  // zh-TW
        ));
        return resolver;
    }
}
```

### Message Property Files

**File Structure**:
```
src/main/resources/i18n/
├── messages_zh_CN.properties   # Chinese (Simplified)
├── messages_en_US.properties   # English
├── messages_zh_TW.properties   # Chinese (Traditional)
├── validation_zh_CN.properties
├── validation_en_US.properties
├── business_zh_CN.properties
└── business_en_US.properties
```

**messages_zh_CN.properties**:
```properties
# Common messages
common.success=操作成功
common.error=操作失敗
common.not.found=資源不存在

# Employee module
employee.created=員工創建成功
employee.updated=員工更新成功
employee.deleted=員工刪除成功
employee.not.found=員工不存在，ID: {0}
```

**messages_en_US.properties**:
```properties
# Common messages
common.success=Operation successful
common.error=Operation failed
common.not.found=Resource not found

# Employee module
employee.created=Employee created successfully
employee.updated=Employee updated successfully
employee.deleted=Employee deleted successfully
employee.not.found=Employee not found, ID: {0}
```

### MessageSource Helper

```java
@Component
@RequiredArgsConstructor
public class MessageHelper {

    private final MessageSource messageSource;

    /**
     * Get message for current locale
     */
    public String getMessage(String code, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, args, locale);
    }

    /**
     * Get message with default value
     */
    public String getMessage(String code, String defaultMessage, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, args, defaultMessage, locale);
    }
}
```

### Usage in Service

```java
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeDao employeeDao;
    private final MessageHelper messageHelper;

    /**
     * Get employee by ID with i18n error message
     */
    public EmployeeVO getById(Long id) {
        EmployeeEntity entity = employeeDao.selectById(id);

        if (entity == null) {
            // Throw exception with i18n message
            String message = messageHelper.getMessage("employee.not.found", id);
            throw new BusinessException(message);
        }

        return SmartBeanUtil.copy(entity, EmployeeVO.class);
    }

    /**
     * Create employee with i18n success message
     */
    public String create(EmployeeAddForm form) {
        EmployeeEntity entity = SmartBeanUtil.copy(form, EmployeeEntity.class);
        employeeDao.insert(entity);

        // Return i18n success message
        return messageHelper.getMessage("employee.created");
    }
}
```

**Time to Implement**: 8-10 minutes

---

## Pattern 2: Frontend i18n (Vue I18n)

### Setup Vue I18n

```bash
# Install Vue I18n
npm install vue-i18n@9
```

**i18n Configuration** (`src/i18n/index.js`):
```javascript
import { createI18n } from 'vue-i18n';
import zhCN from './locales/zh-CN.json';
import enUS from './locales/en-US.json';
import zhTW from './locales/zh-TW.json';

const i18n = createI18n({
  legacy: false,  // Use Composition API
  locale: localStorage.getItem('locale') || 'zh-CN',
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS,
    'zh-TW': zhTW,
  },
});

export default i18n;
```

**Main.js Integration**:
```javascript
import { createApp } from 'vue';
import App from './App.vue';
import i18n from './i18n';

const app = createApp(App);
app.use(i18n);
app.mount('#app');
```

### Message Files

**locales/zh-CN.json**:
```json
{
  "common": {
    "add": "新增",
    "edit": "編輯",
    "delete": "刪除",
    "save": "保存",
    "cancel": "取消",
    "search": "搜尋",
    "reset": "重置",
    "confirm": "確認",
    "success": "操作成功",
    "error": "操作失敗"
  },
  "employee": {
    "title": "員工管理",
    "name": "姓名",
    "email": "郵箱",
    "department": "部門",
    "status": "狀態",
    "createdAt": "創建時間",
    "addEmployee": "新增員工",
    "editEmployee": "編輯員工",
    "deleteConfirm": "確定要刪除員工 {name} 嗎？"
  }
}
```

**locales/en-US.json**:
```json
{
  "common": {
    "add": "Add",
    "edit": "Edit",
    "delete": "Delete",
    "save": "Save",
    "cancel": "Cancel",
    "search": "Search",
    "reset": "Reset",
    "confirm": "Confirm",
    "success": "Operation successful",
    "error": "Operation failed"
  },
  "employee": {
    "title": "Employee Management",
    "name": "Name",
    "email": "Email",
    "department": "Department",
    "status": "Status",
    "createdAt": "Created At",
    "addEmployee": "Add Employee",
    "editEmployee": "Edit Employee",
    "deleteConfirm": "Are you sure to delete employee {name}?"
  }
}
```

### Usage in Vue Components

**Template Usage**:
```vue
<template>
  <div>
    <h1>{{ $t('employee.title') }}</h1>

    <a-button type="primary" @click="addEmployee">
      {{ $t('employee.addEmployee') }}
    </a-button>

    <a-table :columns="columns" :data-source="employees">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'actions'">
          <a-button @click="editEmployee(record)">
            {{ $t('common.edit') }}
          </a-button>
          <a-button danger @click="deleteEmployee(record)">
            {{ $t('common.delete') }}
          </a-button>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

// Dynamic column headers
const columns = [
  { title: t('employee.name'), dataIndex: 'name', key: 'name' },
  { title: t('employee.email'), dataIndex: 'email', key: 'email' },
  { title: t('employee.department'), dataIndex: 'departmentName', key: 'departmentName' },
  { title: t('employee.status'), dataIndex: 'status', key: 'status' },
  { title: t('common.actions'), key: 'actions' },
];

// Delete confirmation with parameter
const deleteEmployee = (employee) => {
  Modal.confirm({
    title: t('employee.deleteConfirm', { name: employee.name }),
    onOk: () => {
      // Delete logic
    },
  });
};
</script>
```

**Composition API Usage**:
```vue
<script setup>
import { useI18n } from 'vue-i18n';
import { message } from 'ant-design-vue';

const { t } = useI18n();

const saveEmployee = async () => {
  try {
    await employeeApi.save(form);
    message.success(t('common.success'));
  } catch (error) {
    message.error(t('common.error'));
  }
};
</script>
```

**Time to Implement**: 10-12 minutes

---

## Pattern 3: Language Switcher

### Language Switcher Component

```vue
<template>
  <a-dropdown>
    <a-button>
      <GlobalOutlined />
      {{ currentLanguage }}
    </a-button>
    <template #overlay>
      <a-menu @click="changeLanguage">
        <a-menu-item key="zh-CN">简体中文</a-menu-item>
        <a-menu-item key="en-US">English</a-menu-item>
        <a-menu-item key="zh-TW">繁體中文</a-menu-item>
      </a-menu>
    </template>
  </a-dropdown>
</template>

<script setup>
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { GlobalOutlined } from '@ant-design/icons-vue';

const { locale } = useI18n();

const languageNames = {
  'zh-CN': '简体中文',
  'en-US': 'English',
  'zh-TW': '繁體中文',
};

const currentLanguage = computed(() => languageNames[locale.value]);

/**
 * Change language
 */
const changeLanguage = ({ key }) => {
  locale.value = key;
  localStorage.setItem('locale', key);

  // Sync with backend API calls
  // Update Axios default headers
  import('@/api/request').then(({ default: request }) => {
    request.defaults.headers.common['Accept-Language'] = key;
  });

  // Reload page to apply new language (optional)
  // window.location.reload();
};
</script>
```

### Axios Interceptor (Sync Language Header)

```javascript
// src/api/request.js
import axios from 'axios';
import i18n from '@/i18n';

const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
});

// Request interceptor - add Accept-Language header
request.interceptors.request.use(
  (config) => {
    const locale = i18n.global.locale.value;
    config.headers['Accept-Language'] = locale;
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

export default request;
```

**Time to Implement**: 8-10 minutes

---

## Pattern 4: API Error Message i18n

### Backend Error Response

```java
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageHelper messageHelper;

    /**
     * Handle business exceptions with i18n
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseDTO<Void> handleBusinessException(BusinessException e) {
        // Exception already contains i18n message
        return ResponseDTO.error(e.getCode(), e.getMessage());
    }

    /**
     * Handle validation exceptions with i18n
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseDTO<Void> handleValidationException(MethodArgumentNotValidException e) {
        BindingResult result = e.getBindingResult();
        FieldError error = result.getFieldError();

        if (error != null) {
            // Get i18n validation message
            String message = messageHelper.getMessage(
                "validation." + error.getDefaultMessage(),
                error.getField()
            );
            return ResponseDTO.error(UserErrorCode.PARAM_ERROR, message);
        }

        return ResponseDTO.error(UserErrorCode.PARAM_ERROR,
            messageHelper.getMessage("validation.error"));
    }
}
```

### Validation Messages

**validation_zh_CN.properties**:
```properties
validation.error=驗證失敗
validation.NotBlank={0} 不能為空
validation.NotNull={0} 不能為空
validation.Email={0} 格式不正確
validation.Size={0} 長度必須在 {1} 到 {2} 之間
validation.Min={0} 不能小於 {1}
validation.Max={0} 不能大於 {1}
```

**validation_en_US.properties**:
```properties
validation.error=Validation failed
validation.NotBlank={0} cannot be blank
validation.NotNull={0} cannot be null
validation.Email={0} format is invalid
validation.Size={0} length must be between {1} and {2}
validation.Min={0} cannot be less than {1}
validation.Max={0} cannot be greater than {1}
```

### Entity Validation

```java
public class EmployeeAddForm {

    @NotBlank(message = "NotBlank")
    private String name;

    @Email(message = "Email")
    @NotBlank(message = "NotBlank")
    private String email;

    @NotNull(message = "NotNull")
    private Long deptId;
}
```

**Error Response** (zh-CN):
```json
{
  "code": 10001,
  "msg": "姓名 不能為空",
  "ok": false,
  "data": null
}
```

**Error Response** (en-US):
```json
{
  "code": 10001,
  "msg": "name cannot be blank",
  "ok": false,
  "data": null
}
```

**Time to Implement**: 5-8 minutes

---

## Pattern 5: Extract i18n Keys

### Auto-Extract Hardcoded Strings

**Grep Script** (`scripts/extract-i18n.sh`):
```bash
#!/bin/bash

# Extract hardcoded Chinese strings from Vue files
grep -r -P "[\u4e00-\u9fa5]+" src/views --include="*.vue" | \
  grep -v "i18n" | \
  sed 's/.*"\([^"]*[\u4e00-\u9fa5][^"]*\)".*/\1/' | \
  sort -u > i18n-candidates.txt

echo "Found $(wc -l < i18n-candidates.txt) hardcoded strings"
echo "Review i18n-candidates.txt and add to locales/*.json"
```

**Usage**:
```bash
chmod +x scripts/extract-i18n.sh
./scripts/extract-i18n.sh
```

### ESLint Plugin (Enforce i18n)

```javascript
// .eslintrc.js
module.exports = {
  rules: {
    // Warn on Chinese characters outside $t()
    'no-irregular-whitespace': 'error',
    'vue/no-irregular-whitespace': 'error',
  },
};
```

**Time to Implement**: 12-15 minutes

---

## Common Errors and Quick Fixes

### Error 1: Missing Translation Key

**Symptom**: Shows key instead of translation (e.g., "employee.name")

**Cause**: Key not defined in locale file

**Fix**: Add fallback
```javascript
const i18n = createI18n({
  fallbackLocale: 'zh-CN',
  missingWarn: true,  // Warn in console
  fallbackWarn: true,
});
```

---

### Error 2: Language Not Synced

**Symptom**: Frontend Chinese, backend English errors

**Cause**: Accept-Language header not sent

**Fix**: Add Axios interceptor
```javascript
request.interceptors.request.use((config) => {
  config.headers['Accept-Language'] = i18n.global.locale.value;
  return config;
});
```

---

### Error 3: Special Characters Garbled

**Symptom**: UTF-8 characters show as ???

**Cause**: Properties file encoding wrong

**Fix**: Use native2ascii or set encoding
```properties
# File encoding: UTF-8
employee.name=员工姓名
```

Or configure IDE to use UTF-8 for .properties files.

---

## Time Estimates

| Task | Implementation | Testing | Total |
|------|---------------|---------|-------|
| Backend MessageSource | 6 min | 2 min | 8 min |
| Frontend Vue I18n | 8 min | 2 min | 10 min |
| API Error i18n | 4 min | 1 min | 5 min |
| Language Switcher | 6 min | 2 min | 8 min |
| Extract i18n Keys | 10 min | 2 min | 12 min |

---

**See Also**:
- [SmartAdmin Patterns](../../../../shared/knowledge/smartadmin-patterns.md) - ResponseDTO pattern
- [Full-Text Search Integration](../full-text-search-integration/) - Multi-language search
