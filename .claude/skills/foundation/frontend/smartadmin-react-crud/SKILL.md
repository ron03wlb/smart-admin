---
name: smartadmin-react-crud
description: [P0 - Critical] Generate complete React 19 CRUD module for SmartAdmin (TypeScript types + API client + List component + Form modal + Tests). Use when creating new React business modules, implementing CRUD features with Ant Design 5, or scaffolding complete React components. Triggers when (1) User requests "create/generate React CRUD module", (2) User mentions creating React component with CRUD operations, (3) User wants to scaffold React module (frontend only), (4) After defining entity requirements for React frontend, (5) User explicitly requests React code generation.
---

# SmartAdmin React CRUD Generator

Generate complete React 19 CRUD modules from entity specifications - automatically creates TypeScript types, API client, list component with Table/Pagination/Search, form modal with Add/Edit mode, and tests with 80%+ coverage following SmartAdmin React patterns.

## Quick Start

**Most common usage:**
```
User: "Create a Product React CRUD module with name, price, category, and stock fields"
```

You will:
1. Gather entity requirements (fields, types, validations, relationships)
2. Generate TypeScript types (QueryForm, VO, AddForm, UpdateForm)
3. Generate API client with ResponseModel handling
4. Generate React list component (Table + Pagination + Search + PrivilegeButton)
5. Generate React form modal (Add/Edit mode + validation)
6. Generate Vitest + React Testing Library tests (80%+ coverage)

**Time savings**: 4-6 hours → **30 minutes** (92% improvement)

---

## Trigger Keywords

This skill is automatically activated when the user's request contains:

**Primary Keywords** (High confidence):
- "create React CRUD module" - Generate complete React CRUD functionality
- "generate React CRUD" - Scaffold React components with CRUD operations
- "scaffold React module" - Create new React business module from scratch
- "React CRUD generator" - Explicit skill invocation
- "create React component" - Context: with CRUD operations

**Secondary Keywords** (Medium confidence):
- "React list component" - Context: with CRUD table
- "React table component" - Context: with data fetching
- "React form modal" - Context: with Add/Edit mode
- "generate React form" - Context: CRUD workflow

**Phrase Patterns**:
- "Create [Entity] React CRUD module with [fields]" - Example: "Create Product React CRUD module with name, price"
- "Generate React CRUD for [Entity]" - Example: "Generate React CRUD for Employee"
- "I need to scaffold [Entity] React module" - Example: "I need to scaffold Customer React module"

**Example User Requests**:
```
User: "Create a Product React CRUD module with name, price, category, and stock fields"
User: "Generate React CRUD for Employee with firstName, lastName, email, department"
User: "Scaffold an Order React module with orderId, customerId, orderDate, totalAmount"
User: "Create React list and form components for Brand entity"
```

**Note**: This skill can also be manually invoked via `/react-crud` command. Supports phase-based execution: `--phase=1-5` or `--all-phases`.

---

## Phase-Based Execution

### Complete CRUD (All Phases):
```bash
/react-crud Employee --module=system --all-phases
# Generates: TypeScript types + API client + List component + Form modal + Tests
# Time: ~25 minutes (vs 4-6 hours manual coding)
```

### Phase 1: TypeScript Types Only (~3 minutes)
```bash
/react-crud Order --module=business --phase=1
# Generates: order-types.ts (QueryForm, VO, AddForm, UpdateForm)
# 100% compatible with backend DTOs
```

### Phase 2: API Client Only (~5 minutes)
```bash
/react-crud Brand --module=business --phase=2
# Generates: brand-api.ts (postRequest/getRequest wrapper, ResponseModel handling)
# Requires Phase 1 types
```

### Phase 3: List Component Only (~8 minutes)
```bash
/react-crud Product --module=business --phase=3
# Generates: ProductList.tsx (Table, Pagination, Search, PrivilegeButton)
# Requires Phase 1-2
```

### Phase 4: Form Modal Only (~6 minutes)
```bash
/react-crud Employee --module=system --phase=4
# Generates: EmployeeFormModal.tsx (Add/Edit mode, validation, API submission)
# Requires Phase 1-2
```

