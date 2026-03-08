---
name: smartadmin-crud-generator
description: [P0 - Critical] Generate complete full-stack CRUD module for SmartAdmin (Backend Entity/Dao/Manager/Service/Controller + Frontend Vue components + API client + Tests + Documentation). Use when creating new business modules, implementing CRUD features, or scaffolding complete modules. Triggers when (1) User requests "create/generate CRUD module/feature", (2) User mentions creating new business entity with CRUD operations, (3) User wants to scaffold a complete module (frontend + backend), (4) After defining entity requirements but before implementation, (5) User explicitly requests full-stack code generation.
---

# SmartAdmin Full-Stack CRUD Generator

Generate complete CRUD modules from entity specifications - automatically creates backend (Entity/Dao/Manager/Service/Controller), frontend (Vue components + API client), tests, and API documentation following SmartAdmin patterns.

## Quick Start

**Most common usage:**
```
User: "Create a Product CRUD module with name, price, category, and stock fields"
```

You will:
1. Gather entity requirements (fields, types, validations, relationships)
2. Generate backend layer (Entity → Dao → Manager → Service → Controller)
3. Generate frontend components (List view + Form modal + API client)
4. Generate tests (Unit tests + Integration tests)
5. Generate API documentation (Swagger annotations)
6. Validate with ArchitectureTest

## Trigger Keywords

This skill is automatically activated when the user's request contains:

**Primary Keywords** (High confidence):
- "create CRUD module" - Generate complete full-stack CRUD functionality
- "generate CRUD" - Scaffold backend + frontend CRUD operations
- "full-stack CRUD" - Complete module generation (Backend + Frontend + Tests)
- "scaffold module" - Create new business module from scratch
- "generate entity" - Generate entity with CRUD operations

**Secondary Keywords** (Medium confidence):
- "create business module" - Context: with CRUD operations
- "implement CRUD operations" - Context: for a specific entity
- "generate full-stack code" - Context: CRUD workflow
- "add new module" - Context: with entity definition

**Phrase Patterns**:
- "Create [Entity] CRUD module with [fields]" - Example: "Create Product CRUD module with name, price, category"
- "Generate CRUD for [Entity]" - Example: "Generate CRUD for Employee"
- "I need to scaffold [Entity] module" - Example: "I need to scaffold Customer module"

**Example User Requests**:
```
User: "Create a Product CRUD module with name, price, category, and stock fields"
User: "Generate full-stack CRUD for Employee with firstName, lastName, email, department"
User: "Scaffold an Order module with orderId, customerId, orderDate, totalAmount"
```

**Note**: This skill can also be manually invoked via `/smartadmin-crud-generator` command. Supports phase-based execution: `--backend-only`, `--frontend-only`, `--tests-only`, or `--all-phases`.

## v2.0.0 Composite Skill Architecture

**NEW in v2.0.0 (2026-01-27)**: This skill now consolidates 4 previously separate skills into a unified phase-based workflow.

### Consolidated Skills

This skill **replaces and consolidates**:
- ✅ **smartadmin-mybatis** (Phase 1: Backend) → `--backend-only`
- ✅ **smartadmin-vue-crud** (Phase 2: Frontend) → `--frontend-only`
- ✅ **smartadmin-api-docs** (Phase 3: API Docs) → `--docs-only`
- ✅ **smartadmin-integration-test patterns** (Phase 4: Tests) → included in `--all-phases`

### Phase-Based Execution

**Complete CRUD (All Phases):**
```bash
/crud Employee --all-phases
# Generates: Backend + Frontend + API Docs + Tests
# Time: ~20 minutes (vs 45 minutes with separate skills)
```

**Backend Only (Phase 1):**
```bash
/crud Order --backend-only
# Generates: Entity, Dao, Manager, Service, Controller, Domain objects (Form/VO)
# Consolidates: former /mybatis command
```

**Frontend Only (Phase 2):**
```bash
/crud Brand --frontend-only
# Generates: Vue list component, form modal, API client, TypeScript types
# Consolidates: former /vue-crud command
```

**API Documentation Only (Phase 3):**
```bash
/crud Product --docs-only
# Generates: Swagger/Knife4j annotations (@Tag, @Operation, @Schema)
# Consolidates: former /api-docs command
```

