---
name: naming-convention-checker
description: Validate SmartAdmin naming conventions
priority: P1
category: quality
---

# Naming Convention Checker

Validate SmartAdmin naming conventions for classes, tables, fields, and methods.

## When to Use

- Checking class naming (XXXController, XXXService)
- Validating table naming (singular form)
- Ensuring boolean field naming (deleted, not isDeleted)
- Pre-commit validation

## Naming Rules

| Type | Pattern | Example |
|------|---------|---------|
| Controller | `{Name}Controller` | `EmployeeController` |
| Service | `{Name}Service` | `EmployeeService` |
| Manager | `{Name}Manager` | `EmployeeManager` |
| Dao | `{Name}Dao` | `EmployeeDao` |
| Entity | `{Name}Entity` | `EmployeeEntity` |
| AddForm | `{Name}AddForm` | `EmployeeAddForm` |
| UpdateForm | `{Name}UpdateForm` | `EmployeeUpdateForm` |
| QueryForm | `{Name}QueryForm` | `EmployeeQueryForm` |
| VO | `{Name}VO` | `EmployeeVO` |
| Table | `t_{name}` (singular) | `t_employee` |
| Boolean | No `is` prefix | `deleted`, not `isDeleted` |

## Related Rules

- [F01-naming-conventions.md](../../../rules/foundation/F01-naming-conventions.md)

## Example Session

**User:** Check naming conventions in employee module

**AI Actions:**
1. Scan class names
2. Validate table names
3. Check boolean fields
4. Report violations
