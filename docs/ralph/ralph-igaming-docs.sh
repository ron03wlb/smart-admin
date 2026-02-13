#!/bin/bash
# ralph-igaming-docs.sh
# Immortal Ralph Wiggum loop for iGaming documentation optimization
# Usage: ./docs/ralph/ralph-igaming-docs.sh [max-iterations]
# Recommended: tmux new-session -d -s ralph "bash docs/ralph/ralph-igaming-docs.sh"

# P22: Removed -e flag. set -e is an anti-pattern for daemon-style long-running scripts.
# All errors are handled explicitly via if/else and || operators.
set -uo pipefail

# ===== API Key Configuration =====
# Load from .env if ANTHROPIC_API_KEY not already set
if [ -z "${ANTHROPIC_API_KEY:-}" ]; then
  SCRIPT_DIR_INIT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  if [ -f "$SCRIPT_DIR_INIT/.env" ]; then
    # shellcheck disable=SC1091
    source "$SCRIPT_DIR_INIT/.env"
    echo "Loaded API key from .env"
  else
    echo "ERROR: ANTHROPIC_API_KEY not set and no .env found at $SCRIPT_DIR_INIT/.env"
    echo "Create docs/ralph/.env with: export ANTHROPIC_API_KEY='sk-ant-...'"
    exit 1
  fi
fi
export ANTHROPIC_API_KEY

# ===== Configuration =====
MAX_ITERATIONS=${1:-9999}  # Effectively infinite (~694 days @ 5s/iteration)

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
RATE_LIMIT_FILE="$RALPH_DIR/.rate-limit-until"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

# Rate limit handling
RATE_LIMIT_RETRY_COUNT=0
MAX_RATE_LIMIT_RETRIES=5

# Outcome tracking counters
PRODUCTIVE_COUNT=0
RATE_LIMITED_COUNT=0
ERROR_COUNT=0
IDLE_COUNT=0
NO_PROGRESS_COUNT=0

# ===== Pre-flight checks =====
echo -e "${GREEN}═══════════════════════════════════════════════${NC}"
echo -e "${GREEN}  Ralph Wiggum: iGaming Doc Optimization       ${NC}"
echo -e "${GREEN}  Mode: Immortal Loop (P22)                    ${NC}"
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
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Ralph loop started (immortal mode). Max iterations: $MAX_ITERATIONS" >> "$LOG_FILE"
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

# Write rate-limit-until file (P22: survives restarts)
write_rate_limit_file() {
  local WAIT_SECONDS="$1"
  local RESUME_AT=$(( $(date +%s) + WAIT_SECONDS ))
  echo "$RESUME_AT" > "$RATE_LIMIT_FILE"
}

