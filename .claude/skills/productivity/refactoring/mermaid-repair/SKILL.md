---
name: mermaid-repair
description: [P2 - Productivity] Automated Mermaid diagram syntax validation and repair for SmartAdmin's <br/> rendering environment. Fixes style syntax errors, node ID quoting, and color code pollution.
---

# Mermaid Repair Skill

**Version**: 1.0.0
**Priority**: P2 (Productivity/Refactoring)
**Category**: Documentation Repair
**Status**: Stable

---

## Overview

Automated detection and repair of Mermaid diagram syntax errors in SmartAdmin project. Specifically designed for SmartAdmin's rendering environment which uses `<br/>` tags instead of standard `\n`.

**Key Capabilities**:
- Style syntax error detection and repair
- Node ID quoting (for names with spaces)
- Color code pollution cleanup
- Batch file processing

---

## Version

**Skill Version**: 1.0.0
**Last Updated**: 2026-02-03
**Compatible With**: SmartAdmin v4.0.0+, .claude/ system v3.0.2+

---

## Trigger Keywords

This skill is automatically activated when the user's request contains:

**Primary Keywords** (High confidence):
- "mermaid repair" - Repair Mermaid diagrams
- "mermaid syntax error" - Fix syntax issues
- "fix mermaid style" - Style syntax repair
- "mermaid parse error" - Parser error resolution

**Secondary Keywords** (Medium confidence):
- "diagram render failed" - Context: Mermaid rendering issues
- "mermaid validation" - Context: Validate diagram syntax
- "style syntax error" - Context: Mermaid style definition errors
- "color code pollution" - Context: Invalid hex color codes

**Phrase Patterns**:
- "Fix mermaid [diagram/style] in [file]" - Example: "Fix mermaid style in 05-01_Risk_Control.md"
- "Validate mermaid [diagrams] in [directory]" - Example: "Validate mermaid diagrams in docs/iGaming/"
- "Repair [style/color] errors in [file]" - Example: "Repair color errors in README.md"

**Example User Requests**:
```
User: "Fix Mermaid style syntax errors in docs/iGaming/"
User: "Validate all Mermaid diagrams in the project"
User: "Repair color code pollution in 05-01_Risk_Control_System.md"
User: "My mermaid diagram is not rendering, help fix it"
```

**Note**: This skill can also be manually invoked via `/mermaid-repair` or `/fix-mermaid` command.

---

## Core Functions

| Function | Description |
|----------|-------------|
| **Style Syntax Fix** | Auto-quote node IDs with spaces, clean color codes |
| **SmartAdmin Adaptation** | Uses `<br/>` tags (not `\n`) per SmartAdmin spec |
| **Batch Processing** | Scan and repair multiple files at once |
| **Validation** | Verify syntax correctness before/after repair |
| **Pre-commit Hook** | Prevents future Mermaid errors |

---

## Error Types Handled

### Type A: Node ID Space Not Quoted

**Severity**: High (Blocks rendering)

**Error Pattern**:
```mermaid
style Risk Engine fill:#DDA0DD
```

**Fixed**:
```mermaid
style "Risk Engine" fill:#DDA0DD
```

### Type B: Color Code Pollution

**Severity**: High (Style failure)

**Error Pattern**:
```mermaid
style B fill:#BonusBonus3366
```

**Fixed**:
```mermaid
style B fill:#333366
```

---

## Usage Examples

### Example 1: Batch Repair

**Command**:
```
/mermaid-repair docs/iGaming/ --fix
```

**Output**:
```
Scanned: 45 files
Fixed: 4 files (12 errors)
  - Type A (Node ID): 7 fixes
  - Type B (Color): 5 fixes
All diagrams now render correctly.
```

### Example 2: Validation Only

**Command**:
```
/mermaid-repair docs/iGaming/ --validate
```

**Output**:
```
Validation Report:
- 05-01_Risk_Control.md: 3 errors
- 04-02_Bonus_Engine.md: 2 errors
Run with --fix to auto-repair.
```

### Example 3: Single File

**Command**:
```
/mermaid-repair README.md --fix
```

---

## Scripts

The skill includes Python automation scripts:

| Script | Purpose |
|--------|---------|
| `scripts/fix_style_syntax.py` | Fix style syntax errors in a single file |
| `scripts/validate_mermaid.py` | Validate Mermaid syntax |
| `scripts/batch_repair.py` | Batch repair multiple files |

**Usage**:
```bash
python scripts/fix_style_syntax.py <file_path>
python scripts/validate_mermaid.py <file_path>
python scripts/batch_repair.py <directory>
```

---

## SmartAdmin Mermaid Specification

**Critical Differences from Standard Mermaid**:
- Use `<br/>` HTML tags for line breaks (NOT `\n`)
- Node labels don't need quotes when using `<br/>`
- sequenceDiagram Note blocks MUST use `<br/>`
- stateDiagram-v2 does NOT support `<br/>` (use multi-line note blocks instead)

**Reference**: [CLAUDE.md#mermaid-diagram-standards](../../../../../CLAUDE.md#mermaid-diagram-standards)

---

## Validation Flow

```
1. Scan target files for Mermaid code blocks
2. Parse each diagram for syntax errors
3. Apply appropriate fixes (quote/clean)
4. Re-validate fixed diagrams
5. Generate repair report
6. (Optional) Run pre-commit hook
```

---

## Quality Standards

| Metric | Target |
|--------|--------|
| Scan Speed | >100 files/second |
| Repair Accuracy | >99% |
| False Positive Rate | <1% |

---

## Related Rules

- **[CLAUDE.md - Mermaid Standards](../../../../../CLAUDE.md#mermaid-diagram-standards)** - SmartAdmin Mermaid specification
- **[mermaid-best-practices.md](../../extended/domain/igaming-multi-tenant-wallet-pm/knowledge/mermaid-best-practices.md)** - Detailed best practices

---

## Files

```
mermaid-repair/
├── SKILL.md                    # This file
├── config.yml                  # Skill configuration
├── README.md                   # Detailed documentation
├── knowledge/
│   ├── error-patterns.md       # Error pattern library
│   └── smartadmin-mermaid-spec.md  # SmartAdmin spec
├── scripts/
│   ├── fix_style_syntax.py     # Style fixer
│   ├── validate_mermaid.py     # Validator
│   └── batch_repair.py         # Batch processor
└── examples/
    └── example-1-style-fix.md  # 2026-02-03 repair case
```

---

**Maintainer**: SmartAdmin Skills Team
**Last Updated**: 2026-02-03
