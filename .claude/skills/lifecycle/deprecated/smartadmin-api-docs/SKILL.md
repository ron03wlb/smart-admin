---
name: smartadmin-api-docs
description: Auto-generate and maintain Knife4j/OpenAPI API documentation for SmartAdmin. Use when (1) User requests "update API docs", "generate swagger annotations", or "document API"; (2) After creating new controllers/endpoints; (3) When Form/VO fields are added/modified; (4) User mentions "Swagger", "Knife4j", "OpenAPI", or "API documentation"; (5) During code review if endpoints lack @Operation annotations; (6) When frontend team needs API contract documentation.
---

# SmartAdmin API Documentation Generator

Auto-generate and maintain Knife4j/OpenAPI documentation for SmartAdmin controllers, ensuring API documentation stays synchronized with code.

## Quick Start

**Most common usage:**
```
User: "Add API documentation to BrandController"
User: "Update swagger annotations for Employee module"
User: "Generate OpenAPI spec for all endpoints"
```

You will:
1. Analyze Controller methods and identify endpoints
2. Generate `@Operation` annotations with appropriate summaries
3. Add `@Schema` annotations to Form/VO classes
4. Apply `@Tag` using `AdminSwaggerTagConst`
5. Validate annotations follow SmartAdmin patterns
6. (Optional) Generate OpenAPI 3.0 specification

## Why This Skill Matters

**Problem:** API documentation often lags behind code, causing integration issues.

**Root Causes:**
- Manual annotation is tedious and error-prone
- Developers forget to update @Schema descriptions
- Inconsistent annotation styles across controllers
- No validation that docs match actual endpoints

**Solution:**
This skill generates **consistent, complete** Swagger annotations following SmartAdmin patterns, ensuring:
- ✅ Every endpoint has @Operation summary
- ✅ All Form/VO fields have @Schema descriptions
- ✅ Consistent naming and structure
- ✅ Validation rules documented in descriptions

## Core Tasks

### Task 1: Document Controller Endpoints

**When:** New controller created or endpoints added

**Workflow:**

1. **Read the Controller** - Analyze methods and return types
2. **Identify Pattern** - Determine operation type (Query/Add/Update/Delete)
3. **Generate @Operation** - Create summary following naming convention
4. **Apply @Tag** - Use appropriate `AdminSwaggerTagConst` value

**Example:**

```java
// Before
@RestController
@RequiredArgsConstructor
public class BrandController {
    private final BrandService brandService;

    @PostMapping("/brand/query")
    public ResponseDTO<PageResult<BrandVO>> queryBrand(@RequestBody @Valid BrandQueryForm queryForm) {
        return brandService.queryBrand(queryForm);
    }
}

// After
@RestController
@Tag(name = AdminSwaggerTagConst.Business.MANAGER_GOODS)  // ← Added
@RequiredArgsConstructor
public class BrandController {
    private final BrandService brandService;

    @Operation(summary = "Query brands with pagination")  // ← Added
    @PostMapping("/brand/query")
    public ResponseDTO<PageResult<BrandVO>> queryBrand(@RequestBody @Valid BrandQueryForm queryForm) {
        return brandService.queryBrand(queryForm);
    }
}
```

**Pattern Reference:**

| Method Name | Endpoint Pattern | @Operation Summary |
|-------------|------------------|--------------------|
| `query{Entity}` | POST `/{entity}/query` | "Query {entity}s with pagination" |
| `add{Entity}` | POST `/{entity}/add` | "Add {entity}" |
| `update{Entity}` | POST `/{entity}/update` | "Update {entity}" |
| `delete{Entity}` | POST `/{entity}/delete` | "Delete {entity}" |
| `batchDelete` | POST `/{entity}/batchDelete` | "Batch delete {entity}s" |
| `getById` | GET `/{entity}/get/{id}` | "Get {entity} by ID" |

**Complete CRUD Controller Example:**

```java
@RestController
@Tag(name = AdminSwaggerTagConst.Business.MANAGER_GOODS)
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Query products with pagination")
    @PostMapping("/product/query")
    public ResponseDTO<PageResult<ProductVO>> queryProduct(@RequestBody @Valid ProductQueryForm queryForm) {
        return productService.queryProduct(queryForm);
    }

    @Operation(summary = "Add product")
    @PostMapping("/product/add")
    public ResponseDTO<String> addProduct(@RequestBody @Valid ProductAddForm addForm) {
        return productService.addProduct(addForm);
    }

    @Operation(summary = "Update product")
    @PostMapping("/product/update")
    public ResponseDTO<String> updateProduct(@RequestBody @Valid ProductUpdateForm updateForm) {
        return productService.updateProduct(updateForm);
    }

    @Operation(summary = "Batch delete products")
    @PostMapping("/product/batchDelete")
    public ResponseDTO<String> batchDelete(@RequestBody List<Long> productIdList) {
        return productService.batchDelete(productIdList);
    }

    @Operation(summary = "Get product by ID")
    @GetMapping("/product/get/{productId}")
    public ResponseDTO<ProductVO> getById(@PathVariable Long productId) {
        return productService.getById(productId);
    }
}
```

