# Git Hooks 架構設計文檔

> **版本**: 1.1.0
> **設計日期**: 2026-02-09
> **設計者**: Phase 8 Implementation Team
> **狀態**: ✅ Implemented (P0 + P1 Complete)

---

## 🎯 設計目標

將現有成熟驗證腳本（9個，~700 lines）整合到 Git Hooks，實現：
1. **Pre-commit**: 快速文檔質量驗證（Mermaid、Emoji、stateDiagram）
2. **Pre-push**: 綜合質量門禁（需求純度、架構完整性、交叉引用）
3. **自動化率**: 0% → 90%（預防文檔品質退步）

---

## 📋 核心原則

### 1. 快速失敗 (Fail Fast)
- 第一個錯誤即停止（不浪費開發者時間）
- 清晰的錯誤訊息和修復建議
- 錯誤訊息包含文件路徑和行號

### 2. 增量驗證 (Incremental)
- **Pre-commit**: 只驗證 staged files（不驗證整個倉庫）
- 使用 `git diff --cached --name-only --diff-filter=ACM` 獲取 staged files
- 過濾相關文件類型（`.md` for 文檔驗證，`.java` for Spotless）

### 3. 並行執行 (Parallel)
- 多個驗證任務並行執行（提高性能）
- 使用 bash 後台執行（`&`）和 `wait` 命令
- 目標：將驗證時間控制在 30 秒內（99% 情況）

### 4. 友好錯誤 (User-friendly)
- 彩色輸出（錯誤=紅色，警告=黃色，成功=綠色）
- 進度提示（讓開發者知道 hook 在運行）
- 修復建議（告訴開發者如何解決問題）

### 5. 可繞過 (Bypassable)
- 提供 `--no-verify` 選項（緊急情況）
- 在錯誤訊息中說明如何 bypass
- 記錄 bypass 使用情況（未來可能添加審計日誌）

---

## 🏗️ Pre-commit Hook 架構

### 功能模塊

```
pre-commit
├── [1] Header & Setup (30 lines)
│   ├── Shebang: #!/bin/bash
│   ├── 環境變數設置
│   ├── 彩色輸出配置
│   └── 錯誤處理設置 (set -e)
│
├── [2] Staged Files 檢測 (40 lines)
│   ├── 檢測 staged Markdown 文件
│   │   └── git diff --cached --name-only --diff-filter=ACM "*.md"
│   ├── 檢測 staged Java 文件
│   │   └── git diff --cached --name-only --diff-filter=ACM "*.java"
│   └── 如果沒有相關文件，跳過驗證
│
├── [3] Java 格式化 (30 lines)
│   ├── 保留現有 Spotless 邏輯
│   ├── cd smart-admin-api-java21-springboot3
│   ├── ./gradlew spotlessApply
│   └── 錯誤處理和用戶提示
│
├── [4] 文檔驗證（並行執行，60 lines）
│   ├── [4.1] Mermaid 語法驗證
│   │   ├── 檢查 mmdc CLI 是否安裝
│   │   ├── 如果未安裝，跳過並警告
│   │   ├── 調用: scripts/validate-mermaid.sh <staged-files-dir>
│   │   └── 解析 exit code 和錯誤訊息
│   │
│   ├── [4.2] Emoji 檢測
│   │   ├── 調用: scripts/detect-mermaid-emoji.sh <staged-files-dir>
│   │   ├── 解析 exit code
│   │   └── 簡化輸出（只顯示錯誤文件清單）
│   │
│   └── [4.3] stateDiagram <br/> 檢測
│       ├── 調用: scripts/detect-statediagram-br.sh <staged-files-dir>
│       ├── 解析 exit code
│       └── 簡化輸出（只顯示錯誤文件清單）
│
└── [5] 錯誤彙總和用戶提示 (30 lines)
    ├── 顯示所有驗證結果
    ├── 提供修復建議
    ├── 說明 --no-verify bypass 選項
    └── Exit with appropriate code (0=success, 1=failure)
```

### 並行執行策略

