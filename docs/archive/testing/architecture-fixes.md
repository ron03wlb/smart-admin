# SmartAdmin Architecture Fixes Guide

> **⚠️ DOCUMENT RELOCATED**
> **This file has been reorganized for better navigation and maintenance.**
> **Updated**: 2026-01-21

---

## New Location

This document has been split into multiple focused guides for better usability:

### 📖 Start Here

**[Architecture Overview](./architecture/overview.md)**
- Quick reference for all architecture rules
- Violation detection methods
- Fix workflow guide

### 🔧 Specific Fix Guides

**[Employee Service Fix](./architecture/fix-employee-transactional.md)**
- Fix `EmployeeService.updatePassword()` @Transactional violation
- Estimated time: 15 minutes

**[Role Service Fix](./architecture/fix-role-transactional.md)**
- Fix `RoleService` @Transactional violations
- Create RoleManager class
- Estimated time: 30 minutes

---

## Why the Change?

The original 545-line document has been reorganized to:

✅ **Better navigation** - Each violation has its own focused guide
✅ **Reduced redundancy** - Removed duplicate code examples (from 400+ lines to 80 lines of focused snippets)
✅ **Easier maintenance** - Independent files are easier to update
✅ **Faster lookup** - Direct links to specific violations

---

## Quick Links

| I need to... | Go to |
|--------------|-------|
| **Understand all architecture rules** | [Architecture Overview](./architecture/overview.md) |
| **Fix EmployeeService violation** | [Employee Fix Guide](./architecture/fix-employee-transactional.md) |
| **Fix RoleService violations** | [Role Fix Guide](./architecture/fix-role-transactional.md) |
| **Run verification tests** | [Quick Reference - Commands](./quick-reference.md#commands) |
| **Learn testing strategy** | [Testing Strategy](./testing-strategy.md) |

---

## Document Structure

```
/docs/testing/
├── README.md                           # Navigation hub
├── quick-reference.md                  # Commands cheatsheet
├── architecture/                       # Architecture fixes
│   ├── overview.md                    # Rules & detection (80 lines)
│   ├── fix-employee-transactional.md  # Employee fix (120 lines)
│   └── fix-role-transactional.md      # Role fix (150 lines)
├── testing-strategy.md                 # Testing guide (simplified)
└── unit-test-implementation-plan.md    # 6-week roadmap (simplified)
```

---

**Ready to fix violations? Start with [Architecture Overview](./architecture/overview.md)**