### Backward Compatibility

**Old commands still work** with deprecation warnings (until 2026-06-30):

```bash
# ⚠️ Deprecated (routes to /crud --backend-only)
/mybatis generate Employee

# ⚠️ Deprecated (routes to /crud --frontend-only)
/vue-crud Brand

# ⚠️ Deprecated (routes to /crud --docs-only)
/api-docs ProductController
```

**Migration Timeline:**
- **Weeks 1-12** (Soft Deprecation): Commands work with warnings
- **Weeks 13-24** (Hard Deprecation): Commands show errors + migration guide
- **Week 25+** (Removal): Old commands permanently removed

**See:** [skill-aliases.json](../skill-aliases.json) for routing configuration

### Phase Documentation

Each phase has detailed implementation guides:
- **Phase 1**: [phases/phase-1-backend.md](phases/phase-1-backend.md) - Backend layer generation (MyBatis, layered architecture)
- **Phase 2**: [phases/phase-2-frontend.md](phases/phase-2-frontend.md) - Vue 3 + Ant Design + TypeScript
- **Phase 3**: [phases/phase-3-api-docs.md](phases/phase-3-api-docs.md) - Knife4j/OpenAPI annotations
- **Phase 4**: [phases/phase-4-tests.md](phases/phase-4-tests.md) - Integration tests with Testcontainers

## Why This Skill Solves Integration Issues

**Problem:** "開發整合不行" (Development integration not working)

**Root Causes:**
- Frontend/backend type mismatches
- Inconsistent naming between layers
- Missing validation synchronization
- API contract drift

**Solution:**
This skill generates **synchronized code** from a single source of truth (entity specification), ensuring:
- ✅ Same field names across all layers
- ✅ Matching types (Java ↔ TypeScript)
- ✅ Synchronized validations (backend ↔ frontend)
- ✅ Correct API contracts (ResponseDTO, PageResult)


## 📚 詳細文檔

以下章節已提取至單獨文檔：

1. **[Generation Workflow](docs/generation-workflow.md)** - 完整的 4 階段生成流程
   - Phase 1: Backend Generation (Entity → Dao → Manager → Service → Controller)
   - Phase 2: Frontend Generation (List View + Form Modal + API Client)
   - Phase 3: API Documentation (Swagger/Knife4j annotations)
   - Phase 4: Test Generation (Unit + Integration tests)

2. **[Common Patterns](docs/common-patterns.md)** - SmartAdmin 常用開發模式
   - ResponseDTO patterns, Pagination patterns, Bean conversion
   - Transaction management, Validation patterns

3. **[Phase Execution Logic](docs/phase-execution-logic.md)** - v2.0.0 階段式執行邏輯
   - Sequential execution, Phase dependencies
   - Error handling and rollback strategies

4. **[Phase Implementation Details](phases/)** - 各階段詳細實施文檔
   - [Phase 1: Backend](phases/phase-1-backend.md)
   - [Phase 2: Frontend](phases/phase-2-frontend.md)
   - [Phase 3: API Docs](phases/phase-3-api-docs.md)
   - [Phase 4: Tests](phases/phase-4-tests.md)

---

## File Generation Summary

For a Product CRUD module, generate:

**Backend (9 files):**
1. ProductEntity.java
2. ProductAddForm.java
3. ProductUpdateForm.java
4. ProductQueryForm.java
5. ProductVO.java
6. ProductDao.java
7. ProductDao.xml (MyBatis mapper)
8. ProductManager.java
9. ProductService.java
10. ProductController.java

**Frontend (4 files):**
1. product-api.ts
2. types.ts
3. product-list.vue
4. product-form-modal.vue

**Tests (3 files):**
1. ProductServiceIntegrationTest.java
2. ProductTestFixture.java
3. (Optional) ProductControllerIntegrationTest.java

**Total: ~16 files, ~2000-3000 lines of code**

## Validation Checklist

Before completing generation:

