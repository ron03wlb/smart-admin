#!/bin/bash
# scripts/validate-file-numbering.sh
# iGaming 文檔編號驗證工具
# 用法: ./scripts/validate-file-numbering.sh [目錄路徑]
# 兼容 bash 3.x (macOS 默認)

set -e

TARGET_DIR="${1:-docs/iGaming}"
ERROR_COUNT=0

echo "🔢 驗證文件編號規範: $TARGET_DIR"
echo "================================"

# 規則 1: 主編號格式 XX-YY[-ZZ]_Name.md
echo "📋 規則 1: 檢查命名格式"
while IFS= read -r file; do
    filename=$(basename "$file")
    # 跳過 README.md 和特殊文件
    if [[ "$filename" =~ ^(README|INDEX|CHANGELOG)\.md$ ]]; then
        continue
    fi
    # 檢查格式: XX-YY_Name.md 或 XX-YY-ZZ_Name.md
    if [[ ! "$filename" =~ ^[0-9]{2}-[0-9]{2}(-[0-9]{2})?_.+\.md$ ]]; then
        # 允許 seamless-wallet 子目錄的特殊格式
        if [[ "$file" =~ seamless-wallet/ ]]; then
            continue
        fi
        # 允許 implementation-guides 子目錄
        if [[ "$file" =~ implementation-guides/ ]]; then
            continue
        fi
        # 允許 concepts 子目錄
        if [[ "$file" =~ concepts/ ]]; then
            continue
        fi
        # 允許 diagrams 子目錄
        if [[ "$file" =~ diagrams/ ]]; then
            continue
        fi
        # 允許 000_improve 目錄
        if [[ "$file" =~ 000_improve/ ]]; then
            continue
        fi
        # 允許 architecture-decisions 目錄 (ADR 格式)
        if [[ "$file" =~ architecture-decisions/ ]]; then
            continue
        fi
        # 允許 reports 目錄
        if [[ "$file" =~ reports/ ]]; then
            continue
        fi
        # 允許 implementation 目錄 (Phase-based naming: 00-name.md)
        if [[ "$file" =~ implementation/ ]]; then
            continue
        fi
        # 允許 .templates 目錄
        if [[ "$file" =~ \.templates/ ]]; then
            continue
        fi
        # 允許 docs/database 目錄（自由命名的技術文檔）
        if [[ "$file" =~ docs/database/ ]]; then
            continue
        fi
        # 允許 .claude/ 目錄 (skills, agents, knowledge files)
        if [[ "$file" =~ \.claude/ ]]; then
            continue
        fi
        # 允許 .agent/ 目錄
        if [[ "$file" =~ \.agent/ ]]; then
            continue
        fi
        # 允許 .specs/ 目錄 (OpenSpec templates and configs)
        if [[ "$file" =~ \.specs/ ]]; then
            continue
        fi
        # 允許 docs/workflow/ 目錄 (Claude Code 工作流程文件)
        if [[ "$file" =~ docs/workflow/ ]]; then
            continue
        fi
        # 允許 docs/superpowers/ 目錄 (Superpowers 設計規格)
        if [[ "$file" =~ docs/superpowers/ ]]; then
            continue
        fi
        # 允許根目錄特殊文件
        if [[ "$filename" =~ ^SSOT ]]; then
            continue
        fi
        # 允許 docs/iGaming/ 根目錄的管理文件 (STANDARDS, AUDIT, TEMPLATE, TRANSLATION, 品質報告)
        if [[ "$filename" =~ ^(STANDARDS|AUDIT|TEMPLATE|TRANSLATION|EXECUTION).*\.md$ ]]; then
            continue
        fi
        # 允許 quality-reports/ 目錄
        if [[ "$file" =~ quality-reports/ ]]; then
            continue
        fi
        # 允許 testing/ 目錄
        if [[ "$file" =~ /testing/ ]]; then
            continue
        fi
        # 允許 research/ 目錄
        if [[ "$file" =~ /research/ ]]; then
            continue
        fi
        # 允許 adr/ 目錄
        if [[ "$file" =~ /adr/ ]]; then
            continue
        fi
        echo "❌ 格式錯誤: $file"
        ((ERROR_COUNT++)) || true
    fi
done < <(find "$TARGET_DIR" -name "*.md" -type f ! -path "*/archive/*" ! -path "*/source-archive/*" ! -path "*/requirements/*" ! -path "*/architecture/*" ! -path "*/backup-corrupted/*")

# 規則 2: 檢查重複編號 (使用臨時文件方式兼容 bash 3.x)
echo ""
echo "📋 規則 2: 檢查重複編號"
TEMP_FILE=$(mktemp)
trap "rm -f $TEMP_FILE" EXIT

find "$TARGET_DIR" -name "*.md" -type f ! -path "*/archive/*" ! -path "*/source-archive/*" ! -path "*/requirements/*" ! -path "*/architecture/*" ! -path "*/backup-corrupted/*" | while IFS= read -r file; do
    filename=$(basename "$file")
    # 提取編號部分 (XX-YY 或 XX-YY-ZZ)
    if [[ "$filename" =~ ^([0-9]{2}-[0-9]{2}(-[0-9]{2})?)_ ]]; then
        number="${BASH_REMATCH[1]}"
        dir=$(dirname "$file")
        echo "${dir}/${number}|$file"
    fi
done | sort > "$TEMP_FILE"

prev_key=""
prev_file=""
while IFS='|' read -r key file; do
    if [[ "$key" == "$prev_key" ]]; then
        echo "❌ 重複編號: $(basename "$key")"
        echo "   → $prev_file"
        echo "   → $file"
        ((ERROR_COUNT++)) || true
    fi
    prev_key="$key"
    prev_file="$file"
done < "$TEMP_FILE"

# 規則 3: 檢查深度超標 (最多 3 層: XX-YY-ZZ)
echo ""
echo "📋 規則 3: 檢查編號深度"
while IFS= read -r file; do
    filename=$(basename "$file")
    if [[ "$filename" =~ ^[0-9]{2}-[0-9]{2}-[0-9]{2}-[0-9]{2}_ ]]; then
        echo "❌ 深度超標 (>3層): $file"
        ((ERROR_COUNT++)) || true
    fi
done < <(find "$TARGET_DIR" -name "*.md" -type f ! -path "*/archive/*" ! -path "*/source-archive/*" ! -path "*/requirements/*" ! -path "*/architecture/*" ! -path "*/backup-corrupted/*")

echo ""
echo "================================"
echo "📊 驗證結果: 發現 $ERROR_COUNT 個錯誤"

if [[ $ERROR_COUNT -gt 0 ]]; then
    exit 1
else
    echo "✅ 所有文件符合命名規範"
    exit 0
fi
