#!/bin/bash
# validate_links.sh v2.0.0
# 掃描所有 Markdown 文件中的斷裂鏈接 (bash 3.2 / macOS 兼容)

DOCS_DIR="${1:-docs/iGaming}"
BROKEN_LINKS=0
LINKS_TMP=$(mktemp)
trap "rm -f '$LINKS_TMP'" EXIT

echo "=========================================="
echo "  iGaming 文檔連結驗證 v2.0.0"
echo "  掃描目錄: $DOCS_DIR"
echo "=========================================="
echo ""

while IFS= read -r file; do
    # 使用 python3 提取所有內部 Markdown 連結（跳過 code block）
    python3 - "$file" > "$LINKS_TMP" 2>/dev/null <<'PYEOF'
import re, sys
filepath = sys.argv[1]
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()
# 移除 fenced code blocks (``` ... ```) 避免誤判
content = re.sub(r'```.*?```', '', content, flags=re.DOTALL)
# 移除 inline code (`...`) 避免誤判
content = re.sub(r'`.+?`', '', content)
links = re.findall(r'\[.*?\]\(([^)]+)\)', content)
for link in links:
    if not link.startswith('http') and not link.startswith('#'):
        # 移除錨點和查詢參數
        clean = link.split('#')[0].split('?')[0]
        if clean:
            print(clean)
PYEOF

    dir=$(dirname "$file")

    while IFS= read -r link; do
        target="$dir/$link"

        if [ ! -f "$target" ] && [ ! -d "$target" ]; then
            echo "  [BROKEN] $file"
            echo "    -> $link"
            BROKEN_LINKS=$((BROKEN_LINKS + 1))
        fi
    done < "$LINKS_TMP"

done < <(find "$DOCS_DIR" -name "*.md" -not -path "*/archive/*" -not -path "*/source-archive/*" -not -path "*/adr/*" -not -path "*/quality-reports/*" 2>/dev/null | sort)

echo ""
echo "=========================================="
echo "  結果摘要"
echo "=========================================="
echo "  Broken Links: $BROKEN_LINKS"
echo "=========================================="

if [ $BROKEN_LINKS -gt 0 ]; then
    exit 1
else
    exit 0
fi
