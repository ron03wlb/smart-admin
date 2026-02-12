#!/bin/bash
# ralph-igaming-docs.sh
# 24-hour Ralph Wiggum loop for iGaming documentation optimization
# Usage: ./docs/ralph/ralph-igaming-docs.sh [max-iterations]
# Recommended: tmux new-session -d -s ralph-igaming "./docs/ralph/ralph-igaming-docs.sh 120"

set -euo pipefail

# ===== Configuration =====
MAX_ITERATIONS=${1:-20}  # Temporary: 20 iterations for testing (was 120)

# Dynamically detect project root (cross-platform)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
RALPH_DIR="$PROJECT_ROOT/docs/ralph"
PROMPT_FILE="$RALPH_DIR/PROMPT.md"
PROGRESS_FILE="$RALPH_DIR/progress.md"
GUARDRAILS_FILE="$RALPH_DIR/guardrails.md"
LOG_DIR="$RALPH_DIR/logs"
LOG_FILE="$LOG_DIR/ralph-$(date +%Y%m%d-%H%M%S).log"
START_TIME=$(date +%s)

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

# Rate limit handling
RATE_LIMIT_RETRY_COUNT=0
MAX_RATE_LIMIT_RETRIES=5

# ===== Pre-flight checks =====
echo -e "${GREEN}═══════════════════════════════════════════════${NC}"
echo -e "${GREEN}  Ralph Wiggum: iGaming Doc Optimization       ${NC}"
echo -e "${GREEN}  Mode B (Bash Loop) — Fresh Context Each Run  ${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════${NC}"
echo ""
echo "  Max iterations : $MAX_ITERATIONS"
echo "  Project root   : $PROJECT_ROOT"
echo "  Log file       : $LOG_FILE"
echo ""

cd "$PROJECT_ROOT"

# Ensure log directory exists
mkdir -p "$LOG_DIR"

# Verify required files exist
for f in "$PROMPT_FILE" "$PROGRESS_FILE" "$GUARDRAILS_FILE"; do
  if [ ! -f "$f" ]; then
    echo -e "${RED}ERROR: $f not found. Create it first.${NC}"
    exit 1
  fi
done

echo -e "${CYAN}Starting loop at $(date)${NC}"
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Ralph loop started. Max iterations: $MAX_ITERATIONS" >> "$LOG_FILE"
echo ""

# ===== Rate Limit Handling Functions =====
calculate_wait_until() {
  local RESET_TIME="$1"  # e.g., "4pm" or "2am"

  # Parse hour and am/pm
  local HOUR=$(echo "$RESET_TIME" | grep -oE "[0-9]+")
  local AMPM=$(echo "$RESET_TIME" | grep -oE "(am|pm)")

  # Convert to 24-hour format
  if [ "$AMPM" = "pm" ] && [ "$HOUR" -ne 12 ]; then
    HOUR=$((HOUR + 12))
  elif [ "$AMPM" = "am" ] && [ "$HOUR" -eq 12 ]; then
    HOUR=0
  fi

  # Get current time in Asia/Taipei timezone
  local NOW_EPOCH=$(TZ="Asia/Taipei" date +%s)
  local RESET_EPOCH=$(TZ="Asia/Taipei" date -d "today $HOUR:00" +%s 2>/dev/null || date -v "${HOUR}H" +%s 2>/dev/null)

  # If reset time already passed today, add 24 hours
  if [ $RESET_EPOCH -le $NOW_EPOCH ]; then
    RESET_EPOCH=$(TZ="Asia/Taipei" date -d "tomorrow $HOUR:00" +%s 2>/dev/null || date -v +1d -v "${HOUR}H" +%s 2>/dev/null)
  fi

  # Fallback: if date calculation failed, return 3600 (60 minutes)
  if [ -z "$RESET_EPOCH" ] || [ "$RESET_EPOCH" = "0" ]; then
    echo "3600"
    return 0
  fi

  local WAIT_SECONDS=$((RESET_EPOCH - NOW_EPOCH))

  # Safety: cap at reasonable value
  if [ $WAIT_SECONDS -lt 0 ]; then
    WAIT_SECONDS=3600  # 60 minutes fallback
  fi

  echo "$WAIT_SECONDS"
}

