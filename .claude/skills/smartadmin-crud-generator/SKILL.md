---
name: smartadmin-crud-generator
description: Generate complete full-stack CRUD module for SmartAdmin (Backend Entity/Dao/Manager/Service/Controller + Frontend Vue components + API client + Tests + Documentation). Use when creating new business modules, implementing CRUD features, or scaffolding complete modules. Triggers when (1) User requests "create/generate CRUD module/feature", (2) User mentions creating new business entity with CRUD operations, (3) User wants to scaffold a complete module (frontend + backend), (4) After defining entity requirements but before implementation, (5) User explicitly requests full-stack code generation.
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

## Generation Workflow

### Step 1: Gather Requirements

Ask user for entity specification:

**Required Information:**
- Entity name (e.g., "Product", "Order", "Customer")
- Fields with types and validations
- Foreign key relationships
- Unique constraints
- Business rules

**Example Dialogue:**
```
You: "Let me gather the requirements for the Product module.

**Entity name:** Product

**Fields needed:**
1. What fields should Product have?
2. Which fields are required vs optional?
3. Any foreign keys (e.g., categoryId)?
4. Any unique constraints (e.g., productCode)?
5. Should it support soft delete?
6. Any special validations?"
```

**Capture in structured format:**
```yaml
Entity: Product
Fields:
  - productName: String (required, max 100 chars, unique)
  - productCode: String (required, max 50 chars, unique)
  - price: BigDecimal (required, min 0)
  - stock: Integer (required, min 0)
  - categoryId: Long (required, FK to Category)
  - description: String (optional, max 500 chars)
  - status: Integer (required, enum: 1=Available, 2=OutOfStock, 3=Discontinued)
  - deletedFlag: Boolean (soft delete)

Relationships:
  - belongsTo: Category (via categoryId)

Unique Constraints:
  - productName
  - productCode

Business Rules:
  - Price must be positive
  - Stock cannot be negative
  - Cannot delete if stock > 0
```

### Step 2: Generate Backend Layers

Generate in order (bottom-up):

**CRITICAL: Package Imports (v4.0.0+)**

Always use the correct package paths per SmartAdmin v4.0.0:

```java
// Foundation domain objects
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import net.lab1024.sa.foundation.domain.response.PageResult;
import net.lab1024.sa.foundation.domain.request.PageParam;

// Utilities (documented exceptions to foundation.* naming)
import net.lab1024.sa.util.SmartBeanUtil;              // NOT common.core.util!
import net.lab1024.sa.base.mybatis.util.SmartPageUtil; // NOT common.core.util!
```

**Never use deprecated packages:**
- ❌ `net.lab1024.sa.common.core.util.*` (removed in v4.0.0)
- ❌ `net.lab1024.sa.common.core.domain.*` (removed in v4.0.0)

**QueryForm extends PageParam - Add @EqualsAndHashCode:**

```java
@Data
@EqualsAndHashCode(callSuper = false)  // Required when extending PageParam
public class ProductQueryForm extends PageParam {
    // fields...
}
```

#### 2.1 Entity

```java
@Data
@TableName("t_product")
public class ProductEntity {
    @TableId(type = IdType.AUTO)
    private Long productId;

    private String productName;
    private String productCode;
    private BigDecimal price;
    private Integer stock;
    private Long categoryId;
    private String description;
    private Integer status;
    private Boolean deletedFlag;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

**File:** `sa-admin/src/main/java/.../product/domain/entity/ProductEntity.java`

#### 2.2 Forms (Add/Update/Query)

```java
@Data
public class ProductAddForm {
    @NotNull(message = "商品名称不能为空")
    @Length(max = 100, message = "商品名称最多100字符")
    private String productName;

    @NotNull(message = "商品编码不能为空")
    @Length(max = 50, message = "商品编码最多50字符")
    private String productCode;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0", message = "价格不能为负")
    private BigDecimal price;

    // ... other fields
}

@Data
public class ProductUpdateForm {
    @NotNull(message = "商品ID不能为空")
    private Long productId;
    // ... same fields as Add
}