### Phase 5: Tests Only (~3 minutes)
```bash
/react-crud Employee --module=system --phase=5
# Generates: EmployeeList.spec.tsx, EmployeeFormModal.spec.tsx
# Vitest + React Testing Library, 80%+ coverage
```

---

## Phase 1: TypeScript Types Generation

### Input
Entity definition with fields, types, and validation rules.

### Output
File: `smart-admin-web-react/src/api/{module}/{entity}-types.ts`

### Generated Types

```typescript
/**
 * {Entity} 查詢表單（對應後端 {Entity}QueryForm）
 */
export interface {Entity}QueryForm {
  keyword?: string;           // 搜索關鍵字
  pageNum: number;            // 分頁頁碼
  pageSize: number;           // 分頁大小
  // ... 其他過濾字段（由後端 QueryForm 決定）
}

/**
 * {Entity} VO（對應後端 {Entity}VO）
 */
export interface {Entity}VO {
  {entity}Id: number;         // 主鍵
  // ... 所有字段（含 JOIN 字段）
  createTime: string;         // ISO 8601 格式
  updateTime: string;
}

/**
 * {Entity} 新增表單（對應後端 {Entity}AddForm）
 */
export interface {Entity}AddForm {
  // ... 除 ID 外的所有字段
}

/**
 * {Entity} 更新表單（對應後端 {Entity}UpdateForm）
 */
export interface {Entity}UpdateForm {
  {entity}Id: number;         // 必須有 ID
  // ... 與 AddForm 相同的字段
}

/**
 * {Entity} 批量刪除表單
 */
export interface {Entity}BatchDeleteForm {
  {entity}IdList: number[];
}
```

### Java ↔ TypeScript Type Mapping

| Java Type | TypeScript Type | Example |
|-----------|-----------------|---------|
| Long, Integer | number | `employeeId: number` |
| String | string | `name: string` |
| Boolean | boolean | `deleted: boolean` |
| LocalDateTime | string | `createTime: string` (ISO 8601) |
| BigDecimal | number | `price: number` |
| List&lt;T&gt; | T[] | `List<Long>` → `number[]` |
| Enum | string \| number | `StatusEnum` → `1 \| 2 \| 3` |

---

## Phase 2: API Client Generation

### Output
File: `smart-admin-web-react/src/api/{module}/{entity}-api.ts`

### Generated Code

```typescript
import { postRequest, getRequest } from '@/api/base/request';
import type { PageResult } from '@/api/base/page.model';
import type {
  {Entity}QueryForm,
  {Entity}VO,
  {Entity}AddForm,
  {Entity}UpdateForm,
  {Entity}BatchDeleteForm,
} from './{entity}-types';

const BASE_URL = '/{module}/{entity}';

/**
 * {Entity} API 客戶端
 */
export const {entity}Api = {
  /**
   * 分頁查詢
   */
  query: (queryForm: {Entity}QueryForm) => {
    return postRequest<PageResult<{Entity}VO>>(`${BASE_URL}/query`, queryForm);
  },

  /**
   * 按 ID 獲取詳情
   */
  getById: ({entity}Id: number) => {
    return getRequest<{Entity}VO>(`${BASE_URL}/get/${{{entity}Id}}`);
  },

  /**
   * 添加
   */
  add: (addForm: {Entity}AddForm) => {
    return postRequest<void>(`${BASE_URL}/add`, addForm);
  },

  /**
   * 更新
   */
  update: (updateForm: {Entity}UpdateForm) => {
    return postRequest<void>(`${BASE_URL}/update`, updateForm);
  },

  /**
   * 刪除
   */
  delete: ({entity}Id: number) => {
    return postRequest<void>(`${BASE_URL}/delete/${{{entity}Id}}`);
  },

  /**
   * 批量刪除
   */
  batchDelete: (batchDeleteForm: {Entity}BatchDeleteForm) => {
    return postRequest<void>(`${BASE_URL}/batchDelete`, batchDeleteForm);
  },
};
```

### Key Features
- ✅ Uses SmartAdmin's `postRequest`/`getRequest` wrappers
- ✅ Automatic ResponseModel unpacking
- ✅ TypeScript generics for type safety
- ✅ Consistent error handling

---

## Phase 3: List Component Generation

### Output
File: `smart-admin-web-react/src/views/{module}/{entity}/{Entity}List.tsx`

