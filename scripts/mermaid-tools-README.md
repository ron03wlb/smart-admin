# Mermaid Tools - stateDiagram 修復工具集

> **版本**: 1.0.0
> **建立日期**: 2026-02-04
> **維護者**: SmartAdmin Development Team

---

## 📋 工具清單

| 工具名稱 | 類型 | 用途 | 執行時間 |
|---------|------|------|----------|
| `detect-statediagram-br.sh` | Shell | 檢測所有 stateDiagram 中的 `<br/>` 錯誤 | < 10 秒 |
| `fix-statediagram-br-tags.py` | Python | 修復單個或多個文件的 `<br/>` 錯誤 | < 5 秒/文件 |
| `batch-fix-statediagram-br.sh` | Shell | 批量修復所有錯誤文件 | < 1 分鐘 |
| `validate-mermaid.sh` | Shell | 驗證 Mermaid 語法（需 Mermaid CLI） | < 30 秒 |

---

## 🚀 快速開始

### 1. 檢測錯誤

```bash
cd /path/to/smart-admin

# 掃描所有 iGaming 文檔
./scripts/detect-statediagram-br.sh docs/iGaming/

# 輸出示例：
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
#   檢測完成
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# 總文件數:     12
# 錯誤文件數:   11
# 總錯誤數:     50+
#
# 詳細報告已保存至: /tmp/statediagram-errors-20260204-171443.txt
# 錯誤文件清單已保存至: /tmp/statediagram-error-files.txt
```

### 2. 修復錯誤（推薦方法）

```bash
# 方法 A: 批量自動修復（推薦）
./scripts/batch-fix-statediagram-br.sh --verify

# 方法 B: 修復單個文件
python3 scripts/fix-statediagram-br-tags.py \
  --strategy auto \
  --verify \
  docs/iGaming/03_Game_Center/03-03_Seamless_Wallet_Analysis.md
```

### 3. 驗證修復結果

```bash
# 驗證所有 Mermaid 圖表
./scripts/validate-mermaid.sh docs/iGaming/

# 或使用 Git diff 檢查修改
git diff docs/iGaming/
```

---

## 🔧 工具詳細說明

### 1. detect-statediagram-br.sh

**用途**: 掃描並檢測 stateDiagram 中的 `<br/>` 標籤錯誤

**語法**:
```bash
./scripts/detect-statediagram-br.sh [目錄路徑]
```

**參數**:
- `目錄路徑` (可選): 要掃描的目錄，默認為 `docs/iGaming`

**輸出**:
- 終端顯示錯誤統計和前 3 個錯誤示例
- 詳細錯誤報告: `/tmp/statediagram-errors-YYYYMMDD-HHMMSS.txt`
- 錯誤文件清單: `/tmp/statediagram-error-files.txt`

**退出碼**:
- `0`: 無錯誤
- `1`: 發現錯誤

**範例**:
```bash
# 掃描 iGaming 文檔
./scripts/detect-statediagram-br.sh docs/iGaming/

# 掃描所有文檔
./scripts/detect-statediagram-br.sh docs/
```

---

### 2. fix-statediagram-br-tags.py

**用途**: 修復單個或多個文件的 stateDiagram `<br/>` 錯誤

**語法**:
```bash
python3 scripts/fix-statediagram-br-tags.py [選項] <文件1> [文件2] ...
```

**選項**:
- `--strategy <auto|simple|note>`: 修復策略（默認：auto）
  - `auto`: 自動選擇（P0/P1 使用 note，P2 使用 simple）
  - `simple`: 簡化標籤（快速修復）
  - `note`: 移至 note 區塊（保留完整信息）
- `--dry-run`: 僅預覽，不實際修改文件
- `--verify`: 使用 Mermaid CLI 驗證修復結果（需安裝 `@mermaid-js/mermaid-cli`）

**範例**:
```bash
# 自動修復單個文件
python3 scripts/fix-statediagram-br-tags.py \
  docs/iGaming/03_Game_Center/03-03_Seamless_Wallet_Analysis.md

# Dry-run 模式（預覽）
python3 scripts/fix-statediagram-br-tags.py \
  --dry-run \
  --strategy auto \
  docs/iGaming/01_Player_Center/01-01_Player_Lifecycle.md

# 批量修復多個文件
python3 scripts/fix-statediagram-br-tags.py \
  --strategy note \
  --verify \
  docs/iGaming/**/*.md
```

**輸出**:
```
===========================================================
Mermaid stateDiagram <br/> 修復工具
============================================================
策略: auto
模式: EXECUTE
驗證: 啟用
============================================================

✓ docs/iGaming/03_Game_Center/03-03_Seamless_Wallet_Analysis.md
  策略: note, 修復圖表: 1, 修復標籤: 10

============================================================
修復完成
============================================================
總文件數:     1
修復圖表數:   1
修復標籤總數: 10
============================================================
```

---

### 3. batch-fix-statediagram-br.sh

**用途**: 批量修復所有錯誤文件並生成報告

**語法**:
```bash
./scripts/batch-fix-statediagram-br.sh [選項]
```

**選項**:
- `--dry-run`: 僅預覽，不實際修改
- `--verify`: 使用 Mermaid CLI 驗證修復結果
- `--strategy <auto|simple|note>`: 強制指定修復策略

**執行流程**:
1. **檢測階段**: 掃描所有錯誤文件
2. **修復階段**: 調用 Python 腳本逐個修復
3. **報告階段**: 生成修復報告（`/tmp/statediagram-fix-report-YYYYMMDD-HHMMSS.md`）

