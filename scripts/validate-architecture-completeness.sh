#!/bin/bash
# Architecture Layer Technical Completeness Validation Script
#
# Purpose: Verify that Architecture layer documents contain complete technical implementation
# Usage: ./scripts/validate-architecture-completeness.sh
# Exit: 0 if complete, 1 if missing back-references

ARCH_DIR="docs/iGaming/architecture"
TOTAL_DOCS=0
JAVA_COUNT=0
SQL_COUNT=0
YAML_COUNT=0
MERMAID_COUNT=0
BACKREF_COUNT=0
MISSING_BACKREFS=""

echo "========================================="
echo "Architecture Layer Completeness Check"
echo "========================================="
echo ""
echo "Scanning directory: $ARCH_DIR"
echo ""

# Check if architecture directory exists
if [ ! -d "$ARCH_DIR" ]; then
    echo "❌ ERROR: Architecture directory not found: $ARCH_DIR"
    exit 1
fi

# Process all markdown files (excluding README.md)
for file in $(find $ARCH_DIR -name "*.md" -not -name "README.md"); do
    TOTAL_DOCS=$((TOTAL_DOCS + 1))
    filename=$(basename "$file")

    # Check for Java code blocks (grep file directly to avoid echo truncation)
    if grep -q '```java' "$file" 2>/dev/null; then
        JAVA_COUNT=$((JAVA_COUNT + 1))
    fi

    # Check for SQL schema
    if grep -qiE '(CREATE TABLE|CREATE INDEX|ALTER TABLE)' "$file" 2>/dev/null; then
        SQL_COUNT=$((SQL_COUNT + 1))
    fi

    # Check for YAML config
    if grep -qE '```ya?ml' "$file" 2>/dev/null; then
        YAML_COUNT=$((YAML_COUNT + 1))
    fi

    # Check for Mermaid diagrams
    if grep -q '```mermaid' "$file" 2>/dev/null; then
        MERMAID_COUNT=$((MERMAID_COUNT + 1))
    fi

    # Check for back-reference to requirements (skip ADR files)
    if [[ "$file" == */adr/* ]]; then
        # ADR files are architecture decision records, no business requirements needed
        BACKREF_COUNT=$((BACKREF_COUNT + 1))
    elif grep -qE '> \*\*Business Requirements\*\*|> \*\*需求文檔\*\*|> \*\*業務需求|requirements/' "$file" 2>/dev/null; then
        BACKREF_COUNT=$((BACKREF_COUNT + 1))
    else
        echo "⚠️  Missing backref: $filename"
        MISSING_BACKREFS="$MISSING_BACKREFS\n  - $filename"
    fi
done

echo ""
echo "========================================="
echo "Summary Statistics"
echo "========================================="
echo "Total Documents: $TOTAL_DOCS"
echo ""
echo "Technical Content Coverage:"
echo "  Java Code:      $JAVA_COUNT ($(( JAVA_COUNT * 100 / TOTAL_DOCS ))%)"
echo "  SQL Schema:     $SQL_COUNT ($(( SQL_COUNT * 100 / TOTAL_DOCS ))%)"
echo "  YAML Config:    $YAML_COUNT ($(( YAML_COUNT * 100 / TOTAL_DOCS ))%)"
echo "  Mermaid Diagram: $MERMAID_COUNT ($(( MERMAID_COUNT * 100 / TOTAL_DOCS ))%)"
echo ""
echo "Cross-Reference Integrity:"
echo "  Business Req Backref: $BACKREF_COUNT ($(( BACKREF_COUNT * 100 / TOTAL_DOCS ))%)"
echo ""

echo "========================================="
echo "Result"
echo "========================================="

if [ $BACKREF_COUNT -eq $TOTAL_DOCS ]; then
    echo "✅ PASSED: All Architecture documents have business requirements back-references"
    echo ""
    echo "Technical Coverage:"
    echo "  - Java code: $(( JAVA_COUNT * 100 / TOTAL_DOCS ))% of documents"
    echo "  - SQL Schema: $(( SQL_COUNT * 100 / TOTAL_DOCS ))% of documents"
    echo "  - YAML Config: $(( YAML_COUNT * 100 / TOTAL_DOCS ))% of documents"
    echo "  - Mermaid Diagrams: $(( MERMAID_COUNT * 100 / TOTAL_DOCS ))% of documents"
    exit 0
else
    MISSING_COUNT=$((TOTAL_DOCS - BACKREF_COUNT))
    echo "❌ FAILED: $MISSING_COUNT document(s) missing business requirements back-references"
    echo ""
    echo "Missing Back-References:"
    echo -e "$MISSING_BACKREFS"
    echo ""
    echo "Action Items:"
    echo "1. Add '> **Business Requirements**: [...]' header to documents listed above"
    echo "2. Ensure cross-reference points to correct Requirements layer document"
    echo "3. Update 'Last Synced' timestamp"
    exit 1
fi
