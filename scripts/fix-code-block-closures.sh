#!/bin/bash

# IGaming 代碼塊閉合標記徹底修正腳本 v2.0
# 目標：將所有 ````text / ````yaml / ````markdown 替換為 ```
# 範圍：全 docs/IGaming/ 目錄（179 處錯誤）

set -e

TARGET_DIR="docs/IGaming"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
BACKUP_DIR="docs/IGaming-backup-comprehensive-${TIMESTAMP}"

echo "🚀 代碼塊閉合標記徹底修正腳本 v2.0"
echo "======================================="
echo ""
echo "目標目錄: $TARGET_DIR"
echo "預計修正: 179 處錯誤"
echo ""

# 檢查目標目錄
if [ ! -d "$TARGET_DIR" ]; then
    echo "❌ 錯誤：目標目錄 $TARGET_DIR 不存在"
    exit 1
fi

# 創建備份
echo "📦 創建完整備份：$BACKUP_DIR"
cp -r "$TARGET_DIR" "$BACKUP_DIR"
echo "✅ 備份完成"
echo ""

# 統計修正前的錯誤數量
echo "🔍 修正前錯誤統計："
TEXT_BEFORE=$(grep -r '^````text$' "$TARGET_DIR" --include="*.md" 2>/dev/null | wc -l || echo "0")
YAML_BEFORE=$(grep -r '^````yaml$' "$TARGET_DIR" --include="*.md" 2>/dev/null | wc -l || echo "0")
MARKDOWN_BEFORE=$(grep -r '^````markdown$' "$TARGET_DIR" --include="*.md" 2>/dev/null | wc -l || echo "0")
TOTAL_BEFORE=$((TEXT_BEFORE + YAML_BEFORE + MARKDOWN_BEFORE))

echo "  - ````text: $TEXT_BEFORE"
echo "  - ````yaml: $YAML_BEFORE"
echo "  - ````markdown: $MARKDOWN_BEFORE"
echo "  - 總計: $TOTAL_BEFORE"
echo ""

# 執行批量修正
echo "🔧 開始批量修正..."

# 修正 ````text → ```
find "$TARGET_DIR" -name "*.md" -type f -exec sed -i 's/^````text$/```/g' {} \; 2>/dev/null
echo "  ✓ 修正 ````text"

# 修正 ````yaml → ```
find "$TARGET_DIR" -name "*.md" -type f -exec sed -i 's/^````yaml$/```/g' {} \; 2>/dev/null
echo "  ✓ 修正 ````yaml"

# 修正 ````markdown → ```
find "$TARGET_DIR" -name "*.md" -type f -exec sed -i 's/^````markdown$/```/g' {} \; 2>/dev/null
echo "  ✓ 修正 ````markdown"

echo "✅ 批量修正完成！"
echo ""

# 統計修正後的錯誤數量
echo "🔍 修正後驗證："
TEXT_AFTER=$(grep -r '^````text$' "$TARGET_DIR" --include="*.md" 2>/dev/null | wc -l || echo "0")
YAML_AFTER=$(grep -r '^````yaml$' "$TARGET_DIR" --include="*.md" 2>/dev/null | wc -l || echo "0")
MARKDOWN_AFTER=$(grep -r '^````markdown$' "$TARGET_DIR" --include="*.md" 2>/dev/null | wc -l || echo "0")
TOTAL_AFTER=$((TEXT_AFTER + YAML_AFTER + MARKDOWN_AFTER))

echo "  - ````text: $TEXT_AFTER"
echo "  - ````yaml: $YAML_AFTER"
echo "  - ````markdown: $MARKDOWN_AFTER"
echo "  - 總計: $TOTAL_AFTER"
echo ""

# 計算修正數量
FIXED=$((TOTAL_BEFORE - TOTAL_AFTER))
echo "📊 修正統計："
echo "  - 修正前錯誤: $TOTAL_BEFORE"
echo "  - 修正後錯誤: $TOTAL_AFTER"
echo "  - 成功修正: $FIXED"
echo ""

if [ "$TOTAL_AFTER" -eq 0 ]; then
    echo "✅ 所有錯誤已修正！"
    echo ""
    echo "📝 下一步："
    echo "  1. 查看 Git 變更: git diff docs/IGaming/"
    echo "  2. 提交變更: git add . && git commit -m 'fix(docs): 修正 179 處代碼塊閉合標記錯誤'"
    echo "  3. 備份位置: $BACKUP_DIR"
else
    echo "⚠️  警告：仍有 $TOTAL_AFTER 處錯誤未修正"
    echo "請手動檢查這些文件"
fi
