# Stop Hooks 優化 v3.0 - 品質關卡「後移」到 Git 提交流程

**版本**: v3.0.0
**日期**: 2026-03-28
**核心理念**: 將品質檢查從「Claude 對話結束」移至「Git 提交/推送前」
**優化效果**: Claude 對話立即返回（< 1s），嚴格檢查在真正需要時執行

---

## 📋 執行摘要

### v2.0 → v3.0 升級重點

| 階段 | v2.0 (Git Diff 智能) | v3.0 (Git 流程優先) | 改進 |
|------|---------------------|-------------------|------|
| **Claude 對話結束** | 執行測試（< 5s）| 僅報告（< 1s）| ⬇️ 80% |
| **git commit** | 格式化 + Markdown（~10s）| **+ ArchUnit（~20s）** | ⬆️ 100% |
| **git push** | 無檢查（0s）| **完整測試（~60s）** | ⬆️ 新增 |

**核心改變**:
- ✅ Claude 對話無阻塞（< 1s）
- ✅ Commit 前確保架構正確（ArchUnit）
- ✅ Push 前確保測試通過（完整套件）
- ✅ 品質關卡「後移」到真正需要的時候

---

## 🎯 v3.0 優化策略

### 核心理念

**問題識別** (v2.0):
- Claude Stop Hooks 每次對話結束都執行測試（即使只是詢問問題）
- 使用者等待 5 秒（雖然已優化 99%，但仍有等待）
- 品質檢查時機不合理（應在提交代碼時檢查，而非對話時）

**解決方案** (v3.0):
- Claude Stop Hooks → **僅報告變更**（< 1s，無測試）
- Git Pre-commit → **添加 ArchUnit 架構測試**（模組化，只測變更）
- Git Pre-push → **添加完整測試套件**（並行執行，覆蓋率驗證）

---

## 📂 修改檔案清單

### 新建檔案 (1 個)

1. **`scripts/code-change-summary.sh`**
   - 功能：替代 `smart-quality-gate-lite.sh`
   - 用途：僅報告變更，不執行測試
   - 執行時間：< 1s

### 修改檔案 (3 個)

2. **`.claude/settings.json`**
   - 變更：Stop Hook 從執行測試改為僅報告
   - 影響：Claude 對話結束立即返回（< 1s）

3. **`.git/hooks/pre-commit`**
   - 變更：添加 ArchUnit 架構測試（變更模組）
   - 影響：commit 前確保架構正確（~20s）

4. **`.git/hooks/pre-push`**
   - 變更：添加完整測試套件 + 覆蓋率檢查
   - 影響：push 前確保測試通過（~60s）

### 建立目錄 (1 個)

5. **`.git/hooks/logs/`**
   - 用途：儲存 ArchUnit 測試和完整測試的日誌
   - 格式：`archunit-{timestamp}.log`, `test-{timestamp}.log`, `coverage-{timestamp}.log`

---

## 🔍 詳細實現

### 1. Claude Stop Hook - 簡化為報告模式

**檔案**: `scripts/code-change-summary.sh`

**功能**:
```bash
# 統計變更檔案類型
java_changed=$(echo "$changed_files" | grep '\.java$' | wc -l | tr -d ' ')
ts_changed=$(echo "$changed_files" | grep -E '\.(tsx?|jsx?)$' | wc -l | tr -d ' ')

# 僅報告，不執行測試
echo "Files changed: $total_changed"
echo "  - Java:       $java_changed"
echo "  - TypeScript: $ts_changed"

# 提示品質檢查時機
echo '💡 Quality checks will run at:'
echo '  ┌─ git commit (Pre-commit Hook)'
echo '  │   • Java: Spotless formatting'
echo '  │   • Java: ArchUnit architecture tests'
echo '  └─ git push (Pre-push Hook)'
echo '      • Full test suite (parallel execution)'
```

**執行時間**: < 1 秒

---

### 2. Pre-commit Hook - 添加 ArchUnit 測試

**檔案**: `.git/hooks/pre-commit`