```bash
# 偽代碼示例
VALIDATION_ERRORS=0

# 檢測 staged Markdown 文件
STAGED_MD_FILES=$(git diff --cached --name-only --diff-filter=ACM "*.md")

if [ -n "$STAGED_MD_FILES" ]; then
    # 創建臨時目錄（只包含 staged files）
    TEMP_DIR=$(mktemp -d)

    # 複製 staged files 到臨時目錄（保留目錄結構）
    for file in $STAGED_MD_FILES; do
        mkdir -p "$TEMP_DIR/$(dirname "$file")"
        cp "$file" "$TEMP_DIR/$file"
    done

    # 並行執行驗證任務
    (
        ./scripts/validate-mermaid.sh "$TEMP_DIR" > /tmp/mermaid-result.txt 2>&1
        echo $? > /tmp/mermaid-exit.txt
    ) &
    MERMAID_PID=$!

    (
        ./scripts/detect-mermaid-emoji.sh "$TEMP_DIR" > /tmp/emoji-result.txt 2>&1
        echo $? > /tmp/emoji-exit.txt
    ) &
    EMOJI_PID=$!

    (
        ./scripts/detect-statediagram-br.sh "$TEMP_DIR" > /tmp/statediagram-result.txt 2>&1
        echo $? > /tmp/statediagram-exit.txt
    ) &
    STATEDIAGRAM_PID=$!

    # 等待所有後台任務完成
    wait $MERMAID_PID $EMOJI_PID $STATEDIAGRAM_PID

    # 檢查結果
    MERMAID_EXIT=$(cat /tmp/mermaid-exit.txt)
    EMOJI_EXIT=$(cat /tmp/emoji-exit.txt)
    STATEDIAGRAM_EXIT=$(cat /tmp/statediagram-exit.txt)

    # 顯示錯誤（如果有）
    if [ "$MERMAID_EXIT" -ne 0 ]; then
        echo "❌ Mermaid 語法驗證失敗"
        cat /tmp/mermaid-result.txt | grep "✗" # 只顯示錯誤行
        VALIDATION_ERRORS=$((VALIDATION_ERRORS + 1))
    fi

    # ... 類似處理 EMOJI 和 STATEDIAGRAM

    # 清理臨時文件
    rm -rf "$TEMP_DIR" /tmp/*-result.txt /tmp/*-exit.txt
fi

# 最終結果
if [ $VALIDATION_ERRORS -gt 0 ]; then
    echo ""
    echo "💡 修復建議："
    echo "  1. 查看上述錯誤訊息並修復"
    echo "  2. 重新 git add <fixed-files>"
    echo "  3. 如果緊急，可使用: git commit --no-verify"
    exit 1
else
    echo "✅ 所有驗證通過！"
    exit 0
fi
```

### 性能優化

| 優化策略 | 描述 | 預期性能提升 |
|---------|------|-------------|
| **增量驗證** | 只驗證 staged files，不驗證整個倉庫 | 10x - 100x faster |
| **並行執行** | 3 個驗證任務並行（Mermaid、Emoji、stateDiagram） | 3x faster |
| **mmdc Fallback** | 如果 mmdc 未安裝，跳過 Mermaid 語法驗證（只保留正則檢查） | 避免阻塞 commit |
| **簡化輸出** | 不顯示完整報告，只顯示錯誤文件清單 | 更快的視覺反饋 |

**預期性能**:
- 小型提交（1-5 files）：**5-10 秒**
- 中型提交（10-20 files）：**15-20 秒**
- 大型提交（50+ files）：**30-45 秒**

---

## 🚀 Pre-push Hook 架構

### 功能模塊

```
pre-push
├── [1] Header & Setup (30 lines)
│   ├── Shebang: #!/bin/bash
│   ├── 環境變數設置
│   ├── 彩色輸出配置
│   └── 錯誤處理設置 (set -e)
│
├── [2] 簡化質量門禁 (100 lines)
│   ├── 不生成完整質量報告（太慢）
│   ├── 只執行關鍵驗證：
│   │   ├── validate-requirements-purity.sh
│   │   ├── validate-architecture-completeness.sh
│   │   └── validate-cross-references.py（如果存在）
│   └── 並行執行驗證任務
│
├── [3] 結果解析和顯示 (40 lines)
│   ├── 解析 exit code
│   ├── 顯示失敗的驗證項目
│   └── 提供詳細報告路徑（如果需要）
│
└── [4] 用戶提示 (30 lines)
    ├── 顯示質量門禁狀態摘要
    ├── 提供修復建議
    ├── 說明 --no-verify bypass 選項
    └── Exit with appropriate code (0=success, 1=failure)
```

