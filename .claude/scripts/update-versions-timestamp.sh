#!/bin/bash
#
# update-versions-timestamp.sh - Auto-update VERSIONS.yml last_updated timestamp
#
# Usage:
#   ./update-versions-timestamp.sh [--dry-run]
#
# This script updates the last_updated field in .claude/skills/VERSIONS.yml
# to the current date. Designed to be called from pre-commit hooks.
#
# Version: 1.0.0
# Created: 2026-02-07

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
VERSIONS_FILE="$PROJECT_ROOT/.claude/skills/VERSIONS.yml"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Parse arguments
DRY_RUN=false
if [[ "$1" == "--dry-run" ]]; then
    DRY_RUN=true
fi

# Check if VERSIONS.yml exists
if [[ ! -f "$VERSIONS_FILE" ]]; then
    echo -e "${RED}Error: VERSIONS.yml not found at $VERSIONS_FILE${NC}"
    exit 1
fi

# Get current date in YYYY-MM-DD format
TODAY=$(date +%Y-%m-%d)

# Get current last_updated value
CURRENT_DATE=$(grep -E "^last_updated:" "$VERSIONS_FILE" | sed 's/last_updated: *"\?\([0-9-]*\)"\?/\1/' | tr -d ' ')

# Check if update is needed
if [[ "$CURRENT_DATE" == "$TODAY" ]]; then
    echo -e "${GREEN}VERSIONS.yml already up to date (${TODAY})${NC}"
    exit 0
fi

# Display what will be changed
echo -e "${YELLOW}Updating VERSIONS.yml:${NC}"
echo "  Old date: $CURRENT_DATE"
echo "  New date: $TODAY"

if $DRY_RUN; then
    echo -e "${YELLOW}[DRY RUN] No changes made${NC}"
    exit 0
fi

# Perform the update
# Use sed to replace the last_updated line
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS requires empty string for -i
    sed -i '' "s/last_updated: .*/last_updated: \"$TODAY\"/" "$VERSIONS_FILE"
else
    # Linux
    sed -i "s/last_updated: .*/last_updated: \"$TODAY\"/" "$VERSIONS_FILE"
fi

# Verify the update
UPDATED_DATE=$(grep -E "^last_updated:" "$VERSIONS_FILE" | sed 's/last_updated: *"\?\([0-9-]*\)"\?/\1/' | tr -d ' ')

if [[ "$UPDATED_DATE" == "$TODAY" ]]; then
    echo -e "${GREEN}Successfully updated VERSIONS.yml to ${TODAY}${NC}"
    exit 0
else
    echo -e "${RED}Error: Failed to update VERSIONS.yml${NC}"
    exit 1
fi
