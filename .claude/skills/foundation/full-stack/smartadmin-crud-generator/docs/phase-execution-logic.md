## Phase-Based Execution Logic (v2.0.0)

### Execution Mode Detection

When user makes a request, determine execution mode:

**Mode 1: Complete CRUD (--all-phases)**
- **Triggers**:
  - User says "create CRUD module"
  - User wants "full-stack module"
  - User requests "complete Employee management"
  - No specific phase mentioned
- **Execution**: Run Phase 1 → Phase 2 → Phase 3 → Phase 4
- **Time**: ~20 minutes

**Mode 2: Backend Only (--backend-only)**
- **Triggers**:
  - User says "generate backend only"
  - User wants "Entity/Dao/Manager/Service/Controller"
  - User mentions "MyBatis" or "database layer"
  - Former `/mybatis` command users
- **Execution**: Run Phase 1 only
- **Time**: ~8 minutes
- **Consolidates**: smartadmin-mybatis skill

**Mode 3: Frontend Only (--frontend-only)**
- **Triggers**:
  - User says "generate frontend only"
  - User wants "Vue components"
  - User mentions "Ant Design" or "TypeScript"
  - Former `/vue-crud` command users
- **Execution**: Run Phase 2 only
- **Time**: ~7 minutes
- **Consolidates**: smartadmin-vue-crud skill

**Mode 4: API Docs Only (--docs-only)**
- **Triggers**:
  - User says "add API documentation"
  - User wants "Swagger annotations"
  - User mentions "Knife4j" or "OpenAPI"
  - Former `/api-docs` command users
- **Execution**: Run Phase 3 only
- **Time**: ~3 minutes
- **Consolidates**: smartadmin-api-docs skill

### Phase Execution Sequence

#### Phase 1: Backend Generation

**Input Required**:
- Entity name
- Field specifications (name, type, validations)
- Module name (e.g., "goods", "oa", "system")
- Foreign key relationships

**Execution Steps**:
1. Read [phases/phase-1-backend.md](phases/phase-1-backend.md)
2. Generate files in order:
   - Domain objects (Entity, Forms, VO)
   - Dao interface + Mapper.xml (if complex queries)
   - Manager class (transaction layer)
   - Service class (business logic with Vavr)
   - Controller class (API endpoints)
3. Validate package imports (use `net.lab1024.sa.foundation.*`)
4. Add proper annotations (@Transactional, @SaCheckPermission, @Valid)

**Output**:
```
smartadmin-modules/smartadmin-business/src/main/java/net/lab1024/sa/business/{module}/
├── controller/{Entity}Controller.java
├── service/{Entity}Service.java
├── manager/{Entity}Manager.java
├── dao/{Entity}Dao.java
└── domain/
    ├── entity/{Entity}Entity.java
    ├── form/{Entity}QueryForm.java, {Entity}AddForm.java, {Entity}UpdateForm.java
    └── vo/{Entity}VO.java
```

**Time**: ~8 minutes

#### Phase 2: Frontend Generation

**Input Required**:
- Entity name
- Backend field list (from Phase 1 or user specification)
- Module route path

**Execution Steps**:
1. Read [phases/phase-2-frontend.md](phases/phase-2-frontend.md)
2. Generate files in order:
   - TypeScript types (matching backend VOs/Forms)
   - API client (typed Axios with ResponseDTO handling)
   - List component (a-table with pagination)
   - Form modal (a-modal with validation)
3. Map Java types → TypeScript types
4. Add permission directives (v-privilege)
5. Configure routes

**Output**:
```
smart-admin-web/src/
├── api/{module}/{entity}-types.ts
├── api/{module}/{entity}-api.ts
└── views/{module}/{entity}/
    ├── {entity}-list.vue
    └── {entity}-form-modal.vue
```

**Time**: ~7 minutes

#### Phase 3: API Documentation Generation

**Input Required**:
- Controller class file path
- Form/VO class file paths
- Module name

**Execution Steps**:
1. Read [phases/phase-3-api-docs.md](phases/phase-3-api-docs.md)
2. Add annotations to existing files:
   - Controller: @Tag annotation
   - Methods: @Operation with summary
   - Form/VO fields: @Schema with description and examples
3. Import Swagger packages
4. Select appropriate AdminSwaggerTagConst

**Output**:
- Modified Controller.java (with @Tag and @Operation)
- Modified Form classes (with @Schema)
- Modified VO classes (with @Schema)

