#!/bin/bash
# scripts/pre-commit-igaming.sh
# iGaming 文檔 Pre-commit 驗證
# 安裝: ln -sf ../../scripts/pre-commit-igaming.sh .git/hooks/pre-commit

set -e

# 獲取暫存的 .md 文件
STAGED_MD_FILES=$(git diff --cached --name-only --diff-filter=ACM | grep -E '\.md$' | grep -E '^docs/iGaming/' || true)

if [[ -z "$STAGED_MD_FILES" ]]; then
    exit 0
fi

echo "🔍 Pre-commit: 驗證 iGaming 文檔..."

# 1. Bonus 損壞檢測 (僅檢查暫存文件)
echo "📋 檢查 1: Bonus 損壞模式"
for file in $STAGED_MD_FILES; do
    if grep -qE "(Bonus[0-9]+-Bonus[0-9]+|2Bonus26|BonusBonus)" "$file"; then
        echo "❌ 發現 Bonus 損壞模式: $file"
        exit 1
    fi
done
echo "✓ Bonus 檢查通過"

# 2. 文件命名規範 (僅檢查新增文件)
echo "📋 檢查 2: 文件命名規範"
NEW_FILES=$(git diff --cached --name-only --diff-filter=A | grep -E '\.md$' | grep -E '^docs/iGaming/' || true)
for file in $NEW_FILES; do
    filename=$(basename "$file")
    # 跳過特殊文件
    if [[ "$filename" =~ ^(README|INDEX)\.md$ ]]; then
        continue
    fi
    # 跳過 seamless-wallet 子目錄
    if [[ "$file" =~ seamless-wallet/ ]]; then
        continue
    fi
    # 跳過 implementation-guides 子目錄
    if [[ "$file" =~ implementation-guides/ ]]; then
        continue
    fi
    # 跳過 concepts 子目錄
    if [[ "$file" =~ concepts/ ]]; then
        continue
    fi
    # 跳過 000_improve 目錄
    if [[ "$file" =~ 000_improve/ ]]; then
        continue
    fi
    # 驗證格式
    if [[ ! "$filename" =~ ^[0-9]{2}-[0-9]{2}(-[0-9]{2})?_.+\.md$ ]]; then
        echo "❌ 命名格式錯誤: $file"
        echo "   期望格式: XX-YY_Name.md 或 XX-YY-ZZ_Name.md"
        exit 1
    fi
done
echo "✓ 命名規範檢查通過"

# 3. Mermaid 語法驗證 (如果已安裝 validate-mermaid.sh)
if [[ -x "./scripts/validate-mermaid.sh" ]]; then
    echo "📋 檢查 3: Mermaid 語法"
    for file in $STAGED_MD_FILES; do
        if grep -q '```mermaid' "$file"; then
            if ! ./scripts/validate-mermaid.sh "$file" >/dev/null 2>&1; then
                echo "❌ Mermaid 語法錯誤: $file"
                exit 1
            fi
        fi
    done
    echo "✓ Mermaid 語法檢查通過"
fi

# 4. 更新 VERSIONS.yml 時間戳 (如果有 skill 相關變更)
STAGED_SKILL_FILES=$(git diff --cached --name-only --diff-filter=ACM | grep -E '^\.(claude|agent)/skills/' || true)
if [[ -n "$STAGED_SKILL_FILES" ]]; then
    echo "📋 檢查 4: 更新 VERSIONS.yml 時間戳"
    if [[ -x ".claude/scripts/update-versions-timestamp.sh" ]]; then
        ./.claude/scripts/update-versions-timestamp.sh
        git add .claude/skills/VERSIONS.yml 2>/dev/null || true
        echo "✓ VERSIONS.yml 時間戳已更新"
    fi
fi

echo "✅ Pre-commit 驗證完成"
exit 0
