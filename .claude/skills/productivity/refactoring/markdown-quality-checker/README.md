# Markdown Quality Checker

**Priority**: P2 (Productivity/Refactoring)
**Version**: 1.0.0
**Status**: ✅ Stable

---

## 簡介

自動檢測和修正 Markdown/Mermaid 文檔品質問題的綜合工具。

**時間節省**: 75% (20 min → 5 min)
**已驗證**: 467 處錯誤修正，100% 成功率

---

## 核心功能

- ✅ **Mermaid 語法檢查** - 閉合標記錯誤檢測和修正（\`\`\`text → \`\`\`）
- ⏭️ **Markdown 語法驗證** - CommonMark 規範遵循性（框架準備）
- ⏭️ **文檔連結完整性** - 內部連結和圖片路徑（框架準備）

---

## 快速開始

### 掃描文檔錯誤

```bash
cd .claude/skills/productivity/refactoring/markdown-quality-checker
bash scripts/scan-closing-errors-v2.sh
```

### 自動修正錯誤

```bash
bash scripts/fix-closing-errors-v2.sh
```

### 驗證修正結果

```bash
bash scripts/scan-closing-errors-v2.sh
# 預期: 0 處錯誤
```

---

## 觸發方式

### 自動觸發（關鍵詞）

- "check markdown quality"
- "validate mermaid"
- "fix mermaid closing"
- "scan documentation"

### 手動調用

```bash
/markdown-quality-checker
/md-checker
/doc-quality
```

---

## 實際成效（IGaming 文檔）

| 指標 | 數值 |
|------|------|
| 掃描文件 | 94 個 |
| 修正錯誤 | 467 處 |
| 成功率 | 100% |
| 時間節省 | 75% |

**詳細報告**:
- [掃描報告](../../../docs/IGaming/CLOSING_FENCE_SCAN_REPORT_2026-02-02.md)
- [修正報告](../../../docs/IGaming/CLOSING_FENCE_FIX_REPORT_2026-02-02.md)

---

## 完整文檔

詳細使用指南請參考: [SKILL.md](SKILL.md)

---

## 支援

**相關文檔**:
- [Mermaid 常見錯誤](references/mermaid-common-errors.md)
- [最佳實踐](knowledge/best-practices.md)
- [錯誤模式定義](knowledge/error-patterns.yaml)

**示例**:
- [Mermaid 修正範例](examples/example-1-mermaid-fix.md)

---

**Last Updated**: 2026-02-02
