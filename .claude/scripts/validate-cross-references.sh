#!/bin/bash

# validate-cross-references.sh
# Validates all markdown cross-references in AI documentation
# Compatible with macOS and Linux
# Usage: ./.claude/scripts/validate-cross-references.sh

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counters
total_links=0
broken_links=0
valid_links=0

echo "================================================"
echo "AI Documentation Cross-Reference Validation"
echo "================================================"
echo ""

# Function to check if a file exists (relative to source file)
check_file() {
    local link="$1"
    local source_file="$2"

    # Remove anchor (#section) from link
    local file_path="${link%%#*}"

    # Get source directory
    local source_dir=$(dirname "$source_file")

    # Resolve path relative to source file
    local target_path="$source_dir/$file_path"

    # Check if file exists
    if [ -f "$target_path" ]; then
        return 0
    else
        # Try from project root
        if [ -f "$file_path" ]; then
            return 0
        else
            return 1
        fi
    fi
}

# Find all markdown files in AI documentation
echo "Scanning files..."
md_files=$(find CLAUDE.md .claude .agent -name "*.md" -type f 2>/dev/null | sort)

file_count=$(echo "$md_files" | wc -l | tr -d ' ')
echo "Found $file_count markdown files"
echo ""

# Check each file for markdown links
while IFS= read -r file; do
    # Skip if file doesn't exist
    [ ! -f "$file" ] && continue

    # Extract markdown links using sed (macOS compatible)
    # Pattern: [text](path)
    links=$(sed -n 's/.*\[\([^]]*\)\](\([^)]*\)).*/\2/p' "$file" 2>/dev/null || true)

    if [ -z "$links" ]; then
        continue
    fi

    has_checked_links=false

    # Process each link
    while IFS= read -r path; do
        # Skip empty paths
        [ -z "$path" ] && continue

        # Skip external links (http://, https://, mailto:)
        if [[ "$path" =~ ^https?:// ]] || [[ "$path" =~ ^mailto: ]]; then
            continue
        fi

        # Skip anchor-only links (#section)
        if [[ "$path" =~ ^# ]]; then
            continue
        fi

        # Print file header only if we have links to check
        if [ "$has_checked_links" = false ]; then
            echo "Checking: $file"
            has_checked_links=true
        fi

        ((total_links++))

        # Check if target file exists
        if check_file "$path" "$file"; then
            ((valid_links++))
            echo -e "  ${GREEN}✓${NC} $path"
        else
            ((broken_links++))
            echo -e "  ${RED}✗${NC} $path ${RED}(BROKEN)${NC}"
        fi
    done <<< "$links"

    if [ "$has_checked_links" = true ]; then
        echo ""
    fi
done <<< "$md_files"

# Print summary
echo "================================================"
echo "Summary"
echo "================================================"
echo "Total links checked: $total_links"
echo -e "Valid links: ${GREEN}$valid_links${NC}"
echo -e "Broken links: ${RED}$broken_links${NC}"
echo ""

# Exit with error if broken links found
if [ $broken_links -gt 0 ]; then
    echo -e "${RED}❌ Validation FAILED${NC}"
    echo "Please fix broken links before committing."
    exit 1
else
    echo -e "${GREEN}✅ Validation PASSED${NC}"
    echo "All cross-references are valid."
    exit 0
fi
