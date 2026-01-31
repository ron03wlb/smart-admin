#!/bin/bash

# ====================================================================
# SmartAdmin Skill Registry Verification Script
# ====================================================================
# Purpose: Verify skill-registry.yml consistency with actual directory structure
# Version: 1.0.0
# Created: 2026-01-29
# ====================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
SKILLS_DIR="$PROJECT_ROOT/.claude/skills"
REGISTRY_FILE="$SKILLS_DIR/skill-registry.yml"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
TOTAL_CHECKS=0
PASSED_CHECKS=0
FAILED_CHECKS=0

# Fast mode (skip expensive checks)
FAST_MODE=false
if [[ "${1:-}" == "--fast" ]]; then
    FAST_MODE=true
fi

# ====================================================================
# Helper Functions
# ====================================================================

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

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# ====================================================================
# Verification Functions
# ====================================================================

check_registry_file_exists() {
    ((TOTAL_CHECKS++))
    if [[ -f "$REGISTRY_FILE" ]]; then
        print_success "skill-registry.yml exists"
        return 0
    else
        print_error "skill-registry.yml NOT FOUND: $REGISTRY_FILE"
        return 1
    fi
}

check_registry_yaml_valid() {
    ((TOTAL_CHECKS++))
    if command -v yq &> /dev/null; then
        if yq eval '.' "$REGISTRY_FILE" &> /dev/null; then
            print_success "skill-registry.yml is valid YAML"
            return 0
        else
            print_error "skill-registry.yml is INVALID YAML"
            return 1
        fi
    else
        print_warning "yq not installed, skipping YAML validation"
        return 0
    fi
}

extract_skill_paths() {
    # Try yq v4 first with validation, fallback to grep if anything fails
    if command -v yq &> /dev/null && yq --version 2>&1 | grep -q "version v4"; then
        echo "[DEBUG] Using yq v4 for extraction" >&2
        local paths_from_yq
        paths_from_yq=$(yq eval '.skills.*.path' "$REGISTRY_FILE" 2>/dev/null || true)

        # Only use yq output if it's not empty
        if [[ -n "$paths_from_yq" ]]; then
            echo "[DEBUG] yq extracted $(echo "$paths_from_yq" | wc -l) paths" >&2
            echo "$paths_from_yq"
            return 0
        else
            echo "[DEBUG] yq output was empty, falling back to grep" >&2
        fi
    else
        echo "[DEBUG] yq v4 not found, using grep fallback" >&2
    fi

    # Fallback: use grep (yq not found, not v4, or failed to extract)
    # Add || true to prevent set -e from exiting if grep finds no matches
    local paths_from_grep
    paths_from_grep=$(grep 'path:' "$REGISTRY_FILE" | sed 's/.*path: *"\([^"]*\)".*/\1/' || true)
    echo "[DEBUG] grep extracted $(echo "$paths_from_grep" | wc -l) paths" >&2
    echo "$paths_from_grep"
}

