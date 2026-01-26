# Deprecated Skills

This directory contains skills that have been deprecated and consolidated into composite skills.

## CRUD Pipeline Consolidation (v2.0.0 - 2026-01-27)

The following skills have been **consolidated into `smartadmin-crud-generator`**:

### Deprecated Skills

| Skill | Deprecated Date | Consolidated Into | Phase Mapping |
|-------|----------------|-------------------|---------------|
| **smartadmin-mybatis** | 2026-01-27 | smartadmin-crud-generator | Phase 1: Backend (--backend-only) |
| **smartadmin-vue-crud** | 2026-01-27 | smartadmin-crud-generator | Phase 2: Frontend (--frontend-only) |
| **smartadmin-api-docs** | 2026-01-27 | smartadmin-crud-generator | Phase 3: API Docs (--docs-only) |

### Backward Compatibility

**Status**: Soft Deprecation (12 weeks, until 2026-06-30)

Old commands still work via [skill-aliases.json](../skill-aliases.json) with deprecation warnings:

```bash
# ⚠️ Deprecated (still works with warning)
/mybatis generate Employee
# Routes to: /crud Employee --backend-only

# ⚠️ Deprecated (still works with warning)
/vue-crud Brand
# Routes to: /crud Brand --frontend-only

# ⚠️ Deprecated (still works with warning)
/api-docs ProductController
# Routes to: /crud Product --docs-only
```

### Migration Guide

**Before** (4 separate commands, ~45 minutes):
```bash
/mybatis generate Employee
/vue-crud Employee
/api-docs EmployeeController
/integration-test EmployeeService
```

**After** (1 command, ~20 minutes):
```bash
/crud Employee --all-phases
```

### Deprecation Timeline

- **Weeks 1-12** (Soft Deprecation, until 2026-06-30):
  - ⚠️ Commands display warnings
  - ✅ Auto-route to new skill
  - 📚 Migration guide in warning message

- **Weeks 13-24** (Hard Deprecation, until 2026-09-30):
  - ❌ Commands display errors + migration guide
  - ✅ Can still access via explicit flags
  - 📚 Documentation shows new patterns only

- **Week 25+** (Complete Removal, after 2026-10-01):
  - ❌ Old commands permanently removed
  - ✅ Only consolidated skill remains

### Directory Structure

**Note**: Physical directories for deprecated skills will be moved in the next commit. Currently they remain in the skills directory for backward compatibility during the soft deprecation period.

**Future Location** (after file lock resolution):
```
.claude/skills/_deprecated/
├── smartadmin-mybatis/      # To be moved
├── smartadmin-vue-crud/     # To be moved
└── smartadmin-api-docs/     # To be moved
```

### Related Documentation

- [Skills Catalog](../README.md) - Updated to reflect consolidation
- [Skill Aliases](../skill-aliases.json) - Backward compatibility configuration
- [CRUD Generator](../smartadmin-crud-generator/) - New composite skill documentation

---

**Last Updated**: 2026-01-27
**Consolidation Version**: v2.0.0
