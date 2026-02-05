#!/bin/bash
# check_file_numbering.sh v2.0.0
# 檢查 iGaming 文檔編號規範 (bash 3.2 兼容)
#   - 格式驗證: XX-YY_*.md (最多 3 層: XX-YY-ZZ)
#   - 深度檢查: 超過 3 層編號視為違規
#   - 重複偵測: 不同目錄下同一編號
#   - 中文文件名偵測: 非 ASCII 字元
#   - 模組編號衝突偵測

DOCS_DIR="${1:-docs/iGaming}"
REPORT_MODE="${2:-}"

VIOLATIONS=0
WARNINGS=0
TOTAL_FILES=0
TOTAL_MODULES=0

# Temp files for tracking
SEEN_NUMBERS_FILE=$(mktemp)
MODULE_MAP_FILE=$(mktemp)
trap "rm -f '$SEEN_NUMBERS_FILE' '$MODULE_MAP_FILE'" EXIT

echo "=========================================="
echo "  iGaming 文檔編號檢查 v2.0.0"
echo "  掃描目錄: $DOCS_DIR"
echo "=========================================="
echo ""

# --- Check 1: 格式驗證 ---
echo "--- Check 1: 編號格式驗證 ---"
while IFS= read -r file; do
    TOTAL_FILES=$((TOTAL_FILES + 1))
    bname=$(basename "$file")

    # 跳過特殊文件
    if [ "$bname" = "README.md" ] || [ "$bname" = "INDEX.md" ]; then
        continue
    fi

    # 跳過非編號目錄下的文件
    parent_dir=$(basename "$(dirname "$file")")
    case "$parent_dir" in
        [0-9][0-9]_*) ;;
        *) continue ;;
    esac

    # 檢查是否符合 XX-YY 格式
    case "$bname" in
        [0-9][0-9]-[0-9][0-9]_*.md) ;;
        [0-9][0-9]-[0-9][0-9]-*.md) ;;
        *)
            echo "  [FORMAT] $file"
            VIOLATIONS=$((VIOLATIONS + 1))
            ;;
    esac
done < <(find "$DOCS_DIR" -name "*.md" -not -path "*/archive/*" -not -path "*/000_improve/*" 2>/dev/null | sort)
echo ""

# --- Check 2: 深度檢查（最多 3 層） ---
echo "--- Check 2: 編號深度檢查 (max 3 層) ---"
while IFS= read -r file; do
    bname=$(basename "$file")
    # 提取前綴（到 _ 之前的部分）
    prefix=$(echo "$bname" | sed 's/_.*$//')
    # 計算 - 的數量
    dash_count=$(echo "$prefix" | awk -F'-' '{print NF-1}')

    if [ "$dash_count" -gt 2 ] 2>/dev/null; then
        echo "  [DEPTH>3] $file (depth=$((dash_count + 1)))"
        VIOLATIONS=$((VIOLATIONS + 1))
    fi
done < <(find "$DOCS_DIR" -name "*.md" -not -path "*/archive/*" -not -path "*/000_improve/*" 2>/dev/null | sort)
echo ""

# --- Check 3: 重複編號偵測 ---
echo "--- Check 3: 重複編號偵測 ---"
while IFS= read -r file; do
    bname=$(basename "$file")
    # 提取 XX-YY 前綴
    number=$(echo "$bname" | grep -oE '^[0-9]{2}-[0-9]{2}' 2>/dev/null || true)

    if [ -n "$number" ]; then
        parent=$(basename "$(dirname "$file")")
        echo "${number}|${parent}|${file}" >> "$SEEN_NUMBERS_FILE"
    fi
done < <(find "$DOCS_DIR" -name "*.md" -not -path "*/archive/*" -not -path "*/000_improve/*" 2>/dev/null | sort)

