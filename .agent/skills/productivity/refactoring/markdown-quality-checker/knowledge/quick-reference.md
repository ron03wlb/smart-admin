# Markdown Quality Checker - Quick Reference

## Mermaid 常見問題

### 1. 未閉合的代碼區塊
```markdown
<!-- ❌ 錯誤 -->
```mermaid
graph TD
    A --> B

<!-- ✅ 正確 -->
```mermaid
graph TD
    A --> B
```
```

### 2. 語法錯誤
```mermaid
<!-- ❌ 錯誤: 箭頭方向錯誤 -->
graph TD
    A <- B

<!-- ✅ 正確 -->
graph TD
    A --> B
    B --> A
```

### 3. 節點 ID 問題
```mermaid
<!-- ❌ 錯誤: ID 包含特殊字符 -->
graph TD
    my-node --> other

<!-- ✅ 正確: 使用引號 -->
graph TD
    A["my-node"] --> B["other"]
```

## 連結檢查規則

| 類型 | 範例 | 檢查項目 |
|------|------|----------|
| 內部連結 | `[text](./file.md)` | 文件存在 |
| 錨點連結 | `[text](#heading)` | 標題存在 |
| 外部連結 | `[text](https://...)` | 可達性 |

## 格式規範

### 標題層級
```markdown
<!-- ❌ 錯誤: 跳級 -->
# H1
### H3

<!-- ✅ 正確: 連續層級 -->
# H1
## H2
### H3
```

### 代碼區塊
```markdown
<!-- ❌ 錯誤: 無語言標記 -->
```
code here
```

<!-- ✅ 正確: 指定語言 -->
```java
code here
```
```

### 列表格式
```markdown
<!-- ❌ 錯誤: 混合標記 -->
- item 1
* item 2
+ item 3

<!-- ✅ 正確: 統一標記 -->
- item 1
- item 2
- item 3
```

## 快速檢查命令

```bash
# 使用 markdownlint
npx markdownlint docs/**/*.md

# Mermaid 驗證
npx @mermaid-js/mermaid-cli -i diagram.md -o output.svg
```

## 嚴重程度定義

| 級別 | 說明 | 範例 |
|------|------|------|
| 🔴 Error | 必須修復 | 斷開的連結、語法錯誤 |
| 🟡 Warning | 建議修復 | 標題跳級、無語言標記 |
| 🔵 Info | 可選改進 | 行長度、空白行 |
