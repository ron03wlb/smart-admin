# SmartAdmin Frontend Patterns

> **Authoritative source** for SmartAdmin frontend architecture patterns, API integration, and Vue component conventions.
>
> **Last Updated:** 2026-01-21 (v2.2.0)
> **Applies to:** smart-admin-web (Vue 3 + Ant Design Vue)

---

## Overview

SmartAdmin frontend follows strict conventions for project structure, API integration, permission control, and component patterns. This document defines the **mandatory patterns** that all frontend code must follow for consistency and maintainability.

---

## 1. Project Structure & Organization

### Directory Structure

```
smart-admin-web/src/
├── api/
│   ├── base-model/              # ResponseModel, PageResultModel definitions
│   ├── business/                # Business module APIs
│   ├── support/                 # Support module APIs
│   └── system/                  # System module APIs
│       └── employee-api.ts
├── components/
│   ├── business/                # Business-specific components
│   └── framework/               # Shared framework components
│       ├── smart-enum-select/
│       ├── smart-enum-radio/
│       └── smart-enum-checkbox/
├── store/modules/               # Pinia stores (user, dict, menu, etc.)
├── router/                      # Vue Router configuration
└── views/
    ├── business/                # Business modules
    ├── support/                 # Support modules
    └── system/                  # System modules
        └── employee/
            ├── employee-list.vue
            └── components/
                ├── employee-form-modal.vue
                └── employee-form-drawer.vue
```

### Naming Conventions (MANDATORY)

| File Type | Pattern | Example |
|-----------|---------|---------|
| List page | `<module>-list.vue` | `employee-list.vue` |
| Form modal | `<module>-form-modal.vue` | `employee-form-modal.vue` |
| Form drawer | `<module>-form-drawer.vue` | `employee-form-drawer.vue` |
| API module | `api/<category>/<module>-api.ts` | `api/system/employee-api.ts` |
| Nested components | `views/<module>/components/` | `views/employee/components/` |

---

## 2. Backend API Integration

### ResponseModel Structure (MANDATORY)

**All backend APIs return this structure:**

```typescript
interface ResponseModel<T> {
  code: number;        // Response code
  data: T;             // Response data (generic type)
  msg?: string;        // Error/success message
  success: boolean;    // Operation success flag
}
```

**Usage pattern (ALWAYS check `success` first):**

```typescript
const response = await employeeApi.queryEmployee(queryForm);
if (response.success) {
  // Success: use response.data
  tableData.value = response.data.list;
  total.value = response.data.total;
} else {
  // Error: display response.msg to user
  message.error(response.msg || '操作失败');
}
```

### PageResultModel Structure (MANDATORY)

**All paginated queries return this structure:**

```typescript
interface PageResultModel<T> {
  list: Array<T>;      // Result list
  total: number;       // Total records
  pageNum?: number;    // Current page number
  pageSize?: number;   // Page size
  pages?: number;      // Total pages
  emptyFlag?: boolean; // Is result empty
}
```

**Usage pattern:**

```typescript
const response = await employeeApi.queryEmployee(queryForm);
if (response.success) {
  const pageResult: PageResultModel<EmployeeVO> = response.data;
  tableData.value = pageResult.list;   // Extract list for table
  total.value = pageResult.total;       // Extract total for pagination
}
```

### API Request Methods (MANDATORY)

**SmartAdmin provides three standard request methods:**

```typescript
import { getRequest, postRequest, postEncryptRequest } from '/@/lib/axios';

// POST request (most common)
postRequest(url, params)

// GET request (simple queries, deletions)
getRequest(url, params)

// Encrypted POST request (sensitive data)
postEncryptRequest(url, params)
```

### API Module Pattern (MANDATORY)

**Create API modules following this structure:**

```typescript
// src/api/system/employee-api.ts
import { getRequest, postRequest } from '/@/lib/axios';

export const employeeApi = {
  // Query employees (paginated)
  queryEmployee: (params) => {
    return postRequest('/employee/query', params);
  },

  // Add employee
  addEmployee: (params) => {
    return postRequest('/employee/add', params);
  },

  // Update employee
  updateEmployee: (params) => {
    return postRequest('/employee/update', params);
  },

  // Delete employee
  deleteEmployee: (employeeId) => {
    return getRequest(`/employee/delete/${employeeId}`);
  },

  // Batch delete
  batchDelete: (employeeIds) => {
    return postRequest('/employee/batchDelete', employeeIds);
  }
};
```

### Error Handling (MANDATORY)

**Always handle errors consistently:**