**Backend:**
- [ ] **Package imports use v4.1.0 paths** (`net.lab1024.sa.common.core.util.*`, `net.lab1024.sa.common.mybatis.util.*`, `net.lab1024.sa.common.core.domain.*`)
- [ ] **QueryForm has `@EqualsAndHashCode(callSuper = false)`** when extending PageParam
- [ ] Entity has `@TableName` and `@TableId`
- [ ] Forms have proper `@NotNull`, `@Length` validations
- [ ] VO matches Entity fields + joined fields
- [ ] Dao extends BaseMapper
- [ ] Manager has `@Transactional` on write methods
- [ ] Service validates business rules
- [ ] Controller has Swagger annotations
- [ ] All layers follow naming conventions

**Frontend:**
- [ ] API client returns `ResponseDTO<T>`
- [ ] TypeScript types match backend VOs/Forms
- [ ] List component has pagination
- [ ] Form modal has validations matching backend
- [ ] Permission checks (v-privilege) if needed

**Integration:**
- [ ] Field names match across all layers
- [ ] Types are compatible (Java ↔ TypeScript)
- [ ] Validations synchronized (backend ↔ frontend)
- [ ] ArchitectureTest passes

## Common Patterns

### Enum Handling

**Backend:**
```java
public enum ProductStatusEnum implements BaseEnum {
    AVAILABLE(1, "可用"),
    OUT_OF_STOCK(2, "缺货"),
    DISCONTINUED(3, "已下架");

    private final Integer value;
    private final String desc;
}
```

**Frontend:**
```typescript
export const ProductStatusEnum = {
  AVAILABLE: { value: 1, label: '可用' },
  OUT_OF_STOCK: { value: 2, label: '缺货' },
  DISCONTINUED: { value: 3, label: '已下架' },
} as const;
```

### Foreign Key Relationships

**Service layer joins category name:**
```java
// In ProductDao.xml
LEFT JOIN t_category c ON p.category_id = c.category_id

// In ProductVO
private String categoryName;  // Populated from join
```

**Frontend displays category:**
```vue
<template>
  <a-select v-model:value="form.categoryId">
    <a-select-option v-for="cat in categories" :key="cat.id" :value="cat.id">
      {{ cat.name }}
    </a-select-option>
  </a-select>
</template>
```

### Unique Constraint Validation

**Service validates before insert/update:**
```java
ProductEntity existing = productDao.getByProductCode(form.getProductCode(), null);
if (existing != null) {
    return ResponseDTO.userErrorParam("商品编码已存在");
}
```

**Frontend shows immediate feedback:**
```vue
<a-form-item label="商品编码" name="productCode">
  <a-input
    v-model:value="form.productCode"
    @blur="checkProductCodeUnique"
  />
</a-form-item>
```

## References

For detailed implementation patterns:
- [Backend Layer Patterns](references/backend-layer-patterns.md) - Entity, Dao, Manager, Service, Controller
- [Frontend Component Patterns](references/frontend-component-patterns.md) - Vue components, API clients
- [Validation Synchronization](references/validation-synchronization.md) - Keep backend/frontend validations in sync

## Time Savings

**Manual Implementation:**
- Backend layers: 3-4 hours
- Frontend components: 2-3 hours
- Tests: 1-2 hours
- Documentation: 30 minutes
- **Total: 6-9 hours**

**With This Skill:**
- Requirements gathering: 10 minutes
- Code generation: 5 minutes
- Review and adjust: 15 minutes
- **Total: 30 minutes**

---

## Troubleshooting

### Common Issues by Phase

**Phase 1 Issues**:

**Problem**: "SmartBeanUtil not found"
- **Cause**: Wrong import path (using deprecated `common.core.util.*`)
- **Solution**: Use `import net.lab1024.sa.common.core.util.SmartBeanUtil;`

**Problem**: "ArchitectureTest fails: Service uses java.util.Optional"
- **Cause**: Using Optional instead of Vavr Option
- **Solution**: Change `Optional<T>` to `io.vavr.control.Option<T>`

**Problem**: "Field injection detected"
- **Cause**: Using @Autowired on fields
- **Solution**: Use constructor injection with @RequiredArgsConstructor

**Phase 2 Issues**:

