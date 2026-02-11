#!/bin/bash
# Purpose: 驗證業務術語翻譯一致性（使用 TRANSLATION_GLOSSARY.md 標準）
# Usage: ./scripts/check-terminology-consistency-zh-tw.sh docs/iGaming/

TARGET_DIR="${1:-docs/iGaming}"
ERRORS=0

echo "檢查業務術語翻譯一致性..."

# 標準術語對照（來自 TRANSLATION_GLOSSARY.md）
declare -A TERM_MAP=(
  ["Valid Turnover"]="有效投注額"
  ["Playable Balance"]="可下注餘額"
  ["Self-Exclusion"]="自我排除"
  ["KYC"]="身份驗證"
  ["AML"]="反洗錢"
)

# 禁止的替代翻譯（不一致）
declare -A BAD_ALTERNATIVES=(
  ["有效投注額"]="流水|投注流水|有效流水"
  ["可下注餘額"]="可用餘額|可投注餘額"
  ["自我排除"]="自我隔離|自我禁入"
)

for TERM in "${!BAD_ALTERNATIVES[@]}"; do
  ALTERNATIVES="${BAD_ALTERNATIVES[$TERM]}"

  FOUND=$(grep -rn --include="*.md" -E "($ALTERNATIVES)" "$TARGET_DIR" 2>/dev/null | \
    grep -v "TRANSLATION_GLOSSARY.md")

  if [ -n "$FOUND" ]; then
    echo "❌ 錯誤：發現不一致的術語翻譯（應統一使用「$TERM」）"
    echo "$FOUND" | head -5
    ERRORS=$((ERRORS + 1))
  fi
done

if [ $ERRORS -eq 0 ]; then
  echo "✅ 通過：所有業務術語翻譯一致"
  exit 0
else
  echo "❌ 失敗：發現 $ERRORS 個術語不一致"
  exit 1
fi
