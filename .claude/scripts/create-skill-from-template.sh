#!/bin/bash
# ====================================================================
# SmartAdmin Skills - Skill Generator
# ====================================================================
# Purpose: Create new skill from template
# Usage:
#   ./create-skill-from-template.sh <skill-name> <category> <priority>
#   Example: ./create-skill-from-template.sh example-skill backend P0
# ====================================================================

set -e

if [ $# -lt 3 ]; then
  echo "Usage: $0 <skill-name> <category> <priority>"
  echo ""
  echo "Categories: backend, full-stack, testing, domain, orchestration, quality, devops, integration, composite, analysis, refactoring"
  echo "Priority: P0 (foundation), P1 (extended), P2 (productivity)"
  exit 1
fi

SKILL_NAME=$1
CATEGORY=$2
PRIORITY=$3

# Determine target directory based on priority
case "$PRIORITY" in
  P0)
    if [ "$CATEGORY" = "backend" ] || [ "$CATEGORY" = "full-stack" ] || [ "$CATEGORY" = "testing" ]; then
      TARGET_DIR=".claude/skills/foundation/$CATEGORY/$SKILL_NAME"
    else
      echo "❌ Error: P0 skills must be backend, full-stack, or testing"
      exit 1
    fi
    ;;
  P1)
    TARGET_DIR=".claude/skills/extended/$CATEGORY/$SKILL_NAME"
    ;;
  P2)
    TARGET_DIR=".claude/skills/productivity/$CATEGORY/$SKILL_NAME"
    ;;
  *)
    echo "❌ Error: Priority must be P0, P1, or P2"
    exit 1
    ;;
esac

echo "🔨 Creating skill: $SKILL_NAME"
echo "   Category: $CATEGORY"
echo "   Priority: $PRIORITY"
echo "   Target: $TARGET_DIR"
echo ""

# Create directory structure
mkdir -p "$TARGET_DIR"/{references,examples,templates}

# Create config.yml
cat > "$TARGET_DIR/config.yml" << EOF
# ====================================================================
# $SKILL_NAME - Skill Configuration
# ====================================================================
# Version: 1.0.0
# Priority: $PRIORITY
# Category: $CATEGORY
# ====================================================================

metadata:
  name: "$SKILL_NAME"
  version: "1.0.0"
  priority: "$PRIORITY"
  type: "atomic"
  category: "$CATEGORY"
  visibility: "public"
  status: "draft"
  description: |
    TODO: Add skill description

  tags:
    - TODO

triggers:
  keywords:
    - "TODO"
  patterns:
    - "TODO"

# TODO: Complete configuration
EOF

# Create README.md
cat > "$TARGET_DIR/README.md" << EOF
# $SKILL_NAME - Quick Reference

**Version**: 1.0.0
**Priority**: $PRIORITY
**Category**: $CATEGORY
**Status**: 🚧 Draft

---

## 🚀 快速觸發

**關鍵字**:
- "TODO"

---

## 📊 核心功能

1. **TODO**: Description
2. **TODO**: Description

---

## 📖 詳細文檔

參考 [SKILL.md](SKILL.md) 完整指南

---

**Last Updated**: $(date +%Y-%m-%d)
EOF

# Create SKILL.md skeleton
cat > "$TARGET_DIR/SKILL.md" << EOF
# $SKILL_NAME

**Version**: 1.0.0
**Status**: 🚧 Draft
**Priority**: $PRIORITY
**Category**: $CATEGORY

---

## Overview

TODO: Describe when to use this skill

---

## Usage

TODO: Add examples

---

## Implementation

TODO: Add implementation steps
EOF

echo "✅ Created skill: $TARGET_DIR"
echo ""
echo "Next steps:"
echo "1. Edit config.yml and fill in TODO sections"
echo "2. Write SKILL.md documentation"
echo "3. Update skill-registry.yml"
echo "4. Run verification: ./verify-skill-registry.sh"
