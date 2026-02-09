#!/bin/bash
# scripts/validate-architecture-completeness.sh
# Verify Architecture documents contain complete technical content and back-references
#
# Usage: ./scripts/validate-architecture-completeness.sh
# Exit: 0 = all complete, 1 = missing content or back-references
#
# Spec: docs/iGaming/quality-reports/README.md
# Version: 1.0.0

set -euo pipefail

ARCH_DIR="docs/iGaming/architecture"
TOTAL_FILES=0
MISSING_BACKREF=0
MISSING_FILES=()

# Coverage counters
HAS_JAVA=0
HAS_SQL=0
HAS_YAML=0
HAS_MERMAID=0
HAS_BACKREF=0

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  架構層技術完整性檢查"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "掃描目錄: $ARCH_DIR"
echo ""

if [[ ! -d "$ARCH_DIR" ]]; then
    echo "⚠️  目錄不存在: $ARCH_DIR"
    echo "✅ 跳過檢查"
    exit 0
fi

while IFS= read -r file; do
    TOTAL_FILES=$((TOTAL_FILES + 1))
    CONTENT=$(cat "$file" 2>/dev/null || true)

    # Check for Java code blocks
    if echo "$CONTENT" | grep -q '```java' 2>/dev/null; then
        HAS_JAVA=$((HAS_JAVA + 1))
    fi

    # Check for SQL schema
    if echo "$CONTENT" | grep -qiE '(CREATE TABLE|CREATE INDEX|ALTER TABLE)' 2>/dev/null; then
        HAS_SQL=$((HAS_SQL + 1))
    fi

    # Check for YAML config
    if echo "$CONTENT" | grep -qE '```ya?ml' 2>/dev/null; then
        HAS_YAML=$((HAS_YAML + 1))
    fi

    # Check for Mermaid diagrams
    if echo "$CONTENT" | grep -q '```mermaid' 2>/dev/null; then
        HAS_MERMAID=$((HAS_MERMAID + 1))
    fi

    # Check for back-reference to requirements
    if echo "$CONTENT" | grep -qE '> \*\*Business Requirements\*\*|> \*\*需求文檔\*\*|requirements/' 2>/dev/null; then
        HAS_BACKREF=$((HAS_BACKREF + 1))
    else
        MISSING_BACKREF=$((MISSING_BACKREF + 1))
        MISSING_FILES+=("$file")
        echo "⚠️  Missing backref: $file"
    fi

done < <(find "$ARCH_DIR" -name "*.md" -type f ! -path "*/archive/*" ! -name "README.md" ! -name "INDEX.md" 2>/dev/null | sort)

# Calculate coverage percentages
if [[ $TOTAL_FILES -gt 0 ]]; then
    JAVA_PCT=$((HAS_JAVA * 100 / TOTAL_FILES))
    SQL_PCT=$((HAS_SQL * 100 / TOTAL_FILES))
    YAML_PCT=$((HAS_YAML * 100 / TOTAL_FILES))
    MERMAID_PCT=$((HAS_MERMAID * 100 / TOTAL_FILES))
    BACKREF_PCT=$((HAS_BACKREF * 100 / TOTAL_FILES))
else
    JAVA_PCT=0
    SQL_PCT=0
    YAML_PCT=0
    MERMAID_PCT=0
    BACKREF_PCT=0
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  覆蓋率指標"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "總文件數:       $TOTAL_FILES"
echo ""

# Report with pass/fail indicators
report_metric() {
    local label="$1"
    local actual="$2"
    local target="$3"
    local count="$4"
    if [[ $actual -ge $target ]]; then
        echo "✅ $label: ${actual}% (${count}/${TOTAL_FILES}) [目標: ≥${target}%]"
    else
        echo "❌ $label: ${actual}% (${count}/${TOTAL_FILES}) [目標: ≥${target}%]"
    fi
}

report_metric "Java 代碼塊" "$JAVA_PCT" 70 "$HAS_JAVA"
report_metric "SQL Schema" "$SQL_PCT" 40 "$HAS_SQL"
report_metric "YAML Config" "$YAML_PCT" 30 "$HAS_YAML"
report_metric "Mermaid 圖表" "$MERMAID_PCT" 70 "$HAS_MERMAID"
report_metric "需求回溯引用" "$BACKREF_PCT" 100 "$HAS_BACKREF"

ERRORS=0

# Check coverage thresholds
if [[ $JAVA_PCT -lt 70 ]]; then ERRORS=$((ERRORS + 1)); fi
if [[ $SQL_PCT -lt 40 ]]; then ERRORS=$((ERRORS + 1)); fi
if [[ $YAML_PCT -lt 30 ]]; then ERRORS=$((ERRORS + 1)); fi
if [[ $MERMAID_PCT -lt 70 ]]; then ERRORS=$((ERRORS + 1)); fi
if [[ $MISSING_BACKREF -gt 0 ]]; then ERRORS=$((ERRORS + 1)); fi

echo ""
if [[ $ERRORS -gt 0 ]]; then
    if [[ $MISSING_BACKREF -gt 0 ]]; then
        echo "缺少回溯引用的文件 ($MISSING_BACKREF):"
        for f in "${MISSING_FILES[@]}"; do
            echo "  → $f"
        done
        echo ""
    fi
    echo "❌ 架構完整性不合格 ($ERRORS 項指標未達標)"
    exit 1
else
    echo "✅ 架構層技術完整性合格"
    exit 0
fi
