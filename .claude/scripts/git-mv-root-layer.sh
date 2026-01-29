#!/bin/bash
# ====================================================================
# Git-based Root Layer Cleanup v1.0.0
# ====================================================================
# Purpose: Use git mv to move root-layer skills (preserves Git history)
# Date: 2026-01-29
# ====================================================================

set -e

cd .claude/skills

# Create backup directories
mkdir -p _deprecated/root-layer-v3
mkdir -p _deprecated/non-skill-items-v3
mkdir -p _deprecated/skill-files-v3

echo "=== Moving Skill Directories (28 items) ==="

# Skills to move
SKILLS=(
  "apm-integration-skill"
  "batch-plan-executor"
  "cache-strategy-generator"
  "cicd-pipeline-builder"
  "db-migration-manager"
  "fraud-detection-pattern-generator"
  "full-text-search-integration"
  "i18n-generator"
  "igame-feature-builder"
  "igame-pm-analyst"
  "igaming-multi-tenant-wallet-pm"
  "java-performance-pro"
  "liteflow-rule-builder"
  "message-queue-pattern-generator"
  "quality-gate-orchestrator"
  "report-generator-skill"
  "scheduled-task-manager"
  "security-hardening-pro"
  "smartadmin-api-docs"
  "smartadmin-crud-generator"
  "smartadmin-integration-test"
  "smartadmin-mybatis"
  "smartadmin-performance-suite"
  "smartadmin-testing-suite"
  "smartadmin-vue-crud"
  "test-fixture-generator"
  "vavr-refactoring-assistant"
  "websocket-sse-realtime-generator"
)

skill_count=0
for skill in "${SKILLS[@]}"; do
  if [ -d "$skill" ]; then
    git mv "$skill" "_deprecated/root-layer-v3/$skill" 2>/dev/null || mv "$skill" "_deprecated/root-layer-v3/$skill"
    echo "  ✓ Moved: $skill"
    skill_count=$((skill_count + 1))
  fi
done

echo "Skills moved: $skill_count/${#SKILLS[@]}"

echo ""
echo "=== Moving .skill Files (9 items) ==="

# .skill files to move
SKILL_FILES=(
  "cicd-pipeline-builder.skill"
  "db-migration-manager.skill"
  "igame-feature-builder.skill"
  "java-performance-pro.skill"
  "security-hardening-pro.skill"
  "smartadmin-api-docs.skill"
  "smartadmin-crud-generator.skill"
  "smartadmin-integration-test.skill"
  "smartadmin-vue-crud.skill"
)

file_count=0
for file in "${SKILL_FILES[@]}"; do
  if [ -f "$file" ]; then
    git mv "$file" "_deprecated/skill-files-v3/$file" 2>/dev/null || mv "$file" "_deprecated/skill-files-v3/$file"
    echo "  ✓ Moved: $file"
    file_count=$((file_count + 1))
  fi
done

echo ".skill files moved: $file_count/${#SKILL_FILES[@]}"

echo ""
echo "=== Moving Non-Skill Text Files ==="

# Non-skill text files
TEXT_FILES=(
  "better-auth-best-practices"
  "brainstorming"
  "claude-settings-audit"
  "cloudflare"
  "design-md"
  "dispatching-parallel-agents"
  "executing-plans"
  "file-organizer"
  "find-bugs"
  "finishing-a-development-branch"
  "frontend-design"
  "git-pushing"
  "iterate-pr"
  "prompt-engineering"
  "receiving-code-review"
  "requesting-code-review"
  "review-implementing"
  "semgrep-rule-creator"
  "skill-creator"
  "subagent-driven-development"
  "supabase-postgres-best-practices"
  "test-driven-development"
  "tinybird"
  "verification-before-completion"
  "webapp-testing"
  "writing-plans"
  "writing-skills"
)

text_count=0
for file in "${TEXT_FILES[@]}"; do
  if [ -f "$file" ]; then
    git mv "$file" "_deprecated/non-skill-items-v3/$file" 2>/dev/null || mv "$file" "_deprecated/non-skill-items-v3/$file"
    echo "  ✓ Moved: $file"
    text_count=$((text_count + 1))
  fi
done

echo "Text files moved: $text_count/${#TEXT_FILES[@]}"

echo ""
echo "=== Moving Documentation Files ==="

DOC_FILES=(
  "MIGRATION-REPORT-v3.0.0.md"
  "MONITORING-SYSTEM-DESIGN.md"
  "NEW-SKILLS-OVERVIEW.md"
  "P1-BASELINE-TEST-SUMMARY.md"
  "SPRINT1-PROGRESS.md"
  "skill-aliases.json"
)

doc_count=0
for file in "${DOC_FILES[@]}"; do
  if [ -f "$file" ]; then
    git mv "$file" "_deprecated/non-skill-items-v3/$file" 2>/dev/null || mv "$file" "_deprecated/non-skill-items-v3/$file"
    echo "  ✓ Moved: $file"
    doc_count=$((doc_count + 1))
  fi
done

echo "Documentation files moved: $doc_count/${#DOC_FILES[@]}"

# Create README files
cat > _deprecated/root-layer-v3/README.md <<'EOF'
# Root Layer Archive (v3.0.0)

已於 v4.0.0 遷移至分層結構。

## 遷移信息

- **遷移映射表**: `../../scripts/root-to-hierarchical-mapping.json`
- **遷移日期**: 2026-01-29
- **保留期限**: 2026-07-29 (6 個月後刪除)
- **備份原因**: v4.0.0 根層清理，所有技能已遷移至分層結構

## 還原方法

如需還原任何技能：

```bash
cp -r .claude/skills/_deprecated/root-layer-v3/{skill-name} .claude/skills/
```

## 警告

⚠️ 這些是舊版本的重複副本。正式版本已在分層結構中：
- foundation/
- extended/
- productivity/
- lifecycle/

請勿直接使用此備份中的文件。
EOF

cat > _deprecated/non-skill-items-v3/README.md <<'EOF'
# Non-Skill Items Archive (v3.0.0)

這些項目不應在 .claude/skills/ 中，已於 v4.0.0 遷移時歸檔。

## 內容類別

- **開發工作流**: dispatching-parallel-agents, git-pushing, etc.
- **測試/設計**: test-driven-development, design-md, etc.
- **技能開發**: skill-creator, writing-skills, etc.
- **特定技術**: better-auth-best-practices, cloudflare, etc.

## 建議歸檔位置

- 工作流相關: `.claude/docs/workflows/`
- 技能開發: `.claude/docs/skill-development/`
- 技術文檔: `docs/technical/`

## 保留期限

2026-07-29 (6 個月後評估是否永久刪除或遷移到正確位置)
EOF

echo ""
echo "✅ Cleanup completed!"
echo ""
echo "Summary:"
echo "  - Skill directories: $skill_count moved"
echo "  - .skill files: $file_count moved"
echo "  - Text files: $text_count moved"
echo "  - Documentation files: $doc_count moved"
echo ""
echo "Next: Verify and commit changes"
