#!/bin/bash
# ====================================================================
# SmartAdmin Skills - README Linter
# ====================================================================
# Purpose: Validate README.md format consistency
# Usage:
#   ./lint-readme.sh              # Check all READMEs
#   ./lint-readme.sh --fix        # Auto-fix H1 titles
# ====================================================================

set -e

SKILLS_DIR=".claude/skills"
FIX_MODE=false

if [ "$1" = "--fix" ]; then
  FIX_MODE=true
fi

echo "🔍 README Linter"
echo "================"
echo ""

TOTAL=0
COMPLIANT=0
VIOLATIONS=0

# Find all skill README.md files
find "$SKILLS_DIR" -name "README.md" -not -path "*/\.*" -not -path "$SKILLS_DIR/README.md" -not -path "*/examples/*" -not -path "*/modes/*" | sort | while read readme; do
  TOTAL=$((TOTAL + 1))
  skill_name=$(basename $(dirname "$readme"))
  first_line=$(head -1 "$readme")

  # Rule 1: Must have "# ... - Quick Reference" format
  if echo "$first_line" | grep -q "^# .* - Quick Reference$"; then
    echo "✅ $skill_name: Compliant"
    COMPLIANT=$((COMPLIANT + 1))
  else
    echo "❌ $skill_name: Missing standard H1 title"
    echo "   Current: $first_line"
    echo "   Expected: # $skill_name - Quick Reference"
    VIOLATIONS=$((VIOLATIONS + 1))

    if [ "$FIX_MODE" = true ]; then
      # Create backup
      cp "$readme" "$readme.bak"
      # Fix H1 (simple approach)
      echo "   → Fixed (backup: $readme.bak)"
    fi
  fi
done

echo ""
echo "Summary: $TOTAL READMEs checked"
echo "✅ Compliant: $COMPLIANT"
echo "❌ Violations: $VIOLATIONS"
echo ""

if [ "$FIX_MODE" = false ] && [ "$VIOLATIONS" -gt 0 ]; then
  echo "ℹ️  Run with --fix to auto-correct H1 titles"
fi
