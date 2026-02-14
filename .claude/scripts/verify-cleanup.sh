#!/bin/bash
# ====================================================================
# Root Layer Cleanup Verification Script v1.0.0
# ====================================================================
# Purpose: Verify Week 1 cleanup was successful
# Usage: bash .claude/scripts/verify-cleanup.sh
# ====================================================================

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
PASS=0
FAIL=0

print_header() {
  echo -e "${BLUE}╔══════════════════════════════════════════════════════════════════╗${NC}"
  echo -e "${BLUE}║  Week 1 Root Layer Cleanup - Verification Report                ║${NC}"
  echo -e "${BLUE}╚══════════════════════════════════════════════════════════════════╝${NC}"
  echo ""
}

print_test() {
  local test_name="$1"
  local test_result="$2"
  local details="$3"

  if [ "$test_result" = "PASS" ]; then
    echo -e "${GREEN}✓${NC} $test_name"
    [ -n "$details" ] && echo -e "  ${BLUE}→${NC} $details"
    PASS=$((PASS + 1))
  else
    echo -e "${RED}✗${NC} $test_name"
    [ -n "$details" ] && echo -e "  ${RED}→${NC} $details"
    FAIL=$((FAIL + 1))
  fi
}

print_section() {
  echo ""
  echo -e "${YELLOW}═══ $1 ═══${NC}"
  echo ""
}

# Change to skills directory
cd .claude/skills

print_header

# ====================================================================
# Test 1: Root Layer Should Be Clean
# ====================================================================
print_section "Test 1: Root Layer Cleanliness"

# Get list of items in root excluding legitimate directories
remaining=$(ls -1 | grep -v -E "^(foundation|extended|productivity|lifecycle|skill-registry\.yml|README\.md|_shared|_deprecated|\.agents|SKILL-INVOCATION-GUIDE\.md)$" || true)

if [ -z "$remaining" ]; then
  print_test "Root layer is clean" "PASS" "No skill directories or files in root"
else
  item_count=$(echo "$remaining" | wc -l)
  print_test "Root layer is clean" "FAIL" "$item_count items still in root:"
  echo "$remaining" | sed 's/^/    - /'
fi

# ====================================================================
# Test 2: Backup Directories Exist
# ====================================================================
print_section "Test 2: Backup Directory Structure"

if [ -d "_deprecated/root-layer-v3" ]; then
  print_test "root-layer-v3 directory exists" "PASS"
else
  print_test "root-layer-v3 directory exists" "FAIL" "Directory not found"
fi

if [ -d "_deprecated/non-skill-items-v3" ]; then
  print_test "non-skill-items-v3 directory exists" "PASS"
else
  print_test "non-skill-items-v3 directory exists" "FAIL" "Directory not found"
fi

if [ -d "_deprecated/skill-files-v3" ]; then
  print_test "skill-files-v3 directory exists" "PASS"
else
  print_test "skill-files-v3 directory exists" "FAIL" "Directory not found"
fi

# ====================================================================
# Test 3: Backup Completeness
# ====================================================================
print_section "Test 3: Backup Completeness"

# Check skill directories (should be 28)
if [ -d "_deprecated/root-layer-v3" ]; then
  skill_count=$(ls -1 _deprecated/root-layer-v3 | grep -v "README.md" | wc -l)
  if [ "$skill_count" -eq 28 ]; then
    print_test "28 skill directories backed up" "PASS" "Found: $skill_count/28"
  else
    print_test "28 skill directories backed up" "FAIL" "Found: $skill_count/28 (expected 28)"
  fi
fi

# Check .skill files (should be 9)
if [ -d "_deprecated/skill-files-v3" ]; then
  file_count=$(ls -1 _deprecated/skill-files-v3 | grep -v "README.md" | wc -l)
  if [ "$file_count" -eq 9 ]; then
    print_test "9 .skill files backed up" "PASS" "Found: $file_count/9"
  else
    print_test "9 .skill files backed up" "FAIL" "Found: $file_count/9 (expected 9)"
  fi
fi

# Check non-skill items (should be 33: 27 directories + 6 doc files)
if [ -d "_deprecated/non-skill-items-v3" ]; then
  item_count=$(ls -1 _deprecated/non-skill-items-v3 | grep -v "README.md" | wc -l)
  if [ "$item_count" -eq 33 ]; then
    print_test "33 non-skill items backed up" "PASS" "Found: $item_count/33"
  else
    print_test "33 non-skill items backed up" "FAIL" "Found: $item_count/33 (expected 33)"
  fi
fi

# ====================================================================
# Test 4: Hierarchical Structure Intact
# ====================================================================
print_section "Test 4: Hierarchical Structure Integrity"

# Check foundation/
if [ -d "foundation/backend" ] && [ -d "foundation/full-stack" ] && [ -d "foundation/testing" ]; then
  backend_count=$(ls -1 foundation/backend | wc -l)
  fullstack_count=$(ls -1 foundation/full-stack | wc -l)
  testing_count=$(ls -1 foundation/testing | wc -l)
  print_test "foundation/ structure intact" "PASS" "backend:$backend_count full-stack:$fullstack_count testing:$testing_count"
