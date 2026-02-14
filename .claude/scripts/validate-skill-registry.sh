#!/bin/bash

# ====================================================================
# Skills Registry Validation Script v1.0.0
# ====================================================================
# Purpose: Validate skill registry consistency across .claude/ and .agent/
# Usage: .claude/scripts/validate-skill-registry.sh [--verbose]
# Created: 2026-02-07
# ====================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SKILLS_DIR="$(dirname "$SCRIPT_DIR")/skills"
AGENT_SKILLS_DIR="$(dirname "$(dirname "$SCRIPT_DIR")")/.agent/skills"

VERBOSE=false
if [[ "$1" == "--verbose" ]]; then
    VERBOSE=true
fi

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

ERRORS=0
WARNINGS=0

echo "======================================================================"
echo "Skills Registry Validation v1.0.0"
echo "======================================================================"
echo ""

# ====================================================================
# Test 1: Verify all skills in registry have physical directories
# ====================================================================
echo "Test 1: Validating skill paths in registry..."

REGISTRY_SKILLS=$(grep -E "^  [a-z]+-" "$SKILLS_DIR/skill-registry.yml" | sed 's/://g' | sed 's/^[[:space:]]*//' | head -50)
MISSING_DIRS=0

while IFS= read -r skill; do
    SKILL_PATH=$(grep -A5 "^  $skill:" "$SKILLS_DIR/skill-registry.yml" | grep "path:" | head -1 | sed 's/.*path: "\(.*\)".*/\1/' || echo "")
    if [[ -n "$SKILL_PATH" && ! -d "$SKILLS_DIR/$SKILL_PATH" ]]; then
        echo -e "${RED}✗${NC} Missing directory: $SKILL_PATH"
        ((MISSING_DIRS++))
        ((ERRORS++))
    elif [[ "$VERBOSE" == true && -n "$SKILL_PATH" ]]; then
        echo -e "${GREEN}✓${NC} Found: $SKILL_PATH"
    fi
done <<< "$REGISTRY_SKILLS"

if [[ "$MISSING_DIRS" -eq 0 ]]; then
    echo -e "${GREEN}✓${NC} All skill directories exist"
fi

echo ""

# ====================================================================
# Test 2: Verify categories section completeness
# ====================================================================
echo "Test 2: Checking categories coverage..."

# Extract skills from categories section
CATEGORIZED_SKILLS=$(sed -n '/^categories:/,/^# /p' "$SKILLS_DIR/skill-registry.yml" | grep -E "^      - " | sed 's/.*- //' | sort -u | wc -l | tr -d ' ')

# Expected: All 32 active + 4 deprecated = 36
EXPECTED_CATEGORIZED=36

if [[ "$CATEGORIZED_SKILLS" -ge 30 ]]; then
    echo -e "${GREEN}✓${NC} Categories section has $CATEGORIZED_SKILLS skills (expected ~$EXPECTED_CATEGORIZED)"
else
    echo -e "${RED}✗${NC} Categories section only has $CATEGORIZED_SKILLS skills (expected $EXPECTED_CATEGORIZED)"
    ((ERRORS++))
fi

echo ""

# ====================================================================
# Test 3: Verify VERSIONS.yml synchronization
# ====================================================================
echo "Test 3: Validating VERSIONS.yml sync..."

if [[ -f "$SKILLS_DIR/VERSIONS.yml" ]]; then
    VERSIONS_COUNT=$(grep -c "version:" "$SKILLS_DIR/VERSIONS.yml" || echo "0")
    if [[ "$VERSIONS_COUNT" -ge 35 ]]; then
        echo -e "${GREEN}✓${NC} VERSIONS.yml has $VERSIONS_COUNT version entries"
    else
        echo -e "${YELLOW}⚠${NC} VERSIONS.yml may be missing entries ($VERSIONS_COUNT found)"
        ((WARNINGS++))
    fi
else
    echo -e "${RED}✗${NC} VERSIONS.yml not found"
    ((ERRORS++))
fi

echo ""

# ====================================================================
# Test 4: Check deprecated skill aliases
# ====================================================================
echo "Test 4: Checking skill-aliases.json..."

if [[ -f "$SKILLS_DIR/skill-aliases.json" ]]; then
    ALIAS_COUNT=$(grep -c "redirect_to" "$SKILLS_DIR/skill-aliases.json" || echo "0")
    if [[ "$ALIAS_COUNT" -ge 4 ]]; then
        echo -e "${GREEN}✓${NC} skill-aliases.json has $ALIAS_COUNT redirects (expected 4)"
    else
        echo -e "${YELLOW}⚠${NC} skill-aliases.json has only $ALIAS_COUNT redirects (expected 4)"
        ((WARNINGS++))
    fi
else
    echo -e "${YELLOW}⚠${NC} skill-aliases.json not found"
    ((WARNINGS++))
fi

echo ""

# ====================================================================
# Test 5: Cross-check .agent/skills sync
# ====================================================================
echo "Test 5: Verifying .agent/skills mirror..."

if [[ -d "$AGENT_SKILLS_DIR" ]]; then
    CLAUDE_ACTIVE_COUNT=$(find "$SKILLS_DIR" -path "*lifecycle*" -prune -o -name "SKILL.md" -print 2>/dev/null | wc -l | tr -d ' ')
    AGENT_COUNT=$(find "$AGENT_SKILLS_DIR" -name "SKILL.md" 2>/dev/null | wc -l | tr -d ' ')

    if [[ "$AGENT_COUNT" -eq "$CLAUDE_ACTIVE_COUNT" ]]; then
        echo -e "${GREEN}✓${NC} .agent/skills has $AGENT_COUNT skills (matches .claude active skills)"
    else
        echo -e "${YELLOW}⚠${NC} .agent/skills has $AGENT_COUNT skills, .claude has $CLAUDE_ACTIVE_COUNT active"
        ((WARNINGS++))
    fi
else
    echo -e "${YELLOW}⚠${NC} .agent/skills directory not found"
    ((WARNINGS++))
fi

echo ""

# ====================================================================
# Summary
# ====================================================================
echo "======================================================================"
echo "Validation Summary"
echo "======================================================================"
echo ""

if [[ "$ERRORS" -eq 0 && "$WARNINGS" -eq 0 ]]; then
    echo -e "${GREEN}✓ ALL TESTS PASSED${NC}"
    echo ""
    echo "Skills registry is consistent!"
    exit 0
elif [[ "$ERRORS" -eq 0 ]]; then
    echo -e "${YELLOW}⚠ PASSED WITH WARNINGS${NC}"
    echo ""
    echo "Warnings: $WARNINGS"
    echo "Please review warnings above."
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
