#!/bin/bash
# 檢查文件是否超過大小限制

MAX_LINES=2500
LARGE_FILES=0

find docs/iGaming -name "*.md" -type f 2>/dev/null | while read file; do
    lines=$(wc -l < "$file" 2>/dev/null)

    if [ $lines -gt $MAX_LINES ]; then
        echo "⚠️  超大文件: $file ($lines lines, 限制 $MAX_LINES)"
        LARGE_FILES=$((LARGE_FILES + 1))
    fi
done

if [ $LARGE_FILES -le 5 ]; then
    echo "✅ 超大文件數量符合規範 (<= 5 個)"
    exit 0
else
    echo "❌ 超大文件數量過多 ($LARGE_FILES > 5)"
    exit 1
fi
