#!/bin/bash

# ====================================================================
# Skills v4.0.0 Structure Validation Script
# ====================================================================
# Purpose: Validate hierarchical directory structure and path consistency
# Created: 2026-01-30
# Updated: 2026-03-08 (v4.0.0 - aligned to 15 skills)
# ====================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "======================================================================"
echo "Skills v4.0.0 Structure Validation"
echo "======================================================================"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

ERRORS=0
WARNINGS=0

# ====================================================================
# Test 1: Directory Structure
# ====================================================================
echo "Test 1: Verifying directory structure..."

REQUIRED_DIRS=(
    "foundation/backend"
    "foundation/frontend"
    "foundation/full-stack"
    "foundation/testing"
    "extended/domain"
    "extended/quality"
)

for dir in "${REQUIRED_DIRS[@]}"; do
    if [ ! -d "$dir" ]; then
        echo -e "${RED}✗${NC} Missing directory: $dir"
        ((ERRORS++))
    else
        echo -e "${GREEN}✓${NC} Found: $dir"
    fi
done

# Check no phantom directories exist
PHANTOM_DIRS=(
    "extended/orchestration"
    "productivity"
    "lifecycle"
)

for dir in "${PHANTOM_DIRS[@]}"; do
    if [ -d "$dir" ]; then
        echo -e "${RED}✗${NC} Phantom directory should not exist: $dir"
        ((ERRORS++))
    else
        echo -e "${GREEN}✓${NC} Confirmed absent: $dir"
    fi
done

echo ""

# ====================================================================
# Test 2: Skill Count Validation
# ====================================================================
echo "Test 2: Validating skill counts..."

FOUNDATION_COUNT=$(find foundation -name "SKILL.md" 2>/dev/null | wc -l | tr -d ' ')
EXTENDED_COUNT=$(find extended -name "SKILL.md" 2>/dev/null | wc -l | tr -d ' ')

echo "Foundation (P0): $FOUNDATION_COUNT skills (expected: 7)"
if [ "$FOUNDATION_COUNT" -eq 7 ]; then
    echo -e "${GREEN}✓${NC} Foundation count correct"
else
    echo -e "${RED}✗${NC} Foundation count mismatch!"
    ((ERRORS++))
fi

echo "Extended (P1): $EXTENDED_COUNT skills (expected: 8)"
if [ "$EXTENDED_COUNT" -eq 8 ]; then
    echo -e "${GREEN}✓${NC} Extended count correct"
else
    echo -e "${RED}✗${NC} Extended count mismatch!"
    ((ERRORS++))
fi

TOTAL_COUNT=$((FOUNDATION_COUNT + EXTENDED_COUNT))
echo ""
echo "Total: $TOTAL_COUNT skills (expected: 15)"
if [ "$TOTAL_COUNT" -eq 15 ]; then
    echo -e "${GREEN}✓${NC} Total count correct"
else
    echo -e "${RED}✗${NC} Total count mismatch!"
    ((ERRORS++))
fi

echo ""

# ====================================================================
# Test 3: skill-registry.yml Path Validation
# ====================================================================
echo "Test 3: Validating skill-registry.yml paths..."

# Extract all paths from skill-registry.yml
REGISTRY_PATHS=$(grep -E "^\s+path: " skill-registry.yml | sed 's/.*path: "\(.*\)".*/\1/')

REGISTRY_COUNT=0
REGISTRY_ERRORS=0
while IFS= read -r path; do
    ((REGISTRY_COUNT++))
    if [ ! -d "$path" ]; then
        echo -e "${RED}✗${NC} Registry path does not exist: $path"
        ((REGISTRY_ERRORS++))
        ((ERRORS++))
    fi
done <<< "$REGISTRY_PATHS"

if [ "$REGISTRY_ERRORS" -eq 0 ]; then
    echo -e "${GREEN}✓${NC} All registry paths exist ($REGISTRY_COUNT/$REGISTRY_COUNT)"
else
    echo -e "${RED}✗${NC} Found $REGISTRY_ERRORS invalid paths"
fi

echo ""

# ====================================================================
# Test 4: Old Rule Path Reference Detection
# ====================================================================
echo "Test 4: Checking for old rule path references..."

OLD_RULE_ERRORS=0

# Check for old numeric-prefix rule references
OLD_PATTERNS=(
    "10-architecture-rules"
    "09-manager-layer"
    "07-dependency-injection"
)

for pattern in "${OLD_PATTERNS[@]}"; do
    MATCHES=$(grep -rl "$pattern" foundation/ extended/ --include="*.md" --include="*.yml" 2>/dev/null | wc -l | tr -d ' ')
    if [ "$MATCHES" -gt 0 ]; then
        echo -e "${RED}✗${NC} Found $MATCHES files referencing old path: $pattern"
        ((OLD_RULE_ERRORS++))
        ((ERRORS++))
    fi
