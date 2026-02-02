#!/bin/bash

# Mermaid 語法檢查器
# 版本: 1.0.0
# 功能: 掃描 Mermaid 圖表閉合標記錯誤

set -e

TARGET_DIR="${1:-docs/IGaming}"
MODE="${2:-scan}"

# 顏色輸出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 函數：掃描錯誤
scan_errors() {
    echo -e "${YELLOW}🔍 掃描 Mermaid 語法錯誤...${NC}"
    echo ""

    # 排除審計報告中的示例（這些是故意的錯誤示例）
    local text_count=$(grep -r '````text' "$TARGET_DIR" --include="*.md" 2>/dev/null | \
        grep -v "CORRECTION_REPORT.md" | \
        grep -v "IGaming_Documentation_Audit_Report.md" | \
        grep -v "DOCUMENTATION_AUDIT_REPORT.md" | \
        grep -v '缺少閉合標記' | \
        grep -v '問題.*使用' | \
        wc -l || echo "0")

    local yaml_count=$(grep -r '````yaml' "$TARGET_DIR" --include="*.md" 2>/dev/null | \
        grep -v "CORRECTION_REPORT.md" | \
        grep -v "IGaming_Documentation_Audit_Report.md" | \
        grep -v "DOCUMENTATION_AUDIT_REPORT.md" | \
        wc -l || echo "0")

    local markdown_count=$(grep -r '````markdown' "$TARGET_DIR" --include="*.md" 2>/dev/null | \
        grep -v "CORRECTION_REPORT.md" | \
        grep -v "LOGIC_ANALYSIS_REPORT.md" | \
        wc -l || echo "0")

    local total=$((text_count + yaml_count + markdown_count))

    echo "📊 掃描結果："
    echo "  - 總錯誤數: $total"
    echo "  - ````text: $text_count"
    echo "  - ````yaml: $yaml_count"
    echo "  - ````markdown: $markdown_count"
    echo ""

    if [ "$total" -gt 0 ]; then
        echo -e "${RED}🔴 發現 $total 處錯誤！${NC}"
        return 1
    else
        echo -e "${GREEN}✅ 未發現錯誤${NC}"
        return 0
    fi
}

# 主流程
echo "🚀 Mermaid 語法檢查器 v1.0.0"
echo "============================"
echo "目標目錄: $TARGET_DIR"
echo ""

case "$MODE" in
    scan)
        scan_errors
        ;;
    *)
        echo -e "${RED}❌ 未知模式: $MODE${NC}"
        echo "支持的模式: scan"
        exit 1
        ;;
esac
