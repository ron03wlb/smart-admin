#!/usr/bin/env bash

#
# Link Validation Script for SmartAdmin Documentation
#
# Purpose: Validate all internal links in Markdown files to prevent broken references
# Usage: ./.claude/scripts/validate-links.sh
# Exit: 0 if all links valid, 1 if broken links found
#

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counters
total_links=0
broken_links=0
valid_links=0

# Get repository root (where .git is located)
REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "$REPO_ROOT"

echo "🔍 Validating documentation links in: $REPO_ROOT"
echo ""

# Function to extract and validate links from a file
process_file() {
    local file="$1"
    local line_num=0

    while IFS= read -r line; do
        ((line_num++))

        # Extract markdown links: [text](link)
        # Using sed to extract links (portable across macOS and Linux)
        local links_in_line=$(echo "$line" | sed -n 's/.*\[.*\](\([^)]*\)).*/\1/p' 2>/dev/null || true)

        # If line contains markdown links, process them
        if echo "$line" | grep -q '\[.*\](.*' 2>/dev/null; then
            # Extract all links from this line using a more robust approach
            local temp_line="$line"
            while [[ "$temp_line" =~ \]\(([^\)]+)\) ]]; do
                local link="${BASH_REMATCH[1]}"
                temp_line="${temp_line#*]($link)}"  # Remove processed link

                ((total_links++))
                check_link "$file" "$line_num" "$link"
            done
        fi
    done < "$file"
}

# Function to check if a link is valid
check_link() {
    local file="$1"
    local line_num="$2"
    local link="$3"

    # Skip external URLs (http://, https://, ftp://)
    if [[ "$link" =~ ^https?:// ]] || [[ "$link" =~ ^ftp:// ]]; then
        ((valid_links++))
        return 0
    fi

    # Skip mailto links
    if [[ "$link" =~ ^mailto: ]]; then
        ((valid_links++))
        return 0
    fi

    # Skip anchor-only links (#section)
    if [[ "$link" =~ ^# ]]; then
        ((valid_links++))
        return 0
    fi

    # Extract path (remove anchor if present)
    local path="${link%%#*}"

    # Skip empty paths
    if [[ -z "$path" ]]; then
        ((valid_links++))
        return 0
    fi

    # Get directory of the current markdown file
    local file_dir="$(dirname "$file")"

    # Resolve relative path
    local target_path
    if [[ "$path" = /* ]]; then
        # Absolute path from repo root
        target_path="${REPO_ROOT}${path}"
    else
        # Relative path
        target_path="${file_dir}/${path}"
    fi

    # Normalize path (resolve .. and .)
    if [[ -d "$(dirname "$target_path")" ]]; then
        target_path="$(cd "$(dirname "$target_path")" 2>/dev/null && pwd)/$(basename "$target_path")"
    fi

    # Check if target exists (file or directory)
    if [[ -e "$target_path" ]] || [[ -d "$target_path" ]]; then
        ((valid_links++))
        return 0
    else
        ((broken_links++))
        echo -e "${RED}✗ BROKEN LINK${NC}"
        echo -e "  File: ${YELLOW}$file${NC}:${line_num}"
        echo -e "  Link: ${YELLOW}$link${NC}"
        echo -e "  Expected path: ${YELLOW}$target_path${NC}"
        echo ""
        return 1
    fi
}

# Find and process all markdown files
md_files=$(find . -name "*.md" -type f \
    | grep -v node_modules \
    | grep -v ".git" \
    | grep -v "build/" \
    | grep -v "dist/" \
    | sort)

# Process each file
for file in $md_files; do
    process_file "$file"
done

# Summary
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 Link Validation Summary"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "Total links checked: ${YELLOW}$total_links${NC}"
echo -e "Valid links: ${GREEN}$valid_links${NC}"
echo -e "Broken links: ${RED}$broken_links${NC}"
echo ""

if [[ $broken_links -eq 0 ]]; then
    echo -e "${GREEN}✅ All links are valid!${NC}"
    exit 0
else
    echo -e "${RED}❌ Found $broken_links broken link(s)${NC}"
    echo ""
    echo "💡 To fix:"
    echo "  1. Update the broken links in the files listed above"
    echo "  2. Or create the missing files/directories"
    echo "  3. Run this script again to verify"
    exit 1
fi
