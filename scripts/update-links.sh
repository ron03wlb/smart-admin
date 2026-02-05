#!/bin/bash
# update-links.sh v1.0.0
# 批量替換 Markdown 文件中的內部連結
#
# 用法:
#   ./scripts/update-links.sh <mapping-file> [--dry-run]
#
# mapping-file 格式 (JSON):
#   { "old_path": "new_path", ... }
#
# 範例:
#   ./scripts/update-links.sh docs/iGaming/reports/link-mapping.json --dry-run

set -euo pipefail

MAPPING_FILE="${1:-}"
DRY_RUN="${2:-}"
DOCS_DIR="docs/iGaming"

if [[ -z "$MAPPING_FILE" ]]; then
    echo "用法: $0 <mapping-file.json> [--dry-run]"
    echo ""
    echo "mapping-file 格式:"
    echo '  { "03_Player_Journey/03-02_VIP_Loyalty.md": "01_Player_Center/01-02_VIP_Loyalty.md" }'
    exit 1
fi

if [[ ! -f "$MAPPING_FILE" ]]; then
    echo "Error: mapping file not found: $MAPPING_FILE"
    exit 1
fi

TOTAL_REPLACEMENTS=0
TOTAL_FILES_CHANGED=0

echo "=========================================="
echo "  iGaming 連結批量更新 v1.0.0"
echo "  Mapping: $MAPPING_FILE"
if [[ "$DRY_RUN" == "--dry-run" ]]; then
    echo "  Mode: DRY RUN (不會實際修改)"
fi
echo "=========================================="
echo ""

# 解析 JSON mapping（使用 python3 避免 jq 依賴）
mapfile -t PAIRS < <(python3 -c "
import json, sys
with open('$MAPPING_FILE') as f:
    mapping = json.load(f)
for old, new in mapping.items():
    print(f'{old}|||{new}')
")

echo "載入 ${#PAIRS[@]} 個路徑映射"
echo ""

# 收集所有 .md 文件
mapfile -t MD_FILES < <(find "$DOCS_DIR" -name "*.md" -not -path "*/archive/*" | sort)

for pair in "${PAIRS[@]}"; do
    OLD_PATH="${pair%%|||*}"
    NEW_PATH="${pair##*|||}"

    echo "--- $OLD_PATH"
    echo "  -> $NEW_PATH"

    replacements=0

    for md_file in "${MD_FILES[@]}"; do
        # 檢查文件是否包含舊路徑
        if grep -qF "$OLD_PATH" "$md_file" 2>/dev/null; then
            count=$(grep -cF "$OLD_PATH" "$md_file" 2>/dev/null || true)
            replacements=$((replacements + count))

            echo "  [MATCH] $md_file ($count 處)"

            if [[ "$DRY_RUN" != "--dry-run" ]]; then
                # 使用 sed 替換（macOS 兼容）
                if [[ "$(uname)" == "Darwin" ]]; then
                    sed -i '' "s|${OLD_PATH}|${NEW_PATH}|g" "$md_file"
                else
                    sed -i "s|${OLD_PATH}|${NEW_PATH}|g" "$md_file"
                fi
                TOTAL_FILES_CHANGED=$((TOTAL_FILES_CHANGED + 1))
            fi
        fi
    done

    if [[ $replacements -eq 0 ]]; then
        echo "  (no matches)"
    fi

    TOTAL_REPLACEMENTS=$((TOTAL_REPLACEMENTS + replacements))
    echo ""
done

echo "=========================================="
echo "  結果摘要"
echo "=========================================="
echo "  Mappings:     ${#PAIRS[@]}"
echo "  Replacements: $TOTAL_REPLACEMENTS"
if [[ "$DRY_RUN" == "--dry-run" ]]; then
    echo "  Mode: DRY RUN — 未實際修改任何文件"
else
    echo "  Files Changed: $TOTAL_FILES_CHANGED"
fi
echo "=========================================="
