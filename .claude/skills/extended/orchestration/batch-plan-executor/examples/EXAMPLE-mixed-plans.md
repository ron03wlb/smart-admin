# Example: Mixed Plan Types Execution

**Scenario**: Mixed Plan Types Batch Execution
**Version**: v1.0.0
**Complexity**: Medium
**Duration**: ~75 minutes (sequential) | ~35 minutes (parallel in v1.1.0+)

---

## Overview

此示例展示如何同時執行三種不同類型的方案：
1. **Claude Code Plan** (CRUD Generator)
2. **Skills Phase Doc** (Integration Testing)
3. **Project Plan** (Multi-tenant Setup)

### Learning Objectives

- ✅ 理解三種方案類型的識別機制
- ✅ 學習文件級別衝突檢測
- ✅ 掌握 Dry-run 模式的使用
- ✅ 了解混合類型方案的執行策略

---

## Scenario Setup

### Input: 3 Plan Files

#### Plan 1: Claude Code Plan (Product CRUD)

**File Path**: `~/.claude/plans/product-crud.md`

**Content**:

```markdown
---
name: product-crud
type: crud
created_at: 2026-01-28
---

# Product CRUD Module Implementation Plan

## Overview
Create a complete CRUD module for Product management with full-stack implementation.

## Entity Definition

```java
// File: smart-admin-api/smartadmin-modules/smartadmin-business/src/main/java/net/lab1024/sa/business/product/domain/entity/ProductEntity.java
package net.lab1024.sa.business.product.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_product")
public class ProductEntity {
    @TableId(type = IdType.AUTO)
    private Long productId;

    private String productName;
    private String productCode;
    private String description;
    private Integer categoryId;
    private Double price;
    private Integer stock;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Boolean deleted;
}
```

## Controller Implementation

```java
// File: smart-admin-api/smartadmin-modules/smartadmin-business/src/main/java/net/lab1024/sa/business/product/controller/ProductController.java
package net.lab1024.sa.business.product.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.PageResponseDTO;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product")
@Tag(name = "Product Management")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Query product list with pagination")
    @PostMapping("/query")
    @SaCheckPermission("product:query")
    public ResponseDTO<PageResponseDTO<ProductVO>> query(@RequestBody ProductQueryForm queryForm) {
        return productService.query(queryForm);
    }

    @Operation(summary = "Create product")
    @PostMapping("/create")
    @SaCheckPermission("product:create")
    public ResponseDTO<Long> create(@RequestBody @Valid ProductAddForm addForm) {
        return productService.create(addForm);
    }
}
```

## Frontend Implementation

```typescript
// File: smart-admin-web/src/api/product/product-api.ts
import { postRequest } from '@/utils/request';
import type { ProductQueryForm, ProductVO, ProductAddForm } from './product-types';

export const productApi = {
  query: (queryForm: ProductQueryForm) => {
    return postRequest<PageResponseDTO<ProductVO>>('/api/product/query', queryForm);
  },

  create: (addForm: ProductAddForm) => {
    return postRequest<number>('/api/product/create', addForm);
  },
};
```

## Implementation Checklist
- [ ] Create ProductEntity
- [ ] Create ProductDao/Mapper
- [ ] Create ProductService
- [ ] Create ProductController
- [ ] Create Vue components (ProductList.vue, ProductForm.vue)
- [ ] Create product-api.ts
```

---

#### Plan 2: Skills Phase Doc (Integration Testing)

**File Path**: `.claude/skills/smartadmin-testing-suite/phases/phase-2-integration-tests.md`

**Content**:

```markdown
# Phase 2: Integration Testing with Testcontainers

**Phase**: 2/4
**Status**: ✅ Production Ready
**Duration**: 15-20 minutes
**Complexity**: Medium

## Overview
This phase implements Spring Boot integration tests with Testcontainers for the Product module.

## Test Setup

```java
// File: smart-admin-api/smartadmin-modules/smartadmin-business/src/test/java/net/lab1024/sa/business/product/ProductServiceIntegrationTest.java
package net.lab1024.sa.business.product;