### 簡化策略

**問題**: `generate-quality-report.sh` 生成 ~500 行完整報告，耗時 30-60 秒，不適合 pre-push hook。

**解決方案**: 不調用 `generate-quality-report.sh`，而是直接調用核心驗證腳本：

```bash
# 並行執行核心驗證
(
    ./scripts/validate-requirements-purity.sh > /tmp/purity-result.txt 2>&1
    echo $? > /tmp/purity-exit.txt
) &
PURITY_PID=$!

(
    ./scripts/validate-architecture-completeness.sh > /tmp/completeness-result.txt 2>&1
    echo $? > /tmp/completeness-exit.txt
) &
COMPLETENESS_PID=$!

# 等待完成
wait $PURITY_PID $COMPLETENESS_PID

# 檢查結果
PURITY_EXIT=$(cat /tmp/purity-exit.txt)
COMPLETENESS_EXIT=$(cat /tmp/completeness-exit.txt)

# 顯示摘要（不顯示完整報告）
echo "📊 質量門禁檢查結果："
echo ""
if [ "$PURITY_EXIT" -eq 0 ]; then
    echo "  ✅ 需求層業務純度檢查"
else
    echo "  ❌ 需求層業務純度檢查 (發現技術關鍵詞)"
    grep "❌ FAILED:" /tmp/purity-result.txt | head -5 # 只顯示前 5 個錯誤
fi

if [ "$COMPLETENESS_EXIT" -eq 0 ]; then
    echo "  ✅ 架構層技術完整性檢查"
else
    echo "  ❌ 架構層技術完整性檢查 (缺少 back-references)"
    grep "⚠️  Missing backref:" /tmp/completeness-result.txt | head -5
fi

# 最終結果
if [ "$PURITY_EXIT" -ne 0 ] || [ "$COMPLETENESS_EXIT" -ne 0 ]; then
    echo ""
    echo "💡 詳細報告："
    echo "  需求純度: cat /tmp/purity-result.txt"
    echo "  架構完整性: cat /tmp/completeness-result.txt"
    echo ""
    echo "💡 修復後重新 push，或使用: git push --no-verify"
    exit 1
else
    echo ""
    echo "✅ 所有質量門禁通過！"
    exit 0
fi
```

**性能提升**:
- 原方案（generate-quality-report.sh）：30-60 秒
- 簡化方案（直接調用核心腳本）：**10-15 秒**（並行執行）

---

## 🔧 安裝腳本架構 (install-hooks.sh)

### 功能模塊