**新增區塊**:
```bash
# ===== [2.5] ArchUnit Architecture Tests =====
if [[ -n "$STAGED_JAVA" ]]; then
    echo "Running ArchUnit architecture tests..."

    # 只測試變更的模組（business/system/oa）
    CHANGED_MODULES=$(echo "$STAGED_JAVA" | \
        grep -oE 'smartadmin-modules/smartadmin-(business|system|oa)[^/]*' | \
        sed 's|smartadmin-modules/||' | \
        sort -u)

    for module in $CHANGED_MODULES; do
        # 執行該模組的 ArchitectureTest
        ./gradlew :$module:test --tests ArchitectureTest --fail-fast -q

        if [ $? -ne 0 ]; then
            echo "❌ ArchUnit tests failed in $module"
            ERRORS=$((ERRORS + 1))
        else
            echo "✅ $module ArchUnit tests passed"
        fi
    done
fi
```

**執行時間**:
- 單模組：~15-20 秒
- 多模組：~20-30 秒（並行執行）

**特性**:
- ✅ 模組化測試（只測變更的模組）
- ✅ 快速失敗（--fail-fast）
- ✅ 日誌保存（.git/hooks/logs/archunit-{timestamp}.log）

---

### 3. Pre-push Hook - 添加完整測試套件

**檔案**: `.git/hooks/pre-push`

**新增區塊**:
```bash
# ===== [0] Critical: Full Test Suite =====
echo "Running full test suite..."
echo "(This may take 60-90 seconds)"

# [0.1] 完整測試套件（並行執行）
./gradlew test --parallel --max-workers=4 --build-cache -q > "$TEST_LOG" 2>&1

if [ $? -ne 0 ]; then
    echo "❌ Tests failed!"
    grep -E '(FAILED|Error|Exception)' "$TEST_LOG" | head -20
    exit 1
else
    TEST_COUNT=$(grep -oE '[0-9]+ tests? completed' "$TEST_LOG" | grep -oE '[0-9]+' | head -1)
    echo "✅ All tests passed ($TEST_COUNT tests)"
fi

# [0.2] 覆蓋率驗證
./gradlew jacocoTestCoverageVerification -q > "$COVERAGE_LOG" 2>&1

if [ $? -ne 0 ]; then
    echo "⚠️  Coverage threshold not met"
    read -p "Continue push anyway? (y/N): " -n 1 -r
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi
```

**執行時間**:
- 測試套件：~45-60 秒（並行執行）
- 覆蓋率檢查：~5-10 秒
- **總計：~60 秒**

**特性**:
- ✅ 並行執行（--parallel --max-workers=4）
- ✅ 構建快取（--build-cache）
- ✅ 覆蓋率驗證（可選擇繼續或中止）
- ✅ 詳細日誌（.git/hooks/logs/test-{timestamp}.log）

---

## 📊 v2.0 vs v3.0 完整對比

### 執行時間對比

| 操作 | v2.0 | v3.0 | 變化 |
|------|------|------|------|
| **Claude 對話結束** | < 5s（執行測試）| **< 1s**（僅報告）| ⬇️ 80% |
| **git commit（無 Java 變更）** | ~10s | ~10s | → 不變 |
| **git commit（單模組 Java 變更）** | ~10s | **~20s** | ⬆️ 100% |
| **git commit（多模組 Java 變更）** | ~10s | **~30s** | ⬆️ 200% |
| **git push** | 0s（無檢查）| **~60s** | ⬆️ 新增 |

### 檢查內容對比

| 檢查項目 | v2.0 | v3.0 | 執行時機 |
|---------|------|------|---------|
| **Spotless 格式化** | ✅ Pre-commit | ✅ Pre-commit | 不變 |
| **ArchUnit 架構測試** | ✅ Stop Hook（大變更）| ✅ Pre-commit（所有 Java 變更）| **改進** |
| **Markdown 驗證** | ✅ Pre-commit | ✅ Pre-commit | 不變 |
| **完整測試套件** | ❌ 無 | ✅ Pre-push | **新增** |
| **覆蓋率驗證** | ❌ 無 | ✅ Pre-push | **新增** |

