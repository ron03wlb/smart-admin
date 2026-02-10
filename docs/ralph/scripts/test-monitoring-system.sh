#!/bin/bash
#
# End-to-End Test for Ralph Loop Monitoring System
#
# This script validates that all monitoring components work correctly together.
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RALPH_DIR="$SCRIPT_DIR/.."
TEST_DIR="/tmp/ralph-test-$$"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo "════════════════════════════════════════════════════════"
echo "  Ralph Loop Monitoring System - End-to-End Test"
echo "════════════════════════════════════════════════════════"
echo ""

# Create temporary test environment
mkdir -p "$TEST_DIR/metrics"
mkdir -p "$TEST_DIR/logs"

PASS_COUNT=0
FAIL_COUNT=0

# Test function
test_component() {
  local name="$1"
  local command="$2"

  echo -n "Testing $name... "

  if eval "$command" > /dev/null 2>&1; then
    echo -e "${GREEN}✅ PASS${NC}"
    ((PASS_COUNT++))
    return 0
  else
    echo -e "${RED}❌ FAIL${NC}"
    ((FAIL_COUNT++))
    return 1
  fi
}

# ===== Test 1: usage-tracker.js =====
echo "═══ Test 1: usage-tracker.js ═══"

cat <<'EOF' | node "$SCRIPT_DIR/usage-tracker.js" \
  --iteration=1 \
  --duration=30 \
  --log-file="$TEST_DIR/metrics/test1.json" > /dev/null 2>&1
Input tokens: 10000
Output tokens: 2000
Cached tokens: 5000
✅ Task completed
EOF

test_component "usage-tracker.js execution" "test -f $TEST_DIR/metrics/test1.json"
test_component "usage-tracker.js JSON format" "node -e 'JSON.parse(require(\"fs\").readFileSync(\"$TEST_DIR/metrics/test1.json\", \"utf8\"))'"
test_component "usage-tracker.js token extraction" "node -e 'const data=JSON.parse(require(\"fs\").readFileSync(\"$TEST_DIR/metrics/test1.json\", \"utf8\")); if (data[0].tokens.input !== 10000) process.exit(1)'"

echo ""

# ===== Test 2: quota-manager.js =====
echo "═══ Test 2: quota-manager.js ═══"

# Create multiple test metrics files with recent timestamps
for i in {1..3}; do
  cat > "$TEST_DIR/metrics/test$i.json" <<EOF
[
  {"timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)", "iteration": $i, "tokens": {"input": 10000, "output": 2000, "cached": 5000, "total": 7500}, "api_calls": 1, "duration_seconds": 30, "success": true}
]
EOF
  touch "$TEST_DIR/metrics/test$i.json"
done

node "$SCRIPT_DIR/quota-manager.js" \
  --metrics-dir="$TEST_DIR/metrics" \
  --window-minutes=60 > "$TEST_DIR/quota.json" 2>&1

test_component "quota-manager.js execution" "test -f $TEST_DIR/quota.json"
test_component "quota-manager.js JSON format" "node -e 'JSON.parse(require(\"fs\").readFileSync(\"$TEST_DIR/quota.json\", \"utf8\"))'"
test_component "quota-manager.js aggregation" "node -e 'const data=JSON.parse(require(\"fs\").readFileSync(\"$TEST_DIR/quota.json\", \"utf8\")); if (data.accumulated.input_tokens !== 30000) process.exit(1)'"
test_component "quota-manager.js risk_score" "node -e 'const data=JSON.parse(require(\"fs\").readFileSync(\"$TEST_DIR/quota.json\", \"utf8\")); if (typeof data.risk_score !== \"number\") process.exit(1)'"

echo ""

# ===== Test 3: threshold-checker.sh =====
echo "═══ Test 3: threshold-checker.sh ═══"

# Create a low-risk quota file
cat > "$TEST_DIR/quota.json" <<EOF
{
  "risk_score": 0.2,
  "risk_components": {
    "token_usage_pct": 30.0,
    "api_call_rate_pct": 10.0,
    "failure_rate": 0.0,
    "five_hour_proximity": 0.0
  },
  "recommendation": "continue"
}
EOF

QUOTA_FILE="$TEST_DIR/quota.json" bash "$SCRIPT_DIR/threshold-checker.sh" > /dev/null 2>&1
THRESHOLD_EXIT=$?

