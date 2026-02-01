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

