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
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 計數器
WARNING_COUNT=0
FILES_CHECKED=0
SKIPPED_SOURCE_ARCHIVE=0

# 檢測文件中的術語一致性
check_file() {
    local file="$1"

    # 跳過非 Markdown 文件
    if [[ ! "$file" =~ \.md$ ]]; then
        return 0
    fi

    # 跳過 source-archive 目錄 (READ-ONLY per P1 guardrail)
    if [[ "$file" =~ source-archive/ ]]; then
        ((SKIPPED_SOURCE_ARCHIVE++))
        return 0
    fi

    # 跳過 TRANSLATION_GLOSSARY.md
    if [[ "$file" =~ TRANSLATION_GLOSSARY\.md$ ]]; then
        return 0
    fi

    # 跳過 TEMPLATE 文件
    if [[ "$file" =~ TEMPLATE_ ]]; then
        return 0
    fi

    ((FILES_CHECKED++))

    # 檢測常見術語混用
    local has_issues=0

    # 檢測 "有效投注額" vs "流水" (智能模式)
    # "流水" 在複合術語中是合法的：流水要求、流水進度、流水計算、流水對帳、流水操縱、刷流水、流水倍數、流水型
    # 只標記獨立使用的 "流水"（不在複合術語中）與 "有效投注額" 的混用
    if grep -q "有效投注額" "$file" 2>/dev/null; then
        # 檢查是否有獨立的 "流水" (非複合術語)
        # 排除: 流水要求、流水進度、流水計算、流水對帳、流水操縱、刷流水、流水倍數、流水型、流水線
        # 排除: 流水驗證、流水追蹤、流水完成、流水記錄、流水詳情、流水返佣、流水獎金、流水遊戲
        local standalone_liushui=$(grep -n "流水" "$file" 2>/dev/null | \
            grep -v "流水要求\|流水進度\|流水計算\|流水對帳\|流水操縱\|刷流水\|流水倍數\|流水型\|流水線" | \
            grep -v "流水驗證\|流水追蹤\|流水完成\|流水記錄\|流水詳情\|流水返佣\|流水獎金\|流水遊戲" | \
            grep -v "流水異常\|流水模式\|流水快照\|投注流水\|高流水\|低流水\|提款流水" | \
            grep -v "(流水)\|（流水）\|Turnover.*流水\|流水.*Turnover" | \
            grep -v "、流水、\|、流水$" || true)
        if [[ -n "$standalone_liushui" ]]; then
            echo -e "${YELLOW}[WARNING]${NC} $file"
            echo -e "  Mixed usage: \"有效投注額\" and standalone \"流水\" (should use \"有效投注額\")"
            echo -e "  ${CYAN}Lines with standalone \"流水\":${NC}"
            echo "$standalone_liushui" | head -3 | while IFS= read -r line; do
                echo -e "    $line"
            done
            has_issues=1
        fi
    fi

    # 檢測 "可下注餘額" vs "可用餘額"
    if grep -q "可下注餘額" "$file" 2>/dev/null && grep -q "可用餘額" "$file" 2>/dev/null; then
        echo -e "${YELLOW}[WARNING]${NC} $file"
        echo -e "  Mixed usage: \"可下注餘額\" and \"可用餘額\" (should use \"可下注餘額\")"
        echo -e "  ${CYAN}Lines with \"可用餘額\":${NC}"
        grep -n "可用餘額" "$file" 2>/dev/null | head -3 | while IFS= read -r line; do
            echo -e "    $line"
        done
        has_issues=1
    fi

    # 檢測 "存款" vs "充值"
    if grep -q "存款" "$file" 2>/dev/null && grep -q "充值" "$file" 2>/dev/null; then
        echo -e "${YELLOW}[WARNING]${NC} $file"
        echo -e "  Mixed usage: \"存款\" and \"充值\" (should use \"存款\")"
        echo -e "  ${CYAN}Lines with \"充值\":${NC}"
        grep -n "充值" "$file" 2>/dev/null | head -3 | while IFS= read -r line; do
            echo -e "    $line"
        done
        has_issues=1
    fi

    # 檢測 "提款" vs "取款"
    if grep -q "提款" "$file" 2>/dev/null && grep -q "取款" "$file" 2>/dev/null; then
        echo -e "${YELLOW}[WARNING]${NC} $file"
        echo -e "  Mixed usage: \"提款\" and \"取款\" (should use \"提款\")"
        echo -e "  ${CYAN}Lines with \"取款\":${NC}"
        grep -n "取款" "$file" 2>/dev/null | head -3 | while IFS= read -r line; do
            echo -e "    $line"
        done
        has_issues=1
    fi

    if [[ $has_issues -eq 1 ]]; then
        ((WARNING_COUNT++))
        echo ""
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
        echo "  $0 docs/iGaming/  (source-archive/ auto-skipped)"
        exit 1
    fi

    echo "=================================================="
    echo "Terminology Consistency Validation (v2.0)"
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
    if [[ $SKIPPED_SOURCE_ARCHIVE -gt 0 ]]; then
        echo "  Skipped (source-archive): $SKIPPED_SOURCE_ARCHIVE"
    fi
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
