#!/bin/bash
# scripts/detect-statediagram-br.sh
# Detect <br/> tags inside stateDiagram-v2 blocks in Markdown files
# stateDiagram-v2 does NOT support <br/> tags (unlike other Mermaid types)
#
# Usage: ./scripts/detect-statediagram-br.sh [directory]
# Exit: 0 = no errors, 1 = <br/> found in stateDiagram blocks
#
# Version: 1.0.0

set -euo pipefail

TARGET_DIR="${1:-docs/iGaming}"
ERROR_COUNT=0
ERROR_FILES=0
REPORT_FILE="/tmp/statediagram-errors-$(date +%Y%m%d-%H%M%S).txt"
ERROR_LIST="/tmp/statediagram-error-files.txt"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  stateDiagram <br/> 檢測工具"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "掃描目錄: $TARGET_DIR"
echo ""

> "$REPORT_FILE"
> "$ERROR_LIST"

TOTAL_FILES=0

while IFS= read -r file; do
    # Check if file contains stateDiagram
    if ! grep -q 'stateDiagram' "$file" 2>/dev/null; then
        continue
    fi
    TOTAL_FILES=$((TOTAL_FILES + 1))

    # Extract stateDiagram blocks and check for <br/>
    # Use awk to find content between ```mermaid (containing stateDiagram) and ```
    FILE_ERRORS=0
    IN_MERMAID=0
    IS_STATE=0
    LINE_NUM=0

    while IFS= read -r line; do
        LINE_NUM=$((LINE_NUM + 1))

        if [[ "$line" =~ ^\`\`\`mermaid ]]; then
            IN_MERMAID=1
            IS_STATE=0
            continue
        fi

        if [[ $IN_MERMAID -eq 1 ]] && [[ "$line" =~ stateDiagram ]]; then
            IS_STATE=1
            continue
        fi

        if [[ "$line" =~ ^\`\`\` ]] && [[ $IN_MERMAID -eq 1 ]]; then
            IN_MERMAID=0
            IS_STATE=0
            continue
        fi

        if [[ $IS_STATE -eq 1 ]] && echo "$line" | grep -qi '<br/>\|<br>' 2>/dev/null; then
            echo "❌ $file:$LINE_NUM: $line" >> "$REPORT_FILE"
            FILE_ERRORS=$((FILE_ERRORS + 1))
        fi
    done < "$file"

    if [[ $FILE_ERRORS -gt 0 ]]; then
        ERROR_FILES=$((ERROR_FILES + 1))
        ERROR_COUNT=$((ERROR_COUNT + FILE_ERRORS))
        echo "$file" >> "$ERROR_LIST"
        echo "❌ $file ($FILE_ERRORS errors)"
    fi

done < <(find "$TARGET_DIR" -name "*.md" -type f ! -path "*/archive/*" ! -path "*/backup-corrupted/*" 2>/dev/null | sort)

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  檢測完成"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "總文件數:     $TOTAL_FILES"
echo "錯誤文件數:   $ERROR_FILES"
echo "總錯誤數:     $ERROR_COUNT"

if [[ $ERROR_COUNT -gt 0 ]]; then
    echo ""
    echo "詳細報告已保存至: $REPORT_FILE"
    echo "錯誤文件清單已保存至: $ERROR_LIST"
    exit 1
else
    echo ""
    echo "✅ 無 stateDiagram <br/> 錯誤"
    exit 0
fi
