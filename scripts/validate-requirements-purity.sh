#!/bin/bash
# Requirements Layer Business Purity Validation Script
#
# Purpose: Verify that Requirements layer documents contain only business content
# Usage: ./scripts/validate-requirements-purity.sh
# Exit: 0 if pure, 1 if violations detected

# Technical keywords that should NOT appear in business requirements
# (except in cross-reference lines like "→ **[...](...)" or "Related Doc:" or "Refinement Note:")
# Using precise patterns to avoid false positives with business prose

# Core infrastructure (no false positives)
KEYWORDS="Redis|PostgreSQL|MySQL|HikariCP|Redisson|Kafka|Elasticsearch"

# Cryptographic algorithms (use precise forms)
KEYWORDS="$KEYWORDS|HMAC-SHA256|SHA-256|SHA256|AES-256|AES-GCM|Base64"

# Framework annotations (Spring/Java - no false positives)
KEYWORDS="$KEYWORDS|@Transactional|@Service|@Controller|@Component|@Autowired|@Cacheable"
KEYWORDS="$KEYWORDS|RestTemplate|@Repository|@Mapper"

# Architecture patterns (use word boundaries to avoid matching in prose)
KEYWORDS="$KEYWORDS| TCC | SAGA compensation| SAGA pattern| SAGA flow"

# Database DDL (precise SQL statements only)
KEYWORDS="$KEYWORDS|CREATE TABLE|CREATE INDEX|ALTER TABLE|DROP TABLE|ADD CONSTRAINT"

# Database DML (match full SQL patterns only, avoid prose words)
KEYWORDS="$KEYWORDS|SELECT .* FROM|INSERT INTO|UPDATE .* SET|DELETE FROM"
KEYWORDS="$KEYWORDS|primary key|foreign key|FOREIGN KEY|PRIMARY KEY"
KEYWORDS="$KEYWORDS|JSONB|GIN index|B-tree index"

# HTTP status codes (technical implementation details)
KEYWORDS="$KEYWORDS|HTTP 503|HTTP 200|HTTP 201|HTTP 400|HTTP 401|HTTP 404"

# SmartAdmin internals (should not appear in business requirements)
KEYWORDS="$KEYWORDS|ResponseDTO|PageResult|SmartBeanUtil|SmartPageUtil"

REQUIREMENTS_DIR="docs/iGaming/requirements"
VIOLATION_COUNT=0
VIOLATION_DETAILS=""

echo "========================================="
echo "Requirements Layer Business Purity Check"
echo "========================================="
echo ""
echo "Scanning directory: $REQUIREMENTS_DIR"
echo "Keywords: $KEYWORDS"
echo ""

# Check if requirements directory exists
if [ ! -d "$REQUIREMENTS_DIR" ]; then
    echo "❌ ERROR: Requirements directory not found: $REQUIREMENTS_DIR"
    exit 1
fi

# Find all markdown files (excluding README.md)
for file in $(find $REQUIREMENTS_DIR -name "*.md" -not -name "README.md"); do
    # Extract violations (technical keywords NOT in cross-reference lines or metadata)
    violations=$(grep -inE "$KEYWORDS" "$file" | \
                 grep -v "→ \*\*\[" | \
                 grep -v "Related Doc:" | \
                 grep -v "> \*\*Refinement Note\*\*:" | \
                 grep -v "Refinement Note:" | \
                 grep -v "Technical Implementation](../../architecture" | \
                 grep -v "Technical details" | \
                 grep -v "technical details" | \
                 grep -v "Canonical Source" | \
                 grep -v "Canonical Implementation:" | \
                 grep -v "## .* Implementation" | \
                 grep -v "### .* Implementation" | \
                 grep -v "Implementation Timeline" | \
                 grep -v "Implementation Process" | \
                 grep -v "B{Select" | \
                 grep -v "Q --> R\[Update" | \
                 grep -v "A\[.*\]" | \
                 grep -v "    [A-Z] -->.*Update" | \
                 grep -v "    [A-Z] -->.*Select" | \
                 grep -v "    [A-Z] -->.*Delete" | \
                 grep -v "(moved to Architecture layer)" | \
                 grep -v "algorithms.*moved to" | \
                 grep -v "pattern.*moved to" | \
                 grep -v "implementation.*moved to" | \
                 grep -v "\*\*Note\*\*:" | \
                 grep -v "(.*is the approved implementation algorithm)" | \
                 grep -v "(Implementation algorithm:" | \
                 grep -v "| Enforce.*minimum standard |")

    if [ -n "$violations" ]; then
        echo "❌ FAILED: $file"
        echo "$violations"
        echo ""
        VIOLATION_COUNT=$((VIOLATION_COUNT + 1))
        VIOLATION_DETAILS="$VIOLATION_DETAILS\n$file:\n$violations\n"
    fi
done

echo "========================================="
echo "Summary"
echo "========================================="

if [ $VIOLATION_COUNT -eq 0 ]; then
    echo "✅ PASSED: All Requirements documents are business-pure"
    echo "No technical keywords found in business requirements layer"
    exit 0
else
    echo "❌ FAILED: $VIOLATION_COUNT document(s) contain technical keywords"
    echo ""
    echo "Action Items:"
    echo "1. Review violations listed above"
    echo "2. Remove technical keywords from Requirements layer"
    echo "3. Add cross-references to Architecture layer documents"
    echo "4. Update Refinement Note in document header"
    exit 1
fi
