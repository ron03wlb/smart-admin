# SmartAdmin Skills Validation Guide

**Version**: 1.0.0
**Last Updated**: 2026-02-02
**Status**: Production Ready

---

## Overview

SmartAdmin Skills 驗證系統確保 .claude/skills/ 目錄結構和元數據的完整性與一致性。系統包含兩個核心組件：

1. **驗證腳本** (.claude/scripts/validate-skill-consistency.sh) - 獨立運行的驗證工具
2. **Git Pre-commit Hook** (.githooks/pre-commit) - 提交前自動驗證

### 為什麼需要驗證？

**問題場景**:
- 新增技能時忘記更新 skill-registry.yml
- 手動修改元數據文件時出現計數不一致
- 缺少必要的 config.yml 配置文件
- 依賴圖中的技能缺失

**解決方案**:
- 自動檢測所有不一致問題
- 提交前強制驗證（Git hook）
- 詳細的錯誤信息和修復建議

---

## Installation

### Step 1: 驗證腳本權限（Linux/Mac）

chmod +x .claude/scripts/validate-skill-consistency.sh

**Windows 用戶**: 無需此步驟，Bash 會自動處理。

### Step 2: 配置 Git Hooks

git config core.hooksPath .githooks

**驗證配置**:
git config core.hooksPath  # 應輸出: .githooks

### Step 3: Hook 權限（Linux/Mac）

chmod +x .githooks/pre-commit

**安裝完成驗證**:
bash .claude/scripts/validate-skill-consistency.sh

---

## Usage

### 手動運行驗證

**基本用法**:
bash .claude/scripts/validate-skill-consistency.sh

### Git Commit 自動驗證

**觸發條件**:
Pre-commit hook 只在以下文件變更時運行：
- .claude/skills/**/*
- CLAUDE.md
- .claude/META.md
- .claude/skills/VERSIONS.yml

---

## Validation Checks

### Check 1: Skill Count Consistency

**檢查內容**: 驗證技能數量在所有文件中一致

**檢查文件**:
1. 實際文件系統: find .claude/skills -name "SKILL.md" | wc -l
2. skill-registry.yml: total_skills: 35
3. .claude/skills/README.md: **Total Skills**: 35

### Check 2: Registry Completeness

**檢查內容**: 驗證所有 SKILL.md 文件都在 skill-registry.yml 中註冊

### Check 3: Config.yml Coverage

**檢查內容**: 驗證所有技能目錄都有 config.yml 配置文件

### Check 4: Knowledge Coverage Statistics

**檢查內容**: 統計擁有 knowledge/ 目錄的技能比例

**注意**: 這是 WARNING 等級，不會阻塞提交。

### Check 5: Dependency Graph Integrity

**檢查內容**: 驗證 dependency_graph 中的技能數與 active_skills 一致

---

## Test Results

### Test Scenario 1: Skill Count Mismatch
**Status**: PASSED - Error correctly detected

### Test Scenario 2: Registry Missing Entry
**Status**: PASSED - Missing entry correctly detected

### Test Scenario 3: Missing config.yml
**Status**: PASSED - Missing config.yml correctly detected

---

## Troubleshooting

### Issue: Hook Not Running

**Solution**:
git config core.hooksPath .githooks

### Issue: Permission Denied (Linux/Mac)

**Solution**:
chmod +x .githooks/pre-commit
chmod +x .claude/scripts/validate-skill-consistency.sh

---

## Best Practices

### 1. 新增技能時的完整清單

**添加技能時必須**:
1. 創建技能目錄和 SKILL.md
2. 創建 config.yml
3. 在 skill-registry.yml 中添加條目
4. 更新 total_skills 和 active_skills
5. 添加到 dependency_graph 的相應 tier
6. 同步更新 CLAUDE.md、.claude/skills/README.md、VERSIONS.yml
7. 運行驗證: bash .claude/scripts/validate-skill-consistency.sh
8. 提交變更

### 2. 定期驗證

建議每週運行一次完整驗證:
bash .claude/scripts/validate-skill-consistency.sh

---

## FAQ

**Q: 為什麼 Knowledge Coverage 只是 WARNING？**

A: 知識庫建設是漸進式的，不應該阻塞日常開發。計劃在 Phase 1-2 中逐步提升覆蓋率到 100%。

**Q: 可以暫時禁用 hook 嗎？**

A: 可以，但不建議：
git config --unset core.hooksPath  # 禁用
git config core.hooksPath .githooks  # 重新啟用

---

## Related Documents

- Consistency Report (docs/audit/CONSISTENCY-REPORT-2026-02-02.md) - Phase 1 Day 1 修復報告
- Git Hooks README (.githooks/README.md) - Hook 安裝和使用指南
- Skills Registry (.claude/skills/skill-registry.yml) - SSOT 技能註冊表
- Optimization Plan (.claude/plans/delegated-toasting-barto.md) - 完整優化計劃

---

**維護者**: SmartAdmin Team
**版本**: 1.0.0 (Production Ready)
