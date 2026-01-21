#!/bin/bash

set -e

echo "======================================"
echo "Claude Config Verification Script"
echo "======================================"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

ERRORS=0
WARNINGS=0

# Function to print colored output
print_error() {
    echo -e "${RED}❌ $1${NC}"
    ((ERRORS++))
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
    ((WARNINGS++))
}

echo "1. Checking agent count..."
AGENT_COUNT=$(ls -1 .claude/agents/*.md 2>/dev/null | wc -l)
if [ "$AGENT_COUNT" -eq 9 ]; then
    print_success "Found 9 agents"
else
    print_error "Expected 9 agents, found $AGENT_COUNT"
fi
echo ""

echo "2. Validating agent structure..."
for agent in .claude/agents/*.md; do
    agent_name=$(basename "$agent")

    if ! grep -q "^name:" "$agent"; then
        print_error "$agent_name: Missing 'name' field"
    fi

    if ! grep -q "^description:" "$agent"; then
        print_error "$agent_name: Missing 'description' field"
    fi

    if ! grep -q "^model:" "$agent"; then
        print_error "$agent_name: Missing 'model' field"
    fi
done
print_success "Agent structure validation complete"
echo ""

echo "3. Checking knowledge base files..."
KNOWLEDGE_FILES=(
    ".claude/shared/knowledge/smartadmin-patterns.md"
    ".claude/shared/knowledge/smartadmin-frontend-patterns.md"
    ".claude/shared/knowledge/project-architecture.md"
    ".claude/shared/knowledge/quality-standards.md"
)

for file in "${KNOWLEDGE_FILES[@]}"; do
    if [ -f "$file" ]; then
        print_success "Found: $(basename $file)"
    else
        print_error "Missing: $file"
    fi
done
echo ""

echo "4. Checking template files..."
TEMPLATE_FILES=(
    ".claude/shared/templates/agent-base.md"
    ".claude/shared/templates/technical-agent-mixin.md"
    ".claude/shared/templates/analysis-agent-mixin.md"
)

for file in "${TEMPLATE_FILES[@]}"; do
    if [ -f "$file" ]; then
        print_success "Found: $(basename $file)"
    else
        print_error "Missing: $file"
    fi
done
echo ""

echo "5. Checking orchestration files..."
ORCHESTRATION_FILES=(
    ".claude/shared/orchestration/decision-matrix.md"
    ".claude/shared/orchestration/agent-dependencies.md"
    ".claude/shared/orchestration/workflow-patterns.md"
)

for file in "${ORCHESTRATION_FILES[@]}"; do
    if [ -f "$file" ]; then
        print_success "Found: $(basename $file)"
    else
        print_error "Missing: $file"
    fi
done
echo ""

echo "6. Checking for duplication..."
PATTERN_COUNT=$(grep -r "Controller → Service → Manager → Dao" .claude/agents/ 2>/dev/null | wc -l)

if [ "$PATTERN_COUNT" -gt 1 ]; then
    print_warning "SmartAdmin pattern found in $PATTERN_COUNT agent files (potential duplication)"
    grep -r "Controller → Service → Manager → Dao" .claude/agents/ 2>/dev/null | head -5 | while read line; do
        echo "    $line"
    done
else
    print_success "No SmartAdmin pattern duplication detected"
fi

# Check ResponseDTO duplication
RESPONSE_COUNT=$(grep -r "ResponseDTO\.ok" .claude/agents/ 2>/dev/null | wc -l)
if [ "$RESPONSE_COUNT" -gt 2 ]; then
    print_warning "ResponseDTO pattern found in $RESPONSE_COUNT agent files"
fi

# Check LambdaQueryWrapper duplication
WRAPPER_COUNT=$(grep -r "LambdaQueryWrapper" .claude/agents/ 2>/dev/null | wc -l)
if [ "$WRAPPER_COUNT" -gt 2 ]; then
    print_warning "LambdaQueryWrapper pattern found in $WRAPPER_COUNT agent files"
fi
echo ""

echo "7. Checking documentation..."
if [ -f ".claude/README.md" ]; then
    print_success "README.md exists"
else
    print_warning "README.md missing"
fi

if [ -f ".claude/docs/OPTIMIZATION-COMPLETE.md" ]; then
    if grep -q "v2.3.0\|v2.4.0\|v2.5.0" ".claude/docs/OPTIMIZATION-COMPLETE.md"; then
        print_success "Documentation version up to date"
    else
        print_warning "Documentation may need version update"
    fi
else
    print_error "OPTIMIZATION-COMPLETE.md missing"
fi
echo ""

echo "8. Validating agent knowledge base references..."
for agent in .claude/agents/*.md; do
    agent_name=$(basename "$agent")

    # Check if agent references shared knowledge
    if grep -q "smartadmin-patterns.md" "$agent"; then
        print_success "$agent_name references smartadmin-patterns.md"
    else
        print_warning "$agent_name doesn't reference smartadmin-patterns.md"
    fi

    # Check for Foundation Knowledge section
    if grep -q "## Foundation Knowledge" "$agent"; then
        print_success "$agent_name has Foundation Knowledge section"
    else
        print_warning "$agent_name missing Foundation Knowledge section"
    fi
done
echo ""

echo "9. Checking for broken cross-references..."
BROKEN_REFS=0

# Find markdown files and check links
for file in $(find .claude -name "*.md" 2>/dev/null); do
    # Extract relative markdown links
    grep -o '\[.*\](\.\.*/.*\.md)' "$file" 2>/dev/null | while read link; do
        # Extract path from [text](path)
        ref_path=$(echo "$link" | sed 's/.*(\(.*\))/\1/')

        # Resolve relative to file's directory
        file_dir=$(dirname "$file")
        full_path="$file_dir/$ref_path"

        if [ ! -f "$full_path" ]; then
            print_error "Broken reference in $(basename $file): $ref_path"
            ((BROKEN_REFS++))
        fi
    done
