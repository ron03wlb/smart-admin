#!/bin/bash
# Ralph 進度監控腳本
# 使用方式: ./monitor-ralph.sh

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "           📊 Ralph 進度監控面板"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# 運行狀態
RALPH_RUNNING=$(ps aux | grep -c "ralph-igaming-docs.sh" || echo "0")
CLAUDE_RUNNING=$(ps aux | grep -c "claude" | grep -v grep || echo "0")

if [ "$RALPH_RUNNING" -gt 0 ] || [ "$CLAUDE_RUNNING" -gt 0 ]; then
    echo "🟢 狀態: 運行中"
else
    echo "🔴 狀態: 已停止"
fi

# 任務統計
DONE=$(grep -c '^- \[x\]' docs/ralph/progress.md 2>/dev/null || echo "0")
TODO=$(grep -c '^- \[ \]' docs/ralph/progress.md 2>/dev/null || echo "0")
STUCK=$(grep -c '^- \[!\]' docs/ralph/progress.md 2>/dev/null || echo "0")
TOTAL=$((DONE + TODO + STUCK))
if [ "$TOTAL" -gt 0 ]; then
    PCT=$((DONE * 100 / TOTAL))
else
    PCT=0
fi

echo ""
echo "📝 任務進度:"
echo "   已完成: $DONE / $TOTAL ($PCT%)"
echo "   待完成: $TODO"
echo "   卡住:   $STUCK"

# Git 提交統計
COMMITS_1H=$(git log --oneline --since="1 hour ago" | wc -l)
COMMITS_TODAY=$(git log --oneline --since="today" | wc -l)

echo ""
echo "📦 Git 提交:"
echo "   過去 1 小時: $COMMITS_1H 個"
echo "   今天總計:   $COMMITS_TODAY 個"

# 最新日志文件
LATEST_LOG=$(ls -t docs/ralph/logs/*.log 2>/dev/null | head -1)
if [ -n "$LATEST_LOG" ]; then
    echo ""
    echo "📄 最新日志: $(basename "$LATEST_LOG")"

    # 最後一次迭代信息
    LAST_ITERATION=$(grep -E "Iteration #[0-9]+" "$LATEST_LOG" | tail -1)
    if [ -n "$LAST_ITERATION" ]; then
        echo "   $LAST_ITERATION"
    fi
fi

# Phase 狀態
echo ""
echo "🎯 Phase 狀態:"
if grep -q "Phase 7A COMPLETE" docs/ralph/progress.md; then
    echo "   ✅ Phase 7A: 完成 (19 個文件)"
else
    echo "   ⏳ Phase 7A: 進行中"
fi

if grep -q "Phase 7B COMPLETE" docs/ralph/progress.md; then
    echo "   ✅ Phase 7B: 完成 (28 個文件)"
else
    echo "   ⏳ Phase 7B: 進行中"
fi

if grep -q "Phase 7C.*COMPLETE" docs/ralph/progress.md; then
    echo "   ✅ Phase 7C: 完成 (術語 + 引用)"
else
    echo "   ⏳ Phase 7C: 進行中"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "💡 提示:"
echo "   查看實時日志: tail -f $LATEST_LOG"
echo "   查看提交歷史: git log --oneline --since='1 hour ago'"
echo "   檢查質量指標: bash scripts/measure-business-completeness.sh"
echo ""
