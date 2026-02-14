#!/bin/bash
# stateDiagram <br/> 標籤檢測腳本
# 用途：掃描所有 iGaming 文檔，檢測 stateDiagram 中的 <br/> 錯誤

set -e

# 顏色輸出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 統計
total_files=0
error_files=0
total_errors=0

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  stateDiagram <br/> 標籤檢測工具${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# 設置搜索目錄（默認為 docs/iGaming）
SEARCH_DIR="${1:-docs/iGaming}"

echo -e "${GREEN}掃描目錄: ${SEARCH_DIR}${NC}"
echo ""

# 臨時文件（存儲錯誤報告）
ERROR_REPORT="/tmp/statediagram-errors-$(date +%Y%m%d-%H%M%S).txt"
echo "# stateDiagram <br/> 錯誤報告" > "$ERROR_REPORT"
echo "生成時間: $(date)" >> "$ERROR_REPORT"
echo "" >> "$ERROR_REPORT"

# 查找所有包含 stateDiagram 的 Markdown 文件
echo -e "${YELLOW}階段 1: 查找 stateDiagram 文件...${NC}"
files_with_statediagram=$(grep -rl "stateDiagram-v2" "$SEARCH_DIR" --include="*.md" || true)

if [ -z "$files_with_statediagram" ]; then
    echo -e "${GREEN}✓ 未找到包含 stateDiagram-v2 的文件${NC}"
    exit 0
fi

# 檢測每個文件
echo -e "${YELLOW}階段 2: 檢測 <br/> 標籤...${NC}"
echo ""

for file in $files_with_statediagram; do
    total_files=$((total_files + 1))

    # 提取 stateDiagram 代碼塊並檢查 <br/> 標籤
    # 使用 awk 提取 ```mermaid ... stateDiagram-v2 ... ``` 代碼塊
    errors=$(awk '
        /```mermaid/ { in_mermaid=1; next }
        in_mermaid && /stateDiagram-v2/ { in_state=1; next }
        in_mermaid && in_state && /<br\/?>/ {
            print NR":"$0
            error_count++
        }
        /```/ {
            if (in_state) {
                in_state=0
                in_mermaid=0
            }
        }
        END { if (error_count > 0) exit 1; else exit 0 }
    ' "$file") || true

    if [ -n "$errors" ]; then
        error_files=$((error_files + 1))
        file_error_count=$(echo "$errors" | wc -l)
        total_errors=$((total_errors + file_error_count))

        echo -e "${RED}✗ $file${NC}"
        echo -e "${RED}  錯誤數: $file_error_count${NC}"

        # 顯示前 3 個錯誤
        echo "$errors" | head -3 | while IFS=: read -r line_num content; do
            echo -e "    ${YELLOW}Line $line_num:${NC} $(echo "$content" | sed 's/^[[:space:]]*//')"
        done

        if [ "$file_error_count" -gt 3 ]; then
            echo -e "    ${YELLOW}... 還有 $((file_error_count - 3)) 個錯誤${NC}"
        fi
        echo ""

        # 寫入錯誤報告
        echo "## $file" >> "$ERROR_REPORT"
        echo "錯誤數: $file_error_count" >> "$ERROR_REPORT"
        echo "" >> "$ERROR_REPORT"
        echo '```' >> "$ERROR_REPORT"
        echo "$errors" >> "$ERROR_REPORT"
        echo '```' >> "$ERROR_REPORT"
        echo "" >> "$ERROR_REPORT"
    fi
done

# 總結報告
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  檢測完成${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "總文件數:     $total_files"
echo "錯誤文件數:   $error_files"
echo "總錯誤數:     $total_errors"
echo ""

if [ $error_files -gt 0 ]; then
    echo -e "${YELLOW}詳細報告已保存至: $ERROR_REPORT${NC}"
    echo ""
    echo -e "${GREEN}修復建議:${NC}"
    echo "  1. 查看錯誤報告: cat $ERROR_REPORT"
    echo "  2. 執行自動修復: ./scripts/batch-fix-statediagram-br.sh"
    echo "  3. 手動修復參考: .claude/skills/.../mermaid-best-practices.md §4"
    echo ""

    # 生成文件清單（用於批量修復）
    echo "$files_with_statediagram" | while read -r file; do
        grep -l "<br/>" "$file" 2>/dev/null || true
    done | sort -u > /tmp/statediagram-error-files.txt

    echo -e "${GREEN}錯誤文件清單已保存至: /tmp/statediagram-error-files.txt${NC}"

    exit 1
else
    echo -e "${GREEN}✓ 所有文件通過檢測，無 <br/> 錯誤${NC}"
    exit 0
fi
