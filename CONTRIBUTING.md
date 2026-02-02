# 貢獻指南 (Contributing Guide)

感謝您對 SmartAdmin 項目的貢獻！本指南將幫助您了解如何為項目做出貢獻。

## 📋 目錄

- [代碼規範](#代碼規範)
- [文檔規範](#文檔規範)
- [Mermaid 圖表規範](#mermaid-圖表規範)
- [提交規範](#提交規範)
- [Pull Request 流程](#pull-request-流程)

---

## 代碼規範

請參考 [CLAUDE.md](CLAUDE.md) 中的詳細規範，包括：

- 命名規範
- 架構規範
- 依賴注入規範
- 事務管理規範

## 文檔規範

### 文檔位置

- **技術文檔**: `docs/` 目錄
- **業務文檔**: `docs/IGaming/` 目錄
- **架構文檔**: `docs/architecture/` 目錄
- **歸檔文檔**: `docs/archive/` 目錄

### 文檔語言

- **代碼註釋**: 英文
- **用戶文檔**: 繁體中文
- **技術文檔**: 繁體中文 (允許英文術語)

---

## Mermaid 圖表規範

SmartAdmin 項目使用 Mermaid 來繪製流程圖、序列圖和狀態圖。請遵循以下規範：

### ✅ 正確寫法

使用標準的三個反引號 ` ``` ` 作為閉合標記：

\`\`\`markdown
\`\`\`mermaid
graph TD
    A[開始] --> B{條件判斷}
    B -->|是| C[執行操作]
    B -->|否| D[結束]
    C --> D
\`\`\`
\`\`\`

### ❌ 錯誤寫法

**不要**使用四個反引號加語言標記作為閉合標記：

\`\`\`markdown
\`\`\`mermaid
graph TD
    A --> B
\`\`\`\`text  ← ❌ 錯誤！不要使用語言標記閉合！
\`\`\`

### 常見錯誤類型

以下閉合標記都是**錯誤的**：

- ` ````text` ❌
- ` ````yaml` ❌
- ` ````markdown` ❌
- ` ````json` ❌

**正確做法**：統一使用 ` ``` ` 作為閉合標記。

### 支持的圖表類型

SmartAdmin 文檔中常用的 Mermaid 圖表類型：

- `graph TD` / `graph LR` - 流程圖
- `flowchart TD` / `flowchart LR` - 流程圖（新語法）
- `sequenceDiagram` - 序列圖
- `stateDiagram-v2` - 狀態圖
- `classDiagram` - 類圖
- `erDiagram` - ER 圖

### 自動化檢查

項目已配置自動化檢查工具，會在 Git commit 前自動檢查 Mermaid 語法：

```bash
# 手動檢查 Mermaid 語法
npm run check-mermaid:scan

# 手動修正 Mermaid 錯誤
npm run check-mermaid:fix
```

### 圖表最佳實踐

1. **複雜度控制**：單個圖表節點數建議不超過 25 個
2. **使用 subgraph**：對複雜流程進行分組
3. **明確方向**：優先使用 `TD`（上下）或 `LR`（左右）
4. **註釋說明**：對關鍵節點添加註釋

---

## 提交規範

### Commit Message 格式

使用 [Conventional Commits](https://www.conventionalcommits.org/) 規範：

```
<type>(<scope>): <subject>

[optional body]

[optional footer]
```

### Type 類型

- `feat`: 新功能
- `fix`: 修復 bug
- `docs`: 文檔變更
- `style`: 代碼格式（不影響代碼運行的變動）
- `refactor`: 重構（既不是新增功能，也不是修復 bug）
- `test`: 測試相關
- `chore`: 構建過程或輔助工具的變動

### 示例

```bash
# 修復 Mermaid 語法錯誤
git commit -m "fix(docs): 修正 IGaming 文檔中的 Mermaid 閉合標記"

# 添加新文檔
git commit -m "docs(igaming): 添加 VIP 系統設計文檔"

# 重構代碼
git commit -m "refactor(service): 提取 Manager 層事務邏輯"
```

---

## Pull Request 流程

### 1. Fork 項目

點擊 GitHub 頁面右上角的 "Fork" 按鈕。

### 2. Clone 到本地

```bash
git clone https://github.com/YOUR_USERNAME/smart-admin.git
cd smart-admin
```

### 3. 創建分支

```bash
git checkout -b feature/your-feature-name
```

### 4. 提交變更

```bash
git add .
git commit -m "feat(scope): 描述您的變更"
```

### 5. Push 到 Fork 倉庫

```bash
git push origin feature/your-feature-name
```

### 6. 創建 Pull Request

在 GitHub 上創建 PR，描述您的變更內容。

### PR 檢查清單

提交 PR 前，請確認：

- [ ] 代碼符合項目規範（參考 [CLAUDE.md](CLAUDE.md)）
- [ ] 文檔已更新（如果適用）
- [ ] Mermaid 圖表語法正確
- [ ] Pre-commit 檢查通過
- [ ] 所有測試通過
- [ ] Commit message 符合規範

---

## 🙏 感謝您的貢獻！

如有任何問題，請：

- 查看 [README.md](README.md)
- 查看 [CLAUDE.md](CLAUDE.md)
- 提交 [Issue](https://github.com/smart-admin/smart-admin/issues)

**SmartAdmin Team**