### Generated Component Structure

```tsx
import React, { useState, useEffect } from 'react';
import { Table, Button, message, Form, Input, Space, Modal } from 'antd';
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import { PrivilegeButton } from '@/components/framework/privilege/PrivilegeButton';
import { {entity}Api } from '@/api/{module}/{entity}-api';
import type { {Entity}VO, {Entity}QueryForm } from '@/api/{module}/{entity}-types';
import { {Entity}FormModal } from './{Entity}FormModal';

/**
 * {Entity} 列表頁面
 */
export const {Entity}List: React.FC = () => {
  // ===== State Management =====
  const [data, setData] = useState<{Entity}VO[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [queryForm, setQueryForm] = useState<{Entity}QueryForm>({
    pageNum: 1,
    pageSize: 10,
    keyword: '',
  });
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [formModalVisible, setFormModalVisible] = useState(false);
  const [currentRecord, setCurrentRecord] = useState<{Entity}VO | null>(null);

  // ===== Data Fetching =====
  const fetchData = async () => {
    setLoading(true);
    try {
      const response = await {entity}Api.query(queryForm);
      if (response.success) {
        setData(response.data.list);
        setTotal(response.data.total);
      } else {
        message.error(response.msg || '查詢失敗');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [queryForm]);

  // ===== Event Handlers =====
  const handleAdd = () => {
    setCurrentRecord(null);
    setFormModalVisible(true);
  };

  const handleEdit = (record: {Entity}VO) => {
    setCurrentRecord(record);
    setFormModalVisible(true);
  };

  const handleDelete = (record: {Entity}VO) => {
    Modal.confirm({
      title: '確認刪除',
      content: `確定要刪除「${record.name}」嗎？`,
      onOk: async () => {
        const response = await {entity}Api.delete(record.{entity}Id);
        if (response.success) {
          message.success('刪除成功');
          fetchData();
        } else {
          message.error(response.msg || '刪除失敗');
        }
      },
    });
  };

  const handleBatchDelete = () => {
    Modal.confirm({
      title: '批量刪除確認',
      content: `確定要刪除選中的 ${selectedRowKeys.length} 條記錄嗎？`,
      onOk: async () => {
        const response = await {entity}Api.batchDelete({
          {entity}IdList: selectedRowKeys as number[],
        });
        if (response.success) {
          message.success('批量刪除成功');
          setSelectedRowKeys([]);
          fetchData();
        } else {
          message.error(response.msg || '批量刪除失敗');
        }
      },
    });
  };

  // ===== Table Columns =====
  const columns = [
    {
      title: 'ID',
      dataIndex: '{entity}Id',
      key: '{entity}Id',
      width: 80,
    },
    // ... 其他字段列（由實體字段決定）
    {
      title: '創建時間',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      fixed: 'right' as const,
      render: (_: any, record: {Entity}VO) => (
        <Space>
          <PrivilegeButton
            permissionCode="{module}:{entity}:update"
            type="link"
            size="small"
            onClick={() => handleEdit(record)}
          >
            編輯
          </PrivilegeButton>
          <PrivilegeButton
            permissionCode="{module}:{entity}:delete"
            type="link"
            size="small"
            danger
            onClick={() => handleDelete(record)}
          >
            刪除
          </PrivilegeButton>
        </Space>
      ),
    },
  ];

  // ===== Render =====
  return (
    <div>
      {/* 搜索表單 */}
      <Form layout="inline" style={{ marginBottom: 16 }}>
        <Form.Item label="關鍵字">
          <Input
            placeholder="請輸入搜索關鍵字"
            value={queryForm.keyword}
            onChange={(e) =>
              setQueryForm({ ...queryForm, keyword: e.target.value })
            }
            onPressEnter={fetchData}
            style={{ width: 200 }}
          />
        </Form.Item>
        <Form.Item>
          <Button type="primary" onClick={fetchData}>
            查詢
          </Button>
          <Button
            onClick={() =>
              setQueryForm({ pageNum: 1, pageSize: 10, keyword: '' })
            }
            style={{ marginLeft: 8 }}
          >
            重置
          </Button>
        </Form.Item>
      </Form>

      {/* 操作按鈕 */}
      <Space style={{ marginBottom: 16 }}>
        <PrivilegeButton
          permissionCode="{module}:{entity}:add"
          type="primary"
          icon={<PlusOutlined />}
          onClick={handleAdd}
        >
          新增
        </PrivilegeButton>
        <PrivilegeButton
          permissionCode="{module}:{entity}:delete"
          danger
          icon={<DeleteOutlined />}
          disabled={selectedRowKeys.length === 0}
          onClick={handleBatchDelete}
        >
          批量刪除
        </PrivilegeButton>
      </Space>

      {/* 數據表格 */}
      <Table
        dataSource={data}
        columns={columns}
        loading={loading}
        rowKey="{entity}Id"
        rowSelection={{
          selectedRowKeys,
          onChange: setSelectedRowKeys,
        }}
        pagination={{
          current: queryForm.pageNum,
          pageSize: queryForm.pageSize,
          total: total,
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 條`,
          onChange: (pageNum, pageSize) =>
            setQueryForm({ ...queryForm, pageNum, pageSize }),
        }}
      />

      {/* 表單模態框 */}
      <{Entity}FormModal
        visible={formModalVisible}
        record={currentRecord}
        onCancel={() => setFormModalVisible(false)}
        onSuccess={() => {
          setFormModalVisible(false);
          fetchData();
        }}
      />
    </div>
  );
};
```

### Key Features
- ✅ Table with Ant Design components
- ✅ Pagination (current, pageSize, total)
- ✅ Search form with keyword input
- ✅ PrivilegeButton for permission control
- ✅ Add/Edit/Delete/BatchDelete operations
- ✅ ResponseModel error handling
- ✅ Loading states

---

## Phase 4: Form Modal Generation

### Output
File: `smart-admin-web-react/src/views/{module}/{entity}/{Entity}FormModal.tsx`

### Generated Component

```tsx
import React, { useEffect } from 'react';
import { Modal, Form, Input, message } from 'antd';
import { {entity}Api } from '@/api/{module}/{entity}-api';
import type { {Entity}VO, {Entity}AddForm, {Entity}UpdateForm } from '@/api/{module}/{entity}-types';

