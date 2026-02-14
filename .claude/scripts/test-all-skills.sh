#!/bin/bash

# ====================================================================
# SmartAdmin Skills - Complete Testing Suite
# ====================================================================
# Purpose: Automated testing for all skills with priority-based strategy
# Version: 1.0.0
# Created: 2026-01-30
# ====================================================================

set -eo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
SKILLS_DIR="$PROJECT_ROOT/.claude/skills"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# ====================================================================
# Counters
# ====================================================================

P0_TOTAL=0
P0_PASSED=0
P0_FAILED=0
P1_TOTAL=0
P1_PASSED=0
P1_FAILED=0
P2_TOTAL=0
P2_PASSED=0
P2_FAILED=0
DEPRECATED_TOTAL=0
DEPRECATED_CHECKED=0

FAILED_SKILLS=()

# ====================================================================
# Print Functions
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

print_header() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
}

print_separator() {
    echo ""
    echo -e "${BLUE}----------------------------------------${NC}"
    echo ""
}

# ====================================================================
# Extract metadata from config.yml
# ====================================================================

extract_skill_id() {
    local config_file="$1"
    grep -E "^[[:space:]]*name:" "$config_file" 2>/dev/null | head -1 | sed 's/.*name:[[:space:]]*"\([^"]*\)".*/\1/' || echo ""
}

extract_skill_priority() {
    local config_file="$1"
    local priority
    priority=$(grep -E "^[[:space:]]*priority:" "$config_file" 2>/dev/null | head -1 | sed 's/.*priority:[[:space:]]*"\([^"]*\)".*/\1/')
    if [[ -z "$priority" ]]; then
        echo "P2"
    else
        echo "$priority"
    fi
}

is_deprecated_skill() {
    local config_file="$1"
    [[ "$config_file" == *"/lifecycle/deprecated/"* ]] && return 0
    return 1
}

# ====================================================================
# Validate skill structure
# ====================================================================

validate_skill_structure() {
    local skill_dir="$1"
    local errors=0

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

    # Check for either "metadata:" or "skill_metadata:" section
    if ! grep -q "metadata:" "$config_file" && ! grep -q "skill_metadata:" "$config_file"; then
        print_error "Missing metadata/skill_metadata section in $config_file"
        ((errors++))
        return $errors
    fi

    # Required fields (note: using "name" instead of "id" as some configs use name)
    local required_fields=("name" "version" "priority" "type" "category")
    for field in "${required_fields[@]}"; do
        if ! grep -q "^\s*${field}:" "$config_file"; then
            print_error "Missing $field in $config_file"
            ((errors++))
        fi
    done

    return $errors
}

# ====================================================================
# Test a single skill
# ====================================================================

test_single_skill() {
    local config_file="$1"
    local skill_dir
    skill_dir="$(dirname "$config_file")"
    local skill_name
    skill_name="$(basename "$skill_dir")"
    local priority
    priority="$(extract_skill_priority "$config_file")"

    echo ""
    print_info "Testing: $skill_name"

    # Check if deprecated
    if is_deprecated_skill "$config_file"; then
        priority="DEPRECATED"
    fi

    # Increment total counter
    case "$priority" in
        P0) ((P0_TOTAL++)) ;;
        P1) ((P1_TOTAL++)) ;;
        P2) ((P2_TOTAL++)) ;;
        DEPRECATED) ((DEPRECATED_TOTAL++)) ;;
    esac

    # Run validation tests
    local test_passed=true

    if ! validate_skill_structure "$skill_dir"; then
        test_passed=false
    fi

    if ! validate_config_schema "$config_file"; then
        test_passed=false
    fi

    # Update counters
    if [[ "$test_passed" == "true" ]]; then
        print_success "PASSED: $skill_name ($priority)"
        case "$priority" in
            P0) ((P0_PASSED++)) ;;
            P1) ((P1_PASSED++)) ;;
            P2) ((P2_PASSED++)) ;;
            DEPRECATED) ((DEPRECATED_CHECKED++)) ;;
        esac
        return 0
    else
        print_error "FAILED: $skill_name ($priority)"
        FAILED_SKILLS+=("$skill_name ($priority)")
        case "$priority" in
            P0) ((P0_FAILED++)) ;;
            P1) ((P1_FAILED++)) ;;
            P2) ((P2_FAILED++)) ;;
        esac
        return 1
    fi
}

# ====================================================================
# Test Week 4-5 new skills (complete testing)
# ====================================================================