clear_rate_limit_file() {
  rm -f "$RATE_LIMIT_FILE" 2>/dev/null || true
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
      echo -e "${YELLOW}Reset time too far ($RESET_TIME). Capping wait at 4 hours...${NC}"
      echo "[$(date '+%Y-%m-%d %H:%M:%S')] RATE_LIMIT: type=$LIMIT_TYPE, reset=$RESET_TIME, wait=240min (capped), retry=$RATE_LIMIT_RETRY_COUNT" >> "$LOG_FILE"
      WAIT_SECONDS=14400
    else
      WAIT_MINUTES=$((WAIT_SECONDS / 60))
      echo -e "${YELLOW}Daily limit. Waiting until $RESET_TIME ($WAIT_MINUTES min)...${NC}"
      echo "[$(date '+%Y-%m-%d %H:%M:%S')] RATE_LIMIT: type=$LIMIT_TYPE, reset=$RESET_TIME, wait=${WAIT_MINUTES}min, retry=$RATE_LIMIT_RETRY_COUNT" >> "$LOG_FILE"
    fi
    write_rate_limit_file "$WAIT_SECONDS"
    sleep $WAIT_SECONDS
    clear_rate_limit_file
    return 0
  fi

  # Priority 2: Detect 5-hour usage limit
  if echo "$OUTPUT" | grep -qiE "(5.?hour|five.?hour)"; then
    LIMIT_TYPE="5hour_limit"
    echo -e "${RED}5-hour limit detected. Waiting 60 minutes...${NC}"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] RATE_LIMIT: type=$LIMIT_TYPE, wait=60min, retry=$RATE_LIMIT_RETRY_COUNT" >> "$LOG_FILE"
    write_rate_limit_file 3600
    sleep 3600
    clear_rate_limit_file

    # Reset quota tracking
    rm -f "$RALPH_DIR/quota.json" 2>/dev/null || true
    return 0
  fi

  # Priority 3: Exponential backoff for unknown limits
  LIMIT_TYPE="unknown_exponential"
  ((RATE_LIMIT_RETRY_COUNT++)) || true
  WAIT_MINUTES=$((5 * (2 ** (RATE_LIMIT_RETRY_COUNT - 1))))
  [ $WAIT_MINUTES -gt 120 ] && WAIT_MINUTES=120

  echo -e "${YELLOW}Rate limit retry #$RATE_LIMIT_RETRY_COUNT. Waiting $WAIT_MINUTES min...${NC}"
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] RATE_LIMIT: type=$LIMIT_TYPE, wait=${WAIT_MINUTES}min, retry=$RATE_LIMIT_RETRY_COUNT" >> "$LOG_FILE"
  WAIT_SECONDS=$((WAIT_MINUTES * 60))
  write_rate_limit_file "$WAIT_SECONDS"
  sleep $WAIT_SECONDS
  clear_rate_limit_file

  # P22: Instead of exit, do a long rest and reset counter
  if [ $RATE_LIMIT_RETRY_COUNT -ge $MAX_RATE_LIMIT_RETRIES ]; then
    echo -e "${YELLOW}Rate limit retry limit reached ($MAX_RATE_LIMIT_RETRIES). Long rest (4 hours) then reset...${NC}"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] LONG_REST: rate_limit_retry_limit (count=$RATE_LIMIT_RETRY_COUNT), sleeping 4h" >> "$LOG_FILE"
    RATE_LIMIT_RETRY_COUNT=0
    write_rate_limit_file 14400
    sleep 14400
    clear_rate_limit_file
  fi
}

# ===== Monitoring Variables =====
LAST_QUOTA_CHECK=$(date +%s)
QUOTA_CHECK_INTERVAL=1800  # 30 minutes
ITERATION_START_TIME=$(date +%s)