interface {Entity}FormModalProps {
  visible: boolean;
  record: {Entity}VO | null;
  onCancel: () => void;
  onSuccess: () => void;
}

/**
 * {Entity} 表單模態框（Add/Edit 模式）
 */
export const {Entity}FormModal: React.FC<{Entity}FormModalProps> = ({
  visible,
  record,
  onCancel,
  onSuccess,
}) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = React.useState(false);
  const isEdit = record !== null;

  // ===== Form Initialization =====
  useEffect(() => {
    if (visible && record) {
      // Edit mode: fill form with existing data
      form.setFieldsValue(record);
    } else if (visible) {
      // Add mode: reset form
      form.resetFields();
    }
  }, [visible, record, form]);

  // ===== Form Submission =====
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      const response = isEdit
        ? await {entity}Api.update({
            ...values,
            {entity}Id: record.{entity}Id,
          } as {Entity}UpdateForm)
        : await {entity}Api.add(values as {Entity}AddForm);

      if (response.success) {
        message.success(isEdit ? '更新成功' : '添加成功');
        onSuccess();
      } else {
        message.error(response.msg || (isEdit ? '更新失敗' : '添加失敗'));
      }
    } catch (error) {
      // Validation failed or API error
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title={isEdit ? '編輯{Entity}' : '新增{Entity}'}
      open={visible}
      onOk={handleSubmit}
      onCancel={onCancel}
      confirmLoading={loading}
      width={600}
      destroyOnClose
    >
      <Form
        form={form}
        labelCol={{ span: 6 }}
        wrapperCol={{ span: 16 }}
        autoComplete="off"
      >
        {/* 表單字段（由實體字段決定） */}
        <Form.Item
          label="名稱"
          name="name"
          rules={[
            { required: true, message: '請輸入名稱' },
            { min: 2, max: 100, message: '長度為 2-100 個字符' },
          ]}
        >
          <Input placeholder="請輸入名稱" />
        </Form.Item>

        {/* ... 其他字段 ... */}
      </Form>
    </Modal>
  );
};
```

### Key Features
- ✅ Add/Edit mode detection (`record === null` vs `record !== null`)
- ✅ Form validation with Ant Design rules
- ✅ API submission with loading state
- ✅ Success/Error message handling
- ✅ Modal destroyOnClose (reset form)

---

## Phase 5: Test Generation

### Output
Files:
- `smart-admin-web-react/src/views/{module}/{entity}/__tests__/{Entity}List.spec.tsx`
- `smart-admin-web-react/src/views/{module}/{entity}/__tests__/{Entity}FormModal.spec.tsx`

### Generated Tests

```tsx
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { {Entity}List } from '../{Entity}List';
import { {entity}Api } from '@/api/{module}/{entity}-api';

