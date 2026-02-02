#!/bin/bash
# 簡化版閉合標記錯誤掃描器
# 使用更簡單的邏輯和更好的錯誤處理

set -e

TARGET_DIR="docs/IGaming"
OUTPUT_FILE="docs/IGaming/CLOSING_FENCE_SCAN_REPORT.txt"

echo "🚀 開始掃描 IGaming 文檔閉合標記錯誤..."
echo "📂 目標目錄: $TARGET_DIR"
echo ""

# 統計變量
total_files=0
total_errors=0
declare -A error_types

# 首先使用 find 找出所有 .md 文件
echo "📝 正在收集文件列表..."
readarray -t md_files < <(find "$TARGET_DIR" -name "*.md" -type f ! -name "*REPORT*" ! -name "*CORRECTION*" ! -name "*backup*" 2>/dev/null)

echo "找到 ${#md_files[@]} 個 Markdown 文件"
echo ""

# 對每個文件進行掃描
for file in "${md_files[@]}"; do
    # 使用 grep 檢查是否包含可疑模式
    if grep -q '^```[a-z]' "$file" 2>/dev/null; then
        echo "🔍 掃描: ${file#docs/IGaming/}"

        # 使用 awk 進行上下文分析
        awk -v file="$file" '
        BEGIN {
            line_num = 0
            file_errors = 0
        }
        {
            line_num++
            current = $0

            # 檢查是否為 ```language 模式
            if (current ~ /^```(text|yaml|markdown|sql|json)\s*$/) {
                match(current, /^```([a-z]+)/, arr)
                lang = arr[1]

                # 檢查前一行是否有內容（非空）
                if (prev_line != "" && prev_line !~ /^[[:space:]]*$/) {
                    # 檢查後一行（需要讀取下一行）
                    getline next_line
                    line_num++

                    # 如果後一行為空、分隔符或標題，這是閉合標記錯誤
                    if (next_line ~ /^[[:space:]]*$/ || next_line ~ /^---/ || next_line ~ /^#/) {
                        file_errors++
                        printf "  Line %d: ```%s (閉合錯誤)\n", line_num-1, lang
                        printf "    前一行: %s\n", substr(prev_line, 1, 60)
                    }

                    # 將 next_line 設為當前行
                    prev_line = next_line
                    current = next_line
                }
            }

            prev_line = current
        }
        END {
            if (file_errors > 0) {
                printf "  ✗ 發現 %d 處錯誤\n\n", file_errors
                exit file_errors
            }
        }
        ' "$file"

        # 檢查 awk 退出碼
        awk_exit=$?
        if [ $awk_exit -gt 0 ]; then
            ((total_files++))
            ((total_errors += awk_exit))

            # 統計錯誤類型
            for lang in text yaml markdown sql json; do
                count=$(grep -c "^\`\`\`$lang" "$file" 2>/dev/null || echo 0)
                if [ $count -gt 0 ]; then
                    error_types[$lang]=$((${error_types[$lang]:-0} + count))
                fi
            done
        fi
    fi
done

echo ""
echo "========================================"
echo "📊 掃描統計"
echo "========================================"
echo "受影響文件數: $total_files"
echo "總錯誤數: $total_errors"
echo ""

if [ $total_errors -gt 0 ]; then
    echo "錯誤類型分布:"
    for lang in "${!error_types[@]}"; do
        count=${error_types[$lang]}
        percentage=$(awk "BEGIN {printf \"%.1f\", ($count / $total_errors) * 100}")
        echo "  - \`\`\`$lang: $count 處 ($percentage%)"
    done
    echo ""
    echo "⚠️  發現 $total_errors 處閉合標記錯誤"
    exit 1
else
    echo "✅ 未發現閉合標記錯誤"
    exit 0
fi
