#!/bin/bash
# scripts/scan-broken-links.sh
# iGaming 文檔斷鏈掃描工具 (macOS / Linux 兼容)
# 用法: ./scripts/scan-broken-links.sh [目錄路徑]
# 範例: ./scripts/scan-broken-links.sh docs/iGaming/

set -e

TARGET_DIR="${1:-docs/iGaming}"
BROKEN_COUNT=0
TOTAL_LINKS=0
LINKS_TMP=$(mktemp)
trap "rm -f '$LINKS_TMP'" EXIT

echo "🔍 掃描目錄: $TARGET_DIR"
echo "================================"

# 找出所有 Markdown 文件中的相對鏈接
while IFS= read -r file; do
    # 使用 python3 提取連結（跳過 code block 和 inline code）
    python3 - "$file" > "$LINKS_TMP" 2>/dev/null <<'PYEOF'
import re, sys
filepath = sys.argv[1]
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()
content = re.sub(r'```.*?```', '', content, flags=re.DOTALL)
content = re.sub(r'`.+?`', '', content)
links = re.findall(r'\[.*?\]\(([^)]+)\)', content)
for link in links:
    if not link.startswith('http') and not link.startswith('#'):
        clean = link.split('#')[0].split('?')[0]
        if clean:
            print(clean)
PYEOF

    file_dir=$(dirname "$file")

    while IFS= read -r link_path; do
        ((TOTAL_LINKS++)) || true

        # 解析目標路徑 (macOS 兼容，不使用 realpath -m)
        target="$file_dir/$link_path"

        # 檢查文件是否存在
        if [[ ! -f "$target" && ! -d "$target" ]]; then
            echo "❌ 斷鏈: $file"
            echo "   → $link_path"
            ((BROKEN_COUNT++)) || true
        fi
    done < "$LINKS_TMP"
done < <(find "$TARGET_DIR" -name "*.md" -type f ! -path "*/backup-corrupted/*" ! -path "*/source-archive/*" ! -path "*/adr/*" ! -path "*/quality-reports/*" 2>/dev/null | sort)

echo "================================"
echo "📊 掃描結果:"
echo "   總鏈接數: $TOTAL_LINKS"
echo "   斷鏈數: $BROKEN_COUNT"

if [[ $BROKEN_COUNT -gt 0 ]]; then
    echo "⚠️ 發現 $BROKEN_COUNT 個斷鏈"
    exit 1
else
    echo "✅ 所有鏈接有效"
    exit 0
fi
