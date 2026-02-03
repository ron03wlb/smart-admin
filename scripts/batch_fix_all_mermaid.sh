#!/bin/bash
# 批量修復所有 iGaming 文檔中的 Mermaid 換行問題
# Phase 2.4: P2 全量修復

set -e

echo "開始批量修復 iGaming Mermaid 圖表..."

# 找出所有包含 sequenceDiagram 的檔案（排除已處理的 P0/P1 檔案）
files_with_sequence=$(grep -rl "sequenceDiagram" docs/iGaming --include="*.md" | grep -v "reconciliation.md" | grep -v "02-03_Reconciliation_System.md" | grep -v "01-security.md" | grep -v "02-04-01_Flowcharts_and_Sequences.md")

total_files=0
modified_files=0
total_note_fixes=0
total_node_fixes=0

for file in $files_with_sequence; do
    total_files=$((total_files + 1))
    echo "處理 [$total_files]: $file"

    # 執行修復並捕獲輸出
    output=$(py scripts/fix_mermaid_newlines_v2.py "$file" 2>&1)

    if [ $? -eq 0 ]; then
        # 解析修復統計
        note_fixes=$(echo "$output" | grep -oP '(?<=sequenceDiagram Note 區塊修復: )\d+' || echo "0")
        node_fixes=$(echo "$output" | grep -oP '(?<=Graph/Flowchart 節點修復: )\d+' || echo "0")

        if [ "$note_fixes" -gt 0 ] || [ "$node_fixes" -gt 0 ]; then
            modified_files=$((modified_files + 1))
            total_note_fixes=$((total_note_fixes + note_fixes))
            total_node_fixes=$((total_node_fixes + node_fixes))
            echo "  ✓ 修復完成: Note blocks: $note_fixes, Graph nodes: $node_fixes"
        else
            echo "  ⊘ 無需修復"
        fi
    else
        echo "  ✗ 修復失敗"
        echo "$output"
    fi
done

echo ""
echo "批量修復完成！"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "總檔案數:      $total_files"
echo "修改檔案數:    $modified_files"
echo "Note 修復總數: $total_note_fixes"
echo "節點修復總數:  $total_node_fixes"
echo "總修復數:      $((total_note_fixes + total_node_fixes))"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
