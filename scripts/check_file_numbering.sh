#!/bin/bash
# 檢查文件編號是否符合 XX-YY 規範

VIOLATIONS=0

find docs/iGaming -name "*.md" | grep -E "/[0-9]{2}_" | while read file; do
    basename=$(basename "$file")

    # 檢查格式: XX-YY_*.md
    if ! [[ $basename =~ ^[0-9]{2}-[0-9]{2}_.*\.md$ ]]; then
        # 排除 README 和特殊文件
        if [[ $basename != "README.md" ]] && [[ $basename != "INDEX.md" ]]; then
            echo "❌ 編號不符: $file"
            VIOLATIONS=$((VIOLATIONS + 1))
        fi
    fi
done

if [ $VIOLATIONS -eq 0 ]; then
    echo "✅ 所有文件編號符合規範"
    exit 0
else
    echo "❌ 發現 $VIOLATIONS 個編號違規"
    exit 1
fi
