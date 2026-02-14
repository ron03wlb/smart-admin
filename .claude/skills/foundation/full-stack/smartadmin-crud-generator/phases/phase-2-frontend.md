# Phase 2: Frontend Component Generation

**Purpose**: Generate Vue 3 + Ant Design CRUD components with TypeScript types and API integration.

**Integration**: This phase consolidates logic from `smartadmin-vue-crud` skill.

---

## Execution Order

1. **TypeScript Types** (matching backend VOs/Forms)
2. **API Client** (typed Axios with ResponseDTO handling)
3. **List Component** (a-table with search, pagination)
4. **Form Modal** (a-modal with validation)
5. **Route Configuration**

---

## 1. Generate TypeScript Types

**File**: `smart-admin-web/src/api/{module}/{entity}-types.ts`

**Pattern** (from smartadmin-vue-crud):
```typescript
/**
 * {EntityName} Types
 *
 * @author {author}
 * @date {date}
 */

// Query Form (matches backend QueryForm)
export interface {Entity}QueryForm {
  keyword?: string;
  // Generated fields matching backend QueryForm
  pageNum: number;
  pageSize: number;
}

// VO (matches backend VO)
export interface {Entity}VO {
  {entity}Id: number;
  // Generated fields matching backend VO
  createTime: string;
  updateTime: string;
}

// Add Form (matches backend AddForm)
export interface {Entity}AddForm {
  // Generated fields matching backend AddForm (excluding ID)
}

// Update Form (matches backend UpdateForm)
export interface {Entity}UpdateForm {
  {entity}Id: number;
  // Same fields as AddForm
}

// Batch Delete Form
export interface {Entity}BatchDeleteForm {
  {entity}IdList: number[];
}
```

**Type Mapping** (Java ↔ TypeScript):
| Java Type | TypeScript Type |
|-----------|-----------------|
| Long, Integer | number |
| String | string |
| Boolean | boolean |
| LocalDateTime | string |
| BigDecimal | number |
| List<T> | T[] |
| Enum | string \| number |

---

## 2. Generate API Client

**File**: `smart-admin-web/src/api/{module}/{entity}-api.ts`

**Pattern** (from smartadmin-vue-crud):
```typescript
import { postRequest, getRequest } from '@/lib/axios';
import type {
  {Entity}QueryForm,
  {Entity}VO,
  {Entity}AddForm,
  {Entity}UpdateForm,
  {Entity}BatchDeleteForm,
} from './{entity}-types';

const BASE_URL = '/{module}/{entity}';

/**
 * {EntityName} API
 */
export const {entity}Api = {
  /**
   * Query with pagination
   */
  query: (queryForm: {Entity}QueryForm) => {
    return postRequest<PageResult<{Entity}VO>>(`${BASE_URL}/query`, queryForm);
  },

  /**
   * Get by ID
   */
  getById: ({entity}Id: number) => {
    return getRequest<{Entity}VO>(`${BASE_URL}/get/${{{entity}Id}}`);
  },

  /**
   * Add {entity}
   */
  add: (addForm: {Entity}AddForm) => {
    return postRequest<void>(`${BASE_URL}/add`, addForm);
  },

  /**
   * Update {entity}
   */
  update: (updateForm: {Entity}UpdateForm) => {
    return postRequest<void>(`${BASE_URL}/update`, updateForm);
  },

  /**
   * Delete {entity}
   */
  delete: ({entity}Id: number) => {
    return postRequest<void>(`${BASE_URL}/delete/${{{entity}Id}}`);
  },

  /**
   * Batch delete {entity}s
   */
  batchDelete: (batchDeleteForm: {Entity}BatchDeleteForm) => {
    return postRequest<void>(`${BASE_URL}/batchDelete`, batchDeleteForm);
  },
};
```

**Key Features**:
- ✅ Uses SmartAdmin's `postRequest`/`getRequest` wrappers
- ✅ Automatic ResponseDTO unwrapping
- ✅ TypeScript generics for type safety
- ✅ Consistent error handling

