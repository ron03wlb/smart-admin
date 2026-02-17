#!/bin/bash
# validate-mermaid.sh
# 驗證 iGaming 文檔中的 Mermaid 圖表語法
#
# 關鍵規則（來自 docs/ralph/guardrails.md P2）:
#   - 所有圖表類型（graph, flowchart, sequenceDiagram等）: 使用 <br/> 換行
#   - 例外: stateDiagram-v2 不能使用 <br/>, 應使用多行 note 區塊
#
# 使用方式:
#   bash scripts/validate-mermaid.sh <file_or_directory>
#
# Exit Codes:
#   0 - PASS (所有 Mermaid 語法正確)
#   1 - FAIL (發現語法錯誤)

set -uo pipefail

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 錯誤計數器
ERROR_COUNT=0
FILES_CHECKED=0

# 檢測文件中的 Mermaid 語法
check_file() {
    local file="$1"

    # 跳過非 Markdown 文件
    if [[ ! "$file" =~ \.md$ ]]; then
        return 0
    fi

    # 跳過 source-archive (READ-ONLY per P1 guardrail)
    if [[ "$file" == *"/source-archive/"* ]]; then
        return 0
    fi

    ((FILES_CHECKED++))

    local file_errors=0
    local in_mermaid=0
    local in_statediagram=0
    local line_num=0

    # 逐行讀取文件
    while IFS= read -r line; do
        ((line_num++))

        # 檢測 Mermaid 代碼塊開始
        if [[ "$line" =~ ^\`\`\`mermaid ]]; then
            in_mermaid=1
            in_statediagram=0
            continue
        fi

        # 檢測 Mermaid 代碼塊結束
        if [[ "$line" =~ ^\`\`\` ]] && [[ $in_mermaid -eq 1 ]]; then
            in_mermaid=0
            in_statediagram=0
            continue
        fi

        # 在 Mermaid 區塊內
        if [[ $in_mermaid -eq 1 ]]; then
            # 檢測 stateDiagram-v2
            if [[ "$line" =~ ^[[:space:]]*stateDiagram-v2 ]]; then
                in_statediagram=1
            fi

            # P2 Rule: stateDiagram-v2 不能使用 <br/>
            if [[ $in_statediagram -eq 1 ]] && [[ "$line" =~ \<br/?\> ]]; then
                echo -e "${RED}[ERROR]${NC} $file:$line_num"
                echo -e "  stateDiagram-v2 cannot use <br/> tags"
                echo -e "  Line: ${YELLOW}$line${NC}"
                echo -e "  Suggestion: Use multi-line note blocks instead:"
                echo -e "    ${GREEN}note right of StateA${NC}"
                echo -e "    ${GREEN}    Line 1${NC}"
                echo -e "    ${GREEN}    Line 2${NC}"
                echo -e "    ${GREEN}end note${NC}"
                echo ""
                ((file_errors++))
            fi

            # P2 Rule: 其他圖表類型應使用 <br/>, 不使用 \n
            # 注意：這個檢測較複雜，需要排除字符串內的 \n
            # 簡化版：檢測 ["...\n..."] 模式
            if [[ $in_statediagram -eq 0 ]] && [[ "$line" =~ \[\".*\\n.*\"\] ]]; then
                echo -e "${YELLOW}[WARNING]${NC} $file:$line_num"
                echo -e "  Found \\n in node label (should use <br/>)"
                echo -e "  Line: ${YELLOW}$line${NC}"
                echo -e "  Suggestion: Replace \\n with <br/>"
                echo -e "    Example: ${GREEN}A[Line 1<br/>Line 2]${NC}"
                echo ""
                # 這是警告，不增加錯誤計數
            fi
        fi
    done < "$file"

    if [[ $file_errors -gt 0 ]]; then
        echo -e "${RED}✗ FAIL${NC}: $file ($file_errors Mermaid syntax errors)"
        ((ERROR_COUNT += file_errors))
        return 1
    else
        echo -e "${GREEN}✓ PASS${NC}: $file"
        return 0
    fi
}

# 主函數
main() {
    if [[ $# -eq 0 ]]; then
        echo "Usage: $0 <file_or_directory> [file_or_directory...]"
        echo ""
        echo "Examples:"
        echo "  $0 docs/iGaming/architecture/03_Game/Game_Integration.md"
        echo "  $0 docs/iGaming/architecture/"
        exit 1
    fi

    echo "=================================================="
    echo "Mermaid Syntax Validation"
    echo "=================================================="
    echo ""
    echo "Rules (from guardrails.md P2):"
    echo "  1. graph/flowchart/sequence/class diagrams: Use <br/> for line breaks"
    echo "  2. stateDiagram-v2: Do NOT use <br/>, use multi-line note blocks"
    echo ""

    # 處理所有輸入參數
    for target in "$@"; do
        if [[ -f "$target" ]]; then
            # 單個文件
            check_file "$target"
        elif [[ -d "$target" ]]; then
            # 目錄：遞歸處理所有 .md 文件
            while IFS= read -r -d '' file; do
                check_file "$file"
            done < <(find "$target" -name "*.md" -type f -print0)
        else
            echo -e "${RED}[ERROR]${NC} Path not found: $target"
            ((ERROR_COUNT++))
        fi
    done

    echo ""
    echo "=================================================="

    if [[ $FILES_CHECKED -eq 0 ]]; then
        echo -e "${GREEN}✓ PASS${NC}: No files to check"
        exit 0
    fi

    echo "Statistics:"
    echo "  Files checked: $FILES_CHECKED"
    echo "  Syntax errors: $ERROR_COUNT"
    echo ""

    if [[ $ERROR_COUNT -eq 0 ]]; then
        echo -e "${GREEN}✓ PASS${NC}: All Mermaid diagrams use correct syntax"
        exit 0
    else
        echo -e "${RED}✗ FAIL${NC}: Found $ERROR_COUNT Mermaid syntax errors"
        echo ""
        echo "Reference: docs/ralph/guardrails.md (P2)"
        exit 1
    fi
}

# 執行主函數
main "$@"
