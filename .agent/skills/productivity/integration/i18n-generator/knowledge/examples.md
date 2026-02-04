# I18n Generator - Examples

## 範例 1: 員工模塊完整 i18n

**後端翻譯文件**:

```properties
# messages_zh_TW.properties
# 員工模塊
employee.title=員工管理
employee.list=員工列表
employee.add=新增員工
employee.edit=編輯員工
employee.delete=刪除員工
employee.search.placeholder=請輸入員工姓名或工號

# 欄位標籤
employee.field.name=姓名
employee.field.phone=電話
employee.field.email=電子郵件
employee.field.department=部門
employee.field.position=職位

# 驗證訊息
employee.validation.name.required=請輸入員工姓名
employee.validation.phone.invalid=電話格式不正確

# 操作訊息
employee.message.add.success=員工新增成功
employee.message.update.success=員工更新成功
employee.message.delete.success=員工刪除成功
```

**前端 locale JSON**:

```json
{
  "employee": {
    "title": "員工管理",
    "list": "員工列表",
    "add": "新增員工",
    "edit": "編輯員工",
    "delete": "刪除員工",
    "search": {
      "placeholder": "請輸入員工姓名或工號"
    },
    "field": {
      "name": "姓名",
      "phone": "電話",
      "email": "電子郵件",
      "department": "部門",
      "position": "職位"
    }
  }
}
```

---

## 範例 2: 語言切換組件

```vue
<template>
  <a-dropdown>
    <template #overlay>
      <a-menu @click="changeLocale">
        <a-menu-item key="zh-TW">繁體中文</a-menu-item>
        <a-menu-item key="en">English</a-menu-item>
        <a-menu-item key="ja">日本語</a-menu-item>
      </a-menu>
    </template>
    <a-button>
      <global-outlined />
      {{ currentLocaleName }}
    </a-button>
  </a-dropdown>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const { locale, availableLocales } = useI18n()

const localeNames = {
  'zh-TW': '繁體中文',
  'en': 'English',
  'ja': '日本語',
}

const currentLocaleName = computed(() => localeNames[locale.value])

const changeLocale = ({ key }) => {
  locale.value = key
  localStorage.setItem('locale', key)
  // 通知後端更新 cookie
  updateLocaleApi(key)
}
</script>
```

---

## 範例 3: 錯誤訊息國際化

```java
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final MessageSource messageSource;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseDTO<Void> handleValidation(MethodArgumentNotValidException ex) {
        FieldError error = ex.getBindingResult().getFieldError();
        String message = messageSource.getMessage(
            "validation." + error.getField(),
            new Object[]{error.getField()},
            error.getDefaultMessage(),
            LocaleContextHolder.getLocale()
        );
        return ResponseDTO.error(UserErrorCode.PARAM_ERROR, message);
    }
}
```