**Time**: ~3 minutes

#### Phase 4: Integration Tests Generation

**Input Required**:
- Entity name
- Service class location
- Test data specifications

**Execution Steps**:
1. Read [phases/phase-4-tests.md](phases/phase-4-tests.md)
2. Generate test files:
   - Integration test class extending BaseIntegrationTest
   - Test methods for CRUD operations
   - Testcontainers setup (if needed)
3. Add test data builders
4. Validate @Transactional on test methods

**Output**:
```
smartadmin-modules/smartadmin-business/src/test/java/net/lab1024/sa/business/{module}/
└── service/{Entity}IntegrationTest.java
```

**Time**: ~5 minutes

### Phase Combination Examples

**Example 1: Complete CRUD**
```
User: "Create Employee CRUD module with name, email, department fields"

Execution:
1. Gather requirements (confirm fields, types, validations)
2. Phase 1: Generate backend (Entity, Dao, Manager, Service, Controller)
3. Phase 2: Generate frontend (Vue components, API client, types)
4. Phase 3: Add API documentation (Swagger annotations)
5. Phase 4: Generate integration tests
6. Validate with ArchitectureTest
7. Provide usage instructions

Total: ~20 minutes
```

**Example 2: Backend Only (former /mybatis)**
```
User: "Generate backend Dao layer for Product"

Execution:
1. Gather requirements (fields, types, relationships)
2. Phase 1 only: Generate Entity, Dao, Manager, Service, Controller
3. Skip Phase 2, 3, 4
4. Provide backend usage instructions

Total: ~8 minutes
```

**Example 3: Frontend Only (former /vue-crud)**
```
User: "Create Vue frontend for Order module (backend already exists)"

Execution:
1. Read existing backend VO/Form classes to extract field info
2. Phase 2 only: Generate TypeScript types, API client, Vue components
3. Skip Phase 1, 3, 4
4. Provide frontend integration instructions

Total: ~7 minutes
```

**Example 4: Add Docs to Existing Code (former /api-docs)**
```
User: "Add Swagger documentation to BrandController"

Execution:
1. Read existing Controller, Form, VO classes
2. Phase 3 only: Add @Tag, @Operation, @Schema annotations
3. Skip Phase 1, 2, 4
4. Verify in Knife4j UI (http://localhost:1024/doc.html)

Total: ~3 minutes
```

### Backward Compatibility Routing

When detecting deprecated command patterns, route to appropriate phase:

```
User input: "/mybatis generate Employee"
Detection: Old command pattern
Response: "⚠️ The '/mybatis' command is deprecated. Routing to '/crud Employee --backend-only'..."
Execution: Phase 1 only
```

```
User input: "/vue-crud Brand"
Detection: Old command pattern
Response: "⚠️ The '/vue-crud' command is deprecated. Routing to '/crud Brand --frontend-only'..."
Execution: Phase 2 only
```

```
User input: "/api-docs ProductController"
Detection: Old command pattern
Response: "⚠️ The '/api-docs' command is deprecated. Routing to '/crud Product --docs-only'..."
Execution: Phase 3 only
```

### Validation After Each Phase

**Phase 1 Validation**:
- [ ] Entity uses @TableName, @TableId, @TableLogic
- [ ] Dao extends BaseMapper
- [ ] Manager has @Transactional(rollbackFor = Throwable.class)
- [ ] Service uses Vavr Option (not java.util.Optional)
- [ ] Controller has @SaCheckPermission and @Valid

**Phase 2 Validation**:
- [ ] TypeScript types match backend Java classes
- [ ] API client uses postRequest/getRequest wrappers
- [ ] List component has a-table with pagination
- [ ] Form modal has validation rules
- [ ] Permission strings match backend @SaCheckPermission

**Phase 3 Validation**:
- [ ] Controller has @Tag with AdminSwaggerTagConst
- [ ] All methods have @Operation with clear summaries
- [ ] All Form/VO fields have @Schema with descriptions and examples
- [ ] Visit http://localhost:1024/doc.html to verify UI display

**Phase 4 Validation**:
- [ ] Test class extends BaseIntegrationTest
- [ ] Tests use @SpringBootTest and @TestMethodOrder
- [ ] Testcontainers configured for PostgreSQL + Redis
- [ ] All CRUD operations have test methods
- [ ] Tests pass: `./gradlew :smartadmin-app:test --tests {Entity}IntegrationTest`

---

