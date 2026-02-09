#!/bin/bash
# scripts/validate-mermaid.sh
# Validate Mermaid diagram syntax in Markdown files
# Uses mmdc (Mermaid CLI) if available, falls back to basic regex checks
#
# Usage: ./scripts/validate-mermaid.sh [directory]
# Exit: 0 = all pass, 1 = validation failures
#
# Version: 1.0.0

set -euo pipefail

TARGET_DIR="${1:-docs/iGaming}"
HAS_MMDC=false
TOTAL_FILES=0
TOTAL_DIAGRAMS=0
PASS_COUNT=0
FAIL_COUNT=0

if command -v mmdc &> /dev/null; then
    HAS_MMDC=true
fi

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Mermaid 語法驗證工具"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
if [[ "$HAS_MMDC" == "true" ]]; then
    echo "✓ Mermaid CLI 已安裝"
else
    echo "⚠️  Mermaid CLI 未安裝 (使用基本正則驗證)"
    echo "   安裝: npm install -g @mermaid-js/mermaid-cli"
fi
echo ""
echo "掃描目錄: $TARGET_DIR"
echo ""

# Valid Mermaid diagram types
VALID_TYPES="graph|flowchart|sequenceDiagram|classDiagram|stateDiagram|stateDiagram-v2|erDiagram|journey|gantt|pie|quadrantChart|requirementDiagram|gitGraph|mindmap|timeline|sankey|xychart|block"

TEMP_DIR=$(mktemp -d "${TMPDIR:-/tmp}/mermaid-validate.XXXXXX")
trap "rm -rf '$TEMP_DIR'" EXIT

while IFS= read -r file; do
    if ! grep -q '```mermaid' "$file" 2>/dev/null; then
        continue
    fi
    TOTAL_FILES=$((TOTAL_FILES + 1))

    FILE_PASS=true
    DIAGRAM_IDX=0

    # Extract mermaid blocks
    IN_MERMAID=0
    BLOCK=""

    while IFS= read -r line; do
        if [[ "$line" =~ ^\`\`\`mermaid ]]; then
            IN_MERMAID=1
            BLOCK=""
            continue
        fi

        if [[ "$line" =~ ^\`\`\` ]] && [[ $IN_MERMAID -eq 1 ]]; then
            IN_MERMAID=0
            DIAGRAM_IDX=$((DIAGRAM_IDX + 1))
            TOTAL_DIAGRAMS=$((TOTAL_DIAGRAMS + 1))

            # Validate this block
            if [[ -z "$BLOCK" ]]; then
                echo "✗ $file (diagram #$DIAGRAM_IDX): empty mermaid block"
                FILE_PASS=false
                continue
            fi

            # Check diagram type
            FIRST_LINE=$(echo "$BLOCK" | head -1 | xargs)
            if ! echo "$FIRST_LINE" | grep -qE "^($VALID_TYPES)" 2>/dev/null; then
                echo "✗ $file (diagram #$DIAGRAM_IDX): unknown diagram type: $FIRST_LINE"
                FILE_PASS=false
                continue
            fi

            # If mmdc available, do full validation
            if [[ "$HAS_MMDC" == "true" ]]; then
                TEMP_MDD="$TEMP_DIR/diagram-$DIAGRAM_IDX.mmd"
                echo "$BLOCK" > "$TEMP_MDD"
                if ! mmdc -i "$TEMP_MDD" -o "$TEMP_DIR/out.svg" -q 2>/dev/null; then
                    echo "✗ $file (diagram #$DIAGRAM_IDX): mmdc validation failed"
                    FILE_PASS=false
                fi
            fi

            continue
        fi

        if [[ $IN_MERMAID -eq 1 ]]; then
            BLOCK="${BLOCK}${line}
"
        fi
    done < "$file"

    # Check for unclosed mermaid block
    if [[ $IN_MERMAID -eq 1 ]]; then
        echo "✗ $file: unclosed mermaid code block"
        FILE_PASS=false
    fi

    if [[ "$FILE_PASS" == "true" ]]; then
        echo "✓ $file"
        PASS_COUNT=$((PASS_COUNT + 1))
    else
        FAIL_COUNT=$((FAIL_COUNT + 1))
    fi

done < <(find "$TARGET_DIR" -name "*.md" -type f ! -path "*/archive/*" ! -path "*/backup-corrupted/*" 2>/dev/null | sort)

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  驗證完成"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "總文件數:     $TOTAL_FILES"
echo "總圖表數:     $TOTAL_DIAGRAMS"
echo "驗證通過:     $PASS_COUNT"
echo "驗證失敗:     $FAIL_COUNT"

if [[ $FAIL_COUNT -gt 0 ]]; then
    echo ""
    echo "❌ $FAIL_COUNT 個文件驗證失敗"
    exit 1
else
    echo ""
    echo "✅ 所有 Mermaid 圖表語法正確"
    exit 0
fi
