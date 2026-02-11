#!/bin/bash
# 批量修復 stateDiagram <br/> 標籤
# 用途：掃描所有 iGaming 文檔並自動修復 stateDiagram 中的 <br/> 錯誤

set -e

# 顏色輸出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 解析參數
DRY_RUN=false
VERIFY=false
STRATEGY="auto"

while [[ $# -gt 0 ]]; do
    case $1 in
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --verify)
            VERIFY=true
            shift
            ;;
        --strategy)
            STRATEGY="$2"
            shift 2
            ;;
        *)
            echo "未知參數: $1"
            echo "用法: $0 [--dry-run] [--verify] [--strategy auto|simple|note]"
            exit 1
            ;;
    esac
done

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  批量修復 stateDiagram <br/> 標籤${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "策略:   $STRATEGY"
echo "模式:   $([ "$DRY_RUN" = true ] && echo "DRY-RUN（僅預覽）" || echo "EXECUTE（實際修復）")"
echo "驗證:   $([ "$VERIFY" = true ] && echo "啟用" || echo "禁用")"
echo ""

# 階段 1: 檢測錯誤文件
echo -e "${YELLOW}階段 1: 檢測錯誤文件...${NC}"
echo ""

if [ "$DRY_RUN" = true ]; then
    # Dry-run 模式：直接掃描所有 stateDiagram 文件
    ERROR_FILES=$(grep -rl "stateDiagram-v2" docs/iGaming --include="*.md" | \
                  xargs grep -l "<br/>" 2>/dev/null || true)
else
    # 執行模式：使用檢測腳本生成錯誤清單
    if ./scripts/detect-statediagram-br.sh docs/iGaming > /dev/null 2>&1; then
        echo -e "${GREEN}✓ 未發現錯誤，無需修復${NC}"
        exit 0
    fi

    # 讀取錯誤文件清單
    if [ -f /tmp/statediagram-error-files.txt ]; then
        ERROR_FILES=$(cat /tmp/statediagram-error-files.txt)
    else
        echo -e "${RED}✗ 錯誤: 未找到錯誤文件清單${NC}"
        exit 1
    fi
fi

if [ -z "$ERROR_FILES" ]; then
    echo -e "${GREEN}✓ 未發現錯誤，無需修復${NC}"
    exit 0
fi

ERROR_FILE_COUNT=$(echo "$ERROR_FILES" | wc -l)
echo -e "${YELLOW}發現 $ERROR_FILE_COUNT 個文件需要修復${NC}"
echo ""

# 階段 2: 執行修復
echo -e "${YELLOW}階段 2: 執行修復...${NC}"
echo ""

# 構建 Python 命令參數
PYTHON_ARGS=""
if [ "$DRY_RUN" = true ]; then
    PYTHON_ARGS="$PYTHON_ARGS --dry-run"
fi
if [ "$VERIFY" = true ]; then
    PYTHON_ARGS="$PYTHON_ARGS --verify"
fi
PYTHON_ARGS="$PYTHON_ARGS --strategy $STRATEGY"

# 調用 Python 修復腳本
python3 scripts/fix-statediagram-br-tags.py $PYTHON_ARGS $ERROR_FILES

exit_code=$?

# 階段 3: 生成報告
echo ""
echo -e "${YELLOW}階段 3: 生成修復報告...${NC}"
echo ""

REPORT_FILE="/tmp/statediagram-fix-report-$(date +%Y%m%d-%H%M%S).md"

cat > "$REPORT_FILE" <<EOF
# stateDiagram <br/> 修復報告

**生成時間**: $(date)
**策略**: $STRATEGY
**模式**: $([ "$DRY_RUN" = true ] && echo "DRY-RUN" || echo "EXECUTE")
**驗證**: $([ "$VERIFY" = true ] && echo "啟用" || echo "禁用")

---

## 修復文件清單

EOF

echo "$ERROR_FILES" | while read -r file; do
    echo "- \`$file\`" >> "$REPORT_FILE"
done

cat >> "$REPORT_FILE" <<EOF

---

## 修復策略說明

### 方案 A (simple): 簡化標籤
- **適用**: P2 文件（錯誤數 ≤ 5）
- **轉換規則**: 去除 \`<br/>\` 和分隔線，提取關鍵詞
- **範例**: \`Win Request<br/>━━━━━<br/>Credit Balance\` → \`Win Request - Credit\`

### 方案 B (note): 移至 Note 區塊
- **適用**: P0/P1 文件（核心業務文檔）
- **轉換規則**: 簡化 transition label，詳細信息移至 \`note\` 區塊
- **範例**: 保留完整信息（包括分隔線 \`━━━━━\`）

---

## 下一步行動

EOF

if [ "$DRY_RUN" = true ]; then
    cat >> "$REPORT_FILE" <<EOF
1. 查看修復預覽，確認修復策略正確
2. 執行實際修復：\`./scripts/batch-fix-statediagram-br.sh --verify\`
3. 驗證修復結果：\`./scripts/validate-mermaid.sh docs/iGaming\`
EOF
else
    cat >> "$REPORT_FILE" <<EOF
1. 查看修復後的文件：\`git diff\`
2. 驗證 Mermaid 語法：\`./scripts/validate-mermaid.sh docs/iGaming\`
3. 提交修改：\`git add . && git commit -m "fix(docs): 修復 stateDiagram Mermaid 語法錯誤"\`
EOF
fi

echo -e "${GREEN}修復報告已保存至: $REPORT_FILE${NC}"
echo ""

# 顯示報告摘要
if [ "$DRY_RUN" = false ]; then
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}  修復完成${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo "請執行以下命令驗證修復結果："
    echo "  ./scripts/validate-mermaid.sh docs/iGaming"
    echo ""
    echo "如果驗證通過，請提交修改："
    echo "  git add docs/iGaming"
    echo "  git commit -m \"fix(docs): 修復 stateDiagram Mermaid 語法錯誤\""
    echo ""
fi

exit $exit_code
