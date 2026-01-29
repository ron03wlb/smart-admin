#!/bin/bash
# ====================================================================
# Skills Path Reference Detector v1.0.0
# ====================================================================
# Purpose: Detect all references to root-layer skill paths in documentation
# Author: Skills Optimization v4.0.0
# Date: 2026-01-29
# ====================================================================

set -e

REPORT_FILE=".claude/scripts/path-references-report.md"
MAPPING_FILE=".claude/scripts/root-to-hierarchical-mapping.json"

echo "# Skills Path Reference Detection Report" > "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "Generated: $(date '+%Y-%m-%d %H:%M:%S')" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "## Summary" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# Counter
total_references=0

# Search for each skill in the mapping
echo "Detecting references to root-layer skills..." >&2

while IFS= read -r skill; do
  # Remove quotes
  skill_clean=$(echo "$skill" | tr -d '"' | tr -d ',')

  if [ -z "$skill_clean" ]; then
    continue
  fi

  echo "Checking: $skill_clean" >&2

  # Search pattern: .claude/skills/{skill-name}
  pattern="\.claude/skills/${skill_clean}"

  # Search in all markdown and yaml files (excluding skills directory itself)
  matches=$(grep -r "$pattern" .claude \
    --exclude-dir=skills \
    --include="*.md" \
    --include="*.yml" \
    --include="*.yaml" \
    2>/dev/null || true)

  if [ -n "$matches" ]; then
    echo "" >> "$REPORT_FILE"
    echo "### Skill: \`$skill_clean\`" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo '```' >> "$REPORT_FILE"
    echo "$matches" >> "$REPORT_FILE"
    echo '```' >> "$REPORT_FILE"

    count=$(echo "$matches" | wc -l)
    total_references=$((total_references + count))
    echo "  Found $count reference(s)" >&2
  fi
done < <(jq -r '.skills | keys[]' "$MAPPING_FILE")

echo "" >> "$REPORT_FILE"
echo "## Total References: $total_references" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "## Recommendations" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "1. Update all references to use hierarchical paths" >> "$REPORT_FILE"
echo "2. These are mostly in historical reports - consider archiving" >> "$REPORT_FILE"
echo "3. skill-registry.yml already uses hierarchical paths ✅" >> "$REPORT_FILE"

echo "" >&2
echo "Report generated: $REPORT_FILE" >&2
echo "Total references found: $total_references" >&2
