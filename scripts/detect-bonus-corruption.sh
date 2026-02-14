#!/bin/bash
# scripts/detect-bonus-corruption.sh
# iGaming 文檔 Bonus 損壞模式檢測工具
# 用法: ./scripts/detect-bonus-corruption.sh [目錄路徑]

set -e

TARGET_DIR="${1:-docs/iGaming}"
CORRUPTION_COUNT=0

echo "🔍 檢測 Bonus 損壞模式: $TARGET_DIR"
echo "================================"

# 損壞模式列表
PATTERNS=(
    "Bonus[0-9]+-Bonus[0-9]+"    # Bonus4-Bonus1 (應為 04-01)
    "2Bonus26"                    # 2Bonus26 (應為 2026)
    "BonusBonus"                  # BonusBonus (應為 00)
    "[0-9]Bonus[0-9]"            # 1Bonus0 (應為 100)
)

for pattern in "${PATTERNS[@]}"; do
    echo "🔎 檢查模式: $pattern"
    while IFS= read -r match; do
        if [[ -n "$match" ]]; then
            echo "❌ $match"
            ((CORRUPTION_COUNT++)) || true
        fi
    done < <(grep -rn "$pattern" "$TARGET_DIR" --include="*.md" \
             --exclude-dir="backup-corrupted" \
             --exclude-dir="archive" 2>/dev/null || true)
done

echo ""
echo "================================"
echo "📊 檢測結果: 發現 $CORRUPTION_COUNT 處損壞"

if [[ $CORRUPTION_COUNT -gt 0 ]]; then
    echo "⚠️ 請手動修復上述損壞位置"
    exit 1
else
    echo "✅ 無損壞模式"
    exit 0
fi