---

### Task 2: Document Form Classes

**When:** Form classes created or fields modified

**Workflow:**

1. **Add @Schema to class** - Describe the form's purpose
2. **Add @Schema to each field** - Include constraints and business rules
3. **Document enums in description** - e.g., "Status (1=Active, 0=Disabled)"
4. **Reference FK relationships** - e.g., "Category ID (FK to t_category)"

**Example:**

```java
// Before
@Data
public class ProductAddForm {
    @NotBlank(message = "Product name cannot be empty")
    @Length(max = 100, message = "Product name cannot exceed 100 characters")
    private String productName;

    @NotNull(message = "Price cannot be empty")
    @DecimalMin(value = "0", message = "Price must be >= 0")
    private BigDecimal price;

    @NotNull(message = "Category ID cannot be empty")
    private Long categoryId;

    @NotNull(message = "Status cannot be empty")
    private Integer status;
}

// After
@Data
@Schema(description = "Product add form")  // ← Added
public class ProductAddForm {

    @Schema(description = "Product name (max 100 chars, unique)")  // ← Added
    @NotBlank(message = "Product name cannot be empty")
    @Length(max = 100, message = "Product name cannot exceed 100 characters")
    private String productName;

    @Schema(description = "Price (must be >= 0)")  // ← Added
    @NotNull(message = "Price cannot be empty")
    @DecimalMin(value = "0", message = "Price must be >= 0")
    private BigDecimal price;

    @Schema(description = "Category ID (FK to t_category)")  // ← Added
    @NotNull(message = "Category ID cannot be empty")
    private Long categoryId;

    @Schema(description = "Status (1=Available, 2=OutOfStock, 3=Discontinued)")  // ← Added
    @NotNull(message = "Status cannot be empty")
    private Integer status;
}
```

**Schema Description Patterns:**

| Field Type | Pattern | Example |
|------------|---------|---------|
| String with max length | "{Name} (max {N} chars)" | "Product name (max 100 chars)" |
| Unique field | "{Name} (unique)" | "Product code (unique)" |
| FK reference | "{Name} (FK to {table})" | "Category ID (FK to t_category)" |
| Enum | "{Name} ({value}={label}, ...)" | "Status (1=Active, 0=Disabled)" |
| Numeric constraint | "{Name} (must be {constraint})" | "Price (must be >= 0)" |
| Optional field | "{Name} (optional)" | "Description (optional)" |

---

### Task 3: Document VO Classes

**When:** VO classes created or response structure changes

**Workflow:**

1. **Add @Schema to class** - "{ Entity} view object"
2. **Add @Schema to each field** - Describe what data represents
3. **Mark joined fields** - e.g., "Category name (joined from t_category)"
4. **Document computed fields** - e.g., "Full name (actualName)"

**Example:**

```java
@Data
@Schema(description = "Product view object")  // ← Added
public class ProductVO {

    @Schema(description = "Product ID")  // ← Added
    private Long productId;

    @Schema(description = "Product name")  // ← Added
    private String productName;

    @Schema(description = "Product code (unique)")  // ← Added
    private String productCode;

    @Schema(description = "Price")  // ← Added
    private BigDecimal price;

    @Schema(description = "Stock quantity")  // ← Added
    private Integer stock;

    @Schema(description = "Category name (joined from t_category)")  // ← Added
    private String categoryName;

    @Schema(description = "Status (1=Available, 2=OutOfStock, 3=Discontinued)")  // ← Added
    private Integer status;

    @Schema(description = "Create time")  // ← Added
    private LocalDateTime createTime;

    @Schema(description = "Update time")  // ← Added
    private LocalDateTime updateTime;
}
```

---

### Task 4: Document QueryForm Classes

**When:** Query/search functionality implemented

**Workflow:**

1. **Add @Schema to class** - "{Entity} query form"
2. **Document search fields** - Specify what they search (name, code, etc.)
3. **Document filter fields** - Explain filter behavior
4. **Inherit pagination** - QueryForm extends PageParam (pageNum, pageSize)

**Example:**

```java
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "Product query form")  // ← Added
public class ProductQueryForm extends PageParam {

    @Schema(description = "Search keyword (product name or code)")  // ← Added
    private String keyword;

    @Schema(description = "Category ID filter")  // ← Added
    private Long categoryId;

    @Schema(description = "Status filter (1=Available, 2=OutOfStock, 3=Discontinued)")  // ← Added
    private Integer status;

    @Schema(description = "Deleted flag filter (true=deleted, false=active)")  // ← Added
    private Boolean deletedFlag;
}
```

---

### Task 5: Apply AdminSwaggerTagConst

**When:** Adding @Tag to controllers

**Common Tags:**

