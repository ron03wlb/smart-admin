#!/bin/bash
# Purpose: 驗證繁體中文編碼正確性（UTF-8，無亂碼）
# Usage: ./scripts/validate-zh-tw-encoding.sh docs/iGaming/

TARGET_DIR="${1:-docs/iGaming}"
ERRORS=0

echo "檢查繁體中文編碼..."

# 檢查文件編碼（應為 UTF-8）
while IFS= read -r -d '' file; do
  ENCODING=$(file -b --mime-encoding "$file")

  if [[ "$ENCODING" != "utf-8" && "$ENCODING" != "us-ascii" ]]; then
    echo "❌ 錯誤：$file 編碼為 $ENCODING（應為 UTF-8）"
    ERRORS=$((ERRORS + 1))
  fi
done < <(find "$TARGET_DIR" -type f -name "*.md" -print0)

# 檢查是否混用簡體中文（簡體標誌字符）
SIMPLIFIED_CHARS=$(grep -rn --include="*.md" -E "[\u4E00-\u9FA5]" "$TARGET_DIR" 2>/dev/null | \
  grep -E "(个|门|为|务|实|现|验|认|证|记|号|还|过|这|国)" | head -20)

if [ -n "$SIMPLIFIED_CHARS" ]; then
  echo "⚠️ 警告：可能混用簡體中文字符（應全部使用繁體中文）"
  echo "$SIMPLIFIED_CHARS"
  ERRORS=$((ERRORS + 1))
fi

if [ $ERRORS -eq 0 ]; then
  echo "✅ 通過：所有文件使用正確的 UTF-8 繁體中文編碼"
  exit 0
else
  echo "❌ 失敗：發現 $ERRORS 個編碼問題"
  exit 1
fi