---

## 3. Generate List Component

**File**: `smart-admin-web/src/views/{module}/{entity}/{entity}-list.vue`

**Pattern** (from smartadmin-vue-crud):
```vue
<template>
  <div class="{entity}-list">
    <a-card title="{Entity} Management" :bordered="false">
      <!-- Search Form -->
      <a-form layout="inline" :model="queryForm" class="search-form">
        <a-form-item label="Keyword">
          <a-input
            v-model:value="queryForm.keyword"
            placeholder="Search {entity}"
            @pressEnter="handleQuery"
          />
        </a-form-item>
        <!-- Generated search fields -->
        <a-form-item>
          <a-button type="primary" @click="handleQuery">
            <SearchOutlined /> Search
          </a-button>
          <a-button @click="handleReset" style="margin-left: 8px">
            Reset
          </a-button>
        </a-form-item>
      </a-form>

      <!-- Action Buttons -->
      <div class="table-actions">
        <a-button
          type="primary"
          @click="handleAdd"
          v-privilege="'{module}:{entity}:add'"
        >
          <PlusOutlined /> Add {Entity}
        </a-button>
        <a-button
          danger
          :disabled="selectedRowKeys.length === 0"
          @click="handleBatchDelete"
          v-privilege="'{module}:{entity}:delete'"
        >
          <DeleteOutlined /> Batch Delete
        </a-button>
      </div>

      <!-- Data Table -->
      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="tableLoading"
        :pagination="pagination"
        :row-selection="{
          selectedRowKeys: selectedRowKeys,
          onChange: onSelectChange,
        }"
        @change="handleTableChange"
        row-key="{entity}Id"
      >
        <template #bodyCell="{ column, record }">
          <!-- Generated custom columns -->
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button
                type="link"
                size="small"
                @click="handleEdit(record)"
                v-privilege="'{module}:{entity}:update'"
              >
                <EditOutlined /> Edit
              </a-button>
              <a-button
                type="link"
                size="small"
                danger
                @click="handleDelete(record)"
                v-privilege="'{module}:{entity}:delete'"
              >
                <DeleteOutlined /> Delete
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- Form Modal -->
    <{Entity}FormModal
      v-model:visible="formModalVisible"
      :form-data="formData"
      :mode="formMode"
      @success="handleFormSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue';
import { message, Modal } from 'ant-design-vue';
import {
  SearchOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
} from '@ant-design/icons-vue';
import { {entity}Api } from '@/api/{module}/{entity}-api';
import type { {Entity}VO, {Entity}QueryForm } from '@/api/{module}/{entity}-types';
import {Entity}FormModal from './{entity}-form-modal.vue';

// Query Form
const queryForm = reactive<{Entity}QueryForm>({
  keyword: '',
  pageNum: 1,
  pageSize: 10,
});

// Table Data
const tableData = ref<{Entity}VO[]>([]);
const tableLoading = ref(false);
const selectedRowKeys = ref<number[]>([]);

// Pagination
const pagination = computed(() => ({
  current: queryForm.pageNum,
  pageSize: queryForm.pageSize,
  total: totalRecords.value,
  showSizeChanger: true,
  showTotal: (total: number) => `Total ${total} records`,
}));
const totalRecords = ref(0);

// Table Columns
const columns = [
  {
    title: 'ID',
    dataIndex: '{entity}Id',
    key: '{entity}Id',
    width: 80,
  },
  // Generated columns based on entity fields
  {
    title: 'Create Time',
    dataIndex: 'createTime',
    key: 'createTime',
    width: 180,
  },
  {
    title: 'Actions',
    key: 'action',
    width: 150,
    fixed: 'right',
  },
];

// Form Modal
const formModalVisible = ref(false);
const formMode = ref<'add' | 'edit'>('add');
const formData = ref<{Entity}VO | null>(null);

// Query Handler
const handleQuery = async () => {
  tableLoading.value = true;
  try {
    const res = await {entity}Api.query(queryForm);
    tableData.value = res.data.list;
    totalRecords.value = res.data.total;
  } catch (error) {
    message.error('Query failed');
  } finally {
    tableLoading.value = false;
  }
};

// Reset Handler
const handleReset = () => {
  queryForm.keyword = '';
  queryForm.pageNum = 1;
  handleQuery();
};

// Table Change Handler
const handleTableChange = (pag: any) => {
  queryForm.pageNum = pag.current;
  queryForm.pageSize = pag.pageSize;
  handleQuery();
};

// Row Selection
const onSelectChange = (keys: number[]) => {
  selectedRowKeys.value = keys;
};

// Add Handler
const handleAdd = () => {
  formMode.value = 'add';
  formData.value = null;
  formModalVisible.value = true;
};

// Edit Handler
const handleEdit = (record: {Entity}VO) => {
  formMode.value = 'edit';
  formData.value = { ...record };
  formModalVisible.value = true;
};

// Delete Handler
const handleDelete = (record: {Entity}VO) => {
  Modal.confirm({
    title: 'Confirm Delete',
    content: `Are you sure you want to delete this {entity}?`,
    onOk: async () => {
      try {
        await {entity}Api.delete(record.{entity}Id);
        message.success('Deleted successfully');
        handleQuery();
      } catch (error) {
        message.error('Delete failed');
      }
    },
  });
};

// Batch Delete Handler
const handleBatchDelete = () => {
  Modal.confirm({
    title: 'Confirm Batch Delete',
    content: `Are you sure you want to delete ${selectedRowKeys.value.length} {entity}s?`,
    onOk: async () => {
      try {
        await {entity}Api.batchDelete({
          {entity}IdList: selectedRowKeys.value,
        });
        message.success('Batch deleted successfully');
        selectedRowKeys.value = [];
        handleQuery();
      } catch (error) {
        message.error('Batch delete failed');
      }
    },
  });
};

// Form Success Handler
const handleFormSuccess = () => {
  formModalVisible.value = false;
  handleQuery();
};

// Initialize
onMounted(() => {
  handleQuery();
});
</script>

<style scoped lang="less">
.{entity}-list {
  .search-form {
    margin-bottom: 16px;
  }

  .table-actions {
    margin-bottom: 16px;

    button + button {
      margin-left: 8px;
    }
  }
}
</style>
```

