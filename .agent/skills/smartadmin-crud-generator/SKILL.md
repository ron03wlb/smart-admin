---
name: smartadmin-crud-generator
description: Generates complete CRUD modules for SmartAdmin project including backend (Controller, Service, Manager, Mapper, Entity), frontend (Vue components), tests, and API documentation
---

# SmartAdmin CRUD Generator

Generates complete CRUD modules following SmartAdmin architecture patterns.

## Usage

```bash
/crud Employee --all-phases        # Full stack: Backend + Frontend + Tests + Docs
/crud Product --backend-only       # Backend only
/crud Customer --frontend-only     # Frontend only (requires existing backend)
```

## Generated Structure

### Backend (Java 21 + Spring Boot 3)

```
sa-admin/src/main/java/net/lab1024/sa/admin/module/{module}/
├── controller/
│   └── {Entity}Controller.java      # REST API endpoints
├── service/
│   └── {Entity}Service.java         # Business logic with Vavr Option/Try
├── manager/
│   └── {Entity}Manager.java         # @Transactional, @Cacheable
├── dao/
│   └── {Entity}Dao.java             # MyBatis Plus mapper
├── domain/
│   ├── entity/
│   │   └── {Entity}Entity.java      # Database entity
│   ├── form/
│   │   ├── {Entity}AddForm.java     # Create request
│   │   ├── {Entity}UpdateForm.java  # Update request
│   │   └── {Entity}QueryForm.java   # Query request
│   └── vo/
│       └── {Entity}VO.java          # Response VO
```

### Frontend (Vue 3 + Ant Design Vue)

```
smart-admin-web/src/views/{module}/{entity}/
├── {entity}-list.vue                # List page with table
├── {entity}-form.vue                # Add/Edit modal form
└── {entity}.api.ts                  # API integration
```

### Tests

```
sa-admin/src/test/java/net/lab1024/sa/admin/module/{module}/
├── {Entity}ServiceTest.java         # Unit tests
└── {Entity}IntegrationTest.java     # Integration tests with Testcontainers
```

## Architecture Rules Applied

| Layer      | Rule                    | Implementation                 |
| ---------- | ----------------------- | ------------------------------ |
| Controller | No direct Mapper access | Calls Service only             |
| Service    | Vavr Option/Try         | `Option<Entity>` returns       |
| Service    | Constructor injection   | `@RequiredArgsConstructor`     |
| Manager    | @Transactional          | Multi-table operations         |
| Mapper     | LambdaQueryWrapper      | Type-safe queries              |
| Entity     | BIGSERIAL PK            | `@TableId(type = IdType.AUTO)` |

## Workflow

```
1. Parse user request for entity name and module
2. Read existing project structure
3. Generate Entity with proper annotations
4. Generate Mapper with default methods
5. Generate Manager with @Transactional
6. Generate Service with Vavr patterns
7. Generate Controller with REST endpoints
8. Generate Form and VO classes
9. If frontend requested:
   a. Generate Vue components
   b. Generate API service
10. If tests requested:
    a. Generate unit tests
    b. Generate integration tests
11. Run quality checks
12. Generate API documentation
```

## Related Rules

- [foundation/01-naming-conventions.md](../../rules/foundation/01-naming-conventions.md)
- [foundation/10-architecture-rules.md](../../rules/foundation/10-architecture-rules.md)
- [technology/functional/08-vavr-fundamentals.md](../../rules/technology/functional/08-vavr-fundamentals.md)
- [technology/database/09-mybatis-plus-core.md](../../rules/technology/database/09-mybatis-plus-core.md)

## Example Session

**User:** `/crud Employee --backend-only`

**AI Agent Actions:**
1. Create `EmployeeEntity.java` with @TableName, @TableId
2. Create `EmployeeDao.java` extending BaseMapper
3. Create `EmployeeManager.java` with @Service, @Transactional
4. Create `EmployeeService.java` with Option returns
5. Create `EmployeeController.java` with @RestController
6. Create Form/VO classes
7. Run `./gradlew check` to validate
8. Report generation status
