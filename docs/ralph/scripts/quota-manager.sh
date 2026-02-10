#!/bin/bash
#
# Ralph Loop - Quota Manager
#
# Aggregates token usage from the last 30 minutes and calculates quota status.
#
# Usage:
#   bash quota-manager.sh > ../quota.json
#
# Environment variables (for testing):
#   TEST_MODE=1        - Use test metrics directory
#   METRICS_DIR=path   - Override metrics directory
#   WINDOW_MINUTES=30  - Override time window (default: 30)
#

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
METRICS_DIR="${METRICS_DIR:-${SCRIPT_DIR}/../metrics}"
THRESHOLDS_FILE="${SCRIPT_DIR}/../thresholds.yml"
WINDOW_MINUTES="${WINDOW_MINUTES:-30}"

# Detect if jq is available
if ! command -v jq &> /dev/null; then
  echo '{"error": "jq is not installed. Please install jq to use quota-manager."}' >&2
  exit 1
fi

# Get current time and window start time
NOW=$(date +%s)
WINDOW_START=$((NOW - WINDOW_MINUTES * 60))
WINDOW_START_ISO=$(date -u -d "@$WINDOW_START" +"%Y-%m-%dT%H:%M:%SZ" 2>/dev/null || date -u -r "$WINDOW_START" +"%Y-%m-%dT%H:%M:%SZ")
NOW_ISO=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

# Find all metrics files modified in the last WINDOW_MINUTES
# Note: On Windows Git Bash, find uses Unix paths
RECENT_FILES=$(find "$METRICS_DIR" -name "*.json" -type f -mmin -"$WINDOW_MINUTES" 2>/dev/null || echo "")

if [ -z "$RECENT_FILES" ]; then
  # No recent files, output minimal quota status
  cat <<EOF
{
  "window_start": "$WINDOW_START_ISO",
  "window_end": "$NOW_ISO",
  "accumulated": {
    "input_tokens": 0,
    "output_tokens": 0,
    "cached_tokens": 0,
    "effective_tokens": 0,
    "api_calls": 0
  },
  "failure_count": 0,
  "success_count": 0,
  "estimated_quota": {
    "tier": "tier-4",
    "remaining_input_pct": 100.0
  },
  "risk_score": 0.0,
  "recommendation": "continue"
}
EOF
  exit 0
fi

