#!/bin/bash
# scripts/scan-broken-links.sh
# iGaming 文檔斷鏈掃描工具
# 用法: ./scripts/scan-broken-links.sh [目錄路徑]
# 範例: ./scripts/scan-broken-links.sh docs/iGaming/

set -e

TARGET_DIR="${1:-docs/iGaming}"
BROKEN_COUNT=0
TOTAL_LINKS=0

echo "🔍 掃描目錄: $TARGET_DIR"
echo "================================"

# 找出所有 Markdown 文件中的相對鏈接
while IFS= read -r file; do
    # 提取 [text](path) 格式的鏈接，排除 http/https 外部鏈接
    while IFS= read -r link; do
        if [[ -n "$link" && ! "$link" =~ ^https?:// && ! "$link" =~ ^# ]]; then
            ((TOTAL_LINKS++)) || true

            # 移除錨點 (#section)
            link_path="${link%%#*}"

            # 計算絕對路徑
            file_dir=$(dirname "$file")
            abs_path=$(cd "$file_dir" && realpath -m "$link_path" 2>/dev/null || echo "")

            # 檢查文件是否存在
            if [[ ! -f "$abs_path" && ! -d "$abs_path" ]]; then
                echo "❌ 斷鏈: $file"
                echo "   → $link"
                ((BROKEN_COUNT++)) || true
            fi
        fi
    done < <(grep -oE '\]\([^)]+\)' "$file" 2>/dev/null | sed 's/\](//;s/)$//' || true)
done < <(find "$TARGET_DIR" -name "*.md" -type f ! -path "*/backup-corrupted/*")

echo "================================"
echo "📊 掃描結果:"
echo "   總鏈接數: $TOTAL_LINKS"
echo "   斷鏈數: $BROKEN_COUNT"

if [[ $BROKEN_COUNT -gt 0 ]]; then
    echo "⚠️ 發現 $BROKEN_COUNT 個斷鏈"
    exit 1
else
    echo "✅ 所有鏈接有效"
    exit 0
fi
