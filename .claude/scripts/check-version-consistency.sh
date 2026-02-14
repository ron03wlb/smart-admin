#!/bin/bash

# ====================================================================
# SmartAdmin Version Consistency Checker
# ====================================================================
# Purpose: Verify version consistency across documentation files
# Version: 1.1.0 (Bash 3.2 compatible)
# Created: 2026-01-29
# Updated: 2026-02-01
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
SKILL_REGISTRY=".claude/skills/skill-registry.yml"
SKILLS_README=".claude/skills/README.md"
CLAUDE_MD="CLAUDE.md"
META_MD=".claude/META.md"

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
            # Extract version from "This Document" row in version table
            version=$(grep "| \*\*This Document\*\*" "$PROJECT_ROOT/$file" | sed 's/.*| \([0-9.]*\) |.*/\1/')
            ;;
        *.claude/META.md)
            # Extract version from ".claude/ System" row in component table
            version=$(grep "| \*\*\.claude/ System\*\*" "$PROJECT_ROOT/$file" | sed 's/.*| \([0-9.]*\) |.*/\1/')
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

    local has_error=false

    # Extract versions from all files
    print_info "Extracting versions..."

    local registry_ver
    registry_ver=$(extract_version "$SKILL_REGISTRY")
    print_info "skill-registry.yml: $registry_ver"

    local readme_ver
    readme_ver=$(extract_version "$SKILLS_README")
    print_info "skills/README.md: $readme_ver"

    local claude_ver
    claude_ver=$(extract_version "$CLAUDE_MD")
    print_info "CLAUDE.md: $claude_ver"

    local meta_ver
    meta_ver=$(extract_version "$META_MD")
    print_info ".claude/META.md: $meta_ver"

    # Check for empty versions
    if [[ -z "$registry_ver" ]] || [[ -z "$readme_ver" ]] || [[ -z "$claude_ver" ]] || [[ -z "$meta_ver" ]]; then
        print_error "Failed to extract one or more versions"
        return 1
    fi

    # Check consistency
    echo ""
    print_header "Consistency Analysis"

    # skill-registry.yml should match skills/README.md
    ((TOTAL_CHECKS++))
    if [[ "$registry_ver" == "$readme_ver" ]]; then
        print_success "skill-registry.yml ↔ skills/README.md: $registry_ver"
    else
        print_error "Version MISMATCH: skill-registry.yml ($registry_ver) ≠ skills/README.md ($readme_ver)"
        has_error=true
    fi

    if [[ "$has_error" == true ]]; then
        return 1
    fi

    return 0
}

check_skill_counts() {
    print_header "Skill Count Consistency"

    local registry_file="$PROJECT_ROOT/$SKILL_REGISTRY"
    local readme_file="$PROJECT_ROOT/$SKILLS_README"

    if [[ ! -f "$registry_file" ]] || [[ ! -f "$readme_file" ]]; then
        print_error "Required files not found"
        return 1
    fi

    # Extract from skill-registry.yml
    local registry_total
    registry_total=$(grep "^total_skills:" "$registry_file" | sed 's/.*: \(.*\)/\1/' | tr -d ' ')

    # Extract from README.md
    local readme_total
    readme_total=$(grep "^\*\*Total Skills\*\*:" "$readme_file" | sed 's/.*: \([0-9]*\) .*/\1/' | tr -d ' ')

    print_info "Registry total: $registry_total"
    print_info "README total: $readme_total"

    ((TOTAL_CHECKS++))
    if [[ "$registry_total" == "$readme_total" ]]; then
        print_success "Skill counts match: $registry_total"
        return 0
    else
        print_error "Skill count MISMATCH: registry=$registry_total, README=$readme_total"
        return 1
    fi
}

main() {
    print_header "SmartAdmin Version Consistency Check"

    print_info "Project root: $PROJECT_ROOT"
    echo ""

    # Step 1: Check file existence
    print_header "File Existence Check"
    local files_exist=true

    check_file_exists "skill-registry.yml" "$SKILL_REGISTRY" || files_exist=false
    check_file_exists "skills/README.md" "$SKILLS_README" || files_exist=false
    check_file_exists "CLAUDE.md" "$CLAUDE_MD" || files_exist=false
    check_file_exists ".claude/META.md" "$META_MD" || files_exist=false

    if [[ "$files_exist" == false ]]; then
        print_error "Some files are missing. Aborting."
        exit 1
    fi

    echo ""
    print_success "All required files exist"

    # Step 2: Check version consistency
    if ! check_version_consistency; then
        print_error "Version consistency check FAILED"
        exit 1
    fi

    # Step 3: Check skill counts
    if ! check_skill_counts; then
        print_error "Skill count consistency check FAILED"
        exit 1
    fi

    # Final summary
    echo ""
    print_header "Summary"
    print_success "Total checks: $TOTAL_CHECKS"
    print_success "Passed: $PASSED_CHECKS"

    if [[ $FAILED_CHECKS -gt 0 ]]; then
        print_error "Failed: $FAILED_CHECKS"
        exit 1
    else
        print_success "All checks PASSED ✨"
        exit 0
    fi
}

main