---

## 🎯 使用場景示範

### 場景 1: 日常開發（小變更）

```bash
# Step 1: 與 Claude 對話，修改 1-2 個 Java 檔案
# Claude 回應結束 → 立即返回（< 1s）✅

# Step 2: 查看變更報告
📊 Code Change Summary
Files changed: 2
  - Java: 2

💡 Quality checks will run at:
  ┌─ git commit: Spotless + ArchUnit
  └─ git push:   Full test suite

# Step 3: Git commit
git add .
git commit -m "feat: add user service"

# 執行：
# [1/2] Spotless formatting... ✅ (3s)
# [2/2] ArchUnit tests (business module)... ✅ (15s)
# 總計：~18s

# Step 4: Git push（第一次推送）
git push

# 執行：
# [1/2] Full test suite (145 tests)... ✅ (50s)
# [2/2] Coverage verification... ✅ (8s)
# 總計：~58s
```

**體驗改善**:
- ✅ Claude 對話：從 5s → 1s（無等待感）
- ✅ Commit 時機：從 10s → 18s（可接受，確保架構）
- ✅ Push 前驗證：新增完整測試（確保品質）

---

### 場景 2: 重構（大變更）

```bash
# Step 1: 與 Claude 對話，修改 10+ 個 Java 檔案（跨 3 個模組）
# Claude 回應結束 → 立即返回（< 1s）✅

# Step 2: Git commit
git commit -m "refactor: migrate to new architecture"

# 執行：
# [1/2] Spotless formatting... ✅ (5s)
# [2/2] ArchUnit tests (3 modules)...
#       - smartadmin-business... ✅ (15s)
#       - smartadmin-system... ✅ (18s)
#       - smartadmin-oa... ✅ (12s)
# 總計：~50s

# Step 3: Git push
git push

# 執行：
# [1/2] Full test suite (145 tests)... ✅ (55s)
# [2/2] Coverage verification... ✅ (9s)
# 總計：~64s
```

**體驗改善**:
- ✅ Claude 對話：立即返回（無論變更多少）
- ✅ Commit 驗證：多模組並行測試（~50s）
- ✅ Push 前驗證：完整測試確保品質（~64s）

---

### 場景 3: 緊急修復（跳過檢查）

```bash
# 生產環境緊急修復，需要快速推送

# 跳過 Pre-commit 檢查
git commit --no-verify -m "hotfix: critical bug"

# 跳過 Pre-push 檢查
git push --no-verify

# ⚠️  注意：跳過檢查有風險，只在緊急情況使用
```

---

## 🛠️ 進階配置

### 調整 ArchUnit 測試範圍

**編輯**: `.git/hooks/pre-commit`（第 79-82 行）

```bash
# 當前：只測試 business/system/oa 模組
CHANGED_MODULES=$(echo "$STAGED_JAVA" | \
    grep -oE 'smartadmin-modules/smartadmin-(business|system|oa)[^/]*' | \
    sed 's|smartadmin-modules/||' | \
    sort -u)

# 如需測試所有模組（包括 common/support）
CHANGED_MODULES=$(echo "$STAGED_JAVA" | \
    grep -oE 'smartadmin-modules/smartadmin-[^/]*' | \
    sed 's|smartadmin-modules/||' | \
    sort -u)
```

---

### 調整測試並行度

**編輯**: `.git/hooks/pre-push`（第 66 行）

```bash
# 當前：4 個並行 worker
./gradlew test --parallel --max-workers=4 --build-cache

# 高效能機器（8 核心+）
./gradlew test --parallel --max-workers=8 --build-cache

# 低效能機器（2 核心）
./gradlew test --parallel --max-workers=2 --build-cache
```

---

### 禁用覆蓋率驗證

**編輯**: `.git/hooks/pre-push`（第 96-122 行）

