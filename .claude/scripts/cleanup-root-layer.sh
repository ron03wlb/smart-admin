#!/bin/bash
# ====================================================================
# Root Layer Cleanup Script v1.0.0
# ====================================================================
# Purpose: Backup and remove root-layer skills and non-skill items
# Execution Mode: DRY RUN by default (set DRY_RUN=false to execute)
# Date: 2026-01-29
# ====================================================================

set -e

# Configuration
DRY_RUN="${DRY_RUN:-true}"
SKILLS_DIR="skills"
BACKUP_SKILLS="skills/_deprecated/root-layer-v3"
BACKUP_NON_SKILLS="skills/_deprecated/non-skill-items-v3"
BACKUP_SKILL_FILES="skills/_deprecated/skill-files-v3"
LOG_FILE="scripts/cleanup-root-layer.log"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Initialize log
echo "====================================================================" > "$LOG_FILE"
echo "Root Layer Cleanup - $(date '+%Y-%m-%d %H:%M:%S')" >> "$LOG_FILE"
echo "DRY_RUN: $DRY_RUN" >> "$LOG_FILE"
echo "====================================================================" >> "$LOG_FILE"

log() {
  echo "$1" | tee -a "$LOG_FILE"
}

log_color() {
  echo -e "$1$2${NC}" | tee -a "$LOG_FILE"
}

execute_or_log() {
  local cmd="$1"
  if [ "$DRY_RUN" = "true" ]; then
    log "[DRY RUN] Would execute: $cmd"
  else
    log "[EXECUTING] $cmd"
    eval "$cmd" >> "$LOG_FILE" 2>&1
  fi
}

# ====================================================================
# Phase 1: Create Backup Directories
# ====================================================================
log_color "$YELLOW" "\n### Phase 1: Create Backup Directories ###"

execute_or_log "mkdir -p '$BACKUP_SKILLS'"
execute_or_log "mkdir -p '$BACKUP_NON_SKILLS'"
execute_or_log "mkdir -p '$BACKUP_SKILL_FILES'"

# ====================================================================
# Phase 2: Backup Skill Directories
# ====================================================================
log_color "$YELLOW" "\n### Phase 2: Backup Skill Directories (28 items) ###"

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
  if [ -d "$SKILLS_DIR/$skill" ]; then
    execute_or_log "mv '$SKILLS_DIR/$skill' '$BACKUP_SKILLS/$skill'"
    skill_count=$((skill_count + 1))
    log_color "$GREEN" "  ✓ Moved: $skill"
  else
    log_color "$RED" "  ✗ Not found: $skill"
  fi
done

log "\nSkills moved: $skill_count/${#SKILLS[@]}"

# ====================================================================
# Phase 3: Backup Non-Skill Items
# ====================================================================
log_color "$YELLOW" "\n### Phase 3: Backup Non-Skill Items (27 items) ###"

NON_SKILLS=(
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

non_skill_count=0
for item in "${NON_SKILLS[@]}"; do
  if [ -d "$SKILLS_DIR/$item" ]; then
    execute_or_log "mv '$SKILLS_DIR/$item' '$BACKUP_NON_SKILLS/$item'"
    non_skill_count=$((non_skill_count + 1))
    log_color "$GREEN" "  ✓ Moved: $item"
  else
    log_color "$RED" "  ✗ Not found: $item"
  fi
done

log "\nNon-skill items moved: $non_skill_count/${#NON_SKILLS[@]}"

# ====================================================================
# Phase 4: Backup .skill Files
# ====================================================================
log_color "$YELLOW" "\n### Phase 4: Backup .skill Files (9 items) ###"

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

skill_file_count=0
for file in "${SKILL_FILES[@]}"; do
  if [ -f "$SKILLS_DIR/$file" ]; then
    execute_or_log "mv '$SKILLS_DIR/$file' '$BACKUP_SKILL_FILES/$file'"
    skill_file_count=$((skill_file_count + 1))
    log_color "$GREEN" "  ✓ Moved: $file"
  else
    log_color "$RED" "  ✗ Not found: $file"
  fi
done

log "\n.skill files moved: $skill_file_count/${#SKILL_FILES[@]}"

# ====================================================================
# Phase 5: Create README in Backup Directories
# ====================================================================
log_color "$YELLOW" "\n### Phase 5: Create README Files ###"

cat > "$BACKUP_SKILLS/README.md" <<'EOF'
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
cp -r _deprecated/root-layer-v3/{skill-name} .claude/skills/
```

## 警告

⚠️ 這些是舊版本的重複副本。正式版本已在分層結構中：
- foundation/
- extended/
- productivity/
- lifecycle/

請勿直接使用此備份中的文件。
EOF

if [ "$DRY_RUN" = "false" ]; then
  log_color "$GREEN" "  ✓ Created: $BACKUP_SKILLS/README.md"
fi

cat > "$BACKUP_NON_SKILLS/README.md" <<'EOF'
# Non-Skill Items Archive (v3.0.0)

這些目錄不應在 .claude/skills/ 中，已於 v4.0.0 遷移時歸檔。

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

if [ "$DRY_RUN" = "false" ]; then
  log_color "$GREEN" "  ✓ Created: $BACKUP_NON_SKILLS/README.md"
fi

# ====================================================================
# Phase 6: Verification
# ====================================================================
log_color "$YELLOW" "\n### Phase 6: Verification ###"

log "\n驗證剩餘的根層目錄："
if [ "$DRY_RUN" = "false" ]; then
  remaining=$(ls -1 "$SKILLS_DIR" | grep -v -E "^(foundation|extended|productivity|lifecycle|skill-registry\.yml|README\.md|_shared|_deprecated|\.agents|SKILL-INVOCATION-GUIDE\.md)$" || true)
  if [ -z "$remaining" ]; then
    log_color "$GREEN" "  ✓ 根層已清理乾淨！"
  else
    log_color "$RED" "  ✗ 仍有剩餘項目："
    echo "$remaining" | while read item; do
      log_color "$RED" "    - $item"
    done
  fi
else
  log "  (Skipped in DRY RUN mode)"
fi

# ====================================================================
# Summary
# ====================================================================
log_color "$YELLOW" "\n### Cleanup Summary ###"
log "Skill directories:     $skill_count/${#SKILLS[@]} moved"
log "Non-skill items:       $non_skill_count/${#NON_SKILLS[@]} moved"
log ".skill files:          $skill_file_count/${#SKILL_FILES[@]} moved"
log ""
log "Backup locations:"
log "  - Skills:            $BACKUP_SKILLS"
log "  - Non-skills:        $BACKUP_NON_SKILLS"
log "  - .skill files:      $BACKUP_SKILL_FILES"
log ""
log "Log file:             $LOG_FILE"

if [ "$DRY_RUN" = "true" ]; then
  log_color "$YELLOW" "\n⚠️  DRY RUN MODE - No actual changes made"
  log_color "$YELLOW" "To execute cleanup, run:"
  log_color "$YELLOW" "  DRY_RUN=false bash .claude/scripts/cleanup-root-layer.sh"
else
  log_color "$GREEN" "\n✅ Cleanup completed successfully!"
fi

echo ""
