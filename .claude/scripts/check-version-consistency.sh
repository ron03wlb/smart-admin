#!/bin/bash

# ====================================================================
# SmartAdmin Version Consistency Checker
# ====================================================================
# Purpose: Verify version consistency across documentation files
# Version: 1.0.0
# Created: 2026-01-29
# ====================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Files to check
declare -A VERSION_FILES=(
    ["skill-registry.yml"]=".claude/skills/skill-registry.yml"
    ["skills/README.md"]=".claude/skills/README.md"
    ["CLAUDE.md"]="CLAUDE.md"
    [".claude/META.md"]=".claude/META.md"
)

# Counters
TOTAL_CHECKS=0
PASSED_CHECKS=0
FAILED_CHECKS=0

print_header() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
    ((PASSED_CHECKS++))
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
    ((FAILED_CHECKS++))
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

extract_version() {
    local file="$1"
    local version=""

    case "$file" in
        *skill-registry.yml)
            version=$(grep "^version:" "$PROJECT_ROOT/$file" | head -1 | sed 's/.*"\(.*\)".*/\1/' | tr -d ' ')
            ;;
        *README.md)
            version=$(grep "^\*\*Version\*\*:" "$PROJECT_ROOT/$file" | head -1 | sed 's/.*: \(.*\)/\1/' | tr -d ' ')
            ;;
        *CLAUDE.md)
            # Look for SmartAdmin version
            version=$(grep "SmartAdmin.*v[0-9]" "$PROJECT_ROOT/$file" | head -1 | sed 's/.*v\([0-9.]*\).*/\1/')
            ;;
        *.claude/META.md)
            version=$(grep ".claude/.*version:" "$PROJECT_ROOT/$file" | head -1 | sed 's/.*: \(.*\)/\1/' | tr -d ' ')
            ;;
    esac

    echo "$version"
}

check_file_exists() {
    ((TOTAL_CHECKS++))
    local name="$1"
    local path="$2"
    local full_path="$PROJECT_ROOT/$path"

    if [[ -f "$full_path" ]]; then
        print_success "File exists: $name"
        return 0
    else
        print_error "File NOT FOUND: $name ($path)"
        return 1
    fi
}

check_version_consistency() {
    print_header "Version Consistency Check"

    declare -A versions
    local has_error=false

    # Extract versions from all files
    for name in "${!VERSION_FILES[@]}"; do
        local path="${VERSION_FILES[$name]}"
        local full_path="$PROJECT_ROOT/$path"

        if [[ -f "$full_path" ]]; then
            local ver
            ver=$(extract_version "$path")
            versions["$name"]="$ver"
            print_info "$name: $ver"
        else
            print_error "$name: FILE NOT FOUND"
            has_error=true
        fi
    done

    if [[ "$has_error" == true ]]; then
        return 1
    fi

    # Check consistency
    echo ""
    print_header "Consistency Analysis"

    # skill-registry.yml should match skills/README.md
    ((TOTAL_CHECKS++))
    if [[ "${versions[skill-registry.yml]}" == "${versions[skills/README.md]}" ]]; then
        print_success "skill-registry.yml ↔ skills/README.md: ${versions[skill-registry.yml]}"
    else
        print_error "Version MISMATCH: skill-registry.yml (${versions[skill-registry.yml]}) ≠ skills/README.md (${versions[skills/README.md]})"
    fi

    return 0
}

check_skill_counts() {
    print_header "Skill Count Consistency"

    local registry_file="$PROJECT_ROOT/.claude/skills/skill-registry.yml"
    local readme_file="$PROJECT_ROOT/.claude/skills/README.md"

    if [[ ! -f "$registry_file" ]] || [[ ! -f "$readme_file" ]]; then
        print_error "Required files not found"
        return 1
    fi

    # Extract counts from registry
    local registry_total
    registry_total=$(grep "^total_skills:" "$registry_file" | awk '{print $2}')

    # Extract counts from README
    local readme_total
    readme_total=$(grep "^\*\*Total Skills\*\*:" "$readme_file" | sed 's/.*: \([0-9]*\).*/\1/')

    print_info "skill-registry.yml total_skills: $registry_total"
    print_info "skills/README.md Total Skills: $readme_total"

    ((TOTAL_CHECKS++))
    if [[ "$registry_total" == "$readme_total" ]]; then
        print_success "Skill count matches: $registry_total"
    else
        print_error "Skill count MISMATCH: registry=$registry_total, README=$readme_total"
    fi
}

check_last_updated() {
    print_header "Last Updated Date Check"

    local registry_file="$PROJECT_ROOT/.claude/skills/skill-registry.yml"
    local readme_file="$PROJECT_ROOT/.claude/skills/README.md"

    if [[ ! -f "$registry_file" ]] || [[ ! -f "$readme_file" ]]; then
        print_error "Required files not found"
        return 1
    fi

    local registry_date
    registry_date=$(grep "^last_updated:" "$registry_file" | sed 's/.*"\(.*\)".*/\1/')

    local readme_date
    readme_date=$(grep "^\*\*Last Updated\*\*:" "$readme_file" | sed 's/.*: \(.*\)/\1/')

    print_info "skill-registry.yml last_updated: $registry_date"
    print_info "skills/README.md Last Updated: $readme_date"

    ((TOTAL_CHECKS++))
    if [[ "$registry_date" == "$readme_date" ]]; then
        print_success "Last updated dates match: $registry_date"
    else
        print_error "Last updated MISMATCH: registry=$registry_date, README=$readme_date"
    fi
}

main() {
    print_header "SmartAdmin Version Consistency Checker"

    print_info "Project root: $PROJECT_ROOT"

    # Check all files exist
    print_header "File Existence Check"
    for name in "${!VERSION_FILES[@]}"; do
        check_file_exists "$name" "${VERSION_FILES[$name]}"
    done

    # Check version consistency
    check_version_consistency

    # Check skill counts
    check_skill_counts

    # Check last updated dates
    check_last_updated

    # Summary
    print_header "Summary"
    echo "Total checks: $TOTAL_CHECKS"
    echo -e "${GREEN}Passed: $PASSED_CHECKS${NC}"

    if [[ $FAILED_CHECKS -gt 0 ]]; then
        echo -e "${RED}Failed: $FAILED_CHECKS${NC}"
        echo ""
        print_error "Version consistency check FAILED"
        exit 1
    else
        echo -e "${GREEN}Failed: $FAILED_CHECKS${NC}"
        echo ""
        print_success "All version consistency checks PASSED ✅"
        exit 0
    fi
}

main "$@"
