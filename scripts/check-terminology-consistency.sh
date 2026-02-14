#!/bin/bash
# scripts/check-terminology-consistency.sh
# Check terminology consistency across iGaming documentation
# Ensures standardized terms are used consistently
#
# Usage: ./scripts/check-terminology-consistency.sh
# Exit: 0 = consistent (≥95%), 1 = violations found

set -euo pipefail

DOCS_DIR="docs/iGaming"
VIOLATIONS=0
TOTAL_CHECKS=0

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Terminology Consistency Check"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [[ ! -d "$DOCS_DIR" ]]; then
    echo "Directory not found: $DOCS_DIR"
    exit 0
fi

# Define term pairs: preferred_term | deprecated_alternatives (pipe-separated regex)
# Format: "PREFERRED|DEPRECATED_REGEX|DESCRIPTION"
TERM_RULES=(
    "Valid Turnover|有效投注額|流水|Chinese terms for Valid Turnover"
    "Playable Balance|可下注餘額|Chinese terms for Playable Balance"
    "self-exclusion|self exclusion|Hyphenation of self-exclusion"
    "multi-tenant|multi tenant|multitenant|Hyphenation of multi-tenant"
)

check_term() {
    local preferred="$1"
    local deprecated_pattern="$2"
    local description="$3"

    # Count files with deprecated terms (excluding source-archive)
    local count
    count=$(grep -rlE "$deprecated_pattern" "$DOCS_DIR/requirements/" "$DOCS_DIR/architecture/" 2>/dev/null | wc -l | tr -d ' ')

    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))

    if [[ $count -gt 0 ]]; then
        echo "  ⚠️  $description: $count files use deprecated term(s)"
        echo "     Preferred: \"$preferred\""
        VIOLATIONS=$((VIOLATIONS + 1))
    else
        echo "  ✅ $description: consistent"
    fi
}

echo ""
echo "Checking terminology rules..."
echo ""

for rule in "${TERM_RULES[@]}"; do
    IFS='|' read -r preferred deprecated1 rest <<< "$rule"
    # Build regex from remaining parts (everything after preferred)
    parts=()
    IFS='|' read -ra parts <<< "$rule"

    preferred="${parts[0]}"
    description="${parts[${#parts[@]}-1]}"

    # Build deprecated pattern from middle parts
    deprecated_parts=()
    for ((i=1; i<${#parts[@]}-1; i++)); do
        deprecated_parts+=("${parts[$i]}")
    done

    if [[ ${#deprecated_parts[@]} -gt 0 ]]; then
        deprecated_regex=$(IFS='|'; echo "${deprecated_parts[*]}")
        check_term "$preferred" "$deprecated_regex" "$description"
    fi
done

echo ""

# Calculate consistency
if [[ $TOTAL_CHECKS -gt 0 ]]; then
    CONSISTENT=$((TOTAL_CHECKS - VIOLATIONS))
    PCT=$((CONSISTENT * 100 / TOTAL_CHECKS))
else
    PCT=100
fi

TARGET=95

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Results"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Checks: $TOTAL_CHECKS"
echo "Violations: $VIOLATIONS"
echo "Consistency: ${PCT}%"
echo "Target: ≥${TARGET}%"

if [[ $PCT -ge $TARGET ]]; then
    echo "✅ Terminology consistency meets target"
    exit 0
else
    echo "❌ Terminology consistency below target"
    exit 1
fi
