#!/bin/bash
# IGaming 文檔閉合標記錯誤修正腳本
# 功能：修正使用語言標識符作為閉合標記的錯誤（如 ```text 改為 ```）

set -e

TARGET_DIR="docs/IGaming"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
BACKUP_DIR="docs/IGaming-backup-closing-fence-${TIMESTAMP}"
LOG_FILE="docs/IGaming/CLOSING_FENCE_FIX_LOG.txt"

echo "🚀 IGaming 文檔閉合標記錯誤修正腳本 v1.0"
echo "========================================="
echo ""
echo "目標目錄: $TARGET_DIR"
echo ""

# 創建備份
echo "📦 創建完整備份：$BACKUP_DIR"
cp -r "$TARGET_DIR" "$BACKUP_DIR"
echo "✅ 備份完成"
echo ""

# 初始化日誌
{
    echo "IGaming 文檔閉合標記錯誤修正日誌"
    echo "執行時間: $(date)"
    echo "備份目錄: $BACKUP_DIR"
    echo ""
    echo "========================================="
    echo ""
} > "$LOG_FILE"

# 統計變量
total_files=0
total_fixes=0
text_fixes=0
yaml_fixes=0
markdown_fixes=0
sql_fixes=0
json_fixes=0

# 修正函數：處理單個文件
fix_file() {
    local file="$1"
    local file_fixes=0

    # 排除審計報告
    if [[ "$file" =~ REPORT|CORRECTION|backup ]]; then
        return 0
    fi

    # 創建臨時文件
    local temp_file=$(mktemp)

    # 讀取文件並處理
    local prev_line=""
    local modified=false

    while IFS= read -r line || [ -n "$line" ]; do
        # 檢查當前行是否為 ```language 模式
        if [[ "$line" =~ ^\`\`\`(text|yaml|markdown|sql|json)$ ]]; then
            local language="${BASH_REMATCH[1]}"

            # 檢查前一行是否非空（去除空白）
            local prev_trimmed=$(echo "$prev_line" | xargs)

            if [ -n "$prev_trimmed" ]; then
                # 讀取下一行來判斷
                local next_line=""
                if IFS= read -r next_line <&3; then
                    local next_trimmed=$(echo "$next_line" | xargs)

                    # 如果後一行為空、分隔符或標題，這是閉合標記錯誤
                    if [ -z "$next_trimmed" ] || [[ "$next_trimmed" =~ ^--- ]] || [[ "$next_trimmed" =~ ^# ]]; then
                        # 替換為純 ```
                        echo "\`\`\`" >> "$temp_file"
                        echo "$next_line" >> "$temp_file"

                        # 記錄修正
                        ((file_fixes++))
                        ((total_fixes++))

                        case "$language" in
                            text) ((text_fixes++)) ;;
                            yaml) ((yaml_fixes++)) ;;
                            markdown) ((markdown_fixes++)) ;;
                            sql) ((sql_fixes++)) ;;
                            json) ((json_fixes++)) ;;
                        esac

                        modified=true

                        # 記錄到日誌
                        echo "  修正: \`\`\`$language -> \`\`\`" >> "$LOG_FILE"

                        # 更新 prev_line 為替換後的內容
                        prev_line="\`\`\`"
                        continue
                    else
                        # 不是閉合標記錯誤，寫入原內容
                        echo "$line" >> "$temp_file"
                        echo "$next_line" >> "$temp_file"
                        prev_line="$next_line"
                        continue
                    fi
                fi
            fi
        fi

        # 寫入當前行
        echo "$line" >> "$temp_file"
        prev_line="$line"

    done < "$file" 3< "$file"

    # 如果有修正，替換原文件
    if [ "$modified" = true ]; then
        mv "$temp_file" "$file"
        ((total_files++))

        # 記錄到日誌
        echo "文件: ${file#docs/IGaming/}" >> "$LOG_FILE"
        echo "  修正數: $file_fixes" >> "$LOG_FILE"
        echo "" >> "$LOG_FILE"
    else
        rm -f "$temp_file"
    fi

    return $file_fixes
}

# 遍歷所有 .md 文件
echo "🔧 開始修正..."

while IFS= read -r -d '' file; do
    fix_file "$file"
done < <(find "$TARGET_DIR" -name "*.md" -type f -print0)

echo "✅ 修正完成！"
echo ""

# 生成統計報告
{
    echo ""
    echo "========================================="
    echo "修正統計"
    echo "========================================="
    echo "  - 修正文件數: $total_files"
    echo "  - 總修正數: $total_fixes"
    echo ""
    echo "錯誤類型分布:"
    [ $text_fixes -gt 0 ] && echo "  - \`\`\`text: $text_fixes 處"
    [ $yaml_fixes -gt 0 ] && echo "  - \`\`\`yaml: $yaml_fixes 處"
    [ $markdown_fixes -gt 0 ] && echo "  - \`\`\`markdown: $markdown_fixes 處"
    [ $sql_fixes -gt 0 ] && echo "  - \`\`\`sql: $sql_fixes 處"
    [ $json_fixes -gt 0 ] && echo "  - \`\`\`json: $json_fixes 處"
    echo ""
    echo "========================================="
} | tee -a "$LOG_FILE"

echo ""
echo "📊 修正統計："
echo "  - 修正文件數: $total_files"
echo "  - 總修正數: $total_fixes"
echo ""

if [ $total_fixes -gt 0 ]; then
    echo "✅ 修正成功！"
    echo ""
    echo "📝 下一步："
    echo "  1. 查看 Git 變更: git diff docs/IGaming/"
    echo "  2. 驗證修正: bash scripts/scan-closing-fence-errors.sh"
    echo "  3. 提交變更: git add . && git commit"
    echo "  4. 備份位置: $BACKUP_DIR"
    echo "  5. 日誌文件: $LOG_FILE"
else
    echo "ℹ️  未發現需要修正的錯誤"
fi
