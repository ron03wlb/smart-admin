---
name: markdown-quality-checker
description: [P2 - Productivity] Automatically check and fix Markdown/Mermaid quality issues (closing fences, syntax validation, broken links). Proven: 467 errors fixed, 100% success (IGaming docs).
---

# Markdown Quality Checker Skill

**Version**: 1.0.0
**Priority**: P2 (Productivity/Refactoring)
**Category**: Refactoring
**Status**: Stable

---

## 概述

自動檢測和修正 Markdown/Mermaid 文檔品質問題，包含閉合標記錯誤、語法違規和連結有效性。

**時間節省**: 20 分鐘 → 5 分鐘（**75% 改善**）
**已驗證成效**: 467 處錯誤修正，100% 成功率（IGaming 文檔）

---

## 核心功能

| 功能 | 描述 | 狀態 |
|------|------|------|
| **Mermaid 語法檢查** | 檢測閉合標記錯誤（\`\`\`text → \`\`\`） | ✅ 已實現 |
| **Markdown 語法驗證** | CommonMark 規範遵循性檢查 | ⏭️ 框架準備 |
| **文檔連結完整性** | 內部連結和圖片路徑驗證 | ⏭️ 框架準備 |

---

## Trigger Keywords

This skill is automatically activated when the user's request contains:

**Primary Keywords** (High confidence):
- "check markdown quality" - Check documentation quality
- "validate mermaid" - Validate Mermaid diagram syntax
- "fix mermaid closing" - Fix closing fence errors
- "scan documentation" - Scan for documentation errors
- "markdown lint" - Markdown linting
- "doc quality" - Document quality check

**Secondary Keywords** (Medium confidence):
- "mermaid diagram error" - Context: syntax validation
- "markdown validation" - Context: quality checking
- "broken links" - Context: link validation

**Phrase Patterns**:
- "Check [documentation] quality" - Example: "Check IGaming documentation quality"
- "Fix [Mermaid/Markdown] errors in [directory]" - Example: "Fix Mermaid errors in docs/"
- "Validate [file] syntax" - Example: "Validate README.md syntax"

**Example User Requests**:
```
User: "Check all IGaming documentation for Mermaid errors"
User: "Fix mermaid closing fence errors in docs/"
User: "Validate markdown syntax in README.md"
User: "Scan documentation for broken links"
```

**Note**: This skill can also be manually invoked via `/markdown-quality-checker`, `/md-checker`, or `/doc-quality` command.

### 命令別名

```bash
# 完整名稱
/markdown-quality-checker

# 簡短別名
/md-checker
/doc-quality
/mermaid-fix
```

---

## 使用範例

### 場景 1: 修正 Mermaid 閉合標記錯誤

**問題**:
文檔使用錯誤的閉合標記導致 Mermaid 解析失敗：

```
Error parsing Mermaid diagram!
Parse error on line 31:
...LAYER : from_player```text---
----------------------^
```

**原因**: 使用語言標識符作為閉合標記

**解決**:
```bash
# 掃描所有 Markdown 文件
cd .claude/skills/productivity/refactoring/markdown-quality-checker
bash scripts/scan-closing-errors-v2.sh

# 自動修正所有錯誤
bash scripts/fix-closing-errors-v2.sh
```

**輸出**:
```
✅ 掃描 94 個文件
✅ 修正 467 處錯誤
✅ 驗證: 0 處剩餘錯誤
⏱️ 時間節省: 20 分鐘 → 5 分鐘 (75%)
```

---

### 場景 2: 掃描特定目錄

**需求**: 只檢查 docs/IGaming/ 目錄

**解決**:
```bash
# 修改 TARGET_DIR 變數
export TARGET_DIR="docs/IGaming"
bash scripts/scan-closing-errors-v2.sh
```

---

### 場景 3: 掃描後僅查看報告（不修正）

**需求**: 先了解錯誤情況，決定是否修正

