#!/bin/bash
# 閉合標記錯誤掃描器 v2
# 使用純 Bash 邏輯進行上下文分析

# set -e  # 移除以允許繼續掃描所有文件

TARGET_DIR="docs/IGaming"

echo "🚀 開始掃描 IGaming 文檔閉合標記錯誤..."
echo "📂 目標目錄: $TARGET_DIR"
echo ""

# 統計變量
total_files=0
total_errors=0
text_errors=0
yaml_errors=0
markdown_errors=0
sql_errors=0
json_errors=0

# 臨時文件存儲詳細信息
DETAILS_FILE="/tmp/closing-errors-details.txt"
> "$DETAILS_FILE"

# 找出所有 .md 文件
echo "📝 正在收集文件列表..."
readarray -t md_files < <(find "$TARGET_DIR" -name "*.md" -type f ! -name "*REPORT*" ! -name "*CORRECTION*" ! -name "*backup*" 2>/dev/null)

echo "找到 ${#md_files[@]} 個 Markdown 文件"
echo ""

# 處理每個文件
for file in "${md_files[@]}"; do
    rel_path="${file#docs/IGaming/}"

    # 先快速檢查是否包含可疑模式
    if ! grep -q '^```[a-z]' "$file" 2>/dev/null; then
        continue
    fi

    echo "🔍 掃描: $rel_path"

    # 讀取文件到數組
    mapfile -t lines < "$file"
    total_lines=${#lines[@]}

    file_errors=0

    # 遍歷每一行
    for ((i=0; i<total_lines; i++)); do
        line="${lines[$i]}"

        # 檢查是否為 ```language 模式
        if [[ "$line" =~ ^\`\`\`(text|yaml|markdown|sql|json)[[:space:]]*$ ]]; then
            lang="${BASH_REMATCH[1]}"

            # 獲取前一行和後一行
            prev_line=""
            next_line=""

            if [ $i -gt 0 ]; then
                prev_line="${lines[$((i-1))]}"
            fi

            if [ $i -lt $((total_lines-1)) ]; then
                next_line="${lines[$((i+1))]}"
            fi

            # 去除空白
            prev_trimmed=$(echo "$prev_line" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
            next_trimmed=$(echo "$next_line" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')

            # 判斷是否為閉合標記錯誤
            # 條件：前一行有內容 AND (後一行為空 OR 分隔符 OR 標題)
            if [ -n "$prev_trimmed" ]; then
                if [ -z "$next_trimmed" ] || [[ "$next_trimmed" =~ ^--- ]] || [[ "$next_trimmed" =~ ^# ]]; then
                    # 這是閉合標記錯誤
                    ((file_errors++))
                    ((total_errors++))

                    # 統計錯誤類型
                    case "$lang" in
                        text) ((text_errors++)) ;;
                        yaml) ((yaml_errors++)) ;;
                        markdown) ((markdown_errors++)) ;;
                        sql) ((sql_errors++)) ;;
                        json) ((json_errors++)) ;;
                    esac

                    # 截取上下文
                    context=$(echo "$prev_trimmed" | cut -c1-60)

                    # 記錄詳細信息
                    echo "  Line $((i+1)): \`\`\`$lang (閉合錯誤)"
                    echo "    上下文: $context..."

                    # 保存到詳細文件
                    {
                        echo "文件: $rel_path"
                        echo "  Line $((i+1)): \`\`\`$lang"
                        echo "  上下文: $context..."
                        echo ""
                    } >> "$DETAILS_FILE"
                fi
            fi
        fi
    done

    if [ $file_errors -gt 0 ]; then
        ((total_files++))
        echo "  ✗ 發現 $file_errors 處錯誤"
    else
        echo "  ✓ 無錯誤"
    fi
    echo ""
done

# 輸出統計
echo "========================================"
echo "📊 掃描統計"
echo "========================================"
echo "掃描文件總數: ${#md_files[@]}"
echo "受影響文件數: $total_files"
echo "總錯誤數: $total_errors"
echo ""

if [ $total_errors -gt 0 ]; then
    echo "錯誤類型分布:"

    if [ $text_errors -gt 0 ]; then
        percentage=$(awk "BEGIN {printf \"%.1f\", ($text_errors / $total_errors) * 100}")
        echo "  - \`\`\`text: $text_errors 處 ($percentage%)"
    fi

    if [ $yaml_errors -gt 0 ]; then
        percentage=$(awk "BEGIN {printf \"%.1f\", ($yaml_errors / $total_errors) * 100}")
        echo "  - \`\`\`yaml: $yaml_errors 處 ($percentage%)"
    fi

    if [ $markdown_errors -gt 0 ]; then
        percentage=$(awk "BEGIN {printf \"%.1f\", ($markdown_errors / $total_errors) * 100}")
        echo "  - \`\`\`markdown: $markdown_errors 處 ($percentage%)"
    fi

    if [ $sql_errors -gt 0 ]; then
        percentage=$(awk "BEGIN {printf \"%.1f\", ($sql_errors / $total_errors) * 100}")
        echo "  - \`\`\`sql: $sql_errors 處 ($percentage%)"
    fi

    if [ $json_errors -gt 0 ]; then
        percentage=$(awk "BEGIN {printf \"%.1f\", ($json_errors / $total_errors) * 100}")
        echo "  - \`\`\`json: $json_errors 處 ($percentage%)"
    fi

    echo ""
    echo "========================================"
    echo "📁 詳細錯誤清單"
    echo "========================================"
    echo ""
    cat "$DETAILS_FILE"
    echo ""
    echo "⚠️  發現 $total_errors 處閉合標記錯誤"
    exit 1
else
    echo "✅ 未發現閉合標記錯誤"
    exit 0
fi
