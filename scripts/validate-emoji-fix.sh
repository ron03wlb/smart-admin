#!/bin/bash
#############################################################################
# Mermaid Emoji 修復驗證腳本
#############################################################################
# 用途: 驗證 Mermaid 圖表修復後的正確性
# 用法: ./validate-emoji-fix.sh <目標目錄>
# 範例: ./validate-emoji-fix.sh docs/iGaming/
#
# 驗證內容:
#   1. 檢查是否還存在 emoji（應為 0）
#   2. 驗證 Mermaid 語法（使用 Mermaid CLI）
#   3. 生成驗證報告
#############################################################################

set -euo pipefail

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Emoji Unicode 範圍
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

# 檢查 Mermaid CLI 是否安裝
MERMAID_CLI_AVAILABLE=false
if command -v mmdc &> /dev/null; then
    MERMAID_CLI_AVAILABLE=true
    echo -e "${GREEN}✓ Mermaid CLI 已安裝${NC}"
else
    echo -e "${YELLOW}⚠ Mermaid CLI 未安裝，將跳過語法驗證${NC}"
    echo -e "${YELLOW}  安裝命令: npm install -g @mermaid-js/mermaid-cli${NC}"
fi

echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}  Mermaid Emoji 修復驗證報告${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "目標目錄: ${BLUE}$TARGET_DIR${NC}"
echo -e "驗證時間: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

# 臨時文件
TEMP_MD_FILES=$(mktemp)
TEMP_EMOJI_RESULTS=$(mktemp)
TEMP_MERMAID_BLOCKS=$(mktemp)
TEMP_SYNTAX_ERRORS=$(mktemp)

trap "rm -f $TEMP_MD_FILES $TEMP_EMOJI_RESULTS $TEMP_MERMAID_BLOCKS $TEMP_SYNTAX_ERRORS" EXIT

# ═══════════════════════════════════════════════════════════════
# 階段 1: 檢查殘留 emoji
# ═══════════════════════════════════════════════════════════════
echo -e "${YELLOW}[1/3] 檢查 Mermaid 代碼塊中的殘留 emoji...${NC}"

find "$TARGET_DIR" -type f -name "*.md" -exec grep -l '```mermaid' {} \; > "$TEMP_MD_FILES"
TOTAL_MD_FILES=$(wc -l < "$TEMP_MD_FILES")

EMOJI_FOUND_COUNT=0

while IFS= read -r file; do
    # 提取 Mermaid 代碼塊並檢測 emoji
    EMOJI_IN_FILE=$(awk '
        /```mermaid/ { in_mermaid=1; block_start=NR+1; next }
        /```/ && in_mermaid { in_mermaid=0; next }
        in_mermaid {
            while (match($0, /[\x{2600}-\x{26FF}\x{2700}-\x{27BF}\x{1F300}-\x{1F9FF}]/)) {
                emoji = substr($0, RSTART, RLENGTH)
                printf "%s:%d:%s:%s\n", FILENAME, NR, emoji, $0
                $0 = substr($0, RSTART + RLENGTH)
            }
        }
    ' "$file" 2>/dev/null || true)

    if [ -n "$EMOJI_IN_FILE" ]; then
        EMOJI_FOUND_COUNT=$((EMOJI_FOUND_COUNT + 1))
        echo "$EMOJI_IN_FILE" >> "$TEMP_EMOJI_RESULTS"
        echo -e "  ${RED}✗${NC} $file - 發現殘留 emoji"
    fi
done < "$TEMP_MD_FILES"

echo ""

if [ $EMOJI_FOUND_COUNT -eq 0 ]; then
    echo -e "${GREEN}✓ 通過: 未發現 Mermaid 代碼塊中的 emoji${NC}"
else
    echo -e "${RED}✗ 失敗: 發現 $EMOJI_FOUND_COUNT 個文件仍包含 emoji${NC}"
    echo ""
    echo -e "${CYAN}殘留 emoji 詳情:${NC}"

    awk -F: '{
        file=$1
        line=$2
        emoji=$3
        text=$4
        files[file] = files[file] sprintf("    Line %d: %s (in: %s)\n", line, emoji, text)
        file_emoji_count[file]++
    }
    END {
        for (f in files) {
            printf "  %s (%d emoji):\n%s", f, file_emoji_count[f], files[f]
        }
    }' "$TEMP_EMOJI_RESULTS"
fi

echo ""

# ═══════════════════════════════════════════════════════════════
# 階段 2: Mermaid 語法驗證（使用 Mermaid CLI）
# ═══════════════════════════════════════════════════════════════
echo -e "${YELLOW}[2/3] 驗證 Mermaid 圖表語法...${NC}"

SYNTAX_ERROR_COUNT=0
VALIDATED_COUNT=0

if [ "$MERMAID_CLI_AVAILABLE" = true ]; then
    while IFS= read -r file; do
        # 提取所有 Mermaid 代碼塊
        awk '
            /```mermaid/ {
                in_mermaid=1
                block_num++
                block_content=""
                next
            }
            /```/ && in_mermaid {
                in_mermaid=0
                printf "BLOCK_START:%d\n%s\nBLOCK_END\n", block_num, block_content
                next
            }
            in_mermaid {
                block_content = block_content $0 "\n"
            }
        ' "$file" > "$TEMP_MERMAID_BLOCKS"

        # 驗證每個代碼塊
        CURRENT_BLOCK=""
        BLOCK_NUM=0
        IN_BLOCK=false

        while IFS= read -r line; do
            if [[ $line == BLOCK_START:* ]]; then
                BLOCK_NUM=$(echo "$line" | cut -d: -f2)
                CURRENT_BLOCK=""
                IN_BLOCK=true
            elif [[ $line == "BLOCK_END" ]]; then
                # 驗證這個代碼塊
                TEMP_MMD=$(mktemp --suffix=.mmd)
                echo "$CURRENT_BLOCK" > "$TEMP_MMD"

                if mmdc -i "$TEMP_MMD" -o /tmp/temp_output.svg -q 2>&1 | grep -q "Error"; then
                    SYNTAX_ERROR_COUNT=$((SYNTAX_ERROR_COUNT + 1))
                    echo -e "  ${RED}✗${NC} $file - Block $BLOCK_NUM 語法錯誤"
                    echo "$file:Block_$BLOCK_NUM" >> "$TEMP_SYNTAX_ERRORS"
                else
                    VALIDATED_COUNT=$((VALIDATED_COUNT + 1))
                fi

                rm -f "$TEMP_MMD" /tmp/temp_output.svg
                IN_BLOCK=false
            elif [ "$IN_BLOCK" = true ]; then
                CURRENT_BLOCK="${CURRENT_BLOCK}${line}\n"
            fi
        done < "$TEMP_MERMAID_BLOCKS"

    done < "$TEMP_MD_FILES"

    echo ""
    if [ $SYNTAX_ERROR_COUNT -eq 0 ]; then
        echo -e "${GREEN}✓ 通過: 所有 $VALIDATED_COUNT 個 Mermaid 代碼塊語法正確${NC}"
    else
        echo -e "${RED}✗ 失敗: 發現 $SYNTAX_ERROR_COUNT 個語法錯誤${NC}"
        echo ""
        echo -e "${CYAN}語法錯誤詳情:${NC}"
        cat "$TEMP_SYNTAX_ERRORS"
    fi
else
    echo -e "${YELLOW}⚠ 跳過: Mermaid CLI 未安裝${NC}"
fi

echo ""

# ═══════════════════════════════════════════════════════════════
# 階段 3: 生成驗證總結
# ═══════════════════════════════════════════════════════════════
echo -e "${YELLOW}[3/3] 生成驗證總結...${NC}"
echo ""

echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}  驗證總結${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"

printf "%-30s %10s\n" "檢查項" "結果"
echo "───────────────────────────────────────────────────────────────"

# 1. Emoji 檢查
if [ $EMOJI_FOUND_COUNT -eq 0 ]; then
    printf "%-30s ${GREEN}%10s${NC}\n" "Emoji 殘留檢查" "✓ 通過"
else
    printf "%-30s ${RED}%10s${NC}\n" "Emoji 殘留檢查" "✗ 失敗 ($EMOJI_FOUND_COUNT)"
fi

# 2. 語法驗證
if [ "$MERMAID_CLI_AVAILABLE" = true ]; then
    if [ $SYNTAX_ERROR_COUNT -eq 0 ]; then
        printf "%-30s ${GREEN}%10s${NC}\n" "Mermaid 語法驗證" "✓ 通過"
    else
        printf "%-30s ${RED}%10s${NC}\n" "Mermaid 語法驗證" "✗ 失敗 ($SYNTAX_ERROR_COUNT)"
    fi
else
    printf "%-30s ${YELLOW}%10s${NC}\n" "Mermaid 語法驗證" "⊘ 跳過"
fi

echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"

# 最終結果
if [ $EMOJI_FOUND_COUNT -eq 0 ] && [ $SYNTAX_ERROR_COUNT -eq 0 ]; then
    echo -e "${GREEN}✓ 驗證通過: 所有檢查均成功${NC}"
    exit 0
elif [ $EMOJI_FOUND_COUNT -gt 0 ]; then
    echo -e "${RED}✗ 驗證失敗: 發現殘留 emoji${NC}"
    echo -e "${YELLOW}建議: 重新運行修復腳本${NC}"
    echo "  ./scripts/fix-mermaid-emoji.py --strategy hybrid <file>"
    exit 1
elif [ $SYNTAX_ERROR_COUNT -gt 0 ]; then
    echo -e "${RED}✗ 驗證失敗: Mermaid 語法錯誤${NC}"
    echo -e "${YELLOW}建議: 手動檢查錯誤文件${NC}"
    echo "  使用 Mermaid Live Editor: https://mermaid.live/"
    exit 1
else
    echo -e "${GREEN}✓ 驗證完成（部分檢查跳過）${NC}"
    exit 0
fi
