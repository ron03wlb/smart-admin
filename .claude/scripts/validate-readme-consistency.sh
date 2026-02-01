#!/bin/bash
# ====================================================================
# SmartAdmin README Consistency Validator
# ====================================================================
# Purpose: Validate skill count consistency across documentation files
# Version: 1.0.0
# Created: 2026-02-01
# ====================================================================

set -e

echo "=== README 一致性檢查 ==="
echo ""

EXIT_CODE=0

# ====================================================================
# 1. Skill 計數驗證
# ====================================================================
echo "1. 檢查 Skill 計數一致性..."

# 計算實際 skill 數量
TOTAL_ACTUAL=$(find .claude/skills -name SKILL.md | wc -l | tr -d ' ')

# P0 (Foundation)
P0_ACTUAL=$(find .claude/skills/foundation -name SKILL.md 2>/dev/null | wc -l | tr -d ' ')

# P1 (Extended)
P1_ACTUAL=$(find .claude/skills/extended -name SKILL.md 2>/dev/null | wc -l | tr -d ' ')

# P2 (Productivity)
P2_ACTUAL=$(find .claude/skills/productivity -name SKILL.md 2>/dev/null | wc -l | tr -d ' ')

# Deprecated (Lifecycle)
DEPRECATED_ACTUAL=$(find .claude/skills/lifecycle/deprecated -name SKILL.md 2>/dev/null | wc -l | tr -d ' ')

echo "  實際計數:"
echo "    Total: $TOTAL_ACTUAL"
echo "    P0: $P0_ACTUAL"
echo "    P1: $P1_ACTUAL"
echo "    P2: $P2_ACTUAL"
echo "    Deprecated: $DEPRECATED_ACTUAL"
echo ""

# 檢查 skills/README.md
if [ -f ".claude/skills/README.md" ]; then
  README_TOTAL=$(grep -o 'Total Skills: [0-9]*' .claude/skills/README.md 2>/dev/null | grep -o '[0-9]*' | head -1 || echo "0")

  if [ "$README_TOTAL" -ne "$TOTAL_ACTUAL" ]; then
    echo "  ❌ skills/README.md: Total mismatch (declared=$README_TOTAL, actual=$TOTAL_ACTUAL)"
    EXIT_CODE=1
  else
    echo "  ✅ skills/README.md: Total correct ($TOTAL_ACTUAL)"
  fi
fi

# 檢查 META.md
if [ -f ".claude/META.md" ]; then
  META_COUNT=$(grep -o '[0-9]* specialized skills' .claude/META.md | grep -o '^[0-9]*' | head -1)

  if [ "$META_COUNT" -ne "$TOTAL_ACTUAL" ]; then
    echo "  ❌ META.md: Total mismatch (declared=$META_COUNT, actual=$TOTAL_ACTUAL)"
    EXIT_CODE=1
  else
    echo "  ✅ META.md: Total correct ($TOTAL_ACTUAL)"
  fi
fi

# 檢查 skill-registry.yml
if [ -f ".claude/skills/skill-registry.yml" ]; then
  REGISTRY_TOTAL=$(grep -c "^  [a-z-]*:$" .claude/skills/skill-registry.yml 2>/dev/null || echo "0")

  # 減去 3 個頂層鍵（foundation, extended, productivity, lifecycle 等）
  REGISTRY_SKILLS=$((REGISTRY_TOTAL - 10))

  if [ "$REGISTRY_SKILLS" -ne "$TOTAL_ACTUAL" ]; then
    echo "  ⚠️  skill-registry.yml: Skill count may differ (registry=$REGISTRY_SKILLS, actual=$TOTAL_ACTUAL)"
    # Warning only, not error
  else
    echo "  ✅ skill-registry.yml: Skill count matches ($TOTAL_ACTUAL)"
  fi
fi

echo ""

# ====================================================================
# 2. Skill 名稱一致性檢查
# ====================================================================
echo "2. 檢查 Skill 名稱一致性（目錄名 vs config.yml）..."

NAME_MISMATCH=0

