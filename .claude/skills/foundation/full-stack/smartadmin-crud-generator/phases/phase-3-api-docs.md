# Phase 3: API Documentation Generation

**Purpose**: Generate Knife4j/OpenAPI documentation for all CRUD endpoints.

**Integration**: This phase consolidates logic from `smartadmin-api-docs` skill.

---

## Swagger Annotations Strategy

Add annotations to:
1. **Controller Class** - `@Tag`
2. **Controller Methods** - `@Operation`
3. **Form Classes** - `@Schema` on fields
4. **VO Classes** - `@Schema` on fields

---

## 1. Controller @Tag Annotation

**Pattern** (from smartadmin-api-docs):
```java
@RestController
@Tag(name = AdminSwaggerTagConst.Business.MANAGER_{MODULE})  // ← Add this
@RequiredArgsConstructor
public class {Entity}Controller {
    ...
}
```

**Tag Selection**:
| Module | Tag Constant |
|--------|--------------|
| goods | `AdminSwaggerTagConst.Business.MANAGER_GOODS` |
| oa | `AdminSwaggerTagConst.Business.MANAGER_OA` |
| erp | `AdminSwaggerTagConst.Business.MANAGER_ERP` |
| custom | Create new constant in `AdminSwaggerTagConst` |

**If new module**, add to `AdminSwaggerTagConst.java`:
```java
public static class Business {
    @Schema(description = "{Module} Management")
    public static final String MANAGER_{MODULE} = "{Module}";
}
```

---

## 2. Controller Method @Operation Annotations

**Pattern**:
```java
@Operation(summary = "{Summary following naming convention}")
@PostMapping("/{module}/{entity}/{action}")
@SaCheckPermission("{module}:{entity}:{permission}")
public ResponseDTO<...> methodName(...) {
    ...
}
```

**Naming Conventions** (from smartadmin-api-docs):

| Method Pattern | @Operation Summary |
|----------------|--------------------|
| `query{Entity}` | "Query {entity}s with pagination" |
| `getById` | "Get {entity} by ID" |
| `add{Entity}` | "Add {entity}" |
| `update{Entity}` | "Update {entity}" |
| `delete{Entity}` | "Delete {entity}" |
| `batchDelete` | "Batch delete {entity}s" |
| `export{Entity}` | "Export {entity} data" |
| `import{Entity}` | "Import {entity} data" |

**Example**:
```java
@Operation(summary = "Query brands with pagination")
@PostMapping("/brand/query")
@SaCheckPermission("goods:brand:query")
public ResponseDTO<PageResult<BrandVO>> queryBrand(@RequestBody @Valid BrandQueryForm queryForm) {
    return brandService.queryBrand(queryForm);
}

@Operation(summary = "Get brand by ID")
@GetMapping("/brand/get/{brandId}")
@SaCheckPermission("goods:brand:query")
public ResponseDTO<BrandVO> getById(@PathVariable Long brandId) {
    return brandService.getById(brandId);
}

@Operation(summary = "Add brand")
@PostMapping("/brand/add")
@SaCheckPermission("goods:brand:add")
public ResponseDTO<Void> add(@RequestBody @Valid BrandAddForm addForm) {
    return brandService.add(addForm);
}

@Operation(summary = "Update brand")
@PostMapping("/brand/update")
@SaCheckPermission("goods:brand:update")
public ResponseDTO<Void> update(@RequestBody @Valid BrandUpdateForm updateForm) {
    return brandService.update(updateForm);
}

@Operation(summary = "Delete brand")
@PostMapping("/brand/delete/{brandId}")
@SaCheckPermission("goods:brand:delete")
public ResponseDTO<Void> delete(@PathVariable Long brandId) {
    return brandService.delete(brandId);
}

@Operation(summary = "Batch delete brands")
@PostMapping("/brand/batchDelete")
@SaCheckPermission("goods:brand:delete")
public ResponseDTO<Void> batchDelete(@RequestBody @Valid BrandBatchDeleteForm batchDeleteForm) {
    return brandService.batchDelete(batchDeleteForm);
}
```