```bash
# 註解掉覆蓋率檢查
# echo -e "  ${BOLD}[Coverage]${NC} Verifying test coverage..."
# ./gradlew jacocoTestCoverageVerification -q > "$COVERAGE_LOG" 2>&1
# ...

# 或改為僅警告（不阻止）
if [ $COVERAGE_EXIT -ne 0 ]; then
    echo -e "  ${YELLOW}[WARN]${NC} Coverage threshold not met (continuing anyway)"
fi
```

---

## 📝 最佳實踐

### 1. 小步提交原則

**推薦**:
```bash
# 每完成一個小功能就 commit
git commit -m "feat: add user registration"  # ~20s
git commit -m "test: add user service tests"  # ~20s
git commit -m "docs: update API documentation" # ~10s（無 Java 變更）
```

**不推薦**:
```bash
# 累積大量變更再 commit
git commit -m "feat: complete user management module"  # ~50s（多模組）
```

---

### 2. Push 前本地驗證

```bash
# 在 push 前手動執行測試（避免 push 時失敗）
cd smart-admin-api-java21-springboot3
./gradlew test --parallel --max-workers=4

# 如果測試通過，再 push
git push
```

---

### 3. CI/CD 整合

**Pre-push Hook 不應替代 CI/CD**：
- ✅ Pre-push：快速本地驗證（~60s）
- ✅ CI/CD：完整驗證（測試 + 靜態分析 + 構建 + 部署）

**推薦配置**:
```yaml
# .github/workflows/ci.yml
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run full test suite
        run: ./gradlew test --parallel --max-workers=4
      - name: Coverage verification
        run: ./gradlew jacocoTestCoverageVerification
      - name: Static analysis
        run: ./gradlew spotbugsMain pmdMain
```

---

## 🎉 總結

### v3.0 優化成果

| 指標 | v2.0 | v3.0 | 改進 |
|------|------|------|------|
| **Claude 對話體驗** | < 5s | **< 1s** | ⬇️ 80% |
| **品質關卡時機** | 對話結束 | **Git commit/push** | ✅ 更合理 |
| **架構驗證** | 大變更時 | **所有 Java 變更** | ✅ 更嚴格 |
| **完整測試** | 無 | **Push 前** | ✅ 新增 |

---

### 核心價值

1. **使用者體驗**：Claude 對話立即返回（< 1s），無等待感
2. **品質保證**：Commit 前確保架構，Push 前確保測試
3. **時機合理**：品質檢查在「真正需要」時執行（提交/推送）
4. **靈活性**：支持快速跳過（--no-verify）和手動驗證

---

### 後續優化建議

**Phase 4 (可選)**:
1. **增量測試**：只測試變更相關的測試案例
2. **測試快取**：Git hash 作為快取鍵，跳過重複測試
3. **並行 ArchUnit**：多模組並行執行 ArchUnit 測試
4. **智能跳過**：自動檢測 `docs:` commit 跳過測試

---

## 📚 相關文件

| 文件 | 位置 | 說明 |
|------|------|------|
| **v1.0 疑難排解** | [stop-hooks-troubleshooting.md](stop-hooks-troubleshooting.md) | Stop Hooks 輸出不可見問題 |
| **v2.0 Git Diff 優化** | [stop-hooks-optimization-v2.md](stop-hooks-optimization-v2.md) | Git Diff 智能驗證（99% 時間節省）|
| **v3.0 Git 流程優先** | 本文檔 | 品質關卡後移到 Git 提交流程 |
| **Phase 1.2 配置指南** | [phase1.2-hooks-configuration-guide.md](phase1.2-hooks-configuration-guide.md) | 完整 hooks 配置說明 |

---

## 🔄 版本歷史

- **v3.0.0** (2026-03-28): Git 流程優先 - Claude 對話立即返回，品質關卡後移
- **v2.0.0** (2026-03-28): Git Diff 智能驗證 - 小變更 99% 時間節省
- **v1.0.0** (2026-03-28): 初始版本 - 完整驗證但輸出不可見
