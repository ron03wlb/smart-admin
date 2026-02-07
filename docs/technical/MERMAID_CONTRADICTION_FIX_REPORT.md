# Mermaid Contradiction Fix Report

**Version**: 1.0.0
**Created**: 2026-02-07
**Status**: Active

---

## Executive Summary

This report documents the contradiction between SmartAdmin's Mermaid rendering specification and Mermaid's native stateDiagram-v2 limitations, along with the resolution strategy.

**Core Contradiction**:
- SmartAdmin requires `<br/>` HTML tags for line breaks (NOT `\n`)
- **stateDiagram-v2 does NOT support `<br/>` tags** (causes parse error)

---

## 1. The Contradiction

### SmartAdmin Specification (CLAUDE.md)

SmartAdmin's rendering environment requires HTML `<br/>` tags for line breaks in Mermaid diagrams:

```markdown
# SmartAdmin Environment Requirement:
- Use `<br/>` tags: `A[Line 1<br/>Line 2]`
- Do NOT use `\n`: `A["Line 1\nLine 2"]` (not supported)
```

### Mermaid stateDiagram-v2 Limitation

However, **stateDiagram-v2** is an exception:

```mermaid
# This FAILS in stateDiagram-v2:
stateDiagram-v2
    A --> B: Event<br/>Action    # Parse error!
    note right of A : Text<br/>More    # Parse error!
```

---

## 2. Resolution Strategy

### Rule Hierarchy

| Diagram Type | Line Break | Example |
|--------------|------------|---------|
| flowchart | `<br/>` | `A[Line 1<br/>Line 2]` |
| sequenceDiagram | `<br/>` | `Note over A: Line 1<br/>Line 2` |
| graph | `<br/>` | `A[Line 1<br/>Line 2]` |
| classDiagram | `<br/>` | `class Foo { attr1<br/>attr2 }` |
| **stateDiagram-v2** | **Multi-line note block** | See below |

### stateDiagram-v2 Correct Patterns

**For Transition Labels** - Simplify to single line:
```mermaid
# Instead of:  A --> B: Event<br/>Action
# Use:         A --> B: Event
stateDiagram-v2
    A --> B: Event
```

**For Note Blocks** - Use multi-line format:
```mermaid
# Instead of:  note right of A : Line1<br/>Line2
# Use:
stateDiagram-v2
    note right of A
        Line 1
        Line 2
        Line 3
    end note
```

---

## 3. Detection & Repair

### Detection Script

```bash
# Detect stateDiagram <br/> errors
./scripts/detect-statediagram-br.sh docs/iGaming/
```

**Detection Regex**:
```regex
(?:stateDiagram-v2[\s\S]*?)(-->.*<br/>|note.*<br/>)
```

### Repair Options

| Priority | Document Type | Strategy |
|----------|---------------|----------|
| P0/P1 | Core docs | Use multi-line note blocks (preserve detail) |
| P2 | Supporting docs | Simplify transition labels |

### Batch Repair Script

```bash
# Fix stateDiagram <br/> errors (with verification)
./scripts/batch-fix-statediagram-br.sh --verify
```

---

## 4. Documentation Updates

### Updated Files

| File | Change |
|------|--------|
| `CLAUDE.md` | Added stateDiagram exception section |
| `.claude/skills/productivity/refactoring/mermaid-repair/knowledge/error-patterns.md` | Added Type F |
| `.claude/shared/knowledge/mermaid-best-practices.md` | (To be created) |

### CLAUDE.md Excerpt

```markdown
### stateDiagram-v2 Specific Rules

**CRITICAL**: stateDiagram-v2 has different syntax requirements.

**Do NOT use `<br/>` in stateDiagram-v2:**
- Transition labels: `A --> B: Event<br/>Action` (will fail)
- Note blocks: `note right of A : Text<br/>More` (will fail)

**Correct syntax:**
- Use multi-line note blocks with `end note`
- Simplify transition labels to single line
```

---

## 5. Validation Workflow

### Pre-commit Hook

The pre-commit hook now validates Mermaid syntax and detects stateDiagram `<br/>` errors:

```yaml
# .husky/pre-commit
- name: Mermaid Syntax Check
  run: ./scripts/validate-mermaid.sh docs/iGaming/
  on_failure: block
```

### CI/CD Integration

```yaml
# .github/workflows/mermaid-validation.yml
jobs:
  validate-mermaid:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Validate Mermaid Diagrams
        run: |
          ./scripts/validate-mermaid.sh docs/
          ./scripts/detect-statediagram-br.sh docs/
```

---

## 6. Summary

### Key Takeaways

1. **SmartAdmin uses `<br/>`** for all Mermaid line breaks
2. **stateDiagram-v2 is the ONLY exception** - it does NOT support `<br/>`
3. For stateDiagram-v2: use multi-line note blocks or simplify labels
4. Added Type F to mermaid-repair skill's error-patterns.md
5. Pre-commit hook prevents future violations

### References

- [CLAUDE.md - Mermaid Standards](../../CLAUDE.md#mermaid-diagram-standards)
- [error-patterns.md - Type F](../../.claude/skills/productivity/refactoring/mermaid-repair/knowledge/error-patterns.md)
- [Mermaid Official Docs](https://mermaid.js.org/syntax/stateDiagram.html)

---

**Report Author**: Claude Code
**Last Updated**: 2026-02-07
