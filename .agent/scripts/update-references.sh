#!/bin/bash
# 更新 .agent/ 系統中的所有檔案引用

echo "開始更新交叉引用..."

# 定義替換映射（舊路徑 -> 新路徑）
declare -A replacements=(
  # Foundation
  ["foundation/01-naming-conventions.md"]="foundation/F01-naming-conventions.md"
  ["foundation/02-oop-principles.md"]="foundation/F02-oop-principles.md"
  ["foundation/09-manager-layer.md"]="foundation/F03-manager-layer.md"
  ["foundation/10-architecture-rules.md"]="foundation/F04-architecture-rules.md"

  # Database
  ["technology/database/05-postgresql-mybatis-integration.md"]="technology/database/D05-postgresql-mybatis-integration.md"
  ["technology/database/09-mybatis-plus-postgresql.md"]="technology/database/D06-mybatis-plus-postgresql.md"

  # Functional
  ["technology/functional/08-vavr-fundamentals.md"]="technology/functional/P01-vavr-fundamentals.md"
  ["technology/functional/08-vavr-advanced.md"]="technology/functional/P02-vavr-advanced.md"
  ["technology/functional/08-vavr-mybatis-integration.md"]="technology/functional/P03-vavr-mybatis-integration.md"

  # Patterns
  ["technology/patterns/03-concurrency-rules.md"]="technology/patterns/P04-concurrency-rules.md"

  # Security
  ["security/07-owasp-top10-part1.md"]="security/S01-owasp-top10-part1.md"
  ["security/07-owasp-top10-part2.md"]="security/S02-owasp-top10-part2.md"

  # Quality Tools
  ["quality-tools/11-checkstyle-rules.md"]="quality-tools/Q01-checkstyle-rules.md"
  ["quality-tools/12-pmd-rules.md"]="quality-tools/Q02-pmd-rules.md"
  ["quality-tools/13-spotbugs-rules.md"]="quality-tools/Q03-spotbugs-rules.md"
  ["quality-tools/14-spotless-rules.md"]="quality-tools/Q04-spotless-rules.md"
  ["quality-tools/15-error-prone-rules.md"]="quality-tools/Q05-error-prone-rules.md"
  ["quality-tools/16-jacoco-coverage-rules.md"]="quality-tools/Q06-jacoco-coverage-rules.md"

  # Workflows
  ["workflows/06-sonarqube-rules.md"]="workflows/W01-sonarqube-rules.md"
  ["workflows/17-commit-message-conventions.md"]="workflows/W02-commit-message-conventions.md"
)

# 搜尋並替換所有.md檔案中的引用
for old in "${!replacements[@]}"; do
  new="${replacements[$old]}"
  echo "替換: $old -> $new"

  # 在 .agent/, CLAUDE.md, .claude/ 中搜尋並替換
  find .agent -name "*.md" -type f -exec sed -i '' "s|$old|$new|g" {} +
  sed -i '' "s|$old|$new|g" CLAUDE.md 2>/dev/null || true
  find .claude -name "*.md" -type f -exec sed -i '' "s|$old|$new|g" {} + 2>/dev/null || true
done

echo "✅ 交叉引用更新完成"
