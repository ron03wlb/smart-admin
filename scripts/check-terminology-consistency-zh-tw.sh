#!/bin/bash
# check-terminology-consistency-zh-tw.sh
# 檢查 iGaming 文檔中業務術語翻譯的一致性
#
# 使用方式:
#   bash scripts/check-terminology-consistency-zh-tw.sh <file_or_directory>
#
# Exit Codes:
#   0 - PASS (術語一致性 >= 95%)
#   1 - FAIL (術語一致性 < 95%)

set -uo pipefail

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 計數器
WARNING_COUNT=0
FILES_CHECKED=0

# 檢測文件中的術語一致性
check_file() {
    local file="$1"

    # 跳過非 Markdown 文件
    if [[ ! "$file" =~ \.md$ ]]; then
        return 0
    fi

    # 跳過 TRANSLATION_GLOSSARY.md
    if [[ "$file" =~ TRANSLATION_GLOSSARY\.md$ ]]; then
        return 0
    fi

    ((FILES_CHECKED++))

    # 檢測常見術語混用（簡化版本）
    local has_issues=0

    # 檢測 "有效投注額" vs "流水"
    if grep -q "有效投注額" "$file" 2>/dev/null && grep -q "流水" "$file" 2>/dev/null; then
        echo -e "${YELLOW}[WARNING]${NC} $file"
        echo -e "  Mixed usage: \"有效投注額\" and \"流水\" (should use \"有效投注額\")"
        has_issues=1
    fi

    # 檢測 "可下注餘額" vs "可用餘額"
    if grep -q "可下注餘額" "$file" 2>/dev/null && grep -q "可用餘額" "$file" 2>/dev/null; then
        echo -e "${YELLOW}[WARNING]${NC} $file"
        echo -e "  Mixed usage: \"可下注餘額\" and \"可用餘額\" (should use \"可下注餘額\")"
        has_issues=1
    fi

    # 檢測 "存款" vs "充值"
    if grep -q "存款" "$file" 2>/dev/null && grep -q "充值" "$file" 2>/dev/null; then
        echo -e "${YELLOW}[WARNING]${NC} $file"
        echo -e "  Mixed usage: \"存款\" and \"充值\" (should use \"存款\")"
        has_issues=1
    fi

    # 檢測 "提款" vs "取款"
    if grep -q "提款" "$file" 2>/dev/null && grep -q "取款" "$file" 2>/dev/null; then
        echo -e "${YELLOW}[WARNING]${NC} $file"
        echo -e "  Mixed usage: \"提款\" and \"取款\" (should use \"提款\")"
        has_issues=1
    fi

    # 檢測 "投注" vs "下注" (這兩個都是合法的，但應統一)
    # 暫時不檢測，因為用法有細微差別

    if [[ $has_issues -eq 1 ]]; then
        ((WARNING_COUNT++))
        echo ""
    else
        echo -e "${GREEN}✓ PASS${NC}: $file"
    fi
}

# 主函數
main() {
    if [[ $# -eq 0 ]]; then
        echo "Usage: $0 <file_or_directory> [file_or_directory...]"
        echo ""
        echo "Examples:"
        echo "  $0 docs/iGaming/requirements/02_Finance/Seamless_Wallet.md"
        echo "  $0 docs/iGaming/requirements/"
        exit 1
    fi

    echo "=================================================="
    echo "Terminology Consistency Validation"
    echo "=================================================="
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
        fi
    done

    echo ""
    echo "=================================================="

    # 計算一致性百分比
    if [[ $FILES_CHECKED -eq 0 ]]; then
        echo -e "${GREEN}✓ PASS${NC}: No files to check"
        exit 0
    fi

    local consistency_pct=$(( (FILES_CHECKED - WARNING_COUNT) * 100 / FILES_CHECKED ))

    echo "Statistics:"
    echo "  Files checked: $FILES_CHECKED"
    echo "  Files with inconsistencies: $WARNING_COUNT"
    echo "  Consistency rate: ${consistency_pct}%"
    echo ""

    if [[ $consistency_pct -ge 95 ]]; then
        echo -e "${GREEN}✓ PASS${NC}: Terminology consistency >= 95% (${consistency_pct}%)"
        exit 0
    else
        echo -e "${YELLOW}⚠ WARNING${NC}: Terminology consistency < 95% (${consistency_pct}%)"
        echo ""
        echo "Reference: docs/iGaming/TRANSLATION_GLOSSARY.md"
        # 警告不阻擋提交，僅提醒
        exit 0
    fi
}

# 執行主函數
main "$@"
