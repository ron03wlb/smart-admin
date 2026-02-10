#!/bin/bash
# scripts/measure-business-completeness.sh
# Measure business completeness of requirements documents
# Each requirements file should contain: Business Value + Success Metrics + Acceptance Criteria
#
# Usage: ./scripts/measure-business-completeness.sh
# Exit: 0 = target met (≥90%), 1 = below target

set -euo pipefail

REQ_DIR="docs/iGaming/requirements"
TOTAL=0
COMPLETE=0
INCOMPLETE_FILES=()

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Requirements Business Completeness Check"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [[ ! -d "$REQ_DIR" ]]; then
    echo "Directory not found: $REQ_DIR"
    echo "Skipping check"
    exit 0
fi

while IFS= read -r file; do
    TOTAL=$((TOTAL + 1))
    SCORE=0

    # Check for Business Value (various formats)
    if grep -qiE '(business value|business impact|business benefit|business objective|value proposition)' "$file" 2>/dev/null; then
        SCORE=$((SCORE + 1))
    fi

    # Check for Success Metrics / KPI
    if grep -qiE '(success metric|success criteria|KPI|key performance|measurement|measurable)' "$file" 2>/dev/null; then
        SCORE=$((SCORE + 1))
    fi

    # Check for Acceptance Criteria
    if grep -qiE '(acceptance criteria|acceptance condition|definition of done|requirements? summary|functional requirement)' "$file" 2>/dev/null; then
        SCORE=$((SCORE + 1))
    fi

    # Need at least 2 out of 3 to be considered complete
    if [[ $SCORE -ge 2 ]]; then
        COMPLETE=$((COMPLETE + 1))
    else
        INCOMPLETE_FILES+=("$file (score: $SCORE/3)")
    fi

done < <(find "$REQ_DIR" -name "*.md" -type f ! -name "README.md" ! -name "INDEX.md" ! -path "*/archive/*" 2>/dev/null | sort)

# Calculate percentage
if [[ $TOTAL -gt 0 ]]; then
    PCT=$((COMPLETE * 100 / TOTAL))
else
    PCT=0
fi

TARGET=90

echo ""
echo "Total requirements files: $TOTAL"
echo "Complete (≥2/3 criteria): $COMPLETE"
echo "Completeness: ${PCT}%"
echo "Target: ≥${TARGET}%"
echo ""

if [[ ${#INCOMPLETE_FILES[@]} -gt 0 ]]; then
    echo "Incomplete files (${#INCOMPLETE_FILES[@]}):"
    for f in "${INCOMPLETE_FILES[@]}"; do
        echo "  - $f"
    done
    echo ""
fi

if [[ $PCT -ge $TARGET ]]; then
    echo "✅ Business completeness meets target (${PCT}% ≥ ${TARGET}%)"
    exit 0
else
    echo "❌ Business completeness below target (${PCT}% < ${TARGET}%)"
    exit 1
fi
