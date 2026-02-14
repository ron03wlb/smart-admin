#!/bin/bash
#############################################################################
# Mermaid Emoji 批量修復腳本
#############################################################################
# 用途: 批量修復多個文件中的 Mermaid emoji 錯誤
# 用法: ./batch-fix-mermaid-emoji.sh [選項] <目標目錄>
#
# 選項:
#   --priority <P0|P1|P2>  僅處理指定優先級的文件
#   --strategy <策略>      修復策略 (css/text/hybrid)
#   --dry-run             預覽模式
#   --verify              驗證修復結果
#   --no-review           跳過手動審核
#   -v, --verbose         詳細輸出
#
# 範例:
#   ./batch-fix-mermaid-emoji.sh --priority P0 --strategy css docs/iGaming/
#   ./batch-fix-mermaid-emoji.sh --priority P2 --strategy text --no-review docs/iGaming/
#############################################################################

set -euo pipefail

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# 默認參數
PRIORITY=""
STRATEGY="text"
DRY_RUN=false
VERIFY=false
NO_REVIEW=false
VERBOSE=false
TARGET_DIR=""

# 腳本目錄
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 解析參數
while [[ $# -gt 0 ]]; do
    case $1 in
        --priority)
            PRIORITY="$2"
            shift 2
            ;;
        --strategy)
            STRATEGY="$2"
            shift 2
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --verify)
            VERIFY=true
            shift
            ;;
        --no-review)
            NO_REVIEW=true
            shift
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        -*)
            echo "未知選項: $1"
            exit 1
            ;;
        *)
            TARGET_DIR="$1"
            shift
            ;;
    esac
done

# 檢查目標目錄
if [ -z "$TARGET_DIR" ]; then
    echo -e "${RED}錯誤: 缺少目標目錄參數${NC}"
    echo "用法: $0 [選項] <目標目錄>"
    exit 1
fi

if [ ! -d "$TARGET_DIR" ]; then
    echo -e "${RED}錯誤: 目錄不存在: $TARGET_DIR${NC}"
    exit 1
fi

# 驗證策略
if [[ ! "$STRATEGY" =~ ^(css|text|hybrid)$ ]]; then
    echo -e "${RED}錯誤: 無效的策略: $STRATEGY${NC}"
    echo "有效策略: css, text, hybrid"
    exit 1
fi

# 驗證優先級
if [ -n "$PRIORITY" ] && [[ ! "$PRIORITY" =~ ^(P0|P1|P2)$ ]]; then
    echo -e "${RED}錯誤: 無效的優先級: $PRIORITY${NC}"
    echo "有效優先級: P0, P1, P2"
    exit 1
fi

echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}  Mermaid Emoji 批量修復${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "目標目錄: ${BLUE}$TARGET_DIR${NC}"
echo -e "優先級過濾: ${BLUE}${PRIORITY:-所有文件}${NC}"
echo -e "修復策略: ${BLUE}$STRATEGY${NC}"
echo -e "模式: ${BLUE}$([ "$DRY_RUN" = true ] && echo "預覽模式 (不修改文件)" || echo "修復模式")${NC}"
echo -e "驗證: ${BLUE}$([ "$VERIFY" = true ] && echo "啟用" || echo "跳過")${NC}"
echo -e "手動審核: ${BLUE}$([ "$NO_REVIEW" = true ] && echo "跳過" || echo "啟用")${NC}"
echo ""

# 臨時文件
TEMP_FILES_LIST=$(mktemp)
TEMP_PRIORITY_FILES=$(mktemp)
TEMP_FIX_REPORT=$(mktemp)

trap "rm -f $TEMP_FILES_LIST $TEMP_PRIORITY_FILES $TEMP_FIX_REPORT" EXIT

# ═══════════════════════════════════════════════════════════════
# 階段 1: 檢測受影響文件
# ═══════════════════════════════════════════════════════════════
echo -e "${YELLOW}[1/4] 檢測受影響文件...${NC}"

# 運行檢測腳本
"$SCRIPT_DIR/detect-mermaid-emoji.sh" "$TARGET_DIR" > "$TEMP_FILES_LIST" 2>&1 || true

# 從檢測結果中提取文件清單
grep -E "^  (✗|✓)" "$TEMP_FILES_LIST" | sed 's/.*✗ //' | awk '{print $1}' > "$TEMP_PRIORITY_FILES" || true

TOTAL_AFFECTED_FILES=$(wc -l < "$TEMP_PRIORITY_FILES")

if [ $TOTAL_AFFECTED_FILES -eq 0 ]; then
    echo -e "${GREEN}✓ 未發現需要修復的文件${NC}"
    exit 0
fi

echo -e "發現 ${RED}$TOTAL_AFFECTED_FILES${NC} 個受影響文件"
echo ""

# ═══════════════════════════════════════════════════════════════
# 階段 2: 優先級過濾（如果指定）
# ═══════════════════════════════════════════════════════════════
if [ -n "$PRIORITY" ]; then
    echo -e "${YELLOW}[2/4] 過濾 $PRIORITY 優先級文件...${NC}"

    TEMP_FILTERED=$(mktemp)

    case $PRIORITY in
        P0)
            # P0: 核心業務文件（5+ emoji 或關鍵路徑）
            grep -E "(MFA|VIP|Wallet|Bonus|Lifecycle|Seamless)" "$TEMP_PRIORITY_FILES" > "$TEMP_FILTERED" || true
            ;;
        P1)
            # P1: 重要功能文件（3-4 emoji 或次要功能）
            grep -E "(RBAC|Risk|Technical|OAuth)" "$TEMP_PRIORITY_FILES" > "$TEMP_FILTERED" || true
            ;;
        P2)
            # P2: 支持文檔（1-2 emoji 或輔助文檔）
            grep -vE "(MFA|VIP|Wallet|Bonus|Lifecycle|Seamless|RBAC|Risk|Technical|OAuth)" "$TEMP_PRIORITY_FILES" > "$TEMP_FILTERED" || true
            ;;
    esac

    mv "$TEMP_FILTERED" "$TEMP_PRIORITY_FILES"
    FILTERED_FILE_COUNT=$(wc -l < "$TEMP_PRIORITY_FILES")

    echo -e "過濾後剩餘 ${BLUE}$FILTERED_FILE_COUNT${NC} 個文件"
    echo ""

    if [ $FILTERED_FILE_COUNT -eq 0 ]; then
        echo -e "${YELLOW}⚠ 無符合條件的文件需要修復${NC}"
        exit 0
    fi
fi

# ═══════════════════════════════════════════════════════════════
# 階段 3: 批量修復文件
# ═══════════════════════════════════════════════════════════════
echo -e "${YELLOW}[3/4] 批量修復文件...${NC}"

FIXED_COUNT=0
ERROR_COUNT=0
SKIPPED_COUNT=0

while IFS= read -r file; do
    echo -e "${CYAN}處理文件:${NC} $file"

    # 構建修復命令
    FIX_CMD="python3 $SCRIPT_DIR/fix-mermaid-emoji.py --strategy $STRATEGY"

    if [ "$DRY_RUN" = true ]; then
        FIX_CMD="$FIX_CMD --dry-run"
    fi

    if [ "$VERIFY" = true ]; then
        FIX_CMD="$FIX_CMD --verify"
    fi

    if [ "$VERBOSE" = true ]; then
        FIX_CMD="$FIX_CMD -v"
    fi

    FIX_CMD="$FIX_CMD $file"

    # 執行修復
    if eval "$FIX_CMD" >> "$TEMP_FIX_REPORT" 2>&1; then
        FIXED_COUNT=$((FIXED_COUNT + 1))
        echo -e "  ${GREEN}✓${NC} 修復成功"

        # P0/P1 文件手動審核
        if [ "$NO_REVIEW" = false ] && [ -n "$PRIORITY" ] && [[ "$PRIORITY" =~ ^(P0|P1)$ ]]; then
            echo -e "  ${YELLOW}⏸ ${NC} 需要手動審核"
            echo ""
            echo -e "  文件: ${BLUE}$file${NC}"
            echo -e "  操作:"
            echo -e "    1. 在 Mermaid Live Editor 中打開: https://mermaid.live/"
            echo -e "    2. 檢查圖表渲染是否正確"
            echo -e "    3. 確認語義是否保留"
            echo ""
            read -p "  按 Enter 繼續，或輸入 'skip' 跳過後續審核: " REVIEW_INPUT

            if [[ "$REVIEW_INPUT" == "skip" ]]; then
                NO_REVIEW=true
            fi
        fi
    else
        ERROR_COUNT=$((ERROR_COUNT + 1))
        echo -e "  ${RED}✗${NC} 修復失敗"
    fi

    echo ""
