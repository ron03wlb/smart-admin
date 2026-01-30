#!/bin/bash

echo "=== .claude/skills 優化驗證 ==="
echo ""

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

ERRORS=0

# 1. 路徑引用檢查
echo "📍 Phase 1: 路徑引用"
if grep -q "business-logic" /Users/zhangxuanrong/Documents/Workspace/Java/smart-admin/.claude/skills/README.md; then
    echo -e "${RED}❌ 發現過時的 business-logic 路徑${NC}"
    ((ERRORS++))
else
    echo -e "${GREEN}✅ 無 business-logic 路徑引用${NC}"
fi

# 2. 文件完整性檢查
echo "📄 Phase 2: 文件完整性"
if [ -f "/Users/zhangxuanrong/Documents/Workspace/Java/smart-admin/.claude/skills/productivity/devops/cicd-pipeline-builder/README.md" ] && \
   [ -f "/Users/zhangxuanrong/Documents/Workspace/Java/smart-admin/.claude/skills/productivity/devops/db-migration-manager/README.md" ]; then
    echo -e "${GREEN}✅ 所有 README.md 已補齊${NC}"
else
    echo -e "${RED}❌ 缺少 README.md${NC}"
    ((ERRORS++))
fi

# 3. 目錄結構檢查
echo "🗂️  Phase 3: 目錄結構"
if [ -d "/Users/zhangxuanrong/Documents/Workspace/Java/smart-admin/.claude/.claude" ]; then
    echo -e "${RED}❌ 嵌套目錄仍存在${NC}"
    ((ERRORS++))
else
    echo -e "${GREEN}✅ 嵌套目錄已清理${NC}"
fi

# 4. 分類標籤檢查
echo "🏷️  Phase 4: 分類標籤"
P2_COUNT=$(grep "Total Skills" /Users/zhangxuanrong/Documents/Workspace/Java/smart-admin/.claude/skills/README.md | sed 's/.*P2: \([0-9]*\).*/\1/')
if [ "$P2_COUNT" = "15" ]; then
    echo -e "${GREEN}✅ P2 技能計數正確 (15)${NC}"
else
    echo -e "${RED}❌ P2 技能計數錯誤 ($P2_COUNT)${NC}"
    ((ERRORS++))
fi

echo ""
echo "======================================="
if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}✅ 所有驗證通過！${NC}"
    exit 0
else
    echo -e "${RED}❌ 發現 $ERRORS 個問題${NC}"
    exit 1
fi
