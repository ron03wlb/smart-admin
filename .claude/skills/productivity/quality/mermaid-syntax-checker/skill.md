# Mermaid 語法檢查器 (mermaid-syntax-checker)

**優先級**: P2 (Productivity - Quality)
**執行模式**: Interactive (需要用戶確認)
**預估時間**: 5-15 分鐘

## 功能描述

自動化 Mermaid 圖表語法檢查和修正工具，專門解決常見的閉合標記錯誤。

## 檢測能力

1. **閉合標記錯誤**：
   - ❌ ````text / ````yaml / ````markdown
   - ✅ 正確應為 ```

2. **語法驗證**：
   - 集成 Mermaid CLI `--validate` 模式
   - 檢測無效的圖表語法

3. **統計報告**：
   - 掃描文件數量
   - 錯誤類型分布
   - 修正建議優先級

## 執行模式

### Mode 1: 掃描模式（Scan Only）
```bash
# 僅檢查不修正
npm run check-mermaid:scan
```

### Mode 2: 批量修正模式（Batch Fix）
```bash
# 自動修正所有錯誤
npm run check-mermaid:fix
```

### Mode 3: 回滾模式（Rollback）
```bash
# 回滾到修正前狀態
npm run check-mermaid:rollback
```

## 觸發關鍵字

- "檢查 Mermaid 語法"
- "修正 Mermaid 閉合標記"
- "驗證 Mermaid 圖表"
- "mermaid syntax check"
- "mermaid validation"

## 輸出範例

```
🚀 Mermaid 語法檢查器 v1.0.0
============================
目標目錄: docs/IGaming

🔍 掃描 Mermaid 語法錯誤...

📊 掃描結果：
  - 總錯誤數: 0
  - ````text: 0
  - ````yaml: 0
  - ````markdown: 0

✅ 未發現錯誤
```

## 安全措施

1. **自動備份**：修正前自動創建備份目錄
2. **Git 集成**：Pre-commit hook 自動檢查
3. **排除審計報告**：自動排除包含錯誤示例的文檔
4. **回滾機制**：提供一鍵回滾功能

## 依賴項

- `@mermaid-js/mermaid-cli` (npm package) - v11.12.0+
- `bash` / `sed` / `grep` (系統工具)
- `husky` (Git hooks 管理) - v10.0.0+

## 已配置的工具

### 1. 檢查腳本
- **位置**: `scripts/check-mermaid.sh`
- **功能**: 掃描 Mermaid 閉合標記錯誤
- **過濾**: 自動排除審計報告中的示例

### 2. 修正腳本
- **位置**: `scripts/fix-mermaid-closures.sh`
- **功能**: 批量修正所有錯誤的閉合標記
- **備份**: 自動創建帶時間戳的備份

### 3. Pre-commit Hook
- **位置**: `.husky/pre-commit`
- **功能**: Git commit 前自動檢查
- **行為**: 發現錯誤時阻止提交

### 4. VS Code Snippets
- **位置**: `.vscode/mermaid.code-snippets`
- **功能**: 提供 10 個 Mermaid 模板
- **包含**: flowchart, sequence, state, class, ER 等

## 使用範例

### 範例 1: 日常檢查

```bash
# 每日定期掃描
npm run check-mermaid:scan
```

### 範例 2: 修正錯誤

```bash
# 批量修正所有錯誤
npm run check-mermaid:fix
```

### 範例 3: Git Commit

```bash
# Pre-commit hook 會自動觸發檢查
git add docs/IGaming/some-file.md
git commit -m "docs: 更新文檔"

