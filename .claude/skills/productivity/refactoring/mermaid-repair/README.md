# Mermaid Repair Skill

**Version**: 1.0.0
**Priority**: P2 (Productivity - Refactoring)
**Category**: 文檔修復與驗證
**Created**: 2026-02-03
**Status**: ✅ Production Ready

---

## 🎯 用途

自動檢測和修復 SmartAdmin 項目中的 Mermaid 圖表語法錯誤。專為 SmartAdmin 的特殊渲染環境設計（使用 `<br/>` 而非標準 `\n`）。

## 🚀 核心功能

### 1. Style 語法修復

**檢測模式**：
- 節點 ID 包含空格但未加引號
- 顏色碼格式錯誤或污染
- 重複的 style 關鍵字

**修復策略**：
- 自動為節點 ID 添加雙引號
- 清理顏色碼污染
- 規範化 style 語句格式

### 2. SmartAdmin 環境適配

**關鍵差異**：
- ✅ 使用 `<br/>` 標籤進行換行（非標準 Mermaid 的 `\n`）
- ✅ 節點標籤不需要雙引號（使用 `<br/>` 時）
- ✅ sequenceDiagram Note 區塊必須使用 `<br/>`

### 3. 自動化驗證

**檢查項目**：
- Mermaid 語法正確性
- Style 定義完整性
- 顏色碼有效性（十六進制格式）
- 節點 ID 引號規則

---

## 📋 觸發關鍵字

| 場景 | 關鍵字 |
|------|--------|
| 錯誤修復 | "mermaid 修復", "mermaid 語法錯誤" |
| Style 問題 | "style 語法", "mermaid style 錯誤" |
| 渲染失敗 | "圖表渲染失敗", "mermaid parse error" |
| 批次檢查 | "檢查所有 mermaid", "掃描 mermaid 錯誤" |

---

## 🔧 使用示例

### 場景 1: 批次修復 style 錯誤

**輸入**：
```
"檢查並修復 iGaming 文檔中的所有 Mermaid style 語法錯誤"
```

**執行步驟**：
1. 掃描所有 `docs/iGaming/**/*.md` 文件
2. 檢測 style 語法錯誤（節點名空格、顏色碼格式）
3. 生成修復報告
4. 應用修復

**輸出**：
- 錯誤清單（文件路徑、行號、錯誤類型）
- 修復建議（diff 格式）
- 自動修復結果

---

### 場景 2: 驗證單個文件

**輸入**：
```
"驗證 05-01_Risk_Control_System.md 的 Mermaid 語法"
```

**執行步驟**：
1. 解析文件中的所有 Mermaid 代碼塊
2. 驗證語法正確性
3. 檢查 style 定義

**輸出**：
- ✅ 語法正確 / ❌ 發現錯誤
- 錯誤詳情（位置、類型、修復建議）

---

### 場景 3: 檢測顏色碼污染

**輸入**：
```
"檢測 Mermaid 圖表中的顏色碼污染"
```

**執行步驟**：
1. 掃描所有 `style` 語句
2. 正則表達式檢測非十六進制字符
3. 生成污染報告

**輸出**：
```
發現 5 個顏色碼污染實例：
- README.md:189: #BonusBonus3366 → 應為 #333366
- 04-02_Bonus_Calculation_Engine.md:422: #FFEBonus82 → 應為 #FFEB82
```

---

## 📚 錯誤類型庫

### Type A: 節點名空格未加引號

**嚴重性**: 🔴 高（阻塞渲染）

**錯誤模式**：
```mermaid
style Risk Engine fill:#DDA0DD
style Rule Engine (LiteFlow) fill:#FFE4B5
```

**修復方式**：
```mermaid
style "Risk Engine" fill:#DDA0DD
style "Rule Engine (LiteFlow)" fill:#FFE4B5
```

**檢測正則表達式**：
```regex
^\s*style\s+[^"]*\s[^"]*fill:
```

---

### Type B: 顏色碼污染

**嚴重性**: 🔴 高（樣式失敗）

**錯誤模式**：
```mermaid
style B fill:#BonusBonus3366
style EXCLUSIVE_GROUP fill:#FFEBonus82
```

**修復方式**：
```mermaid
style B fill:#333366
style EXCLUSIVE_GROUP fill:#FFEB82
```

**檢測正則表達式**：
```regex
fill:#[0-9A-Fa-f]*[^0-9A-Fa-f,\s]
```

---

## 🛠️ 自動化腳本

### 1. fix_style_syntax.py

**功能**：修復 style 語法錯誤

**使用方式**：
```bash
python scripts/fix_style_syntax.py <file_path>
```

**輸出**：
- 修復前後對比
- 替換次數統計

---

### 2. validate_mermaid.py

**功能**：驗證 Mermaid 語法正確性

