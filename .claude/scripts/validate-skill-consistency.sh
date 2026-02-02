#!/bin/bash
# ====================================================================
# SmartAdmin Skills Consistency Validator
# ====================================================================
# Purpose: Validate consistency between filesystem and skill-registry.yml
# Usage: bash .claude/scripts/validate-skill-consistency.sh
# Exit Codes:
#   0 - All checks passed
#   1 - Validation errors found
# ====================================================================

set -e

echo "🔍 Validating SmartAdmin Skills Consistency..."
echo ""

# ====================================================================
# Check 1: Skill Count Consistency
# ====================================================================
echo "📊 Check 1: Skill Count Consistency"

# Calculate actual skill count (exclude .agents/)
actual_count=$(find .claude/skills -name "SKILL.md" -not -path "*/.agents/*" 2>/dev/null | wc -l)

# Extract skill count from registry
registry_count=$(grep "^total_skills:" .claude/skills/skill-registry.yml | awk '{print $2}')

# Extract skill count from README
readme_count=$(grep "^\*\*Total Skills\*\*:" .claude/skills/README.md | awk '{print $3}')

# Compare counts
if [ "$actual_count" != "$registry_count" ]; then
    echo "❌ ERROR: Skill count mismatch!"
    echo "   Actual (filesystem):  $actual_count"
    echo "   Registry (YAML):      $registry_count"
    echo "   README (MD):          $readme_count"
    echo ""
    echo "🔧 Fix: Update .claude/skills/skill-registry.yml total_skills to $actual_count"
    exit 1
fi

if [ "$actual_count" != "$readme_count" ]; then
    echo "❌ ERROR: README skill count mismatch!"
    echo "   Actual (filesystem):  $actual_count"
    echo "   README (MD):          $readme_count"
    echo ""
    echo "🔧 Fix: Update .claude/skills/README.md Total Skills to $actual_count"
    exit 1
fi

echo "✅ Skill count consistent: $actual_count"
echo ""

# ====================================================================
# Check 2: Registry Completeness
# ====================================================================
echo "📊 Check 2: Registry Completeness"

missing_skills=()
while IFS= read -r skill_path; do
    skill_name=$(dirname "$skill_path" | xargs basename)
    if ! grep -q "^  $skill_name:" .claude/skills/skill-registry.yml; then
        missing_skills+=("$skill_name")
    fi
done < <(find .claude/skills -name "SKILL.md" -not -path "*/.agents/*" 2>/dev/null)

if [ ${#missing_skills[@]} -gt 0 ]; then
    echo "❌ ERROR: ${#missing_skills[@]} skill(s) not found in registry:"
    for skill in "${missing_skills[@]}"; do
        echo "   - $skill"
    done
    echo ""
    echo "🔧 Fix: Add missing skills to .claude/skills/skill-registry.yml"
    exit 1
fi

echo "✅ All skills registered in skill-registry.yml"
echo ""

# ====================================================================
# Check 3: Config.yml Coverage
# ====================================================================
echo "📊 Check 3: Config.yml Coverage"

missing_configs=()
while IFS= read -r skill_path; do
    config_path="${skill_path%SKILL.md}config.yml"
    if [ ! -f "$config_path" ]; then
        skill_name=$(dirname "$skill_path" | xargs basename)
        missing_configs+=("$skill_name")
    fi
done < <(find .claude/skills -name "SKILL.md" -not -path "*/.agents/*" 2>/dev/null)

if [ ${#missing_configs[@]} -gt 0 ]; then
    echo "❌ ERROR: ${#missing_configs[@]} skill(s) missing config.yml:"
    for skill in "${missing_configs[@]}"; do
        echo "   - $skill"
    done
    echo ""
    echo "🔧 Fix: Create config.yml for each missing skill"
    exit 1
fi

echo "✅ All skills have config.yml"
echo ""

# ====================================================================
# Check 4: Knowledge Coverage Statistics
# ====================================================================
echo "📊 Check 4: Knowledge Coverage Statistics"

knowledge_count=$(find .claude/skills -type d -name "knowledge" -not -path "*/.agents/*" 2>/dev/null | wc -l)
coverage=$((knowledge_count * 100 / actual_count))

echo "📈 Knowledge Directory Coverage: $coverage% ($knowledge_count/$actual_count)"

if [ $coverage -lt 50 ]; then
    echo "⚠️  WARNING: Knowledge coverage is below 50%"
    echo "   Recommendation: Add knowledge/ directories for uncovered skills"
fi

if [ $coverage -ge 80 ]; then
    echo "🎉 Excellent! Knowledge coverage is above 80%"
fi

echo ""

# ====================================================================
# Check 5: Dependency Graph Integrity (Optional)
# ====================================================================
echo "📊 Check 5: Dependency Graph Integrity"

# Check if all skills in registry are in dependency_graph
skills_in_registry=$(grep "^  [a-z]" .claude/skills/skill-registry.yml | grep -v "^  #" | awk -F: '{print $1}' | sed 's/^  //' | grep -v "^tier" | grep -v "^depended" | grep -v "^depends" | wc -l)

# Count skills in dependency_graph (tier_0 + tier_1 + tier_2 + tier_3)
skills_in_graph=$(grep -A 100 "^  tier_0:" .claude/skills/skill-registry.yml | grep "^    -" | wc -l)
skills_in_graph=$((skills_in_graph + $(grep -A 100 "^  tier_1:" .claude/skills/skill-registry.yml | grep "^    -" | wc -l)))
skills_in_graph=$((skills_in_graph + $(grep -A 100 "^  tier_2:" .claude/skills/skill-registry.yml | grep "^    -" | wc -l)))
skills_in_graph=$((skills_in_graph + $(grep -A 100 "^  tier_3:" .claude/skills/skill-registry.yml | grep "^    -" | wc -l)))

# Note: Some skills in registry might be in categories section, so we check active_skills instead
active_skills=$(grep "^active_skills:" .claude/skills/skill-registry.yml | awk '{print $2}')

if [ "$skills_in_graph" -eq "$active_skills" ]; then
    echo "✅ Dependency graph complete ($skills_in_graph/$active_skills active skills)"
else
    echo "⚠️  WARNING: Dependency graph may be incomplete"
    echo "   Skills in graph:  $skills_in_graph"
    echo "   Active skills:    $active_skills"
fi

echo ""

# ====================================================================
# Summary
# ====================================================================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ All consistency checks passed!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📊 Summary Statistics:"
echo "   Total Skills:           $actual_count"
echo "   Active Skills:          $active_skills"
echo "   Deprecated Skills:      3"
echo "   Knowledge Coverage:     $coverage% ($knowledge_count/$actual_count)"
echo ""
echo "📅 Last Validated: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""
