#!/bin/bash
# ====================================================================
# SmartAdmin Skills - Version Sync Tool
# ====================================================================
# Purpose: Sync versions between VERSIONS.yml and config.yml files
# Usage:
#   ./sync-skill-versions.sh --dry-run   # Check without modifying
#   ./sync-skill-versions.sh --update    # Sync all versions
# ====================================================================

set -e

VERSIONS_FILE=".claude/skills/VERSIONS.yml"
SKILLS_DIR=".claude/skills"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if VERSIONS.yml exists
if [ ! -f "$VERSIONS_FILE" ]; then
  echo -e "${RED}❌ Error: $VERSIONS_FILE not found${NC}"
  exit 1
fi

# Parse command line args
DRY_RUN=true
if [ "$1" = "--update" ]; then
  DRY_RUN=false
  echo "🔄 Sync Mode: UPDATE (will modify config.yml files)"
else
  echo "👁️  Sync Mode: DRY-RUN (check only, no modifications)"
fi

echo ""
echo "======================================================================"
echo "SmartAdmin Skills Version Sync"
echo "======================================================================"
echo ""

# Counters
TOTAL=0
IN_SYNC=0
OUT_OF_SYNC=0

# Find all config.yml files
find "$SKILLS_DIR" -name "config.yml" -not -path "*/.agents/*" -not -path "*/_shared/*" | sort | while read config_file; do
  TOTAL=$((TOTAL + 1))

  # Extract skill name from path
  skill_dir=$(dirname "$config_file")
  skill_name=$(basename "$skill_dir")

  # Extract version from config.yml
  config_version=$(grep -E "^\s+(skill_)?metadata:" -A10 "$config_file" | grep "version:" | head -1 | sed 's/.*version: "\(.*\)"/\1/' | tr -d ' ')

  if [ -z "$config_version" ]; then
    # Try alternative format: "version:" "x.x.x"
    config_version=$(grep -E "^\s+version:" "$config_file" | head -1 | sed 's/.*version: "\(.*\)"/\1/' | tr -d ' ')
  fi

  # Extract version from VERSIONS.yml
  yaml_version=$(grep -A3 "${skill_name}:" "$VERSIONS_FILE" | grep "version:" | head -1 | sed 's/.*version: "\(.*\)"/\1/' | tr -d ' ')

  if [ -z "$config_version" ]; then
    echo -e "${RED}⚠️  [$TOTAL] $skill_name: Cannot parse version from config.yml${NC}"
    continue
  fi

  if [ -z "$yaml_version" ]; then
    echo -e "${YELLOW}⚠️  [$TOTAL] $skill_name: Not found in VERSIONS.yml${NC}"
    continue
  fi

  if [ "$yaml_version" = "$config_version" ]; then
    echo -e "${GREEN}✅ [$TOTAL] $skill_name: v$yaml_version (in sync)${NC}"
    IN_SYNC=$((IN_SYNC + 1))
  else
    echo -e "${YELLOW}🔄 [$TOTAL] $skill_name: VERSIONS.yml v$yaml_version ≠ config.yml v$config_version${NC}"
    OUT_OF_SYNC=$((OUT_OF_SYNC + 1))

    # Update if not dry-run
    if [ "$DRY_RUN" = false ]; then
      # Update version in config.yml (simple sed approach)
      if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        sed -i '' "s/version: \"$config_version\"/version: \"$yaml_version\"/" "$config_file"
      else
        # Linux
        sed -i "s/version: \"$config_version\"/version: \"$yaml_version\"/" "$config_file"
      fi
      echo -e "   ${GREEN}→ Updated to v$yaml_version${NC}"
    fi
  fi
done

echo ""
echo "======================================================================"
echo "Sync Summary"
echo "======================================================================"
echo -e "Total skills checked: ${TOTAL}"
echo -e "✅ In sync:           ${GREEN}${IN_SYNC}${NC}"
echo -e "🔄 Out of sync:       ${YELLOW}${OUT_OF_SYNC}${NC}"
echo ""

if [ "$DRY_RUN" = true ] && [ "$OUT_OF_SYNC" -gt 0 ]; then
  echo -e "${YELLOW}ℹ️  Run with --update to sync all versions${NC}"
elif [ "$DRY_RUN" = false ] && [ "$OUT_OF_SYNC" -gt 0 ]; then
  echo -e "${GREEN}✅ All versions synced successfully${NC}"
fi

echo ""