# Aggregate data from all recent files
AGGREGATED=$(echo "$RECENT_FILES" | while read -r file; do
  if [ -f "$file" ]; then
    cat "$file"
  fi
done | jq -s '
  # Flatten all arrays into a single array
  flatten |
  # Calculate aggregated metrics
  {
    input_tokens: (map(.tokens.input // 0) | add // 0),
    output_tokens: (map(.tokens.output // 0) | add // 0),
    cached_tokens: (map(.tokens.cached // 0) | add // 0),
    api_calls: (map(.api_calls // 1) | add // 0),
    success_count: (map(select(.success == true)) | length),
    failure_count: (map(select(.success == false)) | length)
  } |
  # Calculate effective tokens (cached discount: 90%)
  . + {
    effective_tokens: (.input_tokens + .output_tokens - (.cached_tokens * 0.9 | floor))
  }
')

# Extract aggregated values
INPUT_TOKENS=$(echo "$AGGREGATED" | jq -r '.input_tokens')
OUTPUT_TOKENS=$(echo "$AGGREGATED" | jq -r '.output_tokens')
CACHED_TOKENS=$(echo "$AGGREGATED" | jq -r '.cached_tokens')
EFFECTIVE_TOKENS=$(echo "$AGGREGATED" | jq -r '.effective_tokens')
API_CALLS=$(echo "$AGGREGATED" | jq -r '.api_calls')
SUCCESS_COUNT=$(echo "$AGGREGATED" | jq -r '.success_count')
FAILURE_COUNT=$(echo "$AGGREGATED" | jq -r '.failure_count')

# Read tier limits from thresholds.yml (default to tier-4 if not found)
if [ -f "$THRESHOLDS_FILE" ]; then
  # Extract tier (simple grep, not full YAML parser)
  API_TIER=$(grep -E "^api_tier:" "$THRESHOLDS_FILE" | awk '{print $2}' | tr -d '"' || echo "tier-4")
  INPUT_TPM=$(grep -A 5 "^  ${API_TIER}:" "$THRESHOLDS_FILE" | grep "input_tpm:" | awk '{print $2}' || echo "400000")
else
  API_TIER="tier-4"
  INPUT_TPM="400000"
fi

# Calculate usage percentage (for 30-minute window, divide by 30)
# TPM = Tokens Per Minute, so for 30 minutes: max_tokens = INPUT_TPM * 30
MAX_TOKENS_IN_WINDOW=$((INPUT_TPM * WINDOW_MINUTES))
TOKEN_USAGE_PCT=$(echo "scale=4; ($INPUT_TOKENS * 100.0) / $MAX_TOKENS_IN_WINDOW" | bc -l 2>/dev/null || echo "0.0")

# Calculate failure rate
TOTAL_CALLS=$((SUCCESS_COUNT + FAILURE_COUNT))
if [ "$TOTAL_CALLS" -gt 0 ]; then
  FAILURE_RATE=$(echo "scale=4; $FAILURE_COUNT / $TOTAL_CALLS" | bc -l)
else
  FAILURE_RATE="0.0"
fi

# Calculate API call rate (calls per minute)
API_CALL_RATE=$(echo "scale=4; $API_CALLS / $WINDOW_MINUTES" | bc -l)
# Normalize to percentage (assume max 50 calls per minute for tier-4)
API_CALL_RATE_PCT=$(echo "scale=4; ($API_CALL_RATE * 100.0) / 50" | bc -l)
if (( $(echo "$API_CALL_RATE_PCT > 100" | bc -l) )); then
  API_CALL_RATE_PCT="100.0"
fi

# Calculate 5-hour window proximity (placeholder - requires tracking session start time)
# For now, assume 0.0 (will be enhanced in threshold-checker.sh)
FIVE_HOUR_PROXIMITY="0.0"

# Calculate risk score using weighted formula
# risk_score = (token_usage_pct × 0.40) + (api_call_rate_pct × 0.30) + (failure_rate × 0.15) + (five_hour_proximity × 0.15)
RISK_SCORE=$(echo "scale=4; ($TOKEN_USAGE_PCT * 0.40 / 100) + ($API_CALL_RATE_PCT * 0.30 / 100) + ($FAILURE_RATE * 0.15) + ($FIVE_HOUR_PROXIMITY * 0.15)" | bc -l)

# Determine recommendation based on risk score
if (( $(echo "$RISK_SCORE < 0.3" | bc -l) )); then
  RECOMMENDATION="continue"
elif (( $(echo "$RISK_SCORE < 0.6" | bc -l) )); then
  RECOMMENDATION="monitor"
elif (( $(echo "$RISK_SCORE < 0.8" | bc -l) )); then
  RECOMMENDATION="rest_soon"
else
  RECOMMENDATION="rest_now"
fi

# Calculate remaining quota percentage
REMAINING_INPUT_PCT=$(echo "scale=2; 100.0 - $TOKEN_USAGE_PCT" | bc -l)

# Output JSON
cat <<EOF
{
  "window_start": "$WINDOW_START_ISO",
  "window_end": "$NOW_ISO",
  "accumulated": {
    "input_tokens": $INPUT_TOKENS,
    "output_tokens": $OUTPUT_TOKENS,
    "cached_tokens": $CACHED_TOKENS,
    "effective_tokens": $EFFECTIVE_TOKENS,
    "api_calls": $API_CALLS
  },
  "failure_count": $FAILURE_COUNT,
  "success_count": $SUCCESS_COUNT,
  "estimated_quota": {
    "tier": "$API_TIER",
    "input_tpm": $INPUT_TPM,
    "max_tokens_in_window": $MAX_TOKENS_IN_WINDOW,
    "token_usage_pct": $TOKEN_USAGE_PCT,
    "remaining_input_pct": $REMAINING_INPUT_PCT
  },
  "risk_components": {
    "token_usage_pct": $TOKEN_USAGE_PCT,
    "api_call_rate_pct": $API_CALL_RATE_PCT,
    "failure_rate": $FAILURE_RATE,
    "five_hour_proximity": $FIVE_HOUR_PROXIMITY
  },
  "risk_score": $RISK_SCORE,
  "recommendation": "$RECOMMENDATION"
}
EOF
