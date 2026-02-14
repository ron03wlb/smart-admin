#!/bin/bash

# ====================================================================
# SmartAdmin Skills - Complete Verification Suite
# ====================================================================
# Purpose: Run all skill verification checks
# Version: 1.0.0
# Created: 2026-01-29
# ====================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

print_header() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

FAILED_CHECKS=0

print_header "SmartAdmin Skills - Complete Verification Suite"

# 1. Verify skill registry
print_header "Step 1/3: Verify Skill Registry"
if bash "$SCRIPT_DIR/verify-skill-registry.sh" --fast; then
    print_success "Skill registry verification PASSED"
else
    print_error "Skill registry verification FAILED"
    ((FAILED_CHECKS++))
fi

# 2. Validate dependency graph
print_header "Step 2/3: Validate Dependency Graph"
if python3 "$SCRIPT_DIR/validate-dependency-graph.py"; then
    print_success "Dependency graph validation PASSED"
else
    print_error "Dependency graph validation FAILED"
    ((FAILED_CHECKS++))
fi

# 3. Check version consistency
print_header "Step 3/3: Check Version Consistency"
if bash "$SCRIPT_DIR/check-version-consistency.sh"; then
    print_success "Version consistency check PASSED"
else
    print_error "Version consistency check FAILED"
    ((FAILED_CHECKS++))
fi

# Final summary
print_header "Verification Summary"

if [[ $FAILED_CHECKS -eq 0 ]]; then
    print_success "All verifications PASSED ✅"
    exit 0
else
    print_error "$FAILED_CHECKS check(s) FAILED"
    exit 1
fi
