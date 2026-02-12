#!/bin/bash
# Mermaid 語法驗證腳本
# 用途：使用 Mermaid CLI 驗證所有圖表的語法正確性

set -e

# 顏色輸出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 統計
total_files=0
validated_files=0
failed_files=0
total_diagrams=0

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  Mermaid 語法驗證工具${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# 檢查 Mermaid CLI 是否安裝
if ! command -v mmdc &> /dev/null; then
    echo -e "${YELLOW}⚠ 警告: 未安裝 Mermaid CLI（跳過語法驗證）${NC}"
    echo ""
    echo "請安裝 Mermaid CLI："
    echo "  npm install -g @mermaid-js/mermaid-cli"
    echo ""
    exit 0
fi

echo -e "${GREEN}✓ Mermaid CLI 已安裝${NC}"
echo ""

# 設置搜索目錄（默認為 docs/iGaming）
SEARCH_DIR="${1:-docs/iGaming}"

echo -e "${GREEN}掃描目錄: ${SEARCH_DIR}${NC}"
echo ""

# 創建臨時目錄
TEMP_DIR=$(mktemp -d)
trap "rm -rf $TEMP_DIR" EXIT

# 查找所有包含 Mermaid 的 Markdown 文件
echo -e "${YELLOW}階段 1: 查找 Mermaid 圖表...${NC}"
files_with_mermaid=$(grep -rl "```mermaid" "$SEARCH_DIR" --include="*.md" || true)

if [ -z "$files_with_mermaid" ]; then
    echo -e "${GREEN}✓ 未找到包含 Mermaid 圖表的文件${NC}"
    exit 0
fi

# 驗證每個文件
echo -e "${YELLOW}階段 2: 驗證 Mermaid 語法...${NC}"
echo ""

for file in $files_with_mermaid; do
    total_files=$((total_files + 1))

    # 提取所有 Mermaid 代碼塊
    awk '
        /```mermaid/ { in_mermaid=1; diagram=""; next }
        in_mermaid && /```/ {
            print diagram > "/tmp/mermaid-block-" NR ".mmd"
            in_mermaid=0
            diagram=""
            next
        }
        in_mermaid { diagram = diagram $0 "\n" }
    ' "$file"

    # 驗證提取的代碼塊
    diagram_files=$(ls /tmp/mermaid-block-*.mmd 2>/dev/null || true)

    if [ -n "$diagram_files" ]; then
        file_valid=true

        for diagram_file in $diagram_files; do
            total_diagrams=$((total_diagrams + 1))

            # 使用 mmdc 驗證語法（生成 SVG）
            if mmdc -i "$diagram_file" -o "$TEMP_DIR/output.svg" -q 2>/dev/null; then
                : # 驗證成功
            else
                file_valid=false
                echo -e "${RED}✗ $file${NC}"
                echo -e "${RED}  圖表驗證失敗: $diagram_file${NC}"

                # 顯示錯誤詳情
                mmdc -i "$diagram_file" -o "$TEMP_DIR/output.svg" 2>&1 | head -5 | while read -r line; do
                    echo -e "    ${YELLOW}$line${NC}"
                done
                echo ""
                break
            fi

            # 清理臨時文件
            rm -f "$diagram_file"
        done

        if $file_valid; then
            validated_files=$((validated_files + 1))
            echo -e "${GREEN}✓ $file${NC}"
        else
            failed_files=$((failed_files + 1))
        fi
    fi
done

# 總結報告
echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  驗證完成${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "總文件數:     $total_files"
echo "總圖表數:     $total_diagrams"
echo "驗證通過:     $validated_files"
echo "驗證失敗:     $failed_files"
echo ""

if [ $failed_files -eq 0 ]; then
    echo -e "${GREEN}✓ 所有 Mermaid 圖表語法正確${NC}"
    exit 0
else
    echo -e "${RED}✗ 發現 $failed_files 個文件存在語法錯誤${NC}"
    echo ""
    echo -e "${GREEN}修復建議:${NC}"
    echo "  1. 使用 Mermaid Live Editor 測試: https://mermaid.live/"
    echo "  2. 參考最佳實踐: .claude/skills/.../mermaid-best-practices.md"
    exit 1
fi
