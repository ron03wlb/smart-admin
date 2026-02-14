#!/bin/bash
#
# Ralph Loop - Rest Strategy Calculator
#
# Calculates optimal rest duration based on risk score.
#
# Usage:
#   REST_MINUTES=$(bash rest-strategy.sh)
#   echo "Rest for $REST_MINUTES minutes"
#
# Output: Number of minutes to rest (stdout)
#
# Environment variables (for testing):
#   TEST_MODE=1          - Enable test mode
#   QUOTA_FILE=path      - Override quota file path
#   THRESHOLDS_FILE=path - Override thresholds file path
#   FORCE_RISK_SCORE=0.75 - Override risk score for testing
#

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
QUOTA_FILE="${QUOTA_FILE:-${SCRIPT_DIR}/../quota.json}"
THRESHOLDS_FILE="${THRESHOLDS_FILE:-${SCRIPT_DIR}/../thresholds.yml}"

# Check if quota file exists
if [ ! -f "$QUOTA_FILE" ]; then
  echo "10" # Default: 10 minutes if no data
  exit 0
fi

# Read risk score from quota.json
if [ -n "${FORCE_RISK_SCORE:-}" ]; then
  RISK_SCORE="$FORCE_RISK_SCORE"
else
  RISK_SCORE=$(node -p "JSON.parse(require('fs').readFileSync('$QUOTA_FILE', 'utf8')).risk_score" 2>/dev/null || echo "0.5")
fi

# Read rest strategy from thresholds.yml (if exists)
if [ -f "$THRESHOLDS_FILE" ]; then
  # Extract rest strategy for different risk bands
  # Moderate (0.3-0.6): min_minutes / max_minutes
  MODERATE_MIN=$(grep -A 10 "rest_strategy:" "$THRESHOLDS_FILE" | grep -A 3 "moderate:" | grep "min_minutes:" | awk '{print $2}' || echo "5")
  MODERATE_MAX=$(grep -A 10 "rest_strategy:" "$THRESHOLDS_FILE" | grep -A 3 "moderate:" | grep "max_minutes:" | awk '{print $2}' || echo "10")

  # High (0.6-0.8): min_minutes / max_minutes
  HIGH_MIN=$(grep -A 10 "rest_strategy:" "$THRESHOLDS_FILE" | grep -A 3 "high:" | grep "min_minutes:" | awk '{print $2}' || echo "15")
  HIGH_MAX=$(grep -A 10 "rest_strategy:" "$THRESHOLDS_FILE" | grep -A 3 "high:" | grep "max_minutes:" | awk '{print $2}' || echo "30")

  # Critical (0.8-1.0): min_minutes / max_minutes
  CRITICAL_MIN=$(grep -A 10 "rest_strategy:" "$THRESHOLDS_FILE" | grep -A 3 "critical:" | grep "min_minutes:" | awk '{print $2}' || echo "45")
  CRITICAL_MAX=$(grep -A 10 "rest_strategy:" "$THRESHOLDS_FILE" | grep -A 3 "critical:" | grep "max_minutes:" | awk '{print $2}' || echo "60")
else
  # Default values
  MODERATE_MIN=5
  MODERATE_MAX=10
  HIGH_MIN=15
  HIGH_MAX=30
  CRITICAL_MIN=45
  CRITICAL_MAX=60
fi

# Calculate rest duration based on risk score
# Using linear interpolation within each band

if (( $(awk -v rs="$RISK_SCORE" 'BEGIN {print (rs < 0.3)}') )); then
  # Low risk: no rest needed
  REST_MINUTES=0

elif (( $(echo "$RISK_SCORE < 0.6" | bc -l) )); then
  # Moderate risk (0.3-0.6): linear interpolation
  # Formula: min + (score - 0.3) / (0.6 - 0.3) * (max - min)
  RANGE=$(echo "$MODERATE_MAX - $MODERATE_MIN" | bc -l)
  SCORE_OFFSET=$(echo "$RISK_SCORE - 0.3" | bc -l)
  REST_MINUTES=$(echo "$MODERATE_MIN + ($SCORE_OFFSET / 0.3) * $RANGE" | bc -l | awk '{print int($1+0.5)}')

elif (( $(echo "$RISK_SCORE < 0.8" | bc -l) )); then
  # High risk (0.6-0.8): linear interpolation
  RANGE=$(echo "$HIGH_MAX - $HIGH_MIN" | bc -l)
  SCORE_OFFSET=$(echo "$RISK_SCORE - 0.6" | bc -l)
  REST_MINUTES=$(echo "$HIGH_MIN + ($SCORE_OFFSET / 0.2) * $RANGE" | bc -l | awk '{print int($1+0.5)}')

else
  # Critical risk (0.8-1.0): linear interpolation
  RANGE=$(echo "$CRITICAL_MAX - $CRITICAL_MIN" | bc -l)
  SCORE_OFFSET=$(echo "$RISK_SCORE - 0.8" | bc -l)
  REST_MINUTES=$(echo "$CRITICAL_MIN + ($SCORE_OFFSET / 0.2) * $RANGE" | bc -l | awk '{print int($1+0.5)}')

  # Cap at maximum
  if (( REST_MINUTES > CRITICAL_MAX )); then
    REST_MINUTES=$CRITICAL_MAX
  fi
fi

# Output rest duration
echo "$REST_MINUTES"
