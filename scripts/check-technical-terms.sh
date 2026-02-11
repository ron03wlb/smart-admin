#!/bin/bash
# Purpose: 驗證技術術語保持英文（未被誤譯）
# Usage: ./scripts/check-technical-terms.sh docs/iGaming/

TARGET_DIR="${1:-docs/iGaming}"
ERRORS=0

# 技術術語列表（應保持英文）
TECH_TERMS=(
  "Controller" "Service" "Manager" "Dao" "Repository"
  "ResponseDTO" "PageResult" "Option" "@Transactional"
  "Multi-Tenant" "Row-Level Security" "Session" "Token"
)

# 錯誤翻譯列表（不應出現）
BAD_TRANSLATIONS=(
  "控制器" "服務" "管理層" "數據訪問對象" "倉儲"
  "響應對象" "分頁結果" "可選類型" "事務注解"
  "多租戶" "行級安全" "會話" "令牌"
)

echo "檢查技術術語是否保持英文..."

for i in "${!BAD_TRANSLATIONS[@]}"; do
  BAD="${BAD_TRANSLATIONS[$i]}"
  GOOD="${TECH_TERMS[$i]}"

  FOUND=$(grep -rn --include="*.md" "$BAD" "$TARGET_DIR" 2>/dev/null)
  if [ -n "$FOUND" ]; then
    echo "❌ 錯誤：發現技術術語被誤譯為「$BAD」（應保持「$GOOD」）"
    echo "$FOUND"
    ERRORS=$((ERRORS + 1))
  fi
done

if [ $ERRORS -eq 0 ]; then
  echo "✅ 通過：所有技術術語保持英文"
  exit 0
else
  echo "❌ 失敗：發現 $ERRORS 個技術術語誤譯"
  exit 1
fi
