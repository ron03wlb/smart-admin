#!/bin/bash
#
# detect-keyword-conflicts.sh - Detect keyword conflicts across skills
#
# Usage:
#   ./detect-keyword-conflicts.sh [--verbose]
#
# This script extracts trigger keywords from all SKILL.md files and
# identifies potential conflicts where multiple skills share keywords.
#
# Version: 1.0.0
# Created: 2026-02-07

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
SKILLS_DIR="$PROJECT_ROOT/.claude/skills"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Settings
VERBOSE=false
if [[ "$1" == "--verbose" ]]; then
    VERBOSE=true
fi

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Skill Keyword Conflict Detector${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Temporary file for keyword mapping
TEMP_FILE=$(mktemp)
trap "rm -f $TEMP_FILE" EXIT

# Extract keywords from all SKILL.md files
echo "Scanning SKILL.md files for trigger keywords..."
echo ""

find "$SKILLS_DIR" -name "SKILL.md" -type f | while read -r skill_file; do
    # Get skill name from directory
    skill_dir=$(dirname "$skill_file")
    skill_name=$(basename "$skill_dir")

    # Skip _shared directory
    [[ "$skill_name" == "_shared" ]] && continue

    # Extract keywords from YAML frontmatter
    if grep -q "trigger_keywords:" "$skill_file" 2>/dev/null; then
        # Extract keywords after trigger_keywords: line
        in_keywords=false
        while IFS= read -r line; do
            if [[ "$line" =~ ^trigger_keywords: ]]; then
                in_keywords=true
                continue
            fi
            if $in_keywords; then
                # Stop if we hit another YAML key
                if [[ "$line" =~ ^[a-z_]+: ]] && [[ ! "$line" =~ ^- ]]; then
                    break
                fi
                # Extract keyword (remove leading "- " and quotes)
                if [[ "$line" =~ ^[[:space:]]*-[[:space:]]* ]]; then
                    keyword=$(echo "$line" | sed 's/^[[:space:]]*-[[:space:]]*//; s/"//g; s/'"'"'//g' | tr '[:upper:]' '[:lower:]')
                    if [[ -n "$keyword" ]]; then
                        echo "$keyword|$skill_name" >> "$TEMP_FILE"
                    fi
                fi
            fi
        done < "$skill_file"
    fi
done

# Count total keywords
TOTAL_KEYWORDS=$(wc -l < "$TEMP_FILE" | tr -d ' ')
echo "Found $TOTAL_KEYWORDS keyword entries"
echo ""

# Find conflicts (keywords used by multiple skills)
echo -e "${YELLOW}Analyzing keyword conflicts...${NC}"
echo ""

CONFLICT_COUNT=0

# Sort and find duplicates
sort "$TEMP_FILE" | cut -d'|' -f1 | uniq -c | sort -rn | while read -r count keyword; do
    if [[ "$count" -gt 1 ]]; then
        : $((CONFLICT_COUNT++))

        echo -e "${RED}Conflict #$CONFLICT_COUNT:${NC} '$keyword' used by $count skills:"

        # List skills using this keyword
        grep "^$keyword|" "$TEMP_FILE" | cut -d'|' -f2 | while read -r skill; do
            echo "  - $skill"
        done

        echo ""
    fi
done

# Summary
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Summary${NC}"
echo -e "${BLUE}========================================${NC}"
echo "Total keywords scanned: $TOTAL_KEYWORDS"

# Count conflicts
ACTUAL_CONFLICTS=$(sort "$TEMP_FILE" | cut -d'|' -f1 | uniq -c | awk '$1 > 1 {print}' | wc -l | tr -d ' ')

if [[ "$ACTUAL_CONFLICTS" -gt 0 ]]; then
    echo -e "${YELLOW}Keyword conflicts found: $ACTUAL_CONFLICTS${NC}"
    echo ""
    echo "Resolution options:"
    echo "1. Update keyword-resolution-matrix.yml with priority rules"
    echo "2. Remove duplicate keywords from lower-priority skills"
    echo "3. Make keywords more specific to avoid overlap"
    exit 1
else
    echo -e "${GREEN}No keyword conflicts found!${NC}"
    exit 0
fi
