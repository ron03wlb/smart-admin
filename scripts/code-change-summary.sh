#!/bin/bash

# Code Change Summary - 僅報告模式
# 版本: v3.0.0
# 日期: 2026-03-28
# 用途：Claude Stop Hook（快速報告，不執行測試）
# 說明：品質檢查已移至 Git Pre-commit/Pre-push Hooks

set -e

echo '========================================'
echo '📊 Code Change Summary'
echo '========================================'

# 檢查變更
changed_files=$(git diff --name-only HEAD 2>/dev/null || echo '')

if [ -z "$changed_files" ]; then
    echo '✓ No changes detected'
    echo '========================================'
    exit 0
fi

# 統計變更檔案類型
java_changed=$(echo "$changed_files" | grep '\.java$' | wc -l | tr -d ' ')
ts_changed=$(echo "$changed_files" | grep -E '\.(tsx?|jsx?)$' | wc -l | tr -d ' ')
spec_changed=$(echo "$changed_files" | grep '\.spec\.md$' | wc -l | tr -d ' ')
md_changed=$(echo "$changed_files" | grep '\.md$' | wc -l | tr -d ' ')
config_changed=$(echo "$changed_files" | grep -E '\.(json|yaml|yml|properties|xml)$' | wc -l | tr -d ' ')
total_changed=$(echo "$changed_files" | wc -l | tr -d ' ')

echo "Files changed: $total_changed"
echo "  - Java:       $java_changed"
echo "  - TypeScript: $ts_changed"
echo "  - Specs:      $spec_changed"
echo "  - Markdown:   $md_changed"
echo "  - Config:     $config_changed"
echo ''

# 提示品質檢查時機
echo '💡 Quality checks will run at:'
echo '  ┌─ git commit (Pre-commit Hook)'
echo '  │   • Java: Spotless formatting'
echo '  │   • Java: ArchUnit architecture tests'
echo '  │   • Markdown: File numbering, Mermaid validation'
echo '  │   • Execution time: ~20 seconds'
echo '  │'
echo '  └─ git push (Pre-push Hook)'
echo '      • Full test suite (parallel execution)'
echo '      • Test coverage verification'
echo '      • Execution time: ~60 seconds'
echo ''

# 手動檢查建議
if [ "$java_changed" -gt 0 ]; then
    echo '🔍 Optional manual checks (Java):'
    echo '  ./gradlew :smartadmin-app:test --tests ArchitectureTest'
    echo '  ./gradlew :smartadmin-app:test'
    echo '  ./gradlew :smartadmin-app:jacocoTestReport'
    echo ''
fi

# 快速跳過提示
echo '⚡ Quick bypass options:'
echo '  git commit --no-verify  # Skip pre-commit checks'
echo '  git push --no-verify    # Skip pre-push checks'
echo '  git commit -m "docs: ..." # Auto-skip tests for doc commits'

echo '========================================'

exit 0
