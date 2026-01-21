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
if [ "$AGENT_COUNT" -eq 8 ]; then
    print_success "Found 8 agents"
else
    print_error "Expected 8 agents, found $AGENT_COUNT"
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
    print_warning "SmartAdmin pattern found in $PATTERN_COUNT agent files"
    echo "   This may indicate duplication."
else
    print_success "No duplication detected"
fi
echo ""

echo "7. Checking documentation..."
if [ -f ".claude/README.md" ]; then
    print_success "README.md exists"
else
    print_warning "README.md missing"
fi

if [ -f ".claude/docs/OPTIMIZATION-COMPLETE.md" ]; then
    if grep -q "v2.3.0" ".claude/docs/OPTIMIZATION-COMPLETE.md"; then
        print_success "Documentation version up to date"
    else
        print_warning "Documentation may need version update"
    fi
else
    print_error "OPTIMIZATION-COMPLETE.md missing"
fi
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