check_skill_paths_exist() {
    print_header "Checking Skill Paths"

    local paths
    paths=$(extract_skill_paths)

    if [[ -z "$paths" ]]; then
        print_error "No skill paths found in registry"
        return 1
    fi

    local path_count=0
    local missing_paths=()

    while IFS= read -r path; do
        [[ -z "$path" ]] && continue
        ((TOTAL_CHECKS++))
        ((path_count++))

        local full_path="$SKILLS_DIR/$path"
        if [[ -d "$full_path" ]]; then
            print_success "Path exists: $path"
        else
            print_error "Path NOT FOUND: $path"
            missing_paths+=("$path")
        fi
    done <<< "$paths"

    print_info "Total paths checked: $path_count"

    if [[ ${#missing_paths[@]} -gt 0 ]]; then
        echo ""
        print_error "Missing paths (${#missing_paths[@]}):"
        for p in "${missing_paths[@]}"; do
            echo "  - $p"
        done
        return 1
    fi

    return 0
}

check_config_yml_exists() {
    if [[ "$FAST_MODE" == true ]]; then
        print_info "Skipping config.yml checks (fast mode)"
        return 0
    fi

    print_header "Checking config.yml Files"

    local paths
    paths=$(extract_skill_paths)

    local missing_configs=()

    while IFS= read -r path; do
        [[ -z "$path" ]] && continue
        ((TOTAL_CHECKS++))

        local config_file="$SKILLS_DIR/$path/config.yml"
        if [[ -f "$config_file" ]]; then
            print_success "config.yml exists: $path"
        else
            print_error "config.yml NOT FOUND: $path"
            missing_configs+=("$path")
        fi
    done <<< "$paths"

    if [[ ${#missing_configs[@]} -gt 0 ]]; then
        echo ""
        print_error "Missing config.yml (${#missing_configs[@]}):"
        for p in "${missing_configs[@]}"; do
            echo "  - $p/config.yml"
        done
        return 1
    fi

    return 0
}

check_skill_md_exists() {
    if [[ "$FAST_MODE" == true ]]; then
        print_info "Skipping SKILL.md checks (fast mode)"
        return 0
    fi

    print_header "Checking SKILL.md Files"

    local paths
    paths=$(extract_skill_paths)

    local missing_docs=()

    while IFS= read -r path; do
        [[ -z "$path" ]] && continue
        ((TOTAL_CHECKS++))

        local skill_md="$SKILLS_DIR/$path/SKILL.md"
        if [[ -f "$skill_md" ]]; then
            print_success "SKILL.md exists: $path"
        else
            print_error "SKILL.md NOT FOUND: $path"
            missing_docs+=("$path")
        fi
    done <<< "$paths"

    if [[ ${#missing_docs[@]} -gt 0 ]]; then
        echo ""
        print_error "Missing SKILL.md (${#missing_docs[@]}):"
        for p in "${missing_docs[@]}"; do
            echo "  - $p/SKILL.md"
        done
        return 1
    fi

    return 0
}

check_skill_counts() {
    ((TOTAL_CHECKS++))

    print_header "Checking Skill Counts"

    local total_skills
    local active_skills
    local deprecated_skills

    if command -v yq &> /dev/null; then
        total_skills=$(yq eval '.total_skills' "$REGISTRY_FILE" 2>/dev/null || true)
        active_skills=$(yq eval '.active_skills' "$REGISTRY_FILE" 2>/dev/null || true)
        deprecated_skills=$(yq eval '.deprecated_skills' "$REGISTRY_FILE" 2>/dev/null || true)
    else
        total_skills=$(grep "^total_skills:" "$REGISTRY_FILE" | awk '{print $2}' || true)
        active_skills=$(grep "^active_skills:" "$REGISTRY_FILE" | awk '{print $2}' || true)
        deprecated_skills=$(grep "^deprecated_skills:" "$REGISTRY_FILE" | awk '{print $2}' || true)
    fi

    local actual_count
    actual_count=$(extract_skill_paths | grep -c . || echo 0)

    print_info "Registry metadata:"
    echo "  - total_skills: ${total_skills:-0}"
    echo "  - active_skills: ${active_skills:-0}"
    echo "  - deprecated_skills: ${deprecated_skills:-0}"
    echo "  - Actual skill paths: $actual_count"

    # Use default value of 0 if variables are empty
    if [[ "${total_skills:-0}" -eq "$actual_count" ]]; then
        print_success "Skill count matches: $total_skills = $actual_count"
    else
        print_error "Skill count MISMATCH: registry=$total_skills, actual=$actual_count"
        return 1
    fi

    local expected_total=$(( ${active_skills:-0} + ${deprecated_skills:-0} ))
    if [[ "${total_skills:-0}" -eq "$expected_total" ]]; then
        print_success "Skill count formula valid: ${total_skills:-0} = ${active_skills:-0} + ${deprecated_skills:-0}"
    else
        print_error "Skill count formula INVALID: ${total_skills:-0} ≠ ${active_skills:-0} + ${deprecated_skills:-0}"
        return 1
    fi

    return 0
}

# ====================================================================
# Main Execution
# ====================================================================

main() {
    print_header "SmartAdmin Skill Registry Verification"

    if [[ "$FAST_MODE" == true ]]; then
        print_info "Running in FAST MODE (skipping expensive checks)"
    fi

    print_info "Project root: $PROJECT_ROOT"
    print_info "Skills directory: $SKILLS_DIR"
    print_info "Registry file: $REGISTRY_FILE"

    # Run checks
    check_registry_file_exists || exit 1
    check_registry_yaml_valid
    check_skill_paths_exist
    check_config_yml_exists
    check_skill_md_exists
    check_skill_counts

    # Summary
    print_header "Verification Summary"
    echo "Total checks: $TOTAL_CHECKS"
    echo -e "${GREEN}Passed: $PASSED_CHECKS${NC}"
    if [[ $FAILED_CHECKS -gt 0 ]]; then
        echo -e "${RED}Failed: $FAILED_CHECKS${NC}"
        echo ""
        print_error "Verification FAILED"
        exit 1
    else
        echo -e "${GREEN}Failed: $FAILED_CHECKS${NC}"
        echo ""
        print_success "All checks PASSED ✅"
        exit 0
    fi
}

# Run main
main "$@"