import net.lab1024.sa.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ProductServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ProductService productService;

    @Test
    void testCreateProduct() {
        ProductAddForm form = ProductAddForm.builder()
            .productName("Test Product")
            .productCode("TEST-001")
            .price(99.99)
            .build();

        ResponseDTO<Long> response = productService.create(form);
        assertTrue(response.getOk());
    }
}
```

## Controller Test

```java
// File: smart-admin-api/smartadmin-modules/smartadmin-business/src/test/java/net/lab1024/sa/business/product/ProductControllerIntegrationTest.java
package net.lab1024.sa.business.product;

import net.lab1024.sa.BaseControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerIntegrationTest extends BaseControllerTest {

    @Test
    void testQueryProducts() throws Exception {
        mockMvc.perform(post("/api/product/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pageNum\":1,\"pageSize\":10}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(true));
    }
}
```

## Implementation Checklist
- [ ] Create ProductServiceIntegrationTest
- [ ] Create ProductControllerIntegrationTest
- [ ] Add Testcontainers configuration
- [ ] Verify all tests pass
```

---

#### Plan 3: Project Plan (Multi-tenant Setup)

**File Path**: `docs/plans/tenant/multi-tenant-setup.md`

**Content**:

```markdown
---
module: tenant
feature: multi-tenant-setup
type: custom
priority: high
dependencies: []
skill: null  # Requires manual execution
---

# Multi-tenant Setup Implementation Plan

## Overview
Implement multi-tenant architecture for Product module with tenant isolation.

## Database Schema

```sql
-- File: smart-admin-api/smartadmin-app/src/main/resources/db/migration/V1_1__add_tenant_support.sql
ALTER TABLE t_product ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 0;
CREATE INDEX idx_tenant_product ON t_product(tenant_id);
```

## Tenant Interceptor

```java
// File: smart-admin-api/smartadmin-support/src/main/java/net/lab1024/sa/base/module/support/tenant/TenantInterceptor.java
package net.lab1024.sa.support.tenant;

import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

public class TenantInterceptor extends TenantLineInnerInterceptor {

    @Override
    public Expression getTenantId() {
        Long tenantId = TenantContext.getCurrentTenantId();
        return new LongValue(tenantId);
    }

    @Override
    public boolean ignoreTable(String tableName) {
        return !tableName.startsWith("t_");
    }
}
```

## Update Product Entity

```java
// File: smart-admin-api/smartadmin-modules/smartadmin-business/src/main/java/net/lab1024/sa/business/product/domain/entity/ProductEntity.java
// (Modify existing file)

@Data
@TableName("t_product")
public class ProductEntity {
    // ... existing fields ...

    // Add tenant field
    private Long tenantId;
}
```

## Implementation Checklist
- [ ] Create database migration script
- [ ] Implement TenantInterceptor
- [ ] Update ProductEntity with tenantId
- [ ] Update ProductService to handle tenant context
- [ ] Add tenant validation in Controller
```

---

## Execution Process

### Step 1: Dry-run Analysis

**Command**:

```bash
/batch-execute --dry-run \
  ~/.claude/plans/product-crud.md \
  .claude/skills/smartadmin-testing-suite/phases/phase-2-integration-tests.md \
  docs/plans/tenant/multi-tenant-setup.md
```

### Step 2: Plan Discovery & Classification

**Discovery Process**:

```
╔══════════════════════════════════════════════════════════════════
║ Phase 1: Plan Discovery
╠══════════════════════════════════════════════════════════════════
║ Scanning plans...
║
║ Plan #1: ~/.claude/plans/product-crud.md
║   - Type: CLAUDE_CODE_PLAN (detected from file path)
║   - Extracted metadata:
║     • name: product-crud
║     • type: crud
║     • created_at: 2026-01-28
║   - Skill mapping: smartadmin-crud-generator (confidence: 1.0)
║
║ Plan #2: .claude/skills/smartadmin-testing-suite/phases/phase-2-integration-tests.md
║   - Type: SKILL_PHASE_DOC (detected from file path)
║   - Extracted metadata:
║     • skill_name: smartadmin-testing-suite
║     • phase_number: 2
║     • phase_name: Integration Testing with Testcontainers
║   - Skill mapping: smartadmin-testing-suite (confidence: 1.0)
║
║ Plan #3: docs/plans/tenant/multi-tenant-setup.md
║   - Type: PROJECT_PLAN (detected from file path)
║   - Extracted metadata:
║     • module: tenant
║     • feature: multi-tenant-setup
║     • type: custom
║     • priority: high
║   - Skill mapping: None (manual execution required)
║     ⚠ Warning: No skill mapping for module 'tenant'
╠══════════════════════════════════════════════════════════════════
║ Discovery Summary:
║   - Total Plans: 3
║   - Claude Code Plans: 1
║   - Skills Phase Docs: 1
║   - Project Plans: 1
║   - Successfully Mapped: 2/3 (66.7%)
║   - Requires Manual Execution: 1
╚══════════════════════════════════════════════════════════════════
```

### Step 3: Conflict Detection

**Conflict Analysis**:

```
╔══════════════════════════════════════════════════════════════════
║ Phase 2: Conflict Detection
╠══════════════════════════════════════════════════════════════════
║ Extracting affected files from each plan...
║
║ Plan #1 (product-crud) affects:
║   - ProductEntity.java
║   - ProductController.java
║   - ProductService.java
║   - ProductDao.java
║   - ProductMapper.xml
║   - product-api.ts
║   - ProductList.vue
║   - ProductForm.vue
║
║ Plan #2 (integration-tests) affects:
║   - ProductServiceIntegrationTest.java
║   - ProductControllerIntegrationTest.java
║
║ Plan #3 (multi-tenant-setup) affects:
║   - V1_1__add_tenant_support.sql
║   - TenantInterceptor.java
║   - ProductEntity.java  ⚠ CONFLICT!
║
║ Detecting conflicts...
║
║ ✗ Conflict #1: ProductEntity.java
║   - Affected by: product-crud, multi-tenant-setup
║   - Severity: HIGH (file-level conflict)
║   - Resolution: Serialize execution
║
║ ✓ No conflict: ProductController.java
║   - Only affected by: product-crud
║
║ ✓ No conflict: ProductServiceIntegrationTest.java
║   - Only affected by: integration-tests
╠══════════════════════════════════════════════════════════════════
║ Conflict Summary:
║   - Total Conflicts: 1
║   - File-level Conflicts: 1 (HIGH)
║   - Module-level Conflicts: 0 (MEDIUM) [v1.0.0 - Not implemented]
║   - Dependency Conflicts: 0 (CRITICAL) [v1.0.0 - Not implemented]
╠══════════════════════════════════════════════════════════════════
║ Conflict Details:
║
║ Conflict #1: smart-admin-api/smartadmin-modules/smartadmin-business/.../ProductEntity.java
║   Conflicting Plans:
║     - [crud] product-crud
║     - [custom] multi-tenant-setup
║   Conflict Type: FILE_LEVEL
║   Severity: HIGH
║   Resolution Strategy: Serial Execution
║     • Execute product-crud first (creates ProductEntity)
║     • Execute multi-tenant-setup second (modifies ProductEntity)
╚══════════════════════════════════════════════════════════════════
```

### Step 4: Execution Plan Generation

**Execution Plan**:

```
╔══════════════════════════════════════════════════════════════════
║ Phase 3: Execution Plan Generation
╠══════════════════════════════════════════════════════════════════
║ Resolving conflicts and creating execution waves...
║
║ Wave 1 (Serial - 1 plan):
║   Plan: product-crud
║   Skill: smartadmin-crud-generator
║   Reason: Must execute first (creates ProductEntity.java)
║   Estimated Duration: 25 minutes
║
║ Wave 2 (Serial - 1 plan):
║   Plan: multi-tenant-setup
║   Skill: None (Manual execution required)
║   Reason: Depends on ProductEntity.java from Wave 1
║   Estimated Duration: 30 minutes (manual)
║
║ Wave 3 (Parallel - 1 plan) [v1.0.0: Sequential]:
║   Plan: integration-tests
║   Skill: smartadmin-testing-suite
║   Reason: No conflicts, can run independently
║   Estimated Duration: 20 minutes
╠══════════════════════════════════════════════════════════════════
║ Execution Strategy:
║   - Total Waves: 3
║   - Serial Waves: 2 (Wave 1, Wave 2)
║   - Parallel Waves: 1 (Wave 3) [v1.0.0: Sequential]
║   - Total Plans: 3
║   - Auto-executable Plans: 2 (product-crud, integration-tests)
║   - Manual Plans: 1 (multi-tenant-setup)
╚══════════════════════════════════════════════════════════════════
```

### Step 5: Time Estimation

**Time Estimation Report**:

```
╔══════════════════════════════════════════════════════════════════
║ Phase 4: Time Estimation
╠══════════════════════════════════════════════════════════════════
║ Estimating execution time...
║
║ Plan Estimates:
║   - product-crud (crud): 25 minutes
║   - multi-tenant-setup (custom, manual): 30 minutes
║   - integration-tests (testing): 20 minutes
║
║ Sequential Execution:
║   Wave 1: 25 min (product-crud)
║   Wave 2: 30 min (multi-tenant-setup)
║   Wave 3: 20 min (integration-tests)
║   Total: 75 minutes
║
║ Parallel Execution (v1.1.0+):
║   Wave 1: 25 min (product-crud)
║   Wave 2: 30 min (multi-tenant-setup)
║   Wave 3: 20 min (integration-tests, parallel with Wave 2 tail)
║   Total: 55 min (wave overlap optimization)
║
║ v1.0.0 Actual (Serial Only):
║   Total: 75 minutes (no parallelization)
╠══════════════════════════════════════════════════════════════════
║ Time Estimation Summary:
║   - Sequential Execution: 75 minutes
║   - Parallel Execution (v1.1.0+): 35 minutes
║   - Time Saved (v1.1.0+): 40 minutes (53% reduction)
║   - v1.0.0 Execution: 75 minutes (serial only)
╚══════════════════════════════════════════════════════════════════
```

### Step 6: Risk Assessment

**Risk Assessment Report**:

```
╔══════════════════════════════════════════════════════════════════
║ Phase 5: Risk Assessment
╠══════════════════════════════════════════════════════════════════
║ Assessing execution risks...
║
║ Plan #1: product-crud
║   Risk Level: MEDIUM (score: 2)
║   Risk Factors:
║     • File conflict with multi-tenant-setup (ProductEntity.java)
║     • No validation errors
║   Recommendation: Execute first to establish baseline
║
║ Plan #2: integration-tests
║   Risk Level: LOW (score: 0)
║   Risk Factors:
║     • No conflicts detected
║     • High skill mapping confidence (1.0)
║   Recommendation: Safe to execute
║
║ Plan #3: multi-tenant-setup
║   Risk Level: HIGH (score: 5)
║   Risk Factors:
║     • File conflict with product-crud (ProductEntity.java)
║     • No skill mapping - manual execution required
║     • High priority flag
║   Recommendation: Execute after product-crud, manual review required
╠══════════════════════════════════════════════════════════════════
║ Risk Summary:
║   - HIGH Risk Plans: 1 (multi-tenant-setup)
║   - MEDIUM Risk Plans: 1 (product-crud)
║   - LOW Risk Plans: 1 (integration-tests)
║
║ Overall Risk: MEDIUM-HIGH
║   - Proceed with caution
║   - Manual intervention required for 1 plan
║   - Conflicts resolved through serialization
╚══════════════════════════════════════════════════════════════════
```

### Step 7: Pre-Execution Report (Complete)

**Complete Dry-run Report**:

```
╔══════════════════════════════════════════════════════════════════
║ Batch Plan Executor - Dry-run Report
║ Mode: SIMULATION (No actual execution)
╠══════════════════════════════════════════════════════════════════
║ Plan Discovery:
║   - Total Plans: 3
║   - Claude Code Plans: 1 (product-crud)
║   - Skills Phase Docs: 1 (integration-tests)
║   - Project Plans: 1 (multi-tenant-setup)
╠══════════════════════════════════════════════════════════════════
║ Skill Mapping Validation:
║   - Successfully Mapped: 2/3 (66.7%)
║   - Requires Manual Execution: 1 (multi-tenant-setup)
║   - Mapping Success Rate: 66.7%
║
║   Plans Requiring Manual Execution:
║     - multi-tenant-setup (module: tenant, no skill mapping)
╠══════════════════════════════════════════════════════════════════
║ Conflict Analysis:
║   - Total Conflicts: 1
║   - File-level Conflicts: 1 (HIGH)
║   - Module-level Conflicts: 0 (MEDIUM)
║   - Dependency Conflicts: 0 (CRITICAL)
║
║   File-level Conflicts:
║     1. smart-admin-api/smartadmin-modules/smartadmin-business/.../ProductEntity.java
║        Conflicting Plans: product-crud, multi-tenant-setup
║        Resolution: Serialize (product-crud → multi-tenant-setup)
╠══════════════════════════════════════════════════════════════════
║ Execution Plan:
║   - Total Execution Waves: 3
║   - Serial Execution Groups: 2
║   - Parallel Execution Groups: 1 (v1.0.0: Sequential)
║
║   Wave 1 (Serial): product-crud (25 min)
║   Wave 2 (Serial): multi-tenant-setup (30 min, manual)
║   Wave 3 (Sequential): integration-tests (20 min)
╠══════════════════════════════════════════════════════════════════
║ Time Estimation:
║   - Sequential Execution: 75 minutes
║   - Parallel Execution (v1.1.0+): 35 minutes
║   - Time Saved (v1.1.0+): 40 minutes (53% reduction)
║   - v1.0.0 Execution: 75 minutes (serial only)
╠══════════════════════════════════════════════════════════════════
║ Risk Assessment:
║   - HIGH Risk Plans: 1 (multi-tenant-setup)
║   - MEDIUM Risk Plans: 1 (product-crud)
║   - LOW Risk Plans: 1 (integration-tests)
║
║   HIGH Risk Plans:
║     - multi-tenant-setup (score: 5)
║         • File conflict (ProductEntity.java)
║         • No skill mapping
╠══════════════════════════════════════════════════════════════════
║ Recommendations:
║   • Execute product-crud first to establish baseline
║   • Manually execute multi-tenant-setup after product-crud
║   • Review ProductEntity.java merge after multi-tenant-setup
║   • Run integration-tests last to verify all changes
║   • Consider adding skill mapping for tenant module
╚══════════════════════════════════════════════════════════════════

DRY-RUN COMPLETE - No actual execution performed.
To execute, run: /batch-execute --mode=interactive [plan-files...]
```

---

## Actual Execution (v1.0.0)

### Command

```bash
# After reviewing dry-run report, execute with interactive mode
/batch-execute --mode=interactive \
  ~/.claude/plans/product-crud.md \
  .claude/skills/smartadmin-testing-suite/phases/phase-2-integration-tests.md \
  docs/plans/tenant/multi-tenant-setup.md
```

### Execution Flow

**Wave 1: product-crud**

```
╔══════════════════════════════════════════════════════════════════
║ Executing Plan 1/3: product-crud
╠══════════════════════════════════════════════════════════════════
║ Plan Type: Claude Code Plan (crud)
║ Skill: smartadmin-crud-generator
║ Estimated Duration: 25 minutes
║ Risk Level: MEDIUM
╠══════════════════════════════════════════════════════════════════
║ Files to Create/Modify:
║   - ProductEntity.java (CREATE)
║   - ProductController.java (CREATE)
║   - ProductService.java (CREATE)
║   - ProductDao.java (CREATE)
║   - ProductMapper.xml (CREATE)
║   - product-api.ts (CREATE)
║   - ProductList.vue (CREATE)
║   - ProductForm.vue (CREATE)
╠══════════════════════════════════════════════════════════════════
║ Invoking skill: smartadmin-crud-generator
║   ⚙ Generating entity...
║   ⚙ Generating DAO layer...
║   ⚙ Generating service layer...
║   ⚙ Generating controller...
║   ⚙ Generating frontend components...
║   ⚙ Running quality gates...
║     ✓ ArchitectureTest: PASSED
║     ✓ Compilation: PASSED
║     ✓ Unit Tests: PASSED
║
║ ✓ Plan completed successfully (Actual: 23 minutes)
╚══════════════════════════════════════════════════════════════════
```

**Wave 2: multi-tenant-setup (Manual)**

```
╔══════════════════════════════════════════════════════════════════
║ Executing Plan 2/3: multi-tenant-setup
╠══════════════════════════════════════════════════════════════════
║ Plan Type: Project Plan (custom)
║ Skill: None (Manual execution required)
║ Estimated Duration: 30 minutes
║ Risk Level: HIGH
╠══════════════════════════════════════════════════════════════════
║ ⚠ WARNING: This plan requires manual execution
║   - No skill mapping for module 'tenant'
║   - File conflict with product-crud (ProductEntity.java)
╠══════════════════════════════════════════════════════════════════
║ Options:
║   [E] Execute manually now (pause batch)
║   [S] Skip this plan
║   [A] Abort batch execution
╚══════════════════════════════════════════════════════════════════
User choice: E

╔══════════════════════════════════════════════════════════════════
║ Manual Execution Mode
╠══════════════════════════════════════════════════════════════════
║ Plan file opened in editor for manual execution.
║ Please complete the following tasks:
║   1. Create database migration script (V1_1__add_tenant_support.sql)
║   2. Implement TenantInterceptor class
║   3. Update ProductEntity with tenantId field
║   4. Update ProductService for tenant context
║
║ Press [Enter] when manual execution is complete...
╚══════════════════════════════════════════════════════════════════

[User completes manual execution - 28 minutes]

╔══════════════════════════════════════════════════════════════════
║ Manual Execution Complete
╠══════════════════════════════════════════════════════════════════
║ Run quality gates? [Y/n]: Y
║
║ Running quality gates...
║   ✓ ArchitectureTest: PASSED
║   ✓ Compilation: PASSED
║   ⚠ Unit Tests: SKIPPED (manual verification)
║
║ ✓ Plan completed successfully (Actual: 28 minutes)
╚══════════════════════════════════════════════════════════════════
```

**Wave 3: integration-tests**

```
╔══════════════════════════════════════════════════════════════════
║ Executing Plan 3/3: integration-tests
╠══════════════════════════════════════════════════════════════════
║ Plan Type: Skills Phase Doc (phase-2)
║ Skill: smartadmin-testing-suite
║ Estimated Duration: 20 minutes
║ Risk Level: LOW
╠══════════════════════════════════════════════════════════════════
║ Files to Create/Modify:
║   - ProductServiceIntegrationTest.java (CREATE)
║   - ProductControllerIntegrationTest.java (CREATE)
╠══════════════════════════════════════════════════════════════════
║ Invoking skill: smartadmin-testing-suite (phase-2)
║   ⚙ Generating integration tests...
║   ⚙ Configuring Testcontainers...
║   ⚙ Running tests...
║     ✓ ProductServiceIntegrationTest: PASSED (5 tests)
║     ✓ ProductControllerIntegrationTest: PASSED (3 tests)
║
║ ✓ Plan completed successfully (Actual: 18 minutes)
╚══════════════════════════════════════════════════════════════════
```

---

## Post-Execution Summary

**Final Report**:

```
╔══════════════════════════════════════════════════════════════════
║ Batch Plan Executor - Post-Execution Summary
╠══════════════════════════════════════════════════════════════════
║ Execution Results:
║   - Total Plans: 3
║   - Successful: 3 (100%)
║   - Failed: 0
║   - Skipped: 0
║   - Manual Execution: 1 (multi-tenant-setup)
╠══════════════════════════════════════════════════════════════════
║ File Changes:
║   - Files Created: 13
║     • ProductEntity.java
║     • ProductController.java
║     • ProductService.java
║     • ProductDao.java
║     • ProductMapper.xml
║     • product-api.ts
║     • ProductList.vue
║     • ProductForm.vue
║     • V1_1__add_tenant_support.sql
║     • TenantInterceptor.java
║     • ProductServiceIntegrationTest.java
║     • ProductControllerIntegrationTest.java
║   - Files Modified: 1
║     • ProductEntity.java (added tenantId field)
║   - Files Deleted: 0
╠══════════════════════════════════════════════════════════════════
║ Quality Gate Results:
║   ✓ ArchitectureTest: PASSED (all plans)
║   ✓ Compilation: PASSED (all plans)
║   ✓ Unit Tests: PASSED (product-crud, integration-tests)
║   ⚠ Unit Tests: SKIPPED (multi-tenant-setup, manual verification)
╠══════════════════════════════════════════════════════════════════
║ Execution Time Breakdown:
║   Wave 1 (product-crud): 23 minutes
║   Wave 2 (multi-tenant-setup): 28 minutes (manual)
║   Wave 3 (integration-tests): 18 minutes
║
║ Total Execution Time: 69 minutes
║   - Planned (sequential): 75 minutes
║   - Actual: 69 minutes
║   - Efficiency: 92% (under estimate)
╠══════════════════════════════════════════════════════════════════
║ Recommendations:
║   ✓ All plans executed successfully
║   • Consider adding skill mapping for tenant module
║   • Run full regression test suite to verify tenant isolation
║   • Document manual steps for multi-tenant-setup
╚══════════════════════════════════════════════════════════════════

Batch execution completed successfully!
Log saved to: .claude/metrics/batch-executions/batch-exec-20260129-143500.log
```

---

## Key Learnings

### 1. Plan Type Identification

**Lesson**: 三種方案類型的識別基於文件路徑優先級。

- **Claude Code Plans**: YAML frontmatter + 路徑 `~/.claude/plans/`
- **Skills Phase Docs**: 文件名格式 `phase-N-*.md` + 路徑 `.claude/skills/*/phases/`
- **Project Plans**: YAML frontmatter + 路徑 `docs/plans/{module}/`

### 2. Conflict Detection Accuracy

**Lesson**: 文件衝突檢測依賴於方案中明確記錄的文件路徑。

**最佳實踐**:
- 使用代碼塊文件註釋: `// File: path/to/file.java`
- 添加 "Files to Modify" 部分
- 使用反引號引用文件: `` `ProductEntity.java` ``

### 3. Skill Mapping Challenges

**Lesson**: 自定義模組（如 tenant）可能無 Skill 映射。

**解決方案**:
- 在 YAML frontmatter 中明確指定 `skill:`
- 配置自定義映射規則
- 接受手動執行模式

### 4. Manual Execution Workflow

**Lesson**: 手動執行需要人工干預，但仍可納入批量流程。

**流程**:
1. Batch executor 暫停並打開方案文件
2. 用戶手動完成實施
3. 用戶確認完成
4. 可選: 運行質量門檢查
5. Batch executor 繼續下一個方案

### 5. Execution Order Matters

**Lesson**: 文件衝突會自動調整執行順序。

**示例**:
- `product-crud` 必須在 `multi-tenant-setup` 之前執行
- 因為 `multi-tenant-setup` 修改 `product-crud` 創建的 `ProductEntity.java`

---

## Comparison: v1.0.0 vs v1.1.0+

| Aspect | v1.0.0 (Serial) | v1.1.0+ (Parallel) |
|--------|-----------------|---------------------|
| Execution Mode | Sequential | Parallel (max 5) |
| Wave 1 | product-crud (25 min) | product-crud (25 min) |
| Wave 2 | multi-tenant-setup (30 min) | multi-tenant-setup (30 min) |
| Wave 3 | integration-tests (20 min) | integration-tests (parallel with Wave 2) |
| **Total Time** | **75 minutes** | **35 minutes** |
| **Time Saved** | **-** | **40 minutes (53%)** |

---

## Related Examples

- **[CRUD Batch Example](EXAMPLE-crud-batch.md)** - 批量 CRUD 生成（5 個模組）
- **[LiteFlow Migration](EXAMPLE-liteflow-migration.md)** - LiteFlow 8 階段遷移
- **[High-Risk Batch](EXAMPLE-high-risk-batch.md)** - 高風險方案批量執行

---

## Summary

此示例展示了 Batch Plan Executor 如何處理混合類型方案執行的完整流程：

1. ✅ **Plan Discovery**: 自動識別三種方案類型
2. ✅ **Skill Mapping**: 自動映射 2/3 方案，1 個需要手動執行
3. ✅ **Conflict Detection**: 檢測到 1 個文件級別衝突（ProductEntity.java）
4. ✅ **Execution Planning**: 生成 3 波次執行計劃，自動解決衝突
5. ✅ **Risk Assessment**: 識別 1 個高風險方案（multi-tenant-setup）
6. ✅ **Execution**: 串行執行 3 個方案（v1.0.0），69 分鐘完成

**關鍵收穫**:
- 混合類型方案可以批量執行
- 文件衝突自動檢測並通過串行化解決
- 手動執行方案可以無縫集成到批量流程
- Dry-run 模式是執行前風險評估的關鍵

---

**Last Updated**: 2026-01-29
**Example Version**: v1.0.0
**Status**: ✅ Complete
