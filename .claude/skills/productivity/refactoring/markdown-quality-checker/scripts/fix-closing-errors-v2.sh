#!/bin/bash
# 閉合標記錯誤批量修正腳本 v2
# 使用上下文分析進行精確修正

TARGET_DIR="docs/IGaming"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
BACKUP_DIR="docs/IGaming-backup-closing-fix-${TIMESTAMP}"
LOG_FILE="/tmp/closing-fix-log-${TIMESTAMP}.txt"

echo "🚀 開始批量修正閉合標記錯誤..."
echo "======================================="
echo "目標目錄: $TARGET_DIR"
echo "備份目錄: $BACKUP_DIR"
echo "日誌文件: $LOG_FILE"
echo ""

# 創建完整備份
echo "📦 創建完整備份..."
if [ -d "$TARGET_DIR" ]; then
    cp -r "$TARGET_DIR" "$BACKUP_DIR"
    echo "✅ 備份完成: $BACKUP_DIR"
else
    echo "❌ 錯誤：目標目錄不存在"
    exit 1
fi
echo ""

# 統計變量
total_files=0
total_fixed=0
text_fixed=0
yaml_fixed=0
markdown_fixed=0
sql_fixed=0
json_fixed=0

# 初始化日誌
{
    echo "=========================================="
    echo "閉合標記錯誤批量修正日誌"
    echo "=========================================="
    echo "時間: $(date)"
    echo "目標: $TARGET_DIR"
    echo "備份: $BACKUP_DIR"
    echo ""
} > "$LOG_FILE"

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

    echo "🔧 處理: $rel_path"

    # 讀取文件到數組
    mapfile -t lines < "$file"
    total_lines=${#lines[@]}

    # 新文件內容
    declare -a new_lines=()
    file_fixed=0

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
            if [ -n "$prev_trimmed" ]; then
                if [ -z "$next_trimmed" ] || [[ "$next_trimmed" =~ ^--- ]] || [[ "$next_trimmed" =~ ^# ]]; then
                    # 這是閉合標記錯誤，修正為三個反引號
                    new_lines+=('```')

                    ((file_fixed++))
                    ((total_fixed++))

                    # 統計錯誤類型
                    case "$lang" in
                        text) ((text_fixed++)) ;;
                        yaml) ((yaml_fixed++)) ;;
                        markdown) ((markdown_fixed++)) ;;
                        sql) ((sql_fixed++)) ;;
                        json) ((json_fixed++)) ;;
                    esac

                    # 記錄修正
                    {
                        echo "文件: $rel_path"
                        echo "  Line $((i+1)): \\\`\\\`\\\`$lang → \\\`\\\`\\\`"
                        echo "  上下文: $(echo "$prev_trimmed" | cut -c1-60)..."
                        echo ""
                    } >> "$LOG_FILE"

                    echo "  Line $((i+1)): \\\`\\\`\\\`$lang → \\\`\\\`\\\`"
                    continue
                fi
            fi
        fi

        # 保留原行
        new_lines+=("$line")
    done

    # 如果有修正，寫回文件
    if [ $file_fixed -gt 0 ]; then
        ((total_files++))

        # 寫回文件（保持原始換行符）
        printf "%s\n" "${new_lines[@]}" > "$file"

        echo "  ✅ 修正 $file_fixed 處"
    else
        echo "  ⏭️  無需修正"
    fi
    echo ""
done

# 輸出統計
echo "========================================"
echo "📊 修正統計"
echo "========================================"
echo "受影響文件數: $total_files"
echo "總修正數: $total_fixed"
echo ""

if [ $total_fixed -gt 0 ]; then
    echo "修正類型分布:"

    if [ $text_fixed -gt 0 ]; then
        percentage=$(awk "BEGIN {printf \"%.1f\", ($text_fixed / $total_fixed) * 100}")
        echo "  - \\\`\\\`\\\`text → \\\`\\\`\\\`: $text_fixed 處 ($percentage%)"
    fi

    if [ $yaml_fixed -gt 0 ]; then
        percentage=$(awk "BEGIN {printf \"%.1f\", ($yaml_fixed / $total_fixed) * 100}")
        echo "  - \\\`\\\`\\\`yaml → \\\`\\\`\\\`: $yaml_fixed 處 ($percentage%)"
    fi

    if [ $markdown_fixed -gt 0 ]; then
        percentage=$(awk "BEGIN {printf \"%.1f\", ($markdown_fixed / $total_fixed) * 100}")
        echo "  - \\\`\\\`\\\`markdown → \\\`\\\`\\\`: $markdown_fixed 處 ($percentage%)"
    fi

    if [ $sql_fixed -gt 0 ]; then
        percentage=$(awk "BEGIN {printf \"%.1f\", ($sql_fixed / $total_fixed) * 100}")
        echo "  - \\\`\\\`\\\`sql → \\\`\\\`\\\`: $sql_fixed 處 ($percentage%)"
    fi

    if [ $json_fixed -gt 0 ]; then
        percentage=$(awk "BEGIN {printf \"%.1f\", ($json_fixed / $total_fixed) * 100}")
        echo "  - \\\`\\\`\\\`json → \\\`\\\`\\\`: $json_fixed 處 ($percentage%)"
    fi
fi

# 寫入統計到日誌
{
    echo "=========================================="
    echo "修正統計"
    echo "=========================================="
    echo "受影響文件數: $total_files"
    echo "總修正數: $total_fixed"
    echo ""
    echo "修正類型:"
    echo "  - text: $text_fixed"
    echo "  - yaml: $yaml_fixed"
    echo "  - markdown: $markdown_fixed"
    echo "  - sql: $sql_fixed"
    echo "  - json: $json_fixed"
    echo ""
    echo "完成時間: $(date)"
} >> "$LOG_FILE"

echo ""
echo "========================================"
echo "✅ 修正完成！"
echo "========================================"
echo ""
echo "📄 日誌文件: $LOG_FILE"
echo "📦 備份位置: $BACKUP_DIR"
echo ""
echo "📝 下一步："
echo "  1. 查看修正日誌: cat $LOG_FILE"
echo "  2. 檢查 Git 變更: git diff docs/IGaming/ | head -100"
echo "  3. 驗證修正結果: bash scripts/scan-closing-errors-v2.sh"
echo "  4. 如需回滾: rm -rf docs/IGaming && mv $BACKUP_DIR docs/IGaming"
echo ""

if [ $total_fixed -gt 0 ]; then
    exit 0
else
    echo "⚠️  未發現需要修正的錯誤"
    exit 1
fi
