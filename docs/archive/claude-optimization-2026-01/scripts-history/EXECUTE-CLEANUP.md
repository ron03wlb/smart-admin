# Week 1 根層清理 - 執行指南

**狀態**: 📋 準備就緒，等待執行
**風險**: 🟢 低風險（所有文件已備份映射）
**預計時間**: 1-2 分鐘

---

## 方法 1: Windows 批次檔（推薦 - 最簡單）

### 執行步驟

1. **關閉 VSCode**（避免文件鎖定）
   - 保存所有未保存的工作
   - 完全關閉 VSCode（File → Exit）

2. **雙擊批次檔**
   ```
   📍 位置: .claude\scripts\cleanup-root-layer.bat
   ```
   - 在文件管理器中找到該文件
   - 雙擊執行
   - 窗口會顯示進度並在完成後暫停

3. **檢查結果**
   - 窗口會顯示移動統計
   - 預期: Skills moved: 28/28, Files moved: 9/9, etc.

4. **驗證**（見下方"執行後驗證"章節）

---

## 方法 2: PowerShell（如果批次檔失敗）

### 執行步驟

1. **關閉 VSCode**

2. **以管理員身份打開 PowerShell**
   - 按 Win + X
   - 選擇"Windows PowerShell (管理員)"

3. **執行命令**
   ```powershell
   cd C:\Workspace\open_source\smart-admin\.claude\skills

   # 創建備份目錄
   New-Item -ItemType Directory -Force -Path "_deprecated\root-layer-v3"
   New-Item -ItemType Directory -Force -Path "_deprecated\non-skill-items-v3"
   New-Item -ItemType Directory -Force -Path "_deprecated\skill-files-v3"

   # 移動技能目錄（28 個）
   $skills = @(
     "apm-integration-skill", "batch-plan-executor", "cache-strategy-generator",
     "cicd-pipeline-builder", "db-migration-manager", "fraud-detection-pattern-generator",
     "full-text-search-integration", "i18n-generator", "igame-feature-builder",
     "igame-pm-analyst", "igaming-multi-tenant-wallet-pm", "java-performance-pro",
     "liteflow-rule-builder", "message-queue-pattern-generator",
     "quality-gate-orchestrator", "report-generator-skill", "scheduled-task-manager",
     "security-hardening-pro", "smartadmin-api-docs", "smartadmin-crud-generator",
     "smartadmin-integration-test", "smartadmin-mybatis", "smartadmin-performance-suite",
     "smartadmin-testing-suite", "smartadmin-vue-crud", "test-fixture-generator",
     "vavr-refactoring-assistant", "websocket-sse-realtime-generator"
   )

   foreach ($skill in $skills) {
     if (Test-Path $skill) {
       Move-Item $skill "_deprecated\root-layer-v3\$skill" -Force
       Write-Host "√ Moved: $skill" -ForegroundColor Green
     }
   }

   # 移動 .skill 文件（9 個）
   $skillFiles = @(
     "cicd-pipeline-builder.skill", "db-migration-manager.skill",
     "igame-feature-builder.skill", "java-performance-pro.skill",
     "security-hardening-pro.skill", "smartadmin-api-docs.skill",
     "smartadmin-crud-generator.skill", "smartadmin-integration-test.skill",
     "smartadmin-vue-crud.skill"
   )

   foreach ($file in $skillFiles) {
     if (Test-Path $file) {
       Move-Item $file "_deprecated\skill-files-v3\$file" -Force
       Write-Host "√ Moved: $file" -ForegroundColor Green
     }
   }

   # 移動非技能文本文件（27 個）
   $textFiles = @(
     "better-auth-best-practices", "brainstorming", "claude-settings-audit",
     "cloudflare", "design-md", "dispatching-parallel-agents",
     "executing-plans", "file-organizer", "find-bugs",
     "finishing-a-development-branch", "frontend-design", "git-pushing",
     "iterate-pr", "prompt-engineering", "receiving-code-review",
     "requesting-code-review", "review-implementing", "semgrep-rule-creator",
     "skill-creator", "subagent-driven-development", "supabase-postgres-best-practices",
     "test-driven-development", "tinybird", "verification-before-completion",
     "webapp-testing", "writing-plans", "writing-skills"
   )

   foreach ($file in $textFiles) {
     if (Test-Path $file) {
       Move-Item $file "_deprecated\non-skill-items-v3\$file" -Force
       Write-Host "√ Moved: $file" -ForegroundColor Green
     }
   }

   # 移動文檔文件（6 個）
   $docFiles = @(
     "MIGRATION-REPORT-v3.0.0.md", "MONITORING-SYSTEM-DESIGN.md",
     "NEW-SKILLS-OVERVIEW.md", "P1-BASELINE-TEST-SUMMARY.md",
     "SPRINT1-PROGRESS.md", "skill-aliases.json"
   )

   foreach ($file in $docFiles) {
     if (Test-Path $file) {
       Move-Item $file "_deprecated\non-skill-items-v3\$file" -Force
       Write-Host "√ Moved: $file" -ForegroundColor Green
     }
   }

   Write-Host "`n✅ Cleanup completed!" -ForegroundColor Green
   ```

---

## 方法 3: 手動移動（保底方案）

如果以上方法都失敗：

1. **關閉 VSCode**
2. **打開文件管理器**，導航到 `.claude\skills\`
3. **手動拖動文件**到對應的 `_deprecated\` 子目錄：
   - 28 個技能目錄 → `_deprecated\root-layer-v3\`
   - 9 個 .skill 文件 → `_deprecated\skill-files-v3\`
   - 27 個非技能目錄 → `_deprecated\non-skill-items-v3\`
   - 6 個文檔文件 → `_deprecated\non-skill-items-v3\`

---

## 執行後驗證

### 1. 檢查根層只剩合法目錄

在 Git Bash 中執行：

```bash
cd .claude/skills
ls -1 | grep -v -E "^(foundation|extended|productivity|lifecycle|skill-registry\.yml|README\.md|_shared|_deprecated|\.agents|SKILL-INVOCATION-GUIDE\.md)$"
```

**預期結果**: 無輸出（表示根層已清理乾淨）

### 2. 驗證備份完整性

```bash
# 檢查技能目錄數量
ls -1 _deprecated/root-layer-v3/ | wc -l
# 預期: 28