```typescript
import { message, notification } from 'ant-design-vue';

// 1. Check response.success before accessing response.data
if (response.success) {
  // Success path
} else {
  // Error path: display response.msg
  message.error(response.msg || '操作失败');
}

// 2. Use try-finally for loading states
tableLoading.value = true;
try {
  const response = await employeeApi.queryEmployee(queryForm);
  // ... handle response
} finally {
  tableLoading.value = false;  // Always reset loading
}

// 3. Use notification for important errors
if (!response.success) {
  notification.error({
    message: '操作失败',
    description: response.msg,
    duration: 5
  });
}
```

---

## 3. Permission System

### v-privilege Directive (MANDATORY)

**All action buttons MUST have permission controls:**

```vue
<!-- Add button -->
<a-button
  v-privilege="'system:employee:add'"
  type="primary"
  @click="showAddModal">
  添加员工
</a-button>

<!-- Edit button -->
<a-button
  v-privilege="'system:employee:update'"
  type="link"
  @click="showEditModal(record)">
  编辑
</a-button>

<!-- Delete button -->
<a-button
  v-privilege="'system:employee:delete'"
  type="link"
  danger
  @click="deleteRecord(record.id)">
  删除
</a-button>

<!-- Batch delete -->
<a-button
  v-privilege="'system:employee:batchDelete'"
  danger
  :disabled="selectedRowKeys.length === 0"
  @click="batchDelete">
  批量删除
</a-button>
```

### Permission Format (MANDATORY)

**Pattern:** `'module:entity:action'`

**Examples:**
- `'system:employee:add'` - Add employee
- `'system:employee:update'` - Update employee
- `'system:employee:delete'` - Delete employee
- `'system:employee:query'` - Query employees
- `'business:order:export'` - Export orders

**Alignment with backend:**
- Frontend `v-privilege="'system:employee:add'"`
- Backend `@SaCheckPermission("system:employee:add")`

**Behavior:**
- Element is **hidden** if user lacks permission (not just disabled)
- Use `useUserStore()` to access current user permissions
- Token managed automatically by Sa-Token integration

---

## 4. SmartAdmin CRUD Patterns

### Pattern 1: List Page (Table + Pagination)

**Component structure:**

```vue
<template>
  <a-card>
    <!-- 1. Query Form -->
    <a-form layout="inline" :model="queryForm">
      <a-form-item label="关键字">
        <a-input
          v-model:value.trim="queryForm.keyword"
          placeholder="姓名/手机号"
          @pressEnter="query"
        />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" @click="query">查询</a-button>
        <a-button @click="reset">重置</a-button>
      </a-form-item>
    </a-form>

    <!-- 2. Action Buttons -->
    <div class="smart-table-operate-wrapper">
      <a-button v-privilege="'system:employee:add'" type="primary" @click="add">
        新增
      </a-button>
    </div>

    <!-- 3. Table -->
    <a-table
      size="small"
      :columns="columns"
      :data-source="tableData"
      :pagination="false"
      :loading="tableLoading"
      row-key="employeeId"
      bordered
    >
      <template #bodyCell="{ text, record, column }">
        <template v-if="column.dataIndex === 'operate'">
          <a-button v-privilege="'system:employee:update'" type="link" @click="edit(record)">
            编辑
          </a-button>
        </template>
      </template>
    </a-table>

    <!-- 4. Pagination -->
    <div class="smart-query-table-page">
      <a-pagination
        showSizeChanger
        v-model:current="queryForm.pageNum"
        v-model:pageSize="queryForm.pageSize"
        :total="total"
        @change="query"
      />
    </div>

    <!-- 5. Form Modal -->
    <EmployeeFormModal ref="formModalRef" @refresh="query" />
  </a-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import { employeeApi } from '/@/api/system/employee-api';

const tableLoading = ref(false);
const tableData = ref([]);
const total = ref(0);

const queryForm = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: ''
});

async function query() {
  tableLoading.value = true;
  try {
    const response = await employeeApi.queryEmployee(queryForm);
    if (response.success) {
      tableData.value = response.data.list;
      total.value = response.data.total;
    } else {
      message.error(response.msg || '查询失败');
    }
  } finally {
    tableLoading.value = false;
  }
}

onMounted(() => {
  query();
});
</script>
```

### Pattern 2: Form Modal (Add/Edit)

**Component structure:**