test_component "threshold-checker.sh low-risk (exit 0)" "test $THRESHOLD_EXIT -eq 0"

# Create a high-risk quota file
cat > "$TEST_DIR/quota.json" <<EOF
{
  "risk_score": 0.85,
  "risk_components": {
    "token_usage_pct": 85.0,
    "api_call_rate_pct": 70.0,
    "failure_rate": 0.1,
    "five_hour_proximity": 0.8
  },
  "recommendation": "rest_now"
}
EOF

QUOTA_FILE="$TEST_DIR/quota.json" bash "$SCRIPT_DIR/threshold-checker.sh" > /dev/null 2>&1
THRESHOLD_EXIT=$?

test_component "threshold-checker.sh high-risk (exit 1)" "test $THRESHOLD_EXIT -eq 1"

echo ""

# ===== Test 4: rest-strategy.sh =====
echo "═══ Test 4: rest-strategy.sh ═══"

# Test low risk (0.2)
cat > "$TEST_DIR/quota.json" <<EOF
{"risk_score": 0.2}
EOF
REST_MINUTES=$(QUOTA_FILE="$TEST_DIR/quota.json" bash "$SCRIPT_DIR/rest-strategy.sh")
test_component "rest-strategy.sh low-risk (0 min)" "test $REST_MINUTES -eq 0"

# Test moderate risk (0.5)
cat > "$TEST_DIR/quota.json" <<EOF
{"risk_score": 0.5}
EOF
REST_MINUTES=$(QUOTA_FILE="$TEST_DIR/quota.json" bash "$SCRIPT_DIR/rest-strategy.sh")
test_component "rest-strategy.sh moderate-risk (5-10 min)" "test $REST_MINUTES -ge 5 && test $REST_MINUTES -le 10"

# Test high risk (0.7)
cat > "$TEST_DIR/quota.json" <<EOF
{"risk_score": 0.7}
EOF
REST_MINUTES=$(QUOTA_FILE="$TEST_DIR/quota.json" bash "$SCRIPT_DIR/rest-strategy.sh")
test_component "rest-strategy.sh high-risk (15-30 min)" "test $REST_MINUTES -ge 15 && test $REST_MINUTES -le 30"

# Test critical risk (0.9)
cat > "$TEST_DIR/quota.json" <<EOF
{"risk_score": 0.9}
EOF
REST_MINUTES=$(QUOTA_FILE="$TEST_DIR/quota.json" bash "$SCRIPT_DIR/rest-strategy.sh")
test_component "rest-strategy.sh critical-risk (45-60 min)" "test $REST_MINUTES -ge 45 && test $REST_MINUTES -le 60"

echo ""

# ===== Test 5: thresholds.yml =====
echo "═══ Test 5: thresholds.yml ═══"

test_component "thresholds.yml exists" "test -f $RALPH_DIR/thresholds.yml"
test_component "thresholds.yml api_tier" "grep -q 'api_tier:' $RALPH_DIR/thresholds.yml"
test_component "thresholds.yml token_thresholds" "grep -q 'token_thresholds:' $RALPH_DIR/thresholds.yml"
test_component "thresholds.yml rest_strategy" "grep -q 'rest_strategy:' $RALPH_DIR/thresholds.yml"

echo ""

# ===== Cleanup =====
rm -rf "$TEST_DIR"

# ===== Summary =====
echo "════════════════════════════════════════════════════════"
echo "  Test Results Summary"
echo "════════════════════════════════════════════════════════"
echo ""
echo -e "  ${GREEN}PASS${NC}: $PASS_COUNT tests"
echo -e "  ${RED}FAIL${NC}: $FAIL_COUNT tests"
echo ""

if [ $FAIL_COUNT -eq 0 ]; then
  echo -e "${GREEN}✅ All tests passed!${NC}"
  echo ""
  echo "Ralph Loop monitoring system is ready for use."
  echo ""
  echo "Next steps:"
  echo "  1. Review configuration: docs/ralph/thresholds.yml"
  echo "  2. Test with dry-run: bash docs/ralph/ralph-igaming-docs.sh 3"
  echo "  3. Launch in tmux: tmux new-session -d -s ralph-igaming \\\"bash docs/ralph/ralph-igaming-docs.sh 120\\\""
  echo ""
  exit 0
else
  echo -e "${RED}❌ Some tests failed.${NC}"
  echo ""
  echo "Please review the failed tests above and fix the issues."
  exit 1
fi