**範例**:
```bash
# 批量修復（推薦）
./scripts/batch-fix-statediagram-br.sh --verify

# Dry-run 模式
./scripts/batch-fix-statediagram-br.sh --dry-run

# 強制使用 simple 策略
./scripts/batch-fix-statediagram-br.sh --strategy simple
```

---

### 4. validate-mermaid.sh

**用途**: 驗證所有 Mermaid 圖表的語法正確性

**前置需求**:
```bash
# 安裝 Mermaid CLI
npm install -g @mermaid-js/mermaid-cli
```

**語法**:
```bash
./scripts/validate-mermaid.sh [目錄路徑]
```

**範例**:
```bash
# 驗證 iGaming 文檔
./scripts/validate-mermaid.sh docs/iGaming/

# 驗證所有文檔
./scripts/validate-mermaid.sh docs/
```

**輸出**:
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Mermaid 語法驗證工具
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✓ Mermaid CLI 已安裝

掃描目錄: docs/iGaming/

階段 1: 查找 Mermaid 圖表...
階段 2: 驗證 Mermaid 語法...

✓ docs/iGaming/01_Player_Center/01-01_Player_Lifecycle.md
✓ docs/iGaming/03_Game_Center/03-03_Seamless_Wallet_Analysis.md

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  驗證完成
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
總文件數:     12
總圖表數:     15
驗證通過:     12
驗證失敗:     0

✓ 所有 Mermaid 圖表語法正確
```

---

## 📊 典型工作流程

### 場景 1: 新文檔開發（預防錯誤）

```bash
# Step 1: 使用模板創建新文檔
cp docs/iGaming/.templates/statediagram-template.md \
   docs/iGaming/new-feature.md

# Step 2: 編輯文檔（遵循模板）
vim docs/iGaming/new-feature.md

# Step 3: 驗證語法
./scripts/validate-mermaid.sh docs/iGaming/new-feature.md

# Step 4: 提交前檢查（Pre-commit hook 會自動執行）
git add docs/iGaming/new-feature.md
git commit -m "docs(igaming): 新增 XXX 功能文檔"
```

### 場景 2: 修復現有錯誤（批量處理）

```bash
# Step 1: 檢測所有錯誤
./scripts/detect-statediagram-br.sh docs/iGaming/

# Step 2: 查看錯誤報告
cat /tmp/statediagram-errors-*.txt

# Step 3: 執行批量修復
./scripts/batch-fix-statediagram-br.sh --verify

# Step 4: 查看修復差異
git diff docs/iGaming/

# Step 5: 驗證修復結果
./scripts/validate-mermaid.sh docs/iGaming/

# Step 6: 提交修復
git add docs/iGaming/
git commit -m "fix(docs): 修復 stateDiagram Mermaid 語法錯誤"
```

### 場景 3: Pull Request 審查

```bash
# Step 1: 檢查 PR 中的 Mermaid 修改
git diff origin/master...feature-branch docs/**/*.md | grep -A 10 "```mermaid"

# Step 2: 驗證語法（CI/CD 會自動執行）
# 見 .github/workflows/mermaid-validation.yml

# Step 3: 手動驗證（如果需要）
./scripts/validate-mermaid.sh docs/iGaming/
```

---

## 🚨 故障排查

### 問題 1: Python 腳本無法執行

**症狀**:
```
bash: python3: command not found
```

**解決方法**:
```bash
# 檢查 Python 版本
python --version  # 或 python3 --version

# 如果未安裝，請安裝 Python 3.7+
# Ubuntu/Debian:
sudo apt-get install python3

# macOS:
brew install python3

# Windows:
# 從 https://www.python.org/downloads/ 下載安裝
```

### 問題 2: Mermaid CLI 驗證失敗

**症狀**:
```
✗ 錯誤: 未安裝 Mermaid CLI
```

**解決方法**:
```bash
# 安裝 Mermaid CLI
npm install -g @mermaid-js/mermaid-cli

# 驗證安裝
mmdc --version
```

### 問題 3: 修復後圖表仍然無法渲染

**症狀**: 修復後的 Mermaid 圖表在文檔中無法正常顯示

**排查步驟**:
1. **複製圖表代碼**: 複製修復後的 Mermaid 代碼
2. **線上測試**: 在 [Mermaid Live Editor](https://mermaid.live/) 中測試
3. **檢查語法**: 確認是否有其他語法錯誤（如未閉合的 state 區塊）
4. **手動修復**: 根據錯誤信息手動調整

### 問題 4: Pre-commit Hook 未執行

**症狀**: 提交包含 `<br/>` 錯誤的代碼沒有被阻止

**解決方法**:
```bash
# 檢查 Pre-commit hook 是否存在
ls -la .git/hooks/pre-commit

# 如果不存在，複製模板
cp scripts/pre-commit.template .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit

# 測試 hook
./git/hooks/pre-commit
```

---

## 📚 參考資源

- **Mermaid 最佳實踐**: `.claude/skills/extended/domain/igaming-multi-tenant-wallet-pm/knowledge/mermaid-best-practices.md`
- **CLAUDE.md**: `CLAUDE.md#mermaid-diagram-standards`
- **文檔模板**: `docs/iGaming/.templates/statediagram-template.md`
- **Mermaid 官方文檔**: https://mermaid.js.org/
- **Mermaid Live Editor**: https://mermaid.live/

---

## 📞 支援

如有問題，請參考：
1. **Mermaid Best Practices**: 完整語法指南和錯誤案例
2. **GitHub Issues**: 提交 Bug 或功能請求
3. **Team Chat**: 聯繫 SmartAdmin Development Team

---

**版本**: 1.0.0
**最後更新**: 2026-02-04
**維護者**: SmartAdmin Development Team