```vue
<template>
  <a-modal
    :visible="visible"
    :title="form.employeeId ? '编辑员工' : '新增员工'"
    :confirmLoading="confirmLoading"
    @ok="handleSubmit"
    @cancel="close"
    width="600px"
  >
    <a-form
      ref="formRef"
      :model="form"
      :rules="rules"
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 16 }"
    >
      <a-form-item label="姓名" name="name">
        <a-input v-model:value="form.name" placeholder="请输入姓名" />
      </a-form-item>

      <a-form-item label="手机号" name="phone">
        <a-input v-model:value="form.phone" placeholder="请输入手机号" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { message } from 'ant-design-vue';
import { employeeApi } from '/@/api/system/employee-api';

const visible = ref(false);
const confirmLoading = ref(false);
const formRef = ref();

const form = reactive({
  employeeId: undefined,
  name: '',
  phone: ''
});

const rules = {
  name: [{ required: true, message: '请输入姓名' }],
  phone: [
    { required: true, message: '请输入手机号' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }
  ]
};

const emit = defineEmits(['refresh']);

function show(record?) {
  visible.value = true;
  if (record) {
    Object.assign(form, record);
  }
}

function close() {
  visible.value = false;
  formRef.value?.resetFields();
}

async function handleSubmit() {
  try {
    await formRef.value?.validate();
    confirmLoading.value = true;

    const api = form.employeeId ? employeeApi.updateEmployee : employeeApi.addEmployee;
    const response = await api(form);

    if (response.success) {
      message.success(form.employeeId ? '编辑成功' : '添加成功');
      close();
      emit('refresh');
    } else {
      message.error(response.msg || '操作失败');
    }
  } catch (error) {
    // Validation failed
  } finally {
    confirmLoading.value = false;
  }
}

defineExpose({ show });
</script>
```

### Pattern 3: Form Drawer (Alternative to Modal)

**Similar structure to Form Modal, but uses `<a-drawer>` instead of `<a-modal>`:**

```vue
<a-drawer
  :visible="visible"
  :title="form.employeeId ? '编辑员工' : '新增员工'"
  width="600px"
  @close="close"
>
  <!-- Form content identical to modal pattern -->

  <template #footer>
    <a-space>
      <a-button @click="close">取消</a-button>
      <a-button type="primary" :loading="confirmLoading" @click="handleSubmit">
        确定
      </a-button>
    </a-space>
  </template>
</a-drawer>
```

---

## 5. Ant Design Vue Integration

### Table Component

**Standard configuration:**

```vue
<a-table
  size="small"                    <!-- Use 'small' for compact layout -->
  :columns="columns"
  :data-source="tableData"
  :pagination="false"              <!-- Custom pagination below table -->
  :loading="tableLoading"
  :scroll="{ x: 1200 }"           <!-- Horizontal scroll for wide tables -->
  row-key="employeeId"            <!-- Unique key field -->
  bordered                         <!-- Show borders -->
>
  <template #bodyCell="{ text, record, column }">
    <!-- Custom cell rendering -->
  </template>
</a-table>
```

### Pagination Component

**Standard configuration:**

```vue
<a-pagination
  showSizeChanger                 <!-- Allow changing page size -->
  showQuickJumper                 <!-- Allow jumping to page -->
  show-less-items                 <!-- Compact display -->
  :pageSizeOptions="[10, 20, 50, 100]"
  v-model:current="queryForm.pageNum"
  v-model:pageSize="queryForm.pageSize"
  :total="total"
  @change="query"
  :show-total="(total) => `共 ${total} 条`"
/>
```

### Form Component

**Standard configuration:**

```vue
<a-form
  ref="formRef"
  :model="form"
  :rules="rules"
  :label-col="{ span: 6 }"        <!-- Label width -->
  :wrapper-col="{ span: 16 }"     <!-- Input width -->
>
  <a-form-item label="姓名" name="name">
    <a-input v-model:value="form.name" />
  </a-form-item>
</a-form>
```

### Message & Notification

**Use Ant Design Vue feedback components:**

```typescript
import { message, notification, Modal } from 'ant-design-vue';

// Success message (3s auto-close)
message.success('操作成功');

// Error message
message.error(response.msg || '操作失败');

// Warning message
message.warning('请选择至少一条记录');

// Notification (persistent)
notification.error({
  message: '操作失败',
  description: response.msg,
  duration: 5
});

// Confirmation dialog
Modal.confirm({
  title: '提示',
  content: '确定要删除吗?',
  okText: '确定',
  okType: 'danger',
  cancelText: '取消',
  async onOk() {
    // Handle confirmation
  }
});
```

---

## 6. SmartAdmin Custom Components

### smart-enum-select (Enum Dropdown)

```vue
<smart-enum-select
  v-model:value="form.status"
  enumName="EMPLOYEE_STATUS_ENUM"
  placeholder="请选择状态"
  width="200px"
/>
```

### smart-enum-radio (Enum Radio Group)

```vue
<smart-enum-radio
  v-model:value="queryForm.status"
  enumName="EMPLOYEE_STATUS_ENUM"
/>
```

### smart-enum-checkbox (Enum Checkbox Group)