// Mock API
vi.mock('@/api/{module}/{entity}-api');

describe('{Entity}List Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should fetch and display data on mount', async () => {
    const mockData = {
      success: true,
      data: {
        list: [
          { {entity}Id: 1, name: 'Test {Entity}', createTime: '2026-03-06' },
        ],
        total: 1,
      },
    };
    vi.mocked({entity}Api.query).mockResolvedValue(mockData);

    render(<{Entity}List />);

    await waitFor(() => {
      expect(screen.getByText('Test {Entity}')).toBeInTheDocument();
    });

    expect({entity}Api.query).toHaveBeenCalledWith({
      pageNum: 1,
      pageSize: 10,
      keyword: '',
    });
  });

  it('should handle search keyword change', async () => {
    const user = userEvent.setup();
    render(<{Entity}List />);

    const searchInput = screen.getByPlaceholderText('請輸入搜索關鍵字');
    await user.type(searchInput, 'test');

    const queryButton = screen.getByRole('button', { name: '查詢' });
    await user.click(queryButton);

    await waitFor(() => {
      expect({entity}Api.query).toHaveBeenCalledWith(
        expect.objectContaining({ keyword: 'test' })
      );
    });
  });

  it('should open form modal when add button clicked', async () => {
    const user = userEvent.setup();
    render(<{Entity}List />);

    const addButton = screen.getByRole('button', { name: /新增/i });
    await user.click(addButton);

    // Assert modal opened
    await waitFor(() => {
      expect(screen.getByText(/新增{Entity}/i)).toBeInTheDocument();
    });
  });

  it('should call delete API when delete button clicked', async () => {
    const mockData = {
      success: true,
      data: {
        list: [
          { {entity}Id: 1, name: 'Test {Entity}', createTime: '2026-03-06' },
        ],
        total: 1,
      },
    };
    vi.mocked({entity}Api.query).mockResolvedValue(mockData);
    vi.mocked({entity}Api.delete).mockResolvedValue({ success: true });

    const user = userEvent.setup();
    render(<{Entity}List />);

    await waitFor(() => {
      expect(screen.getByText('Test {Entity}')).toBeInTheDocument();
    });

    const deleteButton = screen.getByRole('button', { name: /刪除/i });
    await user.click(deleteButton);

    // Confirm modal
    const confirmButton = screen.getByRole('button', { name: '確定' });
    await user.click(confirmButton);

    await waitFor(() => {
      expect({entity}Api.delete).toHaveBeenCalledWith(1);
    });
  });
});
```

### Test Coverage Requirements
- ✅ Component rendering
- ✅ Data fetching (Mock API)
- ✅ User interactions (click, type, submit)
- ✅ Form validation
- ✅ Permission control (usePrivilege Hook)
- ✅ Error handling
- **Target**: >= 80% coverage

---

## Vue vs React Pattern Mapping

| Feature | Vue 3 | React 19 |
|---------|-------|---------|
| **State Management** | `reactive({ count: 0 })` | `useState(0)` |
| **Two-Way Binding** | `v-model="form.name"` | `value={form.name} onChange={...}` |
| **Permission Control** | `v-privilege="'system:user:add'"` | `<PrivilegeButton permissionCode="system:user:add">` |
| **List Rendering** | `v-for="item in list"` | `list.map(item => ...)` |
| **Conditional Rendering** | `v-if="visible"` | `{visible && <Component />}` |
| **Event Handling** | `@click="handleClick"` | `onClick={handleClick}` |
| **Component Props** | `<MyComponent :prop="value" />` | `<MyComponent prop={value} />` |
| **Emit Events** | `emit('update', data)` | `onUpdate(data)` (callback) |
| **Side Effects** | `watchEffect(() => {...})` | `useEffect(() => {...}, [])` |
| **Form Validation** | `rules={[...]}` | `rules={[...]}` (Ant Design) |

---

## Permission Alignment Checklist

**CRITICAL**: Ensure frontend permission codes match backend annotations **exactly**.

### Backend (Java)
```java
@SaCheckPermission("system:employee:query")
@PostMapping("/system/employee/query")
public ResponseDTO<PageResult<EmployeeVO>> query(...) { ... }

