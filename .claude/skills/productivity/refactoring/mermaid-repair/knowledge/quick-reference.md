# Mermaid Repair - Quick Reference

**Version**: 1.0.0
**Last Updated**: 2026-02-07
**Time Estimate**: 2-10 minutes per file

## When to Use

Use this skill when:
- Mermaid diagrams are not rendering in SmartAdmin documentation
- You encounter style syntax errors in `.md` files
- Batch validation is needed for entire documentation directories
- Pre-commit validation for Mermaid syntax

## Key Commands

### Single File Validation
```bash
python3 .claude/skills/productivity/refactoring/mermaid-repair/scripts/validate_mermaid.py path/to/file.md
```

### Batch Repair
```bash
python3 .claude/skills/productivity/refactoring/mermaid-repair/scripts/batch_repair.py --directory docs/iGaming/
```

### Style Syntax Fix
```bash
python3 .claude/skills/productivity/refactoring/mermaid-repair/scripts/fix_style_syntax.py path/to/file.md
```

## SmartAdmin Mermaid Rules

### Line Breaks
| Environment | Syntax | Example |
|-------------|--------|---------|
| SmartAdmin | `<br/>` | `A[Line 1<br/>Line 2]` |
| Standard Mermaid | `\n` | `A["Line 1\nLine 2"]` |

**CRITICAL**: SmartAdmin uses `<br/>` tags, NOT `\n` escape sequences.

### Node ID Quoting
```mermaid
# Correct - No spaces, no quotes needed
graph TD
    A[Simple] --> B[Node]

# Correct - Spaces require quotes
graph TD
    A["Node with spaces"] --> B["Another node"]

# Incorrect - Unquoted spaces
graph TD
    A[Node with spaces] --> B[Broken]
```

### Color Codes
```mermaid
# Incorrect - Color codes in style
style A fill:#f9f,stroke:#333

# Correct - Use class definitions instead
classDef highlight fill:#f9f,stroke:#333
class A highlight
```

## Common Error Patterns

| Error | Fix |
|-------|-----|
| `\n` in node labels | Replace with `<br/>` |
| Unquoted node IDs with spaces | Add double quotes |
| Missing closing fence | Add closing ` ``` ` |
| Invalid style color format | Use hex codes `#fff` or named colors |

## Verification

After repair, verify with:
```bash
# Single file
python3 scripts/validate_mermaid.py path/to/file.md

# Entire directory
python3 scripts/batch_repair.py --directory docs/ --verify-only
```

## Related Skills

- [markdown-quality-checker](../../refactoring/markdown-quality-checker/) - General Markdown quality
- [igame-pm-analyst](../../../extended/domain/igame-pm-analyst/) - iGaming documentation with Mermaid

## References

- [SmartAdmin Mermaid Spec](smartadmin-mermaid-spec.md)
- [Error Patterns](error-patterns.md)