```vue
<smart-enum-checkbox
  v-model:value="form.permissions"
  enumName="PERMISSION_ENUM"
/>
```

### $smartEnumPlugin (Display Enum Description)

```vue
<template>
  <span>{{ $smartEnumPlugin.getDescByValue('GENDER_ENUM', record.gender) }}</span>
</template>
```

### TableOperator (Column Management)

```vue
<TableOperator
  v-model="columns"
  :tableId="TABLE_ID_CONST.SYSTEM.EMPLOYEE"
  :refresh="query"
/>
```

---

## 7. Vue 3 Composition API Standards

### ref vs reactive

**Use `ref()` for:**
- Primitive values (string, number, boolean)
- Arrays and objects that need reassignment
- Template refs (DOM elements, child components)

```typescript
const tableLoading = ref(false);
const tableData = ref([]);
const formModalRef = ref();
```

**Use `reactive()` for:**
- Form objects (rarely reassigned)
- Query parameters

```typescript
const form = reactive({
  employeeId: undefined,
  name: '',
  phone: ''
});

const queryForm = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: ''
});
```

### Lifecycle Hooks

**Common patterns:**

```typescript
import { onMounted, onBeforeUnmount, watch } from 'vue';

// Initialize data on mount
onMounted(() => {
  query();
});

// Cleanup on unmount
onBeforeUnmount(() => {
  // Clear timers, remove listeners
});

// Watch for changes
watch(() => queryForm.keyword, () => {
  // Reset to page 1 when keyword changes
  queryForm.pageNum = 1;
  query();
});
```

### Async/Await Pattern

**Always use try-finally for loading states:**

```typescript
async function query() {
  tableLoading.value = true;
  try {
    const response = await employeeApi.queryEmployee(queryForm);
    if (response.success) {
      tableData.value = response.data.list;
      total.value = response.data.total;
    } else {
      message.error(response.msg || '查询失败');
    }
  } finally {
    tableLoading.value = false;  // Always reset
  }
}
```

---

## 8. TypeScript Integration

### Define Interfaces for DTOs

**Match backend structures:**

```typescript
// Entity (matches backend VO)
export interface EmployeeVO {
  employeeId: number;
  name: string;
  phone: string;
  email?: string;
  gender: number;
  departmentId: number;
  departmentName?: string;
  disabledFlag: boolean;
  createTime: string;
}

// Query form (matches backend QueryForm)
export interface EmployeeQueryForm {
  pageNum: number;
  pageSize: number;
  keyword?: string;
  status?: number;
  departmentId?: number;
}

// Add form (matches backend AddForm)
export interface EmployeeAddForm {
  name: string;
  phone: string;
  email?: string;
  gender: number;
  departmentId: number;
  roleIdList?: number[];
}

// Update form (matches backend UpdateForm)
export interface EmployeeUpdateForm extends EmployeeAddForm {
  employeeId: number;
}
```

---

## 9. State Management (Pinia)

### Use Existing Stores

**User store (authentication):**

```typescript
import { useUserStore } from '/@/store/modules/user';

const userStore = useUserStore();
const currentUser = userStore.userInfo;
const permissions = userStore.permissions;
```

**Dictionary/Enum store:**

```typescript
import { useDictStore } from '/@/store/modules/dict';

const dictStore = useDictStore();
const statusOptions = dictStore.getDict('EMPLOYEE_STATUS');
```

---

## 10. Backend Alignment Checklist

**Before implementing frontend features, verify:**

- ✅ Backend API endpoint exists (check Controller)
- ✅ Request DTO structure matches backend Form
- ✅ Response DTO structure matches backend VO
- ✅ Permission strings match backend `@SaCheckPermission`
- ✅ Enum names match backend enum definitions
- ✅ Error codes and messages are consistent
- ✅ Pagination parameters are correct (pageNum, pageSize)

**Test integration:**
1. Use Swagger/Knife4j to test backend API directly
2. Verify ResponseModel structure in network tab
3. Confirm permission checks work correctly
4. Test error scenarios (validation failures, business logic errors)

---

## Summary

**Key principles:**
1. **Consistency**: Follow naming conventions and patterns strictly
2. **Backend alignment**: Always match backend API contracts and permissions
3. **Error handling**: Check `response.success` and display `response.msg`
4. **Permissions**: Use `v-privilege` on all action buttons
5. **TypeScript**: Define interfaces matching backend DTOs
6. **Loading states**: Use try-finally to ensure loading flags reset
7. **Component patterns**: Use established CRUD patterns (list, form-modal, form-drawer)

**This document is the single source of truth for SmartAdmin frontend patterns. All Vue code must conform to these standards.**