**使用方式**：
```bash
python scripts/validate_mermaid.py <file_path>
```

**輸出**：
- ✅ 語法正確
- ❌ 錯誤清單（類型、位置、建議）

---

### 3. batch_repair.py

**功能**：批次修復多個文件

**使用方式**：
```bash
python scripts/batch_repair.py docs/iGaming/
```

**輸出**：
- 掃描文件總數
- 修復文件清單
- 總替換次數

---

## 📊 修復案例研究

### 案例 1: iGaming 文檔 2026-02-03 修復

**背景**：發現 12 行 style 語法錯誤（2 種類型）

**修復範圍**：
- 4 個文件（非 archive）
- Type A: 7 行（節點名空格）
- Type B: 5 行（顏色碼污染）

**修復結果**：
- ✅ 所有 sequenceDiagram 渲染正常
- ✅ 樣式應用正確
- ✅ 通過 Mermaid Live Editor 驗證

**詳細報告**: [examples/example-1-style-fix.md](examples/example-1-style-fix.md)

---

## 🔍 Pre-commit Hook 集成

### 自動檢查邏輯

Mermaid Repair skill 與 `.git/hooks/pre-commit` 集成，防止未來錯誤：

```bash
# 檢查 style 語法錯誤（節點名包含空格但未加引號）
STYLE_ERRORS=$(echo "$CHANGED_MD_FILES" | xargs grep -n '^\s*style [^"]*\s[^"]*fill:' || true)

# 檢查顏色碼污染（包含非十六進制字符）
COLOR_ERRORS=$(echo "$CHANGED_MD_FILES" | xargs grep -nP 'fill:#[0-9A-Fa-f]*[^0-9A-Fa-f,\s]' || true)
```

**觸發時機**：提交包含 `*.md` 文件時

**阻止提交條件**：
- 發現節點名空格未加引號
- 發現顏色碼格式錯誤

---

## 📁 文件結構

```
mermaid-repair/
├── README.md                          # 本文件
├── knowledge/
│   ├── error-patterns.md              # 錯誤模式庫（詳細說明）
│   └── smartadmin-mermaid-spec.md     # SmartAdmin Mermaid 規範
├── scripts/
│   ├── fix_style_syntax.py            # Style 語法修復
│   ├── validate_mermaid.py            # Mermaid 驗證工具
│   └── batch_repair.py                # 批次修復工具
└── examples/
    └── example-1-style-fix.md         # 2026-02-03 修復案例
```

---

## ⚙️ 配置選項

### 全局設置

**顏色碼清理模式**：
- `conservative`: 僅清理明顯污染（默認）
- `aggressive`: 嚴格驗證所有十六進制格式

**節點 ID 引號規則**：
- `auto_quote`: 自動為包含空格的節點 ID 添加引號（默認）
- `manual`: 僅報告錯誤，不自動修復

---

## 🎯 質量標準

### 修復後驗證

**必須通過**：
1. ✅ Mermaid Live Editor 渲染正常
2. ✅ GitHub Markdown 預覽正確
3. ✅ VSCode Mermaid 插件無錯誤

### 性能指標

| 指標 | 目標 |
|------|------|
| 掃描速度 | >100 文件/秒 |
| 修復準確率 | >99% |
| 誤報率 | <1% |

---

## 📖 相關資源

### SmartAdmin 文檔

- [mermaid-best-practices.md](../../../extended/domain/igaming-multi-tenant-wallet-pm/knowledge/mermaid-best-practices.md) - SmartAdmin Mermaid 標準
- [CLAUDE.md](../../../../../CLAUDE.md#mermaid-diagram-standards) - Mermaid Diagram Standards 章節

### 外部資源

- [Mermaid 官方文檔](https://mermaid.js.org/)
- [Mermaid Live Editor](https://mermaid.live/) - 實時驗證工具
- [Mermaid Style 語法](https://mermaid.js.org/syntax/flowchart.html#styling-nodes)

---

## 🤝 貢獻指南

### 新增錯誤模式

1. 在 `knowledge/error-patterns.md` 中記錄新模式
2. 更新檢測正則表達式
3. 添加測試案例
4. 更新本 README

### 優化修復策略

1. 提交 Issue 或 PR
2. 包含錯誤樣本和預期修復
3. 通過現有測試案例
4. 更新文檔

---

## 📝 版本歷史

| 版本 | 日期 | 變更內容 |
|------|------|---------|
| 1.0.0 | 2026-02-03 | 初始發布 - Style 語法修復 + SmartAdmin 環境適配 |

---

## 📧 聯絡資訊

**Maintained By**: SmartAdmin Team
**Created By**: Claude Sonnet 4.5
**Last Updated**: 2026-02-03

---

**Status**: ✅ Production Ready