handle_rate_limit() {
  local OUTPUT="$1"
  local LIMIT_TYPE="unknown"

  # Priority 1: Detect explicit reset time (daily limit)
  if echo "$OUTPUT" | grep -qE "resets [0-9]+(am|pm)"; then
    LIMIT_TYPE="daily_reset"
    RESET_TIME=$(echo "$OUTPUT" | grep -oE "[0-9]+(am|pm)" | head -1)
    WAIT_SECONDS=$(calculate_wait_until "$RESET_TIME")

    # Cap at 4 hours (safety)
    if [ $WAIT_SECONDS -gt 14400 ]; then
      echo -e "${YELLOW}⚠️ Reset time too far ($RESET_TIME). Capping wait at 4 hours...${NC}"
      echo "[$(date '+%Y-%m-%d %H:%M:%S')] RATE_LIMIT: type=$LIMIT_TYPE, reset=$RESET_TIME, wait=240min (capped), retry=$RATE_LIMIT_RETRY_COUNT" >> "$LOG_FILE"
      sleep 14400
    else
      WAIT_MINUTES=$((WAIT_SECONDS / 60))
      echo -e "${YELLOW}📅 Daily limit. Waiting until $RESET_TIME ($WAIT_MINUTES min)...${NC}"
      echo "[$(date '+%Y-%m-%d %H:%M:%S')] RATE_LIMIT: type=$LIMIT_TYPE, reset=$RESET_TIME, wait=${WAIT_MINUTES}min, retry=$RATE_LIMIT_RETRY_COUNT" >> "$LOG_FILE"
      sleep $WAIT_SECONDS
    fi
    return 0
  fi

  # Priority 2: Detect 5-hour usage limit
  if echo "$OUTPUT" | grep -qiE "(5.?hour|five.?hour)"; then
    LIMIT_TYPE="5hour_limit"
    echo -e "${RED}⏰ 5-hour limit detected. Waiting 60 minutes...${NC}"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] RATE_LIMIT: type=$LIMIT_TYPE, wait=60min, retry=$RATE_LIMIT_RETRY_COUNT" >> "$LOG_FILE"
    sleep 3600

    # Reset quota tracking
    rm -f "$RALPH_DIR/quota.json" 2>/dev/null || true
    return 0
  fi

  # Priority 3: Exponential backoff for unknown limits
  LIMIT_TYPE="unknown_exponential"
  ((RATE_LIMIT_RETRY_COUNT++))
  WAIT_MINUTES=$((5 * (2 ** (RATE_LIMIT_RETRY_COUNT - 1))))
  [ $WAIT_MINUTES -gt 120 ] && WAIT_MINUTES=120

  echo -e "${YELLOW}🔄 Rate limit retry #$RATE_LIMIT_RETRY_COUNT. Waiting $WAIT_MINUTES min...${NC}"
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] RATE_LIMIT: type=$LIMIT_TYPE, wait=${WAIT_MINUTES}min, retry=$RATE_LIMIT_RETRY_COUNT" >> "$LOG_FILE"
  sleep $((WAIT_MINUTES * 60))

  # Safety: exit if stuck for 5+ retries
  if [ $RATE_LIMIT_RETRY_COUNT -ge $MAX_RATE_LIMIT_RETRIES ]; then
    echo -e "${RED}🛑 SAFETY EXIT: Rate limit retry limit exceeded ($MAX_RATE_LIMIT_RETRIES attempts)${NC}"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] SAFETY_EXIT: rate_limit_retry_exceeded (count=$RATE_LIMIT_RETRY_COUNT)" >> "$LOG_FILE"
    exit 3
  fi
}

