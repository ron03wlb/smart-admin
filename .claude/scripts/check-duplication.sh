#!/bin/bash

# check-duplication.sh
# Detects duplicated content blocks across AI documentation
# Compatible with macOS and Linux
#
# Version: 1.0.0
# Last Updated: 2026-01-24

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
total_blocks=0
duplicates_found=0

# Temp directory for processing
TEMP_DIR=$(mktemp -d)
trap 'rm -rf "$TEMP_DIR"' EXIT

echo "================================================"
echo "Content Duplication Check"
echo "================================================"
echo ""

# Find all markdown files in CLAUDE.md, .claude/, and .agent/
echo "Scanning files..."
files=$(find . -type f \( -name "CLAUDE.md" -o -path "./.claude/*.md" -o -path "./.agent/*.md" \) 2>/dev/null || true)
file_count=$(echo "$files" | grep -v "^$" | wc -l | tr -d ' ')
echo "Found $file_count markdown files in CLAUDE.md, .claude/, .agent/"
echo ""

if [ "$file_count" -eq 0 ]; then
    echo "${YELLOW}⚠️  No markdown files found${NC}"
    exit 0
fi

# Extract and hash code blocks from all files
echo "${BLUE}Extracting code blocks...${NC}"
for file in $files; do
    if [ ! -f "$file" ]; then
        continue
    fi

    # Use awk for efficient processing (use tab as delimiter to avoid pipe conflicts)
    awk -v file="$file" '
    BEGIN { in_block=0; block=""; start_line=0 }
    /^```/ {
        if (in_block == 0) {
            in_block = 1
            start_line = NR
            block = ""
        } else {
            in_block = 0
            if (length(block) > 100) {
                # Normalize and print block info (use tab as delimiter)
                gsub(/^[[:space:]]+|[[:space:]]+$/, "", block)
                gsub(/[[:space:]]+/, " ", block)
                print block "\t" file "\t" start_line "\t" NR
            }
        }
        next
    }
    in_block == 1 { block = block $0 "\n" }
    ' "$file" >> "$TEMP_DIR/blocks_raw.txt"
done

# Hash the blocks (using first field which is the normalized content)
if [ -f "$TEMP_DIR/blocks_raw.txt" ]; then
    while IFS=$'\t' read -r content file start end; do
        # Hash the content
        if command -v md5sum >/dev/null 2>&1; then
            hash=$(echo "$content" | md5sum | cut -d' ' -f1)
        else
            hash=$(echo "$content" | md5)
        fi
        echo "$hash"$'\t'"$file"$'\t'"$start"$'\t'"$end" >> "$TEMP_DIR/blocks_hashed.txt"
        total_blocks=$((total_blocks + 1))
    done < "$TEMP_DIR/blocks_raw.txt"
fi

echo "  Processed $total_blocks code blocks"
echo ""

# Check for duplicated code blocks
echo "${BLUE}Checking for duplicates...${NC}"
if [ -f "$TEMP_DIR/blocks_hashed.txt" ]; then
    # Find duplicate hashes (use tab delimiter)
    cut -f1 "$TEMP_DIR/blocks_hashed.txt" | sort | uniq -d > "$TEMP_DIR/dup_hashes.txt"

    if [ -s "$TEMP_DIR/dup_hashes.txt" ]; then
        while read -r hash; do
            echo "  ${RED}✗ DUPLICATE FOUND:${NC}"
            grep "^$hash" "$TEMP_DIR/blocks_hashed.txt" | while IFS=$'\t' read -r h filepath start end; do
                # Calculate line count
                lines=$((end - start))
                echo "    File: $filepath:$start-$end ($lines lines)"
            done
            echo ""
            duplicates_found=$((duplicates_found + 1))
        done < "$TEMP_DIR/dup_hashes.txt"
    else
        echo "  ${GREEN}✓ No duplicated code blocks${NC}"
    fi
else
    echo "  ${GREEN}✓ No code blocks found${NC}"
fi
echo ""

# Summary
echo "================================================"
echo "Summary"
echo "================================================"
echo "Total blocks checked: $total_blocks"
echo "Duplicates found: $duplicates_found"
echo ""

if [ $duplicates_found -gt 0 ]; then
    echo "${RED}❌ DUPLICATION DETECTED${NC}"
    echo "Please consolidate duplicated content into single source of truth."
    echo "See CONTENT_MAP.md for ownership rules."
    exit 1
else
    echo "${GREEN}✅ NO DUPLICATION DETECTED${NC}"
    exit 0
fi
