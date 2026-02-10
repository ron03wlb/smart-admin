#!/bin/bash
# 檢測 SSOT 概念重複定義

CONCEPTS=(
    "可下注餘額|Playable Balance"
    "有效投注|Valid Turnover"
    "PII 定義|Personally Identifiable Information"
    "Blind Index"
    "Crypto-Shredding"
)

VIOLATIONS=0

for concept in "${CONCEPTS[@]}"; do
    echo "🔍 檢查概念: $concept"

    # 搜尋包含該概念定義的文件
    files=$(grep -rl "$concept" docs/iGaming --include="*.md" 2>/dev/null | grep -v "archive")

    # 統計定義出現次數
    count=$(echo "$files" | wc -l)

    # 如果出現超過 10 次，可能違規 (核心概念跨模組引用是合理的)
    if [ $count -gt 10 ]; then
        echo "⚠️  疑似違規: $concept 出現在 $count 個文件中"
        VIOLATIONS=$((VIOLATIONS + 1))
    fi
done

if [ $VIOLATIONS -eq 0 ]; then
    echo "✅ 無 SSOT 違規"
    exit 0
else
    echo "❌ 發現 $VIOLATIONS 個潛在 SSOT 違規"
    exit 1
fi