# 找出跨目錄重複
if [ -s "$SEEN_NUMBERS_FILE" ]; then
    sort "$SEEN_NUMBERS_FILE" | while IFS='|' read -r num parent path; do
        # 找同編號但不同目錄的
        matches=$(grep "^${num}|" "$SEEN_NUMBERS_FILE" | grep -v "|${parent}|" || true)
        if [ -n "$matches" ]; then
            echo "$num" >> "${SEEN_NUMBERS_FILE}.dups"
        fi
    done

    if [ -f "${SEEN_NUMBERS_FILE}.dups" ]; then
        sort -u "${SEEN_NUMBERS_FILE}.dups" | while read -r dup_num; do
            echo "  [DUPLICATE] $dup_num"
            grep "^${dup_num}|" "$SEEN_NUMBERS_FILE" | while IFS='|' read -r _ _ path; do
                echo "    -> $path"
            done
            WARNINGS=$((WARNINGS + 1))
        done
        rm -f "${SEEN_NUMBERS_FILE}.dups"
    fi
fi
echo ""

# --- Check 4: 非 ASCII 文件名偵測 ---
echo "--- Check 4: 非 ASCII 文件名偵測 ---"
while IFS= read -r file; do
    bname=$(basename "$file")
    if echo "$bname" | LC_ALL=C grep -q '[^[:print:][:space:]]' 2>/dev/null; then
        echo "  [NON-ASCII] $file"
        WARNINGS=$((WARNINGS + 1))
    fi
    # 也檢查中文字符
    if python3 -c "import sys; s='$bname'; sys.exit(0 if any(ord(c)>127 for c in s) else 1)" 2>/dev/null; then
        echo "  [NON-ASCII] $file"
        WARNINGS=$((WARNINGS + 1))
    fi
done < <(find "$DOCS_DIR" -name "*.md" -not -path "*/archive/*" -not -path "*/000_improve/*" 2>/dev/null | sort)
echo ""

# --- Check 5: 模組編號衝突偵測 ---
echo "--- Check 5: 模組目錄編號衝突 ---"
while IFS= read -r dir; do
    dname=$(basename "$dir")
    mod_num=$(echo "$dname" | grep -oE '^[0-9]{2}' || true)

    if [ -n "$mod_num" ]; then
        TOTAL_MODULES=$((TOTAL_MODULES + 1))
        existing=$(grep "^${mod_num}|" "$MODULE_MAP_FILE" 2>/dev/null | head -1 | cut -d'|' -f2 || true)

        if [ -n "$existing" ]; then
            echo "  [MODULE CONFLICT] $mod_num"
            echo "    -> $existing"
            echo "    -> $dir"
            VIOLATIONS=$((VIOLATIONS + 1))
        else
            echo "${mod_num}|${dir}" >> "$MODULE_MAP_FILE"
        fi
    fi
done < <(find "$DOCS_DIR" -mindepth 1 -maxdepth 1 -type d -not -path "*/archive*" -not -path "*/000_improve*" 2>/dev/null | sort)
echo ""

# --- Summary ---
echo "=========================================="
echo "  結果摘要"
echo "=========================================="
echo "  Violations: $VIOLATIONS"
echo "  Warnings:   $WARNINGS"
echo "  Total Files: $TOTAL_FILES"
echo "  Modules:     $TOTAL_MODULES"
echo "=========================================="

# --- JSON Report ---
if [ "$REPORT_MODE" = "--report" ]; then
    REPORT_DIR="$DOCS_DIR/reports"
    mkdir -p "$REPORT_DIR"
    REPORT_FILE="$REPORT_DIR/numbering-baseline.json"
    cat > "$REPORT_FILE" << EOF
{
  "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "script_version": "2.0.0",
  "docs_dir": "$DOCS_DIR",
  "total_files": $TOTAL_FILES,
  "total_modules": $TOTAL_MODULES,
  "violations": $VIOLATIONS,
  "warnings": $WARNINGS
}
EOF
    echo ""
    echo "Report saved: $REPORT_FILE"
fi

if [ $VIOLATIONS -gt 0 ]; then
    exit 1
else
    exit 0
fi
