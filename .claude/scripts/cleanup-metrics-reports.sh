#!/bin/bash
#
# cleanup-metrics-reports.sh - Archive old metrics reports
#
# Usage:
#   ./cleanup-metrics-reports.sh [--dry-run] [--retention-days N]
#
# This script archives metrics reports older than the retention period.
# Default retention: 30 days for daily reports
#
# Version: 1.0.0
# Created: 2026-02-07

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
REPORTS_DIR="$PROJECT_ROOT/.claude/metrics/reports"
ARCHIVE_DIR="$PROJECT_ROOT/.claude/metrics/archive"

# Default settings
RETENTION_DAYS=30
DRY_RUN=false

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Parse arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --retention-days)
            RETENTION_DAYS="$2"
            shift 2
            ;;
        *)
            echo "Unknown option: $1"
            echo "Usage: $0 [--dry-run] [--retention-days N]"
            exit 1
            ;;
    esac
done

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Metrics Reports Cleanup Script${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo "Reports directory: $REPORTS_DIR"
echo "Archive directory: $ARCHIVE_DIR"
echo "Retention days: $RETENTION_DAYS"
echo "Dry run: $DRY_RUN"
echo ""

# Check if reports directory exists
if [[ ! -d "$REPORTS_DIR" ]]; then
    echo -e "${YELLOW}Reports directory does not exist: $REPORTS_DIR${NC}"
    exit 0
fi

# Create archive directory if needed
if [[ "$DRY_RUN" == false ]]; then
    mkdir -p "$ARCHIVE_DIR"
fi

# Find old reports
OLD_REPORTS=$(find "$REPORTS_DIR" -name "*.md" -type f -mtime +"$RETENTION_DAYS" 2>/dev/null || true)

if [[ -z "$OLD_REPORTS" ]]; then
    echo -e "${GREEN}No reports older than $RETENTION_DAYS days found.${NC}"
    exit 0
fi

# Count reports
REPORT_COUNT=$(echo "$OLD_REPORTS" | wc -l | tr -d ' ')
echo -e "${YELLOW}Found $REPORT_COUNT reports older than $RETENTION_DAYS days:${NC}"
echo ""

# Process each report
ARCHIVED_COUNT=0
while IFS= read -r report; do
    [[ -z "$report" ]] && continue

    filename=$(basename "$report")

    if $DRY_RUN; then
        echo -e "  ${BLUE}[DRY RUN]${NC} Would archive: $filename"
    else
        # Move to archive
        mv "$report" "$ARCHIVE_DIR/"
        echo -e "  ${GREEN}Archived:${NC} $filename"
        : $((ARCHIVED_COUNT++))
    fi
done <<< "$OLD_REPORTS"

echo ""

if $DRY_RUN; then
    echo -e "${YELLOW}[DRY RUN] No files were actually moved${NC}"
    echo "Run without --dry-run to archive $REPORT_COUNT reports"
else
    echo -e "${GREEN}Successfully archived $ARCHIVED_COUNT reports${NC}"

    # Show current report count
    REMAINING=$(find "$REPORTS_DIR" -name "*.md" -type f 2>/dev/null | wc -l | tr -d ' ')
    echo "Remaining reports: $REMAINING"
fi

echo ""
echo -e "${GREEN}Cleanup complete!${NC}"
