#!/bin/bash
#############################################################################
# Mermaid Emoji 檢測腳本
#############################################################################
# 用途: 檢測 Markdown 文件中 Mermaid 圖表的 emoji 使用情況
# 用法: ./detect-mermaid-emoji.sh <目標目錄>
# 範例: ./detect-mermaid-emoji.sh docs/iGaming/
#
# 輸出格式:
#   - 受影響文件清單（含行號）
#   - Emoji 類型統計
#   - 按優先級分類的文件清單
#############################################################################

set -euo pipefail

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Emoji Unicode 範圍（包含常見 emoji 區塊）
# U+2600-U+26FF: Miscellaneous Symbols
# U+2700-U+27BF: Dingbats
# U+1F300-U+1F9FF: Miscellaneous Symbols and Pictographs, Emoticons, Transport, etc.
EMOJI_PATTERN='[\x{2600}-\x{26FF}\x{2700}-\x{27BF}\x{1F300}-\x{1F9FF}]'

# 檢查參數
if [ $# -eq 0 ]; then
    echo -e "${RED}錯誤: 缺少目標目錄參數${NC}"
    echo "用法: $0 <目標目錄>"
    echo "範例: $0 docs/iGaming/"
    exit 1
fi

TARGET_DIR="$1"

if [ ! -d "$TARGET_DIR" ]; then
    echo -e "${RED}錯誤: 目錄不存在: $TARGET_DIR${NC}"
    exit 1
fi

echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}  Mermaid Emoji 檢測報告${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "目標目錄: ${BLUE}$TARGET_DIR${NC}"
echo -e "檢測時間: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

# 臨時文件
TEMP_FILES=$(mktemp)
TEMP_RESULTS=$(mktemp)
TEMP_EMOJI_STATS=$(mktemp)

trap "rm -f $TEMP_FILES $TEMP_RESULTS $TEMP_EMOJI_STATS" EXIT

# 查找所有包含 Mermaid 代碼塊的 Markdown 文件
echo -e "${YELLOW}[1/4] 掃描 Markdown 文件...${NC}"
find "$TARGET_DIR" -type f -name "*.md" -exec grep -l '```mermaid' {} \; > "$TEMP_FILES"

TOTAL_MD_FILES=$(wc -l < "$TEMP_FILES")
echo -e "找到 ${GREEN}$TOTAL_MD_FILES${NC} 個包含 Mermaid 的文件"
echo ""

# 檢測每個文件中的 emoji
echo -e "${YELLOW}[2/4] 檢測 Mermaid 代碼塊中的 emoji...${NC}"

AFFECTED_FILE_COUNT=0
TOTAL_EMOJI_COUNT=0

while IFS= read -r file; do
    # 提取 Mermaid 代碼塊並檢測 emoji
    EMOJI_FOUND=$(awk '
        /```mermaid/ { in_mermaid=1; block_start=NR+1; next }
        /```/ && in_mermaid { in_mermaid=0; next }
        in_mermaid {
            # 檢測 emoji（使用 Perl 正則）
            while (match($0, /[\x{2600}-\x{26FF}\x{2700}-\x{27BF}\x{1F300}-\x{1F9FF}]/)) {
                emoji = substr($0, RSTART, RLENGTH)
                printf "%s:%d:%s:%s\n", FILENAME, NR, emoji, $0
                $0 = substr($0, RSTART + RLENGTH)
            }
        }
    ' "$file" 2>/dev/null || true)

    if [ -n "$EMOJI_FOUND" ]; then
        AFFECTED_FILE_COUNT=$((AFFECTED_FILE_COUNT + 1))

        # 計算此文件的 emoji 數量
        FILE_EMOJI_COUNT=$(echo "$EMOJI_FOUND" | wc -l)
        TOTAL_EMOJI_COUNT=$((TOTAL_EMOJI_COUNT + FILE_EMOJI_COUNT))

        # 記錄結果
        echo "$EMOJI_FOUND" >> "$TEMP_RESULTS"

        # 顯示進度
        echo -e "  ${RED}✗${NC} $file (${FILE_EMOJI_COUNT} 個 emoji)"
    fi
done < "$TEMP_FILES"

echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}  檢測統計${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "受影響文件數: ${RED}$AFFECTED_FILE_COUNT${NC} / $TOTAL_MD_FILES"
echo -e "總 Emoji 實例: ${RED}$TOTAL_EMOJI_COUNT${NC}"
echo ""

if [ $AFFECTED_FILE_COUNT -eq 0 ]; then
    echo -e "${GREEN}✓ 未發現 Mermaid 圖表中的 emoji 使用${NC}"
    exit 0
fi

# Emoji 類型統計
echo -e "${YELLOW}[3/4] 分析 Emoji 類型...${NC}"

# 統計各種 emoji 的出現次數
awk -F: '{
    emoji=$3
    count[emoji]++
}
END {
    for (e in count) {
        printf "%s\t%d\n", e, count[e]
    }
}' "$TEMP_RESULTS" | sort -t$'\t' -k2 -rn > "$TEMP_EMOJI_STATS"

echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}  Emoji 類型統計（按頻率排序）${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
printf "%-10s %10s\n" "Emoji" "出現次數"
echo "───────────────────────────────────────────────────────────────"

while IFS=$'\t' read -r emoji count; do
    printf "%-10s %10d\n" "$emoji" "$count"
done < "$TEMP_EMOJI_STATS"

echo ""

# 按文件分組顯示詳細錯誤
echo -e "${YELLOW}[4/4] 生成詳細報告...${NC}"
echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}  受影響文件詳細清單${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"

# 按文件分組
awk -F: '{
    file=$1
    line=$2
    emoji=$3
    text=$4
    files[file] = files[file] sprintf("  Line %d: %s (in: %s)\n", line, emoji, text)
    file_emoji_count[file]++
}
END {
    for (f in files) {
        printf "\n%s (%d emoji):\n%s", f, file_emoji_count[f], files[f]
    }
}' "$TEMP_RESULTS"

echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}  優先級分類建議${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"

echo ""
echo -e "${RED}【P0 - 核心業務文件】${NC}（建議使用 CSS 樣式修復）"
echo "以下文件含 5+ emoji 或為核心業務邏輯："
awk -F: '{file=$1; count[file]++}
END {
    for (f in count) {
        if (count[f] >= 5 || f ~ /VIP|Wallet|Bonus|Lifecycle|MFA/) {
            printf "  - %s (%d emoji)\n", f, count[f]
        }
    }
}' "$TEMP_RESULTS"

echo ""
echo -e "${YELLOW}【P1 - 重要功能文件】${NC}（建議使用混合策略）"
echo "以下文件含 3-4 emoji："
awk -F: '{file=$1; count[file]++}
END {
    for (f in count) {
        if (count[f] >= 3 && count[f] < 5 && f !~ /VIP|Wallet|Bonus|Lifecycle|MFA/) {
            printf "  - %s (%d emoji)\n", f, count[f]
        }
    }
}' "$TEMP_RESULTS"

echo ""
echo -e "${GREEN}【P2 - 支持文檔】${NC}（建議使用純文字替換）"
echo "以下文件含 1-2 emoji："
awk -F: '{file=$1; count[file]++}
END {
    for (f in count) {
        if (count[f] < 3) {
            printf "  - %s (%d emoji)\n", f, count[f]
        }
    }
}' "$TEMP_RESULTS"

echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}  修復建議${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo "1. 使用自動修復腳本:"
echo "   ./scripts/fix-mermaid-emoji.py --strategy hybrid <file>"
echo ""
echo "2. P0 文件手動審核（保留視覺吸引力）:"
echo "   - 使用 CSS classDef 和 class 屬性"
echo "   - 範例: classDef rating5 fill:#4CAF50,stroke:#2E7D32"
echo ""
echo "3. P1/P2 文件自動修復（快速處理）:"
echo "   ./scripts/batch-fix-mermaid-emoji.sh --priority P2"
echo ""
echo "4. 驗證修復結果:"
echo "   ./scripts/validate-emoji-fix.sh docs/iGaming/"
echo ""
echo -e "${GREEN}檢測完成！${NC}"
echo ""
