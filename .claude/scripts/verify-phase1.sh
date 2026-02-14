#!/bin/bash
set -e

echo "🔍 Phase 1 Verification"
echo "======================="

# Check 1: README 覆蓋率
# 排除: 根目錄 README, examples/, modes/ 子目錄
README_COUNT=$(find .claude/skills -name "README.md" -not -path "*/\.*" -not -path ".claude/skills/README.md" -not -path "*/examples/*" -not -path "*/modes/*" | wc -l | tr -d ' ')
EXPECTED=33  # 32 原有 + 1 spring-pattern-checker
if [ "$README_COUNT" -eq "$EXPECTED" ]; then
  echo "✅ [1/3] README coverage: 100% ($README_COUNT/$EXPECTED)"
else
  echo "❌ [1/3] README: $README_COUNT/$EXPECTED"
fi

# Check 2: spring skill 遷移
if [ -d ".claude/skills/extended/quality/spring-pattern-checker" ]; then
  echo "✅ [2/3] spring-pattern-checker migrated"
else
  echo "❌ [2/3] Not migrated"
fi

# Check 3: 關鍵字衝突矩陣
if [ -f ".claude/skills/keyword-resolution-matrix.yml" ]; then
  echo "✅ [3/3] keyword-resolution-matrix.yml exists"
else
  echo "❌ [3/3] Missing"
fi

echo ""
echo "Phase 1 verification complete"
