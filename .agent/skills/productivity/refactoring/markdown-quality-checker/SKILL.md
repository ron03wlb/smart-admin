---
name: markdown-quality-checker
description: "Markdown/Mermaid 品質檢查與修復"
priority: P2
category: refactoring
---

# Markdown Quality Checker

檢測並修復 Markdown 和 Mermaid 圖表的品質問題，包括語法錯誤、斷開的連結和格式不一致。

## Usage

```
User: "Check markdown quality in docs/ directory"
AI: [Scan files, detect issues, generate fix report]
```

## When to Use

- 文檔品質審查
- Mermaid 圖表驗證
- 修復斷開的連結
- Markdown 格式統一
- CI/CD 文檔檢查

## Generated Output

- 品質問題報告
- 自動修復建議
- Mermaid 語法修正
- 連結驗證結果

## Checks Performed

1. **Mermaid 圖表**
   - 語法驗證
   - 閉合標籤檢查
   - 節點連接驗證

2. **連結檢查**
   - 內部連結驗證
   - 錨點存在性
   - 外部連結可達性

3. **格式規範**
   - 標題層級
   - 代碼區塊標記
   - 列表格式

4. **內容品質**
   - 表格格式
   - 圖片替代文字
   - 空白行規範

## Workflow

1. **掃描文件**
   - 收集所有 .md 文件
   - 解析 Markdown 結構

2. **執行檢查**
   - Mermaid 語法驗證
   - 連結可達性檢查
   - 格式規範檢查

3. **生成報告**
   - 問題分類列表
   - 嚴重程度標記
   - 修復建議

4. **自動修復 (可選)**
   - 閉合未關閉的標籤
   - 修正格式問題

## Related Rules

- [F01-naming-conventions.md](../../../rules/foundation/F01-naming-conventions.md)

## Example Session

**User:** 檢查 docs/ 目錄下的 Markdown 品質

**AI Agent Actions:**
1. 掃描 docs/**/*.md 文件
2. 驗證所有 Mermaid 圖表語法
3. 檢查內部連結是否有效
4. 檢查格式規範
5. 生成品質報告
6. 提供修復建議或自動修復
