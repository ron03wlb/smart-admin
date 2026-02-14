# Mermaid Repair

**Priority**: P2 (Productivity - Refactoring)
**Version**: 1.0.0
**Status**: Stable

---

## Overview

Automated Mermaid diagram syntax validation and repair for SmartAdmin's special rendering environment. SmartAdmin uses `<br/>` HTML tags instead of standard `\n` for line breaks.

---

## When to Use

- Mermaid diagrams not rendering correctly
- Style syntax errors (node IDs with spaces, color code pollution)
- Validating Mermaid syntax across documentation files
- Batch repair of multiple markdown files
- Pre-commit validation of diagram syntax

---

## Core Capabilities

| Capability | Description |
|-----------|-------------|
| **Style Syntax Fix** | Auto-quote node IDs with spaces, clean color codes |
| **SmartAdmin Adaptation** | Uses `<br/>` tags per SmartAdmin spec |
| **Batch Processing** | Scan and repair multiple files |
| **Validation** | Verify syntax before/after repair |

---

## Error Types Handled

| Type | Severity | Example |
|------|----------|---------|
| Node ID Space | High | `style Risk Engine fill:#DDA0DD` → `style "Risk Engine" fill:#DDA0DD` |
| Color Pollution | High | `fill:#BonusBonus3366` → `fill:#333366` |
| Duplicate Style | Medium | `style style` → `style` |

---

## Usage Examples

```
# Batch repair
"Fix mermaid errors in docs/iGaming/"

# Single file validation
"Validate mermaid in README.md"

# Detect color pollution
"Check for mermaid color code issues"
```

---

## SmartAdmin Mermaid Specification

- Use `<br/>` HTML tags for line breaks (NOT `\n`)
- Node labels don't need quotes when using `<br/>`
- sequenceDiagram Note blocks MUST use `<br/>`
- stateDiagram-v2 does NOT support `<br/>` (use multi-line note blocks)

---

## Related Rules

- [CLAUDE.md - Mermaid Standards](../../../../CLAUDE.md#mermaid-diagram-standards)

---

**Maintainer**: SmartAdmin Skills Team
**Last Updated**: 2026-02-07
