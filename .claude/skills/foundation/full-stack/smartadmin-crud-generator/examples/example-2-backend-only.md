# Example 2: Backend Only Generation (--backend-only)

**Scenario**: Generate backend Dao layer for Product module. Frontend will be handled by a separate team or already exists.

**Time**: ~8 minutes
**Mode**: --backend-only
**Consolidates**: Former `/mybatis` command

---

## User Request

```
User: "Generate backend only for Product module with:
- Product name (required, max 100 chars)
- Product code (required, unique, max 50 chars)
- Category ID (required, FK)
- Price (required, positive, decimal)
- Stock (required, non-negative, integer)
- Status (required, enum: 1=Available, 2=OutOfStock, 3=Discontinued)
- Description (optional, max 500 chars)"
```

---

## AI Detection

**Triggers for --backend-only**:
- ✅ User says "backend only"
- ✅ User mentions "Dao layer" or "database layer"
- ✅ User wants "Entity/Manager/Service/Controller"
- ✅ No mention of frontend requirements

**AI Response**:
```
Detected: --backend-only mode
Executing: Phase 1 only (Backend Generation)
Skipping: Phase 2 (Frontend), Phase 3 (API Docs), Phase 4 (Tests)

Estimated time: ~8 minutes
```

---

## Phase 1: Backend Generation (8 minutes)

### Generated Files

**Package**: `sa-admin/src/main/java/net/lab1024/sa/admin/module/business/goods/`

```
goods/
├── controller/ProductController.java (6 endpoints)
├── service/ProductService.java (CRUD + Vavr Option)
├── manager/ProductManager.java (@Transactional)
├── dao/ProductDao.java (extends BaseMapper)
└── domain/
    ├── entity/ProductEntity.java (@TableName)
    ├── form/
    │   ├── ProductQueryForm.java (extends PageParam)
    │   ├── ProductAddForm.java (@Valid annotations)
    │   ├── ProductUpdateForm.java (includes productId)
    │   └── ProductBatchDeleteForm.java (List<Long>)
    └── vo/ProductVO.java (includes categoryName from JOIN)
```

**Mapper XML**: `resources/mapper/goods/ProductDao.xml` (complex queries with JOIN)

### Key Features

✅ **Entity**:
- `@TableId(type = IdType.AUTO)` for primary key
- `@TableLogic` for soft delete (deletedFlag field)
- Field naming: `deletedFlag` NOT `isDeleted` (ArchUnit enforced)

✅ **Dao**:
- Extends `BaseMapper<ProductEntity>`
- Custom method: `query(Page, QueryForm)` with XML Mapper
- Unique constraint check: `getByProductCode(String code, Long excludeId)`

✅ **Manager**:
- `@Transactional(rollbackFor = Throwable.class)` on all write methods
- CRUD methods: add, update, delete, batchDelete
- Constructor injection only (no @Autowired fields)

✅ **Service**:
- Uses `io.vavr.control.Option` (NOT `java.util.Optional`)
- Returns `ResponseDTO<T>` for all methods
- Validates product code uniqueness before insert/update
- Pagination: `SmartPageUtil.convert2PageQuery(form)`

✅ **Controller**:
- `@SaCheckPermission` on all endpoints
- `@Valid` on request bodies
- RESTful paths: `/goods/product/query`, `/goods/product/add`, etc.

### Time Breakdown

1. **Entity + Domain Objects** (2 min): Entity, QueryForm, AddForm, UpdateForm, VO, BatchDeleteForm
2. **Dao + Mapper** (2 min): Dao interface, Mapper.xml with JOIN query
3. **Manager** (1 min): Transaction layer with 4 methods
4. **Service** (2 min): Business logic with Vavr Option, validation, pagination
5. **Controller** (1 min): 6 REST endpoints with permissions

**Total**: 8 minutes

---

## Validation

### Run ArchitectureTest

```bash
./gradlew :sa-admin:test --tests ArchitectureTest
```

**Expected**: Pass ✅

**Validates**:
- Service uses Vavr Option (not java.util.Optional)
- Manager has @Transactional
- Controller never directly accesses Dao
- No field injection (@Autowired)
- Boolean field named `deletedFlag`

### Manual Verification

- [ ] Entity uses correct annotations
- [ ] Dao extends BaseMapper
- [ ] Manager has transactions
- [ ] Service returns ResponseDTO
- [ ] Controller has permissions

---

## Output Summary

**Files Generated**: 11 files
**Time**: ~8 minutes
**Consolidates**: Former `/mybatis` command

**Next Steps**:
1. ✅ Backend ready for API testing (use Postman/curl)
2. ⏳ Frontend team can start using API contract
3. ⏳ Add API documentation later with `--docs-only`
4. ⏳ Add integration tests later

**Integration with Frontend Team**:
```
Backend team provides:
- API endpoints: GET/POST /goods/product/*
- Request/Response contracts: ProductQueryForm, ProductVO, etc.
- Permission strings: goods:product:query, goods:product:add, etc.

Frontend team implements:
- TypeScript types matching backend VOs
- API client using provided endpoints
- Permission checks matching backend strings
```

---

## Backward Compatibility

**Old Command** (deprecated):
```bash
/mybatis generate Product
```

**New Command**:
```bash
/crud Product --backend-only
```

**Deprecation Warning**:
```
⚠️ The '/mybatis' command is deprecated. Use '/crud Product --backend-only' instead.

Migration Guide: https://smartadmin.ai/docs/skills/crud-consolidation

This command will be removed after Week 24 (2026-06-30).
```

---

## Related Examples

- **[Example 1](example-1-complete-crud.md)** - Complete CRUD (all phases)
- **[Example 3](example-3-frontend-only.md)** - Add frontend to this backend later
- **[Example 4](example-4-add-docs.md)** - Add API docs to generated backend

**Complete Documentation**: [SKILL.md](../SKILL.md) | [phase-1-backend.md](../phases/phase-1-backend.md)