```
install-hooks.sh (~400 lines)
├── [1] Header & Configuration (50 lines)
│   ├── Shebang, 顏色配置
│   ├── 常量定義（HOOKS_DIR, BACKUP_DIR 等）
│   └── 工具函數（log_info, log_error, log_success）
│
├── [2] 環境檢查 (50 lines)
│   ├── 檢查是否在 Git 倉庫根目錄
│   │   └── test -d .git || exit 1
│   ├── 檢查 .git/hooks/ 目錄是否存在
│   ├── 檢查必要的驗證腳本是否存在
│   │   ├── scripts/validate-mermaid.sh
│   │   ├── scripts/detect-mermaid-emoji.sh
│   │   ├── scripts/detect-statediagram-br.sh
│   │   ├── scripts/validate-requirements-purity.sh
│   │   └── scripts/validate-architecture-completeness.sh
│   └── 檢查外部依賴（mmdc CLI）
│       ├── command -v mmdc &> /dev/null
│       ├── 如果未安裝，警告但不阻止安裝
│       └── 提供安裝指南
│
├── [3] 現有 Hooks 備份 (80 lines)
│   ├── 檢測現有 pre-commit、pre-push 是否存在
│   ├── 創建備份目錄
│   │   └── .git/hooks/backup-$(date +%Y%m%d-%H%M%S)/
│   ├── 複製現有 hooks 到備份目錄
│   ├── 記錄備份元數據
│   │   └── .git/hooks/backup-*/BACKUP_INFO.txt
│   └── 顯示備份路徑
│
├── [4] Hooks 生成 (150 lines)
│   ├── [4.1] 生成 pre-commit
│   │   ├── 從模板生成（embedded in install-hooks.sh）
│   │   ├── 設置可執行權限 (chmod +x)
│   │   └── 驗證語法正確性 (bash -n)
│   │
│   └── [4.2] 生成 pre-push
│       ├── 從模板生成
│       ├── 設置可執行權限
│       └── 驗證語法正確性
│
├── [5] 錯誤處理 (70 lines)
│   ├── 檢測衝突（自定義 hooks vs 生成 hooks）
│   ├── 回滾機制（安裝失敗時恢復備份）
│   ├── 清理臨時文件
│   └── 用戶友好的錯誤訊息
│
└── [6] 成功提示 (50 lines)
    ├── 顯示安裝成功訊息
    ├── 列出已安裝的 hooks
    ├── 說明如何卸載 (./scripts/uninstall-hooks.sh)
    ├── 說明 bypass 機制 (--no-verify)
    └── 提示查看使用指南 (docs/development/git-hooks-guide.md)
```

### Hooks 模板嵌入策略

使用 heredoc 嵌入 hooks 模板到 `install-hooks.sh` 中：

```bash
# 生成 pre-commit hook
cat > .git/hooks/pre-commit <<'EOF'
#!/bin/bash
# SmartAdmin Git Pre-commit Hook
# 自動生成於: $(date)
# 管理者: scripts/install-hooks.sh

set -e

# 顏色配置
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}🔍 執行 pre-commit 檢查...${NC}"

# [檢測 staged files 邏輯]
# [Java 格式化邏輯]
# [文檔驗證邏輯]
# ...

echo -e "${GREEN}✅ Pre-commit 檢查通過！${NC}"
exit 0
EOF

chmod +x .git/hooks/pre-commit
bash -n .git/hooks/pre-commit || {
    echo "❌ 錯誤：生成的 pre-commit hook 語法錯誤"
    exit 1
}
```

**優點**:
- 單一腳本安裝（不需要額外的模板文件）
- 易於維護（所有邏輯在一個文件）
- 易於版本控制（install-hooks.sh 本身被追蹤）

---

## 🗑️ 卸載腳本架構 (uninstall-hooks.sh)

### 功能模塊

```
uninstall-hooks.sh (~100 lines)
├── [1] Header & Configuration (20 lines)
│
├── [2] 環境檢查 (20 lines)
│   ├── 檢查是否在 Git 倉庫根目錄
│   └── 檢查 hooks 是否已安裝
│
├── [3] 恢復備份 (40 lines)
│   ├── 查找最新的備份目錄
│   ├── 恢復備份的 hooks（如果存在）
│   └── 如果沒有備份，刪除生成的 hooks
│
├── [4] 清理臨時文件 (10 lines)
│   └── rm -f /tmp/*-result.txt /tmp/*-exit.txt
│
└── [5] 成功提示 (10 lines)
    ├── 顯示卸載成功訊息
    └── 說明如何重新安裝
```

---

## 🎯 關鍵決策

### 決策 1: mmdc CLI 依賴處理

**問題**: `validate-mermaid.sh` 需要 `mmdc` CLI，但不是所有開發者都有安裝。

**方案比較**:

| 方案 | 優點 | 缺點 | 結論 |
|------|------|------|------|
| **A. 強制要求安裝** | 完整驗證 | 阻礙開發者 onboarding | ❌ 不採用 |
| **B. 檢測並警告** | 不阻塞 commit | 降低驗證覆蓋率 | ⚠️ 次選 |
| **C. Fallback 降級驗證** | 友好 + 基本驗證 | 需要額外實現 | ✅ **採用** |

