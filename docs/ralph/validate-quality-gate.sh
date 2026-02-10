#!/bin/bash
# validate-quality-gate.sh
# Quality gate validation for iGaming documentation
# Usage: ./docs/ralph/validate-quality-gate.sh

set -uo pipefail

# Get project root from script location (portable)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
IGAMING_DIR="$PROJECT_ROOT/docs/iGaming"
ARCH_DIR="$IGAMING_DIR/architecture"
REQ_DIR="$IGAMING_DIR/requirements"

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

PASS=0
FAIL=0
WARN=0

echo "═══════════════════════════════════════════════"
echo "  iGaming Documentation Quality Gate"
echo "  Date: $(date '+%Y-%m-%d %H:%M:%S')"
echo "═══════════════════════════════════════════════"
echo ""

# ===== Gate 1: Cross-Reference Completeness =====
echo "── Gate 1: Cross-Reference Completeness ──"

# 1a: Architecture → Requirements back-references
ARCH_TOTAL=$(find "$ARCH_DIR" -name "*.md" -not -name "README.md" -not -path "*/quality-reports/*" -not -path "*/adr/*" -type f | wc -l | tr -d ' ')
ARCH_WITH_BACKREF=$(grep -rl 'Business Requirements' "$ARCH_DIR" --include="*.md" | grep -v README.md | grep -v quality-reports | grep -v adr | wc -l | tr -d ' ')
ARCH_PCT=$((ARCH_WITH_BACKREF * 100 / ARCH_TOTAL))

if [ "$ARCH_PCT" -ge 95 ]; then
  echo -e "  ${GREEN}✅ Architecture → Requirements: $ARCH_WITH_BACKREF/$ARCH_TOTAL ($ARCH_PCT%) [target: ≥95%]${NC}"
  PASS=$((PASS + 1))
else
  echo -e "  ${RED}❌ Architecture → Requirements: $ARCH_WITH_BACKREF/$ARCH_TOTAL ($ARCH_PCT%) [target: ≥95%]${NC}"
  FAIL=$((FAIL + 1))
fi

# 1b: Requirements → Architecture forward-references
REQ_TOTAL=$(find "$REQ_DIR" -name "*.md" -not -name "README.md" -type f | wc -l | tr -d ' ')
REQ_WITH_FWDREF=$(grep -rl 'Related Architecture\|Related Doc.*architecture\|> \*\*Technical Implementation\*\*:' "$REQ_DIR" --include="*.md" | grep -v README.md | wc -l | tr -d ' ')
REQ_PCT=$((REQ_WITH_FWDREF * 100 / REQ_TOTAL))

if [ "$REQ_PCT" -ge 95 ]; then
  echo -e "  ${GREEN}✅ Requirements → Architecture: $REQ_WITH_FWDREF/$REQ_TOTAL ($REQ_PCT%) [target: ≥95%]${NC}"
  PASS=$((PASS + 1))
else
  echo -e "  ${RED}❌ Requirements → Architecture: $REQ_WITH_FWDREF/$REQ_TOTAL ($REQ_PCT%) [target: ≥95%]${NC}"
  FAIL=$((FAIL + 1))
fi

echo ""

# ===== Gate 2: Canonical Source Display Text Consistency =====
echo "── Gate 2: Canonical Source Display Text ──"

STALE_DISPLAY=$(grep -rl '\[source/' "$IGAMING_DIR" --include="*.md" | grep -v source-archive | wc -l | tr -d ' ')

if [ "$STALE_DISPLAY" -eq 0 ]; then
  echo -e "  ${GREEN}✅ No stale 'source/' display text found${NC}"
  PASS=$((PASS + 1))
else
  echo -e "  ${YELLOW}⚠️  $STALE_DISPLAY files with stale 'source/' display text${NC}"
  WARN=$((WARN + 1))
  grep -rl '\[source/' "$IGAMING_DIR" --include="*.md" | grep -v source-archive | while read f; do
    echo "     - ${f#$PROJECT_ROOT/}"
  done
fi

echo ""

# ===== Gate 3: Architecture Technical Coverage =====
echo "── Gate 3: Architecture Technical Coverage ──"

# Count core architecture docs (excluding Overview, READMEs, quality-reports, adr, 09_Infrastructure)
CORE_ARCH=$(find "$ARCH_DIR" -name "*.md" -not -name "README.md" -not -path "*/quality-reports/*" -not -path "*/adr/*" -not -path "*/00_Overview/*" -not -path "*/09_Infrastructure/*" -type f | wc -l | tr -d ' ')

JAVA_COUNT=$(grep -rl '```java' "$ARCH_DIR" --include="*.md" | grep -v README.md | grep -v quality-reports | grep -v adr | grep -v 00_Overview | grep -v 09_Infrastructure | wc -l | tr -d ' ')
MERMAID_COUNT=$(grep -rl '```mermaid' "$ARCH_DIR" --include="*.md" | grep -v README.md | grep -v quality-reports | grep -v adr | grep -v 00_Overview | grep -v 09_Infrastructure | wc -l | tr -d ' ')
SQL_COUNT=$(grep -rl '```sql' "$ARCH_DIR" --include="*.md" | grep -v README.md | grep -v quality-reports | grep -v adr | grep -v 00_Overview | grep -v 09_Infrastructure | wc -l | tr -d ' ')