```java
// Business module tags
AdminSwaggerTagConst.Business.MANAGER_GOODS       // 商品管理
AdminSwaggerTagConst.Business.MANAGER_CATEGORY    // 类目管理
AdminSwaggerTagConst.Business.MANAGER_ORDER       // 订单管理

// System module tags
AdminSwaggerTagConst.System.SYSTEM_EMPLOYEE       // 员工管理
AdminSwaggerTagConst.System.SYSTEM_ROLE           // 角色管理
AdminSwaggerTagConst.System.SYSTEM_MENU           // 菜单管理
AdminSwaggerTagConst.System.SYSTEM_DEPARTMENT     // 部门管理
```

**Where to find tags:**
- Check `AdminSwaggerTagConst.java` in `smartadmin-app/src/main/java/.../constant/`
- If tag doesn't exist, add it to the appropriate inner class

**Example:**

```java
// If BrandController doesn't have a specific tag, use related tag
@Tag(name = AdminSwaggerTagConst.Business.MANAGER_GOODS)
public class BrandController {
    // ...
}
```

---

### Task 6: Generate OpenAPI Specification

**When:** Frontend needs API contract or for API documentation portal

**Process:**

1. Run application with Knife4j enabled
2. Access Swagger UI: `http://localhost:1024/doc.html`
3. Generate OpenAPI JSON: `http://localhost:1024/v3/api-docs`
4. Save to file: `openapi.yaml` or `openapi.json`

**OpenAPI Output Example:**

```yaml
openapi: 3.0.1
info:
  title: SmartAdmin API
  version: 4.0.0
paths:
  /brand/query:
    post:
      tags:
        - 商品管理
      summary: Query brands with pagination
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/BrandQueryForm'
      responses:
        '200':
          description: OK
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ResponseDTOPageResultBrandVO'
components:
  schemas:
    BrandQueryForm:
      type: object
      properties:
        keyword:
          type: string
          description: Search keyword (brand name)
        status:
          type: integer
          description: Status filter (1=Enabled, 0=Disabled)
        pageNum:
          type: integer
          format: int64
        pageSize:
          type: integer
          format: int64
```

---

## Common Patterns

### File Upload Endpoint

```java
@Operation(summary = "Upload employee avatar")
@PostMapping("/employee/uploadAvatar")
public ResponseDTO<String> uploadAvatar(
    @RequestParam("file") MultipartFile file
) {
    return employeeService.uploadAvatar(file);
}
```

### Export Endpoint

```java
@Operation(summary = "Export employee list to Excel")
@PostMapping("/employee/export")
public void exportEmployee(
    @RequestBody EmployeeQueryForm queryForm,
    HttpServletResponse response
) {
    employeeService.exportEmployee(queryForm, response);
}
```

### Import Endpoint

```java
@Operation(summary = "Import employees from Excel")
@PostMapping("/employee/import")
public ResponseDTO<String> importEmployee(
    @RequestParam("file") MultipartFile file
) {
    return employeeService.importEmployee(file);
}
```

---

## Validation Checklist

Before completing API documentation:

**Controller Level:**
- [ ] @Tag annotation with AdminSwaggerTagConst value
- [ ] All public methods have @Operation annotation
- [ ] @Operation summaries follow naming conventions
- [ ] Appropriate HTTP methods (@PostMapping, @GetMapping)

**Form Classes:**
- [ ] Class has @Schema(description = "{Entity} {add/update/query} form")
- [ ] All fields have @Schema annotations
- [ ] Enum values documented in descriptions
- [ ] FK relationships noted
- [ ] Constraints mentioned (max length, min/max values)

**VO Classes:**
- [ ] Class has @Schema(description = "{Entity} view object")
- [ ] All fields have @Schema annotations
- [ ] Joined fields marked with "(joined from {table})"
- [ ] Computed fields explained

**QueryForm Classes:**
- [ ] Extends PageParam
- [ ] Has @EqualsAndHashCode(callSuper = false)
- [ ] Search fields specify what they search
- [ ] Filter fields explain filter behavior

**Overall Quality:**
- [ ] Descriptions are clear and concise
- [ ] Consistent terminology across module
- [ ] No generic descriptions like "field1", "the field"
- [ ] Business rules documented where applicable

---

## References

Detailed patterns and examples:
- [references/swagger-patterns.md](references/swagger-patterns.md) - Complete Swagger annotation patterns with examples

---

## Time Savings

**Manual API Documentation:**
- 5-10 minutes per endpoint
- 35+ controllers × 5 endpoints = 175 endpoints
- **Total: 15-30 hours**

**Skill-Generated Documentation:**
- Analyze patterns: 5 minutes
- Generate annotations: 10 minutes per controller
- Validation: 5 minutes
- **Total per controller: 20 minutes**
- **Total for 35 controllers: ~12 hours**

**Time Saved: 3-18 hours (20-60% reduction)**

**Quality Improvements:**
- ✅ 100% endpoint coverage (vs. 60-70% manual)
- ✅ Consistent naming across controllers
- ✅ All constraints documented
- ✅ Frontend/backend contract alignment
