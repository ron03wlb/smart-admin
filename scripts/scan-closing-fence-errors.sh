#!/bin/bash
# IGaming 文檔閉合標記錯誤掃描器
# 功能：檢測使用語言標識符作為閉合標記的錯誤（如 ```text 而非 ```）

set -e

TARGET_DIR="docs/IGaming"
OUTPUT_FILE="docs/IGaming/CLOSING_FENCE_ERRORS_REPORT.txt"
TEMP_FILE="/tmp/closing-fence-scan-$$.txt"

echo "🚀 開始掃描 IGaming 文檔..."
echo "📂 目標目錄: $TARGET_DIR"
echo ""

# 清空臨時文件
> "$TEMP_FILE"

# 統計變量
total_files=0
total_errors=0
text_count=0
yaml_count=0
markdown_count=0
sql_count=0
json_count=0

# 掃描所有 .md 文件（排除審計報告）
while IFS= read -r -d '' file; do
    # 排除審計報告和備份文件
    if [[ "$file" =~ REPORT|CORRECTION|backup ]]; then
        continue
    fi

    file_has_errors=false
    file_error_count=0

    # 讀取文件內容到數組
    mapfile -t lines < "$file"
    total_lines=${#lines[@]}

    # 遍歷每一行
    for ((i=0; i<total_lines; i++)); do
        line="${lines[$i]}"

        # 檢查是否為 ```language 模式（三個反引號 + 語言標識符）
        if [[ "$line" =~ ^\`\`\`(text|yaml|markdown|sql|json|python|java|bash|javascript)$ ]]; then
            language="${BASH_REMATCH[1]}"

            # 獲取前一行和後一行
            prev_line=""
            next_line=""

            if [ $i -gt 0 ]; then
                prev_line="${lines[$((i-1))]}"
            fi

            if [ $i -lt $((total_lines-1)) ]; then
                next_line="${lines[$((i+1))]}"
            fi

            # 判斷是否為閉合標記錯誤
            # 1. 前一行不為空（有內容）
            # 2. 後一行為空、分隔符（---）或標題（#開頭）

            is_error=false

            # 前一行有內容（去除空白後非空）
            prev_trimmed=$(echo "$prev_line" | xargs)
            if [ -n "$prev_trimmed" ]; then
                # 後一行為空、分隔符或標題
                next_trimmed=$(echo "$next_line" | xargs)

                if [ -z "$next_trimmed" ] || [[ "$next_trimmed" =~ ^--- ]] || [[ "$next_trimmed" =~ ^# ]]; then
                    is_error=true
                fi
            fi

            # 如果是錯誤，記錄
            if [ "$is_error" = true ]; then
                if [ "$file_has_errors" = false ]; then
                    file_has_errors=true
                    ((total_files++))
                    echo "文件: ${file#docs/IGaming/}" >> "$TEMP_FILE"
                fi

                ((file_error_count++))
                ((total_errors++))

                # 統計語言類型
                case "$language" in
                    text) ((text_count++)) ;;
                    yaml) ((yaml_count++)) ;;
                    markdown) ((markdown_count++)) ;;
                    sql) ((sql_count++)) ;;
                    json) ((json_count++)) ;;
                esac

                # 截取上下文（前一行內容，最多 50 字符）
                context=$(echo "$prev_trimmed" | cut -c1-50)

                echo "  Line $((i+1)): \`\`\`$language" >> "$TEMP_FILE"
                echo "    上下文: $context..." >> "$TEMP_FILE"
            fi
        fi
    done

    # 文件結束標記
    if [ "$file_has_errors" = true ]; then
        echo "  錯誤數: $file_error_count 處" >> "$TEMP_FILE"
        echo "" >> "$TEMP_FILE"
    fi

done < <(find "$TARGET_DIR" -name "*.md" -type f -print0)

# 生成報告
{
    echo "================================================================================"
    echo "IGaming 文檔閉合標記錯誤掃描報告"
    echo "================================================================================"
    echo ""
    echo "📊 掃描統計"
    echo "  - 受影響文件數: $total_files 個"
    echo "  - 總錯誤數: $total_errors 處"
    echo ""
    echo "📈 錯誤類型分布"

    if [ $total_errors -gt 0 ]; then
        if [ $text_count -gt 0 ]; then
            percentage=$(awk "BEGIN {printf \"%.1f\", ($text_count / $total_errors) * 100}")
            echo "  - \`\`\`text: $text_count 處 ($percentage%)"
        fi
        if [ $yaml_count -gt 0 ]; then
            percentage=$(awk "BEGIN {printf \"%.1f\", ($yaml_count / $total_errors) * 100}")
            echo "  - \`\`\`yaml: $yaml_count 處 ($percentage%)"
        fi
        if [ $markdown_count -gt 0 ]; then
            percentage=$(awk "BEGIN {printf \"%.1f\", ($markdown_count / $total_errors) * 100}")
            echo "  - \`\`\`markdown: $markdown_count 處 ($percentage%)"
        fi
        if [ $sql_count -gt 0 ]; then
            percentage=$(awk "BEGIN {printf \"%.1f\", ($sql_count / $total_errors) * 100}")
            echo "  - \`\`\`sql: $sql_count 處 ($percentage%)"
        fi
        if [ $json_count -gt 0 ]; then
            percentage=$(awk "BEGIN {printf \"%.1f\", ($json_count / $total_errors) * 100}")
            echo "  - \`\`\`json: $json_count 處 ($percentage%)"
        fi
    fi

    echo ""
    echo "================================================================================"
    echo "📁 詳細錯誤清單"
    echo "================================================================================"
    echo ""

    # 插入詳細錯誤清單
    cat "$TEMP_FILE"

    echo "================================================================================"
    echo "✅ 掃描完成"
    echo "================================================================================"
} > "$OUTPUT_FILE"

# 輸出到控制台
cat "$OUTPUT_FILE"

# 清理臨時文件
rm -f "$TEMP_FILE"

echo ""
echo "📄 報告已保存至: $OUTPUT_FILE"

# 返回結果
if [ $total_errors -gt 0 ]; then
    echo ""
    echo "⚠️  發現 $total_errors 處閉合標記錯誤"
    exit 1
else
    echo ""
    echo "✅ 未發現閉合標記錯誤"
    exit 0
fi
