---
name: i18n-generator
description: "國際化 (i18n) 生成"
priority: P2
category: integration
---

# I18n Generator (國際化)

為 SmartAdmin 應用程式生成國際化支援，包括後端 MessageSource 和前端 Vue I18n 配置。

## Usage

```
User: "Add i18n support for Employee module in Chinese and English"
AI: [Generate message properties, Vue I18n config, locale switching]
```

## When to Use

- 添加多語言支援
- 創建翻譯文件
- 實現語言切換功能
- 錯誤訊息國際化
- 日期/數字格式化

## Generated Output

- Spring MessageSource 配置
- messages_*.properties 翻譯文件
- Vue I18n 配置
- 語言切換 API
- 日期/數字格式化工具

## Workflow

1. **配置後端 MessageSource**
   - Spring MessageSource bean
   - 翻譯文件結構

2. **創建翻譯文件**
   - messages_zh_TW.properties
   - messages_en.properties

3. **整合前端 Vue I18n**
   - 配置 vue-i18n
   - 創建 locale JSON 文件

4. **實現語言切換**
   - API 端點
   - 前端切換組件

5. **格式化工具**
   - 日期格式化 (DateTimeFormatter)
   - 數字格式化 (NumberFormat)

## Related Rules

- [F01-naming-conventions.md](../../../rules/foundation/F01-naming-conventions.md)

## Example Session

**User:** 為員工模塊添加中英文國際化支援

**AI Agent Actions:**
1. 創建 `messages_zh_TW.properties` 中文翻譯
2. 創建 `messages_en.properties` 英文翻譯
3. 配置 `MessageSourceConfig`
4. 創建 Vue I18n locale 文件
5. 實現語言切換下拉選單組件