# 檢查 .skill 文件數量
ls -1 _deprecated/skill-files-v3/ | wc -l
# 預期: 9

# 檢查非技能項目數量
ls -1 _deprecated/non-skill-items-v3/ | wc -l
# 預期: 33 (27 個目錄 + 6 個文檔文件)
```

### 3. 驗證分層結構完整

```bash
# 檢查 foundation/
ls foundation/backend foundation/full-stack foundation/testing
# 預期: 顯示各分類下的技能

# 檢查 extended/
ls extended/business-logic extended/domain extended/orchestration extended/quality
# 預期: 顯示各分類下的技能

# 檢查 productivity/
ls productivity/analysis productivity/composite productivity/infrastructure
# 預期: 顯示各分類下的技能
```

---

## Git Commit（驗證通過後）

```bash
cd .claude
git add .
git commit -m "refactor(skills): remove root layer duplicates (v4.0.0 P0 migration)

Week 1 Day 5 completion:
- Moved 28 skill directories to _deprecated/root-layer-v3/
- Moved 9 .skill files to _deprecated/skill-files-v3/
- Moved 27 non-skill items to _deprecated/non-skill-items-v3/
- Moved 6 documentation files to _deprecated/non-skill-items-v3/
- Root layer now clean (only foundation/extended/productivity/lifecycle remain)
- All paths in skill-registry.yml already use hierarchical structure
- No external dependencies affected (verified in CLAUDE.md and 00-INDEX.md)

Breaking Change: Root layer skills removed
Backup Location: .claude/skills/_deprecated/root-layer-v3/
Retention Period: 6 months (until 2026-07-29)
Migration Guide: See .claude/scripts/dependency-detection-report.md

Related: Skills System v4.0.0 Optimization

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## 回滾方案（如遇問題）

如果清理後發現任何問題，可立即回滾：

```bash
# 方案 1: 使用 Git 回滾（推薦）
cd .claude
git reset --hard HEAD~1

# 方案 2: 手動恢復
cd .claude/skills
cp -r _deprecated/root-layer-v3/* .
cp -r _deprecated/skill-files-v3/* .
cp -r _deprecated/non-skill-items-v3/* .
```

---

## 常見問題

### Q: 執行批次檔時出現"拒絕訪問"錯誤
**A**: 確保已完全關閉 VSCode 和所有可能鎖定文件的程序（Git 客戶端、文件管理器預覽等）

### Q: 部分文件移動失敗
**A**: 記下失敗的文件名，手動移動這些文件，然後繼續驗證

### Q: 驗證時發現根層還有文件
**A**: 檢查是否為新添加的文件（不在清理列表中），如果是舊文件，手動移動到對應的 `_deprecated/` 目錄

---

## 下一步

驗證通過並提交 commit 後，立即進入 **Week 2-3: 分類重組**

→ 詳見 [完整優化計劃](.claude/plans/stateful-juggling-sloth.md)

---

**準備好了嗎？** 關閉 VSCode，雙擊 `cleanup-root-layer.bat` 開始清理！ 🚀
