#!/bin/bash
# scripts/validate-requirements-purity.sh
# Validate that business requirements documents are free of technical keywords
# Requirements layer should contain pure business language only
#
# Usage: ./scripts/validate-requirements-purity.sh
# Exit: 0 = all pure, 1 = technical keywords detected
#
# Spec: docs/iGaming/quality-reports/README.md
# Version: 1.0.0

set -euo pipefail

REQUIREMENTS_DIR="docs/iGaming/requirements"
VIOLATIONS=0
VIOLATION_FILES=0
TOTAL_FILES=0

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  需求層業務純度檢查"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "掃描目錄: $REQUIREMENTS_DIR"
echo ""

if [[ ! -d "$REQUIREMENTS_DIR" ]]; then
    echo "⚠️  目錄不存在: $REQUIREMENTS_DIR"
    echo "✅ 跳過檢查"
    exit 0
fi

# Technical keywords that should NOT appear in requirements docs
# (case-insensitive where appropriate)
TECH_KEYWORDS=(
    # Infrastructure
    'Redis'
    'PostgreSQL'
    'Kafka'
    'Elasticsearch'
    'RabbitMQ'
    'MongoDB'
    # Cryptography
    'HMAC-SHA256'
    'AES-256-GCM'
    'SHA-256'
    'RSA-2048'
    'Argon2'
    # Frameworks & Annotations
    '@Transactional'
    '@Service'
    '@Controller'
    '@Repository'
    'RestTemplate'
    'WebClient'
    'JPA'
    'MyBatis'
    # Architecture Patterns (specific terms)
    'TCC'
    'SAGA compensation'
    'Circuit Breaker'
    # Database DDL
    'CREATE TABLE'
    'CREATE INDEX'
    'ALTER TABLE'
    'SELECT \* FROM'
    'INSERT INTO'
    'primary key'
    'foreign key'
    # HTTP Status Codes
    'HTTP 200'
    'HTTP 400'
    'HTTP 401'
    'HTTP 403'
    'HTTP 404'
    'HTTP 500'
    'HTTP 503'
    # SmartAdmin
    'ResponseDTO'
    'PageResult'
    'SmartBeanUtil'
    'SmartPageUtil'
)

while IFS= read -r file; do
    TOTAL_FILES=$((TOTAL_FILES + 1))
    FILE_VIOLATIONS=0

    # Read file content, excluding legitimate lines
    CONTENT=$(cat "$file" | \
        grep -v '→ \*\*\[' | \
        grep -v '> \*\*Refinement Note\*\*' | \
        grep -v 'Related Doc:' | \
        grep -v 'Canonical Source:' | \
        grep -v '^## .*Implementation' | \
        grep -v '^### .*Implementation' | \
        grep -v '^> \*\*Technical Implementation\*\*' | \
        sed '/^```mermaid$/,/^```$/d' \
        2>/dev/null || true)

    for keyword in "${TECH_KEYWORDS[@]}"; do
        MATCHES=$(echo "$CONTENT" | grep -c "$keyword" 2>/dev/null || true)
        if [[ "$MATCHES" -gt 0 ]]; then
            if [[ $FILE_VIOLATIONS -eq 0 ]]; then
                echo "❌ FAILED: $file"
            fi
            # Show first match with line number from original file
            FIRST_MATCH=$(grep -n "$keyword" "$file" 2>/dev/null | head -1 || true)
            echo "   [$keyword] ($MATCHES occurrences) → $FIRST_MATCH"
            FILE_VIOLATIONS=$((FILE_VIOLATIONS + MATCHES))
        fi
    done

    if [[ $FILE_VIOLATIONS -gt 0 ]]; then
        VIOLATION_FILES=$((VIOLATION_FILES + 1))
        VIOLATIONS=$((VIOLATIONS + FILE_VIOLATIONS))
    fi

done < <(find "$REQUIREMENTS_DIR" -name "*.md" -type f ! -path "*/archive/*" ! -name "README.md" ! -name "INDEX.md" 2>/dev/null | sort)

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  檢查結果"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "掃描文件數:   $TOTAL_FILES"
echo "違規文件數:   $VIOLATION_FILES"
echo "違規總數:     $VIOLATIONS"

if [[ $VIOLATIONS -gt 0 ]]; then
    PURITY=$(( (TOTAL_FILES - VIOLATION_FILES) * 100 / TOTAL_FILES ))
    echo "業務純度:     ${PURITY}% (目標: ≥95%)"
    echo ""
    echo "❌ 發現技術關鍵詞 (應移至 Architecture 層)"
    exit 1
else
    echo "業務純度:     100%"
    echo ""
    echo "✅ 所有需求文件業務純度合格"
    exit 0
fi
