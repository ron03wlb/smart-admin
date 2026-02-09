#!/bin/bash
# scripts/detect-mermaid-emoji.sh
# Detect emoji characters inside Mermaid code blocks
# Emoji can cause rendering issues in some Mermaid environments
#
# Usage: ./scripts/detect-mermaid-emoji.sh [directory]
# Exit: 0 = no emoji found, 1 = emoji detected
#
# Version: 1.1.0

set -euo pipefail

TARGET_DIR="${1:-docs/iGaming}"
ERROR_COUNT=0
ERROR_FILES=0

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Mermaid Emoji 檢測工具"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "掃描目錄: $TARGET_DIR"
echo ""

TOTAL_FILES=0

while IFS= read -r file; do
    if ! grep -q '```mermaid' "$file" 2>/dev/null; then
        continue
    fi
    TOTAL_FILES=$((TOTAL_FILES + 1))

    # Use awk to extract mermaid blocks with line numbers, then check for emoji with perl (one call per file)
    FILE_ERRORS=$(awk '
        /^```mermaid/ { in_mermaid=1; next }
        /^```/ && in_mermaid { in_mermaid=0; next }
        in_mermaid { print NR ":" $0 }
    ' "$file" | perl -ne '
        use utf8;
        binmode(STDIN, ":utf8");
        if (/[\x{1F600}-\x{1F64F}\x{1F300}-\x{1F5FF}\x{1F680}-\x{1F6FF}\x{1F1E0}-\x{1F1FF}\x{2600}-\x{26FF}\x{2700}-\x{27BF}\x{1F900}-\x{1F9FF}\x{1FA00}-\x{1FAFF}]/) {
            print;
            $count++;
        }
        END { exit($count ? 1 : 0) }
    ' 2>/dev/null) || true

    if [[ -n "$FILE_ERRORS" ]]; then
        COUNT=$(echo "$FILE_ERRORS" | wc -l | tr -d ' ')
        ERROR_FILES=$((ERROR_FILES + 1))
        ERROR_COUNT=$((ERROR_COUNT + COUNT))
        echo "$FILE_ERRORS" | while IFS= read -r line; do
            echo "❌ $file:$line"
        done
    fi

done < <(find "$TARGET_DIR" -name "*.md" -type f ! -path "*/archive/*" ! -path "*/source-archive/*" ! -path "*/backup-corrupted/*" 2>/dev/null | sort)

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  檢測完成"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "掃描文件數:   $TOTAL_FILES"
echo "問題文件數:   $ERROR_FILES"
echo "Emoji 總數:   $ERROR_COUNT"

if [[ $ERROR_COUNT -gt 0 ]]; then
    echo ""
    echo "❌ 發現 $ERROR_COUNT 個 emoji (可能導致 Mermaid 渲染問題)"
    exit 1
else
    echo ""
    echo "✅ 無 Mermaid emoji 問題"
    exit 0
fi
