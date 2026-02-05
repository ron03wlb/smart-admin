# SmartAdmin CRUD Generator Examples

This directory contains end-to-end examples demonstrating the phase-based execution modes of the consolidated CRUD generator.

---

## Examples Overview

| Example | Mode | Time | Use Case |
|---------|------|------|----------|
| **[Example 1](example-1-complete-crud.md)** | --all-phases | ~20 min | Complete CRUD module from scratch (recommended for new modules) |
| **[Example 2](example-2-backend-only.md)** | --backend-only | ~8 min | Backend only when frontend exists or separate teams (consolidates /mybatis) |
| **[Example 3](example-3-frontend-only.md)** | --frontend-only | ~7 min | Frontend only when backend exists (consolidates /vue-crud) |
| **[Example 4](example-4-add-docs.md)** | --docs-only | ~3 min | Add Swagger docs to existing code (consolidates /api-docs) |

---

## Example 1: Complete CRUD Module (--all-phases)

**Scenario**: Create Employee management module from scratch

**What's Generated**:
- ✅ Backend (11 files): Entity, Dao, Dao.xml, Manager, Service, Controller, Forms, VO
- ✅ Frontend (4 files): TypeScript types, API client, List component, Form modal
- ✅ Tests (1 file): Integration tests with Testcontainers
- ✅ Documentation: Swagger annotations on all classes

**Time**: ~20 minutes (vs 45 minutes with separate skills)

**View**: [example-1-complete-crud.md](example-1-complete-crud.md)

---

## Example 2: Backend Only (--backend-only)

**Scenario**: Generate backend Dao layer for Product module (frontend handled by separate team)

**What's Generated**:
- ✅ Backend (11 files): Entity, Dao, Dao.xml, Manager, Service, Controller, Forms, VO
- ❌ Frontend: Skipped
- ❌ Tests: Skipped (can add later)
- ❌ Documentation: Skipped (can add later)

**Time**: ~8 minutes

**Consolidates**: Former `/mybatis` command

**View**: [example-2-backend-only.md](example-2-backend-only.md)

---

## Example 3: Frontend Only (--frontend-only)

**Scenario**: Create Vue frontend for Order module (backend already exists)

**What's Generated**:
- ❌ Backend: Skipped (already exists)
- ✅ Frontend (4 files): TypeScript types, API client, List component, Form modal
- ❌ Tests: Skipped
- ❌ Documentation: Skipped (assumes backend has it)

**Time**: ~7 minutes

**Consolidates**: Former `/vue-crud` command

**View**: [example-3-frontend-only.md](example-3-frontend-only.md)

---

## Example 4: Add API Documentation (--docs-only)

**Scenario**: Add Swagger documentation to existing BrandController

**What's Generated**:
- ❌ Backend: No new files (modifies existing)
- ❌ Frontend: Skipped
- ❌ Tests: Skipped
- ✅ Documentation: Adds @Tag, @Operation, @Schema annotations

**Time**: ~3 minutes

**Consolidates**: Former `/api-docs` command

**View**: [example-4-add-docs.md](example-4-add-docs.md)

---

## Quick Comparison: Before vs After

### Before Consolidation (4 separate skills)

**Generate complete Employee CRUD**:
```bash
# Step 1: Backend (smartadmin-mybatis)
User: "/mybatis generate Employee"
Time: ~12 minutes

# Step 2: Frontend (smartadmin-vue-crud)
User: "/vue-crud Employee"
Time: ~10 minutes

# Step 3: Documentation (smartadmin-api-docs)
User: "/api-docs EmployeeController"
Time: ~5 minutes

# Step 4: Integration Tests (smartadmin-integration-test)
User: "/integration-test EmployeeService"
Time: ~8 minutes

# Total: 4 commands, ~35 minutes, context switching overhead
```

### After Consolidation (1 composite skill)

**Generate complete Employee CRUD**:
```bash
# Single command
User: "Create Employee CRUD module with name, email, department"
# Auto-detects: --all-phases mode
# Executes: Phase 1 → Phase 2 → Phase 3 → Phase 4

# Total: 1 command, ~20 minutes, no context switching
```

**Time Savings**: 35 min → 20 min (43% improvement)
**Complexity Reduction**: 4 commands → 1 command (75% reduction)
**Integration Quality**: Automatic synchronization across all layers

---

## Usage Patterns

### Pattern 1: New Module Development (Recommended)

