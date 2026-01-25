# SmartAdmin Swagger/Knife4j Annotation Patterns

Complete guide to Swagger annotation patterns used in SmartAdmin controllers.

## Table of Contents

1. [Controller-Level Annotations](#controller-level-annotations)
2. [Operation Annotations](#operation-annotations)
3. [Parameter Annotations](#parameter-annotations)
4. [Schema Annotations](#schema-annotations)
5. [Response Documentation](#response-documentation)
6. [Common Patterns by Operation Type](#common-patterns-by-operation-type)

---

## Controller-Level Annotations

### @Tag - Controller Classification

```java
@RestController
@Tag(name = AdminSwaggerTagConst.Business.MANAGER_GOODS)
@RequiredArgsConstructor
public class ProductController {
    // endpoints...
}
```

**SmartAdmin Tag Constants** (`AdminSwaggerTagConst.java`):

```java
public class AdminSwaggerTagConst {
    public static class Business {
        public static final String MANAGER_GOODS = "商品管理";
        public static final String MANAGER_CATEGORY = "类目管理";
        public static final String MANAGER_ORDER = "订单管理";
    }
    
    public static class System {
        public static final String SYSTEM_EMPLOYEE = "员工管理";
        public static final String SYSTEM_ROLE = "角色管理";
        public static final String SYSTEM_MENU = "菜单管理";
    }
}
```

---

## Operation Annotations

### @Operation - Endpoint Documentation

**Pattern:**
```java
@Operation(summary = "{Action} {Entity} [{Additional Context}]")
```

**Examples:**

```java
@Operation(summary = "Query employees with pagination")
@PostMapping("/employee/query")
public ResponseDTO<PageResult<EmployeeVO>> queryEmployee(@RequestBody @Valid EmployeeQueryForm queryForm) {
    return employeeService.queryEmployee(queryForm);
}

@Operation(summary = "Add employee")
@PostMapping("/employee/add")
public ResponseDTO<String> addEmployee(@RequestBody @Valid EmployeeAddForm addForm) {
    return employeeService.addEmployee(addForm);
}

@Operation(summary = "Update employee")
@PostMapping("/employee/update")
public ResponseDTO<String> updateEmployee(@RequestBody @Valid EmployeeUpdateForm updateForm) {
    return employeeService.updateEmployee(updateForm);
}

@Operation(summary = "Batch delete employees")
@PostMapping("/employee/batchDelete")
public ResponseDTO<String> batchDelete(@RequestBody List<Long> employeeIdList) {
    return employeeService.batchDelete(employeeIdList);
}

@Operation(summary = "Get employee by ID")
@GetMapping("/employee/get/{employeeId}")
public ResponseDTO<EmployeeVO> getById(@PathVariable Long employeeId) {
    return employeeService.getById(employeeId);
}
```

**Common Action Verbs:**
- Query / Search / List - For paginated queries
- Get / Retrieve - For single record retrieval
- Add / Create - For creating new records
- Update / Modify - For updating existing records
- Delete / Remove - For deletion (soft or hard)
- Batch {Action} - For bulk operations
- Export / Import - For data export/import

---

## Parameter Annotations

### @RequestBody Parameters

```java
@PostMapping("/product/query")
public ResponseDTO<PageResult<ProductVO>> queryProduct(
    @RequestBody @Valid ProductQueryForm queryForm
) {
    return productService.queryProduct(queryForm);
}
```

### @PathVariable Parameters

```java
@GetMapping("/product/get/{productId}")
public ResponseDTO<ProductVO> getById(
    @PathVariable Long productId
) {
    return productService.getById(productId);
}
```

### @RequestParam Parameters

```java
@GetMapping("/product/list")
public ResponseDTO<List<ProductVO>> list(
    @RequestParam(required = false) String keyword
) {
    return productService.list(keyword);
}
```

---

## Schema Annotations

### Form Classes

```java
@Data
@Schema(description = "Product add form")
public class ProductAddForm {

    @Schema(description = "Product name")
    @NotBlank(message = "Product name cannot be empty")
    @Length(max = 100, message = "Product name cannot exceed 100 characters")
    private String productName;

    @Schema(description = "Product code (unique)")
    @NotBlank(message = "Product code cannot be empty")
    @Length(max = 50, message = "Product code cannot exceed 50 characters")
    private String productCode;

    @Schema(description = "Price (must be positive)")
    @NotNull(message = "Price cannot be empty")
    @DecimalMin(value = "0", message = "Price must be greater than or equal to 0")
    private BigDecimal price;

    @Schema(description = "Stock quantity")
    @NotNull(message = "Stock cannot be empty")
    @Min(value = 0, message = "Stock must be greater than or equal to 0")
    private Integer stock;

    @Schema(description = "Category ID (FK to t_category)")
    @NotNull(message = "Category ID cannot be empty")
    private Long categoryId;

    @Schema(description = "Status (1=Available, 2=OutOfStock, 3=Discontinued)")
    @NotNull(message = "Status cannot be empty")
    private Integer status;
}
```

### VO Classes

```java
@Data
@Schema(description = "Product view object")
public class ProductVO {

    @Schema(description = "Product ID")
    private Long productId;

    @Schema(description = "Product name")
    private String productName;

    @Schema(description = "Product code")
    private String productCode;

    @Schema(description = "Price")
    private BigDecimal price;

    @Schema(description = "Stock quantity")
    private Integer stock;

    @Schema(description = "Category name (joined from t_category)")
    private String categoryName;

    @Schema(description = "Status (1=Available, 2=OutOfStock, 3=Discontinued)")
    private Integer status;

    @Schema(description = "Create time")
    private LocalDateTime createTime;

    @Schema(description = "Update time")
    private LocalDateTime updateTime;
}
```

### Query Form Classes

```java
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "Product query form")
public class ProductQueryForm extends PageParam {

    @Schema(description = "Search keyword (product name or code)")
    private String keyword;

    @Schema(description = "Category ID filter")
    private Long categoryId;

    @Schema(description = "Status filter (1=Available, 2=OutOfStock, 3=Discontinued)")
    private Integer status;

    @Schema(description = "Deleted flag filter (true=deleted, false=active)")
    private Boolean deletedFlag;
}
```

---

## Response Documentation

### Standard ResponseDTO Pattern

SmartAdmin uses `ResponseDTO<T>` wrapper for all API responses:

```java
// Success with data
ResponseDTO.ok(data);

// Success without data
ResponseDTO.ok();

// User error (validation failure, business rule violation)
ResponseDTO.userErrorParam("Error message");

// System error
ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
```

**Swagger Documentation:**

```java
@Operation(summary = "Query products with pagination")
@PostMapping("/product/query")
public ResponseDTO<PageResult<ProductVO>> queryProduct(
    @RequestBody @Valid ProductQueryForm queryForm
) {
    return productService.queryProduct(queryForm);
}
```

**Response structure:**
```json
{
  "ok": true,
  "code": 200,
  "msg": "操作成功",
  "data": {
    "total": 100,
    "list": [
      {
        "productId": 1,
        "productName": "Product A",
        "price": 99.99
      }
    ],
    "pageNum": 1,
    "pageSize": 10,
    "totalPages": 10
  }
}
```

---

## Common Patterns by Operation Type

### CRUD Query Pattern

```java
@Operation(summary = "Query {entity} with pagination")
@PostMapping("/{entity}/query")
public ResponseDTO<PageResult<{Entity}VO>> query{Entity}(
    @RequestBody @Valid {Entity}QueryForm queryForm
) {
    return {entity}Service.query{Entity}(queryForm);
}
```

**Full example:**
```java
@Operation(summary = "Query products with pagination")
@PostMapping("/product/query")
public ResponseDTO<PageResult<ProductVO>> queryProduct(
    @RequestBody @Valid ProductQueryForm queryForm
) {
    return productService.queryProduct(queryForm);
}
```

### CRUD Add Pattern

```java
@Operation(summary = "Add {entity}")
@PostMapping("/{entity}/add")
public ResponseDTO<String> add{Entity}(
    @RequestBody @Valid {Entity}AddForm addForm
) {
    return {entity}Service.add{Entity}(addForm);
}
```

### CRUD Update Pattern

```java
@Operation(summary = "Update {entity}")
@PostMapping("/{entity}/update")
public ResponseDTO<String> update{Entity}(
    @RequestBody @Valid {Entity}UpdateForm updateForm
) {
    return {entity}Service.update{Entity}(updateForm);
}
```

### CRUD Delete Pattern

```java
@Operation(summary = "Delete {entity}")
@PostMapping("/{entity}/delete")
public ResponseDTO<String> delete{Entity}(
    @RequestBody Long {entity}Id
) {
    return {entity}Service.delete{Entity}({entity}Id);
}
```

### Batch Delete Pattern

```java
@Operation(summary = "Batch delete {entity}s")
@PostMapping("/{entity}/batchDelete")
public ResponseDTO<String> batchDelete(
    @RequestBody List<Long> {entity}IdList
) {
    return {entity}Service.batchDelete({entity}IdList);
}
```

### Get By ID Pattern

```java
@Operation(summary = "Get {entity} by ID")
@GetMapping("/{entity}/get/{id}")
public ResponseDTO<{Entity}VO> getById(
    @PathVariable Long {entity}Id
) {
    return {entity}Service.getById({entity}Id);
}
```

---

## Special Patterns

### File Upload

```java
@Operation(summary = "Upload employee avatar")
@PostMapping("/employee/uploadAvatar")
public ResponseDTO<String> uploadAvatar(
    @RequestParam("file") MultipartFile file
) {
    return employeeService.uploadAvatar(file);
}
```

### Export

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

### Import

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

## Validation Rules in Schema

### Common Validation Annotations with Descriptions

```java
@Schema(description = "Email address (valid email format required)")
@NotBlank(message = "Email cannot be empty")
@Email(message = "Invalid email format")
private String email;

@Schema(description = "Phone number (11 digits)")
@NotBlank(message = "Phone cannot be empty")
@Pattern(regexp = "^1[3-9]\\d{9}$", message = "Invalid phone number format")
private String phone;

@Schema(description = "Age (must be between 18 and 100)")
@NotNull(message = "Age cannot be empty")
@Min(value = 18, message = "Age must be at least 18")
@Max(value = 100, message = "Age cannot exceed 100")
private Integer age;

@Schema(description = "ID card number (18 digits)")
@Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$",
         message = "Invalid ID card number format")
private String idCard;
```

---

## Best Practices

1. **Always use @Operation on endpoint methods** - Provides clear API documentation
2. **Add @Schema to all Form/VO fields** - Documents request/response structure
3. **Include constraints in description** - e.g., "Product name (max 100 chars, unique)"
4. **Use AdminSwaggerTagConst for @Tag** - Ensures consistency across controllers
5. **Document FK relationships** - e.g., "Category ID (FK to t_category)"
6. **Specify enum values** - e.g., "Status (1=Available, 2=OutOfStock, 3=Discontinued)"
7. **Follow naming conventions** - query{Entity}, add{Entity}, update{Entity}, delete{Entity}
8. **Use appropriate HTTP methods** - POST for commands/queries, GET for simple retrieval

---

## Quick Reference

| Operation | HTTP Method | Endpoint Pattern | Summary Pattern |
|-----------|-------------|------------------|-----------------|
| Query | POST | `/{entity}/query` | "Query {entity}s with pagination" |
| Add | POST | `/{entity}/add` | "Add {entity}" |
| Update | POST | `/{entity}/update` | "Update {entity}" |
| Delete | POST | `/{entity}/delete` | "Delete {entity}" |
| Batch Delete | POST | `/{entity}/batchDelete` | "Batch delete {entity}s" |
| Get by ID | GET | `/{entity}/get/{id}` | "Get {entity} by ID" |
| Export | POST | `/{entity}/export` | "Export {entity} list to Excel" |
| Import | POST | `/{entity}/import` | "Import {entity}s from Excel" |
