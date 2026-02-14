#!/bin/bash
# normalize-versions.sh v1.0.0
# 掃描/統一 iGaming 文檔版本號 (bash 3.2 兼容)
#
# 用法:
#   ./scripts/normalize-versions.sh [--scan]              # 掃描當前版本分佈
#   ./scripts/normalize-versions.sh --set v4.0.0          # 統一版本號
#   ./scripts/normalize-versions.sh --set v4.0.0 --dry-run

DOCS_DIR="docs/iGaming"
MODE="${1:---scan}"
TARGET_VERSION="${2:-}"
DRY_RUN="${3:-}"

echo "=========================================="
echo "  iGaming 文檔版本管理 v1.0.0"
echo "  Mode: $MODE"
echo "=========================================="
echo ""

if [ "$MODE" = "--scan" ]; then
    # --- Scan Mode ---
    echo "--- 版本聲明掃描 ---"
    echo ""

    VERSION_TMP=$(mktemp)
    trap "rm -f '$VERSION_TMP'" EXIT

    while IFS= read -r file; do
        # 提取版本聲明（**版本**: v1.0.0 或 **文檔版本**: 3.0.0）
        ver=$(grep -oE '(版本|Version)[^:]*:\s*v?[0-9]+\.[0-9]+\.[0-9]+' "$file" 2>/dev/null | head -1 | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' || true)

        if [ -n "$ver" ]; then
            echo "v${ver}|$(basename "$file")|${file}" >> "$VERSION_TMP"
        fi
    done < <(find "$DOCS_DIR" -name "*.md" -not -path "*/archive/*" -not -path "*/000_improve/*" 2>/dev/null | sort)

    if [ -s "$VERSION_TMP" ]; then
        echo "| 版本 | 文件數 | 文件列表 |"
        echo "|------|--------|---------|"

        sort "$VERSION_TMP" | cut -d'|' -f1 | sort -uV | while read -r ver; do
            count=$(grep -c "^${ver}|" "$VERSION_TMP")
            files=$(grep "^${ver}|" "$VERSION_TMP" | cut -d'|' -f2 | tr '\n' ', ' | sed 's/,$//')
            echo "| $ver | $count | $files |"
        done

        total_versions=$(sort "$VERSION_TMP" | cut -d'|' -f1 | sort -u | wc -l | tr -d ' ')
        total_files=$(wc -l < "$VERSION_TMP" | tr -d ' ')
        echo ""
        echo "Total: $total_files 個文件, $total_versions 個不同版本"
    else
        echo "未找到版本聲明"
    fi

    echo ""
    echo "--- 變更日誌版本引用 ---"
    while IFS= read -r file; do
        changelog_versions=$(grep -oE '###\s+v[0-9]+\.[0-9]+\.[0-9]+' "$file" 2>/dev/null | grep -oE 'v[0-9]+\.[0-9]+\.[0-9]+' | tr '\n' ', ' | sed 's/,$//' || true)
        if [ -n "$changelog_versions" ]; then
            echo "  $(basename "$file"): $changelog_versions"
        fi
    done < <(find "$DOCS_DIR" -name "*.md" -not -path "*/archive/*" -not -path "*/000_improve/*" 2>/dev/null | sort)

elif [ "$MODE" = "--set" ]; then
    # --- Set Mode ---
    if [ -z "$TARGET_VERSION" ]; then
        echo "Error: 需指定目標版本, e.g., --set v4.0.0"
        exit 1
    fi

    TARGET="${TARGET_VERSION#v}"
    echo "目標版本: v$TARGET"
    if [ "$DRY_RUN" = "--dry-run" ]; then
        echo "Mode: DRY RUN"
    fi
    echo ""

    CHANGES=0

    while IFS= read -r file; do
        current=$(grep -oE '(版本|Version)[^:]*:\s*v?[0-9]+\.[0-9]+\.[0-9]+' "$file" 2>/dev/null | head -1 | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' || true)

        if [ -n "$current" ] && [ "$current" != "$TARGET" ]; then
            echo "  [UPDATE] $(basename "$file"): v$current -> v$TARGET"

            if [ "$DRY_RUN" != "--dry-run" ]; then
                # macOS sed 兼容
                sed -i '' "s/\(版本[^:]*: *v\{0,1\}\)[0-9][0-9]*\.[0-9][0-9]*\.[0-9][0-9]*/\1$TARGET/" "$file"
            fi

            CHANGES=$((CHANGES + 1))
        fi
    done < <(find "$DOCS_DIR" -name "*.md" -not -path "*/archive/*" -not -path "*/000_improve/*" 2>/dev/null | sort)

    echo ""
    echo "=========================================="
    echo "  結果: $CHANGES 個文件需要更新"
    if [ "$DRY_RUN" = "--dry-run" ]; then
        echo "  Mode: DRY RUN — 未實際修改"
    fi
    echo "=========================================="
else
    echo "用法:"
    echo "  $0 [--scan]                   # 掃描版本分佈"
    echo "  $0 --set v4.0.0              # 統一版本號"
    echo "  $0 --set v4.0.0 --dry-run    # 預覽變更"
    exit 1
fi