**Use**: Example 1 (--all-phases)

```bash
User: "Create Customer CRUD module with name, email, phone, address, city, country"
AI: Executes all 4 phases automatically
Result: Complete module ready for deployment
```

### Pattern 2: Microservices / Separate Teams

**Backend Team**:
```bash
User: "Generate backend for Payment module"
AI: Executes Phase 1 only (--backend-only)
Result: Backend API ready
```

**Frontend Team** (later):
```bash
User: "Create Vue frontend for Payment (backend exists)"
AI: Executes Phase 2 only (--frontend-only)
Result: Frontend UI ready
```

### Pattern 3: Incremental Enhancement

**Day 1**: Generate backend
```bash
User: "Create Order backend"
Mode: --backend-only
```

**Day 2**: Add frontend
```bash
User: "Add Vue frontend for Order"
Mode: --frontend-only (detects backend exists)
```

**Day 3**: Add documentation
```bash
User: "Add Swagger docs to OrderController"
Mode: --docs-only
```

**Day 4**: Add tests
```bash
User: "Generate integration tests for OrderService"
Mode: Uses phase-4 patterns (future feature)
```

### Pattern 4: Documentation Sprint

**Scenario**: Team decides to document all existing APIs

```bash
User: "Add Swagger docs to all controllers in goods module"
AI: Executes --docs-only for each controller
Result: All APIs documented in ~30 minutes (10 controllers × 3 min each)
```

---

## Validation Checklist

After using any example, validate:

### Backend Validation
- [ ] Run `./gradlew :smartadmin-app:test --tests ArchitectureTest` → Pass
- [ ] Check imports use `net.lab1024.sa.common.core.*` (not deprecated packages)
- [ ] Verify Service uses `io.vavr.control.Option` (not `java.util.Optional`)
- [ ] Confirm Manager has `@Transactional(rollbackFor = Throwable.class)`
- [ ] Validate Controller has `@SaCheckPermission` on all endpoints

### Frontend Validation
- [ ] TypeScript types match backend Java classes (field names, types)
- [ ] API client uses `postRequest`/`getRequest` wrappers
- [ ] List component has pagination (`a-table` with `pagination` prop)
- [ ] Form modal has validation rules matching backend
- [ ] Permission strings match backend (`v-privilege` === `@SaCheckPermission`)

### Documentation Validation
- [ ] Visit http://localhost:1024/doc.html
- [ ] Find controller in Knife4j UI under correct tag
- [ ] All endpoints show with clear summaries
- [ ] Request/Response models display field descriptions
- [ ] "Try It Out" works for sample requests

### Integration Validation
- [ ] Field names identical across all layers
- [ ] Types compatible (Java ↔ TypeScript mapping correct)
- [ ] Validations synchronized (backend ↔ frontend)
- [ ] End-to-end test: Add → Query → Update → Delete → Success

---

## Troubleshooting

See [SKILL.md](../SKILL.md#troubleshooting) for detailed phase-specific troubleshooting.

**Common Issues**:

**"SmartBeanUtil not found"**
- Use: `import net.lab1024.sa.common.core.util.SmartBeanUtil;`

**"ArchitectureTest fails: Service uses Optional"**
- Change: `Optional<T>` → `io.vavr.control.Option<T>`

**"TypeScript type mismatch"**
- Check: [phase-2-frontend.md](../phases/phase-2-frontend.md#type-mapping)
- Common: `Long/Integer` → `number`, `LocalDateTime` → `string`

**"API docs not showing in Knife4j"**
- Verify: `@Tag(name = AdminSwaggerTagConst.Business.MANAGER_{MODULE})`
- Restart: Application to reload Swagger config

---

## Related Documentation

- **[SKILL.md](../SKILL.md)** - Complete skill documentation with all patterns
- **[phase-1-backend.md](../phases/phase-1-backend.md)** - Backend generation patterns
- **[phase-2-frontend.md](../phases/phase-2-frontend.md)** - Frontend generation patterns
- **[phase-3-api-docs.md](../phases/phase-3-api-docs.md)** - API documentation patterns
- **[phase-4-tests.md](../phases/phase-4-tests.md)** - Integration test patterns
- **[skill-aliases.json](../../skill-aliases.json)** - Backward compatibility configuration

---

**Last Updated**: 2026-01-27
**Version**: 2.0.0 (CRUD Pipeline Consolidation)