# ===== Main Loop (P22: while true = immortal) =====
ITERATION=0
while true; do
  ((ITERATION++)) || true
  i=$ITERATION  # backward compatibility

  ELAPSED=$(( $(date +%s) - START_TIME ))
  HOURS=$(( ELAPSED / 3600 ))
  MINS=$(( (ELAPSED % 3600) / 60 ))

  # Progress stats from progress.md
  # Note: grep -c returns exit 1 when count=0, so use || true to suppress
  DONE=$(grep -c '^- \[x\]' "$PROGRESS_FILE" 2>/dev/null) || true
  TODO=$(grep -c '^- \[ \]' "$PROGRESS_FILE" 2>/dev/null) || true
  STUCK=$(grep -c '^- \[!\]' "$PROGRESS_FILE" 2>/dev/null) || true
  # Ensure numeric (default 0 if empty)
  DONE=${DONE:-0}; DONE=${DONE//[^0-9]/}; DONE=${DONE:-0}
  TODO=${TODO:-0}; TODO=${TODO//[^0-9]/}; TODO=${TODO:-0}
  STUCK=${STUCK:-0}; STUCK=${STUCK//[^0-9]/}; STUCK=${STUCK:-0}
  TOTAL=$((DONE + TODO + STUCK))
  if [ "$TOTAL" -gt 0 ]; then
    PCT=$((DONE * 100 / TOTAL))
  else
    PCT=0
  fi

  echo ""
  echo -e "${YELLOW}════════════════════════════════════════════════════${NC}"
  echo -e "${YELLOW}  Iteration #$i (immortal mode)                    ${NC}"
  echo -e "${YELLOW}  Runtime: ${HOURS}h ${MINS}m                      ${NC}"
  echo -e "${YELLOW}  Progress: $DONE/$TOTAL ($PCT%) | Stuck: $STUCK   ${NC}"
  echo -e "${YELLOW}════════════════════════════════════════════════════${NC}"

  echo "[$(date '+%Y-%m-%d %H:%M:%S')] Iteration #$i (done=$DONE todo=$TODO stuck=$STUCK)" >> "$LOG_FILE"

  # ===== 24-hour safety valve (P22: sleep + reset, NOT exit) =====
  if [ $ELAPSED -gt 86400 ]; then
    echo -e "${YELLOW}════════════════════════════════════════════${NC}"
    echo -e "${YELLOW}  24-hour cycle. Resting 2 hours...         ${NC}"
    echo -e "${YELLOW}════════════════════════════════════════════${NC}"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] 24h cycle at iteration #$i, resting 2h" >> "$LOG_FILE"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] CYCLE_SUMMARY: productive=$PRODUCTIVE_COUNT rate_limited=$RATE_LIMITED_COUNT errors=$ERROR_COUNT idle=$IDLE_COUNT" >> "$LOG_FILE"
    write_rate_limit_file 7200
    sleep 7200
    clear_rate_limit_file
    START_TIME=$(date +%s)  # Reset 24h timer
    echo -e "${GREEN}24h rest complete. Starting new cycle.${NC}"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] New 24h cycle started" >> "$LOG_FILE"
  fi

  # ===== Pre-iteration quota probe (P22) =====
  if [ -f "$RATE_LIMIT_FILE" ]; then
    RESUME_AT=$(cat "$RATE_LIMIT_FILE" 2>/dev/null || echo "0")
    NOW_EPOCH=$(date +%s)
    if [ "$RESUME_AT" -gt "$NOW_EPOCH" ] 2>/dev/null; then
      WAIT_SECS=$((RESUME_AT - NOW_EPOCH))
      WAIT_MINS=$((WAIT_SECS / 60))
      echo -e "${YELLOW}Quota recovery in progress, waiting ${WAIT_MINS} minutes...${NC}"
      echo "[$(date '+%Y-%m-%d %H:%M:%S')] QUOTA_PROBE: sleeping ${WAIT_MINS}min" >> "$LOG_FILE"
      sleep "$WAIT_SECS"
      clear_rate_limit_file
      echo -e "${GREEN}Quota recovered, resuming.${NC}"
      echo "[$(date '+%Y-%m-%d %H:%M:%S')] QUOTA_PROBE: recovered, resuming" >> "$LOG_FILE"
    else
      # Expired rate limit file, remove it
      clear_rate_limit_file
    fi
  fi

  # ===== Pre-iteration quality gate probe (P25) =====
  # Run quality gate to determine what still needs work
  GATE_FAILURES=""
  GATE_EXIT=0
  if [ -x "$RALPH_DIR/validate-quality-gate.sh" ]; then
    GATE_OUTPUT=$("$RALPH_DIR/validate-quality-gate.sh" 2>&1) || GATE_EXIT=$?
    # Extract failing gates for context injection
    GATE_FAILURES=$(echo "$GATE_OUTPUT" | grep -E '❌|FAIL' | sed 's/\x1B\[[0-9;]*m//g' || true)
  fi

  # ===== Check if truly complete (TODO=0 AND quality gate passes) =====
  if [ "$TODO" -eq 0 ] && [ "$DONE" -gt 0 ]; then
    if [ "$GATE_EXIT" -eq 0 ]; then
      echo -e "${GREEN}════════════════════════════════════════════${NC}"
      echo -e "${GREEN}  ALL TASKS COMPLETE + QUALITY GATE PASSED! ${NC}"
      echo -e "${GREEN}  Iteration: #$i | Runtime: ${HOURS}h ${MINS}m${NC}"
      echo -e "${GREEN}  Tasks completed: $DONE                    ${NC}"
      echo -e "${GREEN}════════════════════════════════════════════${NC}"
      echo "[$(date '+%Y-%m-%d %H:%M:%S')] COMPLETE (pre-check, QG PASSED) at iteration #$i" >> "$LOG_FILE"
      exit 0
    else
      echo -e "${YELLOW}════════════════════════════════════════════${NC}"
      echo -e "${YELLOW}  Tasks done but QUALITY GATE FAILED        ${NC}"
      echo -e "${YELLOW}  Continuing to fix failing gates...        ${NC}"
      echo -e "${YELLOW}════════════════════════════════════════════${NC}"
      echo "[$(date '+%Y-%m-%d %H:%M:%S')] TODO=0 but QG FAILED, continuing iteration #$i" >> "$LOG_FILE"
      echo "$GATE_FAILURES" >> "$LOG_FILE"
    fi
  fi

  # ===== Detect current phase for context hints =====
  CURRENT_PHASE=$(grep -oE 'Phase 1[1-4]' "$PROGRESS_FILE" 2>/dev/null | tail -1 || echo "unknown")
  if [[ "$CURRENT_PHASE" == *"12"* ]]; then
    PHASE_HINT="Phase 12 (Mermaid Repair): Max 3 files per iteration. Use mmdc to validate."
  elif [[ "$CURRENT_PHASE" == *"13"* ]]; then
    PHASE_HINT="Phase 13 (Coverage Enhancement): Max 5 files per iteration. SmartAdmin patterns mandatory."
  elif [[ "$CURRENT_PHASE" == *"14"* ]]; then
    PHASE_HINT="Phase 14 (CI/CD Automation): Focus on workflow files and script integration."
  else
    PHASE_HINT="Max 5 files per iteration."
  fi

  # ===== Execute Claude Code =====
  PROMPT_CONTENT=$(cat "$PROMPT_FILE")

  OUTPUT=$(claude --dangerously-skip-permissions \
    "${PROMPT_CONTENT}

---
## Iteration Context (auto-injected)
- Current iteration: #$i (immortal mode)
- Runtime: ${HOURS}h ${MINS}m
- Tasks done: $DONE | Tasks remaining: $TODO | Tasks stuck: $STUCK
- Progress: $PCT%
- Phase hint: $PHASE_HINT
- Quality gate status: $([ "$GATE_EXIT" -eq 0 ] && echo 'ALL PASSED' || echo "FAILED — fix these gates before declaring RALPH_COMPLETE")
$([ -n "$GATE_FAILURES" ] && echo "- Failing gates:
$GATE_FAILURES" || echo "")

IMPORTANT: Do NOT output RALPH_COMPLETE unless ALL quality gates pass. If quality gates fail, focus on fixing the failing gates.
Begin immediately. Read docs/ralph/progress.md and docs/ralph/guardrails.md first, then execute the next unchecked task." \
    2>&1) || true

  # Log output (last 50 lines)
  echo "--- Iteration #$i output (last 50 lines) ---" >> "$LOG_FILE"
  echo "$OUTPUT" | tail -50 >> "$LOG_FILE"
  echo "--- End iteration #$i ---" >> "$LOG_FILE"

  # ===== Record iteration duration (P22: usage-tracker.js disabled — produces 0 tokens) =====
  ITERATION_END_TIME=$(date +%s)
  ITERATION_DURATION=$((ITERATION_END_TIME - ITERATION_START_TIME))
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] Iteration #$i duration=${ITERATION_DURATION}s" >> "$LOG_FILE"
  ITERATION_START_TIME=$(date +%s)

  # ===== Post-iteration validation (Phase 1 quality gates) =====
  echo -e "${CYAN}Running post-translation validation...${NC}"

  # Get files modified in this iteration (iGaming docs only)
  TRANSLATED_FILES=$(git diff --name-only HEAD~1 2>/dev/null | grep '^docs/iGaming/.*\.md$' || true)

  if [ -n "$TRANSLATED_FILES" ]; then
    VALIDATION_FAILED=0

    echo "  Files to validate: $(echo "$TRANSLATED_FILES" | wc -l)"

    # Run 4 validation scripts (non-blocking - warnings only)
    if bash scripts/check-technical-terms.sh $TRANSLATED_FILES 2>&1 | tee -a "$LOG_FILE"; then
      echo "  Technical terms check: PASS"
    else
      echo "  Technical terms check: FAIL (see log)"
      ((VALIDATION_FAILED++)) || true
    fi

    if bash scripts/validate-zh-tw-encoding.sh $TRANSLATED_FILES 2>&1 | tee -a "$LOG_FILE"; then
      echo "  Encoding check: PASS"
    else
      echo "  Encoding check: FAIL (see log)"
      ((VALIDATION_FAILED++)) || true
    fi

    if bash scripts/check-terminology-consistency-zh-tw.sh $TRANSLATED_FILES 2>&1 | tee -a "$LOG_FILE"; then
      echo "  Terminology check: PASS"
    else
      echo "  Terminology check: WARNING (non-blocking)"
    fi

    if bash scripts/validate-mermaid.sh $TRANSLATED_FILES 2>&1 | tee -a "$LOG_FILE"; then
      echo "  Mermaid syntax check: PASS"
    else
      echo "  Mermaid syntax check: FAIL (see log)"
      ((VALIDATION_FAILED++)) || true
    fi

    if [ $VALIDATION_FAILED -gt 0 ]; then
      echo -e "${YELLOW}$VALIDATION_FAILED validation check(s) failed (warnings logged)${NC}"
    else
      echo -e "${GREEN}All validation checks passed!${NC}"
      # Reset rate limit retry counter on successful iteration
      RATE_LIMIT_RETRY_COUNT=0
    fi

    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Validation: $VALIDATION_FAILED failures" >> "$LOG_FILE"
  else
    echo "  No iGaming files modified in this iteration (skipping validation)"
  fi

  # ===== Classify iteration outcome =====
  if [ -n "$TRANSLATED_FILES" ]; then
    OUTCOME="productive"
    ((PRODUCTIVE_COUNT++)) || true
    NO_PROGRESS_COUNT=0
  elif echo "$OUTPUT" | grep -qiE "(rate.?limit|usage.?limit|capacity|overloaded|429|5.?hour|five.?hour|hit.+limit|your.+limit|resets [0-9]+(am|pm))"; then
    OUTCOME="rate_limited"
    ((RATE_LIMITED_COUNT++)) || true
  elif echo "$OUTPUT" | grep -qiE "(error|exception|fatal)" && ! echo "$OUTPUT" | grep -qiE "(no error|without error)"; then
    OUTCOME="error"
    ((ERROR_COUNT++)) || true
    NO_PROGRESS_COUNT=0
  else
    OUTCOME="idle"
    ((IDLE_COUNT++)) || true
    ((NO_PROGRESS_COUNT++)) || true
  fi
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] Iteration #$i OUTCOME=$OUTCOME" >> "$LOG_FILE"

  # ===== No-progress detection =====
  if [ $NO_PROGRESS_COUNT -ge 3 ]; then
    echo -e "${YELLOW}WARNING: $NO_PROGRESS_COUNT consecutive idle iterations. Extending cooldown to 60s...${NC}"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] NO_PROGRESS: $NO_PROGRESS_COUNT consecutive idle iterations" >> "$LOG_FILE"
    sleep 60
  fi

  # ===== Check completion signal (P25: only exit if quality gate passes) =====
  if echo "$OUTPUT" | grep -q "RALPH_COMPLETE"; then
    echo -e "${CYAN}══════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  RALPH_COMPLETE signal received. Verifying... ${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════${NC}"

    # Run quality gate to verify
    FINAL_GATE_EXIT=0
    if [ -x "$RALPH_DIR/validate-quality-gate.sh" ]; then
      "$RALPH_DIR/validate-quality-gate.sh" 2>&1 | tee -a "$LOG_FILE" || FINAL_GATE_EXIT=$?
    fi

    if [ "$FINAL_GATE_EXIT" -eq 0 ]; then
      echo -e "${GREEN}══════════════════════════════════════════════${NC}"
      echo -e "${GREEN}  RALPH_COMPLETE + QUALITY GATE PASSED!       ${NC}"
      echo -e "${GREEN}  Iteration: #$i | Runtime: ${HOURS}h ${MINS}m${NC}"
      echo -e "${GREEN}══════════════════════════════════════════════${NC}"
      echo "[$(date '+%Y-%m-%d %H:%M:%S')] RALPH_COMPLETE at iteration #$i (QG PASSED)" >> "$LOG_FILE"
      exit 0
    else
      echo -e "${YELLOW}══════════════════════════════════════════════${NC}"
      echo -e "${YELLOW}  RALPH_COMPLETE rejected: quality gate FAILED ${NC}"
      echo -e "${YELLOW}  Continuing to fix failing gates...          ${NC}"
      echo -e "${YELLOW}══════════════════════════════════════════════${NC}"
      echo "[$(date '+%Y-%m-%d %H:%M:%S')] RALPH_COMPLETE rejected (QG FAILED) at iteration #$i, continuing" >> "$LOG_FILE"
    fi
  fi

  # ===== Enhanced rate limit + 5-hour window detection =====
  if echo "$OUTPUT" | grep -qiE "(rate.?limit|usage.?limit|capacity|overloaded|429|5.?hour|five.?hour|hit.+limit|your.+limit|resets [0-9]+(am|pm))"; then
    echo -e "${YELLOW}API limit detected${NC}"

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

  # ===== Quota monitoring (P22: simplified, outcome-based) =====
  NOW=$(date +%s)
  TIME_SINCE_LAST_CHECK=$((NOW - LAST_QUOTA_CHECK))

  if [ $TIME_SINCE_LAST_CHECK -ge $QUOTA_CHECK_INTERVAL ]; then
    echo -e "${CYAN}Quota status check...${NC}"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Quota check at iteration #$i (productive=$PRODUCTIVE_COUNT rate_limited=$RATE_LIMITED_COUNT errors=$ERROR_COUNT idle=$IDLE_COUNT)" >> "$LOG_FILE"

    # Simplified check: based on recent outcomes, not broken token data
    if [ $RATE_LIMITED_COUNT -gt 3 ]; then
      echo -e "${YELLOW}Frequent rate limits detected ($RATE_LIMITED_COUNT). Resting 30 minutes...${NC}"
      echo "[$(date '+%Y-%m-%d %H:%M:%S')] QUOTA_REST: 30min (rate_limited=$RATE_LIMITED_COUNT)" >> "$LOG_FILE"
      write_rate_limit_file 1800
      sleep 1800
      clear_rate_limit_file
      # Reset counter after rest
      RATE_LIMITED_COUNT=0
    fi

    LAST_QUOTA_CHECK=$NOW
  fi

  # Inter-iteration cooldown (5 seconds)
  sleep 5
done

# This point should never be reached (while true loop)
echo "[$(date '+%Y-%m-%d %H:%M:%S')] UNEXPECTED: Loop exited" >> "$LOG_FILE"
