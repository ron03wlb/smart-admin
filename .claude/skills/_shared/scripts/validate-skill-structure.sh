#!/usr/bin/env bash

# ====================================================================
# SmartAdmin Skill Structure Validator
# ====================================================================
# Purpose: Validate that skill directories conform to v3.0.0 standards
# Usage:
#   ./validate-skill-structure.sh --all                    # Validate all skills
#   ./validate-skill-structure.sh --skills foundation/     # Validate specific category
#   ./validate-skill-structure.sh --skill archunit-test-generator  # Single skill
# ====================================================================

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
TOTAL_SKILLS=0
PASSED_SKILLS=0
FAILED_SKILLS=0
WARNINGS=0

# Required files for all skills
REQUIRED_FILES=("SKILL.md" "README.md" "config.yml")

# Script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
SKILLS_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# ====================================================================
# Helper Functions
# ====================================================================

print_header() {
    echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║  SmartAdmin Skill Structure Validator v3.0.0           ║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

print_summary() {
    echo ""
    echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║  Validation Summary                                    ║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
    echo -e "Total Skills Checked: ${TOTAL_SKILLS}"
    echo -e "${GREEN}✓ Passed: ${PASSED_SKILLS}${NC}"
    if [ $FAILED_SKILLS -gt 0 ]; then
        echo -e "${RED}✗ Failed: ${FAILED_SKILLS}${NC}"
    fi
    if [ $WARNINGS -gt 0 ]; then
        echo -e "${YELLOW}⚠ Warnings: ${WARNINGS}${NC}"
    fi
    echo ""
}

validate_skill() {
    local skill_path="$1"
    local skill_name="$(basename "$skill_path")"
    local has_errors=false

    ((TOTAL_SKILLS++))

    echo -e "${BLUE}Validating:${NC} $skill_name"

    # Check required files
    for file in "${REQUIRED_FILES[@]}"; do
        if [ ! -f "$skill_path/$file" ]; then
            echo -e "  ${RED}✗ Missing required file: $file${NC}"
            has_errors=true
        else
            echo -e "  ${GREEN}✓ Found: $file${NC}"
        fi
    done

    # Check SKILL.md has frontmatter
    if [ -f "$skill_path/SKILL.md" ]; then
        if head -n 1 "$skill_path/SKILL.md" | grep -q "^---$"; then
            echo -e "  ${GREEN}✓ SKILL.md has YAML frontmatter${NC}"
        else
            echo -e "  ${YELLOW}⚠ SKILL.md missing YAML frontmatter${NC}"
            ((WARNINGS++))
        fi
    fi

    # Check config.yml structure
    if [ -f "$skill_path/config.yml" ]; then
        if grep -q "^metadata:" "$skill_path/config.yml" && \
           grep -q "^triggers:" "$skill_path/config.yml" && \
           grep -q "^execution:" "$skill_path/config.yml"; then
            echo -e "  ${GREEN}✓ config.yml has required sections${NC}"
        else
            echo -e "  ${RED}✗ config.yml missing required sections (metadata/triggers/execution)${NC}"
            has_errors=true
        fi
    fi

    # Check for phases/ or modes/ directory (for composite skills)
    if [ -d "$skill_path/phases" ]; then
        local phase_count=$(find "$skill_path/phases" -name "*.md" | wc -l)
        echo -e "  ${BLUE}ℹ Found phases/ directory with $phase_count phase docs${NC}"
    fi

    if [ -d "$skill_path/modes" ]; then
        local mode_count=$(find "$skill_path/modes" -name "*.md" | wc -l)
        echo -e "  ${BLUE}ℹ Found modes/ directory with $mode_count mode docs${NC}"
    fi

    # Summary for this skill
    if [ "$has_errors" = false ]; then
        echo -e "  ${GREEN}✓ Validation passed${NC}"
        ((PASSED_SKILLS++))
    else
        echo -e "  ${RED}✗ Validation failed${NC}"
        ((FAILED_SKILLS++))
    fi

    echo ""
}

find_skills() {
    local search_path="$1"

    # Find all directories that contain a SKILL.md or config.yml
    find "$search_path" -mindepth 1 -maxdepth 3 -type d | while read -r dir; do
        if [ -f "$dir/SKILL.md" ] || [ -f "$dir/config.yml" ]; then
            echo "$dir"
        fi
    done
}

# ====================================================================
# Main Script
# ====================================================================

print_header

# Parse arguments
MODE=""
TARGET_PATH=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --all)
            MODE="all"
            shift
            ;;
        --skills)
            MODE="category"
            TARGET_PATH="$2"
            shift 2
            ;;
        --skill)
            MODE="single"
            TARGET_PATH="$2"
            shift 2
            ;;
        -h|--help)
            echo "Usage:"
            echo "  $0 --all                    # Validate all skills"
            echo "  $0 --skills foundation/     # Validate specific category"
            echo "  $0 --skill archunit-test-generator  # Validate single skill"
            echo ""
            exit 0
            ;;
        *)
            echo -e "${RED}Error: Unknown option $1${NC}"
            exit 1
            ;;
    esac
done

# Default to --all if no mode specified
if [ -z "$MODE" ]; then
    MODE="all"
fi

# Execute validation based on mode
case $MODE in
    all)
        echo -e "${BLUE}Mode:${NC} Validate all skills"
        echo ""

        # Validate foundation, extended, productivity skills
        for category in foundation extended productivity lifecycle; do
            if [ -d "$SKILLS_ROOT/$category" ]; then
                echo -e "${BLUE}Scanning category:${NC} $category/"
                find_skills "$SKILLS_ROOT/$category" | while read -r skill; do
                    validate_skill "$skill"
                done
            fi
        done
        ;;

    category)
        echo -e "${BLUE}Mode:${NC} Validate category: $TARGET_PATH"
        echo ""

        if [ -d "$SKILLS_ROOT/$TARGET_PATH" ]; then
            find_skills "$SKILLS_ROOT/$TARGET_PATH" | while read -r skill; do
                validate_skill "$skill"
            done
        else
            echo -e "${RED}Error: Category not found: $TARGET_PATH${NC}"
            exit 1
        fi
        ;;

    single)
        echo -e "${BLUE}Mode:${NC} Validate single skill: $TARGET_PATH"
        echo ""

        # Try to find the skill
        if [ -d "$SKILLS_ROOT/$TARGET_PATH" ]; then
            validate_skill "$SKILLS_ROOT/$TARGET_PATH"
        else
            # Search for it in all categories
            FOUND_PATH=$(find "$SKILLS_ROOT" -type d -name "$TARGET_PATH" | head -n 1)
            if [ -n "$FOUND_PATH" ]; then
                validate_skill "$FOUND_PATH"
            else
                echo -e "${RED}Error: Skill not found: $TARGET_PATH${NC}"
                exit 1
            fi
        fi
        ;;
esac

print_summary

# Exit with error code if any failures
if [ $FAILED_SKILLS -gt 0 ]; then
    exit 1
else
    exit 0
fi