---

## 3. Form Class @Schema Annotations

**Pattern**:
```java
@Data
public class {Entity}AddForm {

    @Schema(description = "{Field description}", required = true, example = "{example}")
    @NotBlank(message = "{Field} cannot be empty")
    private String {field};

    // Generated for each field
}
```

**Field Description Guidelines**:
- Use clear, concise English descriptions
- Include constraints in description (e.g., "max 100 characters")
- Provide realistic example values
- Indicate if field is required

**Example**:
```java
@Data
public class BrandAddForm {

    @Schema(description = "Brand name (max 100 characters)", required = true, example = "Nike")
    @NotBlank(message = "Brand name cannot be empty")
    @Size(max = 100, message = "Brand name cannot exceed 100 characters")
    private String brandName;

    @Schema(description = "Brand logo URL", example = "https://example.com/logo.png")
    private String logoUrl;

    @Schema(description = "Status (1: Enabled, 0: Disabled)", required = true, example = "1")
    @NotNull(message = "Status cannot be null")
    private Integer status;

    @Schema(description = "Display order (higher value = higher priority)", example = "1")
    private Integer displayOrder;

    @Schema(description = "Brand description (max 500 characters)", example = "Global sports brand")
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
}
```

**For QueryForm**:
```java
@Data
public class BrandQueryForm extends PageParam {

    @Schema(description = "Search keyword (matches brand name)", example = "Nike")
    private String keyword;

    @Schema(description = "Status filter (1: Enabled, 0: Disabled, null: All)", example = "1")
    private Integer status;

    @Schema(description = "Start create time", example = "2024-01-01 00:00:00")
    private LocalDateTime createTimeBegin;

    @Schema(description = "End create time", example = "2024-12-31 23:59:59")
    private LocalDateTime createTimeEnd;
}
```

---

## 4. VO Class @Schema Annotations

**Pattern**:
```java
@Data
public class {Entity}VO {

    @Schema(description = "{Entity} ID", example = "1")
    private Long {entity}Id;

    @Schema(description = "{Field description}", example = "{example}")
    private String {field};

    @Schema(description = "Create time", example = "2024-01-01 12:00:00")
    private LocalDateTime createTime;

    @Schema(description = "Update time", example = "2024-01-01 12:00:00")
    private LocalDateTime updateTime;
}
```

**Example**:
```java
@Data
public class BrandVO {

    @Schema(description = "Brand ID", example = "1")
    private Long brandId;

    @Schema(description = "Brand name", example = "Nike")
    private String brandName;

    @Schema(description = "Brand logo URL", example = "https://example.com/logo.png")
    private String logoUrl;

    @Schema(description = "Status (1: Enabled, 0: Disabled)", example = "1")
    private Integer status;

    @Schema(description = "Display order", example = "1")
    private Integer displayOrder;

    @Schema(description = "Brand description", example = "Global sports brand")
    private String description;

    @Schema(description = "Create time", example = "2024-01-01 12:00:00")
    private LocalDateTime createTime;

    @Schema(description = "Update time", example = "2024-01-01 12:00:00")
    private LocalDateTime updateTime;
}
```

---

## 5. Import Statements

Add required imports to each file:

**Controller**:
```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.lab1024.sa.system.constant.AdminSwaggerTagConst;
```

**Form/VO**:
```java
import io.swagger.v3.oas.annotations.media.Schema;
```

---

## Complete Example

