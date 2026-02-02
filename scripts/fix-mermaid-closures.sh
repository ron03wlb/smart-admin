#!/bin/bash

# IGaming Mermaid 圖表閉合標記修正腳本
# 目標：將 ````text / ````yaml / ````markdown 替換為 ```
# 版本: 1.0.0
# 日期: 2026-02-02

set -e

TARGET_DIR="docs/IGaming"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
BACKUP_DIR="docs/IGaming-backup-${TIMESTAMP}"

echo "🚀 Mermaid 閉合標記修正腳本"
echo "============================"
echo ""

# 檢查目標目錄是否存在
if [ ! -d "$TARGET_DIR" ]; then
    echo "❌ 錯誤：目標目錄 $TARGET_DIR 不存在"
    exit 1
fi

# 創建備份
echo "📦 創建備份：$BACKUP_DIR"
cp -r "$TARGET_DIR" "$BACKUP_DIR"
echo "✅ 備份完成"
echo ""

# 修正前統計
echo "📊 修正前統計："
TEXT_COUNT_BEFORE=$(grep -r '````text' "$TARGET_DIR" --include="*.md" 2>/dev/null | wc -l || echo "0")
YAML_COUNT_BEFORE=$(grep -r '````yaml' "$TARGET_DIR" --include="*.md" 2>/dev/null | wc -l || echo "0")
MARKDOWN_COUNT_BEFORE=$(grep -r '````markdown' "$TARGET_DIR" --include="*.md" 2>/dev/null | wc -l || echo "0")
TOTAL_BEFORE=$((TEXT_COUNT_BEFORE + YAML_COUNT_BEFORE + MARKDOWN_COUNT_BEFORE))

echo "  - 總錯誤數: $TOTAL_BEFORE"
echo "  - ````text: $TEXT_COUNT_BEFORE"
echo "  - ````yaml: $YAML_COUNT_BEFORE"
echo "  - ````markdown: $MARKDOWN_COUNT_BEFORE"
echo ""

if [ "$TOTAL_BEFORE" -eq 0 ]; then
    echo "✅ 未發現需要修正的錯誤"
    exit 0
fi

# 開始修正
echo "🔧 開始修正閉合標記..."

# 修正 ````text
echo "  - 修正 ````text..."
find "$TARGET_DIR" -name "*.md" -type f -exec sed -i 's/^````text$/```/g' {} \;

# 修正 ````yaml
echo "  - 修正 ````yaml..."
find "$TARGET_DIR" -name "*.md" -type f -exec sed -i 's/^````yaml$/```/g' {} \;

# 修正 ````markdown
echo "  - 修正 ````markdown..."
find "$TARGET_DIR" -name "*.md" -type f -exec sed -i 's/^````markdown$/```/g' {} \;

echo "✅ 修正完成！"
echo ""

# 修正後統計
echo "📊 修正後統計："
TEXT_COUNT_AFTER=$(grep -r '````text' "$TARGET_DIR" --include="*.md" 2>/dev/null | wc -l || echo "0")
YAML_COUNT_AFTER=$(grep -r '````yaml' "$TARGET_DIR" --include="*.md" 2>/dev/null | wc -l || echo "0")
MARKDOWN_COUNT_AFTER=$(grep -r '````markdown' "$TARGET_DIR" --include="*.md" 2>/dev/null | wc -l || echo "0")
TOTAL_AFTER=$((TEXT_COUNT_AFTER + YAML_COUNT_AFTER + MARKDOWN_COUNT_AFTER))

echo "  - 總錯誤數: $TOTAL_AFTER"
echo "  - ````text: $TEXT_COUNT_AFTER"
echo "  - ````yaml: $YAML_COUNT_AFTER"
echo "  - ````markdown: $MARKDOWN_COUNT_AFTER"
echo ""

# 生成變更報告
FIXED_COUNT=$((TOTAL_BEFORE - TOTAL_AFTER))
echo "📈 修正結果："
echo "  - 修正數量: $FIXED_COUNT"
echo "  - 成功率: $(awk "BEGIN {printf \"%.1f\", ($FIXED_COUNT/$TOTAL_BEFORE)*100}")%"
echo ""

# Git diff 統計（如果在 Git 倉庫中）
if git rev-parse --git-dir > /dev/null 2>&1; then
    echo "📊 Git 變更統計："
    git diff --shortstat "$TARGET_DIR" || echo "  - 無法獲取 Git 統計"
    echo ""
fi

echo "✅ 所有操作完成！"
echo ""
echo "💡 建議下一步："
echo "  1. 檢查變更：git diff docs/iGame"
echo "  2. 驗證修正：grep -r '````text' docs/iGame --include=\"*.md\""
echo "  3. 提交變更：git add docs/iGame && git commit -m \"fix(docs): 修正 Mermaid 閉合標記\""
echo ""
echo "🔄 如需回滾："
echo "  rm -rf $TARGET_DIR && cp -r $BACKUP_DIR $TARGET_DIR"