**實施方案 C**:
```bash
if command -v mmdc &> /dev/null; then
    # mmdc 已安裝，執行完整驗證
    ./scripts/validate-mermaid.sh "$TEMP_DIR"
else
    # mmdc 未安裝，執行正則檢查（降級驗證）
    echo "⚠️  mmdc 未安裝，跳過 Mermaid 語法驗證"
    echo "   安裝方法: npm install -g @mermaid-js/mermaid-cli"

    # 基本正則檢查（檢測明顯語法錯誤）
    grep -r "```mermaid" "$TEMP_DIR" | while read -r line; do
        # 檢查是否有未閉合的代碼塊等
        # ...基本驗證邏輯
    done
fi
```

### 決策 2: Pre-push 質量門禁簡化

**問題**: `generate-quality-report.sh` 生成完整報告耗時 30-60 秒，影響開發體驗。

**方案**: 不調用 `generate-quality-report.sh`，直接調用核心驗證腳本（並行執行）。

**性能對比**:
- 原方案：30-60 秒（順序執行 + 生成完整報告）
- 簡化方案：**10-15 秒**（並行執行 + 只顯示摘要）

### 決策 3: 並行執行 vs 順序執行

**方案**: 並行執行（使用 bash 後台任務）

**實施**:
```bash
# 並行執行 3 個驗證任務
task1 & PID1=$!
task2 & PID2=$!
task3 & PID3=$!

# 等待所有任務完成
wait $PID1 $PID2 $PID3

# 檢查結果
# ...
```

**預期性能提升**: 3x faster（理論上，實際約 2.5x due to overhead）

---

## 📊 成功指標

### 性能指標

| 指標 | 目標 | 測量方法 |
|------|------|---------|
| **Pre-commit 時間** | < 30 秒（99% 情況） | time git commit -m "test" |
| **Pre-push 時間** | < 15 秒（90% 情況） | time git push |
| **開發者滿意度** | > 80% 滿意 | 問卷調查（安裝 1 週後） |

### 質量指標

| 指標 | 目標 | 測量方法 |
|------|------|---------|
| **自動化率** | 90% 文檔驗證自動化 | 人工 review 時間減少 |
| **錯誤檢出率** | > 95% Mermaid 錯誤被阻止 | 監控 post-push 錯誤數量 |
| **Bypass 濫用率** | < 10% commits 使用 --no-verify | Git logs 分析 |

---

## ✅ 實施狀態

### P0: Core Hooks (2026-02-09) — COMPLETED

| 組件 | 狀態 | 說明 |
|------|------|------|
| `scripts/install-hooks.sh` | ✅ | ~380 lines, 生成 pre-commit + pre-push |
| `scripts/uninstall-hooks.sh` | ✅ | ~85 lines, 備份恢復機制 |
| `build.gradle.kts` | ✅ | installGitHooks task 委派至 install-hooks.sh |
| Pre-commit hook | ✅ | Spotless + 並行 Markdown 驗證 |
| Pre-push hook | ✅ | 並行質量門禁 |

### P1: Validation Scripts (2026-02-09) — COMPLETED

| 腳本 | 狀態 | Pre-commit | Pre-push |
|------|------|-----------|----------|
| `detect-statediagram-br.sh` | ✅ | ✓ | |
| `validate-mermaid.sh` | ✅ | ✓ | |
| `detect-mermaid-emoji.sh` | ✅ | ✓ | |
| `validate-requirements-purity.sh` | ✅ | | ✓ |
| `validate-architecture-completeness.sh` | ✅ | | ✓ |

### 實施備註

1. **Symlink 處理**: 發現原始 pre-commit 是 symlink → pre-commit-igaming.sh，
   `cat >` 會寫穿 symlink 覆蓋原文件。修復為先 `rm -f` 再寫入。
2. **Forward-compatible 設計**: 使用 `run_if_exists()` 函數，不存在的腳本自動跳過不阻塞。
3. **現有腳本整合**: detect-bonus-corruption.sh、validate-file-numbering.sh 等 5 個
   iGaming 專用腳本直接整合到 hooks 中。

---

**文檔版本**: 1.1.0
**最後更新**: 2026-02-09
**作者**: Phase 8 Implementation Team