done

if [ $BROKEN_REFS -eq 0 ]; then
    print_success "No broken cross-references found"
fi
echo ""

echo "10. Validating hook integration..."

# Check hooks.json exists
if [ -f ".claude/hooks.json" ]; then
    print_success "hooks.json exists"

    # Validate JSON syntax (if jq is available)
    if command -v jq &> /dev/null; then
        if jq empty .claude/hooks.json 2>/dev/null; then
            print_success "hooks.json is valid JSON"
        else
            print_error "hooks.json has invalid JSON syntax"
        fi
    fi
else
    print_warning "hooks.json missing (hooks system not configured)"
fi

# Check review agents support machine-readable output
for agent in code-reviewer architect-reviewer documentation-engineer; do
    if [ -f ".claude/agents/${agent}.md" ]; then
        if grep -q "Machine-Readable Output\|JSON output" ".claude/agents/${agent}.md"; then
            print_success "${agent} supports machine-readable output"
        else
            print_warning "${agent} missing machine-readable output section"
        fi
    fi
done
echo ""

echo "11. Collecting configuration metrics..."

TOTAL_LINES=$(find .claude -name "*.md" -exec wc -l {} + 2>/dev/null | tail -1 | awk '{print $1}')
AGENT_COUNT=$(ls -1 .claude/agents/*.md 2>/dev/null | wc -l)
KNOWLEDGE_LINES=$(find .claude/shared/knowledge -name "*.md" -exec wc -l {} + 2>/dev/null | tail -1 | awk '{print $1}')
ORCHESTRATION_LINES=$(find .claude/shared/orchestration -name "*.md" -exec wc -l {} + 2>/dev/null | tail -1 | awk '{print $1}')
MERMAID_COUNT=$(grep -r "```mermaid" .claude/shared/orchestration 2>/dev/null | wc -l)

echo "  Total markdown lines: $TOTAL_LINES"
echo "  Agent count: $AGENT_COUNT"
echo "  Knowledge base lines: $KNOWLEDGE_LINES"
echo "  Orchestration lines: $ORCHESTRATION_LINES"
echo "  Mermaid diagrams: $MERMAID_COUNT"
echo ""

echo "======================================"
echo "Verification Complete"
echo "======================================"
echo "Errors: $ERRORS"
echo "Warnings: $WARNINGS"
echo ""

if [ $ERRORS -gt 0 ]; then
    echo -e "${RED}Verification FAILED${NC}"
    exit 1
else
    echo -e "${GREEN}Verification PASSED${NC}"
    exit 0
fi