test_new_skills() {
    print_header "Week 4-5 New Skills - Complete Testing"

    local new_skills=(
        "$SKILLS_DIR/productivity/integration/postgresql-best-practices/config.yml"
        "$SKILLS_DIR/productivity/refactoring/smartadmin-manager-extractor/config.yml"
        "$SKILLS_DIR/extended/quality/concurrency-safety-auditor/config.yml"
    )

    local new_skills_passed=0
    local new_skills_total=3

    for config in "${new_skills[@]}"; do
        if [[ -f "$config" ]]; then
            if test_single_skill "$config"; then
                ((new_skills_passed++))
            fi
        else
            print_error "New skill config not found: $config"
        fi
    done

    print_separator
    print_info "New Skills Result: $new_skills_passed / $new_skills_total PASSED"

    if [[ $new_skills_passed -eq $new_skills_total ]]; then
        print_success "All new skills PASSED ✅"
    else
        print_error "Some new skills FAILED"
    fi
}

# ====================================================================
# Test all skills by priority
# ====================================================================

test_all_skills() {
    print_header "Testing All Skills by Priority"

    # Create a temporary array to avoid subshell issues
    local configs=()
    while IFS= read -r config; do
        configs+=("$config")
    done < <(find "$SKILLS_DIR" -name "config.yml" -type f | sort)

    # Test each skill
    for config in "${configs[@]}"; do
        test_single_skill "$config" || true
    done
}

# ====================================================================
# Generate test summary
# ====================================================================

generate_summary() {
    print_header "Test Summary"

    local total_skills=$((P0_TOTAL + P1_TOTAL + P2_TOTAL))
    local total_passed=$((P0_PASSED + P1_PASSED + P2_PASSED))
    local total_failed=$((P0_FAILED + P1_FAILED + P2_FAILED))
    local pass_rate=0

    if [[ $total_skills -gt 0 ]]; then
        pass_rate=$(awk "BEGIN {printf \"%.1f\", ($total_passed / $total_skills) * 100}")
    fi

    echo "📊 Overall Results:"
    echo "   Total Skills Tested: $total_skills"
    echo "   Passed: $total_passed"
    echo "   Failed: $total_failed"
    echo "   Pass Rate: $pass_rate%"
    echo ""

    echo "📊 By Priority:"
    echo ""
    echo "   P0 (Foundation): $P0_PASSED / $P0_TOTAL passed"
    if [[ $P0_TOTAL -gt 0 ]]; then
        local p0_rate
        p0_rate=$(awk "BEGIN {printf \"%.1f\", ($P0_PASSED / $P0_TOTAL) * 100}")
        echo "                    Pass Rate: $p0_rate%"
    fi
    echo ""

    echo "   P1 (Extended):   $P1_PASSED / $P1_TOTAL passed"
    if [[ $P1_TOTAL -gt 0 ]]; then
        local p1_rate
        p1_rate=$(awk "BEGIN {printf \"%.1f\", ($P1_PASSED / $P1_TOTAL) * 100}")
        echo "                    Pass Rate: $p1_rate%"
    fi
    echo ""

    echo "   P2 (Productivity): $P2_PASSED / $P2_TOTAL passed"
    if [[ $P2_TOTAL -gt 0 ]]; then
        local p2_rate
        p2_rate=$(awk "BEGIN {printf \"%.1f\", ($P2_PASSED / $P2_TOTAL) * 100}")
        echo "                    Pass Rate: $p2_rate%"
    fi
    echo ""

    if [[ $DEPRECATED_TOTAL -gt 0 ]]; then
        echo "   Deprecated:      $DEPRECATED_CHECKED / $DEPRECATED_TOTAL checked"
        echo ""
    fi

    # List failed skills
    if [[ ${#FAILED_SKILLS[@]} -gt 0 ]]; then
        echo ""
        print_error "Failed Skills:"
        for skill in "${FAILED_SKILLS[@]}"; do
            echo "   - $skill"
        done
        echo ""
    fi

    # Final verdict
    print_separator
    if [[ $total_failed -eq 0 ]]; then
        print_success "All tests PASSED ✅"
        print_success "Pass Rate: $pass_rate% (Target: ≥95%)"
        return 0
    else
        print_error "$total_failed skill(s) FAILED"
        if awk "BEGIN {exit !($pass_rate >= 95)}"; then
            print_success "Pass Rate: $pass_rate% (Target: ≥95%) ✅"
            return 0
        else
            print_error "Pass Rate: $pass_rate% (Target: ≥95%) ❌"
            return 1
        fi
    fi
}

# ====================================================================
# Main execution
# ====================================================================

main() {
    print_header "SmartAdmin Skills - Complete Testing Suite"

    print_info "Project root: $PROJECT_ROOT"
    print_info "Skills directory: $SKILLS_DIR"

    # Step 1: Test new skills (Week 4-5)
    test_new_skills

    # Step 2: Test all skills
    test_all_skills

    # Step 3: Generate summary
    generate_summary
}

main "$@"
