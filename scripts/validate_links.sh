#!/bin/bash
# 掃描所有 Markdown 文件中的斷裂鏈接

DOCS_DIR="docs/iGaming"
BROKEN_LINKS=0

echo "🔍 掃描 iGaming 文檔鏈接..."

find "$DOCS_DIR" -name "*.md" | while read file; do
    # 提取所有 Markdown 鏈接
    grep -oP '\[.*?\]\(\K[^)]+' "$file" 2>/dev/null | while read link; do
        # 忽略外部鏈接和錨點
        if [[ $link == http* ]] || [[ $link == #* ]]; then
            continue
        fi

        # 解析相對路徑
        dir=$(dirname "$file")
        target="$dir/$link"

        # 移除錨點
        target="${target%%#*}"

        # 檢查文件是否存在
        if [[ ! -f "$target" ]]; then
            echo "❌ 斷裂鏈接: $file -> $link"
            BROKEN_LINKS=$((BROKEN_LINKS + 1))
        fi
    done
done

# 輸出結果
if [ $BROKEN_LINKS -eq 0 ]; then
    echo "✅ 所有鏈接有效"
    exit 0
else
    echo "❌ 發現 $BROKEN_LINKS 個斷裂鏈接"
    exit 1
fi
