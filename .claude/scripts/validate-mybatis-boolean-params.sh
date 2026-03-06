#!/bin/bash
# validate-mybatis-boolean-params.sh
#
# Purpose: Validate that all Boolean parameters mapped to PostgreSQL SMALLINT
#          columns in MyBatis XML Mapper files explicitly specify typeHandler.
#
# Usage:
#   bash .claude/scripts/validate-mybatis-boolean-params.sh
#
# Exit Codes:
#   0 - All Boolean parameters have typeHandler
#   1 - Found violations (missing typeHandler)

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
JAVA_BASE_DIR="${PROJECT_ROOT}/smart-admin-api-java21-springboot3"

echo "🔍 MyBatis Boolean Parameter Validation"
echo ""

# Define Boolean field patterns (common SmartAdmin patterns)
FIELD_PATTERNS=("deletedFlag" "disabledFlag" "administratorFlag" "lockedFlag" "enabledFlag")

echo "📋 Boolean field patterns to check: ${#FIELD_PATTERNS[@]}"
printf "   - %s\n" "${FIELD_PATTERNS[@]}"
echo ""

# Step 1: Search for all Boolean parameter references in one grep
echo "📋 Scanning source XML Mapper files (excluding build/)..."

# Build regex pattern for grep: (deletedFlag|disabledFlag|...)
PATTERN_REGEX=$(IFS="|"; echo "${FIELD_PATTERNS[*]}")

# Single grep to find all Boolean parameter references in source directories
ALL_MATCHES=$(grep -rn "#{[^}]*\(${PATTERN_REGEX}\)[^}]*}" \
    --include="*Mapper.xml" \
    "${JAVA_BASE_DIR}"/*/src/main/resources/ \
    2>/dev/null || true)

# Step 2: Filter out lines that already have typeHandler
VIOLATIONS=$(echo "$ALL_MATCHES" | grep -v "typeHandler=" || true)

# Count files and violations
FILES_SCANNED=$(find "${JAVA_BASE_DIR}" -type f -name "*Mapper.xml" \
    -path "*/src/main/resources/*" \
    2>/dev/null | wc -l)

VIOLATION_COUNT=$(echo "$VIOLATIONS" | grep -c ":" || true)

echo "   Files scanned: ${FILES_SCANNED}"
echo ""

# Step 3: Report results
if [[ $VIOLATION_COUNT -eq 0 ]]; then
    echo -e "${GREEN}✅ PASS: All Boolean parameters have typeHandler${NC}"
    echo ""
    echo "   Boolean field patterns checked: ${#FIELD_PATTERNS[@]}"
    echo "   Violations found: 0"
    echo ""
    exit 0
else
    echo -e "${RED}❌ FAIL: Found ${VIOLATION_COUNT} Boolean parameters without typeHandler${NC}"
    echo ""
    echo "Issue locations (showing first 20):"
    echo "$VIOLATIONS" | head -20 | while IFS= read -r line; do
        # Extract file path and line number for cleaner display
        file_line=$(echo "$line" | cut -d: -f1-2)
        rel_file="${file_line#${PROJECT_ROOT}/}"
        echo -e "${YELLOW}  ${rel_file}${NC}"
    done
    if [[ $VIOLATION_COUNT -gt 20 ]]; then
        echo -e "${YELLOW}  ... and $((VIOLATION_COUNT - 20)) more${NC}"
    fi
    echo ""
    echo "Fix: Add typeHandler attribute to each parameter:"
    echo "  #{param, jdbcType=SMALLINT, typeHandler=net.lab1024.sa.common.mybatis.typehandler.BooleanToSmallintTypeHandler}"
    echo ""
    echo "See: .agent/rules/technology/database/D03-postgresql-mybatis.md#mandatory-boolean-type-handling-smallint-mapping"
    echo ""
    exit 1
fi