**BrandController.java** (Fully Annotated):
```java
package net.lab1024.sa.business.goods.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.system.constant.AdminSwaggerTagConst;
import net.lab1024.sa.business.goods.domain.form.*;
import net.lab1024.sa.business.goods.domain.vo.BrandVO;
import net.lab1024.sa.business.goods.service.BrandService;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * Brand Controller
 *
 * @author SmartAdmin Generator
 * @date 2024-01-01
 */
@RestController
@Tag(name = AdminSwaggerTagConst.Business.MANAGER_GOODS)
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @Operation(summary = "Query brands with pagination")
    @PostMapping("/goods/brand/query")
    @SaCheckPermission("goods:brand:query")
    public ResponseDTO<PageResult<BrandVO>> query(@RequestBody @Valid BrandQueryForm queryForm) {
        return brandService.query(queryForm);
    }

    @Operation(summary = "Get brand by ID")
    @GetMapping("/goods/brand/get/{brandId}")
    @SaCheckPermission("goods:brand:query")
    public ResponseDTO<BrandVO> getById(@PathVariable Long brandId) {
        return brandService.getById(brandId);
    }

    @Operation(summary = "Add brand")
    @PostMapping("/goods/brand/add")
    @SaCheckPermission("goods:brand:add")
    public ResponseDTO<Void> add(@RequestBody @Valid BrandAddForm addForm) {
        return brandService.add(addForm);
    }

    @Operation(summary = "Update brand")
    @PostMapping("/goods/brand/update")
    @SaCheckPermission("goods:brand:update")
    public ResponseDTO<Void> update(@RequestBody @Valid BrandUpdateForm updateForm) {
        return brandService.update(updateForm);
    }

    @Operation(summary = "Delete brand")
    @PostMapping("/goods/brand/delete/{brandId}")
    @SaCheckPermission("goods:brand:delete")
    public ResponseDTO<Void> delete(@PathVariable Long brandId) {
        return brandService.delete(brandId);
    }

    @Operation(summary = "Batch delete brands")
    @PostMapping("/goods/brand/batchDelete")
    @SaCheckPermission("goods:brand:delete")
    public ResponseDTO<Void> batchDelete(@RequestBody @Valid BrandBatchDeleteForm batchDeleteForm) {
        return brandService.batchDelete(batchDeleteForm);
    }
}
```

---

## Knife4j UI Preview

After adding annotations, documentation will be available at:
- **URL**: http://localhost:1024/doc.html
- **Group**: Select appropriate module group
- **Controller**: Find {Entity}Controller under {Module} tag

**UI Features**:
- View all endpoints with summaries
- See request/response models with field descriptions
- Test APIs directly in browser (Try It Out)
- View example values
- See validation constraints

---

## Validation Checklist

After generation, verify:
- [ ] Controller has `@Tag(name = AdminSwaggerTagConst.Business.MANAGER_{MODULE})`
- [ ] All methods have `@Operation(summary = "...")`
- [ ] All Form/VO fields have `@Schema(description = "...")`
- [ ] Example values are realistic and helpful
- [ ] Required fields marked with `required = true`
- [ ] Visit http://localhost:1024/doc.html and verify:
  - [ ] Controller appears in correct tag group
  - [ ] All endpoints visible with summaries
  - [ ] Request/Response models show descriptions
  - [ ] Can successfully test API calls

---

## Common Issues & Solutions

**Issue 1**: Controller not appearing in Knife4j UI
- **Solution**: Check `@Tag` annotation is present and correct
- **Solution**: Verify controller has at least one `@Operation` method
- **Solution**: Restart application to reload Swagger config

**Issue 2**: Field descriptions not showing
- **Solution**: Ensure `@Schema(description = "...")` on each field
- **Solution**: Check import: `io.swagger.v3.oas.annotations.media.Schema`

**Issue 3**: Example values not helpful
- **Solution**: Use realistic values (e.g., "Nike" not "string")
- **Solution**: Match data type (number example for numeric fields)

---

## Integration Notes

This phase consolidates:
- ✅ Controller `@Tag` patterns from smartadmin-api-docs
- ✅ Method `@Operation` naming conventions
- ✅ Form/VO `@Schema` annotation patterns
- ✅ Knife4j UI integration and validation
- ✅ SmartAdmin tag constants usage

**Documentation Standards**:
- Clear, concise English descriptions
- Realistic example values
- Consistent naming across endpoints
- Complete coverage of all fields