done < "$TEMP_PRIORITY_FILES"

# ═══════════════════════════════════════════════════════════════
# 階段 4: 生成修復報告
# ═══════════════════════════════════════════════════════════════
echo -e "${YELLOW}[4/4] 生成修復報告...${NC}"
echo ""

echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}  批量修復統計${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"

printf "%-30s %10s\n" "統計項" "數量"
echo "───────────────────────────────────────────────────────────────"
printf "%-30s %10d\n" "總受影響文件" "$TOTAL_AFFECTED_FILES"

if [ -n "$PRIORITY" ]; then
    printf "%-30s %10d\n" "過濾後文件 ($PRIORITY)" "$FILTERED_FILE_COUNT"
fi

printf "%-30s ${GREEN}%10d${NC}\n" "成功修復" "$FIXED_COUNT"
printf "%-30s ${RED}%10d${NC}\n" "修復失敗" "$ERROR_COUNT"

echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"

# 保存詳細報告
REPORT_FILE="$SCRIPT_DIR/../MERMAID_EMOJI_FIX_REPORT_$(date +%Y%m%d_%H%M%S).md"
cat > "$REPORT_FILE" <<EOF
# Mermaid Emoji 批量修復報告

**生成時間**: $(date '+%Y-%m-%d %H:%M:%S')
**目標目錄**: $TARGET_DIR
**優先級過濾**: ${PRIORITY:-所有文件}
**修復策略**: $STRATEGY
**模式**: $([ "$DRY_RUN" = true ] && echo "預覽模式" || echo "修復模式")

## 統計摘要

- 總受影響文件: $TOTAL_AFFECTED_FILES
$([ -n "$PRIORITY" ] && echo "- 過濾後文件 ($PRIORITY): $FILTERED_FILE_COUNT")
- 成功修復: $FIXED_COUNT
- 修復失敗: $ERROR_COUNT

## 詳細日誌

\`\`\`
$(cat "$TEMP_FIX_REPORT")
\`\`\`

## 下一步操作

1. **驗證修復結果**:
   \`\`\`bash
   ./scripts/validate-emoji-fix.sh $TARGET_DIR
   \`\`\`

2. **手動審核 P0 文件**（如果適用）:
   - 在 Mermaid Live Editor 中打開: https://mermaid.live/
   - 確認圖表渲染正確
   - 檢查語義是否保留

3. **提交修改**:
   \`\`\`bash
   git add $TARGET_DIR
   git commit -m "fix(docs): 修復 Mermaid 圖表中的 emoji 錯誤

   - 修復 $FIXED_COUNT 個文件
   - 策略: $STRATEGY
   - 優先級: ${PRIORITY:-所有}

   Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
   \`\`\`
EOF

echo -e "${GREEN}✓ 詳細報告已保存:${NC} $REPORT_FILE"
echo ""

# 最終結果
if [ "$DRY_RUN" = true ]; then
    echo -e "${GREEN}✓ 預覽完成（文件未修改）${NC}"
    exit 0
elif [ $ERROR_COUNT -gt 0 ]; then
    echo -e "${RED}✗ 批量修復完成，但有 $ERROR_COUNT 個文件失敗${NC}"
    exit 1
else
    echo -e "${GREEN}✓ 批量修復完成！${NC}"
    exit 0
fi