**解決**:
```bash
# 掃描並生成報告
bash scripts/scan-closing-errors-v2.sh > /tmp/scan-report.txt

# 查看報告
cat /tmp/scan-report.txt
```

---

## 錯誤類型詳解

### 錯誤 1: Mermaid 閉合標記使用語言標識符

#### ❌ 錯誤模式

```markdown
\```mermaid
graph TD
    A --> B
\```text    ← 錯誤！閉合標記不應有語言標識符
```

#### ✅ 正確模式

```markdown
\```mermaid
graph TD
    A --> B
\```         ← 正確！純三個反引號
```

**檢測邏輯** (上下文分析):
1. 找到 `\`\`\`(text|yaml|markdown|sql|json)` 模式
2. 檢查前一行是否有內容（非空）
3. 檢查後一行是否為空、分隔符（`---`）或標題（`#`）
4. 如果符合 → 這是閉合標記錯誤

**影響**:
- 🔴 Mermaid Live Editor 解析失敗
- 🟡 GitHub Markdown 可能誤解析
- 🟡 VS Code Preview 顯示異常

---

### 錯誤 2: Markdown 標題層級跳躍（待實現）

#### ❌ 錯誤

```markdown
# H1 標題
### H3 標題    ← 跳過了 H2
```

#### ✅ 正確

```markdown
# H1 標題
## H2 標題
### H3 標題
```

---

### 錯誤 3: 斷裂的內部連結（待實現）

#### ❌ 錯誤

```markdown
[查看文檔](./non-existent-file.md)    ← 文件不存在
```

#### ✅ 正確

```markdown
[查看文檔](./existing-file.md)
```

---

## 實際案例：IGaming 文檔修正

### 修正統計

| 指標 | 數值 |
|------|------|
| **掃描文件總數** | 94 個 |
| **受影響文件數** | 61 個 |
| **總錯誤數** | 467 處 |
| **修正成功率** | 100% |
| **時間節省** | 75% (20 min → 5 min) |

### 錯誤類型分布

| 錯誤類型 | 數量 | 百分比 |
|---------|------|--------|
| \`\`\`text | 348 | 74.5% |
| \`\`\`markdown | 86 | 18.4% |
| \`\`\`yaml | 15 | 3.2% |
| \`\`\`sql | 18 | 3.9% |

### 修正前後對比

**修正前**:
```
受影響文件數: 61
總錯誤數: 467
```

**修正後**:
```
受影響文件數: 0
總錯誤數: 0
驗證掃描: ✅ PASSED
```

**詳細報告**:
- [掃描報告](../../../../docs/IGaming/CLOSING_FENCE_SCAN_REPORT_2026-02-02.md)
- [修正報告](../../../../docs/IGaming/CLOSING_FENCE_FIX_REPORT_2026-02-02.md)

---

## 技術細節

### 檢測流程

```
1. 收集所有 .md 文件 (排除 *REPORT*, *CORRECTION*, *backup*)
   ↓
2. 逐行掃描
   ↓
3. 檢測 ```language 模式
   ↓
4. 上下文分析
   - 前一行: 是否有內容?
   - 後一行: 是否為空/分隔符/標題?
   ↓
5. 判定: 開啟標記 vs 閉合標記
   ↓
6. 記錄錯誤 (if 閉合標記)
```

### 修正流程

```
1. 創建完整備份 (docs/IGaming-backup-TIMESTAMP/)
   ↓
2. 讀取文件到數組
   ↓
3. 對每一行檢測
   - 如果是閉合標記錯誤 → 替換為 ```
   - 否則 → 保留原行
   ↓
4. 寫回文件
   ↓
5. 記錄詳細日誌 (/tmp/closing-fix-log-TIMESTAMP.txt)
   ↓
6. 重新掃描驗證
```

### 安全措施

- ✅ 完整備份（可回滾）
- ✅ 詳細日誌（每處修正都有記錄）
- ✅ 排除特殊文件（審計報告、歷史文檔）
- ✅ 驗證機制（修正後自動重新掃描）

---

## 驗證方法

### 1. 自動化掃描驗證

```bash
# 執行掃描腳本
bash scripts/scan-closing-errors-v2.sh

