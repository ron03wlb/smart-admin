# SmartAdmin Full-Stack CRUD Generator

> One-command full-stack CRUD module: Backend (Entity/Dao/Manager/Service/Controller) + Frontend (Vue 3) + API Docs + Tests

For complete reference, see [SKILL.md](SKILL.md).

---

## Quick Start

```bash
# Create a complete CRUD module
User: "Create a Product CRUD module with name, price, category, and stock fields"

# System auto-executes:
# 1. Collect entity requirements (fields, types, validation)
# 2. Generate backend layers (Entity -> Dao -> Manager -> Service -> Controller)
# 3. Generate frontend components (list page + form modal + API client)
# 4. Generate tests (unit + integration with Testcontainers)
# 5. Generate API docs (Swagger/Knife4j annotations)
# 6. Validate architecture compliance (ArchitectureTest)
```

### Generated File Structure

```
smartadmin-modules/smartadmin-business/src/main/java/
└── net/lab1024/sa/business/product/
    ├── domain/
    │   ├── entity/ProductEntity.java
    │   ├── form/ProductAddForm.java
    │   ├── form/ProductUpdateForm.java
    │   ├── form/ProductQueryForm.java
    │   └── vo/ProductVO.java
    ├── dao/ProductDao.java
    ├── manager/ProductManager.java
    ├── service/ProductService.java
    └── controller/ProductController.java

smart-admin-web/src/
├── views/product/
│   ├── product-list.vue          # List page (table + search + pagination)
│   └── product-form-modal.vue    # Form modal (add/edit)
└── api/product-api.ts             # API client (TypeScript + Axios)

smartadmin-app/src/test/java/
└── net/lab1024/sa/business/product/
    ├── service/ProductServiceTest.java
    └── integration/ProductControllerIntegrationTest.java
```

---

## Execution Modes

| Mode | Command | Time | Use When |
|------|---------|------|----------|
| **All Phases** (recommended) | `--all-phases` | ~20 min | New module, need full-stack |
| **Backend Only** | `--backend-only` | ~10 min | API-only, frontend by another team |
| **Frontend Only** | `--frontend-only` | ~8 min | Backend API exists, need UI |
| **Docs Only** | `--docs-only` | ~3 min | Existing code needs API docs |

### Mode 1: `--all-phases` (Recommended)

Full-stack generation: Backend + Frontend + API Docs + Tests.

```bash
/crud Employee --all-phases
```

**Phases**: Phase 1 (Backend layers) -> Phase 2 (Vue 3 + Ant Design Vue) -> Phase 3 (Knife4j annotations) -> Phase 4 (Tests with Testcontainers)

### Mode 2: `--backend-only`

Generate Entity, Dao, Manager, Service, Controller, and domain objects (Form, VO, QueryForm).

```bash
/crud Order --backend-only
```

Follows SmartAdmin patterns: `@Transactional` in Manager only, `io.vavr.control.Option` in Service, `ResponseDTO` returns, constructor injection.

### Mode 3: `--frontend-only`

Generate Vue 3 components (Composition API + `<script setup>`) + Ant Design Vue 4 + TypeScript API client.

```bash
/crud Brand --frontend-only
```

### Mode 4: `--docs-only`

Add Knife4j annotations (`@Tag`, `@Operation`, `@Schema`) to existing code.

```bash
/crud Product --docs-only
```

---

## When to Use / When NOT to Use

**Use when**:
- New business module (Order, Product, User management)
- Rapid prototyping and validation
- API development (backend-frontend separation)
- Legacy code needs API documentation

**Do NOT use when**:
- Complex business logic (multi-table joins, workflows) -- generate base code first, then extend manually
- Highly custom UI (designer-driven) -- use `--backend-only`, build frontend manually
- Non-RESTful APIs (GraphQL, gRPC, WebSocket)
- Mature module already in production

---

## Composed Skills

This generator integrates four previously standalone skills:

| Original Skill | Integrated As | Deprecated Command |
|---------------|---------------|-------------------|
| `smartadmin-mybatis` | `--backend-only` | `/mybatis` |
| `smartadmin-vue-crud` | `--frontend-only` | `/vue-crud` |
| `smartadmin-api-docs` | `--docs-only` | `/api-docs` |

Deprecated commands still work with migration warnings during the soft-deprecation period.

**Efficiency gain**: 56% time reduction (45 min separately --> 20 min unified).

---

## Multi-Step Workflow

For incremental generation (validate between steps):

```bash
/crud Product --backend-only       # Step 1: Generate backend
# Test API with Postman/curl
/crud Product --frontend-only      # Step 2: Generate frontend
/crud Product --docs-only          # Step 3: Add API docs
```

---

## Verification

```bash
# Compile check
./gradlew :smartadmin-app:compileJava

# Architecture compliance
./gradlew :smartadmin-app:test --tests ArchitectureTest

# Run backend
./gradlew :smartadmin-app:bootRun
# API docs at http://localhost:1024/doc.html

# Run frontend (if generated)
cd smart-admin-web && npm run dev
# UI at http://localhost:7080
```

---

## Related Resources

- **[SKILL.md](SKILL.md)** - Full technical specification and implementation details
- **Phase docs**: [Backend](phases/phase-1-backend.md) | [Frontend](phases/phase-2-frontend.md) | [API Docs](phases/phase-3-api-docs.md) | [Tests](phases/phase-4-tests.md)
- **Examples**: [examples/](examples/) - Complete Product CRUD sample
- **SmartAdmin**: [Patterns](../../../shared/knowledge/smartadmin-patterns.md) | [Architecture Rules](CLAUDE.md)
- **External**: [MyBatis-Plus](https://baomidou.com/) | [Vue 3](https://vuejs.org/) | [Ant Design Vue](https://antdv.com/) | [Knife4j](https://doc.xiaominfo.com/)

---

**Skill Version**: 2.0.0 | **Status**: Stable | **Compatible with**: SmartAdmin v4.0.0+

**Last Updated**: 2026-01-30