# ===== Monitoring Variables (added by usage monitoring system) =====
LAST_QUOTA_CHECK=$(date +%s)
QUOTA_CHECK_INTERVAL=1800  # 30 minutes
ITERATION_START_TIME=$(date +%s)

# ===== Main Loop =====
for i in $(seq 1 $MAX_ITERATIONS); do
  ELAPSED=$(( $(date +%s) - START_TIME ))
  HOURS=$(( ELAPSED / 3600 ))
  MINS=$(( (ELAPSED % 3600) / 60 ))

  # Progress stats from progress.md
  DONE=$(grep -c '^- \[x\]' "$PROGRESS_FILE" 2>/dev/null || true)
  TODO=$(grep -c '^- \[ \]' "$PROGRESS_FILE" 2>/dev/null || true)
  STUCK=$(grep -c '^- \[!\]' "$PROGRESS_FILE" 2>/dev/null || true)
  # Set default to 0 if empty
  DONE=${DONE:-0}
  TODO=${TODO:-0}
  STUCK=${STUCK:-0}
  TOTAL=$((DONE + TODO + STUCK))
  if [ "$TOTAL" -gt 0 ]; then
    PCT=$((DONE * 100 / TOTAL))
  else
    PCT=0
  fi

  echo ""
  echo -e "${YELLOW}════════════════════════════════════════════════════${NC}"
  echo -e "${YELLOW}  Iteration #$i / $MAX_ITERATIONS                  ${NC}"
  echo -e "${YELLOW}  Runtime: ${HOURS}h ${MINS}m                      ${NC}"
  echo -e "${YELLOW}  Progress: $DONE/$TOTAL ($PCT%) | Stuck: $STUCK   ${NC}"
  echo -e "${YELLOW}════════════════════════════════════════════════════${NC}"

  echo "[$(date '+%Y-%m-%d %H:%M:%S')] Iteration #$i (done=$DONE todo=$TODO stuck=$STUCK)" >> "$LOG_FILE"

  # ===== 24-hour safety valve =====
  if [ $ELAPSED -gt 86400 ]; then
    echo -e "${RED}════════════════════════════════════════════${NC}"
    echo -e "${RED}  24-hour limit reached. Stopping safely.  ${NC}"
    echo -e "${RED}════════════════════════════════════════════${NC}"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] 24h timeout at iteration #$i" >> "$LOG_FILE"
    exit 1
  fi

  # ===== Check if already complete =====
  if [ "$TODO" -eq 0 ] && [ "$DONE" -gt 0 ]; then
    echo -e "${GREEN}════════════════════════════════════════════${NC}"
    echo -e "${GREEN}  ALL TASKS COMPLETE before iteration #$i!  ${NC}"
    echo -e "${GREEN}  Runtime: ${HOURS}h ${MINS}m               ${NC}"
    echo -e "${GREEN}  Tasks completed: $DONE                    ${NC}"
    echo -e "${GREEN}════════════════════════════════════════════${NC}"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] COMPLETE (pre-check) at iteration #$i" >> "$LOG_FILE"
    exit 0
  fi

  # ===== Execute Claude Code =====
  PROMPT_CONTENT=$(cat "$PROMPT_FILE")

  OUTPUT=$(claude --dangerously-skip-permissions \
    "${PROMPT_CONTENT}

---
## Iteration Context (auto-injected)
- Current iteration: #$i / $MAX_ITERATIONS
- Runtime: ${HOURS}h ${MINS}m
- Tasks done: $DONE | Tasks remaining: $TODO | Tasks stuck: $STUCK
- Progress: $PCT%

Begin immediately. Read docs/ralph/progress.md and docs/ralph/guardrails.md first, then execute the next unchecked task." \
    2>&1) || true

  # Log output (last 50 lines)
  echo "--- Iteration #$i output (last 50 lines) ---" >> "$LOG_FILE"
  echo "$OUTPUT" | tail -50 >> "$LOG_FILE"
  echo "--- End iteration #$i ---" >> "$LOG_FILE"

  # ===== Record usage (added by usage monitoring system) =====
  ITERATION_END_TIME=$(date +%s)
  ITERATION_DURATION=$((ITERATION_END_TIME - ITERATION_START_TIME))

  # Log usage to metrics
  echo "$OUTPUT" | node "$RALPH_DIR/scripts/usage-tracker.js" \
    --iteration=$i \
    --duration=$ITERATION_DURATION \
    --log-file="$RALPH_DIR/metrics/$(date +%Y-%m-%d-%H%M).json" \
    2>&1 | tee -a "$LOG_FILE" || {
      echo "WARNING: usage-tracker failed, continuing anyway" | tee -a "$LOG_FILE"
    }

  ITERATION_START_TIME=$(date +%s)

  # ===== Post-iteration validation (Phase 1 quality gates) =====
  echo -e "${CYAN}🔍 Running post-translation validation...${NC}"

  # Get files modified in this iteration (iGaming docs only)
  TRANSLATED_FILES=$(git diff --name-only HEAD~1 2>/dev/null | grep '^docs/iGaming/.*\.md$' || true)

  if [ -n "$TRANSLATED_FILES" ]; then
    VALIDATION_FAILED=0

    echo "  Files to validate: $(echo "$TRANSLATED_FILES" | wc -l)"

    # Run 4 validation scripts (non-blocking - warnings only)
    if bash scripts/check-technical-terms.sh $TRANSLATED_FILES 2>&1 | tee -a "$LOG_FILE"; then
      echo "  ✓ Technical terms check: PASS"
    else
      echo "  ✗ Technical terms check: FAIL (see log)"
      ((VALIDATION_FAILED++))
    fi

    if bash scripts/validate-zh-tw-encoding.sh $TRANSLATED_FILES 2>&1 | tee -a "$LOG_FILE"; then
      echo "  ✓ Encoding check: PASS"
    else
      echo "  ✗ Encoding check: FAIL (see log)"
      ((VALIDATION_FAILED++))
    fi

    if bash scripts/check-terminology-consistency-zh-tw.sh $TRANSLATED_FILES 2>&1 | tee -a "$LOG_FILE"; then
      echo "  ✓ Terminology check: PASS"
    else
      echo "  ⚠ Terminology check: WARNING (non-blocking)"
    fi

    if bash scripts/validate-mermaid.sh $TRANSLATED_FILES 2>&1 | tee -a "$LOG_FILE"; then
      echo "  ✓ Mermaid syntax check: PASS"
    else
      echo "  ✗ Mermaid syntax check: FAIL (see log)"
      ((VALIDATION_FAILED++))
    fi

    if [ $VALIDATION_FAILED -gt 0 ]; then
      echo -e "${YELLOW}⚠️ $VALIDATION_FAILED validation check(s) failed (warnings logged)${NC}"
    else
      echo -e "${GREEN}✅ All validation checks passed!${NC}"
      # Reset rate limit retry counter on successful iteration
      RATE_LIMIT_RETRY_COUNT=0
    fi

    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Validation: $VALIDATION_FAILED failures" >> "$LOG_FILE"
  else
    echo "  No iGaming files modified in this iteration (skipping validation)"
  fi

  # ===== Check completion signal =====
  if echo "$OUTPUT" | grep -q "RALPH_COMPLETE"; then
    echo -e "${GREEN}══════════════════════════════════════════════${NC}"
    echo -e "${GREEN}  RALPH_COMPLETE signal received!              ${NC}"
    echo -e "${GREEN}  Iteration: #$i / $MAX_ITERATIONS             ${NC}"
    echo -e "${GREEN}  Runtime: ${HOURS}h ${MINS}m                  ${NC}"
    echo -e "${GREEN}══════════════════════════════════════════════${NC}"

    # Final validation
    echo ""
    echo "Running final quality gate..."
    if [ -x "$RALPH_DIR/validate-quality-gate.sh" ]; then
      "$RALPH_DIR/validate-quality-gate.sh" 2>&1 | tee -a "$LOG_FILE"
    fi

    echo "[$(date '+%Y-%m-%d %H:%M:%S')] RALPH_COMPLETE at iteration #$i" >> "$LOG_FILE"
    exit 0
  fi

  # ===== Enhanced rate limit + 5-hour window detection =====
  if echo "$OUTPUT" | grep -qiE "(rate.?limit|usage.?limit|capacity|overloaded|429|5.?hour|five.?hour|hit.+limit|your.+limit|resets [0-9]+(am|pm))"; then
    echo -e "${YELLOW}⚠️ API limit detected${NC}"

    # Use hybrid intelligent wait strategy
    handle_rate_limit "$OUTPUT"

    # Update last quota check time (for 5-hour limit case)
    LAST_QUOTA_CHECK=$(date +%s)

    continue
  fi

  # ===== Error detection =====
  if echo "$OUTPUT" | grep -qiE "(error|exception|fatal)"; then
    echo -e "${YELLOW}Possible error detected in output. Continuing...${NC}"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Possible error at iteration #$i" >> "$LOG_FILE"
  fi

  # ===== Quota monitoring (every 30 minutes) =====
  NOW=$(date +%s)
  TIME_SINCE_LAST_CHECK=$((NOW - LAST_QUOTA_CHECK))

  if [ $TIME_SINCE_LAST_CHECK -ge $QUOTA_CHECK_INTERVAL ]; then
    echo -e "${CYAN}🔍 Checking usage quota...${NC}"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Quota check triggered at iteration #$i" >> "$LOG_FILE"

    # Update quota cache (stdout → quota.json, stderr → log)
    node "$RALPH_DIR/scripts/quota-manager.js" > "$RALPH_DIR/quota.json" 2>> "$LOG_FILE" || {
      echo "WARNING: quota-manager failed, skipping check" | tee -a "$LOG_FILE"
      LAST_QUOTA_CHECK=$NOW
      continue
    }

    # Check thresholds
    bash "$RALPH_DIR/scripts/threshold-checker.sh"
    THRESHOLD_STATUS=$?

    if [ $THRESHOLD_STATUS -eq 1 ]; then
      # Calculate rest duration (using Node.js version for cross-platform compatibility)
      REST_MINUTES=$(node "$RALPH_DIR/scripts/rest-strategy.js")

      echo -e "${YELLOW}⏳ Resting for $REST_MINUTES minutes (quota management)...${NC}"
      echo "[$(date '+%Y-%m-%d %H:%M:%S')] REST triggered: ${REST_MINUTES} min" >> "$LOG_FILE"

      # Sleep
      sleep $((REST_MINUTES * 60))

      echo -e "${GREEN}✅ Rest completed. Resuming...${NC}"
      echo "[$(date '+%Y-%m-%d %H:%M:%S')] REST completed, resuming" >> "$LOG_FILE"

    elif [ $THRESHOLD_STATUS -eq 2 ]; then
      # Critical stop
      echo -e "${RED}🛑 CRITICAL: Quota threshold exceeded. Stopping safely.${NC}"
      echo "[$(date '+%Y-%m-%d %H:%M:%S')] CRITICAL_STOP: quota exceeded" >> "$LOG_FILE"
      exit 2
    fi

    LAST_QUOTA_CHECK=$NOW
  fi

  # Inter-iteration cooldown (5 seconds)
  sleep 5
done

echo ""
echo -e "${YELLOW}═════════════════════════════════════════════${NC}"
echo -e "${YELLOW}  Max iterations ($MAX_ITERATIONS) reached.  ${NC}"
echo -e "${YELLOW}═════════════════════════════════════════════${NC}"
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Max iterations reached" >> "$LOG_FILE"
exit 1
