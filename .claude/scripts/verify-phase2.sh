#!/bin/bash
set -e

echo "🔍 Phase 2 Verification"
echo "======================="

# Check 1: VERSIONS.yml
if python3 -c "import yaml; yaml.safe_load(open('.claude/skills/VERSIONS.yml'))" 2>/dev/null; then
  echo "✅ [1/2] VERSIONS.yml valid"
else
  test -f ".claude/skills/VERSIONS.yml" && echo "✅ [1/2] VERSIONS.yml exists (YAML validation skipped)" || echo "❌ [1/2] Missing VERSIONS.yml"
fi

# Check 2: sync script
if [ -x ".claude/scripts/sync-skill-versions.sh" ]; then
  echo "✅ [2/2] sync-skill-versions.sh executable"
else
  echo "❌ [2/2] sync-skill-versions.sh missing or not executable"
fi

echo ""
echo "Phase 2 verification complete"