@Data
@EqualsAndHashCode(callSuper = true)
public class ProductQueryForm extends PageParam {
    private String keyword;  // Search productName or productCode
    private Long categoryId;
    private Integer status;
    private Boolean deletedFlag;
}
```

**Files:**
- `ProductAddForm.java`
- `ProductUpdateForm.java`
- `ProductQueryForm.java`

#### 2.3 VO (Value Object)

```java
@Data
public class ProductVO {
    private Long productId;
    private String productName;
    private String productCode;
    private BigDecimal price;
    private Integer stock;
    private Long categoryId;
    private String categoryName;  // Joined from Category
    private String description;
    private Integer status;
    private String statusName;  // Enum display name
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

#### 2.4 Dao (MyBatis Mapper)

```java
@Mapper
public interface ProductDao extends BaseMapper<ProductEntity> {

    /**
     * Query products with pagination
     */
    List<ProductVO> queryProduct(
        @Param("page") Page page,
        @Param("queryForm") ProductQueryForm queryForm
    );

    /**
     * Get by product name
     */
    ProductEntity getByProductName(
        @Param("productName") String productName,
        @Param("excludeId") Long excludeId
    );

    /**
     * Get by product code
     */
    ProductEntity getByProductCode(
        @Param("productCode") String productCode,
        @Param("excludeId") Long excludeId
    );
}
```

**XML Mapper:**
```xml
<mapper namespace="...ProductDao">
    <select id="queryProduct" resultType="...ProductVO">
        SELECT
            p.product_id,
            p.product_name,
            p.product_code,
            p.price,
            p.stock,
            p.category_id,
            c.category_name,
            p.description,
            p.status,
            p.create_time,
            p.update_time
        FROM t_product p
        LEFT JOIN t_category c ON p.category_id = c.category_id
        <where>
            <if test="queryForm.deletedFlag != null">
                AND p.deleted_flag = #{queryForm.deletedFlag}
            </if>
            <if test="queryForm.keyword != null and queryForm.keyword != ''">
                AND (p.product_name LIKE CONCAT('%', #{queryForm.keyword}, '%')
                     OR p.product_code LIKE CONCAT('%', #{queryForm.keyword}, '%'))
            </if>
            <if test="queryForm.categoryId != null">
                AND p.category_id = #{queryForm.categoryId}
            </if>
            <if test="queryForm.status != null">
                AND p.status = #{queryForm.status}
            </if>
        </where>
        ORDER BY p.create_time DESC
    </select>
</mapper>
```

#### 2.5 Manager

```java
@Service
public class ProductManager {

    @Resource
    private ProductDao productDao;

    @Transactional(rollbackFor = Exception.class)
    public void saveProduct(ProductEntity product) {
        productDao.insert(product);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(ProductEntity product) {
        productDao.updateById(product);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long productId) {
        ProductEntity product = new ProductEntity();
        product.setProductId(productId);
        product.setDeletedFlag(true);
        productDao.updateById(product);
    }
}
```

#### 2.6 Service

```java
@Service
public class ProductService {

    @Resource private ProductDao productDao;
    @Resource private ProductManager productManager;
    @Resource private CategoryDao categoryDao;

    public ResponseDTO<PageResult<ProductVO>> queryProduct(ProductQueryForm queryForm) {
        queryForm.setDeletedFlag(false);
        Page pageParam = SmartPageUtil.convert2PageQuery(queryForm);
        List<ProductVO> productList = productDao.queryProduct(pageParam, queryForm);
        PageResult<ProductVO> pageResult = SmartPageUtil.convert2PageResult(pageParam, productList);
        return ResponseDTO.ok(pageResult);
    }

    public ResponseDTO<String> addProduct(ProductAddForm form) {
        // Validate unique productName
        ProductEntity existing = productDao.getByProductName(form.getProductName(), null);
        if (existing != null) {
            return ResponseDTO.userErrorParam("商品名称已存在");
        }

        // Validate unique productCode
        existing = productDao.getByProductCode(form.getProductCode(), null);
        if (existing != null) {
            return ResponseDTO.userErrorParam("商品编码已存在");
        }

        // Validate category exists
        CategoryEntity category = categoryDao.selectById(form.getCategoryId());
        if (category == null || category.getDeletedFlag()) {
            return ResponseDTO.userErrorParam("分类不存在");
        }

        // Save
        ProductEntity entity = SmartBeanUtil.copy(form, ProductEntity.class);
        entity.setDeletedFlag(false);
        productManager.saveProduct(entity);

        return ResponseDTO.ok();
    }

    public ResponseDTO<String> updateProduct(ProductUpdateForm form) {
        // Check exists
        ProductEntity product = productDao.selectById(form.getProductId());
        if (product == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }

        // Validate unique productName
        ProductEntity existing = productDao.getByProductName(
            form.getProductName(),
            form.getProductId()
        );
        if (existing != null) {
            return ResponseDTO.userErrorParam("商品名称已存在");
        }

        // Validate unique productCode
        existing = productDao.getByProductCode(
            form.getProductCode(),
            form.getProductId()
        );
        if (existing != null) {
            return ResponseDTO.userErrorParam("商品编码已存在");
        }

        // Validate category
        CategoryEntity category = categoryDao.selectById(form.getCategoryId());
        if (category == null || category.getDeletedFlag()) {
            return ResponseDTO.userErrorParam("分类不存在");
        }

        // Update
        ProductEntity entity = SmartBeanUtil.copy(form, ProductEntity.class);
        productManager.updateProduct(entity);

        return ResponseDTO.ok();
    }

    public ResponseDTO<String> deleteProduct(Long productId) {
        ProductEntity product = productDao.selectById(productId);
        if (product == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }

        // Business rule: cannot delete if stock > 0
        if (product.getStock() > 0) {
            return ResponseDTO.userErrorParam("库存不为0，无法删除");
        }

        productManager.deleteProduct(productId);
        return ResponseDTO.ok();
    }
}
```

#### 2.7 Controller

```java
@RestController
@Tag(name = AdminSwaggerTagConst.Business.PRODUCT)
public class ProductController {

    @Resource private ProductService productService;

    @Operation(summary = "查询商品列表")
    @PostMapping("/product/query")
    public ResponseDTO<PageResult<ProductVO>> queryProduct(
        @RequestBody @Valid ProductQueryForm queryForm
    ) {
        return productService.queryProduct(queryForm);
    }

    @Operation(summary = "添加商品")
    @PostMapping("/product/add")
    public ResponseDTO<String> addProduct(@RequestBody @Valid ProductAddForm form) {
        return productService.addProduct(form);
    }

    @Operation(summary = "更新商品")
    @PostMapping("/product/update")
    public ResponseDTO<String> updateProduct(@RequestBody @Valid ProductUpdateForm form) {
        return productService.updateProduct(form);
    }

    @Operation(summary = "删除商品")
    @GetMapping("/product/delete/{productId}")
    public ResponseDTO<String> deleteProduct(@PathVariable Long productId) {
        return productService.deleteProduct(productId);
    }
}
```

### Step 3: Generate Frontend Components

#### 3.1 API Client

```typescript
// product-api.ts
import { getRequest, postRequest } from '@/lib/axios';
import type { PageResult, ResponseDTO } from '@/types/common';
import type { ProductVO, ProductAddForm, ProductUpdateForm, ProductQueryForm } from './types';

export const productApi = {
  /**
   * Query products with pagination
   */
  query: (queryForm: ProductQueryForm): Promise<ResponseDTO<PageResult<ProductVO>>> => {
    return postRequest('/product/query', queryForm);
  },

  /**
   * Add product
   */
  add: (form: ProductAddForm): Promise<ResponseDTO<string>> => {
    return postRequest('/product/add', form);
  },

  /**
   * Update product
   */
  update: (form: ProductUpdateForm): Promise<ResponseDTO<string>> => {
    return postRequest('/product/update', form);
  },

  /**
   * Delete product
   */
  delete: (productId: number): Promise<ResponseDTO<string>> => {
    return getRequest(`/product/delete/${productId}`);
  },
};
```

#### 3.2 TypeScript Types

```typescript
// types.ts
export interface ProductVO {
  productId: number;
  productName: string;
  productCode: string;
  price: number;
  stock: number;
  categoryId: number;
  categoryName: string;
  description: string;
  status: number;
  statusName: string;
  createTime: string;
  updateTime: string;
}

export interface ProductAddForm {
  productName: string;
  productCode: string;
  price: number;
  stock: number;
  categoryId: number;
  description?: string;
  status: number;
}

export interface ProductUpdateForm extends ProductAddForm {
  productId: number;
}

export interface ProductQueryForm {
  pageNum: number;
  pageSize: number;
  keyword?: string;
  categoryId?: number;
  status?: number;
}
```

#### 3.3 List Component

```vue
<!-- product-list.vue -->
<template>
  <div class="product-container">
    <!-- Search Form -->
    <a-card :bordered="false" size="small">
      <a-form layout="inline">
        <a-form-item label="搜索">
          <a-input
            v-model:value="queryForm.keyword"
            placeholder="商品名称/编码"
            style="width: 200px"
          />
        </a-form-item>
        <a-form-item label="分类">
          <CategorySelect
            v-model:value="queryForm.categoryId"
            placeholder="请选择分类"
            style="width: 200px"
          />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleQuery">查询</a-button>
            <a-button @click="handleReset">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- Action Bar -->
    <a-card :bordered="false" size="small" class="mt-2">
      <a-button type="primary" @click="handleAdd">
        <template #icon><PlusOutlined /></template>
        添加商品
      </a-button>
    </a-card>

    <!-- Table -->
    <a-card :bordered="false" size="small" class="mt-2">
      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="tableLoading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="productId"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'price'">
            ¥{{ record.price.toFixed(2) }}
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ record.statusName }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleEdit(record)">
                编辑
              </a-button>
              <a-button type="link" size="small" danger @click="handleDelete(record)">
                删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- Form Modal -->
    <ProductFormModal
      v-model:visible="formModalVisible"
      :form-data="formData"
      @success="handleFormSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import { productApi } from './product-api';
import ProductFormModal from './product-form-modal.vue';
import type { ProductVO, ProductQueryForm } from './types';

const queryForm = reactive<ProductQueryForm>({
  pageNum: 1,
  pageSize: 10,
});

const tableData = ref<ProductVO[]>([]);
const tableLoading = ref(false);
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
});

const columns = [
  { title: '商品编码', dataIndex: 'productCode', key: 'productCode' },
  { title: '商品名称', dataIndex: 'productName', key: 'productName' },
  { title: '价格', dataIndex: 'price', key: 'price' },
  { title: '库存', dataIndex: 'stock', key: 'stock' },
  { title: '分类', dataIndex: 'categoryName', key: 'categoryName' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime' },
  { title: '操作', key: 'action', width: 150 },
];

const formModalVisible = ref(false);
const formData = ref(null);

function getStatusColor(status: number) {
  const colors = { 1: 'success', 2: 'warning', 3: 'error' };
  return colors[status] || 'default';
}

async function loadTableData() {
  tableLoading.value = true;
  try {
    const res = await productApi.query(queryForm);
    if (res.ok) {
      tableData.value = res.data.list;
      pagination.total = res.data.total;
    }
  } finally {
    tableLoading.value = false;
  }
}

function handleQuery() {
  queryForm.pageNum = 1;
  loadTableData();
}

function handleReset() {
  Object.assign(queryForm, { pageNum: 1, pageSize: 10, keyword: '', categoryId: null });
  loadTableData();
}

function handleAdd() {
  formData.value = null;
  formModalVisible.value = true;
}

function handleEdit(record: ProductVO) {
  formData.value = { ...record };
  formModalVisible.value = true;
}

async function handleDelete(record: ProductVO) {
  const res = await productApi.delete(record.productId);
  if (res.ok) {
    message.success('删除成功');
    loadTableData();
  }
}

function handleTableChange(pag: any) {
  queryForm.pageNum = pag.current;
  queryForm.pageSize = pag.pageSize;
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;
  loadTableData();
}

function handleFormSuccess() {
  formModalVisible.value = false;
  loadTableData();
}

onMounted(() => {
  loadTableData();
});
</script>
```

#### 3.4 Form Modal

```vue
<!-- product-form-modal.vue -->
<template>
  <a-modal
    :visible="visible"
    :title="formData?.productId ? '编辑商品' : '添加商品'"
    :confirm-loading="confirmLoading"
    @ok="handleSubmit"
    @cancel="handleCancel"
    width="600px"
  >
    <a-form :model="form" :rules="rules" ref="formRef" :label-col="{ span: 6 }">
      <a-form-item label="商品名称" name="productName">
        <a-input v-model:value="form.productName" placeholder="请输入商品名称" />
      </a-form-item>

      <a-form-item label="商品编码" name="productCode">
        <a-input v-model:value="form.productCode" placeholder="请输入商品编码" />
      </a-form-item>

      <a-form-item label="价格" name="price">
        <a-input-number
          v-model:value="form.price"
          :min="0"
          :precision="2"
          placeholder="请输入价格"
          style="width: 100%"
        />
      </a-form-item>

      <a-form-item label="库存" name="stock">
        <a-input-number
          v-model:value="form.stock"
          :min="0"
          placeholder="请输入库存"
          style="width: 100%"
        />
      </a-form-item>

      <a-form-item label="分类" name="categoryId">
        <CategorySelect
          v-model:value="form.categoryId"
          placeholder="请选择分类"
        />
      </a-form-item>

      <a-form-item label="描述" name="description">
        <a-textarea
          v-model:value="form.description"
          placeholder="请输入描述"
          :rows="3"
        />
      </a-form-item>

      <a-form-item label="状态" name="status">
        <a-radio-group v-model:value="form.status">
          <a-radio :value="1">可用</a-radio>
          <a-radio :value="2">缺货</a-radio>
          <a-radio :value="3">已下架</a-radio>
        </a-radio-group>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue';
import { message } from 'ant-design-vue';
import { productApi } from './product-api';
import type { ProductAddForm, ProductUpdateForm } from './types';

const props = defineProps<{
  visible: boolean;
  formData: any;
}>();

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void;
  (e: 'success'): void;
}>();

const formRef = ref();
const confirmLoading = ref(false);

const form = reactive<ProductAddForm>({
  productName: '',
  productCode: '',
  price: 0,
  stock: 0,
  categoryId: null as any,
  description: '',
  status: 1,
});

const rules = {
  productName: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  productCode: [{ required: true, message: '请输入商品编码', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
};

watch(
  () => props.visible,
  (val) => {
    if (val && props.formData) {
      Object.assign(form, props.formData);
    } else if (val) {
      Object.assign(form, {
        productName: '',
        productCode: '',
        price: 0,
        stock: 0,
        categoryId: null,
        description: '',
        status: 1,
      });
    }
  }
);

async function handleSubmit() {
  try {
    await formRef.value.validate();
    confirmLoading.value = true;

    const isUpdate = props.formData?.productId;
    const res = isUpdate
      ? await productApi.update(form as ProductUpdateForm)
      : await productApi.add(form);

    if (res.ok) {
      message.success(isUpdate ? '更新成功' : '添加成功');
      emit('success');
    }
  } finally {
    confirmLoading.value = false;
  }
}

function handleCancel() {
  emit('update:visible', false);
}
</script>
```

### Step 4: Generate Tests

Use the **smartadmin-integration-test** skill to generate:
- Service integration tests
- Manager integration tests
- Controller integration tests (with MockMvc)

### Step 5: Generate Documentation

Add Swagger annotations (already included in Controller above).

### Step 6: Validate

Run ArchitectureTest to ensure layering rules are followed:

```bash
./gradlew :sa-admin:test --tests ArchitectureTest
```

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
- [ ] **Package imports use v4.0.0 paths** (`net.lab1024.sa.util.*`, `net.lab1024.sa.base.mybatis.util.*`, `net.lab1024.sa.foundation.domain.*`)
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
sa-admin/src/main/java/net/lab1024/sa/admin/module/business/{module}/
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
sa-admin/src/test/java/net/lab1024/sa/admin/module/business/{module}/
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
- [ ] Tests pass: `./gradlew :sa-admin:test --tests {Entity}IntegrationTest`

---

## Troubleshooting

### Common Issues by Phase

**Phase 1 Issues**:

**Problem**: "SmartBeanUtil not found"
- **Cause**: Wrong import path (using deprecated `common.core.util.*`)
- **Solution**: Use `import net.lab1024.sa.util.SmartBeanUtil;`

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