---

## 4. Generate Form Modal

**File**: `smart-admin-web/src/views/{module}/{entity}/{entity}-form-modal.vue`

**Pattern**:
```vue
<template>
  <a-modal
    :visible="visible"
    :title="mode === 'add' ? 'Add {Entity}' : 'Edit {Entity}'"
    :confirm-loading="submitLoading"
    @ok="handleSubmit"
    @cancel="handleCancel"
    width="600px"
  >
    <a-form
      ref="formRef"
      :model="form"
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 16 }"
    >
      <!-- Generated form fields based on AddForm/UpdateForm -->
      <a-form-item
        label="{Field}"
        name="{field}"
        :rules="[{ required: true, message: 'Please input {field}' }]"
      >
        <a-input v-model:value="form.{field}" placeholder="{Field}" />
      </a-form-item>
      <!-- More generated fields -->
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue';
import { message } from 'ant-design-vue';
import type { FormInstance } from 'ant-design-vue';
import { {entity}Api } from '@/api/{module}/{entity}-api';
import type {
  {Entity}AddForm,
  {Entity}UpdateForm,
  {Entity}VO,
} from '@/api/{module}/{entity}-types';

// Props
interface Props {
  visible: boolean;
  formData: {Entity}VO | null;
  mode: 'add' | 'edit';
}

const props = defineProps<Props>();

// Emits
const emit = defineEmits<{
  (e: 'update:visible', visible: boolean): void;
  (e: 'success'): void;
}>();

// Form
const formRef = ref<FormInstance>();
const form = reactive<{Entity}AddForm | {Entity}UpdateForm>({
  // Generated default values
});
const submitLoading = ref(false);

// Watch formData changes
watch(
  () => props.formData,
  (data) => {
    if (data && props.mode === 'edit') {
      Object.assign(form, data);
    } else {
      formRef.value?.resetFields();
    }
  },
  { immediate: true }
);

// Submit Handler
const handleSubmit = async () => {
  try {
    await formRef.value?.validate();
    submitLoading.value = true;

    if (props.mode === 'add') {
      await {entity}Api.add(form as {Entity}AddForm);
      message.success('Added successfully');
    } else {
      await {entity}Api.update(form as {Entity}UpdateForm);
      message.success('Updated successfully');
    }

    emit('success');
  } catch (error) {
    if (error instanceof Error) {
      message.error(error.message || 'Submit failed');
    }
  } finally {
    submitLoading.value = false;
  }
};

// Cancel Handler
const handleCancel = () => {
  formRef.value?.resetFields();
  emit('update:visible', false);
};
</script>
```