done

# Check 09-mybatis-plus-core (should be D04-mybatis-plus-core)
MYBATIS_MATCHES=$(grep -rl "09-mybatis-plus-core" foundation/ extended/ --include="*.md" --include="*.yml" 2>/dev/null | wc -l | tr -d ' ')
if [ "$MYBATIS_MATCHES" -gt 0 ]; then
    echo -e "${RED}✗${NC} Found $MYBATIS_MATCHES files referencing old path: 09-mybatis-plus-core"
    ((OLD_RULE_ERRORS++))
    ((ERRORS++))
fi

if [ "$OLD_RULE_ERRORS" -eq 0 ]; then
    echo -e "${GREEN}✓${NC} No old rule path references found"
fi

echo ""

# ====================================================================
# Test 5: Phantom Skill Reference Detection
# ====================================================================
echo "Test 5: Checking for phantom skill references in routing files..."

# Only check routing/registry files, not historical documentation
# Historical references (e.g., "Merged from: X", "Former smartadmin-mybatis") are acceptable
PHANTOM_SKILLS="smartadmin-testing-suite|smartadmin-performance-suite|quality-gate-orchestrator|batch-plan-executor|mermaid-repair"

PHANTOM_MATCHES=0
for check_file in skill-registry.yml README.md; do
    if [ -f "$check_file" ]; then
        FILE_MATCHES=$(grep -c -E "$PHANTOM_SKILLS" "$check_file" 2>/dev/null || true)
        FILE_MATCHES=${FILE_MATCHES:-0}
        FILE_MATCHES=$(echo "$FILE_MATCHES" | tr -d '[:space:]')
        if [ "$FILE_MATCHES" -gt 0 ]; then
            echo -e "${RED}✗${NC} Found $FILE_MATCHES phantom references in $check_file"
            ((PHANTOM_MATCHES++))
        fi
    fi
done

if [ "$PHANTOM_MATCHES" -gt 0 ]; then
    ((ERRORS++))
else
    echo -e "${GREEN}✓${NC} No phantom skill references in routing files"
fi

# Info: show count of historical merge references (acceptable)
HISTORICAL_REFS=$(grep -rl -E "smartadmin-mybatis|smartadmin-vue-crud|smartadmin-api-docs|fraud-detection-pattern|igaming-multi-tenant-wallet" foundation/ extended/ --include="*.md" --include="*.yml" 2>/dev/null | wc -l | tr -d ' ')
if [ "$HISTORICAL_REFS" -gt 0 ]; then
    echo -e "${GREEN}✓${NC} Found $HISTORICAL_REFS files with historical merge references (acceptable)"
fi

echo ""

# ====================================================================
# Test 6: Each skill has required files
# ====================================================================
echo "Test 6: Validating skill completeness..."

SKILL_DIRS=$(find foundation extended -name "SKILL.md" -exec dirname {} \;)
INCOMPLETE=0

while IFS= read -r skill_dir; do
    SKILL_NAME=$(basename "$skill_dir")
    if [ ! -f "$skill_dir/config.yml" ]; then
        echo -e "${YELLOW}!${NC} Missing config.yml: $skill_dir"
        ((WARNINGS++))
    fi
    if [ ! -d "$skill_dir/knowledge" ]; then
        echo -e "${YELLOW}!${NC} Missing knowledge/: $skill_dir"
        ((WARNINGS++))
    fi
done <<< "$SKILL_DIRS"

if [ "$WARNINGS" -eq 0 ]; then
    echo -e "${GREEN}✓${NC} All 15 skills have SKILL.md, config.yml, and knowledge/"
else
    echo -e "${YELLOW}!${NC} Some skills are incomplete (see warnings above)"
fi

echo ""

# ====================================================================
# Summary
# ====================================================================
echo "======================================================================"
echo "Validation Summary"
echo "======================================================================"
echo ""

if [ "$ERRORS" -eq 0 ] && [ "$WARNINGS" -eq 0 ]; then
    echo -e "${GREEN}✓ ALL TESTS PASSED${NC}"
    echo ""
    echo "Skills v4.0.0 structure is valid! (15 skills: P0=7, P1=8)"
    exit 0
elif [ "$ERRORS" -eq 0 ]; then
    echo -e "${GREEN}✓ ALL TESTS PASSED${NC} (with $WARNINGS warnings)"
    echo ""
    echo "Skills v4.0.0 structure is valid! (15 skills: P0=7, P1=8)"
    exit 0
else
    echo -e "${RED}✗ VALIDATION FAILED${NC}"
    echo ""
    echo "Errors: $ERRORS"
    echo "Warnings: $WARNINGS"
    echo ""
    echo "Please fix the errors above before proceeding."
    exit 1
fi