@SaCheckPermission("system:employee:add")
@PostMapping("/system/employee/add")
public ResponseDTO<Void> add(...) { ... }
```

### Frontend (React)
```tsx
<PrivilegeButton permissionCode="system:employee:add">新增</PrivilegeButton>
<PrivilegeButton permissionCode="system:employee:update">編輯</PrivilegeButton>
<PrivilegeButton permissionCode="system:employee:delete">刪除</PrivilegeButton>
```

### Validation
- ✅ Permission string format: `{module}:{entity}:{operation}`
- ✅ Frontend `permissionCode` === Backend `@SaCheckPermission` value
- ✅ usePrivilege Hook checks `pointsList.webPerms`
- ✅ Super admin (`administratorFlag=true`) bypasses all checks

---

## Validation Checklist

### Pre-Execution Checks
- [ ] SmartAdmin project root detected
- [ ] Node.js 18.0+ available
- [ ] `smart-admin-web-react/` directory exists
- [ ] Entity name provided (CamelCase)
- [ ] Module name provided (system/business/oa)

### Post-Execution Checks
- [ ] **TypeScript Compilation**: `npm run type-check` (0 errors)
- [ ] **Vitest Tests**: `npm run test` (All pass, >= 80% coverage)
- [ ] **ESLint**: `npm run lint` (0 errors)
- [ ] **Permission Alignment**: Frontend codes match backend annotations
- [ ] **ResponseModel Handling**: `response.success` checked before data access
- [ ] **API Integration**: Endpoints match backend Controller paths

### Quality Gates
```bash
# Run from smart-admin-web-react/ directory
npm run type-check     # TypeScript compilation (MUST pass)
npm run test           # Vitest tests (MUST >= 80% coverage)
npm run lint           # ESLint (MUST pass)
```

---

## Common Issues & Solutions

### Issue 1: TypeScript Type Errors
**Symptom**: `Type 'X' is not assignable to type 'Y'`

**Solution**:
- Verify backend DTO types match frontend types
- Check Java → TypeScript type mapping (Long → number, LocalDateTime → string)
- Ensure ResponseModel generic types are correct

### Issue 2: Permission Not Working
**Symptom**: Button visible despite lacking permission

**Solution**:
- Check `permissionCode` spelling (must match backend exactly)
- Verify `pointsList` contains the permission in Redux state
- Check `administratorFlag` (super admin bypasses checks)
- Use React DevTools to inspect usePrivilege return value

### Issue 3: API Call Fails
**Symptom**: `response.success === false`

**Solution**:
- Verify API endpoint path (must match backend `@PostMapping`/`@GetMapping`)
- Check request payload structure (must match backend `@RequestBody`)
- Inspect Network tab for HTTP status code
- Check backend console for error logs

### Issue 4: Tests Failing
**Symptom**: Vitest tests fail or timeout

**Solution**:
- Ensure all API methods are mocked (`vi.mock('@/api/...')`)
- Use `waitFor` for async operations
- Check test data matches component expectations
- Verify user-event setup (`userEvent.setup()`)

---

## Example: Complete Employee CRUD

### Generated Files (5 files, ~25 minutes)

```
smart-admin-web-react/src/
├── api/system/
│   ├── employee-types.ts              # Phase 1 (3 min)
│   └── employee-api.ts                # Phase 2 (5 min)
└── views/system/employee/
    ├── EmployeeList.tsx               # Phase 3 (8 min)
    ├── EmployeeFormModal.tsx          # Phase 4 (6 min)
    └── __tests__/
        ├── EmployeeList.spec.tsx      # Phase 5 (3 min)
        └── EmployeeFormModal.spec.tsx
```

### Verification Commands

```bash
cd smart-admin-web-react