if [ "$CORE_ARCH" -gt 0 ]; then
  JAVA_PCT=$((JAVA_COUNT * 100 / CORE_ARCH))
  MERMAID_PCT=$((MERMAID_COUNT * 100 / CORE_ARCH))
  SQL_PCT=$((SQL_COUNT * 100 / CORE_ARCH))
else
  JAVA_PCT=0
  MERMAID_PCT=0
  SQL_PCT=0
fi

# Java coverage
if [ "$JAVA_PCT" -ge 80 ]; then
  echo -e "  ${GREEN}✅ Java code: $JAVA_COUNT/$CORE_ARCH ($JAVA_PCT%) [target: ≥80%]${NC}"
  PASS=$((PASS + 1))
else
  echo -e "  ${RED}❌ Java code: $JAVA_COUNT/$CORE_ARCH ($JAVA_PCT%) [target: ≥80%]${NC}"
  FAIL=$((FAIL + 1))
fi

# Mermaid coverage
if [ "$MERMAID_PCT" -ge 80 ]; then
  echo -e "  ${GREEN}✅ Mermaid diagrams: $MERMAID_COUNT/$CORE_ARCH ($MERMAID_PCT%) [target: ≥80%]${NC}"
  PASS=$((PASS + 1))
else
  echo -e "  ${RED}❌ Mermaid diagrams: $MERMAID_COUNT/$CORE_ARCH ($MERMAID_PCT%) [target: ≥80%]${NC}"
  FAIL=$((FAIL + 1))
fi

# SQL coverage
if [ "$SQL_PCT" -ge 60 ]; then
  echo -e "  ${GREEN}✅ SQL schema: $SQL_COUNT/$CORE_ARCH ($SQL_PCT%) [target: ≥60%]${NC}"
  PASS=$((PASS + 1))
else
  echo -e "  ${RED}❌ SQL schema: $SQL_COUNT/$CORE_ARCH ($SQL_PCT%) [target: ≥60%]${NC}"
  FAIL=$((FAIL + 1))
fi

echo ""

# ===== Gate 4: Mermaid Syntax Validation =====
echo "── Gate 4: Mermaid Syntax Validation ──"

if [ -x "$PROJECT_ROOT/scripts/detect-statediagram-br.sh" ]; then
  SD_ISSUES=$("$PROJECT_ROOT/scripts/detect-statediagram-br.sh" "$IGAMING_DIR" 2>&1 | grep -c "ERROR\|VIOLATION" || true)
  if [ "$SD_ISSUES" -eq 0 ]; then
    echo -e "  ${GREEN}✅ No stateDiagram <br/> violations${NC}"
    PASS=$((PASS + 1))
  else
    echo -e "  ${RED}❌ $SD_ISSUES stateDiagram <br/> violations found${NC}"
    FAIL=$((FAIL + 1))
  fi
else
  echo -e "  ${YELLOW}⚠️  detect-statediagram-br.sh not found, skipping${NC}"
  WARN=$((WARN + 1))
fi

echo ""

# ===== Gate 5: Requirements Business Purity =====
echo "── Gate 5: Requirements Business Purity ──"

TECH_KEYWORDS="@Transactional|@Service|@Controller|@Component|@Autowired|@Cacheable|@Repository|@Mapper|CREATE TABLE|DROP TABLE|ALTER TABLE|SELECT .* FROM|INSERT INTO|ResponseDTO|PageResult|SmartBeanUtil"
IMPURE=$(grep -rlE "$TECH_KEYWORDS" "$REQ_DIR" --include="*.md" | grep -v README.md | wc -l | tr -d ' ')

if [ "$IMPURE" -eq 0 ]; then
  echo -e "  ${GREEN}✅ Requirements layer is business-pure (0 violations)${NC}"
  PASS=$((PASS + 1))
else
  echo -e "  ${RED}❌ $IMPURE files contain technical keywords${NC}"
  FAIL=$((FAIL + 1))
fi

echo ""

# ===== Summary =====
echo "═══════════════════════════════════════════════"
TOTAL_GATES=$((PASS + FAIL))
echo "  Results: $PASS/$TOTAL_GATES passed, $FAIL failed, $WARN warnings"

if [ "$FAIL" -eq 0 ]; then
  echo -e "  ${GREEN}══ QUALITY GATE: PASSED ══${NC}"
  echo "═══════════════════════════════════════════════"
  exit 0
else
  echo -e "  ${RED}══ QUALITY GATE: FAILED ══${NC}"
  echo "═══════════════════════════════════════════════"
  exit 1
fi
