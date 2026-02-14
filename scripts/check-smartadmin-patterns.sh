#!/bin/bash
# scripts/check-smartadmin-patterns.sh
# Check SmartAdmin pattern compliance in architecture documentation
# Validates Java code examples follow SmartAdmin conventions
#
# Usage: ./scripts/check-smartadmin-patterns.sh
# Exit: 0 = compliant (≥95%), 1 = violations found

set -euo pipefail

ARCH_DIR="docs/iGaming/architecture"
VIOLATIONS=0
TOTAL_FILES_WITH_JAVA=0
COMPLIANT_FILES=0

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  SmartAdmin Pattern Compliance Check"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [[ ! -d "$ARCH_DIR" ]]; then
    echo "Directory not found: $ARCH_DIR"
    exit 0
fi

echo ""

while IFS= read -r file; do
    # Only check files that contain Java code blocks
    if ! grep -q '```java' "$file" 2>/dev/null; then
        continue
    fi

    TOTAL_FILES_WITH_JAVA=$((TOTAL_FILES_WITH_JAVA + 1))
    FILE_VIOLATIONS=0

    # Check 1: @Autowired field injection (anti-pattern)
    if grep -qE '@Autowired' "$file" 2>/dev/null; then
        # Verify it's inside a java code block (not just mentioned in text)
        if python3 -c "
import re, sys
with open('$file', 'r') as f:
    content = f.read()
java_blocks = re.findall(r'\`\`\`java(.*?)\`\`\`', content, re.DOTALL)
for block in java_blocks:
    # Match actual @Autowired annotations (line starts with optional whitespace + @Autowired)
    if re.search(r'^\s*@Autowired', block, re.MULTILINE):
        sys.exit(1)
sys.exit(0)
" 2>/dev/null; then
            : # Not in java block, OK
        else
            echo "  ⚠️  @Autowired in Java code: $file"
            FILE_VIOLATIONS=$((FILE_VIOLATIONS + 1))
        fi
    fi

    # Check 2: java.util.Optional instead of Vavr Option in Service layer
    if grep -qE 'java\.util\.Optional' "$file" 2>/dev/null; then
        if python3 -c "
import re, sys
with open('$file', 'r') as f:
    content = f.read()
java_blocks = re.findall(r'\`\`\`java(.*?)\`\`\`', content, re.DOTALL)
for block in java_blocks:
    if 'java.util.Optional' in block or 'Optional<' in block:
        if 'Service' in block or 'service' in '$file'.lower():
            sys.exit(1)
sys.exit(0)
" 2>/dev/null; then
            : # OK
        else
            echo "  ⚠️  java.util.Optional in Service code: $file"
            FILE_VIOLATIONS=$((FILE_VIOLATIONS + 1))
        fi
    fi

    # Check 3: @Transactional outside Manager layer context
    # Improved: check nearest class declaration before each @Transactional
    if grep -qE '@Transactional' "$file" 2>/dev/null; then
        if python3 -c "
import re, sys
with open('$file', 'r') as f:
    content = f.read()
java_blocks = re.findall(r'\`\`\`java(.*?)\`\`\`', content, re.DOTALL)
for block in java_blocks:
    if '@Transactional' not in block:
        continue
    # Find all class declarations and @Transactional annotation positions
    # Use ^\s*@Transactional to match only actual annotations, not comments
    class_decls = [(m.start(), m.group(1)) for m in re.finditer(r'class\s+(\w+)', block)]
    trans_pos = [m.start() for m in re.finditer(r'^\s*@Transactional', block, re.MULTILINE)]
    for tpos in trans_pos:
        # Find nearest class declaration before this @Transactional
        nearest_class = None
        for cpos, cname in class_decls:
            if cpos < tpos:
                nearest_class = cname
        if nearest_class and nearest_class.endswith('Service') and not nearest_class.endswith('Manager'):
            sys.exit(1)
sys.exit(0)
" 2>/dev/null; then
            : # OK
        else
            echo "  ⚠️  @Transactional in Service class: $file"
            FILE_VIOLATIONS=$((FILE_VIOLATIONS + 1))
        fi
    fi

    if [[ $FILE_VIOLATIONS -eq 0 ]]; then
        COMPLIANT_FILES=$((COMPLIANT_FILES + 1))
    else
        VIOLATIONS=$((VIOLATIONS + FILE_VIOLATIONS))
    fi

done < <(find "$ARCH_DIR" -name "*.md" -type f ! -path "*/archive/*" ! -name "README.md" ! -name "INDEX.md" 2>/dev/null | sort)

# Calculate compliance
if [[ $TOTAL_FILES_WITH_JAVA -gt 0 ]]; then
    PCT=$((COMPLIANT_FILES * 100 / TOTAL_FILES_WITH_JAVA))
else
    PCT=100
fi

TARGET=95

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Results"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Files with Java code: $TOTAL_FILES_WITH_JAVA"
echo "Compliant files: $COMPLIANT_FILES"
echo "Violations: $VIOLATIONS"
echo "Compliance: ${PCT}%"
echo "Target: ≥${TARGET}%"

if [[ $PCT -ge $TARGET ]]; then
    echo "✅ SmartAdmin pattern compliance meets target"
    exit 0
else
    echo "❌ SmartAdmin pattern compliance below target"
    exit 1
fi
