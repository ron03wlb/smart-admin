# Version Alignment

This document tracks version alignment across documentation systems.

## Current Versions

| System | Version | Last Updated |
|--------|---------|-------------|
| CLAUDE.md | 1.0.0 | 2026-01-22 |
| .claude/ | 2.5.0 | 2026-01-21 |
| .agent/rules/ | (unversioned) | 2025-12-15 |

## Update Protocol

When updating any system:

1. Update the source documentation
2. Update dependent references (use grep to find)
3. Update version number
4. Update this alignment document
5. Test that cross-references work
6. Commit all changes together

## Cross-Reference Map

| Content | CLAUDE.md | .claude/ | .agent/rules/ |
|---------|-----------|----------|---------------|
| Anti-patterns | Top 3 + link | quality-standards.md (source) | 01-naming-conventions.md |
| ResponseDTO | Example | smartadmin-patterns.md (source) | 02-api-response.md |
| Layered arch | Overview | smartadmin-patterns.md (source) | 10-architecture-rules.md |
| Build commands | Common | project-architecture.md (source) | N/A |

## Maintenance Schedule

- **Quarterly**: Review cross-references, ensure alignment
- **On major change**: Update all three systems synchronously
- **On version bump**: Update this document

## Version History

### 2026-01-22 - Version Alignment Established
- Created VERSION_ALIGNMENT.md
- CLAUDE.md versioned as 1.0.0
- .claude/ at v2.5.0
- Documented cross-reference relationships

---

**Note**: This document should be updated whenever CLAUDE.md, .claude/, or .agent/rules/ versions change.