**Problem**: "Type mismatch: Long vs number"
- **Cause**: Incorrect Java ↔ TypeScript mapping
- **Solution**: Check [phase-2-frontend.md](phases/phase-2-frontend.md#type-mapping) for correct mappings

**Problem**: "API call returns undefined"
- **Cause**: Missing ResponseDTO unwrapping
- **Solution**: Use `postRequest<PageResult<T>>()` wrapper, not raw axios

**Problem**: "Permission button not hiding"
- **Cause**: Wrong permission string in v-privilege
- **Solution**: Ensure frontend string matches backend @SaCheckPermission exactly

**Phase 3 Issues**:

**Problem**: "Controller not showing in Knife4j"
- **Cause**: Missing @Tag annotation or wrong tag constant
- **Solution**: Add `@Tag(name = AdminSwaggerTagConst.Business.MANAGER_{MODULE})`

**Problem**: "Field descriptions not displaying"
- **Cause**: Missing @Schema on Form/VO fields
- **Solution**: Add `@Schema(description = "...", example = "...")` to each field

**Phase 4 Issues**:

**Problem**: "Testcontainers failed to start"
- **Cause**: Docker not running or insufficient resources
- **Solution**: Start Docker Desktop, ensure 4GB+ memory allocated

**Problem**: "Tests fail: Transaction not rolling back"
- **Cause**: Missing @Transactional on test methods
- **Solution**: Add `@Transactional` to each test method for automatic rollback

---

## Version History

**v2.0.0** (2026-01-27):
- Consolidated 4 skills (mybatis, vue-crud, api-docs, integration-test patterns) into phase-based workflow
- Added --all-phases, --backend-only, --frontend-only, --docs-only execution modes
- Reduced CRUD generation time: 45 min → 20 min (55% improvement)
- Backward compatibility via skill-aliases.json

**v1.0.0** (2025-12-01):
- Initial full-stack CRUD generator

**Savings: 6-9 hours → 30 minutes (92-95% reduction)**

---

## 相關規則

本技能生成的代碼必須符合以下 SmartAdmin 規範：

### 強制要求

- **[Architecture Rules - Complete](./../../../.agent/rules/foundation/F04-architecture-rules.md)**
  - 嚴格遵循 Controller → Service → Manager → Dao 分層架構
  - Service 層使用 `io.vavr.control.Option`（禁止 `java.util.Optional`）
  - 構造器注入（@RequiredArgsConstructor + private final）
  - ResponseDTO 統一響應格式

- **[Naming Conventions](./../../../.agent/rules/foundation/F01-naming-conventions.md)**
  - 類別命名：XXXController, XXXService, XXXManager, XXXDao
  - Entity 命名：XXXEntity（不是 XXXDomain, XXXPO, XXXDO）
  - Form 命名：XXXAddForm, XXXUpdateForm, XXXQueryForm
  - VO 命名：XXXVO（不是 XXXDTO, XXXResponse）
  - 布林欄位：`deleted` 不是 `isDeleted`
  - 表名：單數形式（`t_employee` 不是 `t_employees`）

- **[Manager Layer Rules](./../../../.agent/rules/foundation/F03-manager-layer.md)**
  - Service 若需 @Transactional → 提取至 Manager 層
  - @Transactional 必須包含 `rollbackFor = Throwable.class`
  - Manager 層處理跨表事務和快取邏輯

### 參考指引

- **[SmartAdmin Patterns](./../../../.claude/shared/knowledge/smartadmin-patterns.md)**
  - ResponseDTO Pattern - API 響應格式
  - Domain Objects Pattern - Entity/Form/VO 使用規範
  - Pagination Pattern - SmartPageUtil 分頁處理
  - Bean Conversion - SmartBeanUtil 對象轉換

- **[Dependency Injection](./../../../.agent/rules/foundation/F04-architecture-rules.md)**
  - 構造器注入強制要求（禁止 @Autowired 欄位注入）

- **[Exception Handling](./../../../.agent/rules/technology/patterns/04-exception-logging.md)**
  - 異常處理層級（Service 返回 ResponseDTO.error()）
  - 業務異常使用 ResponseDTO 封裝

---

## 參考資料

- [SmartAdmin Architecture Documentation](./../../../../docs/architecture/) - 系統架構設計
- [Backend Layer Patterns](references/backend-layer-patterns.md) - 後端分層模式詳解
- [Frontend Component Patterns](references/frontend-component-patterns.md) - 前端組件模式詳解
- [ArchitectureTest.java](./../../../.agent/configs/ArchitectureTest.java) - 架構測試驗證
