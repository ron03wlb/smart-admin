#!/bin/bash
#
# Ralph Loop Monitoring System - Demo Script
#
# 此腳本演示監控系統的工作流程，模擬 3 次迭代
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RALPH_DIR="$SCRIPT_DIR/.."
METRICS_DIR="$RALPH_DIR/metrics"
DEMO_LOG="$RALPH_DIR/logs/demo-$(date +%Y%m%d-%H%M%S).log"

# 顏色
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

echo "════════════════════════════════════════════════════════"
echo "  Ralph Loop Monitoring System - 演示模式"
echo "════════════════════════════════════════════════════════"
echo ""
echo "此演示將模擬 3 次 Ralph 迭代，展示監控系統的工作流程"
echo ""

# 確保目錄存在
mkdir -p "$METRICS_DIR"
mkdir -p "$RALPH_DIR/logs"

# ===== 模擬迭代 1: 低使用量 =====
echo -e "${CYAN}═══ 迭代 #1: 低使用量 (10k tokens) ═══${NC}"
echo ""

# 模擬 Claude 輸出
MOCK_OUTPUT_1=$(cat <<'EOF'
Input tokens: 10000
Output tokens: 2500
Cached tokens: 5000
✅ Task completed: Created cross-reference from Risk_Strategy to Risk_System_Architecture
EOF
)

echo "1. 記錄 token 使用量..."
echo "$MOCK_OUTPUT_1" | node "$SCRIPT_DIR/usage-tracker.js" \
  --iteration=1 \
  --duration=30 \
  --log-file="$METRICS_DIR/demo-iter1.json"

echo ""
echo "   查看記錄的數據:"
cat "$METRICS_DIR/demo-iter1.json" | head -15
echo ""

# ===== 模擬迭代 2: 中等使用量 =====
echo -e "${CYAN}═══ 迭代 #2: 中等使用量 (25k tokens) ═══${NC}"
echo ""

MOCK_OUTPUT_2=$(cat <<'EOF'
Input tokens: 25000
Output tokens: 6000
Cached tokens: 12000
✅ Task completed: Enhanced Mermaid diagrams with proper <br/> tags
EOF
)

sleep 2
echo "2. 記錄 token 使用量..."
echo "$MOCK_OUTPUT_2" | node "$SCRIPT_DIR/usage-tracker.js" \
  --iteration=2 \
  --duration=45 \
  --log-file="$METRICS_DIR/demo-iter2.json"

echo ""
echo "   查看記錄的數據:"
cat "$METRICS_DIR/demo-iter2.json" | head -15
echo ""

# ===== 模擬迭代 3: 高使用量 =====
echo -e "${CYAN}═══ 迭代 #3: 高使用量 (50k tokens) ═══${NC}"
echo ""

MOCK_OUTPUT_3=$(cat <<'EOF'
Input tokens: 50000
Output tokens: 12000
Cached tokens: 20000
✅ Task completed: Validated all cross-references and fixed broken links
EOF
)

sleep 2
echo "3. 記錄 token 使用量..."
echo "$MOCK_OUTPUT_3" | node "$SCRIPT_DIR/usage-tracker.js" \
  --iteration=3 \
  --duration=60 \
  --log-file="$METRICS_DIR/demo-iter3.json"

echo ""

# ===== 配額檢查 (模擬 30 分鐘後) =====
echo -e "${YELLOW}═══════════════════════════════════════${NC}"
echo -e "${YELLOW}  配額檢查 (30 分鐘窗口)             ${NC}"
echo -e "${YELLOW}═══════════════════════════════════════${NC}"
echo ""

# 確保所有 metrics 文件都有最新時間戳
touch "$METRICS_DIR/demo-iter1.json"
touch "$METRICS_DIR/demo-iter2.json"
touch "$METRICS_DIR/demo-iter3.json"

echo "4. 聚合最近 30 分鐘的使用量..."
node "$SCRIPT_DIR/quota-manager.js" \
  --metrics-dir="$METRICS_DIR" \
  --window-minutes=60 > "$RALPH_DIR/quota.json"

echo ""
echo "   配額狀態:"
cat "$RALPH_DIR/quota.json" | head -35
echo ""

# ===== 閾值檢查 =====
echo -e "${CYAN}5. 檢查閾值...${NC}"
bash "$SCRIPT_DIR/threshold-checker.sh" 2>&1 | tail -3
THRESHOLD_STATUS=$?

echo ""
echo "   閾值檢查結果: exit code = $THRESHOLD_STATUS"
if [ $THRESHOLD_STATUS -eq 0 ]; then
  echo -e "   ${GREEN}✅ 安全 - 繼續運行${NC}"
elif [ $THRESHOLD_STATUS -eq 1 ]; then
  echo -e "   ${YELLOW}⏳ 建議休息${NC}"

  # 計算休息時長
  REST_MINUTES=$(node "$SCRIPT_DIR/rest-strategy.js")
  echo "   建議休息時長: $REST_MINUTES 分鐘"
elif [ $THRESHOLD_STATUS -eq 2 ]; then
  echo -e "   ${RED}🛑 嚴重 - 需要停止${NC}"
fi

echo ""

# ===== 摘要報告 =====
echo -e "${GREEN}════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}  演示完成 - 監控系統摘要                              ${NC}"
echo -e "${GREEN}════════════════════════════════════════════════════════${NC}"
echo ""

TOTAL_INPUT=$(node -p "
  const q = JSON.parse(require('fs').readFileSync('$RALPH_DIR/quota.json', 'utf8'));
  q.accumulated.input_tokens;
")

TOTAL_OUTPUT=$(node -p "
  const q = JSON.parse(require('fs').readFileSync('$RALPH_DIR/quota.json', 'utf8'));
  q.accumulated.output_tokens;
")

RISK_SCORE=$(node -p "
  const q = JSON.parse(require('fs').readFileSync('$RALPH_DIR/quota.json', 'utf8'));
  q.risk_score.toFixed(4);
")

RECOMMENDATION=$(node -p "
  const q = JSON.parse(require('fs').readFileSync('$RALPH_DIR/quota.json', 'utf8'));
  q.recommendation;
")

echo "總使用量 (3 次迭代):"
echo "  - Input tokens:  $TOTAL_INPUT"
echo "  - Output tokens: $TOTAL_OUTPUT"
echo "  - Risk score:    $RISK_SCORE"
echo "  - Recommendation: $RECOMMENDATION"
echo ""

echo "生成的文件:"
echo "  - Metrics: $METRICS_DIR/demo-iter*.json (3 個文件)"
echo "  - Quota:   $RALPH_DIR/quota.json"
echo "  - Logs:    $DEMO_LOG"
echo ""

echo -e "${CYAN}查看詳細數據:${NC}"
echo "  cat $RALPH_DIR/quota.json"
echo "  ls -la $METRICS_DIR/demo-iter*.json"
echo ""

echo -e "${GREEN}✅ 監控系統運行正常！${NC}"
echo ""

# ===== 清理提示 =====
echo -e "${YELLOW}清理演示數據 (可選):${NC}"
echo "  rm -f $METRICS_DIR/demo-iter*.json"
echo "  rm -f $RALPH_DIR/quota.json"
echo ""