for skill_dir in .claude/skills/*/*/ .claude/skills/*/*/*/; do
  if [ -f "$skill_dir/config.yml" ]; then
    DIR_NAME=$(basename "$skill_dir")

    # 提取 config.yml 中的 name (支持兩種格式)
    CONFIG_NAME=$(grep -A 10 "^metadata:" "$skill_dir/config.yml" 2>/dev/null | grep "^  name:" | sed 's/^  name: *//; s/"//g; s/'\''//g' | head -1)

    if [ -z "$CONFIG_NAME" ]; then
      # 嘗試舊格式 skill_metadata
      CONFIG_NAME=$(grep -A 10 "^skill_metadata:" "$skill_dir/config.yml" 2>/dev/null | grep "^  name:" | sed 's/^  name: *//; s/"//g; s/'\''//g' | head -1)
    fi

    if [ -n "$CONFIG_NAME" ] && [ "$DIR_NAME" != "$CONFIG_NAME" ]; then
      echo "  ❌ Name mismatch: directory='$DIR_NAME', config='$CONFIG_NAME'"
      NAME_MISMATCH=$((NAME_MISMATCH + 1))
      EXIT_CODE=1
    fi
  fi
done

if [ $NAME_MISMATCH -eq 0 ]; then
  echo "  ✅ All skill names consistent"
else
  echo "  ❌ Found $NAME_MISMATCH name mismatches"
fi

echo ""

# ====================================================================
# 3. VERSIONS.yml 同步檢查
# ====================================================================
echo "3. 檢查 VERSIONS.yml 與實際 skills 同步..."

if [ -f ".claude/skills/VERSIONS.yml" ]; then
  # 計算 VERSIONS.yml 中的 skill 數量
  VERSIONS_COUNT=$(grep -c "^    [a-z-]*:" .claude/skills/VERSIONS.yml 2>/dev/null || echo "0")

  if [ "$VERSIONS_COUNT" -ne "$TOTAL_ACTUAL" ]; then
    echo "  ⚠️  VERSIONS.yml: Skill count may differ (versions=$VERSIONS_COUNT, actual=$TOTAL_ACTUAL)"
  else
    echo "  ✅ VERSIONS.yml: Skill count matches ($TOTAL_ACTUAL)"
  fi
else
  echo "  ⚠️  VERSIONS.yml not found"
fi

echo ""

# ====================================================================
# 4. Examples 覆蓋率檢查
# ====================================================================
echo "4. 檢查 Examples 覆蓋率..."

SKILLS_WITH_EXAMPLES=$(find .claude/skills -type d -name examples | wc -l | tr -d ' ')
COVERAGE=$(awk "BEGIN {printf \"%.0f\", ($SKILLS_WITH_EXAMPLES/$TOTAL_ACTUAL)*100}")

echo "  Examples 覆蓋率: $SKILLS_WITH_EXAMPLES/$TOTAL_ACTUAL ($COVERAGE%)"

if [ "$COVERAGE" -lt 50 ]; then
  echo "  ⚠️  Coverage below target (50%)"
else
  echo "  ✅ Coverage meets target (≥50%)"
fi

echo ""

# ====================================================================
# 結果摘要
# ====================================================================
echo "=== 檢查結果摘要 ==="
echo ""

if [ $EXIT_CODE -eq 0 ]; then
  echo "✅ All checks passed!"
  echo ""
  echo "Summary:"
  echo "- Total Skills: $TOTAL_ACTUAL"
  echo "  - P0 (Foundation): $P0_ACTUAL"
  echo "  - P1 (Extended): $P1_ACTUAL"
  echo "  - P2 (Productivity): $P2_ACTUAL"
  echo "  - Deprecated: $DEPRECATED_ACTUAL"
  echo "- Examples Coverage: $COVERAGE%"
  echo "- Documentation: Consistent"
else
  echo "❌ Some checks failed (see above for details)"
  echo ""
  echo "Please fix the reported issues before committing."
fi

echo ""
exit $EXIT_CODE
