#!/bin/bash

# ====================================================================
# SmartAdmin Skills - Test Helper Functions
# ====================================================================
# Purpose: Helper functions for testing individual skills
# Version: 1.0.0
# Created: 2026-01-30
# ====================================================================

set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# ====================================================================
# Helper Functions
# ====================================================================

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# ====================================================================
# Extract metadata from config.yml
# ====================================================================

extract_skill_id() {
    local config_file="$1"
    yq '.metadata.id' "$config_file" 2>/dev/null || echo ""
}

extract_skill_priority() {
    local config_file="$1"
    yq '.metadata.priority' "$config_file" 2>/dev/null || echo "P2"
}

extract_skill_type() {
    local config_file="$1"
    yq '.metadata.type' "$config_file" 2>/dev/null || echo "atomic"
}

extract_skill_category() {
    local config_file="$1"
    yq '.metadata.category' "$config_file" 2>/dev/null || echo ""
}

# ====================================================================
# Validate skill structure
# ====================================================================

validate_skill_structure() {
    local skill_dir="$1"
    local errors=0

    # Check required files
    if [[ ! -f "$skill_dir/config.yml" ]]; then
        print_error "Missing config.yml in $skill_dir"
        ((errors++))
    fi

    if [[ ! -f "$skill_dir/SKILL.md" ]]; then
        print_error "Missing SKILL.md in $skill_dir"
        ((errors++))
    fi

    if [[ ! -f "$skill_dir/README.md" ]]; then
        print_error "Missing README.md in $skill_dir"
        ((errors++))
    fi

    return $errors
}

# ====================================================================
# Validate config.yml schema
# ====================================================================

validate_config_schema() {
    local config_file="$1"
    local errors=0

    # Check metadata section
    if ! yq '.metadata' "$config_file" >/dev/null 2>&1; then
        print_error "Missing metadata section in $config_file"
        ((errors++))
    fi

    # Check required metadata fields
    local required_fields=("id" "name" "version" "priority" "type" "category")
    for field in "${required_fields[@]}"; do
        if [[ -z "$(yq ".metadata.$field" "$config_file" 2>/dev/null)" ]] || \
           [[ "$(yq ".metadata.$field" "$config_file" 2>/dev/null)" == "null" ]]; then
            print_error "Missing metadata.$field in $config_file"
            ((errors++))
        fi
    done

    # Check triggers section
    if ! yq '.triggers' "$config_file" >/dev/null 2>&1; then
        print_warning "Missing triggers section in $config_file (optional)"
    fi

    return $errors
}

# ====================================================================
# Test skill documentation
# ====================================================================

test_skill_documentation() {
    local skill_dir="$1"
    local errors=0

    # Check SKILL.md has required sections
    local skill_md="$skill_dir/SKILL.md"
    if [[ -f "$skill_md" ]]; then
        # Check for key sections
        if ! grep -q "## Purpose" "$skill_md" && ! grep -q "## 用途" "$skill_md"; then
            print_warning "SKILL.md missing Purpose section"
        fi

        if ! grep -q "## Usage" "$skill_md" && ! grep -q "## 使用方式" "$skill_md"; then
            print_warning "SKILL.md missing Usage section"
        fi
    fi

    return $errors
}

# ====================================================================
# Test skill based on priority
# ====================================================================

test_skill_by_priority() {
    local config_file="$1"
    local skill_dir="$(dirname "$config_file")"
    local priority="$(extract_skill_priority "$config_file")"
    local skill_id="$(extract_skill_id "$config_file")"

    print_info "Testing skill: $skill_id (Priority: $priority)"

    # Basic structure validation (all priorities)
    if ! validate_skill_structure "$skill_dir"; then
        return 1
    fi

    # Config schema validation (all priorities)
    if ! validate_config_schema "$config_file"; then
        return 1
    fi

    # Documentation test (all priorities)
    test_skill_documentation "$skill_dir"

    case "$priority" in
        P0)
            # P0: Full manual + automated testing required
            print_info "P0 skill - Full testing required"
            # For now, we validate structure and config
            # Full functional testing would be added here
            ;;
        P1)
            # P1: Critical path testing
            print_info "P1 skill - Critical path testing"
            # Validate core functionality exists
            ;;
        P2)
            # P2: Sampling + new skills complete testing
            print_info "P2 skill - Structure and config validation"
            ;;
        *)
            print_warning "Unknown priority: $priority"
            ;;
    esac

    return 0
}

# ====================================================================
# Check if skill is deprecated
# ====================================================================

is_deprecated_skill() {
    local config_file="$1"
    [[ "$config_file" == *"/lifecycle/deprecated/"* ]] && return 0
    return 1
}

# ====================================================================
# Export functions for use in other scripts
# ====================================================================

export -f print_success
export -f print_error
export -f print_info
export -f print_warning
export -f extract_skill_id
export -f extract_skill_priority
export -f extract_skill_type
export -f extract_skill_category
export -f validate_skill_structure
export -f validate_config_schema
export -f test_skill_documentation
export -f test_skill_by_priority
export -f is_deprecated_skill