# 預期輸出
========================================
📊 掃描統計
========================================
掃描文件總數: X
受影響文件數: 0
總錯誤數: 0

✅ 未發現閉合標記錯誤
```

### 2. 手動抽樣檢查

```bash
# 檢查特定文件
cat docs/IGaming/00_Concept_&_Analysis/00-03_Data_Model_Overview.md | grep -A 2 "^```mermaid"

# 確認閉合標記為純三個反引號
```

### 3. Mermaid 語法驗證（可選）

```bash
# 安裝 Mermaid CLI
npm install -g @mermaid-js/mermaid-cli

# 驗證單個文件
mmdc --validate -i docs/file.md
```

---

## 常見錯誤和修正

### 問題 1: 掃描報告 "Permission Denied"

**原因**: 腳本沒有執行權限

**修正**:
```bash
chmod +x scripts/scan-closing-errors-v2.sh
chmod +x scripts/fix-closing-errors-v2.sh
```

---

### 問題 2: 修正後仍然有錯誤

**原因**: 可能修正的文件不完整（排除了某些文件）

**檢查**:
```bash
# 查看哪些文件被排除
grep -r "REPORT\|CORRECTION\|backup" docs/
```

---

### 問題 3: 備份佔用空間太大

**原因**: 每次修正都創建完整備份

**解決**:
```bash
# 刪除舊備份
rm -rf docs/*-backup-*
```

---

## 參考文檔

### 內部文檔

- [Mermaid 常見錯誤](references/mermaid-common-errors.md)
- [Markdown 標準](references/markdown-standards.md)
- [驗證規則詳解](references/validation-rules.md)

### 知識庫

- [錯誤模式定義](knowledge/error-patterns.yaml)
- [修正策略](knowledge/fix-strategies.md)
- [最佳實踐](knowledge/best-practices.md)

### 實際案例

- [IGaming 文檔掃描報告](../../../../docs/IGaming/CLOSING_FENCE_SCAN_REPORT_2026-02-02.md)
- [IGaming 文檔修正報告](../../../../docs/IGaming/CLOSING_FENCE_FIX_REPORT_2026-02-02.md)

---

## 快速參考表

| 任務 | 命令 | 預期時間 |
|------|------|---------|
| **掃描錯誤** | `bash scripts/scan-closing-errors-v2.sh` | ~2 分鐘 |
| **修正錯誤** | `bash scripts/fix-closing-errors-v2.sh` | ~3 分鐘 |
| **驗證結果** | `bash scripts/scan-closing-errors-v2.sh` | ~2 分鐘 |
| **查看備份** | `ls -la docs/*-backup-*` | 即時 |
| **查看日誌** | `cat /tmp/closing-fix-log-*.txt` | 即時 |

---

## 後續擴展

### Phase 2: Markdown 語法驗證（待開發）

**工具**: markdownlint-cli

**功能**:
- 標題層級檢查
- 連結格式驗證
- 表格語法檢查
- 程式碼塊語言標識

**整合點**: `scripts/scan-markdown-syntax.sh`

### Phase 3: 文檔連結檢查（待開發）

**工具**: Bash + find + test

**功能**:
- 內部連結有效性
- 圖片路徑檢查
- 錨點連結驗證
- 相對路徑解析

**整合點**: `scripts/check-doc-links.sh`

---

## 版本歷史

**v1.0.0** (2026-02-02)
- ✅ Mermaid 閉合標記檢查和修正
- ✅ 上下文分析邏輯
- ✅ 完整備份機制
- ✅ 詳細日誌記錄
- ✅ 已驗證：467 錯誤，100% 成功率

---

**Skill Version**: 1.0.0
**Last Updated**: 2026-02-02
**Compatible With**: SmartAdmin v4.0.0+, .claude/ system v3.0.2+