else
  print_test "foundation/ structure intact" "FAIL" "Missing subdirectories"
fi

# Check extended/
if [ -d "extended/business-logic" ] && [ -d "extended/domain" ]; then
  business_count=$(ls -1 extended/business-logic | wc -l)
  domain_count=$(ls -1 extended/domain | wc -l)
  print_test "extended/ structure intact" "PASS" "business-logic:$business_count domain:$domain_count"
else
  print_test "extended/ structure intact" "FAIL" "Missing subdirectories"
fi

# Check productivity/
if [ -d "productivity/infrastructure" ]; then
  infra_count=$(ls -1 productivity/infrastructure | wc -l)
  print_test "productivity/ structure intact" "PASS" "infrastructure:$infra_count"
else
  print_test "productivity/ structure intact" "FAIL" "Missing subdirectories"
fi

# ====================================================================
# Test 5: README Files Created
# ====================================================================
print_section "Test 5: Documentation"

if [ -f "_deprecated/root-layer-v3/README.md" ]; then
  print_test "root-layer-v3/README.md created" "PASS"
else
  print_test "root-layer-v3/README.md created" "FAIL" "File not found"
fi

if [ -f "_deprecated/non-skill-items-v3/README.md" ]; then
  print_test "non-skill-items-v3/README.md created" "PASS"
else
  print_test "non-skill-items-v3/README.md created" "FAIL" "File not found"
fi

# ====================================================================
# Test 6: Sample Skills Verification
# ====================================================================
print_section "Test 6: Sample Skills Verification"

# Verify a few key skills are in backup
if [ -d "_deprecated/root-layer-v3/smartadmin-crud-generator" ]; then
  print_test "smartadmin-crud-generator in backup" "PASS"
else
  print_test "smartadmin-crud-generator in backup" "FAIL"
fi

if [ -d "_deprecated/root-layer-v3/batch-plan-executor" ]; then
  print_test "batch-plan-executor in backup" "PASS"
else
  print_test "batch-plan-executor in backup" "FAIL"
fi

if [ -d "_deprecated/root-layer-v3/quality-gate-orchestrator" ]; then
  print_test "quality-gate-orchestrator in backup" "PASS"
else
  print_test "quality-gate-orchestrator in backup" "FAIL"
fi

# Verify they still exist in hierarchical structure
if [ -d "foundation/full-stack/smartadmin-crud-generator" ]; then
  print_test "smartadmin-crud-generator in hierarchy" "PASS" "foundation/full-stack/"
else
  print_test "smartadmin-crud-generator in hierarchy" "FAIL"
fi

if [ -d "extended/orchestration/batch-plan-executor" ]; then
  print_test "batch-plan-executor in hierarchy" "PASS" "extended/orchestration/"
else
  print_test "batch-plan-executor in hierarchy" "FAIL"
fi

if [ -d "extended/quality/quality-gate-orchestrator" ]; then
  print_test "quality-gate-orchestrator in hierarchy" "PASS" "extended/quality/"
else
  print_test "quality-gate-orchestrator in hierarchy" "FAIL"
fi

# ====================================================================
# Summary
# ====================================================================
print_section "Summary"

TOTAL=$((PASS + FAIL))
SUCCESS_RATE=$((PASS * 100 / TOTAL))

echo ""
echo -e "${BLUE}╔══════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  Test Results                                                    ║${NC}"
echo -e "${BLUE}╠══════════════════════════════════════════════════════════════════╣${NC}"
printf "${BLUE}║${NC}  Total Tests:    %-47s ${BLUE}║${NC}\n" "$TOTAL"
printf "${BLUE}║${NC}  ${GREEN}Passed:${NC}         %-47s ${BLUE}║${NC}\n" "$PASS"
printf "${BLUE}║${NC}  ${RED}Failed:${NC}         %-47s ${BLUE}║${NC}\n" "$FAIL"
printf "${BLUE}║${NC}  Success Rate:   %-47s ${BLUE}║${NC}\n" "$SUCCESS_RATE%"
echo -e "${BLUE}╚══════════════════════════════════════════════════════════════════╝${NC}"
echo ""

if [ "$FAIL" -eq 0 ]; then
  echo -e "${GREEN}✅ All tests passed! Week 1 cleanup is successful.${NC}"
  echo ""
  echo -e "${YELLOW}Next Steps:${NC}"
  echo "1. Review the verification results"
  echo "2. Create Git commit (see EXECUTE-CLEANUP.md for template)"
  echo "3. Proceed to Week 2-3: Category Reorganization"
  echo ""
  exit 0
else
  echo -e "${RED}❌ Some tests failed. Please review and fix issues.${NC}"
  echo ""
  echo -e "${YELLOW}Troubleshooting:${NC}"
  echo "- Check if cleanup script completed successfully"
  echo "- Verify no permission errors occurred"
  echo "- Review EXECUTE-CLEANUP.md for rollback instructions"
  echo ""
  exit 1
fi