---

## 5. Generate Route Configuration

**File**: `smart-admin-web/src/router/modules/{module}.ts`

**Add route**:
```typescript
{
  path: '/{entity}',
  name: '{Entity}',
  component: () => import('@/views/{module}/{entity}/{entity}-list.vue'),
  meta: {
    title: '{Entity} Management',
    permission: '{module}:{entity}:query',
  },
}
```

---

## File Structure

```
smart-admin-web/src/
├── api/
│   └── {module}/
│       ├── {entity}-types.ts         (TypeScript types)
│       └── {entity}-api.ts           (API client)
└── views/
    └── {module}/
        └── {entity}/
            ├── {entity}-list.vue      (List component)
            └── {entity}-form-modal.vue (Form modal)
```

---

## Component Features

### List Component
- ✅ a-table with pagination
- ✅ Search form with keyword + custom filters
- ✅ Action buttons (Add, Batch Delete)
- ✅ Row selection for batch operations
- ✅ Permission-controlled buttons (`v-privilege`)
- ✅ Ant Design icons
- ✅ Responsive layout

### Form Modal
- ✅ a-modal with validation
- ✅ Support both Add and Edit modes
- ✅ Form field validation
- ✅ Auto-populate for Edit mode
- ✅ Loading state during submission
- ✅ Success/error messages

### API Client
- ✅ Typed with ResponseDTO
- ✅ Automatic error handling
- ✅ Consistent with backend endpoints
- ✅ Type-safe with TypeScript

---

## Permission Integration

**SmartAdmin Permission Pattern**:
```vue
<a-button v-privilege="'{module}:{entity}:add'">Add</a-button>
<a-button v-privilege="'{module}:{entity}:update'">Edit</a-button>
<a-button v-privilege="'{module}:{entity}:delete'">Delete</a-button>
```

**Backend Permission** (SaCheckPermission):
```java
@SaCheckPermission("{module}:{entity}:query")
@SaCheckPermission("{module}:{entity}:add")
@SaCheckPermission("{module}:{entity}:update")
@SaCheckPermission("{module}:{entity}:delete")
```

**Matching**: Frontend `v-privilege` MUST match backend `@SaCheckPermission`

---

## Validation Checklist

After generation, verify:
- [ ] TypeScript types match backend VOs/Forms (field names, types)
- [ ] API endpoints match backend Controller paths
- [ ] Permission strings match backend @SaCheckPermission
- [ ] Form validation rules match backend @Valid annotations
- [ ] Table columns display correct data
- [ ] Pagination works correctly
- [ ] Search form filters work
- [ ] Add/Edit/Delete operations work

---

## Integration Notes

This phase consolidates:
- ✅ Vue 3 Composition API patterns from smartadmin-vue-crud
- ✅ Ant Design Vue components and styling
- ✅ TypeScript type safety (Java ↔ TypeScript mapping)
- ✅ SmartAdmin permission system integration
- ✅ Pinia state management (if needed for complex scenarios)
- ✅ ResponseDTO/PageResult handling patterns
