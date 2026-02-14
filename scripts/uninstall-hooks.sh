#!/bin/bash
# scripts/uninstall-hooks.sh
# SmartAdmin Git Hooks Uninstaller
# Removes hooks installed by install-hooks.sh, restoring backups if available
#
# Usage: ./scripts/uninstall-hooks.sh
#
# Version: 1.0.0
# Date: 2026-02-09

set -euo pipefail

# --- Colors ---
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BOLD='\033[1m'
NC='\033[0m'

BLUE='\033[0;34m'
log_info()    { echo -e "${BLUE}[INFO]${NC} $*"; }
log_success() { echo -e "${GREEN}[OK]${NC} $*"; }
log_warn()    { echo -e "${YELLOW}[WARN]${NC} $*"; }

# --- Project root ---
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
HOOKS_DIR="$PROJECT_ROOT/.git/hooks"

echo ""
echo -e "${BOLD}SmartAdmin Git Hooks Uninstaller${NC}"
echo ""

# Verify git repository
if [[ ! -d "$PROJECT_ROOT/.git" ]]; then
    echo -e "${RED}[ERROR]${NC} Not a git repository: $PROJECT_ROOT"
    exit 1
fi

HOOKS=("pre-commit" "pre-push")
REMOVED=0

# Check for SmartAdmin hooks and remove them
for hook in "${HOOKS[@]}"; do
    hook_path="$HOOKS_DIR/$hook"
    if [[ -f "$hook_path" ]]; then
        if grep -q "SmartAdmin Git" "$hook_path" 2>/dev/null; then
            rm "$hook_path"
            log_success "Removed SmartAdmin $hook hook"
            REMOVED=$((REMOVED + 1))
        else
            log_warn "$hook exists but is not a SmartAdmin hook (skipped)"
        fi
    fi
done

# Find and restore latest backup
LATEST_BACKUP=$(find "$HOOKS_DIR" -maxdepth 1 -type d -name "backup-*" 2>/dev/null | sort -r | head -1)

if [[ -n "$LATEST_BACKUP" ]] && [[ -d "$LATEST_BACKUP" ]]; then
    echo ""
    log_info "Found backup: $LATEST_BACKUP"
    RESTORED=0

    for hook in "${HOOKS[@]}"; do
        if [[ -f "$LATEST_BACKUP/$hook" ]] && [[ ! -f "$HOOKS_DIR/$hook" ]]; then
            cp "$LATEST_BACKUP/$hook" "$HOOKS_DIR/$hook"
            chmod +x "$HOOKS_DIR/$hook"
            log_success "Restored $hook from backup"
            RESTORED=$((RESTORED + 1))
        fi
    done

    if [[ $RESTORED -eq 0 ]]; then
        log_info "No hooks to restore from backup"
    fi
fi

echo ""
if [[ $REMOVED -gt 0 ]]; then
    echo -e "${GREEN}${BOLD}Uninstall complete.${NC} Removed $REMOVED hook(s)."
else
    echo -e "${YELLOW}No SmartAdmin hooks found to remove.${NC}"
fi
echo ""
echo "  Re-install: ./scripts/install-hooks.sh"
echo ""
