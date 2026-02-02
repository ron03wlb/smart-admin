#!/bin/bash
# 功能：檢測 IGaming 文檔中 stateDiagram 的 'end note' 錯誤

OUTPUT_DIR="docs/IGaming/mermaid-fix-reports"
mkdir -p "$OUTPUT_DIR"

TIMESTAMP=$(date +%Y%m%d-%H%M%S)
REPORT_FILE="$OUTPUT_DIR/mermaid-errors-$TIMESTAMP.md"

echo "# Mermaid 'end note' 錯誤檢測報告" > "$REPORT_FILE"
echo "**檢測時間**: $(date)" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# 統計變數
total_errors=0
affected_files=0

echo "## 📊 受影響文件清單" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# 查找所有包含 'end note' 的文件（排除報告和歸檔）
grep -r "end note" docs/IGaming --include="*.md" -l | \
  grep -v "mermaid-fix-reports" | \
  grep -v "CORRECTION_REPORT" | \
  grep -v "AUDIT_REPORT" | \
  while read -r file; do

    # 統計該文件中的錯誤數量
    error_count=$(grep -c "end note" "$file")

    if [ $error_count -gt 0 ]; then
      affected_files=$((affected_files + 1))
      total_errors=$((total_errors + error_count))

      echo "### ❌ \`$file\` - **$error_count 處錯誤**" >> "$REPORT_FILE"
      echo "" >> "$REPORT_FILE"

      # 顯示錯誤上下文（前2行 + 錯誤行 + 後1行）
      grep -n -B 2 -A 1 "end note" "$file" | head -20 >> "$REPORT_FILE"
      echo "" >> "$REPORT_FILE"
      echo "---" >> "$REPORT_FILE"
      echo "" >> "$REPORT_FILE"
    fi
done

# 生成統計摘要
echo "" >> "$REPORT_FILE"
echo "## 📈 統計摘要" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "| 指標 | 數值 |" >> "$REPORT_FILE"
echo "|------|------|" >> "$REPORT_FILE"
echo "| **受影響文件數** | $affected_files |" >> "$REPORT_FILE"
echo "| **錯誤總數 ('end note')** | $total_errors |" >> "$REPORT_FILE"
echo "| **錯誤類型** | stateDiagram note 語法錯誤 |" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

echo "## 🔧 修正建議" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "**錯誤模式**:" >> "$REPORT_FILE"
echo '```mermaid' >> "$REPORT_FILE"
echo "note right of State" >> "$REPORT_FILE"
echo "    Multi-line" >> "$REPORT_FILE"
echo "    Content" >> "$REPORT_FILE"
echo "end note    ← 錯誤：stateDiagram-v2 不支持此語法" >> "$REPORT_FILE"
echo '```' >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "**正確模式**:" >> "$REPORT_FILE"
echo '```mermaid' >> "$REPORT_FILE"
echo "note right of State : Multi-line content (合併為單行)" >> "$REPORT_FILE"
echo '```' >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

echo "✅ 檢測完成！報告已生成: $REPORT_FILE"
echo "   - 受影響文件: $affected_files"
echo "   - 錯誤總數: $total_errors"
