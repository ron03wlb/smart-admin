#!/bin/bash
#
# Ralph Loop - Threshold Checker
#
# Checks if current usage exceeds thresholds and returns decision.
#
# Exit codes:
#   0 - Continue (safe to proceed)
#   1 - Rest (should take a break)
#   2 - Critical stop (emergency shutdown)
#
# Usage:
#   bash threshold-checker.sh
#   echo $?  # Check exit code
#
# Environment variables (for testing):
#   TEST_MODE=1          - Enable test mode
#   QUOTA_FILE=path      - Override quota file path
#   THRESHOLDS_FILE=path - Override thresholds file path
#

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
QUOTA_FILE="${QUOTA_FILE:-${SCRIPT_DIR}/../quota.json}"
THRESHOLDS_FILE="${THRESHOLDS_FILE:-${SCRIPT_DIR}/../thresholds.yml}"
LOG_DIR="${SCRIPT_DIR}/../logs"

# Ensure log directory exists
mkdir -p "$LOG_DIR"

# Check if quota file exists
if [ ! -f "$QUOTA_FILE" ]; then
  echo "⚠️ Warning: quota.json not found. Run quota-manager first." >&2
  exit 0  # Safe to continue (no data yet)
fi

# Read quota data once (single Node.js call for all values, cross-platform compatible)
QUOTA_VALUES=$(node -p "
  var q = JSON.parse(require('fs').readFileSync('$QUOTA_FILE', 'utf8'));
  [q.risk_score, q.risk_components.token_usage_pct, q.risk_components.failure_rate, q.recommendation].join(' ')
" 2>/dev/null || echo "0.0 0.0 0.0 continue")
read -r RISK_SCORE TOKEN_USAGE_PCT FAILURE_RATE RECOMMENDATION <<< "$QUOTA_VALUES"

# Read thresholds from thresholds.yml (if exists)
if [ -f "$THRESHOLDS_FILE" ]; then
  # Extract critical threshold (default: 90%)
  CRITICAL_THRESHOLD=$(grep -A 2 "token_thresholds:" "$THRESHOLDS_FILE" | grep -A 5 "input:" | grep "critical:" | awk '{print $2}' || echo "90")
  # Extract high threshold (default: 80%)
  HIGH_THRESHOLD=$(grep -A 2 "token_thresholds:" "$THRESHOLDS_FILE" | grep -A 5 "input:" | grep "high:" | awk '{print $2}' || echo "80")
else
  CRITICAL_THRESHOLD="90"
  HIGH_THRESHOLD="80"
fi

# Decision logic
# Priority 1: Critical token usage (>= 90%)
if (( $(echo "$TOKEN_USAGE_PCT >= $CRITICAL_THRESHOLD" | bc -l 2>/dev/null || echo "0") )); then
  echo "🛑 CRITICAL: Token usage ${TOKEN_USAGE_PCT}% >= ${CRITICAL_THRESHOLD}%" >&2
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] CRITICAL_STOP: token_usage=${TOKEN_USAGE_PCT}%" >> "$LOG_DIR/threshold-checker.log"
  exit 2  # Critical stop
fi

# Priority 2: High risk score (>= 0.8)
if (( $(echo "$RISK_SCORE >= 0.8" | bc -l 2>/dev/null || echo "0") )); then
  echo "⚠️ HIGH RISK: Risk score ${RISK_SCORE} >= 0.8" >&2
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] REST_REQUIRED: risk_score=${RISK_SCORE}" >> "$LOG_DIR/threshold-checker.log"
  exit 1  # Rest required
fi

# Priority 3: High token usage (>= 80%)
if (( $(echo "$TOKEN_USAGE_PCT >= $HIGH_THRESHOLD" | bc -l 2>/dev/null || echo "0") )); then
  echo "⚠️ HIGH USAGE: Token usage ${TOKEN_USAGE_PCT}% >= ${HIGH_THRESHOLD}%" >&2
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] REST_RECOMMENDED: token_usage=${TOKEN_USAGE_PCT}%" >> "$LOG_DIR/threshold-checker.log"
  exit 1  # Rest recommended
fi

# Priority 4: Moderate risk score (>= 0.6)
if (( $(echo "$RISK_SCORE >= 0.6" | bc -l 2>/dev/null || echo "0") )); then
  echo "ℹ️ MODERATE RISK: Risk score ${RISK_SCORE} >= 0.6" >&2
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] REST_RECOMMENDED: risk_score=${RISK_SCORE}" >> "$LOG_DIR/threshold-checker.log"
  exit 1  # Rest recommended
fi

# Priority 5: High failure rate (>= 0.3)
if (( $(echo "$FAILURE_RATE >= 0.3" | bc -l 2>/dev/null || echo "0") )); then
  echo "⚠️ HIGH FAILURE RATE: ${FAILURE_RATE} >= 0.3" >&2
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] REST_RECOMMENDED: failure_rate=${FAILURE_RATE}" >> "$LOG_DIR/threshold-checker.log"
  exit 1  # Rest recommended
fi

# Priority 6: Check recommendation from quota-manager
if [ "$RECOMMENDATION" = "rest_now" ]; then
  echo "ℹ️ RECOMMENDATION: rest_now" >&2
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] REST_RECOMMENDED: recommendation=rest_now" >> "$LOG_DIR/threshold-checker.log"
  exit 1  # Rest recommended
fi

# Safe to continue
echo "✅ SAFE: Continue (risk_score=${RISK_SCORE}, token_usage=${TOKEN_USAGE_PCT}%)" >&2
echo "[$(date '+%Y-%m-%d %H:%M:%S')] CONTINUE: risk_score=${RISK_SCORE}, token_usage=${TOKEN_USAGE_PCT}%" >> "$LOG_DIR/threshold-checker.log"
exit 0