# 1. TypeScript compilation
npm run type-check
# Expected: ✓ No errors

# 2. Run tests
npm run test
# Expected: All tests pass, Coverage >= 80%

# 3. Lint check
npm run lint
# Expected: ✓ No errors

# 4. Start dev server
npm run dev
# Navigate to http://localhost:8082/system/employee
```

---

## Advanced Usage

### Custom Hooks Integration
Generate `useEmployeeCrud` custom Hook for reusable logic:

```tsx
export function useEmployeeCrud() {
  const [data, setData] = useState<EmployeeVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [queryForm, setQueryForm] = useState<EmployeeQueryForm>({
    pageNum: 1,
    pageSize: 10,
    keyword: '',
  });

  const query = useCallback(async () => {
    setLoading(true);
    try {
      const response = await employeeApi.query(queryForm);
      if (response.success) {
        setData(response.data.list);
        setTotal(response.data.total);
      }
    } finally {
      setLoading(false);
    }
  }, [queryForm]);

  return { data, loading, total, queryForm, setQueryForm, query };
}
```

### Redux Slice Integration (Optional)
Generate Redux Toolkit slice for global state:

```tsx
import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface EmployeeState {
  list: EmployeeVO[];
  total: number;
  loading: boolean;
}

const employeeSlice = createSlice({
  name: 'employee',
  initialState: {
    list: [],
    total: 0,
    loading: false,
  } as EmployeeState,
  reducers: {
    setEmployeeList: (state, action: PayloadAction<EmployeeVO[]>) => {
      state.list = action.payload;
    },
    setTotal: (state, action: PayloadAction<number>) => {
      state.total = action.payload;
    },
  },
});
```

---

## Best Practices

### 1. Component Organization
- ✅ One component per file
- ✅ Co-locate tests with components (`__tests__/` directory)
- ✅ Group related components in module folder

### 2. State Management
- ✅ Use local state for UI-only state (loading, visible)
- ✅ Use Redux for global state (user, menu, shared data)
- ✅ Avoid prop drilling (use Context or Redux)

### 3. Performance
- ✅ Use `React.memo` for pure components
- ✅ Use `useCallback` for event handlers passed to children
- ✅ Use `useMemo` for expensive calculations
- ✅ Virtualize long lists (react-window)

### 4. Testing
- ✅ Test user interactions, not implementation details
- ✅ Mock API calls, not business logic
- ✅ Use `@testing-library/user-event` for realistic interactions
- ✅ Aim for 80%+ coverage

### 5. TypeScript
- ✅ Define types for all props and state
- ✅ Use `interface` for object shapes
- ✅ Avoid `any`, use `unknown` if truly unknown
- ✅ Leverage utility types (`Omit`, `Pick`, `Partial`)

---

## Version History

### v1.0.0 (2026-03-06)
- ✅ Initial release
- ✅ Phase-based execution (5 phases)
- ✅ TypeScript types generation
- ✅ API client generation
- ✅ List component generation (Table + Pagination + Search)
- ✅ Form modal generation (Add/Edit mode)
- ✅ Test generation (Vitest + React Testing Library)
- ✅ Permission control integration (usePrivilege Hook)
- ✅ ResponseModel error handling
- ✅ 80%+ test coverage requirement

---

## Related Skills

- **smartadmin-crud-generator** (Vue 3 version) - Full-stack CRUD for Vue
- **test-fixture-generator** - Generate test data builders
- **smartadmin-integration-test** - Spring Boot integration tests

---

## Support

**Documentation**:
- [Quick Reference](knowledge/quick-reference.md) - React 19 patterns and examples
- [SmartAdmin Patterns](.claude/shared/knowledge/smartadmin-patterns.md) - Backend patterns
- [Architecture Rules](CLAUDE.md) - Layered architecture

**External Resources**:
- [React 19 Docs](https://react.dev/)
- [Ant Design React](https://ant.design/)
- [Redux Toolkit](https://redux-toolkit.js.org/)
- [Vitest](https://vitest.dev/)
- [React Testing Library](https://testing-library.com/react)

---

**Skill Version**: 1.0.0
**Last Updated**: 2026-03-06
**Status**: Stable
**Estimated Time Savings**: 92% (4-6 hours → 30 minutes)
