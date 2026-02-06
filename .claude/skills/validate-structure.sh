#!/bin/bash

# ====================================================================
# Skills v3.0.0 Structure Validation Script
# ====================================================================
# Purpose: Validate hierarchical directory structure and path consistency
# Created: 2026-01-30
# ====================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "======================================================================"
echo "Skills v3.0.0 Structure Validation"
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
    "foundation/full-stack"
    "foundation/testing"
    "extended/domain"
    "extended/orchestration"
    "extended/quality"
    "productivity/devops"
    "productivity/integration"
    "productivity/composite"
    "productivity/analysis"
    "productivity/refactoring"
    "lifecycle/deprecated"
)

for dir in "${REQUIRED_DIRS[@]}"; do
    if [ ! -d "$dir" ]; then
        echo -e "${RED}✗${NC} Missing directory: $dir"
        ((ERRORS++))
    else
        echo -e "${GREEN}✓${NC} Found: $dir"
    fi
done

echo ""

# ====================================================================
# Test 2: Skill Count Validation
# ====================================================================
echo "Test 2: Validating skill counts..."

FOUNDATION_COUNT=$(find foundation -name "SKILL.md" 2>/dev/null | wc -l | tr -d ' ')
EXTENDED_COUNT=$(find extended -name "SKILL.md" 2>/dev/null | wc -l | tr -d ' ')
PRODUCTIVITY_COUNT=$(find productivity -name "SKILL.md" 2>/dev/null | wc -l | tr -d ' ')
DEPRECATED_COUNT=$(find lifecycle/deprecated -name "SKILL.md" 2>/dev/null | wc -l | tr -d ' ')

echo "Foundation (P0): $FOUNDATION_COUNT skills (expected: 6)"
if [ "$FOUNDATION_COUNT" -eq 6 ]; then
    echo -e "${GREEN}✓${NC} Foundation count correct"
else
    echo -e "${RED}✗${NC} Foundation count mismatch!"
    ((ERRORS++))
fi

echo "Extended (P1): $EXTENDED_COUNT skills (expected: 10)"
if [ "$EXTENDED_COUNT" -eq 10 ]; then
    echo -e "${GREEN}✓${NC} Extended count correct"
else
    echo -e "${RED}✗${NC} Extended count mismatch!"
    ((ERRORS++))
fi

echo "Productivity (P2): $PRODUCTIVITY_COUNT skills (expected: 16)"
if [ "$PRODUCTIVITY_COUNT" -eq 16 ]; then
    echo -e "${GREEN}✓${NC} Productivity count correct"
else
    echo -e "${RED}✗${NC} Productivity count mismatch!"
    ((ERRORS++))
fi

echo "Deprecated: $DEPRECATED_COUNT skills (expected: 4)"
if [ "$DEPRECATED_COUNT" -eq 4 ]; then
    echo -e "${GREEN}✓${NC} Deprecated count correct"
else
    echo -e "${RED}✗${NC} Deprecated count mismatch!"
    ((ERRORS++))
fi

TOTAL_COUNT=$((FOUNDATION_COUNT + EXTENDED_COUNT + PRODUCTIVITY_COUNT + DEPRECATED_COUNT))
echo ""
echo "Total: $TOTAL_COUNT skills (expected: 36)"
if [ "$TOTAL_COUNT" -eq 36 ]; then
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

REGISTRY_ERRORS=0
while IFS= read -r path; do
    if [ ! -d "$path" ]; then
        echo -e "${RED}✗${NC} Registry path does not exist: $path"
        ((REGISTRY_ERRORS++))
        ((ERRORS++))
    fi
done <<< "$REGISTRY_PATHS"

if [ "$REGISTRY_ERRORS" -eq 0 ]; then
    echo -e "${GREEN}✓${NC} All registry paths exist (36/36)"
else
    echo -e "${RED}✗${NC} Found $REGISTRY_ERRORS invalid paths"
fi

echo ""

# ====================================================================
# Test 4: Old Path Detection
# ====================================================================
echo "Test 4: Checking for old path references..."

OLD_PATHS=$(grep -E "^\s+path: \"(backend|full-stack|testing|domain|orchestration|quality|devops|integration|composite|analysis|refactoring|_deprecated)/" skill-registry.yml || true)

if [ -z "$OLD_PATHS" ]; then
    echo -e "${GREEN}✓${NC} No old paths found in skill-registry.yml"
else
    echo -e "${RED}✗${NC} Found old paths:"
    echo "$OLD_PATHS"
    ((ERRORS++))
fi

echo ""

# ====================================================================
# Test 5: Old Directory Detection
# ====================================================================
echo "Test 5: Checking for old directories..."

OLD_DIRS=()
for dir in backend full-stack testing domain orchestration quality devops integration composite analysis refactoring _deprecated; do
    if [ -d "$dir" ]; then
        OLD_DIRS+=("$dir")
    fi
done

if [ ${#OLD_DIRS[@]} -eq 0 ]; then
    echo -e "${GREEN}✓${NC} No old directories found"
else
    echo -e "${RED}✗${NC} Found old directories:"
    for dir in "${OLD_DIRS[@]}"; do
        echo "  - $dir"
    done
    ((ERRORS++))
fi

echo ""

# ====================================================================
# Summary
# ====================================================================
echo "======================================================================"
echo "Validation Summary"
echo "======================================================================"
echo ""

if [ "$ERRORS" -eq 0 ]; then
    echo -e "${GREEN}✓ ALL TESTS PASSED${NC}"
    echo ""
    echo "Skills v3.0.0 hierarchical migration is valid!"
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
