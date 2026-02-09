#!/bin/bash
# ralph-igaming-docs.sh
# 24-hour Ralph Wiggum loop for iGaming documentation optimization
# Usage: ./docs/ralph/ralph-igaming-docs.sh [max-iterations]
# Recommended: tmux new-session -d -s ralph-igaming "./docs/ralph/ralph-igaming-docs.sh 120"

set -euo pipefail

# ===== Configuration =====
MAX_ITERATIONS=${1:-120}
PROJECT_ROOT="/Users/zhangxuanrong/Documents/Workspace/Java/smart-admin"
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

# ===== Main Loop =====
for i in $(seq 1 $MAX_ITERATIONS); do
  ELAPSED=$(( $(date +%s) - START_TIME ))
  HOURS=$(( ELAPSED / 3600 ))
  MINS=$(( (ELAPSED % 3600) / 60 ))

  # Progress stats from progress.md
  DONE=$(grep -c '\- \[x\]' "$PROGRESS_FILE" 2>/dev/null || echo 0)
  TODO=$(grep -c '\- \[ \]' "$PROGRESS_FILE" 2>/dev/null || echo 0)
  STUCK=$(grep -c '\- \[!\]' "$PROGRESS_FILE" 2>/dev/null || echo 0)
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

  OUTPUT=$(claude --print --dangerously-skip-permissions \
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

  # ===== Rate limit detection =====
  if echo "$OUTPUT" | grep -qiE "(rate.?limit|usage.?limit|capacity|overloaded|429)"; then
    echo -e "${YELLOW}Rate limit detected. Waiting 5 minutes...${NC}"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Rate limit at iteration #$i, sleeping 300s" >> "$LOG_FILE"
    sleep 300
    continue
  fi

  # ===== Error detection =====
  if echo "$OUTPUT" | grep -qiE "(error|exception|fatal)" | head -1; then
    echo -e "${YELLOW}Possible error detected in output. Continuing...${NC}"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Possible error at iteration #$i" >> "$LOG_FILE"
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