# 如果檢測到錯誤，會顯示：
# ❌ 發現錯誤的 Mermaid 閉合標記: docs/IGaming/some-file.md
# 請修正後再提交
```

### 範例 4: VS Code 中使用 Snippets

在 Markdown 文件中輸入：
- `mermaid-flow-td` → 生成 flowchart TD 模板
- `mermaid-seq` → 生成 sequenceDiagram 模板
- `mermaid-decision` → 生成決策流程模板

## 限制與注意事項

- 僅處理 Markdown 文件中的 Mermaid 代碼塊
- 不驗證 Mermaid 圖表的業務邏輯正確性
- 建議在修正前先 commit 當前變更
- 審計報告中的錯誤示例會被自動排除

## 與其他 Skill 的關係

- **互補**: `smartadmin-documentation-generator` - 文檔生成後自動驗證
- **前置**: Pre-commit hook 可觸發此 Skill
- **後續**: `quality-gate-orchestrator` 可集成此檢查

## 配置文件

### package.json 腳本

```json
{
  "scripts": {
    "check-mermaid": "bash scripts/check-mermaid.sh",
    "check-mermaid:scan": "bash scripts/check-mermaid.sh docs/IGaming scan",
    "check-mermaid:fix": "bash scripts/check-mermaid.sh docs/IGaming auto-fix",
    "check-mermaid:rollback": "bash scripts/check-mermaid.sh docs/IGaming rollback"
  }
}
```

### Skill Registry

在 `.claude/skills/skill-registry.yml` 中：

```yaml
productivity:
  quality:
    - name: mermaid-syntax-checker
      priority: P2
      triggers:
        - "檢查 Mermaid 語法"
        - "修正 Mermaid 閉合標記"
        - "驗證 Mermaid 圖表"
        - "mermaid syntax check"
        - "mermaid validation"
      execution_mode: interactive
      estimated_time: "5-15 minutes"
      dependencies:
        - "@mermaid-js/mermaid-cli"
        - "husky"
      related_skills:
        - "smartadmin-documentation-generator"
        - "quality-gate-orchestrator"
```

## 版本歷史

- **v1.0.0** (2026-02-02): 初始版本
  - 支持閉合標記檢查和修正
  - Pre-commit hook 集成
  - VS Code Snippets
  - 自動排除審計報告

## 技術實現

### 檢測邏輯

```bash
# 使用 grep 查找錯誤模式，排除審計報告
grep -r '````text' docs/IGaming --include="*.md" | \
    grep -v "CORRECTION_REPORT.md" | \
    grep -v "IGaming_Documentation_Audit_Report.md" | \
    grep -v "DOCUMENTATION_AUDIT_REPORT.md"
```

### 修正邏輯

```bash
# 使用 sed 批量替換
find docs/IGaming -name "*.md" -type f -exec sed -i 's/^````text$/```/g' {} \;
```

### Pre-commit 檢查

```bash
# 檢查暫存的 Markdown 文件
STAGED_MD_FILES=$(git diff --cached --name-only --diff-filter=ACM | grep '\.md$')

# 對每個文件檢查錯誤的閉合標記
for file in $STAGED_MD_FILES; do
    if grep -q '````text\|````yaml\|````markdown' "$file"; then
        echo "❌ 發現錯誤: $file"
        exit 1
    fi
done
```

## 故障排除

### 問題 1: Pre-commit hook 沒有執行

**解決方案**:
```bash
# 重新初始化 Husky
npx husky install
chmod +x .husky/pre-commit
```

### 問題 2: npm 腳本無法執行

**解決方案**:
```bash
# 確保腳本有執行權限
chmod +x scripts/check-mermaid.sh
chmod +x scripts/fix-mermaid-closures.sh
```

### 問題 3: 誤報審計報告中的錯誤

**說明**: 審計報告中包含錯誤示例，這是正常的。檢查腳本已自動排除這些文件。

## 貢獻與反饋

如需改進此 Skill，請：
1. 查看 [CONTRIBUTING.md](../../../../CONTRIBUTING.md)
2. 提交 Issue 或 Pull Request
3. 聯繫 SmartAdmin Team

---

**維護者**: SmartAdmin Team
**最後更新**: 2026-02-02
**文檔版本**: 1.0.0
